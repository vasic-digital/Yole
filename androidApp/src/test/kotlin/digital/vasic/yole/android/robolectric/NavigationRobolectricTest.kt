/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Navigation between all screens
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
class NavigationRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun navigateToFilesScreen() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }

    @Test
    fun navigateToTodoScreen() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("To-Do List").assertIsDisplayed()
    }

    @Test
    fun navigateToQuickNoteScreen() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
    }

    @Test
    fun navigateToMoreScreen() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun fullNavigationCycle() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()

        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("To-Do List").assertIsDisplayed()

        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()

        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }

    @Test
    fun rapidNavigationDoesNotCrash() {
        for (i in 1..10) {
            composeTestRule.onNodeWithText("Files").performClick()
            composeTestRule.onNodeWithText("To-Do").performClick()
            composeTestRule.onNodeWithText("QuickNote").performClick()
            composeTestRule.onNodeWithText("More").performClick()
        }
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }
}
