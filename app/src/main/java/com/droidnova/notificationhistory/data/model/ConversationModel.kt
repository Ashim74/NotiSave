package com.droidnova.notificationhistory.data.model

import android.graphics.drawable.Drawable

data class ConversationModel(
    val conversationKey: String,
    val packageName: String,
    val appIcon: Drawable?,
    val appName: String,
    val title: String,
    val latestMessage: String,
    val latestNotificationId: Long,
    val latestReceivedAt: String,
    val latestReceivedAtEpoch: Long,
    val messageCount: Int
)
