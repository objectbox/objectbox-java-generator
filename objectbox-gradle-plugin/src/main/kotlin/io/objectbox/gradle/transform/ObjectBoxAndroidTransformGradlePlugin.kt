package io.objectbox.gradle.transform

import org.gradle.api.Plugin
import org.gradle.api.Project

class ObjectBoxAndroidTransformGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Cannot use afterEvaluate to register transform, thus out plugin must be applied after Android
        ObjectBoxAndroidTransform.Registration.to(project)
    }

}