package com.droidnova.notificationhistory.presentation.screens.history

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data.model.HistoryDateFilter
import com.droidnova.notificationhistory.data.model.HistoryFilterState
import com.droidnova.notificationhistory.presentation.components.AppListItem
import com.droidnova.notificationhistory.presentation.components.DeleteConfirmationDialog
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.HistoryEmptyState
import com.droidnova.notificationhistory.presentation.components.HistoryLoadingState
import com.droidnova.notificationhistory.presentation.components.NotificationActionSheet
import com.droidnova.notificationhistory.presentation.components.NotificationDetailsDialog
import com.droidnova.notificationhistory.presentation.components.NotificationHistoryCard
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.screens.conversations.ConversationListContent
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import com.droidnova.notificationhistory.utils.toReadableShareText
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


enum class HistoryViewType { Message, Apps }

/** Messages tab: flat history (all notifications) or grouped messaging conversations. */
enum class MessagesMode { All, Conversations }

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
    var showMenu by remember { mutableStateOf(false) }
    var showCustomDateRange by remember { mutableStateOf(false) }
    var viewType by rememberSaveable { mutableStateOf(HistoryViewType.Message) }
    var messagesMode by rememberSaveable { mutableStateOf(MessagesMode.All) }
    var selectedNotification by remember { mutableStateOf<NotificationModel?>(null) }
    var showDetailsDialog by remember { mutableStateOf<NotificationModel?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<NotificationModel?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf(historyFilters.searchQuery) }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(viewType) {
        if (viewType == HistoryViewType.Apps) {
            isSearchActive = false
            searchQuery = ""
            mainViewmodel.updateHistorySearchQuery("")
        }
    }

    Scaffold(
        topBar = {
            Column {
                if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                mainViewmodel.updateHistorySearchQuery(it)
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
                            mainViewmodel.updateHistorySearchQuery("")
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
                                mainViewmodel.updateHistorySearchQuery("")
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
                    title = { Text(stringResource(R.string.history_title)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        if (viewType == HistoryViewType.Message) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.content_description_search)
                                )
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.content_description_menu)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.trash_title)) },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Screens.Trash.route)
                                    }
                                )
                            }
                        }
                    }
                )
                }
                TabRow(selectedTabIndex = if (viewType == HistoryViewType.Message) 0 else 1) {
                    Tab(
                        selected = viewType == HistoryViewType.Message,
                        onClick = { viewType = HistoryViewType.Message },
                        text = { Text(stringResource(R.string.history_tab_messages)) }
                    )
                    Tab(
                        selected = viewType == HistoryViewType.Apps,
                        onClick = { viewType = HistoryViewType.Apps },
                        text = { Text(stringResource(R.string.history_tab_apps)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (viewType) {
            HistoryViewType.Message -> Column(modifier = contentModifier) {
                HistoryFilterBar(
                    filters = historyFilters,
                    appSummaries = appSummaries,
                    onAppSelected = mainViewmodel::setHistoryAppFilter,
                    onDateSelected = { mainViewmodel.setHistoryDateFilter(it) },
                    onCustomDateRequested = { showCustomDateRange = true },
                    onClearFilters = mainViewmodel::clearHistoryFilters
                )
                MessagesModeSelector(
                    selected = messagesMode,
                    onSelected = { messagesMode = it }
                )
                when (messagesMode) {
                    MessagesMode.All -> HistoryScreenContent(
                        modifier = Modifier.weight(1f),
                        isRefreshing = isRefreshing,
                        isLoadingMore = historyLoadState.isLoadingMore,
                        canLoadMore = !historyLoadState.endReached,
                        packages = packages.value,
                        searchQuery = searchQuery,
                        hasActiveFilters = historyFilters.hasActiveFilters,
                        onLoadMore = { mainViewmodel.loadMoreHistory() },
                        onItemClick = { selectedNotification = it },
                        onRefresh = { mainViewmodel.refreshHistory() }
                    )
                    MessagesMode.Conversations -> ConversationListContent(
                        mainViewModel = mainViewmodel,
                        searchQuery = historyFilters.searchQuery,
                        hasActiveFilters = historyFilters.hasActiveFilters,
                        onConversationClick = { conversation ->
                            navController.navigate(
                                Screens.ConversationDetail.createRoute(conversation.conversationKey)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            HistoryViewType.Apps -> AppHistoryContent(
                modifier = contentModifier,
                isRefreshing = isRefreshing,
                packages = appSummaries,
                onAppClick = { packageName ->
                    navController.navigate(Screens.AppsNotificationListScreen.createRoute(packageName))
                },
                onRefresh = { mainViewmodel.refreshHistory() }
            )
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
    packages: List<NotificationModel>,
    searchQuery: String,
    hasActiveFilters: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (NotificationModel) -> Unit,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()
    val isInitialLoading = isRefreshing && packages.isEmpty()

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

    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val grouped = packages.groupBy {
        Instant.ofEpochMilli(it.receivedAtEpoch).atZone(zoneId).toLocalDate()
    }

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(state = listState) {
            if (packages.isEmpty()) {
                item {
                    if (isInitialLoading) {
                        HistoryLoadingState(Modifier.fillMaxSize())
                    } else {
                        val message = when {
                            searchQuery.isNotBlank() -> stringResource(R.string.history_empty_search)
                            hasActiveFilters -> stringResource(R.string.history_empty_filters)
                            else -> stringResource(R.string.history_empty)
                        }
                        HistoryEmptyState(message, Modifier.fillMaxSize())
                    }
                }
            } else {
                grouped.forEach { (date, notifications) ->
                    item {
                        Text(
                            text = when (date) {
                                today -> stringResource(R.string.date_today)
                                today.minusDays(1) -> stringResource(R.string.date_yesterday)
                                else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(notifications, key = { it.id }) { item ->
                        NotificationHistoryCard(
                            notification = item,
                            searchQuery = searchQuery,
                            onClick = { onItemClick(item) }
                        )
                    }
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
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            FilterChip(
                selected = filters.packageName != null,
                onClick = { showAppMenu = true },
                label = { Text(appLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
                        text = {
                            Column {
                                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (label != app.packageName) {
                                    Text(
                                        app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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
            FilterChip(
                selected = filters.dateFilter != HistoryDateFilter.AllTime,
                onClick = { showDateMenu = true },
                label = { Text(filters.dateFilter.displayName()) }
            )
            DropdownMenu(
                expanded = showDateMenu,
                onDismissRequest = { showDateMenu = false }
            ) {
                HistoryDateFilter.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName()) },
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

        if (filters.hasActiveFilters) {
            TextButton(onClick = onClearFilters) {
                Text(stringResource(R.string.history_clear_filters))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagesModeSelector(
    selected: MessagesMode,
    onSelected: (MessagesMode) -> Unit
) {
    val options = MessagesMode.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        stringResource(
                            when (mode) {
                                MessagesMode.All -> R.string.history_mode_all
                                MessagesMode.Conversations -> R.string.history_mode_conversations
                            }
                        )
                    )
                }
            )
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
        title = { Text(stringResource(R.string.history_custom_date_range)) },
        text = {
            Column {
                TextButton(onClick = { showDatePicker(startDate) { startDate = it } }) {
                    Text(stringResource(R.string.history_date_start, startDate.format(formatter)))
                }
                TextButton(onClick = { showDatePicker(endDate) { endDate = it } }) {
                    Text(stringResource(R.string.history_date_end, endDate.format(formatter)))
                }
                errorMessage?.let {
                    Text(
                        text = it,
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
) {

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn {
            if (packages.isEmpty()) {
                item {
                    if (isRefreshing) {
                        HistoryLoadingState(Modifier.fillMaxSize())
                    } else {
                        HistoryEmptyState(stringResource(R.string.history_empty), Modifier.fillMaxSize())
                    }
                }
            } else {
                items(packages, key = { it.packageName }) { latest ->
                    AppListItem(
                        packageName = latest.packageName,
                        title = latest.appName.ifBlank { latest.packageName },
                        trailingText = latest.receivedAt.substringAfter(", "),
                        showChevron = true,
                        onClick = { onAppClick(latest.packageName) }
                    )
                }
            }
        }
    }
}
