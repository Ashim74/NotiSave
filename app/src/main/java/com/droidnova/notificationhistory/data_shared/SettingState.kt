package com.droidnova.notificationhistory.data_shared

data class SettingState (
    var launchCount: Int = 0,
    var showRateUsCard: Boolean = true,
    // Must match the DataStore default in UserPreferences.settingFlow (true), otherwise
    // the UI briefly renders "tracking off" on every cold start.
    var userToggleTracking: Boolean = true,
    var selectedAppsCount: Int = 0,
    var historyRetentionDays: Int = DEFAULT_HISTORY_RETENTION_DAYS,
    ) {
    companion object {
        const val DEFAULT_HISTORY_RETENTION_DAYS = 7
    }
}
