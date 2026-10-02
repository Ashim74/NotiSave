package com.droidnova.notificationhistory.presentation.screens.history

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.droidnova.notificationhistory.presentation.components.AnimatedText
import com.droidnova.notificationhistory.presentation.components.ChoicePill
import com.droidnova.notificationhistory.presentation.components.HeaderButton
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.tileColor
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.ads.NativeAdCard
import com.droidnova.notificationhistory.ads.NativeAdSlots
import com.droidnova.notificationhistory.ads.rememberNativeAds
import com.droidnova.notificationhistory.ads.rememberShowAds
import com.google.android.gms.ads.nativead.NativeAd
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data.model.HistoryDateFilter
import com.droidnova.notificationhistory.data.model.HistoryFilterState
import com.droidnova.notificationhistory.presentation.components.DeleteConfirmationDialog
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.dayGroupedItems
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.HistoryLoadingState
import com.droidnova.notificationhistory.presentation.components.NotificationActionSheet
import com.droidnova.notificationhistory.presentation.components.NotificationDetailsDialog
import com.droidnova.notificationhistory.presentation.components.NotificationHistoryCard
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.fitToWidth
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.screens.conversations.ConversationListContent
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import com.droidnova.notificationhistory.presentation.ui.theme.ScreenListContentPadding
import com.droidnova.notificationhistory.presentation.ui.theme.listItemPadding
import com.droidnova.notificationhistory.utils.Analytics
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import com.droidnova.notificationhistory.utils.toReadableShareText
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


/** One segmented control replaces the former Messages/Apps tabs plus All/Conversations selector. */
enum class HistoryView { All, Conversations, Apps }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    mainViewmodel: MainViewModel,
    navController: NavController,
    initialPackageFilter: String? = null
) {
    val context = LocalContext.current

    // Transient app filter handed over by Insights; it lives in the ViewModel, never DataStore.
    LaunchedEffect(initialPackageFilter) {
        if (initialPackageFilter != null) mainViewmodel.setHistoryAppFilter(initialPackageFilter)
    }
    val packages = mainViewmodel.history.collectAsState()
    val appSummaries by mainViewmodel.appSummaries.collectAsState()
    val historyFilters by mainViewmodel.historyFilters.collectAsState()
    val isRefreshing by mainViewmodel.isHistoryRefreshing.collectAsState()
    val historyLoadState by mainViewmodel.historyLoadState.collectAsState()
    val isPremium by mainViewmodel.isPremium.collectAsState()
    // Native ads between notifications for free users (none while premium or ad-free)
    val nativeAds = rememberNativeAds(enabled = rememberShowAds(isPremium))
    var showMenu by remember { mutableStateOf(false) }
    var showCustomDateRange by remember { mutableStateOf(false) }
    var view by rememberSaveable { mutableStateOf(HistoryView.All) }
    var selectedNotification by remember { mutableStateOf<NotificationModel?>(null) }
    var showDetailsDialog by remember { mutableStateOf<NotificationModel?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<NotificationModel?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf(historyFilters.searchQuery) }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    // Long-press selection on the All list; ids that leave the list drop out of the selection.
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    val isSelecting = selectedIds.isNotEmpty()
    val toggleSelection = { id: Long ->
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    LaunchedEffect(packages.value) {
        val loadedIds = packages.value.mapTo(HashSet()) { it.id }
        selectedIds = selectedIds.filterTo(HashSet()) { it in loadedIds }
    }
    BackHandler(enabled = isSelecting) { selectedIds = emptySet() }

    LaunchedEffect(view) {
        selectedIds = emptySet()
        if (view == HistoryView.Apps) {
            isSearchActive = false
            searchQuery = ""
            mainViewmodel.updateHistorySearchQuery("")
        }
    }
    // "Search" launcher shortcut: land on the flat list with the search field open.
    val searchFocusRequested by mainViewmodel.historySearchFocusRequested.collectAsState()
    LaunchedEffect(searchFocusRequested) {
        if (searchFocusRequested) {
            view = HistoryView.All
            isSearchActive = true
            mainViewmodel.consumeHistorySearchFocus()
        }
    }
    LaunchedEffect(Unit) { Analytics.log(Analytics.HISTORY_OPEN) }

    val clearSearch = {
        searchQuery = ""
        mainViewmodel.updateHistorySearchQuery("")
    }
    val openManageApps = { navController.navigate(Screens.ManageNotifications.route) }

    Scaffold(
        topBar = {
            Column {
                AnimatedContent(
                    targetState = when {
                        isSelecting -> HistoryBarMode.Selecting
                        isSearchActive -> HistoryBarMode.Searching
                        else -> HistoryBarMode.Normal
                    },
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "historyTopBar"
                ) { mode ->
                    when (mode) {
                        HistoryBarMode.Selecting -> TopAppBar(
                            title = {
                                AnimatedText(
                                    text = pluralStringResource(
                                        R.plurals.history_selected_count,
                                        selectedIds.size,
                                        selectedIds.size
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { selectedIds = emptySet() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.content_description_cancel_selection)
                                    )
                                }
                            },
                            actions = {
                                TextButton(onClick = {
                                    selectedIds = packages.value.mapTo(HashSet()) { it.id }
                                }) {
                                    Text(stringResource(R.string.history_select_all))
                                }
                                HeaderButton(
                                    icon = Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.move_to_trash),
                                    onClick = { showBulkDeleteConfirm = true },
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                        HistoryBarMode.Searching -> {
                            SearchBarRow(
                                query = searchQuery,
                                focusRequester = focusRequester,
                                onQueryChange = {
                                    searchQuery = it
                                    mainViewmodel.updateHistorySearchQuery(it)
                                },
                                onClose = {
                                    isSearchActive = false
                                    searchQuery = ""
                                    mainViewmodel.updateHistorySearchQuery("")
                                },
                                onClear = {
                                    searchQuery = ""
                                    mainViewmodel.updateHistorySearchQuery("")
                                }
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        }
                        HistoryBarMode.Normal -> ScreenTopBar(
                            title = stringResource(R.string.history_title),
                            actions = {
                                if (view != HistoryView.Apps) {
                                    HeaderButton(
                                        icon = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.content_description_search),
                                        onClick = { isSearchActive = true }
                                    )
                                }
                                HeaderButton(
                                    icon = Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.trash_title),
                                    onClick = { navController.navigate(Screens.Trash.route) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        )
                    }
                }
                HistoryViewSelector(selected = view, onSelected = { view = it })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (view != HistoryView.Apps) {
                HistoryFilterBar(
                    filters = historyFilters,
                    appSummaries = appSummaries,
                    onAppSelected = mainViewmodel::setHistoryAppFilter,
                    onDateSelected = { mainViewmodel.setHistoryDateFilter(it) },
                    onCustomDateRequested = { showCustomDateRange = true },
                    onClearFilters = mainViewmodel::clearHistoryFilters
                )
            }
            when (view) {
                HistoryView.All -> HistoryScreenContent(
                    modifier = Modifier.weight(1f),
                    isRefreshing = isRefreshing,
                    isLoadingMore = historyLoadState.isLoadingMore,
                    canLoadMore = !historyLoadState.endReached,
                    isCapped = historyLoadState.isCapped,
                    packages = packages.value,
                    searchQuery = searchQuery,
                    hasActiveFilters = historyFilters.hasActiveFilters,
                    selectedIds = selectedIds,
                    nativeAds = nativeAds,
                    onLoadMore = { mainViewmodel.loadMoreHistory() },
                    onItemClick = {
                        if (isSelecting) toggleSelection(it.id) else selectedNotification = it
                    },
                    onItemLongClick = { toggleSelection(it.id) },
                    onRefresh = { mainViewmodel.refreshHistory() },
                    onClearSearch = clearSearch,
                    onClearFilters = mainViewmodel::clearHistoryFilters,
                    onManageApps = openManageApps
                )
                HistoryView.Conversations -> ConversationListContent(
                    mainViewModel = mainViewmodel,
                    searchQuery = historyFilters.searchQuery,
                    hasActiveFilters = historyFilters.hasActiveFilters,
                    onConversationClick = { conversation ->
                        navController.navigate(
                            Screens.ConversationDetail.createRoute(conversation.conversationKey)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    onClearSearch = clearSearch,
                    onClearFilters = mainViewmodel::clearHistoryFilters
                )
                HistoryView.Apps -> AppHistoryContent(
                    modifier = Modifier.weight(1f),
                    isRefreshing = isRefreshing,
                    packages = appSummaries,
                    onAppClick = { packageName ->
                        navController.navigate(Screens.AppsNotificationListScreen.createRoute(packageName))
                    },
                    onRefresh = { mainViewmodel.refreshHistory() },
                    onManageApps = openManageApps
                )
            }
        }
    }

    if (showCustomDateRange) {
        CustomDateRangeDialog(
            initialStart = historyFilters.customStartDate,
            initialEnd = historyFilters.customEndDate,
            onDismiss = { showCustomDateRange = false },
            onApply = { start, end ->
                if (mainViewmodel.setHistoryDateFilter(HistoryDateFilter.Custom, start, end)) {
                    showCustomDateRange = false
                }
            }
        )
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
                mainViewmodel.moveNotificationToTrash(showDeleteConfirmDialog!!)
                showDeleteConfirmDialog = null
            },
            onDismiss = { showDeleteConfirmDialog = null }
        )
    }

    if (showBulkDeleteConfirm && isSelecting) {
        DeleteConfirmationDialog(
            message = pluralStringResource(
                R.plurals.delete_selected_confirmation_message,
                selectedIds.size,
                selectedIds.size
            ),
            onConfirm = {
                mainViewmodel.moveNotificationsToTrash(selectedIds)
                selectedIds = emptySet()
                showBulkDeleteConfirm = false
            },
            onDismiss = { showBulkDeleteConfirm = false }
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
}//historyScreen



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    modifier: Modifier,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    isCapped: Boolean,
    packages: List<NotificationModel>,
    searchQuery: String,
    hasActiveFilters: Boolean,
    selectedIds: Set<Long>,
    nativeAds: List<NativeAd> = emptyList(),
    onLoadMore: () -> Unit,
    onItemClick: (NotificationModel) -> Unit,
    onItemLongClick: (NotificationModel) -> Unit,
    onRefresh: () -> Unit,
    onClearSearch: () -> Unit,
    onClearFilters: () -> Unit,
    onManageApps: () -> Unit
) {
    val listState = rememberLazyListState()
    // The first page is still on its way: show a spinner, never a premature "No notifications".
    val isInitialLoading = packages.isEmpty() && (isRefreshing || isLoadingMore)

    LaunchedEffect(packages, listState, canLoadMore, isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { index ->
                if (
                    index != null &&
                    index >= packages.size - 1 &&
                    canLoadMore &&
                    !isLoadingMore
                ) {
                    onLoadMore()
                }
            }
    }

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(state = listState, contentPadding = ScreenListContentPadding) {
            if (packages.isEmpty()) {
                item(key = "history-state") {
                    // fillParentMaxSize centers the state in the viewport (fillMaxSize is
                    // unbounded inside a lazy list) while keeping pull-to-refresh working.
                    if (isInitialLoading) {
                        HistoryLoadingState(Modifier.fillParentMaxSize())
                    } else {
                        when {
                            searchQuery.isNotBlank() -> EmptyState(
                                title = stringResource(R.string.history_empty_search),
                                modifier = Modifier.fillParentMaxSize(),
                                icon = Icons.Default.Search,
                                actionLabel = stringResource(R.string.content_description_clear_search),
                                onAction = onClearSearch
                            )
                            hasActiveFilters -> EmptyState(
                                title = stringResource(R.string.history_empty_filters),
                                modifier = Modifier.fillParentMaxSize(),
                                actionLabel = stringResource(R.string.history_clear_filters),
                                onAction = onClearFilters
                            )
                            else -> EmptyState(
                                title = stringResource(R.string.history_empty),
                                modifier = Modifier.fillParentMaxSize(),
                                actionLabel = stringResource(R.string.home_manage_apps),
                                onAction = onManageApps
                            )
                        }
                    }
                }
            } else {
                dayGroupedItems(
                    items = packages,
                    key = { it.id },
                    epochOf = { it.receivedAtEpoch },
                    // Each loaded ad gets one slot; no ads loaded means no gaps at all
                    slotAfter = { index ->
                        NativeAdSlots.slotAfter(index)?.takeIf { it < nativeAds.size }
                    },
                    slot = { slot ->
                        NativeAdCard(nativeAds[slot], Modifier.animateItem())
                    }
                ) { item, shape ->
                    NotificationHistoryCard(
                        modifier = Modifier.animateItem(),
                        notification = item,
                        searchQuery = searchQuery,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        selected = item.id in selectedIds,
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
                if (isCapped) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.ScreenHorizontal, vertical = 12.dp)
                                .clip(RoundedCornerShape(Dimens.TileCornerRadius))
                                .background(tintedCardColor())
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.history_cap_hint, packages.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterBar(
    filters: HistoryFilterState,
    appSummaries: List<NotificationModel>,
    onAppSelected: (String?) -> Unit,
    onDateSelected: (HistoryDateFilter) -> Unit,
    onCustomDateRequested: () -> Unit,
    onClearFilters: () -> Unit
) {
    var showAppMenu by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }
    val selectedApp = appSummaries.firstOrNull { it.packageName == filters.packageName }
    val appLabel = selectedApp?.appName?.ifBlank { selectedApp.packageName }
        ?: filters.packageName
        ?: stringResource(R.string.history_filter_all_apps)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenHorizontal, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            ChoicePill(
                label = appLabel,
                selected = filters.packageName != null,
                leadingIcon = Icons.Outlined.Apps,
                onClick = { showAppMenu = true }
            )
            DropdownMenu(
                expanded = showAppMenu,
                onDismissRequest = { showAppMenu = false },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.history_filter_all_apps)) },
                    leadingIcon = { HistoryAppIcon(null) },
                    onClick = {
                        showAppMenu = false
                        onAppSelected(null)
                    }
                )
                appSummaries.forEach { app ->
                    val label = app.appName.ifBlank { app.packageName }
                    DropdownMenuItem(
                        leadingIcon = { HistoryAppIcon(packageName = app.packageName) },
                        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = if (app.packageName == filters.packageName) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        onClick = {
                            showAppMenu = false
                            onAppSelected(app.packageName)
                        }
                    )
                }
            }
        }

        Box {
            ChoicePill(
                label = filters.dateFilter.displayName(),
                selected = filters.dateFilter != HistoryDateFilter.AllTime,
                leadingIcon = Icons.Outlined.DateRange,
                onClick = { showDateMenu = true }
            )
            DropdownMenu(
                expanded = showDateMenu,
                onDismissRequest = { showDateMenu = false }
            ) {
                HistoryDateFilter.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName()) },
                        trailingIcon = if (option == filters.dateFilter) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        onClick = {
                            showDateMenu = false
                            if (option == HistoryDateFilter.Custom) {
                                onCustomDateRequested()
                            } else {
                                onDateSelected(option)
                            }
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = filters.hasActiveFilters,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            ChoicePill(
                label = stringResource(R.string.history_clear_filters),
                selected = false,
                leadingIcon = Icons.Default.Close,
                onClick = onClearFilters
            )
        }
    }
}

/**
 * All / Chats / Apps as one rounded track with a sliding highlight, each option an icon and a
 * short word (replaces the Material segmented buttons).
 */
@Composable
private fun HistoryViewSelector(
    selected: HistoryView,
    onSelected: (HistoryView) -> Unit
) {
    val options = HistoryView.entries
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontal, vertical = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(tintedCardColor())
            .padding(4.dp)
    ) {
        val segment = maxWidth / options.size
        val offset by animateDpAsState(
            targetValue = segment * options.indexOf(selected),
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "historyViewIndicator"
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.primary)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            options.forEach { option ->
                val isSelected = selected == option
                val content by animateColorAsState(
                    if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    label = "historyViewContent"
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelected(option) })
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (option) {
                            HistoryView.All -> Icons.Outlined.Notifications
                            HistoryView.Conversations -> Icons.Outlined.Forum
                            HistoryView.Apps -> Icons.Outlined.Apps
                        },
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            when (option) {
                                HistoryView.All -> R.string.history_mode_all
                                HistoryView.Conversations -> R.string.history_mode_conversations
                                HistoryView.Apps -> R.string.history_tab_apps
                            }
                        ),
                        color = content,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = fitToWidth(MaterialTheme.typography.labelLarge.fontSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDateRangeDialog(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onDismiss: () -> Unit,
    onApply: (LocalDate, LocalDate) -> Unit
) {
    val context = LocalContext.current
    var startDate by remember(initialStart) { mutableStateOf(initialStart ?: LocalDate.now()) }
    var endDate by remember(initialEnd) { mutableStateOf(initialEnd ?: LocalDate.now()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    fun showDatePicker(initialDate: LocalDate, onSelected: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onSelected(LocalDate.of(year, month + 1, dayOfMonth))
                errorMessage = null
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    val rangeError = stringResource(R.string.history_date_range_error)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { IconBadge(Icons.Outlined.DateRange, containerColor = tintedCardColor(), size = 52.dp) },
        title = { Text(stringResource(R.string.history_custom_date_range)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GroupRowGap)) {
                ListRow(
                    title = stringResource(R.string.history_date_start, startDate.format(formatter)),
                    shape = groupedShape(0, 2),
                    leading = { IconBadge(Icons.Outlined.Event, accent = AccentColors.Teal, size = 34.dp) },
                    onClick = { showDatePicker(startDate) { startDate = it } }
                )
                ListRow(
                    title = stringResource(R.string.history_date_end, endDate.format(formatter)),
                    shape = groupedShape(1, 2),
                    leading = { IconBadge(Icons.Outlined.Event, accent = AccentColors.Orange, size = 34.dp) },
                    onClick = { showDatePicker(endDate) { endDate = it } }
                )
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        modifier = Modifier.padding(start = 6.dp, top = 6.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (startDate.isAfter(endDate)) {
                        errorMessage = rangeError
                    } else {
                        onApply(startDate, endDate)
                    }
                }
            ) {
                Text(stringResource(R.string.btn_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}


@Composable
private fun HistoryDateFilter.displayName(): String = stringResource(
    when (this) {
        HistoryDateFilter.AllTime -> R.string.date_filter_all_time
        HistoryDateFilter.Today -> R.string.date_today
        HistoryDateFilter.Yesterday -> R.string.date_yesterday
        HistoryDateFilter.Last7Days -> R.string.date_filter_last_7_days
        HistoryDateFilter.Last30Days -> R.string.date_filter_last_30_days
        HistoryDateFilter.Custom -> R.string.date_filter_custom
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHistoryContent(
    modifier: Modifier,
    isRefreshing: Boolean,
    packages: List<NotificationModel>,
    onAppClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onManageApps: () -> Unit
) {

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(contentPadding = ScreenListContentPadding) {
            if (packages.isEmpty()) {
                item(key = "apps-state") {
                    if (isRefreshing) {
                        HistoryLoadingState(Modifier.fillParentMaxSize())
                    } else {
                        EmptyState(
                            title = stringResource(R.string.history_empty),
                            modifier = Modifier.fillParentMaxSize(),
                            icon = ImageVector.vectorResource(R.drawable.ic_apps),
                            actionLabel = stringResource(R.string.home_manage_apps),
                            onAction = onManageApps
                        )
                    }
                }
            } else {
                itemsIndexed(packages, key = { _, item -> item.packageName }) { index, latest ->
                    val name = latest.appName.ifBlank { latest.packageName }
                    ListRow(
                        modifier = Modifier
                            .animateItem()
                            .appearIn(index)
                            .padding(horizontal = Dimens.ScreenHorizontal, vertical = GroupRowGap / 2),
                        shape = groupedShape(index, packages.size),
                        title = name,
                        value = latest.receivedAt,
                        leading = { AppIconTile(latest.packageName) },
                        onClick = { onAppClick(latest.packageName) }
                    )
                }
            }
        }
    }
}

/** An app's launcher icon on a small rounded tile, the lead of app rows. */
@Composable
private fun AppIconTile(packageName: String) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(Dimens.TileCornerRadius))
            .background(tileColor()),
        contentAlignment = Alignment.Center
    ) {
        HistoryAppIcon(packageName, size = 28.dp)
    }
}

private enum class HistoryBarMode { Normal, Searching, Selecting }

/** The search field as a rounded pill in place of the title bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarRow(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onClear: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_notifications_hint), maxLines = 1) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                        IconButton(onClick = onClear) {
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
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.content_description_close_search)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}
