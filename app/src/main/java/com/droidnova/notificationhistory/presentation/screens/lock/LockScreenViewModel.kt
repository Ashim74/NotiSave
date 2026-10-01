package com.droidnova.notificationhistory.presentation.screens.lock

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.notificationhistory.core.lock.AppLock
import com.droidnova.notificationhistory.utils.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LockStage {
    data object Unlock : LockStage
    data object Forgot : LockStage
    data object RecoveryCode : LockStage
    data object NewPin : LockStage
}

data class LockScreenState(
    val stage: LockStage = LockStage.Unlock,
    val biometricEnabled: Boolean = false,
    val feedback: VerifyFeedback = VerifyFeedback(),
    val newPin: NewPinState = NewPinState(),
    val deviceCheckFailed: Boolean = false,
    val isSaving: Boolean = false
)

/**
 * Unlock with the PIN (or fingerprint, if turned on). Forgot PIN: confirm the phone's screen lock
 * or enter the recovery code, then set a new PIN.
 */
class LockScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val appLock = AppLock.get(application)
    private val repository = appLock.repository
    private val newPin = NewPinController()
    private val feedbackHolder = VerifyFeedbackHolder(viewModelScope, repository)
    private val stage = MutableStateFlow<LockStage>(LockStage.Unlock)
    private val deviceCheckFailed = MutableStateFlow(false)
    private val isSaving = MutableStateFlow(false)
    /** How identity was proven before [LockStage.NewPin]; a spent recovery code is replaced. */
    private var recoveredWith: String? = null

    val state: StateFlow<LockScreenState> = combine(
        repository.config,
        stage,
        feedbackHolder.feedback,
        newPin.state,
        combine(deviceCheckFailed, isSaving, ::Pair)
    ) { config, stage, feedback, newPin, (deviceFailed, saving) ->
        LockScreenState(
            stage = stage,
            biometricEnabled = config.biometricEnabled,
            feedback = feedback,
            newPin = newPin,
            deviceCheckFailed = deviceFailed,
            isSaving = saving
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LockScreenState())

    init {
        feedbackHolder.refreshLockout()
        // While the user types, so the PIN check itself runs at full speed.
        viewModelScope.launch { repository.warmUp() }
    }

    fun submitPin(pin: String) {
        feedbackHolder.verify(
            check = { repository.verifyPin(pin) },
            onSuccess = { unlock() }
        )
    }

    fun onBiometricSuccess() = unlock()

    fun openForgot() {
        deviceCheckFailed.value = false
        stage.value = LockStage.Forgot
    }

    fun openRecoveryCode() {
        feedbackHolder.clear()
        stage.value = LockStage.RecoveryCode
    }

    /** The phone's screen lock is the escape hatch, so it works even during a lockout. */
    fun onDeviceCredentialResult(success: Boolean) {
        if (success) startNewPin(METHOD_DEVICE) else deviceCheckFailed.value = true
    }

    fun submitRecoveryCode(code: String) {
        feedbackHolder.verify(
            check = { repository.verifyRecoveryCode(code) },
            onSuccess = { startNewPin(METHOD_CODE) }
        )
    }

    fun submitNewPin(input: String) {
        if (isSaving.value) return
        val pin = newPin.submit(input) ?: return
        val method = recoveredWith
        isSaving.value = true
        viewModelScope.launch {
            try {
                Analytics.log(
                    Analytics.APP_LOCK_RECOVERED,
                    Bundle().apply { putString(Analytics.PARAM_METHOD, method) }
                )
                // Same for both ways in: new PIN, then straight into the app.
                repository.changePin(pin)
                unlock()
            } finally {
                isSaving.value = false
            }
        }
    }

    /** Returns false at the first screen, where back should leave the app instead. */
    fun back(): Boolean {
        when (stage.value) {
            LockStage.Unlock -> return false
            LockStage.Forgot -> {
                feedbackHolder.clear()
                stage.value = LockStage.Unlock
            }
            LockStage.RecoveryCode -> openForgot()
            // Identity is already proven; leaving would force the user to prove it again.
            LockStage.NewPin -> newPin.back()
        }
        return true
    }

    private fun startNewPin(method: String) {
        recoveredWith = method
        newPin.reset()
        stage.value = LockStage.NewPin
    }

    private fun unlock() {
        stage.value = LockStage.Unlock
        deviceCheckFailed.value = false
        recoveredWith = null
        newPin.reset()
        feedbackHolder.clear()
        appLock.controller.unlock()
    }

    private companion object {
        const val METHOD_DEVICE = "device_credential"
        const val METHOD_CODE = "recovery_code"
    }
}
