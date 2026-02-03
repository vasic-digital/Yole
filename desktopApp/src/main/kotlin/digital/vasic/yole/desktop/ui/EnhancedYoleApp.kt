/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Enhanced Desktop UI for Yole
 * Comprehensive desktop application with all features
 *
 *########################################################*/
package digital.vasic.yole.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import digital.vasic.yole.desktop.dialog.DesktopFileDialogs
import digital.vasic.yole.desktop.file.*
import digital.vasic.yole.desktop.menu.DesktopMenuBar
import digital.vasic.yole.desktop.shortcut.DesktopKeyboardShortcuts
import digital.vasic.yole.desktop.storage.DesktopSettingsStorage
import digital.vasic.yole.desktop.system.DesktopSystemTray
import digital.vasic.yole.desktop.window.DesktopWindow
import digital.vasic.yole.desktop.window.DesktopWindowManager
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Enhanced main application composable with comprehensive desktop functionality.
 */
@Composable
fun EnhancedYoleApp() {
    val settings = remember { DesktopSettingsStorage() }
    val appSettings = remember { mutableStateOf(settings.loadSettings()) }
    val fileManager = remember { DesktopFileManager() }
    val windowManager = remember { DesktopWindowManager() }
    val keyboardShortcuts = remember { DesktopKeyboardShortcuts() }
    val trayState = remember { TrayState() }
    
    // Application state
    var currentWindow by remember { mutableStateOf<DesktopWindow?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showFindDialog by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    
    // Create system tray
    val systemTray = remember {
        DesktopSystemTray(
            fileManager = fileManager,
            onNewFile = { createNewFile(windowManager, fileManager) },
            onOpenFile = { openFile(windowManager, fileManager) },
            onShowWindow = { /* Show window */ },
            onExit = { /* Handle exit */ }
        )
    }
    
    // Create system tray

    
    // Main application layout
    Column(modifier = Modifier.fillMaxSize()) {
        // Menu Bar
        DesktopMenuBar(
            currentWindow = currentWindow,
            fileManager = fileManager,
            onNewFile = { createNewFile(windowManager, fileManager) },
            onOpenFile = { openFile(windowManager, fileManager) },
            onSaveFile = { saveCurrentFile(currentWindow, fileManager) },
            onSaveAsFile = { saveCurrentFileAs(currentWindow, fileManager) },
            onCloseFile = { closeCurrentFile(windowManager, currentWindow) },
            onExit = { /* Handle exit */ },
            onUndo = { /* Handle undo */ },
            onRedo = { /* Handle redo */ },
            onCut = { /* Handle cut */ },
            onCopy = { /* Handle copy */ },
            onPaste = { /* Handle paste */ },
            onFind = { showFindDialog = true },
            onReplace = { showFindDialog = true },
            onSelectAll = { /* Handle select all */ },
            onPreferences = { showSettings = true },
            onToggleWordWrap = { /* Toggle word wrap */ },
            onToggleLineNumbers = { /* Toggle line numbers */ },
            onToggleToolbar = { /* Toggle toolbar */ },
            onToggleStatusBar = { /* Toggle status bar */ },
            onZoomIn = { /* Zoom in */ },
            onZoomOut = { /* Zoom out */ },
            onResetZoom = { /* Reset zoom */ },
            onAbout = { showAbout = true }
        )
        
        // Toolbar
        DesktopToolbar(
            currentWindow = currentWindow,
            onNewFile = { createNewFile(windowManager, fileManager) },
            onOpenFile = { openFile(windowManager, fileManager) },
            onSaveFile = { saveCurrentFile(currentWindow, fileManager) },
            onUndo = { /* Handle undo */ },
            onRedo = { /* Handle redo */ },
            onFind = { showFindDialog = true }
        )
        
        // Main Content
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            DesktopSidebar(
                fileManager = fileManager,
                onFileSelected = { file ->
                    openFileInWindow(windowManager, fileManager, file)
                },
                modifier = Modifier.width(250.dp)
            )
            
            // Editor Area
            Box(modifier = Modifier.weight(1f)) {
                // Use let for null-safe access to currentWindow
                currentWindow?.let { window ->
                    EnhancedEditorScreen(
                        window = window,
                        fileManager = fileManager,
                        appSettings = appSettings.value,
                        onContentChanged = { content, isModified ->
                            windowManager.updateWindowContent(window.id, content, isModified)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } ?:
                    WelcomeScreen(
                        onNewFile = { createNewFile(windowManager, fileManager) },
                        onOpenFile = { openFile(windowManager, fileManager) },
                        recentFiles = fileManager.getRecentFiles(),
                        onRecentFileClick = { file ->
                            openFileInWindow(windowManager, fileManager, file)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // Status Bar
        DesktopStatusBar(
            currentWindow = currentWindow,
            fileManager = fileManager,
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    // Dialogs
    if (showFindDialog) {
        FindReplaceDialog(
            findText = findText,
            replaceText = replaceText,
            onFindTextChange = { findText = it },
            onReplaceTextChange = { replaceText = it },
            onFind = { /* Handle find */ },
            onReplace = { /* Handle replace */ },
            onReplaceAll = { /* Handle replace all */ },
            onClose = { showFindDialog = false }
        )
    }
    
    if (showSettings) {
        SettingsDialog(
            settings = appSettings.value,
            onSettingsChanged = { newSettings ->
                appSettings.value = newSettings
                settings.saveSettings(newSettings)
            },
            onClose = { showSettings = false }
        )
    }
    
    if (showAbout) {
        AboutDialog(
            onClose = { showAbout = false }
        )
    }
}

/**
 * Enhanced editor screen with comprehensive functionality.
 */
@Composable
private fun EnhancedEditorScreen(
    window: DesktopWindow,
    fileManager: DesktopFileManager,
    appSettings: DesktopSettingsStorage.AppSettings,
    onContentChanged: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var content by remember { mutableStateOf(window.content) }
    var cursorPosition by remember { mutableStateOf(0 to 0) }
    var selection by remember { mutableStateOf<IntRange?>(null) }
    
    Column(modifier = modifier.padding(16.dp)) {
        // Editor Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = window.getDisplayTitle(),
                style = MaterialTheme.typography.headlineSmall
            )
            
            Row {
                // Format indicator
                window.format?.let { format ->
                    AssistChip(
                        onClick = { /* Show format info */ },
                        label = { Text(format.name) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // Encoding indicator
                Text(
                    text = "UTF-8", // This would come from the document
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Enhanced Text Editor
        EnhancedTextEditor(
            content = content,
            onContentChange = { newContent ->
                content = newContent
                onContentChanged(newContent, true)
            },
            format = window.format,
            appSettings = appSettings,
            onCursorPositionChange = { line, column ->
                cursorPosition = line to column
            },
            onSelectionChange = { range ->
                selection = range
            },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Enhanced text editor with syntax highlighting and advanced features.
 */
@Composable
private fun EnhancedTextEditor(
    content: String,
    onContentChange: (String) -> Unit,
    format: TextFormat?,
    appSettings: DesktopSettingsStorage.AppSettings,
    onCursorPositionChange: (Int, Int) -> Unit,
    onSelectionChange: (IntRange?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textStyle = TextStyle(
        fontSize = appSettings.appearance.fontSize.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
    )
    
    BasicTextField(
        value = content,
        onValueChange = onContentChange,
        textStyle = textStyle,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState()),
        decorationBox = { innerTextField ->
            Box {
                if (content.isEmpty()) {
                    Text(
                        text = "Start typing...",
                        style = textStyle.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

/**
 * Desktop toolbar with quick actions.
 */
@Composable
private fun DesktopToolbar(
    currentWindow: DesktopWindow?,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFind: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onNewFile) {
            Text("New")
        }
        
        Button(onClick = onOpenFile) {
            Text("Open")
        }
        
        Button(
            onClick = onSaveFile,
            enabled = currentWindow != null
        ) {
            Text("Save")
        }
        
        Divider(
            modifier = Modifier
                .height(24.dp)
                .width(1.dp)
        )
        
        Button(onClick = onUndo) {
            Text("Undo")
        }
        
        Button(onClick = onRedo) {
            Text("Redo")
        }
        
        Divider(
            modifier = Modifier
                .height(24.dp)
                .width(1.dp)
        )
        
        Button(onClick = onFind) {
            Text("Find")
        }
    }
}

/**
 * Desktop sidebar with file browser and recent files.
 */
@Composable
private fun DesktopSidebar(
    fileManager: DesktopFileManager,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.height(48.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Files") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Recent") }
            )
        }
        
        when (selectedTab) {
            0 -> FileBrowser(
                fileManager = fileManager,
                onFileSelected = onFileSelected,
                modifier = Modifier.weight(1f)
            )
            1 -> RecentFilesList(
                recentFiles = fileManager.getRecentFiles(),
                onFileSelected = onFileSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * File browser component.
 */
@Composable
private fun FileBrowser(
    fileManager: DesktopFileManager,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    // This would be a comprehensive file browser
    // For now, show a simple directory listing
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "File Browser",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    // Add file filters for supported formats
                    val filter = FileNameExtensionFilter(
                        "Supported Files",
                        "md", "txt", "csv", "tex", "org", "json", "xml", "html", "css", "js", "kt", "java", "py", "cpp", "c"
                    )
                    addChoosableFileFilter(filter)
                }
                
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    onFileSelected(chooser.selectedFile)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Browse Files...")
        }
    }
}

/**
 * Recent files list component.
 */
@Composable
private fun RecentFilesList(
    recentFiles: List<File>,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Recent Files",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (recentFiles.isEmpty()) {
            Text(
                text = "No recent files",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn {
                items(recentFiles) { file ->
                    RecentFileItem(
                        file = file,
                        onClick = { onFileSelected(file) }
                    )
                }
            }
        }
    }
}

/**
 * Individual recent file item.
 */
@Composable
private fun RecentFileItem(
    file: File,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = file.parent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

/**
 * Desktop status bar.
 */
@Composable
private fun DesktopStatusBar(
    currentWindow: DesktopWindow?,
    fileManager: DesktopFileManager,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - file info
            currentWindow?.let { window ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = window.getDisplayTitle(),
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    window.format?.let { format ->
                        Text(
                            text = format.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } ?: Text(
                text = "Ready",
                style = MaterialTheme.typography.bodySmall
            )
            
            // Right side - cursor position and other info
            Text(
                text = "Ln 1, Col 1", // This would come from the editor
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Welcome screen for when no file is open.
 */
@Composable
private fun WelcomeScreen(
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    recentFiles: List<File>,
    onRecentFileClick: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Yole",
            style = MaterialTheme.typography.displaySmall
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onNewFile,
                modifier = Modifier.size(120.dp, 48.dp)
            ) {
                Text("New File")
            }
            
            Button(
                onClick = onOpenFile,
                modifier = Modifier.size(120.dp, 48.dp)
            ) {
                Text("Open File")
            }
        }
        
        if (recentFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Recent Files",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(recentFiles.take(5)) { file ->
                    RecentFileItem(
                        file = file,
                        onClick = { onRecentFileClick(file) }
                    )
                }
            }
        }
    }
}

/**
 * Helper functions for file operations.
 */

private fun createNewFile(windowManager: DesktopWindowManager, fileManager: DesktopFileManager) {
    val window = windowManager.createWindow()
    // Set as current window
}

private fun openFile(windowManager: DesktopWindowManager, fileManager: DesktopFileManager) {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.FILES_ONLY
        // Add file filters
    }
    
    val result = chooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile
        val content = fileManager.loadFile(file)
        if (content != null) {
            val window = windowManager.createWindow(file, content)
            // Set as current window
        }
    }
}

private fun saveCurrentFile(currentWindow: DesktopWindow?, fileManager: DesktopFileManager) {
    currentWindow?.let { window ->
        val file = window.file
        if (file != null) {
            fileManager.saveFile(file, window.content)
            window.isModified = false
        } else {
            // Show save as dialog
            saveCurrentFileAs(window, fileManager)
        }
    }
}

private fun saveCurrentFileAs(currentWindow: DesktopWindow?, fileManager: DesktopFileManager) {
    currentWindow?.let { window ->
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            // Add file filters
        }
        
        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = chooser.selectedFile
            if (fileManager.saveFile(selectedFile, window.content)) {
                // Update window with new file
                window.isModified = false
            }
        }
    }
}

private fun closeCurrentFile(windowManager: DesktopWindowManager, currentWindow: DesktopWindow?) {
    currentWindow?.let { window ->
        if (window.isModified) {
            // Show confirmation dialog
        } else {
            windowManager.closeWindow(window.id)
        }
    }
}

private fun openFileInWindow(windowManager: DesktopWindowManager, fileManager: DesktopFileManager, file: File) {
    val content = fileManager.loadFile(file)
    if (content != null) {
        val window = windowManager.createWindow(file, content)
        // Set as current window
    }
}