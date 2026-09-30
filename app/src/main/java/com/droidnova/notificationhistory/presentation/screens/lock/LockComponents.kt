package com.droidnova.notificationhistory.presentation.screens.lock

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.core.lock.LockType
import com.droidnova.notificationhistory.core.lock.SecretRules
import java.util.Locale

/** PIN dots + number pad, or a password field. [onSubmit] fires on the ✓ key / IME done. */
@Composable
fun SecretEntry(
    type: LockType,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false
) {
    val maxLength = SecretRules.maxLength(type)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when (type) {
            LockType.Pin -> {
                PinDots(length = value.length, isError = isError)
                Spacer(Modifier.height(24.dp))
                PinPad(
                    enabled = enabled,
                    canSubmit = value.length >= SecretRules.PIN_MIN,
                    onDigit = { digit -> if (value.length < maxLength) onValueChange(value + digit) },
                    onDelete = { onValueChange(value.dropLast(1)) },
                    onSubmit = onSubmit
                )
            }
            LockType.Password -> {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= maxLength) onValueChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    enabled = enabled,
                    isError = isError,
                    singleLine = true,
                    label = { Text(stringResource(R.string.lock_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (value.isNotEmpty()) onSubmit() })
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onSubmit,
                    enabled = enabled && value.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.lock_continue)) }
            }
        }
    }
}

@Composable
private fun PinDots(length: Int, isError: Boolean) {
    val description = stringResource(R.string.pin_dots_description, length)
    val filledColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .height(20.dp)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxOf(length, SecretRules.PIN_MIN)) { index ->
            val filled = index < length
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .then(
                        if (filled) Modifier.background(filledColor)
                        else Modifier.border(1.5.dp, outlineColor, CircleShape)
                    )
            )
        }
    }
}

@Composable
private fun PinPad(
    enabled: Boolean,
    canSubmit: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit
) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { digit ->
                    PinKey(label = digit.toString(), enabled = enabled, onClick = { onDigit(digit) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PinIconKey(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = stringResource(R.string.pin_pad_delete),
                enabled = enabled,
                onClick = onDelete
            )
            PinKey(label = "0", enabled = enabled, onClick = { onDigit('0') })
            PinIconKey(
                icon = Icons.Default.Check,
                description = stringResource(R.string.pin_pad_submit),
                enabled = enabled && canSubmit,
                onClick = onSubmit,
                emphasized = true
            )
        }
    }
}

private val KEY_SIZE = 68.dp

@Composable
private fun PinKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
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
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (emphasized && enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        contentColor = if (emphasized && enabled) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(KEY_SIZE)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description)
        }
    }
}

/** Wrong-attempt / lockout line under an entry field; empty when there is nothing to say. */
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
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
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

/** Choose type → enter → confirm. Input is held here and cleared on every step change. */
@Composable
fun NewSecretContent(
    state: NewSecretState,
    title: String,
    onChooseType: (LockType) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (state.stage) {
            NewSecretStage.ChooseType -> {
                LockHeader(title = title, subtitle = stringResource(R.string.lock_choose_type_title))
                Spacer(Modifier.height(24.dp))
                OptionCard(
                    title = stringResource(R.string.lock_type_pin),
                    description = stringResource(R.string.lock_type_pin_desc),
                    selected = state.type == LockType.Pin,
                    onClick = { onChooseType(LockType.Pin) }
                )
                Spacer(Modifier.height(12.dp))
                OptionCard(
                    title = stringResource(R.string.lock_type_password),
                    description = stringResource(R.string.lock_type_password_desc),
                    selected = state.type == LockType.Password,
                    onClick = { onChooseType(LockType.Password) }
                )
            }
            NewSecretStage.Enter, NewSecretStage.Confirm -> {
                var input by remember(state.stage, state.type) { mutableStateOf("") }
                val isPin = state.type == LockType.Pin
                val heading = when {
                    state.stage == NewSecretStage.Enter && isPin -> R.string.lock_enter_new_pin
                    state.stage == NewSecretStage.Enter -> R.string.lock_enter_new_password
                    isPin -> R.string.lock_confirm_pin
                    else -> R.string.lock_confirm_password
                }
                LockHeader(
                    title = stringResource(heading),
                    subtitle = stringResource(
                        if (isPin) R.string.lock_type_pin_desc else R.string.lock_type_password_desc
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.error?.let { newSecretErrorText(it, state.type) }.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                SecretEntry(
                    type = state.type,
                    value = input,
                    onValueChange = { input = it },
                    onSubmit = { onSubmit(input) },
                    isError = state.error != null
                )
            }
        }
    }
}

@Composable
private fun newSecretErrorText(error: NewSecretError, type: LockType): String = when (error) {
    NewSecretError.TooShort -> stringResource(
        if (type == LockType.Pin) R.string.lock_error_too_short_pin
        else R.string.lock_error_too_short_password
    )
    NewSecretError.TooLong -> stringResource(R.string.lock_error_too_long)
    NewSecretError.DigitsOnly -> stringResource(R.string.lock_error_digits)
    NewSecretError.Mismatch -> stringResource(R.string.lock_error_mismatch)
}

/** A tappable choice card; [isDestructive] tints it with the error color. */
@Composable
fun OptionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    isDestructive: Boolean = false
) {
    val containerColor = when {
        isDestructive -> MaterialTheme.colorScheme.errorContainer
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val contentColor = when {
        isDestructive -> MaterialTheme.colorScheme.onErrorContainer
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Shown exactly once per code; "Done" unlocks only after the user confirms they saved it. */
@Composable
fun RecoveryCodeContent(code: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var saved by remember(code) { mutableStateOf(false) }
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
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
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
        }) { Text(stringResource(R.string.lock_copy_code)) }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { saved = !saved }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = saved, onCheckedChange = { saved = it })
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.lock_code_saved_check))
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDone, enabled = saved, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.lock_done))
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
