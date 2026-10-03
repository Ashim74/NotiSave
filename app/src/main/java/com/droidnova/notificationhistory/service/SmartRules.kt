package com.droidnova.notificationhistory.service

import java.util.Locale

/**
 * What chat apps put in place of a message its sender deleted (WhatsApp and WhatsApp Business,
 * in widely used languages; add others here). Compared after [comparisonKey], without the
 * leading "🚫" icon and the trailing period.
 */
private val deletedPlaceholders = setOf(
    "this message was deleted",
    "this message has been deleted",
    "se eliminó este mensaje",
    "este mensaje fue eliminado",
    "esta mensagem foi apagada",
    "mensagem apagada",
    "ce message a été supprimé",
    "diese nachricht wurde gelöscht",
    "questo messaggio è stato eliminato",
    "pesan ini telah dihapus",
    "pesan ini dihapus",
    "bu mesaj silindi",
    "это сообщение удалено",
    "данное сообщение удалено",
    "تم حذف هذه الرسالة",
)

private val placeholderDecoration = Regex("^[\\p{So}\\p{Mn}\\s]+|[.。!]+$")

/** True when [text] is a chat app's "This message was deleted" stand-in, not a real message. */
internal fun isDeletedPlaceholder(text: String): Boolean {
    if (text.length > 80) return false
    val key = comparisonKey(text).replace(placeholderDecoration, "").trim()
    return key in deletedPlaceholders
}

/** The first of [keywords] found in [text] (case- and spacing-insensitive), or null. */
internal fun matchedKeyword(text: String, keywords: Set<String>): String? {
    if (keywords.isEmpty() || text.isBlank()) return null
    val haystack = comparisonKey(text)
    return keywords.firstOrNull { word ->
        val needle = comparisonKey(word)
        needle.isNotEmpty() && haystack.contains(needle)
    }
}

/** True when the title or message contains any of [blockWords]; spaces and case are ignored. */
internal fun containsBlockedWord(title: String, message: String, blockWords: Set<String>): Boolean {
    if (blockWords.isEmpty()) return false
    val haystack = "$title\n$message".lowercase(Locale.ROOT).replace("\\s".toRegex(), "")
    return blockWords.any { word ->
        val needle = word.lowercase(Locale.ROOT).replace("\\s".toRegex(), "")
        needle.isNotEmpty() && haystack.contains(needle)
    }
}
