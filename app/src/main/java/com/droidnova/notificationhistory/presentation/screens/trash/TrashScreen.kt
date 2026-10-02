package com.droidnova.notificationhistory.presentation.screens.trash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.mapper.toReadableTime
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.components.ActionTile
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HeaderButton
import com.droidnova.notificationhistory.presentation.components.HistoryLoadingState
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.NotificationHistoryCard
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.dayGroupedItems
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(mainViewModel: MainViewModel, navController: NavController) {
    val notifications by mainViewModel.trash.collectAsState()
    val isLoading by mainViewModel.isTrashLoading.collectAsState()
    var selectedNotification by remember { mutableStateOf<NotificationModel?>(null) }
    var permanentDeleteTarget by remember { mutableStateOf<NotificationModel?>(null) }
    var showEmptyConfirmation by remember { mutableStateOf(false) }
    var showRestoreAllConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.trash_title),
                onBack = { navController.popBackStack() },
                actions = {
                    AnimatedVisibility(
                        visible = notifications.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Row(modifier = Modifier.padding(end = 8.dp)) {
                            HeaderButton(
                                icon = Icons.Outlined.Restore,
                                contentDescription = stringResource(R.string.trash_restore_all),
                                onClick = { showRestoreAllConfirmation = true }
                            )
                            HeaderButton(
                                icon = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.trash_empty_action),
                                onClick = { showEmptyConfirmation = true },
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> HistoryLoadingState(Modifier.fillMaxSize())
                notifications.isEmpty() -> EmptyState(
                    title = stringResource(R.string.trash_empty),
                    modifier = Modifier.fillMaxSize(),
                    icon = Icons.Outlined.DeleteSweep,
                    description = stringResource(
                        R.string.trash_empty_description,
                        SettingState.TRASH_RETENTION_DAYS
                    )
                )
                // Grouped by the day each item was deleted, newest first, like History.
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    dayGroupedItems(
                        items = notifications,
                        key = { it.id },
                        epochOf = { it.trashedAtEpoch ?: it.receivedAtEpoch }
                    ) { notification, shape ->
                        val movedTime = notification.trashedAtEpoch?.toReadableTime()
                        NotificationHistoryCard(
                            modifier = Modifier.animateItem(),
                            notification = notification,
                            searchQuery = "",
                            onClick = { selectedNotification = notification },
                            footerText = movedTime?.let { stringResource(R.string.trash_deleted_at, it) },
                            shape = shape,
                            spacing = GroupRowGap
                        )
                    }
                }
            }
        }
    }

    selectedNotification?.let { notification ->
        ModalBottomSheet(onDismissRequest = { selectedNotification = null }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionTile(
                    icon = Icons.Outlined.Restore,
                    label = stringResource(R.string.trash_restore),
                    accent = AccentColors.Green,
                    modifier = Modifier
                        .weight(1f)
                        .appearIn(0),
                    onClick = {
                        selectedNotification = null
                        mainViewModel.restoreNotification(notification)
                    }
                )
                ActionTile(
                    icon = Icons.Outlined.DeleteForever,
                    label = stringResource(R.string.trash_delete_permanently),
                    destructive = true,
                    modifier = Modifier
                        .weight(1f)
                        .appearIn(1),
                    onClick = {
                        selectedNotification = null
                        permanentDeleteTarget = notification
                    }
                )
            }
        }
    }

    permanentDeleteTarget?.let { notification ->
        TrashConfirmDialog(
            icon = Icons.Outlined.DeleteForever,
            title = stringResource(R.string.trash_delete_permanently_question),
            message = stringResource(R.string.trash_delete_confirmation),
            confirmLabel = stringResource(R.string.trash_delete_permanently),
            destructive = true,
            onConfirm = {
                permanentDeleteTarget = null
                mainViewModel.permanentlyDeleteNotification(notification)
            },
            onDismiss = { permanentDeleteTarget = null }
        )
    }

    if (showEmptyConfirmation) {
        TrashConfirmDialog(
            icon = Icons.Outlined.DeleteSweep,
            title = stringResource(R.string.trash_empty_question),
            message = stringResource(R.string.trash_empty_confirmation),
            confirmLabel = stringResource(R.string.trash_empty_action),
            destructive = true,
            onConfirm = {
                showEmptyConfirmation = false
                mainViewModel.emptyTrash()
            },
            onDismiss = { showEmptyConfirmation = false }
        )
    }

    if (showRestoreAllConfirmation) {
        TrashConfirmDialog(
            icon = Icons.Outlined.Restore,
            title = stringResource(R.string.trash_restore_all_question),
            message = stringResource(R.string.trash_restore_all_confirmation),
            confirmLabel = stringResource(R.string.trash_restore_all),
            destructive = false,
            onConfirm = {
                showRestoreAllConfirmation = false
                mainViewModel.restoreAllNotifications()
            },
            onDismiss = { showRestoreAllConfirmation = false }
        )
    }
}

/** Confirmation with an icon badge above a centered title; red when [destructive]. */
@Composable
private fun TrashConfirmDialog(
    icon: ImageVector,
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            if (destructive) {
                IconBadge(
                    icon,
                    containerColor = colors.errorContainer.copy(alpha = 0.5f),
                    contentColor = colors.error,
                    size = 52.dp
                )
            } else {
                IconBadge(icon, containerColor = tintedCardColor(), size = 52.dp)
            }
        },
        title = { Text(title, textAlign = TextAlign.Center) },
        text = { Text(message, textAlign = TextAlign.Center) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(contentColor = colors.error)
                } else {
                    ButtonDefaults.textButtonColors()
                }
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
