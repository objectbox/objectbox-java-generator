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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import io.objectbox.reporting.BasicBuildTracker
import org.gradle.api.plugins.ExtensionContainer
import kotlin.reflect.KClass

/**
 * Track build errors and anonymous stats for Gradle projects.
 */
// Non-final for easier mocking
open class GradleBuildTracker(toolName: String) : BasicBuildTracker(toolName) {

    override fun version(): String? {
        return ProjectEnv.Const.pluginVersion
    }

    fun trackBuild(env: ProjectEnv) {
        sendEventAsync("Build", buildEventProperties(env))
    }

    // Use internal once fixed (Kotlin 1.1.4?)
    fun buildEventProperties(env: ProjectEnv): String {
        val event = StringBuilder()

        // AAID: Anonymous App ID
        val appId = androidAppId(env)
        if (appId != null) {
            event.key("AAID").value(hashBase64WithoutPadding(appId)).comma()
        }
        event.key("BuildOS").valueEscaped(System.getProperty("os.name")).comma()
        event.key("BuildOSVersion").valueEscaped(System.getProperty("os.version")).comma()

        val ci = checkCI()
        if (ci != null) {
            event.key("CI").value(ci).comma()
        }
        // There may be multiple languages in a project, so it's not a single dimension
        val hasKotlinPlugin = env.hasKotlinAndroidPlugin || env.hasKotlinPlugin
        event.key("Kotlin").value(hasKotlinPlugin.toString()).comma()
        event.key("Java").value(env.hasJavaPlugin.toString()).comma()
        event.key("Version").value(ProjectEnv.Const.pluginVersion).comma()
        event.key("Target").value(if (env.hasAndroidPlugin) "Android" else "Other")
        return event.toString()
    }

    // Allow stubbing for testing
    // Use internal once fixed (Kotlin 1.1.4?)
    // TODO how are flavors handled here?
    open fun androidAppId(env: ProjectEnv): String? {
        if (!env.hasAndroidPlugin) {
            return null // Android plugin API not available
        }
        val project = env.project
        val appPlugin = project.plugins.find { it is AppPlugin }
        if (appPlugin != null) {
            val variants = project.extensions[AppExtension::class].applicationVariants
            return variants.firstOrNull()?.applicationId
        }
        val libraryPlugin = project.plugins.find { it is LibraryPlugin }
        if (libraryPlugin != null) {
            val variants = project.extensions[LibraryExtension::class].libraryVariants
            return variants.firstOrNull()?.applicationId
        }
        return null
    }

    private fun checkCI(): String? {
        return when {
        //https://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables
            System.getenv("CI") == "true" -> "T"
        // https://wiki.jenkins.io/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-below
            System.getenv("JENKINS_URL") != null -> "J"
            System.getenv("GITLAB_CI") != null -> "GL" // https://docs.gitlab.com/ee/ci/variables/
            System.getenv("CIRCLECI") != null -> "C" // https://circleci.com/docs/1.0/environment-variables/
        // https://documentation.codeship.com/pro/builds-and-configuration/steps/
            System.getenv("CI_NAME")?.toLowerCase() == "codeship" -> "CS"
            System.getenv("CI") != null -> "Other"
            else -> null
        }
    }

    private operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T {
        return getByType(type.java)
    }

}
