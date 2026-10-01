package com.droidnova.notificationhistory.presentation.screens.trash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.mapper.toReadableTime
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.dayGroupedItems
import com.droidnova.notificationhistory.presentation.components.HistoryLoadingState
import com.droidnova.notificationhistory.presentation.components.NotificationHistoryCard

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
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = { showRestoreAllConfirmation = true }) {
                            Text(stringResource(R.string.trash_restore_all))
                        }
                        IconButton(onClick = { showEmptyConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.trash_empty_action)
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
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.trash_restore)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        selectedNotification = null
                        mainViewModel.restoreNotification(notification)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.trash_delete_permanently)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable {
                        selectedNotification = null
                        permanentDeleteTarget = notification
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    }

    permanentDeleteTarget?.let { notification ->
        AlertDialog(
            onDismissRequest = { permanentDeleteTarget = null },
            title = { Text(stringResource(R.string.trash_delete_permanently_question)) },
            text = { Text(stringResource(R.string.trash_delete_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        permanentDeleteTarget = null
                        mainViewModel.permanentlyDeleteNotification(notification)
                    }
                ) { Text(stringResource(R.string.trash_delete_permanently)) }
            },
            dismissButton = {
                TextButton(onClick = { permanentDeleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEmptyConfirmation) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirmation = false },
            title = { Text(stringResource(R.string.trash_empty_question)) },
            text = { Text(stringResource(R.string.trash_empty_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyConfirmation = false
                        mainViewModel.emptyTrash()
                    }
                ) { Text(stringResource(R.string.trash_empty_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showRestoreAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreAllConfirmation = false },
            title = { Text(stringResource(R.string.trash_restore_all_question)) },
            text = { Text(stringResource(R.string.trash_restore_all_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreAllConfirmation = false
                        mainViewModel.restoreAllNotifications()
                    }
                ) { Text(stringResource(R.string.trash_restore_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAllConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
