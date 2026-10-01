package com.droidnova.notificationhistory.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

/** One tone, shape and elevation for every card, so all screens read as one surface family. */
object AppCardDefaults {
    /** Theme-derived sage tint; correct in light, dark and dynamic color schemes. */
    @Composable
    fun containerColor(): Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    /** Container and content colors animate, so state changes (tracking on/off) fade smoothly. */
    @Composable
    fun colors(
        containerColor: Color = containerColor(),
        contentColor: Color = MaterialTheme.colorScheme.onSurface
    ): CardColors {
        val container by animateColorAsState(containerColor, label = "cardContainer")
        val content by animateColorAsState(contentColor, label = "cardContent")
        return CardDefaults.cardColors(containerColor = container, contentColor = content)
    }
}

/**
 * Filled card with the app's 16 dp shape and flat tonal elevation. Passing [onClick] (or
 * [onLongClick]) makes the whole card clickable with a clipped ripple.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    colors: CardColors = AppCardDefaults.colors(),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val elevation = CardDefaults.cardElevation()
    when {
        onLongClick != null -> Card(
            modifier = modifier
                .clip(shape)
                .combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick),
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = content
        )
        onClick != null -> Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = content
        )
        else -> Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = content
        )
    }
}
