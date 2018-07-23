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
    fun index_unsupportedProperties_failsWithError() {
        val entity = "IndexUnsupported"

        val environment = TestEnvironment("index-unsupported-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index can not be used with @Id.")

        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index is not supported for Float.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index is not supported for Double.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index is not supported for ByteArray.")
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
    fun index_typeAndMaxLength_generatedCodeFlagsMatch() {
        val entity = "IndexGenerated"

        // need stable model file + ids to verify sources match
        val environment = TestEnvironment("index-generated.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val generatedFile = CompilationSubject.assertThat(compilation)
                .generatedSourceFile("io.objectbox.processor.test.MyObjectBox")
        generatedFile.isNotNull()
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/MyObjectBox-index.java"))
    }
}