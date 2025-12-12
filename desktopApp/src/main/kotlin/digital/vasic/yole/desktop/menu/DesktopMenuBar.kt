/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Menu Bar for Yole
 * Native application menu with keyboard shortcuts
 *
 *########################################################*/
package digital.vasic.yole.desktop.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import digital.vasic.yole.desktop.file.DesktopFileManager
import digital.vasic.yole.desktop.window.DesktopWindow
import digital.vasic.yole.format.FormatRegistry
import java.io.File

/**
 * Desktop menu bar with native application menu structure.
 */
@Composable
fun DesktopMenuBar(
    currentWindow: DesktopWindow?,
    fileManager: DesktopFileManager,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onSaveAsFile: () -> Unit,
    onCloseFile: () -> Unit,
    onExit: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onFind: () -> Unit,
    onReplace: () -> Unit,
    onSelectAll: () -> Unit,
    onPreferences: () -> Unit,
    onToggleWordWrap: () -> Unit,
    onToggleLineNumbers: () -> Unit,
    onToggleToolbar: () -> Unit,
    onToggleStatusBar: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedMenu by remember { mutableStateOf<String?>(null) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .onKeyEvent { event ->
                handleMenuKeyboardShortcuts(event, currentWindow, onNewFile, onOpenFile, onSaveFile, onUndo, onRedo, onCut, onCopy, onPaste, onFind, onSelectAll, onPreferences)
            }
    ) {
        // File Menu
        DesktopMenu(
            title = "File",
            isExpanded = expandedMenu == "File",
            onExpand = { expandedMenu = if (it) "File" else null }
        ) {
            DesktopMenuItem("New", "Ctrl+N", onClick = onNewFile)
            DesktopMenuItem("Open...", "Ctrl+O", onClick = onOpenFile)
            DesktopMenuItem("Save", "Ctrl+S", enabled = currentWindow != null, onClick = onSaveFile)
            DesktopMenuItem("Save As...", "Ctrl+Shift+S", enabled = currentWindow != null, onClick = onSaveAsFile)
            DesktopMenuSeparator()
            
            // Recent Files
            val recentFiles = fileManager.getRecentFiles().take(5)
            if (recentFiles.isNotEmpty()) {
                DesktopMenuItem("Recent Files", enabled = false) {}
                recentFiles.forEach { file ->
                    DesktopMenuItem("  ${file.name}", onClick = { 
                        // Open recent file
                        loadRecentFile(file, fileManager)
                    })
                }
                DesktopMenuSeparator()
            }
            
            DesktopMenuItem("Close", "Ctrl+W", enabled = currentWindow != null, onClick = onCloseFile)
            DesktopMenuSeparator()
            DesktopMenuItem("Exit", "Alt+F4", onClick = onExit)
        }
        
        // Edit Menu
        DesktopMenu(
            title = "Edit",
            isExpanded = expandedMenu == "Edit",
            onExpand = { expandedMenu = if (it) "Edit" else null }
        ) {
            DesktopMenuItem("Undo", "Ctrl+Z", enabled = currentWindow != null, onClick = onUndo)
            DesktopMenuItem("Redo", "Ctrl+Y", enabled = currentWindow != null, onClick = onRedo)
            DesktopMenuSeparator()
            DesktopMenuItem("Cut", "Ctrl+X", enabled = currentWindow != null, onClick = onCut)
            DesktopMenuItem("Copy", "Ctrl+C", enabled = currentWindow != null, onClick = onCopy)
            DesktopMenuItem("Paste", "Ctrl+V", enabled = currentWindow != null, onClick = onPaste)
            DesktopMenuSeparator()
            DesktopMenuItem("Find...", "Ctrl+F", enabled = currentWindow != null, onClick = onFind)
            DesktopMenuItem("Replace...", "Ctrl+H", enabled = currentWindow != null, onClick = onReplace)
            DesktopMenuSeparator()
            DesktopMenuItem("Select All", "Ctrl+A", enabled = currentWindow != null, onClick = onSelectAll)
        }
        
        // View Menu
        DesktopMenu(
            title = "View",
            isExpanded = expandedMenu == "View",
            onExpand = { expandedMenu = if (it) "View" else null }
        ) {
            DesktopMenuItem("Word Wrap", onClick = onToggleWordWrap)
            DesktopMenuItem("Line Numbers", onClick = onToggleLineNumbers)
            DesktopMenuSeparator()
            DesktopMenuItem("Toolbar", onClick = onToggleToolbar)
            DesktopMenuItem("Status Bar", onClick = onToggleStatusBar)
            DesktopMenuSeparator()
            DesktopMenuItem("Zoom In", "Ctrl++", onClick = onZoomIn)
            DesktopMenuItem("Zoom Out", "Ctrl+-", onClick = onZoomOut)
            DesktopMenuItem("Reset Zoom", "Ctrl+0", onClick = onResetZoom)
        }
        
        // Tools Menu
        DesktopMenu(
            title = "Tools",
            isExpanded = expandedMenu == "Tools",
            onExpand = { expandedMenu = if (it) "Tools" else null }
        ) {
            DesktopMenuItem("Preferences...", onClick = onPreferences)
        }
        
        // Help Menu
        DesktopMenu(
            title = "Help",
            isExpanded = expandedMenu == "Help",
            onExpand = { expandedMenu = if (it) "Help" else null }
        ) {
            DesktopMenuItem("About Yole", onClick = onAbout)
        }
    }
}

/**
 * Individual menu component.
 */
@Composable
private fun DesktopMenu(
    title: String,
    isExpanded: Boolean,
    onExpand: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box {
        TextButton(
            onClick = { onExpand(!isExpanded) },
            modifier = Modifier.height(32.dp)
        ) {
            Text(title)
        }
        
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpand(false) }
        ) {
            content()
        }
    }
}

/**
 * Individual menu item component.
 */
@Composable
private fun DesktopMenuItem(
    text: String,
    shortcut: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text)
                shortcut?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        onClick = onClick,
        enabled = enabled
    )
}

/**
 * Menu separator.
 */
@Composable
private fun DesktopMenuSeparator() {
    Divider(modifier = Modifier.padding(vertical = 4.dp))
}

/**
 * Handles keyboard shortcuts for menu items.
 */
private fun handleMenuKeyboardShortcuts(
    event: KeyEvent,
    currentWindow: DesktopWindow?,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onFind: () -> Unit,
    onSelectAll: () -> Unit,
    onPreferences: () -> Unit
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    
    val ctrlPressed = event.isCtrlPressed
    val shiftPressed = event.isShiftPressed
    val altPressed = event.isAltPressed
    
    return when (event.key) {
        Key.N -> if (ctrlPressed) { onNewFile(); true } else false
        Key.O -> if (ctrlPressed) { onOpenFile(); true } else false
        Key.S -> if (ctrlPressed && !shiftPressed) { onSaveFile(); true } else false
        Key.S -> if (ctrlPressed && shiftPressed) { /* Save As */ true } else false
        Key.Z -> if (ctrlPressed && !shiftPressed) { onUndo(); true } else false
        Key.Y -> if (ctrlPressed) { onRedo(); true } else false
        Key.Z -> if (ctrlPressed && shiftPressed) { onRedo(); true } else false
        Key.X -> if (ctrlPressed) { onCut(); true } else false
        Key.C -> if (ctrlPressed) { onCopy(); true } else false
        Key.V -> if (ctrlPressed) { onPaste(); true } else false
        Key.F -> if (ctrlPressed) { onFind(); true } else false
        Key.A -> if (ctrlPressed) { onSelectAll(); true } else false
        Key.Comma -> if (ctrlPressed) { onPreferences(); true } else false
        else -> false
    }
}

/**
 * Loads a recent file and opens it in the editor.
 */
private fun loadRecentFile(file: File, fileManager: DesktopFileManager) {
    try {
        val content = fileManager.loadFile(file)
        if (content != null) {
            // Open file in editor - this would be handled by the main application
            println("Opening recent file: ${file.name}")
        } else {
            // Show error message
            println("Could not load file: ${file.name}")
        }
    } catch (e: Exception) {
        // Show error message
        println("Error loading file: ${e.message}")
    }
}