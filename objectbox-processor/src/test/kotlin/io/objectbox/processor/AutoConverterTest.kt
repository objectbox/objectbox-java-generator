package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationSubject
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
        }
        """.trimIndent()
            .let { JavaFileObjects.forSourceString("com.example.MapEntity", it) }

        val environment = TestEnvironment("auto-convert-java-string-map.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .also { CompilationSubject.assertThat(it).succeededWithoutWarnings() }


        environment.schema.entities[0].properties.find { it.dbName == "stringMap" }!!
            .run {
                assertThat(propertyType).isEqualTo(PropertyType.ByteArray)

                assertThat(converter).isEqualTo("io.objectbox.converter.StringMapConverter")
                assertThat(converterClassName).isEqualTo("StringMapConverter")

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
            
            Map<String, Integer> otherMap;
        }
        """.trimIndent()
            .let { JavaFileObjects.forSourceString("com.example.MapEntity", it) }

        val environment = TestEnvironment("auto-convert-java-other-map.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .also {
                CompilationSubject.assertThat(it).failed()
                CompilationSubject.assertThat(it).hadErrorContaining(
                    "Field type \"java.util.Map<java.lang.String,java.lang.Integer>\" is not supported."
                )
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

}