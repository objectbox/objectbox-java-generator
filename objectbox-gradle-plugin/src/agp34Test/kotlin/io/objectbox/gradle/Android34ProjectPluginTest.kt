package io.objectbox.gradle

import org.gradle.testkit.runner.GradleRunner


/**
 * Tests assembling an Android project using Android Plugin 3.4.
 * Notably uses the legacy Transform API.
 */
class Android34ProjectPluginTest : AndroidProjectPluginTest() {

    override val buildScriptAndroidBlock =
        """
        android {
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
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="com.example">
            <application>
            </application>
        </manifest>
        """.trimIndent()

    // Test with the oldest supported version of Gradle (see GradleCompat),
    // also Android Plugin 3.4 does not support Gradle 7.
    private val gradleVersionImpl = "6.1.1"
    override val additionalRunnerConfiguration: ((GradleRunner) -> Unit) = {
        // Do not forward output, many warning messages due to using outdated Android plugin.
        // Enable this when testing to diagnose Gradle task output.
        // it.forwardOutput()
        it.withGradleVersion(gradleVersionImpl)
    }

    override val androidPluginVersion: String = "pre-7.0"
    override val gradleVersion: String = gradleVersionImpl

    override val buildTransformDirectory =
        "build/intermediates/transforms/ObjectBoxAndroidTransform/debug/0"

}