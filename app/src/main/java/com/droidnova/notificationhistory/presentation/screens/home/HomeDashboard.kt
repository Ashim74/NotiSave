package com.droidnova.notificationhistory.presentation.screens.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.components.AnimatedText
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.AppCardDefaults
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.ProBadge
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.ui.res.pluralStringResource
import com.droidnova.notificationhistory.presentation.components.SectionHeader
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.fitToWidth
import com.droidnova.notificationhistory.presentation.components.pulsing
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/** What the status card says about capture right now. */
private enum class CaptureState { Recording, Paused, Reconnecting, AccessNeeded }

/**
 * Home, top to bottom, laid out like Secret Calculator's vault home: a status card with the
 * switch, any one-line warnings, today's numbers as three tiles, four shortcut tiles, then the
 * deleted-messages row. Saved notifications themselves live in History, not here.
 */
@Composable
internal fun HomeDashboard(
    modifier: Modifier,
    state: SettingState,
    hasPermission: Boolean,
    listenerConnected: Boolean,
    todaySummary: HomeTodaySummary,
    showBatteryWarning: Boolean,
    @StringRes oemStepText: Int?,
    deletedCount: Int,
    isPremium: Boolean,
    onDeletedMessages: () -> Unit,
    onTrackingChanged: (Boolean) -> Unit,
    onPermissionAction: () -> Unit,
    onReconnect: () -> Unit,
    onSeeAllHistory: () -> Unit,
    onManageApps: () -> Unit,
    onInsights: () -> Unit,
    onTrash: () -> Unit,
    onBatteryAction: () -> Unit,
    onOemAction: () -> Unit,
    onReliabilityDone: (() -> Unit)?
) {
    val side = Modifier.padding(horizontal = Dimens.ScreenHorizontal)
    val noAppsSelected = state.userToggleTracking && state.selectedAppsCount == 0

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
    ) {
        item(key = "status") {
            CaptureStatusCard(
                modifier = side.appearIn(0),
                trackingEnabled = state.userToggleTracking,
                hasPermission = hasPermission,
                listenerConnected = listenerConnected,
                onTrackingChanged = onTrackingChanged,
                onPermissionAction = onPermissionAction,
                onReconnect = onReconnect
            )
        }
        if (noAppsSelected) {
            item(key = "no-apps") {
                WarningRow(
                    modifier = Modifier.animateItem().then(side).padding(top = 8.dp),
                    icon = Icons.Default.NotificationsOff,
                    title = stringResource(R.string.home_no_apps_selected_title),
                    action = stringResource(R.string.home_select_apps_action),
                    onAction = onManageApps
                )
            }
        }
        if (oemStepText != null) {
            item(key = "battery") {
                ReliabilityCard(
                    modifier = Modifier.animateItem().then(side).padding(top = 8.dp),
                    showBatteryStep = showBatteryWarning,
                    oemStepText = oemStepText,
                    onBatteryAction = onBatteryAction,
                    onOemAction = onOemAction,
                    onDone = onReliabilityDone
                )
            }
        } else if (showBatteryWarning) {
            item(key = "battery") {
                WarningRow(
                    modifier = Modifier.animateItem().then(side).padding(top = 8.dp),
                    icon = Icons.Default.BatteryAlert,
                    title = stringResource(R.string.battery_warning_title),
                    action = stringResource(R.string.fix),
                    onAction = onBatteryAction
                )
            }
        }

        item(key = "today") {
            Column(side.padding(top = 14.dp)) {
                SectionHeader(
                    text = stringResource(R.string.date_today),
                    first = true,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                TodayTiles(summary = todaySummary, onClick = onInsights)
            }
        }
        item(key = "shortcuts") {
            ShortcutTiles(
                modifier = side.padding(top = 8.dp),
                selectedApps = state.selectedAppsCount,
                onHistory = onSeeAllHistory,
                onInsights = onInsights,
                onManageApps = onManageApps,
                onTrash = onTrash
            )
        }
        item(key = "deleted") {
            ListRow(
                modifier = side.padding(top = 8.dp).appearIn(8),
                title = stringResource(R.string.deleted_title),
                value = if (deletedCount > 0) {
                    pluralStringResource(R.plurals.home_deleted_count, deletedCount, deletedCount)
                } else {
                    stringResource(R.string.home_deleted_none)
                },
                leading = { IconBadge(Icons.Outlined.DeleteForever, accent = AccentColors.Purple, size = 38.dp) },
                trailing = if (isPremium) null else ({ ProBadge() }),
                onClick = onDeletedMessages
            )
        }

    }
}

/**
 * The hero of Home: one card that says whether notifications are being saved, with the switch
 * right there. Its tone (teal / grey / blue / red) and icon carry most of the meaning; while
 * recording, the badge breathes so the screen feels alive.
 */
@Composable
private fun CaptureStatusCard(
    modifier: Modifier,
    trackingEnabled: Boolean,
    hasPermission: Boolean,
    listenerConnected: Boolean,
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
        CaptureState.Paused -> stringResource(R.string.home_status_paused)
        CaptureState.Reconnecting -> stringResource(R.string.home_status_reconnecting)
        CaptureState.AccessNeeded -> stringResource(R.string.home_status_access_needed)
    }
    val icon = when (captureState) {
        CaptureState.Recording -> ImageVector.vectorResource(R.drawable.ic_nh_recording)
        CaptureState.Paused -> ImageVector.vectorResource(R.drawable.ic_nh_paused)
        CaptureState.Reconnecting -> Icons.Default.Sync
        CaptureState.AccessNeeded -> Icons.Default.Warning
    }
    val scheme = MaterialTheme.colorScheme
    val accent by animateColorAsState(
        when (captureState) {
            CaptureState.Recording -> scheme.primary
            CaptureState.Paused -> scheme.outline
            CaptureState.Reconnecting -> scheme.tertiary
            CaptureState.AccessNeeded -> scheme.error
        },
        label = "statusAccent"
    )
    val container by animateColorAsState(
        when (captureState) {
            CaptureState.AccessNeeded -> scheme.errorContainer.copy(alpha = 0.55f)
            else -> accent.copy(alpha = 0.14f)
        },
        label = "statusContainer"
    )

    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = when (captureState) {
            CaptureState.Reconnecting -> onReconnect
            CaptureState.AccessNeeded -> onPermissionAction
            else -> null
        },
        colors = AppCardDefaults.colors(containerColor = container)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                // A soft halo behind the badge pulses while recording
                if (captureState == CaptureState.Recording) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .pulsing(minScale = 0.78f)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.18f))
                    )
                }
                AnimatedContent(
                    targetState = icon,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() },
                    label = "statusIcon"
                ) { target ->
                    IconBadge(target, accent = accent, containerColor = accent.copy(alpha = 0.22f), size = 44.dp)
                }
            }
            Spacer(Modifier.width(14.dp))
            AnimatedText(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
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
    }
}

/** Amber one-liner with its fix as a button; the button text says what happens. */
@Composable
private fun WarningRow(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    action: String,
    onAction: () -> Unit
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onAction,
        colors = AppCardDefaults.colors(
            containerColor = AccentColors.Amber.copy(alpha = 0.16f)
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon, accent = AccentColors.Amber, size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}

/**
 * [WarningRow] grown into a checklist when this phone's maker needs an extra step. Done only
 * dismisses that step: while battery optimization is still on, its row keeps the card up.
 * [onDone] is null when the step is back after Done because capture is down.
 */
@Composable
private fun ReliabilityCard(
    modifier: Modifier,
    showBatteryStep: Boolean,
    @StringRes oemStepText: Int,
    onBatteryAction: () -> Unit,
    onOemAction: () -> Unit,
    onDone: (() -> Unit)?
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        colors = AppCardDefaults.colors(
            containerColor = AccentColors.Amber.copy(alpha = 0.16f)
        )
    ) {
        Column(Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.BatteryAlert, accent = AccentColors.Amber, size = 34.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.battery_warning_title),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            if (showBatteryStep) {
                ReliabilityStep(
                    text = stringResource(R.string.reliability_step_battery),
                    action = stringResource(R.string.fix),
                    onAction = onBatteryAction
                )
            }
            ReliabilityStep(
                text = stringResource(oemStepText),
                action = stringResource(R.string.reliability_open),
                onAction = onOemAction
            )
            if (onDone != null) {
                TextButton(onClick = onDone, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.reliability_done))
                }
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ReliabilityStep(text: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.padding(start = 46.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalButton(onClick = onAction) { Text(action) }
    }
}

/** Today's numbers as three tiles: count, apps, and the busiest app's icon. */
@Composable
private fun TodayTiles(summary: HomeTodaySummary, onClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(
            modifier = Modifier.weight(1f).appearIn(1),
            label = stringResource(R.string.home_stat_notifications),
            icon = Icons.Outlined.Notifications,
            accent = AccentColors.Blue,
            onClick = onClick
        ) {
            AnimatedText(
                text = summary.total.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        StatTile(
            modifier = Modifier.weight(1f).appearIn(2),
            label = stringResource(R.string.home_stat_apps),
            icon = Icons.Outlined.Apps,
            accent = AccentColors.Teal,
            onClick = onClick
        ) {
            AnimatedText(
                text = summary.activeApps.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        StatTile(
            modifier = Modifier.weight(1f).appearIn(3),
            label = stringResource(R.string.home_stat_top_app),
            icon = null,
            accent = AccentColors.Purple,
            onClick = onClick
        ) {
            val topPackage = summary.topAppPackage
            if (topPackage != null) {
                HistoryAppIcon(
                    packageName = topPackage,
                    size = 30.dp,
                    contentDescription = summary.topAppLabel
                )
            } else {
                Text(
                    stringResource(R.string.insights_none),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    label: String,
    icon: ImageVector?,
    accent: Color,
    onClick: () -> Unit,
    value: @Composable () -> Unit
) {
    AppCard(modifier = modifier.semantics(mergeDescendants = true) {}, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Box(modifier = Modifier.height(34.dp), contentAlignment = Alignment.Center) { value() }
            }
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = fitToWidth(MaterialTheme.typography.labelMedium.fontSize)
            )
        }
    }
}

/** History, Insights, Apps and Trash as four colored tiles, like the vault's categories. */
@Composable
private fun ShortcutTiles(
    modifier: Modifier,
    selectedApps: Int,
    onHistory: () -> Unit,
    onInsights: () -> Unit,
    onManageApps: () -> Unit,
    onTrash: () -> Unit
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShortcutTile(
            Modifier.weight(1f).appearIn(4),
            ImageVector.vectorResource(R.drawable.ic_nh_history),
            stringResource(R.string.history_title),
            AccentColors.Blue,
            onHistory
        )
        ShortcutTile(
            Modifier.weight(1f).appearIn(5),
            ImageVector.vectorResource(R.drawable.ic_nh_insights),
            stringResource(R.string.insights_title),
            AccentColors.Orange,
            onInsights
        )
        ShortcutTile(
            Modifier.weight(1f).appearIn(6),
            ImageVector.vectorResource(R.drawable.ic_nh_apps_saved),
            stringResource(R.string.home_stat_apps),
            AccentColors.Green,
            onManageApps,
            badge = selectedApps
        )
        ShortcutTile(
            Modifier.weight(1f).appearIn(7),
            Icons.Default.Delete,
            stringResource(R.string.trash_title),
            AccentColors.Rose,
            onTrash
        )
    }
}

@Composable
private fun ShortcutTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    badge: Int? = null
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                IconBadge(icon, accent = accent, size = 40.dp)
                // A solid count on the badge's corner, like an app-icon badge
                if (badge != null) {
                    AnimatedText(
                        text = badge.toString(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 12.dp, y = (-4).dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent)
                            .padding(horizontal = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = fitToWidth(MaterialTheme.typography.labelLarge.fontSize)
            )
        }
    }
}
