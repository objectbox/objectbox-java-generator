package io.objectbox.build

import com.squareup.moshi.JsonWriter
import okio.Buffer
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

/**
 * Track build errors for non-Gradle modules.
 */
// Non-final for easier mocking
open class BasicBuildTracker(val toolName: String) {
    private companion object {
        const val BASE_URL = "https://api.mixpanel.com/track/?data="
        const val TOKEN = "REPLACE_WITH_TOKEN"
        const val TIMEOUT_READ = 15000
        const val TIMEOUT_CONNECT = 20000
    }

    var disconnect = true

    fun trackError(message: String?, throwable: Throwable? = null) {
        sendEventAsync("Error", errorProperties(message, throwable))
    }

    fun trackFatal(message: String?, throwable: Throwable? = null) {
        sendEventAsync("Fatal", errorProperties(message, throwable))
    }

    fun trackStats(completed: Boolean, daoCompat: Boolean, entityCount: Int, propertyCount: Int, toOneCount: Int, toManyCount: Int) {
        val event = StringBuilder()
        event.key("DC").value(daoCompat.toString()).comma()
        event.key("EC").value(entityCount.toString()).comma()
        event.key("PC").value(propertyCount.toString()).comma()
        event.key("T1C").value(toOneCount.toString()).comma()
        event.key("TMC").value(toManyCount.toString()).comma()
        event.key("OK").value(completed.toString())
        sendEventAsync("Stats", event.toString())
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
            if (disconnect) con.disconnect()
        } catch (ignored: Exception) {
        }
    }

    // public for tests in another module
    fun eventData(eventName: String, properties: String): String {
        // https://mixpanel.com/help/reference/http#tracking-events
        val event = StringBuilder()
        event.append("{")
        event.key("event").value(eventName).comma()
        event.key("properties").append(" {")

        event.key("token").value(TOKEN).comma()
        event.key("distinct_id").value(uniqueIdentifier()).comma()
        event.key("ip").append("true").comma()
//        event.key("ip").value("1").comma()
        event.key("Tool").value(toolName).comma()
        try {
            val locale = Locale.getDefault()
            val language = locale.isO3Language
            val country = locale.isO3Country
            event.key("lang").append("\"$language\"").comma()
            event.key("c").append("\"$country\"").comma()
        } catch (e: Exception) {
            // Ignore
        }

        event.append(properties)

        event.append("}").append("}")
        return event.toString()
    }

    open protected fun version(): String? = "1.4.1" // TODO replace with generated property, or update frequently...

    // public for tests in another module
    fun errorProperties(message: String?, throwable: Throwable?): String {
        val event = StringBuilder()
        if (message != null) {
            event.key("Message").valueEscaped(message).comma()
        }
        val version = version()
        if (version != null) {
            event.key("Version").valueEscaped(version)
        }

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

    protected fun StringBuilder.key(value: String): java.lang.StringBuilder {
        append("\"$value\": ")
        return this
    }

    protected fun StringBuilder.value(value: String): java.lang.StringBuilder {
        append("\"$value\"")
        return this
    }

    protected fun StringBuilder.valueEscaped(value: String): java.lang.StringBuilder {
        val buffer = Buffer()
        JsonWriter.of(buffer).value(value)
        append(buffer.readUtf8())
        return this
    }

    protected fun StringBuilder.comma(): java.lang.StringBuilder {
        append(',')
        return this
    }

    fun hashBase64WithoutPadding(input: String): String {
        val murmurHash = Murmur3F()
        murmurHash.update(input.toByteArray())
        // 8 bytes are enough
        val bytes = murmurHash.valueBytesBigEndian.copyOfRange(0, 8)
        return encodeBase64WithoutPadding(bytes)
    }

    // public for tests in another module
    fun uniqueIdentifier(): String {
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

    protected fun encodeBase64WithoutPadding(valueBytesBigEndian: ByteArray?) =
            Base64.encodeBytes(valueBytesBigEndian).removeSuffix("=").removeSuffix("=")

}
