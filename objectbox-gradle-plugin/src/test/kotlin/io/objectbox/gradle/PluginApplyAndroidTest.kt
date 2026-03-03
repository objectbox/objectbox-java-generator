/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2024 ObjectBox Ltd.
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

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test


/**
 * Base class to test applying [ObjectBoxGradlePlugin] configures a Java or Kotlin Android Gradle project as expected.
 */
abstract class PluginApplyAndroidTest : PluginApplyTest() {

    /**
     * Checks the correct compat shim is used.
     */
    abstract fun assertAndroidCompat(project: Project)

    private fun Project.configureAndroidProject() {
        val androidExtension = extensions.getByName("android") as ApplicationExtension
        androidExtension.apply {
            compileSdk = 35 // Matches SDK embedded in buildenv-android CI image to avoid downloading it
            namespace = "io.objectbox.plugin.test"
        }
    }

    /**
     * Because the plugin adds dependencies using [org.gradle.api.DomainObjectCollection.addLater] it is necessary to
     * resolve the dependency graph before being able to inspect them. However, the Android Gradle Plugin only adds the
     * needed classpath configurations during project evaluation. So force project evaluation (and configure the Android
     * project as needed to do so).
     *
     * See also [io.objectbox.gradle.PluginApplyJavaTest.resolveDependencyGraphWithoutDownloadingFiles], especially with
     * details on why the Android plugin warns about doing this and why we ignore this warning.
     */
    private fun Project.resolveDependencyGraphWithoutDownloadingFiles() {
        project.configureAndroidProject()
        (project as ProjectInternal).evaluate()

        project.configurations.getByName("debugCompileClasspath")
            .incoming
            .resolutionResult
            .allDependencies

        project.configurations.getByName("debugUnitTestCompileClasspath")
            .incoming
            .resolutionResult
            .allDependencies
    }

    private fun buildAndroidProject(): Project =
        buildProject {
            pluginManager.apply {
                apply("com.android.application")
                apply(pluginId)
            }
        }

    @Test
    fun apply_afterAndroidPlugin() {
        val project = buildAndroidProject()

        project.resolveDependencyGraphWithoutDownloadingFiles()
        with(project.configurations) {
            assertProcessorDependency(getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME).dependencies)
            assertAndroidDependency(getByName(JavaPlugin.API_CONFIGURATION_NAME).dependencies)
            assertNativeDependency(getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).dependencies)
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        assertAndroidCompat(project)

        // Note: can not evaluate and assert transform task for unit tests as Android plugin requires actual project,
        // this is tested using Gradle TestKit in AndroidPluginTransformTest.
    }

    @Test
    fun apply_afterKotlinAndroidAndKaptPlugin() {
        val project = buildProject {
            pluginManager.apply {
                apply("com.android.application")
                apply("kotlin-android")
                apply("kotlin-kapt")
                apply(pluginId)
            }
        }

        assertKotlinAndroidSetup(project)
    }

    @Test
    fun apply_afterKotlinAndroidPlugin_addsKapt() {
        val project = buildProject {
            pluginManager.apply {
                apply("com.android.application")
                apply("kotlin-android")
                apply(pluginId)
            }
        }

        assertKotlinAndroidSetup(project)
    }

    private fun assertKotlinAndroidSetup(project: Project) {
        project.resolveDependencyGraphWithoutDownloadingFiles()
        with(project.configurations) {
            assertProcessorDependency(getByName(ProjectEnv.Const.KAPT_CONFIGURATION_NAME).dependencies)
            assertAndroidDependency(getByName(JavaPlugin.API_CONFIGURATION_NAME).dependencies)
            assertNativeDependency(getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).dependencies)
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        assertAndroidCompat(project)

        // Note: can not evaluate and assert transform task for unit tests as Android plugin requires actual project,
        // this is tested using Gradle TestKit in AndroidTransformTest.
    }

    private fun assertAndroidDependency(deps: DependencySet) {
        assertEquals("Android lib dependency not found", 1, deps.count {
            it.group == "io.objectbox" && it.name == "$expectedLibWithSyncVariantPrefix-android"
                    && it.version == expectedLibWithSyncVariantVersion
        })
    }

    private val databaseLibraries = listOf(
        "objectbox-android",
        "objectbox-android-objectbrowser",
        "objectbox-sync-android",
        "objectbox-sync-android-objectbrowser",
        "objectbox-sync-server-android"
    )

    @Test
    fun apply_doesNotAddAdditionalDatabaseLibrary() {
        databaseLibraries.forEach {
            assertNoDatabaseLibraryAdded(it)
        }
    }

    private fun assertNoDatabaseLibraryAdded(name: String) {
        val project = buildAndroidProject()

        // Use a custom version that's easy to recognize if this test should fail
        val customVersion = "${ProjectEnv.Const.nativeVersionToApply}-custom"
        project.dependencies.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "io.objectbox:$name:$customVersion")

        project.resolveDependencyGraphWithoutDownloadingFiles()
        val databaseDeps = project.configurations
            .flatMap { configuration ->
                configuration.dependencies
                    .filter { databaseLibraries.contains(it.name) }
            }
        assertEquals(
            "Must not add additional database library, but has:\n${databaseDeps.joinToString("\n")}",
            1,
            databaseDeps.size
        )
    }

}