/*
 * ObjectBox Build Tools
 * Copyright (C) 2019-2024 ObjectBox Ltd.
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

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.model.PropertyType
import org.junit.Test

/**
 * Additional tests for the Type annotation. For basic tests see SimpleEntity test in ObjectBoxProcessorTest.
 */
class TypeTest : BaseProcessorTest() {

    @Test
    fun typeAnnotation_withConverter_supported() {
        val entitySource = """
        package com.example;
        import io.objectbox.annotation.Convert;
        import io.objectbox.annotation.DatabaseType;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Type;
        import io.objectbox.converter.PropertyConverter;
        import io.objectbox.model.PropertyType;
        import java.time.Instant;

        @Entity
        public class Example {
            @Id long id;
            
            @Convert(converter = InstantConverter.class, dbType = Long.class)
            @Type(DatabaseType.DateNano)
            Instant customDateNano;
            
            public static class InstantConverter implements PropertyConverter<Instant, Long> {
                @Override
                public Instant convertToEntityProperty(Long databaseValue) {
                    return null;
                }
        
                @Override
                public Long convertToDatabaseValue(Instant entityProperty) {
                    return 0L;
                }
            }
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        val environment = TestEnvironment("type-custom-datenano.json", useTemporaryModelFile = true)

        environment.compile(listOf(entitySource))
            .assertThatIt { succeededWithoutWarnings() }

        // Assert schema.
        val schema = environment.schema
        Truth.assertThat(schema).isNotNull()
        val schemaEntity = schema.entities[0]
        val schemaPropertyDateNano = schemaEntity.properties.find { it.propertyName == "customDateNano" }
        Truth.assertThat(schemaPropertyDateNano).isNotNull()
        assertType(schemaPropertyDateNano!!, PropertyType.DateNano, hasNonPrimitiveFlag = true)
        Truth.assertThat(schemaPropertyDateNano.customType).isEqualTo("java.time.Instant")

        // Assert model file.
        val model = environment.readModel()
        val modelEntity = model.findEntity("Example", null)
        Truth.assertThat(modelEntity).isNotNull()
        val modelPropertyDateNano = modelEntity!!.properties.find { it.name == "customDateNano" }
        Truth.assertThat(modelPropertyDateNano).isNotNull()
        Truth.assertThat(modelPropertyDateNano!!.type).isEqualTo(io.objectbox.model.PropertyType.DateNano)
    }

    @Test
    fun typeAnnotation_notLong_unsupported() {
        val javaFileObject = """
        package com.example;
        import io.objectbox.annotation.DatabaseType;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Type;

        @Entity
        public class Example {
            @Id long id;
            @Type(DatabaseType.DateNano) int notLong;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        TestEnvironment("type-not-long.json", useTemporaryModelFile = true)
            .compile(listOf(javaFileObject))
            .assertThatIt {
                hadErrorContaining("@Type(DateNano) only supports properties with type Long")
            }
    }

}