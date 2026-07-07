/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2026 ObjectBox Ltd.
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

import com.google.common.truth.Truth.assertThat
import io.objectbox.gradle.ProjectEnv.Const
import io.objectbox.gradle.transform.AndroidPlugin72
import io.objectbox.gradle.util.AndroidCompat
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.junit.Assert.assertNotNull
import org.junit.Test


/**
 * Tests applying [ObjectBoxGradlePlugin] configures a Java or Kotlin Android Gradle project as expected.
 * Tests with Android Plugin 8.13 (the latest of the 8 series that is compatible with the Gradle version of this build).
 *
 * Also tests with the Kotlin Android plugin (and kapt), which is specific to the Android Plugin 8 series: projects
 * using Android Plugin 9 or newer can no longer apply it (they use built-in Kotlin, see PluginApplyAndroid9Test in
 * the agp9Test source set).
 */
open class PluginApplyAndroid813Test : PluginApplyAndroidTest() {

    override fun assertAndroidCompat(project: Project) {
        assertThat(AndroidCompat.getPlugin(project))
            .isInstanceOf(AndroidPlugin72::class.java)
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
            assertProcessorDependency(getByName(Const.KAPT_CONFIGURATION_NAME).dependencies)
            assertAndroidDependency(getByName(JavaPlugin.API_CONFIGURATION_NAME).dependencies)
            assertKotlinDependency(getByName(JavaPlugin.API_CONFIGURATION_NAME).dependencies)
            assertNativeDependency(getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).dependencies)
        }
        assertNotNull(project.tasks.findByPath("objectboxPrepareBuild"))

        assertAndroidCompat(project)

        // Note: can not evaluate and assert transform task for unit tests as Android plugin requires actual project,
        // this is tested using Gradle TestKit in the AndroidProjectPluginTest subclasses.
    }

}
