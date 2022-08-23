package io.objectbox.gradle.util

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer


/**
 * Gradle 7.1 introduces API to get source sets from the
 * [Java plugin extension](https://docs.gradle.org/current/userguide/java_plugin.html#sec:java-extension).
 */
class Gradle71 : GradleCompat() {

    override fun getJavaPluginSourceSets(project: Project): SourceSetContainer {
        val javaExtension = project.extensions.findByType(JavaPluginExtension::class.java)
            ?: error("The Java plugin extension was not found.")
        return javaExtension.sourceSets
    }

}