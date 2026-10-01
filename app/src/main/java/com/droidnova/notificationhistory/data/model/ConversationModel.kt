package com.droidnova.notificationhistory.data.model

data class ConversationModel(
    val conversationKey: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val latestMessage: String,
    val latestNotificationId: Long,
    val latestReceivedAt: String,
    val latestReceivedAtEpoch: Long,
    val messageCount: Int
)
