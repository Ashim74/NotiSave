package com.droidnova.notificationhistory.ads

import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.droidnova.notificationhistory.BuildConfig
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Collapsible anchored adaptive banner.
 * - Renders nothing until [AdsConsentManager.adsReady] (consent gathered + SDK initialized).
 * - Collapses to 0 dp height until an ad loads or when loading fails.
 * - Pauses/resumes with the host lifecycle and is destroyed when it leaves composition.
 * - Ad unit comes from BuildConfig: Google's test unit in debug, the live unit in release.
 */
@Composable
fun CollapsibleAdBanner(
    modifier: Modifier = Modifier,
    collapsiblePlacement: String = "bottom", // "bottom" or "top"
) {
    val context = LocalContext.current
    val adsReady by remember { AdsConsentManager.getInstance(context) }.adsReady.collectAsState()
    if (!adsReady) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Anchored adaptive banner expects width in dp.
    val adWidthDp = configuration.screenWidthDp.coerceAtLeast(320) // safety floor
    val adSize = remember(adWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
    }

    var adHeightPx by remember { mutableIntStateOf(0) }
    val adHeightDp = with(density) { adHeightPx.toDp() }

    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = BuildConfig.BANNER_AD_UNIT_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    // Use AdSize height; this is stable for anchored adaptive banners.
                    adHeightPx = adSize.getHeightInPixels(context)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adHeightPx = 0
                }
            }
        }
    }

    DisposableEffect(adView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_RESUME -> adView.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val extras = Bundle().apply { putString("collapsible", collapsiblePlacement) }
        adView.loadAd(
            AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeightDp)
    ) {
        AndroidView(
            factory = { adView },
            update = { view ->
                // Keep layout stable; height is handled by the Compose Box.
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        )
    }
}
