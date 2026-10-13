/*
 * ObjectBox Build Tools
 * Copyright (C) 2020-2024 ObjectBox Ltd.
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
import org.junit.Test


class DefaultValueTest(processorType: ProcessorType) : BaseProcessorTest(processorType) {

    @Test
    fun defaultValue_emptyString_works() {
        val sourceFile = """
        package com.example;
        import io.objectbox.annotation.DefaultValue;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class DefaultValueEntity {
            @Id long id;
            
            @DefaultValue("") String nonNullString;
        }
        """.trimIndent()

        val environment = env("default-value-empty-string.json", useTemporaryModelFile = true)

        environment.addSourceFile("com.example.DefaultValueEntity", sourceFile)
        environment.compile()
            .assertThatIt { succeededWithoutWarnings() }

        val stringProperty = environment.schema.entities[0].properties.find { it.dbName == "nonNullString" }!!

        stringProperty.run {
            assertThat(propertyType).isEqualTo(PropertyType.String)

            assertThat(converter).isEqualTo("io.objectbox.converter.NullToEmptyStringConverter")
            assertThat(converterClassName).isEqualTo("NullToEmptyStringConverter")

            assertThat(customType).isEqualTo("java.lang.String")
            assertThat(customTypeClassName).isEqualTo("String")
        }
    }

    @Test
    fun defaultValue_notEmptyString_errors() {
        val sourceFile = """
        package com.example;
        import io.objectbox.annotation.DefaultValue;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class DefaultValueEntity {
            @Id long id;
            
            @DefaultValue("42") String nonNullString;
        }
        """.trimIndent()

        val environment = env("default-value-not-empty-string.json", useTemporaryModelFile = true)

        environment.addSourceFile("com.example.DefaultValueEntity", sourceFile)
        environment.compile()
            .assertThatIt {
                failed()
                hadErrorContaining("Only @DefaultValue(\"\") is supported.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun defaultValue_propertyNotString_errors() {
        val sourceFile = """
        package com.example;
        import io.objectbox.annotation.DefaultValue;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class DefaultValueEntity {
            @Id long id;
            
            @DefaultValue("") Integer nonNullInteger;
        }
        """.trimIndent()

        val environment = env("default-value-prop-not-string.json", useTemporaryModelFile = true)

        environment.addSourceFile("com.example.DefaultValueEntity", sourceFile)
        environment.compile()
            .assertThatIt {
                failed()
                hadErrorContaining("For @DefaultValue(\"\") property must be String.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun defaultValue_withConvert_errors() {
        val sourceFile = """
        package com.example;
        import io.objectbox.annotation.Convert;
        import io.objectbox.annotation.DefaultValue;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.converter.PropertyConverter;

        @Entity
        public class DefaultValueEntity {
            @Id long id;
            
            @Convert(dbType = String.class, converter = IgnoredConverter.class)
            @DefaultValue("")
            String nonNullString;
            
            public static class IgnoredConverter implements PropertyConverter<String, String> {
                @Override
                public String convertToEntityProperty(String databaseValue) {
                    return null;
                }

                @Override
                public String convertToDatabaseValue(String entityProperty) {
                    return null;
                }
            }
        }
        """.trimIndent()

        val environment = env("default-value-convert.json", useTemporaryModelFile = true)

        environment.addSourceFile("com.example.DefaultValueEntity", sourceFile)
        environment.compile()
            .assertThatIt {
                failed()
                hadErrorContaining("Can not use both @Convert and @DefaultValue.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

}