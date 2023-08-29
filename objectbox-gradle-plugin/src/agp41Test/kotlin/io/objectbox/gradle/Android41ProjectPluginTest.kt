package io.objectbox.gradle

import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language


/**
 * Tests assembling an Android project using Android Plugin 4.1.
 * Notably uses the legacy Transform API.
 */
class Android41ProjectPluginTest : AndroidProjectPluginTest() {

    @Language("Groovy")
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