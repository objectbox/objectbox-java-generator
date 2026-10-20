/*
 * ObjectBox Build Tools
 * Copyright (C) 2026 ObjectBox Ltd.
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

package io.objectbox.processor.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.generator.GeneratorOutput
import io.objectbox.generator.model.Schema
import io.objectbox.processor.ObjectBoxProcessor
import io.objectbox.processor.ObjectBoxProcessorCore
import io.objectbox.processor.ProcessingClass
import io.objectbox.reporting.BasicBuildTracker
import io.objectbox.reporting.ObjectBoxBuildConfig
import java.io.File
import java.io.FileNotFoundException

/**
 * Creates the ObjectBox KSP processor, registered with KSP via
 * `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.
 */
class ObjectBoxKspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ObjectBoxKspProcessor(environment)
    }
}

/**
 * ObjectBox KSP processor which parses [@Entity][Entity] and [@BaseEntity][BaseEntity]
 * classes to generate helper classes and a model file from. Also helps the user with various
 * errors to prevent misconfigurations or incorrect usage.
 *
 * This is the KSP based equivalent of [ObjectBoxProcessor] (the annotation processor used for
 * Java projects and kapt) and produces identical generated code and model files. Unlike it, this
 * does not support the daoCompat options.
 */
class ObjectBoxKspProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val codeGenerator: CodeGenerator = environment.codeGenerator

    private val customModelPath: String? = environment.options[ObjectBoxProcessor.OPTION_MODEL_PATH]
    private val customDefaultPackage: String? = environment.options[ObjectBoxProcessor.OPTION_MYOBJECTBOX_PACKAGE]
    private val flatbuffersSchemaPath: String? =
        environment.options[ObjectBoxProcessor.OPTION_FLATBUFFERS_SCHEMA_FOLDER]
    private val debug: Boolean = "true" == environment.options[ObjectBoxProcessor.OPTION_DEBUG]
    private val transformationEnabled: Boolean =
        "false" != environment.options[ObjectBoxProcessor.OPTION_TRANSFORMATION_ENABLED] // default true
    private val allowNumberedConstructorArgs: Boolean =
        "false" != environment.options[ObjectBoxProcessor.OPTION_ALLOW_NUMBERED_CONSTRUCTOR_ARGS] // default true

    private val messages = KspMessages(environment.logger, debug)

    /** Entities are processed in the first round, prevents processing generated code in later rounds. */
    private var processed = false

    private val core = Core(
        ObjectBoxProcessorCore.Options(
            customModelPath = customModelPath,
            customDefaultPackage = customDefaultPackage,
            flatbuffersSchemaPath = flatbuffersSchemaPath,
            debug = debug,
            transformationEnabled = transformationEnabled,
            allowNumberedConstructorArgs = allowNumberedConstructorArgs
        )
    )

    // make processed schema accessible for testing
    val schema: Schema?
        get() = core.schema

    init {
        messages.debug(
            """Starting KSP processor
            modelPath=$customModelPath
            myObjectBoxPackage=$customDefaultPackage
            flatbuffersSchemaFolder=$flatbuffersSchemaPath
            transformationEnabled=$transformationEnabled
            allowNumberedConstructorArgs=$allowNumberedConstructorArgs"""
        )
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) return emptyList()
        processed = true
        try {
            findAndParse(resolver)
        } catch (e: Throwable) {
            BasicBuildTracker("Processor").trackFatal("Processing failed", e)
            throw e
        }
        return emptyList()
    }

    private fun findAndParse(resolver: Resolver) {
        // Note: sort by name so assignment of ids to new entities (and so the model file) does not
        // depend on the (unspecified) order symbols are returned in. The annotation processor
        // effectively does the same as javac returns annotated classes sorted by name.
        val entities = resolver.getSymbolsWithAnnotation(Entity::class.java.canonicalName)
            .filterIsInstance<KSClassDeclaration>()
            .sortedBy { it.simpleName.asString() }
            .toList()
        if (entities.isEmpty()) {
            return  // no entities found
        }

        val baseEntities = resolver.getSymbolsWithAnnotation(BaseEntity::class.java.canonicalName)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
        val annotatedElements: Set<KSClassDeclaration> = (entities + baseEntities).toSet()

        val kspProcessing = KspProcessing()
        core.findAndParse(
            entities.map { kspProcessing.wrap(it) },
            annotatedElements.map { kspProcessing.wrap(it) }
        )
    }

    /**
     * The shared processor implementation with the KSP API specific parts.
     */
    private inner class Core(options: Options) : ObjectBoxProcessorCore(messages, options) {

        override fun createGeneratorOutput(annotatedElements: List<ProcessingClass>): GeneratorOutput {
            // Register all files containing entity (or base entity) classes as origins of the
            // generated code: MyObjectBox depends on all of them (aggregating).
            val originFiles = annotatedElements
                .mapNotNull { (it.element as KSClassDeclaration).containingFile }
                .distinct()
            val dependencies = Dependencies(aggregating = true, *originFiles.toTypedArray())
            return KspGeneratorOutput(codeGenerator, dependencies)
        }

        override fun findProjectRoot(entities: List<ProcessingClass>): File {
            val anyEntityFile = entities.firstNotNullOfOrNull { (it.element as KSClassDeclaration).containingFile }
                ?: throw FileNotFoundException("no entity source file")
            return findProjectRootFromSource(File(anyEntityFile.filePath))
        }
    }

    /**
     * KSP has no API to get the project directory (and unlike with the Filer of the annotation
     * processor, files created with the KSP code generator can not be used as a probe as KSP
     * fails the build if they are deleted). So walk up from the path of a source file of an
     * entity until the directory containing the build folder (named "build", or containing the
     * ObjectBox build config file for custom build folder names) is found.
     */
    @Throws(FileNotFoundException::class)
    private fun findProjectRootFromSource(entityFile: File): File {
        var dir: File? = entityFile.parentFile
        for (i in 1..9) {
            val currentDir = dir ?: break
            if (File(currentDir, "build").isDirectory) {
                return currentDir
            }
            // Support custom build folder names, like the probing of the annotation processor:
            // look for a child folder containing the ObjectBox build config file.
            val hasBuildConfigChild = currentDir.listFiles()?.any { child ->
                child.isDirectory && File(child, ObjectBoxBuildConfig.FILE_NAME).exists()
            } ?: false
            if (hasBuildConfigChild) {
                return currentDir
            }
            dir = currentDir.parentFile
        }
        throw FileNotFoundException("Could not determine project folder from ${entityFile.absolutePath}")
    }

}
