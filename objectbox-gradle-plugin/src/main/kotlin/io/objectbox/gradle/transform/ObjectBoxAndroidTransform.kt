/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle.transform

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.FeaturePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.UnitTestVariant
import io.objectbox.gradle.GradleBuildTracker
import io.objectbox.gradle.PluginOptions
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File

/**
 * A byte-code [Transform] to be registered with the Android plugin to run before dexing (regular builds or instrumented
 * unit test builds). The transform results are stored in a directory assigned to this Transform.
 * To also support transformation for local unit test builds the registration code injects custom transform tasks
 * for each build variant with unit tests. They run an [ObjectBoxJavaTransform] before unit test code is compiled.
 *
 * @see ClassTransformer
 */
class ObjectBoxAndroidTransform(val options: PluginOptions) : Transform() {

    object Registration {
        fun to(project: Project, options: PluginOptions) {
            val transform = ObjectBoxAndroidTransform(options)
            getAllExtensions(project).forEach { extension ->
                // for regular build and instrumentation tests
                extension.registerTransform(transform)
                // for local unit tests
                // a transform registered like above does only run when dexing is required (!= for local unit tests)
                // so inject our own transform task before local unit tests are compiled
                when (extension) {
                    is AppExtension -> extension.applicationVariants.all {
                        injectTransformTask(project, options, it, it.unitTestVariant)
                    }
                    is LibraryExtension -> extension.libraryVariants.all {
                        injectTransformTask(project, options, it, it.unitTestVariant)
                    }
                    is FeatureExtension -> extension.featureVariants.all {
                        injectTransformTask(project, options, it, it.unitTestVariant)
                    }
                    is TestExtension -> extension.applicationVariants.all {
                        injectTransformTask(project, options, it, it.unitTestVariant)
                    }
                }
            }
        }

        private fun getAllExtensions(project: Project): Set<BaseExtension> {
            val exClasses = getAndroidExtensionClasses(project)
            if (exClasses.isEmpty()) throw TransformException(
                    "No Android plugin found - please apply ObjectBox plugins after the Android plugin")
            return exClasses.map { project.extensions.getByType(it) as BaseExtension }.toSet()
        }

        fun getAndroidExtensionClasses(project: Project): MutableList<Class<out BaseExtension>> {
            // Should be only one plugin, but let's assume there can be a combination just in case...
            val exClasses = mutableListOf<Class<out BaseExtension>>()
            val plugins = project.plugins
            if (plugins.hasPlugin(LibraryPlugin::class.java)) exClasses += LibraryExtension::class.java
            if (plugins.hasPlugin(TestPlugin::class.java)) exClasses += TestExtension::class.java
            if (plugins.hasPlugin(AppPlugin::class.java)) exClasses += AppExtension::class.java
            if (plugins.hasPlugin(FeaturePlugin::class.java)) exClasses += FeatureExtension::class.java
            return exClasses
        }

        /**
         * Creates a task that transforms the variants JavaCompile and if available KotlinCompile output before the unit
         * test JavaCompile task for that variant runs. Unlike a regular [Transform] this overwrites the variants
         * JavaCompile and KotlinCompile output.
         */
        private fun injectTransformTask(project: Project, options: PluginOptions,
                                        baseVariant: BaseVariant, unitTestVariant: UnitTestVariant?) {
            if (unitTestVariant == null) {
                return
            }

            val baseJavaCompile = baseVariant.javaCompileProvider

            // use register to defer creation until use
            val transformTask = project.tasks.register("objectboxTransform${unitTestVariant.name.capitalize()}")
            transformTask.configure {
                it.group = "objectbox"
                it.description = "Transforms Java bytecode for local unit tests"
                it.mustRunAfter(baseJavaCompile)
                it.doLast {
                    // fine to get() JavaC task, no more need to defer its creation
                    val compileAppOutput = baseJavaCompile.get().destinationDir

                    // using naming scheme promised by https://kotlinlang.org/docs/reference/using-gradle.html#compiler-options
                    val kotlinTaskName = "compile${baseVariant.name.capitalize()}Kotlin"

                    // Kotlin tasks are currently added after project evaluation, so detect them at runtime
                    val kotlinCompile = project.tasks.findByName(kotlinTaskName)
                    if (kotlinCompile != null && kotlinCompile is AbstractCompile) {
                        // Kotlin + Java
                        ObjectBoxJavaTransform(options.debug).transform(listOf(kotlinCompile.destinationDir, compileAppOutput))
                    } else {
                        // Java
                        ObjectBoxJavaTransform(options.debug).transform(listOf(compileAppOutput))
                    }
                }
            }

            unitTestVariant.javaCompileProvider.configure {
                it.dependsOn(transformTask)
            }
        }
    }


    override fun getName(): String {
        return "ObjectBoxAndroidTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return mutableSetOf(QualifiedContent.Scope.PROJECT)
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(info: TransformInvocation) {
        super.transform(info)
        try {
            val probedClasses = mutableListOf<ProbedClass>()

            // write transformed files to directory unique for this transform
            val outDir = info.outputProvider.getContentLocation("objectbox", inputTypes, scopes, Format.DIRECTORY)

            val classProber = ClassProber()
            info.inputs.flatMap { it.directoryInputs }.forEach { directoryInput ->
                // TODO incremental: directoryInput.changedFiles

                directoryInput.file.walk().filter { it.isFile }.forEach { file ->
                    if (file.name.endsWith(".class")) {
                        probedClasses += classProber.probeClass(file, outDir)
                    } else {
                        val relativePath = file.toRelativeString(directoryInput.file)
                        val destFile = File(outDir, relativePath)
                        file.copyTo(destFile, overwrite = true)
                        if (options.debug) println("Copied $file to $destFile")
                    }
                }
            }

            ClassTransformer(options.debug).transformOrCopyClasses(probedClasses)

        } catch (e: Throwable) {
            val buildTracker = GradleBuildTracker("Transformer")
            if (e is TransformException) buildTracker.trackError("Transform failed", e)
            else buildTracker.trackFatal("Transform failed", e)
            throw e
        }
    }

}
