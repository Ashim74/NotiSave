package com.droidnova.notificationhistory.presentation.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOLD_MS = 450L
private const val LETTER_STEP_MS = 28
private val IconSize = 112.dp

// The system splash shows the icon as a ~190dp circle; the intro shrinks from there
private const val SYSTEM_SPLASH_SCALE = 1.7f

/**
 * The first thing a cold start shows, taking over from the system splash (same icon, same place)
 * on the very first frame: the icon settles into its rounded tile and rings like a bell while
 * ripples spread behind it, then the app's name rises in letter by letter.
 *
 * The heavy app UI is only built once that entrance has played ([onEntranceDone]), so building it
 * can't stutter the animation. If the app still isn't [ready] by then, the ripples keep pulsing
 * so the screen never looks frozen. Then the intro fades and lifts away and calls [onFinished].
 */
@Composable
fun SplashIntro(ready: Boolean, onEntranceDone: () -> Unit, onFinished: () -> Unit) {
    val isReady by rememberUpdatedState(ready)
    val name = stringResource(R.string.app_name)
    // Starts at the size and round shape of the system splash icon, so the handoff is seamless
    val iconScale = remember { Animatable(SYSTEM_SPLASH_SCALE) }
    val iconMorph = remember { Animatable(0f) }

    val ring = remember { Animatable(0f) }
    val ripple = remember { Animatable(0f) }
    val letters = remember(name) { name.map { Animatable(0f) } }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { iconMorph.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
            launch {
                iconScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            }
            launch { ripple.animateTo(1f, tween(1300, easing = LinearEasing)) }
            launch {
                delay(260)
                // A bell's swing: big, then smaller and smaller either side, then still
                ring.animateTo(
                    0f,
                    keyframes {
                        durationMillis = 700
                        0f at 0
                        14f at 90
                        -12f at 200
                        9f at 310
                        -6f at 420
                        3f at 530
                        0f at 700
                    }
                )
            }
            val lettersDone = async {
                delay(330)
                letters.forEachIndexed { index, letter ->
                    launch {
                        delay((index * LETTER_STEP_MS).toLong())
                        letter.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                    }
                }
                delay((letters.size * LETTER_STEP_MS).toLong())
            }
            lettersDone.await()
        }
        // Build the app underneath now; give it two frames to compose and lay out
        onEntranceDone()
        withFrameNanos { }
        withFrameNanos { }
        delay(HOLD_MS)
        // Still loading (slow phone, first run): keep the ripples going instead of freezing
        while (!isReady) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(1300, easing = LinearEasing))
        }
        exit.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        onFinished()
    }

    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                scaleX = 1f + exit.value * 0.06f
                scaleY = 1f + exit.value * 0.06f
            }
            .background(colors.background)
            // A soft teal glow behind the icon
            .background(
                Brush.radialGradient(
                    listOf(colors.primary.copy(alpha = 0.16f), colors.background.copy(alpha = 0f)),
                    radius = 900f
                )
            )
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        // The icon sits at the exact center, where the system splash drew it; the name hangs below
        Box(contentAlignment = Alignment.Center) {
            Box(contentAlignment = Alignment.Center) {
                // Three rings spreading out from the icon, one after another
                val rippleColor = colors.primary
                Canvas(Modifier.size(260.dp)) {
                    repeat(3) { i ->
                        val t = ((ripple.value - i * 0.22f) / 0.78f).coerceIn(0f, 1f)
                        if (t > 0f) {
                            val radius = IconSize.toPx() / 2f + t * (size.minDimension / 2f - IconSize.toPx() / 2f)
                            drawCircle(
                                color = rippleColor.copy(alpha = (1f - t) * 0.35f),
                                radius = radius,
                                style = Stroke(width = (1f - t) * 6.dp.toPx() + 1f)
                            )
                        }
                    }
                }
                LauncherIcon(
                    morph = iconMorph.value,
                    modifier = Modifier.graphicsLayer {
                        alpha = 1f
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                        rotationZ = ring.value
                        // Swing from the top, like a bell on its hook
                        transformOrigin = TransformOrigin(0.5f, 0.1f)
                    }
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.offset(y = IconSize / 2 + 44.dp)
            ) {
                name.forEachIndexed { index, char ->
                    val progress = letters[index]
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onBackground,
                        modifier = Modifier.graphicsLayer {
                            alpha = progress.value.coerceIn(0f, 1f)
                            translationY = (1f - progress.value) * 18.dp.toPx()
                        }
                    )
                }
            }
        }
    }
}

/**
 * The launcher icon's artwork (its adaptive foreground, which carries the rounded tile) cropped
 * to that tile, so it looks exactly like the icon on the home screen.
 */
@Composable
private fun LauncherIcon(morph: Float, modifier: Modifier = Modifier) {
    // morph 0: the system splash's circle, flat; morph 1: the launcher's rounded tile, raised
    val shape = RoundedCornerShape(percent = (50 - 23 * morph).roundToInt())
    Box(
        modifier = modifier
            .size(IconSize)
            .shadow(16.dp * morph, shape, clip = false)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            // The tile spans ~280 of the foreground's 432 px; scale so it fills the box edge to edge
            modifier = Modifier.requiredSize(IconSize * 1.55f)
        )
    }
}
