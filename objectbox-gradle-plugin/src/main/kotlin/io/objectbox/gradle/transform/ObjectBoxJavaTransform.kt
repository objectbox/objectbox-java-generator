package io.objectbox.gradle.transform

import io.objectbox.gradle.GradleBuildTracker
import org.gradle.api.Project
import java.io.File

class ObjectBoxJavaTransform(val debug: Boolean) {

    fun transform(compileJavaTaskOutputDir: File) {
        try {
            val allClassFiles = mutableSetOf<File>()
            compileJavaTaskOutputDir.walk().filter { it.isFile }.forEach { file ->
                if (file.name.endsWith(".class")) {
                    allClassFiles += file
                }
            }
            val classProber = ClassProber()
            val probedClasses = allClassFiles.map { classProber.probeClass(it) }
            ClassTransformer(debug).transformOrCopyClasses(probedClasses, compileJavaTaskOutputDir)
        } catch (e: Throwable) {
            val buildTracker = GradleBuildTracker("Transformer")
            if (e is TransformException) buildTracker.trackError("Transform failed", e)
            else buildTracker.trackFatal("Transform failed", e)
            throw e
        }
    }

}