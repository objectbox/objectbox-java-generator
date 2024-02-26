/*
 * Copyright (C) 2020-2024 ObjectBox Ltd.
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

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`


class BasicBuildTrackerTest {

    @Test
    fun sendTestEvent() {
        // Skip if token is not set
        // Note: the below test will also succeed with an invalid token, only an empty token will fail it
        @Suppress("KotlinConstantConditions")
        assumeTrue("BasicBuildTracker.TOKEN not set", "REPLACE_WITH_TOKEN" != BasicBuildTracker.TOKEN)

        // See sendEvent docs: 1 = success
        // Check Mixpanel Live View, the event should show up shortly after this has run.
        val tracker = BasicBuildTracker("BasicBuildTrackerTest")
        val event = tracker.eventData("Test Event", "\"test\":\"success\"", false)
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

}