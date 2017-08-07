package io.objectbox.gradle.transform

import io.objectbox.gradle.ProjectEnv
import org.gradle.api.Plugin
import org.gradle.api.Project

class ObjectBoxAndroidTransformGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val env = ProjectEnv(project)
        if (!env.hasAndroidPlugin) {
            // throw RuntimeException("Use the ObjectBox plugin AFTER applying Android plugin")
            project.logger.warn("ObjectBox: Use the ObjectBox plugin AFTER applying Android plugin." +
                    "NO TRANSFORM SUPPORT for plain Java/Kotlin projects yet. " +
                    "Limited support only!! Especially relations are NOT supported.")
        }
        val pluginVersion = env.objectBoxVersion
        val runtimeVersion = pluginVersion

        val processorDep = "io.objectbox:objectbox-processor:$pluginVersion"
        val depScope = env.dependencyScopeApiOrCompile
        if (env.hasKotlinPlugin) {
            project.plugins.apply("kotlin-kapt")
            project.dependencies.add(depScope, "io.objectbox:objectbox-kotlin:$runtimeVersion")
            project.dependencies.add("kapt", processorDep)
        } else {
            project.dependencies.add("apt", processorDep)
            if(env.hasAndroidPlugin) {
                project.dependencies.add(depScope, "io.objectbox:objectbox-android:$runtimeVersion")
            } else {
                project.dependencies.add(depScope, "io.objectbox:objectbox-java:$runtimeVersion")
            }
        }

        // Cannot use afterEvaluate to register transform, thus our plugin must be applied after Android
        ObjectBoxAndroidTransform.Registration.to(project)
    }

}