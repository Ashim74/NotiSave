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
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.LISTENER_CONNECTED
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.ONBOARDING_COMPLETE
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.SHOW_RATE_US_CARD
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.THEME_MODE
import com.droidnova.notificationhistory.core.review.ReviewState
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.data_shared.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.dataStore by preferencesDataStore(name = DataStoreKeys.PREF_NAME)

/**
 * An explicit flag always wins. Without one (every install older than 0.20), a user who has
 * selected apps or launched more than once is an upgrader and must not see the intro again.
 * The launch counter increments before the first frame, so a brand-new install reads 1.
 */
internal fun resolveOnboardingComplete(
    storedFlag: Boolean?,
    hasAllowedApps: Boolean,
    launchCount: Int
): Boolean = storedFlag ?: (hasAllowedApps || launchCount > 1)

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

    // Written by the notification listener; lets the UI show a reconnect state.
    val listenerConnected: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[LISTENER_CONNECTED] ?: false
    }

    suspend fun setListenerConnected(connected: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[LISTENER_CONNECTED] = connected
        }
    }

    /**
     * The flag is only written by the onboarding flow (added in 0.20). Users upgrading from
     * older versions have no flag, so anyone who already selected apps or launched the app
     * more than once is treated as onboarded instead of being shown the intro.
     */
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        resolveOnboardingComplete(
            storedFlag = prefs[ONBOARDING_COMPLETE],
            hasAllowedApps = prefs[allowedAppsKey]?.isNotEmpty() == true,
            launchCount = prefs[LAUNCH_COUNT] ?: 0
        )
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = true
        }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromStorageKey(prefs[THEME_MODE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.storageKey
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

    /** Atomic read-modify-write; the previous read-then-write could lose concurrent increments. */
    suspend fun incrementLaunchCount() {
        context.dataStore.edit { preference ->
            preference[LAUNCH_COUNT] = (preference[LAUNCH_COUNT] ?: 0) + 1
        }
    }

    /**
     * Counts one "it helped" moment (the user read, copied or restored a saved notification) and
     * returns the review state after it. Also stamps the first-open time the first time it runs.
     */
    suspend fun recordValueMoment(now: Long): ReviewState {
        val prefs = context.dataStore.edit { p ->
            if (p[DataStoreKeys.FIRST_OPEN_AT] == null) p[DataStoreKeys.FIRST_OPEN_AT] = now
            p[DataStoreKeys.VALUE_MOMENTS] = (p[DataStoreKeys.VALUE_MOMENTS] ?: 0) + 1
        }
        return ReviewState(
            firstOpenAt = prefs[DataStoreKeys.FIRST_OPEN_AT] ?: now,
            launchCount = prefs[LAUNCH_COUNT] ?: 0,
            valueMoments = prefs[DataStoreKeys.VALUE_MOMENTS] ?: 0,
            lastAskedAt = prefs[DataStoreKeys.REVIEW_LAST_ASKED_AT],
            askCount = prefs[DataStoreKeys.REVIEW_ASK_COUNT] ?: 0
        )
    }

    /** Stamps the first-open time if it isn't set yet (called on every launch). */
    suspend fun markFirstOpen(now: Long) {
        context.dataStore.edit { p ->
            if (p[DataStoreKeys.FIRST_OPEN_AT] == null) p[DataStoreKeys.FIRST_OPEN_AT] = now
        }
    }

    suspend fun markReviewAsked(now: Long) {
        context.dataStore.edit { p ->
            p[DataStoreKeys.REVIEW_LAST_ASKED_AT] = now
            p[DataStoreKeys.REVIEW_ASK_COUNT] = (p[DataStoreKeys.REVIEW_ASK_COUNT] ?: 0) + 1
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
