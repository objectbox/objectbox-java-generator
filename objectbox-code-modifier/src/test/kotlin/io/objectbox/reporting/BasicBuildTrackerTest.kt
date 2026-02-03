/*
 * ObjectBox Build Tools
 * Copyright (C) 2020-2024 ObjectBox Ltd.
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

import com.google.common.truth.Truth.assertThat
import com.google.crypto.tink.Aead
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import org.junit.Assume.assumeNotNull
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.*


class BasicBuildTrackerTest {

    /**
     * Creates the contents of the analysis-token.txt file.
     *
     * Obtain the project token from MixPanel project settings,
     * insert it below, then manually run this test.
     */
    @Ignore("Set token and run manually to create token file contents")
    @Test
    fun obfuscateToken() {
        val token = "REPLACE_WITH_TOKEN"

        val obfuscatedToken = obfuscateToken(token)
        deobfuscateAndCheckSame(obfuscatedToken, token)

        val keyset = obfuscatedToken.serializedKeysetBase64
        val obfuscatedTokenBase64 = obfuscatedToken.obfuscatedTokenBase64
        println("Store this in src/main/resources/${BasicBuildTracker.TOKEN_FILE}:");
        println("$keyset\n$obfuscatedTokenBase64");
    }

    /**
     * Checks token decryption works ([sendTestEvent] tests the full integration, but is skipped if decryption fails).
     */
    @Test
    fun tokenEncryptionRoundTrip() {
        val token = "DONT_USE_THIS_TOKEN"

        val obfuscatedToken = obfuscateToken(token)

        deobfuscateAndCheckSame(obfuscatedToken, token)
    }

    private fun deobfuscateAndCheckSame(tokenInfo: ObfuscatedTokenInfo, expectedToken: String) {
        BasicBuildTracker("BasicBuildTrackerTest")
            .deobfuscateToken(tokenInfo.serializedKeysetBase64, tokenInfo.obfuscatedTokenBase64)
            .also { assertThat(it).isEqualTo(expectedToken) }
    }

    /**
     * To run this test locally, create an analysis-token.txt file as suggested by the [obfuscateToken] test and
     * store the expected token in the `JAVA_ANALYSIS_TOKEN` environment variable.
     *
     * For CI, the token file is created using /scripts/set-analysis-token.sh (see .gitlab-ci.yml).
     */
    @Test
    fun sendTestEvent() {
        val expectedToken = System.getenv("JAVA_ANALYSIS_TOKEN")
        assumeNotNull(expectedToken) // Skip if expected token not set
        val tracker = BasicBuildTracker("BasicBuildTrackerTest")
        val token = tracker.getToken()
        assumeNotNull(token) // Skip if no token file available

        // Note: the below test would also succeed with an invalid token, but Mixpanel will ignore the event
        // (only a null/empty token will fail it).
        assertThat(token).isEqualTo(expectedToken)

        // See sendEvent docs: 1 = success
        // Check Mixpanel Live View, the event should show up shortly after this has run.
        val event = tracker.eventData("Test Event", "\"test\":\"success\"", false, token)
        // Note: Bypassing the isAnalyticsDisabled check
        assertThat(tracker.sendEventImpl(event)).isEqualTo("1")
    }

    /**
     * This test will fail if running on CI and the environment to disable analytics
     * has not been set (see [BasicBuildTracker]).
     */
    @Test
    fun disableAnalyticsFlag() {
        val tracker = BasicBuildTracker("BasicBuildTrackerTest")
        // Check that analytics is disabled for tests.
        // If this test fails, check Gradle scripts correctly turn off analytics for tests.
        assertThat(tracker.isAnalyticsDisabled).isTrue()

        // Check disabling prevents sending of events, but as much code as possible runs.
        val trackerMock = mock(BasicBuildTracker::class.java)
        `when`(trackerMock.toolName).thenReturn("BasicBuildTrackerTest")
        `when`(trackerMock.isAnalyticsDisabled).thenReturn(true)
        trackerMock.sendEvent("Test Event", "\"test\":\"success\"", false)
        verify(trackerMock, never()).sendEventImpl(any(BasicBuildTracker.Event::class.java))
    }

    // The Mockito.any(Class) return type declaration is nullable breaking Kotlin checks, wrap in non-null type to fix.
    private fun <T> any(type: Class<T>): T = Mockito.any(type)

    data class ObfuscatedTokenInfo(
        val obfuscatedTokenBase64: String,
        val serializedKeysetBase64: String
    )

    private fun obfuscateToken(@Suppress("SameParameterValue") token: String): ObfuscatedTokenInfo {
        AeadConfig.register()

        val handle = KeysetHandle.generateNew(PredefinedAeadParameters.CHACHA20_POLY1305)
        val serializedKeyset = TinkProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get())

        val aead = handle.getPrimitive(Aead::class.java)

        val message = token.encodeToByteArray()
        val ciphertext = aead.encrypt(message, BasicBuildTracker.TOKEN_EMPTY_ASSOCIATED_DATA)

        val serializedKeysetBase64 = Base64.getEncoder().encodeToString(serializedKeyset)
        val obfuscatedTokenBase64 = Base64.getEncoder().encodeToString(ciphertext)
        return ObfuscatedTokenInfo(obfuscatedTokenBase64, serializedKeysetBase64)
    }
}