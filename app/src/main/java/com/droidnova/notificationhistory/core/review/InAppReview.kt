package com.droidnova.notificationhistory.core.review

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalContext
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.utils.Analytics
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Call [onValueMoment] right after the app has visibly helped the user. */
class InAppReview internal constructor(
    private val context: Context,
    private val scope: CoroutineScope?
) {
    private val prefs = UserPreferences(context.applicationContext)

    /**
     * Records the moment and, if [ReviewPolicy] says now is a good time, shows Google Play's
     * review sheet over the current screen. Safe to call often; most calls just count.
     */
    fun onValueMoment() {
        scope?.launch {
            val now = System.currentTimeMillis()
            val state = prefs.recordValueMoment(now)
            if (!ReviewPolicy.shouldAsk(state, now)) return@launch
            val activity = context.findActivity() ?: return@launch
            // Let the dialog/sheet the user just closed finish animating away first
            delay(SETTLE_MS)
            runCatching {
                val manager = ReviewManagerFactory.create(activity)
                val info = manager.requestReview()
                prefs.markReviewAsked(now)
                Analytics.log(activity, Analytics.REVIEW_PROMPTED)
                manager.launchReview(activity, info)
            }.onFailure { Log.w(TAG, "In-app review unavailable", it) }
        }
    }

    private companion object {
        const val TAG = "InAppReview"
        const val SETTLE_MS = 600L
    }
}

@Composable
fun rememberInAppReview(): InAppReview {
    val context = LocalContext.current
    // The activity's scope, not the caller's: a closing dialog must not cancel the prompt
    return remember(context) {
        InAppReview(context, (context.findActivity() as? ComponentActivity)?.lifecycleScope)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
