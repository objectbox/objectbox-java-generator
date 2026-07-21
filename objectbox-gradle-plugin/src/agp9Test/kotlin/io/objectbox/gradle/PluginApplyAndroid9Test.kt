/*
 * ObjectBox Build Tools
 * Copyright (C) 2026 ObjectBox Ltd.
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
import com.google.common.truth.Truth.assertThat
import io.objectbox.gradle.ProjectEnv.Const
import io.objectbox.gradle.transform.AndroidPlugin72
import io.objectbox.gradle.util.AndroidCompat
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test


/**
 * Tests applying [ObjectBoxGradlePlugin] configures an Android Gradle project as expected.
 *
 * Tests with Android Plugin 9 which has built-in Kotlin support enabled by default, so unlike for older versions
 * (see PluginApplyAndroidTest in the test source set) the Kotlin Android plugin can not be applied and the plugin
 * is expected to apply the legacy-kapt plugin of the Android Plugin and to add the ObjectBox Kotlin library.
 *
 * Note: this only works because Android Plugin 9 is compatible with the Gradle version running this build
 * (tests based on ProjectBuilder run the Android Plugin in-process).
 */
open class PluginApplyAndroid9Test : PluginApplyTest() {

    open fun assertAndroidCompat(project: Project) {
        assertThat(AndroidCompat.getPlugin(project))
            .isInstanceOf(AndroidPlugin72::class.java)
    }

    private fun Project.configureAndroidProject() {
        val androidExtension = extensions.getByName("android") as ApplicationExtension
        androidExtension.apply {
            compileSdk = 36 // Matches SDK embedded in buildenv-android CI image to avoid downloading it
            namespace = "io.objectbox.plugin.test"
        }
    }

    /**
     * Like [io.objectbox.gradle.PluginApplyAndroidTest.resolveDependencyGraphWithoutDownloadingFiles] in the test
     * source set: force project evaluation and resolve the dependency graph so dependencies added with addLater
     * (and within withDependencies) can be inspected.
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
    fun apply_afterAndroidPlugin_usesBuiltInKotlin() {
        val project = buildAndroidProject()

        // As the project uses built-in Kotlin, expect the plugin to have applied the legacy-kapt plugin.
        assertThat(project.plugins.hasPlugin(Const.ANDROID_LEGACY_KAPT_PLUGIN_ID)).isTrue()

        project.resolveDependencyGraphWithoutDownloadingFiles()
        with(project.configurations) {
            // The processor should be added to the kapt configuration (of the legacy-kapt plugin).
            assertProcessorDependency(getByName(Const.KAPT_CONFIGURATION_NAME).dependencies)
            assertAndroidDependency(getByName(JavaPlugin.API_CONFIGURATION_NAME).dependencies)
            // As the project uses built-in Kotlin, the Kotlin library should be added.
            assertKotlinDependency(getByName(JavaPlugin.API_CONFIGURATION_NAME).dependencies)
            assertNativeDependency(getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).dependencies)
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        assertAndroidCompat(project)

        // Note: can not evaluate and assert transform task for unit tests as Android plugin requires actual project,
        // this is tested using Gradle TestKit in Android9ProjectPluginTest.
    }

    private fun assertAndroidDependency(deps: DependencySet) {
        assertEquals("Android lib dependency not found", 1, deps.count {
            it.group == Const.OBX_GROUP && it.name == "$expectedLibWithSyncVariantPrefix-android"
                    && it.version == expectedLibWithSyncVariantVersion
        })
    }

    private fun assertKotlinDependency(deps: DependencySet) {
        assertEquals("Kotlin lib dependency not found", 1, deps.count {
            it.group == Const.OBX_GROUP && it.name == Const.OBX_KOTLIN
                    && it.version == Const.OBX_JAVA_VERSION
        })
    }

}
