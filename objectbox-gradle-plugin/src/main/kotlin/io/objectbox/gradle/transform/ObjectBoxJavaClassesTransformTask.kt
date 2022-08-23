package io.objectbox.gradle.transform

import io.objectbox.gradle.getObjectBoxPluginOptions
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskAction

/**
 * Transforms class (byte code) files produced by JVM projects (projects applying just a Java plugin)
 * in place.
 */
abstract class ObjectBoxJavaClassesTransformTask : DefaultTask() {

    @get:Classpath
    abstract val compiledClasses: ConfigurableFileCollection

    @TaskAction
    fun transformClasses() {
        // Get current options during action as this is registered before the
        // evaluation phase finishes (where options are not final values).
        val debug = project.getObjectBoxPluginOptions()?.debug ?: false

        // Currently transforming in place, no need to copy non-transformed files.
        // In the future, might want to change this to output to a custom directory,
        // then re-wire that to be used as the classes directory of a source set.
        ObjectBoxJavaTransform(debug).transform(compiledClasses, null, copyNonTransformed = false)
    }

    internal class ConfigAction(
        private val inputClasspath: FileCollection
    ) : Action<ObjectBoxJavaClassesTransformTask> {
        override fun execute(transformTask: ObjectBoxJavaClassesTransformTask) {
            transformTask.group = "objectbox"
            transformTask.description = "Transforms Java bytecode for JVM projects."
            transformTask.compiledClasses.from(inputClasspath)
        }
    }
}