package com.droidnova.notificationhistory.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.droidnova.notificationhistory.utils.CrashReporter
import java.util.concurrent.TimeUnit

/**
 * Re-arms listener recovery after a reboot or an app update. Both can leave the listener
 * unbound until the user opens the app; an update also replaces our process.
 */
class ListenerBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val appContext = context.applicationContext
        ListenerWatchdog.schedule(appContext)
        runCatching { ListenerReconnector.ensureConnected(appContext) }
            .onFailure(CrashReporter::record)
        // The system's own bind can lag right after boot; check once it has had time.
        ListenerWatchdog.checkSoon(appContext, POST_BOOT_CHECK_DELAY_MS)
    }

    private companion object {
        val POST_BOOT_CHECK_DELAY_MS = TimeUnit.SECONDS.toMillis(30)

        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            // Only delivered to direct-boot-aware receivers. This one isn't, because everything
            // it touches (DataStore, WorkManager, prefs) lives in credential-encrypted storage;
            // BOOT_COMPLETED follows once the user unlocks.
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )
    }
}
