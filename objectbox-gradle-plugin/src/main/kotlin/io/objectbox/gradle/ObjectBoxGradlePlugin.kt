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

import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import io.objectbox.build.ObjectBoxBuildConfig
import io.objectbox.gradle.transform.ObjectBoxAndroidTransform
import io.objectbox.gradle.transform.ObjectBoxJavaTransform
import io.objectbox.gradle.transform.TransformException
import okio.Buffer
import okio.Okio
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

class ObjectBoxGradlePlugin : Plugin<Project> {
    companion object {
        const val DEBUG = false
    }

    val buildTracker = GradleBuildTracker("GradlePlugin")

    override fun apply(project: Project) {
        try {
            val env = ProjectEnv(project)
            if (!(env.hasAndroidPlugin || env.hasJavaPlugin || env.hasKotlinPlugin)) {
                project.logger.warn("${project.name}: No Android, Java or Kotlin plugin detected, " +
                        "apply the ObjectBox plugin AFTER those plugins. ")
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
            else buildTracker.trackFatal("Applying plugin failed", e)
            throw e
        }
    }

    private fun createPlainJavaTransformTask(env: ProjectEnv) {
        // wait until after project evaluation so SourceSets defined in build configs are included
        val project = env.project
        project.afterEvaluate {
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            javaPlugin.sourceSets.forEach { sourceSet ->
                // name task based on SourceSet
                val sourceSetName = sourceSet.name
                val taskName = "objectboxJavaTransform" + if (sourceSetName != "main") sourceSetName.capitalize() else ""

                val task = project.task(taskName)
                task.group = "objectbox"
                task.description = "Transforms Java bytecode"
                if (env.debug) println("### Created $task in $project")

                // attach to lifecycle
                // assumes that classes task depends on compileJava depends on compileKotlin
                val classesTask = project.tasks.findByName(sourceSet.classesTaskName) ?:
                        throw RuntimeException("Could not find classes task '${sourceSet.classesTaskName}'.")
                val compileJavaTask = project.tasks.findByName(sourceSet.compileJavaTaskName) as JavaCompile? ?:
                        throw RuntimeException("Could not find compileJava task '${sourceSet.compileJavaTaskName}'.")

                classesTask.dependsOn(task)
                task.mustRunAfter(compileJavaTask)

                task.doLast {
                    if (env.debug) println("### Executing $task in $project")

                    val compileJavaTaskOutputDir = compileJavaTask.destinationDir
                    ObjectBoxJavaTransform(env.debug).transform(listOf(compileJavaTaskOutputDir))
                }
            }
        }
    }

    private fun createPrepareTask(env: ProjectEnv) {
        val project = env.project
        val task = project.task("objectboxPrepareBuild")
        task.group = "objectbox"
        if (DEBUG) println("### Created $task in $project")
        val buildTask = project.tasks.findByName("preBuild") ?: project.tasks.getByName("build")
        buildTask.dependsOn(task)
        task.doFirst {
            if (env.debug) println("### Executing $task in $project")
            buildTracker.trackBuild(env)

//            if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isEmpty()) {
//                // TODO check
//            }

            val aptConf = project.configurations.findByName("kapt") ?:
                    project.configurations.findByName("annotationProcessor") ?:
                    project.configurations.findByName("apt")
            val foundDependency = aptConf?.dependencies?.firstOrNull { dep -> dep.group == "io.objectbox" }
            if (foundDependency == null) {
                var msg = "No ObjectBox annotation processor configuration found. Please check your build scripts."
                if (!env.hasAndroidPlugin) msg += "Currently only Android projects are fully supported."
                throw RuntimeException(msg)
            }

            writeBuildConfig(env)
        }
    }

    private fun writeBuildConfig(env: ProjectEnv) {
        val buildDir = env.project.buildDir
        if (!buildDir.exists()) buildDir.mkdirs()
        val file = File(buildDir, ObjectBoxBuildConfig.FILE_NAME)
        var flavor: String? = null
//        val extClass = ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(env.project).singleOrNull()
//        if (extClass != null) {
//            val ext = env.project.extensions.getByType(extClass) as BaseExtension
//            flavor = ext?.defaultConfig?.dimension
//        }
        val options = ObjectBoxBuildConfig(env.project.projectDir.absolutePath, flavor)
        val adapter = Moshi.Builder().build().adapter<ObjectBoxBuildConfig>(ObjectBoxBuildConfig::class.java)
        val buffer = Buffer()
        val jsonWriter = JsonWriter.of(buffer)
        jsonWriter.indent = "  "
        adapter.toJson(jsonWriter, options)
        val sink = Okio.sink(file)
        sink.use {
            buffer.readAll(it)
        }
    }

    private fun addDependenciesAnnotationProcessor(env: ProjectEnv) {
        // Does not seem to work with Android projects; project should do that themselves:
        val processorDep = "io.objectbox:objectbox-processor:${ProjectEnv.Const.pluginVersion}"
        val project = env.project
        if (project.hasConfig("kapt")) {
            project.addDep("kapt", processorDep)
        } else if (project.hasConfig("annotationProcessor")) {
            // Android uses annotationProcessor
            project.addDep("annotationProcessor", processorDep)
        } else if (project.hasConfig("apt")) {
            // https://bitbucket.org/hvisser/android-apt or custom apt
            // https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_compile_avoidance
            project.addDep("apt", processorDep)
        } else if (env.hasKotlinPlugin) {
            if (!project.plugins.hasPlugin("kotlin-kapt")) {
                // Does not seem to work reliable; project should do that themselves:
                project.plugins.apply("kotlin-kapt")
                project.addDep("kapt", processorDep)
                if (DEBUG) println("### Kotlin KAPT plugin added")
            }
        }
    }

    private fun Project.hasConfig(name: String): Boolean {
        return configurations.findByName(name) != null
    }

    private fun Project.addDep(configurationName: String, dep: String) {
        dependencies.add(configurationName, dep)
    }

    private fun addDependencies(env: ProjectEnv) {
        val runtimeVersion = ProjectEnv.Const.runtimeVersion
        val compileConfig = env.configApiOrCompile
        val project = env.project

        if (env.hasKotlinPlugin) {
            if (DEBUG) println("### Kotlin plugin detected")
            project.addDep(compileConfig, "io.objectbox:objectbox-kotlin:$runtimeVersion")
        }

        if (env.hasAndroidPlugin) {
            // for this detection to work apply the plugin after the dependencies block
            if (!project.hasObjectBoxDep("objectbox-android") &&
                    !project.hasObjectBoxDep("objectbox-android-objectbrowser")) {
                project.addDep(compileConfig, "io.objectbox:objectbox-android:$runtimeVersion")
            }

            // for instrumented unit tests
            // add jsr305 to prevent conflict with other versions added by test dependencies, like espresso
            // https://github.com/objectbox/objectbox-java/issues/73
            project.addDep(env.configAndroidTestImplOrCompile, "com.google.code.findbugs:jsr305:3.0.2")

            // for local unit tests
            addNativeDependency(env, env.configTestImplOrCompile)
        } else {
            project.addDep(compileConfig, "io.objectbox:objectbox-java:$runtimeVersion")
            addNativeDependency(env, compileConfig)
        }
    }

    private fun addNativeDependency(env: ProjectEnv, config: String) {
        val runtimeVersion = ProjectEnv.Const.runtimeVersion
        val project = env.project

        if (DEBUG) println("### Detected OS: ${env.osName} is64=${env.is64Bit} " +
                "isLinux64=${env.isLinux64} isWindows64=${env.isWindows64} isMac64=${env.isMac64}")

        // note: for this detection to work apply the plugin after the dependencies block
        if (project.hasObjectBoxDep("objectbox-linux")
                || project.hasObjectBoxDep("objectbox-windows")
                || project.hasObjectBoxDep("objectbox-macos")) {
            if (DEBUG) println("### Detected native dependency, not auto-adding one.")
        } else {
            when {
                env.isLinux64 -> project.addDep(config, "io.objectbox:objectbox-linux:$runtimeVersion")
                env.isWindows64 -> project.addDep(config, "io.objectbox:objectbox-windows:$runtimeVersion")
                env.isMac64 -> project.addDep(config, "io.objectbox:objectbox-macos:$runtimeVersion")
                else -> env.logInfo("Could not set up native dependency for ${env.osName}")
            }
        }
    }

    /**
     * Note: for this detection to work apply this plugin after the dependencies block.
     */
    private fun Project.hasObjectBoxDep(name: String): Boolean {
        val dependency = findObjectBoxDependency(this, name)
        if (DEBUG) println("### $name dependency: $dependency")
        return dependency != null
    }

    private fun findObjectBoxDependency(project: Project, name: String): Dependency? {
        project.configurations.asMap.values
                .filterNot { it.name.contains("test", ignoreCase = true) }
                .forEach { config ->
                    val dependency = config.dependencies.find({ it.group == "io.objectbox" && it.name == name })
                    if (dependency != null) {
                        return dependency
                    }
                }
        return null
    }

}