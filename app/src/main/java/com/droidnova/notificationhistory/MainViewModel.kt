package com.droidnova.notificationhistory

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.droidnova.notificationhistory.core.permission.NotificationAccessChecker
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.data.db.NotificationEntity
import com.droidnova.notificationhistory.data.mapper.convertEntityToModel
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.screens.home.HomeUiEvent
import com.droidnova.notificationhistory.presentation.screens.manage_notification.AppInfo
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.compareTo
import kotlin.rem

class MainViewModel(application: Application): AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).notificationDao()
    private val userPrefs = UserPreferences(application)

    private val _settingState = MutableStateFlow(SettingState())
    val settingState: StateFlow<SettingState> = _settingState.asStateFlow()

    private val _events = MutableSharedFlow<HomeUiEvent>()
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    private val _history = MutableStateFlow<List<NotificationModel>>(emptyList())
    val history: StateFlow<List<NotificationModel>> = _history.asStateFlow()
    private var currentOffset = 0
    private val pageSize = 100
    private var endReached = false


    private val _allInstalledApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allInstalledApps: StateFlow<List<AppInfo>> =
        combine(_allInstalledApps, userPrefs.allowedApps) { apps, allowed ->
            apps.map { it.copy(isAllowed = it.packageName in allowed) }
                .sortedWith(
                    compareByDescending<AppInfo> { it.isAllowed }
                        .thenBy { it.appName.lowercase() }
                )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    private var allowedPackagesSet = emptySet<String>()
    private var awaitingGrant = false
    private var isInitialized = false


    init {

        viewModelScope.launch {
            userPrefs.allowedApps.collect { allowed ->
                allowedPackagesSet = allowed
                _settingState.update { it.copy(selectedAppsCount = allowed.size) }
            }
        }
        viewModelScope.launch {
            userPrefs.settingFlow.collectLatest {settingState->
                _settingState.value = settingState
                if (!isInitialized) {
                    isInitialized = true
                    userPrefs.updateLaunchCount(settingState.launchCount + 1)
                    if (settingState.snoozeUntilLaunch < 2) {
                        userPrefs.updateSnoozeUntilLaunch(2)
                    }
                }
            }
        }
        refreshListenerGranted()
        loadMoreHistory()
    }
    /** Called when user taps "Enable". */
    fun onEnableClick() {
        Log.d("MyTAG", "MainViewModel.onEnableClick() called")
        val granted = NotificationAccessChecker.hasNotificationAccessPermission(getApplication())
        Log.d("MyTAG", "MainViewModel.onEnableClick() granted=$granted")
        if (granted) {
            // Already granted: persist toggle + do work
            viewModelScope.launch {
                userPrefs.setToggleTracking(true)
            }
        } else {
            Log.d("MyTAG","MainViewModel.onEnableClick() not granted")
            // Not granted: ask user
            awaitingGrant = true
            Log.e("Mantsha","MainViewModel.onEnableClick() awaitingGrant=$awaitingGrant")
            viewModelScope.launch {
                _events.emit(HomeUiEvent.OpenNotificationAccessSettings)
            }
        }
    }

    /** Call from UI when screen resumes (user could have granted in Settings). */
    fun onResume() {
        Log.e("switch", "onresumefunction called ")
        val granted = NotificationAccessChecker.hasNotificationAccessPermission(getApplication())
        Log.e("MyTAG", "MainViewModel.onResume() granted=$granted")
        if (granted) {
            if (awaitingGrant) {
                awaitingGrant = false
                viewModelScope.launch {
                    Log.e("switch", "MainViewModel.onResume() granted=true")
                    userPrefs.setToggleTracking(true)
                  _events.emit(HomeUiEvent.DoWorkAfterEnabled)
                }
            }
        } else {
            Log.e("switch", "MainViewModel.onResume() granted=false")
        }
    }

    fun setToggleTracking(isSwitchOn: Boolean) {
        Log.d("MyTAG", "MainViewModel.onUserWantsTrackingChange() called with isSwitchOn=$isSwitchOn")
        viewModelScope.launch {
            userPrefs.setToggleTracking(isSwitchOn)
        }
    }

    fun refreshListenerGranted() = onResume()

    fun getAllInstalledApps(context: Context) {
        Log.d("MyTAG", "MainViewModel.getAllInstalledApps() called")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val allApps = getInstalledApps(context,allowedPackagesSet)
                _allInstalledApps.value = allApps
            }
        }
    }

    /** Call when user toggles an app in the UI */
    fun addToAllowedApps(packageName: String, add: Boolean) {
        Log.e("Mantsh2232"," viewmodel functionaddToAllowedAppsCalled()  packgename $packageName,checked${Boolean}")
        viewModelScope.launch {
            if (add) userPrefs.allowApp(packageName) else userPrefs.blockApp(packageName)
            // No manual refresh needed: allowedApps flow triggers recompute.
        }
    }
    fun loadMoreHistory() {
        if (endReached) return
        viewModelScope.launch(Dispatchers.IO) {
            val entities = dao.getNotifications(pageSize, currentOffset)
            val models = entities.map { convertEntityToModel(getApplication(), it) }
            if (models.isNotEmpty()) {
                currentOffset += models.size
                _history.update { it + models }
            }
            if (models.size < pageSize) {
                endReached = true
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllNotifications()
            currentOffset = 0
            endReached = false
            _history.value = emptyList()
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
}