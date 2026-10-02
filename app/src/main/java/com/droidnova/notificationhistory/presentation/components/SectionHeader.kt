package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/**
 * A section's heading: a short accent bar and a bold title in the main text color, so groups
 * stand out when scanning a screen. Its top padding tops up the normal card gap to
 * [Dimens.SectionSpacing]; pass [first] for a header at the top of a screen.
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, first: Boolean = false) {
    Row(
        modifier = modifier
            .padding(
                start = 4.dp,
                top = if (first) 0.dp else Dimens.SectionSpacing - Dimens.CardSpacing
            )
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentBar()
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The small rounded bar that marks every section heading. */
@Composable
fun AccentBar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 4.dp, height = 16.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
    )
}
