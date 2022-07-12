package io.objectbox.reporting

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`


class BasicBuildTrackerTest {

    @Test
    fun sendTestEvent() {
        // See sendEvent docs: 1 = success
        // Check Mixpanel Live View, the event should show up shortly after this has run.
        assertThat(
            BasicBuildTracker("BasicBuildTrackerTest")
                .sendEventImpl("Test Event", "\"test\":\"success\"", false)
        ).isEqualTo("1")
    }

    /**
     * This test will fail if running on CI and the environment to disable analytics
     * has not been set (see [BasicBuildTracker]).
     */
    @Test
    fun disableAnalyticsFlag() {
        val tracker = BasicBuildTracker("BasicBuildTrackerTest")
        // Check analytics is disabled on CI.
        val isCI = System.getenv("CI") == "true"
        if (isCI) {
            assertThat(tracker.isAnalyticsDisabled).isTrue()
        } else {
            // Check analytics is enabled by default.
            assertThat(tracker.isAnalyticsDisabled).isFalse()
        }

        // Check disabling prevents sending of events.
        val trackerMock = mock(BasicBuildTracker::class.java)
        `when`(trackerMock.isAnalyticsDisabled).thenReturn(true)
        trackerMock.sendEvent("Test Event", "\"test\":\"success\"", false)
        verify(trackerMock, never()).sendEventImpl("", "", false)
        assertThat(trackerMock.shouldSendBuildEvent()).isFalse()


    }

}