/*
 * ObjectBox Build Tools
 * Copyright (C) 2026 ObjectBox Ltd.
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
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjectSubject
import com.google.testing.compile.JavaFileObjects

/**
 * The result of running a processor with [TestEnvironment.compile], with assertions that work
 * for both the annotation processor and the KSP processor.
 *
 * See [TestEnvironment] for an example.
 */
interface TestCompilation {

    fun succeededWithoutWarnings()

    fun failed()

    fun hadErrorContaining(message: String)

    fun hadErrorCount(count: Int)

    fun hadErrorContainingMatch(pattern: String)

    /**
     * Asserts the given class was generated and its source tree matches the given source.
     */
    fun generatedSourceFileMatches(fullyQualifiedName: String, source: String)

    /**
     * Asserts the given class was generated and returns its content.
     */
    fun generatedSourceFileContent(qualifiedName: String): String

    /**
     * Asserts the given class was generated and its source tree matches the
     * given file in the expected-source test resources.
     */
    fun assertGeneratedSourceMatches(qualifiedName: String, fileName: String): TestCompilation

    /**
     * Assumes type is in "io.objectbox.processor.test" package and file is named "$simpleName.java".
     */
    fun assertGeneratedSourceMatches(simpleName: String): TestCompilation {
        return assertGeneratedSourceMatches(
            "io.objectbox.processor.test.$simpleName",
            "$simpleName.java"
        )
    }

    fun assertThatIt(block: TestCompilation.() -> Unit): TestCompilation {
        block(this)
        return this
    }

}

/**
 * [TestCompilation] based on a compile-testing [Compilation] of the annotation processor.
 */
class JavacTestCompilation(private val compilation: Compilation) : TestCompilation {

    private val subject: CompilationSubject
        get() = CompilationSubject.assertThat(compilation)

    override fun succeededWithoutWarnings() {
        subject.succeededWithoutWarnings()
    }

    override fun failed() {
        subject.failed()
    }

    override fun hadErrorContaining(message: String) {
        subject.hadErrorContaining(message)
    }

    override fun hadErrorCount(count: Int) {
        subject.hadErrorCount(count)
    }

    override fun hadErrorContainingMatch(pattern: String) {
        subject.hadErrorContainingMatch(pattern)
    }

    override fun generatedSourceFileMatches(fullyQualifiedName: String, source: String) {
        subject.generatedSourceFile(fullyQualifiedName).also {
            it.isNotNull()
            it.hasSourceEquivalentTo(JavaFileObjects.forSourceString(fullyQualifiedName, source))
        }
    }

    override fun generatedSourceFileContent(qualifiedName: String): String {
        val generatedFile = compilation.generatedSourceFile(qualifiedName)
        assertWithMessage("Expected generated source file for %s", qualifiedName)
            .that(generatedFile.isPresent).isTrue()
        return generatedFile.get().getCharContent(true).toString()
    }

    override fun assertGeneratedSourceMatches(qualifiedName: String, fileName: String): TestCompilation {
        val generatedFile = subject.generatedSourceFile(qualifiedName)
        generatedFile.isNotNull()
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/$fileName"))
        return this
    }

}

/**
 * [TestCompilation] based on a [KspCompiler.KspCompilation] of the KSP processor.
 *
 * Note: source assertions use the same source tree comparison as for the annotation processor.
 */
class KspTestCompilation(private val compilation: KspCompiler.KspCompilation) : TestCompilation {

    override fun succeededWithoutWarnings() {
        assertWithMessage("Expected success without warnings, but got messages:\n%s", messagesForDisplay())
            .that(compilation.succeeded && compilation.warnings.isEmpty()).isTrue()
    }

    override fun failed() {
        assertWithMessage("Expected failure, but succeeded with messages:\n%s", messagesForDisplay())
            .that(compilation.succeeded).isFalse()
    }

    override fun hadErrorContaining(message: String) {
        assertWithMessage("Expected an error containing '%s', but got messages:\n%s", message, messagesForDisplay())
            .that(compilation.errors.any { it.contains(message) }).isTrue()
    }

    override fun hadErrorCount(count: Int) {
        assertWithMessage("Expected %s errors, but got messages:\n%s", count, messagesForDisplay())
            .that(compilation.errors).hasSize(count)
    }

    override fun hadErrorContainingMatch(pattern: String) {
        assertWithMessage("Expected an error matching '%s', but got messages:\n%s", pattern, messagesForDisplay())
            .that(compilation.errors.any { Regex(pattern).containsMatchIn(it) }).isTrue()
    }

    override fun generatedSourceFileMatches(fullyQualifiedName: String, source: String) {
        generatedSourceOrFail(fullyQualifiedName)
            .hasSourceEquivalentTo(JavaFileObjects.forSourceString(fullyQualifiedName, source))
    }

    override fun assertGeneratedSourceMatches(qualifiedName: String, fileName: String): TestCompilation {
        generatedSourceOrFail(qualifiedName)
            .hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/$fileName"))
        return this
    }

    override fun generatedSourceFileContent(qualifiedName: String): String {
        val generatedFile = compilation.generatedSourceFileOrNull(qualifiedName)
        assertWithMessage("Expected generated source file for %s", qualifiedName)
            .that(generatedFile).isNotNull()
        return generatedFile!!.readText()
    }

    private fun generatedSourceOrFail(qualifiedName: String): JavaFileObjectSubject {
        val generatedFile = compilation.generatedSourceFileOrNull(qualifiedName)
        assertWithMessage("Expected generated source file for %s", qualifiedName)
            .that(generatedFile).isNotNull()
        return JavaFileObjectSubject.assertThat(
            JavaFileObjects.forSourceString(qualifiedName, generatedFile!!.readText())
        )
    }

    private fun messagesForDisplay(): String {
        // Leave out info messages (like debug output of the processor).
        return compilation.messages.filterNot { it.startsWith("info: ") || it.startsWith("logging: ") }
            .joinToString("\n")
            .ifEmpty { "(no messages)" }
    }

}
