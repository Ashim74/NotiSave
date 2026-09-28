package com.droidnova.notificationhistory

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.notificationhistory.core.permission.NotificationAccessChecker
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.data.mapper.convertEntityToModel
import com.droidnova.notificationhistory.data.model.HistoryDateFilter
import com.droidnova.notificationhistory.data.model.HistoryFilterState
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data.model.toDateBounds
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.screens.home.HomeUiEvent
import com.droidnova.notificationhistory.presentation.screens.select_app.AppInfo
import com.droidnova.notificationhistory.utils.getInstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.jvm.Volatile

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).notificationDao()
    private val userPrefs = UserPreferences(application)

    private val _settingState = MutableStateFlow(SettingState())
    val settingState: StateFlow<SettingState> = _settingState.asStateFlow()

    private val _hasNotificationAccess = MutableStateFlow(false)
    val hasNotificationAccess: StateFlow<Boolean> = _hasNotificationAccess.asStateFlow()

    private val _events = MutableSharedFlow<HomeUiEvent>()
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    private val _history = MutableStateFlow<List<NotificationModel>>(emptyList())
    val history: StateFlow<List<NotificationModel>> = _history.asStateFlow()

    data class HistoryLoadState(
        val isLoadingMore: Boolean = false,
        val endReached: Boolean = false
    )

    private val _historyLoadState = MutableStateFlow(HistoryLoadState())
    val historyLoadState: StateFlow<HistoryLoadState> = _historyLoadState.asStateFlow()

    private val _historyFilters = MutableStateFlow(HistoryFilterState())
    val historyFilters: StateFlow<HistoryFilterState> = _historyFilters.asStateFlow()

    private val _appSummaries = MutableStateFlow<List<NotificationModel>>(emptyList())
    val appSummaries: StateFlow<List<NotificationModel>> = _appSummaries.asStateFlow()

    private val _titleFilters = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val titleFilters: StateFlow<Map<String, Set<String>>> = _titleFilters.asStateFlow()

    private val _isHistoryRefreshing = MutableStateFlow(false)
    val isHistoryRefreshing: StateFlow<Boolean> = _isHistoryRefreshing.asStateFlow()

    val isPremium: StateFlow<Boolean> =
        userPrefs.isPremium.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false
        )

    private val _showPremiumWelcome = MutableStateFlow(false)
    val showPremiumWelcome: StateFlow<Boolean> = _showPremiumWelcome.asStateFlow()
    private var hasRemoveAdsClick = false
    private var lastPremiumStatus = false

    private val pageSize = 100
    private var endReached = false
    private var hasLoadedInitialHistory = false
    private var lastRetentionDays = SettingState.DEFAULT_HISTORY_RETENTION_DAYS
    private var historyLoadGeneration = 0
    private var latestHistoryCursor: HistoryCursor? = null
    private var historyCursor: HistoryCursor? = null
    private var activeHistoryQuery = HistoryPageQuery()
    private var historySearchJob: Job? = null

    @Volatile
    private var activeHistoryLoadGeneration: Int? = null
    private var refreshGeneration: Int? = null


    private val _allInstalledApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allInstalledApps: StateFlow<List<AppInfo>> =
        combine(_allInstalledApps, userPrefs.allowedApps) { apps, allowed ->
            apps.map { it.copy(isAllowed = it.packageName in allowed) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    private var allowedPackagesSet = emptySet<String>()
    private var awaitingGrant = false

    data class AppHistoryUiState(
        val notifications: List<NotificationModel> = emptyList(),
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val endReached: Boolean = false,
        val searchQuery: String = "",
        val cursorReceivedAt: Long? = null,
        val cursorId: Long? = null,
        val generation: Int = 0
    )

    private val appHistoryStates =
        mutableMapOf<String, MutableStateFlow<AppHistoryUiState>>()
    private val appHistorySearchJobs = mutableMapOf<String, Job>()

    private data class HistoryCursor(val receivedAt: Long, val id: Long)

    private data class HistoryPageQuery(
        val packageName: String? = null,
        val searchQuery: String = "",
        val startInclusive: Long = Long.MIN_VALUE,
        val endExclusive: Long = Long.MAX_VALUE
    )


    init {
        viewModelScope.launch {
            userPrefs.allTitleFilters.collect { map ->
                _titleFilters.value = map
            }
        }

        viewModelScope.launch {
            userPrefs.allowedApps.collect { allowed ->
                allowedPackagesSet = allowed
            }
        }
        viewModelScope.launch {
            userPrefs.settingFlow.collectLatest { settingState ->
                val retentionChanged = settingState.historyRetentionDays != lastRetentionDays
                lastRetentionDays = settingState.historyRetentionDays
                _settingState.value = settingState
                if (!hasLoadedInitialHistory) {
                    hasLoadedInitialHistory = true
                    refreshHistory(settingState.historyRetentionDays)
                } else if (retentionChanged) {
                    refreshHistory(settingState.historyRetentionDays)
                }
            }
        }
        viewModelScope.launch {
            dao.observeLatestNotification().collectLatest { latest ->
                if (latest == null) {
                    if (_history.value.isNotEmpty()) {
                        resetHistoryAndLoad()
                    }
                    latestHistoryCursor = null
                    return@collectLatest
                }

                val latestCursor = HistoryCursor(latest.receivedAt, latest.id)
                val previous = latestHistoryCursor
                latestHistoryCursor = latestCursor

                if (previous != null && latestCursor.isNewerThan(previous)) {
                    resetHistoryAndLoad()
                } else if (previous == null && hasLoadedInitialHistory && _history.value.isEmpty()) {
                    resetHistoryAndLoad()
                }
            }
        }
        viewModelScope.launch {
            dao.observeLatestNotificationsByApp().collectLatest { entities ->
                _appSummaries.value = entities.map {
                    convertEntityToModel(getApplication(), it)
                }
            }
        }
        refreshListenerGranted()
    }

    /** Called when user taps "Enable". */
    fun onEnableClick() {
        val granted = NotificationAccessChecker.hasNotificationAccessPermission(getApplication())
        if (granted) {
            viewModelScope.launch {
                enableTrackingAndRouteIfNeeded()
            }
        } else {
            awaitingGrant = true
        }
    }

    /** Call from UI when screen resumes (user could have granted in Settings). */
    fun onResume() {
        val granted = NotificationAccessChecker.hasNotificationAccessPermission(getApplication())
        _hasNotificationAccess.value = granted
        if (granted) {
            if (awaitingGrant) {
                awaitingGrant = false
                viewModelScope.launch {
                    enableTrackingAndRouteIfNeeded()
                }
            }
        } else {
            if (awaitingGrant) {
                awaitingGrant = false
                viewModelScope.launch {
                    _events.emit(HomeUiEvent.ShowPermissionRequiredMessage)
                }
            }
            viewModelScope.launch {
                userPrefs.setToggleTracking(false)
            }
        }
    }

    fun onPermissionSettingsOpened() {
        awaitingGrant = true
    }

    fun setToggleTracking(isSwitchOn: Boolean) {
        viewModelScope.launch {
            userPrefs.setToggleTracking(isSwitchOn)
        }
    }

    fun refreshListenerGranted() = onResume()

    fun incrementLaunchCount() {
        viewModelScope.launch {
            val setting = userPrefs.settingFlow.first()
            userPrefs.updateLaunchCount(setting.launchCount + 1)
        }
    }

    fun getAllInstalledApps(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val allApps = getInstalledApps(context, allowedPackagesSet)
                _allInstalledApps.value = allApps
            }
        }
    }

    /** Call when user toggles an app in the allow_notify_screen  UI */
    fun addToAllowedApps(packageName: String, add: Boolean) {
        viewModelScope.launch {
            if (add) userPrefs.allowApp(packageName) else userPrefs.blockApp(packageName)
            // No manual refresh needed: allowedApps flow triggers recompute.
        }
    }

    suspend fun setAllowedAppsForPackages(packageNames: List<String>, add: Boolean) {
        withContext(Dispatchers.IO) {
            val updatedAllowedApps = if (add) packageNames.toSet() else emptySet()
            userPrefs.setAllowedApps(updatedAllowedApps)
        }
    }

    fun addTitleFilter(packageName: String, title: String) {
        // Update cache immediately
        _titleFilters.update { current ->
            val updated = current[packageName].orEmpty().plus(title)
            current.toMutableMap().apply { put(packageName, updated) }
        }
        // Persist in DataStore
        viewModelScope.launch {
            userPrefs.addTitleFilter(packageName, title)
        }
    }

    fun removeTitleFilter(packageName: String, title: String) {
        viewModelScope.launch {
            userPrefs.removeTitleFilter(packageName, title)
        }
    }

    fun clearTitleFilters(packageName: String) {
        viewModelScope.launch {
            userPrefs.clearTitleFilters(packageName)
        }
    }

    fun refreshHistory() {
        refreshHistory(lastRetentionDays)
    }

    fun updateHistorySearchQuery(query: String) {
        historySearchJob?.cancel()
        historySearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val normalized = normalizeSearchQuery(query)
            if (_historyFilters.value.searchQuery != normalized) {
                _historyFilters.update { it.copy(searchQuery = normalized) }
                resetHistoryAndLoad()
            }
        }
    }

    fun setHistoryAppFilter(packageName: String?) {
        if (_historyFilters.value.packageName == packageName) return
        _historyFilters.update { it.copy(packageName = packageName) }
        resetHistoryAndLoad()
    }

    fun setHistoryDateFilter(
        dateFilter: HistoryDateFilter,
        customStartDate: LocalDate? = null,
        customEndDate: LocalDate? = null
    ): Boolean {
        if (dateFilter == HistoryDateFilter.Custom &&
            (customStartDate == null || customEndDate == null || customStartDate.isAfter(customEndDate))
        ) return false

        val updated = _historyFilters.value.copy(
            dateFilter = dateFilter,
            customStartDate = if (dateFilter == HistoryDateFilter.Custom) customStartDate else null,
            customEndDate = if (dateFilter == HistoryDateFilter.Custom) customEndDate else null
        )
        if (updated == _historyFilters.value) return true
        _historyFilters.value = updated
        resetHistoryAndLoad()
        return true
    }

    fun clearHistoryFilters() {
        val current = _historyFilters.value
        if (!current.hasActiveFilters) return
        _historyFilters.value = current.copy(
            packageName = null,
            dateFilter = HistoryDateFilter.AllTime,
            customStartDate = null,
            customEndDate = null
        )
        resetHistoryAndLoad()
    }

    private fun refreshHistory(retentionDays: Int) {
        val sanitizedDays = retentionDays.coerceAtLeast(0)
        _isHistoryRefreshing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            if (sanitizedDays > 0) {
                val threshold = System.currentTimeMillis() -
                        TimeUnit.DAYS.toMillis(sanitizedDays.toLong())
                dao.deleteNotificationsOlderThan(threshold)
            }
            withContext(Dispatchers.Main) {
                resetHistoryAndLoad()
            }
        }
    }

    private fun resetHistoryAndLoad() {
        historyLoadGeneration++
        refreshGeneration = historyLoadGeneration
        activeHistoryLoadGeneration = null
        historyCursor = null
        endReached = false
        activeHistoryQuery = _historyFilters.value.toPageQuery()
        _history.value = emptyList()
        _historyLoadState.value = HistoryLoadState()
        _isHistoryRefreshing.value = true
        loadMoreHistory()
    }

    fun loadMoreHistory() {
        if (endReached || _historyLoadState.value.isLoadingMore) return
        val generation = historyLoadGeneration
        if (activeHistoryLoadGeneration == generation) return
        activeHistoryLoadGeneration = generation
        _historyLoadState.value = _historyLoadState.value.copy(isLoadingMore = true)
        val cursor = historyCursor
        val query = activeHistoryQuery
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entities = dao.getHistoryPage(
                    packageName = query.packageName,
                    searchQuery = query.searchQuery,
                    startInclusive = query.startInclusive,
                    endExclusive = query.endExclusive,
                    cursorReceivedAt = cursor?.receivedAt,
                    cursorId = cursor?.id,
                    limit = pageSize
                )
                val models = entities.map { convertEntityToModel(getApplication(), it) }
                if (generation != historyLoadGeneration) {
                    return@launch
                }
                entities.lastOrNull()?.let {
                    historyCursor = HistoryCursor(it.receivedAt, it.id)
                }
                if (models.isNotEmpty()) {
                    _history.update { current ->
                        val existingIds = current.asSequence().map { it.id }.toHashSet()
                        current + models.filterNot { it.id in existingIds }
                    }
                }
                if (models.size < pageSize) {
                    endReached = true
                    _historyLoadState.update { it.copy(endReached = true) }
                }
            } finally {
                if (activeHistoryLoadGeneration == generation) {
                    activeHistoryLoadGeneration = null
                    if (refreshGeneration == generation) {
                        refreshGeneration = null
                        _isHistoryRefreshing.value = false
                    }
                    _historyLoadState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    fun clearAllHistory() {
        clearAllHistoryUiState()
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllNotifications()
        }
    }

    private fun clearAllHistoryUiState() {
        historyLoadGeneration++
        activeHistoryLoadGeneration = null
        refreshGeneration = null
        historyCursor = null
        endReached = true
        _history.value = emptyList()
        _historyLoadState.value = HistoryLoadState(endReached = true)
        _isHistoryRefreshing.value = false
        _appSummaries.value = emptyList()
        appHistoryStates.values.forEach { stateFlow ->
            val current = stateFlow.value
            stateFlow.value = current.copy(
                notifications = emptyList(),
                isRefreshing = false,
                isLoadingMore = false,
                endReached = true,
                cursorReceivedAt = null,
                cursorId = null,
                generation = current.generation + 1
            )
        }
    }

    fun updateHistoryRetentionDays(days: Int) {
        val sanitizedDays = days.coerceAtLeast(0)
        if (sanitizedDays == lastRetentionDays) return
        viewModelScope.launch {
            userPrefs.updateHistoryRetentionDays(sanitizedDays)
        }
    }

    private fun getAppHistoryState(packageName: String): MutableStateFlow<AppHistoryUiState> {
        return appHistoryStates.getOrPut(packageName) { MutableStateFlow(AppHistoryUiState()) }
    }

    fun appHistoryState(packageName: String): StateFlow<AppHistoryUiState> =
        getAppHistoryState(packageName).asStateFlow()

    fun ensureAppHistoryLoaded(packageName: String) {
        val state = getAppHistoryState(packageName).value
        if (state.notifications.isEmpty() && !state.isLoadingMore && !state.isRefreshing) {
            refreshAppHistory(packageName)
        }
    }

    fun refreshAppHistory(packageName: String) {
        val stateFlow = getAppHistoryState(packageName)
        resetAppHistory(packageName, stateFlow.value.searchQuery)
    }

    fun updateAppHistorySearchQuery(packageName: String, query: String) {
        appHistorySearchJobs.remove(packageName)?.cancel()
        appHistorySearchJobs[packageName] = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val normalized = normalizeSearchQuery(query)
            if (getAppHistoryState(packageName).value.searchQuery != normalized) {
                resetAppHistory(packageName, normalized)
            }
        }
    }

    private fun resetAppHistory(packageName: String, searchQuery: String) {
        val stateFlow = getAppHistoryState(packageName)
        val nextGeneration = stateFlow.value.generation + 1
        stateFlow.value = AppHistoryUiState(
            notifications = emptyList(),
            isRefreshing = true,
            isLoadingMore = false,
            endReached = false,
            searchQuery = searchQuery,
            generation = nextGeneration
        )
        loadMoreAppHistory(packageName, nextGeneration)
    }

    fun loadMoreAppHistory(packageName: String, generationOverride: Int? = null) {
        val stateFlow = getAppHistoryState(packageName)
        val current = stateFlow.value
        if (current.endReached || current.isLoadingMore) return
        val generation = generationOverride ?: current.generation
        stateFlow.value = current.copy(isLoadingMore = true)
        viewModelScope.launch(Dispatchers.IO) {
            val entities = dao.getHistoryPage(
                packageName = packageName,
                searchQuery = current.searchQuery,
                startInclusive = Long.MIN_VALUE,
                endExclusive = Long.MAX_VALUE,
                cursorReceivedAt = current.cursorReceivedAt,
                cursorId = current.cursorId,
                limit = pageSize,
            )
            val models = entities.map { convertEntityToModel(getApplication(), it) }
            withContext(Dispatchers.Main) {
                val latest = stateFlow.value
                if (latest.generation != generation) {
                    return@withContext
                }
                val existingIds = latest.notifications.asSequence().map { it.id }.toHashSet()
                val updatedNotifications =
                    latest.notifications + models.filterNot { it.id in existingIds }
                val reachedEnd = models.size < pageSize
                val nextCursor = entities.lastOrNull()
                stateFlow.value = latest.copy(
                    notifications = updatedNotifications,
                    endReached = reachedEnd,
                    isLoadingMore = false,
                    isRefreshing = false,
                    cursorReceivedAt = nextCursor?.receivedAt ?: latest.cursorReceivedAt,
                    cursorId = nextCursor?.id ?: latest.cursorId
                )
            }
        }
    }

    fun deleteNotification(notification: NotificationModel) {
        removeNotificationFromUi(notification.id)
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteNotificationById(notification.id)
        }
    }

    private fun removeNotificationFromUi(notificationId: Long) {
        _history.update { current -> current.filterNot { it.id == notificationId } }
        _appSummaries.update { current -> current.filterNot { it.id == notificationId } }
        appHistoryStates.values.forEach { stateFlow ->
            stateFlow.update { current ->
                current.copy(
                    notifications = current.notifications.filterNot { it.id == notificationId }
                )
            }
        }
    }

    fun hideRateUsCard() {
        viewModelScope.launch {
            userPrefs.hideRateUsCard()
        }
    }

    fun resetLaunchCount() {
        viewModelScope.launch {
            userPrefs.updateLaunchCount(0)
        }
    }

    fun setPremiumPurchased(isPremium: Boolean) {
        viewModelScope.launch {
            userPrefs.setPremium(isPremium)
        }
        val shouldShowWelcome = isPremium && !lastPremiumStatus && hasRemoveAdsClick
        _showPremiumWelcome.value = shouldShowWelcome
        if (isPremium) {
            hasRemoveAdsClick = false
        }
        lastPremiumStatus = isPremium
    }

    fun dismissPremiumWelcome() {
        _showPremiumWelcome.value = false
    }

    fun onRemoveAdsClicked() {
        hasRemoveAdsClick = true
    }

    private fun HistoryFilterState.toPageQuery(): HistoryPageQuery {
        val bounds = toDateBounds()
        return HistoryPageQuery(
            packageName = packageName,
            searchQuery = searchQuery,
            startInclusive = bounds.startInclusive,
            endExclusive = bounds.endExclusive
        )
    }

    private fun HistoryCursor.isNewerThan(other: HistoryCursor): Boolean =
        receivedAt > other.receivedAt || (receivedAt == other.receivedAt && id > other.id)

    private suspend fun enableTrackingAndRouteIfNeeded() {
        userPrefs.setToggleTracking(true)
        if (userPrefs.allowedApps.first().isEmpty()) {
            _events.emit(HomeUiEvent.NavigateToSelectApps)
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        val SEARCH_WHITESPACE = Regex("\\s+")

        fun normalizeSearchQuery(query: String): String =
            query.trim().replace(SEARCH_WHITESPACE, " ")
    }
}
