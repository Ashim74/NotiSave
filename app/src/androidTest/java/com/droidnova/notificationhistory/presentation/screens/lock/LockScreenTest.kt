package com.droidnova.notificationhistory.presentation.screens.lock

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidnova.notificationhistory.core.lock.AppLockController.Gate
import com.droidnova.notificationhistory.presentation.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setLockScreen(
        state: LockScreenState,
        deviceRecoveryAvailable: Boolean = true,
        actions: LockScreenActions = LockScreenActions()
    ) {
        compose.setContent {
            AppTheme(dynamicColor = false) {
                LockScreenContent(
                    state = state,
                    biometricAvailable = false,
                    deviceRecoveryAvailable = deviceRecoveryAvailable,
                    actions = actions,
                    eraseDelaySeconds = 0
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
    fun pinPadSubmitsTypedDigits() {
        var submitted: String? = null
        setLockScreen(LockScreenState(), actions = LockScreenActions(onSubmitSecret = { submitted = it }))
        listOf("1", "2", "3", "4").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithContentDescription("Confirm").performClick()
        assertEquals("1234", submitted)
    }

    @Test
    fun wrongAttemptAndLockoutAreExplained() {
        setLockScreen(LockScreenState(feedback = VerifyFeedback(attemptsLeft = 2)))
        compose.onNodeWithText("Incorrect. 2 attempts left before a timeout.").assertExists()
    }

    @Test
    fun forgotOptionsHideScreenLockWhenUnavailable() {
        setLockScreen(LockScreenState(stage = LockStage.ForgotOptions), deviceRecoveryAvailable = false)
        compose.onNodeWithText("Use your phone's screen lock").assertDoesNotExist()
        compose.onNodeWithText("Enter recovery code").assertExists()
        compose.onNodeWithText("Erase history and reset lock").assertExists()
    }

    @Test
    fun eraseNeedsTheConfirmationWord() {
        var erased = 0
        setLockScreen(
            LockScreenState(stage = LockStage.EraseConfirm),
            actions = LockScreenActions(onConfirmErase = { erased++ })
        )
        compose.onNodeWithText("Erase and reset").assertIsNotEnabled()
        compose.onNodeWithText("Type ERASE to confirm").performTextInput("erase")
        compose.onNodeWithText("Erase and reset").assertIsEnabled().performClick()
        assertEquals(1, erased)
    }
}
