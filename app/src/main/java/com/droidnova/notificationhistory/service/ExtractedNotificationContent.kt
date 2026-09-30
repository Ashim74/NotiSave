package com.droidnova.notificationhistory.service

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

internal data class ExtractedNotificationContent(
    val title: String,
    val message: String,
    val conversationTitle: String? = null
) {
    val hasMeaningfulContent: Boolean
        get() = title.isNotBlank() || message.isNotBlank()

    fun normalizedForDedupe(): String =
        "${comparisonKey(title)}\u0000${comparisonKey(message)}"

    fun contentFingerprint(): String = sha256(normalizedForDedupe())
}

/** One chat message inside a MessagingStyle notification. */
internal data class ExtractedMessage(
    val sender: String,
    val text: String,
    val timestamp: Long
) {
    /** Identity of the message itself, independent of how the app re-renders the thread. */
    fun fingerprint(): String = sha256("${comparisonKey(sender)}\u0000${comparisonKey(text)}")

    /** Group chats name the sender; 1:1 chats already show the contact as the title. */
    fun displayText(title: String): String =
        if (sender.isEmpty() || comparisonKey(sender) == comparisonKey(title)) text
        else "$sender: $text"
}

internal fun sha256(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    val result = CharArray(bytes.size * 2)
    bytes.forEachIndexed { index, byte ->
        val unsigned = byte.toInt() and 0xff
        result[index * 2] = HEX_CHARS[unsigned ushr 4]
        result[index * 2 + 1] = HEX_CHARS[unsigned and 0x0f]
    }
    return String(result)
}

private const val HEX_CHARS = "0123456789abcdef"

private val invisibleCharacters = Regex("[\\u0000\\u200B-\\u200D\\u2060\\uFEFF]")
private val horizontalWhitespace = Regex("[\\p{Zs}\\t\\u000B\\f]+")
private val allWhitespace = Regex("\\s+")
private val meaninglessValues = setOf("null", "undefined", "none", "n/a", "...", "…", "-", "—")

internal fun sanitizeNotificationText(value: CharSequence?): String {
    val raw = runCatching { value?.toString() }.getOrNull().orEmpty()
    if (raw.isBlank()) return ""

    val cleaned = raw
        .replace(invisibleCharacters, "")
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()
        .map { line -> line.trim().replace(horizontalWhitespace, " ") }
        .filter { line -> line.isNotEmpty() && line.lowercase(Locale.ROOT) !in meaninglessValues }
        .joinToString("\n")
        .trim()

    return cleaned.takeUnless {
        it.isEmpty() || it.lowercase(Locale.ROOT) in meaninglessValues
    }.orEmpty()
}

internal fun chooseMeaningfulTitle(
    conversationTitle: CharSequence?,
    title: CharSequence?,
    bigTitle: CharSequence?,
    subText: CharSequence?
): String {
    val conversation = sanitizeNotificationText(conversationTitle)
    if (conversation.isNotEmpty()) return conversation

    val normal = sanitizeNotificationText(title)
    val expanded = sanitizeNotificationText(bigTitle)
    val primaryTitle = when {
        normal.isEmpty() -> expanded
        expanded.isEmpty() -> normal
        comparisonKey(expanded).contains(comparisonKey(normal)) -> expanded
        comparisonKey(normal).contains(comparisonKey(expanded)) -> normal
        expanded.length >= normal.length -> expanded
        else -> normal
    }

    return primaryTitle.ifEmpty { sanitizeNotificationText(subText) }
}

internal fun combineMeaningfulText(
    sourcesInPriorityOrder: List<List<CharSequence?>>,
    title: String
): String {
    val result = mutableListOf<String>()
    val titleKey = comparisonKey(title)

    sourcesInPriorityOrder.forEach { source ->
        source.forEach { value ->
            sanitizeNotificationText(value)
                .lineSequence()
                .filter { it.isNotBlank() }
                .forEach { line -> addIfUseful(result, line, titleKey) }
        }
    }

    return result.joinToString("\n")
}

private fun addIfUseful(result: MutableList<String>, candidate: String, titleKey: String) {
    val candidateKey = comparisonKey(candidate)
    if (candidateKey.isEmpty() || candidateKey == titleKey) return

    var replaceIndex: Int? = null
    result.forEachIndexed { index, existing ->
        val existingKey = comparisonKey(existing)
        if (candidateKey == existingKey || isPreviewOf(candidateKey, existingKey)) return
        if (isPreviewOf(existingKey, candidateKey)) replaceIndex = index
    }

    if (replaceIndex != null) {
        result[replaceIndex] = candidate
    } else {
        result += candidate
    }
}

private fun isPreviewOf(possiblePreview: String, completeText: String): Boolean {
    val preview = possiblePreview.trimEnd('.', '…').trim()
    if (preview.length < 12 || preview.length >= completeText.length) return false
    return completeText.contains(preview)
}

internal fun comparisonKey(value: String): String {
    val normalized = runCatching {
        Normalizer.normalize(value, Normalizer.Form.NFKC)
    }.getOrDefault(value)
    return normalized
        .lowercase(Locale.ROOT)
        .replace(allWhitespace, " ")
        .trim()
}
