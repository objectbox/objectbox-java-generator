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
import org.intellij.lang.annotations.Language
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests Kotlin entities with properties of inline value class types (like unsigned integer types).
 */
class InlineValueClassTest(processorType: ProcessorType) : BaseProcessorTest(processorType) {

    @Before
    fun assumeKsp() {
        assumeTrue("Kotlin sources are only supported by KSP test setup", processorType == ProcessorType.KSP)
    }

    /**
     * Tests generated code compiles when using [Kotlin inline value classes](https://kotlinlang.org/docs/inline-classes.html).
     * Notably for [Kotlin unsigned integer types](https://kotlinlang.org/docs/unsigned-integer-types.html).
     *
     * By default, their byte code getter names are "mangled". So they need to be annotated to
     * explicitly rename them to use them from Java.
     *
     * Note for unsigned integers: Kotlin just re-interprets the bits as unsigned, so must take care
     * when using through Java, e.g. regarding overflow.
     *
     * If this breaks, update [the integer property type docs](https://docs.objectbox.io/property-types#integers).
     *
     * [Original issue](https://github.com/objectbox/objectbox-java/issues/1170)
     */
    @Test
    fun inlineValueClasses() {
        @Language("kotlin")
        val source = """
        package com.example

        import io.objectbox.annotation.Entity
        import io.objectbox.annotation.Id
        import io.objectbox.annotation.Unsigned

        @Entity
        data class InlineValueClasses(
            @Id var id: Long = 0,
            // @Unsigned not required, but makes sense to be used: test both cases
            @get:JvmName("getUnsignedByte")
            var unsignedByte: UByte = 0u,
            @Unsigned
            @get:JvmName("getUnsignedShort")
            var unsignedShort: UShort = 0u,
            @Unsigned
            @get:JvmName("getUnsignedInt")
            var unsignedInt: UInt = 0u,
            @Unsigned
            @get:JvmName("getUnsignedLong")
            var unsignedLong: ULong = 0u,
            @get:JvmName("getCustomValue")
            var customValue: CustomValue = CustomValue(0)
        )

        @JvmInline
        value class CustomValue(val long: Long)
        """.trimIndent()

        env("inline-value-classes.json", useTemporaryModelFile = true).run {
            addKotlinSourceFile("com.example.InlineValueClasses", source)
            compile()
                .assertThatIt {
                    succeededWithoutWarnings()
                    // Assert the generated Cursor calls the renamed getters
                    assertGeneratedSourceMatches(
                        "com.example.InlineValueClassesCursor",
                        "InlineValueClassesCursor.java"
                    )
                }

            val entity = schema.entities.single()
            // Constructors with value class parameters are not callable from Java.
            assertThat(entity.hasAllArgsConstructor()).isFalse()
            for (prop in entity.properties) {
                // Value classes map to the type they compile to (like with KAPT,
                // where the annotation processor only sees the compiled types).
                when (prop.propertyName) {
                    "id" -> assertPrimitiveType(prop, PropertyType.Long)
                    "unsignedByte" -> assertPrimitiveType(prop, PropertyType.Byte)
                    "unsignedShort" -> assertPrimitiveType(prop, PropertyType.Short)
                    "unsignedInt" -> assertPrimitiveType(prop, PropertyType.Int)
                    "unsignedLong" -> assertPrimitiveType(prop, PropertyType.Long)
                    "customValue" -> assertPrimitiveType(prop, PropertyType.Long)
                    else -> throw AssertionError("Unexpected property ${prop.propertyName}")
                }
                if (prop.propertyName != "id") {
                    assertThat(prop.getterMethodName).isEqualTo("get" + prop.propertyName.replaceFirstChar { it.uppercase() })
                    assertThat(prop.isUnsigned).isEqualTo(prop.propertyName != "unsignedByte" && prop.propertyName != "customValue")
                }
            }
        }
    }

    @Test
    fun inlineValueClass_withoutJvmName_fails() {
        @Language("kotlin")
        val source = """
        package com.example

        import io.objectbox.annotation.Entity
        import io.objectbox.annotation.Id

        @Entity
        class Example(
            @Id var id: Long = 0,
            var unsignedInt: UInt = 0u
        )
        """.trimIndent()

        env("not-generated.json", useTemporaryModelFile = true)
            .apply { addKotlinSourceFile("com.example.Example", source) }
            .compile()
            .assertThatIt {
                failed()
                hadErrorContaining(
                    "The getter of a property with an inline value class type has a mangled name," +
                            " rename it like @get:JvmName(\"getUnsignedInt\")."
                )
            }
    }

    @Test
    fun inlineValueClass_nullable_fails() {
        @Language("kotlin")
        val source = """
        package com.example

        import io.objectbox.annotation.Entity
        import io.objectbox.annotation.Id

        @Entity
        class Example(
            @Id var id: Long = 0,
            @get:JvmName("getUnsignedInt")
            var unsignedInt: UInt? = null
        )
        """.trimIndent()

        env("not-generated.json", useTemporaryModelFile = true)
            .apply { addKotlinSourceFile("com.example.Example", source) }
            .compile()
            .assertThatIt {
                failed()
                hadErrorContaining("Nullable inline value class types are not supported, use a not-null type.")
            }
    }

    @Test
    fun inlineValueClass_withoutBackingField_ignored() {
        @Language("kotlin")
        val source = """
        package com.example

        import io.objectbox.annotation.Entity
        import io.objectbox.annotation.Id

        @Entity
        class Example(
            @Id var id: Long = 0
        ) {
            // Does not compile to a field, so is not a property (and its mangled getter is no issue).
            val computed: UInt
                get() = id.toUInt()
        }
        """.trimIndent()

        env("not-generated.json", useTemporaryModelFile = true).run {
            addKotlinSourceFile("com.example.Example", source)
            compile()
                .assertThatIt { succeededWithoutWarnings() }
            assertThat(schema.entities.single().properties.map { it.propertyName }).containsExactly("id")
        }
    }
}
