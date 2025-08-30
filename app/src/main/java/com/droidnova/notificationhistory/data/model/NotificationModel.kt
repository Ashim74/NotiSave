package com.droidnova.notificationhistory.data.model

import android.graphics.drawable.Drawable

data class NotificationModel(
    val packageName: String,
    val appIcon: Drawable?,
    val appName: String,
    val title: String,
    val text: String,
    val receivedAt: String
)
