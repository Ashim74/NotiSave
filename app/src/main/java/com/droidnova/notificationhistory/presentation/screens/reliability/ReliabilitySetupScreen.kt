package com.droidnova.notificationhistory.presentation.screens.reliability

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.areAlertsEnabled
import com.droidnova.notificationhistory.presentation.components.oemReliabilityStep
import com.droidnova.notificationhistory.presentation.components.openAppNotificationSettings
import com.droidnova.notificationhistory.presentation.components.openNotificationAccessSettings
import com.droidnova.notificationhistory.presentation.components.rememberBatteryOptimizationState
import com.droidnova.notificationhistory.presentation.screens.home.HomeViewModel
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/**
 * "Keep capture running": every permission and setting the listener needs to survive in the
 * background, one step each, ticked off live as the user grants them. Opens on its own once
 * after notification access is granted (see HomeScreen); Home's reliability card leads back.
 *
 * The OEM step can't be read back from the system, so the user confirms it themselves after
 * visiting the screen; that confirmation is the same flag Home's card uses for Done.
 */
@Composable
fun ReliabilitySetupScreen(
    mainViewModel: MainViewModel,
    homeViewModel: HomeViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val hasAccess by mainViewModel.hasNotificationAccess.collectAsState()
    val battery = rememberBatteryOptimizationState()
    val oemStep = remember { oemReliabilityStep(context) }
    val oemConfirmed by homeViewModel.oemGuidanceDismissed.collectAsState()
    var oemOpened by rememberSaveable { mutableStateOf(false) }
    var alertsEnabled by remember { mutableStateOf(areAlertsEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mainViewModel.onResume()
                alertsEnabled = areAlertsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val alertsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        alertsEnabled = granted && areAlertsEnabled(context)
        // Denied for good: the dialog won't show again, so the system page is the only way.
        if (!alertsEnabled) openAppNotificationSettings(context)
    }

    val steps = buildList {
        add(
            SetupStep(
                key = "access",
                icon = Icons.Outlined.NotificationsActive,
                accent = AccentColors.Blue,
                title = R.string.reliability_setup_access_title,
                description = R.string.reliability_setup_access_desc,
                done = hasAccess,
                action = R.string.alerts_allow,
                onAction = {
                    mainViewModel.onPermissionSettingsOpened()
                    openNotificationAccessSettings(context)
                }
            )
        )
        add(
            SetupStep(
                key = "battery",
                icon = Icons.Outlined.BatteryChargingFull,
                accent = AccentColors.Green,
                title = R.string.reliability_setup_battery_title,
                description = R.string.reliability_setup_battery_desc,
                done = battery.isIgnored,
                action = R.string.alerts_allow,
                onAction = battery.requestExemption
            )
        )
        if (oemStep != null) {
            add(
                SetupStep(
                    key = "oem",
                    icon = Icons.Outlined.PlayCircle,
                    accent = AccentColors.Amber,
                    title = R.string.reliability_setup_oem_title,
                    description = oemStep.textRes,
                    done = oemConfirmed,
                    action = if (oemOpened) R.string.reliability_setup_oem_confirm else R.string.reliability_open,
                    onAction = {
                        if (oemOpened) {
                            homeViewModel.dismissOemGuidance()
                        } else {
                            oemOpened = true
                            oemStep.open(context)
                        }
                    }
                )
            )
        }
        add(
            SetupStep(
                key = "alerts",
                icon = Icons.Outlined.WarningAmber,
                accent = AccentColors.Purple,
                title = R.string.reliability_setup_alerts_title,
                description = R.string.reliability_setup_alerts_desc,
                done = alertsEnabled,
                action = R.string.alerts_allow,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        alertsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppNotificationSettings(context)
                    }
                }
            )
        )
    }
    val allDone = steps.all { it.done }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.reliability_setup_title),
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = Dimens.ScreenHorizontal,
                    end = Dimens.ScreenHorizontal,
                    bottom = 16.dp
                )
            ) {
                item(key = "intro") {
                    Text(
                        text = stringResource(R.string.reliability_setup_subtitle),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                steps.forEachIndexed { index, step ->
                    item(key = step.key) {
                        SetupStepCard(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .appearIn(index),
                            step = step
                        )
                    }
                }
            }
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenHorizontal, vertical = 12.dp)
            ) {
                Text(
                    stringResource(
                        if (allDone) R.string.reliability_setup_finish else R.string.reliability_setup_later
                    )
                )
            }
        }
    }
}

private class SetupStep(
    val key: String,
    val icon: ImageVector,
    val accent: Color,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val done: Boolean,
    @StringRes val action: Int,
    val onAction: () -> Unit
)

@Composable
private fun SetupStepCard(modifier: Modifier, step: SetupStep) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(step.icon, accent = step.accent, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(step.title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(step.description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            if (step.done) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.reliability_setup_step_done),
                    tint = AccentColors.Green,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                FilledTonalButton(onClick = step.onAction) { Text(stringResource(step.action)) }
            }
        }
    }
}

/**
 * Whether anything the setup covers still needs the user. Notification alerts are left out:
 * they only matter once capture has already stopped, so they never reopen the setup alone.
 */
internal fun reliabilitySetupPending(
    batteryIgnored: Boolean,
    hasOemStep: Boolean,
    oemConfirmed: Boolean
): Boolean = !batteryIgnored || (hasOemStep && !oemConfirmed)
