/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
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

import io.objectbox.GradlePluginBuildConfig
import io.objectbox.logging.log
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import java.util.*

class ProjectEnv(val project: Project) {
    object Const {
        const val name: String = "objectbox"
        const val pluginVersion = GradlePluginBuildConfig.VERSION
        const val javaVersionToApply = GradlePluginBuildConfig.APPLIES_JAVA_VERSION
        const val nativeVersionToApply = GradlePluginBuildConfig.APPLIES_NATIVE_VERSION
        const val nativeSyncVersionToApply = GradlePluginBuildConfig.APPLIES_NATIVE_SYNC_VERSION
    }

    /** Note: Plugin extension, values only available after evaluation phase. */
    val options: ObjectBoxPluginExtension = project.extensions.create(Const.name, ObjectBoxPluginExtension::class.java)

    // Note: can not use types as this project uses Android and Kotlin plugin API as compileOnly,
    // so the classes might be missing from projects that do not have the Android or Kotlin plugin on the classpath.
    // All Android plugins also apply the AndroidBasePlugin.
    val hasAndroidPlugin = project.plugins.hasPlugin("com.android.base")
    val hasKotlinAndroidPlugin = project.plugins.hasPlugin("kotlin-android")
    val hasKotlinPlugin = project.plugins.hasPlugin("kotlin")

    // The Java Library and Java Application plugin,
    // as well as the Kotlin JVM and Android plugin also apply the Java plugin.
    val hasJavaPlugin = project.plugins.hasPlugin(JavaPlugin::class.java)

    val osName: String = System.getProperty("os.name")
    val is64Bit = System.getProperty("sun.arch.data.model") == "64"
    private val osNameLowerCase = osName.toLowerCase(Locale.US)
    private val isLinux = osNameLowerCase.contains("linux")
    private val isMac = osNameLowerCase.contains("mac")
    private val isWindows = osNameLowerCase.contains("windows")
    val isLinux64 = isLinux && is64Bit
    val isMac64 = isMac && is64Bit
    val isWindows64 = isWindows && is64Bit


    /**
     * See Gradle [java-library plugin configurations](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph)
     * and [java plugin configurations](https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_plugin_and_dependency_management)
     * (used by `applications` plugin).
     */
    val configApiOrImplOrCompile: String by lazy {
        if (project.configurations.findByName("api") != null) {
            // Projects applying the java-library plugin.
            // Try to use api by default so consuming projects inherit the dependency.
            "api"
        } else if (project.configurations.findByName("implementation") != null) {
            // Projects applying the application plugin (does not have api configuration).
            "implementation"
        } else {
            "compile"
        }
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

    fun logInfo(msg: String) = project.logger.info(msg)

    /**
     * Logs after evaluation phase when plugin options have been read
     * and it is known if debug mode is enabled by build script.
     * Using function for [message] to avoid String getting built unless in debug mode.
     */
    fun logDebug(message: () -> String) {
        project.afterEvaluate {
            if (options.debug.get()) log(message())
        }
    }
}