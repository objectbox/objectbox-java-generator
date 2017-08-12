package io.objectbox.gradle.transform

import io.objectbox.gradle.BuildTracker
import io.objectbox.gradle.ProjectEnv
import org.gradle.api.Plugin
import org.gradle.api.Project

class ObjectBoxAndroidTransformGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val buildTracker = BuildTracker("GradlePlugin")
        try {
            val env = ProjectEnv(project)
            if (!env.hasAndroidPlugin) {
                // throw RuntimeException("Use the ObjectBox plugin AFTER applying Android plugin")
                project.logger.warn("ObjectBox: Use the ObjectBox plugin AFTER applying Android plugin. " +
                        "There is NO TRANSFORM SUPPORT for plain Java/Kotlin projects yet. " +
                        "Without transformations, functionality is limited, e.g. relations are unsupported.")
            }
            addDependencies(env, project)

            // Cannot use afterEvaluate to register transform, thus our plugin must be applied after Android
            if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isNotEmpty()) {
                ObjectBoxAndroidTransform.Registration.to(project)
            }

            val task = project.task("objectboxVerifySetup")
            task.dependsOn(project.configurations.getByName(env.dependencyScopeApiOrCompile))
            task.doFirst {
                buildTracker.trackBuild(env)

                val env = ProjectEnv(project) // Now Options are available
                if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isEmpty()) {
                    // TODO check
                }
            }
        } catch(e: Throwable) {
            if(e is TransformException) buildTracker.trackError("Transform preparation failed", e)
            else buildTracker.trackFatal("Applying plugin failed", e)
            throw e
        }
    }

    private fun addDependencies(env: ProjectEnv, project: Project) {
        val pluginVersion = env.objectBoxVersion
        val runtimeVersion = pluginVersion

        val processorDep = "io.objectbox:objectbox-processor:$pluginVersion"
        val depScope = env.dependencyScopeApiOrCompile
        if (env.hasKotlinPlugin) {
            if(!project.plugins.hasPlugin("kotlin-kapt")) {
                project.plugins.apply("kotlin-kapt")
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
            } else {
                project.dependencies.add(depScope, "io.objectbox:objectbox-java:$runtimeVersion")
            }
        }
    }

}