package com.droidnova.notificationhistory.presentation.screens.conversations

import android.content.Context
import android.util.Log
import com.droidnova.notificationhistory.core.apps.AppInfoCache
import com.droidnova.notificationhistory.data.db.NotificationDao
import com.droidnova.notificationhistory.data.mapper.convertConversationRowToModel
import com.droidnova.notificationhistory.data.mapper.convertEntityToModel
import com.droidnova.notificationhistory.data.model.ConversationModel
import com.droidnova.notificationhistory.data.model.NotificationModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Filters applied to the conversation list; mirrors the Messages history filters. */
data class ConversationQuery(
    val packageName: String? = null,
    val searchQuery: String = "",
    val startInclusive: Long = Long.MIN_VALUE,
    val endExclusive: Long = Long.MAX_VALUE,
    /** Hidden apps, kept out of the list. */
    val excludedPackages: List<String> = emptyList()
)

data class ConversationListUiState(
    val conversations: List<ConversationModel> = emptyList(),
    val hasLoaded: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false
)

data class ConversationDetailUiState(
    val conversationKey: String,
    val title: String = "",
    val packageName: String = "",
    val appName: String = "",
    /** Newest first; the screen renders them reversed for chronological reading. */
    val messages: List<NotificationModel> = emptyList(),
    val hasLoaded: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false
)

/**
 * Database-backed, keyset-paged conversation list and conversation detail pages.
 *
 * Loaded pages are refreshed in place whenever the active conversation rows change (new message,
 * move to Trash, restore, retention cleanup), but only while a screen is collecting the state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationHistoryController(
    private val context: Context,
    private val dao: NotificationDao,
    private val scope: CoroutineScope,
    private val pageSize: Int
) {
    private val listState = MutableStateFlow(ConversationListUiState())
    val conversationList: StateFlow<ConversationListUiState> = listState.asStateFlow()

    private val listQuery = MutableStateFlow(ConversationQuery())
    private val listMutex = Mutex()
    private var loadedListQuery: ConversationQuery? = null

    private val detailStates = mutableMapOf<String, MutableStateFlow<ConversationDetailUiState>>()
    private val detailMutexes = mutableMapOf<String, Mutex>()
    private val detailJobs = mutableMapOf<String, Job>()

    init {
        scope.launch {
            listState.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .flatMapLatest { active ->
                    if (active) {
                        combine(dao.observeConversationsSignature(), listQuery) { _, query -> query }
                    } else {
                        emptyFlow()
                    }
                }
                .collectLatest { query -> reloadList(query) }
        }
    }

    fun setQuery(query: ConversationQuery) {
        listQuery.value = query
    }

    fun loadMoreConversations() {
        val current = listState.value
        if (!current.hasLoaded || current.endReached || current.isLoadingMore) return
        scope.launch {
            listMutex.withLock {
                val latest = listState.value
                val query = loadedListQuery
                if (latest.endReached || query != listQuery.value || query == null) return@withLock
                listState.update { it.copy(isLoadingMore = true) }
                try {
                    val last = latest.conversations.lastOrNull()
                    val page = loadConversationPage(
                        query = query,
                        cursorReceivedAt = last?.latestReceivedAtEpoch,
                        cursorId = last?.latestNotificationId,
                        limit = pageSize
                    )
                    listState.update { state ->
                        val existingKeys = state.conversations.mapTo(hashSetOf()) { it.conversationKey }
                        state.copy(
                            conversations = state.conversations +
                                page.filterNot { it.conversationKey in existingKeys },
                            endReached = page.size < pageSize
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load more conversations", e)
                } finally {
                    listState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    /** Reloads from the top, keeping as many rows as are already loaded (stable scroll). */
    private suspend fun reloadList(query: ConversationQuery) {
        listMutex.withLock {
            val sameQuery = query == loadedListQuery
            if (!sameQuery) {
                listState.value = ConversationListUiState()
            }
            val limit = if (sameQuery) {
                listState.value.conversations.size.coerceAtLeast(pageSize)
            } else {
                pageSize
            }
            try {
                val page = loadConversationPage(query, null, null, limit)
                loadedListQuery = query
                listState.value = ConversationListUiState(
                    conversations = page.distinctBy { it.conversationKey },
                    hasLoaded = true,
                    endReached = page.size < limit
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversations", e)
                listState.update { it.copy(hasLoaded = true, isLoadingMore = false) }
            }
        }
    }

    private suspend fun loadConversationPage(
        query: ConversationQuery,
        cursorReceivedAt: Long?,
        cursorId: Long?,
        limit: Int
    ): List<ConversationModel> = withContext(Dispatchers.IO) {
        val rows = dao.getConversationPage(
            packageName = query.packageName,
            excludedPackages = query.excludedPackages,
            searchQuery = query.searchQuery,
            startInclusive = query.startInclusive,
            endExclusive = query.endExclusive,
            cursorReceivedAt = cursorReceivedAt,
            cursorId = cursorId,
            limit = limit
        )
        val pm = context.packageManager
        rows.map { row ->
            convertConversationRowToModel(row, AppInfoCache.label(pm, row.packageName))
        }
    }

    fun conversationDetail(conversationKey: String): StateFlow<ConversationDetailUiState> =
        detailState(conversationKey).asStateFlow()

    fun loadOlderMessages(conversationKey: String) {
        val stateFlow = detailState(conversationKey)
        val current = stateFlow.value
        if (!current.hasLoaded || current.endReached || current.isLoadingMore) return
        val mutex = detailMutex(conversationKey)
        scope.launch {
            mutex.withLock {
                val latest = stateFlow.value
                if (latest.endReached) return@withLock
                stateFlow.update { it.copy(isLoadingMore = true) }
                try {
                    val oldest = latest.messages.lastOrNull()
                    val page = loadMessagesPage(
                        conversationKey = conversationKey,
                        cursorReceivedAt = oldest?.receivedAtEpoch,
                        cursorId = oldest?.id,
                        limit = pageSize
                    )
                    stateFlow.update { state ->
                        val existingIds = state.messages.mapTo(hashSetOf()) { it.id }
                        state.copy(
                            messages = state.messages +
                                page.messages.filterNot { it.id in existingIds },
                            endReached = page.messages.size < pageSize
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load older conversation messages", e)
                } finally {
                    stateFlow.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    private fun detailState(conversationKey: String): MutableStateFlow<ConversationDetailUiState> =
        detailStates.getOrPut(conversationKey) {
            MutableStateFlow(ConversationDetailUiState(conversationKey)).also { stateFlow ->
                observeDetail(conversationKey, stateFlow)
            }
        }

    private fun detailMutex(conversationKey: String): Mutex =
        detailMutexes.getOrPut(conversationKey) { Mutex() }

    /**
     * Drops the detail state and its signature observer once the screen leaves composition.
     * Without this every opened conversation stayed resident (with a live collector) for the
     * lifetime of the ViewModel.
     */
    fun releaseDetail(conversationKey: String) {
        detailJobs.remove(conversationKey)?.cancel()
        detailStates.remove(conversationKey)
        detailMutexes.remove(conversationKey)
    }

    private fun observeDetail(
        conversationKey: String,
        stateFlow: MutableStateFlow<ConversationDetailUiState>
    ) {
        detailJobs[conversationKey] = scope.launch {
            stateFlow.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .flatMapLatest { active ->
                    if (active) dao.observeConversationSignature(conversationKey) else emptyFlow()
                }
                .collectLatest { reloadDetail(conversationKey, stateFlow) }
        }
    }

    private suspend fun reloadDetail(
        conversationKey: String,
        stateFlow: MutableStateFlow<ConversationDetailUiState>
    ) {
        detailMutex(conversationKey).withLock {
            val limit = stateFlow.value.messages.size.coerceAtLeast(pageSize)
            try {
                val page = loadMessagesPage(conversationKey, null, null, limit)
                val newest = page.messages.firstOrNull()
                stateFlow.update { state ->
                    state.copy(
                        title = page.newestConversationName
                            ?: newest?.title?.ifBlank { null }
                            ?: state.title.ifBlank { newest?.appName.orEmpty() },
                        packageName = newest?.packageName ?: state.packageName,
                        appName = newest?.appName ?: state.appName,
                        messages = page.messages,
                        hasLoaded = true,
                        endReached = page.messages.size < limit
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversation messages", e)
                stateFlow.update { it.copy(hasLoaded = true) }
            }
        }
    }

    private class MessagesPage(
        val messages: List<NotificationModel>,
        val newestConversationName: String?
    )

    private suspend fun loadMessagesPage(
        conversationKey: String,
        cursorReceivedAt: Long?,
        cursorId: Long?,
        limit: Int
    ): MessagesPage = withContext(Dispatchers.IO) {
        val entities = dao.getConversationMessagesPage(
            conversationKey = conversationKey,
            cursorReceivedAt = cursorReceivedAt,
            cursorId = cursorId,
            limit = limit
        )
        val pm = context.packageManager
        MessagesPage(
            messages = entities.map { entity ->
                convertEntityToModel(entity, AppInfoCache.label(pm, entity.packageName))
            },
            newestConversationName = entities.firstOrNull()?.conversationName?.ifBlank { null }
        )
    }

    /** Optimistically hides a notification that was just moved to Trash. */
    fun removeNotifications(ids: Set<Long>) {
        if (ids.isEmpty()) return
        detailStates.values.forEach { stateFlow ->
            stateFlow.update { state ->
                state.copy(messages = state.messages.filterNot { it.id in ids })
            }
        }
        listState.update { state ->
            state.copy(
                conversations = state.conversations.filterNot {
                    it.latestNotificationId in ids && it.messageCount <= 1
                }
            )
        }
    }

    /** Clear All History moved every active row to Trash. */
    fun clearAll() {
        listState.update { it.copy(conversations = emptyList(), endReached = true) }
        detailStates.values.forEach { stateFlow ->
            stateFlow.update { it.copy(messages = emptyList(), endReached = true) }
        }
    }

    private companion object {
        const val TAG = "ConversationHistory"
    }
}
