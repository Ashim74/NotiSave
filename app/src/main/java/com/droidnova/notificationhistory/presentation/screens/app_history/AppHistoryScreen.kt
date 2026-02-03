package com.droidnova.notificationhistory.presentation.screens.app_history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.presentation.components.NotificationActionSheet
import com.droidnova.notificationhistory.utils.IntentUtils
import com.droidnova.notificationhistory.utils.toReadableShareText

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedNotification by remember { mutableStateOf<NotificationModel?>(null) }

    val title = notifications.firstOrNull()?.appName?.ifBlank { packageName } ?: packageName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding
                )
        ) {
            items(notifications) { item ->
                HistoryCard(item, onClick = { selectedNotification = it })
            }
        }
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
                mainViewModel.deleteNotification(notification)
                selectedNotification = null
            },
            onDismiss = { selectedNotification = null }
        )
    }
}

@Composable
fun HistoryCard(model: NotificationModel, onClick: (NotificationModel) -> Unit) {
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
