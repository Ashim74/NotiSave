package com.droidnova.notificationhistory.data.mapper

import android.content.Context
import android.content.pm.PackageManager
import com.droidnova.notificationhistory.core.apps.AppInfoCache
import com.droidnova.notificationhistory.data.db.ConversationSummaryRow
import com.droidnova.notificationhistory.data.db.NotificationEntity
import com.droidnova.notificationhistory.data.model.ConversationModel
import com.droidnova.notificationhistory.data.model.NotificationModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Resolves the app label through [AppInfoCache]; call off the main thread for uncached packages. */
fun convertEntityToModel(context: Context, notificationEntity: NotificationEntity): NotificationModel =
    convertEntityToModel(
        notificationEntity,
        AppInfoCache.label(context.packageManager, notificationEntity.packageName)
    )

fun convertEntityToModel(
    notificationEntity: NotificationEntity,
    appName: String
): NotificationModel = NotificationModel(
    id = notificationEntity.id,
    packageName = notificationEntity.packageName,
    appName = appName,
    title = notificationEntity.title,
    text = notificationEntity.message,
    receivedAt = notificationEntity.receivedAt.toReadableTime(),
    receivedAtEpoch = notificationEntity.receivedAt,
    isTrashed = notificationEntity.isTrashed,
    trashedAtEpoch = notificationEntity.trashedAt
)

fun convertConversationRowToModel(
    row: ConversationSummaryRow,
    appName: String
): ConversationModel = ConversationModel(
    conversationKey = row.conversationKey,
    packageName = row.packageName,
    appName = appName,
    title = row.conversationName?.takeIf { it.isNotBlank() }
        ?: row.latestTitle.ifBlank { appName },
    latestMessage = row.latestMessage.ifBlank { row.latestTitle },
    latestNotificationId = row.latestId,
    latestReceivedAt = row.latestReceivedAt.toReadableTime(),
    latestReceivedAtEpoch = row.latestReceivedAt,
    messageCount = row.messageCount
)

fun fetchAppName(pm: PackageManager, packageName: String): String =
    AppInfoCache.label(pm, packageName)

fun Long.toReadableTime(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(ReadableTimeFormatter.current())

/**
 * One formatter per locale instead of one per row. Rebuilt only when the default locale changes.
 */
internal object ReadableTimeFormatter {
    private const val PATTERN = "dd MMM yyyy, hh:mm a"

    @Volatile
    private var cached: Pair<Locale, DateTimeFormatter>? = null

    fun current(): DateTimeFormatter {
        val locale = Locale.getDefault()
        cached?.takeIf { it.first == locale }?.let { return it.second }
        return DateTimeFormatter.ofPattern(PATTERN, locale).also { cached = locale to it }
    }
}
