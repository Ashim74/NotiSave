package com.droidnova.notificationhistory.presentation.screens.lock

import android.app.Activity
import androidx.compose.material.icons.outlined.Key
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.core.lock.AppLockController
import com.droidnova.notificationhistory.core.lock.LocalBiometricGate
import com.droidnova.notificationhistory.core.lock.RecoveryCodes
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

private const val APP_CONTENT_KEY = "app_content"

/** Older 12-character codes from before the PIN-only lock are still accepted. */
private const val RECOVERY_INPUT_MAX = 20

/**
 * The lock screen *replaces* the app rather than covering it: while locked, history is never
 * composed, so screenshots and TalkBack can't reach it. The saveable state holder keeps the
 * app's navigation and scroll state across a lock/unlock.
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
fun LockScreen(viewModel: LockScreenViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as? Activity
    val biometricGate = LocalBiometricGate.current

    // Back never skips the lock: it steps back through the flow, then leaves the app.
    BackHandler { if (!viewModel.back()) activity?.moveTaskToBack(true) }

    val biometricAvailable = remember(state.biometricEnabled) {
        state.biometricEnabled && biometricGate?.canUseBiometric() == true
    }
    val deviceLockAvailable = remember { biometricGate?.isDeviceSecure() == true }

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

    // A phone-screen-lock result that arrived after Android recreated the activity.
    val restoredDeviceResult = biometricGate?.restoredDeviceCredentialResult?.collectAsState()?.value
    LaunchedEffect(restoredDeviceResult) {
        if (restoredDeviceResult != null) {
            biometricGate?.consumeRestoredDeviceCredentialResult()
            viewModel.onDeviceCredentialResult(restoredDeviceResult)
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
        deviceLockAvailable = deviceLockAvailable,
        actions = LockScreenActions(
            onSubmitPin = viewModel::submitPin,
            onBiometric = promptBiometric,
            onForgot = viewModel::openForgot,
            onUseDeviceLock = {
                biometricGate?.confirmDeviceCredential(deviceTitle, deviceDescription) { success ->
                    viewModel.onDeviceCredentialResult(success)
                }
            },
            onUseRecoveryCode = viewModel::openRecoveryCode,
            onSubmitRecoveryCode = viewModel::submitRecoveryCode,
            onSubmitNewPin = viewModel::submitNewPin,
            onBack = { viewModel.back() }
        )
    )
}

class LockScreenActions(
    val onSubmitPin: (String) -> Unit = {},
    val onBiometric: () -> Unit = {},
    val onForgot: () -> Unit = {},
    val onUseDeviceLock: () -> Unit = {},
    val onUseRecoveryCode: () -> Unit = {},
    val onSubmitRecoveryCode: (String) -> Unit = {},
    val onSubmitNewPin: (String) -> Unit = {},
    val onBack: () -> Unit = {}
)

@Composable
fun LockScreenContent(
    state: LockScreenState,
    biometricAvailable: Boolean,
    deviceLockAvailable: Boolean,
    actions: LockScreenActions
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
            AnimatedContent(
                targetState = state.stage,
                modifier = Modifier.widthIn(max = 420.dp),
                transitionSpec = {
                    (fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.96f)) togetherWith fadeOut(tween(120))
                },
                contentKey = { it::class },
                label = "lockStage"
            ) { stage ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (stage) {
                        LockStage.Unlock -> UnlockStage(state, biometricAvailable, actions)
                        LockStage.Forgot -> ForgotStage(state, deviceLockAvailable, actions)
                        LockStage.RecoveryCode -> RecoveryCodeStage(state, actions)
                        LockStage.NewPin -> NewPinContent(
                            state = state.newPin,
                            onSubmit = actions.onSubmitNewPin,
                            enabled = !state.isSaving,
                            checking = state.isSaving
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlockStage(state: LockScreenState, biometricAvailable: Boolean, actions: LockScreenActions) {
    val feedback = state.feedback
    LockHeader(
        title = stringResource(R.string.lock_screen_title),
        subtitle = stringResource(R.string.lock_screen_subtitle)
    )
    Spacer(Modifier.height(4.dp))
    VerifyFeedbackText(feedback)
    Spacer(Modifier.height(4.dp))
    PinEntry(
        onComplete = actions.onSubmitPin,
        enabled = !feedback.isChecking && !feedback.isLockedOut,
        checking = feedback.isChecking,
        errorKey = feedback.errorKey,
        onFingerprint = if (biometricAvailable) actions.onBiometric else null
    )
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = actions.onForgot) {
        Text(stringResource(R.string.lock_forgot_pin))
    }
}

@Composable
private fun ForgotStage(state: LockScreenState, deviceLockAvailable: Boolean, actions: LockScreenActions) {
    FlowBackButton(onBack = actions.onBack)
    LockHeader(
        title = stringResource(R.string.lock_forgot_title),
        subtitle = stringResource(R.string.lock_forgot_subtitle),
        icon = Icons.Default.Refresh
    )
    Spacer(Modifier.height(24.dp))
    // Each way back in is one icon-led row, the most convenient first.
    Column(verticalArrangement = Arrangement.spacedBy(GroupRowGap)) {
        val count = if (deviceLockAvailable) 2 else 1
        if (deviceLockAvailable) {
            ListRow(
                modifier = Modifier.appearIn(0),
                shape = groupedShape(0, count),
                title = stringResource(R.string.lock_use_screen_lock),
                leading = { IconBadge(ImageVector.vectorResource(R.drawable.ic_fingerprint), accent = AccentColors.Blue) },
                onClick = actions.onUseDeviceLock
            )
        }
        ListRow(
            modifier = Modifier.appearIn(1),
            shape = groupedShape(count - 1, count),
            title = stringResource(R.string.lock_use_recovery_code),
            leading = { IconBadge(Icons.Outlined.Key, accent = AccentColors.Green) },
            onClick = actions.onUseRecoveryCode
        )
    }
    if (deviceLockAvailable && state.deviceCheckFailed) {
        Spacer(Modifier.height(8.dp))
        ErrorLine(stringResource(R.string.lock_device_failed))
    }
    if (!deviceLockAvailable) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.lock_no_screen_lock),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecoveryCodeStage(state: LockScreenState, actions: LockScreenActions) {
    var code by remember { mutableStateOf("") }
    val feedback = state.feedback
    LaunchedEffect(feedback.errorKey) {
        if (feedback.errorKey != null) code = ""
    }
    val canSubmit = RecoveryCodes.normalize(code).length >= RecoveryCodes.LENGTH &&
        !feedback.isChecking && !feedback.isLockedOut
    val submit = { if (canSubmit) actions.onSubmitRecoveryCode(code) }

    FlowBackButton(onBack = actions.onBack)
    LockHeader(
        title = stringResource(R.string.lock_recover_code_title),
        subtitle = stringResource(R.string.lock_recover_code_desc),
        icon = Icons.Outlined.Key
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = code,
        onValueChange = { if (it.length <= RECOVERY_INPUT_MAX) code = it },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.TileCornerRadius),
        singleLine = true,
        enabled = !feedback.isChecking && !feedback.isLockedOut,
        isError = feedback.attemptsLeft != null,
        label = { Text(stringResource(R.string.lock_recovery_code_label)) },
        placeholder = { Text(stringResource(R.string.lock_recovery_code_hint)) },
        textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { submit() })
    )
    Spacer(Modifier.height(4.dp))
    VerifyFeedbackText(feedback)
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = submit,
        enabled = canSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(stringResource(R.string.lock_continue))
    }
}
