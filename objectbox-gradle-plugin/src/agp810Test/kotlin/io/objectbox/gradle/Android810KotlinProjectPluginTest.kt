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

import com.google.common.truth.Truth.assertThat
import io.objectbox.gradle.ProjectEnv.Const
import io.objectbox.gradle.transform.getInitializedFields
import javassist.bytecode.ClassFile
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language
import java.io.DataInputStream
import java.io.File

/**
 * Tests assembling an Android project with a Java and a Kotlin entity class using Android Plugin
 * 8.10, the lowest version supporting KSP: as the project has applied the KSP plugin, the ObjectBox
 * plugin uses KSP, the KSP processor generates code for both entities and the byte code transform
 * runs on both entity classes.
 */
class Android810KotlinProjectPluginTest : AndroidProjectPluginTest() {

    // Uses the android.namespace property instead of setting package name in AndroidManifest.xml.
    @Language("Groovy")
    override val buildScriptAndroidBlock =
        """
        android {
            namespace 'com.example'
            compileSdkVersion $compileSdk
            defaultConfig {
                applicationId "com.example"
                minSdkVersion $minSdk
            }
            compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
            }
        }
        """.trimIndent()

    // Test with the oldest possible version of Gradle (Android Plugin 8.10 requires Gradle 8.11.1).
    override val gradleVersion: String = "8.11.1"

    override val expectedAndroidPluginVersion: String = "8.10.1"

    override fun configureTestRunner(gradleTestRunner: GradleTestRunner) {
        super.configureTestRunner(gradleTestRunner)
        gradleTestRunner.apply {
            additionalPlugins += "org.jetbrains.kotlin.android"
            additionalPlugins += Const.KSP_PLUGIN_ID
            additionalBlocks +=
                """
                tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                    kotlinOptions.jvmTarget = "1.8"
                }
                """.trimIndent()
            debugKspProcessor()
        }
    }

    override fun addSourceFiles(gradleRunner: GradleTestRunner) {
        super.addSourceFiles(gradleRunner)
        gradleRunner.addSourceFile(
            "KotlinEntity.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.relation.ToMany
            import io.objectbox.relation.ToOne

            @Entity
            class KotlinEntity {
                @Id var id: Long = 0
                var note: String? = null
                lateinit var toOneProperty: ToOne<KotlinEntity>
                lateinit var toManyProperty: ToMany<KotlinEntity>
            }
            """.trimIndent()
        )
    }

    override fun assertBuildResult(result: BuildResult, transformDir: File) {
        // The plugin used KSP (and did not apply kapt), the processor ran and generated code.
        assertThat(result.task(":kaptDebugKotlin")).isNull()
        assertThat(result.task(":kspDebugKotlin")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val generatedDir = File(testProjectDir.root, "build/generated/ksp/debug/java/com/example")
        assertThat(File(generatedDir, "MyObjectBox.java").exists()).isTrue()
        assertThat(File(generatedDir, "KotlinEntity_.java").exists()).isTrue()
        assertThat(File(generatedDir, "ExampleEntity_.java").exists()).isTrue()

        // The byte code transform ran on the Kotlin entity class.
        File(transformDir, "com/example/KotlinEntity.class").inputStream().use { fileInputStream ->
            val classFile = ClassFile(DataInputStream(fileInputStream))

            assertBoxStoreField(classFile)

            // Check the constructor initializes the (lateinit) relation properties.
            val constructors = classFile.methods.filter { it.isConstructor }
            assertThat(constructors).hasSize(1)
            assertThat(classFile.getInitializedFields(constructors[0]))
                .containsExactly("toOneProperty", "toManyProperty")
        }

        assertCursorTransformed(transformDir, "KotlinEntity")
    }

}
