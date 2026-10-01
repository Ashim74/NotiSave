package com.droidnova.notificationhistory.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExtractedMessageTest {

    @Test
    fun `fingerprint ignores timestamp, case and spacing`() {
        val first = ExtractedMessage(sender = "Alice", text = "Hi  there", timestamp = 1L)
        val repost = ExtractedMessage(sender = "alice", text = "hi there", timestamp = 2L)

        assertEquals(first.fingerprint(), repost.fingerprint())
    }

    @Test
    fun `different sender or text changes the fingerprint`() {
        val base = ExtractedMessage(sender = "Alice", text = "hi", timestamp = 1L)

        assertNotEquals(base.fingerprint(), base.copy(sender = "Bob").fingerprint())
        assertNotEquals(base.fingerprint(), base.copy(text = "hello").fingerprint())
    }

    @Test
    fun `one-to-one chat shows the text only`() {
        val message = ExtractedMessage(sender = "Alice", text = "hi", timestamp = 1L)

        assertEquals("hi", message.displayText(title = "Alice"))
    }

    @Test
    fun `group chat prefixes the sender`() {
        val message = ExtractedMessage(sender = "Alice", text = "hi", timestamp = 1L)

        assertEquals("Alice: hi", message.displayText(title = "Family"))
    }
}
