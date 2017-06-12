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
        /** Set by ObjectBox plugin */
        val OPTION_TRANSFORMATION_ENABLED: String = "objectbox.transformationEnabled"
    }

    // make processed schema accessible for testing
    var schema: Schema? = null

    private var elementUtils: Elements by Delegates.notNull()
    private var typeUtils: Types by Delegates.notNull()
    private var filer: Filer by Delegates.notNull()
    private var projectRoot: File? = null
    private var customModelPath: String? = null
    private var daoCompat: Boolean = false
    private var transformationEnabled: Boolean = false
    private var daoCompatPackage: String? = null
    var errorCount = 0

    @Synchronized override fun init(env: ProcessingEnvironment) {
        super.init(env)

        elementUtils = env.elementUtils
        typeUtils = env.typeUtils
        filer = env.filer

        customModelPath = env.options[OPTION_MODEL_PATH]
        daoCompat = "true" == env.options[OPTION_DAO_COMPAT]
        daoCompatPackage = env.options[OPTION_DAO_PACKAGE]
        transformationEnabled = "true" == env.options[OPTION_TRANSFORMATION_ENABLED]

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
        options.add(OPTION_DAO_COMPAT)
        options.add(OPTION_DAO_PACKAGE)
        options.add(OPTION_TRANSFORMATION_ENABLED)
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
        val relations = Relations(processingEnv.messager)

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

            parseEntity(schema, relations, entity)
        }
        if (errorCount > 0) {
            return
        }

        if (schema == null) {
            return  // no entities found
        }

        if (!relations.resolve(schema)) {
            return // resolving relations failed
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

    private fun parseEntity(schema: Schema, relations: Relations, entity: Element) {
        val entityModel = schema.addEntity(entity.simpleName.toString())
        entityModel.isSkipGeneration = true // processor may not generate duplicate entity source files
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
        val fields = ElementFilter.fieldsIn(entity.enclosedElements)
        var hasBoxStore = false
        for (field in fields) {
            parseField(entityModel, relations, field)
            hasBoxStore = hasBoxStore || (field.simpleName.toString() == "__boxStore")
        }
        if (!transformationEnabled && !hasBoxStore && relations.hasRelations(entityModel)) {
            error("Entity ${entityModel.className} has relations and thus must have a field \"__boxStore\" of type" +
                    "BoxStore; please add it manually", entity)
            return
        }

        // add missing foreign key properties and indexes for to-one relations
        relations.ensureForeignKeys(entityModel)

        // Call only after all properties have been parsed
        entityModel.isConstructors = hasAllArgsConstructor(entity, entityModel)
    }

    private fun hasAllArgsConstructor(entity: Element, entityModel: io.objectbox.generator.model.Entity): Boolean {
        // check constructors for an valid all-args constructor
        val constructors = ElementFilter.constructorsIn(entity.enclosedElements)
        val properties = entityModel.properties
        for (constructor in constructors) {
            if (properties.size == constructor.parameters.size) {
                var diff = false
                for ((idx, param) in constructor.parameters.withIndex()) {
                    val property = properties[idx].parsedElement as VariableElement
                    if (param.asType() != property.asType() || param.simpleName != property.simpleName) {
                        diff = true
                        break
                    }
                }
                if (!diff) {
                    return true
                }
            }
        }
        return false
    }

    private fun parseField(entityModel: io.objectbox.generator.model.Entity, relations: Relations,
                           field: VariableElement) {
        // ignore static, transient or @Transient fields
        val modifiers = field.modifiers
        if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.TRANSIENT)
                || field.hasAnnotation(Transient::class.java)) {
            return
        }

        if (field.hasAnnotation(Relation::class.java)) {
            // @Relation property
            val toOne = parseRelation(field)
            relations.collectToOne(entityModel, toOne)
        } else if (isTypeEqual(field.asType(), ToOne::class.java.name, eraseTypeParameters = true)) {
            // ToOne<TARGET> property
            val toOne = parseToOne(field)
            relations.collectToOne(entityModel, toOne)
        } else if (!field.hasAnnotation(Convert::class.java)
                && isTypeEqual(field.asType(), List::class.java.name, eraseTypeParameters = true)) {
            if (!field.hasAnnotation(Backlink::class.java)) {
                error("Is this a custom type or to-many relation? Add @Convert or @Backlink. (${field.qualifiedName})",
                        field)
                return
            }
            // List<TARGET> property
            val toMany = parseToMany(field)
            relations.collectToMany(entityModel, toMany)
        } else if (isTypeEqual(field.asType(), ToMany::class.java.name, eraseTypeParameters = true)) {
            if (!field.hasAnnotation(Backlink::class.java)) {
                error("ToMany field must be annotated with @Backlink. (${field.qualifiedName})", field)
                return
            }
            // ToMany<TARGET> property
            val toMany = parseToMany(field)
            relations.collectToMany(entityModel, toMany)
        } else {
            // regular property
            parseProperty(entityModel, field)
        }
    }

    private fun parseToMany(field: VariableElement): ToManyRelation {
        // assuming List<TargetType> or ToMany<TargetType>
        val toManyTypeMirror = field.asType() as DeclaredType
        val targetTypeMirror = toManyTypeMirror.typeArguments[0] as DeclaredType
        // can simply get as element as code would not have compiled if target type is not known
        val targetEntityName = targetTypeMirror.asElement().simpleName

        val backlinkAnnotation = field.getAnnotation(Backlink::class.java)

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
        return ToOneRelation(
                propertyName = field.simpleName.toString(),
                // can simply get as element as code would not have compiled if target type is not known
                targetEntityName = targetType.asElement().simpleName.toString(),
                targetIdName = targetIdName,
                targetIdDbName = field.getAnnotation(NameInDb::class.java)?.value?.nullIfBlank(),
                targetIdUid = field.getAnnotation(Uid::class.java)?.value?.let { if (it == 0L) null else it },
                variableIsToOne = isExplicitToOne,
                variableFieldAccessible = !field.modifiers.contains(Modifier.PRIVATE)
        )
    }

    private fun parseProperty(entity: io.objectbox.generator.model.Entity, field: VariableElement): Property? {

        // Compare with EntityClassASTVisitor.endVisit()
        // and GreendaoModelTranslator.convertProperty()

        // Why nullable? A property might not be parsed due to an error. We do not throw here.
        val propertyBuilder: Property.PropertyBuilder?

        if (field.hasAnnotation(Convert::class.java)) {
            // verify @Convert custom type
            propertyBuilder = parseCustomProperty(entity, field)
        } else {
            // verify that supported type is used
            propertyBuilder = parseSupportedProperty(entity, field)
        }
        if (propertyBuilder == null) {
            return null
        }
        propertyBuilder.property.parsedElement = field

        // checks if field is accessible
        if (!field.modifiers.contains(Modifier.PRIVATE)) {
            propertyBuilder.fieldAccessible()
        }

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

        return propertyBuilder.property
    }

    private fun parseCustomProperty(entity: io.objectbox.generator.model.Entity, field: VariableElement): Property.PropertyBuilder? {
        // extract @Convert annotation member values
        // as they are types, need to access them via annotation mirrors
        val annotationMirror = getAnnotationMirror(field, Convert::class.java) ?: return null // did not find @Convert mirror

        // converter and dbType value existence guaranteed by compiler
        val converter = getAnnotationValueType(annotationMirror, "converter")
        val dbType = getAnnotationValueType(annotationMirror, "dbType")

        val propertyType = getPropertyType(dbType)
        if (propertyType == null) {
            error("@Convert dbType type is not supported, use a Java primitive wrapper class. (${field.qualifiedName})",
                    field)
            return null
        }

        // may be a parameterized type like List<CustomType>, so erase any type parameters
        val customType = typeUtils.erasure(field.asType())

        val propertyBuilder = entity.addProperty(propertyType, field.simpleName.toString())
        propertyBuilder.customType(customType.toString(), converter.toString())
        // note: custom types are already assumed non-primitive by Property#isNonPrimitiveType()
        return propertyBuilder
    }

    private fun parseSupportedProperty(entity: io.objectbox.generator.model.Entity, field: VariableElement): PropertyBuilder? {
        val typeMirror = field.asType()
        val propertyType = getPropertyType(typeMirror)
        if (propertyType == null) {
            error("Field type is not supported, use @Convert or @Transient. (${field.qualifiedName})", field)
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

    private fun getPropertyType(typeMirror: TypeMirror?): PropertyType? {
        if (typeMirror == null) {
            return null
        }

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
        errorCount++
        printMessage(Diagnostic.Kind.ERROR, message, element)
    }

    private fun printMessage(kind: Diagnostic.Kind, message: String, element: Element? = null) {
        if (element != null) {
            processingEnv.messager.printMessage(kind, message, element)
        } else {
            processingEnv.messager.printMessage(kind, "ObjectBox: " + message)
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
