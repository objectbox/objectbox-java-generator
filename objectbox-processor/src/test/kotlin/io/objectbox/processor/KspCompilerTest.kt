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

package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import io.objectbox.generator.model.PropertyType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Verifies the [KspCompiler] test infrastructure itself: running the KSP processor on Kotlin and
 * Java sources without a Gradle build.
 */
class KspCompilerTest {

    @JvmField
    @Rule
    val tempFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun modelPathOptions(): Pair<java.io.File, Map<String, String>> {
        val modelFile = tempFolder.newFolder("objectbox-models").resolve("default.json")
        return modelFile to mapOf(
            ObjectBoxProcessor.OPTION_MODEL_PATH to modelFile.absolutePath,
            ObjectBoxProcessor.OPTION_DEBUG to "true"
        )
    }

    @Test
    fun kotlinAndJavaSources_processedTogether() {
        val (modelFile, options) = modelPathOptions()
        val compiler = KspCompiler()

        val kotlinEntity = KspCompiler.SourceFile.kotlin(
            "KotlinExample.kt",
            """
            package example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            data class KotlinExample(
                @Id var id: Long = 0,
                var text: String? = null,
                var count: Int = 0
            )
            """.trimIndent()
        )
        val javaEntity = KspCompiler.SourceFile.java(
            "example.JavaExample",
            """
            package example;

            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;

            @Entity
            public class JavaExample {
                @Id long id;
                String note;
            }
            """.trimIndent()
        )

        val compilation = compiler.compile(listOf(kotlinEntity, javaEntity), options)

        assertThat(compilation.errors).isEmpty()
        assertThat(compilation.succeeded).isTrue()

        // Schema is accessible and contains both entities with expected properties.
        val schema = compiler.processor!!.schema!!
        assertThat(schema.entities.map { it.className }).containsExactly("JavaExample", "KotlinExample")
        val kotlinExample = schema.entities.single { it.className == "KotlinExample" }
        val textProperty = kotlinExample.properties.single { it.propertyName == "text" }
        assertThat(textProperty.propertyType).isEqualTo(PropertyType.String)
        val countProperty = kotlinExample.properties.single { it.propertyName == "count" }
        assertThat(countProperty.propertyType).isEqualTo(PropertyType.Int)
        assertThat(countProperty.isTypeNotNull).isTrue()

        // Code is generated for both entities and the model file is written.
        assertThat(compilation.generatedSourceFileOrNull("example.MyObjectBox")).isNotNull()
        assertThat(compilation.generatedSourceFileOrNull("example.KotlinExample_")).isNotNull()
        assertThat(compilation.generatedSourceFileOrNull("example.JavaExampleCursor")).isNotNull()
        assertThat(modelFile.exists()).isTrue()
        assertThat(modelFile.readText()).contains("\"name\": \"KotlinExample\"")
    }

    @Test
    fun javaSourceFromResource_processed() {
        val (_, options) = modelPathOptions()
        val compiler = KspCompiler()

        val compilation = compiler.compile(
            listOf(KspCompiler.SourceFile.javaResource("SimpleKotlinEntity")),
            options
        )

        assertThat(compilation.errors).isEmpty()
        assertThat(compilation.succeeded).isTrue()
        val schema = compiler.processor!!.schema!!
        assertThat(schema.entities.single().className).isEqualTo("SimpleKotlinEntity")
    }

    @Test
    fun entityWithoutId_failsWithError() {
        val (_, options) = modelPathOptions()
        val compiler = KspCompiler()

        val entity = KspCompiler.SourceFile.kotlin(
            "NoId.kt",
            """
            package example

            import io.objectbox.annotation.Entity

            @Entity
            class NoId {
                var text: String? = null
            }
            """.trimIndent()
        )

        val compilation = compiler.compile(listOf(entity), options)

        assertThat(compilation.succeeded).isFalse()
        assertThat(compilation.errors.single())
            .contains("No @Id property found for 'NoId', add @Id on a not-null long property.")
    }

}
