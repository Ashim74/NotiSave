package com.droidnova.notificationhistory.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationIdentityTest {

    private val whatsApp = "com.whatsapp"

    private fun messaging(
        packageName: String = whatsApp,
        shortcutId: String? = null,
        conversationTitle: String? = null,
        title: String? = null,
        senders: List<String?> = emptyList(),
        isGroupConversation: Boolean? = null,
        isGroupSummary: Boolean = false,
        selfName: String? = null
    ) = ConversationSignals(
        packageName = packageName,
        appLabel = "WhatsApp",
        shortcutId = shortcutId,
        conversationTitle = conversationTitle,
        title = title,
        isMessageCategory = true,
        isMessagingStyle = true,
        isGroupSummary = isGroupSummary,
        isGroupConversation = isGroupConversation,
        messageSenders = senders,
        selfName = selfName
    )

    @Test
    fun `shortcut id has priority and key is package scoped`() {
        val identity = resolveConversationIdentity(
            messaging(shortcutId = "alice@s.whatsapp.net", title = "Alice", senders = listOf("Alice"))
        )

        assertEquals("com.whatsapp|s:alice@s.whatsapp.net", identity?.key)
        assertEquals("Alice", identity?.displayName)
    }

    @Test
    fun `same contact produces the same key across notifications`() {
        val first = resolveConversationIdentity(
            messaging(title = "Alice", senders = listOf("Alice"))
        )
        val second = resolveConversationIdentity(
            messaging(title = "Alice (3 messages)", senders = listOf("Alice", "Alice", null))
        )

        assertEquals(first?.key, second?.key)
        assertEquals("Alice", second?.displayName)
    }

    @Test
    fun `two contacts stay separate`() {
        val alice = resolveConversationIdentity(messaging(title = "Alice", senders = listOf("Alice")))
        val bob = resolveConversationIdentity(messaging(title = "Bob", senders = listOf("Bob")))

        assertNotEquals(alice?.key, bob?.key)
    }

    @Test
    fun `group chat uses its title and differs from a personal chat with a member`() {
        val group = resolveConversationIdentity(
            messaging(
                conversationTitle = "Family",
                title = "Family: Alice",
                senders = listOf("Alice", "Bob"),
                isGroupConversation = true
            )
        )
        val personal = resolveConversationIdentity(messaging(title = "Alice", senders = listOf("Alice")))

        assertEquals("Family", group?.displayName)
        assertTrue(group!!.key.contains("|t:family"))
        assertNotEquals(group.key, personal?.key)
    }

    @Test
    fun `group without a reliable title is not attributed to a member`() {
        assertNull(
            resolveConversationIdentity(
                messaging(title = "Alice", senders = listOf("Alice"), isGroupConversation = true)
            )
        )
        assertNull(
            resolveConversationIdentity(messaging(title = "Someone", senders = listOf("Alice", "Bob")))
        )
    }

    @Test
    fun `own replies do not turn a personal chat into a group`() {
        val identity = resolveConversationIdentity(
            messaging(title = "Alice", senders = listOf("Alice", "Me", null), selfName = "Me")
        )

        assertEquals("Alice", identity?.displayName)
    }

    @Test
    fun `same conversation in different apps is never combined`() {
        val whatsAppChat = resolveConversationIdentity(messaging(title = "Alice", senders = listOf("Alice")))
        val telegramChat = resolveConversationIdentity(
            messaging(packageName = "org.telegram.messenger", title = "Alice", senders = listOf("Alice"))
        )

        assertNotEquals(whatsAppChat?.key, telegramChat?.key)
        assertTrue(telegramChat!!.key.startsWith("org.telegram.messenger|"))
    }

    @Test
    fun `summaries and group summaries are not grouped`() {
        assertNull(resolveConversationIdentity(messaging(title = "WhatsApp", isGroupSummary = true)))
        assertNull(resolveConversationIdentity(messaging(title = "3 messages from 2 chats")))
        assertNull(resolveConversationIdentity(messaging(title = "5 new messages")))
        assertNull(resolveConversationIdentity(messaging(title = "WhatsApp")))
    }

    @Test
    fun `non messaging notifications are not grouped`() {
        val signals = ConversationSignals(
            packageName = "com.example.shop",
            shortcutId = "promo",
            title = "Your order shipped"
        )

        assertNull(resolveConversationIdentity(signals))
    }

    @Test
    fun `blank and malformed values are handled safely`() {
        assertNull(resolveConversationIdentity(messaging(shortcutId = "  ", title = " ", senders = listOf(null, " "))))
        assertNull(resolveConversationIdentity(messaging(packageName = " ", title = "Alice")))
        assertNull(resolveConversationIdentity(messaging(title = "null")))
    }

    @Test
    fun `title key ignores case and spacing differences`() {
        val first = resolveConversationIdentity(messaging(conversationTitle = "Project  Team"))
        val second = resolveConversationIdentity(messaging(conversationTitle = "project team"))

        assertEquals(first?.key, second?.key)
    }
}
