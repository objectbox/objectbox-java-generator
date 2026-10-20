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

import io.objectbox.generator.model.Schema

/**
 * [TestEnvironment] that runs the KSP processor using [KspCompiler].
 */
class KspTestEnvironment(
    modelFile: String,
    myObjectBoxPackage: String? = null,
    optionDisableTransform: Boolean = false,
    useTemporaryModelFile: Boolean = false
) : TestEnvironment(modelFile, myObjectBoxPackage, optionDisableTransform, useTemporaryModelFile) {

    private val kspCompiler = KspCompiler()

    override val schema: Schema
        get() = kspCompiler.processor!!.schema!!

    override fun compileSources(files: List<TestSourceFile>): TestCompilation {
        val kspSourceFiles = files.map {
            if (it.isKotlin) {
                KspCompiler.SourceFile.kotlin(it.fullyQualifiedName.substringAfterLast('.') + ".kt", it.source)
            } else {
                KspCompiler.SourceFile.java(it.fullyQualifiedName, it.source)
            }
        }
        val compilation = kspCompiler.compile(kspSourceFiles, processorOptions())
        return KspTestCompilation(compilation)
    }
}
