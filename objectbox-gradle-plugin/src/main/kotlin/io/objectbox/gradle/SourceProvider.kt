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
    fun registerTransform()
}

class AndroidPluginSourceProvider(val project: Project) : SourceProvider {
    val androidExtension = project.extensions.getByType(AndroidConfig::class.java)!!

    override fun sourceFiles(): Sequence<FileTree> =
            androidExtension.sourceSets.asSequence().map { it.java.sourceFiles }

    override val encoding: String?
        get() = androidExtension.compileOptions.encoding

    override fun addGeneratorTask(generatorTask: Task, targetGenDir: File, writeToBuildFolder: Boolean) {
// Futile attempt to mimik the workaround of this in build.gradle:
// android.sourceSets.main.java.srcDirs += 'build/generated/source/objectbox'
//        if (project.plugins.hasPlugin("kotlin-android")) {
//            // Workaround for Kotlin Android plugin: depending on the order of plugin definitions,
//            // this helps to make generated classes available to Kotlin:
//            val javaSourceSet = androidExtension.sourceSets.findByName("main").java
//            val newJavaSrcDirs = javaSourceSet.srcDirs.toMutableList()
//            newJavaSrcDirs += targetGenDir
//            javaSourceSet.setSrcDirs(newJavaSrcDirs)
//        }

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

    override fun registerTransform() {
        ObjectBoxAndroidTransform.Registration.to(project)
    }

}

class JavaPluginSourceProvider(val project: Project) : SourceProvider {
    val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)!!

    override fun sourceFiles(): Sequence<FileTree> =
            javaPlugin.sourceSets.asSequence().map { it.allJava.asFileTree }

    override val encoding: String?
        get() = project.tasks.withType(JavaCompile::class.java).firstOrNull()?.options?.encoding

    override fun addGeneratorTask(generatorTask: Task, targetGenDir: File, writeToBuildFolder: Boolean) {
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

    override fun registerTransform() {
        project.logger.info("No transformer for Java projects")
    }
}
