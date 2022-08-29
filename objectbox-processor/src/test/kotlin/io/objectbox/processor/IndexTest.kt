package io.objectbox.processor

import com.google.common.truth.Truth.assertWithMessage
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

        val environment = TestEnvironment("index-auto-detect.json", useTemporaryModelFile = true)

        environment.compile(entity)
            .assertThatIt { succeededWithoutWarnings() }

        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{ prop ->
                if (prop.isPrimaryKey) {
                    return@propLoop
                }
                assertWithMessage("${prop.propertyName} should have index").that(prop.index).isNotNull()

                // assert index has expected type
                val expectedIndexFlag = if (prop.propertyType == PropertyType.String
                        || prop.propertyType == PropertyType.ByteArray) {
                    PropertyFlags.INDEX_HASH // 2048
                } else {
                    PropertyFlags.INDEXED // 8
                }
                assertWithMessage("${prop.propertyName} index type is wrong")
                        .that(prop.index!!.indexFlags)
                        .isEqualTo(expectedIndexFlag)
            }
        }
    }

    @Test
    fun index_unsupportedProperties_failsWithError() {
        val entity = "IndexUnsupported"

        TestEnvironment("index-unsupported.json", useTemporaryModelFile = true)
            .compile(entity)
            .assertThatIt {
                failed()

                hadErrorContaining("@Id property is unique and indexed by default, remove @Index.")

                hadErrorContaining("@Index can not be used with a ToOne relation, remove @Index.")
                hadErrorContaining("@Index can not be used with a ToMany relation, remove @Index.")
                hadErrorContaining("@Index can not be used with a List relation, remove @Index.")

                hadErrorContaining("@Index is not supported for Float, remove @Index.")
                hadErrorContaining("@Index is not supported for Double, remove @Index.")
                hadErrorContaining("@Index is not supported for ByteArray, remove @Index.")
                hadErrorContaining("@Index is not supported for StringArray, remove @Index.")
            }
    }

    @Test
    fun index_hash_failsForUnsupportedTypes() {
        val entity = "IndexHashNotSupported"

        TestEnvironment("index-hash-not.json", useTemporaryModelFile = true)
            .compile(entity)
            .assertThatIt {
                failed()

                hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Boolean}.")
                hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Int}.")
                hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Long}.")
                hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Byte}.")
                hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Char}.")
                hadErrorContaining("IndexType.HASH is not supported for ${PropertyType.Date}.")
            }
    }

    @Test
    fun index_hash64_failsForUnsupportedTypes() {
        val entity = "IndexHash64NotSupported"

        TestEnvironment("index-hash64-not.json", useTemporaryModelFile = true)
            .compile(entity)
            .assertThatIt {
                failed()

                hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Boolean}.")
                hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Int}.")
                hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Long}.")
                hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Byte}.")
                hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Char}.")
                hadErrorContaining("IndexType.HASH64 is not supported for ${PropertyType.Date}.")
            }
    }

    @Test
    fun index_type_ifSetOverridesDefault() {
        val entity = "IndexTypeOverride"

        val environment = TestEnvironment("index-override.json", useTemporaryModelFile = true)

        environment.compile(entity)
            .assertThatIt { succeededWithoutWarnings() }

        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{ prop ->
                if (prop.isPrimaryKey) {
                    return@propLoop
                }
                assertWithMessage("${prop.propertyName} should have index").that(prop.index).isNotNull()

                // assert index type is overridden from default type
                val expectedIndexFlag = when (prop.propertyName) {
                    "valueProp" -> PropertyFlags.INDEXED // 8
                    "hash64Prop" -> PropertyFlags.INDEX_HASH64 // 4096
                    else -> {
                        Assert.fail("No mapping for property ${prop.propertyName}")
                        0
                    }
                }
                assertWithMessage("${prop.propertyName} index type is wrong")
                        .that(prop.index!!.indexFlags)
                        .isEqualTo(expectedIndexFlag)
            }
        }
    }

    @Test
    fun index_typeAndMaxLength_generatedCodeFlagsMatch() {
        val entity = "IndexGenerated"

        // need stable model file + ids to verify sources match
        val environment = TestEnvironment("index-generated.json")

        environment.compile(entity)
            .assertThatIt { succeededWithoutWarnings() }
            .assertGeneratedSourceMatches(
            "io.objectbox.processor.test.MyObjectBox",
            "MyObjectBox-index.java"
        )
    }
}