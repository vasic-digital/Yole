/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Settings modification flows
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
class SettingsRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun settingsScreenLoads() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun toggleLineNumbers() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Show line numbers").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun toggleAutoSave() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Auto-save").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun formatsListDisplayed() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Formats").assertIsDisplayed()
        composeTestRule.onNodeWithText("Markdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Todo.txt").assertIsDisplayed()
    }

    @Test
    fun aboutSectionDisplayed() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("About Yole").assertIsDisplayed()
    }

    @Test
    fun navigateBackFromSettings() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("More Options").assertIsDisplayed()
    }
}
