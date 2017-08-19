package io.objectbox.gradle

import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import io.objectbox.build.ObjectBoxBuildConfig
import io.objectbox.gradle.transform.ObjectBoxAndroidTransform
import io.objectbox.gradle.transform.TransformException
import okio.Buffer
import okio.Okio
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class ObjectBoxGradlePlugin : Plugin<Project> {
    companion object {
        const val DEBUG = false
    }

    val buildTracker = GradleBuildTracker("GradlePlugin")

    override fun apply(project: Project) {
        try {
            val env = ProjectEnv(project)
            if (!env.hasAndroidPlugin) {
                // throw RuntimeException("Use the ObjectBox plugin AFTER applying Android plugin")
                project.logger.warn("${project.name}: Use the ObjectBox plugin AFTER applying Android plugin. " +
                        "There is NO TRANSFORM SUPPORT for plain Java/Kotlin projects yet. " +
                        "Without transformations, functionality is limited, e.g. relations are unsupported. ")
            }
            addDependenciesAnnotationProcessor(env)
            addDependencies(env)

            // Cannot use afterEvaluate to register transform, thus our plugin must be applied after Android
            if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isNotEmpty()) {
                ObjectBoxAndroidTransform.Registration.to(project)
            }

            createPrepareTask(env)
        } catch (e: Throwable) {
            if (e is TransformException) buildTracker.trackError("Transform preparation failed", e)
            else buildTracker.trackFatal("Applying plugin failed", e)
            throw e
        }
    }

    private fun createPrepareTask(env: ProjectEnv) {
        val project = env.project
        val task = project.task("objectboxPrepareBuild")
        if (DEBUG) println("### Created $task in $project")
        val buildTask = project.tasks.findByName("preBuild") ?: project.tasks.getByName("build")
        buildTask.dependsOn(task)
        task.doFirst {
            val taskEnv = ProjectEnv(project) // Now Options are available
            if (DEBUG) println("### Executing $task in $project")
            buildTracker.trackBuild(taskEnv)

//            if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isEmpty()) {
//                // TODO check
//            }

            val aptConf = project.configurations.findByName("kapt") ?:
                    project.configurations.findByName("annotationProcessor") ?:
                    project.configurations.findByName("apt")
            val foundDependency = aptConf?.dependencies?.firstOrNull() { it.group == "io.objectbox" }
            if (foundDependency == null) {
                var msg = "No ObjectBox annotation processor configuration found. Please check your build scripts."
                if (!env.hasAndroidPlugin) msg += "Currently only Android projects are fully supported."
                throw RuntimeException(msg)
            }

            writeBuildConfig(env)
        }
    }

    private fun writeBuildConfig(env: ProjectEnv) {
        val file = File(env.project.buildDir, ObjectBoxBuildConfig.FILE_NAME)
        val options = ObjectBoxBuildConfig(env.project.projectDir.absolutePath)
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
        val processorDep = "io.objectbox:objectbox-processor:${env.objectBoxVersion}"
        val project = env.project
        if (project.configurations.findByName("kapt") != null) {
            project.dependencies.add("kapt", processorDep)
        } else if (project.configurations.findByName("annotationProcessor") != null) {
            // Android uses annotationProcessor
            project.dependencies.add("annotationProcessor", processorDep)
        } else if (project.configurations.findByName("apt") != null) {
            // https://bitbucket.org/hvisser/android-apt or custom apt
            // https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_compile_avoidance
            project.dependencies.add("apt", processorDep)
        } else if (env.hasKotlinPlugin) {
            if (!project.plugins.hasPlugin("kotlin-kapt")) {
                // Does not seem to work reliable; project should do that themselves:
                project.plugins.apply("kotlin-kapt")
                project.dependencies.add("kapt", processorDep)
                if (DEBUG) println("### Kotlin KAPT plugin added")
            }
        }
    }

    private fun addDependencies(env: ProjectEnv) {
        val pluginVersion = env.objectBoxVersion
        val runtimeVersion = pluginVersion
        val depScope = env.dependencyScopeApiOrCompile
        val project = env.project
        if (env.hasKotlinPlugin) {
            if (DEBUG) println("### Kotlin plugin detected")
            project.dependencies.add(depScope, "io.objectbox:objectbox-kotlin:$runtimeVersion")
        }

        if (env.hasAndroidPlugin) {
            project.dependencies.add(depScope, "io.objectbox:objectbox-android:$runtimeVersion")
            project.dependencies.add("androidTestCompile", "com.google.code.findbugs:jsr305:3.0.2")
        } else {
            project.dependencies.add(depScope, "io.objectbox:objectbox-java:$runtimeVersion")
        }
    }

}