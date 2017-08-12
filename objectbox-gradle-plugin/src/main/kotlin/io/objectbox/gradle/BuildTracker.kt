package io.objectbox.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.squareup.moshi.JsonWriter
import io.objectbox.build.BasicBuildTracker
import okio.Buffer
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import kotlin.reflect.KClass

/**
 * Track build errors and anonymous stats for Gradle projects.
 */
// Non-final for easier mocking
open class BuildTracker(toolName: String) : BasicBuildTracker(toolName) {

    override fun version(): String? {
        return ProjectEnv.Const.objectBoxVersion
    }

    fun trackBuild(env: ProjectEnv) {
        sendEventAsync("Build", buildEventProperties(env))
    }

    internal fun buildEventProperties(env: ProjectEnv): String {
        val event = StringBuilder()

        // AAID: Anonymous App ID
        val appId = androidAppId(env.project)
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
        event.key("Version").value(env.objectBoxVersion).comma()
        event.key("Target").value(if (env.hasAndroidPlugin) "Android" else "Other")
        return event.toString()
    }

    private fun StringBuilder.key(value: String): java.lang.StringBuilder {
        append("\"$value\": ")
        return this
    }

    private fun StringBuilder.value(value: String): java.lang.StringBuilder {
        append("\"$value\"")
        return this
    }

    private fun StringBuilder.valueEscaped(value: String): java.lang.StringBuilder {
        val buffer = Buffer()
        JsonWriter.of(buffer).value(value)
        append(buffer.readUtf8())
        return this
    }

    private fun StringBuilder.comma(): java.lang.StringBuilder {
        append(',')
        return this
    }

    // Allow stubbing for testing
    open internal fun androidAppId(project: Project): String? {
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
        return getByType(type.java)!!
    }

}
