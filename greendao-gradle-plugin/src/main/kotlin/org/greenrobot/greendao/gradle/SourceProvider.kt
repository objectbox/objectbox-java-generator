package org.greenrobot.greendao.gradle

import com.android.build.gradle.AndroidConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File

interface SourceProvider {
    fun sourceFiles(): Sequence<FileTree>
    fun sourceTree(): FileTree = sourceFiles().reduce { a, b -> a + b }
    val encoding: String? get
    fun addGeneratorTask(generatorTask: Task, targetGenDir: File, writeToBuildFolder: Boolean)
}

val ANDROID_PLUGINS = listOf(
    "android", "android-library", "com.android.application", "com.android.library"
)

class AndroidPluginSourceProvider(val project: Project): SourceProvider {
    val androidExtension = project.extensions.getByType(AndroidConfig::class.java)
    init {
        require(androidExtension != null) {
            "There is no android plugin applied to the project"
        }
    }

    override fun sourceFiles(): Sequence<FileTree> =
        androidExtension.sourceSets.asSequence().map { it.java.sourceFiles }

    override val encoding: String?
        get() = androidExtension.compileOptions.encoding

    override fun addGeneratorTask(generatorTask: Task, targetGenDir: File, writeToBuildFolder: Boolean) {
        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            // Android application
            val android = project.extensions.getByType(AppExtension::class.java)
            android.applicationVariants.all { variant: BaseVariant ->
                addGeneratorTask(variant, generatorTask, targetGenDir, writeToBuildFolder)
            }
        } else if (project.plugins.hasPlugin(LibraryPlugin::class.java)) {
            // Android library
            val android = project.extensions.getByType(LibraryExtension::class.java)
            android.libraryVariants.all { variant: BaseVariant ->
                addGeneratorTask(variant, generatorTask, targetGenDir, writeToBuildFolder)
            }
        }
    }

    fun addGeneratorTask(variant: BaseVariant, objectboxTask: Task, targetGenDir: File, writeToBuildFolder: Boolean) {
        if (writeToBuildFolder) {
            variant.registerJavaGeneratingTask(objectboxTask, targetGenDir)
        } else {
            // user takes care of adding to source dirs, just add the task
            variant.javaCompiler.dependsOn(objectboxTask)
        }
    }
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

    override val encoding: String?
        get() = project.tasks.withType(JavaCompile::class.java).firstOrNull()?.let {
            it.options.encoding
        }

    override fun addGeneratorTask(generatorTask: Task, targetGenDir: File, writeToBuildFolder: Boolean) {
        val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
        // for the main source set...
        val mainSourceSet = javaPlugin.sourceSets.maybeCreate("main")
        // ...make the compile task depend on the generator task
        val compileJavaTask = project.tasks.getByName(mainSourceSet.compileJavaTaskName) as JavaCompile
        compileJavaTask.dependsOn(generatorTask)
        if (writeToBuildFolder) {
            // ...add the generated sources folder to the source dirs
            mainSourceSet.java.srcDir(targetGenDir)
            // ...ensure the compile task has them on the classpath
            compileJavaTask.setSource(compileJavaTask.source + generatorTask.outputs.files)
        }
    }
}

/** @throws RuntimeException if no supported plugins applied */
val Project.sourceProvider: SourceProvider
    get() = when {
        project.plugins.hasPlugin("java") -> JavaPluginSourceProvider(project)

        ANDROID_PLUGINS.any { project.plugins.hasPlugin(it) } -> AndroidPluginSourceProvider(project)

        else -> throw RuntimeException("greenDAO supports only Java and Android projects. " +
            "None of the corresponding plugins have been applied to the project.")
    }
