package io.objectbox.gradle

import org.gradle.api.Project

/**
 * Gradle plugin extension, which collects all ObjectBox options
 *
 * NOTE Requirements: open because Gradle inherits from it, Project as constructor param.
 */
open class LegacyOptions(val project: Project) {

    /** If detailed log output should be created. */
    var debug: Boolean = false

}