package com.droidnova.notificationhistory.service

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationContentExtractionTest {

    @Test
    fun `conversation title has priority`() {
        assertEquals(
            "Family chat",
            chooseMeaningfulTitle(
                conversationTitle = "Family chat",
                title = "Alice",
                bigTitle = "Alice sent a message",
                subText = "Messenger"
            )
        )
    }

    @Test
    fun `expanded title wins when it contains more information`() {
        assertEquals(
            "Build completed successfully",
            chooseMeaningfulTitle(
                conversationTitle = null,
                title = "Build completed",
                bigTitle = "Build completed successfully",
                subText = null
            )
        )
    }

    @Test
    fun `expanded text replaces its shortened preview`() {
        val complete = "This is the complete notification message with all important details."

        val result = combineMeaningfulText(
            sourcesInPriorityOrder = listOf(
                listOf(complete),
                listOf("This is the complete notification message...")
            ),
            title = "Update"
        )

        assertEquals(complete, result)
    }

    @Test
    fun `multiple lines retain order and remove repetitions`() {
        val result = combineMeaningfulText(
            sourcesInPriorityOrder = listOf(
                listOf("First message", "Second message", "First message"),
                listOf("Second message", "Third message")
            ),
            title = "Conversation"
        )

        assertEquals("First message\nSecond message\nThird message", result)
    }

    @Test
    fun `blank null-like and title duplicate values are removed`() {
        val result = combineMeaningfulText(
            sourcesInPriorityOrder = listOf(
                listOf(null, " ", "null", "…", "Status", "Useful body")
            ),
            title = "Status"
        )

        assertEquals("Useful body", result)
    }

    @Test
    fun `dedupe normalization ignores case and whitespace only`() {
        val first = ExtractedNotificationContent("Alice", "Hello   there")
        val second = ExtractedNotificationContent(" alice ", "hello\nthere")

        assertEquals(first.normalizedForDedupe(), second.normalizedForDedupe())
        assertEquals(first.contentFingerprint(), second.contentFingerprint())
    }
}
