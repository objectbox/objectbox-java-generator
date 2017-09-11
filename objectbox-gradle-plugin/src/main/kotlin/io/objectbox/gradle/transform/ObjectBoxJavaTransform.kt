package io.objectbox.gradle.transform

import io.objectbox.gradle.GradleBuildTracker
import org.gradle.api.Project
import java.io.File

class ObjectBoxJavaTransform(val project: Project) {

    val classProber = ClassProber(true) // TODO turn on debug temp
    val classTransformer = ClassTransformer(true) // TODO turn on debug temp

    fun transform(compileJavaTaskOutputDir: File) {
        try {
            val allClassFiles = mutableSetOf<File>()
            compileJavaTaskOutputDir.walk().filter { it.isFile }.forEach { file ->
                if (file.name.endsWith(".class")) {
                    allClassFiles += file;
                }
            }
            val probedClasses = allClassFiles.map { classProber.probeClass(it) }
            classTransformer.transformOrCopyClasses(probedClasses, compileJavaTaskOutputDir)
        } catch (e: Throwable) {
            val buildTracker = GradleBuildTracker("Transformer")
            if (e is TransformException) buildTracker.trackError("Transform failed", e)
            else buildTracker.trackFatal("Transform failed", e)
            throw e
        }
    }

}