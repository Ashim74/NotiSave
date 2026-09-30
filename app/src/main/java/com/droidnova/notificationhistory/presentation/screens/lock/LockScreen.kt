package com.droidnova.notificationhistory.presentation.screens.lock

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.core.lock.AppLockController
import com.droidnova.notificationhistory.core.lock.LocalBiometricGate
import com.droidnova.notificationhistory.core.lock.LockType
import com.droidnova.notificationhistory.core.lock.RecoveryCodes
import com.droidnova.notificationhistory.core.lock.RecoveryMethod
import kotlinx.coroutines.delay

private const val APP_CONTENT_KEY = "app_content"
private const val ERASE_DELAY_SECONDS = 10

/**
 * The lock screen *replaces* the app rather than covering it: while locked, history is never
 * composed, so screenshots, TalkBack and the Recents thumbnail can't reach it. The saveable
 * state holder keeps the app's navigation and scroll state across a lock/unlock.
 */
@Composable
fun AppLockGate(
    gate: AppLockController.Gate,
    lockContent: @Composable () -> Unit,
    appContent: @Composable () -> Unit
) {
    val holder = rememberSaveableStateHolder()
    when (gate) {
        AppLockController.Gate.Loading -> Unit
        AppLockController.Gate.Locked -> lockContent()
        AppLockController.Gate.Open -> holder.SaveableStateProvider(APP_CONTENT_KEY) { appContent() }
    }
}

@Composable
fun LockScreen(onHistoryErased: () -> Unit, viewModel: LockScreenViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as? Activity
    val biometricGate = LocalBiometricGate.current

    // Back never skips the lock: it steps back through the flow, then leaves the app.
    BackHandler { if (!viewModel.back()) activity?.moveTaskToBack(true) }

    val biometricAvailable = remember(state.biometricEnabled) {
        state.biometricEnabled && biometricGate?.canUseBiometric() == true
    }
    val deviceRecoveryAvailable = remember(state.recoveryMethod) {
        state.recoveryMethod == RecoveryMethod.DeviceAndCode && biometricGate?.isDeviceSecure() == true
    }

    val biometricTitle = stringResource(R.string.lock_biometric_prompt_title)
    val biometricSubtitle = stringResource(R.string.lock_biometric_prompt_subtitle)
    val cancel = stringResource(R.string.cancel)
    val deviceTitle = stringResource(R.string.lock_device_confirm_title)
    val deviceDescription = stringResource(R.string.lock_device_confirm_desc)
    val promptBiometric: () -> Unit = {
        biometricGate?.authenticateBiometric(biometricTitle, biometricSubtitle, cancel) { success ->
            if (success) viewModel.onBiometricSuccess()
        }
    }

    // Offer fingerprint straight away, once per lock (this screen leaves composition on unlock).
    var autoPrompted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable && !autoPrompted && state.stage == LockStage.Unlock) {
            autoPrompted = true
            promptBiometric()
        }
    }

    LockScreenContent(
        state = state,
        biometricAvailable = biometricAvailable,
        deviceRecoveryAvailable = deviceRecoveryAvailable,
        actions = LockScreenActions(
            onSubmitSecret = viewModel::submitSecret,
            onBiometric = promptBiometric,
            onForgot = viewModel::openForgotOptions,
            onUseDeviceCredential = {
                biometricGate?.confirmDeviceCredential(deviceTitle, deviceDescription) { success ->
                    viewModel.onDeviceCredentialResult(success)
                }
            },
            onUseRecoveryCode = viewModel::openRecoveryCode,
            onSubmitRecoveryCode = viewModel::submitRecoveryCode,
            onOpenErase = viewModel::openEraseConfirm,
            onConfirmErase = { viewModel.eraseHistoryAndReset(onHistoryErased) },
            onChooseNewType = viewModel::chooseNewSecretType,
            onSubmitNewSecret = viewModel::submitNewSecret,
            onFinishRecovery = viewModel::finishRecovery,
            onBack = { viewModel.back() }
        )
    )
}

class LockScreenActions(
    val onSubmitSecret: (String) -> Unit = {},
    val onBiometric: () -> Unit = {},
    val onForgot: () -> Unit = {},
    val onUseDeviceCredential: () -> Unit = {},
    val onUseRecoveryCode: () -> Unit = {},
    val onSubmitRecoveryCode: (String) -> Unit = {},
    val onOpenErase: () -> Unit = {},
    val onConfirmErase: () -> Unit = {},
    val onChooseNewType: (LockType) -> Unit = {},
    val onSubmitNewSecret: (String) -> Unit = {},
    val onFinishRecovery: () -> Unit = {},
    val onBack: () -> Unit = {}
)

@Composable
fun LockScreenContent(
    state: LockScreenState,
    biometricAvailable: Boolean,
    deviceRecoveryAvailable: Boolean,
    actions: LockScreenActions,
    eraseDelaySeconds: Int = ERASE_DELAY_SECONDS
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val stage = state.stage) {
                    LockStage.Unlock -> UnlockStage(state, biometricAvailable, actions)
                    LockStage.ForgotOptions -> ForgotOptionsStage(state, deviceRecoveryAvailable, actions)
                    LockStage.RecoveryCode -> RecoveryCodeStage(state, actions)
                    LockStage.NewSecret -> {
                        FlowBackButton(onBack = actions.onBack)
                        NewSecretContent(
                            state = state.newSecret,
                            title = stringResource(R.string.lock_new_secret_title_reset),
                            onChooseType = actions.onChooseNewType,
                            onSubmit = actions.onSubmitNewSecret
                        )
                    }
                    is LockStage.ShowRecoveryCode ->
                        RecoveryCodeContent(code = stage.code, onDone = actions.onFinishRecovery)
                    LockStage.EraseConfirm -> EraseStage(eraseDelaySeconds, actions)
                }
            }
        }
    }
}

@Composable
private fun UnlockStage(state: LockScreenState, biometricAvailable: Boolean, actions: LockScreenActions) {
    val isPin = state.lockType == LockType.Pin
    var input by remember { mutableStateOf("") }
    val feedback = state.feedback
    // A wrong entry or a lockout empties the field for the next try.
    LaunchedEffect(feedback.attemptsLeft, feedback.isLockedOut) {
        if (feedback.attemptsLeft != null || feedback.isLockedOut) input = ""
    }

    LockHeader(
        title = stringResource(if (isPin) R.string.lock_screen_title_pin else R.string.lock_screen_title_password),
        subtitle = stringResource(R.string.lock_screen_subtitle)
    )
    Spacer(Modifier.height(12.dp))
    VerifyFeedbackText(feedback)
    Spacer(Modifier.height(12.dp))
    SecretEntry(
        type = state.lockType,
        value = input,
        onValueChange = { input = it },
        onSubmit = { actions.onSubmitSecret(input) },
        enabled = !feedback.isChecking && !feedback.isLockedOut,
        isError = feedback.attemptsLeft != null
    )
    Spacer(Modifier.height(16.dp))
    if (biometricAvailable) {
        TextButton(onClick = actions.onBiometric) {
            Text(stringResource(R.string.lock_unlock_biometric))
        }
    }
    TextButton(onClick = actions.onForgot) {
        Text(stringResource(if (isPin) R.string.lock_forgot_pin else R.string.lock_forgot_password))
    }
}

@Composable
private fun ForgotOptionsStage(
    state: LockScreenState,
    deviceRecoveryAvailable: Boolean,
    actions: LockScreenActions
) {
    FlowBackButton(onBack = actions.onBack)
    LockHeader(
        title = stringResource(R.string.lock_forgot_title),
        subtitle = stringResource(R.string.lock_forgot_subtitle)
    )
    Spacer(Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (deviceRecoveryAvailable) {
            OptionCard(
                title = stringResource(R.string.lock_recover_device_title),
                description = stringResource(R.string.lock_recover_device_desc),
                onClick = actions.onUseDeviceCredential
            )
            if (state.deviceCheckFailed) {
                Text(
                    stringResource(R.string.lock_device_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        OptionCard(
            title = stringResource(R.string.lock_recover_code_title),
            description = stringResource(R.string.lock_recover_code_desc),
            onClick = actions.onUseRecoveryCode
        )
        OptionCard(
            title = stringResource(R.string.lock_recover_erase_title),
            description = stringResource(R.string.lock_recover_erase_desc),
            onClick = actions.onOpenErase,
            isDestructive = true
        )
    }
}

@Composable
private fun RecoveryCodeStage(state: LockScreenState, actions: LockScreenActions) {
    var code by remember { mutableStateOf("") }
    val feedback = state.feedback
    LaunchedEffect(feedback.attemptsLeft, feedback.isLockedOut) {
        if (feedback.attemptsLeft != null || feedback.isLockedOut) code = ""
    }
    val complete = RecoveryCodes.normalize(code).length == RecoveryCodes.LENGTH

    FlowBackButton(onBack = actions.onBack)
    LockHeader(
        title = stringResource(R.string.lock_recover_code_title),
        subtitle = stringResource(R.string.lock_recover_code_desc)
    )
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
        value = code,
        onValueChange = { if (it.length <= RECOVERY_INPUT_MAX) code = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !feedback.isChecking && !feedback.isLockedOut,
        isError = feedback.attemptsLeft != null,
        label = { Text(stringResource(R.string.lock_recovery_code_label)) },
        placeholder = { Text(stringResource(R.string.lock_recovery_code_hint)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false
        )
    )
    Spacer(Modifier.height(8.dp))
    VerifyFeedbackText(feedback)
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { actions.onSubmitRecoveryCode(code) },
        enabled = complete && !feedback.isChecking && !feedback.isLockedOut,
        modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.lock_continue)) }
}

private const val RECOVERY_INPUT_MAX = 20

@Composable
private fun EraseStage(eraseDelaySeconds: Int, actions: LockScreenActions) {
    val eraseWord = stringResource(R.string.lock_erase_word)
    var typed by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableIntStateOf(eraseDelaySeconds) }
    var erasing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
    }
    val confirmed = typed.trim().equals(eraseWord, ignoreCase = true)

    FlowBackButton(onBack = actions.onBack)
    LockHeader(
        title = stringResource(R.string.lock_erase_title),
        subtitle = stringResource(R.string.lock_erase_body)
    )
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
        value = typed,
        onValueChange = { typed = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.lock_erase_type_hint, eraseWord)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            erasing = true
            actions.onConfirmErase()
        },
        enabled = confirmed && secondsLeft == 0 && !erasing,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (secondsLeft > 0) {
                stringResource(R.string.lock_erase_button_wait, secondsLeft)
            } else {
                stringResource(R.string.lock_erase_button)
            },
            textAlign = TextAlign.Center
        )
    }
}
