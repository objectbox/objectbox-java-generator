package io.objectbox.gradle.transform

import org.gradle.api.Project
import org.gradle.api.provider.Property

abstract class AndroidPluginCompat {

    abstract fun registerTransform(project: Project, debug: Property<Boolean>, hasKotlinPlugin: Boolean)
    
}