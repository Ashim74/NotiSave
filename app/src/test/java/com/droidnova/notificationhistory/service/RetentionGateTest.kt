package com.droidnova.notificationhistory.service

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionGateTest {

    private val intervalMs = TimeUnit.HOURS.toMillis(6)

    @Test
    fun `first acquire always succeeds`() {
        val gate = RetentionGate(intervalMs)
        assertTrue(gate.tryAcquire(now = 1_000L))
    }

    @Test
    fun `second acquire within interval is rejected`() {
        val gate = RetentionGate(intervalMs)
        assertTrue(gate.tryAcquire(now = 1_000L))
        assertFalse(gate.tryAcquire(now = 1_000L + intervalMs - 1))
    }

    @Test
    fun `acquire succeeds again after interval elapses`() {
        val gate = RetentionGate(intervalMs)
        assertTrue(gate.tryAcquire(now = 1_000L))
        assertTrue(gate.tryAcquire(now = 1_000L + intervalMs))
    }

    @Test
    fun `clock moving backwards does not lock the gate forever`() {
        val gate = RetentionGate(intervalMs)
        assertTrue(gate.tryAcquire(now = 1_000_000L))
        // Device clock jumped back before the recorded run time.
        assertTrue(gate.tryAcquire(now = 500L))
    }

    @Test
    fun `concurrent acquires admit exactly one caller`() {
        val gate = RetentionGate(intervalMs)
        val threads = 16
        val ready = CountDownLatch(threads)
        val go = CountDownLatch(1)
        val admitted = AtomicInteger(0)
        val now = 42_000L

        val workers = (1..threads).map {
            Thread {
                ready.countDown()
                go.await()
                if (gate.tryAcquire(now)) admitted.incrementAndGet()
            }.apply { start() }
        }
        ready.await()
        go.countDown()
        workers.forEach { it.join() }

        assertEquals(1, admitted.get())
    }
}
