package com.droidnova.notificationhistory.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Items grouped under "Today" / "Yesterday" / a date, each day drawn as one segmented block
 * (see [groupedShape]). [epochOf] picks the timestamp to group by; [content] draws one item
 * with the shape it should use.
 *
 * [slotAfter] can open a gap after an item (by its position in the whole list) for something
 * else, such as a native ad, drawn by [slot]. The rows on either side of a gap get rounded ends,
 * so each piece still reads as its own block.
 */
fun <T> LazyListScope.dayGroupedItems(
    items: List<T>,
    key: (T) -> Any,
    epochOf: (T) -> Long,
    slotAfter: (globalIndex: Int) -> Int? = { null },
    slot: @Composable LazyItemScope.(slot: Int) -> Unit = {},
    content: @Composable LazyItemScope.(item: T, shape: Shape) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val groups = items.groupBy { Instant.ofEpochMilli(epochOf(it)).atZone(zone).toLocalDate() }
    var first = true
    var globalIndex = 0
    groups.forEach { (date, dayItems) ->
        val isFirst = first
        first = false
        item(key = "day-$date", contentType = "day-header") {
            DayHeader(date = date, first = isFirst, modifier = Modifier.animateItem())
        }
        // Split the day into runs at every slot; each run is one segmented block
        var run = mutableListOf<T>()
        val flush = { runItems: List<T> ->
            itemsIndexed(runItems, key = { _, item -> key(item) }, contentType = { _, _ -> "row" }) { index, item ->
                content(item, groupedShape(index, runItems.size))
            }
        }
        dayItems.forEach { dayItem ->
            run += dayItem
            val slotIndex = slotAfter(globalIndex)
            globalIndex++
            if (slotIndex != null) {
                flush(run)
                run = mutableListOf()
                item(key = "slot-$slotIndex", contentType = "slot") { slot(slotIndex) }
            }
        }
        if (run.isNotEmpty()) flush(run)
    }
}

@Composable
private fun DayHeader(date: LocalDate, first: Boolean, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    SectionHeader(
        text = when (date) {
            today -> stringResource(R.string.date_today)
            today.minusDays(1) -> stringResource(R.string.date_yesterday)
            else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        },
        first = true,
        modifier = modifier.padding(
            start = Dimens.ScreenHorizontal,
            end = Dimens.ScreenHorizontal,
            top = if (first) 4.dp else 14.dp,
            bottom = 6.dp
        )
    )
}
