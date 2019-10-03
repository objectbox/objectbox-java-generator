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

package io.objectbox.reporting

import com.squareup.moshi.JsonWriter
import io.objectbox.CodeModifierBuildConfig
import okio.Buffer
import org.greenrobot.essentials.Base64
import org.greenrobot.essentials.hash.Murmur3F
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
open class BasicBuildTracker(private val toolName: String) {
    private companion object {
        private const val PROPERTIES_KEY_UID = "uid"
        private const val PROPERTIES_KEY_LAST_DAY_BUILD_SENT = "lastBuildEvent"
        private const val PROPERTIES_KEY_BUILD_COUNT = "buildCount"

        private const val HOUR_IN_MILLIS = 3600 * 1000

        // https://developer.mixpanel.com/docs/http
        const val BASE_URL = "https://api.mixpanel.com/track/?data="
        const val TOKEN = "REPLACE_WITH_TOKEN"
        const val TIMEOUT_READ = 15000
        const val TIMEOUT_CONNECT = 20000
    }

    private val buildPropertiesFile = BuildPropertiesFile(object : BuildPropertiesFile.FileCreateListener {
        override fun onFailedToCreateFile(message: String, e: Exception) {
            trackNoBuildPropertiesFile(message, e)
        }
    })

    var disconnect = true

    /**
     * Returns true if the time stamp of the last sent build event in the build properties file does not exist or is
     * older than 24 hours. If so updates the time stamp to the current time.
     */
    fun shouldSendBuildEvent(): Boolean {
        val timeProperty: String? = buildPropertiesFile.properties.getProperty(PROPERTIES_KEY_LAST_DAY_BUILD_SENT)
        val timestamp = timeProperty?.toLongOrNull()
        return if (timestamp == null || timestamp < System.currentTimeMillis() - 24 * HOUR_IN_MILLIS) {
            // set last sent to current time
            buildPropertiesFile.properties[PROPERTIES_KEY_LAST_DAY_BUILD_SENT] = System.currentTimeMillis().toString()
            buildPropertiesFile.write()
            true // allow sending
        } else {
            false // prevent sending
        }
    }

    /**
     * Increments the build counter. To reset the counter see [getAndResetBuildCount].
     */
    fun countBuild() {
        val countProperty: String? = buildPropertiesFile.properties.getProperty(PROPERTIES_KEY_BUILD_COUNT)
        val buildCount = countProperty?.toIntOrNull()
        val newBuildCount = if (buildCount == null || buildCount < 0) {
            1
        } else {
            buildCount + 1
        }
        buildPropertiesFile.properties[PROPERTIES_KEY_BUILD_COUNT] = newBuildCount.toString()
        buildPropertiesFile.write()
    }

    /**
     * Gets the number of builds that were counted so far, or 1 if none were counted. Resets the counter to 0.
     *
     * Builds are counted with [countBuild].
     */
    fun getAndResetBuildCount(): Int {
        val countProperty: String? = buildPropertiesFile.properties.getProperty(PROPERTIES_KEY_BUILD_COUNT)

        buildPropertiesFile.properties[PROPERTIES_KEY_BUILD_COUNT] = "0"
        buildPropertiesFile.write()

        return countProperty?.toIntOrNull() ?: 1
    }

    fun trackError(message: String?, throwable: Throwable? = null) {
        sendEventAsync("Error", errorProperties(message, throwable))
    }

    fun trackFatal(message: String?, throwable: Throwable? = null) {
        sendEventAsync("Fatal", errorProperties(message, throwable))
    }

    fun trackNoBuildPropertiesFile(message: String?, throwable: Throwable? = null) {
        sendEventAsync("NoBuildProperties", errorProperties(message, throwable), false)
    }

    fun trackStats(
        completed: Boolean,
        daoCompat: Boolean,
        entityCount: Int,
        propertyCount: Int,
        toOneCount: Int,
        toManyCount: Int
    ) {
        val event = StringBuilder()
        event.key("DC").value(daoCompat.toString()).comma()
        event.key("EC").value(entityCount.toString()).comma()
        event.key("PC").value(propertyCount.toString()).comma()
        event.key("T1C").value(toOneCount.toString()).comma()
        event.key("TMC").value(toManyCount.toString()).comma()
        event.key("OK").value(completed.toString())
        sendEventAsync("Stats", event.toString())
    }

    fun sendEventAsync(eventName: String, eventProperties: String, sendUniqueId: Boolean = true) {
        if (sendUniqueId && buildPropertiesFile.hasNoFile) {
            return // can not save state (e.g. unique ID) so do not send events
        }
        Thread(Runnable { sendEvent(eventName, eventProperties, sendUniqueId) }).start()
    }

    private fun sendEvent(eventName: String, eventProperties: String, sendUniqueId: Boolean) {
        val event = eventData(eventName, eventProperties, sendUniqueId)

        // https://developer.mixpanel.com/docs/http#section-base64-for-mixpanel
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
    fun eventData(eventName: String, properties: String, addUniqueId: Boolean): String {
        // https://developer.mixpanel.com/docs/http#section-tracking-events
        val event = StringBuilder()
        event.append("{")
        event.key("event").value(eventName).comma()
        event.key("properties").append(" {")

        event.key("token").value(TOKEN).comma()
        if (addUniqueId) {
            event.key("distinct_id").value(uniqueIdentifier()).comma()
        }
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

    protected open fun version(): String? = CodeModifierBuildConfig.VERSION

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
        val bytes = murmurHash.valueBytesBigEndian.copyOf(8)
        return encodeBase64WithoutPadding(bytes)
    }

    // public for tests in another module
    fun uniqueIdentifier(): String {
        val uid: String? = buildPropertiesFile.properties.getProperty(PROPERTIES_KEY_UID)

        return if (uid.isNullOrBlank()) {
            val bytes = ByteArray(8)
            SecureRandom().nextBytes(bytes)
            val newUid = encodeBase64WithoutPadding(bytes)
            buildPropertiesFile.properties[PROPERTIES_KEY_UID] = newUid
            buildPropertiesFile.write()
            newUid
        } else {
            uid
        }
    }

    private fun encodeBase64WithoutPadding(valueBytesBigEndian: ByteArray?) =
        Base64.encodeBytes(valueBytesBigEndian).removeSuffix("=").removeSuffix("=")

}
