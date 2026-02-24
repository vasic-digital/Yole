/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: QuickNote workflow
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
class QuickNoteRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun quickNoteScreenLoads() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
    }

    @Test
    fun enterNoteContent() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("Meeting notes")
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchToPreview() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("# My Note")
        composeTestRule.onNodeWithText("Preview").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchBackToEdit() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("# My Note")
        composeTestRule.onNodeWithText("Preview").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun saveNote() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("Save this note")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
    }
}
