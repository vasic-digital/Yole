/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Test Stub Components for Desktop App UI Testing
 *
 *########################################################*/
package digital.vasic.yole.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Stub implementation of MainScreen for testing.
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen")
            .semantics { contentDescription = "Main application window" }
    ) {
        Text("Main Screen", style = MaterialTheme.typography.headlineMedium)
        FileBrowserScreen(
            onFileSelected = {},
            onNavigateToEditor = {}
        )
    }
}

/**
 * Stub implementation of SettingsScreen for testing.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        
        // Theme settings
        Column(modifier = Modifier.testTag("settings_theme")) {
            Text("Theme Settings")
            var themeModeExpanded by remember { mutableStateOf(false) }
            var selectedThemeMode by remember { mutableStateOf("system") }
            
            Box {
                Button(
                    onClick = { themeModeExpanded = true },
                    modifier = Modifier.testTag("theme_mode_dropdown")
                ) {
                    Text("Theme: $selectedThemeMode")
                }
                DropdownMenu(
                    expanded = themeModeExpanded,
                    onDismissRequest = { themeModeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Dark") },
                        onClick = { 
                            selectedThemeMode = "dark"
                            themeModeExpanded = false
                        },
                        modifier = Modifier.testTag("theme_mode_dark")
                    )
                }
            }
        }
        
        // Editor settings
        Column(modifier = Modifier.testTag("settings_editor")) {
            Text("Editor Settings")
            var showLineNumbers by remember { mutableStateOf(true) }
            var autoSave by remember { mutableStateOf(false) }
            
            Row {
                Text("Show Line Numbers")
                Switch(
                    checked = showLineNumbers,
                    onCheckedChange = { showLineNumbers = it },
                    modifier = Modifier.testTag("show_line_numbers_switch")
                )
            }
            Row {
                Text("Auto Save")
                Switch(
                    checked = autoSave,
                    onCheckedChange = { autoSave = it },
                    modifier = Modifier.testTag("auto_save_switch")
                )
            }
        }
        
        // Accessibility settings
        Column(modifier = Modifier.testTag("settings_accessibility")) {
            Text("Accessibility Settings")
            var highContrast by remember { mutableStateOf(false) }
            Switch(
                checked = highContrast,
                onCheckedChange = { highContrast = it },
                modifier = Modifier.testTag("high_contrast_switch")
            )
        }
    }
}

/**
 * Stub implementation of FileBrowserScreen for testing.
 */
@Composable
fun FileBrowserScreen(
    onFileSelected: (String) -> Unit,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("file_browser_screen")
            .semantics { contentDescription = "File browser" }
    ) {
        Text("File Browser", style = MaterialTheme.typography.headlineMedium)
        
        // Mock file list
        Column(modifier = Modifier.testTag("file_list")) {
            TextButton(
                onClick = { onFileSelected("test.md") },
                modifier = Modifier.testTag("file_item_test.md")
            ) {
                Text("test.md")
            }
        }
    }
}

/**
 * Stub implementation of EditorScreen for testing.
 */
@Composable
fun EditorScreen(
    content: String,
    onContentChange: (String) -> Unit,
    showLineNumbers: Boolean,
    autoSave: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("editor_screen")
            .semantics { contentDescription = "Text editor" }
    ) {
        Text("Editor", style = MaterialTheme.typography.headlineMedium)
        
        // Line numbers (when enabled)
        if (showLineNumbers) {
            Text(
                "1\n2\n3",
                modifier = Modifier.testTag("line_numbers")
            )
        }
        
        // Text field
        var text by remember(content) { mutableStateOf(content) }
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                onContentChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("editor_text_field")
        )
        
        // Toggle line numbers button
        TextButton(
            onClick = { },
            modifier = Modifier.testTag("toggle_line_numbers")
        ) {
            Text("Toggle Line Numbers")
        }
    }
}
