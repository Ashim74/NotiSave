package com.droidnova.notificationhistory.presentation.screens.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.droidnova.notificationhistory.component.RateUsCard
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.components.AnimatedText
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.AppCardDefaults
import com.droidnova.notificationhistory.presentation.components.CountPill
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.NotificationHistoryCard
import com.droidnova.notificationhistory.presentation.components.SectionHeader
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.fitToWidth
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.components.pulsing
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/** What the status card says about capture right now. */
private enum class CaptureState { Recording, Paused, Reconnecting, AccessNeeded }

/**
 * Home, top to bottom, laid out like Secret Calculator's vault home: a status card with the
 * switch, any one-line warnings, today's numbers as three tiles, four shortcut tiles, then the
 * most recent notifications as one grouped block.
 */
@Composable
internal fun HomeDashboard(
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
    onTrash: () -> Unit,
    onBatteryAction: () -> Unit,
    onNotificationClick: (NotificationModel) -> Unit,
    onRateCancel: () -> Unit,
    onRateConfirmed: () -> Unit,
    onRated: (Int) -> Unit,
    onFeedback: () -> Unit
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
        if (showBatteryWarning) {
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

        item(key = "recent-header") {
            Row(
                modifier = side
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    text = stringResource(R.string.home_recent_section),
                    modifier = Modifier.weight(1f),
                    first = true
                )
                if (recentNotifications.isNotEmpty()) {
                    TextButton(onClick = onSeeAllHistory) {
                        Text(stringResource(R.string.home_see_all))
                    }
                } else {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
        if (recentNotifications.isEmpty()) {
            item(key = "recent-empty") {
                EmptyState(
                    modifier = side,
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(R.string.home_recent_empty_title)
                )
            }
        } else {
            itemsIndexed(recentNotifications, key = { _, item -> item.id }) { index, notification ->
                NotificationHistoryCard(
                    modifier = Modifier
                        .animateItem()
                        .appearIn(6 + index),
                    notification = notification,
                    searchQuery = "",
                    onClick = { onNotificationClick(notification) },
                    shape = groupedShape(index, recentNotifications.size),
                    spacing = GroupRowGap
                )
            }
        }

        if (showRateCard) {
            item(key = "rate") {
                Box(modifier = side.padding(top = 12.dp)) {
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
        CaptureState.Recording -> Icons.Default.Notifications
        CaptureState.Paused -> Icons.Default.Pause
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
            ImageVector.vectorResource(R.drawable.ic_history),
            stringResource(R.string.history_title),
            AccentColors.Blue,
            onHistory
        )
        ShortcutTile(
            Modifier.weight(1f).appearIn(5),
            ImageVector.vectorResource(R.drawable.ic_insights),
            stringResource(R.string.insights_title),
            AccentColors.Orange,
            onInsights
        )
        ShortcutTile(
            Modifier.weight(1f).appearIn(6),
            ImageVector.vectorResource(R.drawable.ic_apps),
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
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconBadge(icon, accent = accent, size = 40.dp)
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = fitToWidth(MaterialTheme.typography.labelLarge.fontSize)
                )
            }
            if (badge != null) {
                CountPill(
                    count = badge,
                    color = accent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 5.dp, end = 5.dp)
                )
            }
        }
    }
}
