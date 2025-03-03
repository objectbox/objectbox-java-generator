/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2025 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test


/**
 * Tests applying [ObjectBoxGradlePlugin] configures a Java or Kotlin desktop Gradle project as expected.
 */
open class PluginApplyJavaTest : PluginApplyTest() {

    @Test
    fun apply_noRequiredPlugins_fails() {
        val project = ProjectBuilder.builder().build()
        assertThrows(PluginApplicationException::class.java) {
            project.project.pluginManager.apply(pluginId)
        }.also {
            assertEquals("Failed to apply plugin '$pluginId'.", it.message)
            assertThat(it.cause, instanceOf(InvalidPluginException::class.java))
        }
    }

    @Test
    fun apply_beforeJavaPlugin_fails() {
        assertApplyBeforePluginFails("java")
    }

    @Test
    fun apply_beforeApplicationPlugin_fails() {
        assertApplyBeforePluginFails("application")
    }

    @Test
    fun apply_beforeJavaLibraryPlugin_fails() {
        assertApplyBeforePluginFails("java-library")
    }

    private fun assertApplyBeforePluginFails(plugin: String) {
        val project = ProjectBuilder.builder().build()
        assertThrows(PluginApplicationException::class.java) {
            project.pluginManager.apply {
                apply(pluginId)
                apply(plugin)
            }
        }.also {
            assertEquals("Failed to apply plugin '$pluginId'.", it.message)
        }
    }

    @Test
    fun apply_afterJavaPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("java")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertJavaProject(project, "implementation")
    }

    @Test
    fun apply_afterApplicationPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("application") // Note: application plugin adds java plugin.
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertJavaProject(project, "implementation")
    }

    @Test
    fun apply_afterJavaLibraryPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("java-library")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertJavaProject(project, "api")
    }

    private fun assertJavaProject(project: Project, configuration: String) {
        with(project.configurations) {
            assertProcessorDependency(getByName("annotationProcessor").dependencies)

            getByName(configuration).dependencies.let {
                assertJavaDependency(it)
                assertNativeDependency(it)
            }
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
        // Is created.
        val transformTask = project.tasks.findByPath("transform${sourceSetSuffix}ObjectBoxClasses")
        assertNotNull(transformTask)

        // Depends on compile task of source set.
        assertEquals(
            1, transformTask!!
                .taskDependencies.getDependencies(transformTask).count { it.name == "compile${sourceSetSuffix}Java" })

        // Classes task of source set should depend on it.
        val classesTask = project.tasks.getByName(classesTaskName)
        assertEquals(
            1, classesTask
                .taskDependencies.getDependencies(classesTask).count { it.name == transformTask.name })
    }

    @Test
    fun apply_afterKotlinAndKaptPlugin() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("kotlin")
            apply("kotlin-kapt")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertKotlinSetup(project)
    }

    @Test
    fun apply_afterKotlinPlugin_addsKapt() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply {
            apply("kotlin")
            apply(pluginId)
        }
        project.enableObjectBoxPluginDebugMode()

        assertKotlinSetup(project)
    }

    private fun assertKotlinSetup(project: Project) {
        with(project.configurations) {
            assertProcessorDependency(getByName("kapt").dependencies)

            getByName("api").dependencies.let { deps ->
                assertEquals(1, deps.count {
                    it.group == "io.objectbox" && it.name == "objectbox-kotlin"
                            && it.version == ProjectEnv.Const.javaVersionToApply
                })
                assertJavaDependency(deps)
                assertNativeDependency(deps)
            }
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        // Note: using the internal evaluate is not nice, but beats writing a full-blown integration test.
        (project as ProjectInternal).evaluate()

        // AFTER EVALUATE.
        // Note: by default only main and test source sets exist.
        // Note: transform is not supported for Kotlin code/tasks, so these match plain Java plugin.
        assertTransformTask(project, "", "classes")
        assertTransformTask(project, "Test", "testClasses")
    }

    private fun assertProcessorDependency(apDeps: DependencySet) {
        assertEquals("objectbox-processor dependency not found", 1, apDeps.count {
            it.group == "io.objectbox" && it.name == "objectbox-processor"
                    && it.version == ProjectEnv.Const.pluginVersion
        })
    }

    private fun assertJavaDependency(compileDeps: DependencySet) {
        assertEquals("objectbox-java dependency not found", 1, compileDeps.count {
            it.group == "io.objectbox" && it.name == "objectbox-java"
                    && it.version == ProjectEnv.Const.javaVersionToApply
        })
    }

    open fun assertNativeDependency(compileDeps: DependencySet) {
        assertEquals("JNI lib dependency not found", 1, compileDeps.count {
            it.group == "io.objectbox"
                    && (it.name == "$expectedLibWithSyncVariantPrefix-linux"
                    || it.name == "$expectedLibWithSyncVariantPrefix-windows"
                    || it.name == "$expectedLibWithSyncVariantPrefix-macos")
                    && it.version == expectedLibWithSyncVariantVersion
        })
    }

}