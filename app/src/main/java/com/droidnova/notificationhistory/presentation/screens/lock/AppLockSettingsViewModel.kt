package com.droidnova.notificationhistory.presentation.screens.lock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.notificationhistory.core.lock.AppLock
import com.droidnova.notificationhistory.core.lock.AutoLockTimeout
import com.droidnova.notificationhistory.core.lock.LockConfig
import com.droidnova.notificationhistory.utils.Analytics
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** An action that needs the current PIN first. */
enum class ProtectedAction { Change, NewRecoveryCode }

sealed interface LockSettingsFlow {
    /** The settings list. */
    data object None : LockSettingsFlow
    data object SetupPin : LockSettingsFlow
    data class VerifyCurrent(val action: ProtectedAction) : LockSettingsFlow
    data object ChangePin : LockSettingsFlow
    data class ShowRecoveryCode(val code: String, val afterSetup: Boolean) : LockSettingsFlow
}

enum class LockSettingsMessage { LockOn, PinChanged, LockOff }

data class AppLockSettingsState(
    val config: LockConfig = LockConfig(),
    val flow: LockSettingsFlow = LockSettingsFlow.None,
    val newPin: NewPinState = NewPinState(),
    val feedback: VerifyFeedback = VerifyFeedback(),
    val isSaving: Boolean = false
)

/**
 * Turn on: new PIN → confirm → recovery code. Change PIN and new recovery code ask for the
 * current PIN first; turning the lock off only needs a confirmation.
 */
class AppLockSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppLock.get(application).repository
    private val newPin = NewPinController()
    private val feedbackHolder = VerifyFeedbackHolder(viewModelScope, repository)
    private val flow = MutableStateFlow<LockSettingsFlow>(LockSettingsFlow.None)
    private val isSaving = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<LockSettingsMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<LockSettingsMessage> = _messages

    val state: StateFlow<AppLockSettingsState> = combine(
        repository.config,
        flow,
        newPin.state,
        feedbackHolder.feedback,
        isSaving
    ) { config, flow, newPin, feedback, saving ->
        AppLockSettingsState(config, flow, newPin, feedback, saving)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppLockSettingsState())

    fun startSetup() {
        newPin.reset()
        flow.value = LockSettingsFlow.SetupPin
    }

    fun startProtected(action: ProtectedAction) {
        feedbackHolder.clear()
        flow.value = LockSettingsFlow.VerifyCurrent(action)
    }

    fun submitNewPin(input: String) {
        if (isSaving.value) return
        val pin = newPin.submit(input) ?: return
        when (flow.value) {
            LockSettingsFlow.SetupPin -> save {
                val code = repository.enable(pin)
                Analytics.log(Analytics.APP_LOCK_ENABLED)
                flow.value = LockSettingsFlow.ShowRecoveryCode(code, afterSetup = true)
            }
            LockSettingsFlow.ChangePin -> save {
                repository.changePin(pin)
                Analytics.log(Analytics.APP_LOCK_CHANGED)
                finish(LockSettingsMessage.PinChanged)
            }
            else -> Unit
        }
    }

    fun submitCurrentPin(pin: String) {
        val action = (flow.value as? LockSettingsFlow.VerifyCurrent)?.action ?: return
        feedbackHolder.verify(
            check = { repository.verifyPin(pin) },
            onSuccess = { perform(action) }
        )
    }

    /** After the recovery code was shown (setup or regenerate). */
    fun finishRecoveryCode() {
        val afterSetup = (flow.value as? LockSettingsFlow.ShowRecoveryCode)?.afterSetup == true
        newPin.reset()
        flow.value = LockSettingsFlow.None
        if (afterSetup) _messages.tryEmit(LockSettingsMessage.LockOn)
    }

    fun setTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch { repository.setTimeout(timeout) }
    }

    /** Call only after a successful fingerprint prompt when enabling. */
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBiometricEnabled(enabled) }
    }

    /** Returns false when the settings list is showing (the screen should pop). */
    fun back(): Boolean {
        when (flow.value) {
            LockSettingsFlow.None -> return false
            LockSettingsFlow.SetupPin, LockSettingsFlow.ChangePin -> if (!newPin.back()) cancelFlow()
            is LockSettingsFlow.VerifyCurrent -> cancelFlow()
            // The code is already active; it must be acknowledged before leaving.
            is LockSettingsFlow.ShowRecoveryCode -> Unit
        }
        return true
    }

    private fun cancelFlow() {
        newPin.reset()
        flow.value = LockSettingsFlow.None
    }

    private suspend fun perform(action: ProtectedAction) {
        when (action) {
            ProtectedAction.Change -> {
                newPin.reset()
                flow.value = LockSettingsFlow.ChangePin
            }
            ProtectedAction.NewRecoveryCode -> {
                val code = repository.regenerateRecoveryCode()
                flow.value = LockSettingsFlow.ShowRecoveryCode(code, afterSetup = false)
            }
        }
    }

    /**
     * No PIN needed: the user is already inside the unlocked app, and turning the lock off only
     * removes protection; it reveals nothing that isn't already on screen.
     */
    fun turnOff() {
        if (isSaving.value) return
        save {
            repository.disable()
            Analytics.log(Analytics.APP_LOCK_DISABLED)
            finish(LockSettingsMessage.LockOff)
        }
    }

    private fun finish(message: LockSettingsMessage) {
        newPin.reset()
        flow.value = LockSettingsFlow.None
        _messages.tryEmit(message)
    }

    private fun save(block: suspend () -> Unit) {
        isSaving.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                isSaving.value = false
            }
        }
    }
}
