package io.objectbox.gradle

import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language


/**
 * Tests assembling an Android project using Android Plugin 7.2.
 * Notably uses the new ASM based Transform API.
 */
class Android72ProjectPluginTest : AndroidProjectPluginTest() {

    // Uses the android.namespace property instead of setting package name in AndroidManifest.xml.
    @Language("Groovy")
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

    @Language("XML")
    override val androidManifest =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application>
            </application>
        </manifest>
        """.trimIndent()

    // Android Plugin 7.2 does not support Gradle 8.
    private val gradleVersionImpl = "7.3.3"
    override val additionalRunnerConfiguration: ((GradleRunner) -> Unit) = {
        // Do not forward output, many warning messages due to using outdated Android plugin.
        // Enable this when testing to diagnose Gradle task output.
        // it.forwardOutput()
        it.withGradleVersion(gradleVersionImpl)
    }

    override val androidPluginVersion: String = "7.2.2"
    override val gradleVersion: String = gradleVersionImpl

    // New ASM based transformers output to a different path.
    override val buildTransformDirectory =
        "build/intermediates/asm_instrumented_project_classes/debug"

}