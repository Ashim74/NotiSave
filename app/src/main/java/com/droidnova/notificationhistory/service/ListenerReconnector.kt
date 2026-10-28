package com.droidnova.notificationhistory.service

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import com.droidnova.notificationhistory.core.permission.NotificationAccessChecker
import com.droidnova.notificationhistory.utils.CrashReporter
import java.util.concurrent.TimeUnit

enum class ReconnectAction { NONE, REQUEST_REBIND, FORCE_TOGGLE }

/**
 * Recovers a dead notification-listener binding. The system sometimes kills the
 * listener and never rebinds it, which silently stops capture until reboot.
 *
 * Escalation: first a plain [NotificationListenerService.requestRebind]; if the
 * listener is still disconnected after [ESCALATION_WINDOW_MS], toggle the service
 * component off/on, which forces the system to re-evaluate enabled listeners.
 * The rebind alone only helps a listener the system snoozed; after the process was
 * killed and not restarted it does nothing (seen on Android 10 / MIUI), so callers
 * follow it up instead of waiting for the next periodic run.
 *
 * The attempt timestamps live in SharedPreferences, not memory: the background
 * [ListenerWatchdog] usually runs in a freshly started process, and escalation has
 * to carry over from the run before it.
 */
object ListenerReconnector {

    private val ESCALATION_WINDOW_MS = TimeUnit.SECONDS.toMillis(15)
    private val FORCE_TOGGLE_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(10)

    private const val PREFS_NAME = "listener_reconnector"
    private const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
    private const val KEY_LAST_FORCE_TOGGLE_AT = "last_force_toggle_at"

    /** Call from the listener's onListenerConnected so escalation state resets. */
    fun notifyConnected(context: Context) {
        // The toggle cooldown deliberately survives a reconnect.
        runCatching { prefs(context).edit().putLong(KEY_LAST_ATTEMPT_AT, 0L).apply() }
            .onFailure(CrashReporter::record)
    }

    /**
     * Safe to call on every Activity resume and from background work; does nothing
     * when healthy. Returns the action taken. [userInitiated] (the Reconnect tap) skips
     * the settle wait: the user is looking at a dead listener, so escalate now.
     */
    @Synchronized
    fun ensureConnected(context: Context, userInitiated: Boolean = false): ReconnectAction {
        val hasPermission = NotificationAccessChecker.hasNotificationAccessPermission(context)
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        val action = nextReconnectAction(
            hasPermission = hasPermission,
            isConnected = NotificationListener.isConnected,
            lastAttemptAt = prefs.getLong(KEY_LAST_ATTEMPT_AT, 0L),
            lastForceToggleAt = prefs.getLong(KEY_LAST_FORCE_TOGGLE_AT, 0L),
            now = now,
            escalationWindowMs = ESCALATION_WINDOW_MS,
            forceToggleCooldownMs = FORCE_TOGGLE_COOLDOWN_MS,
            userInitiated = userInitiated
        )
        if (action == ReconnectAction.NONE) return action

        // commit, not apply: the watchdog's process may die right after this returns.
        val editor = prefs.edit().putLong(KEY_LAST_ATTEMPT_AT, now)
        if (action == ReconnectAction.FORCE_TOGGLE) editor.putLong(KEY_LAST_FORCE_TOGGLE_AT, now)
        editor.commit()
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
        return action
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

internal fun nextReconnectAction(
    hasPermission: Boolean,
    isConnected: Boolean,
    lastAttemptAt: Long,
    lastForceToggleAt: Long,
    now: Long,
    escalationWindowMs: Long,
    forceToggleCooldownMs: Long,
    userInitiated: Boolean = false
): ReconnectAction {
    if (!hasPermission || isConnected) return ReconnectAction.NONE
    val sinceLastAttempt = now - lastAttemptAt
    val sinceLastToggle = now - lastForceToggleAt
    return when {
        // No prior attempt (or the clock moved backwards): start gently.
        lastAttemptAt == 0L || sinceLastAttempt < 0 -> ReconnectAction.REQUEST_REBIND
        // A recent attempt may still be settling; don't spam the system.
        sinceLastAttempt < escalationWindowMs && !userInitiated -> ReconnectAction.NONE
        // Toggling the component is heavy; while it's cooling down, keep rebinding.
        lastForceToggleAt != 0L && sinceLastToggle in 0 until forceToggleCooldownMs ->
            ReconnectAction.REQUEST_REBIND
        // The gentle path didn't work; force the system to re-evaluate the listener.
        else -> ReconnectAction.FORCE_TOGGLE
    }
}

/** Why the watchdog thinks the listener needs recovery, if it does. */
enum class ListenerHealth { NOT_EXPECTED, HEALTHY, DISCONNECTED, SILENTLY_KILLED }

/**
 * [persistedConnected] is the last state the listener wrote itself. When the process is
 * killed (OEM battery managers do this) onListenerDisconnected never runs, so the flag
 * still says connected while the live, in-memory state of the new process says not.
 */
internal fun assessListenerHealth(
    hasPermission: Boolean,
    trackingEnabled: Boolean,
    persistedConnected: Boolean,
    liveConnected: Boolean
): ListenerHealth = when {
    !hasPermission || !trackingEnabled -> ListenerHealth.NOT_EXPECTED
    liveConnected -> ListenerHealth.HEALTHY
    persistedConnected -> ListenerHealth.SILENTLY_KILLED
    else -> ListenerHealth.DISCONNECTED
}
