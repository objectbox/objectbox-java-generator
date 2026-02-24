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
import org.gradle.api.plugins.JavaPlugin
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
        val project = buildProject {
            pluginManager.apply {
                apply("java")
                apply(pluginId)
            }
        }

        assertJavaProject(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
    }

    @Test
    fun apply_afterApplicationPlugin() {
        val project = buildProject {
            pluginManager.apply {
                apply("application") // Note: application plugin adds java plugin.
                apply(pluginId)
            }
        }

        assertJavaProject(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
    }

    @Test
    fun apply_afterJavaLibraryPlugin() {
        val project = buildProject {
            pluginManager.apply {
                apply("java-library")
                apply(pluginId)
            }
        }

        assertJavaProject(project, JavaPlugin.API_CONFIGURATION_NAME)
    }

    /**
     * The ObjectBox plugin adds dependencies using [org.gradle.api.DomainObjectCollection.addLater] (see
     * [ObjectBoxGradlePlugin.addDependencies]). As a side effect they won't appear in the configuration they were added
     * to until the dependency graph is resolved. So to assert dependencies were added by the plugin, the graph must be
     * resolved.
     *
     * To resolve the graph [org.gradle.api.artifacts.Configuration.resolve] could be used, but it will download files.
     * So instead access all incoming dependencies, which only resolves the graph.
     *
     * Also use the compileClasspath configuration all others contribute to as the api and implementation
     * configurations can not be resolved themselves.
     *
     * Despite this being similar to what the Kotlin Gradle plugin tests do, note that the Android plugin warns about
     * and the Gradle folks [don't recommend resolving configurations before task execution](https://docs.gradle.org/current/userguide/best_practices_tasks.html#dont_resolve_configurations_before_task_execution).
     * So if ever enforced (see https://github.com/gradle/gradle/issues/2298 for a discussion), this approach might
     * break in the future. An alternative (that does require downloading files) is to use Gradle TestKit instead and
     * maybe to inspect output of the dependencies task or to use a custom task to do validation.
     */
    private fun Project.resolveDependencyGraphWithoutDownloadingFiles() {
        configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
            .incoming
            .resolutionResult
            .allDependencies
    }

    private fun assertJavaProject(project: Project, configuration: String) {
        project.resolveDependencyGraphWithoutDownloadingFiles()
        with(project.configurations) {
            assertProcessorDependency(getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME).dependencies)

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
        assertTransformTask(project, "", JavaPlugin.CLASSES_TASK_NAME)
        assertTransformTask(project, "Test", JavaPlugin.TEST_CLASSES_TASK_NAME)
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
        val project = buildProject {
            pluginManager.apply {
                apply("kotlin")
                apply("kotlin-kapt")
                apply(pluginId)
            }
        }

        assertKotlinSetup(project)
    }

    @Test
    fun apply_afterKotlinPlugin_addsKapt() {
        val project = buildProject {
            pluginManager.apply {
                apply("kotlin")
                apply(pluginId)
            }
        }

        assertKotlinSetup(project)
    }

    private fun assertKotlinSetup(project: Project) {
        project.resolveDependencyGraphWithoutDownloadingFiles()
        with(project.configurations) {
            assertProcessorDependency(getByName(ProjectEnv.Const.KAPT_CONFIGURATION_NAME).dependencies)

            getByName(JavaPlugin.API_CONFIGURATION_NAME).dependencies.let { deps ->
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
        assertTransformTask(project, "", JavaPlugin.CLASSES_TASK_NAME)
        assertTransformTask(project, "Test", JavaPlugin.TEST_CLASSES_TASK_NAME)
    }

    private fun assertJavaDependency(deps: DependencySet) {
        assertEquals("objectbox-java dependency not found", 1, deps.count {
            it.group == "io.objectbox" && it.name == "objectbox-java"
                    && it.version == ProjectEnv.Const.javaVersionToApply
        })
    }

}