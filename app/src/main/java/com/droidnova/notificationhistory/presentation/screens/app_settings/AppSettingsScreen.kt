package com.droidnova.notificationhistory.presentation.screens.app_settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.res.vectorResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
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
import com.droidnova.notificationhistory.data.backup.BackupFormatException
import com.droidnova.notificationhistory.data_shared.ThemeColor
import com.droidnova.notificationhistory.data_shared.ThemeMode
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.AppCardDefaults
import com.droidnova.notificationhistory.presentation.components.ChoicePill
import com.droidnova.notificationhistory.presentation.components.GradientBanner
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.ProBadge
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.SectionHeader
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.pulsing
import com.droidnova.notificationhistory.presentation.components.rememberBatteryOptimizationState
import com.droidnova.notificationhistory.presentation.components.openNotificationAccessSettings
import com.droidnova.notificationhistory.presentation.dialogs.PremiumPurchaseBottomSheet
import com.droidnova.notificationhistory.presentation.dialogs.PremiumWelcomeDialog
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import com.droidnova.notificationhistory.presentation.ui.theme.isDark
import com.droidnova.notificationhistory.presentation.ui.theme.scheme
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import java.time.LocalDate

/**
 * Settings in the Secret Calculator style: a Premium banner, then small labelled groups, each one
 * tinted card of one-line, icon-led rows. Choices (theme, retention) are pills right under their
 * row instead of dialogs.
 */
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
    val themeColor by mainViewModel.themeColor.collectAsState()
    val isTransferring by mainViewModel.isTransferring.collectAsState()
    val deletedAlertsEnabled by mainViewModel.deletedAlertsEnabled.collectAsState()
    val keywordAlerts by mainViewModel.keywordAlerts.collectAsState()
    val hiddenApps by mainViewModel.hiddenApps.collectAsState()

    // Backup, restore and export go through the system file picker, so no storage permission.
    fun report(@PluralsRes done: Int): (Result<Int>) -> Unit = { result ->
        val message = result.fold(
            onSuccess = { count -> context.resources.getQuantityString(done, count, count) },
            onFailure = {
                context.getString(if (it is BackupFormatException) R.string.restore_invalid else R.string.transfer_failed)
            }
        )
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { mainViewModel.backup(it, report(R.plurals.backup_done)) }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { mainViewModel.exportCsv(it, report(R.plurals.export_done)) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { mainViewModel.restore(it, report(R.plurals.restore_done)) }
    }
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
        val onRemoveAds: () -> Unit = {
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
        val openAccess = {
            mainViewModel.onPermissionSettingsOpened()
            openNotificationAccessSettings(context)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = Dimens.ScreenHorizontal, end = Dimens.ScreenHorizontal, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isPremium) {
                item(key = "premium") {
                    GradientBanner(
                        modifier = Modifier.appearIn(0),
                        icon = Icons.Filled.WorkspacePremium,
                        title = stringResource(R.string.premium_sheet_title),
                        subtitle = stringResource(R.string.premium_sheet_subtitle),
                        onClick = onRemoveAds
                    )
                }
            }

            item(key = "tracking") {
                TrackingRow(
                    modifier = Modifier.appearIn(1),
                    checked = state.userToggleTracking && hasPermission,
                    hasPermission = hasPermission,
                    onCheckedChange = { enabled ->
                        when {
                            !enabled -> mainViewModel.setToggleTracking(false)
                            hasPermission -> mainViewModel.onEnableClick()
                            else -> openAccess()
                        }
                    }
                )
            }

            item(key = "capture") {
                SettingsGroup(stringResource(R.string.settings_tracking_section), Modifier.appearIn(2)) {
                    SettingRow(
                        icon = Icons.Outlined.NotificationsActive,
                        accent = if (hasPermission) AccentColors.Blue else MaterialTheme.colorScheme.error,
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
                        onClick = if (hasPermission) null else openAccess
                    )
                    SettingRow(
                        icon = ImageVector.vectorResource(R.drawable.ic_nh_apps_saved),
                        accent = AccentColors.Purple,
                        title = stringResource(R.string.home_manage_apps),
                        value = pluralStringResource(
                            R.plurals.settings_apps_count,
                            state.selectedAppsCount,
                            state.selectedAppsCount
                        ),
                        onClick = { navController.navigate(Screens.ManageNotifications.route) }
                    )
                    SettingRow(
                        icon = if (batteryOptimization.isIgnored) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryAlert,
                        accent = if (batteryOptimization.isIgnored) AccentColors.Green else AccentColors.Amber,
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
                        onClick = if (batteryOptimization.isIgnored) null else batteryOptimization.requestExemption
                    )
                }
            }

            item(key = "history") {
                SettingsGroup(stringResource(R.string.settings_history_section), Modifier.appearIn(3)) {
                    SettingRow(
                        icon = Icons.Outlined.AutoDelete,
                        accent = AccentColors.Teal,
                        title = stringResource(R.string.settings_retention),
                        value = stringResource(R.string.set_retention_summary)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 62.dp, end = 8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(RETENTION_OPTIONS_DAYS) { days ->
                            val selected = days == state.historyRetentionDays
                            val locked = !isPremium && !selected && days in PRO_RETENTION_DAYS
                            ChoicePill(
                                label = retentionShortLabel(days),
                                selected = selected,
                                locked = locked,
                                onClick = { if (locked) onRemoveAds() else mainViewModel.updateHistoryRetentionDays(days) }
                            )
                        }
                    }
                    SettingRow(
                        icon = Icons.Outlined.DeleteOutline,
                        accent = AccentColors.Rose,
                        title = stringResource(R.string.trash_title),
                        onClick = { navController.navigate(Screens.Trash.route) }
                    )
                    SettingRow(
                        icon = Icons.Outlined.DeleteSweep,
                        accent = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.settings_clear_history),
                        titleColor = MaterialTheme.colorScheme.error,
                        showChevron = false,
                        onClick = { showClearConfirmation = true }
                    )
                }
            }

            item(key = "backup") {
                val busyValue = if (isTransferring) stringResource(R.string.settings_transfer_running) else null
                SettingsGroup(stringResource(R.string.settings_backup_section), Modifier.appearIn(4)) {
                    SettingRow(
                        icon = Icons.Outlined.CloudUpload,
                        accent = AccentColors.Blue,
                        title = stringResource(R.string.settings_backup),
                        value = busyValue,
                        locked = !isPremium,
                        onClick = { if (isPremium) backupLauncher.launch(backupFileName("json")) else onRemoveAds() }
                    )
                    SettingRow(
                        icon = Icons.Outlined.SettingsBackupRestore,
                        accent = AccentColors.Green,
                        title = stringResource(R.string.settings_restore),
                        locked = !isPremium,
                        onClick = { if (isPremium) restoreLauncher.launch(BACKUP_MIME_TYPES) else onRemoveAds() }
                    )
                    SettingRow(
                        icon = Icons.Outlined.TableChart,
                        accent = AccentColors.Teal,
                        title = stringResource(R.string.settings_export_csv),
                        locked = !isPremium,
                        onClick = { if (isPremium) csvLauncher.launch(backupFileName("csv")) else onRemoveAds() }
                    )
                }
            }

            item(key = "alerts") {
                SettingsGroup(stringResource(R.string.settings_alerts_section), Modifier.appearIn(4)) {
                    SettingRow(
                        icon = Icons.Outlined.DeleteForever,
                        accent = AccentColors.Purple,
                        title = stringResource(R.string.deleted_title),
                        onClick = { navController.navigate(Screens.DeletedMessages.route) }
                    )
                    SettingRow(
                        icon = Icons.Outlined.NotificationImportant,
                        accent = AccentColors.Rose,
                        title = stringResource(R.string.settings_deleted_alerts),
                        locked = !isPremium,
                        trailing = { Switch(checked = deletedAlertsEnabled, onCheckedChange = null) },
                        onClick = {
                            if (isPremium) mainViewModel.setDeletedAlertsEnabled(!deletedAlertsEnabled) else onRemoveAds()
                        }
                    )
                    SettingRow(
                        icon = Icons.Outlined.Sell,
                        accent = AccentColors.Amber,
                        title = stringResource(R.string.settings_keyword_alerts),
                        value = if (isPremium && keywordAlerts.isNotEmpty()) keywordAlerts.size.toString() else null,
                        locked = !isPremium,
                        onClick = {
                            if (isPremium) navController.navigate(Screens.KeywordAlerts.route) else onRemoveAds()
                        }
                    )
                }
            }

            item(key = "general") {
                SettingsGroup(stringResource(R.string.settings_general_section), Modifier.appearIn(4)) {
                    SettingRow(
                        icon = Icons.Outlined.Palette,
                        accent = AccentColors.Orange,
                        title = stringResource(R.string.settings_theme)
                    )
                    SingleChoiceSegmentedButtonRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                    ) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = mode == themeMode,
                                onClick = { mainViewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = mode == themeMode) {
                                        Icon(mode.icon(), contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize))
                                    }
                                }
                            ) {
                                Text(stringResource(mode.shortLabelRes()), maxLines = 1)
                            }
                        }
                    }
                    // Second line: the color. Pro colors stay visible but locked, and open the
                    // Premium sheet instead of applying.
                    val dark = themeMode.isDark()
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(ThemeColor.entries, key = { it.storageKey }) { color ->
                            val locked = color.pro && !isPremium
                            ChoicePill(
                                label = stringResource(color.labelRes()),
                                selected = color == themeColor,
                                dotColor = color.scheme(dark).primary,
                                locked = locked,
                                showCheck = true,
                                onClick = { if (locked) onRemoveAds() else mainViewModel.setThemeColor(color) }
                            )
                        }
                    }
                    SettingRow(
                        icon = Icons.Outlined.Lock,
                        accent = AccentColors.Blue,
                        title = stringResource(R.string.settings_app_lock),
                        value = stringResource(
                            if (appLockEnabled) R.string.settings_value_on else R.string.settings_value_off
                        ),
                        onClick = { navController.navigate(Screens.AppLock.route) }
                    )
                    SettingRow(
                        icon = Icons.Outlined.VisibilityOff,
                        accent = AccentColors.Purple,
                        title = stringResource(R.string.settings_hidden_apps),
                        value = if (isPremium && hiddenApps.isNotEmpty()) hiddenApps.size.toString() else null,
                        locked = !isPremium,
                        onClick = { if (isPremium) navController.navigate(Screens.HiddenApps.route) else onRemoveAds() }
                    )
                }
            }

            item(key = "app") {
                SettingsGroup(stringResource(R.string.settings_app_section), Modifier.appearIn(5)) {
                    if (isPremium) {
                        SettingRow(
                            icon = Icons.Default.Star,
                            accent = AccentColors.Amber,
                            title = stringResource(R.string.settings_premium_active)
                        )
                    }
                    // UMP mandates a privacy-options entry point while consent is revocable (EEA/UK/US states).
                    if (privacyOptionsRequired) {
                        SettingRow(
                            icon = Icons.Outlined.PrivacyTip,
                            accent = AccentColors.Teal,
                            title = stringResource(R.string.settings_privacy_options),
                            onClick = { activity?.let { consentManager.showPrivacyOptionsForm(it) } }
                        )
                    }
                    SettingRow(
                        icon = Icons.Outlined.Feedback,
                        accent = AccentColors.Green,
                        title = stringResource(R.string.settings_feedback),
                        onClick = { IntentUtil.sendSupportMail(context, isBug = false) }
                    )
                    SettingRow(
                        icon = Icons.Outlined.Info,
                        accent = AccentColors.Purple,
                        title = stringResource(R.string.about_title),
                        onClick = { navController.navigate(Screens.AboutScreen.route) }
                    )
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            icon = {
                IconBadge(
                    Icons.Default.Delete,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.error,
                    size = 52.dp
                )
            },
            title = { Text(stringResource(R.string.clear_history_confirmation_title), textAlign = TextAlign.Center) },
            text = { Text(stringResource(R.string.clear_history_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        mainViewModel.clearAllHistory()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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

/**
 * The capture switch as one highlighted row: the whole row toggles, the badge breathes while
 * notifications are being saved, and the card fills with the accent when on.
 */
@Composable
private fun TrackingRow(
    checked: Boolean,
    hasPermission: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (checked) scheme.primaryContainer else AppCardDefaults.containerColor(),
        label = "trackingContainer"
    )
    val shape = MaterialTheme.shapes.medium
    ListRow(
        modifier = modifier
            .clip(shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        shape = shape,
        title = stringResource(
            when {
                !hasPermission -> R.string.home_status_access_needed
                checked -> R.string.home_status_recording
                else -> R.string.home_status_paused
            }
        ),
        containerColor = container,
        titleColor = if (checked) scheme.onPrimaryContainer else scheme.onSurface,
        leading = {
            IconBadge(
                ImageVector.vectorResource(R.drawable.ic_nh_recording),
                accent = if (hasPermission) scheme.primary else scheme.error,
                modifier = if (checked) Modifier.pulsing() else Modifier
            )
        },
        trailing = { Switch(checked = checked, onCheckedChange = null) }
    )
}

/** A small uppercase label over one tinted card that holds a group of rows. */
@Composable
private fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        SectionHeader(title, Modifier.padding(bottom = 6.dp), first = true)
        AppCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .animateContentSize(),
                content = content
            )
        }
    }
}

/** One setting on one line inside a [SettingsGroup]: colored badge, title, optional value. */
@Composable
private fun SettingRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    value: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean? = null,
    /** A Premium feature the user doesn't have: a PRO tag replaces the trailing content. */
    locked: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ListRow(
        title = title,
        value = value,
        valueColor = valueColor,
        titleColor = titleColor,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.Transparent,
        leading = { IconBadge(icon, accent = accent, modifier = Modifier.size(38.dp)) },
        trailing = if (locked) ({ ProBadge() }) else trailing,
        onClick = onClick,
        showChevron = showChevron ?: (onClick != null && !locked && trailing == null)
    )
}

/** Some file managers label JSON as plain text or binary, so the picker accepts those too. */
private val BACKUP_MIME_TYPES = arrayOf("application/json", "application/octet-stream", "text/plain")

/** e.g. NotiSave-2026-10-03.json */
private fun backupFileName(extension: String): String =
    "NotiSave-${LocalDate.now()}.$extension"

/** 0 means "never delete"; see the retention gate in the listener and MainViewModel. */
private val RETENTION_OPTIONS_DAYS = listOf(7, 14, 30, 90, 365, 0)

/** Keeping more than 90 days is Premium. A value chosen before that rule stays in effect. */
private val PRO_RETENTION_DAYS = setOf(365, 0)

@Composable
private fun retentionShortLabel(days: Int): String = when {
    days <= 0 -> stringResource(R.string.set_retention_forever_short)
    days == 365 -> stringResource(R.string.set_retention_year_short)
    else -> pluralStringResource(R.plurals.settings_retention_days, days, days)
}

@StringRes
private fun ThemeMode.shortLabelRes(): Int = when (this) {
    ThemeMode.System -> R.string.set_theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}

@StringRes
private fun ThemeColor.labelRes(): Int = when (this) {
    ThemeColor.Teal -> R.string.theme_color_teal
    ThemeColor.Blue -> R.string.theme_color_blue
    ThemeColor.Forest -> R.string.theme_color_forest
    ThemeColor.Purple -> R.string.theme_color_purple
    ThemeColor.Rose -> R.string.theme_color_rose
    ThemeColor.Sunset -> R.string.theme_color_sunset
    ThemeColor.Gold -> R.string.theme_color_gold
}

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.System -> Icons.Outlined.BrightnessAuto
    ThemeMode.Light -> Icons.Outlined.LightMode
    ThemeMode.Dark -> Icons.Outlined.DarkMode
}
