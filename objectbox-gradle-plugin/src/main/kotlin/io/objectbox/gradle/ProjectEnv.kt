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
import java.util.*

class ProjectEnv(val project: Project) {
    object Const {
        const val name: String = "objectbox"
        const val pluginVersion = GradlePluginBuildConfig.VERSION
        const val javaVersionToApply = GradlePluginBuildConfig.APPLIES_JAVA_VERSION
        /** Native libraries that do NOT contain Sync support. */
        const val nativeVersionToApply = GradlePluginBuildConfig.APPLIES_NATIVE_VERSION
        /** Native libraries that DO contain Sync support. */
        const val nativeSyncVersionToApply = GradlePluginBuildConfig.APPLIES_NATIVE_SYNC_VERSION
    }

    /** Note: Plugin extension, values only available after evaluation phase. */
    val options: PluginOptions = project.extensions.create(Const.name, PluginOptions::class.java, project)
    /** Note: Extension value, only available after evaluation phase. */
    val debug: Boolean
        get() = options.debug

    val androidPluginIds = listOf("android", "android-library", /* Legacy Android Plugin */
        "com.android.application", "com.android.library", /* Android Plugin */
        "com.android.feature" /* Instant App Module */)
    val hasAndroidPlugin = androidPluginIds.any { project.plugins.hasPlugin(it) }

    val hasKotlinAndroidPlugin = project.plugins.hasPlugin("kotlin-android")
    val hasKotlinPlugin = project.plugins.hasPlugin("kotlin")
    val hasJavaPlugin = project.plugins.hasPlugin("java")

    val osName: String = System.getProperty("os.name")
    val is64Bit = System.getProperty("sun.arch.data.model") == "64"
    private val osNameLowerCase = osName.toLowerCase(Locale.US)
    val isLinux = osNameLowerCase.contains("linux")
    val isMac = osNameLowerCase.contains("mac")
    val isWindows = osNameLowerCase.contains("windows")
    val isLinux64 = isLinux && is64Bit
    val isMac64 = isMac && is64Bit
    val isWindows64 = isWindows && is64Bit


    /**
     * See https://developer.android.com/studio/build/gradle-plugin-3-0-0-migration.html#new_configurations and
     * https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph
     */
    val configApiOrCompile: String by lazy {
        if (project.configurations.findByName("api") != null) "api" else "compile"
    }
    val configAndroidTestImplOrCompile: String by lazy {
        if (project.configurations.findByName("androidTestImplementation") != null) {
            "androidTestImplementation"
        } else {
            "androidTestCompile"
        }
    }
    val configTestImplOrCompile: String by lazy {
        if (project.configurations.findByName("testImplementation") != null) {
            "testImplementation"
        } else {
            "testCompile"
        }
    }

    fun logDebug(msg: String) = project.logger.debug(msg)
    fun logInfo(msg: String) = project.logger.info(msg)
    fun logWarn(msg: String) = project.logger.warn(msg)
}