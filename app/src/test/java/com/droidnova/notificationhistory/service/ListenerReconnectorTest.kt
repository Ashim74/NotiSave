package com.droidnova.notificationhistory.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ListenerReconnectorTest {

    private val window = 15_000L
    private val cooldown = 600_000L
    private val now = 10_000_000L

    private fun action(
        hasPermission: Boolean = true,
        isConnected: Boolean = false,
        lastAttemptAt: Long = 0L,
        lastForceToggleAt: Long = 0L,
        now: Long = this.now
    ) = nextReconnectAction(
        hasPermission = hasPermission,
        isConnected = isConnected,
        lastAttemptAt = lastAttemptAt,
        lastForceToggleAt = lastForceToggleAt,
        now = now,
        escalationWindowMs = window,
        forceToggleCooldownMs = cooldown
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
        assertEquals(ReconnectAction.NONE, action(lastAttemptAt = now - window + 1))
    }

    @Test
    fun `stale attempt escalates to component toggle`() {
        assertEquals(ReconnectAction.FORCE_TOGGLE, action(lastAttemptAt = now - window))
    }

    @Test
    fun `clock moving backwards restarts with a plain rebind`() {
        assertEquals(ReconnectAction.REQUEST_REBIND, action(lastAttemptAt = now + 1))
    }

    // Persisted state: the watchdog's next run is ~15 minutes later, often in a new process.

    @Test
    fun `persisted attempt from the previous watchdog run escalates`() {
        assertEquals(
            ReconnectAction.FORCE_TOGGLE,
            action(lastAttemptAt = now - 15 * 60_000L)
        )
    }

    @Test
    fun `toggle within the cooldown falls back to a rebind`() {
        assertEquals(
            ReconnectAction.REQUEST_REBIND,
            action(lastAttemptAt = now - window, lastForceToggleAt = now - cooldown + 1)
        )
    }

    @Test
    fun `toggle is allowed again once the cooldown has passed`() {
        assertEquals(
            ReconnectAction.FORCE_TOGGLE,
            action(lastAttemptAt = now - window, lastForceToggleAt = now - cooldown)
        )
    }

    @Test
    fun `toggle timestamp in the future does not block escalation forever`() {
        assertEquals(
            ReconnectAction.FORCE_TOGGLE,
            action(lastAttemptAt = now - window, lastForceToggleAt = now + 60_000L)
        )
    }

    @Test
    fun `cooldown never suppresses the first rebind`() {
        assertEquals(
            ReconnectAction.REQUEST_REBIND,
            action(lastAttemptAt = 0L, lastForceToggleAt = now - 1)
        )
    }
}

class ListenerHealthTest {

    private fun health(
        hasPermission: Boolean = true,
        trackingEnabled: Boolean = true,
        persistedConnected: Boolean = false,
        liveConnected: Boolean = false
    ) = assessListenerHealth(hasPermission, trackingEnabled, persistedConnected, liveConnected)

    @Test
    fun `flag says connected but live says not is a silent kill`() {
        assertEquals(
            ListenerHealth.SILENTLY_KILLED,
            health(persistedConnected = true, liveConnected = false)
        )
    }

    @Test
    fun `both flag and live say disconnected is a plain disconnect`() {
        assertEquals(ListenerHealth.DISCONNECTED, health())
    }

    @Test
    fun `live connection is healthy whatever the flag says`() {
        assertEquals(ListenerHealth.HEALTHY, health(persistedConnected = true, liveConnected = true))
        assertEquals(ListenerHealth.HEALTHY, health(persistedConnected = false, liveConnected = true))
    }

    @Test
    fun `nothing is expected without notification access`() {
        assertEquals(
            ListenerHealth.NOT_EXPECTED,
            health(hasPermission = false, persistedConnected = true)
        )
    }

    @Test
    fun `nothing is expected while the user has tracking off`() {
        assertEquals(
            ListenerHealth.NOT_EXPECTED,
            health(trackingEnabled = false, persistedConnected = true)
        )
    }
}
