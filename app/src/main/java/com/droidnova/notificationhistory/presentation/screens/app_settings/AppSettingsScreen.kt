package com.droidnova.notificationhistory.presentation.screens.app_settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.ads.AdsConsentManager
import com.droidnova.notificationhistory.billing.LocalPremiumBillingManager
import com.droidnova.notificationhistory.core.lock.AppLock
import com.droidnova.notificationhistory.data_shared.ThemeMode
import com.droidnova.notificationhistory.presentation.components.AnimatedText
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.AppCardDefaults
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListGroup
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.SectionHeader
import com.droidnova.notificationhistory.presentation.components.rememberBatteryOptimizationState
import com.droidnova.notificationhistory.presentation.components.openNotificationAccessSettings
import com.droidnova.notificationhistory.presentation.dialogs.PremiumPurchaseBottomSheet
import com.droidnova.notificationhistory.presentation.dialogs.PremiumWelcomeDialog
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.navigation.navigateToTab
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
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
    val batteryOptimization = rememberBatteryOptimizationState()
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showPurchaseSheet by remember { mutableStateOf(false) }
    val consentManager = remember { AdsConsentManager.getInstance(context) }
    val privacyOptionsRequired by consentManager.privacyOptionsRequired.collectAsState()
    val themeMode by mainViewModel.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    val appLockEnabled by remember { AppLock.get(context).isEnabled }.collectAsState(initial = false)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mainViewModel.onResume()
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
            ScreenTopBar(title = stringResource(R.string.settings_title))
        }
    ) { padding ->
        val onRemoveAds: (() -> Unit)? = if (isPremium) null else {
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
        val openAccess = {
            mainViewModel.onPermissionSettingsOpened()
            openNotificationAccessSettings(context)
        }
        val errorBadge: @Composable (ImageVector) -> Unit = {
            IconBadge(
                it,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        // Grouped blocks with one-word values instead of a card and a sentence per setting.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "tracking") {
                val checked = state.userToggleTracking && hasPermission
                val container by animateColorAsState(
                    if (checked) MaterialTheme.colorScheme.primaryContainer
                    else AppCardDefaults.containerColor(),
                    label = "trackingContainer"
                )
                ListRow(
                    title = stringResource(
                        when {
                            !hasPermission -> R.string.home_status_access_needed
                            checked -> R.string.home_status_recording
                            else -> R.string.home_status_paused
                        }
                    ),
                    containerColor = container,
                    leading = { IconBadge(Icons.Default.Notifications) },
                    trailing = {
                        Switch(
                            checked = checked,
                            onCheckedChange = { enabled ->
                                when {
                                    !enabled -> mainViewModel.setToggleTracking(false)
                                    hasPermission -> mainViewModel.onEnableClick()
                                    else -> openAccess()
                                }
                            }
                        )
                    }
                )
            }

            item(key = "capture") {
                ListGroup(title = stringResource(R.string.settings_tracking_section)) {
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.settings_notification_access),
                            value = stringResource(
                                if (hasPermission) R.string.settings_permission_granted
                                else R.string.settings_value_needed
                            ),
                            valueColor = if (hasPermission) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            leading = { IconBadge(Icons.Default.Notifications) },
                            onClick = if (hasPermission) null else openAccess
                        )
                    }
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.home_manage_apps),
                            value = pluralStringResource(
                                R.plurals.settings_apps_count,
                                state.selectedAppsCount,
                                state.selectedAppsCount
                            ),
                            leading = { IconBadge(ImageVector.vectorResource(R.drawable.ic_apps)) },
                            onClick = { navController.navigate(Screens.ManageNotifications.route) }
                        )
                    }
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.settings_battery_optimization),
                            value = stringResource(
                                if (batteryOptimization.isIgnored) R.string.settings_battery_unrestricted
                                else R.string.settings_battery_restricted
                            ),
                            valueColor = if (batteryOptimization.isIgnored) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            leading = {
                                IconBadge(
                                    if (batteryOptimization.isIgnored) Icons.Default.CheckCircle
                                    else Icons.Default.Warning
                                )
                            },
                            onClick = if (batteryOptimization.isIgnored) null else batteryOptimization.requestExemption
                        )
                    }
                }
            }

            item(key = "history") {
                ListGroup(title = stringResource(R.string.settings_history_section)) {
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.settings_retention),
                            value = retentionLabel(state.historyRetentionDays),
                            leading = { IconBadge(ImageVector.vectorResource(R.drawable.ic_history)) },
                            onClick = { showRetentionDialog = true }
                        )
                    }
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.trash_title),
                            leading = { IconBadge(Icons.Default.Delete) },
                            onClick = { navController.navigate(Screens.Trash.route) }
                        )
                    }
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.settings_clear_history),
                            titleColor = MaterialTheme.colorScheme.error,
                            leading = { errorBadge(Icons.Default.Delete) },
                            showChevron = false,
                            onClick = { showClearConfirmation = true }
                        )
                    }
                }
            }

            item(key = "general") {
                ListGroup(title = stringResource(R.string.settings_general_section)) {
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.settings_theme),
                            value = stringResource(themeMode.labelRes()),
                            leading = { IconBadge(ImageVector.vectorResource(R.drawable.ic_theme)) },
                            onClick = { showThemeDialog = true }
                        )
                    }
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.settings_app_lock),
                            value = stringResource(
                                if (appLockEnabled) R.string.settings_value_on else R.string.settings_value_off
                            ),
                            leading = { IconBadge(Icons.Default.Lock) },
                            onClick = { navController.navigate(Screens.AppLock.route) }
                        )
                    }
                }
            }

            item(key = "app") {
                ListGroup(title = stringResource(R.string.settings_app_section)) {
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(
                                if (isPremium) R.string.settings_premium_active
                                else R.string.premium_menu_remove_ads
                            ),
                            leading = {
                                IconBadge(
                                    Icons.Default.Star,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            },
                            onClick = onRemoveAds
                        )
                    }
                    // UMP mandates a privacy-options entry point while consent is revocable (EEA/UK/US states).
                    if (privacyOptionsRequired) {
                        row { shape ->
                            ListRow(
                                shape = shape,
                                title = stringResource(R.string.settings_privacy_options),
                                leading = { IconBadge(Icons.Default.AccountBox) },
                                onClick = { activity?.let { consentManager.showPrivacyOptionsForm(it) } }
                            )
                        }
                    }
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.settings_feedback),
                            leading = { IconBadge(Icons.Default.Email) },
                            onClick = { IntentUtil.sendSupportMail(context, isBug = false) }
                        )
                    }
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(R.string.about_title),
                            leading = { IconBadge(Icons.Outlined.Info) },
                            onClick = { navController.navigate(Screens.AboutScreen.route) }
                        )
                    }
                }
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
    if (showRetentionDialog) {
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = { Text(stringResource(R.string.settings_retention_dialog_title)) },
            text = {
                Column {
                    RETENTION_OPTIONS_DAYS.forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    mainViewModel.updateHistoryRetentionDays(days)
                                    showRetentionDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = days == state.historyRetentionDays,
                                onClick = {
                                    mainViewModel.updateHistoryRetentionDays(days)
                                    showRetentionDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(retentionLabel(days))
                        }
                    }
                    Text(
                        text = stringResource(R.string.settings_retention_description),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetentionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme_dialog_title)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    mainViewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == themeMode,
                                onClick = {
                                    mainViewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(mode.labelRes()))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
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

/** 0 means "never delete"; see the retention gate in the listener and MainViewModel. */
private val RETENTION_OPTIONS_DAYS = listOf(7, 14, 30, 90, 0)

@Composable
private fun retentionLabel(days: Int): String =
    if (days <= 0) {
        stringResource(R.string.settings_retention_forever)
    } else {
        pluralStringResource(R.plurals.settings_retention_days, days, days)
    }

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}
