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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.data.model.NotificationModel
// Material 3 imports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.droidnova.notificationhistory.R


enum class HistoryViewType { Message, Apps }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(mainViewmodel: MainViewModel) {
    val packages = mainViewmodel.history.collectAsState()
    Log.e("Mantsha", "HistoryScreen: ${packages.value}")
    var showMenu by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var viewType by remember { mutableStateOf(HistoryViewType.Message) }
    var selectedNotification by remember { mutableStateOf<NotificationModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification History") },
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
                packages = packages.value,
                onLoadMore = { mainViewmodel.loadMoreHistory() },
                onItemClick = { selectedNotification = it }
            )
            HistoryViewType.Apps -> AppHistoryContent(
                modifier = contentModifier,
                packages = packages.value,
                onItemClick = { selectedNotification = it }
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

    selectedNotification?.let { notification ->
        AlertDialog(
            onDismissRequest = { selectedNotification = null },
            title = { Text(notification.appName.ifBlank { notification.packageName }) },
            text = {
                Column {
                    Text("Title: ${notification.title}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Message: ${notification.text}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Arrival: ${notification.receivedAt}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedNotification = null }) {
                    Text("Close")
                }
            }
        )
    }
}



@Composable
fun HistoryScreenContent(
    modifier: Modifier,
    packages: List<NotificationModel>,
    onLoadMore: () -> Unit,
    onItemClick: (NotificationModel) -> Unit
) {
    Log.e("Maaanjha", "HistoryScreenContent:list ${packages}")
    val listState = rememberLazyListState()

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

    LazyColumn(modifier = modifier, state = listState) {
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
    packages: List<NotificationModel>,
    onItemClick: (NotificationModel) -> Unit
) {
    val grouped = packages.groupBy { it.packageName }
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val sortedGroups = grouped.entries.sortedByDescending { entry ->
        entry.value.maxOfOrNull {
            runCatching { LocalDateTime.parse(it.receivedAt, formatter) }.getOrNull()
                ?: LocalDateTime.MIN
        } ?: LocalDateTime.MIN
    }

    LazyColumn(modifier = modifier) {
        if (packages.isEmpty()) {
            item {
                EmptyValueCard(modifier)
            }
        } else {
            items(sortedGroups) { (_, notifications) ->
                var expanded by remember { mutableStateOf(false) }
                val latest = notifications.maxByOrNull {
                    runCatching { LocalDateTime.parse(it.receivedAt, formatter) }.getOrNull()
                        ?: LocalDateTime.MIN
                } ?: notifications.first()

                Column {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
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
                    if (expanded) {
                        notifications.sortedByDescending {
                            runCatching { LocalDateTime.parse(it.receivedAt, formatter) }
                                .getOrNull() ?: LocalDateTime.MIN
                        }.forEach { item ->
                            ItemHistoryCard(item, onItemClick)
                        }
                    }
                }
            }
        }
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