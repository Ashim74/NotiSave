package com.droidnova.notificationhistory.core.lock

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droidnova.notificationhistory.utils.CrashReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface AppLockStore {
    val config: Flow<LockConfig>

    /** Atomic read-modify-write; returns the stored result. */
    suspend fun update(transform: (LockConfig) -> LockConfig): LockConfig
}

/**
 * Kept in its own file (`datastore/app_lock.preferences_pb`), apart from the general prefs, so
 * backup/transfer rules can exclude it by name and a lock never follows history to a new phone.
 */
private val Context.appLockDataStore by preferencesDataStore(
    name = "app_lock",
    corruptionHandler = ReplaceFileCorruptionHandler { error ->
        CrashReporter.record(error)
        emptyPreferences()
    }
)

class DataStoreAppLockStore(context: Context) : AppLockStore {

    private val dataStore = context.applicationContext.appLockDataStore

    override val config: Flow<LockConfig> = dataStore.data
        .catch { error ->
            if (error !is IOException) throw error
            CrashReporter.record(error)
            emit(emptyPreferences())
        }
        .map { it.toLockConfig() }

    override suspend fun update(transform: (LockConfig) -> LockConfig): LockConfig {
        var result = LockConfig()
        dataStore.edit { prefs ->
            result = transform(prefs.toLockConfig())
            prefs.write(result)
        }
        return result
    }

    private fun Preferences.toLockConfig() = LockConfig(
        pin = hashed(SECRET_HASH, SECRET_SALT, SECRET_ITERATIONS),
        recovery = hashed(RECOVERY_HASH, RECOVERY_SALT, RECOVERY_ITERATIONS),
        biometricEnabled = this[BIOMETRIC] ?: false,
        timeout = AutoLockTimeout.fromStorageKey(this[TIMEOUT]),
        failedAttempts = this[FAILED_ATTEMPTS] ?: 0,
        lockoutStartedElapsed = this[LOCKOUT_STARTED] ?: 0L,
        lockoutDurationMs = this[LOCKOUT_DURATION] ?: 0L
    )

    private fun Preferences.hashed(
        hash: Preferences.Key<String>,
        salt: Preferences.Key<String>,
        iterations: Preferences.Key<Int>
    ): HashedSecret? {
        return HashedSecret(
            hash = this[hash] ?: return null,
            salt = this[salt] ?: return null,
            iterations = this[iterations] ?: return null
        )
    }

    private fun MutablePreferences.write(config: LockConfig) {
        // Keys from the earlier PIN-or-password version; dropped on the first write.
        LEGACY_KEYS.forEach { remove(it) }
        putOrRemove(SECRET_HASH, config.pin?.hash)
        putOrRemove(SECRET_SALT, config.pin?.salt)
        putOrRemove(SECRET_ITERATIONS, config.pin?.iterations)
        putOrRemove(RECOVERY_HASH, config.recovery?.hash)
        putOrRemove(RECOVERY_SALT, config.recovery?.salt)
        putOrRemove(RECOVERY_ITERATIONS, config.recovery?.iterations)
        this[BIOMETRIC] = config.biometricEnabled
        this[TIMEOUT] = config.timeout.storageKey
        this[FAILED_ATTEMPTS] = config.failedAttempts
        this[LOCKOUT_STARTED] = config.lockoutStartedElapsed
        this[LOCKOUT_DURATION] = config.lockoutDurationMs
    }

    private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        // Key names predate the PIN-only lock; kept so an existing lock stays readable.
        val SECRET_HASH = stringPreferencesKey("secret_hash")
        val SECRET_SALT = stringPreferencesKey("secret_salt")
        val SECRET_ITERATIONS = intPreferencesKey("secret_iterations")
        val RECOVERY_HASH = stringPreferencesKey("recovery_hash")
        val RECOVERY_SALT = stringPreferencesKey("recovery_salt")
        val RECOVERY_ITERATIONS = intPreferencesKey("recovery_iterations")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val TIMEOUT = stringPreferencesKey("auto_lock_timeout")
        val LEGACY_KEYS = listOf(
            stringPreferencesKey("lock_type"),
            stringPreferencesKey("recovery_method"),
            booleanPreferencesKey("hide_in_recents")
        )
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val LOCKOUT_STARTED = longPreferencesKey("lockout_started_elapsed")
        val LOCKOUT_DURATION = longPreferencesKey("lockout_duration_ms")
    }
}
