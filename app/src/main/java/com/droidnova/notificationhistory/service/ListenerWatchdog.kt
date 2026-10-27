package com.droidnova.notificationhistory.service

import android.content.Context
import android.os.Bundle
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.droidnova.notificationhistory.core.permission.NotificationAccessChecker
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.utils.Analytics
import com.droidnova.notificationhistory.utils.CrashReporter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Background recovery for the notification listener. Without it, recovery only ran when the
 * user opened the app: a listener the system (or an OEM battery manager) dropped while the app
 * was in the background stayed dead, and a killed process never even raised the alert.
 */
object ListenerWatchdog {

    private const val PERIODIC_WORK = "listener_watchdog"
    private const val ONE_SHOT_WORK = "listener_watchdog_check"

    // Periodic work can't run more often than this.
    private const val PERIOD_MINUTES = 15L

    /** How long a process the watchdog just started gets to receive the system's own bind. */
    private val CONNECT_GRACE_MS = TimeUnit.SECONDS.toMillis(10)
    private val RECOVERY_WAIT_MS = TimeUnit.SECONDS.toMillis(20)
    private val POLL_MS = 500L

    /** The background alert repeats at most this often, so a dead listener can't nag every run. */
    private val ALERT_INTERVAL_MS = TimeUnit.HOURS.toMillis(6)
    private const val PREFS_NAME = "listener_watchdog"
    private const val KEY_LAST_ALERT_AT = "last_alert_at"

    /** Idempotent: an already scheduled watchdog keeps its timing. */
    fun schedule(context: Context) {
        runCatching {
            val request = PeriodicWorkRequestBuilder<ListenerWatchdogWorker>(
                PERIOD_MINUTES,
                TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }.onFailure(CrashReporter::record)
    }

    /**
     * One extra check after [delayMs], so recovery doesn't wait for the next periodic run.
     * Not expedited: WorkManager rejects expedited work that has an initial delay.
     */
    fun checkSoon(context: Context, delayMs: Long) {
        runCatching {
            val request = OneTimeWorkRequestBuilder<ListenerWatchdogWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_SHOT_WORK,
                ExistingWorkPolicy.KEEP,
                request
            )
        }.onFailure(CrashReporter::record)
    }

    internal suspend fun runCheck(context: Context) {
        val prefs = UserPreferences(context)
        val hasPermission = NotificationAccessChecker.hasNotificationAccessPermission(context)
        val trackingEnabled = prefs.settingFlow.first().userToggleTracking
        val persistedConnected = prefs.listenerConnected.first()

        if (hasPermission && trackingEnabled) awaitConnection(CONNECT_GRACE_MS)
        val health = assessListenerHealth(
            hasPermission = hasPermission,
            trackingEnabled = trackingEnabled,
            persistedConnected = persistedConnected,
            liveConnected = NotificationListener.isConnected
        )
        if (health == ListenerHealth.NOT_EXPECTED || health == ListenerHealth.HEALTHY) return

        val action = ListenerReconnector.ensureConnected(context)
        val recovered = awaitConnection(RECOVERY_WAIT_MS)
        // The flag drives Home's "Reconnecting" state; after a silent kill it still says connected.
        prefs.setListenerConnected(NotificationListener.isConnected)
        if (!recovered) maybeAlert(context)

        Analytics.log(
            context,
            Analytics.LISTENER_WATCHDOG_RECOVERY,
            Bundle().apply {
                putString(Analytics.PARAM_REASON, health.name.lowercase())
                putString(Analytics.PARAM_ACTION, action.name.lowercase())
                putLong(Analytics.PARAM_RECOVERED, if (recovered) 1L else 0L)
            }
        )
    }

    private suspend fun awaitConnection(timeoutMs: Long): Boolean {
        var waited = 0L
        while (!NotificationListener.isConnected && waited < timeoutMs) {
            delay(POLL_MS)
            waited += POLL_MS
        }
        return NotificationListener.isConnected
    }

    private fun maybeAlert(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastAlertAt = prefs.getLong(KEY_LAST_ALERT_AT, 0L)
        if (lastAlertAt in (now - ALERT_INTERVAL_MS + 1)..now) return
        prefs.edit().putLong(KEY_LAST_ALERT_AT, now).commit()
        ListenerAlerts.showDisconnected(context)
    }
}

class ListenerWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Never retry or fail: the next periodic run is the retry.
        runCatching { ListenerWatchdog.runCheck(applicationContext) }
            .onFailure(CrashReporter::record)
        return Result.success()
    }
}
