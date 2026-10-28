package com.droidnova.notificationhistory.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R

/**
 * Shown the first time someone taps Reply: in-app reply is new and depends on each chat app,
 * so they should check the chat app to see that it really went.
 */
@Composable
fun ReplyNoticeDialog(
    onGotIt: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            IconBadge(
                Icons.AutoMirrored.Outlined.Reply,
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                size = 52.dp
            )
        },
        title = { Text(stringResource(R.string.reply_notice_title), textAlign = TextAlign.Center) },
        text = { Text(stringResource(R.string.reply_notice_message), textAlign = TextAlign.Center) },
        confirmButton = {
            TextButton(onClick = onGotIt) { Text(stringResource(R.string.reply_notice_got_it)) }
        }
    )
}
