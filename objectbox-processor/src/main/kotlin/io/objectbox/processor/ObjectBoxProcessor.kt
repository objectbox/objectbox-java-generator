package io.objectbox.processor

import io.objectbox.annotation.Entity
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Uid
import io.objectbox.generator.BoxGenerator
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.idsync.IdSyncException
import io.objectbox.generator.model.Schema
import java.io.File
import java.io.FileNotFoundException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
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
    private var messager: Messager by Delegates.notNull()
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
        messager = env.messager

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
        val relations = Relations(messager)

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
            e.printStackTrace()
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

        // parse properties
        val properties = Properties(elementUtils, typeUtils, messager, relations, entityModel, entity)
        properties.parseFields()

        // if not added automatically and relations are used, ensure there is a box store field
        if (!transformationEnabled && relations.hasRelations(entityModel) && !properties.hasBoxStoreField()) {
            error("To use relations in '${entityModel.className}' add a field '__boxStore' of type 'BoxStore'.", entity)
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

    private fun error(message: String, element: Element? = null) {
        errorCount++
        messager.printCustomError(message, element)
    }

}
