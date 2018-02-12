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

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.FeaturePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestPlugin
import io.objectbox.gradle.GradleBuildTracker
import io.objectbox.gradle.PluginOptions
import org.gradle.api.Project
import java.io.File

class ObjectBoxAndroidTransform(val options: PluginOptions) : Transform() {

    object Registration {
        fun to(project: Project, options: PluginOptions) {
            val transform = ObjectBoxAndroidTransform(options)
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
            if (plugins.hasPlugin(FeaturePlugin::class.java)) exClasses += FeatureExtension::class.java
            return exClasses
        }
    }


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
            val outDir = info.outputProvider.getContentLocation("objectbox", inputTypes, scopes, Format.DIRECTORY)
            info.inputs.flatMap { it.directoryInputs }.forEach { directoryInput ->
                // TODO incremental: directoryInput.changedFiles

                directoryInput.file.walk().filter { it.isFile }.forEach { file ->
                    if (file.name.endsWith(".class")) {
                        allClassFiles += file
                    } else {
                        val relativePath = file.toRelativeString(directoryInput.file)
                        val destFile = File (outDir, relativePath)
                        file.copyTo(destFile, overwrite = true)
                        if(options.debug) println("Copied $file to $destFile")
                    }
                }
            }

            val classProber = ClassProber()
            val probedClasses = allClassFiles.map { classProber.probeClass(it) }
            ClassTransformer(options.debug).transformOrCopyClasses(probedClasses, outDir)

        } catch (e: Throwable) {
            val buildTracker = GradleBuildTracker("Transformer")
            if (e is TransformException) buildTracker.trackError("Transform failed", e)
            else buildTracker.trackFatal("Transform failed", e)
            throw e
        }
    }

}
