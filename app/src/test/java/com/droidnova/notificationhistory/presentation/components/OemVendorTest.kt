package com.droidnova.notificationhistory.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class OemVendorTest {

    @Test
    fun `samsung is detected case-insensitively`() {
        assertEquals(OemVendor.Samsung, oemVendorOf("samsung"))
        assertEquals(OemVendor.Samsung, oemVendorOf("SAMSUNG "))
    }

    @Test
    fun `sub-brands map to their parent vendor`() {
        assertEquals(OemVendor.Xiaomi, oemVendorOf("Redmi"))
        assertEquals(OemVendor.Oppo, oemVendorOf("realme"))
        assertEquals(OemVendor.Vivo, oemVendorOf("iQOO"))
        assertEquals(OemVendor.Huawei, oemVendorOf("HONOR"))
    }

    @Test
    fun `unknown or missing manufacturer needs no extra step`() {
        assertEquals(OemVendor.Other, oemVendorOf("Google"))
        assertEquals(OemVendor.Other, oemVendorOf(null))
    }
}

class OemStepVisibilityTest {

    private fun show(
        hasStep: Boolean = true,
        dismissed: Boolean = true,
        captureExpected: Boolean = true,
        listenerConnected: Boolean = true
    ) = shouldShowOemStep(hasStep, dismissed, captureExpected, listenerConnected)

    @Test
    fun `undismissed step is shown`() {
        assertEquals(true, show(dismissed = false))
    }

    @Test
    fun `dismissed step stays hidden while capture works`() {
        assertEquals(false, show())
    }

    @Test
    fun `dismissed step comes back while the listener is disconnected`() {
        assertEquals(true, show(listenerConnected = false))
    }

    @Test
    fun `dismissed step stays hidden when capture is off on purpose`() {
        assertEquals(false, show(captureExpected = false, listenerConnected = false))
    }

    @Test
    fun `no step for this phone means nothing to show`() {
        assertEquals(false, show(hasStep = false, dismissed = false, listenerConnected = false))
    }
}
