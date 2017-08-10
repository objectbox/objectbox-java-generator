package io.objectbox.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.plugins.ExtensionContainer
import org.greenrobot.essentials.Base64
import org.greenrobot.essentials.hash.Murmur3F
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.reflect.KClass

/**
 * Send anonymous data when building.
 *
 * Currently sends:
 * - the version of ObjectBox used
 * - the operating system the build is running on
 * - if Kotlin or only Java code is used
 */
// Non-final for easier mocking
open class Analytics(val env: ProjectEnv) {

    private val BASE_URL = "https://api.mixpanel.com/track/?data="
    private val TOKEN = "REPLACE_WITH_TOKEN"
    private val TIMEOUT_READ = 15000
    private val TIMEOUT_CONNECT = 20000

    fun submitAsync() {
        Thread(Runnable { submit() }).start()
    }

    fun submit() {
        try {
            val url = URL(buildUrl())
            val con = url.openConnection() as HttpURLConnection
            con.connectTimeout = TIMEOUT_CONNECT
            con.readTimeout = TIMEOUT_READ
            con.requestMethod = "GET"
            con.responseCode
        } catch (ignored: Exception) {
        }
    }

    private fun buildUrl(): String {
        val event = buildEventData()

        // https://mixpanel.com/help/reference/http#base64
        val eventEncoded = Base64.encodeBytes(event.toByteArray())

        return BASE_URL + eventEncoded
    }

    internal fun buildEventData(): String {
        val os = System.getProperty("os.name")
        val osVersion = System.getProperty("os.version")
        val hasKotlinPlugin = env.hasKotlinAndroidPlugin || env.hasKotlinPlugin

        // https://mixpanel.com/help/reference/http#tracking-events
        val event = StringBuilder()
        event.append("{")
        event.key("event").value("Build").comma()
        event.key("properties").append(" {")

        event.key("token").value(TOKEN).comma()
        event.key("distinct_id").value(uniqueIdentifier()).comma()
        // AAID: Anonymous App ID
        val appId = androidAppId()
        if (appId != null) {
            event.key("AAID").value(hashBase64(appId)).comma()
        }
        event.key("BuildOS").value(os).comma()
        event.key("BuildOSVersion").value(osVersion).comma()
        val ci = checkCI()
        if (ci != null) {
            event.key("CI").value(ci).comma()
        }
        // There may be multiple languages in a project, so it's not a single dimension
        event.key("Kotlin").value(hasKotlinPlugin.toString()).comma()
        event.key("Java").value(env.hasJavaPlugin.toString()).comma()
        event.key("Version").value(env.objectBoxVersion).comma()
        event.key("Target").value(if (env.hasAndroidPlugin) "Android" else "Other")

        event.append("}").append("}")
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

    private fun StringBuilder.comma(): java.lang.StringBuilder {
        append(",")
        return this
    }

    internal fun hashBase64(input: String): String {
        val murmurHash = Murmur3F()
        murmurHash.update(input.toByteArray())
        return Base64.encodeBytes(murmurHash.valueBytesBigEndian)
    }

    // Allow stubbing for testing
    open internal fun androidAppId(): String? {
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
        val ci = when {
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
        return ci
    }

    private operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T {
        return getByType(type.java)!!
    }

    internal fun uniqueIdentifier(): String {
        // a temp file should survive long enough to report multiple builds as a unique user
        val idFile = File(System.getProperty("java.io.tmpdir"), "objectbox-id.tmp")
        val existingId = if (idFile.canRead()) idFile.readText() else null
        if (existingId != null && existingId.isNotBlank()) {
            return existingId
        } else {
            val newId = UUID.randomUUID().toString()
            idFile.writeText(newId)
            return newId
        }
    }

}
