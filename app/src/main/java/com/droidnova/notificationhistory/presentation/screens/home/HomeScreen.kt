package com.droidnova.notificationhistory.presentation.screens.home

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.billing.LocalPremiumBillingManager
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.presentation.components.NotificationDetailsDialog
import com.droidnova.notificationhistory.presentation.components.NotificationPermissionBottomSheet
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.HeaderButton
import com.droidnova.notificationhistory.presentation.components.rememberBatteryOptimizationState
import com.droidnova.notificationhistory.presentation.components.openNotificationAccessSettings
import com.droidnova.notificationhistory.presentation.dialogs.PremiumPurchaseBottomSheet
import com.droidnova.notificationhistory.presentation.dialogs.PremiumWelcomeDialog
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.navigation.navigateToTab
import com.droidnova.notificationhistory.service.ListenerReconnector
import com.droidnova.notificationhistory.utils.Analytics
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import kotlinx.coroutines.launch



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
    val batteryOptimization = rememberBatteryOptimizationState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val permissionSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.PartiallyExpanded }
    )

    LaunchedEffect(Unit) {
        viewmodel.getAllInstalledApps(context)
        Analytics.log(context, Analytics.HOME_OPEN)
    }
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
            showBatteryWarning = !batteryOptimization.isIgnored,
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
            onTrash = { navController.navigate(Screens.Trash.route) },
            onBatteryAction = batteryOptimization.requestExemption,
            onNotificationClick = { showDetailsDialog = it }
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
    ScreenTopBar(
        title = stringResource(R.string.app_name),
        actions = {
            // Premium is one tap away, like the star in Secret Calculator's vault header
            if (!isPremium) {
                HeaderButton(
                    icon = Icons.Outlined.WorkspacePremium,
                    contentDescription = stringResource(R.string.premium_menu_remove_ads),
                    onClick = onRemoveAds
                )
            }
            Box {
                HeaderButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.content_description_menu),
                    onClick = { showMenu = true },
                    modifier = Modifier.padding(end = 8.dp)
                )
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!isPremium) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.premium_menu_remove_ads)) },
                            leadingIcon = { Icon(Icons.Outlined.WorkspacePremium, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRemoveAds()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.about_title)) },
                        leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onAbout()
                        }
                    )
                }
            }
        }
    )
}

