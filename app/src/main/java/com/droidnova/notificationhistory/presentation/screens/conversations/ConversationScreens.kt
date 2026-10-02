package com.droidnova.notificationhistory.presentation.screens.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.droidnova.notificationhistory.presentation.components.CountPill
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.pressScale
import com.droidnova.notificationhistory.presentation.components.tileColor
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
                    LoadingMoreIndicator()
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
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontal, vertical = GroupRowGap / 2)
            .pressScale(interaction, 0.98f)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick),
        shape = shape,
        color = AppCardDefaults.containerColor()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconTile(
                packageName = conversation.packageName,
                contentDescription = conversation.appName.ifBlank { conversation.packageName }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.messageCount > 1) {
                        CountPill(
                            count = conversation.messageCount,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/** The app's launcher icon on a small rounded tile, like the rows in History. */
@Composable
private fun AppIconTile(
    packageName: String?,
    contentDescription: String?,
    size: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(Dimens.TileCornerRadius))
            .background(tileColor()),
        contentAlignment = Alignment.Center
    ) {
        HistoryAppIcon(
            packageName = packageName,
            size = size * 0.75f,
            contentDescription = contentDescription
        )
    }
}

/** A thin rounded progress line for "loading more", inset like the cards. */
@Composable
private fun LoadingMoreIndicator() {
    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontal + 24.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(50)),
        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    )
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
            ScreenTopBar(
                title = state.title,
                subtitle = state.appName.takeIf { it.isNotBlank() },
                onBack = { navController.popBackStack() },
                actions = {
                    AppIconTile(
                        packageName = state.packageName.ifBlank { null },
                        contentDescription = null,
                        size = 36.dp
                    )
                    Spacer(Modifier.width(12.dp))
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
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
                            LoadingMoreIndicator()
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
    // A centered pill between days, like a messaging app.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics { heading() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(tintedCardColor())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
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
    val interaction = remember { MutableInteractionSource() }
    val bubbleShape = RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontal, vertical = 3.dp)
            .appearIn(0)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .pressScale(interaction, 0.97f)
                .clip(bubbleShape)
                .background(tintedCardColor())
                .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
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
