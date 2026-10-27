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
