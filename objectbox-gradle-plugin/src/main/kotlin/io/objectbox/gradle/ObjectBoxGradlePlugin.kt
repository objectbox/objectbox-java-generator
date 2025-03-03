/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
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

package io.objectbox.gradle

import io.objectbox.gradle.transform.AndroidPluginCompat
import io.objectbox.gradle.transform.ObjectBoxJavaClassesTransformTask
import io.objectbox.gradle.transform.ObjectBoxJavaTransform
import io.objectbox.gradle.transform.TransformException
import io.objectbox.gradle.util.AndroidCompat
import io.objectbox.gradle.util.GradleCompat
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.api.tasks.compile.JavaCompile

/**
 * A Gradle plugin that depending on the other plugins/dependencies of a project it is applied to
 * - adds dependencies for the ObjectBox annotation processor,
 * - adds dependencies for the ObjectBox Java, Kotlin and native (Android, Linux, Windows, Mac) libraries,
 * - for Android projects, configures [AndroidPluginCompat],
 * - for Java projects, adds a [ObjectBoxJavaTransform] task that runs after the compile task.
 * - adds a [PrepareTask] that runs as part of the build task.
 */
open class ObjectBoxGradlePlugin : Plugin<Project> {

    /**
     * The Gradle plugin id as registered in resources/META-INF/gradle-plugins.
     */
    internal open val pluginId = "io.objectbox"

    private val buildTracker = GradleBuildTracker("GradlePlugin")

    override fun apply(project: Project) {
        // Trigger Gradle version check.
        GradleCompat.get()

        try {
            val env = ProjectEnv(project)
            // Note: do not check for just a Kotlin plugin. Currently, the Kotlin Android and Kotlin JVM
            // plugins ensure the Java plugin is applied, but they might not in the future. However, the
            // ObjectBox Java library requires Java support.
            if (!(env.hasAndroidPlugin || env.hasJavaPlugin)) {
                throw InvalidPluginException(
                    "'$pluginId' can only be applied to a project if one of the following is applied before:\n" +
                            "\t* an Android plugin\n" +
                            "\t* the Kotlin Android or JVM plugin\n" +
                            "\t* the Java Library, Java Application or Java plugin\n"
                )
            }
            addDependenciesAnnotationProcessor(env)
            addDependencies(env)

            // ensure Android plugin API is available
            if (env.hasAndroidPlugin) {
                // Cannot use afterEvaluate to register Android transform, thus our plugin must be applied after Android
                AndroidCompat.getPlugin(project).registerTransform(project, env.options.debug, env.hasKotlinPlugin)
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
        val project = env.project
        val sourceSets = GradleCompat.get().getJavaPluginSourceSets(project)
        // Use all so SourceSets defined in build configs available only after evaluation are included.
        sourceSets.all { sourceSet ->
            // name task based on SourceSet
            val taskName = sourceSet.getTaskName("transform", "objectBoxClasses")

            // Add compiled Java project sources, makes Java compile task a dependency.
            val compileJavaTaskOutputDir = project.tasks.withType(JavaCompile::class.java)
                .named(sourceSet.compileJavaTaskName).map { it.destinationDirectory }
            val inputClasspath = project.files(compileJavaTaskOutputDir)

            // Use register to defer creation until use.
            val transformTask = project.tasks.register(
                taskName,
                ObjectBoxJavaClassesTransformTask::class.java,
                ObjectBoxJavaClassesTransformTask.ConfigAction(env.options.debug, inputClasspath)
            )

            // Verify classes and compileJava task exist, attach to lifecycle
            // assumes that classes task depends on compileJava depends on compileKotlin.
            val classesTaskName = sourceSet.classesTaskName
            try {
                project.tasks.named(sourceSet.classesTaskName).configure {
                    it.dependsOn(transformTask)
                }
            } catch (e: UnknownDomainObjectException) {
                throw RuntimeException("Could not find classes task '$classesTaskName'.", e)
            }

            env.logDebug { "Added $taskName task, depends on $classesTaskName task." }
        }
    }

    private fun createPrepareTask(env: ProjectEnv) {
        val project = env.project

        // use register to defer creation until use
        val prepareTaskName = "objectboxPrepareBuild"

        val prepareTask = project.tasks.register(prepareTaskName, PrepareTask::class.java, env, buildTracker)
        env.logDebug { "Registered $prepareTaskName task." }

        // make build task depend on prepare task
        val configureDepends = Action<Task> { it.dependsOn(prepareTask) }
        try {
            project.tasks.named("preBuild").configure(configureDepends) // Android
        } catch (e: Exception) {
            project.tasks.named("build").configure(configureDepends) // Java
        }
    }

    private fun addDependenciesAnnotationProcessor(env: ProjectEnv) {
        val project = env.project
        if ((env.hasKotlinPlugin || env.hasKotlinAndroidPlugin) && !project.hasConfig("kapt")) {
            // Note: no-op if kapt plugin was already applied.
            project.plugins.apply("kotlin-kapt")
            env.logDebug { "Applied 'kotlin-kapt'." }
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

    /**
     * Prefix for libraries that have Sync enabled versions.
     */
    internal open fun getLibWithSyncVariantPrefix(): String {
        // Use non-Sync version.
        return LIBRARY_NAME_PREFIX_DEFAULT
    }

    /**
     * Version for libraries that have Sync enabled versions.
     * All others always use [ProjectEnv.Const.nativeVersionToApply].
     */
    internal open fun getLibWithSyncVariantVersion(): String {
        return ProjectEnv.Const.nativeVersionToApply
    }

    private fun addDependencies(env: ProjectEnv) {
        val compileConfig = env.configApiOrImplOrCompile
        val project = env.project

        // Note: a preview release might apply different versions of the Java and native library,
        // so explicitly apply the Java library to avoid the native library pulling in another version.
        if (!env.hasObjectBoxDep("objectbox-java")) {
            project.addDep(compileConfig, "io.objectbox:objectbox-java:${ProjectEnv.Const.javaVersionToApply}")
        }

        if (env.hasKotlinPlugin || env.hasKotlinAndroidPlugin) {
            env.logDebug { "Kotlin plugin detected" }
            if (env.hasObjectBoxDep("objectbox-kotlin")) {
                env.logDebug { "Detected objectbox-kotlin dependency, not auto-adding." }
            } else {
                project.addDep(compileConfig, "io.objectbox:objectbox-kotlin:${ProjectEnv.Const.javaVersionToApply}")
            }
        }

        if (env.hasAndroidPlugin) {
            // for this detection to work apply the plugin after the dependencies block
            if (!env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_DEFAULT-android")
                && !env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_DEFAULT-android-objectbrowser")
                && !env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-android")
                && !env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-android-objectbrowser")
                && !env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-server-android")
            ) {
                project.addDep(
                    compileConfig,
                    "io.objectbox:${getLibWithSyncVariantPrefix()}-android:${getLibWithSyncVariantVersion()}"
                )
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
        val project = env.project

        env.logDebug {
            "Detected OS: ${env.osName} is64=${env.is64Bit} " +
                    "isLinux64=${env.isLinux64} isWindows64=${env.isWindows64} isMac64=${env.isMac64}"
        }

        // note: for this detection to work apply the plugin after the dependencies block
        // Note: use startsWith to detect e.g. -armv7 and -arm64 and any possible future suffixes.
        if (env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_DEFAULT-linux", searchTestConfigs, startsWith = true)
            || env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_DEFAULT-macos", searchTestConfigs, startsWith = true)
            || env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_DEFAULT-windows", searchTestConfigs, startsWith = true)
            || env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-linux", searchTestConfigs, startsWith = true)
            || env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-server-linux", searchTestConfigs, startsWith = true)
            || env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-macos", searchTestConfigs, startsWith = true)
            || env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-windows", searchTestConfigs, startsWith = true)
        ) {
            env.logDebug { "Detected native dependency, not auto-adding one." }
        } else {
            // Note: -armv7 and -arm64 variants of the Linux library are not added automatically,
            // users are expected to do so themselves if needed.
            val suffix = when {
                env.isLinux64 -> "linux"
                env.isWindows64 -> "windows"
                env.isMac64 -> "macos"
                else -> null
            }
            if (suffix != null) {
                val prefix = getLibWithSyncVariantPrefix()
                val version = getLibWithSyncVariantVersion()
                project.addDep(config, "io.objectbox:$prefix-$suffix:$version")
            } else {
                env.logInfo("Could not set up native dependency for ${env.osName}")
            }
        }
    }

    /**
     * Checks for exact name match. Set [startsWith] to true to only check for prefix.
     *
     * Note: for this detection to work the plugin must be applied after the dependencies block.
     */
    private fun ProjectEnv.hasObjectBoxDep(
        name: String,
        searchTestConfigs: Boolean = false,
        startsWith: Boolean = false
    ): Boolean {
        val dependency = findObjectBoxDependency(project, name, searchTestConfigs, startsWith)
        logDebug { "$name dependency: $dependency" }
        return dependency != null
    }

    private fun findObjectBoxDependency(
        project: Project,
        name: String,
        searchTestConfigs: Boolean,
        startsWith: Boolean
    ): Dependency? {
        if (searchTestConfigs) {
            project.configurations
        } else {
            project.configurations.filterNot { it.name.contains("test", ignoreCase = true) }
        }.forEach { config ->
            config.dependencies.find {
                it.group == "io.objectbox" && (if (startsWith) it.name.startsWith(name) else it.name == name)
            }?.let { return it }
        }
        return null
    }

    companion object {
        const val LIBRARY_NAME_PREFIX_DEFAULT = "objectbox"
        const val LIBRARY_NAME_PREFIX_SYNC = "objectbox-sync"
    }

}