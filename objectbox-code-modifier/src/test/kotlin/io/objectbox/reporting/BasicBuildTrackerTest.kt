package io.objectbox.reporting

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`


class BasicBuildTrackerTest {

    @Test
    fun sendTestEvent() {
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