package io.objectbox.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

public class ObjectBoxProcessorTest {

    @Test
    public void testProcessor() {
        JavaFileObject file = JavaFileObjects.forResource("SimpleEntity.java");
//        JavaFileObject file = JavaFileObjects.forSourceString("HelloWorld",
//                "final class HelloWorld {}"
//        );
        Compilation compilation = javac()
                .withProcessors(new ObjectBoxProcessor())
                .compile(file);
        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation)
                .hadNoteContaining("Processing @Entity annotation.")
                .inFile(file);
    }

}
