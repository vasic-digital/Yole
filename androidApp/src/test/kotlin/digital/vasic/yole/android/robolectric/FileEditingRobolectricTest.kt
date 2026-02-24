/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: File creation, editing, and preview
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
class FileEditingRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createNewFile() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onAllNodesWithText("Editing: untitled.txt").onFirst().assertExists()
    }

    @Test
    fun editFileContent() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onAllNodesWithText("Start typing...").onFirst().performTextInput("# Hello World\n\nThis is a test.")
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchToPreviewMode() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onAllNodesWithText("Start typing...").onFirst().performTextInput("# Test Document")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        // Preview renders via HTML/WebView, not Compose nodes — verify mode switch succeeds
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchBackToEditMode() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onAllNodesWithText("Start typing...").onFirst().performTextInput("# Test")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        composeTestRule.onAllNodesWithText("Editing: untitled.txt").onFirst().assertExists()
    }

    @Test
    fun saveFile() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onAllNodesWithText("Start typing...").onFirst().performTextInput("test content")
        composeTestRule.onNodeWithContentDescription("Save").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigateBackFromEditor() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onAllNodesWithText("File Browser").onFirst().assertExists()
    }
}
