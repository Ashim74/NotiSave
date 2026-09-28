package com.droidnova.notificationhistory.service

/**
 * Thread-safe, bounded short-term memory for duplicate notification callbacks.
 */
class DedupeCache(
    private val ttlMs: Long = 2000L,
    private val maxSize: Int = 512
) {
    init {
        require(ttlMs > 0) { "ttlMs must be positive" }
        require(maxSize > 0) { "maxSize must be positive" }
    }

    private data class CacheKey(val notificationKey: String, val contentDigest: String)

    private val map = LinkedHashMap<CacheKey, Long>(maxSize, 0.75f, true)

    /**
     * Returns true and reserves this key/content pair, or false for a duplicate in the window.
     */
    fun allowAndReserve(
        notificationKey: String,
        normalizedContent: String,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val cacheKey = CacheKey(notificationKey, sha256(normalizedContent))
        synchronized(map) {
            val previous = map[cacheKey]
            val elapsed = previous?.let { now - it }
            if (elapsed != null && elapsed >= 0 && elapsed < ttlMs) {
                // Use a sliding window so a burst of repeated callbacks stays suppressed.
                map[cacheKey] = now
                return false
            }

            map[cacheKey] = now
            if (map.size > maxSize) {
                val iterator = map.entries.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            return true
        }
    }

}
