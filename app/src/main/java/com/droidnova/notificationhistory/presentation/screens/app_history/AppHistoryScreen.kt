package com.droidnova.notificationhistory.presentation.screens.app_history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.HeaderButton
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.presentation.components.DeleteConfirmationDialog
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import androidx.compose.material.icons.outlined.Notifications
import com.droidnova.notificationhistory.presentation.components.dayGroupedItems
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
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "appHistoryTopBar"
            ) { searching ->
                if (searching) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    mainViewModel.updateAppHistorySearchQuery(packageName, it)
                                },
                                placeholder = { Text(stringResource(R.string.search_notifications_hint), maxLines = 1) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    AnimatedVisibility(
                                        visible = searchQuery.isNotEmpty(),
                                        enter = fadeIn() + scaleIn(),
                                        exit = fadeOut() + scaleOut()
                                    ) {
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
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 12.dp)
                                    .height(52.dp)
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = tintedCardColor(),
                                    unfocusedContainerColor = tintedCardColor(),
                                    disabledContainerColor = tintedCardColor(),
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } else {
                    TopAppBar(
                        title = {
                            // The app's own icon next to its name, so the screen is recognizable at a glance
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(Dimens.TileCornerRadius))
                                        .background(tintedCardColor()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    HistoryAppIcon(packageName = packageName, size = 24.dp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        actions = {
                            HeaderButton(
                                icon = Icons.Default.Search,
                                contentDescription = stringResource(R.string.content_description_search),
                                onClick = { isSearchActive = true },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                }
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
                    state = listState,
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    if (notifications.isEmpty()) {
                        item(key = "app-history-state") {
                            if (isRefreshing) {
                                HistoryLoadingState(Modifier.fillParentMaxSize())
                            } else {
                                EmptyState(
                                    title = if (searchQuery.isNotBlank()) {
                                        stringResource(R.string.history_empty_search)
                                    } else {
                                        stringResource(R.string.history_empty)
                                    },
                                    icon = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Outlined.Notifications,
                                    modifier = Modifier.fillParentMaxSize()
                                )
                            }
                        }
                    } else {
                        dayGroupedItems(
                            items = notifications,
                            key = { it.id },
                            epochOf = { it.receivedAtEpoch }
                        ) { item, shape ->
                            NotificationHistoryCard(
                                modifier = Modifier.animateItem(),
                                notification = item,
                                searchQuery = searchQuery,
                                onClick = { selectedNotification = item },
                                shape = shape,
                                spacing = GroupRowGap
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
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .clip(RoundedCornerShape(50)),
                                        trackColor = tintedCardColor()
                                    )
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
