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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Compiles a Kotlin JVM project with Kotlin and Java entity classes using the Kotlin, KSP and
 * ObjectBox plugins, end-to-end: as the project has applied the KSP plugin, the ObjectBox plugin
 * is expected to add the processor to the KSP configuration (and not apply kapt) and add the
 * library dependencies.
 *
 * The expected output resources were originally generated with the kapt based setup (see
 * KotlinKaptCompileTest), which the KSP based setup must reproduce exactly.
 */
class KotlinPluginKspCompileTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun createRunner(): GradleTestRunner {
        return GradleTestRunner(testProjectDir).apply {
            // Resolved from the injected plugin classpath (see testPluginClasspath), so no version.
            additionalPlugins += "org.jetbrains.kotlin.jvm"
            // The project applies the KSP plugin before the ObjectBox plugin.
            additionalPlugins += "com.google.devtools.ksp"
            additionalBlocks = KotlinTestEntities.buildScriptJvmTargetBlock + "\n" + """
                ksp {
                    arg("objectbox.debug", "true")
                }
            """.trimIndent()
        }
    }

    @Test
    fun kotlinProject_userAppliedKsp_generatesExpectedSources() {
        val runner = createRunner()
        KotlinTestEntities.addEntitySources(runner)
        val modelFile = KotlinTestEntities.seedModelFile(testProjectDir)
        val seededModelJson = modelFile.readText()

        val result = runner.build(listOf("assemble", "--stacktrace"))

        // The plugin added the processor to the KSP configuration (and did not apply kapt), so KSP ran.
        assertThat(result.output).doesNotContain("Applied '${ProjectEnv.Const.KOTLIN_KAPT_PLUGIN_ID}'.")
        assertThat(result.task(":kaptKotlin")).isNull()
        assertThat(result.task(":kspKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":compileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":assemble")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // The model file must not be modified: all entities and properties resolve to existing UIDs.
        assertThat(modelFile.readText().replace("\r", ""))
            .isEqualTo(seededModelJson.replace("\r", ""))

        // Generated sources match the expected (originally kapt generated) ones exactly.
        assertGeneratedSourceMatches("MyObjectBox.java")
        assertGeneratedSourceMatches("Customer_.java")
        assertGeneratedSourceMatches("CustomerCursor.java")
        assertGeneratedSourceMatches("JavaNote_.java")
        assertGeneratedSourceMatches("JavaNoteCursor.java")
        for (fileName in listOf("Order_.java", "OrderCursor.java", "Item_.java", "ItemCursor.java")) {
            assertThat(generatedSourceFile(fileName).exists()).isTrue()
        }
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
