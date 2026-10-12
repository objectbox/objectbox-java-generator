/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
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

package io.objectbox.gradle.transform

import io.objectbox.logging.log
import java.io.File

/**
 * Transforms class (byte code) files from multiple directories,
 * overwriting the original class files if no output directory is given.
 *
 * NOTE: This class is also used by the ObjectBox Maven plugin, so it's API
 * should remain stable and it shouldn't use any Gradle APIs.
 *
 * See ObjectBoxAndroidTransform in AGP 3.3 module.
 * @see ClassTransformer
 */
class ObjectBoxJavaTransform(private val debug: Boolean) {

    /**
     * NOTE: This method is also used by the ObjectBox Maven plugin.
     */
    fun transform(byteCodeDirs: List<File>, outDir: File?, copyNonTransformed: Boolean) {
        val probedClasses = mutableListOf<ProbedClass>()
        val classProber = ClassProber()
        byteCodeDirs.forEach { byteCodeDir ->
            if (debug) log("Detected byte code dir ${byteCodeDir.path}")
            byteCodeDir.walk().filter { it.isFile }.forEach { file ->
                if (file.name.endsWith(".class")) {
                    // If no out directory is given, overwrite original files with transformed files: so outDir == byteCodeDir
                    probedClasses += classProber.probeClass(file, outDir ?: byteCodeDir)
                }
            }
        }

        ClassTransformer(debug).transformOrCopyClasses(probedClasses, copyNonTransformed)
    }

}