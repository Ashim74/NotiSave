package com.droidnova.notificationhistory.service

/**
 * Remembers the content last seen for each notification key while the system still shows it.
 *
 * Apps re-post the same notification (same key, same text) when they refresh, re-sort or
 * re-alert it; those updates must not become new history rows. A key that was removed and then
 * posted again is a genuinely new notification, even if the text is identical.
 */
class ActiveContentTracker(private val maxSize: Int = 1024) {

    enum class Verdict {
        /** Same key is still shown with the same content: a re-post, not a new notification. */
        Unchanged,

        /** New content for this key, or the key was dismissed before being posted again. */
        Changed,

        /** Nothing known about this key (e.g. the listener restarted); ask the database. */
        Unknown
    }

    private val entries = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) =
            size > maxSize
    }

    /** Records [fingerprint] as the current content for [key] and classifies the post. */
    fun onPosted(key: String, fingerprint: String): Verdict = synchronized(entries) {
        when (entries.put(key, fingerprint)) {
            null -> Verdict.Unknown
            REMOVED -> Verdict.Changed
            fingerprint -> Verdict.Unchanged
            else -> Verdict.Changed
        }
    }

    fun onRemoved(key: String) {
        synchronized(entries) { entries[key] = REMOVED }
    }

    private companion object {
        // Fingerprints are hex SHA-256, so this can never collide with real content.
        const val REMOVED = "\u0000removed"
    }
}
