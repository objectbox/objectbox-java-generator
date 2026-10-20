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

package io.objectbox.processor.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import io.objectbox.generator.GeneratorOutput
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets

/**
 * [GeneratorOutput] that creates source files using the KSP [CodeGenerator], so they are
 * registered for incremental processing and compiled with the project.
 */
class KspGeneratorOutput(
    private val codeGenerator: CodeGenerator,
    private val dependencies: Dependencies
) : GeneratorOutput(null, null) {

    override fun createWriter(javaPackage: String?, fileOrJavaClassName: String, fileExtension: String): Writer {
        check(fileExtension == ".java") { "Only Java files are supported, got '$fileExtension'." }
        val stream = codeGenerator.createNewFile(
            dependencies,
            javaPackage ?: "",
            fileOrJavaClassName,
            "java"
        )
        return OutputStreamWriter(stream, StandardCharsets.UTF_8)
    }

}
