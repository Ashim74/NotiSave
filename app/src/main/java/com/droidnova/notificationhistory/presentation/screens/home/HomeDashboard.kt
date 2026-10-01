package com.droidnova.notificationhistory.presentation.screens.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.NotificationHistoryCard
import com.droidnova.notificationhistory.presentation.components.SectionHeader
import com.droidnova.notificationhistory.presentation.components.fitToWidth
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/** What the status row says about capture right now. */
private enum class CaptureState { Recording, Paused, Reconnecting, AccessNeeded }

/**
 * Home, top to bottom: one status line with the switch, any one-line warnings, today's numbers
 * in a single strip, Manage apps, then recent notifications as one grouped block.
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        item(key = "status") {
            CaptureStatusRow(
                modifier = side,
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
                    title = stringResource(R.string.battery_warning_title),
                    action = stringResource(R.string.fix),
                    onAction = onBatteryAction
                )
            }
        }

        item(key = "today") {
            TodayStrip(
                modifier = side.padding(top = 12.dp),
                summary = todaySummary,
                onClick = onInsights
            )
        }
        item(key = "manage-apps") {
            ListRow(
                modifier = side.padding(top = 8.dp),
                title = stringResource(R.string.home_manage_apps),
                leading = { IconBadge(Icons.Default.Settings) },
                trailing = { CountChip(state.selectedAppsCount) },
                showChevron = true,
                onClick = onManageApps
            )
        }

        item(key = "recent-header") {
            Row(
                modifier = side
                    .fillMaxWidth()
                    .padding(top = 12.dp),
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
                }
            }
        }
        if (recentNotifications.isEmpty()) {
            item(key = "recent-empty") {
                EmptyState(
                    modifier = side,
                    title = stringResource(R.string.home_recent_empty_title)
                )
            }
        } else {
            itemsIndexed(recentNotifications, key = { _, item -> item.id }) { index, notification ->
                NotificationHistoryCard(
                    modifier = Modifier.animateItem(),
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
                Box(modifier = side.padding(top = 16.dp)) {
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
 * One line that says whether notifications are being saved, with the switch right there.
 * The tone (green / grey / amber / red) carries most of the meaning, so the text stays short.
 */
@Composable
private fun CaptureStatusRow(
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
        CaptureState.Recording -> Icons.Default.CheckCircle
        CaptureState.Paused -> Icons.Default.Info
        CaptureState.Reconnecting -> Icons.Default.Refresh
        CaptureState.AccessNeeded -> Icons.Default.Warning
    }
    val scheme = MaterialTheme.colorScheme
    // Container/on-container pairs keep the text readable in light, dark and dynamic themes.
    val (container, content) = when (captureState) {
        CaptureState.Recording -> scheme.primaryContainer to scheme.onPrimaryContainer
        CaptureState.Paused -> scheme.surfaceVariant to scheme.onSurfaceVariant
        CaptureState.Reconnecting -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        CaptureState.AccessNeeded -> scheme.errorContainer to scheme.onErrorContainer
    }
    val animatedContainer by animateColorAsState(container, label = "statusContainer")
    val animatedContent by animateColorAsState(content, label = "statusContent")

    ListRow(
        modifier = modifier,
        title = title,
        containerColor = animatedContainer,
        titleColor = animatedContent,
        leading = {
            Crossfade(targetState = icon, label = "statusIcon") {
                Icon(it, contentDescription = null, tint = animatedContent, modifier = Modifier.size(28.dp))
            }
        },
        onClick = when (captureState) {
            CaptureState.Reconnecting -> onReconnect
            CaptureState.AccessNeeded -> onPermissionAction
            else -> null
        },
        showChevron = false,
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

/** Amber one-liner with its fix as a button; the button text says what happens. */
@Composable
private fun WarningRow(modifier: Modifier, title: String, action: String, onAction: () -> Unit) {
    ListRow(
        modifier = modifier,
        title = title,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        titleColor = MaterialTheme.colorScheme.onTertiaryContainer,
        leading = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
            )
        },
        onClick = onAction,
        trailing = {
            FilledTonalButton(onClick = onAction) { Text(action) }
        }
    )
}

/** Today's numbers in one compact strip: count, apps, and the busiest app's icon. */
@Composable
private fun TodayStrip(modifier: Modifier, summary: HomeTodaySummary, onClick: () -> Unit) {
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(label = stringResource(R.string.home_stat_notifications)) {
                AnimatedText(
                    text = summary.total.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatColumn(label = stringResource(R.string.home_stat_apps)) {
                AnimatedText(
                    text = summary.activeApps.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatColumn(label = stringResource(R.string.home_stat_top_app)) {
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
}

@Composable
private fun RowScope.StatColumn(label: String, value: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.Center) { value() }
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = fitToWidth(MaterialTheme.typography.labelMedium.fontSize)
        )
    }
}

/** Small pill with a number, e.g. how many apps are being saved. */
@Composable
private fun CountChip(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        AnimatedText(
            text = count.toString(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
