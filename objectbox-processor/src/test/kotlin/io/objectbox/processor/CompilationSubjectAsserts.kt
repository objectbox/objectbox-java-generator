/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2025 ObjectBox Ltd.
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

import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjectSubject
import com.google.testing.compile.JavaFileObjects


/**
 * Asserts the given class exists and the source tree matches the given one.
 *
 * See [TestEnvironment] for an example.
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