/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Enhanced Desktop UI for Yole
 * IDE-style comprehensive desktop application with all features
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import digital.vasic.yole.desktop.file.*
import digital.vasic.yole.desktop.menu.DesktopMenuBar
import digital.vasic.yole.desktop.shortcut.DesktopKeyboardShortcuts
import digital.vasic.yole.desktop.storage.DesktopSettingsStorage
import digital.vasic.yole.desktop.system.DesktopSystemTray
import digital.vasic.yole.desktop.window.DesktopWindow
import digital.vasic.yole.desktop.window.DesktopWindowManager
import digital.vasic.yole.desktop.windowVisibilityToggle
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Undo/Redo manager that tracks text changes.
 * Maintains a stack of text states for undo and redo operations.
 */
class UndoManager(private val maxHistory: Int = 100) {
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    /**
     * Records a new state. Called when the user makes a text change.
     */
    fun recordState(content: String) {
        // Avoid recording duplicate states
        if (undoStack.isNotEmpty() && undoStack.last() == content) return
        undoStack.add(content)
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        // New edit clears redo history
        redoStack.clear()
    }

    /**
     * Undoes the last change. Returns the previous state, or null if nothing to undo.
     */
    fun undo(currentContent: String): String? {
        if (undoStack.isEmpty()) return null
        redoStack.add(currentContent)
        return undoStack.removeAt(undoStack.size - 1)
    }

    /**
     * Redoes the last undone change. Returns the restored state, or null if nothing to redo.
     */
    fun redo(currentContent: String): String? {
        if (redoStack.isEmpty()) return null
        undoStack.add(currentContent)
        return redoStack.removeAt(redoStack.size - 1)
    }

    /**
     * Returns true if undo is available.
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()

    /**
     * Returns true if redo is available.
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * Clears all undo/redo history.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}

/**
 * Enhanced main application composable with IDE-style layout.
 *
 * @param droppedFile A file dropped onto the window via drag-and-drop
 * @param onDroppedFileConsumed Callback when the dropped file has been handled
 * @param onExit Callback to request application exit
 */
@Composable
fun EnhancedYoleApp(
    droppedFile: File? = null,
    onDroppedFileConsumed: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    val settings = remember { DesktopSettingsStorage() }
    val appSettings = remember { mutableStateOf(settings.loadSettings()) }
    val fileManager = remember { DesktopFileManager() }
    val windowManager = remember { DesktopWindowManager() }
    val keyboardShortcuts = remember { DesktopKeyboardShortcuts() }
    val undoManager = remember { UndoManager() }

    // Application state
    var currentWindow by remember { mutableStateOf<DesktopWindow?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showFindDialog by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showToolbar by remember { mutableStateOf(true) }
    var showStatusBar by remember { mutableStateOf(true) }
    var showPreview by remember { mutableStateOf(false) }
    var showSidebar by remember { mutableStateOf(true) }
    var showExportMenu by remember { mutableStateOf(false) }

    // Cursor position state tracked from editor
    var cursorLine by remember { mutableStateOf(1) }
    var cursorColumn by remember { mutableStateOf(1) }

    // Find/Replace match tracking
    var findMatchIndex by remember { mutableStateOf(-1) }
    var findMatchCount by remember { mutableStateOf(0) }
    var findMatchPositions by remember { mutableStateOf(listOf<Int>()) }
    var findStatusMessage by remember { mutableStateOf("") }

    // Helper to update the editor content via the current window
    fun updateContent(newContent: String) {
        currentWindow?.let { window ->
            window.content = newContent
            window.isModified = true
            windowManager.updateWindowContent(window.id, newContent, true)
        }
    }

    // Undo handler
    fun performUndo() {
        val window = currentWindow ?: return
        val previousState = undoManager.undo(window.content)
        if (previousState != null) {
            updateContent(previousState)
        }
    }

    // Redo handler
    fun performRedo() {
        val window = currentWindow ?: return
        val restoredState = undoManager.redo(window.content)
        if (restoredState != null) {
            updateContent(restoredState)
        }
    }

    // Find next match handler
    fun performFind() {
        val window = currentWindow ?: return
        if (findText.isEmpty()) {
            findStatusMessage = ""
            findMatchCount = 0
            findMatchIndex = -1
            findMatchPositions = emptyList()
            return
        }
        val content = window.content
        val matches = mutableListOf<Int>()
        var searchFrom = 0
        while (true) {
            val idx = content.indexOf(findText, searchFrom, ignoreCase = true)
            if (idx < 0) break
            matches.add(idx)
            searchFrom = idx + 1
        }
        findMatchPositions = matches
        findMatchCount = matches.size
        if (matches.isEmpty()) {
            findMatchIndex = -1
            findStatusMessage = "No matches found"
        } else {
            findMatchIndex = if (findMatchIndex < 0 || findMatchIndex >= matches.size - 1) 0 else findMatchIndex + 1
            findStatusMessage = "Match ${findMatchIndex + 1} of ${matches.size}"
        }
    }

    // Replace current match handler
    fun performReplace() {
        val window = currentWindow ?: return
        if (findText.isEmpty()) return
        val content = window.content
        val replaceIdx = if (findMatchIndex >= 0 && findMatchIndex < findMatchPositions.size) {
            findMatchPositions[findMatchIndex]
        } else {
            content.indexOf(findText, ignoreCase = true)
        }
        if (replaceIdx >= 0) {
            undoManager.recordState(content)
            val newContent = content.substring(0, replaceIdx) + replaceText + content.substring(replaceIdx + findText.length)
            updateContent(newContent)
            findStatusMessage = "Replaced 1 occurrence"
            findMatchPositions = emptyList()
            findMatchCount = 0
            findMatchIndex = -1
        } else {
            findStatusMessage = "No match to replace"
        }
    }

    // Replace all handler
    fun performReplaceAll() {
        val window = currentWindow ?: return
        if (findText.isEmpty()) return
        val content = window.content
        val count = content.split(findText, ignoreCase = true).size - 1
        if (count > 0) {
            undoManager.recordState(content)
            val newContent = content.replace(findText, replaceText, ignoreCase = true)
            updateContent(newContent)
            findStatusMessage = "Replaced $count occurrence(s)"
            findMatchPositions = emptyList()
            findMatchCount = 0
            findMatchIndex = -1
        } else {
            findStatusMessage = "No matches found"
        }
    }

    // Export to HTML handler
    fun exportToHtml() {
        val window = currentWindow ?: return
        val chooser = JFileChooser().apply {
            dialogTitle = "Export as HTML"
            selectedFile = File(completionGetDefaultFileName(window.file, extension = ".html"))
            val filter = FileNameExtensionFilter("HTML Files", "html", "htm")
            addChoosableFileFilter(filter)
        }
        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                val format = window.format ?: window.file?.let { fileManager.detectFormatFromFile(it) }
                    ?: FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)
                val htmlContent = if (format != null) {
                    val parser = ParserRegistry.getParser(format)
                    if (parser != null) {
                        val doc = parser.parse(window.content)
                        val bodyHtml = doc.toHtml(lightMode = true)
                        buildString {
                            append("<!DOCTYPE html>\n<html>\n<head>\n")
                            append("<meta charset=\"UTF-8\">\n")
                            append("<title>${window.getDisplayTitle()}</title>\n")
                            append("</head>\n<body>\n")
                            append(bodyHtml)
                            append("\n</body>\n</html>")
                        }
                    } else {
                        "<html><body><pre>${window.content}</pre></body></html>"
                    }
                } else {
                    "<html><body><pre>${window.content}</pre></body></html>"
                }
                chooser.selectedFile.writeText(htmlContent, Charsets.UTF_8)
            } catch (e: Exception) {
                println("ERROR: Failed to export HTML: ${e.message}")
            }
        }
    }

    // Export to PDF handler
    fun exportToPdf() {
        val window = currentWindow ?: return
        val chooser = JFileChooser().apply {
            dialogTitle = "Export as PDF"
            selectedFile = File(completionGetDefaultFileName(window.file, extension = ".pdf"))
            val filter = FileNameExtensionFilter("PDF Files", "pdf")
            addChoosableFileFilter(filter)
        }
        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                val format = window.format ?: window.file?.let { fileManager.detectFormatFromFile(it) }
                    ?: FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)
                val htmlContent = if (format != null) {
                    val parser = ParserRegistry.getParser(format)
                    if (parser != null) {
                        val doc = parser.parse(window.content)
                        doc.toHtml(lightMode = true)
                    } else {
                        "<pre>${window.content}</pre>"
                    }
                } else {
                    "<pre>${window.content}</pre>"
                }
                val pdfHtml = buildString {
                    append("<!DOCTYPE html>\n<html>\n<head>\n")
                    append("<meta charset=\"UTF-8\">\n")
                    append("<title>${window.getDisplayTitle()}</title>\n")
                    append("<style>@media print { body { margin: 1cm; } }</style>\n")
                    append("</head>\n<body>\n")
                    append(htmlContent)
                    append("\n</body>\n</html>")
                }
                val htmlFile = File(chooser.selectedFile.absolutePath.replace(".pdf", ".html"))
                htmlFile.writeText(pdfHtml, Charsets.UTF_8)
                println("INFO: Exported HTML for PDF printing: ${htmlFile.name}")
            } catch (e: Exception) {
                println("ERROR: Failed to export PDF: ${e.message}")
            }
        }
    }

    // Handle file operations that return a new current window
    fun openFileAndSetCurrent(file: File) {
        val content = fileManager.loadFile(file)
        if (content != null) {
            val window = windowManager.createWindow(file, content)
            currentWindow = window
            undoManager.clear()
        }
    }

    // Handle dropped file from drag-and-drop
    LaunchedEffect(droppedFile) {
        droppedFile?.let { file ->
            openFileAndSetCurrent(file)
            onDroppedFileConsumed()
        }
    }

    // Create system tray with full functionality
    val systemTray = remember {
        DesktopSystemTray(
            fileManager = fileManager,
            onNewFile = {
                val window = windowManager.createWindow()
                currentWindow = window
                undoManager.clear()
            },
            onOpenFile = {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.FILES_ONLY
                }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    openFileAndSetCurrent(chooser.selectedFile)
                }
            },
            onShowWindow = { windowVisibilityToggle?.invoke(true) },
            onExit = { onExit() }
        )
    }

    // ========================================================================
    // IDE-Style Main Layout
    // ========================================================================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
            .onKeyEvent { event ->
                val action = keyboardShortcuts.handleKeyEvent(event)
                when (action) {
                    DesktopKeyboardShortcuts.ACTION_NEW_FILE -> {
                        val window = windowManager.createWindow()
                        currentWindow = window
                        undoManager.clear()
                        true
                    }
                    DesktopKeyboardShortcuts.ACTION_OPEN_FILE -> {
                        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.FILES_ONLY }
                        val result = chooser.showOpenDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            openFileAndSetCurrent(chooser.selectedFile)
                        }
                        true
                    }
                    DesktopKeyboardShortcuts.ACTION_SAVE_FILE -> {
                        saveCurrentFile(currentWindow, fileManager); true
                    }
                    DesktopKeyboardShortcuts.ACTION_SAVE_AS_FILE -> {
                        saveCurrentFileAs(currentWindow, fileManager); true
                    }
                    DesktopKeyboardShortcuts.ACTION_CLOSE_FILE -> {
                        closeCurrentFile(windowManager, currentWindow)
                        currentWindow = null
                        true
                    }
                    DesktopKeyboardShortcuts.ACTION_UNDO -> {
                        performUndo(); true
                    }
                    DesktopKeyboardShortcuts.ACTION_REDO -> {
                        performRedo(); true
                    }
                    DesktopKeyboardShortcuts.ACTION_FIND -> {
                        showFindDialog = true; true
                    }
                    DesktopKeyboardShortcuts.ACTION_REPLACE -> {
                        showFindDialog = true; true
                    }
                    DesktopKeyboardShortcuts.ACTION_ZOOM_IN -> {
                        appSettings.value = appSettings.value.copy(
                            appearance = appSettings.value.appearance.copy(
                                fontSize = (appSettings.value.appearance.fontSize + 2).coerceAtMost(32)
                            )
                        )
                        settings.saveSettings(appSettings.value)
                        true
                    }
                    DesktopKeyboardShortcuts.ACTION_ZOOM_OUT -> {
                        appSettings.value = appSettings.value.copy(
                            appearance = appSettings.value.appearance.copy(
                                fontSize = (appSettings.value.appearance.fontSize - 2).coerceAtLeast(8)
                            )
                        )
                        settings.saveSettings(appSettings.value)
                        true
                    }
                    DesktopKeyboardShortcuts.ACTION_RESET_ZOOM -> {
                        appSettings.value = appSettings.value.copy(
                            appearance = appSettings.value.appearance.copy(fontSize = 14)
                        )
                        settings.saveSettings(appSettings.value)
                        true
                    }
                    DesktopKeyboardShortcuts.ACTION_TOGGLE_WORD_WRAP -> {
                        appSettings.value = appSettings.value.copy(
                            editor = appSettings.value.editor.copy(wordWrap = !appSettings.value.editor.wordWrap)
                        )
                        settings.saveSettings(appSettings.value)
                        true
                    }
                    DesktopKeyboardShortcuts.ACTION_TOGGLE_LINE_NUMBERS -> {
                        appSettings.value = appSettings.value.copy(
                            editor = appSettings.value.editor.copy(
                                showLineNumbers = !appSettings.value.editor.showLineNumbers
                            )
                        )
                        settings.saveSettings(appSettings.value)
                        true
                    }
                    DesktopKeyboardShortcuts.ACTION_TOGGLE_TOOLBAR -> {
                        showToolbar = !showToolbar; true
                    }
                    DesktopKeyboardShortcuts.ACTION_TOGGLE_STATUS_BAR -> {
                        showStatusBar = !showStatusBar; true
                    }
                    DesktopKeyboardShortcuts.ACTION_EXIT -> {
                        onExit(); true
                    }
                    else -> false
                }
            }
    ) {
        // Menu Bar
        DesktopMenuBar(
            currentWindow = currentWindow,
            fileManager = fileManager,
            onNewFile = {
                val window = windowManager.createWindow()
                currentWindow = window
                undoManager.clear()
            },
            onOpenFile = {
                val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.FILES_ONLY }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    openFileAndSetCurrent(chooser.selectedFile)
                }
            },
            onSaveFile = { saveCurrentFile(currentWindow, fileManager) },
            onSaveAsFile = { saveCurrentFileAs(currentWindow, fileManager) },
            onCloseFile = {
                closeCurrentFile(windowManager, currentWindow)
                currentWindow = null
            },
            onExit = { onExit() },
            onUndo = { performUndo() },
            onRedo = { performRedo() },
            onCut = { /* Handled natively by text field */ },
            onCopy = { /* Handled natively by text field */ },
            onPaste = { /* Handled natively by text field */ },
            onFind = { showFindDialog = true },
            onReplace = { showFindDialog = true },
            onSelectAll = { /* Handled natively by text field */ },
            onPreferences = { showSettings = true },
            onToggleWordWrap = {
                appSettings.value = appSettings.value.copy(
                    editor = appSettings.value.editor.copy(wordWrap = !appSettings.value.editor.wordWrap)
                )
                settings.saveSettings(appSettings.value)
            },
            onToggleLineNumbers = {
                appSettings.value = appSettings.value.copy(
                    editor = appSettings.value.editor.copy(
                        showLineNumbers = !appSettings.value.editor.showLineNumbers
                    )
                )
                settings.saveSettings(appSettings.value)
            },
            onToggleToolbar = { showToolbar = !showToolbar },
            onToggleStatusBar = { showStatusBar = !showStatusBar },
            onZoomIn = {
                appSettings.value = appSettings.value.copy(
                    appearance = appSettings.value.appearance.copy(
                        fontSize = (appSettings.value.appearance.fontSize + 2).coerceAtMost(32)
                    )
                )
                settings.saveSettings(appSettings.value)
            },
            onZoomOut = {
                appSettings.value = appSettings.value.copy(
                    appearance = appSettings.value.appearance.copy(
                        fontSize = (appSettings.value.appearance.fontSize - 2).coerceAtLeast(8)
                    )
                )
                settings.saveSettings(appSettings.value)
            },
            onResetZoom = {
                appSettings.value = appSettings.value.copy(
                    appearance = appSettings.value.appearance.copy(fontSize = 14)
                )
                settings.saveSettings(appSettings.value)
            },
            onAbout = { showAbout = true },
            onExportHtml = { exportToHtml() },
            onExportPdf = { exportToPdf() }
        )

        // IDE Toolbar (toggleable)
        if (showToolbar) {
            IdeToolbar(
                currentWindow = currentWindow,
                onNewFile = {
                    val window = windowManager.createWindow()
                    currentWindow = window
                    undoManager.clear()
                },
                onOpenFile = {
                    val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.FILES_ONLY }
                    val result = chooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        openFileAndSetCurrent(chooser.selectedFile)
                    }
                },
                onSaveFile = { saveCurrentFile(currentWindow, fileManager) },
                onUndo = { performUndo() },
                onRedo = { performRedo() },
                onFind = { showFindDialog = true },
                onPreview = { showPreview = !showPreview },
                onToggleSidebar = { showSidebar = !showSidebar },
                onExportHtml = { exportToHtml() },
                onExportPdf = { exportToPdf() }
            )
        }

        // Tab bar for open documents
        IdeTabBar(
            windows = windowManager.getWindows(),
            currentWindow = currentWindow,
            onTabSelected = { window -> currentWindow = window },
            onTabClosed = { window ->
                closeCurrentFile(windowManager, window)
                if (currentWindow?.id == window.id) {
                    currentWindow = windowManager.getWindows().lastOrNull()
                }
            },
            onNewTab = {
                val window = windowManager.createWindow()
                currentWindow = window
                undoManager.clear()
            }
        )

        // Main Content Area
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // File Explorer Sidebar (toggleable)
            if (showSidebar) {
                IdeFileExplorer(
                    fileManager = fileManager,
                    onFileSelected = { file -> openFileAndSetCurrent(file) },
                    modifier = Modifier.width(250.dp)
                )

                // Sidebar border
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(IdeDarkBorder)
                )
            }

            // Editor Area
            val window = currentWindow
            if (window != null) {
                if (showPreview) {
                    // Split view: Editor + Preview
                    Row(modifier = Modifier.weight(1f)) {
                        EnhancedEditorScreen(
                            window = window,
                            fileManager = fileManager,
                            appSettings = appSettings.value,
                            undoManager = undoManager,
                            findText = findText,
                            findMatchPositions = findMatchPositions,
                            findMatchIndex = findMatchIndex,
                            onContentChanged = { content, isModified ->
                                windowManager.updateWindowContent(window.id, content, isModified)
                            },
                            onCursorPositionChanged = { line, col ->
                                cursorLine = line
                                cursorColumn = col
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(IdeDarkBorder)
                        )
                        LivePreviewPane(
                            content = window.content,
                            format = window.format ?: window.file?.let { fileManager.detectFormatFromFile(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    EnhancedEditorScreen(
                        window = window,
                        fileManager = fileManager,
                        appSettings = appSettings.value,
                        undoManager = undoManager,
                        findText = findText,
                        findMatchPositions = findMatchPositions,
                        findMatchIndex = findMatchIndex,
                        onContentChanged = { content, isModified ->
                            windowManager.updateWindowContent(window.id, content, isModified)
                        },
                        onCursorPositionChanged = { line, col ->
                            cursorLine = line
                            cursorColumn = col
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                IdeWelcomeScreen(
                    onNewFile = {
                        val w = windowManager.createWindow()
                        currentWindow = w
                        undoManager.clear()
                    },
                    onOpenFile = {
                        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.FILES_ONLY }
                        val result = chooser.showOpenDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            openFileAndSetCurrent(chooser.selectedFile)
                        }
                    },
                    recentFiles = fileManager.getRecentFiles(),
                    onRecentFileClick = { file -> openFileAndSetCurrent(file) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // IDE Status Bar (toggleable)
        if (showStatusBar) {
            IdeEnhancedStatusBar(
                currentWindow = currentWindow,
                fileManager = fileManager,
                cursorLine = cursorLine,
                cursorColumn = cursorColumn,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Dialogs
    if (showFindDialog) {
        FindReplaceDialog(
            findText = findText,
            replaceText = replaceText,
            matchStatusMessage = findStatusMessage,
            onFindTextChange = { newText ->
                findText = newText
                if (newText.isEmpty()) {
                    findMatchPositions = emptyList()
                    findMatchCount = 0
                    findMatchIndex = -1
                    findStatusMessage = ""
                }
            },
            onReplaceTextChange = { replaceText = it },
            onFind = { performFind() },
            onReplace = { performReplace() },
            onReplaceAll = { performReplaceAll() },
            onClose = {
                showFindDialog = false
                findStatusMessage = ""
                findMatchPositions = emptyList()
                findMatchCount = 0
                findMatchIndex = -1
            }
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

// ============================================================================
// IDE Tab Bar
// ============================================================================

/**
 * IDE-style tab bar for open documents.
 */
@Composable
private fun IdeTabBar(
    windows: List<DesktopWindow>,
    currentWindow: DesktopWindow?,
    onTabSelected: (DesktopWindow) -> Unit,
    onTabClosed: (DesktopWindow) -> Unit,
    onNewTab: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(IdeDarkSurfaceVariant)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.Bottom
    ) {
        windows.forEach { window ->
            val isActive = currentWindow?.id == window.id
            Box(
                modifier = Modifier
                    .clickable { onTabSelected(window) }
                    .background(
                        if (isActive) IdeDarkBackground else Color.Transparent
                    )
                    .then(
                        if (isActive) Modifier.border(
                            width = 1.dp,
                            color = IdeDarkBorder
                        ) else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Modified indicator
                    if (window.isModified) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(IdeAccent, shape = MaterialTheme.shapes.small)
                        )
                    }
                    Text(
                        text = window.title,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isActive) IdeTextPrimary else IdeTextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Close button
                    Text(
                        text = "x",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = IdeTextMuted
                        ),
                        modifier = Modifier
                            .clickable { onTabClosed(window) }
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // New tab button
        Box(
            modifier = Modifier
                .clickable { onNewTab() }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = "+",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = IdeTextSecondary
                )
            )
        }
    }

    // Active tab indicator line
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(IdeDarkBorder)
    )
}

// ============================================================================
// IDE Toolbar
// ============================================================================

/**
 * IDE-style toolbar with compact icon buttons.
 */
@Composable
private fun IdeToolbar(
    currentWindow: DesktopWindow?,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFind: () -> Unit,
    onPreview: () -> Unit = {},
    onToggleSidebar: () -> Unit = {},
    onExportHtml: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showExportMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(IdeDarkSurface)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IdeToolbarButton("New", onClick = onNewFile)
        IdeToolbarButton("Open", onClick = onOpenFile)
        IdeToolbarButton("Save", onClick = onSaveFile, enabled = currentWindow != null)

        IdeToolbarSeparator()

        IdeToolbarButton("Undo", onClick = onUndo, enabled = currentWindow != null)
        IdeToolbarButton("Redo", onClick = onRedo, enabled = currentWindow != null)

        IdeToolbarSeparator()

        IdeToolbarButton("Find", onClick = onFind, enabled = currentWindow != null)
        IdeToolbarButton("Preview", onClick = onPreview, enabled = currentWindow != null)
        IdeToolbarButton("Sidebar", onClick = onToggleSidebar)

        IdeToolbarSeparator()

        // Export dropdown
        Box {
            IdeToolbarButton("Export", onClick = { showExportMenu = true }, enabled = currentWindow != null)
            DropdownMenu(
                expanded = showExportMenu,
                onDismissRequest = { showExportMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Export as HTML") },
                    onClick = { onExportHtml(); showExportMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Export as PDF") },
                    onClick = { onExportPdf(); showExportMenu = false }
                )
            }
        }
    }

    // Toolbar bottom border
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(IdeDarkBorder)
    )
}

@Composable
private fun IdeToolbarButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                color = if (enabled) IdeTextPrimary else IdeTextMuted
            )
        )
    }
}

@Composable
private fun IdeToolbarSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(IdeDarkBorder)
    )
    Spacer(modifier = Modifier.width(2.dp))
}

// ============================================================================
// IDE File Explorer Sidebar
// ============================================================================

/**
 * IDE-style file explorer sidebar with tree view.
 */
@Composable
private fun IdeFileExplorer(
    fileManager: DesktopFileManager,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.background(IdeDarkSurface)) {
        // Explorer header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(IdeDarkSurfaceVariant)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedTab == 0) "EXPLORER" else "RECENT",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = IdeTextSecondary
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            // Tab toggle
            Text(
                text = if (selectedTab == 0) "Recent" else "Files",
                style = TextStyle(fontSize = 11.sp, color = IdeAccent),
                modifier = Modifier
                    .clickable { selectedTab = if (selectedTab == 0) 1 else 0 }
                    .padding(4.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(IdeDarkBorder)
        )

        when (selectedTab) {
            0 -> IdeFileBrowser(
                fileManager = fileManager,
                onFileSelected = onFileSelected,
                modifier = Modifier.weight(1f)
            )
            1 -> IdeRecentFilesList(
                recentFiles = fileManager.getRecentFiles(),
                onFileSelected = onFileSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * IDE-style navigable file browser with directory tree.
 */
@Composable
private fun IdeFileBrowser(
    fileManager: DesktopFileManager,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDirectory by remember { mutableStateOf(File(System.getProperty("user.home"))) }
    var showHiddenFiles by remember { mutableStateOf(false) }

    val directoryEntries = remember(currentDirectory, showHiddenFiles) {
        try {
            val entries = currentDirectory.listFiles()?.toList() ?: emptyList()
            val filtered = if (showHiddenFiles) entries else entries.filter { !it.name.startsWith(".") }
            val directories = filtered.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
            val files = filtered.filter { it.isFile }.sortedBy { it.name.lowercase() }
            directories + files
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    Column(modifier = modifier.padding(4.dp)) {
        // Current directory path
        Text(
            text = currentDirectory.absolutePath,
            style = TextStyle(
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = IdeTextMuted
            ),
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Parent navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[..] Up",
                style = TextStyle(fontSize = 11.sp, color = IdeAccent),
                modifier = Modifier
                    .clickable(enabled = currentDirectory.parentFile != null) {
                        currentDirectory.parentFile?.let { currentDirectory = it }
                    }
                    .padding(4.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hidden",
                    style = TextStyle(fontSize = 10.sp, color = IdeTextMuted)
                )
                Checkbox(
                    checked = showHiddenFiles,
                    onCheckedChange = { showHiddenFiles = it },
                    modifier = Modifier.size(20.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = IdeAccent,
                        uncheckedColor = IdeTextMuted
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(IdeDarkBorder)
                .padding(vertical = 2.dp)
        )

        if (directoryEntries.isEmpty()) {
            Text(
                text = "Empty directory",
                style = TextStyle(fontSize = 11.sp, color = IdeTextMuted),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(directoryEntries) { entry ->
                    val isDirectory = entry.isDirectory
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isDirectory) {
                                    currentDirectory = entry
                                } else {
                                    onFileSelected(entry)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDirectory) "D" else "F",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDirectory) IdeAccent else IdeTextMuted
                            ),
                            modifier = Modifier
                                .size(16.dp)
                                .wrapContentSize(Alignment.Center)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = entry.name,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isDirectory) FontWeight.Medium else FontWeight.Normal,
                                color = if (isDirectory) IdeAccent else IdeTextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recent files list in IDE style.
 */
@Composable
private fun IdeRecentFilesList(
    recentFiles: List<File>,
    onFileSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        if (recentFiles.isEmpty()) {
            Text(
                text = "No recent files",
                style = TextStyle(fontSize = 11.sp, color = IdeTextMuted),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyColumn {
                items(recentFiles) { file ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFileSelected(file) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = file.name,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = IdeTextPrimary
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = file.parent ?: "",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = IdeTextMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Live Preview Pane
// ============================================================================

/**
 * Live preview pane that renders document content as styled text.
 */
@Composable
private fun LivePreviewPane(
    content: String,
    format: TextFormat?,
    modifier: Modifier = Modifier
) {
    val previewHtml = remember(content, format) {
        try {
            if (format != null) {
                val parser = ParserRegistry.getParser(format)
                if (parser != null) {
                    val document = parser.parse(content)
                    document.toHtml(lightMode = true)
                } else {
                    "<pre>${content.replace("<", "&lt;").replace(">", "&gt;")}</pre>"
                }
            } else {
                "<pre>${content.replace("<", "&lt;").replace(">", "&gt;")}</pre>"
            }
        } catch (e: Exception) {
            "<pre>Preview error: ${e.message}</pre>"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
    ) {
        // Preview header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(IdeDarkSurfaceVariant)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PREVIEW" + (format?.let { " - ${it.name}" } ?: ""),
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = IdeTextSecondary
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(IdeDarkBorder)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val styledText = remember(previewHtml) {
                htmlToAnnotatedString(previewHtml)
            }
            Text(
                text = styledText,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = IdeTextPrimary,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Converts basic HTML into a styled AnnotatedString.
 */
private fun htmlToAnnotatedString(html: String): AnnotatedString {
    return buildAnnotatedString {
        fun decodeEntities(text: String): String {
            return text
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
        }

        val headingSizes = mapOf(
            1 to 28.sp, 2 to 24.sp, 3 to 20.sp,
            4 to 18.sp, 5 to 16.sp, 6 to 14.sp
        )

        var pos = 0
        val tagStack = mutableListOf<String>()

        val normalized = html
            .replace(Regex("<br\\s*/?>"), "<br>")
            .replace(Regex("<hr\\s*/?>"), "<hr>")

        while (pos < normalized.length) {
            val tagStart = normalized.indexOf('<', pos)

            if (tagStart < 0) {
                append(decodeEntities(normalized.substring(pos)))
                break
            }

            if (tagStart > pos) {
                append(decodeEntities(normalized.substring(pos, tagStart)))
            }

            val tagEnd = normalized.indexOf('>', tagStart)
            if (tagEnd < 0) {
                append(decodeEntities(normalized.substring(tagStart)))
                break
            }

            val tagContent = normalized.substring(tagStart + 1, tagEnd).trim()
            pos = tagEnd + 1

            if (tagContent.startsWith("/")) {
                val tagName = tagContent.substring(1).trim().lowercase()
                tagStack.removeLastOrNull()

                when (tagName) {
                    "p", "div" -> append("\n\n")
                    "h1", "h2", "h3", "h4", "h5", "h6" -> append("\n\n")
                    "li" -> append("\n")
                    "pre" -> append("\n")
                    "ul", "ol" -> append("\n")
                }
                continue
            }

            val tagName = tagContent.split(Regex("\\s+"))[0].lowercase()

            when (tagName) {
                "br" -> append("\n")
                "hr" -> append("\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n")
                "p", "div" -> {
                    if (this.length > 0 && !this.toString().endsWith("\n\n")) {
                        if (!this.toString().endsWith("\n")) append("\n")
                        append("\n")
                    }
                    tagStack.add(tagName)
                }
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val level = tagName[1].digitToInt()
                    if (this.length > 0 && !this.toString().endsWith("\n")) {
                        append("\n")
                    }
                    val startIdx = this.length
                    tagStack.add(tagName)

                    val closingTag = "</$tagName>"
                    val closingIdx = normalized.indexOf(closingTag, pos, ignoreCase = true)
                    if (closingIdx >= 0) {
                        val innerHtml = normalized.substring(pos, closingIdx)
                        val innerText = innerHtml.replace(Regex("<[^>]+>"), "")
                        append(decodeEntities(innerText))
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = headingSizes[level] ?: 14.sp
                            ),
                            start = startIdx,
                            end = this.length
                        )
                        pos = closingIdx + closingTag.length
                        append("\n\n")
                        tagStack.removeLastOrNull()
                    }
                }
                "b", "strong" -> {
                    val startIdx = this.length
                    tagStack.add(tagName)
                    val closingTag = "</$tagName>"
                    val closingIdx = normalized.indexOf(closingTag, pos, ignoreCase = true)
                    if (closingIdx >= 0) {
                        val innerHtml = normalized.substring(pos, closingIdx)
                        val innerText = innerHtml.replace(Regex("<[^>]+>"), "")
                        append(decodeEntities(innerText))
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            start = startIdx,
                            end = this.length
                        )
                        pos = closingIdx + closingTag.length
                        tagStack.removeLastOrNull()
                    }
                }
                "i", "em" -> {
                    val startIdx = this.length
                    tagStack.add(tagName)
                    val closingTag = "</$tagName>"
                    val closingIdx = normalized.indexOf(closingTag, pos, ignoreCase = true)
                    if (closingIdx >= 0) {
                        val innerHtml = normalized.substring(pos, closingIdx)
                        val innerText = innerHtml.replace(Regex("<[^>]+>"), "")
                        append(decodeEntities(innerText))
                        addStyle(
                            SpanStyle(fontStyle = FontStyle.Italic),
                            start = startIdx,
                            end = this.length
                        )
                        pos = closingIdx + closingTag.length
                        tagStack.removeLastOrNull()
                    }
                }
                "code" -> {
                    val startIdx = this.length
                    tagStack.add(tagName)
                    val closingTag = "</code>"
                    val closingIdx = normalized.indexOf(closingTag, pos, ignoreCase = true)
                    if (closingIdx >= 0) {
                        val innerHtml = normalized.substring(pos, closingIdx)
                        val innerText = innerHtml.replace(Regex("<[^>]+>"), "")
                        append(decodeEntities(innerText))
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0xFF333333)
                            ),
                            start = startIdx,
                            end = this.length
                        )
                        pos = closingIdx + closingTag.length
                        tagStack.removeLastOrNull()
                    }
                }
                "s", "del", "strike" -> {
                    val startIdx = this.length
                    tagStack.add(tagName)
                    val closingTag = "</$tagName>"
                    val closingIdx = normalized.indexOf(closingTag, pos, ignoreCase = true)
                    if (closingIdx >= 0) {
                        val innerHtml = normalized.substring(pos, closingIdx)
                        val innerText = innerHtml.replace(Regex("<[^>]+>"), "")
                        append(decodeEntities(innerText))
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.LineThrough),
                            start = startIdx,
                            end = this.length
                        )
                        pos = closingIdx + closingTag.length
                        tagStack.removeLastOrNull()
                    }
                }
                "li" -> {
                    if (this.length > 0 && !this.toString().endsWith("\n")) {
                        append("\n")
                    }
                    append("\u2022 ")
                    tagStack.add(tagName)
                }
                "pre" -> {
                    if (this.length > 0 && !this.toString().endsWith("\n")) {
                        append("\n")
                    }
                    tagStack.add(tagName)
                }
                "ul", "ol" -> {
                    tagStack.add(tagName)
                }
                else -> {
                    if (!tagContent.endsWith("/")) {
                        tagStack.add(tagName)
                    }
                }
            }
        }

        val result = this.toString()
        if (result.endsWith("\n\n\n")) {
            // Cosmetic trimming note
        }
    }
}

// ============================================================================
// Enhanced Editor Screen
// ============================================================================

/**
 * Enhanced editor screen with IDE-style features.
 */
@Composable
private fun EnhancedEditorScreen(
    window: DesktopWindow,
    fileManager: DesktopFileManager,
    appSettings: DesktopSettingsStorage.AppSettings,
    undoManager: UndoManager,
    findText: String = "",
    findMatchPositions: List<Int> = emptyList(),
    findMatchIndex: Int = -1,
    onContentChanged: (String, Boolean) -> Unit,
    onCursorPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var content by remember(window.id) { mutableStateOf(window.content) }

    LaunchedEffect(window.content) {
        if (content != window.content) {
            content = window.content
        }
    }

    Column(modifier = modifier.background(IdeDarkBackground)) {
        // Editor header with format info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(IdeDarkSurfaceVariant)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = window.getDisplayTitle(),
                style = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = IdeTextPrimary
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val detectedFormat = window.format ?: window.file?.let { fileManager.detectFormatFromFile(it) }
                detectedFormat?.let { format ->
                    Text(
                        text = format.name,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = IdeAccent
                        )
                    )
                }
                Text(
                    text = "UTF-8",
                    style = TextStyle(fontSize = 10.sp, color = IdeTextMuted)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(IdeDarkBorder)
        )

        // Enhanced Text Editor
        EnhancedTextEditor(
            content = content,
            onContentChange = { newContent ->
                undoManager.recordState(content)
                content = newContent
                window.content = newContent
                onContentChanged(newContent, true)
            },
            format = window.format ?: window.file?.let { fileManager.detectFormatFromFile(it) },
            appSettings = appSettings,
            findText = findText,
            findMatchPositions = findMatchPositions,
            findMatchIndex = findMatchIndex,
            onCursorPositionChange = { line, column ->
                onCursorPositionChanged(line, column)
            },
            onSelectionChange = { _ -> },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Builds an AnnotatedString with find/replace match highlights.
 */
private fun buildHighlightedText(
    text: String,
    findText: String,
    matchPositions: List<Int>,
    currentMatchIndex: Int
): AnnotatedString {
    if (findText.isEmpty() || matchPositions.isEmpty()) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        append(text)
        val findLen = findText.length
        matchPositions.forEachIndexed { index, pos ->
            val end = (pos + findLen).coerceAtMost(text.length)
            if (pos < text.length) {
                if (index == currentMatchIndex) {
                    addStyle(
                        SpanStyle(background = Color(0xFFFF9800)),
                        start = pos,
                        end = end
                    )
                } else {
                    addStyle(
                        SpanStyle(background = Color(0xFFFFEB3B).copy(alpha = 0.3f)),
                        start = pos,
                        end = end
                    )
                }
            }
        }
    }
}

/**
 * Enhanced text editor with line numbers and IDE styling.
 */
@Composable
private fun EnhancedTextEditor(
    content: String,
    onContentChange: (String) -> Unit,
    format: TextFormat?,
    appSettings: DesktopSettingsStorage.AppSettings,
    findText: String = "",
    findMatchPositions: List<Int> = emptyList(),
    findMatchIndex: Int = -1,
    onCursorPositionChange: (Int, Int) -> Unit,
    onSelectionChange: (IntRange?) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSize = appSettings.appearance.fontSize.sp
    val textStyle = TextStyle(
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        color = IdeTextPrimary,
        lineHeight = (appSettings.appearance.fontSize * 1.5).sp
    )

    val highlightedText = remember(content, findText, findMatchPositions, findMatchIndex) {
        buildHighlightedText(content, findText, findMatchPositions, findMatchIndex)
    }

    var textFieldValue by remember(content, highlightedText) {
        mutableStateOf(
            TextFieldValue(
                annotatedString = highlightedText,
            )
        )
    }

    LaunchedEffect(textFieldValue.selection) {
        val offset = textFieldValue.selection.start
        val textBefore = textFieldValue.text.take(offset)
        val line = textBefore.count { it == '\n' } + 1
        val lastNewline = textBefore.lastIndexOf('\n')
        val column = if (lastNewline >= 0) offset - lastNewline else offset + 1
        onCursorPositionChange(line, column)
    }

    val scrollState = rememberScrollState()

    Row(modifier = modifier.fillMaxSize()) {
        // Line number gutter
        if (appSettings.editor.showLineNumbers) {
            val lineCount = content.count { it == '\n' } + 1
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(52.dp)
                    .background(IdeDarkSurface)
                    .verticalScroll(scrollState)
                    .padding(end = 8.dp, top = 8.dp, start = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = i.toString(),
                            style = textStyle.copy(
                                fontSize = fontSize,
                                color = IdeTextSecondary,
                                lineHeight = (appSettings.appearance.fontSize * 1.5).sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Gutter separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(IdeDarkBorder)
            )
        }

        // Editor field
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                if (newValue.text != textFieldValue.text) {
                    onContentChange(newValue.text)
                }
                textFieldValue = newValue
            },
            textStyle = textStyle,
            cursorBrush = SolidColor(IdeAccent),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(IdeDarkBackground)
                .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                .verticalScroll(scrollState)
                .then(
                    if (!appSettings.editor.wordWrap) Modifier.horizontalScroll(rememberScrollState())
                    else Modifier
                ),
            decorationBox = { innerTextField ->
                Box {
                    if (content.isEmpty()) {
                        Text(
                            text = "Start typing...",
                            style = textStyle.copy(color = IdeTextMuted)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ============================================================================
// IDE Welcome Screen
// ============================================================================

/**
 * Welcome screen with IDE-style layout.
 */
@Composable
private fun IdeWelcomeScreen(
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    recentFiles: List<File>,
    onRecentFileClick: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeDarkBackground)
            .padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Yole",
            style = TextStyle(
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = IdeTextPrimary,
                letterSpacing = 4.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Text Editor",
            style = TextStyle(
                fontSize = 16.sp,
                color = IdeTextSecondary,
                letterSpacing = 2.sp
            )
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IdeWelcomeButton("New File", "Ctrl+N", onClick = onNewFile)
            IdeWelcomeButton("Open File", "Ctrl+O", onClick = onOpenFile)
        }

        if (recentFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "RECENT FILES",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = IdeTextSecondary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.widthIn(max = 400.dp)
            ) {
                items(recentFiles.take(5)) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRecentFileClick(file) }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.name,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = IdeAccent
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = file.parent ?: "",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = IdeTextMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeWelcomeButton(
    label: String,
    shortcut: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(width = 1.dp, color = IdeDarkBorder)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = IdeTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = shortcut,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = IdeTextMuted
                )
            )
        }
    }
}

// ============================================================================
// IDE Enhanced Status Bar
// ============================================================================

/**
 * IDE-style status bar with comprehensive information.
 */
@Composable
private fun IdeEnhancedStatusBar(
    currentWindow: DesktopWindow?,
    fileManager: DesktopFileManager,
    cursorLine: Int = 1,
    cursorColumn: Int = 1,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(24.dp)
            .background(IdeAccent),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - file info
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            currentWindow?.let { window ->
                Text(
                    text = window.getDisplayTitle(),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                )

                val detectedFormat = window.format ?: window.file?.let { fileManager.detectFormatFromFile(it) }
                detectedFormat?.let { format ->
                    Text(
                        text = format.name,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }

                val wordCount = window.content.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                Text(
                    text = "$wordCount words",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            } ?: Text(
                text = "Ready",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            )
        }

        // Right side - cursor position and encoding
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ln $cursorLine, Col $cursorColumn",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            )
            Text(
                text = "UTF-8",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
            Text(
                text = "LF",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}

// ============================================================================
// File Operation Helpers
// ============================================================================

private fun createNewFile(windowManager: DesktopWindowManager, fileManager: DesktopFileManager) {
    val window = windowManager.createWindow()
}

private fun openFile(windowManager: DesktopWindowManager, fileManager: DesktopFileManager) {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.FILES_ONLY
    }

    val result = chooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile
        val content = fileManager.loadFile(file)
        if (content != null) {
            val window = windowManager.createWindow(file, content)
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
            saveCurrentFileAs(window, fileManager)
        }
    }
}

private fun saveCurrentFileAs(currentWindow: DesktopWindow?, fileManager: DesktopFileManager) {
    currentWindow?.let { window ->
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
        }

        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = chooser.selectedFile
            if (fileManager.saveFile(selectedFile, window.content)) {
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
    }
}
