package io.objectbox.gradle

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
import org.gradle.api.Project


class ObjectBoxAndroidTransform(val project: Project) : Transform() {
    object Registration {
        fun to(project: Project) {
            val transform = ObjectBoxAndroidTransform(project)
            getExtension(project).registerTransform(transform)
        }

        fun getExtension(project: Project): BaseExtension {
            val clazz: Class<out BaseExtension> =
                    when {
                        project.plugins.hasPlugin(LibraryPlugin::class.java) -> LibraryExtension::class.java
                        project.plugins.hasPlugin(TestPlugin::class.java) -> TestExtension::class.java
                        project.plugins.hasPlugin(AppPlugin::class.java) -> AppExtension::class.java
                        else -> throw RuntimeException(
                                "No Android plugin found - please apply ObjectBox plugins after the Android plugin")
                    }
            return project.extensions.getByType(clazz)
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

        for (input in info.inputs) {
            for (directoryInput in input.directoryInputs) {
                // TODO incremental: directoryInput.changedFiles

                for (file in directoryInput.file.walk().filter { it.isFile }) {
                    //project.logger.error("ObjectBox Transforming ${file}")
                }
            }
        }
    }
}
