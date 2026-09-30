package com.droidnova.notificationhistory.presentation.screens.lock

import com.droidnova.notificationhistory.core.lock.LockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NewSecretControllerTest {

    private val controller = NewSecretController()

    @Test
    fun `matching entries return the secret`() {
        controller.chooseType(LockType.Pin)
        assertNull(controller.submit("2468"))
        assertEquals(NewSecretStage.Confirm, controller.state.value.stage)
        assertEquals(LockType.Pin to "2468", controller.submit("2468"))
    }

    @Test
    fun `mismatch restarts entry with an error`() {
        controller.chooseType(LockType.Pin)
        controller.submit("2468")
        assertNull(controller.submit("2469"))
        assertEquals(NewSecretStage.Enter, controller.state.value.stage)
        assertEquals(NewSecretError.Mismatch, controller.state.value.error)
        // The old first entry is gone: confirming it alone doesn't complete.
        assertNull(controller.submit("2468"))
        assertEquals(NewSecretStage.Confirm, controller.state.value.stage)
    }

    @Test
    fun `invalid entry stays on the enter step`() {
        controller.chooseType(LockType.Password)
        assertNull(controller.submit("short"))
        assertEquals(NewSecretStage.Enter, controller.state.value.stage)
        assertEquals(NewSecretError.TooShort, controller.state.value.error)
    }

    @Test
    fun `back walks confirm to enter to type, then reports the start`() {
        controller.chooseType(LockType.Pin)
        controller.submit("2468")
        assertEquals(true, controller.back())
        assertEquals(NewSecretStage.Enter, controller.state.value.stage)
        assertEquals(true, controller.back())
        assertEquals(NewSecretStage.ChooseType, controller.state.value.stage)
        assertFalse(controller.back())
    }
}
