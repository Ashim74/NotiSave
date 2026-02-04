package com.droidnova.notificationhistory.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object DataStoreKeys {
    const val PREF_NAME = "notification_prefs"
    val KEY_USER_WANTS_TRACKING = booleanPreferencesKey("user_wants_tracking")
    const val ALLOWED_APPS_KEY = "allowed_apps"
    val LAUNCH_COUNT = intPreferencesKey("launch_count")
    val SHOW_RATE_US_CARD = booleanPreferencesKey("show_us_rate_card")
    val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
    val IS_PREMIUM = booleanPreferencesKey("is_premium")
    const val FILTERS_PREFIX = "filters_"

}
