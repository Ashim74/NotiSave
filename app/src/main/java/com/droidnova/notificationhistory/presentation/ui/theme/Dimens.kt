package com.droidnova.notificationhistory.presentation.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Spacing and shape tokens shared by every screen so layouts line up across tabs. */
object Dimens {
    /** Side margin between the screen edge and cards/rows. */
    val ScreenHorizontal = 12.dp

    /** Gap between two stacked cards. */
    val CardSpacing = 8.dp

    /** Gap between the last card of one section and the next section. */
    val SectionSpacing = 18.dp

    val CardCornerRadius = 22.dp

    /** Smaller tiles and icon wells that sit inside a card. */
    val TileCornerRadius = 14.dp

    /** Touch-friendly but compact: one title line plus an optional value line. */
    val ListRowMinHeight = 52.dp
}

/**
 * Lists add [listItemPadding] to every item instead of `spacedBy`, so cards from shared
 * components and screen-specific items all sit [Dimens.CardSpacing] apart. The list itself only
 * adds the other half of the gap at the top and bottom.
 */
val ScreenListContentPadding = PaddingValues(vertical = Dimens.CardSpacing / 2)

fun Modifier.listItemPadding(): Modifier =
    padding(horizontal = Dimens.ScreenHorizontal, vertical = Dimens.CardSpacing / 2)
