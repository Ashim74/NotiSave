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
import com.droidnova.notificationhistory.utils.CrashReporter
import java.util.concurrent.atomic.AtomicInteger

/**
 * Premium alerts the listener raises about saved notifications: a recovered deleted message, or
 * a keyword match. Both share one channel the user can tune in system settings.
 */
object SmartAlerts {

    private const val CHANNEL_ID = "smart_alerts"
    private const val BASE_ID = 5200
    private val nextId = AtomicInteger(0)

    fun showDeletedMessage(context: Context, appName: String, sender: String, original: String) {
        post(
            context,
            title = context.getString(R.string.alert_deleted_title, sender.ifBlank { appName }),
            text = original,
            subText = appName,
            open = LaunchAction.OPEN_DELETED
        )
    }

    fun showKeyword(context: Context, keyword: String, appName: String, sender: String, message: String) {
        post(
            context,
            title = context.getString(R.string.alert_keyword_title, keyword, sender.ifBlank { appName }),
            text = message,
            subText = appName,
            open = LaunchAction.OPEN_HISTORY
        )
    }

    private fun post(context: Context, title: String, text: String, subText: String, open: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(context, manager)
        // A rolling id keeps several alerts visible at once without growing without bound.
        val id = BASE_ID + nextId.getAndIncrement() % MAX_VISIBLE
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LaunchAction.EXTRA_OPEN, open)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nh_history)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        // Without POST_NOTIFICATIONS on API 33+ the system drops this silently.
        runCatching { manager.notify(id, notification) }.onFailure(CrashReporter::record)
    }

    private fun ensureChannel(context: Context, manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.smart_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.smart_alert_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private const val MAX_VISIBLE = 20
}
