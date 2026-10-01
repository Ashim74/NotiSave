package com.droidnova.notificationhistory.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Text that rolls to its new value instead of jumping, for live counters ("3 saved today") and
 * status lines. Numbers roll up when they grow and down when they shrink.
 */
@Composable
fun AnimatedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            val increasing = (targetState.digits() ?: 0) >= (initialState.digits() ?: 0)
            val direction = if (increasing) 1 else -1
            (fadeIn(tween(220)) + slideInVertically(tween(220)) { direction * it / 2 }) togetherWith
                (fadeOut(tween(120)) + slideOutVertically(tween(120)) { -direction * it / 2 }) using
                SizeTransform(clip = false)
        },
        label = "AnimatedText"
    ) { value ->
        Text(
            text = value,
            style = style,
            fontWeight = fontWeight,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun String.digits(): Long? = filter(Char::isDigit).take(18).toLongOrNull()
