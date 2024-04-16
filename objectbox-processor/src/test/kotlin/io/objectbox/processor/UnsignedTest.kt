/*
 * ObjectBox Build Tools
 * Copyright (C) 2019-2024 ObjectBox Ltd.
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
import io.objectbox.model.PropertyFlags
import org.junit.Test


class UnsignedTest : BaseProcessorTest() {

    @Test
    fun unsigned_detectedAndGeneratesFlag() {
        val javaFileObject = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Unsigned;
        
        @Entity
        public class Example {
            @Id long id;
            @Unsigned int unsigned;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        // Assert generated MyObjectBox file.
        // Need stable model file + ids to verify sources match.
        TestEnvironment("unsigned.json").run {
            compile(listOf(javaFileObject))
                .assertThatIt {
                    succeededWithoutWarnings()
                    generatedSourceFile("com.example.MyObjectBox").run {
                        isNotNull()
                        hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/MyObjectBox-unsigned.java"))
                    }
                }
        }

        // Assert model file, ensure it is re-created on each run.
        val environment = TestEnvironment("unsigned.json", useTemporaryModelFile = true)
        environment.compile(listOf(javaFileObject))
            .assertThatIt { succeededWithoutWarnings() }

        val model = environment.readModel()
        val entity = model.findEntity("Example", null)!!
        val property = entity.properties.find { it.name == "unsigned" }!!
        assertThat(property.flags).isEqualTo(PropertyFlags.UNSIGNED)
    }

    @Test
    fun unsigned_isId_notSupported() {
        val javaFileObject = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Unsigned;
        
        @Entity
        public class Example {
            @Id @Unsigned long id;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        TestEnvironment("not-generated.json", useTemporaryModelFile = true)
            .compile(listOf(javaFileObject))
            .assertThatIt {
                failed()
                hadErrorContaining("@Unsigned can not be used with @Id. ID properties are unsigned internally by default.")
            }
    }

    @Test
    fun unsigned_notInteger_notSupported() {
        val javaFileObject = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Unsigned;
        
        @Entity
        public class Example {
            @Id long id;
            @Unsigned String notAnInteger;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        TestEnvironment("not-generated.json", useTemporaryModelFile = true)
            .compile(listOf(javaFileObject))
            .assertThatIt {
                failed()
                hadErrorContaining("@Unsigned is only supported for integer properties.")
            }
    }
}