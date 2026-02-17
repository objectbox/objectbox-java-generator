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


/**
 * Tests assembling an Android project using Android Plugin 8.1.
 * Notably uses the new ASM based Transform API.
 */
class Android81ProjectPluginTest : AndroidProjectPluginTest() {

    // Uses the android.namespace property instead of setting package name in AndroidManifest.xml.
    @Language("Groovy")
    override val buildScriptAndroidBlock =
        """
        android {
            namespace 'com.example'
            compileSdkVersion 35 // Matches SDK embedded in buildenv-android CI image to avoid downloading it
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
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application>
            </application>
        </manifest>
        """.trimIndent()

    // Test with the oldest possible version of Gradle (JDK 21 requires Gradle 8.5, Android Plugin 8.1 requires Gradle
    // 8.0, this plugin (see GradleCompat) requires Gradle 7.0).
    private val gradleVersionLowest = "8.5"
    override val additionalRunnerConfiguration: ((GradleRunner) -> Unit) = {
        it.forwardOutput()
        it.withGradleVersion(gradleVersionLowest)
    }

    override val expectedAndroidPluginVersion: String = "8.1.4"
    override val expectedGradleVersion: String = gradleVersionLowest

    // New ASM based transformers output to a different path. The path has also changed with Android Gradle Plugin 8.
    override val buildTransformDirectory =
        "build/intermediates/classes/debug/transformDebugClassesWithAsm/dirs"

}