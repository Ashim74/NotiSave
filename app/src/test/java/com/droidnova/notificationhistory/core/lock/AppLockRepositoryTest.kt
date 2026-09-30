package com.droidnova.notificationhistory.core.lock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class InMemoryAppLockStore : AppLockStore {
    val state = MutableStateFlow(LockConfig())
    override val config: Flow<LockConfig> = state

    override suspend fun update(transform: (LockConfig) -> LockConfig): LockConfig {
        state.value = transform(state.value)
        return state.value
    }
}

class AppLockRepositoryTest {

    private val store = InMemoryAppLockStore()
    private var now = 1_000_000L
    // Low iteration count keeps the suite fast; production uses SecretHasher.DEFAULT_ITERATIONS.
    private val repository = AppLockRepository(
        store = store,
        elapsedRealtime = { now },
        hasher = SecretHasher(iterations = 1_000),
        hashDispatcher = Dispatchers.Unconfined
    )

    private fun enablePin(pin: String = "1234") = runBlocking {
        repository.enable(LockType.Pin, pin, RecoveryMethod.DeviceAndCode, biometricEnabled = false)
    }

    @Test
    fun `enable stores only a salted hash, never the secret`() {
        enablePin("1234")
        val config = store.state.value
        assertTrue(config.isEnabled)
        assertEquals(LockType.Pin, config.type)
        assertFalse(config.toString().contains("1234"))
        assertNotEquals("1234", config.secret!!.hash)
    }

    @Test
    fun `same pin hashes differently each time because of the salt`() {
        enablePin("1234")
        val first = store.state.value.secret
        enablePin("1234")
        assertNotEquals(first!!.hash, store.state.value.secret!!.hash)
    }

    @Test
    fun `correct pin succeeds and wrong pin reports attempts left`() = runBlocking {
        enablePin("1234")
        assertEquals(VerifyResult.Wrong(4), repository.verifySecret("0000"))
        assertEquals(VerifyResult.Success, repository.verifySecret("1234"))
        assertEquals(0, store.state.value.failedAttempts)
    }

    @Test
    fun `fifth wrong entry locks out for 30 seconds and blocks even the right pin`() = runBlocking {
        enablePin("1234")
        repeat(4) { repository.verifySecret("0000") }
        assertEquals(VerifyResult.LockedOut(30_000L), repository.verifySecret("0000"))

        now += 10_000L
        val result = repository.verifySecret("1234")
        assertEquals(VerifyResult.LockedOut(20_000L), result)

        now += 20_000L
        assertEquals(VerifyResult.Success, repository.verifySecret("1234"))
    }

    @Test
    fun `lockouts escalate after each further wrong entry`() = runBlocking {
        enablePin("1234")
        repeat(5) { repository.verifySecret("0000") }
        now += 30_000L
        assertEquals(VerifyResult.LockedOut(60_000L), repository.verifySecret("0000"))
        now += 60_000L
        assertEquals(VerifyResult.LockedOut(300_000L), repository.verifySecret("0000"))
    }

    @Test
    fun `lockout survives a reboot`() = runBlocking {
        enablePin("1234")
        repeat(5) { repository.verifySecret("0000") }
        // Reboot: elapsedRealtime restarts near zero, 5 s after boot.
        now = 5_000L
        assertEquals(25_000L, repository.lockoutRemaining(store.state.value))
        assertTrue(repository.verifySecret("1234") is VerifyResult.LockedOut)
    }

    @Test
    fun `recovery code works, is normalized, and shares the lockout counter`() = runBlocking {
        val code = enablePin("1234")
        assertTrue(repository.verifyRecoveryCode("AAAA-AAAA-AAAA") is VerifyResult.Wrong)
        assertEquals(1, store.state.value.failedAttempts)
        assertEquals(VerifyResult.Success, repository.verifyRecoveryCode(code.lowercase().replace("-", " ")))
        assertEquals(0, store.state.value.failedAttempts)
    }

    @Test
    fun `reset after recovery sets the new pin and rotates the recovery code`() = runBlocking {
        val oldCode = enablePin("1234")
        val newCode = repository.resetAfterRecovery(LockType.Password, "correct horse")
        assertNotEquals(oldCode, newCode)
        assertEquals(LockType.Password, store.state.value.type)
        assertTrue(repository.verifyRecoveryCode(oldCode) is VerifyResult.Wrong)
        assertEquals(VerifyResult.Success, repository.verifyRecoveryCode(newCode))
        assertEquals(VerifyResult.Success, repository.verifySecret("correct horse"))
        assertTrue(repository.verifySecret("1234") is VerifyResult.Wrong)
    }

    @Test
    fun `change keeps the recovery code and clears failures`() = runBlocking {
        val code = enablePin("1234")
        repository.verifySecret("0000")
        repository.changeSecret(LockType.Pin, "987654")
        assertEquals(0, store.state.value.failedAttempts)
        assertEquals(VerifyResult.Success, repository.verifySecret("987654"))
        assertEquals(VerifyResult.Success, repository.verifyRecoveryCode(code))
    }

    @Test
    fun `disable clears every secret but keeps display preferences`() = runBlocking {
        enablePin("1234")
        repository.setTimeout(AutoLockTimeout.Minute1)
        repository.setHideInRecents(false)
        repository.setBiometricEnabled(true)
        repeat(5) { repository.verifySecret("0000") }

        repository.disable()

        val config = store.state.value
        assertFalse(config.isEnabled)
        assertNull(config.secret)
        assertNull(config.recovery)
        assertFalse(config.biometricEnabled)
        assertEquals(0L, repository.lockoutRemaining(config))
        assertEquals(AutoLockTimeout.Minute1, config.timeout)
        assertFalse(config.hideInRecents)
    }
}
