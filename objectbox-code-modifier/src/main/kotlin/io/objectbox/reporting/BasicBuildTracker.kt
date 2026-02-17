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

package io.objectbox.reporting

import com.google.crypto.tink.Aead
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.squareup.moshi.JsonWriter
import io.objectbox.CodeModifierBuildConfig
import okio.Buffer
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
open class BasicBuildTracker(
    // open and public for testing purposes only.
    open val toolName: String
) {
    companion object {
        /**
         * If this environment variable contains a value of "true" no events are sent,
         * this is useful to prevent many events being sent by CI builds.
         */
        private const val ENV_VAR_DISABLE_ANALYTICS = "OBX_DISABLE_ANALYTICS"

        private const val PROPERTIES_KEY_UID = "uid"
        private const val PROPERTIES_KEY_LAST_DAY_BUILD_SENT = "lastBuildEvent"
        private const val PROPERTIES_KEY_BUILD_COUNT = "buildCount"

        private const val HOUR_IN_MILLIS = 3600 * 1000

        // https://developer.mixpanel.com/reference/events#track-event
        // Note: adding query param `ip=1` only sets request IP as `distinct_id` if that has no value, yet.
        // For all but the NoBuildProperties event a unique ID is generated and set,
        // so no point in setting the ip param (is better for privacy anyhow).
        const val BASE_URL = "https://api.mixpanel.com/track#live-event"

        // Note: the Gradle build script contains the checkAnalysisToken task that expects this file name.
        // So update the task if changing it.
        const val TOKEN_FILE = "analysis-token.txt"
        val TOKEN_EMPTY_ASSOCIATED_DATA = ByteArray(0)
        const val TIMEOUT_READ = 15000
        const val TIMEOUT_CONNECT = 20000
    }

    private val buildPropertiesFile = BuildPropertiesFile(object : BuildPropertiesFile.FileCreateListener {
        override fun onFailedToCreateFile(message: String, e: Exception) {
            trackNoBuildPropertiesFile(message, e)
        }
    })

    /** Open for testing purposes only. */
    open val isAnalyticsDisabled: Boolean = System.getenv(ENV_VAR_DISABLE_ANALYTICS) == "true"
    var disconnect = true

    /**
     * Returns true if the time stamp of the last sent build event in the build properties file does not exist or is
     * older than 24 hours. If so updates the time stamp to the current time.
     */
    fun shouldSendBuildEvent(): Boolean {
        val timeProperty: String? = buildPropertiesFile.properties.getProperty(PROPERTIES_KEY_LAST_DAY_BUILD_SENT)
        val timestamp = timeProperty?.toLongOrNull()
        return if (
            timestamp == null || timestamp < System.currentTimeMillis() - 24 * HOUR_IN_MILLIS || isAnalyticsDisabled
        ) {
            if (isAnalyticsDisabled) {
                println("[ObjectBox] Analytics disabled, skip sent within last day check.")
            }
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
        sendEvent("Error", errorProperties(message, throwable))
    }

    fun trackFatal(message: String?, throwable: Throwable? = null) {
        sendEvent("Fatal", errorProperties(message, throwable))
    }

    fun trackNoBuildPropertiesFile(message: String?, throwable: Throwable? = null) {
        sendEvent("NoBuildProperties", errorProperties(message, throwable), false)
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
        sendEvent("Stats", event.toString())
    }

    fun getToken(): String? {
        try {
            javaClass.classLoader.getResourceAsStream(TOKEN_FILE)?.use {
                val lines = it.bufferedReader().readLines()
                if (lines.size >= 2) {
                    return deobfuscateToken(lines[0], lines[1])
                }
            }
        } catch (_: Exception) {
            // Ignore
        }
        return null
    }

    /**
     * Takes a Base64 encoded keyset and obfuscated token and returns the decrypted token.
     */
    fun deobfuscateToken(serializedKeysetBase64: String, obfuscatedTokenBase64: String): String {
        val obfuscatedToken = Base64.getDecoder().decode(obfuscatedTokenBase64)
        val serializedKeyset = Base64.getDecoder().decode(serializedKeysetBase64)

        AeadConfig.register()

        val handle = TinkProtoKeysetFormat.parseKeyset(serializedKeyset, InsecureSecretKeyAccess.get())
        val aead = handle.getPrimitive(Aead::class.java)

        return aead.decrypt(obfuscatedToken, TOKEN_EMPTY_ASSOCIATED_DATA).toString(charset = Charsets.UTF_8)
    }

    fun sendEvent(eventName: String, eventProperties: String, sendUniqueId: Boolean = true) {
        if (sendUniqueId && buildPropertiesFile.hasNoFile) {
            return // can not save state (e.g. unique ID) so do not send events
        }
        // Only get token if enabled to prevent leaking it in log message below
        val token: String? = if (isAnalyticsDisabled) null else getToken()
        val event = eventData(eventName, eventProperties, sendUniqueId, token)
        if (token.isNullOrEmpty()) {
            println("[ObjectBox] Analytics disabled, would have sent event: $event")
        } else {
            // Note: never run this in a thread! Code is executed as part of a Gradle build, so if run in a thread it may
            // run on a totally different classpath with e.g. Kotlin API missing or not run at all.
            // https://github.com/objectbox/objectbox-java/issues/946
            // Might be able to use incubating Gradle Worker API (since 5.6) for callers from an explicit Gradle task
            // (e.g. trackBuild), but most callers do not have access to Gradle API (e.g. processor, transformer).
            sendEventImpl(event)
        }
    }

    /**
     * Public and open for testing purposes only.
     *
     * Returns 1 if all data objects provided are valid. This does not signify a valid project token or secret.
     *
     * Returns 0 if one or more data objects in the body are invalid.
     */
    open fun sendEventImpl(event: Event): String? {
        // https://developer.mixpanel.com/reference/events#track-event
        try {
            val url = URL(BASE_URL)
            val con = url.openConnection() as HttpURLConnection
            con.connectTimeout = TIMEOUT_CONNECT
            con.readTimeout = TIMEOUT_READ
            con.setRequestProperty("Accept", "text/plain")
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            con.doOutput = true // POST
            con.outputStream.bufferedWriter().use {
                it.write("data=$event")
            }
            val response = con.inputStream.bufferedReader().readLine()
            if (disconnect) con.disconnect()
            return response
        } catch (_: Exception) {
        }
        return null
    }

    data class Event(val json: String) {
        override fun toString(): String = json
    }

    // public for tests in another module
    fun eventData(eventName: String, properties: String, addUniqueId: Boolean, token: String?): Event {
        // https://developer.mixpanel.com/docs/data-structure-deep-dive#anatomy-of-an-event
        val event = StringBuilder()
        event.append("{")
        event.key("event").value(eventName).comma()
        event.key("properties").append("{")

        if (token != null) {
            event.key("token").value(token).comma()
        }
        if (addUniqueId) {
            event.key("distinct_id").value(uniqueIdentifier()).comma()
        }
        event.key("Tool").value(toolName).comma()
        try {
            val locale = Locale.getDefault()
            val language = locale.isO3Language // ISO 639-2 (three letters).
            val country = locale.isO3Country // ISO 3166-1 alpha-3 (three letters).
            event.key("lang").append("\"$language\"").comma()
            event.key("c").append("\"$country\"").comma()
        } catch (e: Exception) {
            // Ignore
        }

        event.append(properties)

        event.append("}").append("}")
        return Event(event.toString())
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
        Base64.getEncoder().encodeToString(valueBytesBigEndian).removeSuffix("=").removeSuffix("=")

}
