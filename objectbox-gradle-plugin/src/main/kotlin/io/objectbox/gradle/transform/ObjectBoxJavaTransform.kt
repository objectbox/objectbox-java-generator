/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle.transform

import io.objectbox.gradle.GradleBuildTracker
import io.objectbox.logging.log
import java.io.File

/**
 * Transforms class (byte code) files from multiple directories,
 * overwriting the original class files if no output directory is given.
 *
 * @see ClassTransformer
 * @see ObjectBoxAndroidTransform
 */
class ObjectBoxJavaTransform(private val debug: Boolean) {

    fun transform(byteCodeDirs: List<File>, outDir: File? = null, copyNonTransformed: Boolean = true) {
        try {
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
        } catch (e: Throwable) {
            val buildTracker = GradleBuildTracker("Transformer")
            if (e is TransformException) buildTracker.trackError("Transform failed", e)
            else buildTracker.trackFatal("Transform failed", e)
            throw e
        }
    }

}