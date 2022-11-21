package io.objectbox.gradle

import io.objectbox.reporting.ObjectBoxBuildConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject


/**
 * Writes build config file required for processor and tracks builds.
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
        writeBuildConfig()
    }

    private fun writeBuildConfig() {
        // Note: currently not setting Android flavor.
        ObjectBoxBuildConfig(env.project.projectDir.absolutePath, null).writeInto(buildConfigFile)
    }

}