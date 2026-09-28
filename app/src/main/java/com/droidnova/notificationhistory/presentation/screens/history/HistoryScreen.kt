package com.droidnova.notificationhistory.presentation.screens.history

import android.graphics.drawable.Drawable
import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data.model.HistoryDateFilter
import com.droidnova.notificationhistory.data.model.HistoryFilterState
import com.droidnova.notificationhistory.presentation.components.DeleteConfirmationDialog
import com.droidnova.notificationhistory.presentation.components.NotificationActionSheet
import com.droidnova.notificationhistory.presentation.components.NotificationDetailsDialog
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import com.droidnova.notificationhistory.utils.toReadableShareText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


enum class HistoryViewType { Message, Apps }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(mainViewmodel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val packages = mainViewmodel.history.collectAsState()
    val appSummaries by mainViewmodel.appSummaries.collectAsState()
    val historyFilters by mainViewmodel.historyFilters.collectAsState()
    val settingsState by mainViewmodel.settingState.collectAsState()
    val isRefreshing by mainViewmodel.isHistoryRefreshing.collectAsState()
    val historyLoadState by mainViewmodel.historyLoadState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<NotificationModel?>(null) }
    var showRetentionPicker by remember { mutableStateOf(false) }
    var showCustomDateRange by remember { mutableStateOf(false) }
    var viewType by rememberSaveable { mutableStateOf(HistoryViewType.Message) }
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
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                mainViewmodel.updateHistorySearchQuery(it)
                            },
                            placeholder = { Text("Search notifications") },
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
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Close search")
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
                                    contentDescription = "Clear search"
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
                    title = { Text("Notification History") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (viewType == HistoryViewType.Message) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu"
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Clear all History") },
                                    onClick = {
                                        showMenu = false
                                        showConfirm = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("Auto delete (" + formatRetentionDays(settingsState.historyRetentionDays) + ")")
                                    },
                                    onClick = {
                                        showMenu = false
                                        showRetentionPicker = true
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    selected = viewType == HistoryViewType.Message,
                    onClick = { viewType = HistoryViewType.Message }
                ) {
                    Text("Messages")
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    selected = viewType == HistoryViewType.Apps,
                    onClick = { viewType = HistoryViewType.Apps }
                ) {
                    Text("Apps")
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
                HistoryScreenContent(
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

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Clear all History") },
            text = { Text("Are you sure you want to clear all history?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    mainViewmodel.clearAllHistory()
                }) { Text(text = "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showRetentionPicker) {
        RetentionPickerDialog(
            currentRetentionDays = settingsState.historyRetentionDays,
            onDismiss = { showRetentionPicker = false },
            onSelectionConfirmed = { days ->
                showRetentionPicker = false
                mainViewmodel.updateHistoryRetentionDays(days)
            }
        )
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
                mainViewmodel.deleteNotification(showDeleteConfirmDialog!!)
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
                    "Notification",
                    notification.toReadableShareText()
                )
                selectedNotification = null
            },
            onShare = {
                IntentUtil.shareText(
                    context,
                    "Share notification",
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



@OptIn(ExperimentalMaterialApi::class)
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
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )
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

    val grouped = packages.groupBy { it.receivedAt.substringBefore(",") }
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    val sortedGroups = grouped.toList().sortedByDescending { (date, _) ->
        runCatching { LocalDate.parse(date, formatter) }.getOrNull()
    }

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        LazyColumn(state = listState) {
            if (packages.isEmpty()) {
                item {
                    if (isInitialLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val message = when {
                            searchQuery.isNotBlank() -> "No search results."
                            hasActiveFilters -> "No notifications match these filters."
                            else -> "No notification history."
                        }
                        EmptyValueCard(modifier, message)
                    }
                }
            } else {
                sortedGroups.forEach { (date, notifications) ->
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.W800,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    items(notifications, key = { it.id }) { item ->
                        ItemHistoryCard(item, searchQuery, onItemClick)
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
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
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
        ?: "All apps"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (filters.hasActiveFilters) {
            Text(
                text = "Filters active",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Box {
            OutlinedButton(onClick = { showAppMenu = true }) {
                Text(appLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(
                expanded = showAppMenu,
                onDismissRequest = { showAppMenu = false },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("All apps") },
                    onClick = {
                        showAppMenu = false
                        onAppSelected(null)
                    }
                )
                appSummaries.forEach { app ->
                    val label = app.appName.ifBlank { app.packageName }
                    DropdownMenuItem(
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
            OutlinedButton(onClick = { showDateMenu = true }) {
                Text(filters.dateFilter.displayName())
            }
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
                Text("Clear filters")
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom date range") },
        text = {
            Column {
                TextButton(onClick = { showDatePicker(startDate) { startDate = it } }) {
                    Text("Start: ${startDate.format(formatter)}")
                }
                TextButton(onClick = { showDatePicker(endDate) { endDate = it } }) {
                    Text("End: ${endDate.format(formatter)}")
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
                        errorMessage = "Start date must not be after end date."
                    } else {
                        onApply(startDate, endDate)
                    }
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun HistoryDateFilter.displayName(): String = when (this) {
    HistoryDateFilter.AllTime -> "All time"
    HistoryDateFilter.Today -> "Today"
    HistoryDateFilter.Yesterday -> "Yesterday"
    HistoryDateFilter.Last7Days -> "Last 7 days"
    HistoryDateFilter.Last30Days -> "Last 30 days"
    HistoryDateFilter.Custom -> "Custom range"
}

private val RETENTION_DAYS_OPTIONS = listOf(0, 1, 3, 7, 14, 30)

@Composable
private fun RetentionPickerDialog(
    currentRetentionDays: Int,
    onDismiss: () -> Unit,
    onSelectionConfirmed: (Int) -> Unit,
) {
    var selectedOption by remember(currentRetentionDays) { mutableStateOf(currentRetentionDays) }
    val options = remember(currentRetentionDays) {
        (RETENTION_DAYS_OPTIONS + currentRetentionDays).distinct().sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto delete history") },
        text = {
            Column {
                Text(
                    text = "Choose how long to keep your notifications.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                options.forEach { days ->
                    RetentionOptionRow(
                        label = formatRetentionDays(days),
                        selected = selectedOption == days,
                        onClick = { selectedOption = days }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelectionConfirmed(selectedOption) }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RetentionOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun formatRetentionDays(days: Int): String {
    return when {
        days <= 0 -> "Never"
        days == 1 -> "1 day"
        else -> "$days days"
    }
}

@Composable
private fun EmptyValueCard(modifier: Modifier, message: String = "No notification history.") {
    Box {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = "History",
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppHistoryContent(
    modifier: Modifier,
    isRefreshing: Boolean,
    packages: List<NotificationModel>,
    onAppClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val grouped = packages.groupBy { it.packageName }
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val sortedGroups = grouped.entries.sortedByDescending { entry ->
        entry.value.maxOfOrNull {
            runCatching { LocalDateTime.parse(it.receivedAt, formatter) }.getOrNull()
                ?: LocalDateTime.MIN
        } ?: LocalDateTime.MIN
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        LazyColumn {
            if (packages.isEmpty()) {
                item {
                    if (isRefreshing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        EmptyValueCard(modifier)
                    }
                }
            } else {
                items(sortedGroups) { (_, notifications) ->
                    val latest = notifications.maxByOrNull {
                        runCatching { LocalDateTime.parse(it.receivedAt, formatter) }.getOrNull()
                            ?: LocalDateTime.MIN
                    } ?: notifications.first()

                    Column {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .fillMaxWidth()
                                .clickable { onAppClick(latest.packageName) }
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                AppIcon(drawable = latest.appIcon)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = latest.appName.ifBlank { latest.packageName },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.W900
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = latest.receivedAt.substringAfter(", "),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun ItemHistoryCard(model: NotificationModel, searchQuery: String, onClick: (NotificationModel) -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable { onClick(model) },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                AppIcon(drawable = model.appIcon)

                Spacer(Modifier.width(8.dp))

                Text(
                    text = model.appName.ifBlank { model.packageName },
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.W900
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = model.receivedAt.substringAfter(", "),
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }
            Spacer(Modifier.height(8.dp))


            Text(
                text = buildHighlightedText(model.title, searchQuery),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildHighlightedText(model.text, searchQuery),
                style = MaterialTheme.typography.bodyMedium,
                softWrap = true,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildHighlightedText(text: String, query: String): AnnotatedString {
    if (query.isBlank()) {
        return buildAnnotatedString { append(text) }
    }
    return buildAnnotatedString {
        var startIndex = 0
        while (startIndex < text.length) {
            val index = text.indexOf(query, startIndex, ignoreCase = true)
            if (index == -1) {
                append(text.substring(startIndex))
                break
            }
            append(text.substring(startIndex, index))
            withStyle(style = SpanStyle(color = Color.Black, background = Color(0xFFFFF9C4))) {
                append(text.substring(index, index + query.length))
            }
            startIndex = index + query.length
        }
    }
}

@Composable
fun AppIcon(drawable: Drawable?) {
    drawable?.let {
        val bitmap: ImageBitmap = it.toBitmap().asImageBitmap()
        Image(
            modifier = Modifier.size(20.dp),
            bitmap = bitmap,
            contentDescription = null
        )
    }
}
