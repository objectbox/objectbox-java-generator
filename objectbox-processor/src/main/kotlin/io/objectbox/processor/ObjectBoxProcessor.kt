package io.objectbox.processor

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Relation
import io.objectbox.annotation.Transient
import io.objectbox.annotation.Uid
import io.objectbox.generator.BoxGenerator
import io.objectbox.generator.IdUid
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.idsync.IdSyncException
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.Property.PropertyBuilder
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.collections.LinkedHashSet

open class ObjectBoxProcessor : AbstractProcessor() {

    companion object {
        val OPTION_MODEL_PATH: String = "objectbox.modelPath"
        val OPTION_DAO_COMPAT: String = "objectbox.daoCompat"
        val OPTION_DAO_PACKAGE: String = "objectbox.daoPackage"
    }

    // make processed schema accessible for testing
    var schema: Schema? = null

    private var elementUtils: Elements? = null
    private var typeUtils: Types? = null
    private var filer: Filer? = null
    private var projectRoot: File? = null
    private var customModelPath: String? = null
    private var daoCompat: Boolean = false
    private var daoCompatPackage: String? = null

    @Synchronized override fun init(env: ProcessingEnvironment) {
        super.init(env)

        elementUtils = env.elementUtils
        typeUtils = env.typeUtils
        filer = env.filer

        customModelPath = env.options.get(OPTION_MODEL_PATH)
        daoCompat = "true" == env.options.get(OPTION_DAO_COMPAT)
        daoCompatPackage = env.options.get(OPTION_DAO_PACKAGE)

        try {
            projectRoot = findProjectRoot(env.filer)
        } catch (e: FileNotFoundException) {
            throw FileNotFoundException("Could not find project root: ${e.message}")
        }
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        val types = LinkedHashSet<String>()
        types.add(Entity::class.java.canonicalName)
        return types
    }

    override fun getSupportedOptions(): MutableSet<String> {
        val options = LinkedHashSet<String>()
        options.add(OPTION_MODEL_PATH)
        options.add(OPTION_DAO_PACKAGE)
        return options
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, env: RoundEnvironment): Boolean {
        findAndParse(env)
        return false
    }

    private fun findAndParse(env: RoundEnvironment) {
        var schema: Schema? = null
        val toOneByEntity: MutableMap<io.objectbox.generator.model.Entity, MutableList<ToOneRelation>>
                = mutableMapOf()

        for (entity in env.getElementsAnnotatedWith(Entity::class.java)) {
            note(entity, "Processing @Entity annotation.")

            if (schema == null) {
                val defaultJavaPackage = if (daoCompat && daoCompatPackage != null) {
                    daoCompatPackage
                } else {
                    val elementPackage = elementUtils!!.getPackageOf(entity)
                    elementPackage.qualifiedName.toString()
                }
                schema = Schema("default", 1, defaultJavaPackage)
            }

            parseEntity(schema, toOneByEntity, entity)
        }

        if (schema == null) {
            return  // no entities found
        }

        for (entity in schema.entities) {
            for (toOne in toOneByEntity[entity]!!) {
                if (!resolveToOne(schema, entity, toOne)) {
                    return // resolving to-one failed
                }
            }
        }

        if (!syncIdModel(schema)) {
            return // id model sync failed
        }

        this.schema = schema // make processed schema accessible for testing

        try {
            BoxGenerator(daoCompat).generateAll(schema, filer)
        } catch (e: Exception) {
            printMessage(Diagnostic.Kind.ERROR, "Code generation failed: ${e.message}")
        }
    }

    private fun parseEntity(schema: Schema,
            toOneByEntity: MutableMap<io.objectbox.generator.model.Entity, MutableList<ToOneRelation>>,
            entity: Element) {
        val entityModel = schema.addEntity(entity.simpleName.toString())
        entityModel.isSkipGeneration = true // processor may not generate duplicate entity source files
        entityModel.isConstructors = true // has no effect as isSkipGeneration = true
        entityModel.isSkipCreationInDb = false
        entityModel.javaPackage = elementUtils!!.getPackageOf(entity).qualifiedName.toString()
        entityModel.javaPackageDao = daoCompatPackage ?: entityModel.javaPackage
        entityModel.javaPackageTest = entityModel.javaPackageDao // has no effect as tests can not be generated

        // @NameInDb
        val nameInDbAnnotation = entity.getAnnotation(NameInDb::class.java)
        if (nameInDbAnnotation != null) {
            if (nameInDbAnnotation.value.isNotEmpty()) {
                entityModel.dbName = nameInDbAnnotation.value
            }
        }

        // @Uid
        val uidAnnotation = entity.getAnnotation(Uid::class.java)
        if (uidAnnotation != null && uidAnnotation.value != 0L) {
            entityModel.modelUid = uidAnnotation.value
        }

        // TODO ut: add missing entity build steps
        // compare with io.objectbox.codemodifier.GreendaoModelTranslator.convertEntities

        // parse properties
        val toOnes = mutableListOf<ToOneRelation>()
        toOneByEntity.put(entityModel, toOnes)

        val fields = ElementFilter.fieldsIn(entity.enclosedElements)
        for (field in fields) {
            val relationAnnotation = field.getAnnotation(Relation::class.java)
            if (relationAnnotation != null) {
                // @Relation property
                val toOne = parseRelation(field, relationAnnotation)
                toOnes.add(toOne)
            } else {
                // regular property
                parseProperty(entityModel, field)
            }
        }

        // add missing foreign key properties and indexes for to-one relations
        for (toOne in toOnes) {
            ensureForeignKeys(entityModel, toOne)
        }
    }

    private fun parseRelation(field: VariableElement, relationAnnotation: Relation): ToOneRelation {
        // get simple name for type
        val typeElement = elementUtils!!.getTypeElement(field.asType().toString())
        val targetEntityName = typeElement.simpleName

        val targetIdName = if (relationAnnotation.idProperty.isBlank()) null else relationAnnotation.idProperty

        val nameInDbAnnotation = field.getAnnotation(NameInDb::class.java)
        val targetIdDbName = if (nameInDbAnnotation != null && nameInDbAnnotation.value.isNotEmpty())
            nameInDbAnnotation.value else null

        val uidAnnotation = field.getAnnotation(Uid::class.java)
        val targetIdUid = if (uidAnnotation != null && uidAnnotation.value != 0L) uidAnnotation.value else null

        return ToOneRelation(
                propertyName = field.simpleName.toString(),
                targetEntityName = targetEntityName.toString(),
                targetIdName = targetIdName,
                targetIdDbName = targetIdDbName,
                targetIdUid = targetIdUid,
                variableIsToOne = false
        )
    }

    private fun parseProperty(entity: io.objectbox.generator.model.Entity, field: VariableElement) {

        // Compare with EntityClassASTVisitor.endVisit()
        // and GreendaoModelTranslator.convertProperty()

        val enclosingElement = field.enclosingElement as TypeElement
        val fieldName = field.simpleName

        // ignore static, transient or @Transient fields
        val modifiers = field.modifiers
        if (modifiers.contains(Modifier.STATIC)
                || modifiers.contains(Modifier.TRANSIENT)
                || field.getAnnotation(Transient::class.java) != null) {
            note(field, "Ignoring transient field. (%s.%s)", enclosingElement.qualifiedName, fieldName)
            return
        }

        // verify field is accessible
        if (modifiers.contains(Modifier.PRIVATE)) {
            error(field, "Field must not be private. (%s.%s)", enclosingElement.qualifiedName, fieldName)
            return
        }

        val propertyBuilder: Property.PropertyBuilder?

        val convertAnnotation = field.getAnnotation(Convert::class.java)
        if (convertAnnotation != null) {
            // verify @Convert custom type
            propertyBuilder = parseCustomProperty(entity, field, enclosingElement)
        } else {
            // verify that supported type is used
            propertyBuilder = parseSupportedProperty(entity, field, enclosingElement)
        }
        if (propertyBuilder == null) {
            return
        }

        // checks above ensure field is NOT private
        propertyBuilder.fieldAccessible()

        // @Id
        val idAnnotation = field.getAnnotation(Id::class.java)
        if (idAnnotation != null) {
            propertyBuilder.primaryKey()
            if (idAnnotation.assignable) {
                propertyBuilder.idAssignable()
            }
        }

        // @NameInDb
        val nameInDbAnnotation = field.getAnnotation(NameInDb::class.java)
        if (nameInDbAnnotation != null) {
            val name = nameInDbAnnotation.value
            if (name.isNotEmpty()) {
                propertyBuilder.dbName(name)
            }
        } else if (idAnnotation != null && propertyBuilder.property.propertyType == PropertyType.Long) {
            // use special name for @Id column if type is Long
            propertyBuilder.dbName("_id")
        }

        // @Index
        val indexAnnotation = field.getAnnotation(Index::class.java)
        if (indexAnnotation != null) {
            propertyBuilder.indexAsc(null, false)
        }

        // @Uid
        val uidAnnotation = field.getAnnotation(Uid::class.java)
        if (uidAnnotation != null && uidAnnotation.value != 0L) {
            // just storing uid, id model sync will replace with correct id+uid
            propertyBuilder.modelId(IdUid(0, uidAnnotation.value))
        }

        // TODO ut: add remaining property build steps
    }

    private fun parseCustomProperty(entity: io.objectbox.generator.model.Entity,
            field: VariableElement, enclosingElement: TypeElement): Property.PropertyBuilder? {
        // extract @Convert annotation member values
        // as they are types, need to access them via annotation mirrors
        val annotationMirror = getAnnotationMirror(field, Convert::class.java) ?: return null // did not find @Convert mirror

        val converter = getAnnotationValueType(annotationMirror, "converter")
        if (converter == null) {
            error(field, "@Convert requires a value for converter. (%s.%s)",
                    enclosingElement.qualifiedName, field.simpleName)
            return null
        }

        val dbType = getAnnotationValueType(annotationMirror, "dbType")
        if (dbType == null) {
            error(field, "@Convert requires a value for dbType. (%s.%s)",
                    enclosingElement.qualifiedName, field.simpleName)
            return null
        }
        val propertyType = getPropertyType(dbType)
        if (propertyType == null) {
            error(field, "@Convert dbType type is not supported. (%s.%s)",
                    enclosingElement.qualifiedName, field.simpleName)
            return null
        }

        val propertyBuilder = entity.addProperty(propertyType, field.simpleName.toString())
        propertyBuilder.customType(field.asType().toString(), converter.toString())
        // note: custom types are already assumed non-primitive by Property#isNonPrimitiveType()
        return propertyBuilder
    }

    private fun parseSupportedProperty(entity: io.objectbox.generator.model.Entity, field: VariableElement,
            enclosingElement: TypeElement): PropertyBuilder? {
        val typeMirror = field.asType()
        val propertyType = getPropertyType(typeMirror)
        if (propertyType == null) {
            error(field, "Field type is not supported, maybe add @Convert. (%s.%s)",
                    enclosingElement.qualifiedName, field.simpleName)
            return null
        }

        val propertyBuilder = entity.addProperty(propertyType, field.simpleName.toString())

        val isPrimitive = typeMirror.kind.isPrimitive
        if (isPrimitive) {
            // treat primitive types as non-null
            propertyBuilder.notNull()
        } else if (propertyType.isScalar) {
            // treat wrapper types (Long, Integer, ...) of scalar types as non-primitive
            propertyBuilder.nonPrimitiveType()
        }

        return propertyBuilder
    }

    private fun ensureForeignKeys(entityModel: io.objectbox.generator.model.Entity, toOne: ToOneRelation) {
        if (toOne.targetIdName == null) {
            toOne.targetIdName = "${toOne.propertyName}Id"
        }

        val foreignKeyProperty = entityModel.findPropertyByName(toOne.targetIdName)
        if (foreignKeyProperty == null) {
            // foreign key property not explicitly defined in entity, create a virtual one

            val propertyBuilder = entityModel.addProperty(PropertyType.Long, toOne.targetIdName)
            propertyBuilder.notNull()
            propertyBuilder.fieldAccessible()
            propertyBuilder.dbName(toOne.targetIdDbName)
            // just storing uid, id model sync will replace with correct id+uid
            if (toOne.targetIdUid != null) {
                propertyBuilder.modelId(IdUid(0, toOne.targetIdUid))
            }
            // TODO mj: ensure generator's ToOne uses the same targetName (ToOne.nameToOne)
            val targetName = if (toOne.variableIsToOne) toOne.propertyName else "${toOne.propertyName}ToOne"
            propertyBuilder.virtualTargetName(targetName)
        }

        // ensure there is an index on the foreign key property
        val indexForProperty = entityModel.indexes.find {
            // matching for the property is enough as none other than single property ASC indexes are supported
            it.properties.singleOrNull { it.propertyName == toOne.targetIdName } != null
        }
        if (indexForProperty == null) {
            val index = io.objectbox.generator.model.Index()
            index.addPropertyAsc(foreignKeyProperty)
            entityModel.addIndex(index)
        }
    }

    private fun resolveToOne(schema: Schema, entity: io.objectbox.generator.model.Entity, toOne: ToOneRelation): Boolean {
        val targetEntity = schema.entities.singleOrNull {
            it.className == toOne.targetEntityName
        }
        if (targetEntity == null) {
            error("Relation target class ${toOne.targetEntityName} " +
                    "defined in class ${entity.className} could not be found (is it an @Entity?)")
            return false
        }

        val targetIdProperty = entity.findPropertyByName(toOne.targetIdName)
        if (targetIdProperty == null) {
            error("Could not find property ${toOne.targetIdName} in ${entity.className}.")
            return false
        }

        val name = toOne.propertyName
        val nameToOne = if (toOne.variableIsToOne) name else null

        entity.addToOne(targetEntity, targetIdProperty, name, nameToOne)
        return true
    }

    private fun syncIdModel(schema: Schema): Boolean {
        val modelFilePath = if (customModelPath.isNullOrEmpty()) "objectbox-models/default.json" else customModelPath
        val modelFile = File(projectRoot, modelFilePath)
        if (!modelFile.isFile) {
            printMessage(Diagnostic.Kind.ERROR, "Could not find model file at '${modelFile.absolutePath}'.")
            return false
        }

        val idSync: IdSync
        try {
            idSync = IdSync(modelFile)
            idSync.sync(schema)
        } catch (e: IdSyncException) {
            printMessage(Diagnostic.Kind.ERROR, e.message ?: "Could not sync id model for unknown reason.")
            return false
        }

        schema.lastEntityId = idSync.lastEntityId
        schema.lastIndexId = idSync.lastIndexId

        return true
    }

    private fun getAnnotationMirror(element: Element, annotationClass: Class<*>): AnnotationMirror? {
        val annotationMirrors = element.annotationMirrors
        for (annotationMirror in annotationMirrors) {
            val annotationType = annotationMirror.annotationType
            val convertType = elementUtils!!.getTypeElement(annotationClass.canonicalName).asType()
            if (typeUtils!!.isSameType(annotationType, convertType)) {
                return annotationMirror
            }
        }
        return null
    }

    private fun getAnnotationValueType(annotationMirror: AnnotationMirror, memberName: String): TypeMirror? {
        val elementValues = annotationMirror.elementValues
        for ((key, value) in elementValues) {
            val elementName = key.simpleName.toString()
            if (elementName == memberName) {
                // this is a shortcut instead of using entry.getValue().accept(visitor, null)
                return value.value as TypeMirror
            }
        }
        return null
    }

    private fun getPropertyType(typeMirror: TypeMirror): PropertyType? {
        // TODO ut: this only works for Java types, not Kotlin types
        if (isTypeEqual(typeMirror, java.lang.Short::class.java.name) || typeMirror.kind == TypeKind.SHORT) {
            return PropertyType.Short
        }
        if (isTypeEqual(typeMirror, java.lang.Integer::class.java.name) || typeMirror.kind == TypeKind.INT) {
            return PropertyType.Int
        }
        if (isTypeEqual(typeMirror, java.lang.Long::class.java.name) || typeMirror.kind == TypeKind.LONG) {
            return PropertyType.Long
        }

        if (isTypeEqual(typeMirror, java.lang.Float::class.java.name) || typeMirror.kind == TypeKind.FLOAT) {
            return PropertyType.Float
        }
        if (isTypeEqual(typeMirror, java.lang.Double::class.java.name) || typeMirror.kind == TypeKind.DOUBLE) {
            return PropertyType.Double
        }

        if (isTypeEqual(typeMirror, java.lang.Boolean::class.java.name) || typeMirror.kind == TypeKind.BOOLEAN) {
            return PropertyType.Boolean
        }
        if (isTypeEqual(typeMirror, java.lang.Byte::class.java.name) || typeMirror.kind == TypeKind.BYTE) {
            return PropertyType.Byte
        }
        if (isTypeEqual(typeMirror, Date::class.java.name)) {
            return PropertyType.Date
        }
        if (isTypeEqual(typeMirror, java.lang.String::class.java.name)) {
            return PropertyType.String
        }

        if (typeMirror.kind == TypeKind.ARRAY) {
            val arrayType = typeMirror as ArrayType
            if (arrayType.componentType.kind == TypeKind.BYTE) {
                return PropertyType.ByteArray
            }
        }

        return null
    }

    private fun isTypeEqual(typeMirror: TypeMirror, otherType: String): Boolean {
        return otherType == typeMirror.toString()
    }

    private fun error(message: String, vararg args: Any) {
        printMessage(Diagnostic.Kind.ERROR, message, args)
    }

    private fun error(element: Element, message: String, vararg args: Any) {
        printMessage(Diagnostic.Kind.ERROR, message, element, args)
    }

    private fun note(element: Element, message: String, vararg args: Any) {
        printMessage(Diagnostic.Kind.NOTE, message, element, args)
    }

    private fun printMessage(kind: Diagnostic.Kind, message: String, vararg args: Any) {
        val finalMessage = if (args.isNotEmpty()) String.format(message, args) else message
        processingEnv.messager.printMessage(kind, finalMessage)
    }

    private fun printMessage(kind: Diagnostic.Kind, message: String, element: Element, args: Array<out Any>) {
        val finalMessage = if (args.isNotEmpty()) String.format(message, *args) else message
        processingEnv.messager.printMessage(kind, finalMessage, element)
    }
}
