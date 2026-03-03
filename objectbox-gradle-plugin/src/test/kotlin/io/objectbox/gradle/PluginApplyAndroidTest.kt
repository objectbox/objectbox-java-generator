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

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
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

    @Test
    fun apply_afterAndroidPlugin() {
        val project = buildProject {
            pluginManager.apply {
                apply("com.android.application")
                apply(pluginId)
            }
        }

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

    private fun assertProcessorDependency(apDeps: DependencySet) {
        assertEquals("objectbox-processor dependency not found", 1, apDeps.count {
            it.group == "io.objectbox" && it.name == "objectbox-processor"
                    && it.version == ProjectEnv.Const.pluginVersion
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

    open fun assertAndroidDependency(deps: DependencySet) {
        assertEquals("Android lib dependency not found", 1, deps.count {
            it.group == "io.objectbox" && it.name == "$expectedLibWithSyncVariantPrefix-android"
                    && it.version == expectedLibWithSyncVariantVersion
        })
    }

}