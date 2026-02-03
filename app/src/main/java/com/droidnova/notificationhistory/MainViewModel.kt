package com.droidnova.notificationhistory

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.notificationhistory.core.permission.NotificationAccessChecker
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.data.mapper.convertEntityToModel
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.screens.home.HomeUiEvent
import com.droidnova.notificationhistory.presentation.screens.select_app.AppInfo
import com.droidnova.notificationhistory.utils.getInstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.jvm.Volatile

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).notificationDao()
    private val userPrefs = UserPreferences(application)

    private val _settingState = MutableStateFlow(SettingState())
    val settingState: StateFlow<SettingState> = _settingState.asStateFlow()

    private val _events = MutableSharedFlow<HomeUiEvent>()
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    private val _history = MutableStateFlow<List<NotificationModel>>(emptyList())
    val history: StateFlow<List<NotificationModel>> = _history.asStateFlow()

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

    private var currentOffset = 0
    private val pageSize = 100
    private var endReached = false
    private var hasLoadedInitialHistory = false
    private var lastRetentionDays = SettingState.DEFAULT_HISTORY_RETENTION_DAYS
    private var historyLoadGeneration = 0
    private var latestHistoryTimestamp: Long? = null

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
    private var isInitialized = false


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
                if (!isInitialized) {
                    isInitialized = true
                    userPrefs.updateLaunchCount(settingState.launchCount + 1)
                    if (settingState.snoozeUntilLaunch < 2) {
                        userPrefs.updateSnoozeUntilLaunch(2)
                    }
                }
                if (!hasLoadedInitialHistory) {
                    hasLoadedInitialHistory = true
                    refreshHistory(settingState.historyRetentionDays)
                } else if (retentionChanged) {
                    refreshHistory(settingState.historyRetentionDays)
                }
            }
        }
        viewModelScope.launch {
            dao.observeLatestReceivedAt().collectLatest { latest ->
                if (latest == null) {
                    if (_history.value.isNotEmpty()) {
                        refreshHistory(lastRetentionDays)
                    }
                    latestHistoryTimestamp = null
                    return@collectLatest
                }

                val previous = latestHistoryTimestamp
                latestHistoryTimestamp = latest

                if (previous != null && latest > previous) {
                    refreshHistory(lastRetentionDays)
                } else if (previous == null && hasLoadedInitialHistory && _history.value.isEmpty()) {
                    refreshHistory(lastRetentionDays)
                }
            }
        }
        refreshListenerGranted()
    }

    /** Called when user taps "Enable". */
    fun onEnableClick() {
        Log.e("yourTag", "enable")
        val granted = NotificationAccessChecker.hasNotificationAccessPermission(getApplication())
        if (granted) {
            // Already granted: persist toggle + do work
            viewModelScope.launch {
                userPrefs.setToggleTracking(true)
            }
        } else {
            // Not granted: ask user
            awaitingGrant = true
            viewModelScope.launch {
                _events.emit(HomeUiEvent.OpenNotificationAccessSettings)
            }
        }
    }

    /** Call from UI when screen resumes (user could have granted in Settings). */
    fun onResume() {
        Log.e("yourTag", "onResume")
        val granted = NotificationAccessChecker.hasNotificationAccessPermission(getApplication())
        if (granted) {
            if (awaitingGrant) {
                awaitingGrant = false
                viewModelScope.launch {
                    userPrefs.setToggleTracking(true)
                    _events.emit(HomeUiEvent.DoWorkAfterEnabled)
                }
            }
        } else {
        }
    }

    fun setToggleTracking(isSwitchOn: Boolean) {
        Log.e("yourTag", "setToggleTracking")
        viewModelScope.launch {
            userPrefs.setToggleTracking(isSwitchOn)
        }
    }

    fun refreshListenerGranted() = onResume()

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
                historyLoadGeneration++
                refreshGeneration = historyLoadGeneration
                activeHistoryLoadGeneration = null
                currentOffset = 0
                endReached = false
                _history.value = emptyList()
                loadMoreHistory()
            }
        }
    }

    fun loadMoreHistory() {
        if (endReached) return
        val generation = historyLoadGeneration
        if (activeHistoryLoadGeneration == generation) return
        activeHistoryLoadGeneration = generation
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entities = dao.getNotifications(pageSize, currentOffset)
                val models = entities.map { convertEntityToModel(getApplication(), it) }
                if (generation != historyLoadGeneration) {
                    return@launch
                }
                if (models.isNotEmpty()) {
                    currentOffset += models.size
                    _history.update { it + models }
                }
                if (models.size < pageSize) {
                    endReached = true
                }
            } finally {
                if (activeHistoryLoadGeneration == generation) {
                    activeHistoryLoadGeneration = null
                    if (refreshGeneration == generation) {
                        refreshGeneration = null
                        _isHistoryRefreshing.value = false
                    }
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllNotifications()
            withContext(Dispatchers.Main) {
                historyLoadGeneration++
                activeHistoryLoadGeneration = null
                currentOffset = 0
                endReached = false
                _history.value = emptyList()
            }
        }
    }

    fun updateHistoryRetentionDays(days: Int) {
        val sanitizedDays = days.coerceAtLeast(0)
        if (sanitizedDays == lastRetentionDays) return
        viewModelScope.launch {
            userPrefs.updateHistoryRetentionDays(sanitizedDays)
        }
    }

    fun observeNotificationsForPackage(packageName: String) =
        dao.observeNotificationsByPackage(packageName).map { entities ->
            entities.map { convertEntityToModel(getApplication(), it) }
        }

    fun observeLatestNotificationsByApp() =
        dao.observeLatestNotificationsByApp().map { entities ->
            entities.map { convertEntityToModel(getApplication(), it) }
        }

    fun deleteNotification(notification: NotificationModel) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteNotificationById(notification.id)
            withContext(Dispatchers.Main) {
                _history.update { current ->
                    current.filterNot { it.id == notification.id }
                }
            }
        }
    }

    fun hideRateUsCard() {
        viewModelScope.launch {
            userPrefs.hideRateUsCard()
        }
    }

    fun snoozeRateUsCard() {
        viewModelScope.launch {
            val current = _settingState.value.launchCount
            userPrefs.updateSnoozeUntilLaunch(current + 2)
        }
    }

    fun setPremiumPurchased(isPremium: Boolean) {
        viewModelScope.launch {
            userPrefs.setPremium(isPremium)
        }
    }
}
