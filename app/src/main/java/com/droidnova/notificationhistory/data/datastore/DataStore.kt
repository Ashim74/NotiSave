package com.droidnova.notificationhistory.data.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droidnova.notificationhistory.data.datastore.DataStoreKeys.ALLOWED_APPS_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.dataStore by preferencesDataStore(name = DataStoreKeys.PREF_NAME)

class UserPreferences(private val context: Context) {
    private val wantsKey = booleanPreferencesKey(DataStoreKeys.KEY_USER_WANTS_TRACKING)
    private val allowedAppsKey = stringSetPreferencesKey(ALLOWED_APPS_KEY)


    val userToggleTracking: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[wantsKey] ?: false
    }

    suspend fun setToggleTracking(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[wantsKey] = enabled
        }
    }

    // New: allowed apps list
    val allowedApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[allowedAppsKey] ?: emptySet()
    }

    suspend fun allowApp(packageAppName: String) {
        Log.e("Mantsh2232","dataStoreFucntionCalled allowApp() $packageAppName")
        context.dataStore.edit { prefs ->
            val currentAppsPackage = prefs[allowedAppsKey] ?: emptySet()
            prefs[allowedAppsKey] = currentAppsPackage + packageAppName
        }
    }

    suspend fun blockApp(packageAppName: String) {
        Log.e("Mantsh2232"," blockApp $packageAppName")
        context.dataStore.edit { prefs ->
            val currentAppsPackage = prefs[allowedAppsKey] ?: emptySet()
            prefs[allowedAppsKey] = currentAppsPackage - packageAppName
        }
    }
}