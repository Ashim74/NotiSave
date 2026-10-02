package com.droidnova.notificationhistory.core.review

import java.util.concurrent.TimeUnit

/** What the review policy needs to know; read from preferences. */
data class ReviewState(
    val firstOpenAt: Long,
    val launchCount: Int,
    /** Times the app clearly helped: a saved notification read, copied, opened or restored. */
    val valueMoments: Int,
    val lastAskedAt: Long?,
    val askCount: Int
)

/**
 * When to show Google Play's in-app review sheet. The best ratings come from asking right after
 * the app has just helped, from someone who has used it for a while, and never too often:
 *
 * - the app has been around for [MIN_DAYS_INSTALLED] days and opened [MIN_LAUNCHES] times,
 * - it has helped at least [MIN_VALUE_MOMENTS] times (so this isn't a first impression),
 * - at least [COOLDOWN_DAYS] days since we last asked, and at most [MAX_ASKS] asks ever.
 *
 * Play also applies its own quota and may silently skip the sheet; that is expected.
 * We never ask the user's opinion first: Play policy forbids gating the review on it.
 */
object ReviewPolicy {
    const val MIN_DAYS_INSTALLED = 2L
    const val MIN_LAUNCHES = 3
    const val MIN_VALUE_MOMENTS = 3
    const val COOLDOWN_DAYS = 60L
    const val MAX_ASKS = 3

    fun shouldAsk(state: ReviewState, now: Long): Boolean {
        if (state.askCount >= MAX_ASKS) return false
        if (now - state.firstOpenAt < TimeUnit.DAYS.toMillis(MIN_DAYS_INSTALLED)) return false
        if (state.launchCount < MIN_LAUNCHES) return false
        if (state.valueMoments < MIN_VALUE_MOMENTS) return false
        val last = state.lastAskedAt ?: return true
        return now - last >= TimeUnit.DAYS.toMillis(COOLDOWN_DAYS)
    }
}
