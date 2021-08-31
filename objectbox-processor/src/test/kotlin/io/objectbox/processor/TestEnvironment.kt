package io.objectbox.processor

import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.model.Schema
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import javax.tools.JavaFileObject

/**
 * Helps test processor with custom model file directory, easy access to verify schema and model.
 * Uses the compile-testing library (https://github.com/google/compile-testing).
 *
 * Works on a copy of the given model file. After compiling verifies the working copy matches the original model file.
 *
 * If [useTemporaryModelFile] will add "tmp" postfix to model file name so it will be ignored by source control
 * (see .gitignore rules). Removes the file if it exists.
 * Also will not compare against original as it is assumed there is none.
 */
class TestEnvironment(
    modelFile: String,
    private val myObjectBoxPackage: String? = null,
    private val optionDisableTransform: Boolean = false,
    private val useTemporaryModelFile: Boolean = false
) {

    // tests run from IntelliJ are relative to module directory
    private val modelFilesPathModule = "src/test/resources/objectbox-models/"

    // tests run from Gradle are relative to project directory
    private val modelFilesPathProject = "objectbox-processor/$modelFilesPathModule"

    private val modelFilePath: String
    private val modelFilePathOriginal: String
    private val modelFileProcessorOption: List<String>
        get() {
            val options = mutableListOf("-A${ObjectBoxProcessor.OPTION_MODEL_PATH}=$modelFilePath")
            options += "-A${ObjectBoxProcessor.OPTION_DEBUG}=true"
            if (myObjectBoxPackage != null) options += "-A${ObjectBoxProcessor.OPTION_MYOBJECTBOX_PACKAGE}=$myObjectBoxPackage"
            if (optionDisableTransform) options += "-A${ObjectBoxProcessor.OPTION_TRANSFORMATION_ENABLED}=false"
            return options
        }

    val processor = ObjectBoxProcessorShim()
    val schema: Schema
        get() = processor.schema!!

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

    fun compile(vararg files: String, modelExpectedToChange: Boolean = false): Compilation {
        val fileObjects = files.map { JavaFileObjects.forResource("$it.java") }
        return compile(fileObjects, modelExpectedToChange)
    }

    fun compile(files: List<JavaFileObject>, modelExpectedToChange: Boolean = false): Compilation {
        val compilation = Compiler.javac()
            .withProcessors(processor)
            .withOptions(modelFileProcessorOption)
            .compile(files)
        if (!useTemporaryModelFile && !modelExpectedToChange) assertModelFileMatchesOriginal()
        return compilation
    }

    fun compileDaoCompat(vararg files: String): Compilation {
        val fileObjects = files.map { JavaFileObjects.forResource("$it.java") }
        val compilation = Compiler.javac()
            .withProcessors(processor)
            .withOptions(
                modelFileProcessorOption
                // disabled as compat DAO currently requires entity property getters/setters
//                        "-A${ObjectBoxProcessor.OPTION_DAO_COMPAT}=true"
            )
            .compile(fileObjects)
        if (!useTemporaryModelFile) assertModelFileMatchesOriginal()
        return compilation
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