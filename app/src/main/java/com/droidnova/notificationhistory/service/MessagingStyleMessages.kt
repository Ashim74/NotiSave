package com.droidnova.notificationhistory.service

import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle

/** One chat message from a MessagingStyle notification. */
internal data class StyleMessage(
    val text: CharSequence?,
    /** Null for messages the user sent (apps leave the sender empty for "me"). */
    val sender: CharSequence?,
    val timestamp: Long
)

/**
 * Reads the individual messages of a MessagingStyle notification on every Android version.
 *
 * The platform's `Notification.MessagingStyle.Message.getMessagesFromBundleArray` is only
 * public from Android 11 (API 30), and AndroidX's equivalent is package-private, so on
 * Android 8–10 chats lost their per-message history. This reads the same per-message bundle
 * format both of them parse: `text` + `time` (required), and the sender as a `Person`
 * (`sender_person`, Android 9+), an AndroidX person bundle (`person`), or plain `sender` text.
 */
internal fun readMessagingStyleMessages(extras: Bundle?): List<StyleMessage> = runCatching {
    val bundles = extras?.getParcelableArray(Notification.EXTRA_MESSAGES)
        ?: return@runCatching emptyList()
    bundles.mapNotNull { (it as? Bundle)?.toStyleMessage() }
}.getOrDefault(emptyList())

private fun Bundle.toStyleMessage(): StyleMessage? = runCatching {
    // The platform parser drops entries missing either key; do the same.
    if (!containsKey(KEY_TEXT) || !containsKey(KEY_TIMESTAMP)) return@runCatching null
    StyleMessage(
        text = getCharSequence(KEY_TEXT),
        sender = platformPersonName() ?: compatPersonName() ?: getCharSequence(KEY_SENDER),
        timestamp = getLong(KEY_TIMESTAMP)
    )
}.getOrNull()

private fun Bundle.platformPersonName(): CharSequence? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(KEY_SENDER_PERSON, Person::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(KEY_SENDER_PERSON) as? Person
    }
    return person?.name
}

private fun Bundle.compatPersonName(): CharSequence? =
    getBundle(KEY_COMPAT_PERSON)?.getCharSequence(KEY_COMPAT_PERSON_NAME)

private const val KEY_TEXT = "text"
private const val KEY_TIMESTAMP = "time"
private const val KEY_SENDER = "sender"
private const val KEY_SENDER_PERSON = "sender_person"
private const val KEY_COMPAT_PERSON = "person"
private const val KEY_COMPAT_PERSON_NAME = "name"
