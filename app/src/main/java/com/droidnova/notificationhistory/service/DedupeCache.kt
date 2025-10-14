package com.droidnova.notificationhistory.service

/**
 * Short-term memory for duplicates.
 * Same (key + contentHash) ko chhote time-window me sirf ek baar allow karta hai.
 */
class DedupeCache(
    private val ttlMs: Long = 2000L,   // "thodi der" = 2s (tune as needed)
    private val maxSize: Int = 512     // memory bound
) {
    private data class Entry(val hash: Int, var at: Long)

    // LRU-ish behavior (accessOrder = true) so old entries nikal sake
    private val map = LinkedHashMap<String, Entry>(maxSize, 0.75f, true)

    /**
     * true => allow (reserve now), false => duplicate (skip)
     * NOTE: Atomic (synchronized) so no races.
     */
    fun allowAndReserve(key: String, contentHash: Int, now: Long = System.currentTimeMillis()): Boolean {
        synchronized(map) {
            val e = map[key]
            if (e != null && now - e.at < ttlMs && e.hash == contentHash) {
                // Same key + same content in window => duplicate
                return false
            }
            // Reserve / update
            map[key] = Entry(contentHash, now)

            // Bound size
            if (map.size > maxSize) {
                val it = map.entries.iterator()
                if (it.hasNext()) { it.next(); it.remove() }
            }
            return true
        }
    }
}
