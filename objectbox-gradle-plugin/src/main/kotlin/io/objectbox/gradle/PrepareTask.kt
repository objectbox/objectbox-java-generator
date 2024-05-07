/*
 * ObjectBox Build Tools
 * Copyright (C) 2020-2024 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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