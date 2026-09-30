package com.droidnova.notificationhistory.presentation.screens.lock

import com.droidnova.notificationhistory.core.lock.LockType
import com.droidnova.notificationhistory.core.lock.SecretError
import com.droidnova.notificationhistory.core.lock.SecretRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NewSecretStage { ChooseType, Enter, Confirm }

enum class NewSecretError { TooShort, TooLong, DigitsOnly, Mismatch }

data class NewSecretState(
    val stage: NewSecretStage = NewSecretStage.ChooseType,
    val type: LockType = LockType.Pin,
    val error: NewSecretError? = null
)

/**
 * "Choose PIN or password → enter → confirm", shared by setup, change and forgot-PIN reset.
 * The first entry lives only in memory (never in saved state) until it is confirmed.
 */
class NewSecretController {
    private val _state = MutableStateFlow(NewSecretState())
    val state: StateFlow<NewSecretState> = _state.asStateFlow()

    private var firstEntry: String? = null

    fun reset(type: LockType = LockType.Pin) {
        firstEntry = null
        _state.value = NewSecretState(type = type)
    }

    fun chooseType(type: LockType) {
        firstEntry = null
        _state.value = NewSecretState(stage = NewSecretStage.Enter, type = type)
    }

    /** Returns the confirmed secret once both entries match; null while the flow continues. */
    fun submit(input: String): Pair<LockType, String>? {
        val current = _state.value
        when (current.stage) {
            NewSecretStage.ChooseType -> return null
            NewSecretStage.Enter -> {
                val error = SecretRules.validate(current.type, input)
                if (error != null) {
                    _state.value = current.copy(error = error.toFlowError())
                    return null
                }
                firstEntry = input
                _state.value = current.copy(stage = NewSecretStage.Confirm, error = null)
                return null
            }
            NewSecretStage.Confirm -> {
                val first = firstEntry
                if (first == null || first != input) {
                    // Start over: a mismatch usually means the first entry was the typo.
                    firstEntry = null
                    _state.value = current.copy(
                        stage = NewSecretStage.Enter,
                        error = NewSecretError.Mismatch
                    )
                    return null
                }
                firstEntry = null
                return current.type to input
            }
        }
    }

    /** Steps back; false when already at the first step (the caller then leaves the flow). */
    fun back(): Boolean {
        val current = _state.value
        firstEntry = null
        _state.value = when (current.stage) {
            NewSecretStage.ChooseType -> return false
            NewSecretStage.Enter -> current.copy(stage = NewSecretStage.ChooseType, error = null)
            NewSecretStage.Confirm -> current.copy(stage = NewSecretStage.Enter, error = null)
        }
        return true
    }

    private fun SecretError.toFlowError() = when (this) {
        SecretError.TooShort -> NewSecretError.TooShort
        SecretError.TooLong -> NewSecretError.TooLong
        SecretError.DigitsOnly -> NewSecretError.DigitsOnly
    }
}
