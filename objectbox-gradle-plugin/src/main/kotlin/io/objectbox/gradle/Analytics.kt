package io.objectbox.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.plugins.ExtensionContainer
import org.greenrobot.essentials.Base64
import org.greenrobot.essentials.hash.Murmur3F
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
class Analytics(val env: ProjectEnv) {

    private val BASE_URL = "https://api.mixpanel.com/track/?data="
    private val TOKEN = "ddefa920bbb8d1a98e2bd4b85e181668" // ut
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
        // TODO ut: distinct id (possibly persist UUID to file, but where to?), more reliable than MAC or BIOS ID
        val distinctId = UUID.randomUUID().toString()
        val appIdHash = androidAppIdHash() ?: "unknown"
        val os = System.getProperty("os.name")
        val osVersion = System.getProperty("os.version")
        val hasKotlinPlugin = env.hasKotlinAndroidPlugin || env.hasKotlinPlugin

        // detect CI
        // https://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables
        val isTravis = System.getProperty("CI") == "true"
//        // https://wiki.jenkins.io/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-below
        val isJenkins = System.getProperty("JENKINS_URL") != null
        val isContinuousIntegration = isTravis || isJenkins

        // https://mixpanel.com/help/reference/http#tracking-events
        val event = StringBuilder()
        event.append("{")
        event.key("event").value("Build").comma()
        event.key("properties").append(" {")

        event.key("token").value(TOKEN).comma()
        event.key("distinct_id").value(distinctId).comma()
        event.key("Anonymous App ID").value(appIdHash).comma()
        event.key("Build OS").value(os).comma()
        event.key("Build OS Version").value(osVersion).comma()
        event.key("CI Detected").value(isContinuousIntegration.toString()).comma()
        event.key("Kotlin Detected").value(hasKotlinPlugin.toString()).comma()
        event.key("ObjectBox Version").value(env.objectBoxVersion)

        event.append("}").append("}")

        // https://mixpanel.com/help/reference/http#base64
        val eventEncoded = Base64.encodeBytes(event.toString().toByteArray())

        return BASE_URL + eventEncoded
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

    private fun androidAppIdHash(): String? {
        val appId = androidAppId() ?: return null
        val murmurHash = Murmur3F()
        murmurHash.update(appId.toByteArray())
        return murmurHash.valueHexString
    }

    private fun androidAppId(): String? {
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

    private operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T {
        return getByType(type.java)!!
    }

}
