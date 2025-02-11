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

import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.Ignore


/**
 * Tests assembling an Android project using Android Plugin 4.1.
 * Notably uses the legacy Transform API.
 */
@Ignore("objectbox-android is no longer compatible, this needs updates, see objectbox-java#215")
class Android41ProjectPluginTest : AndroidProjectPluginTest() {

    @Language("Groovy")
    override val buildScriptAndroidBlock =
        """
        android {
            compileSdkVersion 33
            defaultConfig {
                applicationId "com.example"
                minSdkVersion 21
                targetSdkVersion 33
            }
            compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
            }
        }
        """.trimIndent()

    @Language("XML")
    override val androidManifest =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="com.example">
            <application>
            </application>
        </manifest>
        """.trimIndent()

    // Test with the oldest supported version of Gradle (see GradleCompat, Android Plugin 4.1 would only need 6.5).
    private val gradleVersionImpl = "7.0.2"
    override val additionalRunnerConfiguration: ((GradleRunner) -> Unit) = {
        // Do not forward output, many warning messages due to using outdated Android plugin.
        // Enable this when testing to diagnose Gradle task output.
        // it.forwardOutput()
        it.withGradleVersion(gradleVersionImpl)
    }

    override val androidPluginVersion: String = "pre-7.0"
    override val gradleVersion: String = gradleVersionImpl

    override val buildTransformDirectory =
        "build/intermediates/transforms/ObjectBoxAndroidTransform/debug/1"

}