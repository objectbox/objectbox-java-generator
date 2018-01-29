/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle

import io.objectbox.GradlePluginBuildConfig
import org.gradle.api.Project

class ProjectEnv(val project: Project) {
    object Const {
        const val name: String = "objectbox"
        const val pluginVersion = GradlePluginBuildConfig.VERSION
        const val runtimeVersion = GradlePluginBuildConfig.VERSION_RUNTIME
    }

    /** Note: Plugin extension, values only available after evaluation phase. */
    val options = project.extensions.create(Const.name, PluginOptions::class.java, project)
    /** Note: Extension value, only available after evaluation phase. */
    val debug: Boolean
        get() = options.debug

    val hasAndroidPlugin = listOf("android", "android-library", "com.android.application", "com.android.library")
            .any { project.plugins.hasPlugin(it) }

    val hasKotlinAndroidPlugin = project.plugins.hasPlugin("kotlin-android")
    val hasKotlinPlugin = project.plugins.hasPlugin("kotlin")
    val hasJavaPlugin = project.plugins.hasPlugin("java")

    /**
     * See https://developer.android.com/studio/build/gradle-plugin-3-0-0-migration.html#new_configurations and
     * https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph
     */
    val dependencyScopeApiOrCompile: String by lazy {
        if (project.configurations.findByName("api") != null) "api" else "compile"
    }

    fun logDebug(msg: String) = project.logger.debug(msg)
    fun logInfo(msg: String) = project.logger.info(msg)
    fun logWarn(msg: String) = project.logger.warn(msg)
}