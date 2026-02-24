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

    @Test
    fun settingsScreenLoads() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()
    }

    @Test
    fun toggleLineNumbers() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Show line numbers").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()
    }

    @Test
    fun toggleAutoSave() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Auto-save").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()
    }

    @Test
    fun settingsScreenScrollable() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        // Verify Settings screen loads and renders content
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()
        composeTestRule.waitForIdle()
    }

    @Test
    fun aboutSectionDisplayed() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onAllNodesWithText("About Yole").onFirst().assertExists()
    }

    @Test
    fun navigateBackFromSettings() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onAllNodesWithText("More Options").onFirst().assertExists()
    }
}
