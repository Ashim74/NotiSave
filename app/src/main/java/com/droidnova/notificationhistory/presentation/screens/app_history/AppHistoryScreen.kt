package com.droidnova.notificationhistory.presentation.screens.app_history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.presentation.components.DeleteConfirmationDialog
import com.droidnova.notificationhistory.presentation.components.HistoryEmptyState
import com.droidnova.notificationhistory.presentation.components.HistoryLoadingState
import com.droidnova.notificationhistory.presentation.components.NotificationActionSheet
import com.droidnova.notificationhistory.presentation.components.NotificationDetailsDialog
import com.droidnova.notificationhistory.presentation.components.NotificationHistoryCard
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import com.droidnova.notificationhistory.utils.toReadableShareText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHistoryScreen(
    mainViewModel: MainViewModel,
    packageName: String,
    navController: NavController
) {
    val context = LocalContext.current
    val appHistoryState by mainViewModel.appHistoryState(packageName).collectAsState()
    val notifications = appHistoryState.notifications
    val isRefreshing = appHistoryState.isRefreshing
    val isLoadingMore = appHistoryState.isLoadingMore
    val canLoadMore = !appHistoryState.endReached
    var selectedNotification by remember { mutableStateOf<NotificationModel?>(null) }
    var showDetailsDialog by remember { mutableStateOf<NotificationModel?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<NotificationModel?>(null) }
    var searchQuery by rememberSaveable(packageName) {
        mutableStateOf(appHistoryState.searchQuery)
    }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    var title by remember { mutableStateOf(packageName) }

    LaunchedEffect(packageName) {
        mainViewModel.ensureAppHistoryLoaded(packageName)
    }
    DisposableEffect(packageName) {
        onDispose { mainViewModel.releaseAppHistory(packageName) }
    }

    LaunchedEffect(notifications) {
        notifications.firstOrNull()?.appName?.ifBlank { packageName }?.let { appName ->
            if (appName.isNotBlank()) {
                title = appName
            }
        }
    }

    LaunchedEffect(notifications, listState, canLoadMore, isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { index ->
                if (
                    index != null &&
                    index >= notifications.size - 1 &&
                    canLoadMore &&
                    !isLoadingMore
                ) {
                    mainViewModel.loadMoreAppHistory(packageName)
                }
            }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                mainViewModel.updateAppHistorySearchQuery(packageName, it)
                            },
                            placeholder = { Text(stringResource(R.string.search_notifications_hint)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                            mainViewModel.updateAppHistorySearchQuery(packageName, "")
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_close_search)
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                mainViewModel.updateAppHistorySearchQuery(packageName, "")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.content_description_clear_search)
                                )
                            }
                        }
                    }
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.content_description_search)
                            )
                        }
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets(bottom = 4.dp)
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { mainViewModel.refreshAppHistory(packageName) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                ) {
                    if (notifications.isEmpty()) {
                        item {
                            if (isRefreshing) {
                                HistoryLoadingState(Modifier.fillMaxSize())
                            } else {
                                HistoryEmptyState(
                                    message = if (searchQuery.isNotBlank()) {
                                        stringResource(R.string.history_empty_search)
                                    } else {
                                        stringResource(R.string.history_empty)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        items(notifications, key = { it.id }) { item ->
                            NotificationHistoryCard(
                                notification = item,
                                searchQuery = searchQuery,
                                onClick = { selectedNotification = item }
                            )
                        }
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showDetailsDialog?.let { notification ->
        NotificationDetailsDialog(
            notification = notification,
            onDismiss = { showDetailsDialog = null }
        )
    }

    if (showDeleteConfirmDialog != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                mainViewModel.moveNotificationToTrash(showDeleteConfirmDialog!!)
                showDeleteConfirmDialog = null
            },
            onDismiss = { showDeleteConfirmDialog = null }
        )
    }

    selectedNotification?.let { notification ->
        NotificationActionSheet(
            onViewDetails = {
                showDetailsDialog = notification
                selectedNotification = null
            },
            onOpenApp = {
                IntentUtil.openApp(context, notification.packageName)
                selectedNotification = null
            },
            onCopy = {
                IntentUtil.copyToClipboard(
                    context,
                    context.getString(R.string.notification_fallback_title),
                    notification.toReadableShareText()
                )
                selectedNotification = null
            },
            onShare = {
                IntentUtil.shareText(
                    context,
                    context.getString(R.string.share_notification_chooser),
                    notification.toReadableShareText()
                )
                selectedNotification = null
            },
            onDelete = {
                showDeleteConfirmDialog = notification
                selectedNotification = null
            },
            onDismiss = { selectedNotification = null }
        )
    }
}
