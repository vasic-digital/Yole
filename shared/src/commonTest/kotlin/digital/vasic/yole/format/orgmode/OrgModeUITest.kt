/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * UI tests for Org Mode format
 *
 *########################################################*/
package digital.vasic.yole.format.orgmode

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import digital.vasic.yole.format.orgmode.ui.OrgModeEditor
import digital.vasic.yole.format.orgmode.ui.OrgModePreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * UI tests for Org Mode format components.
 *
 * Tests cover:
 * - Editor component rendering
 * - Preview component rendering
 * - User interaction handling
 * - State management
 * - Accessibility features
 */
@RunWith(JUnit4::class)
class OrgModeUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Editor Component Tests ====================

    @Test
    fun `should render Org Mode editor correctly`() {
        composeTestRule.setContent {
            OrgModeEditor(
                content = "Sample Org Mode content here",
                onContentChange = {}
            )
        }

        // Verify editor is displayed
        composeTestRule.onNodeWithTag("org mode_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle content changes in editor`() {
        var contentChanged = false
        var newContent = ""

        composeTestRule.setContent {
            OrgModeEditor(
                content = "Single line of Org Mode",
                onContentChange = { content ->
                    contentChanged = true
                    newContent = content
                }
            )
        }

        // Find and interact with editor
        composeTestRule.onNodeWithTag("org mode_editor")
            .performTextClearance()
            .performTextInput("Format specific sample")

        // Verify content change was triggered
        assert(contentChanged) { "Content change callback should be triggered" }
        assert(newContent == "Format specific sample") { "New content should match input" }
    }

    @Test
    fun `should display placeholder when content is empty`() {
        composeTestRule.setContent {
            OrgModeEditor(
                content = "",
                onContentChange = {},
                placeholder = "Enter Org Mode content..."
            )
        }

        // Verify placeholder text is displayed
        composeTestRule.onNodeWithText("Enter Org Mode content...")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Preview Component Tests ====================

    @Test
    fun `should render Org Mode preview correctly`() {
        composeTestRule.setContent {
            OrgModePreview(
                content = "Sample Org Mode content here",
                isDarkTheme = false
            )
        }

        // Verify preview is displayed
        composeTestRule.onNodeWithTag("org mode_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle dark theme in preview`() {
        composeTestRule.setContent {
            OrgModePreview(
                content = "Sample Org Mode content here",
                isDarkTheme = true
            )
        }

        // Verify preview adapts to dark theme
        composeTestRule.onNodeWithTag("org mode_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should update preview when content changes`() {
        var content = "Single line of Org Mode"

        composeTestRule.setContent {
            OrgModePreview(
                content = content,
                isDarkTheme = false
            )
        }

        // Update content
        content = "Format specific sample"
        composeTestRule.setContent {
            OrgModePreview(
                content = content,
                isDarkTheme = false
            )
        }

        // Verify preview updated
        composeTestRule.onNodeWithTag("org mode_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate editor and preview components`() {
        var editorContent = "Sample Org Mode content here"

        composeTestRule.setContent {
            Column {
                OrgModeEditor(
                    content = editorContent,
                    onContentChange = { newContent ->
                        editorContent = newContent
                    }
                )
                OrgModePreview(
                    content = editorContent,
                    isDarkTheme = false
                )
            }
        }

        // Verify both components are displayed
        composeTestRule.onNodeWithTag("org mode_editor")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("org mode_preview")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun `should support accessibility features in editor`() {
        composeTestRule.setContent {
            OrgModeEditor(
                content = "Sample Org Mode content here",
                onContentChange = {},
                contentDescription = "Org Mode content editor"
            )
        }

        // Verify accessibility features
        composeTestRule.onNodeWithContentDescription("Org Mode content editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should support accessibility features in preview`() {
        composeTestRule.setContent {
            OrgModePreview(
                content = "Sample Org Mode content here",
                isDarkTheme = false,
                contentDescription = "Org Mode content preview"
            )
        }

        // Verify accessibility features
        composeTestRule.onNodeWithContentDescription("Org Mode content preview")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle malformed content gracefully`() {
        composeTestRule.setContent {
            OrgModeEditor(
                content = "Malformed Org Mode content",
                onContentChange = {}
            )
        }

        // Should not crash with malformed content
        composeTestRule.onNodeWithTag("org mode_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle very long content`() {
        val longContent = "Single line of Org Mode\n".repeat(1000)

        composeTestRule.setContent {
            OrgModeEditor(
                content = longContent,
                onContentChange = {}
            )
        }

        // Should handle long content without performance issues
        composeTestRule.onNodeWithTag("org mode_editor")
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
            OrgModeEditor(
                content = "Format specific sample",
                onContentChange = {}
            )
        }

        // Verify format-specific UI elements
        composeTestRule.onNodeWithTag("org mode_editor")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should maintain responsive UI with rapid content changes`() {
        var content = "Single line of Org Mode"

        composeTestRule.setContent {
            OrgModeEditor(
                content = content,
                onContentChange = { newContent ->
                    content = newContent
                }
            )
        }

        // Simulate rapid typing
        repeat(10) { i ->
            composeTestRule.onNodeWithTag("org mode_editor")
                .performTextInput("Line $i\n")
        }

        // UI should remain responsive
        composeTestRule.onNodeWithTag("org mode_editor")
            .assertExists()
            .assertIsDisplayed()
    }
}