package com.droidnova.notificationhistory.core.lock

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-HMAC-SHA256 with a per-secret random salt. The iteration count is stored with every
 * hash, so it can be raised later without invalidating existing PINs. CPU-heavy (~100–300 ms
 * on low-end phones): call off the main thread.
 */
class SecretHasher(private val iterations: Int = DEFAULT_ITERATIONS) {

    private val random = SecureRandom()

    fun hash(secret: String): HashedSecret {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return HashedSecret(
            hash = encode(derive(secret, salt, iterations)),
            salt = encode(salt),
            iterations = iterations
        )
    }

    fun matches(secret: String, stored: HashedSecret): Boolean {
        val salt = runCatching { decode(stored.salt) }.getOrNull() ?: return false
        val expected = runCatching { decode(stored.hash) }.getOrNull() ?: return false
        // Constant-time comparison: no timing hint about how many leading bytes matched.
        return MessageDigest.isEqual(derive(secret, salt, stored.iterations), expected)
    }

    private fun derive(secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    companion object {
        const val DEFAULT_ITERATIONS = 120_000
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
    }
}
