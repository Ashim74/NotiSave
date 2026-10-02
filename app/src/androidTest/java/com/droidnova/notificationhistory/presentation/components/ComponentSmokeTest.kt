package com.droidnova.notificationhistory.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidnova.notificationhistory.presentation.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyStateShowsTextAndRunsAction() {
        var clicks = 0
        compose.setContent {
            AppTheme(dynamicColor = false) {
                EmptyState(
                    title = "Nothing here",
                    description = "Explanation",
                    actionLabel = "Fix it",
                    onAction = { clicks++ }
                )
            }
        }
        compose.onNodeWithText("Nothing here").assertIsDisplayed()
        compose.onNodeWithText("Explanation").assertIsDisplayed()
        compose.onNodeWithText("Fix it").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun listRowRendersInDarkThemeWithValueAndTrailingSlot() {
        var clicks = 0
        compose.setContent {
            AppTheme(darkTheme = true, dynamicColor = false) {
                ListRow(
                    title = "Recording",
                    value = "64 apps",
                    leading = { IconBadge(Icons.Default.Warning) },
                    trailing = { Text("trailing") },
                    onClick = { clicks++ }
                )
            }
        }
        compose.onNodeWithText("64 apps").assertIsDisplayed()
        compose.onNodeWithText("trailing").assertIsDisplayed()
        compose.onNodeWithText("Recording").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun listGroupDrawsEveryRow() {
        compose.setContent {
            AppTheme(dynamicColor = false) {
                ListGroup(title = "History") {
                    row { shape -> ListRow(title = "Retention", shape = shape) }
                    row { shape -> ListRow(title = "Trash", shape = shape) }
                }
            }
        }
        compose.onNodeWithText("HISTORY").assertIsDisplayed()
        compose.onNodeWithText("Retention").assertIsDisplayed()
        compose.onNodeWithText("Trash").assertIsDisplayed()
    }
}
