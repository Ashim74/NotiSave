package com.droidnova.notificationhistory.service

import com.droidnova.notificationhistory.service.ActiveContentTracker.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveContentTrackerTest {

    @Test
    fun `first post of a key is unknown`() {
        val tracker = ActiveContentTracker()

        assertEquals(Verdict.Unknown, tracker.onPosted("key", "a"))
    }

    @Test
    fun `re-post with the same content is unchanged`() {
        val tracker = ActiveContentTracker()
        tracker.onPosted("key", "a")

        assertEquals(Verdict.Unchanged, tracker.onPosted("key", "a"))
    }

    @Test
    fun `update with new content is changed`() {
        val tracker = ActiveContentTracker()
        tracker.onPosted("key", "a")

        assertEquals(Verdict.Changed, tracker.onPosted("key", "b"))
    }

    @Test
    fun `same content after dismissal is a new notification`() {
        val tracker = ActiveContentTracker()
        tracker.onPosted("key", "a")
        tracker.onRemoved("key")

        assertEquals(Verdict.Changed, tracker.onPosted("key", "a"))
        assertEquals(Verdict.Unchanged, tracker.onPosted("key", "a"))
    }

    @Test
    fun `oldest keys are evicted beyond max size`() {
        val tracker = ActiveContentTracker(maxSize = 2)
        tracker.onPosted("k1", "a")
        tracker.onPosted("k2", "a")
        tracker.onPosted("k3", "a")

        assertEquals(Verdict.Unknown, tracker.onPosted("k1", "a"))
    }
}
