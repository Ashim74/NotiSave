package com.droidnova.notificationhistory.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import com.droidnova.notificationhistory.core.permission.NotificationAccessChecker
import com.droidnova.notificationhistory.utils.CrashReporter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

enum class ReconnectAction { NONE, REQUEST_REBIND, FORCE_TOGGLE }

/**
 * Recovers a dead notification-listener binding. The system sometimes kills the
 * listener and never rebinds it, which silently stops capture until reboot.
 *
 * Escalation: first a plain [NotificationListenerService.requestRebind]; if the
 * listener is still disconnected after [ESCALATION_WINDOW_MS], toggle the service
 * component off/on, which forces the system to re-evaluate enabled listeners.
 */
object ListenerReconnector {

    private val ESCALATION_WINDOW_MS = TimeUnit.SECONDS.toMillis(15)

    private val lastAttemptAt = AtomicLong(0L)

    /** Call from the listener's onListenerConnected so escalation state resets. */
    fun notifyConnected() {
        lastAttemptAt.set(0L)
    }

    /** Safe to call on every Activity resume; does nothing when healthy. */
    fun ensureConnected(context: Context) {
        val hasPermission = NotificationAccessChecker.hasNotificationAccessPermission(context)
        val now = System.currentTimeMillis()
        val action = nextReconnectAction(
            hasPermission = hasPermission,
            isConnected = NotificationListener.isConnected,
            lastAttemptAt = lastAttemptAt.get(),
            now = now,
            escalationWindowMs = ESCALATION_WINDOW_MS
        )
        if (action == ReconnectAction.NONE) return

        lastAttemptAt.set(now)
        runCatching {
            val component = ComponentName(context, NotificationListener::class.java)
            if (action == ReconnectAction.FORCE_TOGGLE) {
                val pm = context.packageManager
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            NotificationListenerService.requestRebind(component)
        }.onFailure(CrashReporter::record)
    }
}

internal fun nextReconnectAction(
    hasPermission: Boolean,
    isConnected: Boolean,
    lastAttemptAt: Long,
    now: Long,
    escalationWindowMs: Long
): ReconnectAction {
    if (!hasPermission || isConnected) return ReconnectAction.NONE
    val sinceLastAttempt = now - lastAttemptAt
    return when {
        // No prior attempt (or the clock moved backwards): start gently.
        lastAttemptAt == 0L || sinceLastAttempt < 0 -> ReconnectAction.REQUEST_REBIND
        // A recent attempt may still be settling; don't spam the system.
        sinceLastAttempt < escalationWindowMs -> ReconnectAction.NONE
        // The gentle path didn't work; force the system to re-evaluate the listener.
        else -> ReconnectAction.FORCE_TOGGLE
    }
}
