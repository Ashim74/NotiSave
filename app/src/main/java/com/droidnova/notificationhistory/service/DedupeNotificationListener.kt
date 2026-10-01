package com.droidnova.notificationhistory.service

import android.app.Notification
import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.droidnova.notificationhistory.core.permission.NotificationAccessChecker
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.data.db.NotificationDao
import com.droidnova.notificationhistory.data.db.NotificationEntity
import com.droidnova.notificationhistory.data.mapper.fetchAppName
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.utils.CrashReporter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationListener : NotificationListenerService() {

    private val cache = DedupeCache(ttlMs = 2000L)
    private val activeContent = ActiveContentTracker()
    private val persistMutex = Mutex()
    private val crashHandler = CoroutineExceptionHandler { _, throwable ->
        CrashReporter.record(throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + crashHandler)
    private val retentionGate = RetentionGate(RETENTION_CLEANUP_INTERVAL_MS)

    // Completed once the preference caches hold real values; capture coroutines
    // await it so notifications arriving during startup are never dropped.
    private val prefsPrimed = CompletableDeferred<Unit>()

    private lateinit var prefs: UserPreferences

    // Posted on the main looper so it outlives this service instance: if the binding is still
    // dead when it fires (and the user still expects capture), tell them instead of failing silently.
    private val mainHandler = Handler(Looper.getMainLooper())
    private val disconnectAlert = Runnable {
        val context = applicationContext
        if (!isConnected &&
            trackingEnabledSnapshot &&
            NotificationAccessChecker.hasNotificationAccessPermission(context)
        ) {
            ListenerAlerts.showDisconnected(context)
        }
    }

    @Volatile private var allowedCache: Set<String> = emptySet()
    @Volatile private var trackingEnabled: Boolean = false
    @Volatile private var historyRetentionDays: Int = SettingState.DEFAULT_HISTORY_RETENTION_DAYS
    @Volatile private var titleFilters: Map<String, Set<String>> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        prefs = UserPreferences(applicationContext)

        serviceScope.launch {
            try {
                allowedCache = prefs.allowedApps.first()
                val state = prefs.settingFlow.first()
                trackingEnabled = state.userToggleTracking
                historyRetentionDays = state.historyRetentionDays.coerceAtLeast(0)
                titleFilters = prefs.allTitleFilters.first()
            } catch (t: Throwable) {
                CrashReporter.record(t)
            } finally {
                prefsPrimed.complete(Unit)
            }
        }

        serviceScope.launch {
            prefs.allowedApps.collect {
                allowedCache = it
                CrashReporter.setCustomKey("allowed_app_count", it.size)
            }
        }

        serviceScope.launch {
            prefs.settingFlow.collect { state ->
                trackingEnabled = state.userToggleTracking
                trackingEnabledSnapshot = state.userToggleTracking
                historyRetentionDays = state.historyRetentionDays.coerceAtLeast(0)
                CrashReporter.setCustomKey("tracking_enabled", state.userToggleTracking)
            }
        }

        serviceScope.launch {
            prefs.allTitleFilters.collect { titleFilters = it }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        ListenerReconnector.notifyConnected()
        mainHandler.removeCallbacks(disconnectAlert)
        ListenerAlerts.cancel(applicationContext)
        CrashReporter.setCustomKey("listener_connected", true)
        persistConnectedState(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        CrashReporter.setCustomKey("listener_connected", false)
        persistConnectedState(false)
        // Gentle recovery attempt; ListenerReconnector escalates from the UI if this fails.
        runCatching {
            requestRebind(ComponentName(this, NotificationListener::class.java))
        }.onFailure(CrashReporter::record)
        mainHandler.removeCallbacks(disconnectAlert)
        mainHandler.postDelayed(disconnectAlert, DISCONNECT_ALERT_DELAY_MS)
    }

    private fun persistConnectedState(connected: Boolean) {
        serviceScope.launch {
            runCatching { prefs.setListenerConnected(connected) }
                .onFailure(CrashReporter::record)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return

        // Preserve M1: permission, user tracking, and explicit package selection are all
        // required. This fast path only drops on cached state after priming; before that,
        // the IO coroutine below re-evaluates once real values are loaded.
        if (prefsPrimed.isCompleted && !shouldCaptureNotification(
                hasNotificationAccess = isConnected,
                trackingEnabled = trackingEnabled,
                allowedPackages = allowedCache,
                packageName = packageName
            )
        ) return

        val content = NotificationContentExtractor.extract(sbn.notification)
        if (shouldSkip(sbn.notification, content)) return

        // The system key is stable for updates; the fallback is not package-only.
        val notificationKey = sbn.key ?: "$packageName:${sbn.id}:${sbn.tag.orEmpty()}"
        val postTime = sbn.postTime
        val notification = sbn.notification

        serviceScope.launch {
            prefsPrimed.await()

            // Recheck immediately before persistence in case settings changed after the callback.
            if (!shouldCaptureNotification(
                    hasNotificationAccess = isConnected,
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

            val contentFingerprint = content.contentFingerprint()
            val verdict = activeContent.onPosted(notificationKey, contentFingerprint)
            if (verdict == ActiveContentTracker.Verdict.Unchanged) return@launch

            // A summary repeats what its children already say; save the children only.
            // Apps often post the summary a moment before its children, so let them land.
            if (isGroupSummary(notification)) {
                delay(SUMMARY_SETTLE_MS)
                if (hasActiveGroupChildren(sbn)) return@launch
            }

            val conversation = NotificationConversationDetector.detect(
                packageName = packageName,
                notification = notification,
                appLabel = fetchAppName(packageManager, packageName)
            )
            val row = NotificationEntity(
                packageName = packageName,
                title = content.title,
                message = content.message,
                receivedAt = postTime,
                notificationKey = notificationKey,
                contentFingerprint = contentFingerprint,
                conversationTitle = content.conversationTitle,
                conversationKey = conversation?.key,
                conversationName = conversation?.displayName
            )
            val messages = NotificationContentExtractor.extractMessages(notification)

            try {
                val dao = AppDatabase.getInstance(applicationContext).notificationDao()
                // Apps fire several posts per message within milliseconds; checking and
                // inserting under one lock stops parallel posts from each saving a copy.
                persistMutex.withLock {
                    if (messages.isNotEmpty()) {
                        saveNewMessages(dao, row, messages)
                    } else {
                        saveNotification(dao, row, verdict)
                    }
                }
                enforceRetentionIfDue(dao)
            } catch (t: Throwable) {
                // A failing disk/DB (full, locked, mid-migration) must never crash the
                // listener process; losing one notification beats losing the listener.
                CrashReporter.record(t)
            }
        }
    }

    /**
     * Chat apps re-post the whole unread thread on every new message ("hi", then "hi / how are
     * you", ...). Saving the combined text would repeat older messages, so each message becomes
     * its own row and only messages not saved before are inserted.
     */
    private suspend fun saveNewMessages(
        dao: NotificationDao,
        row: NotificationEntity,
        messages: List<ExtractedMessage>
    ) {
        val latestSaved = row.notificationKey?.let {
            dao.latestReceivedAtForKey(it, since = row.receivedAt - KEY_DEDUPE_WINDOW_MS)
        }
        messages.forEach { message ->
            // Message times come from the sender's clock; never place a message in the future.
            val receivedAt = message.timestamp
                .takeIf { it > 0 && it <= row.receivedAt }
                ?: row.receivedAt
            // Anything at or before the newest saved row of this thread was already captured.
            if (latestSaved != null && receivedAt <= latestSaved) return@forEach
            val fingerprint = message.fingerprint()
            if (dao.hasSameContentBetween(
                    packageName = row.packageName,
                    contentFingerprint = fingerprint,
                    from = receivedAt - CONTENT_DEDUPE_WINDOW_MS,
                    to = receivedAt + CONTENT_DEDUPE_WINDOW_MS
                )
            ) return@forEach
            dao.insertApp(
                row.copy(
                    message = message.displayText(row.title),
                    receivedAt = receivedAt,
                    contentFingerprint = fingerprint
                )
            )
        }
    }

    private suspend fun saveNotification(
        dao: NotificationDao,
        row: NotificationEntity,
        verdict: ActiveContentTracker.Verdict
    ) {
        val key = row.notificationKey ?: return
        val fingerprint = row.contentFingerprint ?: return
        val now = System.currentTimeMillis()
        // After a listener restart the tracker is empty: a re-post of a still-shown
        // notification then matches the newest row saved under its key.
        if (verdict == ActiveContentTracker.Verdict.Unknown &&
            dao.latestFingerprintForKey(key, since = now - KEY_DEDUPE_WINDOW_MS) == fingerprint
        ) return
        if (dao.hasSameContentBetween(
                packageName = row.packageName,
                contentFingerprint = fingerprint,
                from = now - CONTENT_DEDUPE_WINDOW_MS,
                to = Long.MAX_VALUE
            )
        ) return
        dao.insertApp(row)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        sbn.key?.let(activeContent::onRemoved)
    }

    private fun isGroupSummary(notification: Notification): Boolean =
        (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

    private fun hasActiveGroupChildren(summary: StatusBarNotification): Boolean = runCatching {
        activeNotifications.orEmpty().any {
            it.packageName == summary.packageName &&
                it.groupKey == summary.groupKey &&
                it.key != summary.key &&
                !isGroupSummary(it.notification)
        }
    }.getOrDefault(false)

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
        if (!retentionGate.tryAcquire(now)) return

        val retention = historyRetentionDays
        if (retention > 0) {
            dao.deleteActiveNotificationsOlderThan(now - TimeUnit.DAYS.toMillis(retention.toLong()))
        }
        // Trash purges on its own fixed schedule, even when history is kept forever.
        dao.deleteTrashedBefore(
            now - TimeUnit.DAYS.toMillis(SettingState.TRASH_RETENTION_DAYS.toLong())
        )
    }

    companion object {
        /**
         * Live binding state, readable from the UI process (the listener runs in the
         * default app process). False also while the service has never been created.
         */
        @Volatile var isConnected: Boolean = false
            private set

        /** Last known user toggle, readable by the delayed alert after the instance is gone. */
        @Volatile private var trackingEnabledSnapshot: Boolean = true

        private val KEY_DEDUPE_WINDOW_MS = TimeUnit.HOURS.toMillis(24)
        private val CONTENT_DEDUPE_WINDOW_MS = TimeUnit.MINUTES.toMillis(5)
        private val SUMMARY_SETTLE_MS = TimeUnit.SECONDS.toMillis(2)
        private val RETENTION_CLEANUP_INTERVAL_MS = TimeUnit.HOURS.toMillis(6)
        private val DISCONNECT_ALERT_DELAY_MS = TimeUnit.SECONDS.toMillis(45)
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
