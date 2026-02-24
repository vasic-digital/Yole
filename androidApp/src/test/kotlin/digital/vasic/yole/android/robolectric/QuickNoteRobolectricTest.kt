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

    @Test
    fun quickNoteScreenLoads() {
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().assertIsDisplayed()
    }

    @Test
    fun enterNoteContent() {
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Start writing your quick note...").onFirst().performTextInput("Meeting notes")
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchToPreview() {
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Start writing your quick note...").onFirst().performTextInput("# My Note")
        composeTestRule.onAllNodesWithText("Preview").onFirst().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchBackToEdit() {
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Start writing your quick note...").onFirst().performTextInput("# My Note")
        composeTestRule.onAllNodesWithText("Preview").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Edit").onFirst().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun saveNote() {
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Start writing your quick note...").onFirst().performTextInput("Save this note")
        composeTestRule.onAllNodesWithText("Save").onFirst().performClick()
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().assertIsDisplayed()
    }
}
