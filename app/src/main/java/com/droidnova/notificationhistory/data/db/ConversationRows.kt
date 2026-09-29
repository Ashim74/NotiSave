package com.droidnova.notificationhistory.data.db

/** One conversation represented by its latest matching active notification. */
data class ConversationSummaryRow(
    val latestId: Long,
    val packageName: String,
    val conversationKey: String,
    val conversationName: String?,
    val latestTitle: String,
    val latestMessage: String,
    val latestReceivedAt: Long,
    val messageCount: Int
)

/** Cheap fingerprint of the active conversation rows, used to refresh loaded pages in place. */
data class ConversationChangeSignature(
    val activeCount: Int,
    val maxId: Long,
    val idSum: Long
)
