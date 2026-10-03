package com.droidnova.notificationhistory.presentation.screens.insights

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.data.insights.InsightsData
import com.droidnova.notificationhistory.data.insights.InsightsRange
import com.droidnova.notificationhistory.data.insights.InsightsRepository
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class InsightsUiState(
    val range: InsightsRange = InsightsRange.Last7Days,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val data: InsightsData? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InsightsRepository(
        dao = AppDatabase.getInstance(application).notificationDao(),
        packageManager = application.packageManager,
        hiddenApps = UserPreferences(application).effectiveHiddenApps
    )

    private val selectedRange = MutableStateFlow(InsightsRange.Last7Days)

    /** Bumped to re-plan against the current clock (day change) or to retry after an error. */
    private val reloadTick = MutableStateFlow(0)

    private sealed interface Result {
        data object Loading : Result
        data object Error : Result
        data class Loaded(val data: InsightsData) : Result
    }

    val uiState: StateFlow<InsightsUiState> =
        combine(selectedRange, reloadTick) { range, _ -> range }
            .flatMapLatest { range ->
                repository.observe(range)
                    .map<InsightsData, Result> { Result.Loaded(it) }
                    .onStart { emit(Result.Loading) }
                    .catch { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "Failed to load insights for $range", e)
                        emit(Result.Error)
                    }
                    .map { result -> range to result }
            }
            .flowOn(Dispatchers.IO)
            .scan(InsightsUiState()) { previous, (range, result) ->
                when (result) {
                    // A refresh of the same range keeps the figures on screen instead of flashing
                    // the loader again; only a new range (or the first load) starts blank.
                    Result.Loading -> if (range == previous.range && previous.data != null) {
                        previous.copy(isLoading = true, hasError = false)
                    } else {
                        InsightsUiState(range = range, isLoading = true)
                    }
                    Result.Error -> InsightsUiState(range = range, isLoading = false, hasError = true)
                    is Result.Loaded -> InsightsUiState(
                        range = range,
                        isLoading = false,
                        data = result.data
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                InsightsUiState()
            )

    fun selectRange(range: InsightsRange) {
        selectedRange.value = range
    }

    /** Re-plans day boundaries against the current clock; call when the screen resumes. */
    fun refresh() {
        reloadTick.update { it + 1 }
    }

    private companion object {
        const val TAG = "InsightsViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
