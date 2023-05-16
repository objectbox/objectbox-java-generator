package io.objectbox.processor

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjectSubject
import com.google.testing.compile.JavaFileObjects


/**
 * Asserts the given class exists and the source tree matches the given one.
 *
 * Use like:
 * ```kotlin
 * @Language("Java")
 * val source =
 *     """
 *     package com.example;
 *
 *     public class Example {}
 *     """.trimIndent()
 * generatedSourceFileMatches("com.example.Example", source)
 * ```
 */
fun CompilationSubject.generatedSourceFileMatches(fullyQualifiedName: String, source: String) {
    generatedSourceFile(fullyQualifiedName).also {
        it.isNotNull()
        it.hasSourceEquivalentTo(JavaFileObjects.forSourceString(fullyQualifiedName, source))
    }
}

fun Compilation.generatedSourceFileOrFail(qualifiedName: String): JavaFileObjectSubject {
    val generatedFile = CompilationSubject
        .assertThat(this)
        .generatedSourceFile(qualifiedName)
    generatedFile.isNotNull()
    return generatedFile
}

fun Compilation.assertGeneratedSourceMatches(qualifiedName: String, fileName: String): Compilation {
    generatedSourceFileOrFail(qualifiedName)
        .hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/$fileName"))
    return this
}

/**
 * Assumes type is in "io.objectbox.processor.test" package and file is named "$simpleName.java".
 */
fun Compilation.assertGeneratedSourceMatches(simpleName: String): Compilation {
    assertGeneratedSourceMatches(
        "io.objectbox.processor.test.$simpleName",
        "$simpleName.java"
    )
    return this
}

fun Compilation.assertThatIt(block: CompilationSubject.() -> Unit): Compilation {
    block(CompilationSubject.assertThat(this))
    return this
}