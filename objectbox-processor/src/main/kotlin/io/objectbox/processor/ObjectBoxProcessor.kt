/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.processor

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Uid
import io.objectbox.build.BasicBuildTracker
import io.objectbox.generator.BoxGenerator
import io.objectbox.generator.GeneratorJob
import io.objectbox.generator.GeneratorOutput
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
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

open class ObjectBoxProcessor : AbstractProcessor() {

    companion object {
        const val OPTION_MODEL_PATH: String = "objectbox.modelPath"
        const val OPTION_MYOBJECTBOX_PACKAGE: String = "objectbox.myObjectBoxPackage"
        const val OPTION_DAO_COMPAT: String = "objectbox.daoCompat"
        const val OPTION_DAO_PACKAGE: String = "objectbox.daoPackage"
        const val OPTION_FLATBUFFERS_SCHEMA_FOLDER: String = "objectbox.flatbuffersSchemaFolder"
        const val OPTION_DEBUG: String = "objectbox.debug"
        /** Set by ObjectBox plugin */
        const val OPTION_TRANSFORMATION_ENABLED: String = "objectbox.transformationEnabled"
        const val OPTION_ALLOW_NUMBERED_CONSTRUCTOR_ARGS: String = "objectbox.allowNumberedConstructorArgs"

        /**
         * Typically selects the top most and lexicographically first package. If entities are in different packages and
         * at least 3 packages deep, selects the first common parent package instead.
         */
        internal fun selectPackage(packages: List<String>): String? {
            // future improvement: to make this smarter we could look for the most commonly used package prefix
            val packagesSorted = packages.toSortedSet()
            if (packagesSorted.size >= 2) {
                val first = packagesSorted.first()
                val second = packagesSorted.iterator().let { it.next(); it.next() }
                var indexCommonDot = -1
                for (commonDotCount in 0..Int.MAX_VALUE) {
                    val indexSub = indexCommonDot + 1
                    val indexDot1 = first.indexOf('.', indexSub)
                    val indexDot2 = second.indexOf('.', indexSub)
                    if (indexDot1 == -1 || indexDot2 != indexDot1
                            || first.substring(indexSub, indexDot1) != second.substring(indexSub, indexDot1)) {
                        if (second.startsWith(first)) {
                            return first // Check for full match separately at the end (last part has no trailing dot)
                        } else if (commonDotCount >= 2) {
                            return first.substring(0, indexCommonDot)
                        } else {
                            break
                        }
                    }
                    indexCommonDot = indexDot1
                }
            }
            return packagesSorted.sorted()[0]
        }
    }

    // make processed schema accessible for testing
    var schema: Schema? = null

    private lateinit var elementUtils: Elements
    private lateinit var typeUtils: Types
    private lateinit var filer: Filer
    private lateinit var messages: Messages
    private var customModelPath: String? = null
    private var customDefaultPackage: String? = null
    private var daoCompat: Boolean = false
    private var transformationEnabled: Boolean = false
    private var daoCompatPackage: String? = null
    private var flatbuffersSchemaPath: String? = null
    private var debug: Boolean = false
    private var allowNumberedConstructorArgs: Boolean = false

    @Synchronized override fun init(env: ProcessingEnvironment) {
        super.init(env)

        elementUtils = env.elementUtils
        typeUtils = env.typeUtils
        filer = env.filer

        val options = env.options
        customModelPath = options[OPTION_MODEL_PATH]
        customDefaultPackage = options[OPTION_MYOBJECTBOX_PACKAGE]
        daoCompat = "true" == options[OPTION_DAO_COMPAT]
        debug = "true" == options[OPTION_DEBUG]
        daoCompatPackage = options[OPTION_DAO_PACKAGE]
        flatbuffersSchemaPath = options[OPTION_FLATBUFFERS_SCHEMA_FOLDER]
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
        options.add(OPTION_MYOBJECTBOX_PACKAGE)
        options.add(OPTION_DAO_COMPAT)
        options.add(OPTION_DAO_PACKAGE)
        options.add(OPTION_FLATBUFFERS_SCHEMA_FOLDER)
        options.add(OPTION_TRANSFORMATION_ENABLED)
        options.add(OPTION_DEBUG)
        options.add(OPTION_ALLOW_NUMBERED_CONSTRUCTOR_ARGS)
        return options
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, env: RoundEnvironment): Boolean {
        try {
            findAndParse(env)
        } catch (e: Throwable) {
            BasicBuildTracker("Processor").trackFatal("Processing failed", e)
            throw e
        }
        return false
    }

    private fun findAndParse(env: RoundEnvironment) {
        val relations = Relations(messages)
        if (messages.errorRaised) {
            return
        }

        val entities = env.getElementsAnnotatedWith(Entity::class.java)
        if (entities.size == 0) {
            return  // no entities found
        }

        val defaultJavaPackage = if (daoCompat && daoCompatPackage != null) {
            daoCompatPackage
        } else if (customDefaultPackage != null) {
            customDefaultPackage
        } else {
            // entities may be in multiple packages, so generate MyObjectBox in one that is least likely to change
            val packages = entities.map { elementUtils.getPackageOf(it).qualifiedName.toString() }
            selectPackage(packages)
        }
        val schema = Schema("default", 1, defaultJavaPackage)

        for (entity in entities) {
            parseEntity(env.rootElements, schema, relations, entity)
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

        var completed = false
        try {
            val job = GeneratorJob(schema, GeneratorOutput.create(filer))
            job.isDaoCompat = daoCompat
            flatbuffersSchemaPath?.let {
                job.outputFlatbuffersSchema = GeneratorOutput.create(it)
            }
            BoxGenerator().generateAll(job)
            completed = true
        } catch (e: Exception) {
            messages.error("Code generation failed: $e")
            e.printStackTrace()
        }
        trackStats(schema, completed)
    }

    private fun trackStats(schema: Schema, completed: Boolean) {
        var toOneCount = 0
        var toManyCount = 0
        var propertyCount = 0
        for (entity in schema.entities) {
            toManyCount += entity.toManyRelations?.size ?: 0
            toOneCount += entity.toOneRelations?.size ?: 0
            propertyCount += entity.properties?.size ?: 0
        }
        BasicBuildTracker("Processor").trackStats(
                daoCompat = daoCompat,
                completed = completed,
                entityCount = schema.entities.size,
                propertyCount = propertyCount,
                toManyCount = toManyCount,
                toOneCount = toOneCount
        )
    }

    private fun parseEntity(rootElements: Set<Element>, schema: Schema, relations: Relations, entity: Element) {
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
        if (uidAnnotation != null) {
            // Note: UID values 0 and -1 are special: print current value and fail later
            val uid = if (uidAnnotation.value == 0L) -1 else uidAnnotation.value
            entityModel.modelUid = uid
        }

        // properties
        parseProperties(rootElements, relations, entityModel, entity)

        // if not added automatically and relations are used, ensure there is a box store field
        if (!transformationEnabled && relations.hasRelations(entityModel) && !entityModel.hasBoxStoreField) {
            messages.error("To use relations in '${entityModel.className}' " +
                    "add a field '__boxStore' of type 'BoxStore'.", entity)
        }

        // add missing target ID properties and indexes for to-one relations
        relations.ensureTargetIdProperties(entityModel)

        // signal if a constructor will be available
        entityModel.isConstructors = hasAllArgsConstructor(entity, entityModel)
    }

    private fun parseProperties(rootElements: Set<Element>, relations: Relations, entityModel: io.objectbox.generator.model.Entity, entity: Element) {
        // get properties starting with root supertype to ensure constructor param order is as expected
        // (from super class to subclass, then from first declared to last declared)

        // walk up inheritance chain
        val classSupertypes = typeUtils.directSupertypes(entity.asType()).mapNotNull { supertype ->
            rootElements.find { typeUtils.isSameType(it.asType(), supertype) }
        }
        if (classSupertypes.isNotEmpty()) {
            // if any, classes are listed before interfaces: so just check first one
            val element = classSupertypes[0]
            if (element.kind == ElementKind.CLASS) {
                if (debug) messages.debug("Parsing super type of ${entity.simpleName}: ${element.simpleName}")
                parseProperties(rootElements, relations, entityModel, element)
            }
        }

        // only include properties for classes with @Entity or @BaseEntity
        if (entity.getAnnotation(Entity::class.java) != null || entity.getAnnotation(BaseEntity::class.java) != null) {
            // parse properties
            val properties = Properties(elementUtils, typeUtils, messages, relations, entityModel, entity)
            properties.parseFields()

            val hasBoxStoreField = properties.hasBoxStoreField()
            entityModel.hasBoxStoreField = entityModel.hasBoxStoreField || hasBoxStoreField // keep true value
        }
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
                // regular and custom type properties (have record of parsed field element)
                val parsedElement = property.parsedElement as VariableElement
                if (param.simpleName != parsedElement.simpleName) {
                    // note: Kotlin generates Java 6 bytecode and may not generate parameter names for any methods.
                    // Java 6 bytecode parameters are named arg0 to argn.
                    if (altName == param.simpleName.toString()) {
                        if (debug) messages.debug("Constructor param name alternative accepted: " +
                                "$altName for ${parsedElement.simpleName}")
                    } else {
                        if (debug) messages.debug("Constructor param name differs: " +
                                "${param.simpleName} vs. ${parsedElement.simpleName} ($altName)")
                        return false
                    }
                }
                // Note: equality check does not work for TypeMirror, use Types utils
                if (!typeUtils.isSameType(param.asType(), parsedElement.asType())) {
                    messages.debug("Constructor param type differs: ${param.asType()} vs. ${parsedElement.asType()}")
                    return false
                }
            } else {
                // special case: virtual property (to-one target id) that has no matching field
                val paramPropertyType = typeHelper.getPropertyType(param.asType())
                if (paramPropertyType != property.propertyType) {
                    messages.debug("Constructor param type differs (virtual property)")
                    return false
                }
                if (param.simpleName.toString() != property.propertyName) {
                    if (altName == property.propertyName) {
                        if (debug) messages.debug("Constructor param name alternative accepted: $altName for ${property.propertyName}")
                    }
                    messages.debug("Constructor param name differs (virtual property)")
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
            if (useDefaultPath) {
                if (!modelFolder.mkdirs()) {
                    messages.error("Could not create default model folder at '${modelFolder.absolutePath}'. " +
                            "Add absolute path to model file with processor option '$OPTION_MODEL_PATH'.")
                    return false
                }
            } else {
                messages.error("The model folder does not exist at '${modelFolder.absolutePath}'" +
                        " (based on the option $OPTION_MODEL_PATH='$customModelPath').")
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
