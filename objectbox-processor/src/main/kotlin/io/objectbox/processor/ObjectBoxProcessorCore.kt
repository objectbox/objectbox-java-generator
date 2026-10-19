/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2026 ObjectBox Ltd.
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

import io.objectbox.annotation.ExternalName
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Sync
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
import io.objectbox.generator.model.Entity as ModelEntity

/**
 * The processor implementation shared by the annotation processor and the KSP processor: parses
 * entity classes ([ProcessingClass] of the API the implementation is for) into a [Schema],
 * verifies it, syncs the ID model file and generates code.
 */
abstract class ObjectBoxProcessorCore(
    protected val messages: CommonMessages,
    protected val options: Options
) {

    /**
     * The processor options both processor implementations support (except [daoCompat] and
     * [daoCompatPackage], which only the annotation processor does).
     */
    class Options(
        val customModelPath: String?,
        val customDefaultPackage: String?,
        val flatbuffersSchemaPath: String?,
        val debug: Boolean,
        val transformationEnabled: Boolean,
        val allowNumberedConstructorArgs: Boolean,
        val daoCompat: Boolean = false,
        val daoCompatPackage: String? = null
    )

    // make processed schema accessible for testing
    var schema: Schema? = null

    private val debug get() = options.debug

    /**
     * Creates the output generated sources are written to.
     */
    protected abstract fun createGeneratorOutput(annotatedElements: List<ProcessingClass>): GeneratorOutput

    /**
     * Determines the directory of the project processing is running for, used to place the
     * default model file in. Implementations use tricks specific to their processor API.
     */
    @Throws(FileNotFoundException::class)
    protected abstract fun findProjectRoot(entities: List<ProcessingClass>): File

    /**
     * Parses the given entity classes (and their super classes in [annotatedElements]) into a
     * [Schema], resolves and verifies it, syncs the ID model file and generates code. Errors are
     * raised through [messages].
     */
    fun findAndParse(entities: List<ProcessingClass>, annotatedElements: List<ProcessingClass>) {
        val relations = Relations(messages)
        if (messages.errorRaised) {
            return
        }

        if (entities.isEmpty()) {
            return  // no entities found
        }

        val defaultJavaPackage = if (options.daoCompat && options.daoCompatPackage != null) {
            options.daoCompatPackage
        } else if (options.customDefaultPackage != null) {
            options.customDefaultPackage
        } else {
            // entities may be in multiple packages, so generate MyObjectBox in one that is least likely to change
            val packages = entities.map { it.packageName }
            ObjectBoxProcessor.selectPackage(packages)
        }
        val schema = Schema(Schema.DEFAULT_NAME, 1, defaultJavaPackage)

        // Parse entities.
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

        if (!syncIdModel(schema, entities)) {
            return // id model sync failed
        }

        this.schema = schema // make processed schema accessible for testing

        var completed = false
        try {
            val job = GeneratorJob(schema, createGeneratorOutput(annotatedElements))
            job.isDaoCompat = options.daoCompat
            options.flatbuffersSchemaPath?.let {
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

    private fun parseEntity(
        annotatedElements: List<ProcessingClass>,
        schema: Schema,
        relations: Relations,
        entity: ProcessingClass
    ) {
        val name = entity.simpleName
        if (debug) messages.debug("Parsing entity $name...")

        schema.entities.find { it.className == name }?.let {
            messages.error("There is already an entity class '$name': '${it.javaPackage}.${it.className}'.", entity)
            return
        }

        val entityModel = schema.addEntity(name)
        entityModel.parsedElement = entity.element
        entityModel.javaPackage = entity.packageName
        entityModel.javaPackageDao = options.daoCompatPackage ?: entityModel.javaPackage

        // @NameInDb
        val nameInDb = entity.stringValueOf(NameInDb::class.java)
        if (nameInDb != null && nameInDb.isNotEmpty()) {
            entityModel.dbName = nameInDb
        }

        // @ExternalName
        entity.stringValueOf(ExternalName::class.java)
            ?.also { entityModel.setExternalName(it) }

        // @Uid
        entity.uidValue()?.also { entityModel.modelUid = it }

        // @Sync
        if (entity.hasAnnotation(Sync::class.java)) {
            entityModel.isSyncEnabled = true
            entityModel.isSyncSharedGlobalIds = entity.booleanValueOf(Sync::class.java, "sharedGlobalIds") == true
        }

        // Parse properties.
        parseProperties(annotatedElements, relations, entityModel, entity)
        // Verify there is an @Id property.
        entityModel.ensureIdProperty()
        // Verify there is at most 1 unique property with REPLACE strategy.
        entityModel.ensureSingleUniqueReplace()

        // if not added automatically and relations are used, ensure there is a box store field
        if (!options.transformationEnabled && relations.hasRelations(entityModel) && !entityModel.hasBoxStoreField) {
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
     * Starting from the given [clazz] walks up the inheritance chain and for each super type
     * that has a match in [annotatedElements] adds that element to the [entityInheritanceChain].
     */
    private fun findAnnotatedSuperElements(
        annotatedElements: List<ProcessingClass>,
        entityInheritanceChain: MutableList<ProcessingClass>,
        clazz: ProcessingClass
    ): Boolean {
        // Implementation note: it can NOT be assumed that for each super type there will be an
        // annotated element. E.g. when processing is incremental the annotation processor only
        // receives annotated elements (for this processor @BaseEntity and @Entity classes).
        for (superClass in clazz.superTypes()) {
            val superName = superClass.qualifiedName ?: continue
            if (debug) messages.debug("${clazz.simpleName} has super type $superName.")

            // If there is an annotated element that matches this super type, find it.
            // Note: the @BaseEntity and @Entity annotation should be restricted to classes by the
            // compiler, but to be sure ignore any annotated element that is not a class.
            // Note: comparing by qualified name, so type parameters are ignored
            // (like BaseEntity<String> matches BaseEntity<T>).
            val matchingElement = annotatedElements.find {
                it.isClass && it.qualifiedName == superName
            }
            // If there is a match, add the element to the list.
            matchingElement?.let {
                entityInheritanceChain.add(it)
                if (debug) messages.debug("$superName is annotated, add to inheritance chain.")
            }

            // Continue with checking the super types of this super type.
            val hasMatches = findAnnotatedSuperElements(annotatedElements, entityInheritanceChain, superClass)

            // Do not check sibling super types if this one or its parents are matches
            // (this is fine as Java has no multi-inheritance).
            if (matchingElement != null || hasMatches) return true
        }

        return false // No matches.
    }

    private fun parseProperties(
        annotatedElements: List<ProcessingClass>,
        relations: Relations,
        entityModel: ModelEntity,
        entityClass: ProcessingClass
    ) {
        // The current entity...
        val entityInheritanceChain = mutableListOf(entityClass)
        // ...and its super classes that are annotated.
        if (findAnnotatedSuperElements(annotatedElements, entityInheritanceChain, entityClass)) {
            if (debug) messages.debug(
                "Detected entity inheritance chain: " +
                        entityInheritanceChain.joinToString(separator = "->") { it.simpleName })
        }

        // Reverse inheritance chain to parse properties starting with the super-most element in the chain
        // to ensure constructor param order is as expected: from super class to sub class,
        // then from first declared to last declared.
        val entitiesSuperMostFirst = entityInheritanceChain.reversed()
        entitiesSuperMostFirst.forEach { clazz ->
            val isSuperEntity = entitiesSuperMostFirst.last() != clazz
            with(Properties(messages, relations, entityModel, clazz, isSuperEntity)) {
                parseFields()
                entityModel.hasBoxStoreField =
                    entityModel.hasBoxStoreField || hasBoxStoreField() // Do not overwrite true.
            }
        }
    }

    /**
     * Returns true if the entity has a constructor where param types, names and order matches the properties of the
     * given entity model.
     */
    private fun ProcessingClass.hasAllPropertiesConstructor(entityModel: ModelEntity): Boolean {
        if (debug) messages.debug("Checking for all-args constructor for ${entityModel.className}...")
        val properties = entityModel.properties
        for (constructor in constructors()) {
            val parameters = constructor.parameters
            if (debug) messages.debug("Checking constructor ${constructor.description}...")
            if (parameters.size == properties.size && parametersMatchProperties(parameters, properties)) {
                if (debug) messages.debug("Valid all-args constructor found")
                return true
            }
        }
        if (debug) messages.debug("No all-args constructor found for ${entityModel.className}")
        return false
    }

    private fun parametersMatchProperties(
        parameters: List<ProcessingParameter>,
        properties: List<Property>
    ): Boolean {
        for ((idx, param) in parameters.withIndex()) {
            val property = properties[idx]
            val altName = if (options.allowNumberedConstructorArgs) "arg$idx" else null
            val parsedField = property.parsedElement as? ProcessingField
            if (parsedField != null) {
                // regular and custom type properties (have record of parsed field)
                if (param.name != parsedField.name) {
                    // note: Kotlin generates Java 6 bytecode and may not generate parameter names for any methods.
                    // Java 6 bytecode parameters are named arg0 to argn.
                    if (altName == param.name) {
                        if (debug) messages.debug(
                            "Constructor param name alternative accepted: " +
                                    "$altName for ${parsedField.name}"
                        )
                    } else {
                        if (debug) messages.debug(
                            "Constructor param name differs: " +
                                    "${param.name} vs. ${parsedField.name} ($altName)"
                        )
                        return false
                    }
                }
                if (!param.type.isSameJavaTypeAs(parsedField.type)) {
                    messages.debug("Constructor param type differs: ${param.type} vs. ${parsedField.type}")
                    return false
                }
            } else {
                // special case: virtual property (to-one target id) that has no matching field
                val paramPropertyType = param.type.propertyTypeOrNull()
                if (paramPropertyType != property.propertyType) {
                    messages.debug("Constructor param type differs (virtual property)")
                    return false
                }
                if (param.name != property.propertyName) {
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
     * Returns true if there is a constructor requiring no parameters. This is also the case if
     * all parameters of a (Kotlin) constructor have default values (the compiler then generates
     * an additional parameterless constructor) or if a (Java) class has no declared constructors
     * (it then has a default constructor, which KSP does not report).
     */
    private fun ProcessingClass.hasNoArgConstructor(): Boolean {
        val constructors = constructors()
        if (constructors.isEmpty()) return true
        if (constructors.any { it.parameters.isEmpty() }) return true
        if (constructors.any { constructor ->
                constructor.parameters.isNotEmpty() && constructor.parameters.all { it.hasDefault }
            }) return true
        return false
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
            daoCompat = options.daoCompat,
            completed = completed,
            entityCount = schema.entities.size,
            propertyCount = propertyCount,
            toManyCount = toManyCount,
            toOneCount = toOneCount
        )
    }

    private fun syncIdModel(schema: Schema, entities: List<ProcessingClass>): Boolean {
        val customModelPath = options.customModelPath
        val useDefaultPath = customModelPath.isNullOrEmpty()
        val modelFile = if (useDefaultPath) {
            try {
                val projectRoot = findProjectRoot(entities)
                File(projectRoot, "objectbox-models/default.json")
            } catch (e: FileNotFoundException) {
                messages.error(
                    "Could not find project root to create model file in. " +
                            "Add absolute path to model file with processor option '${ObjectBoxProcessor.OPTION_MODEL_PATH}'. (${e.message})"
                )
                return false
            }
        } else {
            File(customModelPath)
        }

        val modelFolder = modelFile.parentFile
        if (!modelFolder.isDirectory) {
            if (useDefaultPath) {
                if (!modelFolder.mkdirs()) {
                    messages.error(
                        "Could not create default model folder at '${modelFolder.absolutePath}'. " +
                                "Add absolute path to model file with processor option '${ObjectBoxProcessor.OPTION_MODEL_PATH}'."
                    )
                    return false
                }
            } else {
                messages.error(
                    "The model folder does not exist at '${modelFolder.absolutePath}'" +
                            " (based on the option ${ObjectBoxProcessor.OPTION_MODEL_PATH}='$customModelPath')."
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
