package com.droidnova.notificationhistory.data.mapper

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.droidnova.notificationhistory.data.db.NotificationEntity
import com.droidnova.notificationhistory.data.model.NotificationModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun convertEntityToModel(context: Context, notificationEntity: NotificationEntity):NotificationModel{
    val pm = context.packageManager
    val appIcon = fetchAppIcon(pm,notificationEntity.packageName)
    val appName = fetchAppName(pm,notificationEntity.packageName)
    val notificationModel =NotificationModel(
        packageName = notificationEntity.packageName,
        appIcon = appIcon,
        appName = appName,
        title = notificationEntity.title,
        text = notificationEntity.message,
        receivedAt = notificationEntity.receivedAt.toReadableTime()
    )
    return notificationModel
}
fun fetchAppName(pm: PackageManager, packageName: String): String =
    runCatching {
        val ai = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(ai).toString()
    }.getOrElse { packageName }

fun fetchAppIcon(pm: PackageManager, packageName: String): Drawable? =
    runCatching { pm.getApplicationIcon(packageName) }.getOrNull()


fun Long.toReadableTime(): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
