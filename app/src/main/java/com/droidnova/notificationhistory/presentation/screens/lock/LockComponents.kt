package com.droidnova.notificationhistory.presentation.screens.lock

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.core.lock.PinRules
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Four dots and a number pad. The PIN submits itself on the last digit — there is no confirm
 * key. Each change of [errorKey] shakes the dots, buzzes and clears the entry for the next try.
 */
@Composable
fun PinEntry(
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** The entered PIN is being checked or saved: the dots pulse so it never looks frozen. */
    checking: Boolean = false,
    errorKey: Any? = null,
    onFingerprint: (() -> Unit)? = null
) {
    var value by remember { mutableStateOf("") }
    val shake = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(errorKey) {
        if (errorKey == null) return@LaunchedEffect
        value = ""
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        for (offset in listOf(-18f, 16f, -12f, 8f, -4f, 0f)) {
            shake.animateTo(offset, spring(stiffness = 4_000f))
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PinDots(
            length = value.length,
            isError = errorKey != null && value.isEmpty(),
            checking = checking,
            modifier = Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }
        )
        Spacer(Modifier.height(32.dp))
        PinPad(
            enabled = enabled,
            onDigit = { digit ->
                if (value.length < PinRules.LENGTH) {
                    value += digit
                    if (value.length == PinRules.LENGTH) onComplete(value)
                }
            },
            onDelete = { value = value.dropLast(1) },
            onFingerprint = onFingerprint
        )
    }
}

@Composable
private fun PinDots(length: Int, isError: Boolean, checking: Boolean, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.pin_dots_description, length, PinRules.LENGTH)
    val pulse = rememberInfiniteTransition(label = "pinCheck").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "pinCheckAlpha"
    )
    Row(
        modifier = modifier
            .height(20.dp)
            .graphicsLayer { alpha = if (checking) pulse.value else 1f }
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(PinRules.LENGTH) { index ->
            val filled = index < length
            val color by animateColorAsState(
                when {
                    isError -> MaterialTheme.colorScheme.error
                    filled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                },
                label = "pinDot"
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .then(
                        if (filled) Modifier.background(color)
                        else Modifier.border(1.5.dp, color, CircleShape)
                    )
            )
        }
    }
}

@Composable
private fun PinPad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onFingerprint: (() -> Unit)?
) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    PinKey(label = digit.toString(), enabled = enabled, onClick = { onDigit(digit) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (onFingerprint != null) {
                PinIconKey(
                    icon = ImageVector.vectorResource(R.drawable.ic_fingerprint),
                    description = stringResource(R.string.pin_pad_fingerprint),
                    enabled = enabled,
                    onClick = onFingerprint
                )
            } else {
                Spacer(Modifier.size(KEY_SIZE))
            }
            PinKey(label = "0", enabled = enabled, onClick = { onDigit('0') })
            PinIconKey(
                icon = ImageVector.vectorResource(R.drawable.ic_backspace),
                description = stringResource(R.string.pin_pad_delete),
                enabled = enabled,
                onClick = onDelete
            )
        }
    }
}

private val KEY_SIZE = 72.dp

@Composable
private fun PinKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(KEY_SIZE)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun PinIconKey(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(KEY_SIZE)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description)
        }
    }
}

/** Wrong-attempt / lockout line under the dots; reserves its height so the pad never jumps. */
@Composable
fun VerifyFeedbackText(feedback: VerifyFeedback, modifier: Modifier = Modifier) {
    val text = when {
        feedback.isLockedOut -> stringResource(
            R.string.lock_locked_out,
            formatCountdown(feedback.lockoutRemainingMs)
        )
        feedback.attemptsLeft != null -> pluralStringResource(
            R.plurals.lock_wrong_attempts_left,
            feedback.attemptsLeft,
            feedback.attemptsLeft
        )
        else -> ""
    }
    ErrorLine(text, modifier)
}

@Composable
fun ErrorLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        minLines = 2
    )
}

fun formatCountdown(ms: Long): String {
    val totalSeconds = (ms + 999) / 1000
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun LockHeader(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** "Create a 4-digit PIN" → "Enter it again". [subtitle] explains why on the first step. */
@Composable
fun NewPinContent(
    state: NewPinState,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    checking: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        LockHeader(
            title = stringResource(
                if (state.stage == NewPinStage.Enter) R.string.lock_enter_new_pin
                else R.string.lock_confirm_pin
            ),
            subtitle = if (state.stage == NewPinStage.Enter) subtitle else null
        )
        Spacer(Modifier.height(8.dp))
        ErrorLine(if (state.showMismatch) stringResource(R.string.lock_error_mismatch) else "")
        Spacer(Modifier.height(8.dp))
        // Keyed on the step, so the dots start empty on "enter it again"; only a mismatch shakes.
        key(state.stage) {
            PinEntry(
                onComplete = onSubmit,
                enabled = enabled,
                checking = checking,
                errorKey = state.mismatchCount.takeIf { state.showMismatch }
            )
        }
    }
}

/** Shown once per code, with a copy button. */
@Composable
fun RecoveryCodeContent(code: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.lock_code_copied)
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        LockHeader(
            title = stringResource(R.string.lock_recovery_code_title),
            subtitle = stringResource(R.string.lock_recovery_code_body)
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                text = code,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = {
                val clip = ClipData.newPlainText(copiedMessage, code).apply {
                    // Keeps the code out of the clipboard preview on Android 13+.
                    description.extras = PersistableBundle().apply {
                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                }
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
                // Android 13+ confirms copies itself.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.lock_copy_code)) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.lock_code_saved))
        }
    }
}

/** Back arrow row for the multi-step lock flows. */
@Composable
fun FlowBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    }
}
