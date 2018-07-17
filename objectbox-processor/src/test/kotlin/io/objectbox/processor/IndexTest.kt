package io.objectbox.processor

import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.model.PropertyType
import io.objectbox.model.PropertyFlags
import org.junit.Test


/**
 * Tests related to the @Index annotation.
 */
class IndexTest : BaseProcessorTest() {

    @Test
    fun index_type_autoDetectAsExpected() {
        val entity = "IndexAutoDetect"

        val environment = TestEnvironment("index-auto-detect-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{
                if (it.isPrimaryKey) {
                    return@propLoop
                }
                assertWithMessage("${it.propertyName} should have index").that(it.index).isNotNull()

                // assert index has expected type
                val expectedIndexType = if (it.propertyType == PropertyType.String
                        || it.propertyType == PropertyType.ByteArray) {
                    PropertyFlags.INDEX_HASH // 2048
                } else {
                    PropertyFlags.INDEXED // 8
                }
                assertWithMessage("${it.propertyName} index type is wrong")
                        .that(it.index.type)
                        .isEqualTo(expectedIndexType)
            }
        }
    }

    @Test
    fun index_type_ifSetOverridesDefault() {
        val entity = "IndexTypeOverride"

        val environment = TestEnvironment("index-override-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{
                if (it.isPrimaryKey) {
                    return@propLoop
                }
                assertWithMessage("${it.propertyName} should have index").that(it.index).isNotNull()

                // assert index type is overridden from default type
                // (switched VALUE with HASH and vice versa for all)
                val expectedIndexType = if (it.propertyType == PropertyType.String
                        || it.propertyType == PropertyType.ByteArray) {
                    PropertyFlags.INDEXED // 8
                } else {
                    PropertyFlags.INDEX_HASH // 2048
                }
                assertWithMessage("${it.propertyName} index type is wrong")
                        .that(it.index.type)
                        .isEqualTo(expectedIndexType)
            }
        }
    }

    @Test
    fun index_maxLength_isPickedUp() {
        val entity = "IndexMaxLength"

        val environment = TestEnvironment("index-max-length-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{
                if (it.isPrimaryKey) {
                    return@propLoop
                }
                assertWithMessage("${it.propertyName} should have index").that(it.index).isNotNull()

                // assert index has max length default or set value
                val expectedMaxValueLength = when (it.propertyName) {
                    "byteArrayProp" -> 42
                    else -> 0 // default
                }
                assertWithMessage("${it.propertyName} index max value length is wrong")
                        .that(it.index.maxValueLength)
                        .isEqualTo(expectedMaxValueLength)
            }
        }
    }

    @Test
    fun index_maxLength_failsIfValueNegative() {
        val entity = "IndexMaxLengthFailValue"

        val environment = TestEnvironment("index-max-length-illegal-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).failed()
        // TODO assert error message
    }

    @Test
    fun index_maxLength_failsIfWrongProp() {
        val entity = "IndexMaxLengthFailProp"

        val environment = TestEnvironment("index-max-length-fail2-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).failed()
        // TODO assert error message
    }

    @Test
    fun index_maxLength_failsIfWrongType() {
        val entity = "IndexMaxLengthFailType"

        val environment = TestEnvironment("index-max-length-fail1-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).failed()
        // TODO assert error message
    }

    @Test
    fun index_typeAndMaxLength_generatedCodeFlagsMatch() {
        val entity = "IndexGenerated"

        val environment = TestEnvironment("index-generated-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val generatedFile = CompilationSubject.assertThat(compilation)
                .generatedSourceFile("io.objectbox.processor.test.MyObjectBox")
        generatedFile.isNotNull()
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/MyObjectBox-index.java"))
    }
}