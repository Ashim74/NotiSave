package com.droidnova.notificationhistory.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "apps",
    indices = [
        Index(value = ["receivedAt"], name = "index_apps_receivedAt"),
        Index(
            value = ["packageName", "receivedAt"],
            name = "index_apps_packageName_receivedAt"
        ),
        Index(
            value = ["notificationKey", "contentFingerprint", "receivedAt"],
            name = "index_apps_notificationKey_contentFingerprint_receivedAt"
        ),
        Index(
            value = ["isTrashed", "receivedAt", "id"],
            name = "index_apps_isTrashed_receivedAt_id"
        ),
        Index(
            value = ["isTrashed", "trashedAt", "id"],
            name = "index_apps_isTrashed_trashedAt_id"
        ),
        Index(
            value = ["conversationKey", "isTrashed", "receivedAt", "id"],
            name = "index_apps_conversationKey_isTrashed_receivedAt_id"
        )
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    val message: String,
    val receivedAt: Long,
    val notificationKey: String? = null,
    val contentFingerprint: String? = null,
    val conversationTitle: String? = null,
    @ColumnInfo(defaultValue = "0") val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    /** Package-scoped conversation identity; null for non-messaging or unclassified rows. */
    val conversationKey: String? = null,
    /** Display name of the conversation/contact/group at capture time. */
    val conversationName: String? = null,
    /** When the sender deleted this message ("This message was deleted"); null while it stands. */
    val deletedAt: Long? = null
)
