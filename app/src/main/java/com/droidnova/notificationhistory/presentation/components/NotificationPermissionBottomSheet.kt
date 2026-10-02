package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors

/**
 * Asks for notification access: a floating bell, one line of why, then the four steps as short
 * icon-led lines instead of a paragraph, and one button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onGoToSettings: () -> Unit
) {
    // The full instructions stay available to TalkBack in one announcement.
    val fullDescription = stringResource(R.string.notification_permission_description)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(
                icon = Icons.Outlined.NotificationsActive,
                containerColor = tintedCardColor(),
                size = 68.dp,
                modifier = Modifier.floating()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.notification_permission_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.set_perm_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(tintedCardColor())
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clearAndSetSemantics { contentDescription = fullDescription },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StepRow(0, Icons.Outlined.TouchApp, AccentColors.Blue, stringResource(R.string.set_perm_step_1))
                StepRow(1, Icons.Outlined.Settings, AccentColors.Purple, stringResource(R.string.set_perm_step_2))
                StepRow(2, Icons.Outlined.Apps, AccentColors.Orange, stringResource(R.string.set_perm_step_3))
                StepRow(3, Icons.Outlined.ToggleOn, AccentColors.Green, stringResource(R.string.set_perm_step_4))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGoToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.go_to_settings), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepRow(index: Int, icon: ImageVector, accent: Color, text: String) {
    Row(
        modifier = Modifier.appearIn(index, stepMillis = 70),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, accent = accent, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1
        )
    }
}
