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

import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language


/**
 * Tests assembling an Android project with Java sources using Android Plugin 9.
 */
class Android9ProjectPluginTest : AndroidProjectPluginTest() {

    // Uses the new DSL of Android Plugin 9 (assignment instead of the removed old setter methods).
    @Language("Groovy")
    override val buildScriptAndroidBlock =
        """
        android {
            namespace = 'com.example'
            compileSdk = 36 // Matches SDK embedded in buildenv-android CI image to avoid downloading it
            defaultConfig {
                applicationId = "com.example"
                minSdk = 21
            }
        }
        """.trimIndent()

    // The java block of older tests is not used with Android Plugin 9, the android block configures the project.
    override val buildScriptJavaBlock = ""

    @Language("XML")
    override val androidManifest =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application>
            </application>
        </manifest>
        """.trimIndent()

    // Test with the oldest possible version of Gradle (Android Plugin 9.2 requires Gradle 9.4.1).
    private val gradleVersionLowest = "9.4.1"
    override val additionalRunnerConfiguration: ((GradleRunner) -> Unit) = {
        it.forwardOutput()
        it.withGradleVersion(gradleVersionLowest)
    }

    override val expectedAndroidPluginVersion: String = "9.2.1"
    override val expectedGradleVersion: String = gradleVersionLowest

    override val buildTransformDirectory =
        "build/intermediates/classes/debug/transformDebugClassesWithAsm/dirs"

}
