package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationSubject
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
            val compilation = compile(listOf(javaFileObject))
            CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
            CompilationSubject.assertThat(compilation).generatedSourceFile("com.example.MyObjectBox").run {
                isNotNull()
                hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/MyObjectBox-unsigned.java"))
            }
        }

        // Assert model file, ensure it is re-created on each run.
        val environment = TestEnvironment("unsigned.json", useTemporaryModelFile = true)
        val compilation = environment.compile(listOf(javaFileObject))
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

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

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        val compilation = environment.compile(listOf(javaFileObject))
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining(
            "@Unsigned can not be used with @Id. ID properties are unsigned internally by default."
        )
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

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        val compilation = environment.compile(listOf(javaFileObject))
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining(
            "@Unsigned is only supported for integer properties."
        )
    }
}