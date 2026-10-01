package com.droidnova.notificationhistory.presentation.screens.conversations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.model.ConversationModel
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.AppCardDefaults
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Shape
import com.droidnova.notificationhistory.presentation.components.DeleteConfirmationDialog
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.HistoryEmptyState
import com.droidnova.notificationhistory.presentation.components.HistoryLoadingState
import com.droidnova.notificationhistory.presentation.components.NotificationActionSheet
import com.droidnova.notificationhistory.presentation.components.NotificationDetailsDialog
import com.droidnova.notificationhistory.presentation.ui.theme.ScreenListContentPadding
import com.droidnova.notificationhistory.presentation.ui.theme.listItemPadding
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import com.droidnova.notificationhistory.utils.toReadableShareText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val LOAD_MORE_THRESHOLD = 5

@Composable
fun ConversationListContent(
    mainViewModel: MainViewModel,
    searchQuery: String,
    hasActiveFilters: Boolean,
    onConversationClick: (ConversationModel) -> Unit,
    modifier: Modifier = Modifier,
    onClearSearch: (() -> Unit)? = null,
    onClearFilters: (() -> Unit)? = null
) {
    val state by mainViewModel.conversationList.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val conversations = state.conversations
    val canLoadMore = !state.endReached && !state.isLoadingMore

    val currentCanLoadMore by rememberUpdatedState(canLoadMore)
    val currentSize by rememberUpdatedState(conversations.size)
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            currentCanLoadMore && currentSize > 0 && lastVisible != null &&
                lastVisible >= currentSize - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { mainViewModel.loadMoreConversations() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = ScreenListContentPadding
    ) {
        if (conversations.isEmpty()) {
            item(key = "conversation-state") {
                if (!state.hasLoaded) {
                    HistoryLoadingState(Modifier.fillParentMaxSize())
                } else {
                    when {
                        searchQuery.isNotBlank() -> EmptyState(
                            title = stringResource(R.string.conversations_empty_search),
                            modifier = Modifier.fillParentMaxSize(),
                            actionLabel = onClearSearch?.let {
                                stringResource(R.string.content_description_clear_search)
                            },
                            onAction = onClearSearch
                        )
                        hasActiveFilters -> EmptyState(
                            title = stringResource(R.string.conversations_empty_filters),
                            modifier = Modifier.fillParentMaxSize(),
                            actionLabel = onClearFilters?.let {
                                stringResource(R.string.history_clear_filters)
                            },
                            onAction = onClearFilters
                        )
                        else -> EmptyState(
                            title = stringResource(R.string.conversations_empty),
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                }
            }
        } else {
            itemsIndexed(conversations, key = { _, item -> item.conversationKey }) { index, conversation ->
                ConversationRow(
                    modifier = Modifier.animateItem(),
                    conversation = conversation,
                    onClick = { onConversationClick(conversation) },
                    shape = groupedShape(index, conversations.size)
                )
            }
            if (state.isLoadingMore) {
                item(key = "conversation-loading") {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

/** Two lines: who + when, then the latest message and how many messages there are. */
@Composable
private fun ConversationRow(
    conversation: ConversationModel,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val yesterdayLabel = stringResource(R.string.date_yesterday)
    val time = remember(conversation.latestReceivedAtEpoch, yesterdayLabel) {
        formatConversationTime(conversation.latestReceivedAtEpoch, yesterdayLabel)
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontal, vertical = GroupRowGap / 2),
        shape = shape,
        color = AppCardDefaults.containerColor()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryAppIcon(
                packageName = conversation.packageName,
                size = 36.dp,
                contentDescription = conversation.appName.ifBlank { conversation.packageName }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = time,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.latestMessage.replace('\n', ' '),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.messageCount > 1) {
                        Badge(
                            modifier = Modifier.padding(start = 8.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(conversation.messageCount.toString())
                        }
                    }
                }
            }
        }
    }
}

private sealed interface TimelineRow {
    val key: String

    data class DateHeader(val date: LocalDate) : TimelineRow {
        override val key: String get() = "date-${date.toEpochDay()}"
    }

    data class Message(val notification: NotificationModel) : TimelineRow {
        override val key: String get() = "message-${notification.id}"
    }
}

/**
 * Builds rows for a reversed list (index 0 = newest at the bottom). Each date header follows the
 * oldest loaded message of its day so it renders above that day's messages.
 */
private fun buildTimelineRows(newestFirst: List<NotificationModel>, zoneId: ZoneId): List<TimelineRow> {
    val rows = ArrayList<TimelineRow>(newestFirst.size + 8)
    newestFirst.forEachIndexed { index, notification ->
        rows += TimelineRow.Message(notification)
        val date = notification.receivedAtEpoch.toLocalDate(zoneId)
        val older = newestFirst.getOrNull(index + 1)
        if (older == null || older.receivedAtEpoch.toLocalDate(zoneId) != date) {
            rows += TimelineRow.DateHeader(date)
        }
    }
    return rows
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    mainViewModel: MainViewModel,
    conversationKey: String,
    navController: NavController
) {
    val context = LocalContext.current
    val state by remember(conversationKey) {
        mainViewModel.conversationDetailState(conversationKey)
    }.collectAsStateWithLifecycle()
    DisposableEffect(conversationKey) {
        onDispose { mainViewModel.releaseConversationDetail(conversationKey) }
    }
    val listState = rememberLazyListState()
    val zoneId = remember { ZoneId.systemDefault() }
    val rows = remember(state.messages) { buildTimelineRows(state.messages, zoneId) }
    var selectedNotification by remember { mutableStateOf<NotificationModel?>(null) }
    var showDetailsDialog by remember { mutableStateOf<NotificationModel?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<NotificationModel?>(null) }

    val canLoadOlder = !state.endReached && !state.isLoadingMore
    val currentCanLoadOlder by rememberUpdatedState(canLoadOlder)
    val currentRowCount by rememberUpdatedState(rows.size)
    LaunchedEffect(listState, conversationKey) {
        // Reversed layout: the highest visible index is the oldest message at the top.
        snapshotFlow {
            val topVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            currentCanLoadOlder && currentRowCount > 0 && topVisible != null &&
                topVisible >= currentRowCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { mainViewModel.loadOlderConversationMessages(conversationKey) }
    }

    // Keep following new messages when the reader is already at the latest one.
    val newestId = state.messages.firstOrNull()?.id
    LaunchedEffect(newestId) {
        if (newestId != null && listState.firstVisibleItemIndex <= 1) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HistoryAppIcon(packageName = state.packageName.ifBlank { null })
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.appName.isNotBlank()) {
                                Text(
                                    text = state.appName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(bottom = 4.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                !state.hasLoaded -> HistoryLoadingState(Modifier.fillMaxSize())
                rows.isEmpty() -> HistoryEmptyState(
                    message = stringResource(R.string.conversation_empty_messages),
                    modifier = Modifier.fillMaxSize()
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    reverseLayout = true
                ) {
                    items(rows, key = { it.key }, contentType = { it::class }) { row ->
                        when (row) {
                            is TimelineRow.DateHeader -> TimelineDateHeader(row.date)
                            is TimelineRow.Message -> TimelineMessage(
                                notification = row.notification,
                                conversationTitle = state.title,
                                onClick = { selectedNotification = row.notification }
                            )
                        }
                    }
                    if (state.isLoadingMore) {
                        item(key = "timeline-loading") {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    showDetailsDialog?.let { notification ->
        NotificationDetailsDialog(
            notification = notification,
            onDismiss = { showDetailsDialog = null }
        )
    }

    showDeleteConfirmDialog?.let { notification ->
        DeleteConfirmationDialog(
            onConfirm = {
                mainViewModel.moveNotificationToTrash(notification)
                showDeleteConfirmDialog = null
            },
            onDismiss = { showDeleteConfirmDialog = null }
        )
    }

    selectedNotification?.let { notification ->
        NotificationActionSheet(
            onViewDetails = {
                showDetailsDialog = notification
                selectedNotification = null
            },
            onOpenApp = {
                IntentUtil.openApp(context, notification.packageName)
                selectedNotification = null
            },
            onCopy = {
                IntentUtil.copyToClipboard(
                    context,
                    context.getString(R.string.notification_fallback_title),
                    notification.toReadableShareText()
                )
                selectedNotification = null
            },
            onShare = {
                IntentUtil.shareText(
                    context,
                    context.getString(R.string.share_notification_chooser),
                    notification.toReadableShareText()
                )
                selectedNotification = null
            },
            onDelete = {
                showDeleteConfirmDialog = notification
                selectedNotification = null
            },
            onDismiss = { selectedNotification = null }
        )
    }
}

@Composable
private fun TimelineDateHeader(date: LocalDate) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> stringResource(R.string.date_today)
        today.minusDays(1) -> stringResource(R.string.date_yesterday)
        else -> remember(date) { date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
    }
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TimelineMessage(
    notification: NotificationModel,
    conversationTitle: String,
    onClick: () -> Unit
) {
    val time = remember(notification.receivedAtEpoch) {
        Instant.ofEpochMilli(notification.receivedAtEpoch)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
    // Only show the saved title when it adds information beyond the conversation name.
    val subtitle = notification.title.takeIf {
        it.isNotBlank() && !it.startsWith(conversationTitle, ignoreCase = true)
    }
    // A chat bubble: as wide as its text needs, sender on top, time tucked in the corner.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontal, vertical = 3.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
            color = AppCardDefaults.containerColor()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = notification.text.ifBlank { notification.title },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = time,
                    modifier = Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

/** Time for today's messages, [yesterdayLabel] for yesterday, otherwise a short date. */
private fun formatConversationTime(epochMillis: Long, yesterdayLabel: String): String {
    val zoneId = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    val today = LocalDate.now(zoneId)
    return when (dateTime.toLocalDate()) {
        today -> dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        today.minusDays(1) -> yesterdayLabel
        else -> dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
    }
}
