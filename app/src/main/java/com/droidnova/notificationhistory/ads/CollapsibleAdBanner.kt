package com.droidnova.notificationhistory.ads

import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Professional collapsible adaptive banner:
 * - Calculates ad width from current screen width in dp (anchored adaptive size).
 * - Collapses height to 0 when ad is not loaded / fails.
 * - Uses MATCH_PARENT width and the computed adaptive height.
 * - Releases AdView properly to avoid leaks.
 */
@Composable
fun CollapsibleAdBanner(
    modifier: Modifier = Modifier,
    collapsiblePlacement: String = "bottom", // "bottom" or "top"
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Anchored adaptive banner expects width in dp.
    val adWidthDp = configuration.screenWidthDp.coerceAtLeast(320) // safety floor
    val adSize = remember(adWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
    }

    var adHeightPx by remember { mutableIntStateOf(0) }
    val adHeightDp = with(density) { adHeightPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeightDp)
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    adUnitId = "ca-app-pub-4788231589271799/5044263835"

                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            // Use AdSize height; this is stable for anchored adaptive banners.
                            adHeightPx = adSize.getHeightInPixels(ctx)
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            adHeightPx = 0
                        }
                    }

                    val extras = Bundle().apply {
                        putString("collapsible", collapsiblePlacement)
                    }

                    loadAd(
                        AdRequest.Builder()
                            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                            .build()
                    )
                }
            },
            update = { adView ->
                // Keep layout stable; height is handled by compose Box.
                adView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        )
    }
}
