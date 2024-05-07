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
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.model.PropertyType
import org.junit.Test


class AutoConverterTest : BaseProcessorTest() {

    @Test
    fun javaStringMap_isAutoConverted() {
        val sourceFile = """
        package com.example;
        import java.util.Map;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class MapEntity {
            @Id long id;
            
            Map<String, String> stringMap;
            Map<String, Object> stringFlexMap;
            Map<String, Long> stringLongMap;
        }
        """.trimIndent()
            .let { JavaFileObjects.forSourceString("com.example.MapEntity", it) }

        val environment = TestEnvironment("auto-convert-java-string-map.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .assertThatIt { succeededWithoutWarnings() }

        environment.schema.entities[0].properties.find { it.dbName == "stringMap" }!!
            .run {
                assertThat(propertyType).isEqualTo(PropertyType.Flex)

                assertThat(converter).isEqualTo("io.objectbox.converter.StringMapConverter")
                assertThat(converterClassName).isEqualTo("StringMapConverter")

                assertThat(customType).isEqualTo("java.util.Map")
                assertThat(customTypeClassName).isEqualTo("Map")
            }
        environment.schema.entities[0].properties.find { it.dbName == "stringFlexMap" }!!
            .run {
                assertThat(propertyType).isEqualTo(PropertyType.Flex)

                assertThat(converter).isEqualTo("io.objectbox.converter.StringFlexMapConverter")
                assertThat(converterClassName).isEqualTo("StringFlexMapConverter")

                assertThat(customType).isEqualTo("java.util.Map")
                assertThat(customTypeClassName).isEqualTo("Map")
            }
        environment.schema.entities[0].properties.find { it.dbName == "stringLongMap" }!!
            .run {
                assertThat(propertyType).isEqualTo(PropertyType.Flex)

                assertThat(converter).isEqualTo("io.objectbox.converter.StringLongMapConverter")
                assertThat(converterClassName).isEqualTo("StringLongMapConverter")

                assertThat(customType).isEqualTo("java.util.Map")
                assertThat(customTypeClassName).isEqualTo("Map")
            }
    }

    @Test
    fun javaIntegerMap_isAutoConverted() {
        val sourceFile = """
        package com.example;
        import java.util.Map;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class MapEntity {
            @Id long id;
            
            Map<Integer, String> integerFlexMap;
            Map<Integer, Long> integerLongMap;
        }
        """.trimIndent()
            .let { JavaFileObjects.forSourceString("com.example.MapEntity", it) }

        val environment = TestEnvironment("auto-convert-java-integer-map.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .assertThatIt { succeededWithoutWarnings() }

        environment.schema.entities[0].properties.find { it.dbName == "integerFlexMap" }!!
            .run {
                assertThat(propertyType).isEqualTo(PropertyType.Flex)

                assertThat(converter).isEqualTo("io.objectbox.converter.IntegerFlexMapConverter")
                assertThat(converterClassName).isEqualTo("IntegerFlexMapConverter")

                assertThat(customType).isEqualTo("java.util.Map")
                assertThat(customTypeClassName).isEqualTo("Map")
            }
        environment.schema.entities[0].properties.find { it.dbName == "integerLongMap" }!!
            .run {
                assertThat(propertyType).isEqualTo(PropertyType.Flex)

                assertThat(converter).isEqualTo("io.objectbox.converter.IntegerLongMapConverter")
                assertThat(converterClassName).isEqualTo("IntegerLongMapConverter")

                assertThat(customType).isEqualTo("java.util.Map")
                assertThat(customTypeClassName).isEqualTo("Map")
            }
    }

    @Test
    fun javaLongMap_isAutoConverted() {
        val sourceFile = """
        package com.example;
        import java.util.Map;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class MapEntity {
            @Id long id;
            
            Map<Long, Object> longFlexMap;
            Map<Long, Long> longLongMap;
        }
        """.trimIndent()
            .let { JavaFileObjects.forSourceString("com.example.MapEntity", it) }

        val environment = TestEnvironment("auto-convert-java-long-map.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .assertThatIt { succeededWithoutWarnings() }

        environment.schema.entities[0].properties.find { it.dbName == "longFlexMap" }!!
            .run {
                assertThat(propertyType).isEqualTo(PropertyType.Flex)

                assertThat(converter).isEqualTo("io.objectbox.converter.LongFlexMapConverter")
                assertThat(converterClassName).isEqualTo("LongFlexMapConverter")

                assertThat(customType).isEqualTo("java.util.Map")
                assertThat(customTypeClassName).isEqualTo("Map")
            }
        environment.schema.entities[0].properties.find { it.dbName == "longLongMap" }!!
            .run {
                assertThat(propertyType).isEqualTo(PropertyType.Flex)

                assertThat(converter).isEqualTo("io.objectbox.converter.LongLongMapConverter")
                assertThat(converterClassName).isEqualTo("LongLongMapConverter")

                assertThat(customType).isEqualTo("java.util.Map")
                assertThat(customTypeClassName).isEqualTo("Map")
            }
    }

    @Test
    fun javaOtherMap_errors() {
        val sourceFile = """
        package com.example;
        import java.util.Map;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class MapEntity {
            @Id long id;
            
            Map<Boolean, Integer> otherMap;
        }
        """.trimIndent()
            .let { JavaFileObjects.forSourceString("com.example.MapEntity", it) }

        val environment = TestEnvironment("auto-convert-java-other-map.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .assertThatIt {
                failed()
                hadErrorContaining(
                    "Field type \"java.util.Map<java.lang.Boolean,java.lang.Integer>\" is not supported."
                )
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

}