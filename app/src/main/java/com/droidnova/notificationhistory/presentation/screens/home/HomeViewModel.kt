package com.droidnova.notificationhistory.presentation.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.notificationhistory.core.apps.AppInfoCache
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.data.mapper.convertEntityToModel
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.utils.CrashReporter
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Today's headline numbers for the Home dashboard. */
data class HomeTodaySummary(
    val total: Int = 0,
    val activeApps: Int = 0,
    val topAppPackage: String? = null,
    val topAppLabel: String? = null,
    val topAppCount: Int = 0
)

/**
 * Dashboard data for Home: today's counts and the most recent notifications. Kept apart from
 * [com.droidnova.notificationhistory.MainViewModel] so the shared ViewModel stops growing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).notificationDao()
    private val packageManager = application.packageManager
    private val hiddenApps = UserPreferences(application).effectiveHiddenApps.map { it.toList() }

    // Midnight can pass while the app is backgrounded; refresh() re-anchors the window.
    private val dayStart = MutableStateFlow(startOfTodayMillis())

    fun refresh() {
        dayStart.value = startOfTodayMillis()
    }

    val todaySummary: StateFlow<HomeTodaySummary> = combine(dayStart, hiddenApps) { start, hidden -> start to hidden }
        .flatMapLatest { (start, hidden) ->
            combine(
                dao.observeInsightsSummary(start, Long.MAX_VALUE),
                // A hidden app must not surface as today's most active one.
                dao.observeTopApps(start, Long.MAX_VALUE, 1, hidden)
            ) { summary, topApps ->
                val top = topApps.firstOrNull()
                HomeTodaySummary(
                    total = summary.total,
                    activeApps = summary.activeApps,
                    topAppPackage = top?.packageName,
                    topAppLabel = top?.let { AppInfoCache.label(packageManager, it.packageName) },
                    topAppCount = top?.count ?: 0
                )
            }
        }
        .flowOn(Dispatchers.IO)
        .catch { error ->
            CrashReporter.record(error)
            emit(HomeTodaySummary())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeTodaySummary())

    val recentNotifications: StateFlow<List<NotificationModel>> = hiddenApps
        .flatMapLatest { hidden -> dao.observeRecentActive(RECENT_LIMIT, hidden) }
        .map { entities -> entities.map { convertEntityToModel(getApplication(), it) } }
        .flowOn(Dispatchers.IO)
        .catch { error ->
            CrashReporter.record(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private companion object {
        const val RECENT_LIMIT = 5

        fun startOfTodayMillis(): Long {
            val zone = ZoneId.systemDefault()
            return LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        }
    }
}
