package com.droidnova.notificationhistory.core.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockRulesTest {

    @Test
    fun `pin must be 4 to 8 digits`() {
        assertEquals(SecretError.TooShort, SecretRules.validate(LockType.Pin, "123"))
        assertNull(SecretRules.validate(LockType.Pin, "1234"))
        assertNull(SecretRules.validate(LockType.Pin, "12345678"))
        assertEquals(SecretError.TooLong, SecretRules.validate(LockType.Pin, "123456789"))
        assertEquals(SecretError.DigitsOnly, SecretRules.validate(LockType.Pin, "12a4"))
    }

    @Test
    fun `password needs at least 6 characters`() {
        assertEquals(SecretError.TooShort, SecretRules.validate(LockType.Password, "abc12"))
        assertNull(SecretRules.validate(LockType.Password, "abc123"))
    }

    @Test
    fun `no lockout before the fifth failure, then escalating, capped at an hour`() {
        assertEquals(0L, LockoutPolicy.lockoutFor(4))
        assertEquals(30_000L, LockoutPolicy.lockoutFor(5))
        assertEquals(60_000L, LockoutPolicy.lockoutFor(6))
        assertEquals(300_000L, LockoutPolicy.lockoutFor(7))
        assertEquals(900_000L, LockoutPolicy.lockoutFor(8))
        assertEquals(3_600_000L, LockoutPolicy.lockoutFor(9))
        assertEquals(3_600_000L, LockoutPolicy.lockoutFor(50))
    }

    @Test
    fun `remaining lockout counts only monotonic time and never less after a reboot`() {
        // Same boot.
        assertEquals(20_000L, LockoutPolicy.remaining(100_000L, 30_000L, 110_000L))
        assertEquals(0L, LockoutPolicy.remaining(100_000L, 30_000L, 200_000L))
        // Rebooted: "now" is below the start, so only time since boot counts.
        assertEquals(26_000L, LockoutPolicy.remaining(100_000L, 30_000L, 4_000L))
        assertEquals(0L, LockoutPolicy.remaining(0L, 0L, 5L))
    }

    @Test
    fun `recovery codes are grouped, unambiguous and normalize back`() {
        val code = RecoveryCodes.generate()
        assertTrue(code.matches(Regex("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}")))
        assertTrue(code.none { it in "01OI" })
        assertEquals(RecoveryCodes.LENGTH, RecoveryCodes.normalize(code).length)
        assertEquals("ABCD2345EFGH", RecoveryCodes.normalize(" abcd-2345 efgh "))
    }

    @Test
    fun `hasher accepts only the matching secret`() {
        val hasher = SecretHasher(iterations = 1_000)
        val stored = hasher.hash("hunter22")
        assertTrue(hasher.matches("hunter22", stored))
        assertEquals(false, hasher.matches("hunter23", stored))
        assertEquals(false, hasher.matches("hunter22", stored.copy(salt = "not base64!")))
    }
}
