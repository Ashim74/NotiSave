package com.droidnova.notificationhistory.presentation.screens.lock

import android.widget.Toast
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Key
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.core.lock.AutoLockTimeout
import com.droidnova.notificationhistory.core.lock.BiometricGate
import com.droidnova.notificationhistory.core.lock.LocalBiometricGate
import com.droidnova.notificationhistory.core.lock.LockConfig
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.ChoicePill
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListGroup
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.ProBadge
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.floating
import com.droidnova.notificationhistory.presentation.components.pulsing
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.dialogs.PremiumUpsell
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

@Composable
fun AppLockSettingsScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    viewModel: AppLockSettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val biometricGate = LocalBiometricGate.current
    val isPremium by mainViewModel.isPremium.collectAsState()
    var showPremium by remember { mutableStateOf(false) }

    BackHandler(enabled = state.flow != LockSettingsFlow.None) { viewModel.back() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message.textRes(), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.app_lock_title),
                onBack = { if (!viewModel.back()) navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AnimatedContent(
            targetState = state.flow,
            modifier = Modifier.padding(padding),
            transitionSpec = {
                (fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.96f)) togetherWith fadeOut(tween(120))
            },
            contentKey = { it::class },
            label = "lockSettingsFlow"
        ) { flow ->
            when (flow) {
                LockSettingsFlow.None -> LockSettingsList(
                    config = state.config,
                    biometricGate = biometricGate,
                    viewModel = viewModel,
                    isPremium = isPremium,
                    onLockedFeature = { showPremium = true }
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
                    Spacer(Modifier.height(4.dp))
                    VerifyFeedbackText(state.feedback)
                    Spacer(Modifier.height(4.dp))
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

    PremiumUpsell(mainViewModel, visible = showPremium, onDismiss = { showPremium = false })
}

@Composable
private fun FlowContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
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
    viewModel: AppLockSettingsViewModel,
    isPremium: Boolean,
    onLockedFeature: () -> Unit
) {
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

        item { LockStatusCard(modifier = Modifier.appearIn(0)) }
        item {
            ListGroup(modifier = Modifier.appearIn(1)) {
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.app_lock_change),
                        leading = { IconBadge(Icons.Default.Edit, accent = AccentColors.Blue) },
                        onClick = { viewModel.startProtected(ProtectedAction.Change) }
                    )
                }
                if (canUseBiometric) {
                    row { shape ->
                        // Premium feature. Users who turned it on before it became one keep it
                        // until they switch it off.
                        val locked = !isPremium && !config.biometricEnabled
                        val toggleBiometric = { enable: Boolean ->
                            if (!enable) {
                                viewModel.setBiometricEnabled(false)
                            } else if (locked) {
                                onLockedFeature()
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
                            leading = {
                                IconBadge(ImageVector.vectorResource(R.drawable.ic_fingerprint), accent = AccentColors.Purple)
                            },
                            onClick = { toggleBiometric(!config.biometricEnabled) },
                            trailing = {
                                if (locked) ProBadge() else Switch(checked = config.biometricEnabled, onCheckedChange = null)
                            }
                        )
                    }
                }
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.app_lock_new_recovery_code),
                        leading = { IconBadge(Icons.Outlined.Key, accent = AccentColors.Green) },
                        onClick = { viewModel.startProtected(ProtectedAction.NewRecoveryCode) }
                    )
                }
            }
        }
        item {
            TimeoutCard(
                selected = config.timeout,
                onSelect = viewModel::setTimeout,
                modifier = Modifier.appearIn(2)
            )
        }
        item {
            ListRow(
                modifier = Modifier.appearIn(3),
                title = stringResource(R.string.app_lock_turn_off),
                titleColor = MaterialTheme.colorScheme.error,
                leading = {
                    IconBadge(
                        Icons.Default.Close,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.error
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
            icon = {
                IconBadge(
                    Icons.Default.Lock,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.error,
                    size = 52.dp
                )
            },
            title = { Text(stringResource(R.string.app_lock_turn_off_confirm_title), textAlign = TextAlign.Center) },
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
}

/** Lock off: a floating lock, one line of why, three icon chips and the switch-on button. */
@Composable
private fun LockIntroCard(onTurnOn: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth().appearIn(0)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(
                Icons.Default.Lock,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                size = 72.dp,
                modifier = Modifier.floating()
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.app_lock_intro_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.app_lock_intro_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                FeatureChip(Icons.Default.Edit, stringResource(R.string.lock_feature_pin), AccentColors.Blue, 1)
                FeatureChip(
                    ImageVector.vectorResource(R.drawable.ic_fingerprint),
                    stringResource(R.string.lock_feature_fingerprint),
                    AccentColors.Purple,
                    2
                )
                FeatureChip(
                    Icons.Outlined.Key,
                    stringResource(R.string.lock_feature_recovery),
                    AccentColors.Green,
                    3
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onTurnOn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.app_lock_turn_on))
            }
        }
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, label: String, accent: androidx.compose.ui.graphics.Color, index: Int) {
    Column(
        modifier = Modifier.appearIn(index, stepMillis = 70),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconBadge(icon, accent = accent, size = 44.dp)
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** Lock on: a gradient banner with a softly breathing lock. */
@Composable
private fun LockStatusCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            Icons.Default.Lock,
            containerColor = colors.onPrimary.copy(alpha = 0.18f),
            contentColor = colors.onPrimary,
            size = 44.dp,
            modifier = Modifier.pulsing(minScale = 0.92f)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                stringResource(R.string.app_lock_status_on),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary
            )
            Text(
                stringResource(R.string.app_lock_status_on_desc),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

/** Auto-lock delay as pills right under its row: one tap, no dialog. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeoutCard(
    selected: AutoLockTimeout,
    onSelect: (AutoLockTimeout) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(tintedCardColor())
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.LockClock, accent = AccentColors.Orange)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_lock_timeout),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    stringResource(selected.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        FlowRow(
            modifier = Modifier.padding(start = 50.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AutoLockTimeout.entries.forEach { option ->
                ChoicePill(
                    label = stringResource(option.shortLabelRes()),
                    selected = option == selected,
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@StringRes
private fun AutoLockTimeout.labelRes(): Int = when (this) {
    AutoLockTimeout.Immediately -> R.string.app_lock_timeout_immediately
    AutoLockTimeout.Seconds30 -> R.string.app_lock_timeout_30s
    AutoLockTimeout.Minute1 -> R.string.app_lock_timeout_1m
    AutoLockTimeout.Minutes5 -> R.string.app_lock_timeout_5m
}

@StringRes
private fun AutoLockTimeout.shortLabelRes(): Int = when (this) {
    AutoLockTimeout.Immediately -> R.string.lock_timeout_short_now
    AutoLockTimeout.Seconds30 -> R.string.lock_timeout_short_30s
    AutoLockTimeout.Minute1 -> R.string.lock_timeout_short_1m
    AutoLockTimeout.Minutes5 -> R.string.lock_timeout_short_5m
}

@StringRes
private fun LockSettingsMessage.textRes(): Int = when (this) {
    LockSettingsMessage.LockOn -> R.string.app_lock_turned_on
    LockSettingsMessage.PinChanged -> R.string.app_lock_changed
    LockSettingsMessage.LockOff -> R.string.app_lock_turned_off
}
