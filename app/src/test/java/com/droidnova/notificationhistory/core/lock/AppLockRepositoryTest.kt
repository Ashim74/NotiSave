package com.droidnova.notificationhistory.core.lock

import kotlinx.coroutines.CoroutineScope
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
    /** Number of writes, to check the fast path doesn't touch the disk. */
    var writes = 0

    override suspend fun update(transform: (LockConfig) -> LockConfig): LockConfig {
        writes++
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
        hashDispatcher = Dispatchers.Unconfined,
        // Runs background hash upgrades inline, so tests can assert on them directly.
        backgroundScope = CoroutineScope(Dispatchers.Unconfined)
    )

    private fun enablePin(pin: String = "1234") = runBlocking {
        repository.enable(pin)
    }

    @Test
    fun `enable stores only a salted hash, never the secret`() {
        enablePin("1234")
        val config = store.state.value
        assertTrue(config.isEnabled)
        assertFalse(config.toString().contains("1234"))
        assertNotEquals("1234", config.pin!!.hash)
    }

    @Test
    fun `same pin hashes differently each time because of the salt`() {
        enablePin("1234")
        val first = store.state.value.pin
        enablePin("1234")
        assertNotEquals(first!!.hash, store.state.value.pin!!.hash)
    }

    @Test
    fun `correct pin succeeds and wrong pin reports attempts left`() = runBlocking {
        enablePin("1234")
        assertEquals(VerifyResult.Wrong(4), repository.verifyPin("0000"))
        assertEquals(VerifyResult.Success, repository.verifyPin("1234"))
        assertEquals(0, store.state.value.failedAttempts)
    }

    @Test
    fun `fifth wrong entry locks out for 30 seconds and blocks even the right pin`() = runBlocking {
        enablePin("1234")
        repeat(4) { repository.verifyPin("0000") }
        assertEquals(VerifyResult.LockedOut(30_000L), repository.verifyPin("0000"))

        now += 10_000L
        val result = repository.verifyPin("1234")
        assertEquals(VerifyResult.LockedOut(20_000L), result)

        now += 20_000L
        assertEquals(VerifyResult.Success, repository.verifyPin("1234"))
    }

    @Test
    fun `lockouts escalate after each further wrong entry`() = runBlocking {
        enablePin("1234")
        repeat(5) { repository.verifyPin("0000") }
        now += 30_000L
        assertEquals(VerifyResult.LockedOut(60_000L), repository.verifyPin("0000"))
        now += 60_000L
        assertEquals(VerifyResult.LockedOut(300_000L), repository.verifyPin("0000"))
    }

    @Test
    fun `lockout survives a reboot`() = runBlocking {
        enablePin("1234")
        repeat(5) { repository.verifyPin("0000") }
        // Reboot: elapsedRealtime restarts near zero, 5 s after boot.
        now = 5_000L
        assertEquals(25_000L, repository.lockoutRemaining(store.state.value))
        assertTrue(repository.verifyPin("1234") is VerifyResult.LockedOut)
    }

    @Test
    fun `recovery code works, is normalized, and shares the lockout counter`() = runBlocking {
        val code = enablePin("1234")
        assertTrue(repository.verifyRecoveryCode("AAAA-AAAA") is VerifyResult.Wrong)
        assertEquals(1, store.state.value.failedAttempts)
        assertEquals(VerifyResult.Success, repository.verifyRecoveryCode(code.lowercase().replace("-", " ")))
        assertEquals(0, store.state.value.failedAttempts)
    }

    @Test
    fun `recovery code keeps working after it was used to set a new pin`() = runBlocking {
        val code = enablePin("1234")
        assertEquals(VerifyResult.Success, repository.verifyRecoveryCode(code))
        repository.changePin("5678")
        assertEquals(VerifyResult.Success, repository.verifyPin("5678"))
        assertTrue(repository.verifyPin("1234") is VerifyResult.Wrong)
        assertEquals(VerifyResult.Success, repository.verifyRecoveryCode(code))
    }

    @Test
    fun `a correct pin with no failures to clear does not write to disk`() = runBlocking {
        enablePin("1234")
        val writesBefore = store.writes
        assertEquals(VerifyResult.Success, repository.verifyPin("1234"))
        assertEquals(writesBefore, store.writes)
    }

    @Test
    fun `slow legacy hashes are upgraded after a successful unlock`() = runBlocking {
        // A PIN saved by the earlier, slower build (more iterations).
        val legacy = SecretHasher(iterations = 2_000).hash("1234")
        store.state.value = LockConfig(pin = legacy)

        assertEquals(VerifyResult.Success, repository.verifyPin("1234"))

        val upgraded = store.state.value.pin!!
        assertEquals(1_000, upgraded.iterations)
        assertNotEquals(legacy, upgraded)
        assertEquals(VerifyResult.Success, repository.verifyPin("1234"))
        assertTrue(repository.verifyPin("0000") is VerifyResult.Wrong)
    }

    @Test
    fun `change keeps the recovery code and clears failures`() = runBlocking {
        val code = enablePin("1234")
        repository.verifyPin("0000")
        repository.changePin("9876")
        assertEquals(0, store.state.value.failedAttempts)
        assertEquals(VerifyResult.Success, repository.verifyPin("9876"))
        assertEquals(VerifyResult.Success, repository.verifyRecoveryCode(code))
    }

    @Test
    fun `new pin after phone screen lock ends a running lockout`() = runBlocking {
        enablePin("1234")
        repeat(5) { repository.verifyPin("0000") }
        assertTrue(repository.lockoutRemaining(store.state.value) > 0L)

        repository.changePin("4321")

        assertEquals(0L, repository.lockoutRemaining(store.state.value))
        assertEquals(VerifyResult.Success, repository.verifyPin("4321"))
    }

    @Test
    fun `disable clears the pin, code and fingerprint but keeps the timeout`() = runBlocking {
        enablePin("1234")
        repository.setTimeout(AutoLockTimeout.Minute1)
        repository.setBiometricEnabled(true)
        repeat(5) { repository.verifyPin("0000") }

        repository.disable()

        val config = store.state.value
        assertFalse(config.isEnabled)
        assertNull(config.pin)
        assertNull(config.recovery)
        assertFalse(config.biometricEnabled)
        assertEquals(0L, repository.lockoutRemaining(config))
        assertEquals(AutoLockTimeout.Minute1, config.timeout)
    }
}
