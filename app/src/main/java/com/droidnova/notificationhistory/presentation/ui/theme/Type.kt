package com.droidnova.notificationhistory.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.droidnova.notificationhistory.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/**
 * Same type as the developer's other apps (Secret Calculator, Speedometer): Outfit for display,
 * headline and title styles, the platform sans for body and labels so longer text stays easy to
 * read. Outfit is a downloadable font; until it arrives the platform sans is used at the same weights.
 */
private val outfit = GoogleFont("Outfit")

val DisplayFontFamily = FontFamily(
    Font(googleFont = outfit, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = outfit, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = outfit, fontProvider = provider, weight = FontWeight.Bold)
)

private val BodyFontFamily: FontFamily = FontFamily.Default

private val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
    displayMedium = baseline.displayMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
    displaySmall = baseline.displaySmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = baseline.titleLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = baseline.titleMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = baseline.titleSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = BodyFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = BodyFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = BodyFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = baseline.labelMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = baseline.labelSmall.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium),
)
