package com.droidnova.notificationhistory.data.model

/** App icons are resolved at render time via AppInfoCache, never held per row. */
data class NotificationModel(
    val id: Long,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val receivedAt: String,
    val receivedAtEpoch: Long,
    val isTrashed: Boolean = false,
    val trashedAtEpoch: Long? = null,
    /** When the sender deleted it; null while the message stands. */
    val deletedAtEpoch: Long? = null
)
