package com.droidnova.notificationhistory.utils

import com.droidnova.notificationhistory.data.model.NotificationModel

fun NotificationModel.toReadableShareText(): String {
    val appLabel = appName.ifBlank { packageName }
    val titleLabel = title.ifBlank { "No title" }
    val messageLabel = text.ifBlank { "No message" }
    val timeLabel = receivedAt.ifBlank { "Unknown time" }

    return buildString {
        appendLine("App: $appLabel")
        appendLine("Title: $titleLabel")
        appendLine("Message: $messageLabel")
        append("Time: $timeLabel")
    }
}
