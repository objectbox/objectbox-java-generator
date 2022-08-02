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
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.UnitTestVariant
import io.objectbox.gradle.GradleBuildTracker
import io.objectbox.logging.log
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.util.*

/**
 * A byte-code [Transform] to be registered with the Android plugin to run before dexing (regular builds or instrumented
 * unit test builds). The transform results are stored in a directory assigned to this Transform.
 * To also support transformation for local unit test builds the registration code injects custom transform tasks
 * for each build variant with unit tests. They run an [ObjectBoxJavaTransform] before unit test code is compiled.
 *
 * @see ClassTransformer
 */
class ObjectBoxAndroidTransform(private val debug: Property<Boolean>) : Transform() {

    object Registration {
        fun to(project: Project, debug: Property<Boolean>, hasKotlinPlugin: Boolean) {
            val transform = ObjectBoxAndroidTransform(debug)

            // For regular build and instrumentation (on mobile device) tests,
            // uses the Transform API for Android Plugin 7.1 and older.
            val androidExtension = project.extensions.findByType(BaseExtension::class.java)
                ?: error("The Android Gradle plugin BaseExtension was not found.")
            androidExtension.registerTransform(transform)

            // For local (on dev machine) unit tests.
            // A transform registered like above does only run when dexing is required
            // (!= for local unit tests) so inject a custom transform task before local unit tests are compiled.
            // Note: could use findByType(TestedExtension::class.java), however, then do not
            // have access to compiled sources to add to transform task classpath.
            // Note: see ProjectEnv.androidPluginIds which plugins are supported.
            when (androidExtension) {
                is AppExtension -> androidExtension.applicationVariants.all {
                    injectTransformTask(project, debug, hasKotlinPlugin, it, it.unitTestVariant)
                }
                // Used for Android Instant App base and feature modules, but deprecated as of
                // Android Plugin 3.4.0 (April 2019). Behaves similar to the library plugin.
                // https://developer.android.com/topic/google-play-instant/feature-module-migration
                is FeatureExtension -> androidExtension.featureVariants.all {
                    injectTransformTask(project, debug, hasKotlinPlugin, it, it.unitTestVariant)
                }
                is LibraryExtension -> androidExtension.libraryVariants.all {
                    injectTransformTask(project, debug, hasKotlinPlugin, it, it.unitTestVariant)
                }
                // Note: TestExtension is only used to create a separate instrumentation test module,
                // it can not run local unit tests.
                // https://developer.android.com/studio/test/advanced-test-setup#use-separate-test-modules-for-instrumented-tests
                // is TestExtension ->
            }
        }

        /**
         * Creates a task that transforms the variants JavaCompile and if available KotlinCompile output before the unit
         * test task for that variant runs. Transformed classes are stored in a dedicated build directory which is added
         * to the classpath of the test task.
         *
         * This mimics what registering a [Transform] does which only runs for
         * instrumented (on-device) tests and assembling an app/library
         * (bug report to support unit tests at https://issuetracker.google.com/issues/37076369).
         */
        private fun injectTransformTask(
            project: Project, debug: Property<Boolean>, hasKotlinPlugin: Boolean,
            baseVariant: BaseVariant, unitTestVariant: UnitTestVariant
        ) {
            // Add compiled Java project sources, makes Java compile task a dependency.
            // Note: javaCompileProvider requires at least Android Gradle Plugin 3.3.0
            val inputClasspath = project.files(baseVariant.javaCompileProvider.map { it.destinationDirectory })

            // Add compiled Java test sources, makes Java test compile task a dependency.
            inputClasspath.from(unitTestVariant.javaCompileProvider.map { it.destinationDirectory })

            // Same for Kotlin.
            // Applying Kotlin plugin is optional for Android projects, so check before accessing Kotlin plugin classes.
            if (hasKotlinPlugin) {
                project.plugins.withType(KotlinBasePluginWrapper::class.java) {
                    addDestinationDirOfKotlinCompile(inputClasspath, project, baseVariant)
                    addDestinationDirOfKotlinCompile(inputClasspath, project, unitTestVariant)
                }
            }

            val unitTestVariantNameCapitalized = unitTestVariant.name
                .replaceFirstChar { it.titlecase(Locale.getDefault()) }
            val transformTaskName = "objectboxTransform$unitTestVariantNameCapitalized"
            val outputDir = project.buildDir.resolve("intermediates/objectbox/${unitTestVariant.dirName}")
            // Use register to defer creation until use.
            val transformTask = project.tasks.register(
                transformTaskName,
                ObjectBoxTestClassesTransformTask::class.java,
                ObjectBoxTestClassesTransformTask.ConfigAction(debug, outputDir, inputClasspath)
            )

            // Configure the test classpath by appending the transform output file collection to the start of
            // the test classpath so the transformed files override the original ones. This also makes
            // the test task (the one that runs the tests) depend on the transform task.
            val outputFileCollection = project.files(transformTask.map { it.outputDir })
            val testTaskProvider = project.tasks.named("test$unitTestVariantNameCapitalized", Test::class.java)
            testTaskProvider.configure {
                it.classpath = outputFileCollection + it.classpath
            }
        }

        private fun addDestinationDirOfKotlinCompile(
            inputClasspath: ConfigurableFileCollection,
            project: Project,
            variant: BaseVariant
        ) {
            // Using naming scheme promised by https://kotlinlang.org/docs/reference/using-gradle.html#compiler-options
            val variantNameCapitalized = variant.name
                .replaceFirstChar { it.titlecase(Locale.getDefault()) }
            val kotlinTaskName = "compile${variantNameCapitalized}Kotlin"
            val kotlinCompileTaskProvider = project.tasks.named(kotlinTaskName, KotlinCompile::class.java)
            inputClasspath.from(kotlinCompileTaskProvider.map { it.destinationDirectory })
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
        val debug = debug.get()
        try {
            val probedClasses = mutableListOf<ProbedClass>()

            val classProber = ClassProber()
            info.inputs.forEach { transformInput ->
                // Look through directory inputs to transform or just copy.
                transformInput.directoryInputs.forEach { directoryInput ->
                    if (debug) log("Input directory: ${directoryInput.name} ${directoryInput.file}")
                    // Output files to directory unique for this input directory.
                    val outDir = info.outputProvider.getContentLocation(
                        directoryInput.name,
                        outputTypes,
                        scopes,
                        Format.DIRECTORY
                    )
                    if (debug) log("Output directory: $outDir")

                    // TODO incremental: directoryInput.changedFiles

                    var classes = 0
                    var copied = 0
                    directoryInput.file.walk().filter { it.isFile }.forEach { file ->
                        if (file.name.endsWith(".class")) {
                            probedClasses += classProber.probeClass(file, outDir)
                            classes += 1
                        } else {
                            val relativePath = file.toRelativeString(directoryInput.file)
                            val destFile = File(outDir, relativePath)
                            file.copyTo(destFile, overwrite = true)
                            copied += 1
                        }
                    }
                    if (debug) log("Copied $copied files, will check $classes classes if transform required.")
                }

                // Not looking at class files in JARs, just copy them.
                // It appears only Android Gradle Plugin 3.6.0 uses this to pass the R classes in a JAR.
                // https://github.com/objectbox/objectbox-java/issues/817
                transformInput.jarInputs.forEach { jarInput ->
                    if (debug) log("Input JAR: ${jarInput.name} ${jarInput.file}")
                    // Note: TransformOutputProvider.getContentLocation(name, ...) returns the same file if all params
                    // match. Make sure name differs for each JAR to avoid overwriting an already copied JAR.
                    val outFileJar =
                        info.outputProvider.getContentLocation(jarInput.name, outputTypes, scopes, Format.JAR)
                    jarInput.file.copyTo(outFileJar, overwrite = true)
                    if (debug) log("Output JAR: $outFileJar")
                }
            }

            ClassTransformer(debug).transformOrCopyClasses(probedClasses)

        } catch (e: Throwable) {
            val buildTracker = GradleBuildTracker("Transformer")
            if (e is TransformException) buildTracker.trackError("Transform failed", e)
            else buildTracker.trackFatal("Transform failed", e)
            throw e
        }
    }

}
