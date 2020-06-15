package io.objectbox.gradle

import io.objectbox.reporting.ObjectBoxBuildConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject


open class PrepareTask @Inject constructor(
    private val env: ProjectEnv,
    private val buildTracker: GradleBuildTracker
) : DefaultTask() {

    init {
        group = "objectbox"
    }

    @TaskAction
    fun run() {
        if (env.debug) println("### Executing $name in $project")
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

        writeBuildConfig(env)
    }

    private fun writeBuildConfig(env: ProjectEnv) {
        val buildDir = env.project.buildDir
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