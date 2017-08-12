package io.objectbox.gradle

import io.objectbox.gradle.transform.ObjectBoxAndroidTransform
import io.objectbox.gradle.transform.TransformException
import org.gradle.api.Plugin
import org.gradle.api.Project

class ObjectBoxGradlePlugin : Plugin<Project> {
    companion object {
        const val DEBUG = false
    }

    override fun apply(project: Project) {
        val buildTracker = BuildTracker("GradlePlugin")
        try {
            val env = ProjectEnv(project)
            if (!env.hasAndroidPlugin) {
                // throw RuntimeException("Use the ObjectBox plugin AFTER applying Android plugin")
                project.logger.warn("${project.name}: Use the ObjectBox plugin AFTER applying Android plugin. " +
                        "There is NO TRANSFORM SUPPORT for plain Java/Kotlin projects yet. " +
                        "Without transformations, functionality is limited, e.g. relations are unsupported. ")
            }
            addDependencies(env, project)

            // Cannot use afterEvaluate to register transform, thus our plugin must be applied after Android
            if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isNotEmpty()) {
                ObjectBoxAndroidTransform.Registration.to(project)
            }

            val task = project.task("objectboxVerifySetup")
            if (DEBUG) println("### Created $task in $project")
            var buildTask = project.tasks.findByName("preBuild") ?: project.tasks.getByName("build")
            buildTask.dependsOn(task)
            task.doFirst {
                val taskEnv = ProjectEnv(project) // Now Options are available
                if (DEBUG) println("### Executing $task in $project")
                buildTracker.trackBuild(taskEnv)

                if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isEmpty()) {
                    // TODO check
                }

                val aptConf = project.configurations.findByName("kapt") ?:
                        project.configurations.findByName("annotationProcessor") ?:
                        project.configurations.findByName("apt")
                val foundDependency = aptConf?.dependencies?.firstOrNull() { it.group == "io.objectbox" }
                foundDependency ?:
                        throw RuntimeException("No configuration found with the ObjectBox annotation processor. " +
                                "Currently only Android projects are fully supported.")
            }
        } catch(e: Throwable) {
            if (e is TransformException) buildTracker.trackError("Transform preparation failed", e)
            else buildTracker.trackFatal("Applying plugin failed", e)
            throw e
        }
    }

    private fun addDependencies(env: ProjectEnv, project: Project) {
        val pluginVersion = env.objectBoxVersion
        val runtimeVersion = pluginVersion

        // Does not seem to work with Android projects; project should do that themselves:
        val processorDep = "io.objectbox:objectbox-processor:$pluginVersion"

        val depScope = env.dependencyScopeApiOrCompile
        if (env.hasKotlinPlugin) {
            if (DEBUG) println("### Kotlin plugin detected")

            if (!project.plugins.hasPlugin("kotlin-kapt")) {
                // Does not seem to work reliable; project should do that themselves:
                project.plugins.apply("kotlin-kapt")
                if (DEBUG) println("### Kotlin KAPT plugin added")
            }
            project.dependencies.add(depScope, "io.objectbox:objectbox-kotlin:$runtimeVersion")
            project.dependencies.add("kapt", processorDep)
        } else {
            // Gradle requires custom config!?
            // https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_compile_avoidance
            // Android uses annotationProcessor
            if (project.configurations.findByName("annotationProcessor") != null) {
                project.dependencies.add("annotationProcessor", processorDep)
            } else if (project.configurations.findByName("apt") != null) {
                // https://bitbucket.org/hvisser/android-apt or custom apt
                project.dependencies.add("apt", processorDep)
            }
            if (env.hasAndroidPlugin) {
                project.dependencies.add(depScope, "io.objectbox:objectbox-android:$runtimeVersion")
                project.dependencies.add("androidTestCompile", "com.google.code.findbugs:jsr305:3.0.2")
            } else {
                project.dependencies.add(depScope, "io.objectbox:objectbox-java:$runtimeVersion")
            }
        }
    }

}