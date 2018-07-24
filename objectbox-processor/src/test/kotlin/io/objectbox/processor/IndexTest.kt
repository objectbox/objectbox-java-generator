package io.objectbox.processor

import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.model.PropertyType
import io.objectbox.model.PropertyFlags
import org.junit.Assert
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

        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index can not be used with a ToOne relation.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index can not be used with a ToMany relation.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index can not be used with a List relation.")

        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index is not supported for Float.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index is not supported for Double.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Index is not supported for ByteArray.")
    }

    @Test
    fun index_hash_failsForUnsupportedTypes() {
        val entity = "IndexHashNotSupported"

        val environment = TestEnvironment("index-hash-not-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Boolean}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Int}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Long}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Byte}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Char}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Date}.")
    }

    @Test
    fun index_hash64_failsForUnsupportedTypes() {
        val entity = "IndexHash64NotSupported"

        val environment = TestEnvironment("index-hash64-not-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Boolean}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Int}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Long}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Byte}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Char}.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Date}.")
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
                val expectedIndexType = when (it.propertyName) {
                    "valueProp" -> PropertyFlags.INDEXED // 8
                    "hash64Prop" -> PropertyFlags.INDEX_HASH64 // 4096
                    else -> {
                        Assert.fail("No mapping for property ${it.propertyName}")
                        0
                    }
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