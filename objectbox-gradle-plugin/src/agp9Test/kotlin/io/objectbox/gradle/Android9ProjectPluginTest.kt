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

import org.intellij.lang.annotations.Language


/**
 * Tests assembling an Android project using the latest supported version of the Android Plugin
 * (see build script and [expectedAndroidPluginVersion]) and Gradle (see [gradleVersion]).
 *
 * Android Plugin 9 notably has switched to a new DSL, built-in Kotlin support and requires using
 * its own legacy-kapt plugin (which must be added to the special plugin classpath for
 * GradleTestRunner, see the build script).
 */
open class Android9ProjectPluginTest : AndroidProjectPluginTest() {

    // Uses the new DSL of Android Plugin 9 (assignment instead of the removed old setter methods).
    @Language("Groovy")
    override val buildScriptAndroidBlock =
        """
        android {
            namespace = 'com.example'
            compileSdk = $COMPILE_SDK
            defaultConfig {
                applicationId = "com.example"
                minSdk = $MIN_SDK
            }
        }
        """.trimIndent()

    // Test with the latest possible version of Gradle
    override val gradleVersion = "9.6.1"

    override val expectedAndroidPluginVersion: String = "9.2.1"

    override fun configureTestRunner(gradleTestRunner: GradleTestRunner) {
        super.configureTestRunner(gradleTestRunner)
        gradleTestRunner.debugKaptProcessor()
    }

}
