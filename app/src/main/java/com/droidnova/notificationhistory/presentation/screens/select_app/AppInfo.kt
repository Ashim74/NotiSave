package com.droidnova.notificationhistory.presentation.screens.select_app

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isAllowed: Boolean = false
)
