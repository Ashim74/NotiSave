package com.droidnova.notificationhistory.core.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ReviewPolicyTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val now = 1_000 * day

    /** A user who qualifies on every rule; each test breaks exactly one. */
    private val ready = ReviewState(
        firstOpenAt = now - 5 * day,
        launchCount = 5,
        valueMoments = 4,
        lastAskedAt = null,
        askCount = 0
    )

    @Test
    fun `asks an established user right after the app helped`() {
        assertTrue(ReviewPolicy.shouldAsk(ready, now))
    }

    @Test
    fun `waits until the app has been installed for a couple of days`() {
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(firstOpenAt = now - day), now))
    }

    @Test
    fun `waits for enough launches`() {
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(launchCount = 2), now))
    }

    @Test
    fun `waits until the app has helped a few times`() {
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(valueMoments = 2), now))
    }

    @Test
    fun `respects the cooldown after asking`() {
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(lastAskedAt = now - 10 * day, askCount = 1), now))
        assertTrue(ReviewPolicy.shouldAsk(ready.copy(lastAskedAt = now - 61 * day, askCount = 1), now))
    }

    @Test
    fun `stops after the maximum number of asks`() {
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(lastAskedAt = now - 365 * day, askCount = 3), now))
    }
}
