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
import io.objectbox.generator.GeneratorOutput
import io.objectbox.generator.model.Schema
import io.objectbox.reporting.BasicBuildTracker
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

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
    val schema: Schema?
        get() = if (::core.isInitialized) core.schema else null

    private lateinit var core: Core

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
    private var incremental = true

    @Synchronized
    override fun init(env: ProcessingEnvironment) {
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
        incremental = "false" != options[OPTION_INCREMENTAL] // Default true (opt-out).

        messages = Messages(env.messager, debug)
        core = Core(
            ObjectBoxProcessorCore.Options(
                customModelPath = customModelPath,
                customDefaultPackage = customDefaultPackage,
                flatbuffersSchemaPath = flatbuffersSchemaPath,
                debug = debug,
                transformationEnabled = transformationEnabled,
                allowNumberedConstructorArgs = allowNumberedConstructorArgs,
                daoCompat = daoCompat,
                daoCompatPackage = daoCompatPackage
            )
        )
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
        val entities = env.getElementsAnnotatedWith(Entity::class.java)
        if (entities.size == 0) {
            return  // no entities found
        }

        val annotatedElements = mutableSetOf<Element>().run {
            addAll(entities)
            addAll(env.getElementsAnnotatedWith(BaseEntity::class.java))
            toSet()
        }

        val javaxProcessing = JavaxProcessing(elementUtils, typeUtils)
        core.findAndParse(
            entities.map { javaxProcessing.wrap(it) },
            annotatedElements.map { javaxProcessing.wrap(it) }
        )
    }

    /**
     * The shared processor implementation with the annotation processing API specific parts.
     */
    private inner class Core(options: Options) : ObjectBoxProcessorCore(messages, options) {

        override fun createGeneratorOutput(annotatedElements: List<ProcessingClass>): GeneratorOutput {
            return GeneratorOutput.create(filer)
        }

        override fun findProjectRoot(entities: List<ProcessingClass>): File {
            return findProjectRoot(filer)
        }
    }

}
