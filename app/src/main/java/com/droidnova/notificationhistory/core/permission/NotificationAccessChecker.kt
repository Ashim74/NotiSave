package com.droidnova.notificationhistory.core.permission

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

private const val KEY_ENABLED_LISTENERS = "enabled_notification_listeners"

object NotificationAccessChecker {

    fun hasNotificationAccessPermission(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            KEY_ENABLED_LISTENERS
        ) ?: return false
        return enabledListeners
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it.packageName == context.packageName }
    }
}
