package io.objectbox.processor

import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.junit.Test

class ObjectBoxProcessorTest {

    @Test
    fun testProcessor() {
        val file = JavaFileObjects.forResource("SimpleEntity.java")
        //        JavaFileObject file = JavaFileObjects.forSourceString("HelloWorld",
        //                "final class HelloWorld {}"
        //        );
        val compilation = javac()
                .withProcessors(ObjectBoxProcessorShim())
                .compile(file)
        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
                .hadNoteContaining("Processing @Entity annotation.")
                .inFile(file)
    }

}
