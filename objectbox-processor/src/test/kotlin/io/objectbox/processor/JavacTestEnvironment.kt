/*
 * ObjectBox Build Tools
 * Copyright (C) 2018-2026 ObjectBox Ltd.
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

import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.model.Schema

/**
 * [TestEnvironment] that runs the annotation processor using javac of
 * the compile-testing library (https://github.com/google/compile-testing).
 */
class JavacTestEnvironment(
    modelFile: String,
    myObjectBoxPackage: String? = null,
    optionDisableTransform: Boolean = false,
    useTemporaryModelFile: Boolean = false
) : TestEnvironment(modelFile, myObjectBoxPackage, optionDisableTransform, useTemporaryModelFile) {

    val processor = ObjectBoxProcessorShim()

    override val schema: Schema
        get() = processor.schema!!

    private fun processorOptionsAsCompilerArguments(): List<String> {
        return processorOptions().map { (key, value) -> "-A$key=$value" }
    }

    override fun compileSources(files: List<TestSourceFile>): TestCompilation {
        val javaFileObjects = files.map {
            check(!it.isKotlin) { "The annotation processor only supports Java sources (${it.fullyQualifiedName})." }
            JavaFileObjects.forSourceString(it.fullyQualifiedName, it.source)
        }
        val compilation = Compiler.javac()
            .withProcessors(processor)
            .withOptions(processorOptionsAsCompilerArguments())
            .compile(javaFileObjects)
        return JavacTestCompilation(compilation)
    }

}
