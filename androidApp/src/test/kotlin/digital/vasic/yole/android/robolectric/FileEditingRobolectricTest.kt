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

    private fun openNewDocumentEditor() {
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onAllNodesWithText("Create").onFirst().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun createNewFile() {
        openNewDocumentEditor()
        composeTestRule.onAllNodesWithText("untitled.md").onFirst().assertExists()
    }

    @Test
    fun editFileContent() {
        openNewDocumentEditor()
        composeTestRule.onNodeWithContentDescription("Code editor for untitled.md")
            .performTextInput("# Hello World\n\nThis is a test.")
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchToPreviewMode() {
        openNewDocumentEditor()
        composeTestRule.onNodeWithContentDescription("Code editor for untitled.md")
            .performTextInput("# Test Document")
        composeTestRule.onNodeWithContentDescription("Preview document").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchBackToEditMode() {
        openNewDocumentEditor()
        composeTestRule.onNodeWithContentDescription("Code editor for untitled.md")
            .performTextInput("# Test")
        composeTestRule.onNodeWithContentDescription("Preview document").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        composeTestRule.onAllNodesWithText("untitled.md").onFirst().assertExists()
    }

    @Test
    fun saveFile() {
        openNewDocumentEditor()
        composeTestRule.onNodeWithContentDescription("Code editor for untitled.md")
            .performTextInput("test content")
        composeTestRule.onNodeWithContentDescription("Save file").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigateBackFromEditor() {
        openNewDocumentEditor()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onAllNodesWithText("File Browser").onFirst().assertExists()
    }
}
