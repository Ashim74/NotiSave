package com.droidnova.notificationhistory.service

import java.text.Normalizer
import java.util.Locale

/**
 * Stable identity of a messaging conversation. [key] always starts with the package name so
 * conversations from different apps can never be merged.
 */
internal data class ConversationIdentity(
    val key: String,
    val displayName: String
)

/**
 * Raw, already-extracted notification metadata used to decide whether a notification belongs to a
 * real messaging conversation. Kept free of Android types so the rules are JVM-testable.
 */
internal data class ConversationSignals(
    val packageName: String,
    val appLabel: String? = null,
    val shortcutId: String? = null,
    val conversationTitle: CharSequence? = null,
    val title: CharSequence? = null,
    val isMessageCategory: Boolean = false,
    val isMessagingStyle: Boolean = false,
    val isGroupSummary: Boolean = false,
    /** `true`/`false` when the app reported it (API 28+), `null` when unknown. */
    val isGroupConversation: Boolean? = null,
    /** Sender names of MessagingStyle messages; `null` entries are messages sent by the user. */
    val messageSenders: List<CharSequence?> = emptyList(),
    /** Display name of the device user in MessagingStyle, used to ignore their own messages. */
    val selfName: CharSequence? = null
)

/**
 * Conservative conversation detection. Priority:
 * 1. non-empty shortcut id,
 * 2. reliable conversation/group title,
 * 3. reliable one-to-one sender/contact name,
 * otherwise `null` (the notification stays in the regular history only).
 */
internal fun resolveConversationIdentity(signals: ConversationSignals): ConversationIdentity? {
    val packageName = signals.packageName.trim()
    if (packageName.isEmpty()) return null
    if (signals.isGroupSummary) return null
    if (!signals.isMessagingStyle && !signals.isMessageCategory) return null

    val conversationTitle = sanitizeNotificationText(signals.conversationTitle)
        .singleLineOrEmpty()
        .takeUnless { it.isSummaryLike() || it.isSameName(signals.appLabel) }
        .orEmpty()
    val oneToOneName = oneToOneContactName(signals)

    val shortcutId = runCatching { signals.shortcutId?.trim() }.getOrNull().orEmpty()
    if (shortcutId.isNotEmpty()) {
        val displayName = conversationTitle.ifEmpty { oneToOneName }
        // A shortcut without any readable name would produce an unlabeled conversation.
        if (displayName.isNotEmpty()) {
            return ConversationIdentity(
                key = conversationKey(packageName, "s", shortcutId),
                displayName = displayName
            )
        }
    }

    if (conversationTitle.isNotEmpty()) {
        return ConversationIdentity(
            key = conversationKey(packageName, "t", normalizeConversationName(conversationTitle)),
            displayName = conversationTitle
        )
    }

    if (oneToOneName.isNotEmpty()) {
        return ConversationIdentity(
            key = conversationKey(packageName, "c", normalizeConversationName(oneToOneName)),
            displayName = oneToOneName
        )
    }

    return null
}

private fun oneToOneContactName(signals: ConversationSignals): String {
    // A group without a reliable title must never be attributed to one of its members.
    if (signals.isGroupConversation == true) return ""

    val selfName = sanitizeNotificationText(signals.selfName).singleLineOrEmpty()
    val senders = signals.messageSenders
        .mapNotNull { sender ->
            sanitizeNotificationText(sender).singleLineOrEmpty().takeIf { it.isNotEmpty() }
        }
        .filterNot { it.isSameName(selfName) }
    val distinctSenders = senders.distinctBy { normalizeConversationName(it) }
    val title = sanitizeNotificationText(signals.title)
        .singleLineOrEmpty()
        .replace(messageCountSuffix, "")
        .trim()

    val candidate = when {
        // More than one other participant means this is a group, not a personal chat.
        distinctSenders.size > 1 -> return ""
        distinctSenders.size == 1 -> distinctSenders.single()
        // Only messages from the user (or no MessagingStyle): fall back to the title.
        else -> title
    }

    if (candidate.isEmpty() || candidate.isSummaryLike() || candidate.isSameName(signals.appLabel)) {
        return ""
    }
    return candidate
}

private fun conversationKey(packageName: String, type: String, value: String): String =
    "$packageName|$type:$value"

internal fun normalizeConversationName(value: String): String {
    val normalized = runCatching { Normalizer.normalize(value, Normalizer.Form.NFKC) }
        .getOrDefault(value)
    return normalized.lowercase(Locale.ROOT).replace(whitespace, " ").trim()
}

private fun String.singleLineOrEmpty(): String = if (contains('\n')) "" else this

private fun String.isSameName(other: String?): Boolean =
    !other.isNullOrBlank() && normalizeConversationName(this) == normalizeConversationName(other)

private fun String.isSummaryLike(): Boolean = summaryPatterns.any { it.containsMatchIn(this) }

private val whitespace = Regex("\\s+")

/** WhatsApp-style counters appended to contact names, e.g. "Alice (3 messages)". */
private val messageCountSuffix = Regex("\\s*\\(\\d+\\s+(new\\s+)?messages?\\)$", RegexOption.IGNORE_CASE)

/** Aggregated summaries such as "3 messages from 2 chats" or "5 new messages". */
private val summaryPatterns = listOf(
    Regex("^\\d+\\s+(new\\s+|unread\\s+)?(messages?|chats?|conversations?)\\b", RegexOption.IGNORE_CASE),
    Regex("\\b\\d+\\s+(new\\s+|unread\\s+)?messages?\\s+from\\s+\\d+\\s+(chats?|conversations?)\\b", RegexOption.IGNORE_CASE)
)
