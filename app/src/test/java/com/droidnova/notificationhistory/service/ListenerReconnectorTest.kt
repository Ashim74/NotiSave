package com.droidnova.notificationhistory.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ListenerReconnectorTest {

    private val window = 15_000L

    private fun action(
        hasPermission: Boolean = true,
        isConnected: Boolean = false,
        lastAttemptAt: Long = 0L,
        now: Long = 100_000L
    ) = nextReconnectAction(
        hasPermission = hasPermission,
        isConnected = isConnected,
        lastAttemptAt = lastAttemptAt,
        now = now,
        escalationWindowMs = window
    )

    @Test
    fun `no permission means no action`() {
        assertEquals(ReconnectAction.NONE, action(hasPermission = false))
    }

    @Test
    fun `connected listener needs no action`() {
        assertEquals(ReconnectAction.NONE, action(isConnected = true))
    }

    @Test
    fun `first disconnection attempts a plain rebind`() {
        assertEquals(ReconnectAction.REQUEST_REBIND, action(lastAttemptAt = 0L))
    }

    @Test
    fun `recent attempt is given time to settle`() {
        assertEquals(
            ReconnectAction.NONE,
            action(lastAttemptAt = 100_000L - window + 1, now = 100_000L)
        )
    }

    @Test
    fun `stale attempt escalates to component toggle`() {
        assertEquals(
            ReconnectAction.FORCE_TOGGLE,
            action(lastAttemptAt = 100_000L - window, now = 100_000L)
        )
    }

    @Test
    fun `clock moving backwards restarts with a plain rebind`() {
        assertEquals(
            ReconnectAction.REQUEST_REBIND,
            action(lastAttemptAt = 200_000L, now = 100_000L)
        )
    }
}
