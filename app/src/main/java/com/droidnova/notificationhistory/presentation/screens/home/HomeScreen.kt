package com.droidnova.notificationhistory.presentation.screens.home

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.billing.LocalPremiumBillingManager
import com.droidnova.notificationhistory.component.RateUsCard
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.components.NotificationPermissionBottomSheet
import com.droidnova.notificationhistory.presentation.components.isBatteryOptimizationIgnored
import com.droidnova.notificationhistory.presentation.components.openBatteryOptimizationSettings
import com.droidnova.notificationhistory.presentation.components.openNotificationAccessSettings
import com.droidnova.notificationhistory.presentation.dialogs.PremiumPurchaseBottomSheet
import com.droidnova.notificationhistory.presentation.dialogs.PremiumWelcomeDialog
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import kotlinx.coroutines.launch

private const val RATE_US_LAUNCH_THRESHOLD = 3

private enum class TrackingVisualState { Active, Paused, PermissionRequired }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewmodel: MainViewModel, navController: NavController) {
    val state by viewmodel.settingState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val billingManager = LocalPremiumBillingManager.current
    val isPremium by viewmodel.isPremium.collectAsState()
    val showPremiumWelcome by viewmodel.showPremiumWelcome.collectAsState()
    val hasPermission by viewmodel.hasNotificationAccess.collectAsState()
    val productDetails = billingManager?.productDetails?.collectAsState()?.value
    val isFetchingPrice = billingManager?.isFetchingProductDetails?.collectAsState()?.value ?: false
    val isPurchaseInProgress = billingManager?.isPurchaseInProgress?.collectAsState()?.value ?: false
    val priceLabel = productDetails?.oneTimePurchaseOfferDetailsList
        ?.firstOrNull()
        ?.formattedPrice
    var showPurchaseSheet by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var batteryIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val permissionSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.PartiallyExpanded }
    )

    LaunchedEffect(Unit) { viewmodel.getAllInstalledApps(context) }
    LaunchedEffect(billingManager) {
        billingManager?.errors?.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(isPremium) { if (isPremium) showPurchaseSheet = false }
    LaunchedEffect(viewmodel.events) {
        viewmodel.events.collect { event ->
            when (event) {
                HomeUiEvent.NavigateToSelectApps ->
                    navController.navigate(Screens.ManageNotifications.route)
                HomeUiEvent.ShowPermissionRequiredMessage ->
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.notification_permission_required_message)
                    )
                HomeUiEvent.ShowManageSelectedAppsMessage ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.manage_selected_apps_message),
                        Toast.LENGTH_LONG
                    ).show()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewmodel.onResume()
                batteryIgnored = isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openPurchaseSheet() {
        if (isPremium) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_premium_active))
            }
        } else if (billingManager != null) {
            viewmodel.onRemoveAdsClicked()
            showPurchaseSheet = true
            billingManager.queryProductDetails()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.billing_unavailable))
            }
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                isPremium = isPremium,
                onSettings = { navController.navigate(Screens.AppSettings.route) },
                onAbout = { navController.navigate(Screens.AboutScreen.route) },
                onRemoveAds = ::openPurchaseSheet
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        HomeDashboard(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = state,
            hasPermission = hasPermission,
            showBatteryWarning = !batteryIgnored,
            showRateCard = state.launchCount >= RATE_US_LAUNCH_THRESHOLD && state.showRateUsCard,
            onTrackingChanged = { enabled ->
                when {
                    !enabled -> viewmodel.setToggleTracking(false)
                    !hasPermission -> showPermissionSheet = true
                    else -> viewmodel.onEnableClick()
                }
            },
            onPermissionAction = { showPermissionSheet = true },
            onHistory = { navController.navigate(Screens.History.route) },
            onManageApps = { navController.navigate(Screens.ManageNotifications.route) },
            onInsights = { navController.navigate(Screens.Insights.route) },
            onBatteryAction = { openBatteryOptimizationSettings(context) },
            onRateCancel = viewmodel::resetLaunchCount,
            onRateConfirmed = {
                IntentUtil.openRateUs(context)
                viewmodel.hideRateUsCard()
            },
            onRated = {
                if (it == 5) {
                    IntentUtil.openRateUs(context)
                    viewmodel.hideRateUsCard()
                }
            },
            onFeedback = {
                IntentUtil.sendSupportMail(context, isBug = false)
                viewmodel.resetLaunchCount()
            }
        )
    }

    if (showPermissionSheet && !hasPermission) {
        NotificationPermissionBottomSheet(
            sheetState = permissionSheetState,
            onDismissRequest = { showPermissionSheet = false },
            onGoToSettings = {
                showPermissionSheet = false
                viewmodel.onPermissionSettingsOpened()
                Toast.makeText(
                    context,
                    context.getString(R.string.notification_permission_toast_message),
                    Toast.LENGTH_LONG
                ).show()
                openNotificationAccessSettings(context)
            }
        )
    }

    if (showPurchaseSheet && !isPremium) {
        PremiumPurchaseBottomSheet(
            priceLabel = priceLabel,
            isLoading = isFetchingPrice || isPurchaseInProgress,
            onPurchaseClick = {
                if (activity != null) {
                    billingManager?.launchPurchaseFlow(activity)
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.purchase_unavailable))
                    }
                }
            },
            onDismiss = { showPurchaseSheet = false }
        )
    }

    if (showPremiumWelcome) {
        PremiumWelcomeDialog(
            onDismiss = viewmodel::dismissPremiumWelcome,
            onRestart = {
                viewmodel.dismissPremiumWelcome()
                IntentUtil.restartApp(context)
            },
            onOpenInstagram = { IntentUtil.openInstagram(context) },
            onOpenWhatsapp = { IntentUtil.openWhatsApp(context) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    isPremium: Boolean,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onRemoveAds: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.content_description_menu)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (!isPremium) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.premium_menu_remove_ads)) },
                        onClick = {
                            showMenu = false
                            onRemoveAds()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.about_title)) },
                    onClick = {
                        showMenu = false
                        onAbout()
                    }
                )
            }
        }
    )
}

@Composable
private fun HomeDashboard(
    modifier: Modifier,
    state: SettingState,
    hasPermission: Boolean,
    showBatteryWarning: Boolean,
    showRateCard: Boolean,
    onTrackingChanged: (Boolean) -> Unit,
    onPermissionAction: () -> Unit,
    onHistory: () -> Unit,
    onManageApps: () -> Unit,
    onInsights: () -> Unit,
    onBatteryAction: () -> Unit,
    onRateCancel: () -> Unit,
    onRateConfirmed: () -> Unit,
    onRated: (Int) -> Unit,
    onFeedback: () -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TrackingStatusCard(
                trackingEnabled = state.userToggleTracking,
                hasPermission = hasPermission,
                onTrackingChanged = onTrackingChanged,
                onPermissionAction = onPermissionAction
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.home_view_history),
                    supportingText = stringResource(R.string.home_view_history_description),
                    onClick = onHistory
                )
                DashboardActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Settings,
                    title = stringResource(R.string.home_manage_apps),
                    supportingText = stringResource(
                        R.string.home_selected_apps_count,
                        state.selectedAppsCount
                    ),
                    onClick = onManageApps
                )
            }
        }
        item { InsightsEntryCard(onClick = onInsights) }
        if (state.userToggleTracking && state.selectedAppsCount == 0) {
            item {
                Text(
                    text = stringResource(R.string.select_at_least_one_app_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        if (showBatteryWarning) item { BatteryWarningCard(onBatteryAction) }
        if (showRateCard) {
            item {
                RateUsCard(
                    modifier = Modifier.fillMaxWidth(),
                    onCancelClicked = onRateCancel,
                    onOkClicked = onRateConfirmed,
                    onRated = onRated,
                    onFeedbackClicked = onFeedback
                )
            }
        }
    }
}

@Composable
private fun TrackingStatusCard(
    trackingEnabled: Boolean,
    hasPermission: Boolean,
    onTrackingChanged: (Boolean) -> Unit,
    onPermissionAction: () -> Unit
) {
    val state = when {
        !hasPermission -> TrackingVisualState.PermissionRequired
        trackingEnabled -> TrackingVisualState.Active
        else -> TrackingVisualState.Paused
    }
    val title = when (state) {
        TrackingVisualState.Active -> stringResource(R.string.tracking_active)
        TrackingVisualState.Paused -> stringResource(R.string.tracking_paused)
        TrackingVisualState.PermissionRequired -> stringResource(R.string.tracking_permission_required)
    }
    val description = when (state) {
        TrackingVisualState.Active -> stringResource(R.string.tracking_active_description)
        TrackingVisualState.Paused -> stringResource(R.string.tracking_paused_description)
        TrackingVisualState.PermissionRequired -> stringResource(R.string.tracking_permission_description)
    }
    val icon = when (state) {
        TrackingVisualState.Active -> Icons.Default.CheckCircle
        TrackingVisualState.Paused -> Icons.Default.Info
        TrackingVisualState.PermissionRequired -> Icons.Default.Warning
    }
    val colors = when (state) {
        TrackingVisualState.Active -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        TrackingVisualState.Paused -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TrackingVisualState.PermissionRequired -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = colors, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = trackingEnabled && hasPermission,
                    onCheckedChange = onTrackingChanged
                )
            }
            if (!hasPermission) {
                TextButton(onClick = onPermissionAction, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.go_to_settings))
                }
            }
        }
    }
}

@Composable
private fun DashboardActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    supportingText: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 124.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = title,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = supportingText,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightsEntryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_insights),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_insights),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.home_insights_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BatteryWarningCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    stringResource(R.string.battery_warning_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.battery_warning_description),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = onClick) { Text(stringResource(R.string.fix)) }
        }
    }
}
