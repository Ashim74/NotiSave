package com.droidnova.notificationhistory.presentation.screens.history

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(mainViewmodel: MainViewModel) {
    val packages = mainViewmodel.history.collectAsState()
    Log.e("Mantsha", "HistoryScreen: ${packages.value}")
    var showMenu by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification History") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Back"
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
        }
    ) { innerPadding ->
        HistoryScreenContent(
            modifier = Modifier.padding(innerPadding),
            packages = packages.value,
            onLoadMore = { mainViewmodel.loadMoreHistory() }
        )
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
}



@Composable
fun HistoryScreenContent(
    modifier: Modifier,
    packages: List<NotificationModel>,
    onLoadMore: () -> Unit
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
                Text(
                    "No History Found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W800,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
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
                    ItemHistoryCard(item)
                }
            }
        }
    }
}

@Composable
fun ItemHistoryCard(model: NotificationModel) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth(),
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