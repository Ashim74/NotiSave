package com.droidnova.notificationhistory.data.datastore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingResolutionTest {

    @Test
    fun `fresh install shows onboarding`() {
        assertFalse(resolveOnboardingComplete(storedFlag = null, hasAllowedApps = false, launchCount = 0))
        assertFalse(resolveOnboardingComplete(storedFlag = null, hasAllowedApps = false, launchCount = 1))
    }

    @Test
    fun `upgrader with selected apps skips onboarding`() {
        assertTrue(resolveOnboardingComplete(storedFlag = null, hasAllowedApps = true, launchCount = 1))
    }

    @Test
    fun `upgrader who launched before skips onboarding even without apps`() {
        assertTrue(resolveOnboardingComplete(storedFlag = null, hasAllowedApps = false, launchCount = 2))
    }

    @Test
    fun `explicit flag always wins`() {
        assertTrue(resolveOnboardingComplete(storedFlag = true, hasAllowedApps = false, launchCount = 0))
        assertFalse(resolveOnboardingComplete(storedFlag = false, hasAllowedApps = true, launchCount = 50))
    }

    @Test
    fun `rate card reset to zero does not resurrect onboarding for users with apps`() {
        // "Rate Later" resets launchCount to 0; selected apps must still mark the user onboarded.
        assertTrue(resolveOnboardingComplete(storedFlag = null, hasAllowedApps = true, launchCount = 0))
    }
}
