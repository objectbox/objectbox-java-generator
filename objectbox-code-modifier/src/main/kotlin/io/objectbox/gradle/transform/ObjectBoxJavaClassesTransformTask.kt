/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2024 ObjectBox Ltd.
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

package io.objectbox.gradle.transform

import io.objectbox.logging.logWarning
import io.objectbox.reporting.BasicBuildTracker
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Transforms class (byte code) files produced by JVM projects (projects applying just a Java plugin)
 * in place.
 */
abstract class ObjectBoxJavaClassesTransformTask : DefaultTask() {

    @get:Internal
    abstract val debug: Property<Boolean>

    @get:Classpath
    abstract val compiledClasses: ConfigurableFileCollection

    @TaskAction
    fun transformClasses() {
        // Currently transforming in place, no need to copy non-transformed files.
        // In the future, might want to change this to output to a custom directory,
        // then re-wire that to be used as the classes directory of a source set.
        try {
            // Process classpath in reverse order to ensure output for first
            // items overwrites output for last items.
            val byteCodeDirs = compiledClasses.files.toList().reversed()
            // Currently not modifying JAR files as there are not expected to be
            // some, but instruct users to report if there are.
            byteCodeDirs.forEach {
                if (it.isFile && it.extension == "jar") {
                    logWarning("Detected JAR file in transform classpath ($it), relations might not work, please report this to us.")
                }
            }

            ObjectBoxJavaTransform(debug.get()).transform(
                byteCodeDirs,
                null,
                copyNonTransformed = false
            )
        } catch (e: Throwable) {
            val buildTracker = BasicBuildTracker("Transformer")
            if (e is TransformException) buildTracker.trackError("Transform failed", e)
            else buildTracker.trackFatal("Transform failed", e)
            throw e
        }
    }

    class ConfigAction(
        private val debug: Property<Boolean>,
        private val inputClasspath: FileCollection
    ) : Action<ObjectBoxJavaClassesTransformTask> {
        override fun execute(transformTask: ObjectBoxJavaClassesTransformTask) {
            transformTask.group = "objectbox"
            transformTask.description = "Transforms Java bytecode for JVM projects."
            transformTask.debug.set(debug)
            transformTask.compiledClasses.from(inputClasspath)
        }
    }
}