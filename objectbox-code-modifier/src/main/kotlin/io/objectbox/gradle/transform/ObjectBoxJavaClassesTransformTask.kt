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
        ObjectBoxJavaTransform(debug.get()).transform(compiledClasses, null, copyNonTransformed = false)
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