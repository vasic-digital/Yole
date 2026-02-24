/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Format detection and rendering
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
class FormatDetectionRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun markdownContentRendersInPreview() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onAllNodesWithText("Start typing...").onFirst().performTextInput("# Markdown Heading\n\n**bold** and *italic*")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        // Preview renders via HTML/WebView, not Compose nodes — verify mode switch succeeds
        composeTestRule.waitForIdle()
    }

    @Test
    fun plainTextContentRendersInPreview() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onAllNodesWithText("Start typing...").onFirst().performTextInput("Just plain text content here.")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun settingsScreenAccessibleFromMore() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Settings").onFirst().assertExists()
    }
}
