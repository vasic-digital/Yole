/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Todo.txt workflow
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
class TodoWorkflowRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun addTodoItem() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Buy groceries")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Buy groceries").assertIsDisplayed()
    }

    @Test
    fun addMultipleTodoItems() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        val items = listOf("Task 1", "Task 2", "Task 3")
        for (item in items) {
            composeTestRule.onNodeWithText("Add new todo...").performTextInput(item)
            composeTestRule.onNodeWithText("Add").performClick()
        }
        for (item in items) {
            composeTestRule.onNodeWithText(item).assertIsDisplayed()
        }
    }

    @Test
    fun toggleTodoCompletion() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Toggle me")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Toggle me").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun toggleShowHideDone() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Done item")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Done item").performClick()
        composeTestRule.onNodeWithText("Hide Done").performClick()
        composeTestRule.onNodeWithText("Show Done").assertIsDisplayed()
    }

    @Test
    fun deleteTodoItem() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Delete me")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onAllNodesWithContentDescription("Delete").onFirst().performClick()
        composeTestRule.onNodeWithText("Delete me").assertDoesNotExist()
    }
}
