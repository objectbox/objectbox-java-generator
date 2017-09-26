package io.objectbox.gradle

import org.gradle.api.Project

/**
 * Gradle plugin extension, which collects options for the plugin. Separate from annotation processor options!
 *
 * NOTE Requirements: open because Gradle inherits from it, Project as constructor param.
 */
open class PluginOptions(@Suppress("unused") val project: Project) {

    /** If detailed log output should be created. */
    var debug: Boolean = false

}