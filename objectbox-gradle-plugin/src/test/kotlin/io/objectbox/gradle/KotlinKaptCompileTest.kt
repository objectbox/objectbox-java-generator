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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Compiles a Kotlin JVM project with Kotlin and Java entity classes through kapt, end-to-end.
 *
 * This pins the current kapt based behavior (generated sources and model file of the
 * `kotlin-project` resources) for real Kotlin language constructs (data class with default
 * values, nullable types, is-prefixed property, ToOne/ToMany relations) and a Java entity, so
 * other processor implementations can be verified against it.
 */
class KotlinKaptCompileTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun createRunner(): GradleTestRunner {
        return GradleTestRunner(testProjectDir).apply {
            // Resolved from the injected plugin classpath (see testPluginClasspath).
            additionalPlugins += Const.KOTLIN_JVM_PLUGIN_ID
            additionalBlocks = KotlinTestEntities.buildScriptJvmTargetBlock + "\n" + """
                kapt {
                    arguments {
                        arg("objectbox.debug", "true")
                    }
                }
            """.trimIndent()
        }
    }

    @Test
    fun kotlinProject_kapt_generatesExpectedSourcesAndKeepsModelFile() {
        val runner = createRunner()
        KotlinTestEntities.addEntitySources(runner)
        val modelFile = KotlinTestEntities.seedModelFile(testProjectDir)
        val seededModelJson = modelFile.readText()

        val result = runner.build(listOf("assemble", "--stacktrace"))

        assertThat(result.task(":kaptKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":assemble")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // The model file must not be modified: all entities and properties resolve to existing UIDs.
        assertThat(modelFile.readText().replace("\r", ""))
            .isEqualTo(seededModelJson.replace("\r", ""))

        // Generated sources match the expected ones exactly (UIDs are fixed by the seeded model).
        assertGeneratedSourceMatches("MyObjectBox.java")
        assertGeneratedSourceMatches("Customer_.java")
        assertGeneratedSourceMatches("CustomerCursor.java")
        // Pins accessor-based code generation for a Java entity in a Kotlin project.
        assertGeneratedSourceMatches("JavaNote_.java")
        assertGeneratedSourceMatches("JavaNoteCursor.java")
        for (fileName in listOf("Order_.java", "OrderCursor.java", "Item_.java", "ItemCursor.java")) {
            assertThat(generatedSourceFile(fileName).exists()).isTrue()
        }
    }

    @Test
    fun kotlinProject_kapt_myObjectBoxPackageOption() {
        val runner = createRunner()
        runner.additionalBlocks += """

            kapt {
                arguments {
                    arg("objectbox.myObjectBoxPackage", "com.example.custom")
                }
            }
        """.trimIndent()
        KotlinTestEntities.addEntitySources(runner)
        KotlinTestEntities.seedModelFile(testProjectDir)

        val result = runner.build(listOf("assemble"))

        assertThat(result.task(":kaptKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val myObjectBox = testProjectDir.root
            .resolve("build/generated/source/kapt/main/com/example/custom/MyObjectBox.java")
        assertThat(myObjectBox.exists()).isTrue()
        assertThat(myObjectBox.readText()).contains("package com.example.custom;")
    }

    private fun generatedSourceFile(fileName: String): File {
        return testProjectDir.root.resolve("build/generated/source/kapt/main/com/example/$fileName")
    }

    private fun assertGeneratedSourceMatches(fileName: String) {
        val actualFile = generatedSourceFile(fileName)
        assertThat(actualFile.exists()).isTrue()
        val expected = KotlinTestEntities.readTestResource("kotlin-project/$fileName")
        assertThat(actualFile.readText().replace("\r", ""))
            .isEqualTo(expected.replace("\r", ""))
    }


}
