package com.droidnova.notificationhistory

import android.app.Application
import com.droidnova.notificationhistory.service.ListenerWatchdog
import com.droidnova.notificationhistory.utils.Analytics

class NotificationHistoryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Analytics.init(this)
        // Every process start re-arms the watchdog (KEEP: an existing schedule is left alone).
        ListenerWatchdog.schedule(this)
    }
}
