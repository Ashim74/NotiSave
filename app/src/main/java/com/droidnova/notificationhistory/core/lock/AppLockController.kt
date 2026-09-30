package com.droidnova.notificationhistory.core.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decides whether the UI is shown or the lock screen replaces it. Process-wide, so rotation
 * and activity recreation never re-lock; the activity reports start/stop (ignoring
 * configuration changes) and the store reports config. Main thread only.
 */
class AppLockController(private val elapsedRealtime: () -> Long) {

    enum class Gate {
        /** Lock config not read yet — the splash screen stays up. */
        Loading,
        Open,
        Locked
    }

    private val _gate = MutableStateFlow(Gate.Loading)
    val gate: StateFlow<Gate> = _gate.asStateFlow()

    private var enabled = false
    private var timeoutMs = 0L
    private var stoppedAt: Long? = null
    private var handoffAt: Long? = null

    fun onConfig(enabled: Boolean, timeoutMs: Long) {
        this.enabled = enabled
        this.timeoutMs = timeoutMs
        when {
            _gate.value == Gate.Loading -> _gate.value = if (enabled) Gate.Locked else Gate.Open
            // Turned off in Settings, or reset from the lock screen's erase option.
            !enabled -> _gate.value = Gate.Open
            // Turned on in Settings: the user is already inside, so stay open.
        }
    }

    fun onAppStopped() {
        if (_gate.value == Gate.Open) stoppedAt = elapsedRealtime()
    }

    fun onAppStarted() {
        val stopped = stoppedAt ?: return
        stoppedAt = null
        val handoff = handoffAt
        handoffAt = null
        if (!enabled || _gate.value != Gate.Open) return

        val now = elapsedRealtime()
        val returningFromHandoff = handoff != null && now - handoff <= HANDOFF_GRACE_MS
        if (returningFromHandoff || now - stopped < timeoutMs) return
        _gate.value = Gate.Locked
    }

    /**
     * The app itself is about to open another screen (system settings, share sheet, billing,
     * screen-lock check). Coming back from it within [HANDOFF_GRACE_MS] doesn't re-lock.
     */
    fun beginHandoff() {
        handoffAt = elapsedRealtime()
    }

    /** Resumed without being stopped (dialog-style hand-off, or the launch failed). */
    fun onAppResumed() {
        handoffAt = null
    }

    fun unlock() {
        if (_gate.value == Gate.Locked) _gate.value = Gate.Open
    }

    companion object {
        const val HANDOFF_GRACE_MS = 180_000L
    }
}
