package com.droidnova.notificationhistory.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.ALLOWED_APPS_KEY
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.FILTERS_PREFIX
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.KEY_USER_WANTS_TRACKING
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.LAUNCH_COUNT
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.HISTORY_RETENTION_DAYS
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.IS_PREMIUM
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.SHOW_RATE_US_CARD
import com.droidnova.notificationhistory.data_shared.SettingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.dataStore by preferencesDataStore(name = DataStoreKeys.PREF_NAME)

class UserPreferences(private val context: Context) {

    private val allowedAppsKey = stringSetPreferencesKey(ALLOWED_APPS_KEY)

    val settingFlow: Flow<SettingState> = context.dataStore.data.map { preferences->
        val allowed = preferences[allowedAppsKey] ?: emptySet()
        SettingState(
            launchCount = preferences[LAUNCH_COUNT] ?: 0,
            showRateUsCard = preferences[SHOW_RATE_US_CARD]?: true,
            userToggleTracking = preferences[KEY_USER_WANTS_TRACKING] ?: true,
            selectedAppsCount = allowed.size,
            historyRetentionDays = preferences[HISTORY_RETENTION_DAYS]
                ?: SettingState.DEFAULT_HISTORY_RETENTION_DAYS,
           // allowedApps = allowed,
        )
    }


    suspend fun setToggleTracking(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_WANTS_TRACKING] = enabled
        }
    }

    // New: Flow allowed apps list
    val allowedApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[allowedAppsKey] ?: emptySet()
    }

    // Per-app title filters
    fun titleFiltersFor(packageName: String): Flow<Set<String>> =
        context.dataStore.data.map { prefs ->
            prefs[stringSetPreferencesKey(FILTERS_PREFIX + packageName)] ?: emptySet()
        }

    val allTitleFilters: Flow<Map<String, Set<String>>> = context.dataStore.data.map { prefs ->
        val map = mutableMapOf<String, Set<String>>()
        prefs.asMap().forEach { (key, value) ->
            val name = (key as Preferences.Key<*>).name
            if (name.startsWith(FILTERS_PREFIX)) {
                val pkg = name.removePrefix(FILTERS_PREFIX)
                @Suppress("UNCHECKED_CAST")
                val set = value as? Set<String> ?: emptySet()
                map[pkg] = set
            }
        }
        map
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_PREMIUM] ?: false
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_PREMIUM] = isPremium
        }
    }

    suspend fun allowApp(packageAppName: String) {
        context.dataStore.edit { prefs ->
            val currentAppsPackage = prefs[allowedAppsKey] ?: emptySet()
            prefs[allowedAppsKey] = currentAppsPackage + packageAppName
        }
    }

    suspend fun blockApp(packageAppName: String) {
        context.dataStore.edit { prefs ->
            val currentAppsPackage = prefs[allowedAppsKey] ?: emptySet()
            prefs[allowedAppsKey] = currentAppsPackage - packageAppName
        }
    }

    suspend fun setAllowedApps(packageNames: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[allowedAppsKey] = packageNames
        }
    }

    suspend fun addTitleFilter(packageName: String, title: String) {
        val key = stringSetPreferencesKey(FILTERS_PREFIX + packageName)
        context.dataStore.edit { prefs ->
            val curr = prefs[key] ?: emptySet()
            val newTitle = title.trim()
            if (newTitle.isNotEmpty()) {
                prefs[key] = curr + newTitle
            }
        }
    }

    suspend fun removeTitleFilter(packageName: String, title: String) {
        val key = stringSetPreferencesKey(FILTERS_PREFIX + packageName)
        context.dataStore.edit { prefs ->
            val curr = prefs[key] ?: emptySet()
            prefs[key] = curr - title
        }
    }

    suspend fun clearTitleFilters(packageName: String) {
        val key = stringSetPreferencesKey(FILTERS_PREFIX + packageName)
        context.dataStore.edit { prefs ->
            prefs[key] = emptySet()
        }
    }

    suspend fun updateLaunchCount(value:Int){
        context.dataStore.edit { preference->
            preference[LAUNCH_COUNT] = value
        }
    }

    suspend fun hideRateUsCard(){
        context.dataStore.edit {
            it[SHOW_RATE_US_CARD] = false
        }
    }
    suspend fun updateHistoryRetentionDays(days: Int) {
        context.dataStore.edit { preference ->
            preference[HISTORY_RETENTION_DAYS] = days
        }
    }
}
