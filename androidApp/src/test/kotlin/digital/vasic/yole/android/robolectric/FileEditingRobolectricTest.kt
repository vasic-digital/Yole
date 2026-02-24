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
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
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

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun createNewFile() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Editing: untitled.txt").assertIsDisplayed()
    }

    @Test
    fun editFileContent() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("# Hello World\n\nThis is a test.")
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchToPreviewMode() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("# Test Document")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithText("Test Document").assertIsDisplayed()
    }

    @Test
    fun switchBackToEditMode() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("# Test")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        composeTestRule.onNodeWithText("Editing: untitled.txt").assertIsDisplayed()
    }

    @Test
    fun saveFile() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("test content")
        composeTestRule.onNodeWithContentDescription("Save").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigateBackFromEditor() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }
}
