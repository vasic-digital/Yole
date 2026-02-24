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
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
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

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun mainNavigationItemsAreAccessible() {
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("To-Do").assertIsDisplayed()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
        composeTestRule.onNodeWithText("More").assertIsDisplayed()
    }

    @Test
    fun addButtonHasContentDescription() {
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()
    }

    @Test
    fun todoInputFieldIsAccessible() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").assertIsDisplayed()
    }

    @Test
    fun quickNoteInputFieldIsAccessible() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").assertIsDisplayed()
    }

    @Test
    fun allInteractiveElementsClickable() {
        composeTestRule.onNodeWithText("Files").assertHasClickAction()
        composeTestRule.onNodeWithText("To-Do").assertHasClickAction()
        composeTestRule.onNodeWithText("QuickNote").assertHasClickAction()
        composeTestRule.onNodeWithText("More").assertHasClickAction()
    }
}
