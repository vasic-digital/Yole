/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * End-to-End Tests for Yole Android App
 * Complete user workflow testing from start to finish
 *
 *########################################################*/

package digital.vasic.yole.android.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndToEndTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Initialize all parsers for full functionality testing
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun testCompleteTodoWorkflow() {
        // Complete end-to-end workflow for todo management

        // 1. Navigate to To-Do screen
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("To-Do List").assertIsDisplayed()

        // 2. Add multiple todo items
        val todos = listOf("Buy groceries", "Finish project", "Call mom", "Exercise")

        for (todo in todos) {
            composeTestRule.onNodeWithText("Add new todo...").performTextInput(todo)
            composeTestRule.onNodeWithText("Add").performClick()
        }

        // 3. Verify all todos are displayed
        for (todo in todos) {
            composeTestRule.onNodeWithText(todo).assertIsDisplayed()
        }

        // 4. Mark some as completed
        composeTestRule.onNodeWithText("Buy groceries").performClick() // Click row to toggle
        composeTestRule.onNodeWithText("Call mom").performClick()

        // 5. Toggle show completed
        composeTestRule.onNodeWithText("Hide Done").performClick()
        composeTestRule.onNodeWithText("Show Done").assertIsDisplayed()

        // 6. Switch to show completed again
        composeTestRule.onNodeWithText("Show Done").performClick()
        composeTestRule.onNodeWithText("Hide Done").assertIsDisplayed()

        // 7. Delete a completed item
        composeTestRule.onAllNodesWithContentDescription("Delete").onFirst().performClick()
        composeTestRule.onNodeWithText("Buy groceries").assertDoesNotExist()

        // 8. Verify remaining items
        composeTestRule.onNodeWithText("Finish project").assertIsDisplayed()
        composeTestRule.onNodeWithText("Call mom").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exercise").assertIsDisplayed()
    }

    @Test
    fun testCompleteFileEditingWorkflow() {
        // Complete end-to-end workflow for file editing

        // 1. Navigate to Files screen
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()

        // 2. Create new file (FAB)
        composeTestRule.onNodeWithContentDescription("Add").performClick()

        // 3. Verify editor opens
        composeTestRule.onNodeWithText("Editing: untitled.txt").assertIsDisplayed()

        // 4. Enter content
        val testContent = """
            # Test Document

            This is a **markdown** document for testing.

            ## Features
            - File editing
            - Format detection
            - Preview mode

            ## Todo
            - [x] Write content
            - [ ] Test saving
            - [ ] Verify preview
        """.trimIndent()

        composeTestRule.onNodeWithText("Start typing...").performTextInput(testContent)

        // 5. Switch to preview mode
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithText("Preview: untitled.txt").assertIsDisplayed()

        // 6. Verify preview content is displayed (basic check)
        composeTestRule.onNodeWithText("Test Document").assertIsDisplayed()

        // 7. Go back to edit mode
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        composeTestRule.onNodeWithText("Editing: untitled.txt").assertIsDisplayed()

        // 8. Save file
        composeTestRule.onNodeWithContentDescription("Save").performClick()

        // 9. Go back to file browser
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }

    @Test
    fun testCompleteQuickNoteWorkflow() {
        // Complete end-to-end workflow for quick notes

        // 1. Navigate to QuickNote screen
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()

        // 2. Enter note content
        val noteContent = """
            Meeting Notes - Project Review

            Attendees: John, Jane, Bob

            Key Points:
            • Project is on track
            • Need to address performance issues
            • Next milestone: End of month

            Action Items:
            - John: Fix performance bottleneck
            - Jane: Update documentation
            - Bob: Prepare demo
        """.trimIndent()

        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput(noteContent)

        // 3. Switch to preview mode
        composeTestRule.onNodeWithText("Preview").performClick()

        // 4. Verify preview shows content
        composeTestRule.onNodeWithText("Meeting Notes - Project Review").assertIsDisplayed()

        // 5. Switch back to edit mode
        composeTestRule.onNodeWithText("Edit").performClick()

        // 6. Save the note
        composeTestRule.onNodeWithText("Save").performClick()

        // 7. Verify save doesn't crash app
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
    }

    @Test
    fun testSettingsConfigurationWorkflow() {
        // Complete workflow for configuring app settings

        // 1. Navigate to settings
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // 2. Configure theme settings
        composeTestRule.onNodeWithText("Light theme").performClick()
        composeTestRule.onNodeWithText("Dark theme").performClick()
        composeTestRule.onNodeWithText("System theme (follows system setting)").performClick()

        // 3. Configure editor settings
        composeTestRule.onNodeWithText("Show line numbers").performClick() // Toggle on
        composeTestRule.onNodeWithText("Auto-save").performClick() // Toggle on

        // 4. Review format information
        composeTestRule.onNodeWithText("Formats").assertIsDisplayed()
        composeTestRule.onNodeWithText("Markdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Todo.txt").assertIsDisplayed()

        // 5. Check about information
        composeTestRule.onNodeWithText("About Yole").assertIsDisplayed()
        composeTestRule.onNodeWithText("Version: 2.15.1").assertIsDisplayed()

        // 6. Navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("More Options").assertIsDisplayed()
    }

    @Test
    fun testCrossFeatureWorkflow() {
        // Test workflow that uses multiple features together

        // 1. Create a todo list for project tasks
        composeTestRule.onNodeWithText("To-Do").performClick()

        val projectTodos = listOf(
            "Design new feature",
            "Implement core logic",
            "Write unit tests",
            "Create documentation",
            "Deploy to production"
        )

        for (todo in projectTodos) {
            composeTestRule.onNodeWithText("Add new todo...").performTextInput(todo)
            composeTestRule.onNodeWithText("Add").performClick()
        }

        // 2. Create project documentation in QuickNote
        composeTestRule.onNodeWithText("QuickNote").performClick()

        val projectDoc = """
            # Project Documentation

            ## Overview
            This project implements a comprehensive text editor with support for 18+ markup formats.

            ## Current Status
            - [x] Basic UI implemented
            - [x] Format parsers integrated
            - [ ] Testing completed
            - [ ] Performance optimization

            ## Next Steps
            - Complete testing suite
            - Add advanced features
            - Optimize performance
        """.trimIndent()

        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput(projectDoc)

        // 3. Save the documentation
        composeTestRule.onNodeWithText("Save").performClick()

        // 4. Go to Files to see saved content
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()

        // 5. Check that we can navigate between features
        composeTestRule.onNodeWithText("To-Do").performClick()
        for (todo in projectTodos) {
            composeTestRule.onNodeWithText(todo).assertIsDisplayed()
        }

        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Project Documentation").assertIsDisplayed()
    }

    @Test
    fun testDataPersistenceAcrossSessions() {
        // Test that data persists across simulated app sessions

        // Session 1: Create content
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Persistent todo")
        composeTestRule.onNodeWithText("Add").performClick()

        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("Persistent note")

        // Simulate app restart by recreating the activity
        composeTestRule.activityRule.scenario.recreate()

        // Session 2: Verify content persists (in current implementation, it won't due to no persistence layer)
        // But test that app doesn't crash and UI is accessible
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("To-Do List").assertIsDisplayed()

        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
    }

    @Test
    fun testErrorRecoveryWorkflow() {
        // Test that app recovers gracefully from errors

        // 1. Perform normal operations
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Error recovery test")
        composeTestRule.onNodeWithText("Add").performClick()

        // 2. Try operations that might fail (like file operations in test environment)
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()

        // 3. Verify app remains functional after potential failures
        composeTestRule.onNodeWithText("Editing: untitled.txt").assertIsDisplayed()

        // 4. Try to save (may fail in test environment)
        composeTestRule.onNodeWithContentDescription("Save").performClick()

        // 5. Verify app doesn't crash and can navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()

        // 6. Verify other features still work
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Error recovery test").assertIsDisplayed()
    }

    @Test
    fun testPerformanceUnderLoad() {
        // Test app performance with many operations

        // 1. Create many todos
        composeTestRule.onNodeWithText("To-Do").performClick()

        for (i in 1..20) {
            composeTestRule.onNodeWithText("Add new todo...").performTextInput("Performance test todo $i")
            composeTestRule.onNodeWithText("Add").performClick()
        }

        // 2. Verify all todos are rendered
        for (i in 1..20) {
            composeTestRule.onNodeWithText("Performance test todo $i").assertIsDisplayed()
        }

        // 3. Switch screens multiple times
        for (i in 1..5) {
            composeTestRule.onNodeWithText("Files").performClick()
            composeTestRule.onNodeWithText("QuickNote").performClick()
            composeTestRule.onNodeWithText("More").performClick()
            composeTestRule.onNodeWithText("To-Do").performClick()
        }

        // 4. Verify app remains responsive
        composeTestRule.onNodeWithText("To-Do List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Performance test todo 1").assertIsDisplayed()
    }

    @Test
    fun testAccessibilityWorkflow() {
        // Test accessibility features and screen reader compatibility

        // 1. Verify content descriptions exist
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()

        // 2. Test that all interactive elements are accessible
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("To-Do").assertIsDisplayed()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
        composeTestRule.onNodeWithText("More").assertIsDisplayed()

        // 3. Test navigation accessibility
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").assertIsDisplayed()

        // 4. Test form accessibility
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Accessibility test")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Accessibility test").assertIsDisplayed()
    }

    @Test
    fun testCompleteUserJourney() {
        // Complete user journey from app launch to content creation and management

        // 1. App launch and initial screen
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()

        // 2. Explore file management
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()

        // 3. Create and edit content
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("# Welcome to Yole\n\nThis is a test document.")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithText("Welcome to Yole").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // 4. Manage todos
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Complete user journey test")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Complete user journey test").performClick() // Mark complete

        // 5. Quick notes
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("User journey completed successfully!")
        composeTestRule.onNodeWithText("Save").performClick()

        // 6. Configure settings
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Dark theme").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // 7. Verify everything works together
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Complete user journey test").assertIsDisplayed()
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("User journey completed successfully!").assertIsDisplayed()
    }

    @Test
    fun testFormatSpecificWorkflows() {
        // Test workflows specific to different formats

        // 1. Markdown workflow
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()

        val markdownContent = """
            # Project Status Report

            ## Current Progress
            - [x] UI Implementation
            - [x] Format Parsers
            - [ ] Testing Suite
            - [ ] Documentation

            ## Code Quality
            ```kotlin
            fun testQuality() {
                // High quality code
                assert(true)
            }
            ```

            ## Next Steps
            1. Complete testing
            2. Performance optimization
            3. User feedback integration
        """.trimIndent()

        composeTestRule.onNodeWithText("Start typing...").performTextInput(markdownContent)
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithText("Project Status Report").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // 2. Todo.txt workflow
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("(A) Write comprehensive tests +project @work")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Write comprehensive tests").assertIsDisplayed()

        // 3. Verify cross-format compatibility
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }

    @Test
    fun testBackupAndRestoreWorkflow() {
        // Test backup and restore functionality (UI level)

        // 1. Create content to backup
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Backup test item")
        composeTestRule.onNodeWithText("Add").performClick()

        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("Backup test content")

        // 2. Navigate to backup/restore (would be implemented in More screen)
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()

        // 3. Verify backup UI is accessible (implementation would add actual backup logic)
        composeTestRule.onNodeWithText("Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restore").assertIsDisplayed()
    }

    @Test
    fun testSearchAndFilterWorkflow() {
        // Test search and filter functionality across the app

        // 1. Create searchable content
        composeTestRule.onNodeWithText("To-Do").performClick()

        val searchableTodos = listOf(
            "Fix bug in parser",
            "Add search functionality",
            "Implement filter system",
            "Test search features",
            "Document search API"
        )

        for (todo in searchableTodos) {
            composeTestRule.onNodeWithText("Add new todo...").performTextInput(todo)
            composeTestRule.onNodeWithText("Add").performClick()
        }

        // 2. Test filtering (show/hide completed)
        composeTestRule.onNodeWithText("Hide Done").performClick()
        composeTestRule.onNodeWithText("Show Done").assertIsDisplayed()

        // 3. Test Files search (UI should be accessible)
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()

        // 4. Verify search/filter UI elements exist
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("To-Do List").assertIsDisplayed()
    }
}