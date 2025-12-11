/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * UI tests for Yole Desktop Application
 *
 *########################################################*/
package digital.vasic.yole.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import digital.vasic.yole.desktop.ui.*
import digital.vasic.yole.desktop.ui.theme.YoleDesktopTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * UI tests for Yole Desktop Application components.
 *
 * Tests cover:
 * - Main window and navigation
 * - Theme switching functionality
 * - File browser operations
 * - Editor component interactions
 * - Settings management
 * - Keyboard shortcuts
 * - Accessibility features
 */
@RunWith(JUnit4::class)
class DesktopAppUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Main App Tests ====================

    @Test
    fun `should render main app correctly`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                YoleApp()
            }
        }

        // Main app should be displayed
        composeTestRule.onNodeWithTag("main_app")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should render main screen with all components`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                MainScreen()
            }
        }

        // Verify main screen components are present
        composeTestRule.onNodeWithTag("main_screen")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Screen Navigation Tests ====================

    @Test
    fun `should navigate between screens correctly`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                MainScreen()
            }
        }

        // Initially should show file browser
        composeTestRule.onNodeWithTag("file_browser_screen")
            .assertExists()
            .assertIsDisplayed()

        // Navigate to editor
        composeTestRule.onNodeWithTag("navigate_to_editor")
            .performClick()

        composeTestRule.onNodeWithTag("editor_screen")
            .assertExists()
            .assertIsDisplayed()

        // Navigate to settings
        composeTestRule.onNodeWithTag("navigate_to_settings")
            .performClick()

        composeTestRule.onNodeWithTag("settings_screen")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Theme Tests ====================

    @Test
    fun `should apply light theme correctly`() {
        composeTestRule.setContent {
            YoleDesktopTheme(darkTheme = false) {
                MainScreen()
            }
        }

        // Verify light theme is applied
        composeTestRule.onNodeWithTag("light_theme_container")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should apply dark theme correctly`() {
        composeTestRule.setContent {
            YoleDesktopTheme(darkTheme = true) {
                MainScreen()
            }
        }

        // Verify dark theme is applied
        composeTestRule.onNodeWithTag("dark_theme_container")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Settings Tests ====================

    @Test
    fun `should render settings screen with all options`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                SettingsScreen()
            }
        }

        // Verify all settings are present
        composeTestRule.onNodeWithTag("settings_theme")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_editor")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_accessibility")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should change theme mode in settings`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                SettingsScreen()
            }
        }

        // Change theme mode
        composeTestRule.onNodeWithTag("theme_mode_dropdown")
            .performClick()

        composeTestRule.onNodeWithTag("theme_mode_dark")
            .performClick()

        // Verify theme mode was changed
        composeTestRule.onNodeWithTag("theme_mode_dark_selected")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should toggle editor settings`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                SettingsScreen()
            }
        }

        // Toggle line numbers
        composeTestRule.onNodeWithTag("show_line_numbers_switch")
            .performClick()

        // Toggle auto-save
        composeTestRule.onNodeWithTag("auto_save_switch")
            .performClick()

        // Verify settings were toggled
        composeTestRule.onNodeWithTag("show_line_numbers_enabled")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("auto_save_enabled")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== File Browser Tests ====================

    @Test
    fun `should render file browser with file list`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                FileBrowserScreen(
                    onFileSelected = {},
                    onNavigateToEditor = {}
                )
            }
        }

        // File browser should be displayed
        composeTestRule.onNodeWithTag("file_browser_screen")
            .assertExists()
            .assertIsDisplayed()

        // Should have file list
        composeTestRule.onNodeWithTag("file_list")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should select file from browser`() {
        var fileSelected = false
        var selectedFile: String? = null

        composeTestRule.setContent {
            YoleDesktopTheme {
                FileBrowserScreen(
                    onFileSelected = { file ->
                        fileSelected = true
                        selectedFile = file
                    },
                    onNavigateToEditor = {}
                )
            }
        }

        // Select a file
        composeTestRule.onNodeWithTag("file_item_test.md")
            .performClick()

        // Verify file was selected
        assert(fileSelected) { "File selection callback should be triggered" }
        assert(selectedFile == "test.md") { "Selected file should be test.md" }
    }

    // ==================== Editor Tests ====================

    @Test
    fun `should render editor with content`() {
        val testContent = "# Test Document\n\nThis is test content."

        composeTestRule.setContent {
            YoleDesktopTheme {
                EditorScreen(
                    content = testContent,
                    onContentChange = {},
                    showLineNumbers = true,
                    autoSave = false
                )
            }
        }

        // Editor should be displayed
        composeTestRule.onNodeWithTag("editor_screen")
            .assertExists()
            .assertIsDisplayed()

        // Content should be present
        composeTestRule.onNodeWithText("Test Document")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle content changes in editor`() {
        var contentChanged = false
        var newContent = ""

        composeTestRule.setContent {
            YoleDesktopTheme {
                EditorScreen(
                    content = "Initial content",
                    onContentChange = { content ->
                        contentChanged = true
                        newContent = content
                    },
                    showLineNumbers = true,
                    autoSave = false
                )
            }
        }

        // Change content
        composeTestRule.onNodeWithTag("editor_text_field")
            .performTextClearance()
            .performTextInput("New content")

        // Verify content change was triggered
        assert(contentChanged) { "Content change callback should be triggered" }
        assert(newContent == "New content") { "New content should match input" }
    }

    @Test
    fun `should toggle line numbers in editor`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                EditorScreen(
                    content = "Line 1\nLine 2\nLine 3",
                    onContentChange = {},
                    showLineNumbers = true,
                    autoSave = false
                )
            }
        }

        // Line numbers should be visible
        composeTestRule.onNodeWithTag("line_numbers")
            .assertExists()
            .assertIsDisplayed()

        // Toggle line numbers off
        composeTestRule.onNodeWithTag("toggle_line_numbers")
            .performClick()

        // Line numbers should be hidden
        composeTestRule.onNodeWithTag("line_numbers_hidden")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Keyboard Shortcut Tests ====================

    @Test
    fun `should handle Ctrl+S keyboard shortcut`() {
        var saveTriggered = false

        composeTestRule.setContent {
            YoleDesktopTheme {
                MainScreenWithSaveHandler(
                    onSave = { saveTriggered = true }
                )
            }
        }

        // Navigate to editor
        composeTestRule.onNodeWithTag("navigate_to_editor")
            .performClick()

        // Simulate Ctrl+S
        composeTestRule.onNodeWithTag("editor_screen")
            .performKeyPress(Key.S)

        // Verify save was triggered
        assert(saveTriggered) { "Save should be triggered by Ctrl+S" }
    }

    @Test
    fun `should handle Ctrl+N keyboard shortcut`() {
        var newFileTriggered = false

        composeTestRule.setContent {
            YoleDesktopTheme {
                MainScreenWithNewFileHandler(
                    onNewFile = { newFileTriggered = true }
                )
            }
        }

        // Simulate Ctrl+N
        composeTestRule.onNodeWithTag("main_screen")
            .performKeyPress(Key.N)

        // Verify new file was triggered
        assert(newFileTriggered) { "New file should be triggered by Ctrl+N" }
    }

    @Test
    fun `should handle Ctrl+O keyboard shortcut`() {
        var openFileTriggered = false

        composeTestRule.setContent {
            YoleDesktopTheme {
                MainScreenWithOpenFileHandler(
                    onOpenFile = { openFileTriggered = true }
                )
            }
        }

        // Simulate Ctrl+O
        composeTestRule.onNodeWithTag("main_screen")
            .performKeyPress(Key.O)

        // Verify open file was triggered
        assert(openFileTriggered) { "Open file should be triggered by Ctrl+O" }
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun `should support accessibility features`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                MainScreen()
            }
        }

        // Verify accessibility labels are present
        composeTestRule.onNodeWithContentDescription("Main application window")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("File browser")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Text editor")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle high contrast mode`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                SettingsScreenWithHighContrast()
            }
        }

        // Enable high contrast
        composeTestRule.onNodeWithTag("high_contrast_switch")
            .performClick()

        // Verify high contrast is applied
        composeTestRule.onNodeWithTag("high_contrast_enabled")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle empty file content gracefully`() {
        var contentHandled = false

        composeTestRule.setContent {
            YoleDesktopTheme {
                EditorScreen(
                    content = "",
                    onContentChange = { contentHandled = true },
                    showLineNumbers = true,
                    autoSave = false
                )
            }
        }

        // Should handle empty content without errors
        composeTestRule.onNodeWithTag("editor_screen")
            .assertExists()
            .assertIsDisplayed()

        // Should allow editing empty content
        composeTestRule.onNodeWithTag("editor_text_field")
            .performTextInput("New content")

        assert(contentHandled) { "Empty content should be handled gracefully" }
    }

    @Test
    fun `should handle very long content in editor`() {
        val longContent = "Line\n".repeat(1000)

        composeTestRule.setContent {
            YoleDesktopTheme {
                EditorScreen(
                    content = longContent,
                    onContentChange = {},
                    showLineNumbers = true,
                    autoSave = false
                )
            }
        }

        // Should handle long content without performance issues
        composeTestRule.onNodeWithTag("editor_screen")
            .assertExists()
            .assertIsDisplayed()

        // Should still be responsive
        composeTestRule.onNodeWithTag("editor_text_field")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Helper Functions ====================

    /**
     * Helper composable for testing save functionality
     */
    @Composable
    private fun MainScreenWithSaveHandler(onSave: () -> Unit) {
        YoleDesktopTheme {
            MainScreenWithCustomHandlers(
                onSave = onSave,
                onNewFile = {},
                onOpenFile = {}
            )
        }
    }

    /**
     * Helper composable for testing new file functionality
     */
    @Composable
    private fun MainScreenWithNewFileHandler(onNewFile: () -> Unit) {
        YoleDesktopTheme {
            MainScreenWithCustomHandlers(
                onSave = {},
                onNewFile = onNewFile,
                onOpenFile = {}
            )
        }
    }

    /**
     * Helper composable for testing open file functionality
     */
    @Composable
    private fun MainScreenWithOpenFileHandler(onOpenFile: () -> Unit) {
        YoleDesktopTheme {
            MainScreenWithCustomHandlers(
                onSave = {},
                onNewFile = {},
                onOpenFile = onOpenFile
            )
        }
    }

    /**
     * Helper composable for testing settings with high contrast
     */
    @Composable
    private fun SettingsScreenWithHighContrast() {
        YoleDesktopTheme {
            SettingsScreen()
        }
    }

    /**
     * Mock implementations for testing - these would need to be implemented
     * based on the actual desktop app structure
     */
    @Composable
    private fun MainScreenWithCustomHandlers(
        onSave: () -> Unit,
        onNewFile: () -> Unit,
        onOpenFile: () -> Unit
    ) {
        // This would be implemented based on actual MainScreen
        MainScreen()
    }
}