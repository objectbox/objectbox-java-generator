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
import io.objectbox.gradle.transform.AndroidPlugin72
import io.objectbox.gradle.util.AndroidCompat
import org.gradle.api.Project


/**
 * Tests applying [ObjectBoxGradlePlugin] configures an Android Gradle project as expected.
 *
 * Tests with Android Plugin 9 which has built-in Kotlin support enabled by default, so unlike for older versions
 * (see PluginApplyAndroid810Test in the agp810Test source set) the Kotlin Android plugin can not be applied and
 * the plugin is expected to apply the legacy-kapt plugin of the Android Plugin (available on the classpath for
 * this test, see the build script of this project) and to add the ObjectBox Kotlin library.
 */
open class PluginApplyAndroid9Test : PluginApplyAndroidTest() {

    override val expectsBuiltInKotlin: Boolean = true

    override val compileSdk: Int = 36

    override fun assertAndroidCompat(project: Project) {
        assertThat(AndroidCompat.getPlugin(project))
            .isInstanceOf(AndroidPlugin72::class.java)
    }

}
