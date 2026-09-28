package com.droidnova.notificationhistory.service

import java.util.concurrent.Executors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DedupeCacheTest {

    @Test
    fun `same key and same content inside window is rejected`() {
        val cache = DedupeCache(ttlMs = 2_000L)

        assertTrue(cache.allowAndReserve("key", "title\u0000message", now = 1_000L))
        assertFalse(cache.allowAndReserve("key", "title\u0000message", now = 1_500L))
    }

    @Test
    fun `same key and changed content is allowed`() {
        val cache = DedupeCache(ttlMs = 2_000L)

        assertTrue(cache.allowAndReserve("key", "title\u0000first", now = 1_000L))
        assertTrue(cache.allowAndReserve("key", "title\u0000second", now = 1_100L))
    }

    @Test
    fun `different keys with same content are allowed`() {
        val cache = DedupeCache(ttlMs = 2_000L)

        assertTrue(cache.allowAndReserve("key-1", "title\u0000message", now = 1_000L))
        assertTrue(cache.allowAndReserve("key-2", "title\u0000message", now = 1_100L))
    }

    @Test
    fun `similar but different content is allowed`() {
        val cache = DedupeCache(ttlMs = 2_000L)

        assertTrue(cache.allowAndReserve("key", "code\u00001234", now = 1_000L))
        assertTrue(cache.allowAndReserve("key", "code\u00001235", now = 1_100L))
    }

    @Test
    fun `same key and content outside window is allowed`() {
        val cache = DedupeCache(ttlMs = 2_000L)

        assertTrue(cache.allowAndReserve("key", "title\u0000message", now = 1_000L))
        assertTrue(cache.allowAndReserve("key", "title\u0000message", now = 3_000L))
    }

    @Test
    fun `oldest entry is evicted when cache reaches size limit`() {
        val cache = DedupeCache(ttlMs = 2_000L, maxSize = 2)

        assertTrue(cache.allowAndReserve("key-1", "one", now = 1_000L))
        assertTrue(cache.allowAndReserve("key-2", "two", now = 1_001L))
        assertTrue(cache.allowAndReserve("key-3", "three", now = 1_002L))
        assertTrue(cache.allowAndReserve("key-1", "one", now = 1_003L))
    }

    @Test
    fun `concurrent duplicate reservations allow only one`() {
        val cache = DedupeCache(ttlMs = 2_000L)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val tasks = List(32) {
                java.util.concurrent.Callable {
                    cache.allowAndReserve("key", "same content", now = 1_000L)
                }
            }

            val allowedCount = executor.invokeAll(tasks).count { it.get() }

            assertEquals(1, allowedCount)
        } finally {
            executor.shutdownNow()
        }
    }
}
