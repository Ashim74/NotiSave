            package com.droidnova.notificationhistory.service
            import android.service.notification.NotificationListenerService
            import android.service.notification.StatusBarNotification
            import android.util.Log
            import com.droidnova.notificationhistory.data.datastore.UserPreferences
            import com.droidnova.notificationhistory.data.db.AppDatabase
            import com.droidnova.notificationhistory.data.db.NotificationEntity
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.SupervisorJob
            import kotlinx.coroutines.cancel
            import kotlinx.coroutines.flow.first
            import kotlinx.coroutines.launch

            class  NotificationListener: NotificationListenerService() {
                // IO scope tied to service lifecycle
                private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

                private lateinit var prefs: UserPreferences

                //  Fast, thread-safe reads (no locking)
                @Volatile private var allowedCache: Set<String> = emptySet()

                override fun onCreate() {
                    super.onCreate()
                    prefs = UserPreferences(applicationContext)

                    serviceScope.launch {
                        // Track the allowed package set
                        prefs.allowedApps.collect { set ->
                            allowedCache = set
                            Log.d("NLS", "allowed packages = $set")
                        }
                    }
                }

                override fun onDestroy() {
                    super.onDestroy()
                    serviceScope.cancel()
                }


                override fun onNotificationPosted(sbn: StatusBarNotification) {
                    val pkg = sbn.packageName ?: return
                    // Sirf allowed packages process karo
                    if ( pkg !in allowedCache) {
                        //pkg Not an allowed app – skip
                        return
                    }
                    serviceScope.launch {
                        if (!prefs.userToggleTracking.first()) return@launch

                        val dao = AppDatabase.getInstance(applicationContext).notificationDao()
                        val packageName = sbn.packageName
                        val title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: ""
                        val text = sbn.notification.extras.getCharSequence("android.text")?.toString()  ?:""
                        val postedTime = sbn.postTime

                        val appEntity = NotificationEntity(
                            packageName = packageName,
                            title = title,
                            message = text,
                            receivedAt = postedTime
                        )
                        Log.e("NLSs", "onNotificationPosted: $appEntity")
                        dao.insertApp(appEntity)
                    }
                }
            }