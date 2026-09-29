package com.droidnova.notificationhistory.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Thin wrapper around Crashlytics so callers never crash when Firebase is
 * unavailable (unit tests, stripped builds, early process start).
 */
object CrashReporter {

    fun record(throwable: Throwable) {
        runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
    }

    fun setCustomKey(key: String, value: Boolean) {
        runCatching { FirebaseCrashlytics.getInstance().setCustomKey(key, value) }
    }

    fun setCustomKey(key: String, value: Int) {
        runCatching { FirebaseCrashlytics.getInstance().setCustomKey(key, value) }
    }
}
