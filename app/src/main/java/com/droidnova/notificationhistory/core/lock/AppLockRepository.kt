package com.droidnova.notificationhistory.core.lock

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val hashDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /** Runs follow-up work (hash upgrades) after a successful check has already returned. */
    private val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    val config: Flow<LockConfig> = store.config

    suspend fun current(): LockConfig = store.config.first()

    /** Prepares the hasher while the user is still typing; see [SecretHasher.warmUp]. */
    suspend fun warmUp() = withContext(hashDispatcher) { hasher.warmUp() }

    /** Turns the lock on and returns the recovery code to show the user once. */
    suspend fun enable(pin: String): String {
        val code = RecoveryCodes.generate()
        val hashedPin = hash(pin)
        val hashedCode = hash(RecoveryCodes.normalize(code))
        store.update { it.copy(pin = hashedPin, recovery = hashedCode).clearedFailures() }
        return code
    }

    suspend fun verifyPin(pin: String): VerifyResult =
        verify(pin, target = { it.pin }, replace = { config, hash -> config.copy(pin = hash) })

    suspend fun verifyRecoveryCode(code: String): VerifyResult = verify(
        RecoveryCodes.normalize(code),
        target = { it.recovery },
        replace = { config, hash -> config.copy(recovery = hash) }
    )

    fun lockoutRemaining(config: LockConfig): Long = LockoutPolicy.remaining(
        startedElapsed = config.lockoutStartedElapsed,
        durationMs = config.lockoutDurationMs,
        nowElapsed = elapsedRealtime()
    )

    /**
     * New PIN after the current PIN, the phone screen lock or the recovery code was confirmed.
     * The recovery code stays valid, so it keeps working the next time a PIN is forgotten.
     * Also ends any running lockout.
     */
    suspend fun changePin(pin: String) {
        val hashed = hash(pin)
        store.update { it.copy(pin = hashed).clearedFailures() }
    }

    suspend fun regenerateRecoveryCode(): String {
        val code = RecoveryCodes.generate()
        val hashedCode = hash(RecoveryCodes.normalize(code))
        store.update { it.copy(recovery = hashedCode) }
        return code
    }

    /** Clears the PIN, recovery code and fingerprint opt-in; the timeout is kept for next time. */
    suspend fun disable() {
        store.update { LockConfig(timeout = it.timeout) }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        store.update { it.copy(biometricEnabled = enabled) }
    }

    suspend fun setTimeout(timeout: AutoLockTimeout) {
        store.update { it.copy(timeout = timeout) }
    }

    private suspend fun verify(
        input: String,
        target: (LockConfig) -> HashedSecret?,
        replace: (LockConfig, HashedSecret) -> LockConfig
    ): VerifyResult {
        val config = current()
        val remaining = lockoutRemaining(config)
        if (remaining > 0L) return VerifyResult.LockedOut(remaining)
        val stored = target(config) ?: return VerifyResult.Wrong(LockoutPolicy.FREE_ATTEMPTS)

        val matches = withContext(hashDispatcher) { hasher.matches(input, stored) }
        if (matches) {
            // The usual case has nothing to reset: skip the disk write so unlock is instant.
            if (config.failedAttempts > 0 || config.lockoutDurationMs > 0L) {
                store.update { it.clearedFailures() }
            }
            if (hasher.needsRehash(stored)) upgradeHash(input, stored, target, replace)
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

    /**
     * Re-hashes with the current iteration count after the app has already opened. Skipped if the
     * secret was changed meanwhile, so an upgrade can never overwrite a newer PIN or code.
     */
    private fun upgradeHash(
        input: String,
        stored: HashedSecret,
        target: (LockConfig) -> HashedSecret?,
        replace: (LockConfig, HashedSecret) -> LockConfig
    ) {
        backgroundScope.launch {
            val upgraded = hash(input)
            store.update { if (target(it) == stored) replace(it, upgraded) else it }
        }
    }

    private suspend fun hash(secret: String): HashedSecret =
        withContext(hashDispatcher) { hasher.hash(secret) }

    private fun LockConfig.clearedFailures() =
        copy(failedAttempts = 0, lockoutStartedElapsed = 0L, lockoutDurationMs = 0L)
}
