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

    @Test
    fun navigateToFilesScreen() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onAllNodesWithText("File Browser").onFirst().assertExists()
    }

    @Test
    fun navigateToTodoScreen() {
        composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
        composeTestRule.onAllNodesWithText("To-Do List").onFirst().assertExists()
    }

    @Test
    fun navigateToQuickNoteScreen() {
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().assertIsDisplayed()
    }

    @Test
    fun navigateToMoreScreen() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()
    }

    @Test
    fun fullNavigationCycle() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onAllNodesWithText("File Browser").onFirst().assertExists()

        composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
        composeTestRule.onAllNodesWithText("To-Do List").onFirst().assertExists()

        composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().assertIsDisplayed()

        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()

        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onAllNodesWithText("File Browser").onFirst().assertExists()
    }

    @Test
    fun rapidNavigationDoesNotCrash() {
        for (i in 1..10) {
            composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
            composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
            composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
            composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        }
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onAllNodesWithText("File Browser").onFirst().assertExists()
    }
}
