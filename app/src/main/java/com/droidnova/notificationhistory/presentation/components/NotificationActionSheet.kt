package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors

/** One row of round actions, Secret Calculator style: icon on top, a one-word label under it. */
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tile = Modifier.weight(1f)
            ActionTile(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.hist_action_details),
                onClick = onViewDetails,
                accent = AccentColors.Blue,
                modifier = tile.appearIn(0)
            )
            ActionTile(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                label = stringResource(R.string.hist_action_open),
                onClick = onOpenApp,
                accent = AccentColors.Teal,
                modifier = tile.appearIn(1)
            )
            ActionTile(
                icon = Icons.Outlined.ContentCopy,
                label = stringResource(R.string.content_description_copy),
                onClick = onCopy,
                accent = AccentColors.Purple,
                modifier = tile.appearIn(2)
            )
            ActionTile(
                icon = Icons.Outlined.Share,
                label = stringResource(R.string.content_description_share),
                onClick = onShare,
                accent = AccentColors.Green,
                modifier = tile.appearIn(3)
            )
            ActionTile(
                icon = Icons.Outlined.DeleteOutline,
                label = stringResource(R.string.hist_action_trash),
                onClick = onDelete,
                destructive = true,
                modifier = tile.appearIn(4)
            )
        }
    }
}
