package io.objectbox.processor

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Relation
import io.objectbox.annotation.Transient
import io.objectbox.annotation.Uid
import io.objectbox.codemodifier.nullIfBlank
import io.objectbox.generator.BoxGenerator
import io.objectbox.generator.IdUid
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.idsync.IdSyncException
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.Property.PropertyBuilder
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
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
import kotlin.properties.Delegates

open class ObjectBoxProcessor : AbstractProcessor() {

    companion object {
        val OPTION_MODEL_PATH: String = "objectbox.modelPath"
        val OPTION_DAO_COMPAT: String = "objectbox.daoCompat"
        val OPTION_DAO_PACKAGE: String = "objectbox.daoPackage"
    }

    // make processed schema accessible for testing
    var schema: Schema? = null

    private var elementUtils: Elements by Delegates.notNull()
    private var typeUtils: Types by Delegates.notNull()
    private var filer: Filer by Delegates.notNull()
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
        val toOneByEntity: MutableMap<io.objectbox.generator.model.Entity, MutableList<ToOneRelation>> = mutableMapOf()
        val toManyByEntity: MutableMap<io.objectbox.generator.model.Entity, MutableList<ToManyRelation>> = mutableMapOf()

        for (entity in env.getElementsAnnotatedWith(Entity::class.java)) {
            if (schema == null) {
                val defaultJavaPackage = if (daoCompat && daoCompatPackage != null) {
                    daoCompatPackage
                } else {
                    val elementPackage = elementUtils.getPackageOf(entity)
                    elementPackage.qualifiedName.toString()
                }
                schema = Schema("default", 1, defaultJavaPackage)
            }

            parseEntity(schema, toOneByEntity, toManyByEntity, entity)
        }

        if (schema == null) {
            return  // no entities found
        }

        // resolve to-one relations
        for (entity in schema.entities) {
            for (toOne in toOneByEntity[entity]!!) {
                if (!resolveToOne(schema, entity, toOne)) {
                    return // resolving to-one failed
                }
            }
        }
        // then resolve to-many relations which depend on to-one relations being resolved
        for (entity in schema.entities) {
            for (toMany in toManyByEntity[entity]!!) {
                if (!resolveToMany(schema, entity, toMany)) {
                    return // resolving to-many failed
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
            error("Code generation failed: ${e.message}")
            e.printStackTrace() // TODO ut: might want to include in above error message in the future
        }
    }

    private fun parseEntity(schema: Schema,
            toOneByEntity: MutableMap<io.objectbox.generator.model.Entity, MutableList<ToOneRelation>>,
            toManyByEntity: MutableMap<io.objectbox.generator.model.Entity, MutableList<ToManyRelation>>,
            entity: Element) {
        val entityModel = schema.addEntity(entity.simpleName.toString())
        entityModel.isSkipGeneration = true // processor may not generate duplicate entity source files
        entityModel.isConstructors = true // has no effect as isSkipGeneration = true
        entityModel.isSkipCreationInDb = false
        entityModel.javaPackage = elementUtils.getPackageOf(entity).qualifiedName.toString()
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
        val toManys = mutableListOf<ToManyRelation>()
        toManyByEntity.put(entityModel, toManys)

        val fields = ElementFilter.fieldsIn(entity.enclosedElements)
        for (field in fields) {
            parseField(entityModel, toOnes, toManys, field)
        }

        // add missing foreign key properties and indexes for to-one relations
        for (toOne in toOnes) {
            ensureForeignKeys(entityModel, toOne)
        }
    }

    private fun parseField(entityModel: io.objectbox.generator.model.Entity,
            toOnes: MutableList<ToOneRelation>, toManys: MutableList<ToManyRelation>, field: VariableElement) {
        // ignore static, transient or @Transient fields
        val modifiers = field.modifiers
        if (modifiers.contains(Modifier.STATIC)
                || modifiers.contains(Modifier.TRANSIENT)
                || field.hasAnnotation(Transient::class.java)) {
            return
        }

        // verify field is accessible
        if (modifiers.contains(Modifier.PRIVATE)) {
            error("Field must not be private. (${field.qualifiedName})", field)
            return
        }

        if (field.hasAnnotation(Relation::class.java)) {
            // @Relation property
            val toOne = parseRelation(field)
            toOnes.add(toOne)
        } else if (isTypeEqual(field.asType(), ToOne::class.java.name, eraseTypeParameters = true)) {
            // ToOne<TARGET> property
            val toOne = parseToOne(field)
            toOnes.add(toOne)
        } else if ((!field.hasAnnotation(Convert::class.java)
                && isTypeEqual(field.asType(), List::class.java.name, eraseTypeParameters = true))
                || isTypeEqual(field.asType(), ToMany::class.java.name, eraseTypeParameters = true)) {
            // List<TARGET> or ToMany<TARGET> property
            val toMany = parseToMany(field)
            toManys.add(toMany)
        } else {
            // regular property
            parseProperty(entityModel, field)
        }
    }

    private fun parseToMany(field: VariableElement): ToManyRelation {
        val backlinkAnnotation = field.getAnnotation(Backlink::class.java)
        if (backlinkAnnotation == null) {
            error("ToMany field must be annotated with @Backlink. (${field.qualifiedName})", field)
        }

        // assuming List<TargetType> or ToMany<TargetType>
        val toManyTypeMirror = field.asType() as DeclaredType
        val targetTypeMirror = toManyTypeMirror.typeArguments[0] as DeclaredType
        // can simply get as element as code would not have compiled if target type is not known
        val targetEntityName = targetTypeMirror.asElement().simpleName

        return ToManyRelation(
                propertyName = field.simpleName.toString(),
                targetEntityName = targetEntityName.toString(),
                targetIdName = backlinkAnnotation.to.nullIfBlank()
        )
    }

    private fun parseRelation(field: VariableElement): ToOneRelation {
        val targetTypeMirror = field.asType() as DeclaredType
        val relationAnnotation = field.getAnnotation(Relation::class.java)
        val targetIdName = if (relationAnnotation.idProperty.isBlank()) null else relationAnnotation.idProperty
        return buildToOneRelation(field, targetTypeMirror, targetIdName, false)
    }

    private fun parseToOne(field: VariableElement): ToOneRelation {
        // assuming ToOne<TargetType>
        val toOneTypeMirror = field.asType() as DeclaredType
        val targetTypeMirror = toOneTypeMirror.typeArguments[0] as DeclaredType
        return buildToOneRelation(field, targetTypeMirror, null, true)
    }

    private fun buildToOneRelation(field: VariableElement, targetType: DeclaredType, targetIdName: String?,
            isExplicitToOne: Boolean): ToOneRelation {
        // can simply get as element as code would not have compiled if target type is not known
        val targetElement = targetType.asElement()
        val targetEntityName = targetElement.simpleName

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
                variableIsToOne = isExplicitToOne
        )
    }

    private fun parseProperty(entity: io.objectbox.generator.model.Entity, field: VariableElement) {

        // Compare with EntityClassASTVisitor.endVisit()
        // and GreendaoModelTranslator.convertProperty()

        val propertyBuilder: Property.PropertyBuilder?

        if (field.hasAnnotation(Convert::class.java)) {
            // verify @Convert custom type
            propertyBuilder = parseCustomProperty(entity, field)
        } else {
            // verify that supported type is used
            propertyBuilder = parseSupportedProperty(entity, field)
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
        if (field.hasAnnotation(Index::class.java)) {
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

    private fun parseCustomProperty(entity: io.objectbox.generator.model.Entity, field: VariableElement): Property.PropertyBuilder? {
        // extract @Convert annotation member values
        // as they are types, need to access them via annotation mirrors
        val annotationMirror = getAnnotationMirror(field, Convert::class.java) ?: return null // did not find @Convert mirror

        val converter = getAnnotationValueType(annotationMirror, "converter")
        if (converter == null) {
            error("@Convert requires a value for converter. (${field.qualifiedName})",
                    field)
            return null
        }

        val dbType = getAnnotationValueType(annotationMirror, "dbType")
        if (dbType == null) {
            error("@Convert requires a value for dbType. (${field.qualifiedName})",
                    field)
            return null
        }
        val propertyType = getPropertyType(dbType)
        if (propertyType == null) {
            error("@Convert dbType type is not supported. (${field.qualifiedName})",
                    field)
            return null
        }

        val propertyBuilder = entity.addProperty(propertyType, field.simpleName.toString())
        propertyBuilder.customType(field.asType().toString(), converter.toString())
        // note: custom types are already assumed non-primitive by Property#isNonPrimitiveType()
        return propertyBuilder
    }

    private fun parseSupportedProperty(entity: io.objectbox.generator.model.Entity, field: VariableElement): PropertyBuilder? {
        val typeMirror = field.asType()
        val propertyType = getPropertyType(typeMirror)
        if (propertyType == null) {
            error("Field type is not supported, maybe add @Convert. (${field.qualifiedName})", field)
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

    private fun resolveToMany(schema: Schema, entity: io.objectbox.generator.model.Entity, toMany: ToManyRelation): Boolean {
        val targetEntity = schema.entities.singleOrNull {
            it.className == toMany.targetEntityName
        }
        if (targetEntity == null) {
            error("ToMany target class '${toMany.targetEntityName}' " +
                    "defined in class '${entity.className}' could not be found (is it an @Entity?)")
            return false
        }

        val targetToOne = if (toMany.targetIdName.isNullOrEmpty()) {
            // no explicit target name: just ensure a single to-one relation, then use that
            val targetToOne = targetEntity.toOneRelations.filter {
                it.targetEntity == entity
            }
            if (targetToOne.isEmpty()) {
                error("A to-one relation must be added to '${targetEntity.className}' to create the to-many relation " +
                        "'${toMany.propertyName}' in '${entity.className}'.")
                return false
            } else if (targetToOne.size > 1) {
                error("Set name of one to-one relation of '${targetEntity.className}' as @Backlink 'to' value to " +
                        "create the to-many relation '${toMany.propertyName}' in '${entity.className}'.")
                return false
            }
            targetToOne[0]
        } else {
            // explicit target name: find the related to-one relation
            val targetToOne = targetEntity.toOneRelations.singleOrNull {
                it.targetEntity == entity && it.name == toMany.targetIdName
            }
            if (targetToOne == null) {
                error("Could not find property '${toMany.targetIdName}' in '${entity.className}'.")
                return false
            }
            targetToOne
        }

        entity.addToMany(targetEntity, targetToOne.targetIdProperty, toMany.propertyName)
        return true
    }

    private fun syncIdModel(schema: Schema): Boolean {
        val modelFilePath = if (customModelPath.isNullOrEmpty()) "objectbox-models/default.json" else customModelPath
        val modelFile = File(projectRoot, modelFilePath)

        val modelFolder = modelFile.parentFile
        if (!modelFolder.isDirectory && !modelFolder.mkdirs()) {
            error("Could not create model folder at '${modelFolder.absolutePath}'.")
            return false
        }

        val idSync: IdSync
        try {
            idSync = IdSync(modelFile)
            idSync.sync(schema)
        } catch (e: IdSyncException) {
            error(e.message ?: "Could not sync id model for unknown reason.")
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
            val convertType = elementUtils.getTypeElement(annotationClass.canonicalName).asType()
            if (typeUtils.isSameType(annotationType, convertType)) {
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

    /**
     * @param eraseTypeParameters Set to true to erase type parameters of a generic type, such as ToOne&lt;A&gt;
     *     before comparison.
     */
    private fun isTypeEqual(typeMirror: TypeMirror, otherType: String, eraseTypeParameters: Boolean = false): Boolean {
        if (eraseTypeParameters) {
            return otherType == typeUtils.erasure(typeMirror).toString()
        } else {
            return otherType == typeMirror.toString()
        }
    }

    private fun error(message: String, element: Element? = null) {
        printMessage(Diagnostic.Kind.ERROR, message, element)
    }

    private fun printMessage(kind: Diagnostic.Kind, message: String, element: Element? = null) {
        if (element != null) {
            processingEnv.messager.printMessage(kind, message, element)
        } else {
            processingEnv.messager.printMessage(kind, message)
        }
    }

    val VariableElement.qualifiedName: String
        get() {
            val enclosingElement = enclosingElement as TypeElement
            val fieldName = simpleName
            return "${enclosingElement.qualifiedName}.$fieldName"
        }

    fun <A : Annotation> Element.hasAnnotation(annotationType: Class<A>): Boolean {
        return getAnnotation(annotationType) != null
    }

}
