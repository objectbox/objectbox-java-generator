package io.objectbox.processor

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
 * If model file name ends in "-temp.json" it will be ignored by source control.
 */
class TestEnvironment(modelFile: String,
                      val myObjectBoxPackage: String? = null,
                      val optionDisableTransform: Boolean = false,
                      copyModelFile: Boolean = false) {

    // tests run from IntelliJ are relative to module directory
    private val modelFilesPathModule = "src/test/resources/objectbox-models/"

    // tests run from Gradle are relative to project directory
    private val modelFilesPathProject = "objectbox-processor/$modelFilesPathModule"

    private val modelFilePath: String
    private val modelFileProcessorOption: List<String>
        get() {
            val options = mutableListOf("-A${ObjectBoxProcessor.OPTION_MODEL_PATH}=$modelFilePath")
            options += "-A${ObjectBoxProcessor.OPTION_DEBUG}=true"
            if (optionDisableTransform) options += "-A${ObjectBoxProcessor.OPTION_TRANSFORMATION_ENABLED}=false"
            if (myObjectBoxPackage != null) options += "-A${ObjectBoxProcessor.OPTION_MYOBJECTBOX_PACKAGE}=$myObjectBoxPackage"
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

        modelFilePath = if (copyModelFile) {
            val pathCopy = "$path.copy"
            File(path).copyTo(File(pathCopy), overwrite = true)
            pathCopy
        } else path
    }

    fun compile(vararg files: String): Compilation {
        val fileObjects = files.map { JavaFileObjects.forResource("$it.java") }
        return compile(fileObjects)
    }

    fun compile(files: List<JavaFileObject>): Compilation {
        return Compiler.javac()
                .withProcessors(processor)
                .withOptions(modelFileProcessorOption)
                .compile(files)
    }

    fun compileDaoCompat(vararg files: String): Compilation {
        val fileObjects = files.map { JavaFileObjects.forResource("$it.java") }
        return Compiler.javac()
                .withProcessors(processor)
                .withOptions(
                        modelFileProcessorOption
                        // disabled as compat DAO currently requires entity property getters/setters
//                        "-A${ObjectBoxProcessor.OPTION_DAO_COMPAT}=true"
                )
                .compile(fileObjects)
    }

    fun cleanModelFile() {
        Files.deleteIfExists(File(modelFilePath).toPath())
    }

    fun readModel(): IdSync {
        return IdSync(File(modelFilePath))
    }

    fun isModelFileExists(): Boolean {
        return File(modelFilePath).exists()
    }
}