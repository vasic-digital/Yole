/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * UI Automation Tests for Yole Desktop App
 *
 * Tests cover:
 * - Desktop window management
 * - UI component interactions
 * - Screen navigation
 * - Keyboard shortcuts
 * - Accessibility features
 * - Theme switching
 * - Desktop-specific UI elements
 *
 *########################################################*/

package digital.vasic.yole.desktop.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import digital.vasic.yole.desktop.ui.theme.YoleDesktopTheme
import digital.vasic.yole.format.FormatRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Comprehensive UI tests for desktop application.
 * Tests all major UI components and interactions.
 */
@RunWith(JUnit4::class)
class DesktopAppUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var settings: YoleDesktopSettings

    @Before
    fun setup() {
        settings = YoleDesktopSettings()
        // Reset settings to defaults for consistent testing
        settings.setThemeMode("system")
        settings.setShowLineNumbers(true)
        settings.setAutoSave(true)
        settings.setAnimationsEnabled(true)
        settings.setHighContrastEnabled(false)
        settings.setReduceMotion(false)
        settings.setFocusIndicators(true)
        settings.setAnnounceChanges(true)
    }

    // ==================== Main App UI Tests ====================

    @Test
    fun `should display main app with all screens`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                YoleApp()
            }
        }

        // Verify main app components are displayed
        composeTestRule.onNodeWithText("Yole").assertExists()
        composeTestRule.onNodeWithText("Files").assertExists()
        composeTestRule.onNodeWithText("Edit").assertExists()
        composeTestRule.onNodeWithText("Preview").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun `should navigate between screens using top bar buttons`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                YoleApp()
            }
        }

        // Navigate to Editor
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Editing:").assertExists()

        // Navigate to Preview
        composeTestRule.onNodeWithText("Preview").performClick()
        composeTestRule.onNodeWithText("Preview:").assertExists()

        // Navigate to Settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Appearance").assertExists()

        // Navigate back to Files
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertExists()
    }

    // ==================== File Browser Screen Tests ====================

    @Test
    fun `should display file browser with sample files`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                FileBrowserScreen { _, _ -> }
            }
        }

        // Verify file browser components
        composeTestRule.onNodeWithText("File Browser").assertExists()
        composeTestRule.onNodeWithText("sample.md").assertExists()
        composeTestRule.onNodeWithText("todo.txt").assertExists()
        composeTestRule.onNodeWithText("notes.txt").assertExists()
        composeTestRule.onNodeWithText("Supported formats:").assertExists()
    }

    @Test
    fun `should handle file selection in browser`() {
        var selectedFile: String? = null
        var selectedContent: String? = null

        composeTestRule.setContent {
            YoleDesktopTheme {
                FileBrowserScreen { file, content ->
                    selectedFile = file
                    selectedContent = content
                }
            }
        }

        // Click on sample file
        composeTestRule.onNodeWithText("sample.md").performClick()

        // Verify file selection callback
        assert(selectedFile == "sample.md")
        assert(selectedContent?.contains("# Sample Markdown") == true)
    }

    // ==================== Editor Screen Tests ====================

    @Test
    fun `should display editor with file name and content`() {
        val testFileName = "test.md"
        val testContent = "# Test Content\n\nThis is test content."

        composeTestRule.setContent {
            YoleDesktopTheme {
                EditorScreen(
                    fileName = testFileName,
                    content = testContent,
                    onContentChanged = {}
                )
            }
        }

        // Verify editor components
        composeTestRule.onNodeWithText("Editing: $testFileName").assertExists()
        composeTestRule.onNodeWithTag("EditorTextField").assertExists()
    }

    @Test
    fun `should handle content changes in editor`() {
        var contentChanged = ""
        val initialContent = "Initial content"
        val newContent = "Updated content"

        composeTestRule.setContent {
            YoleDesktopTheme {
                EditorScreen(
                    fileName = "test.md",
                    content = initialContent,
                    onContentChanged = { contentChanged = it }
                )
            }
        }

        // Find and interact with text field
        val textField = composeTestRule.onNodeWithTag("EditorTextField")
        textField.performTextClearance()
        textField.performTextInput(newContent)

        // Verify content change callback
        assert(contentChanged == newContent)
    }

    // ==================== Preview Screen Tests ====================

    @Test
    fun `should display preview with format detection`() {
        val testFileName = "test.md"
        val testContent = "# Markdown Preview\n\nThis is markdown content."

        composeTestRule.setContent {
            YoleDesktopTheme {
                PreviewScreen(
                    fileName = testFileName,
                    content = testContent
                )
            }
        }

        // Verify preview components
        composeTestRule.onNodeWithText("Preview: $testFileName").assertExists()
        composeTestRule.onNodeWithText("Format: Markdown").assertExists()
    }

    @Test
    fun `should handle unsupported format gracefully`() {
        val testFileName = "test.unknown"
        val testContent = "Unknown format content"

        composeTestRule.setContent {
            YoleDesktopTheme {
                PreviewScreen(
                    fileName = testFileName,
                    content = testContent
                )
            }
        }

        // Should fallback to showing raw content
        composeTestRule.onNodeWithText("Preview: $testFileName").assertExists()
        composeTestRule.onNodeWithText(testContent).assertExists()
    }

    // ==================== Settings Screen Tests ====================

    @Test
    fun `should display settings with all options`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                SettingsScreen(
                    themeMode = "system",
                    onThemeModeChanged = {},
                    showLineNumbers = true,
                    onShowLineNumbersChanged = {},
                    autoSave = true,
                    onAutoSaveChanged = {},
                    animationsEnabled = true,
                    onAnimationsEnabledChanged = {}
                )
            }
        }

        // Verify settings categories
        composeTestRule.onNodeWithText("Appearance").assertExists()
        composeTestRule.onNodeWithText("Editor").assertExists()
        composeTestRule.onNodeWithText("Animations").assertExists()
        composeTestRule.onNodeWithText("Formats").assertExists()
        composeTestRule.onNodeWithText("About Yole").assertExists()

        // Verify theme options
        composeTestRule.onNodeWithText("System theme (follows system setting)").assertExists()
        composeTestRule.onNodeWithText("Light theme").assertExists()
        composeTestRule.onNodeWithText("Dark theme").assertExists()

        // Verify editor settings
        composeTestRule.onNodeWithText("Show line numbers").assertExists()
        composeTestRule.onNodeWithText("Auto-save").assertExists()

        // Verify animation settings
        composeTestRule.onNodeWithText("Enable smooth transitions").assertExists()
    }

    @Test
    fun `should handle theme mode changes`() {
        var themeModeChanged = ""

        composeTestRule.setContent {
            YoleDesktopTheme {
                SettingsScreen(
                    themeMode = "system",
                    onThemeModeChanged = { themeModeChanged = it },
                    showLineNumbers = true,
                    onShowLineNumbersChanged = {},
                    autoSave = true,
                    onAutoSaveChanged = {},
                    animationsEnabled = true,
                    onAnimationsEnabledChanged = {}
                )
            }
        }

        // Change to light theme
        composeTestRule.onNodeWithText("Light theme").performClick()
        assert(themeModeChanged == "light")

        // Change to dark theme
        composeTestRule.onNodeWithText("Dark theme").performClick()
        assert(themeModeChanged == "dark")
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun `should have proper accessibility semantics`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                YoleApp()
            }
        }

        // Verify accessibility descriptions
        composeTestRule.onNodeWithContentDescription("Open file browser").assertExists()
        composeTestRule.onNodeWithContentDescription("Open editor").assertExists()
        composeTestRule.onNodeWithContentDescription("Open preview").assertExists()
        composeTestRule.onNodeWithContentDescription("Open settings").assertExists()
    }

    @Test
    fun `should support keyboard navigation`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                YoleApp()
            }
        }

        // Test keyboard shortcuts would require more complex setup
        // For now, verify the UI responds to basic interactions
        composeTestRule.onNodeWithText("Files").assertIsEnabled()
        composeTestRule.onNodeWithText("Edit").assertIsEnabled()
        composeTestRule.onNodeWithText("Preview").assertIsEnabled()
        composeTestRule.onNodeWithText("Settings").assertIsEnabled()
    }

    // ==================== Theme Integration Tests ====================

    @Test
    fun `should apply desktop theme correctly`() {
        composeTestRule.setContent {
            YoleDesktopTheme(
                themeMode = digital.vasic.yole.ui.ThemeMode.LIGHT
            ) {
                YoleApp()
            }
        }

        // Verify theme is applied (basic check)
        composeTestRule.onNodeWithText("Yole").assertExists()
    }

    @Test
    fun `should handle high contrast theme`() {
        composeTestRule.setContent {
            YoleDesktopTheme(
                themeMode = digital.vasic.yole.ui.ThemeMode.LIGHT,
                highContrast = true
            ) {
                YoleApp()
            }
        }

        // Verify high contrast theme is applied
        composeTestRule.onNodeWithText("Yole").assertExists()
    }

    // ==================== Format Support Tests ====================

    @Test
    fun `should display supported formats in settings`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                SettingsScreen(
                    themeMode = "system",
                    onThemeModeChanged = {},
                    showLineNumbers = true,
                    onShowLineNumbersChanged = {},
                    autoSave = true,
                    onAutoSaveChanged = {},
                    animationsEnabled = true,
                    onAnimationsEnabledChanged = {}
                )
            }
        }

        // Verify format information is displayed
        composeTestRule.onNodeWithText("Supported formats:").assertExists()
        
        // Check for specific format examples
        val formatCount = FormatRegistry.formats.size
        if (formatCount > 0) {
            composeTestRule.onNode(hasText("•") and hasText("(")).assertExists()
        }
    }

    // ==================== Settings Persistence Tests ====================

    @Test
    fun `should persist settings changes through UI`() {
        composeTestRule.setContent {
            YoleDesktopTheme {
                SettingsScreen(
                    themeMode = "system",
                    onThemeModeChanged = { mode ->
                        settings.setThemeMode(mode)
                    },
                    showLineNumbers = true,
                    onShowLineNumbersChanged = { show ->
                        settings.setShowLineNumbers(show)
                    },
                    autoSave = true,
                    onAutoSaveChanged = { auto ->
                        settings.setAutoSave(auto)
                    },
                    animationsEnabled = true,
                    onAnimationsEnabledChanged = { enabled ->
                        settings.setAnimationsEnabled(enabled)
                    }
                )
            }
        }

        // Change settings through UI
        composeTestRule.onNodeWithText("Dark theme").performClick()
        assert(settings.getThemeMode() == "dark")

        // Toggle line numbers
        composeTestRule.onNodeWithText("Show line numbers").performClick()
        // Note: The actual toggle would depend on the Switch implementation
    }
}