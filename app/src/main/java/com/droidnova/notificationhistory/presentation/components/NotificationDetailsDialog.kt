package com.droidnova.notificationhistory.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.data.model.NotificationModel
import java.util.regex.Pattern

@Composable
fun NotificationDetailsDialog(
    notification: NotificationModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val urlPattern = remember {
        Pattern.compile(
            "(https?://|www\\.)" +
                    "[-a-zA-Z0-9+&@#/%?=~_:]+" +
                    "(\\.[-a-zA-Z0-9+&@#/%?=~_:]+)*" +
                    "([-a-zA-Z0-9+&@#/%?=~_]*)?"
        )
    }

    // Professional, softer blue (less harsh than Color.Blue)
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val linkColor = remember(primaryColor, onSurfaceColor) {
        lerp(primaryColor, onSurfaceColor, 0.35f)
    }

    val annotatedText = remember(notification.text, linkColor) {
        buildAnnotatedString {
            append(notification.text)

            val matcher = urlPattern.matcher(notification.text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val url = matcher.group()

                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = start,
                    end = end
                )

                addStringAnnotation(
                    tag = "URL",
                    annotation = url,
                    start = start,
                    end = end
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = notification.title.ifBlank { "Notification" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        },
        text = {
            // SelectionContainer makes text copyable (long-press to select/copy)
            SelectionContainer {
                ClickableText(
                    text = annotatedText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = { offset ->
                        annotatedText
                            .getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()
                            ?.let { annotation ->
                                val raw = annotation.item
                                val fixedUrl = if (raw.startsWith("www.", ignoreCase = true)) {
                                    "https://$raw"
                                } else raw

                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl))
                                    )
                                }
                            }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * clamped,
        green = a.green + (b.green - a.green) * clamped,
        blue = a.blue + (b.blue - a.blue) * clamped,
        alpha = a.alpha + (b.alpha - a.alpha) * clamped
    )
}
