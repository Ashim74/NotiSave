package com.droidnova.notificationhistory.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** One tone, shape and elevation for every card, so all screens read as one surface family. */
object AppCardDefaults {
    /** The theme's accent, faintly, over the raised surface (same as Secret Calculator's cards). */
    @Composable
    fun containerColor(): Color = tintedCardColor()

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
 * Flat tinted card with the app's rounded shape. Passing [onClick] (or [onLongClick]) makes the
 * whole card clickable with a clipped ripple and a soft press scale.
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
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val interaction = remember { MutableInteractionSource() }
    val clickable = onClick != null || onLongClick != null
    Card(
        modifier = modifier
            .then(if (clickable) Modifier.pressScale(interaction, 0.98f) else Modifier)
            .clip(shape)
            .then(
                if (clickable) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = ripple(),
                        onClick = onClick ?: {},
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        colors = colors,
        elevation = elevation,
        content = content
    )
}
