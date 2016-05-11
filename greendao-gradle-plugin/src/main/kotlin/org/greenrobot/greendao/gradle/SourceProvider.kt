package org.greenrobot.greendao.gradle

import com.android.build.gradle.AndroidConfig
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.JavaPluginConvention
import java.io.File

interface SourceProvider {
    fun sourceFiles(): Sequence<FileTree>
    fun sourceDirs(): Sequence<File>
}

class AndroidPluginSourceProvider(val project: Project): SourceProvider {
    val androidExtension = project.extensions.getByType(AndroidConfig::class.java)
    init {
        require(androidExtension != null) {
            "There is no android plugin applied to the project"
        }
    }

    override fun sourceFiles(): Sequence<FileTree> =
        androidExtension.sourceSets.asSequence().map { it.java.sourceFiles }

    override fun sourceDirs(): Sequence<File> =
        androidExtension.sourceSets.asSequence().map { it.java.srcDirs }.flatten()
}

class JavaPluginSourceProvider(val project: Project): SourceProvider {
    val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
    init {
        require(javaPluginConvention != null) {
            "There is no java plugin applied to the project"
        }
    }

    override fun sourceFiles(): Sequence<FileTree> =
        javaPluginConvention.sourceSets.asSequence().map { it.allJava.asFileTree }

    override fun sourceDirs(): Sequence<File> =
        javaPluginConvention.sourceSets.asSequence().map { it.allJava.srcDirs }.flatten()
}

val Project.sourceProvider: SourceProvider
    get() = when {
        project.plugins.findPlugin("java") != null -> JavaPluginSourceProvider(project)

        project.plugins.findPlugin("android") != null
            || project.plugins.findPlugin("com.android.application") != null
            || project.plugins.findPlugin("com.android.library") != null -> AndroidPluginSourceProvider(project)

        else -> throw RuntimeException("greenDAO supports only Java or Android projects. " +
            "None of corresponding plugins have been applied to the project")
    }
