package io.objectbox.gradle.transform

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File


/**
 * Transforms class (byte code) files used by Android local unit tests (those that run on the dev machine).
 */
abstract class ObjectBoxTestClassesTransformTask : DefaultTask() {

    @get:Input
    abstract val debug: Property<Boolean>

    @get:Classpath
    abstract val compiledClasses: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun transformClasses() {
        // Clear output directory.
        val outputDir = outputDir.asFile.get()
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        ObjectBoxJavaTransform(debug.get()).transform(compiledClasses, outputDir, copyNonTransformed = false)
    }

    internal class ConfigAction(
        private val debug: Property<Boolean>,
        private val outputDir: File,
        private val inputClasspath: FileCollection
    ) : Action<ObjectBoxTestClassesTransformTask> {
        override fun execute(transformTask: ObjectBoxTestClassesTransformTask) {
            transformTask.group = "objectbox"
            transformTask.description = "Transforms Java bytecode for local unit tests."
            transformTask.debug.set(debug)
            transformTask.outputDir.set(outputDir)
            transformTask.compiledClasses.from(inputClasspath)
        }
    }

}