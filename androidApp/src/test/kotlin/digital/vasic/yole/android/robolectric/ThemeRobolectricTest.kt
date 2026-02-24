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
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
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

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun defaultThemeAppliesWithoutCrash() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun settingsScreenShowsThemeOptions() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Light theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("System theme (follows system setting)").assertIsDisplayed()
    }

    @Test
    fun lightThemeCanBeSelected() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Light theme").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun darkThemeCanBeSelected() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Dark theme").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun systemThemeCanBeSelected() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("System theme (follows system setting)").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
