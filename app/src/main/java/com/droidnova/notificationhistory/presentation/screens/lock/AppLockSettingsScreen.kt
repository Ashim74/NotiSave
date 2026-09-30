package com.droidnova.notificationhistory.presentation.screens.lock

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.core.lock.AutoLockTimeout
import com.droidnova.notificationhistory.core.lock.BiometricGate
import com.droidnova.notificationhistory.core.lock.LocalBiometricGate
import com.droidnova.notificationhistory.core.lock.LockConfig
import com.droidnova.notificationhistory.core.lock.LockType
import com.droidnova.notificationhistory.core.lock.RecoveryMethod
import com.droidnova.notificationhistory.presentation.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSettingsScreen(
    navController: NavController,
    viewModel: AppLockSettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val biometricGate = LocalBiometricGate.current

    BackHandler(enabled = state.flow != LockSettingsFlow.None) { viewModel.back() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message.textRes(), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_lock_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (!viewModel.back()) navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val flow = state.flow) {
            LockSettingsFlow.None -> LockSettingsList(
                config = state.config,
                padding = padding,
                biometricGate = biometricGate,
                viewModel = viewModel
            )
            else -> FlowContainer(padding) {
                when (flow) {
                    LockSettingsFlow.SetupSecret, LockSettingsFlow.ChangeSecret -> NewSecretContent(
                        state = state.newSecret,
                        title = stringResource(
                            if (flow == LockSettingsFlow.SetupSecret) R.string.app_lock_setup_title
                            else R.string.app_lock_change
                        ),
                        onChooseType = viewModel::chooseNewSecretType,
                        onSubmit = viewModel::submitNewSecret
                    )
                    LockSettingsFlow.SetupRecovery -> SetupRecoveryStep(
                        biometricGate = biometricGate,
                        isSaving = state.isSaving,
                        onDone = viewModel::completeSetup
                    )
                    is LockSettingsFlow.VerifyCurrent -> VerifyCurrentStep(
                        lockType = state.config.type ?: LockType.Pin,
                        feedback = state.feedback,
                        onSubmit = viewModel::submitCurrentSecret
                    )
                    is LockSettingsFlow.ShowRecoveryCode ->
                        RecoveryCodeContent(code = flow.code, onDone = viewModel::finishRecoveryCode)
                    LockSettingsFlow.None -> Unit
                }
            }
        }
    }
}

@Composable
private fun FlowContainer(padding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}

@Composable
private fun LockSettingsList(
    config: LockConfig,
    padding: PaddingValues,
    biometricGate: BiometricGate?,
    viewModel: AppLockSettingsViewModel
) {
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    val canUseBiometric = remember { biometricGate?.canUseBiometric() == true }
    val deviceSecure = remember { biometricGate?.isDeviceSecure() == true }
    val biometricTitle = stringResource(R.string.app_lock_biometric_enable_title)
    val biometricSubtitle = stringResource(R.string.app_lock_biometric_enable_subtitle)
    val cancel = stringResource(R.string.cancel)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!config.isEnabled) {
            item { LockIntroCard(onTurnOn = viewModel::startSetup) }
            return@LazyColumn
        }

        item { LockStatusCard(config.type ?: LockType.Pin) }
        item { SectionHeader(stringResource(R.string.app_lock_section_unlock)) }
        item {
            LockSettingRow(
                title = stringResource(R.string.app_lock_change),
                supportingText = stringResource(R.string.app_lock_change_desc),
                onClick = { viewModel.startProtected(ProtectedAction.Change) }
            )
        }
        item {
            LockSettingRow(
                title = stringResource(R.string.app_lock_timeout),
                supportingText = stringResource(config.timeout.labelRes()),
                onClick = { showTimeoutDialog = true }
            )
        }
        if (canUseBiometric) {
            item {
                LockSettingRow(
                    title = stringResource(R.string.app_lock_biometric),
                    supportingText = stringResource(R.string.app_lock_biometric_desc),
                    checked = config.biometricEnabled,
                    onCheckedChange = { enable ->
                        if (!enable) {
                            viewModel.setBiometricEnabled(false)
                        } else {
                            // Proves the sensor works for this user before relying on it.
                            biometricGate?.authenticateBiometric(biometricTitle, biometricSubtitle, cancel) {
                                if (it) viewModel.setBiometricEnabled(true)
                            }
                        }
                    }
                )
            }
        }
        item {
            LockSettingRow(
                title = stringResource(R.string.app_lock_hide_recents),
                supportingText = stringResource(R.string.app_lock_hide_recents_desc),
                checked = config.hideInRecents,
                onCheckedChange = viewModel::setHideInRecents
            )
        }

        item { SectionHeader(stringResource(R.string.app_lock_section_recovery)) }
        item {
            LockSettingRow(
                title = stringResource(R.string.app_lock_recovery_method),
                supportingText = stringResource(config.recoveryMethod.labelRes()),
                onClick = { showRecoveryDialog = true }
            )
        }
        if (!deviceSecure || config.recoveryMethod == RecoveryMethod.CodeOnly) {
            item { RecoveryCodeOnlyNote(deviceSecure) }
        }
        item {
            LockSettingRow(
                title = stringResource(R.string.app_lock_new_recovery_code),
                supportingText = stringResource(R.string.app_lock_new_recovery_code_desc),
                onClick = { viewModel.startProtected(ProtectedAction.NewRecoveryCode) }
            )
        }
        item {
            LockSettingRow(
                title = stringResource(R.string.app_lock_turn_off),
                supportingText = stringResource(R.string.app_lock_turn_off_desc),
                isDestructive = true,
                onClick = { viewModel.startProtected(ProtectedAction.TurnOff) }
            )
        }
    }

    if (showTimeoutDialog) {
        ChoiceDialog(
            title = stringResource(R.string.app_lock_timeout_dialog_title),
            options = AutoLockTimeout.entries,
            selected = config.timeout,
            label = { stringResource(it.labelRes()) },
            onSelect = {
                viewModel.setTimeout(it)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false }
        )
    }
    if (showRecoveryDialog) {
        ChoiceDialog(
            title = stringResource(R.string.app_lock_recovery_method),
            options = RecoveryMethod.entries,
            selected = config.recoveryMethod,
            label = { stringResource(it.labelRes()) },
            description = { stringResource(it.descriptionRes()) },
            onSelect = {
                showRecoveryDialog = false
                if (it != config.recoveryMethod) {
                    viewModel.startProtected(ProtectedAction.SetRecoveryMethod(it))
                }
            },
            onDismiss = { showRecoveryDialog = false }
        )
    }
}

@Composable
private fun LockIntroCard(onTurnOn: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.app_lock_intro_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.app_lock_intro_body), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.app_lock_forgot_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onTurnOn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.app_lock_turn_on))
            }
        }
    }
}

@Composable
private fun LockStatusCard(type: LockType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.app_lock_status_on),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(
                        if (type == LockType.Pin) R.string.app_lock_status_on_pin
                        else R.string.app_lock_status_on_password
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RecoveryCodeOnlyNote(deviceSecure: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(
                    if (deviceSecure) R.string.app_lock_code_only_note
                    else R.string.app_lock_no_device_lock_warning
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LockSettingRow(
    title: String,
    supportingText: String,
    onClick: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    isDestructive: Boolean = false
) {
    val titleColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val rowClick: (() -> Unit)? = onClick
        ?: if (checked != null && onCheckedChange != null) {
            { onCheckedChange(!checked) }
        } else {
            null
        }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (rowClick != null) Modifier.clickable(onClick = rowClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDestructive) titleColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (checked != null) {
                Spacer(Modifier.width(12.dp))
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
        }
    }
}

/** Setup step 2: forgot-PIN options, plus fingerprint/face when the phone supports it. */
@Composable
private fun SetupRecoveryStep(
    biometricGate: BiometricGate?,
    isSaving: Boolean,
    onDone: (RecoveryMethod, Boolean) -> Unit
) {
    val deviceSecure = remember { biometricGate?.isDeviceSecure() == true }
    val canUseBiometric = remember { biometricGate?.canUseBiometric() == true }
    var method by rememberSaveable { mutableStateOf(RecoveryMethod.DeviceAndCode) }
    var useBiometric by rememberSaveable { mutableStateOf(false) }
    val biometricTitle = stringResource(R.string.app_lock_biometric_enable_title)
    val biometricSubtitle = stringResource(R.string.app_lock_biometric_enable_subtitle)
    val cancel = stringResource(R.string.cancel)

    LockHeader(
        title = stringResource(R.string.app_lock_recovery_step_title),
        subtitle = stringResource(R.string.app_lock_recovery_step_subtitle)
    )
    Spacer(Modifier.height(24.dp))
    if (deviceSecure) {
        RecoveryMethod.entries.forEach { option ->
            OptionCard(
                title = stringResource(option.labelRes()),
                description = stringResource(option.descriptionRes()),
                selected = option == method,
                onClick = { method = option }
            )
            Spacer(Modifier.height(12.dp))
        }
    } else {
        RecoveryCodeOnlyNote(deviceSecure = false)
        Spacer(Modifier.height(12.dp))
    }
    if (canUseBiometric) {
        LockSettingRow(
            title = stringResource(R.string.app_lock_setup_use_biometric),
            supportingText = stringResource(R.string.app_lock_biometric_desc),
            checked = useBiometric,
            onCheckedChange = { enable ->
                if (!enable) {
                    useBiometric = false
                } else {
                    biometricGate?.authenticateBiometric(biometricTitle, biometricSubtitle, cancel) {
                        if (it) useBiometric = true
                    }
                }
            }
        )
        Spacer(Modifier.height(12.dp))
    }
    Spacer(Modifier.height(12.dp))
    Button(
        // Without a phone screen lock only the recovery code can work.
        onClick = { onDone(if (deviceSecure) method else RecoveryMethod.CodeOnly, useBiometric) },
        enabled = !isSaving,
        modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.lock_continue)) }
}

@Composable
private fun VerifyCurrentStep(
    lockType: LockType,
    feedback: VerifyFeedback,
    onSubmit: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    LaunchedEffect(feedback.attemptsLeft, feedback.isLockedOut) {
        if (feedback.attemptsLeft != null || feedback.isLockedOut) input = ""
    }
    LockHeader(
        title = stringResource(
            if (lockType == LockType.Pin) R.string.app_lock_verify_title_pin
            else R.string.app_lock_verify_title_password
        ),
        subtitle = null
    )
    Spacer(Modifier.height(12.dp))
    VerifyFeedbackText(feedback)
    Spacer(Modifier.height(12.dp))
    SecretEntry(
        type = lockType,
        value = input,
        onValueChange = { input = it },
        onSubmit = { onSubmit(input) },
        enabled = !feedback.isChecking && !feedback.isLockedOut,
        isError = feedback.attemptsLeft != null
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    description: (@Composable (T) -> String)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(label(option))
                            if (description != null) {
                                Text(
                                    description(option),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@StringRes
private fun AutoLockTimeout.labelRes(): Int = when (this) {
    AutoLockTimeout.Immediately -> R.string.app_lock_timeout_immediately
    AutoLockTimeout.Seconds30 -> R.string.app_lock_timeout_30s
    AutoLockTimeout.Minute1 -> R.string.app_lock_timeout_1m
    AutoLockTimeout.Minutes5 -> R.string.app_lock_timeout_5m
}

@StringRes
private fun RecoveryMethod.labelRes(): Int = when (this) {
    RecoveryMethod.DeviceAndCode -> R.string.app_lock_recovery_device_and_code
    RecoveryMethod.CodeOnly -> R.string.app_lock_recovery_code_only
}

@StringRes
private fun RecoveryMethod.descriptionRes(): Int = when (this) {
    RecoveryMethod.DeviceAndCode -> R.string.app_lock_recovery_device_and_code_desc
    RecoveryMethod.CodeOnly -> R.string.app_lock_recovery_code_only_desc
}

@StringRes
private fun LockSettingsMessage.textRes(): Int = when (this) {
    LockSettingsMessage.LockOn -> R.string.app_lock_turned_on
    LockSettingsMessage.LockChanged -> R.string.app_lock_changed
    LockSettingsMessage.LockOff -> R.string.app_lock_turned_off
    LockSettingsMessage.RecoveryMethodChanged -> R.string.app_lock_recovery_changed
}
