package io.objectbox.gradle.transform

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestPlugin
import io.objectbox.gradle.BuildTracker
import org.gradle.api.Project
import java.io.File

class ObjectBoxAndroidTransform(val project: Project) : Transform() {

    object Registration {
        fun to(project: Project) {
            val transform = ObjectBoxAndroidTransform(project)
            getAllExtensions(project).forEach { it.registerTransform(transform) }
        }

        fun getAllExtensions(project: Project): Set<BaseExtension> {
            val exClasses = getAndroidExtensionClasses(project)
            if (exClasses.isEmpty()) throw TransformException(
                    "No Android plugin found - please apply ObjectBox plugins after the Android plugin")
            val result = exClasses.map { project.extensions.getByType(it) as BaseExtension }.toSet()
            return result
        }

        fun getAndroidExtensionClasses(project: Project): MutableList<Class<out BaseExtension>> {
            // Should be only one plugin, but let's assume there can be a combination just in case...
            val exClasses = mutableListOf<Class<out BaseExtension>>()
            val plugins = project.plugins
            if (plugins.hasPlugin(LibraryPlugin::class.java)) exClasses += LibraryExtension::class.java
            if (plugins.hasPlugin(TestPlugin::class.java)) exClasses += TestExtension::class.java
            if (plugins.hasPlugin(AppPlugin::class.java)) exClasses += AppExtension::class.java
            //if (plugins.hasPlugin(InstantAppPlugin::class.java)) exClasses += InstantAppExtension::class.java
            return exClasses
        }
    }

    val classProber = ClassProber(true) // TODO turn on debug temp
    val classTransformer = ClassTransformer(true) // TODO turn on debug temp

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
        try {
            val allClassFiles = mutableSetOf<File>()
            info.inputs.flatMap { it.directoryInputs }.forEach { directoryInput ->
                // TODO incremental: directoryInput.changedFiles

                allClassFiles.addAll(directoryInput.file.walk().filter { it.isFile && it.name.endsWith(".class") })
            }

            val probedClasses = allClassFiles.map { classProber.probeClass(it) }.filterNotNull()
            val outDir = info.outputProvider.getContentLocation("objectbox", inputTypes, scopes, Format.DIRECTORY)

            classTransformer.transformOrCopyClasses(probedClasses, outDir)
        } catch (e: Throwable) {
            val buildTracker = BuildTracker("Transformer")
            if(e is TransformException) buildTracker.trackError("Transform failed", e)
            else buildTracker.trackFatal("Transform failed", e)
            throw e
        }
    }

}
