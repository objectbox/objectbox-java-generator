package io.objectbox.gradle.util

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.AbstractCompile

/**
 * Gradle 4.9 introduces API to avoid task configuration to speed up project configuration.
 * Instead of the actual Task a TaskProvider is returned.
 * Note: the Task will be created if get() is called on the provider, so avoid it.
 *
 * https://docs.gradle.org/current/userguide/task_configuration_avoidance.html
 */
class Gradle49 : GradleCompat() {
    override fun registerTask(project: Project, name: String): Any {
        return project.tasks.register(name)
    }

    override fun configureTask(project: Project, name: String, configure: Action<in Task>) {
        project.tasks.named(name).configure(configure)
    }

    override fun getTask(project: Project, name: String): Any {
        return project.tasks.named(name)
    }

    override fun <T : Task> getTask(project: Project, type: Class<T>, name: String): T {
        return project.tasks.withType(type).named(name).get()
    }
}