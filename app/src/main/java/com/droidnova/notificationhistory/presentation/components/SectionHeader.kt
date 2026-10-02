package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/**
 * Small uppercase label that introduces a group of cards or rows (as in Secret Calculator). Its
 * top padding tops up the normal card gap to [Dimens.SectionSpacing]; pass [first] for a header
 * at the top of a screen.
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, first: Boolean = false) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .padding(
                start = 6.dp,
                top = if (first) 0.dp else Dimens.SectionSpacing - Dimens.CardSpacing
            )
            .semantics { heading() },
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}
