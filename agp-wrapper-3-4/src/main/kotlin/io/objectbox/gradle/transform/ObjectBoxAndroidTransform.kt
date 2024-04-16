/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import io.objectbox.logging.log
import io.objectbox.reporting.BasicBuildTracker
import org.gradle.api.provider.Property
import java.io.File

/**
 * A byte-code [Transform] to be registered with the Android plugin to run before dexing (regular builds or instrumented
 * unit test builds). The transform results are stored in a directory assigned to this Transform.
 * To also support transformation for local unit test builds the registration code injects custom transform tasks
 * for each build variant with unit tests. They run an [ObjectBoxJavaTransform] before unit test code is compiled.
 *
 * @see ClassTransformer
 */
class ObjectBoxAndroidTransform(private val debug: Property<Boolean>) : Transform() {

    override fun getName(): String {
        return "ObjectBoxAndroidTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return mutableSetOf(QualifiedContent.Scope.PROJECT)
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(info: TransformInvocation) {
        super.transform(info)
        val debug = debug.get()
        try {
            val probedClasses = mutableListOf<ProbedClass>()

            val classProber = ClassProber()
            info.inputs.forEach { transformInput ->
                // Look through directory inputs to transform or just copy.
                transformInput.directoryInputs.forEach { directoryInput ->
                    if (debug) log("Input directory: ${directoryInput.name} ${directoryInput.file}")
                    // Output files to directory unique for this input directory.
                    val outDir = info.outputProvider.getContentLocation(
                        directoryInput.name,
                        outputTypes,
                        scopes,
                        Format.DIRECTORY
                    )
                    if (debug) log("Output directory: $outDir")

                    // TODO incremental: directoryInput.changedFiles

                    var classes = 0
                    var copied = 0
                    directoryInput.file.walk().filter { it.isFile }.forEach { file ->
                        if (file.name.endsWith(".class")) {
                            probedClasses += classProber.probeClass(file, outDir)
                            classes += 1
                        } else {
                            val relativePath = file.toRelativeString(directoryInput.file)
                            val destFile = File(outDir, relativePath)
                            file.copyTo(destFile, overwrite = true)
                            copied += 1
                        }
                    }
                    if (debug) log("Copied $copied files, will check $classes classes if transform required.")
                }

                // Not looking at class files in JARs, just copy them.
                // It appears only Android Gradle Plugin 3.6.0 uses this to pass the R classes in a JAR.
                // https://github.com/objectbox/objectbox-java/issues/817
                transformInput.jarInputs.forEach { jarInput ->
                    if (debug) log("Input JAR: ${jarInput.name} ${jarInput.file}")
                    // Note: TransformOutputProvider.getContentLocation(name, ...) returns the same file if all params
                    // match. Make sure name differs for each JAR to avoid overwriting an already copied JAR.
                    val outFileJar =
                        info.outputProvider.getContentLocation(jarInput.name, outputTypes, scopes, Format.JAR)
                    jarInput.file.copyTo(outFileJar, overwrite = true)
                    if (debug) log("Output JAR: $outFileJar")
                }
            }

            ClassTransformer(debug).transformOrCopyClasses(probedClasses)

        } catch (e: Throwable) {
            val buildTracker = BasicBuildTracker("Transformer")
            if (e is TransformException) buildTracker.trackError("Transform failed", e)
            else buildTracker.trackFatal("Transform failed", e)
            throw e
        }
    }

}
