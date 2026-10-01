package com.droidnova.notificationhistory.core.lock

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val LocalBiometricGate = staticCompositionLocalOf<BiometricGate?> { null }

/**
 * Fingerprint/face unlock and "confirm your phone's screen lock" (forgot-PIN recovery).
 * Must be created in `onCreate`: it registers an activity-result launcher.
 */
class BiometricGate(
    private val activity: FragmentActivity,
    private val onHandoff: () -> Unit
) {
    private var pending: ((Boolean) -> Unit)? = null

    /**
     * Set while a phone-screen-lock check is out. Saved with the activity: if Android recreates
     * it meanwhile (rotation, low memory), the result reaches this new instance, which has no
     * [pending] callback, so it is parked in [restoredDeviceCredentialResult] instead of lost.
     */
    private var deviceCredentialPending = false
    private val _restoredDeviceCredentialResult = MutableStateFlow<Boolean?>(null)
    val restoredDeviceCredentialResult: StateFlow<Boolean?> = _restoredDeviceCredentialResult.asStateFlow()

    init {
        val registry = activity.savedStateRegistry
        deviceCredentialPending =
            registry.consumeRestoredStateForKey(SAVED_STATE_KEY)?.getBoolean(KEY_PENDING) == true
        registry.registerSavedStateProvider(SAVED_STATE_KEY) {
            Bundle().apply { putBoolean(KEY_PENDING, deviceCredentialPending) }
        }
    }

    /** The lock screen took the parked result. */
    fun consumeRestoredDeviceCredentialResult() {
        _restoredDeviceCredentialResult.value = null
    }

    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                deliver(true)
            }

            // Cancel, "Use PIN", too many tries, hardware error. A single unrecognised finger
            // (onAuthenticationFailed) keeps the prompt open, so it isn't a result.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                deliver(false)
            }
        }
    )

    // API 26–29: BiometricPrompt can't ask for the screen lock alone, so use the keyguard screen.
    private val credentialLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> deliver(result.resultCode == Activity.RESULT_OK) }

    private val keyguard = activity.getSystemService(KeyguardManager::class.java)

    fun canUseBiometric(): Boolean =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /** False when the phone has no PIN, pattern or password; recovery via it is then hidden. */
    fun isDeviceSecure(): Boolean = keyguard?.isDeviceSecure == true

    fun authenticateBiometric(
        title: String,
        subtitle: String,
        negativeButton: String,
        onResult: (Boolean) -> Unit
    ) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButton)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        start(onResult) { prompt.authenticate(info) }
    }

    /** The phone's own unlock (PIN, pattern, password, or fingerprint where the system allows it). */
    fun confirmDeviceCredential(title: String, description: String, onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(description)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()
            start(onResult, deviceCredential = true) { prompt.authenticate(info) }
        } else {
            @Suppress("DEPRECATION")
            val intent = keyguard?.createConfirmDeviceCredentialIntent(title, description)
            if (intent == null) {
                onResult(false)
                return
            }
            start(onResult, deviceCredential = true) { credentialLauncher.launch(intent) }
        }
    }

    private fun start(
        onResult: (Boolean) -> Unit,
        deviceCredential: Boolean = false,
        launch: () -> Unit
    ) {
        pending = onResult
        deviceCredentialPending = deviceCredential
        onHandoff()
        runCatching(launch).onFailure { deliver(false) }
    }

    private fun deliver(success: Boolean) {
        val callback = pending
        val wasDeviceCredential = deviceCredentialPending
        pending = null
        deviceCredentialPending = false
        when {
            callback != null -> callback(success)
            // The requester was lost to activity recreation; park it for the lock screen.
            wasDeviceCredential -> _restoredDeviceCredentialResult.value = success
        }
    }

    private companion object {
        const val SAVED_STATE_KEY = "biometric_gate"
        const val KEY_PENDING = "device_credential_pending"
    }
}
