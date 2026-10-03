package com.droidnova.notificationhistory.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object DataStoreKeys {
    const val PREF_NAME = "notification_prefs"
    val KEY_USER_WANTS_TRACKING = booleanPreferencesKey("user_wants_tracking")
    const val ALLOWED_APPS_KEY = "allowed_apps"
    val LAUNCH_COUNT = intPreferencesKey("launch_count")
    val SHOW_RATE_US_CARD = booleanPreferencesKey("show_us_rate_card")
    val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
    val IS_PREMIUM = booleanPreferencesKey("is_premium")
    val LISTENER_CONNECTED = booleanPreferencesKey("listener_connected")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val THEME_COLOR = stringPreferencesKey("theme_color")
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    const val FILTERS_PREFIX = "filters_"
    /** Per-app "never save if it contains" words (Premium). */
    const val BLOCK_PREFIX = "block_"

    // Premium features
    val KEYWORD_ALERTS = stringSetPreferencesKey("keyword_alerts")
    val DELETED_ALERTS = booleanPreferencesKey("deleted_message_alerts")
    val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")

    // In-app review timing (see ReviewPolicy)
    val FIRST_OPEN_AT = longPreferencesKey("first_open_at")
    val VALUE_MOMENTS = intPreferencesKey("value_moments")
    val REVIEW_LAST_ASKED_AT = longPreferencesKey("review_last_asked_at")
    val REVIEW_ASK_COUNT = intPreferencesKey("review_ask_count")

    // End of the ad-free period earned by watching a rewarded ad (epoch millis)
    val AD_FREE_UNTIL = longPreferencesKey("ad_free_until")

}
