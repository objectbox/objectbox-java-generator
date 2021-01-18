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

package io.objectbox.gradle

import io.objectbox.gradle.transform.ObjectBoxAndroidTransform
import io.objectbox.gradle.transform.ObjectBoxJavaTransform
import io.objectbox.gradle.transform.TransformException
import io.objectbox.gradle.util.GradleCompat
import io.objectbox.logging.log
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile

/**
 * A Gradle plugin that depending on the other plugins/dependencies of a project it is applied to
 * - adds dependencies for the ObjectBox annotation processor,
 * - adds dependencies for the ObjectBox Java, Kotlin and native (Android, Linux, Windows, Mac) libraries,
 * - for Android projects, configures [ObjectBoxAndroidTransform],
 * - for Java projects, adds a [ObjectBoxJavaTransform] task that runs after the compile task.
 * - adds a [PrepareTask] that runs as part of the build task.
 */
open class ObjectBoxGradlePlugin : Plugin<Project> {
    companion object {
        const val DEBUG = false
    }

    /**
     * The Gradle plugin id as registered in resources/META-INF/gradle-plugins.
     */
    internal open val pluginId = "io.objectbox"

    private val buildTracker = GradleBuildTracker("GradlePlugin")

    override fun apply(project: Project) {
        try {
            val env = ProjectEnv(project)
            if (!(env.hasAndroidPlugin || env.hasJavaPlugin || env.hasKotlinPlugin)) {
                throw InvalidPluginException(
                    "'$pluginId' expects one of the following plugins to be applied to the project:\n" +
                            "\t* java\n" +
                            "\t* kotlin\n" +
                            "\t${env.androidPluginIds.joinToString("\n\t") { "* $it" }}"
                )
            }
            addDependenciesAnnotationProcessor(env)
            addDependencies(env)

            // ensure Android plugin API is available
            if (env.hasAndroidPlugin) {
                // Cannot use afterEvaluate to register Android transform, thus our plugin must be applied after Android
                if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isNotEmpty()) {
                    ObjectBoxAndroidTransform.Registration.to(project, env.options)
                }
            } else {
                // fall back to Gradle task
                createPlainJavaTransformTask(env)
            }

            createPrepareTask(env)
        } catch (e: Throwable) {
            if (e is TransformException) buildTracker.trackError("Transform preparation failed", e)
            else if (e !is InvalidPluginException) buildTracker.trackFatal("Applying plugin failed", e)
            throw e
        }
    }

    private fun createPlainJavaTransformTask(env: ProjectEnv) {
        // wait until after project evaluation so SourceSets defined in build configs are included
        val project = env.project
        project.afterEvaluate { _ ->
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            javaPlugin.sourceSets.forEach { sourceSet ->
                // name task based on SourceSet
                val sourceSetName = sourceSet.name
                val taskName =
                    "objectboxJavaTransform" + if (sourceSetName != "main") sourceSetName.capitalize() else ""

                // use register to defer creation until use
                val transformTask = GradleCompat.get().registerTask(project, taskName)

                // verify classes and compileJava task exist, attach to lifecycle
                // assumes that classes task depends on compileJava depends on compileKotlin
                val classesTaskName = sourceSet.classesTaskName
                try {
                    GradleCompat.get().configureTask(project, sourceSet.classesTaskName, Action {
                        it.dependsOn(transformTask)
                    })
                } catch (e: UnknownDomainObjectException) {
                    throw RuntimeException("Could not find classes task '$classesTaskName'.", e)
                }

                if (env.debug) log("Added $taskName task, depends on $classesTaskName task.")

                val compileJavaTask = try {
                    GradleCompat.get().getTask(project, sourceSet.compileJavaTaskName)
                } catch (e: UnknownTaskException) {
                    throw RuntimeException("Could not find compileJava task '${sourceSet.compileJavaTaskName}'.", e)
                }

                GradleCompat.get().configureTask(project, taskName, Action {
                    it.group = "objectbox"
                    it.description = "Transforms Java bytecode"

                    it.mustRunAfter(compileJavaTask)

                    it.doLast {
                        // fine to get() compileJava task, no more need to defer its creation
                        val compileJavaTaskOutputDir = GradleCompat.get()
                            .getTask(project, JavaCompile::class.java, sourceSet.compileJavaTaskName)
                            .destinationDir
                        ObjectBoxJavaTransform(env.debug).transform(listOf(compileJavaTaskOutputDir))
                    }
                })
            }
        }
    }

    private fun createPrepareTask(env: ProjectEnv) {
        val project = env.project

        // use register to defer creation until use
        val prepareTaskName = "objectboxPrepareBuild"

        val prepareTask = GradleCompat.get()
            .registerTask(project, prepareTaskName, PrepareTask::class.java, env, buildTracker)
        if (DEBUG) log("Registered $prepareTask in $project")

        // make build task depend on prepare task
        val configureDepends = Action<Task> { it.dependsOn(prepareTask) }
        try {
            GradleCompat.get().configureTask(project, "preBuild", configureDepends)  // Android
        } catch (e: Exception) {
            GradleCompat.get().configureTask(project, "build", configureDepends) // Java
        }
    }

    private fun addDependenciesAnnotationProcessor(env: ProjectEnv) {
        val project = env.project
        if ((env.hasKotlinPlugin || env.hasKotlinAndroidPlugin) && !project.hasConfig("kapt")) {
            // Note: no-op if kapt plugin was already applied.
            project.plugins.apply("kotlin-kapt")
            if (DEBUG) log("Applied 'kotlin-kapt'.")
        }

        // Note: use plugin version for processor dependency as processor is part of this project.
        val processorDep = "io.objectbox:objectbox-processor:${ProjectEnv.Const.pluginVersion}"
        // Note: check for and use preferred/best config first, potentially ignoring others.
        when {
            project.hasConfig("kapt") -> {
                // Kotlin (Android + Desktop).
                project.addDep("kapt", processorDep)
            }
            project.hasConfig("annotationProcessor") -> {
                // Android (Java), also Java Desktop with Gradle 5.0 (best as of 5.2) uses annotationProcessor.
                project.addDep("annotationProcessor", processorDep)
            }
            project.hasConfig("apt") -> {
                // https://bitbucket.org/hvisser/android-apt or custom apt
                // https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_compile_avoidance
                project.addDep("apt", processorDep)
            }
            else -> {
                project.logger.warn(
                    "ObjectBox: Could not add dependency on objectbox-processor, " +
                            "no supported configuration (kapt, annotationProcessor, apt) found."
                )
            }
        }
    }

    private fun Project.hasConfig(name: String): Boolean {
        return configurations.findByName(name) != null
    }

    private fun Project.addDep(configurationName: String, dep: String) {
        dependencies.add(configurationName, dep)
    }

    internal open fun getNativeLibraryVersionToApply(): String {
        return ProjectEnv.Const.nativeVersionToApply
    }

    private fun addDependencies(env: ProjectEnv) {
        val compileConfig = env.configApiOrCompile
        val project = env.project

        // Note: a preview release might apply different versions of the Java and native library,
        // so explicitly apply the Java library to avoid the native library pulling in another version.
        if (!project.hasObjectBoxDep("objectbox-java")) {
            project.addDep(compileConfig, "io.objectbox:objectbox-java:${ProjectEnv.Const.javaVersionToApply}")
        }

        if (env.hasKotlinPlugin || env.hasKotlinAndroidPlugin) {
            if (DEBUG) log("Kotlin plugin detected")
            if (project.hasObjectBoxDep("objectbox-kotlin")) {
                if (DEBUG) log("Detected objectbox-kotlin dependency, not auto-adding.")
            } else {
                project.addDep(compileConfig, "io.objectbox:objectbox-kotlin:${ProjectEnv.Const.javaVersionToApply}")
            }
        }

        if (env.hasAndroidPlugin) {
            // for this detection to work apply the plugin after the dependencies block
            if (!project.hasObjectBoxDep("objectbox-android") &&
                !project.hasObjectBoxDep("objectbox-android-objectbrowser")
            ) {
                project.addDep(compileConfig, "io.objectbox:objectbox-android:${getNativeLibraryVersionToApply()}")
            }

            // for instrumented unit tests
            // add jsr305 to prevent conflict with other versions added by test dependencies, like espresso
            // https://github.com/objectbox/objectbox-java/issues/73
            project.addDep(env.configAndroidTestImplOrCompile, "com.google.code.findbugs:jsr305:3.0.2")

            // for local unit tests
            addNativeDependency(env, env.configTestImplOrCompile, true)
        } else {
            addNativeDependency(env, compileConfig, false)
        }
    }

    private fun addNativeDependency(env: ProjectEnv, config: String, searchTestConfigs: Boolean) {
        val nativeVersion = getNativeLibraryVersionToApply()
        val project = env.project

        if (DEBUG) log(
            "Detected OS: ${env.osName} is64=${env.is64Bit} " +
                    "isLinux64=${env.isLinux64} isWindows64=${env.isWindows64} isMac64=${env.isMac64}"
        )

        // note: for this detection to work apply the plugin after the dependencies block
        if (project.hasObjectBoxDep("objectbox-linux", searchTestConfigs)
            || project.hasObjectBoxDep("objectbox-windows", searchTestConfigs)
            || project.hasObjectBoxDep("objectbox-macos", searchTestConfigs)
        ) {
            if (DEBUG) log("Detected native dependency, not auto-adding one.")
        } else {
            when {
                env.isLinux64 -> project.addDep(config, "io.objectbox:objectbox-linux:$nativeVersion")
                env.isWindows64 -> project.addDep(config, "io.objectbox:objectbox-windows:$nativeVersion")
                env.isMac64 -> project.addDep(config, "io.objectbox:objectbox-macos:$nativeVersion")
                else -> env.logInfo("Could not set up native dependency for ${env.osName}")
            }
        }
    }

    /**
     * Note: for this detection to work apply the plugin after the dependencies block.
     */
    private fun Project.hasObjectBoxDep(name: String, searchTestConfigs: Boolean = false): Boolean {
        val dependency = findObjectBoxDependency(this, name, searchTestConfigs)
        if (DEBUG) log("$name dependency: $dependency")
        return dependency != null
    }

    private fun findObjectBoxDependency(project: Project, name: String, searchTestConfigs: Boolean): Dependency? {
        if (searchTestConfigs) {
            project.configurations
        } else {
            project.configurations.filterNot { it.name.contains("test", ignoreCase = true) }
        }.forEach { config ->
            config.dependencies.find { it.group == "io.objectbox" && it.name == name }?.let { return it }
        }
        return null
    }

}