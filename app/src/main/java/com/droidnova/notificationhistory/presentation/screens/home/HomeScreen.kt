package com.droidnova.notificationhistory.presentation.screens.home

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
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
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.NotificationDetailsDialog
import com.droidnova.notificationhistory.presentation.components.NotificationHistoryCard
import com.droidnova.notificationhistory.presentation.components.NotificationPermissionBottomSheet
import com.droidnova.notificationhistory.presentation.components.SectionHeader
import com.droidnova.notificationhistory.presentation.components.StatCard
import com.droidnova.notificationhistory.presentation.components.StatusCard
import com.droidnova.notificationhistory.presentation.components.StatusTone
import com.droidnova.notificationhistory.presentation.components.isBatteryOptimizationIgnored
import com.droidnova.notificationhistory.presentation.components.openBatteryOptimizationSettings
import com.droidnova.notificationhistory.presentation.components.openNotificationAccessSettings
import com.droidnova.notificationhistory.presentation.dialogs.PremiumPurchaseBottomSheet
import com.droidnova.notificationhistory.presentation.dialogs.PremiumWelcomeDialog
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.navigation.navigateToTab
import com.droidnova.notificationhistory.service.ListenerReconnector
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import kotlinx.coroutines.launch

private const val RATE_US_LAUNCH_THRESHOLD = 3

/** What the hero card says about capture right now. */
private enum class CaptureState { Recording, Paused, Reconnecting, AccessNeeded }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewmodel: MainViewModel,
    homeViewModel: HomeViewModel,
    navController: NavController
) {
    val state by viewmodel.settingState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val billingManager = LocalPremiumBillingManager.current
    val isPremium by viewmodel.isPremium.collectAsState()
    val showPremiumWelcome by viewmodel.showPremiumWelcome.collectAsState()
    val hasPermission by viewmodel.hasNotificationAccess.collectAsState()
    val listenerConnected by viewmodel.listenerConnected.collectAsState()
    val todaySummary by homeViewModel.todaySummary.collectAsState()
    val recentNotifications by homeViewModel.recentNotifications.collectAsState()
    val productDetails = billingManager?.productDetails?.collectAsState()?.value
    val isFetchingPrice = billingManager?.isFetchingProductDetails?.collectAsState()?.value ?: false
    val isPurchaseInProgress = billingManager?.isPurchaseInProgress?.collectAsState()?.value ?: false
    val priceLabel = productDetails?.oneTimePurchaseOfferDetailsList
        ?.firstOrNull()
        ?.formattedPrice
    var showPurchaseSheet by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf<NotificationModel?>(null) }
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
                homeViewModel.refresh()
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
            listenerConnected = listenerConnected,
            todaySummary = todaySummary,
            recentNotifications = recentNotifications,
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
            onReconnect = { ListenerReconnector.ensureConnected(context) },
            onSeeAllHistory = { navController.navigateToTab(Screens.History.route) },
            onManageApps = { navController.navigate(Screens.ManageNotifications.route) },
            onInsights = { navController.navigateToTab(Screens.Insights.route) },
            onBatteryAction = { openBatteryOptimizationSettings(context) },
            onNotificationClick = { showDetailsDialog = it },
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

    showDetailsDialog?.let { notification ->
        NotificationDetailsDialog(
            notification = notification,
            onDismiss = { showDetailsDialog = null }
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
    onAbout: () -> Unit,
    onRemoveAds: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
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
    listenerConnected: Boolean,
    todaySummary: HomeTodaySummary,
    recentNotifications: List<NotificationModel>,
    showBatteryWarning: Boolean,
    showRateCard: Boolean,
    onTrackingChanged: (Boolean) -> Unit,
    onPermissionAction: () -> Unit,
    onReconnect: () -> Unit,
    onSeeAllHistory: () -> Unit,
    onManageApps: () -> Unit,
    onInsights: () -> Unit,
    onBatteryAction: () -> Unit,
    onNotificationClick: (NotificationModel) -> Unit,
    onRateCancel: () -> Unit,
    onRateConfirmed: () -> Unit,
    onRated: (Int) -> Unit,
    onFeedback: () -> Unit
) {
    // NotificationHistoryCard carries its own 12 dp side margin, so the list keeps 4 dp and every
    // other item adds 12 dp: cards and rows line up at 16 dp from the screen edge.
    val itemPadding = Modifier.padding(horizontal = 12.dp)
    val noAppsSelected = state.userToggleTracking && state.selectedAppsCount == 0

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CaptureStatusCard(
                modifier = itemPadding,
                trackingEnabled = state.userToggleTracking,
                hasPermission = hasPermission,
                listenerConnected = listenerConnected,
                savedToday = todaySummary.total,
                onTrackingChanged = onTrackingChanged,
                onPermissionAction = onPermissionAction,
                onReconnect = onReconnect
            )
        }
        if (noAppsSelected) {
            item {
                StatusCard(
                    modifier = itemPadding,
                    title = stringResource(R.string.home_no_apps_selected_title),
                    description = stringResource(R.string.select_at_least_one_app_message),
                    icon = Icons.Default.Warning,
                    tone = StatusTone.Warning,
                    onClick = onManageApps,
                    trailing = {
                        Button(onClick = onManageApps) {
                            Text(stringResource(R.string.home_select_apps_action))
                        }
                    }
                )
            }
        }
        if (showBatteryWarning) {
            item {
                StatusCard(
                    modifier = itemPadding,
                    title = stringResource(R.string.battery_warning_title),
                    description = stringResource(R.string.battery_warning_description),
                    icon = Icons.Default.Warning,
                    tone = StatusTone.Warning,
                    onClick = onBatteryAction,
                    trailing = {
                        Button(onClick = onBatteryAction) { Text(stringResource(R.string.fix)) }
                    }
                )
            }
        }

        item { SectionHeader(text = stringResource(R.string.home_today_section), modifier = itemPadding) }
        item {
            Row(
                modifier = itemPadding.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = todaySummary.total.toString(),
                    label = stringResource(R.string.home_stat_notifications),
                    onClick = onInsights
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = todaySummary.activeApps.toString(),
                    label = stringResource(R.string.home_stat_apps),
                    onClick = onInsights
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = todaySummary.topAppLabel ?: stringResource(R.string.insights_none),
                    label = stringResource(R.string.home_stat_top_app),
                    onClick = onInsights
                )
            }
        }

        item {
            ManageAppsCard(
                modifier = itemPadding,
                selectedCount = state.selectedAppsCount,
                onClick = onManageApps
            )
        }

        item {
            Row(
                modifier = itemPadding.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    text = stringResource(R.string.home_recent_section),
                    modifier = Modifier.weight(1f)
                )
                if (recentNotifications.isNotEmpty()) {
                    TextButton(onClick = onSeeAllHistory) {
                        Text(stringResource(R.string.home_see_all))
                    }
                }
            }
        }
        if (recentNotifications.isEmpty()) {
            item {
                EmptyState(
                    modifier = itemPadding,
                    title = stringResource(R.string.home_recent_empty_title),
                    description = stringResource(R.string.home_recent_empty_description)
                )
            }
        } else {
            items(recentNotifications.size, key = { recentNotifications[it].id }) { index ->
                val notification = recentNotifications[index]
                NotificationHistoryCard(
                    notification = notification,
                    searchQuery = "",
                    onClick = { onNotificationClick(notification) }
                )
            }
        }

        if (showRateCard) {
            item {
                Box(modifier = itemPadding) {
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
}

@Composable
private fun CaptureStatusCard(
    modifier: Modifier,
    trackingEnabled: Boolean,
    hasPermission: Boolean,
    listenerConnected: Boolean,
    savedToday: Int,
    onTrackingChanged: (Boolean) -> Unit,
    onPermissionAction: () -> Unit,
    onReconnect: () -> Unit
) {
    val captureState = when {
        !hasPermission -> CaptureState.AccessNeeded
        !trackingEnabled -> CaptureState.Paused
        !listenerConnected -> CaptureState.Reconnecting
        else -> CaptureState.Recording
    }
    val title = when (captureState) {
        CaptureState.Recording -> stringResource(R.string.home_status_recording)
        CaptureState.Paused -> stringResource(R.string.tracking_paused)
        CaptureState.Reconnecting -> stringResource(R.string.home_status_reconnecting)
        CaptureState.AccessNeeded -> stringResource(R.string.tracking_permission_required)
    }
    val description = when (captureState) {
        CaptureState.Recording ->
            pluralStringResource(R.plurals.home_saved_today, savedToday, savedToday)
        CaptureState.Paused -> stringResource(R.string.tracking_paused_description)
        CaptureState.Reconnecting -> stringResource(R.string.home_status_reconnecting_description)
        CaptureState.AccessNeeded -> stringResource(R.string.tracking_permission_description)
    }
    val icon = when (captureState) {
        CaptureState.Recording -> Icons.Default.CheckCircle
        CaptureState.Paused -> Icons.Default.Info
        CaptureState.Reconnecting -> Icons.Default.Refresh
        CaptureState.AccessNeeded -> Icons.Default.Warning
    }
    val tone = when (captureState) {
        CaptureState.Recording -> StatusTone.Positive
        CaptureState.Paused -> StatusTone.Neutral
        CaptureState.Reconnecting -> StatusTone.Warning
        CaptureState.AccessNeeded -> StatusTone.Error
    }

    StatusCard(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        tone = tone,
        onClick = when (captureState) {
            CaptureState.Reconnecting -> onReconnect
            CaptureState.AccessNeeded -> onPermissionAction
            else -> null
        },
        trailing = {
            if (captureState == CaptureState.AccessNeeded) {
                Button(onClick = onPermissionAction) {
                    Text(stringResource(R.string.home_status_allow))
                }
            } else {
                Switch(
                    checked = trackingEnabled && hasPermission,
                    onCheckedChange = onTrackingChanged
                )
            }
        }
    )
}

@Composable
private fun ManageAppsCard(modifier: Modifier, selectedCount: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_manage_apps),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.home_selected_apps_count, selectedCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
