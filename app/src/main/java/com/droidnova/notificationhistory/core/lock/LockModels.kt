package com.droidnova.notificationhistory.core.lock

enum class LockType(val storageKey: String) {
    Pin("pin"), Password("password");

    companion object {
        fun fromStorageKey(key: String?): LockType? = entries.firstOrNull { it.storageKey == key }
    }
}

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

/** Which "Forgot PIN?" options are offered besides erase-and-reset (always available). */
enum class RecoveryMethod(val storageKey: String) {
    /** Phone screen lock first, recovery code as backup. */
    DeviceAndCode("device_and_code"),
    /** Recovery code only — for users whose phone PIN is known to others. */
    CodeOnly("code_only");

    companion object {
        fun fromStorageKey(key: String?): RecoveryMethod =
            entries.firstOrNull { it.storageKey == key } ?: DeviceAndCode
    }
}

/** A salted PBKDF2 hash; the secret itself is never stored. Fields are Base64. */
data class HashedSecret(val hash: String, val salt: String, val iterations: Int)

data class LockConfig(
    val type: LockType? = null,
    val secret: HashedSecret? = null,
    val recovery: HashedSecret? = null,
    val recoveryMethod: RecoveryMethod = RecoveryMethod.DeviceAndCode,
    val biometricEnabled: Boolean = false,
    val timeout: AutoLockTimeout = AutoLockTimeout.Immediately,
    val hideInRecents: Boolean = true,
    /** Wrong PIN/password/recovery-code entries since the last success. */
    val failedAttempts: Int = 0,
    /** `elapsedRealtime` when the current lockout began; see [LockoutPolicy.remaining]. */
    val lockoutStartedElapsed: Long = 0L,
    val lockoutDurationMs: Long = 0L
) {
    val isEnabled: Boolean get() = type != null && secret != null
}

sealed interface VerifyResult {
    data object Success : VerifyResult
    /** Wrong entry; [attemptsLeft] more wrong entries are allowed before a lockout. */
    data class Wrong(val attemptsLeft: Int) : VerifyResult
    data class LockedOut(val remainingMs: Long) : VerifyResult
}

enum class SecretError { TooShort, TooLong, DigitsOnly }

object SecretRules {
    const val PIN_MIN = 4
    const val PIN_MAX = 8
    const val PASSWORD_MIN = 6
    const val PASSWORD_MAX = 64

    fun maxLength(type: LockType): Int = if (type == LockType.Pin) PIN_MAX else PASSWORD_MAX

    fun validate(type: LockType, secret: String): SecretError? = when (type) {
        LockType.Pin -> when {
            !secret.all { it in '0'..'9' } -> SecretError.DigitsOnly
            secret.length < PIN_MIN -> SecretError.TooShort
            secret.length > PIN_MAX -> SecretError.TooLong
            else -> null
        }
        LockType.Password -> when {
            secret.length < PASSWORD_MIN -> SecretError.TooShort
            secret.length > PASSWORD_MAX -> SecretError.TooLong
            else -> null
        }
    }
}
