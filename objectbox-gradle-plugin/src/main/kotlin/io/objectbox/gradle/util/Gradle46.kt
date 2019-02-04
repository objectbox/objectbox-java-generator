package io.objectbox.gradle.util

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Might actually work back to Gradle 3.10, but the minimum supported Android plugin
 * version is 3.0.0 which requires Gradle 4.6.
 */
class Gradle46 : GradleCompat() {
    override fun registerTask(project: Project, name: String): Any {
        return project.tasks.create(name)
    }

    override fun configureTask(project: Project, name: String, configure: Action<in Task>) {
        project.tasks.getByName(name, configure)
    }

    override fun getTask(project: Project, name: String): Any {
        return project.tasks.getByName(name)
    }

    override fun <T : Task> getTask(project: Project, type: Class<T>, name: String): T {
        return project.tasks.withType(type).getByName(name)
    }
}