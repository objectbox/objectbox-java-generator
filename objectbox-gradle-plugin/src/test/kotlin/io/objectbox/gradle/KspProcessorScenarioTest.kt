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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Characterizes the behavior of the KSP processor for Kotlin language constructs and the
 * supported annotations, including error cases, by building Kotlin JVM projects.
 *
 * Note: like KotlinKspCompileTest, this does not use the ObjectBox plugin to test the processor
 * in isolation.
 */
class KspProcessorScenarioTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun createRunner(): GradleTestRunner {
        return GradleTestRunner(testProjectDir).apply {
            applyObjectBoxPlugin = false
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

    private fun generatedSourceFile(fileName: String): File {
        return testProjectDir.root.resolve("build/generated/ksp/main/java/com/example/$fileName")
    }

    private fun modelFile(): File {
        return testProjectDir.root.resolve("objectbox-models/default.json")
    }

    @Test
    fun dataClassWithValProperties_usesAllArgsConstructor() {
        val runner = createRunner()
        runner.addSourceFile(
            "Note.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            data class Note(
                // Note: the @Id property must be var (the assigned ID is set), others can be val
                // as objects are created using the all-args constructor.
                @Id var id: Long = 0,
                val text: String?
            )
            """.trimIndent()
        )

        val result = runner.build(listOf("assemble"))

        assertThat(result.output).contains("[ObjectBox] Valid all-args constructor found")
        assertThat(generatedSourceFile("MyObjectBox.java").exists()).isTrue()
    }

    @Test
    fun isPrefixedLowercaseProperty_usesGetGetter() {
        val runner = createRunner()
        runner.addSourceFile(
            "Place.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            class Place {
                @Id var id: Long = 0
                // starts with "is", but not is-prefixed per Kotlin interop rules ("isl" is lower case)
                var island: String? = null
            }
            """.trimIndent()
        )

        runner.build(listOf("assemble"))

        assertThat(generatedSourceFile("PlaceCursor.java").readText())
            .contains("entity.getIsland()")
    }

    @Test
    fun jvmFieldProperty_usesFieldAccess() {
        val runner = createRunner()
        runner.addSourceFile(
            "Message.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            class Message {
                @Id var id: Long = 0
                @JvmField var text: String? = null
            }
            """.trimIndent()
        )

        runner.build(listOf("assemble"))

        assertThat(generatedSourceFile("MessageCursor.java").readText())
            .contains("entity.text")
    }

    @Test
    fun nameInDb_changesModelNames() {
        val runner = createRunner()
        runner.addSourceFile(
            "Person.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.annotation.NameInDb

            @Entity
            @NameInDb("CustomEntity")
            class Person {
                @Id var id: Long = 0
                @NameInDb("custom_name") var name: String? = null
            }
            """.trimIndent()
        )

        runner.build(listOf("assemble"))

        val modelJson = modelFile().readText()
        assertThat(modelJson).contains("\"name\": \"CustomEntity\"")
        assertThat(modelJson).contains("\"name\": \"custom_name\"")
        assertThat(generatedSourceFile("MyObjectBox.java").readText())
            .contains("entity(\"CustomEntity\")")
    }

    @Test
    fun transientAndNoBackingFieldProperties_areSkipped() {
        val runner = createRunner()
        runner.addSourceFile(
            "Config.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            class Config {
                @Id var id: Long = 0
                var kept: String? = null
                @io.objectbox.annotation.Transient var skippedObx: String? = null
                @Transient var skippedKotlin: String? = null
                val skippedComputed: String get() = "computed"
            }
            """.trimIndent()
        )

        runner.build(listOf("assemble"))

        val entityInfo = generatedSourceFile("Config_.java").readText()
        assertThat(entityInfo).contains("kept")
        assertThat(entityInfo).doesNotContain("skippedObx")
        assertThat(entityInfo).doesNotContain("skippedKotlin")
        assertThat(entityInfo).doesNotContain("skippedComputed")
    }

    @Test
    fun indexAndUnique_addIndexesToModel() {
        val runner = createRunner()
        runner.addSourceFile(
            "User.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.annotation.Index
            import io.objectbox.annotation.Unique

            @Entity
            class User {
                @Id var id: Long = 0
                @Index var indexed: String? = null
                @Unique var unique: String? = null
                @Index @Unique var both: Long = 0
            }
            """.trimIndent()
        )

        runner.build(listOf("assemble"))

        val modelJson = modelFile().readText()
        // All three properties get an index.
        assertThat(modelJson.split("\"indexId\"").size - 1).isEqualTo(3)
        // String properties use a hash index by default (2048), @Unique adds the unique flag (32).
        assertThat(modelJson).contains("\"flags\": 2048")
        assertThat(modelJson).contains("\"flags\": ${2048 + 32}")
        // Non-String properties use a value index (8) plus unique (32).
        assertThat(modelJson).contains("\"flags\": ${8 + 32}")
    }

    @Test
    fun convertAnnotation_usesConverter() {
        val runner = createRunner()
        runner.addSourceFile(
            "Painting.kt",
            """
            package com.example

            import io.objectbox.annotation.Convert
            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.converter.PropertyConverter

            @Entity
            class Painting {
                @Id var id: Long = 0
                @Convert(converter = ColorConverter::class, dbType = Int::class)
                var color: Color? = null
            }

            enum class Color(val id: Int) { RED(1), BLUE(2) }

            class ColorConverter : PropertyConverter<Color?, Int?> {
                override fun convertToEntityProperty(databaseValue: Int?): Color? {
                    return Color.values().find { it.id == databaseValue }
                }

                override fun convertToDatabaseValue(entityProperty: Color?): Int? {
                    return entityProperty?.id
                }
            }
            """.trimIndent()
        )

        runner.build(listOf("assemble"))

        val cursor = generatedSourceFile("PaintingCursor.java").readText()
        assertThat(cursor).contains("ColorConverter")
        // The database type of the converted property is int.
        assertThat(generatedSourceFile("Painting_.java").readText())
            .contains("int.class")
    }

    @Test
    fun typeDateNano_changesPropertyType() {
        val runner = createRunner()
        runner.addSourceFile(
            "Event.kt",
            """
            package com.example

            import io.objectbox.annotation.DatabaseType
            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.annotation.Type

            @Entity
            class Event {
                @Id var id: Long = 0
                @Type(DatabaseType.DateNano) var time: Long? = null
            }
            """.trimIndent()
        )

        runner.build(listOf("assemble"))

        assertThat(generatedSourceFile("MyObjectBox.java").readText())
            .contains("PropertyType.DateNano")
    }

    @Test
    fun vectorTypes_mapToVectorProperties() {
        val runner = createRunner()
        runner.addSourceFile(
            "Vectors.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            class Vectors {
                @Id var id: Long = 0
                var stringList: List<String>? = null
                var stringArray: Array<String>? = null
                var intArray: IntArray? = null
                var floatArray: FloatArray? = null
                var bytes: ByteArray? = null
            }
            """.trimIndent()
        )

        runner.build(listOf("assemble"))

        val entityInfo = generatedSourceFile("Vectors_.java").readText()
        // List<String> is stored like String[], but exposed as List.
        assertThat(entityInfo).contains("java.util.List.class, \"stringList\"")
        assertThat(entityInfo).contains("String[].class, \"stringArray\"")
        assertThat(entityInfo).contains("int[].class, \"intArray\"")
        assertThat(entityInfo).contains("float[].class, \"floatArray\"")
        assertThat(entityInfo).contains("byte[].class, \"bytes\"")
    }

    @Test
    fun mapProperty_usesBuiltInConverter() {
        val runner = createRunner()
        runner.addSourceFile(
            "Settings.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            class Settings {
                @Id var id: Long = 0
                var values: MutableMap<String, String>? = null
            }
            """.trimIndent()
        )

        val result = runner.build(listOf("assemble"))

        assertThat(result.output)
            .contains("Using io.objectbox.converter.StringMapConverter to convert map property 'values'")
        assertThat(generatedSourceFile("SettingsCursor.java").readText())
            .contains("StringMapConverter")
    }

    @Test
    fun anyProperty_usesFlexConverter() {
        val runner = createRunner()
        runner.addSourceFile(
            "Flexible.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            class Flexible {
                @Id var id: Long = 0
                var anything: Any? = null
            }
            """.trimIndent()
        )

        val result = runner.build(listOf("assemble"))

        assertThat(result.output)
            .contains("Using io.objectbox.converter.FlexObjectConverter to convert Object property 'anything'")
    }

    @Test
    fun baseEntityInheritance_detectsChain() {
        val runner = createRunner()
        runner.addSourceFile(
            "Inheritance.kt",
            """
            package com.example

            import io.objectbox.annotation.BaseEntity
            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @BaseEntity
            open class Base {
                @Id var id: Long = 0
            }

            @Entity
            class Sub : Base() {
                var extra: String? = null
            }
            """.trimIndent()
        )

        val result = runner.build(listOf("assemble"))

        assertThat(result.output).contains("[ObjectBox] Detected entity inheritance chain: Sub->Base")
        val entityInfo = generatedSourceFile("Sub_.java").readText()
        assertThat(entityInfo).contains("\"id\"")
        assertThat(entityInfo).contains("\"extra\"")
    }

    @Test
    fun backlink_createsToManyRelation() {
        val runner = createRunner()
        runner.addSourceFile(
            "Shop.kt",
            """
            package com.example

            import io.objectbox.annotation.Backlink
            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.relation.ToMany
            import io.objectbox.relation.ToOne

            @Entity
            class Shop {
                @Id var id: Long = 0
                @Backlink(to = "shop")
                lateinit var products: ToMany<Product>
            }

            @Entity
            class Product {
                @Id var id: Long = 0
                lateinit var shop: ToOne<Shop>
            }
            """.trimIndent()
        )

        runner.build(listOf("assemble"))

        val shopInfo = generatedSourceFile("Shop_.java").readText()
        assertThat(shopInfo).contains("\"products\"")
        assertThat(shopInfo).contains("RelationInfo")
        // The to-one gets a virtual target ID property.
        assertThat(generatedSourceFile("Product_.java").readText()).contains("\"shopId\"")
    }

    @Test
    fun uidOnEntityAndProperty_keepsModelIdentity() {
        val runner = createRunner()
        val sourceWithoutUid =
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.annotation.Uid

            @Entity
            class Stable {
                @Id var id: Long = 0
                var name: String? = null
            }
            """.trimIndent()
        val sourceFile = runner.addSourceFile("Stable.kt", sourceWithoutUid)

        // First build: UIDs are assigned and written to the model file.
        runner.build(listOf("assemble"))
        val modelJson = modelFile().readText()
        val entityUid = Regex("\"id\": \"1:(\\d+)\"").find(modelJson)!!.groupValues[1]
        val propertyUid = Regex("\"id\": \"2:(\\d+)\"").find(modelJson)!!.groupValues[1]

        // Second build: with @Uid annotations matching the assigned UIDs the model must not change.
        sourceFile.writeText(
            sourceWithoutUid
                .replace("class Stable", "@Uid(${entityUid}L)${'\n'}class Stable")
                .replace("var name", "@Uid(${propertyUid}L) var name")
        )
        runner.build(listOf("assemble"))

        assertThat(modelFile().readText()).isEqualTo(modelJson)
    }

    @Test
    fun missingIdProperty_failsWithMessage() {
        val runner = createRunner()
        runner.addSourceFile(
            "NoId.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity

            @Entity
            class NoId {
                var text: String? = null
            }
            """.trimIndent()
        )

        val result = runner.buildAndFail(listOf("assemble"))

        assertThat(result.output)
            .contains("No @Id property found for 'NoId', add @Id on a not-null long property.")
    }

    @Test
    fun unsupportedPropertyType_failsWithMessage() {
        val runner = createRunner()
        runner.addSourceFile(
            "Unsupported.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            class Unsupported {
                @Id var id: Long = 0
                var weird: StringBuilder? = null
            }
            """.trimIndent()
        )

        val result = runner.buildAndFail(listOf("assemble"))

        assertThat(result.output).contains("is not supported")
        assertThat(result.output).contains("using @Convert or @Transient on the field")
    }

    @Test
    fun delegatedProperty_failsWithMessage() {
        val runner = createRunner()
        runner.addSourceFile(
            "Delegated.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id

            @Entity
            class Delegated {
                @Id var id: Long = 0
                val expensive: String by lazy { "computed" }
            }
            """.trimIndent()
        )

        val result = runner.buildAndFail(listOf("assemble"))

        assertThat(result.output).contains("Delegated properties are not supported")
    }

    @Test
    fun relationInSuperEntity_failsWithMessage() {
        val runner = createRunner()
        runner.addSourceFile(
            "SuperRelation.kt",
            """
            package com.example

            import io.objectbox.annotation.BaseEntity
            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.relation.ToOne

            @BaseEntity
            open class BaseWithRelation {
                @Id var id: Long = 0
                lateinit var other: ToOne<WithRelation>
            }

            @Entity
            class WithRelation : BaseWithRelation()
            """.trimIndent()
        )

        val result = runner.buildAndFail(listOf("assemble"))

        assertThat(result.output).contains("A super class of an @Entity must not have a relation.")
    }

    @Test
    fun indexOnRelation_failsWithMessage() {
        val runner = createRunner()
        runner.addSourceFile(
            "IndexedRelation.kt",
            """
            package com.example

            import io.objectbox.annotation.Entity
            import io.objectbox.annotation.Id
            import io.objectbox.annotation.Index
            import io.objectbox.relation.ToOne

            @Entity
            class IndexedRelation {
                @Id var id: Long = 0
                @Index lateinit var other: ToOne<IndexedRelation>
            }
            """.trimIndent()
        )

        val result = runner.buildAndFail(listOf("assemble"))

        assertThat(result.output).contains("@Index can not be used with a ToOne relation, remove @Index.")
    }

}
