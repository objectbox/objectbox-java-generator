package io.objectbox.gradle.transform


/**
 * Tests Transform API using Android Plugin 7.2.
 * Notably uses the new ASM based Transform API.
 */
class AndroidPlugin72TransformTest : AndroidPluginTransformTest() {

    // Uses the android.namespace property instead of setting package name in AndroidManifest.xml.
    override val buildScriptAndroidBlock =
        """
        android {
            namespace 'com.example'
            compileSdkVersion 32
            defaultConfig {
                applicationId "com.example"
                minSdkVersion 21
                targetSdkVersion 32
            }
            compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
            }
        }
        """.trimIndent()

    override val androidManifest =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application>
            </application>
        </manifest>
        """.trimIndent()

    // New ASM based transformers output to a different path.
    override val buildTransformDirectory =
        "build/intermediates/asm_instrumented_project_classes/debug"

}