/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Dialogs for Yole
 * Common dialog components for the desktop application
 *
 *########################################################*/
package digital.vasic.yole.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import digital.vasic.yole.desktop.storage.DesktopSettingsStorage
import digital.vasic.yole.format.FormatRegistry

/**
 * Find and Replace dialog.
 */
@Composable
fun FindReplaceDialog(
    findText: String,
    replaceText: String,
    onFindTextChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onFind: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onCloseRequest = onClose,
        title = "Find and Replace",
        state = rememberDialogState(
            width = 500.dp,
            height = 300.dp,
            position = WindowPosition(Alignment.Center)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Find and Replace",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                // Find field
                OutlinedTextField(
                    value = findText,
                    onValueChange = onFindTextChange,
                    label = { Text("Find") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Replace field
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = onReplaceTextChange,
                    label = { Text("Replace with") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Options
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = false, // This would come from state
                        onCheckedChange = { /* Toggle case sensitive */ }
                    )
                    Text("Case sensitive")
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Checkbox(
                        checked = false, // This would come from state
                        onCheckedChange = { /* Toggle whole words */ }
                    )
                    Text("Whole words only")
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = onFind) {
                        Text("Find Next")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = onReplace) {
                        Text("Replace")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = onReplaceAll) {
                        Text("Replace All")
                    }
                }
            }
        }
    }
}

/**
 * Settings dialog with comprehensive configuration options.
 */
@Composable
fun SettingsDialog(
    settings: DesktopSettingsStorage.AppSettings,
    onSettingsChanged: (DesktopSettingsStorage.AppSettings) -> Unit,
    onClose: () -> Unit
) {
    var currentSettings by remember { mutableStateOf(settings) }
    var selectedCategory by remember { mutableStateOf("appearance") }
    
    Dialog(
        onCloseRequest = onClose,
        title = "Settings",
        state = rememberDialogState(
            width = 800.dp,
            height = 600.dp,
            position = WindowPosition(Alignment.Center)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Categories sidebar
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsCategoryItem(
                        title = "Appearance",
                        selected = selectedCategory == "appearance",
                        onClick = { selectedCategory = "appearance" }
                    )
                    SettingsCategoryItem(
                        title = "Editor",
                        selected = selectedCategory == "editor",
                        onClick = { selectedCategory = "editor" }
                    )
                    SettingsCategoryItem(
                        title = "File",
                        selected = selectedCategory == "file",
                        onClick = { selectedCategory = "file" }
                    )
                    SettingsCategoryItem(
                        title = "Window",
                        selected = selectedCategory == "window",
                        onClick = { selectedCategory = "window" }
                    )
                    SettingsCategoryItem(
                        title = "Shortcuts",
                        selected = selectedCategory == "shortcuts",
                        onClick = { selectedCategory = "shortcuts" }
                    )
                    SettingsCategoryItem(
                        title = "Advanced",
                        selected = selectedCategory == "advanced",
                        onClick = { selectedCategory = "advanced" }
                    )
                }
                
                // Settings content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedCategory) {
                        "appearance" -> AppearanceSettings(
                            settings = currentSettings.appearance,
                            onSettingsChanged = { newAppearance ->
                                currentSettings = currentSettings.copy(appearance = newAppearance)
                            }
                        )
                        "editor" -> EditorSettings(
                            settings = currentSettings.editor,
                            onSettingsChanged = { newEditor ->
                                currentSettings = currentSettings.copy(editor = newEditor)
                            }
                        )
                        "file" -> FileSettings(
                            settings = currentSettings.file,
                            onSettingsChanged = { newFile ->
                                currentSettings = currentSettings.copy(file = newFile)
                            }
                        )
                        "window" -> WindowSettings(
                            settings = currentSettings.window,
                            onSettingsChanged = { newWindow ->
                                currentSettings = currentSettings.copy(window = newWindow)
                            }
                        )
                        "shortcuts" -> ShortcutsSettings(
                            settings = currentSettings.shortcuts,
                            onSettingsChanged = { newShortcuts ->
                                currentSettings = currentSettings.copy(shortcuts = newShortcuts)
                            }
                        )
                        "advanced" -> AdvancedSettings(
                            settings = currentSettings.advanced,
                            onSettingsChanged = { newAdvanced ->
                                currentSettings = currentSettings.copy(advanced = newAdvanced)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onClose) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                onSettingsChanged(currentSettings)
                                onClose()
                            }
                        ) {
                            Text("Apply")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                onSettingsChanged(currentSettings)
                                onClose()
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Settings category item.
 */
@Composable
private fun SettingsCategoryItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/**
 * Appearance settings section.
 */
@Composable
private fun AppearanceSettings(
    settings: DesktopSettingsStorage.AppearanceSettings,
    onSettingsChanged: (DesktopSettingsStorage.AppearanceSettings) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Appearance Settings",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Theme selection
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = settings.themeMode == "system",
                onClick = { onSettingsChanged(settings.copy(themeMode = "system")) }
            )
            Text("System")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = settings.themeMode == "light",
                onClick = { onSettingsChanged(settings.copy(themeMode = "light")) }
            )
            Text("Light")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = settings.themeMode == "dark",
                onClick = { onSettingsChanged(settings.copy(themeMode = "dark")) }
            )
            Text("Dark")
        }
        
        // Font settings
        OutlinedTextField(
            value = settings.fontSize.toString(),
            onValueChange = { newSize ->
                newSize.toIntOrNull()?.let { size ->
                    onSettingsChanged(settings.copy(fontSize = size))
                }
            },
            label = { Text("Font Size") },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Accessibility options
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.highContrastEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(highContrastEnabled = it)) }
            )
            Text("High contrast mode")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.reduceMotion,
                onCheckedChange = { onSettingsChanged(settings.copy(reduceMotion = it)) }
            )
            Text("Reduce motion")
        }
    }
}

/**
 * Editor settings section.
 */
@Composable
private fun EditorSettings(
    settings: DesktopSettingsStorage.EditorSettings,
    onSettingsChanged: (DesktopSettingsStorage.EditorSettings) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Editor Settings",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Editor options
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.showLineNumbers,
                onCheckedChange = { onSettingsChanged(settings.copy(showLineNumbers = it)) }
            )
            Text("Show line numbers")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.wordWrap,
                onCheckedChange = { onSettingsChanged(settings.copy(wordWrap = it)) }
            )
            Text("Word wrap")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.autoSave,
                onCheckedChange = { onSettingsChanged(settings.copy(autoSave = it)) }
            )
            Text("Auto-save")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.enableSyntaxHighlighting,
                onCheckedChange = { onSettingsChanged(settings.copy(enableSyntaxHighlighting = it)) }
            )
            Text("Syntax highlighting")
        }
        
        // Tab settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.insertSpacesForTabs,
                onCheckedChange = { onSettingsChanged(settings.copy(insertSpacesForTabs = it)) }
            )
            Text("Insert spaces for tabs")
        }
        
        OutlinedTextField(
            value = "4", // Default tab size
            onValueChange = {  },
            label = { Text("Tab Size") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * File settings section.
 */
@Composable
private fun FileSettings(
    settings: DesktopSettingsStorage.FileSettings,
    onSettingsChanged: (DesktopSettingsStorage.FileSettings) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "File Settings",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Encoding
        OutlinedTextField(
            value = settings.defaultEncoding,
            onValueChange = { onSettingsChanged(settings.copy(defaultEncoding = it)) },
            label = { Text("Default Encoding") },
            modifier = Modifier.fillMaxWidth()
        )
        
        // File options
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.detectEncoding,
                onCheckedChange = { onSettingsChanged(settings.copy(detectEncoding = it)) }
            )
            Text("Auto-detect file encoding")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.backupOnSave,
                onCheckedChange = { onSettingsChanged(settings.copy(backupOnSave = it)) }
            )
            Text("Create backup on save")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.confirmOverwrite,
                onCheckedChange = { onSettingsChanged(settings.copy(confirmOverwrite = it)) }
            )
            Text("Confirm file overwrite")
        }
    }
}

/**
 * Window settings section.
 */
@Composable
private fun WindowSettings(
    settings: DesktopSettingsStorage.WindowSettings,
    onSettingsChanged: (DesktopSettingsStorage.WindowSettings) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Window Settings",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Window options
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.rememberWindowPosition,
                onCheckedChange = { onSettingsChanged(settings.copy(rememberWindowPosition = it)) }
            )
            Text("Remember window position")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.rememberWindowSize,
                onCheckedChange = { onSettingsChanged(settings.copy(rememberWindowSize = it)) }
            )
            Text("Remember window size")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.minimizeToTray,
                onCheckedChange = { onSettingsChanged(settings.copy(minimizeToTray = it)) }
            )
            Text("Minimize to system tray")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.confirmOnExit,
                onCheckedChange = { onSettingsChanged(settings.copy(confirmOnExit = it)) }
            )
            Text("Confirm on exit")
        }
    }
}

/**
 * Shortcuts settings section.
 */
@Composable
private fun ShortcutsSettings(
    settings: DesktopSettingsStorage.ShortcutSettings,
    onSettingsChanged: (DesktopSettingsStorage.ShortcutSettings) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Keyboard Shortcuts",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.enableCustomShortcuts,
                onCheckedChange = { onSettingsChanged(settings.copy(enableCustomShortcuts = it)) }
            )
            Text("Enable custom keyboard shortcuts")
        }
        
        Text(
            text = "Custom shortcuts can be configured in the shortcuts configuration file.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Advanced settings section.
 */
@Composable
private fun AdvancedSettings(
    settings: DesktopSettingsStorage.AdvancedSettings,
    onSettingsChanged: (DesktopSettingsStorage.AdvancedSettings) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Advanced Settings",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Advanced options
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.enableLogging,
                onCheckedChange = { onSettingsChanged(settings.copy(enableLogging = it)) }
            )
            Text("Enable logging")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.enableCrashReporting,
                onCheckedChange = { onSettingsChanged(settings.copy(enableCrashReporting = it)) }
            )
            Text("Enable crash reporting")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.checkForUpdates,
                onCheckedChange = { onSettingsChanged(settings.copy(checkForUpdates = it)) }
            )
            Text("Check for updates")
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = settings.enableBetaFeatures,
                onCheckedChange = { onSettingsChanged(settings.copy(enableBetaFeatures = it)) }
            )
            Text("Enable beta features")
        }
    }
}

/**
 * About dialog.
 */
@Composable
fun AboutDialog(
    onClose: () -> Unit
) {
    Dialog(
        onCloseRequest = onClose,
        title = "About Yole",
        state = rememberDialogState(
            width = 400.dp,
            height = 500.dp,
            position = WindowPosition(Alignment.Center)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App icon placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Y",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Text(
                    text = "Yole",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Text(
                    text = "Version 2.15.1",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text(
                    text = "A versatile text editor supporting 18+ markup formats including Markdown, LaTeX, CSV, and more.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "© 2025 Milos Vasic\nLicensed under Apache 2.0",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = onClose) {
                    Text("Close")
                }
            }
        }
    }
}