package com.droidnova.notificationhistory.data_shared

data class SettingState (
    var launchCount: Int = 0,
    var showRateUsCard: Boolean = true,
    var userToggleTracking: Boolean = false,//switch on or off track
    var selectedAppsCount: Int = 0,
    var historyRetentionDays: Int = DEFAULT_HISTORY_RETENTION_DAYS,
    ) {
    companion object {
        const val DEFAULT_HISTORY_RETENTION_DAYS = 7
    }
}
