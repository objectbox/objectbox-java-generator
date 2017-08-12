package io.objectbox.gradle.transform

import io.objectbox.gradle.BuildTracker
import io.objectbox.gradle.ProjectEnv
import org.gradle.api.Plugin
import org.gradle.api.Project

class ObjectBoxAndroidTransformGradlePlugin : Plugin<Project> {
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
                val env = ProjectEnv(project) // Now Options are available
                if (DEBUG) println("### Executing $task in $project")
                buildTracker.trackBuild(env)

                if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isEmpty()) {
                    // TODO check
                }
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
            // https://bitbucket.org/hvisser/android-apt
            if (project.plugins.hasPlugin("com.neenbedankt.android-apt")) {
                project.dependencies.add("apt", processorDep)
            } else {
                project.dependencies.add("annotationProcessor", processorDep)
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