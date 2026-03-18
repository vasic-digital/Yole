/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Enhanced Web Application Features
 * IDE-style UI with tabs, line numbers, status bar
 *
 *########################################################*/

package digital.vasic.yole.web

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

// Top-level JS helpers for Wasm compatibility.
// In Kotlin/Wasm, js("...") must be the sole expression in a top-level function body
// and must return a JsAny subtype. Conversions happen in wrapper functions.

private fun jsNavigatorOnLineRaw(): kotlin.js.JsBoolean = js("navigator.onLine")
private fun jsNavigatorOnLine(): Boolean = jsNavigatorOnLineRaw().toBoolean()

private fun jsWindowPrintImpl(): kotlin.js.JsAny = js("window.print()")
private fun jsWindowPrint() { jsWindowPrintImpl() }

// --- IDE Theme Colors ---
object IdeColors {
    // Dark theme
    val darkBackground = Color(0xFF1E1E1E)
    val darkSurface = Color(0xFF252526)
    val darkSurfaceVariant = Color(0xFF2D2D30)
    val darkBorder = Color(0xFF3C3C3C)
    val darkAccent = Color(0xFF007ACC)
    val darkText = Color(0xFFD4D4D4)
    val darkTextSecondary = Color(0xFF858585)
    val darkCurrentLine = Color(0xFF2A2D2E)
    val darkStatusBar = Color(0xFF007ACC)
    val darkTabActive = Color(0xFF1E1E1E)
    val darkTabInactive = Color(0xFF2D2D30)
    val darkSidebarBg = Color(0xFF252526)
    val darkActivityBarBg = Color(0xFF333333)
    val darkMenuHover = Color(0xFF2A2D2E)
    val darkDanger = Color(0xFFF44747)

    // Light theme
    val lightBackground = Color(0xFFFFFFFF)
    val lightSurface = Color(0xFFF3F3F3)
    val lightSurfaceVariant = Color(0xFFECECEC)
    val lightBorder = Color(0xFFD4D4D4)
    val lightAccent = Color(0xFF007ACC)
    val lightText = Color(0xFF1E1E1E)
    val lightTextSecondary = Color(0xFF6E6E6E)
    val lightCurrentLine = Color(0xFFF0F0F0)
    val lightStatusBar = Color(0xFF007ACC)
    val lightTabActive = Color(0xFFFFFFFF)
    val lightTabInactive = Color(0xFFECECEC)
    val lightSidebarBg = Color(0xFFF3F3F3)
    val lightActivityBarBg = Color(0xFFDDDDDD)
    val lightMenuHover = Color(0xFFE8E8E8)
    val lightDanger = Color(0xFFD32F2F)
}

/** Represents an open document tab */
data class DocumentTab(
    val id: String,
    val name: String,
    val content: String,
    val format: String,
    val isDirty: Boolean = false,
    val storageKey: String = "yole_doc_$id"
)

/**
 * Enhanced web application with IDE-style UI
 */
@Composable
fun EnhancedYoleWebApp() {
    // Document management state
    var openTabs by remember { mutableStateOf(listOf<DocumentTab>()) }
    var activeTabId by remember { mutableStateOf<String?>(null) }
    val savedSettings = remember { loadSettings() }
    var isDarkTheme by remember { mutableStateOf(savedSettings.isDarkTheme) }
    var showPreview by remember { mutableStateOf(true) }
    var wordWrap by remember { mutableStateOf(savedSettings.wordWrap) }
    var fontSize by remember { mutableStateOf(savedSettings.fontSize) }
    var showLineNumbers by remember { mutableStateOf(savedSettings.showLineNumbers) }
    var showSidebar by remember { mutableStateOf(true) }

    // PWA state
    var isOffline by remember { mutableStateOf(!jsNavigatorOnLine()) }
    var showInstallPrompt by remember { mutableStateOf(false) }

    // Dialog states
    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showGoToLine by remember { mutableStateOf(false) }
    var goToLineNumber by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNewDocDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var newDocFormat by remember { mutableStateOf("markdown") }

    // Auto-save state
    var lastSavedTimestamp by remember { mutableStateOf<String?>(null) }

    // Cursor state for status bar
    var cursorLine by remember { mutableStateOf(1) }
    var cursorCol by remember { mutableStateOf(1) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Derive active tab
    val activeTab = openTabs.find { it.id == activeTabId }

    // Helper: update active tab content
    fun updateActiveContent(newContent: String) {
        openTabs = openTabs.map {
            if (it.id == activeTabId) it.copy(content = newContent, isDirty = true) else it
        }
    }

    // Helper: create new document
    fun createDocument(format: String) {
        val id = Clock.System.now().toEpochMilliseconds().toString()
        val ext = getDefaultExtensionForFormat(format)
        val name = "untitled.$ext"
        val template = when (format) {
            "markdown" -> "# New Document\n\nStart writing..."
            "todotxt" -> "(A) New task @work +project"
            "csv" -> "Name,Email,Phone\nJohn,john@example.com,555-0100"
            "latex" -> "\\documentclass{article}\n\\begin{document}\nHello World\n\\end{document}"
            "orgmode" -> "#+TITLE: New Document\n* Heading\nContent"
            else -> ""
        }
        val newTab = DocumentTab(
            id = id,
            name = name,
            content = template,
            format = format,
            isDirty = true
        )
        openTabs = openTabs + newTab
        activeTabId = id
    }

    // Load saved documents from localStorage on startup
    LaunchedEffect(Unit) {
        loadSettings()
        // Load saved document list
        val docListStr = localStorage.getItem("yole_doc_list")
        if (docListStr != null && docListStr.isNotEmpty()) {
            val docIds = docListStr.split(",").filter { it.isNotEmpty() }
            val loadedTabs = docIds.mapNotNull { docId ->
                val name = localStorage.getItem("yole_doc_${docId}_name") ?: return@mapNotNull null
                val content = localStorage.getItem("yole_doc_${docId}_content") ?: ""
                val format = localStorage.getItem("yole_doc_${docId}_format") ?: "markdown"
                DocumentTab(id = docId, name = name, content = content, format = format)
            }
            if (loadedTabs.isNotEmpty()) {
                openTabs = loadedTabs
                activeTabId = loadedTabs.first().id
            }
        }
        // If no documents, create a welcome document
        if (openTabs.isEmpty()) {
            val welcomeTab = DocumentTab(
                id = "welcome",
                name = "welcome.md",
                content = "# Welcome to Yole\n\nA professional IDE-style text editor.\n\n## Features\n\n- Multi-tab editing\n- 17+ text format support\n- Live preview\n- Offline support (PWA)\n\nCreate a new document or start editing here.",
                format = "markdown"
            )
            openTabs = listOf(welcomeTab)
            activeTabId = "welcome"
        }

        // Set up offline detection
        window.addEventListener("online", { _: org.w3c.dom.events.Event ->
            isOffline = false
            coroutineScope.launch { snackbarHostState.showSnackbar("Back online") }
        })
        window.addEventListener("offline", { _: org.w3c.dom.events.Event ->
            isOffline = true
            coroutineScope.launch { snackbarHostState.showSnackbar("Offline - changes saved locally") }
        })
        if (!PWAFeatures.isStandaloneMode() && localStorage.getItem("install_prompt_shown") != "true") {
            delay(30000)
            showInstallPrompt = true
            localStorage.setItem("install_prompt_shown", "true")
        }
    }

    // Auto-save active document
    LaunchedEffect(activeTab?.content, activeTab?.id) {
        if (activeTab != null && activeTab.isDirty) {
            delay(2000)
            saveDocumentToLocalStorage(activeTab.content, activeTab.format, activeTab.name)
            // Save individual doc
            localStorage.setItem("yole_doc_${activeTab.id}_name", activeTab.name)
            localStorage.setItem("yole_doc_${activeTab.id}_content", activeTab.content)
            localStorage.setItem("yole_doc_${activeTab.id}_format", activeTab.format)
            // Update doc list
            val allIds = openTabs.joinToString(",") { it.id }
            localStorage.setItem("yole_doc_list", allIds)
            lastSavedTimestamp = Clock.System.now().toString()
            openTabs = openTabs.map {
                if (it.id == activeTab.id) it.copy(isDirty = false) else it
            }
        }
    }

    // Track cursor position: updated when content changes.
    // BasicTextField doesn't expose cursor offset directly in KMP/WASM,
    // so we show total lines and document length as the best approximation.
    LaunchedEffect(activeTab?.content) {
        if (activeTab != null) {
            val lines = activeTab.content.lines()
            cursorLine = lines.size
            cursorCol = (lines.lastOrNull()?.length ?: 0) + 1
        }
    }
    // Note: Actual per-keystroke cursor tracking requires TextFieldValue
    // with selection.start, which is wired in the IdeEditor composable.

    // IDE color accessors
    val bg = if (isDarkTheme) IdeColors.darkBackground else IdeColors.lightBackground
    val surface = if (isDarkTheme) IdeColors.darkSurface else IdeColors.lightSurface
    val surfaceVar = if (isDarkTheme) IdeColors.darkSurfaceVariant else IdeColors.lightSurfaceVariant
    val border = if (isDarkTheme) IdeColors.darkBorder else IdeColors.lightBorder
    val accent = if (isDarkTheme) IdeColors.darkAccent else IdeColors.lightAccent
    val text = if (isDarkTheme) IdeColors.darkText else IdeColors.lightText
    val textSecondary = if (isDarkTheme) IdeColors.darkTextSecondary else IdeColors.lightTextSecondary
    val statusBarBg = if (isDarkTheme) IdeColors.darkStatusBar else IdeColors.lightStatusBar
    val tabActive = if (isDarkTheme) IdeColors.darkTabActive else IdeColors.lightTabActive
    val tabInactive = if (isDarkTheme) IdeColors.darkTabInactive else IdeColors.lightTabInactive
    val sidebarBg = if (isDarkTheme) IdeColors.darkSidebarBg else IdeColors.lightSidebarBg
    val lineNumColor = if (isDarkTheme) IdeColors.darkTextSecondary else IdeColors.lightTextSecondary
    val currentLineBg = if (isDarkTheme) IdeColors.darkCurrentLine else IdeColors.lightCurrentLine
    val danger = if (isDarkTheme) IdeColors.darkDanger else IdeColors.lightDanger

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== MENU BAR =====
            IdeMenuBar(
                isDarkTheme = isDarkTheme,
                surface = surface,
                border = border,
                text = text,
                textSecondary = textSecondary,
                accent = accent,
                onNewFile = { showNewDocDialog = true },
                onOpenFile = {
                    CoroutineScope(Dispatchers.Default).launch {
                        val fileHandle = PWAFeatures.openFileWithFileSystemAccess()
                        if (fileHandle != null) {
                            val content = PWAFeatures.getOpenedFileContent()
                            if (content != null) {
                                val format = detectFormatFromFilename(fileHandle.name)
                                val id = Clock.System.now().toEpochMilliseconds().toString()
                                val newTab = DocumentTab(
                                    id = id,
                                    name = fileHandle.name,
                                    content = content,
                                    format = format
                                )
                                openTabs = openTabs + newTab
                                activeTabId = id
                                snackbarHostState.showSnackbar("Opened: ${fileHandle.name}")
                            }
                        }
                    }
                },
                onSaveFile = {
                    activeTab?.let { tab ->
                        saveDocumentToLocalStorage(tab.content, tab.format, tab.name)
                        localStorage.setItem("yole_doc_${tab.id}_name", tab.name)
                        localStorage.setItem("yole_doc_${tab.id}_content", tab.content)
                        localStorage.setItem("yole_doc_${tab.id}_format", tab.format)
                        val allIds = openTabs.joinToString(",") { it.id }
                        localStorage.setItem("yole_doc_list", allIds)
                        lastSavedTimestamp = Clock.System.now().toString()
                        openTabs = openTabs.map {
                            if (it.id == tab.id) it.copy(isDirty = false) else it
                        }
                        coroutineScope.launch { snackbarHostState.showSnackbar("Saved: ${tab.name}") }
                    }
                },
                onSaveAs = {
                    activeTab?.let { tab ->
                        CoroutineScope(Dispatchers.Default).launch {
                            PWAFeatures.saveFileWithFileSystemAccess(tab.content, tab.name)
                        }
                    }
                },
                onTogglePreview = { showPreview = !showPreview },
                onToggleSidebar = { showSidebar = !showSidebar },
                onFindReplace = { showFindReplace = !showFindReplace },
                onGoToLine = { showGoToLine = !showGoToLine },
                onExport = { showExportDialog = !showExportDialog },
                onSettings = { showSettingsDialog = !showSettingsDialog },
                onToggleTheme = { isDarkTheme = !isDarkTheme },
                onPrint = { jsWindowPrint() }
            )

            // ===== MAIN CONTENT AREA =====
            Row(modifier = Modifier.weight(1f)) {
                // ===== FILE EXPLORER SIDEBAR =====
                if (showSidebar) {
                    IdeSidebar(
                        openTabs = openTabs,
                        activeTabId = activeTabId,
                        sidebarBg = sidebarBg,
                        border = border,
                        text = text,
                        textSecondary = textSecondary,
                        accent = accent,
                        isDarkTheme = isDarkTheme,
                        onTabSelected = { activeTabId = it },
                        onNewDocument = { showNewDocDialog = true },
                        onDeleteDocument = { tabId ->
                            val tab = openTabs.find { it.id == tabId }
                            if (tab != null) {
                                localStorage.removeItem("yole_doc_${tab.id}_name")
                                localStorage.removeItem("yole_doc_${tab.id}_content")
                                localStorage.removeItem("yole_doc_${tab.id}_format")
                                openTabs = openTabs.filter { it.id != tabId }
                                if (activeTabId == tabId) {
                                    activeTabId = openTabs.firstOrNull()?.id
                                }
                                val allIds = openTabs.filter { it.id != tabId }.joinToString(",") { it.id }
                                localStorage.setItem("yole_doc_list", allIds)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Deleted: ${tab.name}")
                                }
                            }
                        }
                    )
                }

                // ===== EDITOR AREA =====
                Column(modifier = Modifier.weight(1f)) {
                    // Tab Bar
                    IdeTabBar(
                        openTabs = openTabs,
                        activeTabId = activeTabId,
                        tabActive = tabActive,
                        tabInactive = tabInactive,
                        border = border,
                        text = text,
                        textSecondary = textSecondary,
                        accent = accent,
                        isDarkTheme = isDarkTheme,
                        onTabSelected = { activeTabId = it },
                        onTabClosed = { tabId ->
                            openTabs = openTabs.filter { it.id != tabId }
                            if (activeTabId == tabId) {
                                activeTabId = openTabs.firstOrNull()?.id
                            }
                        }
                    )

                    // Editor + Preview
                    Row(modifier = Modifier.weight(1f)) {
                        if (activeTab != null) {
                            // Editor with line numbers
                            IdeEditor(
                                content = activeTab.content,
                                onContentChange = { updateActiveContent(it) },
                                fontSize = fontSize,
                                showLineNumbers = showLineNumbers,
                                isDarkTheme = isDarkTheme,
                                bg = bg,
                                text = text,
                                lineNumColor = lineNumColor,
                                currentLineBg = currentLineBg,
                                border = border,
                                onCursorPositionChange = { line, col ->
                                    cursorLine = line
                                    cursorCol = col
                                },
                                modifier = Modifier.weight(if (showPreview) 1f else 2f)
                            )

                            // Vertical divider
                            if (showPreview) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .background(border)
                                )
                            }

                            // Preview panel
                            if (showPreview) {
                                IdePreview(
                                    content = activeTab.content,
                                    format = activeTab.format,
                                    isDarkTheme = isDarkTheme,
                                    surface = surface,
                                    text = text,
                                    textSecondary = textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Yole",
                                        color = textSecondary.copy(alpha = 0.3f),
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Create or open a document to begin",
                                        color = textSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== STATUS BAR =====
            IdeStatusBar(
                activeTab = activeTab,
                cursorLine = cursorLine,
                cursorCol = cursorCol,
                isOffline = isOffline,
                lastSavedTimestamp = lastSavedTimestamp,
                statusBarBg = statusBarBg,
                text = Color.White,
                onFormatChange = { newFormat ->
                    activeTab?.let { tab ->
                        openTabs = openTabs.map {
                            if (it.id == tab.id) it.copy(format = newFormat) else it
                        }
                    }
                },
                onTogglePreview = { showPreview = !showPreview },
                onToggleWordWrap = { wordWrap = !wordWrap },
                onToggleLineNumbers = { showLineNumbers = !showLineNumbers },
                showPreview = showPreview,
                wordWrap = wordWrap,
                showLineNumbers = showLineNumbers
            )
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        )

        // ===== DIALOGS =====

        if (showInstallPrompt) {
            InstallPromptDialog(
                onDismiss = { showInstallPrompt = false },
                onInstall = {
                    CoroutineScope(Dispatchers.Default).launch {
                        PWAFeatures.triggerInstall()
                    }
                }
            )
        }

        if (showNewDocDialog) {
            NewDocumentDialog(
                selectedFormat = newDocFormat,
                onFormatSelected = { newDocFormat = it },
                isDarkTheme = isDarkTheme,
                onConfirm = {
                    createDocument(newDocFormat)
                    showNewDocDialog = false
                    coroutineScope.launch { snackbarHostState.showSnackbar("New ${newDocFormat} document created") }
                },
                onDismiss = { showNewDocDialog = false }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Document") },
                text = { Text("Are you sure you want to delete \"${activeTab?.name}\"? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            activeTab?.let { tab ->
                                localStorage.removeItem("yole_doc_${tab.id}_name")
                                localStorage.removeItem("yole_doc_${tab.id}_content")
                                localStorage.removeItem("yole_doc_${tab.id}_format")
                                openTabs = openTabs.filter { it.id != tab.id }
                                activeTabId = openTabs.firstOrNull()?.id
                                val allIds = openTabs.joinToString(",") { it.id }
                                localStorage.setItem("yole_doc_list", allIds)
                            }
                            showDeleteConfirm = false
                            coroutineScope.launch { snackbarHostState.showSnackbar("Document deleted") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }

        if (showFindReplace) {
            FindReplaceDialog(
                findText = findText,
                replaceText = replaceText,
                onFindTextChange = { findText = it },
                onReplaceTextChange = { replaceText = it },
                onFindNext = {
                    if (findText.isNotEmpty() && activeTab != null) {
                        val count = activeTab.content.windowed(findText.length, 1)
                            .count { it.equals(findText, ignoreCase = true) }
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                if (count > 0) "$count match(es) found" else "No matches found"
                            )
                        }
                    }
                },
                onReplace = {
                    if (findText.isNotEmpty() && activeTab != null) {
                        val idx = activeTab.content.indexOf(findText, ignoreCase = true)
                        if (idx != -1) {
                            val newContent = activeTab.content.substring(0, idx) +
                                replaceText +
                                activeTab.content.substring(idx + findText.length)
                            updateActiveContent(newContent)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Replaced one occurrence") }
                        }
                    }
                },
                onReplaceAll = {
                    if (findText.isNotEmpty() && activeTab != null) {
                        val count = activeTab.content.windowed(findText.length, 1)
                            .count { it.equals(findText, ignoreCase = true) }
                        if (count > 0) {
                            updateActiveContent(activeTab.content.replace(findText, replaceText, ignoreCase = true))
                            coroutineScope.launch { snackbarHostState.showSnackbar("Replaced $count occurrence(s)") }
                        }
                    }
                },
                onDismiss = { showFindReplace = false }
            )
        }

        if (showGoToLine) {
            GoToLineDialog(
                lineNumber = goToLineNumber,
                onLineNumberChange = { goToLineNumber = it },
                onGoToLine = {
                    val line = goToLineNumber.toIntOrNull()
                    if (line != null && activeTab != null) {
                        val totalLines = activeTab.content.lines().size
                        coroutineScope.launch {
                            if (line in 1..totalLines) {
                                snackbarHostState.showSnackbar("Jumped to line $line")
                            } else {
                                snackbarHostState.showSnackbar("Line $line out of range (1-$totalLines)")
                            }
                        }
                    }
                    showGoToLine = false
                },
                onDismiss = { showGoToLine = false }
            )
        }

        if (showExportDialog) {
            ExportDialog(
                onExportPdf = {
                    jsWindowPrint()
                    showExportDialog = false
                    coroutineScope.launch { snackbarHostState.showSnackbar("Print dialog opened (save as PDF)") }
                },
                onExportHtml = {
                    activeTab?.let { tab ->
                        try {
                            val parser = ParserRegistry.getParser(tab.format)
                            val htmlContent = if (parser != null) {
                                val doc = parser.parse(tab.content)
                                val body = parser.toHtml(doc)
                                "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<title>${tab.name}</title>\n</head>\n<body>\n$body\n</body>\n</html>"
                            } else {
                                "<pre>${tab.content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</pre>"
                            }
                            val htmlFilename = tab.name.substringBeforeLast(".") + ".html"
                            downloadFile(htmlContent, htmlFilename)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Exported: $htmlFilename") }
                        } catch (e: Exception) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Export failed: ${e.message}") }
                        }
                    }
                    showExportDialog = false
                },
                onExportMarkdown = {
                    activeTab?.let { tab ->
                        val mdFilename = tab.name.substringBeforeLast(".") + ".md"
                        downloadFile(tab.content, mdFilename)
                        coroutineScope.launch { snackbarHostState.showSnackbar("Exported: $mdFilename") }
                    }
                    showExportDialog = false
                },
                onDismiss = { showExportDialog = false }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                isDarkTheme = isDarkTheme,
                wordWrap = wordWrap,
                fontSize = fontSize,
                showLineNumbers = showLineNumbers,
                onThemeToggle = { isDarkTheme = !isDarkTheme },
                onWordWrapToggle = { wordWrap = !wordWrap },
                onFontSizeChange = { fontSize = it },
                onLineNumbersToggle = { showLineNumbers = !showLineNumbers },
                onSave = {
                    saveSettingsToLocalStorage(isDarkTheme, fontSize, wordWrap, showLineNumbers)
                    coroutineScope.launch { snackbarHostState.showSnackbar("Settings saved") }
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

// ===== IDE MENU BAR =====

@Composable
fun IdeMenuBar(
    isDarkTheme: Boolean,
    surface: Color,
    border: Color,
    text: Color,
    textSecondary: Color,
    accent: Color,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onSaveAs: () -> Unit,
    onTogglePreview: () -> Unit,
    onToggleSidebar: () -> Unit,
    onFindReplace: () -> Unit,
    onGoToLine: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    onPrint: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // App title
        Text(
            "Yole",
            color = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .padding(end = 12.dp)
                .semantics { contentDescription = "Yole editor application" }
        )

        // Menu items as compact text buttons
        IdeMenuButton("File", text, isDarkTheme, onNewFile)
        IdeMenuButton("Open", text, isDarkTheme, onOpenFile)
        IdeMenuButton("Save", text, isDarkTheme, onSaveFile)
        IdeMenuButton("SaveAs", text, isDarkTheme, onSaveAs)

        Box(modifier = Modifier.width(1.dp).height(16.dp).background(border))

        IdeMenuButton("Find", text, isDarkTheme, onFindReplace)
        IdeMenuButton("GoTo", text, isDarkTheme, onGoToLine)

        Box(modifier = Modifier.width(1.dp).height(16.dp).background(border))

        IdeMenuButton("Preview", text, isDarkTheme, onTogglePreview)
        IdeMenuButton("Explorer", text, isDarkTheme, onToggleSidebar)

        Box(modifier = Modifier.width(1.dp).height(16.dp).background(border))

        IdeMenuButton("Export", text, isDarkTheme, onExport)
        IdeMenuButton("Print", text, isDarkTheme, onPrint)

        Spacer(modifier = Modifier.weight(1f))

        // Right side: theme toggle and settings
        IdeMenuButton(
            if (isDarkTheme) "Light" else "Dark",
            text,
            isDarkTheme,
            onToggleTheme
        )
        IdeMenuButton("Settings", text, isDarkTheme, onSettings)
    }

    // Bottom border
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(border))
}

@Composable
fun IdeMenuButton(label: String, textColor: Color, isDarkTheme: Boolean, onClick: () -> Unit) {
    val hoverBg = if (isDarkTheme) IdeColors.darkMenuHover else IdeColors.lightMenuHover
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .semantics { contentDescription = "$label button" }
    ) {
        Text(
            label,
            color = textColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

// ===== FILE EXPLORER SIDEBAR =====

@Composable
fun IdeSidebar(
    openTabs: List<DocumentTab>,
    activeTabId: String?,
    sidebarBg: Color,
    border: Color,
    text: Color,
    textSecondary: Color,
    accent: Color,
    isDarkTheme: Boolean,
    onTabSelected: (String) -> Unit,
    onNewDocument: () -> Unit,
    onDeleteDocument: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(sidebarBg)
    ) {
        // Sidebar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "EXPLORER",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .clickable(onClick = onNewDocument)
                    .padding(4.dp)
                    .semantics { contentDescription = "New document" }
            ) {
                Text("+", color = textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Border
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(border))

        // Section: Open Documents
        Text(
            "OPEN DOCUMENTS",
            color = textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // Document list
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            openTabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                val itemBg = if (isActive) accent.copy(alpha = 0.2f) else Color.Transparent
                val itemText = if (isActive) text else textSecondary

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTabSelected(tab.id) }
                        .background(itemBg)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Format icon
                        Text(
                            getFormatIcon(tab.format),
                            color = getFormatColor(tab.format),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            tab.name + if (tab.isDirty) " *" else "",
                            color = itemText,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    // Delete button
                    Box(
                        modifier = Modifier
                            .clickable { onDeleteDocument(tab.id) }
                            .padding(2.dp)
                            .semantics { contentDescription = "Delete ${tab.name}" }
                    ) {
                        Text(
                            "x",
                            color = textSecondary.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Bottom: storage info
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "localStorage",
                color = textSecondary,
                fontSize = 10.sp
            )
            Text(
                "${openTabs.size} docs",
                color = textSecondary,
                fontSize = 10.sp
            )
        }
    }

    // Right border
    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(border))
}

// ===== TAB BAR =====

@Composable
fun IdeTabBar(
    openTabs: List<DocumentTab>,
    activeTabId: String?,
    tabActive: Color,
    tabInactive: Color,
    border: Color,
    text: Color,
    textSecondary: Color,
    accent: Color,
    isDarkTheme: Boolean,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(if (isDarkTheme) IdeColors.darkSurfaceVariant else IdeColors.lightSurfaceVariant)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom
        ) {
            openTabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                val tabBg = if (isActive) tabActive else tabInactive
                val tabText = if (isActive) text else textSecondary

                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clickable { onTabSelected(tab.id) }
                        .background(tabBg)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        getFormatIcon(tab.format),
                        color = getFormatColor(tab.format),
                        fontSize = 11.sp
                    )
                    Text(
                        tab.name + if (tab.isDirty) " *" else "",
                        color = tabText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.semantics { contentDescription = "Tab: ${tab.name}" }
                    )
                    // Close button
                    Box(
                        modifier = Modifier
                            .clickable { onTabClosed(tab.id) }
                            .padding(start = 4.dp)
                            .semantics { contentDescription = "Close tab ${tab.name}" }
                    ) {
                        Text(
                            "x",
                            color = textSecondary.copy(alpha = if (isActive) 0.7f else 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Tab separator
                if (!isActive) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(border)
                    )
                }
            }
        }

        // Active tab indicator line
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(border))
    }
}

// ===== IDE EDITOR =====

@Composable
fun IdeEditor(
    content: String,
    onContentChange: (String) -> Unit,
    fontSize: Int,
    showLineNumbers: Boolean,
    isDarkTheme: Boolean,
    bg: Color,
    text: Color,
    lineNumColor: Color,
    currentLineBg: Color,
    border: Color,
    onCursorPositionChange: ((line: Int, col: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lines = content.lines()
    val editorScrollState = rememberScrollState()
    var textFieldValue by remember(content) {
        mutableStateOf(TextFieldValue(content, TextRange(content.length)))
    }

    Row(
        modifier = modifier
            .fillMaxHeight()
            .background(bg)
    ) {
        // Line number gutter
        if (showLineNumbers) {
            val gutterBg = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF8F8F8)
            val gutterWidth = when {
                lines.size >= 1000 -> 56.dp
                lines.size >= 100 -> 48.dp
                else -> 40.dp
            }

            Column(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(gutterBg)
                    .verticalScroll(editorScrollState)
                    .padding(top = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                lines.forEachIndexed { index, _ ->
                    Text(
                        text = "${index + 1}",
                        color = lineNumColor,
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = (fontSize * 1.5).sp,
                        modifier = Modifier.semantics {
                            contentDescription = "Line ${index + 1}"
                        }
                    )
                }
            }

            // Gutter border
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(border)
            )
        }

        // Text editor
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                if (newValue.text != content) {
                    onContentChange(newValue.text)
                }
                // Report cursor position
                val pos = newValue.selection.start
                val textBefore = newValue.text.take(pos)
                val curLine = textBefore.count { it == '\n' } + 1
                val lastNewline = textBefore.lastIndexOf('\n')
                val curCol = if (lastNewline >= 0) pos - lastNewline else pos + 1
                onCursorPositionChange?.invoke(curLine, curCol)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(editorScrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { contentDescription = "Code editor" },
            textStyle = TextStyle(
                color = text,
                fontSize = fontSize.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = (fontSize * 1.5).sp
            ),
            decorationBox = @Composable { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (content.isEmpty()) {
                        Text(
                            "Start typing...",
                            color = lineNumColor.copy(alpha = 0.5f),
                            fontSize = fontSize.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ===== IDE PREVIEW =====

@Composable
fun IdePreview(
    content: String,
    format: String,
    isDarkTheme: Boolean,
    surface: Color,
    text: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(surface)
    ) {
        // Preview header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PREVIEW",
                color = textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (isDarkTheme) IdeColors.darkBorder else IdeColors.lightBorder)
        )

        // Preview content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (format) {
                "markdown" -> MarkdownPreview(content, format)
                "todotxt" -> TodoTxtPreviewEnhanced(content, text, textSecondary)
                "csv" -> CsvPreviewEnhanced(content, text)
                else -> PlainTextPreviewEnhanced(content, text)
            }
        }
    }
}

@Composable
private fun TodoTxtPreviewEnhanced(content: String, text: Color, textSecondary: Color) {
    content.lines().forEach { line ->
        if (line.isNotBlank()) {
            val isDone = line.trimStart().startsWith("x ")
            Text(
                text = line,
                style = TextStyle(
                    color = if (isDone) textSecondary else text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CsvPreviewEnhanced(content: String, text: Color) {
    content.lines().forEach { line ->
        if (line.isNotBlank()) {
            Text(
                text = line,
                style = TextStyle(
                    color = text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun PlainTextPreviewEnhanced(content: String, text: Color) {
    Text(
        text = content,
        style = TextStyle(
            color = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    )
}

// ===== IDE STATUS BAR =====

@Composable
fun IdeStatusBar(
    activeTab: DocumentTab?,
    cursorLine: Int,
    cursorCol: Int,
    isOffline: Boolean,
    lastSavedTimestamp: String?,
    statusBarBg: Color,
    text: Color,
    onFormatChange: (String) -> Unit,
    onTogglePreview: () -> Unit,
    onToggleWordWrap: () -> Unit,
    onToggleLineNumbers: () -> Unit,
    showPreview: Boolean,
    wordWrap: Boolean,
    showLineNumbers: Boolean
) {
    val content = activeTab?.content ?: ""
    val wordCount = content.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    val lineCount = content.lines().size
    val charCount = content.length

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(statusBarBg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection status
            Text(
                if (isOffline) "Offline" else "Online",
                color = text,
                fontSize = 11.sp
            )

            // Storage indicator
            Text(
                "localStorage",
                color = text.copy(alpha = 0.8f),
                fontSize = 11.sp
            )

            if (lastSavedTimestamp != null) {
                Text(
                    "Saved",
                    color = text.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }

        // Right side
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeTab != null) {
                // Cursor position
                Text(
                    "Ln $cursorLine, Col $cursorCol",
                    color = text,
                    fontSize = 11.sp,
                    modifier = Modifier.semantics {
                        contentDescription = "Line $cursorLine, Column $cursorCol"
                    }
                )

                // Word count
                Text(
                    "$wordCount words",
                    color = text.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )

                // Encoding
                Text(
                    "UTF-8",
                    color = text,
                    fontSize = 11.sp
                )

                // Format
                Box(
                    modifier = Modifier
                        .clickable { /* format dropdown would go here */ }
                        .semantics { contentDescription = "Format: ${activeTab.format}" }
                ) {
                    Text(
                        activeTab.format.replaceFirstChar { it.uppercase() },
                        color = text,
                        fontSize = 11.sp
                    )
                }

                // Toggle buttons
                StatusBarToggle("Preview", showPreview, text, onTogglePreview)
                StatusBarToggle("Wrap", wordWrap, text, onToggleWordWrap)
                StatusBarToggle("Ln#", showLineNumbers, text, onToggleLineNumbers)
            }
        }
    }
}

@Composable
private fun StatusBarToggle(label: String, enabled: Boolean, text: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
            .semantics { contentDescription = "$label: ${if (enabled) "on" else "off"}" }
    ) {
        Text(
            label,
            color = if (enabled) text else text.copy(alpha = 0.4f),
            fontSize = 11.sp
        )
    }
}

// ===== NEW DOCUMENT DIALOG =====

@Composable
fun NewDocumentDialog(
    selectedFormat: String,
    onFormatSelected: (String) -> Unit,
    isDarkTheme: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formats = listOf(
        "markdown" to "Markdown (.md)",
        "plaintext" to "Plain Text (.txt)",
        "todotxt" to "Todo.txt (.txt)",
        "csv" to "CSV (.csv)",
        "latex" to "LaTeX (.tex)",
        "orgmode" to "Org Mode (.org)",
        "asciidoc" to "AsciiDoc (.adoc)",
        "wikitext" to "WikiText (.wiki)",
        "restructuredtext" to "reStructuredText (.rst)",
        "rmarkdown" to "R Markdown (.rmd)",
        "taskpaper" to "TaskPaper (.taskpaper)",
        "textile" to "Textile (.textile)",
        "creole" to "Creole (.creole)",
        "tiddlywiki" to "TiddlyWiki (.tid)",
        "jupyter" to "Jupyter (.ipynb)",
        "keyvalue" to "Key-Value (.properties)",
        "binary" to "Binary (.bin)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Document") },
        text = {
            Column(
                modifier = Modifier.height(300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                formats.forEach { (id, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFormatSelected(id) }
                            .background(
                                if (selectedFormat == id) Color(0xFF007ACC).copy(alpha = 0.2f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(getFormatIcon(id), fontSize = 12.sp)
                        Text(label, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Dialog implementations (reused from original)

@Composable
fun InstallPromptDialog(
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install Yole") },
        text = {
            Text("Install Yole as a Progressive Web App for offline access and better performance.")
        },
        confirmButton = {
            Button(onClick = onInstall) {
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

@Composable
fun FindReplaceDialog(
    findText: String,
    replaceText: String,
    onFindTextChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find and Replace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = findText,
                    onValueChange = onFindTextChange,
                    label = { Text("Find") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = onReplaceTextChange,
                    label = { Text("Replace with") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onFindNext) {
                    Text("Find Next")
                }
                Button(onClick = onReplace) {
                    Text("Replace")
                }
                Button(onClick = onReplaceAll) {
                    Text("Replace All")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun GoToLineDialog(
    lineNumber: String,
    onLineNumberChange: (String) -> Unit,
    onGoToLine: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Line") },
        text = {
            OutlinedTextField(
                value = lineNumber,
                onValueChange = onLineNumberChange,
                label = { Text("Line number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = onGoToLine) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportDialog(
    onExportPdf: () -> Unit,
    onExportHtml: () -> Unit,
    onExportMarkdown: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Document") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onExportPdf) {
                    Text("Export as PDF")
                }
                TextButton(onClick = onExportHtml) {
                    Text("Export as HTML")
                }
                TextButton(onClick = onExportMarkdown) {
                    Text("Export as Markdown")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SettingsDialog(
    isDarkTheme: Boolean,
    wordWrap: Boolean,
    fontSize: Int,
    showLineNumbers: Boolean,
    onThemeToggle: () -> Unit,
    onWordWrapToggle: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineNumbersToggle: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dark Theme", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onThemeToggle() }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Word Wrap", modifier = Modifier.weight(1f))
                    Switch(
                        checked = wordWrap,
                        onCheckedChange = { onWordWrapToggle() }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Line Numbers", modifier = Modifier.weight(1f))
                    Switch(
                        checked = showLineNumbers,
                        onCheckedChange = { onLineNumbersToggle() }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Font Size", modifier = Modifier.weight(1f))
                    Row {
                        TextButton(onClick = { onFontSizeChange(fontSize - 2) }) {
                            Text("-")
                        }
                        Text(
                            text = "${fontSize}px",
                            modifier = Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = { onFontSizeChange(fontSize + 2) }) {
                            Text("+")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ===== HELPER FUNCTIONS =====

private fun getFormatIcon(format: String): String {
    return when (format) {
        "markdown" -> "M"
        "todotxt" -> "T"
        "csv" -> "C"
        "latex" -> "L"
        "orgmode" -> "O"
        "asciidoc" -> "A"
        "wikitext" -> "W"
        "restructuredtext" -> "R"
        "rmarkdown" -> "R"
        "taskpaper" -> "P"
        "textile" -> "X"
        "creole" -> "E"
        "tiddlywiki" -> "D"
        "jupyter" -> "J"
        "keyvalue" -> "K"
        "binary" -> "B"
        "plaintext" -> "T"
        else -> "F"
    }
}

private fun getFormatColor(format: String): Color {
    return when (format) {
        "markdown" -> Color(0xFF569CD6)
        "todotxt" -> Color(0xFF4EC9B0)
        "csv" -> Color(0xFFDCDCAA)
        "latex" -> Color(0xFFC586C0)
        "orgmode" -> Color(0xFF77AA99)
        "asciidoc" -> Color(0xFFCE9178)
        "wikitext" -> Color(0xFFB5CEA8)
        "jupyter" -> Color(0xFFE06C75)
        else -> Color(0xFFD4D4D4)
    }
}

data class SavedSettings(
    val isDarkTheme: Boolean = true,
    val fontSize: Int = 14,
    val wordWrap: Boolean = true,
    val showLineNumbers: Boolean = true
)

private fun loadSettings(): SavedSettings {
    val settings = localStorage.getItem("yole_web_settings")
    if (settings != null) {
        try {
            val dark = settings.contains("\"theme\":\"dark\"")
            val fs = Regex("\"fontSize\":(\\d+)").find(settings)
                ?.groupValues?.get(1)?.toIntOrNull() ?: 14
            val ww = settings.contains("\"wordWrap\":true")
            val ln = settings.contains("\"showLineNumbers\":true")
            return SavedSettings(isDarkTheme = dark, fontSize = fs.coerceIn(8, 32), wordWrap = ww, showLineNumbers = ln)
        } catch (e: Exception) {
            println("ERROR: Failed to load settings: ${e.message}")
        }
    }
    return SavedSettings()
}

private fun saveSettingsToLocalStorage(isDarkTheme: Boolean, fontSize: Int, wordWrap: Boolean, showLineNumbers: Boolean) {
    val theme = if (isDarkTheme) "dark" else "light"
    val settingsJson = """{"theme":"$theme","fontSize":$fontSize,"wordWrap":$wordWrap,"showLineNumbers":$showLineNumbers}"""
    localStorage.setItem("yole_web_settings", settingsJson)
}

private fun saveDocumentToLocalStorage(content: String, format: String, name: String) {
    val timestamp = Clock.System.now().toEpochMilliseconds()
    localStorage.setItem("yole_web_state_content", content)
    localStorage.setItem("yole_web_state_format", format)
    localStorage.setItem("yole_web_state_name", name)
    localStorage.setItem("yole_web_state_timestamp", timestamp.toString())
}

private fun getDefaultExtensionForFormat(formatId: String): String {
    return when (formatId) {
        "markdown" -> "md"
        "todotxt" -> "txt"
        "csv" -> "csv"
        "latex" -> "tex"
        "orgmode" -> "org"
        "asciidoc" -> "adoc"
        "wikitext" -> "wiki"
        "restructuredtext" -> "rst"
        "rmarkdown" -> "rmd"
        "taskpaper" -> "taskpaper"
        "textile" -> "textile"
        "creole" -> "creole"
        "tiddlywiki" -> "tid"
        "jupyter" -> "ipynb"
        "keyvalue" -> "properties"
        "binary" -> "bin"
        "plaintext" -> "txt"
        else -> "txt"
    }
}
