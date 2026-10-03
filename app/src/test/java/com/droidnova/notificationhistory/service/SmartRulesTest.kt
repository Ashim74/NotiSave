package com.droidnova.notificationhistory.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRulesTest {

    @Test
    fun `deleted placeholders are recognised with icon, case and period variations`() {
        assertTrue(isDeletedPlaceholder("This message was deleted"))
        assertTrue(isDeletedPlaceholder("🚫 This message was deleted."))
        assertTrue(isDeletedPlaceholder("🚫️ this message was deleted"))
        assertTrue(isDeletedPlaceholder("Se eliminó este mensaje"))
        assertTrue(isDeletedPlaceholder("Pesan ini telah dihapus"))
    }

    @Test
    fun `real messages that mention deletion are not placeholders`() {
        assertFalse(isDeletedPlaceholder("Why was this message deleted?"))
        assertFalse(isDeletedPlaceholder("I deleted the file"))
        assertFalse(isDeletedPlaceholder(""))
    }

    @Test
    fun `keyword match ignores case and spacing and returns the keyword`() {
        assertEquals("OTP", matchedKeyword("Your otp is 123456", setOf("bank", "OTP")))
        assertEquals("Boss", matchedKeyword("Ali\nMessage from boss", setOf("Boss")))
        assertNull(matchedKeyword("Nothing here", setOf("urgent")))
        assertNull(matchedKeyword("Anything", emptySet()))
        assertNull(matchedKeyword("Anything", setOf("  ")))
    }

    @Test
    fun `block words match title or message, ignoring spaces and case`() {
        assertTrue(containsBlockedWord("Promo", "50% off today", setOf("50%off")))
        assertTrue(containsBlockedWord("SALE now", "", setOf("sale")))
        assertFalse(containsBlockedWord("Mom", "Dinner at 8", setOf("sale")))
        assertFalse(containsBlockedWord("Mom", "Dinner", emptySet()))
    }
}
