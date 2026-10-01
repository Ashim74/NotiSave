package com.droidnova.notificationhistory.core.lock

/** How long the app may sit in the background before it asks for the PIN again. */
enum class AutoLockTimeout(val storageKey: String, val millis: Long) {
    Immediately("immediately", 0L),
    Seconds30("30s", 30_000L),
    Minute1("1m", 60_000L),
    Minutes5("5m", 300_000L);

    companion object {
        fun fromStorageKey(key: String?): AutoLockTimeout =
            entries.firstOrNull { it.storageKey == key } ?: Immediately
    }
}

/** A salted PBKDF2 hash; the secret itself is never stored. Fields are Base64. */
data class HashedSecret(val hash: String, val salt: String, val iterations: Int)

data class LockConfig(
    val pin: HashedSecret? = null,
    val recovery: HashedSecret? = null,
    val biometricEnabled: Boolean = false,
    val timeout: AutoLockTimeout = AutoLockTimeout.Immediately,
    /** Wrong PIN/recovery-code entries since the last success. */
    val failedAttempts: Int = 0,
    /** `elapsedRealtime` when the current lockout began; see [LockoutPolicy.remaining]. */
    val lockoutStartedElapsed: Long = 0L,
    val lockoutDurationMs: Long = 0L
) {
    val isEnabled: Boolean get() = pin != null
}

sealed interface VerifyResult {
    data object Success : VerifyResult
    /** Wrong entry; [attemptsLeft] more wrong entries are allowed before a lockout. */
    data class Wrong(val attemptsLeft: Int) : VerifyResult
    data class LockedOut(val remainingMs: Long) : VerifyResult
}

object PinRules {
    /** Fixed length, so the keypad can submit on the last digit without a confirm key. */
    const val LENGTH = 4

    fun isValid(pin: String): Boolean = pin.length == LENGTH && pin.all { it in '0'..'9' }
}
