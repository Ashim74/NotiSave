package com.droidnova.notificationhistory.core.lock

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-HMAC-SHA256 with a per-secret random salt. The iteration count is stored with every
 * hash, so it can change without invalidating existing PINs (see [needsRehash]).
 *
 * Android's PBKDF2 is pure Java, so cost scales hard on low-end CPUs: 120 000 iterations took
 * seconds on a cold start on a Snapdragon 439 and made unlock feel frozen. A 4-digit PIN has
 * only 10 000 values, so stretching barely slows an offline guess anyway; the real defence is
 * the lockout plus app-private storage. Call off the main thread.
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

    /** True for hashes made with another iteration count; re-hash them after a successful match. */
    fun needsRehash(stored: HashedSecret): Boolean = stored.iterations != iterations

    /**
     * Loads the crypto provider and lets the JIT compile the hash loop, so the first real check
     * runs at full speed. Cheap enough to run while the lock screen is showing.
     */
    fun warmUp() {
        runCatching { derive("0000", ByteArray(SALT_BYTES), iterations) }
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
        const val DEFAULT_ITERATIONS = 10_000
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
    }
}
