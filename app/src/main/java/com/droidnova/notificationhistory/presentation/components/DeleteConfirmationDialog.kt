package com.droidnova.notificationhistory.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.droidnova.notificationhistory.R

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String = stringResource(R.string.delete_confirmation_message)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirmation_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.move_to_trash)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
