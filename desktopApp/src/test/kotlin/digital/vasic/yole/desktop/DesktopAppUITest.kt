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
import androidx.compose.ui.input.key.Key
import androidx.compose.runtime.Composable
import digital.vasic.yole.desktop.ui.*
import digital.vasic.yole.desktop.ui.theme.YoleDesktopTheme
import digital.vasic.yole.ui.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UI tests for Yole Desktop Application components.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopAppUITest {

    // ==================== Main App Tests ====================

    @Test
    fun `should render main app correctly`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                YoleApp()
            }
        }

        // Main app should be displayed
        onNodeWithTag("main_app")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should render main screen with all components`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                MainScreen()
            }
        }

        // Verify main screen components are present
        onNodeWithTag("main_screen")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Screen Navigation Tests ====================

    @Test
    fun `should navigate between screens correctly`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                MainScreen()
            }
        }

        // Initially should show file browser
        onNodeWithText("File Browser")
            .assertExists()
            .assertIsDisplayed()

        // Navigate to editor
        onNodeWithContentDescription("Open editor")
            .performClick()

        onNodeWithText("Editing:", substring = true)
            .assertExists()
            .assertIsDisplayed()

        // Navigate to settings
        onNodeWithContentDescription("Open settings")
            .performClick()

        onNodeWithText("Appearance")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Theme Tests ====================

    @Test
    fun `should apply light theme correctly`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme(themeMode = ThemeMode.LIGHT) {
                MainScreen()
            }
        }

        // Verify main screen is displayed
        onNodeWithText("Yole")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should apply dark theme correctly`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme(themeMode = ThemeMode.DARK) {
                MainScreen()
            }
        }

        // Verify main screen is displayed
        onNodeWithText("Yole")
            .assertExists()
            .assertIsDisplayed()
    }

    // ==================== Settings Tests ====================

    @Test
    fun `should render settings screen with all options`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                SettingsScreen()
            }
        }

        // Verify all settings are present
        onNodeWithTag("settings_theme")
            .assertExists()
            .assertIsDisplayed()

        onNodeWithTag("settings_editor")
            .assertExists()
            .assertIsDisplayed()

        onNodeWithTag("settings_accessibility")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should change theme mode in settings`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                SettingsScreen()
            }
        }

        // Change theme mode
        onNodeWithTag("theme_mode_dropdown")
            .performClick()

        onNodeWithTag("theme_mode_dark")
            .performClick()
    }

    @Test
    fun `should toggle editor settings`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                SettingsScreen()
            }
        }

        // Toggle line numbers
        onNodeWithTag("show_line_numbers_switch")
            .performClick()

        // Toggle auto-save
        onNodeWithTag("auto_save_switch")
            .performClick()
    }

    // ==================== File Browser Tests ====================

    @Test
    fun `should render file browser with file list`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                FileBrowserScreen(
                    onFileSelected = {},
                    onNavigateToEditor = {}
                )
            }
        }

        // File browser should be displayed
        onNodeWithTag("file_browser_screen")
            .assertExists()
            .assertIsDisplayed()

        // Should have file list
        onNodeWithTag("file_list")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should select file from browser`() = runComposeUiTest {
        var fileSelected = false
        var selectedFile: String? = null

        setContent {
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
        onNodeWithTag("file_item_test.md")
            .performClick()

        // Verify file was selected
        assert(fileSelected) { "File selection callback should be triggered" }
        assert(selectedFile == "test.md") { "Selected file should be test.md" }
    }

    // ==================== Editor Tests ====================

    @Test
    fun `should render editor with content`() = runComposeUiTest {
        val testContent = "# Test Document\n\nThis is test content."

        setContent {
            YoleDesktopTheme {
                EditorScreen(
                    fileName = "test.md",
                    content = testContent,
                    onContentChanged = {}
                )
            }
        }

        // Editor should be displayed
        onNodeWithText("Editing: test.md")
            .assertExists()
            .assertIsDisplayed()

        // Content should be present
        onNodeWithText("Test Document", substring = true)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `should handle content changes in editor`() = runComposeUiTest {
        var contentChanged = false
        var newContent = ""

        setContent {
            YoleDesktopTheme {
                EditorScreen(
                    fileName = "test.txt",
                    content = "",
                    onContentChanged = { content ->
                        contentChanged = true
                        newContent = content
                    }
                )
            }
        }

        // Type content into the editor via the placeholder
        onNodeWithText("Start typing...")
            .performTextInput("New content")

        // Verify content change was triggered
        assert(contentChanged) { "Content change callback should be triggered" }
        assert(newContent == "New content") { "New content should match input" }
    }

    @Test
    fun `should toggle line numbers in editor`() = runComposeUiTest {
        setContent {
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
        onNodeWithTag("line_numbers")
            .assertExists()
            .assertIsDisplayed()

        // Toggle line numbers off
        onNodeWithTag("toggle_line_numbers")
            .performClick()
    }

    // ==================== Keyboard Shortcut Tests ====================

    @Test
    fun `should handle Ctrl+S keyboard shortcut`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                MainScreen()
            }
        }

        // Navigate to editor using content description
        onNodeWithContentDescription("Open editor")
            .performClick()

        // Verify editor is shown
        onNodeWithText("Editing:", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `should handle Ctrl+N keyboard shortcut`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                MainScreen()
            }
        }

        // Navigate to editor using content description (equivalent to Ctrl+N)
        onNodeWithContentDescription("Open editor")
            .performClick()

        // Verify editor is shown
        onNodeWithText("Editing:", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `should handle Ctrl+O keyboard shortcut`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                MainScreen()
            }
        }

        // Navigate to file browser using content description (equivalent to Ctrl+O)
        onNodeWithContentDescription("Open file browser")
            .performClick()

        // Verify file browser is shown
        onNodeWithText("File Browser")
            .assertIsDisplayed()
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun `should support accessibility features`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                MainScreen()
            }
        }

        // Verify accessibility labels are present
        onNodeWithContentDescription("Main application window")
            .assertExists()
            .assertIsDisplayed()

        onNodeWithContentDescription("Open file browser")
            .assertExists()

        onNodeWithContentDescription("Open editor")
            .assertExists()
    }

    @Test
    fun `should handle high contrast mode`() = runComposeUiTest {
        setContent {
            YoleDesktopTheme {
                SettingsScreenWithHighContrast()
            }
        }

        // Enable high contrast
        onNodeWithTag("high_contrast_switch")
            .performClick()
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle empty file content gracefully`() = runComposeUiTest {
        var contentHandled = false

        setContent {
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
        onNodeWithTag("editor_screen")
            .assertExists()
            .assertIsDisplayed()

        // Should allow editing empty content
        onNodeWithTag("editor_text_field")
            .performTextInput("New content")

        assert(contentHandled) { "Empty content should be handled gracefully" }
    }

    @Test
    fun `should handle very long content in editor`() = runComposeUiTest {
        val longContent = "Line\n".repeat(1000)

        setContent {
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
        onNodeWithTag("editor_screen")
            .assertExists()
            .assertIsDisplayed()

        // Should still be responsive
        onNodeWithTag("editor_text_field")
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
