package com.droidnova.notificationhistory.presentation.screens.history

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.data.model.NotificationModel
// Material 3 imports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.droidnova.notificationhistory.R
import androidx.navigation.NavController
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.components.NotificationActionSheet
import com.droidnova.notificationhistory.utils.IntentUtils
import com.droidnova.notificationhistory.utils.toReadableShareText
import androidx.compose.material3.LinearProgressIndicator


enum class HistoryViewType { Message, Apps }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(mainViewmodel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val packages = mainViewmodel.history.collectAsState()
    val appSummaries = mainViewmodel.observeLatestNotificationsByApp().collectAsState(initial = emptyList())
    val settingsState by mainViewmodel.settingState.collectAsState()
    val isRefreshing by mainViewmodel.isHistoryRefreshing.collectAsState()
    Log.e("Mantsha", "HistoryScreen: ${packages.value}")
    var showMenu by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showRetentionPicker by remember { mutableStateOf(false) }
    var viewType by rememberSaveable { mutableStateOf(HistoryViewType.Message) }
    var selectedNotification by remember { mutableStateOf<NotificationModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
            )
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
            HistoryViewType.Message -> HistoryScreenContent(
                modifier = contentModifier,
                isRefreshing = isRefreshing,
                packages = packages.value,
                onLoadMore = { mainViewmodel.loadMoreHistory() },
                onItemClick = { selectedNotification = it },
                onRefresh = { mainViewmodel.refreshHistory() }
            )
            HistoryViewType.Apps -> AppHistoryContent(
                modifier = contentModifier,
                isRefreshing = isRefreshing,
                packages = appSummaries.value,
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

    selectedNotification?.let { notification ->
        NotificationActionSheet(
            notification = notification,
            onOpen = {
                IntentUtils.openApp(context, notification.packageName)
                selectedNotification = null
            },
            onCopy = {
                IntentUtils.copyToClipboard(
                    context,
                    "Notification",
                    notification.toReadableShareText()
                )
                selectedNotification = null
            },
            onShare = {
                IntentUtils.shareText(
                    context,
                    "Share notification",
                    notification.toReadableShareText()
                )
                selectedNotification = null
            },
            onDelete = {
                mainViewmodel.deleteNotification(notification)
                selectedNotification = null
            },
            onDismiss = { selectedNotification = null }
        )
    }
}//historyScreen



@Composable
fun HistoryScreenContent(
    modifier: Modifier,
    isRefreshing: Boolean,
    packages: List<NotificationModel>,
    onLoadMore: () -> Unit,
    onItemClick: (NotificationModel) -> Unit,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    LaunchedEffect(packages, listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { index ->
                if (index != null && index >= packages.size - 1) {
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
                    EmptyValueCard(modifier)
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
                    items(notifications) { item ->
                        Log.e("Mantsha", "HistoryScreenContent: ${item}")
                        ItemHistoryCard(item, onItemClick)
                    }
                }
            }
        }
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
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
private fun EmptyValueCard(modifier: Modifier) {
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
                "No History Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

        }
    }
}

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
                    EmptyValueCard(modifier)
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
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun ItemHistoryCard(model: NotificationModel, onClick: (NotificationModel) -> Unit) {
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
            }
            Spacer(Modifier.height(8.dp))


            Text(
                text = model.title,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = model.text,
                style = MaterialTheme.typography.bodyMedium,
                softWrap = true,
                maxLines = Int.MAX_VALUE
            )
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
