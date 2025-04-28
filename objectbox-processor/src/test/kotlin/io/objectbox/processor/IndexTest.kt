/*
 * ObjectBox Build Tools
 * Copyright (C) 2018-2025 ObjectBox Ltd.
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
                    || prop.propertyType == PropertyType.ByteArray
                ) {
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
        TestEnvironment("index-unsupported.json", useTemporaryModelFile = true)
            .apply {
                addSourceFile(
                    fullyQualifiedName = "com.example.IndexUnsupported",
                    source =
                        """
                    package com.example;
            
                    import io.objectbox.annotation.Entity;
                    import io.objectbox.annotation.Id;
                    import io.objectbox.annotation.Index;
                    import io.objectbox.relation.ToOne;
                    import io.objectbox.relation.ToMany;
                    import java.util.Date;
                    import java.util.List;
            
                    @Entity
                    public class IndexUnsupported {
                        @Id @Index long id;
            
                        @Index ToOne<IndexUnsupported> toOne;
                        @Index ToMany<IndexUnsupported> toMany;
                        @Index List<IndexUnsupported> toManyList;
            
                        // byte[], float or double do not support @Index
                        @Index Float floatPropOrNull;
                        @Index float floatProp;
            
                        @Index Double doublePropOrNull;
                        @Index double doubleProp;
            
                        @Index boolean[] booleanArrayProp;
                        @Index byte[] byteArrayProp;
                        @Index short[] shortArrayProp;
                        @Index char[] charArrayProp;
                        @Index int[] intArrayProp;
                        @Index long[] longArrayProp;
                        @Index float[] floatArrayProp;
                        @Index double[] doubleArrayProp;
            
                        @Index String[] stringArrayProp;
                    }
                    """.trimIndent()
                )
            }
            .compile()
            .assertThatIt {
                failed()

                hadErrorContaining("@Id property is unique and indexed by default, remove @Index.")

                hadErrorContaining("@Index can not be used with a ToOne relation, remove @Index.")
                hadErrorContaining("@Index can not be used with a ToMany relation, remove @Index.")
                hadErrorContaining("@Index can not be used with a List relation, remove @Index.")

                hadErrorContaining("@Index is not supported for Float, remove @Index.")
                hadErrorContaining("@Index is not supported for Double, remove @Index.")

                hadErrorContaining("@Index is not supported for BooleanArray, remove @Index.")
                hadErrorContaining("@Index is not supported for ByteArray, remove @Index.")
                hadErrorContaining("@Index is not supported for ShortArray, remove @Index.")
                hadErrorContaining("@Index is not supported for CharArray, remove @Index.")
                hadErrorContaining("@Index is not supported for IntArray, remove @Index.")
                hadErrorContaining("@Index is not supported for LongArray, remove @Index.")
                hadErrorContaining("@Index is not supported for FloatArray, remove @Index.")
                hadErrorContaining("@Index is not supported for DoubleArray, remove @Index.")

                hadErrorContaining("@Index is not supported for StringArray, remove @Index.")
            }
    }

    @Test
    fun index_hash_failsForUnsupportedTypes() {
        TestEnvironment("index-hash-not.json", useTemporaryModelFile = true)
            .apply {
                addSourceFile(
                    fullyQualifiedName = "com.example.IndexHashNotSupported",
                    source =
                        """
                    package com.example;

                    import io.objectbox.annotation.Entity;
                    import io.objectbox.annotation.Id;
                    import io.objectbox.annotation.Index;
                    import io.objectbox.annotation.IndexType;

                    import java.util.Date;

                    @Entity
                    public class IndexHashNotSupported {

                        @Id long id;

                        // only String supports HASH, compare with https://docs.objectbox.io/advanced/custom-types
                        @Index(type = IndexType.HASH) Boolean boolPropOrNull;
                        @Index(type = IndexType.HASH) boolean boolProp;

                        @Index(type = IndexType.HASH) Integer intPropOrNull;
                        @Index(type = IndexType.HASH) int intProp;

                        @Index(type = IndexType.HASH) Long longPropOrNull;
                        @Index(type = IndexType.HASH) long longProp;

                        @Index(type = IndexType.HASH) Byte bytePropOrNull;
                        @Index(type = IndexType.HASH) byte byteProp;

                        @Index(type = IndexType.HASH) Character charPropOrNull;
                        @Index(type = IndexType.HASH) char charProp;

                        @Index(type = IndexType.HASH) Date dateProp;

                    }
                    """.trimIndent()
                )
            }
            .compile()
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
        TestEnvironment("index-hash64-not.json", useTemporaryModelFile = true)
            .apply {
                addSourceFile(
                    fullyQualifiedName = "com.example.IndexHash64NotSupported",
                    source =
                        """
                    package com.example;

                    import io.objectbox.annotation.Entity;
                    import io.objectbox.annotation.Id;
                    import io.objectbox.annotation.Index;
                    import io.objectbox.annotation.IndexType;

                    import java.util.Date;

                    @Entity
                    public class IndexHash64NotSupported {

                        @Id long id;

                        // only String supports HASH64, compare with https://docs.objectbox.io/advanced/custom-types
                        @Index(type = IndexType.HASH64) Boolean boolPropOrNull;
                        @Index(type = IndexType.HASH64) boolean boolProp;

                        @Index(type = IndexType.HASH64) Integer intPropOrNull;
                        @Index(type = IndexType.HASH64) int intProp;

                        @Index(type = IndexType.HASH64) Long longPropOrNull;
                        @Index(type = IndexType.HASH64) long longProp;

                        @Index(type = IndexType.HASH64) Byte bytePropOrNull;
                        @Index(type = IndexType.HASH64) byte byteProp;

                        @Index(type = IndexType.HASH64) Character charPropOrNull;
                        @Index(type = IndexType.HASH64) char charProp;

                        @Index(type = IndexType.HASH64) Date dateProp;

                    }
                    """.trimIndent()
                )
            }
            .compile()
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