package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/** Gap between rows of one group; small, so the group reads as one block. */
val GroupRowGap = 2.dp
private val InnerCorner = 4.dp

/**
 * Segmented-list shape: only the outer corners of a group are fully rounded, so stacked rows
 * read as one block instead of a pile of separate cards.
 */
fun groupedShape(index: Int, count: Int, outer: Dp = Dimens.CardCornerRadius): Shape {
    val top = if (index == 0) outer else InnerCorner
    val bottom = if (index == count - 1) outer else InnerCorner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

class ListGroupScope internal constructor() {
    internal val rows = mutableListOf<@Composable (Shape) -> Unit>()

    /** Adds a row; render it with the given shape (pass it to [ListRow]'s `shape`). */
    fun row(content: @Composable (shape: Shape) -> Unit) {
        rows += content
    }
}

/** An optional small header followed by rows drawn as one segmented block. */
@Composable
fun ListGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: ListGroupScope.() -> Unit
) {
    val scope = ListGroupScope().apply(content)
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) SectionHeader(text = title, first = true, modifier = Modifier.padding(bottom = 6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(GroupRowGap)) {
            scope.rows.forEachIndexed { index, row -> row(groupedShape(index, scope.rows.size)) }
        }
    }
}

/** Small round tonal badge holding an icon; the visual anchor of every row. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * One compact row: leading badge/icon, a title, an optional one-line [value] under it, and a
 * trailing slot. Clickable rows get a chevron unless [trailing] is given.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    /** Search text to highlight inside [title]. */
    highlight: String = "",
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = AppCardDefaults.containerColor(),
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showChevron: Boolean = onClick != null && trailing == null
) {
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier
            .clip(shape)
            .combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = Dimens.ListRowMinHeight)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightedText(title, highlight),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (value != null) {
                    AnimatedText(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = valueColor
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
