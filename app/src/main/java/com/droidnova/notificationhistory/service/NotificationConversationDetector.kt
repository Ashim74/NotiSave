package com.droidnova.notificationhistory.service

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.util.Log

internal object NotificationConversationDetector {

    private const val TAG = "ConversationDetector"
    private const val MESSAGING_STYLE_TEMPLATE = "android.app.Notification\$MessagingStyle"

    fun detect(
        packageName: String,
        notification: Notification,
        appLabel: String?
    ): ConversationIdentity? = try {
        resolveConversationIdentity(readSignals(packageName, notification, appLabel))
    } catch (e: RuntimeException) {
        // Malformed extras must never block saving the notification itself.
        Log.w(TAG, "Conversation detection failed for $packageName", e)
        null
    }

    private fun readSignals(
        packageName: String,
        notification: Notification,
        appLabel: String?
    ): ConversationSignals {
        val extras = runCatching { notification.extras }.getOrNull()
        val messages = readMessagingStyleMessages(extras)
        val template = runCatching { extras?.getString(Notification.EXTRA_TEMPLATE) }.getOrNull()

        return ConversationSignals(
            packageName = packageName,
            appLabel = appLabel,
            shortcutId = runCatching { notification.shortcutId }.getOrNull(),
            conversationTitle = extras.safeText(Notification.EXTRA_CONVERSATION_TITLE),
            title = extras.safeText(Notification.EXTRA_TITLE),
            isMessageCategory = notification.category == Notification.CATEGORY_MESSAGE,
            isMessagingStyle = messages.isNotEmpty() || template == MESSAGING_STYLE_TEMPLATE,
            isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            isGroupConversation = readIsGroupConversation(extras),
            messageSenders = messages.map { it.sender },
            selfName = readSelfName(extras)
        )
    }

    @Suppress("DEPRECATION")
    private fun readSelfName(extras: Bundle?): CharSequence? {
        if (extras == null) return null
        val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                extras.getParcelable<android.app.Person>(Notification.EXTRA_MESSAGING_PERSON)?.name
            }.getOrNull()
        } else {
            null
        }
        return person ?: extras.safeText(Notification.EXTRA_SELF_DISPLAY_NAME)
    }

    private fun readIsGroupConversation(extras: Bundle?): Boolean? {
        if (extras == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            if (extras.containsKey(Notification.EXTRA_IS_GROUP_CONVERSATION)) {
                extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun Bundle?.safeText(key: String): CharSequence? {
        if (this == null) return null
        return runCatching { getCharSequence(key) }.getOrNull()
    }
}
