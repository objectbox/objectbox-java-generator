/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.processor

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.annotation.ExternalName
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Sync
import io.objectbox.annotation.Uid
import io.objectbox.generator.BoxGenerator
import io.objectbox.generator.GeneratorJob
import io.objectbox.generator.GeneratorOutput
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.idsync.IdSyncException
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.Schema
import io.objectbox.reporting.BasicBuildTracker
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
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import io.objectbox.generator.model.Entity as ModelEntity

/**
 * ObjectBox annotation processor which parses [@Entity][Entity] and [@BaseEntity][BaseEntity]
 * classes to generate helper classes and a model file from. Also helps the user with various
 * errors to prevent misconfigurations or incorrect usage.
 *
 * See [ObjectBoxProcessorShim], which is the actual class registered as processor, for more docs.
 */
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
         * Set to false to turn off support for incremental processing.
         */
        const val OPTION_INCREMENTAL: String = "objectbox.incremental"

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
                        || first.substring(indexSub, indexDot1) != second.substring(indexSub, indexDot1)
                    ) {
                        return if (second.startsWith(first)) {
                            first // Check for full match separately at the end (last part has no trailing dot)
                        } else if (commonDotCount >= 2) {
                            first.substring(0, indexCommonDot)
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
    private lateinit var javaLangObjectType: TypeMirror
    private var customModelPath: String? = null
    private var customDefaultPackage: String? = null
    private var daoCompat: Boolean = false
    private var transformationEnabled: Boolean = false
    private var daoCompatPackage: String? = null
    private var flatbuffersSchemaPath: String? = null
    private var debug: Boolean = false
    private var allowNumberedConstructorArgs: Boolean = false
    private var incremental = true

    @Synchronized
    override fun init(env: ProcessingEnvironment) {
        super.init(env)

        elementUtils = env.elementUtils
        typeUtils = env.typeUtils
        filer = env.filer
        javaLangObjectType = elementUtils.getTypeElement(java.lang.Object::class.java.canonicalName).asType()

        val options = env.options
        customModelPath = options[OPTION_MODEL_PATH]
        customDefaultPackage = options[OPTION_MYOBJECTBOX_PACKAGE]
        daoCompat = "true" == options[OPTION_DAO_COMPAT]
        debug = "true" == options[OPTION_DEBUG]
        daoCompatPackage = options[OPTION_DAO_PACKAGE]
        flatbuffersSchemaPath = options[OPTION_FLATBUFFERS_SCHEMA_FOLDER]
        transformationEnabled = "false" != options[OPTION_TRANSFORMATION_ENABLED] // default true
        allowNumberedConstructorArgs = "false" != options[OPTION_ALLOW_NUMBERED_CONSTRUCTOR_ARGS] // default true
        incremental = "false" != options[OPTION_INCREMENTAL] // Default true (opt-out).

        messages = Messages(env.messager, debug)
        messages.debug(
            """Starting processor
            modelPath=$customModelPath
            myObjectBoxPackage=$customDefaultPackage
            daoCompat=$daoCompat
            daoPackage=$daoCompatPackage
            flatbuffersSchemaFolder=$flatbuffersSchemaPath
            transformationEnabled=$transformationEnabled
            allowNumberedConstructorArgs=$allowNumberedConstructorArgs
            incremental=$incremental"""
        )
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        val types = LinkedHashSet<String>()
        types.add(Entity::class.java.canonicalName)
        // Note: on an incremental run Gradle only gives class elements that have one
        // of the supported annotations to the processor.
        // So explicitly include the base entity annotation to get all base entity class elements.
        types.add(BaseEntity::class.java.canonicalName)
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
        options.add(OPTION_INCREMENTAL)
        // Dynamic incremental support (see ObjectBoxProcessorShim):
        // do not advertise processor as incremental if turned off.
        // See OPTION_INCREMENTAL for explanation.
        if (incremental) {
            options.add("org.gradle.annotation.processing.aggregating")
        }
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
        return false // Always return false to allow other processors to handle annotations this supports.
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
        val schema = Schema(Schema.DEFAULT_NAME, 1, defaultJavaPackage)

        // Parse entities.
        val annotatedElements = mutableSetOf<Element>().run {
            addAll(env.getElementsAnnotatedWith(Entity::class.java))
            addAll(env.getElementsAnnotatedWith(BaseEntity::class.java))
            toSet()
        }
        for (entity in entities) {
            parseEntity(annotatedElements, schema, relations, entity)
        }

        if (messages.errorRaised) {
            return // Avoid errors during resolving relations caused by previous errors.
        }

        if (!relations.resolve(schema)) {
            return // resolving relations failed
        }

        if (messages.errorRaised) {
            return // avoid changing files (model file, generated source)
        }

        if (!checkSyncEnabledEntities(schema.entities)) {
            return
        }

        try {
            schema.finish()
        } catch (e: Exception) {
            messages.error("Code generation failed: $e")
            e.printStackTrace()
            return
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

    private fun parseEntity(annotatedElements: Set<Element>, schema: Schema, relations: Relations, entity: Element) {
        val name = entity.simpleName.toString()
        if (debug) messages.debug("Parsing entity $name...")

        schema.entities.find { it.className == name }?.let {
            messages.error("There is already an entity class '$name': '${it.javaPackage}.${it.className}'.", entity)
            return
        }

        val entityModel = schema.addEntity(name)
        entityModel.javaPackage = elementUtils.getPackageOf(entity).qualifiedName.toString()
        entityModel.javaPackageDao = daoCompatPackage ?: entityModel.javaPackage

        // @NameInDb
        val nameInDbAnnotation = entity.getAnnotation(NameInDb::class.java)
        if (nameInDbAnnotation != null) {
            if (nameInDbAnnotation.value.isNotEmpty()) {
                entityModel.dbName = nameInDbAnnotation.value
            }
        }

        // @ExternalName
        entity.getAnnotation(ExternalName::class.java)?.value
            ?.also { entityModel.setExternalName(it) }

        // @Uid
        val uidAnnotation = entity.getAnnotation(Uid::class.java)
        if (uidAnnotation != null) {
            // Note: UID values 0 and -1 are special: print current value and fail later
            val uid = if (uidAnnotation.value == 0L) -1 else uidAnnotation.value
            entityModel.modelUid = uid
        }

        // @Sync
        entity.getAnnotation(Sync::class.java)?.run {
            entityModel.isSyncEnabled = true
            entityModel.isSyncSharedGlobalIds = this.sharedGlobalIds
        }

        // Parse properties.
        parseProperties(annotatedElements, relations, entityModel, entity)
        // Verify there is an @Id property.
        entityModel.ensureIdProperty()
        // Verify there is at most 1 unique property with REPLACE strategy.
        entityModel.ensureSingleUniqueReplace()

        // if not added automatically and relations are used, ensure there is a box store field
        if (!transformationEnabled && relations.hasRelations(entityModel) && !entityModel.hasBoxStoreField) {
            messages.error(
                "To use relations in '${entityModel.className}' " +
                        "add a field '__boxStore' of type 'BoxStore'.", entity
            )
        }

        // Add or verify target ID reference properties for to-one relations.
        relations.ensureToOneIdRefProperties(entityModel)

        // signal if a constructor will be available
        entityModel.setHasAllArgsConstructor(entity.hasAllPropertiesConstructor(entityModel))

        // Require an all-properties or no-arg constructor so the native (JNI) database code can create objects.
        // Note: visibility is not checked, access restrictions don't apply to native code.
        // Note: for unknown reasons, on Android 8.1 or higher native code can create objects
        // even without a no-arg constructor.
        // https://github.com/objectbox/objectbox-java/issues/900
        if (!entityModel.hasAllArgsConstructor() && !entity.hasNoArgConstructor()) {
            messages.error(
                "No-argument or all-properties constructor is required for @Entity class. https://docs.objectbox.io/getting-started#define-entity-classes",
                entity
            )
        }
    }

    /**
     * Starting from the given [type] walks up the inheritance chain and for each super type
     * that has a match in [annotatedElements] adds that element to the [entityInheritanceChain].
     */
    private fun findAnnotatedSuperElements(
        annotatedElements: Set<Element>,
        entityInheritanceChain: MutableList<Element>,
        type: TypeMirror
    ): Boolean {
        // Implementation note: it can NOT be assumed that for each of directSupertypes(type) there
        // will be an element in RoundEnvironment.rootElements. E.g when processing is incremental rootElements
        // only contains annotated elements (for this processor @BaseEntity and @Entity classes).

        // Note: Why can there be multiple super types? Because interfaces are considered super types.
        for (superType in typeUtils.directSupertypes(type)) {
            // Skip if the top-most type (java.lang.Object, also for interfaces) is reached.
            if (typeUtils.isSameType(superType, javaLangObjectType)) {
                continue
            }
            if (debug) messages.debug("$type has super type $superType.")

            // If there is an annotated element that matches this super type, find it.
            val matchingElement = annotatedElements.find { annotatedElement ->
                if (annotatedElement.kind != ElementKind.CLASS) {
                    // The @BaseEntity and @Entity annotation should be restricted to classes by the compiler,
                    // but to be sure ignore any annotated element that is not a class.
                    false
                } else {
                    // Note: if directSupertypes() returns parameterized types they are specific (e.g. BaseEntity<String>),
                    // in contrast parameterized types of elements of the round environment are generic (e.g. BaseEntity<T>).
                    // So before comparing, erase any parameter types (e.g. BaseEntity<T> -> BaseEntity).
                    typeUtils.isSameType(typeUtils.erasure(annotatedElement.asType()), typeUtils.erasure(superType))
                }
            }
            // If there is a match, add the element to the list.
            matchingElement?.let {
                entityInheritanceChain.add(it)
                if (debug) messages.debug("$superType is annotated, add to inheritance chain.")
            }

            // Continue with checking the super types of this super type.
            val hasMatches = findAnnotatedSuperElements(annotatedElements, entityInheritanceChain, superType)

            // Do not check sibling super types if this one or its parents are matches
            // (this is fine as Java has no multi-inheritance).
            if (matchingElement != null || hasMatches) return true
        }

        return false // No matches.
    }

    private fun parseProperties(
        annotatedElements: Set<Element>,
        relations: Relations,
        entityModel: ModelEntity,
        entityElement: Element
    ) {
        // The current entity...
        val entityInheritanceChain = mutableListOf(entityElement)
        // ...and its super classes that are annotated.
        if (findAnnotatedSuperElements(annotatedElements, entityInheritanceChain, entityElement.asType())) {
            if (debug) messages.debug(
                "Detected entity inheritance chain: " +
                        entityInheritanceChain.joinToString(separator = "->") { it.simpleName })
        }

        // Reverse inheritance chain to parse properties starting with the super-most element in the chain
        // to ensure constructor param order is as expected: from super class to sub class,
        // then from first declared to last declared.
        val entitiesSuperMostFirst = entityInheritanceChain.reversed()
        entitiesSuperMostFirst.forEach { element ->
            val isSuperEntity = entitiesSuperMostFirst.last() != element
            with(Properties(elementUtils, typeUtils, messages, relations, entityModel, element, isSuperEntity)) {
                parseFields()
                entityModel.hasBoxStoreField =
                    entityModel.hasBoxStoreField || hasBoxStoreField() // Do not overwrite true.
            }
        }
    }

    private fun ModelEntity.ensureIdProperty() {
        // Note: do not use pkProperty as it is only initialized during schema finalization (2nd pass).
        val idPropertyCount = properties.count { it.isPrimaryKey }
        if (idPropertyCount == 0) {
            messages.error(
                "No @Id property found for '${className}', add @Id on a not-null long property.",
                this
            )
        } else if (idPropertyCount > 1) {
            messages.error(
                "Only one @Id property is allowed for '${className}'.",
                this
            )
        }
    }

    private fun ModelEntity.ensureSingleUniqueReplace() {
        val uniqueReplaceIndexes = indexes.filter { it.isUniqueOnConflictReplace }
        if (uniqueReplaceIndexes.size > 1) {
            messages.error(
                "ConflictStrategy.REPLACE can only be used on a single property, but found multiple in '${className}':\n${
                    uniqueReplaceIndexes.joinToString(separator = "\n") { "  ${it.properties[0].propertyName}" }
                }",
                this
            )
        }
    }

    /**
     * Returns true if the entity has a constructor where param types, names and order matches the properties of the
     * given entity model.
     */
    private fun Element.hasAllPropertiesConstructor(entityModel: ModelEntity): Boolean {
        if (debug) messages.debug("Checking for all-args constructor for ${entityModel.className}...")
        val constructors = ElementFilter.constructorsIn(enclosedElements)
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

    private fun parametersMatchProperties(
        parameters: MutableList<out VariableElement>,
        properties: MutableList<Property>
    ): Boolean {
        val typeHelper = TypeHelper(elementUtils, typeUtils)
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
                        if (debug) messages.debug(
                            "Constructor param name alternative accepted: " +
                                    "$altName for ${parsedElement.simpleName}"
                        )
                    } else {
                        if (debug) messages.debug(
                            "Constructor param name differs: " +
                                    "${param.simpleName} vs. ${parsedElement.simpleName} ($altName)"
                        )
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

    /**
     * Returns true if the [Element] has a constructor requiring no parameters.
     */
    private fun Element.hasNoArgConstructor(): Boolean {
        ElementFilter.constructorsIn(enclosedElements)
            .forEach { if (it.parameters.size == 0) return true }
        return false
    }

    private fun syncIdModel(schema: Schema): Boolean {
        val customModelPath = this.customModelPath
        val useDefaultPath = customModelPath.isNullOrEmpty()
        val modelFile = if (useDefaultPath) {
            try {
                val projectRoot = findProjectRoot(filer)
                File(projectRoot, "objectbox-models/default.json")
            } catch (e: FileNotFoundException) {
                messages.error(
                    "Could not find project root to create model file in. " +
                            "Add absolute path to model file with processor option '$OPTION_MODEL_PATH'. (${e.message})"
                )
                return false
            }
        } else {
            File(customModelPath!!)
        }

        val modelFolder = modelFile.parentFile
        if (!modelFolder.isDirectory) {
            if (useDefaultPath) {
                if (!modelFolder.mkdirs()) {
                    messages.error(
                        "Could not create default model folder at '${modelFolder.absolutePath}'. " +
                                "Add absolute path to model file with processor option '$OPTION_MODEL_PATH'."
                    )
                    return false
                }
            } else {
                messages.error(
                    "The model folder does not exist at '${modelFolder.absolutePath}'" +
                            " (based on the option $OPTION_MODEL_PATH='$customModelPath')."
                )
                return false
            }
        }

        try {
            IdSync(modelFile).sync(schema)
        } catch (e: IdSyncException) {
            messages.error(e.message ?: "Could not sync id model for unknown reason.")
            return false
        }

        return true
    }

    /**
     * Checks sync enabled entities to not contain relations to not synced entities
     * and to only contain unique indexes with REPLACE conflict strategy. Returns
     * false if any check has failed. Adds error messages for each failed check.
     */
    private fun checkSyncEnabledEntities(entities: List<ModelEntity>): Boolean {
        var hasNoFailures = true
        entities
            .filter { it.isSyncEnabled }
            .forEach { syncedEntity ->
                // Check there are no relations to not synced entities.
                syncedEntity.toOneRelations
                    .forEach {
                        if (!it.targetEntity!!.checkIsSynced(syncedEntity, it.name)) {
                            hasNoFailures = false
                        }
                    }
                syncedEntity.toManyRelations
                    .forEach {
                        if (!it.targetEntity!!.checkIsSynced(syncedEntity, it.name)) {
                            hasNoFailures = false
                        }
                    }
                // Check that all unique indexes use the REPLACE conflict strategy.
                val uniqueNotReplaceIndexes = syncedEntity.indexes.filter {
                    it.isUnique && !it.isUniqueOnConflictReplace
                }
                if (uniqueNotReplaceIndexes.isNotEmpty()) {
                    hasNoFailures = false
                    messages.error(
                        "Synced entities must use @Unique(onConflict = ConflictStrategy.REPLACE) for all unique properties, but found others in '${syncedEntity.className}':\n${
                            uniqueNotReplaceIndexes.joinToString(separator = "\n") { "  ${it.properties[0].propertyName}" }
                        }",
                        syncedEntity
                    )
                }
            }
        return hasNoFailures
    }

    private fun ModelEntity.checkIsSynced(syncedEntity: ModelEntity, relationName: String): Boolean {
        return if (isSyncEnabled) {
            true
        } else {
            messages.error(
                "Synced entity '${syncedEntity.className}' can't have a relation to not-synced entity '$className', but found relation '$relationName'.",
                syncedEntity
            )
            false
        }
    }

}
