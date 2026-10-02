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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOLD_MS = 650L
private const val LETTER_STEP_MS = 28
private val IconSize = 112.dp

/**
 * The first thing a cold start shows, right after the system splash (which shows the same icon):
 * the launcher icon pops in and rings like a bell while ripples spread behind it, then the app's
 * name rises in letter by letter. The whole intro then fades and lifts away, revealing the app
 * that has been composing underneath. Calls [onFinished] once it is gone.
 */
@Composable
fun SplashIntro(onFinished: () -> Unit) {
    val name = stringResource(R.string.app_name)
    val iconScale = remember { Animatable(0.6f) }
    val iconAlpha = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val ripple = remember { Animatable(0f) }
    val letters = remember(name) { name.map { Animatable(0f) } }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { iconAlpha.animateTo(1f, tween(180)) }
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
        delay(HOLD_MS)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    modifier = Modifier.graphicsLayer {
                        alpha = iconAlpha.value
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                        rotationZ = ring.value
                        // Swing from the top, like a bell on its hook
                        transformOrigin = TransformOrigin(0.5f, 0.1f)
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.Center) {
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
private fun LauncherIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(IconSize)
            .shadow(16.dp, RoundedCornerShape(30.dp), clip = false)
            .clip(RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            // The tile fills about two thirds of the foreground; scale so it fills the box
            modifier = Modifier.requiredSize(IconSize * 1.47f)
        )
    }
}
