package com.droidnova.notificationhistory.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.droidnova.notificationhistory.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/**
 * Headings in Plus Jakarta Sans (geometric, confident at heavy weights), everything else in Inter
 * (made for small screen text). Both are downloadable fonts; until they arrive, or without Play
 * services, the platform sans is used at the same weights.
 */
private val jakarta = GoogleFont("Plus Jakarta Sans")
private val inter = GoogleFont("Inter")

val DisplayFontFamily = FontFamily(
    Font(googleFont = jakarta, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = jakarta, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = jakarta, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = jakarta, fontProvider = provider, weight = FontWeight.ExtraBold)
)

private val BodyFontFamily = FontFamily(
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Bold)
)

private val baseline = Typography()

/** Big type gets tighter tracking so headings read as one solid word shape. */
private fun TextStyle.heading(weight: FontWeight, tracking: Float) =
    copy(fontFamily = DisplayFontFamily, fontWeight = weight, letterSpacing = tracking.sp)

private fun TextStyle.body(weight: FontWeight, tracking: Float? = null) =
    copy(fontFamily = BodyFontFamily, fontWeight = weight).let { if (tracking != null) it.copy(letterSpacing = tracking.sp) else it }

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.heading(FontWeight.ExtraBold, -1.0f),
    displayMedium = baseline.displayMedium.heading(FontWeight.ExtraBold, -0.8f),
    displaySmall = baseline.displaySmall.heading(FontWeight.ExtraBold, -0.6f),
    headlineLarge = baseline.headlineLarge.heading(FontWeight.ExtraBold, -0.6f),
    headlineMedium = baseline.headlineMedium.heading(FontWeight.ExtraBold, -0.5f),
    headlineSmall = baseline.headlineSmall.heading(FontWeight.Bold, -0.4f),
    titleLarge = baseline.titleLarge.heading(FontWeight.Bold, -0.3f),
    titleMedium = baseline.titleMedium.heading(FontWeight.Bold, -0.1f),
    titleSmall = baseline.titleSmall.heading(FontWeight.SemiBold, 0f),
    bodyLarge = baseline.bodyLarge.body(FontWeight.Normal, 0.1f),
    bodyMedium = baseline.bodyMedium.body(FontWeight.Normal, 0.1f),
    bodySmall = baseline.bodySmall.body(FontWeight.Normal, 0.15f),
    labelLarge = baseline.labelLarge.body(FontWeight.SemiBold),
    labelMedium = baseline.labelMedium.body(FontWeight.SemiBold),
    labelSmall = baseline.labelSmall.body(FontWeight.SemiBold)
)
