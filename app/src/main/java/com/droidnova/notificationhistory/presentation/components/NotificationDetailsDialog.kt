package com.droidnova.notificationhistory.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
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
        val linkStyles = TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
        )
        buildAnnotatedString {
            append(notification.text)

            val matcher = urlPattern.matcher(notification.text)
            while (matcher.find()) {
                val raw = matcher.group()
                val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
                addLink(
                    LinkAnnotation.Url(url, linkStyles) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    start = matcher.start(),
                    end = matcher.end()
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(Dimens.TileCornerRadius))
                    .background(tintedCardColor()),
                contentAlignment = Alignment.Center
            ) {
                HistoryAppIcon(
                    packageName = notification.packageName,
                    size = 38.dp,
                    contentDescription = notification.appName.ifBlank { notification.packageName }
                )
            }
        },
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = notification.title.ifBlank { stringResource(R.string.notification_fallback_title) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(notification.appName, notification.receivedAt)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            // SelectionContainer makes text copyable (long-press to select/copy)
            SelectionContainer {
                Text(
                    text = annotatedText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.TileCornerRadius))
                        .background(tintedCardColor())
                        .heightIn(min = 64.dp, max = 360.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
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
