package com.droidnova.notificationhistory.core.permission

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log

//private const val KEY_ENABLED_LISTENERS = "enabled_notification_listeners"

object NotificationAccessChecker {

    fun hasNotificationAccessPermission(context: Context): Boolean {
        Log.e("mytag", "hasNotificationAccessPermission")
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(context.packageName)
    }
}
