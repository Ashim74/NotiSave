package com.droidnova.notificationhistory.ads

import android.content.Context
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.droidnova.notificationhistory.BuildConfig
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import java.util.concurrent.atomic.AtomicBoolean

/** Where native ads go in a long list: after the 5th item, then every 15th. */
object NativeAdSlots {
    const val FIRST_AFTER = 5
    const val EVERY = 15

    /** The ad slot that follows the item at [globalIndex], or null if no ad goes there. */
    fun slotAfter(globalIndex: Int): Int? {
        val position = globalIndex + 1
        if (position < FIRST_AFTER) return null
        val offset = position - FIRST_AFTER
        return if (offset % EVERY == 0) offset / EVERY else null
    }
}

/**
 * Loads up to [count] native ads once ads are allowed and [enabled], and destroys them when the
 * screen goes away. Each ad is shown once (one per slot), so a short list shows few or none.
 */
@Composable
fun rememberNativeAds(enabled: Boolean, count: Int = 3): List<NativeAd> {
    val context = LocalContext.current
    val adsReady by remember { AdsConsentManager.getInstance(context) }.adsReady.collectAsState()
    val ads = remember { mutableStateListOf<NativeAd>() }
    val alive = remember { AtomicBoolean(true) }
    val unitId = BuildConfig.NATIVE_AD_UNIT_ID

    LaunchedEffect(enabled, adsReady) {
        if (!enabled || !adsReady || unitId.isBlank() || ads.isNotEmpty()) return@LaunchedEffect
        val loader = AdLoader.Builder(context, unitId)
            .forNativeAd { ad ->
                // The screen may already be gone; never keep an ad nobody will destroy
                if (alive.get()) ads += ad else ad.destroy()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) = Unit
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
        loader.loadAds(AdRequest.Builder().build(), count)
    }

    DisposableEffect(Unit) {
        onDispose {
            alive.set(false)
            ads.forEach(NativeAd::destroy)
            ads.clear()
        }
    }
    // Ads off (premium bought, ad-free earned): hide them right away
    return if (enabled) ads else emptyList()
}

/**
 * A native ad drawn like a notification card: tinted rounded tile, the ad's image on the left,
 * an "Ad" label, headline, a line of text and its call-to-action. Built from real Android views
 * because the Ads SDK has to register them for clicks and impressions.
 */
@Composable
fun NativeAdCard(ad: NativeAd, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val style = NativeAdStyle(
        container = tintedCardColor().toArgb(),
        title = colors.onSurface.toArgb(),
        body = colors.onSurfaceVariant.toArgb(),
        accent = colors.primary.toArgb(),
        onAccent = colors.onPrimary.toArgb(),
        badge = colors.primaryContainer.toArgb(),
        onBadge = colors.onPrimaryContainer.toArgb(),
        adLabel = stringResource(R.string.ads_label)
    )
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontal, vertical = Dimens.CardSpacing / 2),
        factory = { context -> NativeAdCardView(context, style).let { holder -> holder.root.apply { tag = holder } } },
        update = { view -> (view.tag as NativeAdCardView).bind(ad) }
    )
}

private data class NativeAdStyle(
    val container: Int,
    val title: Int,
    val body: Int,
    val accent: Int,
    val onAccent: Int,
    val badge: Int,
    val onBadge: Int,
    val adLabel: String
)

private class NativeAdCardView(context: Context, style: NativeAdStyle) {
    val root = NativeAdView(context)
    private val media = MediaView(context)
    private val headline = TextView(context)
    private val body = TextView(context)
    private val cta = Button(context)
    private val icon = ImageView(context)
    private var boundAd: NativeAd? = null

    init {
        val radius = dp(22f)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = radius
                setColor(style.container)
            }
            setPadding(dp(10f).toInt(), dp(10f).toInt(), dp(12f).toInt(), dp(10f).toInt())
        }

        // Image or video, rounded like the app icon tiles; 120dp is the SDK's minimum for video
        media.apply {
            layoutParams = LinearLayout.LayoutParams(dp(120f).toInt(), dp(120f).toInt())
            setImageScaleType(ImageView.ScaleType.CENTER_CROP)
            clipToOutline = true
            outlineProvider = roundedOutline(dp(14f))
        }
        row.addView(media)

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12f).toInt()
            }
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // The "Ad" attribution Google requires, styled like the app's small pills
        val adBadge = TextView(context).apply {
            text = style.adLabel
            setTextColor(style.onBadge)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                cornerRadius = dp(50f)
                setColor(style.badge)
            }
            setPadding(dp(7f).toInt(), dp(1f).toInt(), dp(7f).toInt(), dp(1f).toInt())
        }
        header.addView(adBadge)
        icon.apply {
            layoutParams = LinearLayout.LayoutParams(dp(18f).toInt(), dp(18f).toInt()).apply {
                marginStart = dp(6f).toInt()
            }
            clipToOutline = true
            outlineProvider = roundedOutline(dp(5f))
        }
        header.addView(icon)
        column.addView(header)

        headline.apply {
            setTextColor(style.title)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(4f).toInt(), dp(16f).toInt(), 0)
        }
        column.addView(headline)

        body.apply {
            setTextColor(style.body)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(2f).toInt(), 0, 0)
        }
        column.addView(body)

        cta.apply {
            isAllCaps = false
            setTextColor(style.onAccent)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            minHeight = 0
            minimumHeight = 0
            stateListAnimator = null
            backgroundTintList = null
            background = GradientDrawable().apply {
                cornerRadius = dp(50f)
                setColor(style.accent)
            }
            setPadding(dp(14f).toInt(), dp(6f).toInt(), dp(14f).toInt(), dp(6f).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8f).toInt()
                gravity = Gravity.END
            }
        }
        column.addView(cta)
        row.addView(column)
        root.addView(row, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.mediaView = media
        root.headlineView = headline
        root.bodyView = body
        root.callToActionView = cta
        root.iconView = icon
    }

    fun bind(ad: NativeAd) {
        if (boundAd === ad) return
        boundAd = ad
        headline.text = ad.headline
        body.text = ad.body
        body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
        cta.text = ad.callToAction
        cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
        val adIcon = ad.icon?.drawable
        icon.setImageDrawable(adIcon)
        icon.visibility = if (adIcon == null) View.GONE else View.VISIBLE
        ad.mediaContent?.let(media::setMediaContent)
        root.setNativeAd(ad)
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, root.resources.displayMetrics)

    private fun roundedOutline(radius: Float) = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
}
