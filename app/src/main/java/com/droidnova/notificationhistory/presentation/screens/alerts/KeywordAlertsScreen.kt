package com.droidnova.notificationhistory.presentation.screens.alerts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.AddWordField
import com.droidnova.notificationhistory.presentation.components.AlertsBlockedBanner
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/** Premium: words that raise an alert when a newly saved notification contains one. */
@Composable
fun KeywordAlertsScreen(navController: NavController, mainViewModel: MainViewModel) {
    val keywords by mainViewModel.keywordAlerts.collectAsState()
    val words = keywords.toList().asReversed()

    Scaffold(
        topBar = {
            ScreenTopBar(title = stringResource(R.string.settings_keyword_alerts), onBack = { navController.popBackStack() })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(key = "blocked") {
                AlertsBlockedBanner(Modifier.padding(horizontal = Dimens.ScreenHorizontal, vertical = 4.dp))
            }
            item(key = "intro") {
                Text(
                    stringResource(R.string.keyword_intro),
                    modifier = Modifier
                        .padding(horizontal = Dimens.ScreenHorizontal + 4.dp, vertical = 6.dp)
                        .appearIn(0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "add") {
                AddWordField(
                    placeholder = stringResource(R.string.keyword_add_hint),
                    leadingIcon = Icons.Outlined.Sell,
                    onAdd = mainViewModel::addKeywordAlert,
                    modifier = Modifier
                        .padding(horizontal = Dimens.ScreenHorizontal, vertical = 10.dp)
                        .appearIn(1)
                )
            }
            if (words.isEmpty()) {
                item(key = "empty") {
                    EmptyState(title = stringResource(R.string.keyword_empty), icon = Icons.Outlined.NotificationsActive)
                }
            } else {
                itemsIndexed(words, key = { _, word -> word }) { index, word ->
                    ListRow(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = Dimens.ScreenHorizontal, vertical = GroupRowGap / 2),
                        shape = groupedShape(index, words.size),
                        title = word,
                        leading = { IconBadge(Icons.Outlined.Sell, accent = AccentColors.Amber, size = 34.dp) },
                        trailing = {
                            IconButton(onClick = { mainViewModel.removeKeywordAlert(word) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.remove),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
