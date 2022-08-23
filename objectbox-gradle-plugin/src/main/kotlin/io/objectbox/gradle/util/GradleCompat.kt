package io.objectbox.gradle.util

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.util.GradleVersion

// Note: would move implementations into separate modules, but the gradleApi() dependency version
// is determined by the Gradle version used to build the project.
abstract class GradleCompat {

    companion object {
        private val instance = when {
            GradleVersion.current() >= GradleVersion.version("7.1") -> {
                Gradle71()
            }
            // Using destinationDirectory API (not through this) which requires 6.1.
            // Keep up-to-date with README.
            GradleVersion.current() >= GradleVersion.version("6.1") -> {
                GradleLegacy()
            }
            else -> {
                error("Gradle 6.1 or newer is required.")
            }
        }

        fun get(): GradleCompat {
            return instance
        }
    }

    abstract fun getJavaPluginSourceSets(project: Project): SourceSetContainer
}