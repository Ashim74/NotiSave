package com.droidnova.notificationhistory.presentation.screens.lock

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.notificationhistory.core.lock.AppLock
import com.droidnova.notificationhistory.core.lock.AutoLockTimeout
import com.droidnova.notificationhistory.core.lock.LockConfig
import com.droidnova.notificationhistory.core.lock.LockType
import com.droidnova.notificationhistory.core.lock.RecoveryMethod
import com.droidnova.notificationhistory.utils.Analytics
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** An action that needs the current PIN/password first. */
sealed interface ProtectedAction {
    data object Change : ProtectedAction
    data object TurnOff : ProtectedAction
    data object NewRecoveryCode : ProtectedAction
    data class SetRecoveryMethod(val method: RecoveryMethod) : ProtectedAction
}

sealed interface LockSettingsFlow {
    /** The settings list. */
    data object None : LockSettingsFlow
    data object SetupSecret : LockSettingsFlow
    /** Setup step 2: forgot-PIN options + optional biometric. */
    data object SetupRecovery : LockSettingsFlow
    data class VerifyCurrent(val action: ProtectedAction) : LockSettingsFlow
    data object ChangeSecret : LockSettingsFlow
    data class ShowRecoveryCode(val code: String, val afterSetup: Boolean) : LockSettingsFlow
}

enum class LockSettingsMessage { LockOn, LockChanged, LockOff, RecoveryMethodChanged }

data class AppLockSettingsState(
    val config: LockConfig = LockConfig(),
    val flow: LockSettingsFlow = LockSettingsFlow.None,
    val newSecret: NewSecretState = NewSecretState(),
    val feedback: VerifyFeedback = VerifyFeedback(),
    val isSaving: Boolean = false
)

class AppLockSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppLock.get(application).repository
    private val newSecret = NewSecretController()
    private val feedbackHolder = VerifyFeedbackHolder(viewModelScope, repository)
    private val flow = MutableStateFlow<LockSettingsFlow>(LockSettingsFlow.None)
    private val isSaving = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<LockSettingsMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<LockSettingsMessage> = _messages

    /** Setup step 1 result, held in memory only until the lock is written. */
    private var pendingSetup: Pair<LockType, String>? = null

    val state: StateFlow<AppLockSettingsState> = combine(
        repository.config,
        flow,
        newSecret.state,
        feedbackHolder.feedback,
        isSaving
    ) { config, flow, newSecret, feedback, saving ->
        AppLockSettingsState(config, flow, newSecret, feedback, saving)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppLockSettingsState())

    fun startSetup() {
        newSecret.reset()
        flow.value = LockSettingsFlow.SetupSecret
    }

    fun startProtected(action: ProtectedAction) {
        feedbackHolder.clear()
        flow.value = LockSettingsFlow.VerifyCurrent(action)
    }

    fun chooseNewSecretType(type: LockType) = newSecret.chooseType(type)

    fun submitNewSecret(input: String) {
        val result = newSecret.submit(input) ?: return
        when (flow.value) {
            LockSettingsFlow.SetupSecret -> {
                pendingSetup = result
                flow.value = LockSettingsFlow.SetupRecovery
            }
            LockSettingsFlow.ChangeSecret -> save {
                repository.changeSecret(result.first, result.second)
                Analytics.log(Analytics.APP_LOCK_CHANGED, typeParams(result.first))
                finish(LockSettingsMessage.LockChanged)
            }
            else -> Unit
        }
    }

    fun completeSetup(method: RecoveryMethod, useBiometric: Boolean) {
        val (type, secret) = pendingSetup ?: return startSetup()
        save {
            val code = repository.enable(type, secret, method, useBiometric)
            pendingSetup = null
            Analytics.log(
                Analytics.APP_LOCK_ENABLED,
                typeParams(type).apply {
                    putString(Analytics.PARAM_METHOD, method.storageKey)
                    putString(Analytics.PARAM_BIOMETRIC, useBiometric.toString())
                }
            )
            flow.value = LockSettingsFlow.ShowRecoveryCode(code, afterSetup = true)
        }
    }

    fun submitCurrentSecret(secret: String) {
        val action = (flow.value as? LockSettingsFlow.VerifyCurrent)?.action ?: return
        feedbackHolder.verify(
            check = { repository.verifySecret(secret) },
            onSuccess = { perform(action) }
        )
    }

    /** After the recovery code was shown (setup or regenerate). */
    fun finishRecoveryCode() {
        val afterSetup = (flow.value as? LockSettingsFlow.ShowRecoveryCode)?.afterSetup == true
        flow.value = LockSettingsFlow.None
        if (afterSetup) _messages.tryEmit(LockSettingsMessage.LockOn)
    }

    fun setTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch { repository.setTimeout(timeout) }
    }

    fun setHideInRecents(hide: Boolean) {
        viewModelScope.launch { repository.setHideInRecents(hide) }
    }

    /** Call only after a successful biometric prompt when enabling. */
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBiometricEnabled(enabled) }
    }

    /** Returns false when the settings list is showing (the screen should pop). */
    fun back(): Boolean {
        when (flow.value) {
            LockSettingsFlow.None -> return false
            LockSettingsFlow.SetupSecret, LockSettingsFlow.ChangeSecret ->
                if (!newSecret.back()) cancelFlow()
            LockSettingsFlow.SetupRecovery -> {
                pendingSetup = null
                newSecret.reset()
                flow.value = LockSettingsFlow.SetupSecret
            }
            is LockSettingsFlow.VerifyCurrent -> cancelFlow()
            // The code is already active; it must be acknowledged before leaving.
            is LockSettingsFlow.ShowRecoveryCode -> Unit
        }
        return true
    }

    private fun cancelFlow() {
        pendingSetup = null
        newSecret.reset()
        flow.value = LockSettingsFlow.None
    }

    private suspend fun perform(action: ProtectedAction) {
        when (action) {
            ProtectedAction.Change -> {
                newSecret.reset(state.value.config.type ?: LockType.Pin)
                flow.value = LockSettingsFlow.ChangeSecret
            }
            ProtectedAction.TurnOff -> {
                repository.disable()
                Analytics.log(Analytics.APP_LOCK_DISABLED)
                finish(LockSettingsMessage.LockOff)
            }
            ProtectedAction.NewRecoveryCode -> {
                val code = repository.regenerateRecoveryCode()
                flow.value = LockSettingsFlow.ShowRecoveryCode(code, afterSetup = false)
            }
            is ProtectedAction.SetRecoveryMethod -> {
                repository.setRecoveryMethod(action.method)
                finish(LockSettingsMessage.RecoveryMethodChanged)
            }
        }
    }

    private fun finish(message: LockSettingsMessage) {
        newSecret.reset()
        flow.value = LockSettingsFlow.None
        _messages.tryEmit(message)
    }

    private fun save(block: suspend () -> Unit) {
        if (isSaving.value) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                isSaving.value = false
            }
        }
    }

    private fun typeParams(type: LockType) =
        Bundle().apply { putString(Analytics.PARAM_TYPE, type.storageKey) }
}
