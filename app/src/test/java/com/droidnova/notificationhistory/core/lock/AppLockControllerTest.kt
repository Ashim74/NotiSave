package com.droidnova.notificationhistory.core.lock

import com.droidnova.notificationhistory.core.lock.AppLockController.Gate
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLockControllerTest {

    private var now = 10_000L
    private val controller = AppLockController { now }

    private fun leaveFor(ms: Long) {
        controller.onAppStopped()
        now += ms
        controller.onAppStarted()
    }

    @Test
    fun `cold start is locked only when the lock is on`() {
        assertEquals(Gate.Loading, controller.gate.value)
        controller.onConfig(enabled = true, timeoutMs = 0L)
        assertEquals(Gate.Locked, controller.gate.value)

        val other = AppLockController { now }
        other.onConfig(enabled = false, timeoutMs = 0L)
        assertEquals(Gate.Open, other.gate.value)
    }

    @Test
    fun `immediately relocks on any return`() {
        controller.onConfig(enabled = true, timeoutMs = 0L)
        controller.unlock()
        leaveFor(1L)
        assertEquals(Gate.Locked, controller.gate.value)
    }

    @Test
    fun `timeout lets short trips through and locks longer ones`() {
        controller.onConfig(enabled = true, timeoutMs = 60_000L)
        controller.unlock()
        leaveFor(30_000L)
        assertEquals(Gate.Open, controller.gate.value)
        leaveFor(60_000L)
        assertEquals(Gate.Locked, controller.gate.value)
    }

    @Test
    fun `in-app hand-off does not relock, but only within the grace period`() {
        controller.onConfig(enabled = true, timeoutMs = 0L)
        controller.unlock()

        controller.beginHandoff()
        leaveFor(60_000L)
        assertEquals(Gate.Open, controller.gate.value)

        controller.beginHandoff()
        leaveFor(AppLockController.HANDOFF_GRACE_MS + 1)
        assertEquals(Gate.Locked, controller.gate.value)
    }

    @Test
    fun `hand-off is dropped once the app resumes without leaving`() {
        controller.onConfig(enabled = true, timeoutMs = 0L)
        controller.unlock()
        controller.beginHandoff()
        controller.onAppResumed()
        leaveFor(1_000L)
        assertEquals(Gate.Locked, controller.gate.value)
    }

    @Test
    fun `enabling from settings keeps the user inside, disabling opens the gate`() {
        controller.onConfig(enabled = false, timeoutMs = 0L)
        controller.onConfig(enabled = true, timeoutMs = 0L)
        assertEquals(Gate.Open, controller.gate.value)

        leaveFor(1L)
        assertEquals(Gate.Locked, controller.gate.value)
        controller.onConfig(enabled = false, timeoutMs = 0L)
        assertEquals(Gate.Open, controller.gate.value)
    }

    @Test
    fun `start without a prior stop never locks`() {
        controller.onConfig(enabled = true, timeoutMs = 0L)
        controller.unlock()
        now += 1_000_000L
        controller.onAppStarted()
        assertEquals(Gate.Open, controller.gate.value)
    }
}
