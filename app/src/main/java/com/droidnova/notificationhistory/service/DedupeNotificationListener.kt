package com.droidnova.notificationhistory.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.data.db.NotificationDao
import com.droidnova.notificationhistory.data.db.NotificationEntity
import com.droidnova.notificationhistory.data_shared.SettingState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class NotificationListener : NotificationListenerService() {

    // “Thodi der ke liye model yaad rakhna” => DedupeCache
    private val cache = DedupeCache(ttlMs = 2000L) // 2s window

    // Service scope (IO)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var prefs: UserPreferences

    // Fast reads
    @Volatile private var allowedCache: Set<String> = emptySet()
    @Volatile private var trackingEnabled: Boolean = false
    @Volatile private var historyRetentionDays: Int = SettingState.DEFAULT_HISTORY_RETENTION_DAYS
    @Volatile private var titleFilters: Map<String, Set<String>> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        prefs = UserPreferences(applicationContext)

        runBlocking {
            allowedCache = prefs.allowedApps.first()
            val state = prefs.settingFlow.first()
            trackingEnabled = state.userToggleTracking
            historyRetentionDays = state.historyRetentionDays.coerceAtLeast(0)
            titleFilters = prefs.allTitleFilters.first()
        }

        serviceScope.launch {
            // Allowed apps list ko observe karo
            prefs.allowedApps.collect { set ->
                allowedCache = set
                Log.d("NLS", "allowed packages = $set")
            }
        }

        serviceScope.launch {
            prefs.settingFlow.collect { state ->
                trackingEnabled = state.userToggleTracking
                historyRetentionDays = state.historyRetentionDays.coerceAtLeast(0)
            }
        }

        serviceScope.launch {
            prefs.allTitleFilters.collect { map ->
                titleFilters = map
                Log.d("NLS", "title filters updated: ${map.mapValues { it.value.size }}")
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return

        // 1) Allowed app filter
        if (allowedCache.isNotEmpty() && pkg !in allowedCache) return

        // 2) (Optional) noisy notifications ko skip karo
        if (shouldSkip(sbn)) return

        // 3) Unique key (system-made), fallback if needed
        val key = sbn.key ?: "${pkg}:${sbn.id}:${sbn.tag ?: ""}"

        // 4) Content fingerprint (model ka “essence”): title + text
        val title = extractTitle(sbn.notification)
        val text = extractText(sbn.notification)
        val contentHash = (title + "|" + text).hashCode()

        // 5)  Atomic check+update BEFORE launching coroutine
        val allowed = cache.allowAndReserve(key, contentHash)
        if (!allowed) {
            Log.d("Dedupe", "Duplicate skip (within window): key=$key")
            return
        }

        // 6) DB insert (background)
        serviceScope.launch {
            if (!trackingEnabled) return@launch

            val dao = AppDatabase.getInstance(applicationContext).notificationDao()

            val entity = NotificationEntity(
                packageName = pkg,
                title = title,
                message = text,
                receivedAt = sbn.postTime
            )

            val filters = titleFilters[pkg] ?: emptySet() // Get filters user set for this app
            val titleLower = title.lowercase().replace("\\s".toRegex(), "") // Lowercase and remove all whitespace from title

            val shouldSave = if (filters.isEmpty()) {
                // No filters: save ALL notifications for this app
                true
            } else {
                // Filters exist: save ONLY if the title matches at least one filter
                filters.any { filter ->
                    titleLower.contains(filter.lowercase().replace("\\s".toRegex(), ""))
                }
            }

            if (!shouldSave) {
                // If 'shouldSave' is false: skip saving
                return@launch
            }
            Log.e("NLSs", "onNotificationPosted (matched): $entity")
            dao.insertApp(entity)  // Room 2.6+ ho to @Upsert best hai
            enforceRetention(dao)
        }
    }

    /** Optional: cut noise (rakho ya hatao apni need ke hisab se) */
    private fun shouldSkip(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isFgService = (n.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
        val hasMeaningfulContent = extractTitle(n).isNotBlank() || extractText(n).isNotBlank()
        return (isGroupSummary && !hasMeaningfulContent) || isOngoing || isFgService
    }

    private fun extractTitle(notification: Notification): String {
        val extras = notification.extras
        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: ""
    }

    private fun extractText(notification: Notification): String {
        val extras = notification.extras
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        return when {
            !extras.getCharSequence(Notification.EXTRA_TEXT).isNullOrBlank() ->
                extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            !textLines.isNullOrEmpty() -> textLines.joinToString("\n")
            !extras.getCharSequence(Notification.EXTRA_BIG_TEXT).isNullOrBlank() ->
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
            !extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).isNullOrBlank() ->
                extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString()
            else -> ""
        }
    }

    private suspend fun enforceRetention(dao: NotificationDao) {
        val retention = historyRetentionDays
        if (retention <= 0) return
        val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retention.toLong())
        dao.deleteNotificationsOlderThan(threshold)
    }
}
