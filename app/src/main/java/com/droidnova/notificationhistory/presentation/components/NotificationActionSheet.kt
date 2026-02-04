package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationActionSheet(
    onViewDetails: () -> Unit,
    onOpenApp: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            NotificationActionRow(
                label = "View Details",
                icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                onClick = onViewDetails
            )
            NotificationActionRow(
                label = "Open App",
                icon = { Icon(painter = painterResource(id = R.drawable.ic_open_in_new), contentDescription = null) },
                onClick = onOpenApp
            )
            NotificationActionRow(
                label = "Copy",
                icon = { Icon(painter = painterResource(id = R.drawable.ic_content_copy), contentDescription = null) },
                onClick = onCopy
            )
            NotificationActionRow(
                label = "Share",
                icon = { Icon(imageVector = Icons.Default.Share, contentDescription = null) },
                onClick = onShare
            )
            NotificationActionRow(
                label = "Delete",
                icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null) },
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun NotificationActionRow(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = label) },
        leadingContent = icon,
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
