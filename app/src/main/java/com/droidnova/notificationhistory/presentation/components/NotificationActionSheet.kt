package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.data.model.NotificationModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationActionSheet(
    notification: NotificationModel,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = notification.appName.ifBlank { notification.packageName },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Text(
                text = notification.title.ifBlank { "Notification" },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            NotificationActionRow(
                label = "Open",
                icon = Icons.Default.OpenInNew,
                onClick = onOpen
            )
            NotificationActionRow(
                label = "Copy",
                icon = Icons.Default.ContentCopy,
                onClick = onCopy
            )
            NotificationActionRow(
                label = "Share",
                icon = Icons.Default.Share,
                onClick = onShare
            )
            NotificationActionRow(
                label = "Delete",
                icon = Icons.Default.Delete,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun NotificationActionRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = label) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
