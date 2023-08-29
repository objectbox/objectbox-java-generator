package io.objectbox.gradle.util

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer

open class GradleLegacy : GradleCompat() {

    override fun getJavaPluginSourceSets(project: Project): SourceSetContainer {
        // Replaced by JavaPluginExtension, see Gradle71. Scheduled for removal in Gradle 9.0.
        @Suppress("DEPRECATION")
        return project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    }
}