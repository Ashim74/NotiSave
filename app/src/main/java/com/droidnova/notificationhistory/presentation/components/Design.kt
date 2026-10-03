package com.droidnova.notificationhistory.presentation.components

import com.droidnova.notificationhistory.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * The visual language shared with the developer's other apps (Secret Calculator, Speedometer):
 * softly tinted rounded cards, small uppercase section labels, compact rows led by a round icon
 * badge, and a little motion on everything you touch. Colors come from the theme.
 */

/** Card fill: the theme's accent, faintly, over the raised surface. */
@Composable
fun tintedCardColor(): Color =
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainer)

/** Lighter fill for tiles and icon badges sitting on a card. */
@Composable
fun tileColor(): Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)

/** Shrinks a little while pressed, so taps feel physical. */
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Fades and lifts content in when it first appears, [index] steps after the first item, so
 * grids and lists arrive one tile after another. Runs once per composition of the item.
 */
fun Modifier.appearIn(index: Int, stepMillis: Int = 35): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(
                durationMillis = 320,
                delayMillis = (index * stepMillis).coerceAtMost(400),
                easing = FastOutSlowInEasing
            )
        )
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

/** A slow, endless bob up and down, for empty-state and hero icons. */
fun Modifier.floating(distance: Dp = 6.dp, periodMillis: Int = 2200): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "floating")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatingOffset"
    )
    graphicsLayer { translationY = offset * distance.toPx() }
}

/** A soft breathing scale, for "live" indicators like the recording dot. */
fun Modifier.pulsing(minScale: Float = 0.85f, periodMillis: Int = 1400): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * A round badge holding an icon, the lead of every row. With [accent] the badge takes that color
 * (soft circle, icon in full color), like the category tiles in Secret Calculator.
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    containerColor: Color = accent?.copy(alpha = 0.18f) ?: tileColor(),
    contentColor: Color = accent ?: MaterialTheme.colorScheme.primary,
    size: Dp = 38.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(size * 0.53f))
    }
}

/** A round tinted icon button for top bars, matching the badges in rows. */
@Composable
fun HeaderButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .padding(start = 6.dp)
            .size(40.dp)
            .pressScale(interaction)
            .clip(CircleShape)
            .background(tintedCardColor())
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/**
 * A selectable pill: optional color dot or leading icon, label, and a check when selected
 * ([showCheck]) or a lock when the choice needs Premium ([locked]).
 */
@Composable
fun ChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    locked: Boolean = false,
    leadingIcon: ImageVector? = null,
    showCheck: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        if (selected) colors.primaryContainer.copy(alpha = 0.55f) else colors.surface.copy(alpha = 0.6f),
        label = "pillColor"
    )
    val border by animateColorAsState(if (selected) colors.primary else colors.outlineVariant, label = "pillBorder")
    Row(
        modifier = modifier
            .pressScale(interaction)
            .clip(RoundedCornerShape(50))
            .background(container)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, border), RoundedCornerShape(50))
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dotColor != null) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.primary else colors.onSurface,
            maxLines = 1
        )
        when {
            selected && showCheck ->
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
            locked ->
                Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.choice_locked_premium), tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
    }
}

/** A bold gradient card for the one highlighted action on a screen (Premium). */
@Composable
fun GradientBanner(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.98f)
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon,
            containerColor = colors.onPrimary.copy(alpha = 0.18f),
            contentColor = colors.onPrimary,
            size = 42.dp,
            modifier = Modifier.floating(distance = 2.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onPrimary.copy(alpha = 0.85f), maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onPrimary)
    }
}

/**
 * A round action with its label underneath; several sit side by side (quick actions, sheets).
 * [accent] colors the badge like a category tile.
 */
@Composable
fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    destructive: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressScale(interaction)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when {
            destructive -> IconBadge(
                icon,
                containerColor = colors.errorContainer.copy(alpha = 0.4f),
                contentColor = colors.error,
                size = 48.dp
            )
            accent != null -> IconBadge(icon, accent = accent, size = 48.dp)
            else -> IconBadge(icon, containerColor = tintedCardColor(), size = 48.dp)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (destructive) colors.error else colors.onSurface,
            maxLines = 1
        )
    }
}

/** The top of a bottom sheet or dialog: icon badge, title and one short line. */
@Composable
fun SheetHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier, subtitle: String? = null, accent: Color? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (accent != null) {
            IconBadge(icon, accent = accent, size = 48.dp)
        } else {
            IconBadge(icon, containerColor = tintedCardColor(), size = 48.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A small rounded count in [color], animated when it changes. */
@Composable
fun CountPill(count: Int, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 2.dp)
    ) {
        AnimatedText(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
