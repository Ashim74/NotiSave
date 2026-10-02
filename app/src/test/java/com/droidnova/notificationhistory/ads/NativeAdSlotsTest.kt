package com.droidnova.notificationhistory.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeAdSlotsTest {

    @Test
    fun `first ad after the fifth item, then one every fifteen`() {
        val slots = (0 until 50).mapNotNull { index -> NativeAdSlots.slotAfter(index)?.let { index to it } }
        // Item indexes are 0-based: 4 is the 5th item, 19 the 20th, 34 the 35th, 49 the 50th
        assertEquals(listOf(4 to 0, 19 to 1, 34 to 2, 49 to 3), slots)
    }

    @Test
    fun `short lists get no ad`() {
        assertEquals(emptyList<Int>(), (0 until 4).mapNotNull(NativeAdSlots::slotAfter))
    }
}
