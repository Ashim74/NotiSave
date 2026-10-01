package com.droidnova.notificationhistory.presentation.screens.lock

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidnova.notificationhistory.core.lock.AppLockController.Gate
import com.droidnova.notificationhistory.presentation.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setLockScreen(
        state: LockScreenState,
        deviceLockAvailable: Boolean = true,
        actions: LockScreenActions = LockScreenActions()
    ) {
        compose.setContent {
            AppTheme(dynamicColor = false) {
                LockScreenContent(
                    state = state,
                    biometricAvailable = false,
                    deviceLockAvailable = deviceLockAvailable,
                    actions = actions
                )
            }
        }
    }

    @Test
    fun lockedGateNeverComposesAppContent() {
        compose.setContent {
            AppLockGate(
                gate = Gate.Locked,
                lockContent = { Text("LOCK") },
                appContent = { Text("HISTORY") }
            )
        }
        compose.onNodeWithText("LOCK").assertExists()
        compose.onNodeWithText("HISTORY").assertDoesNotExist()
    }

    @Test
    fun pinSubmitsItselfOnTheFourthDigit() {
        var submitted: String? = null
        setLockScreen(LockScreenState(), actions = LockScreenActions(onSubmitPin = { submitted = it }))
        listOf("1", "2", "3").forEach { compose.onNodeWithText(it).performClick() }
        assertNull(submitted)
        compose.onNodeWithText("4").performClick()
        assertEquals("1234", submitted)
    }

    @Test
    fun wrongAttemptIsExplained() {
        setLockScreen(LockScreenState(feedback = VerifyFeedback(attemptsLeft = 2, rejections = 1)))
        compose.onNodeWithText("Incorrect. 2 attempts left before a timeout.").assertExists()
    }

    @Test
    fun forgotOffersPhoneScreenLockAndRecoveryCode() {
        var usedDeviceLock = 0
        setLockScreen(
            LockScreenState(stage = LockStage.Forgot),
            actions = LockScreenActions(onUseDeviceLock = { usedDeviceLock++ })
        )
        compose.onNodeWithText("Use recovery code").assertExists()
        compose.onNodeWithText("Use phone screen lock").performClick()
        assertEquals(1, usedDeviceLock)
    }

    @Test
    fun forgotWithoutPhoneScreenLockOffersOnlyTheCode() {
        setLockScreen(LockScreenState(stage = LockStage.Forgot), deviceLockAvailable = false)
        compose.onNodeWithText("Use phone screen lock").assertDoesNotExist()
        compose.onNodeWithText("Use recovery code").assertExists()
    }

    @Test
    fun recoveryCodeNeedsAllEightCharacters() {
        var submitted: String? = null
        setLockScreen(
            LockScreenState(stage = LockStage.RecoveryCode),
            actions = LockScreenActions(onSubmitRecoveryCode = { submitted = it })
        )
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("Recovery code").performTextInput("abcd-234")
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("Recovery code").performTextInput("5")
        compose.onNodeWithText("Continue").assertIsEnabled().performClick()
        assertEquals("abcd-2345", submitted)
    }
}
