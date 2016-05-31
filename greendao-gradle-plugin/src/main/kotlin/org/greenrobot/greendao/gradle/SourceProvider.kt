package org.greenrobot.greendao.gradle

import com.android.build.gradle.AndroidConfig
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

interface SourceProvider {
    fun sourceFiles(): Sequence<FileTree>
    fun sourceDirs(): Sequence<File>
    fun sourceTree(): FileTree = sourceFiles().reduce { a, b -> a + b }
    fun addSourceDir(dir: File)
    val encoding: String? get
}

val ANDROID_PLUGINS = listOf(
    "android", "android-library", "com.android.application", "com.android.library"
)

val SUPPORTED_PLUGINS = ANDROID_PLUGINS + "java"

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

    override fun addSourceDir(dir: File) {
        androidExtension.sourceSets.maybeCreate("main").java.srcDir(dir)
    }

    override val encoding: String?
        get() = androidExtension.compileOptions.encoding
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

    override fun addSourceDir(dir: File) {
        javaPluginConvention.sourceSets.maybeCreate("main").java.srcDir(dir)
    }

    override val encoding: String?
        get() = project.tasks.withType(JavaCompile::class.java).firstOrNull()?.let {
            it.options.encoding
        }
}

/** @throws RuntimeException if no supported plugins applied */
val Project.sourceProvider: SourceProvider
    get() = when {
        project.plugins.hasPlugin("java") -> JavaPluginSourceProvider(project)

        ANDROID_PLUGINS.any { project.plugins.hasPlugin(it) } -> AndroidPluginSourceProvider(project)

        else -> throw RuntimeException("greenDAO supports only Java and Android projects. " +
            "None of corresponding plugins have been applied to the project")
    }

/** executes block when [SourceProvider] is available */
fun Project.whenSourceProviderAvailable(block: (SourceProvider) -> Unit) {
    SUPPORTED_PLUGINS.forEach {
        pluginManager.withPlugin(it) {
            block(sourceProvider)
        }
    }
}