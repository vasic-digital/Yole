/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Accessibility checks
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AccessibilityRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainNavigationItemsAreAccessible() {
        composeTestRule.onAllNodesWithText("Files").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("To-Do").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("More").onFirst().assertIsDisplayed()
    }

    @Test
    fun addButtonHasContentDescription() {
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()
    }

    @Test
    fun todoInputFieldIsAccessible() {
        composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Add new todo...").onFirst().assertExists()
    }

    @Test
    fun quickNoteInputFieldIsAccessible() {
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Start writing your quick note...").onFirst().assertExists()
    }

    @Test
    fun allInteractiveElementsClickable() {
        // Verify navigation items are clickable by performing clicks
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.waitForIdle()
    }
}
