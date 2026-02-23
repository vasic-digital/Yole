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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.*
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
 * Enhanced main application composable with comprehensive desktop functionality.
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
    var showExportMenu by remember { mutableStateOf(false) }

    // Cursor position state tracked from editor
    var cursorLine by remember { mutableStateOf(1) }
    var cursorColumn by remember { mutableStateOf(1) }

    // Find/Replace match tracking
    var findMatchIndex by remember { mutableStateOf(-1) }
    var findMatchCount by remember { mutableStateOf(0) }
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
        findMatchCount = matches.size
        if (matches.isEmpty()) {
            findMatchIndex = -1
            findStatusMessage = "No matches found"
        } else {
            // Move to next match
            findMatchIndex = if (findMatchIndex < 0 || findMatchIndex >= matches.size - 1) 0 else findMatchIndex + 1
            findStatusMessage = "Match ${findMatchIndex + 1} of ${matches.size}"
        }
    }

    // Replace current match handler
    fun performReplace() {
        val window = currentWindow ?: return
        if (findText.isEmpty()) return
        val content = window.content
        val idx = content.indexOf(findText, ignoreCase = true)
        if (idx >= 0) {
            undoManager.recordState(content)
            val newContent = content.substring(0, idx) + replaceText + content.substring(idx + findText.length)
            updateContent(newContent)
            findStatusMessage = "Replaced 1 occurrence"
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

    // Export to PDF handler (writes HTML to file with .pdf extension as a lightweight approach)
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
                // Write HTML content to the PDF file - a proper PDF library would be needed
                // for native PDF output, but this provides a functional export
                val pdfHtml = buildString {
                    append("<!DOCTYPE html>\n<html>\n<head>\n")
                    append("<meta charset=\"UTF-8\">\n")
                    append("<title>${window.getDisplayTitle()}</title>\n")
                    append("<style>@media print { body { margin: 1cm; } }</style>\n")
                    append("</head>\n<body>\n")
                    append(htmlContent)
                    append("\n</body>\n</html>")
                }
                // Save as .html alongside the .pdf for print-to-PDF workflow
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

    // Main application layout with keyboard shortcut handling
    Column(
        modifier = Modifier.fillMaxSize().onKeyEvent { event ->
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
            onAbout = { showAbout = true }
        )

        // Toolbar (toggleable)
        if (showToolbar) {
            DesktopToolbar(
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
                onExportHtml = { exportToHtml() },
                onExportPdf = { exportToPdf() }
            )
        }

        // Main Content
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Sidebar
            DesktopSidebar(
                fileManager = fileManager,
                onFileSelected = { file -> openFileAndSetCurrent(file) },
                modifier = Modifier.width(250.dp)
            )

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
                            onContentChanged = { content, isModified ->
                                windowManager.updateWindowContent(window.id, content, isModified)
                            },
                            onCursorPositionChanged = { line, col ->
                                cursorLine = line
                                cursorColumn = col
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
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
                WelcomeScreen(
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

        // Status Bar (toggleable)
        if (showStatusBar) {
            DesktopStatusBar(
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
            onFindTextChange = { findText = it },
            onReplaceTextChange = { replaceText = it },
            onFind = { performFind() },
            onReplace = { performReplace() },
            onReplaceAll = { performReplaceAll() },
            onClose = {
                showFindDialog = false
                findStatusMessage = ""
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

/**
 * Live preview pane that renders document content as HTML using format parsers.
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
            .padding(16.dp)
    ) {
        Text(
            text = "Preview" + (format?.let { " (${it.name})" } ?: ""),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Render the HTML preview as styled text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Display the HTML content as rendered text.
            // For a full HTML renderer a WebView would be needed; we display
            // a simplified text representation by stripping tags.
            val displayText = remember(previewHtml) {
                stripHtmlTags(previewHtml)
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Strips HTML tags for a simplified text preview.
 * Converts common HTML entities and block elements to readable text.
 */
private fun stripHtmlTags(html: String): String {
    return html
        .replace(Regex("<br\\s*/?>"), "\n")
        .replace(Regex("</?p>"), "\n")
        .replace(Regex("</?div>"), "\n")
        .replace(Regex("<li>"), "\n- ")
        .replace(Regex("<h[1-6][^>]*>"), "\n")
        .replace(Regex("</h[1-6]>"), "\n")
        .replace(Regex("<hr\\s*/?>"), "\n---\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

/**
 * Enhanced editor screen with comprehensive functionality.
 */
@Composable
private fun EnhancedEditorScreen(
    window: DesktopWindow,
    fileManager: DesktopFileManager,
    appSettings: DesktopSettingsStorage.AppSettings,
    undoManager: UndoManager,
    onContentChanged: (String, Boolean) -> Unit,
    onCursorPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var content by remember(window.id) { mutableStateOf(window.content) }

    // Keep content in sync with external changes (e.g., undo/redo)
    LaunchedEffect(window.content) {
        if (content != window.content) {
            content = window.content
        }
    }

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
                val detectedFormat = window.format ?: window.file?.let { fileManager.detectFormatFromFile(it) }
                detectedFormat?.let { format ->
                    AssistChip(
                        onClick = { /* Show format info */ },
                        label = { Text(format.name) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Encoding indicator
                Text(
                    text = "UTF-8",
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
                // Record previous state for undo before applying the change
                undoManager.recordState(content)
                content = newContent
                window.content = newContent
                onContentChanged(newContent, true)
            },
            format = window.format ?: window.file?.let { fileManager.detectFormatFromFile(it) },
            appSettings = appSettings,
            onCursorPositionChange = { line, column ->
                onCursorPositionChanged(line, column)
            },
            onSelectionChange = { _ -> },
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
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface
    )

    // Track text field value for cursor position
    var textFieldValue by remember(content) {
        mutableStateOf(TextFieldValue(content))
    }

    // Compute cursor position (line, column) from selection
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
        // Line numbers column
        if (appSettings.editor.showLineNumbers) {
            val lineCount = content.count { it == '\n' } + 1
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(scrollState)
                    .padding(end = 8.dp, top = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    for (i in 1..lineCount) {
                        Text(
                            text = i.toString(),
                            style = textStyle.copy(
                                fontSize = appSettings.appearance.fontSize.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
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
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
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
    onPreview: () -> Unit = {},
    onExportHtml: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showExportMenu by remember { mutableStateOf(false) }

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

        Button(onClick = onUndo, enabled = currentWindow != null) {
            Text("Undo")
        }

        Button(onClick = onRedo, enabled = currentWindow != null) {
            Text("Redo")
        }

        Divider(
            modifier = Modifier
                .height(24.dp)
                .width(1.dp)
        )

        Button(onClick = onFind, enabled = currentWindow != null) {
            Text("Find")
        }

        Button(onClick = onPreview, enabled = currentWindow != null) {
            Text("Preview")
        }

        Divider(
            modifier = Modifier
                .height(24.dp)
                .width(1.dp)
        )

        // Export dropdown
        Box {
            Button(
                onClick = { showExportMenu = true },
                enabled = currentWindow != null
            ) {
                Text("Export")
            }
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
                text = file.parent ?: "",
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
    cursorLine: Int = 1,
    cursorColumn: Int = 1,
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

                    val detectedFormat = window.format ?: window.file?.let { fileManager.detectFormatFromFile(it) }
                    detectedFormat?.let { format ->
                        Text(
                            text = format.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Word count
                    val wordCount = window.content.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                    Text(
                        text = "$wordCount words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } ?: Text(
                text = "Ready",
                style = MaterialTheme.typography.bodySmall
            )

            // Right side - cursor position and encoding
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Ln $cursorLine, Col $cursorColumn",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "UTF-8",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
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