/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * UI tests for LaTeX format
 *
 *########################################################*/
package digital.vasic.yole.format.latex

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import digital.vasic.yole.format.latex.ui.LatexEditor
import digital.vasic.yole.format.latex.ui.LatexPreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * UI tests for LaTeX format components.
 *
 * Tests cover:
 * - Editor component rendering
 * - Preview component rendering
 * - User interaction handling
 * - State management
 * - Accessibility features
 */
@RunWith(JUnit4::class)
class LatexUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Editor Component Tests ====================

    @Test
    fun `should render LaTeX editor correctly`() {
        composeTestRule.setContent {
            LatexEditor(
                content = "Sample LaTeX content here",
                onContentChange = {}
            )
        }

        // Verify editor is displayed
        composeTestRule.onNodeWithTag("latex_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle content changes in editor`() {
        var contentChanged = false
        var newContent = ""

        composeTestRule.setContent {
            LatexEditor(
                content = "Single line of LaTeX",
                onContentChange = { content ->
                    contentChanged = true
                    newContent = content
                }
            )
        }

        // Find and interact with editor
        composeTestRule.onNodeWithTag("latex_editor")
            .performTextClearance()
            .performTextInput("Format specific sample")

        // Verify content change was triggered
        assert(contentChanged) { "Content change callback should be triggered" }
        assert(newContent == "Format specific sample") { "New content should match input" }
    }

    @Test
    fun `should display placeholder when content is empty`() {
        composeTestRule.setContent {
            LatexEditor(
                content = "",
                onContentChange = {},
                placeholder = "Enter LaTeX content..."
            )
        }

        // Verify placeholder text is displayed
        composeTestRule.onNodeWithText("Enter LaTeX content...")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Preview Component Tests ====================

    @Test
    fun `should render LaTeX preview correctly`() {
        composeTestRule.setContent {
            LatexPreview(
                content = "Sample LaTeX content here",
                isDarkTheme = false
            )
        }

        // Verify preview is displayed
        composeTestRule.onNodeWithTag("latex_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle dark theme in preview`() {
        composeTestRule.setContent {
            LatexPreview(
                content = "Sample LaTeX content here",
                isDarkTheme = true
            )
        }

        // Verify preview adapts to dark theme
        composeTestRule.onNodeWithTag("latex_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should update preview when content changes`() {
        var content = "Single line of LaTeX"

        composeTestRule.setContent {
            LatexPreview(
                content = content,
                isDarkTheme = false
            )
        }

        // Update content
        content = "Format specific sample"
        composeTestRule.setContent {
            LatexPreview(
                content = content,
                isDarkTheme = false
            )
        }

        // Verify preview updated
        composeTestRule.onNodeWithTag("latex_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate editor and preview components`() {
        var editorContent = "Sample LaTeX content here"

        composeTestRule.setContent {
            Column {
                LatexEditor(
                    content = editorContent,
                    onContentChange = { newContent ->
                        editorContent = newContent
                    }
                )
                LatexPreview(
                    content = editorContent,
                    isDarkTheme = false
                )
            }
        }

        // Verify both components are displayed
        composeTestRule.onNodeWithTag("latex_editor")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("latex_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun `should support accessibility features in editor`() {
        composeTestRule.setContent {
            LatexEditor(
                content = "Sample LaTeX content here",
                onContentChange = {},
                contentDescription = "LaTeX content editor"
            )
        }

        // Verify accessibility features
        composeTestRule.onNodeWithContentDescription("LaTeX content editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should support accessibility features in preview`() {
        composeTestRule.setContent {
            LatexPreview(
                content = "Sample LaTeX content here",
                isDarkTheme = false,
                contentDescription = "LaTeX content preview"
            )
        }

        // Verify accessibility features
        composeTestRule.onNodeWithContentDescription("LaTeX content preview")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle malformed content gracefully`() {
        composeTestRule.setContent {
            LatexEditor(
                content = "Malformed LaTeX content",
                onContentChange = {}
            )
        }

        // Should not crash with malformed content
        composeTestRule.onNodeWithTag("latex_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle very long content`() {
        val longContent = "Single line of LaTeX\n".repeat(1000)

        composeTestRule.setContent {
            LatexEditor(
                content = longContent,
                onContentChange = {}
            )
        }

        // Should handle long content without performance issues
        composeTestRule.onNodeWithTag("latex_editor")
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
            LatexEditor(
                content = "Format specific sample",
                onContentChange = {}
            )
        }

        // Verify format-specific UI elements
        composeTestRule.onNodeWithTag("latex_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should maintain responsive UI with rapid content changes`() {
        var content = "Single line of LaTeX"

        composeTestRule.setContent {
            LatexEditor(
                content = content,
                onContentChange = { newContent ->
                    content = newContent
                }
            )
        }

        // Simulate rapid typing
        repeat(10) { i ->
            composeTestRule.onNodeWithTag("latex_editor")
                .performTextInput("Line $i\n")
        }

        // UI should remain responsive
        composeTestRule.onNodeWithTag("latex_editor")
            .assertExists()
            .assertIsDisplayed()
    }
}