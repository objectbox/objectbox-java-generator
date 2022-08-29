package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.model.PropertyType
import org.junit.Test


class DefaultValueTest : BaseProcessorTest() {

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
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.DefaultValueEntity", it)
        }

        val environment = TestEnvironment("default-value-empty-string.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
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
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.DefaultValueEntity", it)
        }

        val environment = TestEnvironment("default-value-not-empty-string.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
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
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.DefaultValueEntity", it)
        }

        val environment = TestEnvironment("default-value-prop-not-string.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
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
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.DefaultValueEntity", it)
        }

        val environment = TestEnvironment("default-value-convert.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .assertThatIt {
                failed()
                hadErrorContaining("Can not use both @Convert and @DefaultValue.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

}