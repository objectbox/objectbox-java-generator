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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Writes build config file required for processor and tracks builds.
 *
 * Only ever holds plain values (or, where the real value is not known until after the project
 * has been evaluated, a [Property] set from a lazy `project.provider { }`) as task state -
 * never a [org.gradle.api.Project] or [ProjectEnv] directly. Both are unsupported by the Gradle
 * Configuration Cache: an incompatible task discards the cache entry for the whole build on
 * every run, not just for itself (see https://github.com/objectbox/objectbox-java/issues/948).
 *
 * The constructor-injected flags are static per project (plain `plugins.hasPlugin(...)` checks,
 * correct as soon as the plugin applies), so they can be passed in directly. [projectDirPath],
 * [agpVersion], [applicationId] and [buildConfigFile] are set by the registering code via
 * `project.provider { }`/`project.layout` instead, since - like `env.project.buildDir` and
 * `AndroidCompat.getFirstApplicationId(project)` in the pre-fix version of this task - their
 * real value is only reliable once the project's own build script has finished configuring it.
 */
abstract class PrepareTask @Inject constructor(
    private val hasAndroidPlugin: Boolean,
    private val hasKotlinAndroidPlugin: Boolean,
    private val hasKotlinPlugin: Boolean,
    private val hasJavaPlugin: Boolean,
    private val buildTracker: GradleBuildTracker
) : DefaultTask() {

    @get:Internal
    abstract val projectDirPath: Property<String>

    @get:Internal
    abstract val agpVersion: Property<String>

    @get:Internal
    abstract val applicationId: Property<String>

    @get:OutputFile
    abstract val buildConfigFile: RegularFileProperty

    init {
        group = "objectbox"
    }

    @TaskAction
    fun run() {
        buildTracker.trackBuild(
            BuildEventInfo(
                hasAndroidPlugin = hasAndroidPlugin,
                hasKotlinAndroidPlugin = hasKotlinAndroidPlugin,
                hasKotlinPlugin = hasKotlinPlugin,
                hasJavaPlugin = hasJavaPlugin,
                agpVersion = agpVersion.orNull,
                applicationId = applicationId.orNull
            )
        )
        writeBuildConfig()
    }

    private fun writeBuildConfig() {
        val file = buildConfigFile.get().asFile
        // ObjectBoxBuildConfig.buildFile(outputDir) previously did this as a side effect of
        // resolving the output path; do it here instead, now that resolving the path itself is
        // lazy and side-effect-free.
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        // Note: currently not setting Android flavor.
        ObjectBoxBuildConfig(projectDirPath.get(), null).writeInto(file)
    }

}
