package io.objectbox.gradle.util

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.GradleVersion

abstract class GradleCompat {

    companion object {
        private val instance = if (GradleVersion.current() >= GradleVersion.version("4.9")) {
            Gradle49()
        } else {
            Gradle46()
        }

        fun get(): GradleCompat {
            return instance
        }
    }

    abstract fun registerTask(project: Project, name: String): Any

    abstract fun configureTask(project: Project, name: String, configure: Action<in Task>)

    abstract fun getTask(project: Project, name: String): Any

    abstract fun <T : Task> getTask(project: Project, type: Class<T>, name: String): T
}