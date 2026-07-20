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

import com.google.common.truth.Truth.assertThat
import io.objectbox.gradle.ProjectEnv.Const
import org.junit.Test


/**
 * Tests applying [ObjectBoxGradlePlugin] configures an Android Kotlin Gradle project as expected.
 *
 * Tests with the latest Android Plugin 9.
 *
 * Starting with 9.0 the Android Gradle Plugin applies the Kotlin plugin itself ("built-in Kotlin")
 * and instead of kapt its bundled legacy-kapt plugin must be used. So no longer testing a Java
 * Android project.
 */
open class PluginApplyAndroid9Test : PluginApplyAndroidTest() {

    @Test
    fun apply_afterAndroidPlugin_addsLegacyKapt() {
        val project = buildAndroidProject()

        // The plugin should apply the legacy-kapt plugin.
        assertThat(project.plugins.hasPlugin(Const.ANDROID_LEGACY_KAPT_PLUGIN_ID)).isTrue()

        assertKotlinAndroidSetup(project)
    }

    @Test
    fun apply_afterAndroidAndLegacyKaptPlugin() {
        val project = buildProject {
            pluginManager.apply {
                apply("com.android.application")
                apply(Const.ANDROID_LEGACY_KAPT_PLUGIN_ID)
                apply(pluginId)
            }
        }

        assertKotlinAndroidSetup(project)
    }

}
