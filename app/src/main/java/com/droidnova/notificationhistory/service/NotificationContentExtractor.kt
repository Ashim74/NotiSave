package com.droidnova.notificationhistory.service

import android.app.Notification
import android.os.Bundle

internal object NotificationContentExtractor {

    fun extract(notification: Notification): ExtractedNotificationContent {
        val extras = runCatching { notification.extras }.getOrNull()
        val conversationTitle =
            sanitizeNotificationText(extras.safeText(Notification.EXTRA_CONVERSATION_TITLE))
        val title = chooseMeaningfulTitle(
            conversationTitle = conversationTitle,
            title = extras.safeText(Notification.EXTRA_TITLE),
            bigTitle = extras.safeText(Notification.EXTRA_TITLE_BIG),
            subText = extras.safeText(Notification.EXTRA_SUB_TEXT)
        )

        val message = combineMeaningfulText(
            sourcesInPriorityOrder = listOf(
                extractMessagingStyleMessages(notification),
                listOf(extras.safeText(Notification.EXTRA_BIG_TEXT)),
                extras.safeTextArray(Notification.EXTRA_TEXT_LINES),
                listOf(extras.safeText(Notification.EXTRA_TEXT)),
                listOf(extras.safeText(Notification.EXTRA_SUMMARY_TEXT))
            ),
            title = title
        )

        return ExtractedNotificationContent(
            title = title,
            message = message,
            conversationTitle = conversationTitle.ifEmpty { null }
        )
    }

    private fun extractMessagingStyleMessages(notification: Notification): List<CharSequence?> =
        runCatching {
            val messageBundles = notification.extras
                ?.getParcelableArray(Notification.EXTRA_MESSAGES)
                ?: return@runCatching emptyList()
            Notification.MessagingStyle.Message.getMessagesFromBundleArray(messageBundles)
                .map { message -> message.text }
        }.getOrDefault(emptyList())

    private fun Bundle?.safeText(key: String): CharSequence? {
        if (this == null) return null
        return runCatching { getCharSequence(key) }.getOrNull()
    }

    private fun Bundle?.safeTextArray(key: String): List<CharSequence?> {
        if (this == null) return emptyList()
        return runCatching { getCharSequenceArray(key)?.toList().orEmpty() }
            .getOrDefault(emptyList())
    }
}
