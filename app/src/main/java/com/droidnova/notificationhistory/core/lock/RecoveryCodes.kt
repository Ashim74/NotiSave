package com.droidnova.notificationhistory.core.lock

import java.security.SecureRandom
import java.util.Locale

/** One-time recovery codes: 12 characters shown as `XXXX-XXXX-XXXX`, ~60 bits of entropy. */
object RecoveryCodes {
    // No 0/O or 1/I, so a code copied onto paper reads back unambiguously.
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LENGTH = 12
    private const val GROUP = 4

    fun generate(random: SecureRandom = SecureRandom()): String =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }
            .chunked(GROUP)
            .joinToString("-") { it.joinToString("") }

    /** Accepts lower case, spaces and dashes; what gets hashed and compared. */
    fun normalize(input: String): String =
        input.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }
}
