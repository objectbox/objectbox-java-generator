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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


/**
 * Tests applying [ObjectBoxGradlePlugin] with Android Plugin 7.3 fails because it is too old.
 *
 * Note: unlike the plugin apply tests for supported Android Plugin versions this uses TestKit to run an actual
 * build as Android Plugin 7.3 is not compatible with the Gradle version of this build (so it can not run
 * in-process with ProjectBuilder).
 */
class PluginApplyAndroid73Test {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Test
    fun apply_afterAndroidPlugin_failsTooOld() {
        val gradleRunner = GradleTestRunner(testProjectDir).apply {
            additionalPlugins += "com.android.application"
        }

        // Applying the plugin fails during configuration, so no Android SDK or resolvable dependencies required.
        val result = gradleRunner.buildAndFail(listOf("--stacktrace", "help")) {
            // Use a Gradle version compatible with both Android Plugin 7.3 and the JDK running this build (JDK 21
            // requires at least Gradle 8.5).
            it.withGradleVersion("8.5")
        }

        assertThat(result.output)
            .contains("The ObjectBox Gradle plugin requires Android Gradle Plugin 8.1 or newer")
    }

}
