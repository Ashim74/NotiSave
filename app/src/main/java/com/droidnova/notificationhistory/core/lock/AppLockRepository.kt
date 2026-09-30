package com.droidnova.notificationhistory.core.lock

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * All app-lock rules: hashing, verification with lockout, recovery codes. Callers are
 * responsible for proving identity (current PIN, recovery code or phone screen lock) before
 * calling the mutating methods — the UI flows enforce that order.
 */
class AppLockRepository(
    private val store: AppLockStore,
    private val elapsedRealtime: () -> Long,
    private val hasher: SecretHasher = SecretHasher(),
    private val hashDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    val config: Flow<LockConfig> = store.config

    suspend fun current(): LockConfig = store.config.first()

    /** Turns the lock on and returns the recovery code to show the user once. */
    suspend fun enable(
        type: LockType,
        secret: String,
        recoveryMethod: RecoveryMethod,
        biometricEnabled: Boolean
    ): String {
        val code = RecoveryCodes.generate()
        val hashedSecret = hash(secret)
        val hashedCode = hash(RecoveryCodes.normalize(code))
        store.update {
            it.copy(
                type = type,
                secret = hashedSecret,
                recovery = hashedCode,
                recoveryMethod = recoveryMethod,
                biometricEnabled = biometricEnabled
            ).clearedFailures()
        }
        return code
    }

    suspend fun verifySecret(secret: String): VerifyResult = verify(secret) { it.secret }

    suspend fun verifyRecoveryCode(code: String): VerifyResult =
        verify(RecoveryCodes.normalize(code)) { it.recovery }

    fun lockoutRemaining(config: LockConfig): Long = LockoutPolicy.remaining(
        startedElapsed = config.lockoutStartedElapsed,
        durationMs = config.lockoutDurationMs,
        nowElapsed = elapsedRealtime()
    )

    /** Change PIN/password (after the current one was verified). Recovery code is kept. */
    suspend fun changeSecret(type: LockType, secret: String) {
        val hashed = hash(secret)
        store.update { it.copy(type = type, secret = hashed).clearedFailures() }
    }

    /** After a successful recovery: new PIN/password and a fresh code (the old one is spent). */
    suspend fun resetAfterRecovery(type: LockType, secret: String): String {
        val code = RecoveryCodes.generate()
        val hashedSecret = hash(secret)
        val hashedCode = hash(RecoveryCodes.normalize(code))
        store.update {
            it.copy(type = type, secret = hashedSecret, recovery = hashedCode).clearedFailures()
        }
        return code
    }

    suspend fun regenerateRecoveryCode(): String {
        val code = RecoveryCodes.generate()
        val hashedCode = hash(RecoveryCodes.normalize(code))
        store.update { it.copy(recovery = hashedCode) }
        return code
    }

    /** Clears every secret; timeout / Recents preferences are kept for a future re-enable. */
    suspend fun disable() {
        store.update { LockConfig(timeout = it.timeout, hideInRecents = it.hideInRecents) }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        store.update { it.copy(biometricEnabled = enabled) }
    }

    suspend fun setTimeout(timeout: AutoLockTimeout) {
        store.update { it.copy(timeout = timeout) }
    }

    suspend fun setHideInRecents(hide: Boolean) {
        store.update { it.copy(hideInRecents = hide) }
    }

    suspend fun setRecoveryMethod(method: RecoveryMethod) {
        store.update { it.copy(recoveryMethod = method) }
    }

    private suspend fun verify(input: String, target: (LockConfig) -> HashedSecret?): VerifyResult {
        val config = current()
        val remaining = lockoutRemaining(config)
        if (remaining > 0L) return VerifyResult.LockedOut(remaining)
        val stored = target(config) ?: return VerifyResult.Wrong(LockoutPolicy.FREE_ATTEMPTS)

        val matches = withContext(hashDispatcher) { hasher.matches(input, stored) }
        if (matches) {
            store.update { it.clearedFailures() }
            return VerifyResult.Success
        }
        val updated = store.update {
            val failed = it.failedAttempts + 1
            val lockout = LockoutPolicy.lockoutFor(failed)
            it.copy(
                failedAttempts = failed,
                lockoutStartedElapsed = if (lockout > 0L) elapsedRealtime() else 0L,
                lockoutDurationMs = lockout
            )
        }
        return if (updated.lockoutDurationMs > 0L) {
            VerifyResult.LockedOut(updated.lockoutDurationMs)
        } else {
            VerifyResult.Wrong(LockoutPolicy.FREE_ATTEMPTS - updated.failedAttempts)
        }
    }

    private suspend fun hash(secret: String): HashedSecret =
        withContext(hashDispatcher) { hasher.hash(secret) }

    private fun LockConfig.clearedFailures() =
        copy(failedAttempts = 0, lockoutStartedElapsed = 0L, lockoutDurationMs = 0L)
}
