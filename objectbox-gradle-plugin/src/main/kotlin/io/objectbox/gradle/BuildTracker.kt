package io.objectbox.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.squareup.moshi.JsonWriter
import okio.Buffer
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.greenrobot.essentials.Base64
import org.greenrobot.essentials.hash.Murmur3F
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.*
import kotlin.reflect.KClass

/**
 * Track build errors and anonymous stats.
 */
// Non-final for easier mocking
open class BuildTracker(val toolName: String) {

    private val BASE_URL = "https://api.mixpanel.com/track/?data="
    private val TOKEN = "REPLACE_WITH_TOKEN"
    private val TIMEOUT_READ = 15000
    private val TIMEOUT_CONNECT = 20000

    fun trackBuild(env: ProjectEnv) {
        sendEventAsync("Build", buildEventProperties(env))
    }

    fun trackError(message: String?, throwable: Throwable? = null) {
        sendEventAsync("Error", errorProperties(message, throwable))
    }

    fun trackFatal(message: String?, throwable: Throwable? = null) {
        sendEventAsync("Fatal", errorProperties(message, throwable))
    }

    fun sendEventAsync(eventName: String, eventProperties: String) {
        Thread(Runnable { sendEvent(eventName, eventProperties) }).start()
    }

    private fun sendEvent(eventName: String, eventProperties: String) {
        val event = eventData(eventName, eventProperties)

        // https://mixpanel.com/help/reference/http#base64
        val eventEncoded = Base64.encodeBytes(event.toByteArray())
        try {
            val url = URL(BASE_URL + eventEncoded)
            val con = url.openConnection() as HttpURLConnection
            con.connectTimeout = TIMEOUT_CONNECT
            con.readTimeout = TIMEOUT_READ
            con.requestMethod = "GET"
            con.responseCode
            con.disconnect()
        } catch (ignored: Exception) {
        }
    }

    internal fun eventData(eventName: String, properties: String): String {
        // https://mixpanel.com/help/reference/http#tracking-events
        val event = StringBuilder()
        event.append("{")
        event.key("event").value(eventName).comma()
        event.key("properties").append(" {")

        event.key("token").value(TOKEN).comma()
        event.key("distinct_id").value(uniqueIdentifier()).comma()
        event.key("Tool").value(toolName).comma()

        event.append(properties)

        event.append("}").append("}")
        return event.toString()
    }

    internal fun errorProperties(message: String?, throwable: Throwable?): String {
        val event = StringBuilder()
        if (message != null) {
            event.key("Message").valueEscaped(message).comma()
        }
        event.key("Version").valueEscaped(ProjectEnv.Const.objectBoxVersion)

        if (throwable != null) {
            event.comma()
            var n = 1
            var ex = throwable
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            ex.printStackTrace(printWriter)
            event.key("ExStack").valueEscaped(stringWriter.buffer.toString())
            while (ex != null) {
                event.comma()
                event.key("ExMessage$n").valueEscaped(ex.message ?: "na").comma()
                event.key("ExClass$n").value(ex.javaClass.name)

                if (ex.cause != ex) ex = ex.cause
                n++
            }
        }
        return event.toString()
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

    internal fun hashBase64WithoutPadding(input: String): String {
        val murmurHash = Murmur3F()
        murmurHash.update(input.toByteArray())
        return encodeBase64WithoutPadding(murmurHash.valueBytesBigEndian)
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

    internal fun uniqueIdentifier(): String {
        var file: File? = null
        val fileName = ".objectbox-build.properties"
        try {
            val dir = File(System.getProperty("user.home"))
            if (dir.isDirectory) file = File(dir, fileName)
        } catch (e: Exception) {
            System.err.println("Could not get user dir: " + e) // No stack trace
        }
        if (file == null) file = File(System.getProperty("java.io.tmpdir"), fileName) // Plan B
        val keyUid = "uid"
        var uid: String? = null
        var properties = Properties()
        if (file.exists()) {
            try {
                FileReader(file).use {
                    properties.load(it)
                    uid = properties.getProperty(keyUid)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                properties = Properties()
            }
        }
        if (uid.isNullOrBlank()) {
            val bytes = ByteArray(8)
            SecureRandom().nextBytes(bytes)
            uid = encodeBase64WithoutPadding(bytes)
            properties.put(keyUid, uid)
            FileWriter(file).use {
                properties.store(it, "Properties for ObjectBox build tools")
            }
        }
        return uid!!
    }

    private fun encodeBase64WithoutPadding(valueBytesBigEndian: ByteArray?) =
            Base64.encodeBytes(valueBytesBigEndian).removeSuffix("=").removeSuffix("=")

}
