package com.droidnova.notificationhistory.service

import java.util.concurrent.atomic.AtomicLong

/**
 * Rate-limits retention cleanup to at most once per [intervalMs].
 * Thread-safe via CAS; the first caller in a window wins, concurrent callers lose.
 */
class RetentionGate(private val intervalMs: Long) {

    private val lastRunAt = AtomicLong(0L)

    fun tryAcquire(now: Long): Boolean {
        while (true) {
            val last = lastRunAt.get()
            val elapsed = now - last
            // A negative elapsed means the clock moved backwards; treat the window as expired.
            if (last != 0L && elapsed >= 0 && elapsed < intervalMs) return false
            if (lastRunAt.compareAndSet(last, now)) return true
        }
    }
}
