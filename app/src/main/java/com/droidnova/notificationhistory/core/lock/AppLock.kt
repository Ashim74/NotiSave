package com.droidnova.notificationhistory.core.lock

import android.content.Context
import android.os.SystemClock
import com.droidnova.notificationhistory.utils.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Process-wide wiring of the app lock: one store, repository and controller. */
class AppLock private constructor(context: Context) {

    val repository = AppLockRepository(
        store = DataStoreAppLockStore(context),
        elapsedRealtime = SystemClock::elapsedRealtime
    )
    val controller = AppLockController(SystemClock::elapsedRealtime)

    val isEnabled: Flow<Boolean> = repository.config.map { it.isEnabled }.distinctUntilChanged()

    /** FLAG_SECURE: blank Recents thumbnail, no screenshots or screen recording. */
    val secureWindow: Flow<Boolean> = repository.config
        .map { it.isEnabled && it.hideInRecents }
        .distinctUntilChanged()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            repository.config
                .map { it.isEnabled to it.timeout.millis }
                .distinctUntilChanged()
                .collect { (enabled, timeoutMs) ->
                    controller.onConfig(enabled, timeoutMs)
                    CrashReporter.setCustomKey(CRASH_KEY_LOCK_ON, enabled)
                }
        }
    }

    companion object {
        private const val CRASH_KEY_LOCK_ON = "app_lock_on"

        @Volatile private var instance: AppLock? = null

        fun get(context: Context): AppLock = instance ?: synchronized(this) {
            instance ?: AppLock(context.applicationContext).also { instance = it }
        }
    }
}
