package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.core.apps.AppInfoCache
import com.droidnova.notificationhistory.data.model.NotificationModel

@Composable
fun NotificationHistoryCard(
    notification: NotificationModel,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    footerText: String? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HistoryAppIcon(packageName = notification.packageName)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = notification.appName.ifBlank { notification.packageName },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = notification.receivedAt.substringAfter(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (notification.title.isNotBlank()) {
                Text(
                    text = highlightedText(notification.title, searchQuery),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (notification.text.isNotBlank()) {
                Text(
                    text = highlightedText(notification.text, searchQuery),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!footerText.isNullOrBlank()) {
                Text(
                    text = footerText,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Shows the launcher icon for [packageName] from [AppInfoCache]: cached icons render on the
 * first frame, uncached ones load off the main thread. A null package (or an uninstalled one)
 * falls back to the generic icon.
 */
@Composable
fun HistoryAppIcon(packageName: String?, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val icon = if (packageName != null) rememberAppIcon(packageName) else null
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = modifier.size(size)
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_apps),
            contentDescription = null,
            modifier = modifier.size(size),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState(initialValue = AppInfoCache.peekIcon(packageName), packageName) {
        if (value == null) value = AppInfoCache.icon(context, packageName)
    }
    return icon
}

@Composable
fun PackageAppIcon(packageName: String, modifier: Modifier = Modifier) {
    HistoryAppIcon(packageName = packageName, modifier = modifier)
}

@Composable
fun HistoryEmptyState(message: String, modifier: Modifier = Modifier) {
    EmptyState(title = message, modifier = modifier)
}

@Composable
fun HistoryLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
    }
}

/** Highlights case-insensitive matches of [query] using theme roles (readable in dark mode). */
@Composable
fun highlightedText(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return buildAnnotatedString { append(text) }
    val highlightColor = MaterialTheme.colorScheme.secondaryContainer
    val highlightContentColor = MaterialTheme.colorScheme.onSecondaryContainer
    return buildAnnotatedString {
        var startIndex = 0
        while (startIndex < text.length) {
            val index = text.indexOf(query, startIndex, ignoreCase = true)
            if (index == -1) {
                append(text.substring(startIndex))
                break
            }
            append(text.substring(startIndex, index))
            withStyle(
                SpanStyle(
                    color = highlightContentColor,
                    background = highlightColor
                )
            ) {
                append(text.substring(index, index + query.length))
            }
            startIndex = index + query.length
        }
    }
}
