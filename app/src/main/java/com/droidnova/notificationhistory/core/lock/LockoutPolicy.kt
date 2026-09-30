package com.droidnova.notificationhistory.core.lock

/** Brute-force throttling shared by PIN/password and recovery-code entry. */
object LockoutPolicy {
    /** Wrong entries allowed before the first lockout. */
    const val FREE_ATTEMPTS = 5

    /** 5th wrong → 30 s, 6th → 1 min, 7th → 5 min, 8th → 15 min, 9th and later → 1 h. */
    private val STEPS_MS = longArrayOf(30_000L, 60_000L, 300_000L, 900_000L, 3_600_000L)

    fun lockoutFor(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0L
        return STEPS_MS[(failedAttempts - FREE_ATTEMPTS).coerceAtMost(STEPS_MS.lastIndex)]
    }

    /**
     * Uses only the monotonic `elapsedRealtime` clock, so changing the wall clock can't skip a
     * lockout. `elapsedRealtime` resets on reboot: a "now" below the start means a reboot
     * happened, and the time since boot is then the safe lower bound of the time passed.
     */
    fun remaining(startedElapsed: Long, durationMs: Long, nowElapsed: Long): Long {
        if (durationMs <= 0L) return 0L
        val passed = if (nowElapsed >= startedElapsed) nowElapsed - startedElapsed else nowElapsed
        return (durationMs - passed).coerceAtLeast(0L)
    }
}
