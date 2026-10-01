package com.droidnova.notificationhistory.presentation.screens.lock

import com.droidnova.notificationhistory.core.lock.PinRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NewPinStage { Enter, Confirm }

data class NewPinState(
    val stage: NewPinStage = NewPinStage.Enter,
    /** Bumped on every mismatch, so the UI can shake and clear even on a repeat mismatch. */
    val mismatchCount: Int = 0
) {
    val showMismatch: Boolean get() = mismatchCount > 0 && stage == NewPinStage.Enter
}

/**
 * "Enter a new PIN → enter it again", shared by setup, change and forgot-PIN reset.
 * The first entry lives only in memory (never in saved state) until it is confirmed.
 */
class NewPinController {
    private val _state = MutableStateFlow(NewPinState())
    val state: StateFlow<NewPinState> = _state.asStateFlow()

    private var firstEntry: String? = null

    fun reset() {
        firstEntry = null
        _state.value = NewPinState()
    }

    /** Returns the PIN once both entries match; null while the flow continues. */
    fun submit(pin: String): String? {
        if (!PinRules.isValid(pin)) return null
        val current = _state.value
        return when (current.stage) {
            NewPinStage.Enter -> {
                firstEntry = pin
                _state.value = current.copy(stage = NewPinStage.Confirm)
                null
            }
            NewPinStage.Confirm -> {
                val first = firstEntry
                firstEntry = null
                if (first == pin) {
                    pin
                } else {
                    // Start over: a mismatch usually means the first entry was the typo.
                    _state.value = NewPinState(mismatchCount = current.mismatchCount + 1)
                    null
                }
            }
        }
    }

    /** Steps back from confirm to enter; false when already at the first step. */
    fun back(): Boolean {
        if (_state.value.stage == NewPinStage.Enter) return false
        firstEntry = null
        _state.value = NewPinState()
        return true
    }
}
