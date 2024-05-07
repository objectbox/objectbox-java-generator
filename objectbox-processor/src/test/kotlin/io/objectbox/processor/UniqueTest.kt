/*
 * ObjectBox Build Tools
 * Copyright (C) 2018-2024 ObjectBox Ltd.
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
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.compile.JavaFileObjects
import org.junit.Test


class UniqueTest : BaseProcessorTest() {

    @Test
    fun unique_createsUniqueIndex() {
        val entity = "UniqueCreatesIndex"

        val environment = TestEnvironment("unique-creates-index.json", useTemporaryModelFile = true)

        environment.compile(entity)
            .assertThatIt { succeededWithoutWarnings() }

        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{ prop ->
                if (prop.isPrimaryKey) {
                    return@propLoop
                }

                // assert index is created
                assertWithMessage("${prop.propertyName} should have index")
                    .that(prop.index).isNotNull()

                // assert index is unique
                assertWithMessage("${prop.propertyName} index should be unique")
                    .that(prop.index!!.isUnique).isTrue()
                assertWithMessage("${prop.propertyName} on conflict flag should not be set")
                    .that(prop.index!!.isUniqueOnConflictReplace).isFalse()
            }
        }
    }

    @Test
    fun unique_unsupportedProperties_failsWithError() {
        val entity = "UniqueUnsupported"

        val environment = TestEnvironment("unique-unsupported.json", useTemporaryModelFile = true)

        environment.compile(entity)
            .assertThatIt {
                failed()

                hadErrorContaining("@Id property is unique and indexed by default, remove @Unique.")

                hadErrorContaining("@Unique can not be used with a ToOne relation, remove @Unique.")
                hadErrorContaining("@Unique can not be used with a ToMany relation, remove @Unique.")
                hadErrorContaining("@Unique can not be used with a List relation, remove @Unique.")

                hadErrorContaining("@Unique is not supported for Float, remove @Unique.")
                hadErrorContaining("@Unique is not supported for Double, remove @Unique.")
                hadErrorContaining("@Unique is not supported for ByteArray, remove @Unique.")
            }
    }

    @Test
    fun unique_andIndex_makesIndexUnique() {
        val entity = "UniqueAndIndex"

        val environment = TestEnvironment("unique-and-index.json", useTemporaryModelFile = true)

        environment.compile(entity)
            .assertThatIt { succeededWithoutWarnings() }

        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{ prop ->
                if (prop.isPrimaryKey) {
                    return@propLoop
                }

                // assert index is created
                assertWithMessage("${prop.propertyName} should have index").that(prop.index).isNotNull()

                if (prop.propertyName == "notUniqueProp") {
                    // assert index is non-unique
                    assertWithMessage("${prop.propertyName} index should not be unique")
                        .that(prop.index!!.isUnique).isFalse()
                } else {
                    // assert index is unique
                    assertWithMessage("${prop.propertyName} index should be unique")
                        .that(prop.index!!.isUnique).isTrue()
                }
                assertWithMessage("${prop.propertyName} on conflict flag should not be set")
                    .that(prop.index!!.isUniqueOnConflictReplace).isFalse()
            }
        }
    }

    @Test
    fun unique_onConflictReplaceOnMultiple_fails() {
        val sourceFile = """
        package com.example;
        
        import io.objectbox.annotation.ConflictStrategy;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Unique;
        
        @Entity
        public class Example {
        
            @Id public long id;
        
            @Unique(onConflict = ConflictStrategy.REPLACE)
            public long replace1;
            
            @Unique(onConflict = ConflictStrategy.REPLACE)
            public long replace2;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .assertThatIt {
                failed()
                hadErrorContaining(
                    "ConflictStrategy.REPLACE can only be used on a single property"
                )
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun unique_generatedCodeHasFlag() {
        val entity = "UniqueGenerated"

        // need stable model file + ids to verify sources match
        val environment = TestEnvironment("unique-generated.json")

        environment.compile(entity)
            .assertThatIt { succeededWithoutWarnings() }
            .assertGeneratedSourceMatches(
                "io.objectbox.processor.test.MyObjectBox",
                "MyObjectBox-unique.java"
            )
    }
}