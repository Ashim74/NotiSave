package com.droidnova.notificationhistory.presentation.screens.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewPinControllerTest {

    private val controller = NewPinController()

    @Test
    fun `matching entries return the pin`() {
        assertNull(controller.submit("2468"))
        assertEquals(NewPinStage.Confirm, controller.state.value.stage)
        assertEquals("2468", controller.submit("2468"))
    }

    @Test
    fun `mismatch restarts entry and counts the mismatch`() {
        controller.submit("2468")
        assertNull(controller.submit("2469"))
        assertEquals(NewPinStage.Enter, controller.state.value.stage)
        assertTrue(controller.state.value.showMismatch)
        // The old first entry is gone: confirming it alone doesn't complete.
        assertNull(controller.submit("2468"))
        assertEquals(NewPinStage.Confirm, controller.state.value.stage)
        assertFalse(controller.state.value.showMismatch)
        // A second mismatch bumps the counter so the UI shakes again.
        controller.submit("1111")
        assertEquals(2, controller.state.value.mismatchCount)
    }

    @Test
    fun `invalid input is ignored`() {
        assertNull(controller.submit("123"))
        assertNull(controller.submit("12a4"))
        assertEquals(NewPinStage.Enter, controller.state.value.stage)
    }

    @Test
    fun `back walks confirm to enter, then reports the start`() {
        controller.submit("2468")
        assertTrue(controller.back())
        assertEquals(NewPinStage.Enter, controller.state.value.stage)
        assertFalse(controller.back())
    }
}
