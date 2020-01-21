package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationSubject
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

        val environment = TestEnvironment("not-generated.json")
        environment.cleanModelFile()

        val compilation = environment.compile(listOf(sourceFile))
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining(
            "No @Id property found for 'NoIdEntity', add @Id on a not-null long property."
        )
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

        val environment = TestEnvironment("not-generated.json")
        environment.cleanModelFile()

        val compilation = environment.compile(listOf(sourceFile))
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining(
            "An @Id property must be a not-null long."
        )
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

        val environment = TestEnvironment("not-generated.json")
        environment.cleanModelFile()

        val compilation = environment.compile(listOf(sourceFile))
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining(
            "An @Id property must not be private or have a not-private getter and setter."
        )
        assertThat(environment.isModelFileExists()).isFalse()
    }

}