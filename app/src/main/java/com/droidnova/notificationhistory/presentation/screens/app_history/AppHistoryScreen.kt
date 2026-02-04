package com.droidnova.notificationhistory.presentation.screens.app_history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.presentation.components.DeleteConfirmationDialog
import com.droidnova.notificationhistory.presentation.components.NotificationActionSheet
import com.droidnova.notificationhistory.presentation.components.NotificationDetailsDialog
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import com.droidnova.notificationhistory.utils.toReadableShareText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AppHistoryScreen(
    mainViewModel: MainViewModel,
    packageName: String,
    navController: NavController
) {
    val context = LocalContext.current
    val notifications by mainViewModel
        .observeNotificationsForPackage(packageName)
        .collectAsState(initial = emptyList())
    val isRefreshing by mainViewModel.isHistoryRefreshing.collectAsState()
    var selectedNotification by remember { mutableStateOf<NotificationModel?>(null) }
    var showDetailsDialog by remember { mutableStateOf<NotificationModel?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<NotificationModel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val title = notifications.firstOrNull()?.appName?.ifBlank { packageName } ?: packageName
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { mainViewModel.refreshHistory() }
    )

    val filteredNotifications = notifications.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.text.contains(
            searchQuery,
            ignoreCase = true
        )
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
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
                        }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
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
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pullRefresh(pullRefreshState)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredNotifications) { item ->
                        HistoryCard(item, searchQuery = searchQuery, onClick = { selectedNotification = it })
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
                mainViewModel.deleteNotification(showDeleteConfirmDialog!!)
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
}

@Composable
fun HistoryCard(model: NotificationModel, searchQuery: String, onClick: (NotificationModel) -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable { onClick(model) },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = model.receivedAt.substringAfter(", "),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))


            Text(
                text = buildHighlightedText(text = model.title, query = searchQuery),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildHighlightedText(text = model.text, query = searchQuery),
                style = MaterialTheme.typography.bodyMedium,
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
