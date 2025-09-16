package com.droidnova.notificationhistory.data_shared

data class SettingState (
    var launchCount: Int = 0,
    var showRateUsCard: Boolean = true,
    val snoozeUntilLaunch: Int = 2 ,
    var userToggleTracking: Boolean = false,//switch on or off track
    var selectedAppsCount: Int = 0,
    )
