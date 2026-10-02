package com.droidnova.notificationhistory.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Funnel events for the 0.20 redesign. Every call is wrapped so analytics can never crash
 * the app; [init] must run once (MainActivity) before the context-free [log] does anything.
 */
object Analytics {
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val HOME_OPEN = "home_open"
    const val HISTORY_OPEN = "history_open"
    const val INSIGHTS_OPEN = "insights_open"
    const val RECONNECT_TAPPED = "reconnect_tapped"
    const val LISTENER_ALERT_SHOWN = "listener_alert_shown"
    const val APP_LOCK_ENABLED = "app_lock_enabled"
    const val APP_LOCK_DISABLED = "app_lock_disabled"
    const val APP_LOCK_CHANGED = "app_lock_changed"
    const val APP_LOCK_RECOVERED = "app_lock_recovered"
    const val REVIEW_PROMPTED = "review_prompted"
    const val REWARDED_SHOWN = "rewarded_ad_shown"
    const val REWARDED_EARNED = "rewarded_ad_free_earned"
    const val APP_LOCK_LOCKOUT = "app_lock_lockout"

    // Event params. Never the PIN, its length or any hash.
    const val PARAM_METHOD = "method"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun log(event: String, params: Bundle? = null) {
        appContext?.let { log(it, event, params) }
    }

    fun log(context: Context, event: String, params: Bundle? = null) {
        runCatching { FirebaseAnalytics.getInstance(context).logEvent(event, params) }
    }
}
