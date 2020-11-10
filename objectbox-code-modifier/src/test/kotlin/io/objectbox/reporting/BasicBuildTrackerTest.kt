package io.objectbox.reporting

import com.google.common.truth.Truth.assertThat
import org.junit.Test


class BasicBuildTrackerTest {

    @Test
    fun sendTestEvent() {
        // See sendEvent docs: 1 = success
        // Check Mixpanel Live View, the event should show up shortly after this has run.
        assertThat(
            BasicBuildTracker("BasicBuildTrackerTest")
                .sendEvent("Test Event", "\"test\":\"success\"", false)
        ).isEqualTo("1")
    }

}