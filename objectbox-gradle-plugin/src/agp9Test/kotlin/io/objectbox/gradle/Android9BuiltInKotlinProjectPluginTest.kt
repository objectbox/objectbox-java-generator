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
import io.objectbox.gradle.transform.ClassConst
import io.objectbox.gradle.transform.assignsBoxStoreField
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
 * Tests assembling an Android project with an entity class written in Kotlin using Android Plugin 9 and its
 * built-in Kotlin support (so without the Kotlin Android plugin).
 *
 * Notably expects that the plugin applies the legacy-kapt plugin of the Android Plugin (available on the plugin
 * classpath for this test, see the build script of this project) so the processor runs on the Kotlin entity class.
 */
class Android9BuiltInKotlinProjectPluginTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val buildTransformDirectory =
        "build/intermediates/classes/debug/transformDebugClassesWithAsm/dirs"

    @Test
    fun assemble() {
        val gradleRunner = GradleTestRunner(testProjectDir)
            .apply {
                // Note: classpath for plugins configured in build script of this project (see GradleTestRunner.build).
                additionalPlugins += "com.android.application"
                // Configure the android block instead (new DSL of Android Plugin 9).
                javaBlock = ""
                additionalBlocks =
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
            }

        testProjectDir.newFile("src/main/AndroidManifest.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application>
                </application>
            </manifest>
            """.trimIndent()
        )

        @Language("kotlin")
        val exampleEntitySource =
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.relation.ToMany
            import io.objectbox.relation.ToOne

            @Entity
            class KotlinEntity {
                @Id
                var id: Long = 0
                var text: String? = null
                lateinit var toOneProperty: ToOne<KotlinEntity>
                lateinit var toManyProperty: ToMany<KotlinEntity>
            }
            """.trimIndent()

        gradleRunner.addKotlinSourceFile("KotlinEntity.kt", exampleEntitySource)

        val result = gradleRunner.build(listOf("--stacktrace", "assembleDebug")) {
            it.forwardOutput()
            // Use the latest version of Gradle (other tests cover the lowest working versions).
            it.withGradleVersion("9.6.1")
        }
        assertThat(result.task(":assembleDebug")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val transformDir = File(testProjectDir.root, buildTransformDirectory)

        // Check the Kotlin entity is transformed.
        File(transformDir, "com/example/KotlinEntity.class").inputStream().use { fileInputStream ->
            val classFile = ClassFile(DataInputStream(fileInputStream))

            // Check BoxStore field exists and is accessible.
            val boxStoreField = classFile.fields.find { it.name == ClassConst.boxStoreFieldName }
            assertThat(boxStoreField).isNotNull()
            assertThat(Modifier.isPrivate(boxStoreField!!.accessFlags)).isFalse()
            assertThat(Modifier.isTransient(boxStoreField.accessFlags)).isTrue()
        }

        // Check the Cursor class exists (so the processor did run on the Kotlin entity) and is transformed.
        File(transformDir, "com/example/KotlinEntityCursor.class").inputStream().use { fileInputStream ->
            val pool: ClassPool = ClassPool.getDefault()
            val ctClass: CtClass = pool.makeClass(fileInputStream)
            val attachMethod = ctClass.declaredMethods.first { it.name == ClassConst.cursorAttachEntityMethodName }
            assertThat(attachMethod.assignsBoxStoreField()).isTrue()
        }
    }

}
