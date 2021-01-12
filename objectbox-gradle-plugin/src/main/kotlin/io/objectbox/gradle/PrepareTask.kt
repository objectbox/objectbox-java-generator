package io.objectbox.gradle

import io.objectbox.reporting.ObjectBoxBuildConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject


/**
 * Writes build config file required for processor, checks for annotation processor configuration, plus build tracking.
 */
open class PrepareTask @Inject constructor(
    private val env: ProjectEnv,
    private val buildTracker: GradleBuildTracker
) : DefaultTask() {

    private val buildDir = env.project.buildDir

    @OutputFile
    val buildConfigFile: File = ObjectBoxBuildConfig.buildFile(buildDir)

    init {
        group = "objectbox"
    }

    @TaskAction
    fun run() {
        buildTracker.trackBuild(env)

//            if (ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(project).isEmpty()) {
//                // TODO check
//            }

        val aptConf =
            project.configurations.findByName("kapt") ?: project.configurations.findByName("annotationProcessor")
            ?: project.configurations.findByName("apt")
        val foundDependency = aptConf?.dependencies?.firstOrNull { dep -> dep.group == "io.objectbox" }
        if (foundDependency == null) {
            var msg = "No ObjectBox annotation processor configuration found. Please check your build scripts."
            if (!env.hasAndroidPlugin) msg += "Currently only Android projects are fully supported."
            throw RuntimeException(msg)
        }

        writeBuildConfig()
    }

    private fun writeBuildConfig() {
        if (!buildDir.exists()) buildDir.mkdirs()
//        var flavor: String? = null
//        val extClass = ObjectBoxAndroidTransform.Registration.getAndroidExtensionClasses(env.project).singleOrNull()
//        if (extClass != null) {
//            val ext = env.project.extensions.getByType(extClass) as BaseExtension
//            flavor = ext?.defaultConfig?.dimension
//        }
        ObjectBoxBuildConfig(env.project.projectDir.absolutePath, null).writeInto(buildDir)
    }

}