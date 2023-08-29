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