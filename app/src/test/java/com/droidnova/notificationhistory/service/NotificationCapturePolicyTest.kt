package com.droidnova.notificationhistory.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCapturePolicyTest {

    private val selectedPackage = "com.example.selected"

    @Test
    fun `tracking off prevents capture`() {
        assertFalse(
            shouldCapture(hasAccess = true, tracking = false, packageName = selectedPackage)
        )
    }

    @Test
    fun `missing notification access prevents capture`() {
        assertFalse(
            shouldCapture(hasAccess = false, tracking = true, packageName = selectedPackage)
        )
    }

    @Test
    fun `empty selection prevents capture`() {
        assertFalse(
            shouldCapture(
                hasAccess = true,
                tracking = true,
                packageName = selectedPackage,
                allowedPackages = emptySet()
            )
        )
    }

    @Test
    fun `only explicitly selected package is captured`() {
        val allowed = setOf(selectedPackage)

        assertTrue(
            shouldCapture(
                hasAccess = true,
                tracking = true,
                packageName = selectedPackage,
                allowedPackages = allowed
            )
        )
        assertFalse(
            shouldCapture(
                hasAccess = true,
                tracking = true,
                packageName = "com.example.other",
                allowedPackages = allowed
            )
        )
    }

    @Test
    fun `deselecting final app prevents capture`() {
        val allowedAfterDeselect = emptySet<String>()

        assertFalse(
            shouldCapture(
                hasAccess = true,
                tracking = true,
                packageName = selectedPackage,
                allowedPackages = allowedAfterDeselect
            )
        )
    }

    @Test
    fun `select all allows selected packages and remove all prevents capture`() {
        val allPackages = setOf(selectedPackage, "com.example.other")

        assertTrue(
            shouldCapture(
                hasAccess = true,
                tracking = true,
                packageName = selectedPackage,
                allowedPackages = allPackages
            )
        )
        assertFalse(
            shouldCapture(
                hasAccess = true,
                tracking = true,
                packageName = selectedPackage,
                allowedPackages = emptySet()
            )
        )
    }

    private fun shouldCapture(
        hasAccess: Boolean,
        tracking: Boolean,
        packageName: String,
        allowedPackages: Set<String> = setOf(packageName)
    ): Boolean = shouldCaptureNotification(
        hasNotificationAccess = hasAccess,
        trackingEnabled = tracking,
        allowedPackages = allowedPackages,
        packageName = packageName
    )
}
