package com.droidnova.notificationhistory.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val conversationTitle: String? = null
)
