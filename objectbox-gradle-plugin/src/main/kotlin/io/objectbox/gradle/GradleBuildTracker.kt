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

import io.objectbox.reporting.BasicBuildTracker
import org.gradle.util.GradleVersion
import java.util.*

/**
 * Track build errors and anonymous stats for Gradle projects.
 */
// Non-final for easier mocking
open class GradleBuildTracker(toolName: String) : BasicBuildTracker(toolName) {

    override fun version(): String? {
        return ProjectEnv.Const.OBX_PLUGIN_VERSION
    }

    fun trackBuild(info: BuildEventInfo) {
        countBuild()
        if (shouldSendBuildEvent()) {
            sendEvent("Build", buildEventProperties(info))
        }
    }

    // Use internal once fixed (Kotlin 1.1.4?)
    fun buildEventProperties(info: BuildEventInfo): String {
        val event = StringBuilder()

        // AAID: Anonymous App ID
        val appId = info.applicationId
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
        val hasKotlinPlugin = info.hasKotlinAndroidPlugin || info.hasKotlinPlugin
        event.key("Kotlin").value(hasKotlinPlugin.toString()).comma()
        event.key("Java").value(info.hasJavaPlugin.toString()).comma()
        event.key("Version").value(ProjectEnv.Const.OBX_PLUGIN_VERSION).comma()
        event.key("Target").value(if (info.hasAndroidPlugin) "Android" else "Other").comma()
        if (info.hasAndroidPlugin && info.agpVersion != null) {
            event.key("AGP").value(info.agpVersion).comma()
        }
        event.key("Gradle").value(GradleVersion.current().version)
        return event.toString()
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
