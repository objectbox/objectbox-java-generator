/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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

import io.objectbox.gradle.util.AndroidCompat
import io.objectbox.reporting.BasicBuildTracker
import org.gradle.util.GradleVersion
import java.util.*

/**
 * Track build errors and anonymous stats for Gradle projects.
 */
// Non-final for easier mocking
open class GradleBuildTracker(toolName: String) : BasicBuildTracker(toolName) {

    override fun version(): String? {
        return ProjectEnv.Const.pluginVersion
    }

    fun trackBuild(env: ProjectEnv) {
        countBuild()
        if (shouldSendBuildEvent()) {
            sendEvent("Build", buildEventProperties(env))
        }
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
        event.key("BuildCount").value(getAndResetBuildCount().toString()).comma()

        val ci = checkCI()
        if (ci != null) {
            event.key("CI").value(ci).comma()
        }
        // There may be multiple languages in a project, so it's not a single dimension
        val hasKotlinPlugin = env.hasKotlinAndroidPlugin || env.hasKotlinPlugin
        event.key("Kotlin").value(hasKotlinPlugin.toString()).comma()
        event.key("Java").value(env.hasJavaPlugin.toString()).comma()
        event.key("Version").value(ProjectEnv.Const.pluginVersion).comma()
        event.key("Target").value(if (env.hasAndroidPlugin) "Android" else "Other").comma()
        if (env.hasAndroidPlugin) {
            event.key("AGP").value(AndroidCompat.getPluginVersion(env.project)).comma()
        }
        event.key("Gradle").value(GradleVersion.current().version)
        return event.toString()
    }

    /**
     * Returns the application ID of the first found build variant.
     */
    // Open to allow mocking for testing.
    open fun androidAppId(env: ProjectEnv): String? {
        if (!env.hasAndroidPlugin) {
            return null // Android plugin API not available
        }
        val project = env.project
        return AndroidCompat.getPlugin(project).getFirstApplicationId(project)
    }

    private fun checkCI(): String? {
        return when {
            // https://docs.github.com/en/actions/learn-github-actions/variables#default-environment-variables
            System.getenv("GITHUB_ACTIONS") != null -> "GH"
            // https://docs.travis-ci.com/user/environment-variables/#default-environment-variables
            System.getenv("TRAVIS") != null -> "T"
            // https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#using-environment-variables
            System.getenv("JENKINS_URL") != null -> "J"
            // https://docs.gitlab.com/ee/ci/variables/predefined_variables.html
            System.getenv("GITLAB_CI") != null -> "GL"
            // https://circleci.com/docs/variables/#built-in-environment-variables
            System.getenv("CIRCLECI") != null -> "C"
            // https://docs.cloudbees.com/docs/cloudbees-codeship/latest/pro-builds-and-configuration/environment-variables#_default_environment_variables
            System.getenv("CI_NAME")?.toLowerCase(Locale.ROOT) == "codeship" -> "CS"
            System.getenv("CI") == "true" -> "Other"
            else -> null
        }
    }

}
