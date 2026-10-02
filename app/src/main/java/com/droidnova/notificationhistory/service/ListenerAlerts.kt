package com.droidnova.notificationhistory.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.droidnova.notificationhistory.MainActivity
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.navigation.LaunchAction
import com.droidnova.notificationhistory.utils.Analytics
import com.droidnova.notificationhistory.utils.CrashReporter

/**
 * The one notification this app posts itself: "recording stopped, tap to reconnect".
 * Shown only after a disconnect that a plain rebind did not fix; cancelled on reconnect.
 */
object ListenerAlerts {

    private const val CHANNEL_ID = "listener_status"
    private const val NOTIFICATION_ID = 4101

    fun showDisconnected(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(context, manager)

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LaunchAction.EXTRA_RECONNECT, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nh_history)
            .setContentTitle(context.getString(R.string.alert_listener_stopped_title))
            .setContentText(context.getString(R.string.alert_listener_stopped_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.alert_listener_stopped_text))
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        // Without POST_NOTIFICATIONS on API 33+ the system silently drops this; that's fine.
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
            .onSuccess { Analytics.log(context, Analytics.LISTENER_ALERT_SHOWN) }
            .onFailure(CrashReporter::record)
    }

    fun cancel(context: Context) {
        runCatching {
            context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        }
    }

    private fun ensureChannel(context: Context, manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.alert_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
