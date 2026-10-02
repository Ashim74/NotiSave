package com.droidnova.notificationhistory.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
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
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/**
 * Compact two-line notification row: app icon, title + time, then the body. The app's name
 * lives in the icon (and its TalkBack label), or becomes the title when the notification has
 * none. Pass [shape] from [groupedShape] and a small [spacing] to stack rows as one block.
 */
@Composable
fun NotificationHistoryCard(
    notification: NotificationModel,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    footerText: String? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium,
    spacing: Dp = 8.dp
) {
    val appName = notification.appName.ifBlank { notification.packageName }
    val title = notification.title.ifBlank { appName }
    val body = notification.text.replace('\n', ' ')
    val containerColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else AppCardDefaults.containerColor(),
        label = "notificationSelected"
    )
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontal, vertical = spacing / 2)
            .pressScale(interaction, 0.98f)
            .clip(shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.Top
        ) {
            Crossfade(targetState = selected, label = "notificationIcon") { isSelected ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(Dimens.TileCornerRadius))
                        .background(tileColor()),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.content_description_selected),
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        HistoryAppIcon(
                            packageName = notification.packageName,
                            size = 30.dp,
                            contentDescription = appName
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = highlightedText(title, searchQuery),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = notification.receivedAt.substringAfter(", "),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (body.isNotBlank()) {
                    Text(
                        text = highlightedText(body, searchQuery),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!footerText.isNullOrBlank()) {
                    Text(
                        text = footerText,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
fun HistoryAppIcon(
    packageName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null
) {
    val icon = if (packageName != null) rememberAppIcon(packageName) else null
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = contentDescription,
            modifier = modifier.size(size)
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_apps),
            contentDescription = contentDescription,
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
