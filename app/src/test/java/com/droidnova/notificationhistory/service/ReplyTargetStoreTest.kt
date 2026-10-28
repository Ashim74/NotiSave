package com.droidnova.notificationhistory.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplyTargetStoreTest {

    private data class Target(val notificationKey: String, val postTime: Long)

    private fun registry() = ReplyTargetRegistry<Target>(
        notificationKeyOf = { it.notificationKey },
        postTimeOf = { it.postTime }
    )

    private fun candidate(freeForm: Boolean, reply: Boolean = false) =
        ReplyActionCandidate(hasFreeFormInput = freeForm, isSemanticReply = reply)

    @Test
    fun `semantic reply action is preferred over an earlier free-form one`() {
        val actions = listOf(candidate(freeForm = true), candidate(freeForm = true, reply = true))
        assertEquals(1, chooseReplyActionIndex(actions))
    }

    @Test
    fun `first free-form action is used when none is marked as reply`() {
        val actions = listOf(candidate(freeForm = false), candidate(freeForm = true), candidate(freeForm = true))
        assertEquals(1, chooseReplyActionIndex(actions))
    }

    @Test
    fun `semantic reply without free-form input is not a reply target`() {
        val actions = listOf(candidate(freeForm = false, reply = true), candidate(freeForm = true))
        assertEquals(1, chooseReplyActionIndex(actions))
    }

    @Test
    fun `no free-form action means no reply target`() {
        assertNull(chooseReplyActionIndex(listOf(candidate(freeForm = false), candidate(freeForm = false))))
        assertNull(chooseReplyActionIndex(emptyList()))
    }

    @Test
    fun `newer post replaces the older target`() {
        val store = registry()
        store.put("chat", Target("key-1", postTime = 100))
        store.put("chat", Target("key-2", postTime = 200))
        assertEquals("key-2", store.get("chat")?.notificationKey)
    }

    @Test
    fun `an older post never replaces a newer target`() {
        val store = registry()
        store.put("chat", Target("key-2", postTime = 200))
        store.put("chat", Target("key-1", postTime = 100))
        assertEquals("key-2", store.get("chat")?.notificationKey)
    }

    @Test
    fun `forget drops the target only when its notification key matches`() {
        val store = registry()
        store.put("chat", Target("key-2", postTime = 200))

        store.forget("chat", "key-1")
        assertEquals("key-2", store.get("chat")?.notificationKey)

        store.forget("chat", "key-2")
        assertNull(store.get("chat"))
    }

    @Test
    fun `removing a notification drops only the targets it created`() {
        val store = registry()
        store.put("alice", Target("key-a", postTime = 100))
        store.put("bob", Target("key-b", postTime = 100))

        store.forgetNotification("key-a")

        assertNull(store.get("alice"))
        assertEquals("key-b", store.get("bob")?.notificationKey)
    }

    @Test
    fun `clear drops every target`() {
        val store = registry()
        store.put("alice", Target("key-a", postTime = 100))
        store.put("bob", Target("key-b", postTime = 100))

        store.clear()

        assertNull(store.get("alice"))
        assertNull(store.get("bob"))
    }
}
