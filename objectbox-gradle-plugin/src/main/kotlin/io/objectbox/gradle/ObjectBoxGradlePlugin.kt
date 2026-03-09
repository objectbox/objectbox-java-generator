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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.api.plugins.JavaPlugin
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

    /**
     * Configure the annotation processor.
     *
     * Note that this can't happen in [addDependencies] because it would be too late.
     */
    private fun addDependenciesAnnotationProcessor(env: ProjectEnv) {
        val project = env.project
        if ((env.hasKotlinPlugin || env.hasKotlinAndroidPlugin) &&
            !project.hasConfig(ProjectEnv.Const.KAPT_CONFIGURATION_NAME)
        ) {
            // Note: no-op if kapt plugin was already applied.
            project.plugins.apply("kotlin-kapt")
            env.logDebug { "Applied 'kotlin-kapt'." }
        }

        // Note: use plugin version for processor dependency as processor is part of this project.
        val processorDep = "io.objectbox:objectbox-processor:${ProjectEnv.Const.pluginVersion}"
        // Note: check for and use preferred/best config first, potentially ignoring others.
        when {
            project.hasConfig(ProjectEnv.Const.KAPT_CONFIGURATION_NAME) -> {
                // Kotlin (Android + Desktop).
                project.addDep(ProjectEnv.Const.KAPT_CONFIGURATION_NAME, processorDep)
            }

            project.hasConfig(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME) -> {
                // Android (Java), also Java Desktop with Gradle 5.0 (best as of 5.2) uses annotationProcessor.
                project.addDep(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, processorDep)
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

    private fun Project.addDepLater(dependencySet: DependencySet, dep: String) {
        dependencySet.addLater(
            provider {
                dependencies.create(dep)
            }
        )
    }

    /**
     * Before dependencies of project configurations are resolved, adds required ObjectBox dependencies, if a
     * conflicting one isn't added already:
     *
     * - Java APIs (objectbox-java)
     * - Kotlin APIs (objectbox-kotlin), if the (Android) Kotlin plugin is applied
     * - database library for Android or JVM, depending on if the Android plugin is applied
     * - Findbugs JSR305 nullable annotations, if the Android plugin is applied, only for instrumented unit tests
     */
    private fun addDependencies(env: ProjectEnv) {
        val compileConfig = env.configApiOrImpl
        val project = env.project

        // Use Configuration.withDependencies to also detect dependencies that are added after the plugin is applied
        // (which, if using modern Gradle plugins syntax, they are always).
        project.configurations.getByName(compileConfig).withDependencies { dependencySet ->
            val hasKotlinPlugin = env.hasKotlinPlugin || env.hasKotlinAndroidPlugin
            val hasObxKotlinLibrary = env.hasObjectBoxDep("objectbox-kotlin")

            if (hasKotlinPlugin) {
                if (hasObxKotlinLibrary) {
                    env.logDebug { "Not adding objectbox-kotlin dependency, a configuration has one" }
                } else {
                    project.addDepLater(
                        dependencySet,
                        "io.objectbox:objectbox-kotlin:${ProjectEnv.Const.javaVersionToApply}"
                    )
                }
            }

            // Note: a preview release of the plugin might apply different versions of the Java and database library,
            // so always add the Java library to avoid the Android database library pulling in an older Java library
            // (only the Android library has a dependency on the Java library as it includes Java APIs).
            // But don't add it if the Kotlin library is manually added as it has a dependency on the Java library to
            // avoid pulling in a newer, possibly incompatible, Java library.
            if (env.hasObjectBoxDep("objectbox-java")) {
                env.logDebug { "Not adding objectbox-java dependency, a configuration has one" }
            } else if (hasObxKotlinLibrary) {
                env.logDebug { "Not adding objectbox-java dependency, a configuration has objectbox-kotlin" }
            } else {
                project.addDepLater(dependencySet, "io.objectbox:objectbox-java:${ProjectEnv.Const.javaVersionToApply}")
            }

            // If the Android plugin is applied, add the Android database library, otherwise the JVM database library
            if (env.hasAndroidPlugin) {
                if (!env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_DEFAULT-android")
                    && !env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_DEFAULT-android-objectbrowser")
                    && !env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-android")
                    && !env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-android-objectbrowser")
                    && !env.hasObjectBoxDep("$LIBRARY_NAME_PREFIX_SYNC-server-android")
                ) {
                    project.addDepLater(
                        dependencySet,
                        "io.objectbox:${getLibWithSyncVariantPrefix()}-android:${getLibWithSyncVariantVersion()}"
                    )
                }
            } else {
                addNativeDependency(env, dependencySet, searchTestConfigs = false)
            }
        }

        if (env.hasAndroidPlugin) {
            // For Android local (on developer machine) unit tests add a dependency on the JVM database library
            project.configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
                .withDependencies { dependencySet ->
                    addNativeDependency(env, dependencySet, searchTestConfigs = true)
                }

            // For Android instrumented (on device/emulator) unit tests
            // add jsr305 to prevent conflict with other versions added by test dependencies, like espresso.
            // https://github.com/objectbox/objectbox-java/issues/73
            project.configurations
                .getByName(ProjectEnv.Const.ANDROID_TEST_IMPLEMENTATION_CONFIGURATION_NAME)
                .dependencies
                .let { project.addDepLater(it, "com.google.code.findbugs:jsr305:3.0.2") }
        }
    }

    private fun addNativeDependency(env: ProjectEnv, dependencySet: DependencySet, searchTestConfigs: Boolean) {
        env.logDebug {
            "Detected OS: ${env.osName} is64=${env.is64Bit} " +
                    "isLinux64=${env.isLinux64} isWindows64=${env.isWindows64} isMac64=${env.isMac64}"
        }

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
                env.project.addDepLater(dependencySet, "io.objectbox:$prefix-$suffix:$version")
            } else {
                env.logInfo("Could not set up native dependency for ${env.osName}")
            }
        }
    }

    /**
     * Checks for exact name match. Set [startsWith] to true to only check for prefix.
     *
     * Note: for this detection to work for dependencies added after the plugin is applied, must be called within
     * [org.gradle.api.artifacts.Configuration.withDependencies].
     */
    private fun ProjectEnv.hasObjectBoxDep(
        name: String,
        searchTestConfigs: Boolean = false,
        startsWith: Boolean = false
    ): Boolean {
        val (config, dependency) = findObjectBoxDependency(project, name, searchTestConfigs, startsWith)
            ?: return false
        logDebug { "$name dependency on $config: $dependency" }
        return true
    }

    private fun findObjectBoxDependency(
        project: Project,
        name: String,
        searchTestConfigs: Boolean,
        startsWith: Boolean
    ): Pair<Configuration, Dependency>? {
        if (searchTestConfigs) {
            project.configurations
        } else {
            project.configurations.filterNot { it.name.contains("test", ignoreCase = true) }
        }.forEach { config ->
            config.dependencies.find {
                it.group == "io.objectbox" && (if (startsWith) it.name.startsWith(name) else it.name == name)
            }?.let { return config to it }
        }
        return null
    }

    companion object {
        const val LIBRARY_NAME_PREFIX_DEFAULT = "objectbox"
        const val LIBRARY_NAME_PREFIX_SYNC = "objectbox-sync"
    }

}