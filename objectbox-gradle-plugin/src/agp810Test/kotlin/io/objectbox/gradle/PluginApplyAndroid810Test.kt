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

import io.objectbox.gradle.ProjectEnv.Const
import org.gradle.api.plugins.JavaPlugin
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test


/**
 * Tests applying [ObjectBoxGradlePlugin] configures a Java or Kotlin Android Gradle project as
 * expected.
 *
 * Tests with Android Plugin 8.10 (as 8.1 is incompatible with Gradle 9 required by
 * [PluginApplyAndroidTest]), which is also the lowest version supporting KSP (so Kotlin projects).
 */
open class PluginApplyAndroid810Test : PluginApplyAndroidTest() {

    @Test
    fun apply_afterAndroidPlugin() {
        val project = buildAndroidProject()

        project.resolveDependencyGraphWithoutDownloadingFiles()
        with(project.configurations) {
            assertProcessorDependency(getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME).dependencies)
            getByName(JavaPlugin.API_CONFIGURATION_NAME).dependencies.also {
                it.assertAndroidDependency()
                it.assertJavaDependency()
            }
            assertNativeDependency(getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).dependencies)
        }
        assertNotNull(project.tasks.findByPath(PrepareTask.NAME))

        assertAndroidCompat(project)

        // Note: can not evaluate and assert transform task for unit tests as Android plugin requires actual project,
        // this is tested using Gradle TestKit in the AndroidProjectPluginTest subclasses.
    }

    @Test
    fun apply_afterKotlinAndroidAndKspPlugin() {
        val project = buildProject {
            pluginManager.apply {
                apply(Const.ANDROID_APPLICATION_PLUGIN_ID)
                apply(Const.KOTLIN_ANDROID_PLUGIN_ID)
                apply(Const.KSP_PLUGIN_ID)
                apply(pluginId)
            }
        }

        assertKotlinAndroidSetup(project, Const.KSP_CONFIGURATION_NAME)
    }

    @Test
    fun apply_afterKotlinAndroidPlugin_addsKsp() {
        val project = buildProject {
            pluginManager.apply {
                apply(Const.ANDROID_APPLICATION_PLUGIN_ID)
                apply(Const.KOTLIN_ANDROID_PLUGIN_ID)
                apply(pluginId)
            }
        }

        assertTrue(project.plugins.hasPlugin(Const.KSP_PLUGIN_ID))
        assertKotlinAndroidSetup(project, Const.KSP_CONFIGURATION_NAME)
    }

    @Test
    fun apply_afterKotlinAndroidAndKaptPlugin_usesKapt() {
        val project = buildProject {
            pluginManager.apply {
                apply(Const.ANDROID_APPLICATION_PLUGIN_ID)
                apply(Const.KOTLIN_ANDROID_PLUGIN_ID)
                apply(Const.KOTLIN_KAPT_PLUGIN_ID)
                apply(pluginId)
            }
        }

        // The plugin respects the kapt setup, so does not apply the KSP plugin.
        // Notably this works with Android Plugin versions not supported by KSP.
        assertFalse(project.plugins.hasPlugin(Const.KSP_PLUGIN_ID))
        assertKotlinAndroidSetup(project, Const.KAPT_CONFIGURATION_NAME)
    }

}
