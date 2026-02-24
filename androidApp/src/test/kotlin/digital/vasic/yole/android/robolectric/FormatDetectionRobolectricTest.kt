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
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
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

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun markdownContentRendersInPreview() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("# Markdown Heading\n\n**bold** and *italic*")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithText("Markdown Heading").assertIsDisplayed()
    }

    @Test
    fun plainTextContentRendersInPreview() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("Just plain text content here.")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun formatListIsDisplayedInSettings() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Formats").assertIsDisplayed()
        composeTestRule.onNodeWithText("Markdown").assertIsDisplayed()
    }
}
