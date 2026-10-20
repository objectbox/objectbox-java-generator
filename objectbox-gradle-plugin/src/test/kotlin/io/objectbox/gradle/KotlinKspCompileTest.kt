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
 * Compiles a Kotlin JVM project with Kotlin and Java entity classes through KSP, end-to-end.
 *
 * Asserts the KSP processor produces byte-identical output to the previously kapt based setup:
 * the expected resources of `kotlin-project` were generated using kapt (see KotlinTestEntities).
 *
 * Note: this deliberately does not use the ObjectBox plugin, the KSP plugin and processor
 * dependency are set up manually to test the processor in isolation.
 */
class KotlinKspCompileTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun createRunner(): GradleTestRunner {
        return GradleTestRunner(testProjectDir).apply {
            applyObjectBoxPlugin = false
            // Both resolved from the injected plugin classpath (see testPluginClasspath).
            additionalPlugins += Const.KOTLIN_JVM_PLUGIN_ID
            additionalPlugins += Const.KSP_PLUGIN_ID
            additionalBlocks += KotlinTestEntities.buildScriptJvmTargetBlock
            additionalBlocks +=
                """
                dependencies {
                    implementation "io.objectbox:objectbox-java:${Const.OBX_JAVA_VERSION}"
                    implementation "io.objectbox:objectbox-kotlin:${Const.OBX_JAVA_VERSION}"
                    ksp "io.objectbox:objectbox-processor:${Const.OBX_PLUGIN_VERSION}"
                }
                """.trimIndent()
            debugKspProcessor()
        }
    }

    @Test
    fun kotlinProject_ksp_generatesSameSourcesAsKapt() {
        val runner = createRunner()
        KotlinTestEntities.addEntitySources(runner)
        val modelFile = KotlinTestEntities.seedModelFile(testProjectDir)
        val seededModelJson = modelFile.readText()

        val result = runner.build(listOf("assemble", "--stacktrace"))

        assertThat(result.task(":kspKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":assemble")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // The model file must not be modified: all entities and properties resolve to existing UIDs.
        assertThat(modelFile.readText().replace("\r", ""))
            .isEqualTo(seededModelJson.replace("\r", ""))

        // Generated sources match the kapt generated ones exactly.
        assertGeneratedSourceMatches("MyObjectBox.java")
        assertGeneratedSourceMatches("Customer_.java")
        assertGeneratedSourceMatches("CustomerCursor.java")
        assertGeneratedSourceMatches("JavaNote_.java")
        assertGeneratedSourceMatches("JavaNoteCursor.java")
        for (fileName in listOf("Order_.java", "OrderCursor.java", "Item_.java", "ItemCursor.java")) {
            assertThat(generatedSourceFile(fileName).exists()).isTrue()
        }

        // The probe file used to find the project directory must not remain in resources.
        val resourcesDir = testProjectDir.root.resolve("build/generated/ksp/main/resources")
        val probeFiles = resourcesDir.listFiles()?.filter { it.name.startsWith("objectbox-probe") }
        assertThat(probeFiles ?: emptyList<File>()).isEmpty()
    }

    @Test
    fun kotlinProject_ksp_myObjectBoxPackageOption() {
        val runner = createRunner()
        runner.additionalBlocks +=
            """
            ksp {
                arg("objectbox.myObjectBoxPackage", "com.example.custom")
            }
            """.trimIndent()
        KotlinTestEntities.addEntitySources(runner)
        KotlinTestEntities.seedModelFile(testProjectDir)

        val result = runner.build(listOf("assemble"))

        assertThat(result.task(":kspKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val myObjectBox = testProjectDir.root
            .resolve("build/generated/ksp/main/java/com/example/custom/MyObjectBox.java")
        assertThat(myObjectBox.exists()).isTrue()
        assertThat(myObjectBox.readText()).contains("package com.example.custom;")
    }

    private fun generatedSourceFile(fileName: String): File {
        return testProjectDir.root.resolve("build/generated/ksp/main/java/com/example/$fileName")
    }

    private fun assertGeneratedSourceMatches(fileName: String) {
        val actualFile = generatedSourceFile(fileName)
        assertThat(actualFile.exists()).isTrue()
        val expected = KotlinTestEntities.readTestResource("kotlin-project/$fileName")
        assertThat(actualFile.readText().replace("\r", ""))
            .isEqualTo(expected.replace("\r", ""))
    }

}
