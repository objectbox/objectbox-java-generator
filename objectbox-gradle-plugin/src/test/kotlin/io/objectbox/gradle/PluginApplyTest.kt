package io.objectbox.gradle

import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException


/**
 * Tests applying plugin configures project as expected.
 */
class PluginApplyTest {

    @Rule
    @JvmField
    val exception: ExpectedException = ExpectedException.none()

    @Test
    fun apply_noRequiredPlugins_fails() {
        exception.expect(PluginApplicationException::class.java)
        exception.expectMessage("Failed to apply plugin [id 'io.objectbox']")

        val project = ProjectBuilder.builder().build()
        project.project.pluginManager.apply("io.objectbox")
    }

    @Test
    fun apply_beforeJavaPlugin_fails() {
        exception.expect(PluginApplicationException::class.java)
        exception.expectMessage("Failed to apply plugin [id 'io.objectbox']")

        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("io.objectbox")
            apply("java")
        }
    }

    @Test
    fun apply_afterJavaPlugin_addsDependenciesAndTasks() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("java")
            apply("io.objectbox")
        }

        with(project.configurations) {
            val apDeps = getByName("annotationProcessor").dependencies
            assertEquals(1, apDeps.count { it.group == "io.objectbox" && it.name == "objectbox-processor" })

            val compileDeps = getByName("compile").dependencies
            assertEquals(1, compileDeps.count { it.group == "io.objectbox" && it.name == "objectbox-java" })
            assertEquals(1, compileDeps.count {
                it.group == "io.objectbox" &&
                        (it.name == "objectbox-linux" || it.name == "objectbox-windows" || it.name == "objectbox-macos")
            })
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        // Note: using the internal evaluate is not nice, but beats writing a full-blown integration test.
        (project as ProjectInternal).evaluate()

        // AFTER EVALUATE.
        // Note: by default only main and test source sets exist.
        assertTransformTask(project, "", "classes")
        assertTransformTask(project, "Test", "testClasses")
    }

    private fun assertTransformTask(
        project: Project,
        sourceSetSuffix: String,
        classesTaskName: String
    ) {
        // Created.
        val transformTask = project.tasks.findByPath("objectboxJavaTransform$sourceSetSuffix")
        assertNotNull(transformTask)

        // Must run after compile task.
        assertEquals(1, transformTask!!
            .mustRunAfter.getDependencies(transformTask).count { it.name == "compile${sourceSetSuffix}Java" })

        // Classes task should depend on it.
        val classesTask = project.tasks.getByName(classesTaskName)
        assertEquals(1, classesTask
            .taskDependencies.getDependencies(classesTask).count { it.name == transformTask.name })
    }
}