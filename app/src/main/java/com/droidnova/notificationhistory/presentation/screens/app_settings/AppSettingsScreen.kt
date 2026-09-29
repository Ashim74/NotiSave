package com.droidnova.notificationhistory.presentation.screens.app_settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.droidnova.notificationhistory.presentation.components.isBatteryOptimizationIgnored
import com.droidnova.notificationhistory.presentation.components.openBatteryOptimizationSettings
import com.droidnova.notificationhistory.presentation.components.openNotificationAccessSettings
import com.droidnova.notificationhistory.presentation.dialogs.PremiumPurchaseBottomSheet
import com.droidnova.notificationhistory.presentation.dialogs.PremiumWelcomeDialog
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(mainViewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by mainViewModel.settingState.collectAsState()
    val hasPermission by mainViewModel.hasNotificationAccess.collectAsState()
    val isPremium by mainViewModel.isPremium.collectAsState()
    val showPremiumWelcome by mainViewModel.showPremiumWelcome.collectAsState()
    val billingManager = LocalPremiumBillingManager.current
    val productDetails = billingManager?.productDetails?.collectAsState()?.value
    val isFetchingPrice = billingManager?.isFetchingProductDetails?.collectAsState()?.value ?: false
    val isPurchaseInProgress = billingManager?.isPurchaseInProgress?.collectAsState()?.value ?: false
    val priceLabel = productDetails?.oneTimePurchaseOfferDetailsList
        ?.firstOrNull()
        ?.formattedPrice
    var batteryIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showPurchaseSheet by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mainViewModel.onResume()
                batteryIgnored = isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(billingManager) {
        billingManager?.errors?.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(isPremium) { if (isPremium) showPurchaseSheet = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SettingsSectionTitle(stringResource(R.string.settings_tracking_section)) }
            item {
                TrackingSettingsCard(
                    checked = state.userToggleTracking && hasPermission,
                    hasPermission = hasPermission,
                    onCheckedChange = { enabled ->
                        when {
                            !enabled -> mainViewModel.setToggleTracking(false)
                            hasPermission -> mainViewModel.onEnableClick()
                            else -> {
                                mainViewModel.onPermissionSettingsOpened()
                                openNotificationAccessSettings(context)
                            }
                        }
                    }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notification_access),
                    supportingText = if (hasPermission) {
                        stringResource(R.string.settings_permission_granted)
                    } else {
                        stringResource(R.string.settings_permission_required)
                    },
                    onClick = if (hasPermission) null else {
                        {
                            mainViewModel.onPermissionSettingsOpened()
                            openNotificationAccessSettings(context)
                        }
                    }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Settings,
                    title = stringResource(R.string.home_manage_apps),
                    supportingText = stringResource(
                        R.string.home_selected_apps_count,
                        state.selectedAppsCount
                    ),
                    onClick = { navController.navigate(Screens.ManageNotifications.route) }
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_history_section)) }
            item {
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.home_view_history),
                    supportingText = stringResource(R.string.settings_open_history_description),
                    onClick = { navController.navigate(Screens.History.route) }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.trash_title),
                    supportingText = stringResource(R.string.settings_trash_description),
                    onClick = { navController.navigate(Screens.Trash.route) }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.settings_clear_history),
                    supportingText = stringResource(R.string.settings_clear_history_description),
                    isDestructive = true,
                    onClick = { showClearConfirmation = true }
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_reliability_section)) }
            item {
                SettingsRow(
                    icon = if (batteryIgnored) Icons.Default.CheckCircle else Icons.Default.Warning,
                    title = stringResource(R.string.settings_battery_optimization),
                    supportingText = if (batteryIgnored) {
                        stringResource(R.string.settings_battery_ready)
                    } else {
                        stringResource(R.string.settings_battery_action)
                    },
                    onClick = if (batteryIgnored) null else {
                        { openBatteryOptimizationSettings(context) }
                    }
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_premium_app_section)) }
            item {
                SettingsRow(
                    icon = Icons.Default.Star,
                    title = if (isPremium) {
                        stringResource(R.string.settings_premium_active)
                    } else {
                        stringResource(R.string.premium_menu_remove_ads)
                    },
                    supportingText = if (isPremium) {
                        stringResource(R.string.premium_menu_unlocked_subtitle)
                    } else {
                        stringResource(R.string.premium_menu_remove_ads_subtitle)
                    },
                    onClick = if (isPremium) null else {
                        {
                            if (billingManager != null) {
                                mainViewModel.onRemoveAdsClicked()
                                showPurchaseSheet = true
                                billingManager.queryProductDetails()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.billing_unavailable),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_title),
                    supportingText = stringResource(R.string.app_about_description),
                    onClick = { navController.navigate(Screens.AboutScreen.route) }
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Default.Share,
                    title = stringResource(R.string.settings_feedback),
                    supportingText = stringResource(R.string.settings_feedback_description),
                    onClick = { IntentUtil.sendSupportMail(context, isBug = false) }
                )
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.clear_history_confirmation_title)) },
            text = { Text(stringResource(R.string.clear_history_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        mainViewModel.clearAllHistory()
                    }
                ) { Text(stringResource(R.string.move_to_trash)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showPurchaseSheet && !isPremium) {
        PremiumPurchaseBottomSheet(
            priceLabel = priceLabel,
            isLoading = isFetchingPrice || isPurchaseInProgress,
            onPurchaseClick = {
                if (activity != null) billingManager?.launchPurchaseFlow(activity)
            },
            onDismiss = { showPurchaseSheet = false }
        )
    }
    if (showPremiumWelcome) {
        PremiumWelcomeDialog(
            onDismiss = mainViewModel::dismissPremiumWelcome,
            onRestart = {
                mainViewModel.dismissPremiumWelcome()
                IntentUtil.restartApp(context)
            },
            onOpenInstagram = { IntentUtil.openInstagram(context) },
            onOpenWhatsapp = { IntentUtil.openWhatsApp(context) }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TrackingSettingsCard(
    checked: Boolean,
    hasPermission: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (checked) stringResource(R.string.tracking_active)
                    else if (hasPermission) stringResource(R.string.tracking_paused)
                    else stringResource(R.string.tracking_permission_required),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.settings_tracking_description),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    supportingText: String,
    onClick: (() -> Unit)?,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDestructive) contentColor
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onClick != null) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            content = { content() }
        )
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            content = { content() }
        )
    }
}
