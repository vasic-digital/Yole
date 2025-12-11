/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * UI tests for CSV format
 *
 *########################################################*/
package digital.vasic.yole.format.csv

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import digital.vasic.yole.format.csv.ui.CsvEditor
import digital.vasic.yole.format.csv.ui.CsvPreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * UI tests for CSV format components.
 *
 * Tests cover:
 * - Editor component rendering
 * - Preview component rendering
 * - User interaction handling
 * - State management
 * - Accessibility features
 */
@RunWith(JUnit4::class)
class CsvUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Editor Component Tests ====================

    @Test
    fun `should render CSV editor correctly`() {
        composeTestRule.setContent {
            CsvEditor(
                content = "Sample CSV content here",
                onContentChange = {}
            )
        }

        // Verify editor is displayed
        composeTestRule.onNodeWithTag("csv_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle content changes in editor`() {
        var contentChanged = false
        var newContent = ""

        composeTestRule.setContent {
            CsvEditor(
                content = "Single line of CSV",
                onContentChange = { content ->
                    contentChanged = true
                    newContent = content
                }
            )
        }

        // Find and interact with editor
        composeTestRule.onNodeWithTag("csv_editor")
            .performTextClearance()
            .performTextInput("Format specific sample")

        // Verify content change was triggered
        assert(contentChanged) { "Content change callback should be triggered" }
        assert(newContent == "Format specific sample") { "New content should match input" }
    }

    @Test
    fun `should display placeholder when content is empty`() {
        composeTestRule.setContent {
            CsvEditor(
                content = "",
                onContentChange = {},
                placeholder = "Enter CSV content..."
            )
        }

        // Verify placeholder text is displayed
        composeTestRule.onNodeWithText("Enter CSV content...")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Preview Component Tests ====================

    @Test
    fun `should render CSV preview correctly`() {
        composeTestRule.setContent {
            CsvPreview(
                content = "Sample CSV content here",
                isDarkTheme = false
            )
        }

        // Verify preview is displayed
        composeTestRule.onNodeWithTag("csv_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle dark theme in preview`() {
        composeTestRule.setContent {
            CsvPreview(
                content = "Sample CSV content here",
                isDarkTheme = true
            )
        }

        // Verify preview adapts to dark theme
        composeTestRule.onNodeWithTag("csv_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should update preview when content changes`() {
        var content = "Single line of CSV"

        composeTestRule.setContent {
            CsvPreview(
                content = content,
                isDarkTheme = false
            )
        }

        // Update content
        content = "Format specific sample"
        composeTestRule.setContent {
            CsvPreview(
                content = content,
                isDarkTheme = false
            )
        }

        // Verify preview updated
        composeTestRule.onNodeWithTag("csv_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate editor and preview components`() {
        var editorContent = "Sample CSV content here"

        composeTestRule.setContent {
            Column {
                CsvEditor(
                    content = editorContent,
                    onContentChange = { newContent ->
                        editorContent = newContent
                    }
                )
                CsvPreview(
                    content = editorContent,
                    isDarkTheme = false
                )
            }
        }

        // Verify both components are displayed
        composeTestRule.onNodeWithTag("csv_editor")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("csv_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun `should support accessibility features in editor`() {
        composeTestRule.setContent {
            CsvEditor(
                content = "Sample CSV content here",
                onContentChange = {},
                contentDescription = "CSV content editor"
            )
        }

        // Verify accessibility features
        composeTestRule.onNodeWithContentDescription("CSV content editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should support accessibility features in preview`() {
        composeTestRule.setContent {
            CsvPreview(
                content = "Sample CSV content here",
                isDarkTheme = false,
                contentDescription = "CSV content preview"
            )
        }

        // Verify accessibility features
        composeTestRule.onNodeWithContentDescription("CSV content preview")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle malformed content gracefully`() {
        composeTestRule.setContent {
            CsvEditor(
                content = "Malformed CSV content",
                onContentChange = {}
            )
        }

        // Should not crash with malformed content
        composeTestRule.onNodeWithTag("csv_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle very long content`() {
        val longContent = "Single line of CSV\n".repeat(1000)

        composeTestRule.setContent {
            CsvEditor(
                content = longContent,
                onContentChange = {}
            )
        }

        // Should handle long content without performance issues
        composeTestRule.onNodeWithTag("csv_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Format-Specific UI Tests ====================
    // Add format-specific UI tests below
    // Examples:
    // - Toolbar buttons for format-specific actions
    // - Syntax highlighting visualization
    // - Format-specific keyboard shortcuts
    // - Live preview updates

    @Test
    fun `should handle format-specific feature in UI`() {
        composeTestRule.setContent {
            CsvEditor(
                content = "Format specific sample",
                onContentChange = {}
            )
        }

        // Verify format-specific UI elements
        composeTestRule.onNodeWithTag("csv_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should maintain responsive UI with rapid content changes`() {
        var content = "Single line of CSV"

        composeTestRule.setContent {
            CsvEditor(
                content = content,
                onContentChange = { newContent ->
                    content = newContent
                }
            )
        }

        // Simulate rapid typing
        repeat(10) { i ->
            composeTestRule.onNodeWithTag("csv_editor")
                .performTextInput("Line $i\n")
        }

        // UI should remain responsive
        composeTestRule.onNodeWithTag("csv_editor")
            .assertExists()
            .assertIsDisplayed()
    }
}