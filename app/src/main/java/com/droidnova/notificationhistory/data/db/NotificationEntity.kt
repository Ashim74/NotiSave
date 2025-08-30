package com.droidnova.notificationhistory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    val message: String,
    val receivedAt: Long
)