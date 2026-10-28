package com.droidnova.notificationhistory.presentation.screens.reliability

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilitySetupPendingTest {

    @Test
    fun `battery optimization still on is pending`() {
        assertTrue(reliabilitySetupPending(batteryIgnored = false, hasOemStep = false, oemConfirmed = false))
    }

    @Test
    fun `unconfirmed oem step is pending`() {
        assertTrue(reliabilitySetupPending(batteryIgnored = true, hasOemStep = true, oemConfirmed = false))
    }

    @Test
    fun `phone without an oem step is done once battery is exempt`() {
        assertFalse(reliabilitySetupPending(batteryIgnored = true, hasOemStep = false, oemConfirmed = false))
    }

    @Test
    fun `everything granted is not pending`() {
        assertFalse(reliabilitySetupPending(batteryIgnored = true, hasOemStep = true, oemConfirmed = true))
    }
}
