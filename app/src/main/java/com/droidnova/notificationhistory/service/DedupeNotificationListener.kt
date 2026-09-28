package com.droidnova.notificationhistory.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.droidnova.notificationhistory.core.permission.NotificationAccessChecker
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.data.db.NotificationDao
import com.droidnova.notificationhistory.data.db.NotificationEntity
import com.droidnova.notificationhistory.data_shared.SettingState
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NotificationListener : NotificationListenerService() {

    private val cache = DedupeCache(ttlMs = 2000L)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastRetentionCleanupAt = AtomicLong(0L)

    private lateinit var prefs: UserPreferences

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
            prefs.allowedApps.collect { allowedCache = it }
        }

        serviceScope.launch {
            prefs.settingFlow.collect { state ->
                trackingEnabled = state.userToggleTracking
                historyRetentionDays = state.historyRetentionDays.coerceAtLeast(0)
            }
        }

        serviceScope.launch {
            prefs.allTitleFilters.collect { titleFilters = it }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return

        // Preserve M1: permission, user tracking, and explicit package selection are all required.
        if (!shouldCaptureNotification(
                hasNotificationAccess = NotificationAccessChecker
                    .hasNotificationAccessPermission(applicationContext),
                trackingEnabled = trackingEnabled,
                allowedPackages = allowedCache,
                packageName = packageName
            )
        ) return

        val content = NotificationContentExtractor.extract(sbn.notification)
        if (shouldSkip(sbn.notification, content)) return

        // The system key is stable for updates; the fallback is not package-only.
        val notificationKey = sbn.key ?: "$packageName:${sbn.id}:${sbn.tag.orEmpty()}"

        serviceScope.launch {
            // Recheck immediately before persistence in case settings changed after the callback.
            if (!shouldCaptureNotification(
                    hasNotificationAccess = NotificationAccessChecker
                        .hasNotificationAccessPermission(applicationContext),
                    trackingEnabled = trackingEnabled,
                    allowedPackages = allowedCache,
                    packageName = packageName
                )
            ) return@launch

            if (!matchesTitleFilter(content.title, titleFilters[packageName].orEmpty())) {
                return@launch
            }

            if (!cache.allowAndReserve(notificationKey, content.normalizedForDedupe())) {
                return@launch
            }

            val dao = AppDatabase.getInstance(applicationContext).notificationDao()
            val contentFingerprint = content.contentFingerprint()
            val duplicateCheckAt = System.currentTimeMillis()
            if (dao.hasRecentDuplicate(
                    notificationKey = notificationKey,
                    contentFingerprint = contentFingerprint,
                    since = duplicateCheckAt - PERSISTENT_DEDUPE_WINDOW_MS,
                    until = duplicateCheckAt
                )
            ) return@launch

            dao.insertApp(
                NotificationEntity(
                    packageName = packageName,
                    title = content.title,
                    message = content.message,
                    receivedAt = sbn.postTime,
                    notificationKey = notificationKey,
                    contentFingerprint = contentFingerprint,
                    conversationTitle = content.conversationTitle
                )
            )
            enforceRetentionIfDue(dao)
        }
    }

    private fun shouldSkip(
        notification: Notification,
        content: ExtractedNotificationContent
    ): Boolean {
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isForegroundService =
            (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0

        if (isOngoing || isForegroundService) return true

        // Meaningful group summaries are allowed; empty summaries fail this shared content check.
        return !content.hasMeaningfulContent
    }

    private suspend fun enforceRetentionIfDue(
        dao: NotificationDao,
        now: Long = System.currentTimeMillis()
    ) {
        val retention = historyRetentionDays
        if (retention <= 0) return

        while (true) {
            val lastCleanup = lastRetentionCleanupAt.get()
            val elapsed = now - lastCleanup
            if (lastCleanup != 0L && elapsed >= 0 && elapsed < RETENTION_CLEANUP_INTERVAL_MS) {
                return
            }
            if (lastRetentionCleanupAt.compareAndSet(lastCleanup, now)) break
        }

        val threshold = now - TimeUnit.DAYS.toMillis(retention.toLong())
        dao.deleteActiveNotificationsOlderThan(threshold)
    }

    private companion object {
        val PERSISTENT_DEDUPE_WINDOW_MS = TimeUnit.MINUTES.toMillis(5)
        val RETENTION_CLEANUP_INTERVAL_MS = TimeUnit.HOURS.toMillis(6)
    }
}

internal fun matchesTitleFilter(title: String, filters: Set<String>): Boolean {
    if (filters.isEmpty()) return true
    val normalizedTitle = title.lowercase(Locale.ROOT).replace("\\s".toRegex(), "")
    return filters.any { filter ->
        normalizedTitle.contains(filter.lowercase(Locale.ROOT).replace("\\s".toRegex(), ""))
    }
}

internal fun shouldCaptureNotification(
    hasNotificationAccess: Boolean,
    trackingEnabled: Boolean,
    allowedPackages: Set<String>,
    packageName: String
): Boolean = hasNotificationAccess && trackingEnabled && packageName in allowedPackages
