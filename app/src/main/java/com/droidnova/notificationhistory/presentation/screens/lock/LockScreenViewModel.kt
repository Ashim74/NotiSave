package com.droidnova.notificationhistory.presentation.screens.lock

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.notificationhistory.core.lock.AppLock
import com.droidnova.notificationhistory.core.lock.LockType
import com.droidnova.notificationhistory.core.lock.RecoveryMethod
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.utils.Analytics
import com.droidnova.notificationhistory.utils.CrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LockStage {
    data object Unlock : LockStage
    data object ForgotOptions : LockStage
    data object RecoveryCode : LockStage
    data object NewSecret : LockStage
    data class ShowRecoveryCode(val code: String) : LockStage
    data object EraseConfirm : LockStage
}

data class LockScreenState(
    val stage: LockStage = LockStage.Unlock,
    val lockType: LockType = LockType.Pin,
    val biometricEnabled: Boolean = false,
    val recoveryMethod: RecoveryMethod = RecoveryMethod.DeviceAndCode,
    val feedback: VerifyFeedback = VerifyFeedback(),
    val newSecret: NewSecretState = NewSecretState(),
    val deviceCheckFailed: Boolean = false
)

class LockScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val appLock = AppLock.get(application)
    private val repository = appLock.repository
    private val newSecret = NewSecretController()
    private val feedbackHolder = VerifyFeedbackHolder(viewModelScope, repository)
    private val stage = MutableStateFlow<LockStage>(LockStage.Unlock)
    private val deviceCheckFailed = MutableStateFlow(false)
    private var recoveredWith: String? = null
    private var savingNewSecret = false

    val state: StateFlow<LockScreenState> = combine(
        repository.config,
        stage,
        feedbackHolder.feedback,
        newSecret.state,
        deviceCheckFailed
    ) { config, stage, feedback, newSecret, deviceFailed ->
        LockScreenState(
            stage = stage,
            lockType = config.type ?: LockType.Pin,
            biometricEnabled = config.biometricEnabled,
            recoveryMethod = config.recoveryMethod,
            feedback = feedback,
            newSecret = newSecret,
            deviceCheckFailed = deviceFailed
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LockScreenState())

    init {
        feedbackHolder.refreshLockout()
    }

    fun submitSecret(secret: String) {
        feedbackHolder.verify(
            check = { repository.verifySecret(secret) },
            onSuccess = { unlock() }
        )
    }

    fun onBiometricSuccess() = unlock()

    fun openForgotOptions() {
        deviceCheckFailed.value = false
        stage.value = LockStage.ForgotOptions
    }

    fun openRecoveryCode() {
        feedbackHolder.clear()
        stage.value = LockStage.RecoveryCode
    }

    fun openEraseConfirm() {
        stage.value = LockStage.EraseConfirm
    }

    fun onDeviceCredentialResult(success: Boolean) {
        if (success) {
            startNewSecret(method = METHOD_DEVICE)
        } else {
            deviceCheckFailed.value = true
        }
    }

    fun submitRecoveryCode(code: String) {
        feedbackHolder.verify(
            check = { repository.verifyRecoveryCode(code) },
            onSuccess = { startNewSecret(method = METHOD_CODE) }
        )
    }

    fun chooseNewSecretType(type: LockType) = newSecret.chooseType(type)

    fun submitNewSecret(input: String) {
        if (savingNewSecret) return
        val (type, secret) = newSecret.submit(input) ?: return
        savingNewSecret = true
        viewModelScope.launch {
            val code = repository.resetAfterRecovery(type, secret)
            Analytics.log(
                Analytics.APP_LOCK_RECOVERED,
                Bundle().apply { putString(Analytics.PARAM_METHOD, recoveredWith) }
            )
            stage.value = LockStage.ShowRecoveryCode(code)
            savingNewSecret = false
        }
    }

    /** The user saved the new recovery code; recovery is complete. */
    fun finishRecovery() = unlock()

    /**
     * Last resort: permanently deletes history + trash, then removes the lock (which opens the
     * gate). [onHistoryErased] lets the activity drop its cached history before the UI returns.
     */
    fun eraseHistoryAndReset(onHistoryErased: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(getApplication()).notificationDao().deleteAllNotifications()
                }
            }.onFailure(CrashReporter::record)
            onHistoryErased()
            Analytics.log(Analytics.APP_LOCK_ERASE_RESET)
            resetUi()
            repository.disable()
        }
    }

    /** Returns false at the first screen, where back should leave the app instead. */
    fun back(): Boolean {
        when (stage.value) {
            LockStage.Unlock -> return false
            LockStage.ForgotOptions -> {
                feedbackHolder.clear()
                stage.value = LockStage.Unlock
            }
            LockStage.RecoveryCode, LockStage.EraseConfirm -> openForgotOptions()
            // Identity is already proven; backing out would force the user to prove it again.
            LockStage.NewSecret -> newSecret.back()
            // The new code is already active; leaving without it would strand the user.
            is LockStage.ShowRecoveryCode -> Unit
        }
        return true
    }

    private fun startNewSecret(method: String) {
        recoveredWith = method
        newSecret.reset(state.value.lockType)
        stage.value = LockStage.NewSecret
    }

    private fun unlock() {
        resetUi()
        appLock.controller.unlock()
    }

    private fun resetUi() {
        stage.value = LockStage.Unlock
        deviceCheckFailed.value = false
        recoveredWith = null
        newSecret.reset()
        feedbackHolder.clear()
    }

    private companion object {
        const val METHOD_DEVICE = "device_credential"
        const val METHOD_CODE = "recovery_code"
    }
}
