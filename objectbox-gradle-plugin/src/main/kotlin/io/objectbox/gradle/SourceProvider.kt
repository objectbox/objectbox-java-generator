package io.objectbox.gradle

import com.android.build.gradle.AndroidConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File
import kotlin.reflect.KClass

/** Interface to have some abstraction over Android vs. Java plugins. */
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
        project.plugins.all {
            when (it) {
                is AppPlugin -> applyPlugin(project.extensions[AppExtension::class].applicationVariants,
                        generatorTask, targetGenDir, writeToBuildFolder)
                is LibraryPlugin -> applyPlugin(project.extensions[LibraryExtension::class].libraryVariants,
                        generatorTask, targetGenDir, writeToBuildFolder)
            }
        }
    }

    private fun applyPlugin(variants: DomainObjectSet<out BaseVariant>, objectboxTask: Task, targetGenDir: File,
            writeToBuildFolder: Boolean) {
        variants.all { variant ->
            if (writeToBuildFolder) {
                variant.registerJavaGeneratingTask(objectboxTask, targetGenDir)
            } else {
                // user takes care of adding to source dirs, just add the task
                variant.javaCompiler.dependsOn(objectboxTask)
            }
        }
    }

    private operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T {
        return getByType(type.java)!!
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
        get() = project.tasks.withType(JavaCompile::class.java).firstOrNull()?.options?.encoding

    override fun addGeneratorTask(generatorTask: Task, targetGenDir: File, writeToBuildFolder: Boolean) {
        val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
        // for the main source set...
        val mainSourceSet = javaPlugin.sourceSets.maybeCreate("main")
        // ...make the compile task depend on the objectbox task
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

        else -> throw RuntimeException("ObjectBox supports only Java and Android projects. " +
            "None of the corresponding plugins have been applied to the project.")
    }
