/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Theme switching and custom colors
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
class ThemeRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun defaultThemeAppliesWithoutCrash() {
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Files").onFirst().assertIsDisplayed()
    }

    @Test
    fun settingsScreenShowsThemeOptions() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Light theme").onFirst().assertExists()
        composeTestRule.onAllNodesWithText("Dark theme (IDE)").onFirst().assertExists()
        composeTestRule.onAllNodesWithText("System theme").onFirst().assertExists()
    }

    @Test
    fun lightThemeCanBeSelected() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Light theme").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()
    }

    @Test
    fun darkThemeCanBeSelected() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Dark theme (IDE)").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()
    }

    @Test
    fun systemThemeCanBeSelected() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onAllNodesWithText("System theme").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()
    }
}
