package com.droidnova.notificationhistory.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

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
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    const val FILTERS_PREFIX = "filters_"

    // In-app review timing (see ReviewPolicy)
    val FIRST_OPEN_AT = longPreferencesKey("first_open_at")
    val VALUE_MOMENTS = intPreferencesKey("value_moments")
    val REVIEW_LAST_ASKED_AT = longPreferencesKey("review_last_asked_at")
    val REVIEW_ASK_COUNT = intPreferencesKey("review_ask_count")

}
