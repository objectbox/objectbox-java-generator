package io.objectbox.processor

import com.google.common.truth.Truth
import com.google.testing.compile.CompilationSubject
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

        val compilation = environment.compile(listOf(entitySource))
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

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

        val environment = TestEnvironment("type-not-long.json", useTemporaryModelFile = true)

        val compilation = environment.compile(listOf(javaFileObject))
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("@Type(DateNano) only supports properties with type Long")
    }

}