package com.droidnova.notificationhistory.presentation.screens.lock

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.AppCardDefaults
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListGroup
import com.droidnova.notificationhistory.presentation.components.ListRow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

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
        AnimatedContent(
            targetState = state.flow,
            modifier = Modifier.padding(padding),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            contentKey = { it::class },
            label = "lockSettingsFlow"
        ) { flow ->
            when (flow) {
                LockSettingsFlow.None -> LockSettingsList(
                    config = state.config,
                    biometricGate = biometricGate,
                    viewModel = viewModel
                )
                LockSettingsFlow.SetupPin, LockSettingsFlow.ChangePin -> FlowContainer {
                    NewPinContent(
                        state = state.newPin,
                        onSubmit = viewModel::submitNewPin,
                        subtitle = if (flow == LockSettingsFlow.SetupPin) {
                            stringResource(R.string.app_lock_forgot_info)
                        } else {
                            null
                        },
                        enabled = !state.isSaving,
                        checking = state.isSaving
                    )
                }
                is LockSettingsFlow.VerifyCurrent -> FlowContainer {
                    LockHeader(title = stringResource(R.string.app_lock_verify_title), subtitle = null)
                    Spacer(Modifier.height(8.dp))
                    VerifyFeedbackText(state.feedback)
                    Spacer(Modifier.height(8.dp))
                    PinEntry(
                        onComplete = viewModel::submitCurrentPin,
                        enabled = !state.feedback.isChecking && !state.feedback.isLockedOut,
                        checking = state.feedback.isChecking,
                        errorKey = state.feedback.errorKey
                    )
                }
                is LockSettingsFlow.ShowRecoveryCode -> FlowContainer {
                    RecoveryCodeContent(code = flow.code, onDone = viewModel::finishRecoveryCode)
                }
            }
        }
    }
}

@Composable
private fun FlowContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
    biometricGate: BiometricGate?,
    viewModel: AppLockSettingsViewModel
) {
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showTurnOffDialog by remember { mutableStateOf(false) }
    val canUseBiometric = remember { biometricGate?.canUseBiometric() == true }
    val biometricTitle = stringResource(R.string.app_lock_biometric_enable_title)
    val biometricSubtitle = stringResource(R.string.app_lock_biometric_enable_subtitle)
    val cancel = stringResource(R.string.cancel)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal, vertical = Dimens.CardSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
    ) {
        if (!config.isEnabled) {
            item { LockIntroCard(onTurnOn = viewModel::startSetup) }
            return@LazyColumn
        }

        item { LockStatusCard() }
        item {
            ListGroup {
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.app_lock_change),
                        leading = { IconBadge(Icons.Default.Edit) },
                        onClick = { viewModel.startProtected(ProtectedAction.Change) }
                    )
                }
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.app_lock_timeout),
                        value = stringResource(config.timeout.labelRes()),
                        leading = { IconBadge(ImageVector.vectorResource(R.drawable.ic_history)) },
                        onClick = { showTimeoutDialog = true }
                    )
                }
                if (canUseBiometric) {
                    row { shape ->
                        val toggleBiometric = { enable: Boolean ->
                            if (!enable) {
                                viewModel.setBiometricEnabled(false)
                            } else {
                                // Proves the sensor works for this user before relying on it.
                                biometricGate?.authenticateBiometric(biometricTitle, biometricSubtitle, cancel) {
                                    if (it) viewModel.setBiometricEnabled(true)
                                }
                            }
                        }
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.app_lock_biometric),
                            leading = { IconBadge(ImageVector.vectorResource(R.drawable.ic_fingerprint)) },
                            onClick = { toggleBiometric(!config.biometricEnabled) },
                            trailing = { Switch(checked = config.biometricEnabled, onCheckedChange = null) }
                        )
                    }
                }
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.app_lock_new_recovery_code),
                        leading = { IconBadge(Icons.Default.Refresh) },
                        onClick = { viewModel.startProtected(ProtectedAction.NewRecoveryCode) }
                    )
                }
            }
        }
        item {
            ListRow(
                title = stringResource(R.string.app_lock_turn_off),
                titleColor = MaterialTheme.colorScheme.error,
                leading = {
                    IconBadge(
                        Icons.Default.Close,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                },
                showChevron = false,
                onClick = { showTurnOffDialog = true }
            )
        }
    }

    if (showTurnOffDialog) {
        // A plain confirmation (no PIN): it guards against a stray tap wiping the PIN and code.
        AlertDialog(
            onDismissRequest = { showTurnOffDialog = false },
            title = { Text(stringResource(R.string.app_lock_turn_off_confirm_title)) },
            text = { Text(stringResource(R.string.app_lock_turn_off_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showTurnOffDialog = false
                    viewModel.turnOff()
                }) {
                    Text(
                        stringResource(R.string.app_lock_turn_off_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTurnOffDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showTimeoutDialog) {
        TimeoutDialog(
            selected = config.timeout,
            onSelect = {
                viewModel.setTimeout(it)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false }
        )
    }
}

@Composable
private fun LockIntroCard(onTurnOn: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
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
private fun LockStatusCard() {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        colors = AppCardDefaults.colors(
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
                    stringResource(R.string.app_lock_status_on_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TimeoutDialog(
    selected: AutoLockTimeout,
    onSelect: (AutoLockTimeout) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_lock_timeout_dialog_title)) },
        text = {
            Column {
                AutoLockTimeout.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSelect(option) }
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(option.labelRes()))
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
private fun LockSettingsMessage.textRes(): Int = when (this) {
    LockSettingsMessage.LockOn -> R.string.app_lock_turned_on
    LockSettingsMessage.PinChanged -> R.string.app_lock_changed
    LockSettingsMessage.LockOff -> R.string.app_lock_turned_off
}
