package com.droidnova.notificationhistory.utils

import android.content.Context
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.model.NotificationModel

fun NotificationModel.toReadableShareText(context: Context): String {
    val appLabel = appName.ifBlank { packageName }
    val titleLabel = title.ifBlank { context.getString(R.string.share_text_no_title) }
    val messageLabel = text.ifBlank { context.getString(R.string.share_text_no_message) }
    val timeLabel = receivedAt.ifBlank { context.getString(R.string.share_text_unknown_time) }

    return buildString {
        appendLine(context.getString(R.string.share_text_app, appLabel))
        appendLine(context.getString(R.string.share_text_title, titleLabel))
        appendLine(context.getString(R.string.share_text_message, messageLabel))
        append(context.getString(R.string.share_text_time, timeLabel))
    }
}
