/*
 * ObjectBox Build Tools
 * Copyright (C) 2018-2026 ObjectBox Ltd.
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

import com.google.common.truth.Truth.assertWithMessage
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.model.Schema
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files

/**
 * Helps test a processor with custom model file directory, easy access to verify schema and model.
 * There are implementations to run the annotation processor ([JavacTestEnvironment]) and the KSP
 * processor ([KspTestEnvironment]), typically chosen by [BaseProcessorTest.env].
 *
 * Works on a copy of the given model file. After compiling verifies the working copy matches the original model file.
 *
 * If [useTemporaryModelFile] will add "tmp" postfix to model file name so it will be ignored by source control
 * (see .gitignore rules). Removes the file if it exists.
 * Also will not compare against original as it is assumed there is none.
 *
 * Use this like:
 *
 * ```
 * @Language("Java")
 * val exampleSource =
 *     """
 *     package com.example;
 *
 *     public class Example {}
 *     """.trimIndent()
 *
 * env("example-model.json")
 *     .apply {
 *         addSourceFile("com.example.Example", exampleSource)
 *     }
 *     .compile()
 *     .assertThatIt {
 *         // Assert success
 *         succeededWithoutWarnings()
 *         // Assert failure
 *         // failed()
 *         // hadErrorContaining("<error message>")
 *
 *         // Assert generated code
 *         @Language("Java") val myObjectBoxSource =
 *             """
 *             package com.example;
 *
 *             public class MyObjectBox {
 *             }
 *             """.trimIndent()
 *         generatedSourceFileMatches("com.example.MyObjectBox", myObjectBoxSource)
 *     }
 * ```
 */
abstract class TestEnvironment(
    modelFile: String,
    private val myObjectBoxPackage: String? = null,
    private val optionDisableTransform: Boolean = false,
    private val useTemporaryModelFile: Boolean = false
) {

    // tests run from IntelliJ are relative to module directory
    private val modelFilesPathModule = "src/test/resources/objectbox-models/"

    // tests run from Gradle are relative to project directory
    private val modelFilesPathProject = "objectbox-processor/$modelFilesPathModule"

    protected val modelFilePath: String
    private val modelFilePathOriginal: String

    abstract val schema: Schema

    protected val sourceFiles = mutableListOf<TestSourceFile>()

    init {
        val path = when {
            File(modelFilesPathModule).isDirectory -> modelFilesPathModule
            File(modelFilesPathProject).isDirectory -> modelFilesPathProject
            else -> throw FileNotFoundException("Can not find model file directory.")
        } + modelFile

        modelFilePathOriginal = path
        modelFilePath = if (!useTemporaryModelFile) {
            // Always work on a copy of the original model file. Compare against the original after compiling.
            val pathCopy = "$path.copy"
            File(path).copyTo(File(pathCopy), overwrite = true)
            pathCopy
        } else {
            // Create file name like "default.json.tmp" so model file is ignored by gitignore rules.
            val tempFile = "$path.tmp"
            Files.deleteIfExists(File(tempFile).toPath())
            tempFile
        }
    }

    /**
     * A Java (or for the KSP processor also Kotlin) source file to be compiled with [compile].
     */
    class TestSourceFile(val fullyQualifiedName: String, val source: String, val isKotlin: Boolean = false)

    protected fun processorOptions(): Map<String, String> {
        val options = mutableMapOf(
            ObjectBoxProcessor.OPTION_MODEL_PATH to modelFilePath,
            ObjectBoxProcessor.OPTION_DEBUG to "true"
        )
        if (myObjectBoxPackage != null) options[ObjectBoxProcessor.OPTION_MYOBJECTBOX_PACKAGE] = myObjectBoxPackage
        if (optionDisableTransform) options[ObjectBoxProcessor.OPTION_TRANSFORMATION_ENABLED] = "false"
        return options
    }

    /**
     * Adds a source file to be compiled with [compile].
     *
     * See [TestEnvironment] for an example.
     */
    fun addSourceFile(fullyQualifiedName: String, source: String) {
        sourceFiles.add(TestSourceFile(fullyQualifiedName, source))
    }

    /**
     * Adds a Kotlin source file to be compiled with [compile]. Only supported by the KSP processor.
     */
    fun addKotlinSourceFile(fullyQualifiedName: String, source: String) {
        sourceFiles.add(TestSourceFile(fullyQualifiedName, source, isKotlin = true))
    }

    /**
     * Adds a source file from a .java file resource to be compiled with [compile].
     */
    fun addResourceSourceFile(resourceName: String) {
        sourceFiles.add(resourceSourceFile(resourceName))
    }

    fun addEntitySourceFile(
        name: String = "Example",
        sync: Boolean = false,
        additionalImports: String = "",
        body: () -> String
    ) {
        // language=Java
        val syncImport = if (sync) "import io.objectbox.annotation.Sync;" else ""
        val syncAnnotation = if (sync) "@Sync" else ""

        // language=Java
        val source = """
        package example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        $syncImport
${additionalImports.replaceIndent("        ")}

        @Entity
        $syncAnnotation
        public class $name {
            @Id long id;

${body().replaceIndent("            ")}
        }
        """
        addSourceFile("example.$name", source.trimIndent())
    }

    /**
     * Compiles the source files added with [addSourceFile] and, if not [TestEnvironment.useTemporaryModelFile]
     * and not [modelExpectedToChange], asserts the generated model JSON file matches the one given to [TestEnvironment].
     *
     * See [TestEnvironment] for an example.
     */
    fun compile(modelExpectedToChange: Boolean = false): TestCompilation {
        check(sourceFiles.isNotEmpty())
        return compileAndCheckModel(sourceFiles, modelExpectedToChange)
    }

    /**
     * Variant of [compile] that expects source files as a .java file resource.
     *
     * New code should add source code using [addSourceFile] instead.
     */
    fun compile(vararg files: String, modelExpectedToChange: Boolean = false): TestCompilation {
        val resourceSources = files.map { resourceSourceFile(it) }
        return compileAndCheckModel(resourceSources, modelExpectedToChange)
    }

    private fun compileAndCheckModel(
        files: List<TestSourceFile>,
        modelExpectedToChange: Boolean
    ): TestCompilation {
        val compilation = compileSources(files)
        if (!useTemporaryModelFile && !modelExpectedToChange) assertModelFileMatchesOriginal()
        return compilation
    }

    protected abstract fun compileSources(files: List<TestSourceFile>): TestCompilation

    private fun resourceSourceFile(resourceName: String): TestSourceFile {
        val resourcePath = "$resourceName.java"
        val source = javaClass.classLoader.getResource(resourcePath)?.readText()
            ?: throw IllegalArgumentException("Test resource not found: $resourcePath")
        // Determine the qualified name from the package declaration and file name.
        val packageName = Regex("""package\s+([\w.]+)""").find(source)?.groupValues?.get(1)
        val simpleName = resourcePath.substringAfterLast('/').removeSuffix(".java")
        val qualifiedName = if (packageName != null) "$packageName.$simpleName" else simpleName
        return TestSourceFile(qualifiedName, source)
    }

    /**
     * Asserts the model file contains the same string as the original file it was copied from.
     */
    private fun assertModelFileMatchesOriginal() {
        // Note: remove Windows CR (\r) newline character on files, might be checked out on Windows.
        // ObjectBox always generates LF (\n) only.
        assertWithMessage("Model file:\n    $modelFilePath\nshould have matched original:\n    $modelFilePathOriginal")
            .that(File(modelFilePath).readText().replace("\r", ""))
            .isEqualTo(File(modelFilePathOriginal).readText().replace("\r", ""))
    }

    fun readModel(): IdSync {
        return IdSync(File(modelFilePath))
    }

    fun isModelFileExists(): Boolean {
        return File(modelFilePath).exists()
    }
}
