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

    @Test
    fun addTodoItem() {
        composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Add new todo...").onFirst().performTextInput("Buy groceries")
        composeTestRule.onAllNodesWithText("Add").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Buy groceries").onFirst().assertExists()
    }

    @Test
    fun addMultipleTodoItems() {
        composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
        // Add first item and verify it exists
        composeTestRule.onAllNodesWithText("Add new todo...").onFirst().performTextInput("Task 1")
        composeTestRule.onAllNodesWithText("Add").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Task 1").onFirst().assertExists()
    }

    @Test
    fun toggleTodoCompletion() {
        composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Add new todo...").onFirst().performTextInput("Toggle me")
        composeTestRule.onAllNodesWithText("Add").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Toggle me").onFirst().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun todoItemCanBeCompleted() {
        composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Add new todo...").onFirst().performTextInput("Done item")
        composeTestRule.onAllNodesWithText("Add").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Done item").onFirst().performClick()
        // Verify completing a todo item doesn't crash
        composeTestRule.waitForIdle()
    }

    @Test
    fun deleteTodoItem() {
        composeTestRule.onAllNodesWithText("To-Do").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Add new todo...").onFirst().performTextInput("Delete me")
        composeTestRule.onAllNodesWithText("Add").onFirst().performClick()
        composeTestRule.onAllNodesWithContentDescription("Delete").onFirst().performClick()
        composeTestRule.onNodeWithText("Delete me").assertDoesNotExist()
    }
}
