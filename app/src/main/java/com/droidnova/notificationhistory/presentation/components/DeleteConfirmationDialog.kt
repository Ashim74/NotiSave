package com.droidnova.notificationhistory.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String = stringResource(R.string.delete_confirmation_message)
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            IconBadge(
                Icons.Outlined.DeleteOutline,
                containerColor = colors.errorContainer.copy(alpha = 0.5f),
                contentColor = colors.error,
                size = 52.dp
            )
        },
        title = { Text(stringResource(R.string.delete_confirmation_title), textAlign = TextAlign.Center) },
        text = { Text(message, textAlign = TextAlign.Center) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
            ) { Text(stringResource(R.string.move_to_trash)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
