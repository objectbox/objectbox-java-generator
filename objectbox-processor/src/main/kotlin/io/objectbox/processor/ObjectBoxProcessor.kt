package io.objectbox.processor

import io.objectbox.annotation.Entity
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Uid
import io.objectbox.generator.BoxGenerator
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.idsync.IdSyncException
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.Schema
import java.io.File
import java.io.FileNotFoundException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

open class ObjectBoxProcessor : AbstractProcessor() {

    companion object {
        val OPTION_MODEL_PATH: String = "objectbox.modelPath"
        val OPTION_DAO_COMPAT: String = "objectbox.daoCompat"
        val OPTION_DAO_PACKAGE: String = "objectbox.daoPackage"
        val OPTION_DEBUG: String = "objectbox.debug"
        /** Set by ObjectBox plugin */
        val OPTION_TRANSFORMATION_ENABLED: String = "objectbox.transformationEnabled"
        val OPTION_ALLOW_NUMBERED_CONSTRUCTOR_ARGS: String = "objectbox.allowNumberedConstructorArgs"
    }

    // make processed schema accessible for testing
    var schema: Schema? = null

    private lateinit var elementUtils: Elements
    private lateinit var typeUtils: Types
    private lateinit var filer: Filer
    private lateinit var messages: Messages
    private var customModelPath: String? = null
    private var daoCompat: Boolean = false
    private var transformationEnabled: Boolean = false
    private var daoCompatPackage: String? = null
    private var debug: Boolean = false
    private var allowNumberedConstructorArgs: Boolean = false

    @Synchronized override fun init(env: ProcessingEnvironment) {
        super.init(env)

        elementUtils = env.elementUtils
        typeUtils = env.typeUtils
        filer = env.filer

        val options = env.options
        customModelPath = options[OPTION_MODEL_PATH]
        daoCompat = "true" == options[OPTION_DAO_COMPAT]
        debug = "true" == options[OPTION_DEBUG]
        daoCompatPackage = options[OPTION_DAO_PACKAGE]
        transformationEnabled = "false" != options[OPTION_TRANSFORMATION_ENABLED] // default true
        allowNumberedConstructorArgs = "false" != options[OPTION_ALLOW_NUMBERED_CONSTRUCTOR_ARGS] // default true

        messages = Messages(env.messager, debug)
        messages.info("Starting ObjectBox processor (debug: $debug)")
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
        options.add(OPTION_DEBUG)
        options.add(OPTION_ALLOW_NUMBERED_CONSTRUCTOR_ARGS)
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
        val relations = Relations(messages)

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

        if (schema == null) {
            return  // no entities found
        }

        if (!relations.resolve(schema)) {
            return // resolving relations failed
        }

        if (messages.errorRaised) {
            return // avoid changing files (model file, generated source)
        }

        if (!syncIdModel(schema)) {
            return // id model sync failed
        }

        this.schema = schema // make processed schema accessible for testing

        try {
            BoxGenerator(daoCompat).generateAll(schema, filer)
        } catch (e: Exception) {
            messages.error("Code generation failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun parseEntity(schema: Schema, relations: Relations, entity: Element) {
        val name = entity.simpleName.toString()
        if (debug) messages.debug("Parsing entity $name...")
        val entityModel = schema.addEntity(name)
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

        // parse properties
        val properties = Properties(elementUtils, typeUtils, messages, relations, entityModel, entity)
        properties.parseFields()

        val hasBoxStoreField = properties.hasBoxStoreField()
        entityModel.hasBoxStoreField = hasBoxStoreField

        // if not added automatically and relations are used, ensure there is a box store field
        if (!transformationEnabled && relations.hasRelations(entityModel) && !hasBoxStoreField) {
            messages.error("To use relations in '${entityModel.className}' " +
                    "add a field '__boxStore' of type 'BoxStore'.", entity)
        }

        // add missing foreign key properties and indexes for to-one relations
        relations.ensureForeignKeys(entityModel)

        // signal if a constructor will be available
        entityModel.isConstructors = hasAllArgsConstructor(entity, entityModel)
    }

    /**
     * Returns true if the entity has a constructor where param types, names and order matches the properties of the
     * given entity model.
     */
    private fun hasAllArgsConstructor(entity: Element, entityModel: io.objectbox.generator.model.Entity): Boolean {
        if (debug) messages.debug("Checking for all-args constructor for ${entityModel.className}...")
        val constructors = ElementFilter.constructorsIn(entity.enclosedElements)
        val properties = entityModel.properties
        for (constructor in constructors) {
            val parameters = constructor.parameters
            if (debug) messages.debug("Checking constructor $constructor...")
            if (parameters.size == properties.size && parametersMatchProperties(parameters, properties)) {
                if (debug) messages.debug("Valid all-args constructor found")
                return true
            }
        }
        if (debug) messages.debug("No all-args constructor found for ${entityModel.className}")
        return false
    }

    private fun parametersMatchProperties(parameters: MutableList<out VariableElement>,
                                          properties: MutableList<Property>): Boolean {
        val typeHelper = TypeHelper(typeUtils)
        for ((idx, param) in parameters.withIndex()) {
            val property = properties[idx]
            val altName = if (allowNumberedConstructorArgs) "arg$idx" else null
            if (property.parsedElement != null) {
                val parsedElement = property.parsedElement as VariableElement
                if (param.simpleName != parsedElement.simpleName) {
                    if (altName == param.simpleName.toString()) {
                        if (debug) messages.debug("Constructor param name alternative accepted: " +
                                "$altName for ${parsedElement.simpleName}")
                    } else {
                        if (debug) messages.debug("Constructor param names differ: " +
                                "${param.simpleName} vs. ${parsedElement.simpleName} ($altName)")
                        return false
                    }
                }
                if (property.customType != null) {
                    val converterType = elementUtils.getTypeElement(property.customType).asType()
                    // Note: equality check does not work for TypeMirror
                    if (!typeUtils.isSameType(converterType, param.asType())) {
                        messages.debug("Constructor param types differs (custom type): " +
                                "${param.asType()} vs. $converterType")
                        return false
                    }
                } else if (param.asType() != parsedElement.asType()) {
                    messages.debug("Constructor param types differs: ${param.asType()} vs. ${parsedElement.asType()}")
                    return false
                }
            } else {
                // special case: virtual property (to-one target id) that has no matching field
                val paramPropertyType = typeHelper.getPropertyType(param.asType())
                if (paramPropertyType != property.propertyType) {
                    messages.debug("Constructor param types differs (virtual property)")
                    return false
                }
                if (param.simpleName.toString() != property.propertyName) {
                    if (altName == property.propertyName) {
                        if (debug) messages.debug("Constructor param name alternative accepted: $altName for ${property.propertyName}")
                    }
                    messages.debug("Constructor param names differs (virtual property)")
                    return false
                }
            }
        }
        return true
    }

    private fun syncIdModel(schema: Schema): Boolean {
        val useDefaultPath = customModelPath.isNullOrEmpty()
        val modelFile = if (useDefaultPath) {
            try {
                val projectRoot = findProjectRoot(filer)
                File(projectRoot, "objectbox-models/default.json")
            } catch (e: FileNotFoundException) {
                messages.error("Could not find project root to create model file in. " +
                        "Add absolute path to model file with processor option '$OPTION_MODEL_PATH'. (${e.message})")
                return false
            }
        } else {
            File(customModelPath)
        }

        val modelFolder = modelFile.parentFile
        if (!modelFolder.isDirectory) {
            if (useDefaultPath && !modelFolder.mkdirs()) {
                messages.error("Could not create default model folder at '${modelFolder.absolutePath}'. " +
                        "Add absolute path to model file with processor option '$OPTION_MODEL_PATH'.")
                return false
            } else {
                messages.error("Model folder does not exist at '${modelFolder.absolutePath}'.")
                return false
            }
        }

        val idSync: IdSync
        try {
            idSync = IdSync(modelFile)
            idSync.sync(schema)
        } catch (e: IdSyncException) {
            messages.error(e.message ?: "Could not sync id model for unknown reason.")
            return false
        }

        schema.lastEntityId = idSync.lastEntityId
        schema.lastIndexId = idSync.lastIndexId

        return true
    }

}
