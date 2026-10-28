package com.droidnova.notificationhistory

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenerStatusDisplayTest {

    @Test
    fun `live connection shows recording`() {
        assertTrue(displayListenerConnected(persisted = false, live = true, startupGraceOver = true))
    }

    @Test
    fun `persisted flag bridges a cold start while the system binds`() {
        assertTrue(displayListenerConnected(persisted = true, live = false, startupGraceOver = false))
    }

    @Test
    fun `flag says connected but live says not after the grace is shown as disconnected`() {
        assertFalse(displayListenerConnected(persisted = true, live = false, startupGraceOver = true))
    }

    @Test
    fun `disconnected everywhere is disconnected even during the grace`() {
        assertFalse(displayListenerConnected(persisted = false, live = false, startupGraceOver = false))
    }
}
