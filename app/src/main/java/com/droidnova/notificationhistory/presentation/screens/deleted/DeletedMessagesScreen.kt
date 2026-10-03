package com.droidnova.notificationhistory.presentation.screens.deleted

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.mapper.toReadableTime
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HistoryLoadingState
import com.droidnova.notificationhistory.presentation.components.NotificationDetailsDialog
import com.droidnova.notificationhistory.presentation.components.NotificationHistoryCard
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.dialogs.PremiumUpsell
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/**
 * Messages their senders deleted, recovered from the notification that showed them. Recovery
 * runs for everyone; without Premium the list shows who and when, but the text stays locked,
 * which is the strongest reason to upgrade.
 */
@Composable
fun DeletedMessagesScreen(navController: NavController, mainViewModel: MainViewModel) {
    val deleted by mainViewModel.deletedMessages.collectAsState()
    val isPremium by mainViewModel.isPremium.collectAsState()
    var showPremium by remember { mutableStateOf(false) }
    var openedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val lockedText = stringResource(R.string.deleted_locked_text)

    Scaffold(
        topBar = {
            ScreenTopBar(title = stringResource(R.string.deleted_title), onBack = { navController.popBackStack() })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val rows = deleted
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(key = "intro") { IntroCard(Modifier.padding(horizontal = Dimens.ScreenHorizontal).appearIn(0)) }
            if (!isPremium && !rows.isNullOrEmpty()) {
                item(key = "unlock") {
                    Button(
                        onClick = { showPremium = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.ScreenHorizontal, vertical = 10.dp)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(pluralStringResource(R.plurals.deleted_unlock_button, rows.size, rows.size))
                    }
                }
            }
            when {
                rows == null -> item(key = "loading") { HistoryLoadingState(Modifier.padding(vertical = 48.dp)) }
                rows.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.deleted_empty_title),
                        icon = Icons.Outlined.DeleteForever,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
                else -> itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                    val shown = if (isPremium) row else row.copy(text = lockedText)
                    NotificationHistoryCard(
                        modifier = Modifier.animateItem(),
                        notification = shown,
                        searchQuery = "",
                        footerText = row.deletedAtEpoch?.let {
                            stringResource(R.string.deleted_at, it.toReadableTime())
                        },
                        onClick = { if (isPremium) openedId = row.id else showPremium = true },
                        shape = groupedShape(index, rows.size),
                        spacing = GroupRowGap
                    )
                }
            }
        }
    }

    val opened: NotificationModel? = deleted?.firstOrNull { it.id == openedId }
    if (opened != null && isPremium) {
        NotificationDetailsDialog(notification = opened, onDismiss = { openedId = null })
    }
    PremiumUpsell(mainViewModel, visible = showPremium, onDismiss = { showPremium = false })
}

@Composable
private fun IntroCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(Dimens.CardCornerRadius))
            .background(tintedCardColor())
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.deleted_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
