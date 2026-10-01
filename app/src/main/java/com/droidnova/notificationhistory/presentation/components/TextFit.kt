package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Shrinks single-line text from [maxFontSize] down to 9 sp until it fits, for short labels in
 * fixed-width slots (stat tiles, segmented buttons) that must never wrap. Ellipsis remains the
 * last resort below the minimum.
 */
fun fitToWidth(maxFontSize: TextUnit): TextAutoSize =
    TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = maxFontSize, stepSize = 0.5.sp)
