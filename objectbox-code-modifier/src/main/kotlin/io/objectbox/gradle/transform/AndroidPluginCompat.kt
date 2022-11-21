package io.objectbox.gradle.transform

import org.gradle.api.Project
import org.gradle.api.provider.Property

abstract class AndroidPluginCompat {

    abstract fun registerTransform(project: Project, debug: Property<Boolean>, hasKotlinPlugin: Boolean)

    /**
     * Returns the Android application ID of the first found build variant of the given project.
     *
     * Must be called after the project is evaluated as the Android plugin can change variants until then.
     */
    abstract fun getFirstApplicationId(project: Project): String?

}