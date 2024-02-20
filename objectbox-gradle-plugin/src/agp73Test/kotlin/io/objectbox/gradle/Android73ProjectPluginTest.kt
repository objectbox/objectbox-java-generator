/*
 * Copyright (C) 2022-2023 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle

import org.gradle.util.GradleVersion
import org.intellij.lang.annotations.Language


/**
 * Tests assembling an Android project using Android Plugin 7.3.
 * Notably uses the new ASM based Transform API.
 */
class Android73ProjectPluginTest : AndroidProjectPluginTest() {

    // Uses the android.namespace property instead of setting package name in AndroidManifest.xml.
    @Language("Groovy")
    override val buildScriptAndroidBlock =
        """
        android {
            namespace 'com.example'
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
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application>
            </application>
        </manifest>
        """.trimIndent()

    override val androidPluginVersion: String = "7.3.0"
    override val gradleVersion: String = GradleVersion.current().version

    // New ASM based transformers output to a different path.
    override val buildTransformDirectory =
        "build/intermediates/asm_instrumented_project_classes/debug"

}