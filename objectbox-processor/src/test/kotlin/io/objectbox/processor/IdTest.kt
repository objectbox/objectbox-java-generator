/*
 * ObjectBox Build Tools
 * Copyright (C) 2020-2024 ObjectBox Ltd.
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
import com.google.testing.compile.JavaFileObjects
import org.junit.Test

/**
 * Tests related to the @Id annotation.
 */
class IdTest : BaseProcessorTest() {

    @Test
    fun id_missing() {
        val sourceFile = """
        package io.objectbox.processor.test;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class NoIdEntity {
            long id;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("io.objectbox.processor.test.NoIdEntity", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .assertThatIt {
                failed()
                hadErrorContaining("No @Id property found for 'NoIdEntity', add @Id on a not-null long property.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun testIdNotLong() {
        // test that instead of just failing compilation, processor warns if @Id is not Long
        val sourceFile = """
        package io.objectbox.processor.test;
        import io.objectbox.annotation.Entity; import io.objectbox.annotation.Id;

        @Entity
        public class NotLongEntity {
            @Id String id;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("io.objectbox.processor.test.NotLongEntity", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .assertThatIt {
                failed()
                hadErrorContaining("An @Id property must be a not-null long.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun id_noAccess_shouldWarn() {
        val sourceFile = """
        package com.example.objectbox;
        import io.objectbox.annotation.Entity; import io.objectbox.annotation.Id;

        @Entity
        public class PrivateEntity {
            @Id private long id; // private + no getter or setter
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.objectbox.PrivateEntity", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(listOf(sourceFile))
            .assertThatIt {
                failed()
                hadErrorContaining("An @Id property must not be private or have a not-private getter and setter.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

}