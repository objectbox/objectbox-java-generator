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
import io.objectbox.gradle.transform.ClassConst
import io.objectbox.gradle.transform.assignsBoxStoreField
import io.objectbox.gradle.transform.getInitializedFields
import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.ClassFile
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.DataInputStream
import java.io.File
import java.lang.reflect.Modifier

/**
 * Tests assembling an Android project with Kotlin entity classes using Android Plugin 8.10, the
 * lowest version supporting KSP: as the project has applied the KSP plugin, the ObjectBox plugin
 * uses KSP, the KSP processor generates code and the byte code transform runs on the Kotlin
 * classes.
 */
class Android810KotlinProjectPluginTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Test
    fun assembleKotlinAndroidProject() {
        val gradleRunner = GradleTestRunner(testProjectDir).apply {
            // Note: classpath for plugins configured in build script of this project (see GradleTestRunner.build).
            additionalPlugins += Const.ANDROID_APPLICATION_PLUGIN_ID
            additionalPlugins += "org.jetbrains.kotlin.android"
            additionalPlugins += Const.KSP_PLUGIN_ID

            @Language("Groovy")
            val androidBlock =
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
                tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                    kotlinOptions.jvmTarget = "1.8"
                }
                """.trimIndent()
            additionalBlocks += androidBlock
            debugKspProcessor()
        }

        @Language("XML")
        val manifest =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application>
                </application>
            </manifest>
            """.trimIndent()
        testProjectDir.newFile("src/main/AndroidManifest.xml").writeText(manifest)

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

        // Note: not pinning a Gradle version: while Android Plugin 8.10 supports Gradle 8.11.1,
        // the Kotlin plugin version used for tests requires a newer Gradle version.
        val result = gradleRunner.build(listOf("--stacktrace", "assembleDebug")) {
            it.forwardOutput()
        }

        // The plugin used KSP (and did not apply kapt), the processor ran and generated code.
        assertThat(result.task(":kaptDebugKotlin")).isNull()
        assertThat(result.task(":kspDebugKotlin")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":assembleDebug")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val generatedDir = File(testProjectDir.root, "build/generated/ksp/debug/java/com/example")
        assertThat(File(generatedDir, "MyObjectBox.java").exists()).isTrue()
        assertThat(File(generatedDir, "KotlinEntity_.java").exists()).isTrue()

        // The byte code transform ran on the Kotlin entity class.
        val transformDir = File(
            testProjectDir.root,
            "build/intermediates/classes/debug/transformDebugClassesWithAsm/dirs"
        )
        File(transformDir, "com/example/KotlinEntity.class").inputStream().use { fileInputStream ->
            val classFile = ClassFile(DataInputStream(fileInputStream))

            // Check BoxStore field exists and is accessible.
            val boxStoreField = classFile.fields.find { it.name == ClassConst.boxStoreFieldName }
            assertThat(boxStoreField).isNotNull()
            assertThat(Modifier.isPrivate(boxStoreField!!.accessFlags)).isFalse()
            assertThat(Modifier.isTransient(boxStoreField.accessFlags)).isTrue()

            // Check the constructor initializes the (lateinit) relation properties.
            val constructors = classFile.methods.filter { it.isConstructor }
            assertThat(constructors).hasSize(1)
            assertThat(classFile.getInitializedFields(constructors[0]))
                .containsExactly("toOneProperty", "toManyProperty")
        }

        // Check Cursor is transformed.
        File(transformDir, "com/example/KotlinEntityCursor.class").inputStream().use { fileInputStream ->
            val pool: ClassPool = ClassPool.getDefault()
            val ctClass: CtClass = pool.makeClass(fileInputStream)
            val attachMethod = ctClass.declaredMethods.first { it.name == ClassConst.cursorAttachEntityMethodName }
            assertThat(attachMethod.assignsBoxStoreField()).isTrue()
        }
    }

}
