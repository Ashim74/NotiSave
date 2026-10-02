package com.droidnova.notificationhistory.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.droidnova.notificationhistory.BuildConfig
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.utils.Analytics
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long one watched rewarded ad keeps the app ad-free. */
val AD_FREE_DURATION_MS: Long = TimeUnit.HOURS.toMillis(24)

/**
 * The end of the current ad-free period (epoch millis), or null while it is still being read or
 * when there is none. Turns back to null by itself the moment the period runs out.
 */
@Composable
fun rememberAdFreeUntil(): Long? {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context.applicationContext) }
    // null until read, so nothing flashes an ad before we know
    val until by prefs.adFreeUntil.collectAsState(initial = null)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(until) {
        now = System.currentTimeMillis()
        val end = until ?: return@LaunchedEffect
        if (end > now) {
            delay(end - now)
            now = System.currentTimeMillis()
        }
    }
    return until?.takeIf { it > now }
}

/** True when ads may be shown: not premium, preference read, and no ad-free period running. */
@Composable
fun rememberShowAds(isPremium: Boolean): Boolean {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context.applicationContext) }
    val until by prefs.adFreeUntil.collectAsState(initial = null)
    val adFreeUntil = rememberAdFreeUntil()
    return !isPremium && until != null && adFreeUntil == null
}

/** A rewarded ad that, watched to the end, makes the app ad-free for [AD_FREE_DURATION_MS]. */
class RewardedAdFree internal constructor(
    private val onShow: (Activity) -> Unit,
    /** True when the feature can be offered at all (consent given, a unit configured). */
    val available: Boolean,
    val isLoading: Boolean,
    val isReady: Boolean
) {
    fun show(activity: Activity) = onShow(activity)
}

/**
 * Loads one rewarded ad as soon as ads are allowed, and reloads after each one is shown.
 * [onEarned] runs after the reward is saved.
 */
@Composable
fun rememberRewardedAdFree(onEarned: () -> Unit = {}): RewardedAdFree {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adsReady by remember { AdsConsentManager.getInstance(context) }.adsReady.collectAsState()
    val unitId = BuildConfig.REWARDED_AD_UNIT_ID
    val available = adsReady && unitId.isNotBlank()

    var ad by remember { mutableStateOf<RewardedAd?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadRequest by remember { mutableStateOf(0) }

    LaunchedEffect(available, loadRequest) {
        if (!available || ad != null) return@LaunchedEffect
        isLoading = true
        RewardedAd.load(context, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(loaded: RewardedAd) {
                ad = loaded
                isLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                isLoading = false
            }
        })
    }

    return RewardedAdFree(
        available = available,
        isLoading = isLoading,
        isReady = ad != null,
        onShow = { activity ->
            val current = ad ?: run {
                loadRequest++
                return@RewardedAdFree
            }
            current.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    ad = null
                    loadRequest++
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    ad = null
                    loadRequest++
                }
            }
            Analytics.log(activity, Analytics.REWARDED_SHOWN)
            current.show(activity) {
                // The activity's scope: the screen that offered the ad may be gone by now
                ((activity as? ComponentActivity)?.lifecycleScope ?: scope).launch {
                    UserPreferences(activity.applicationContext)
                        .setAdFreeUntil(System.currentTimeMillis() + AD_FREE_DURATION_MS)
                    Analytics.log(activity, Analytics.REWARDED_EARNED)
                    onEarned()
                }
            }
        }
    )
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
