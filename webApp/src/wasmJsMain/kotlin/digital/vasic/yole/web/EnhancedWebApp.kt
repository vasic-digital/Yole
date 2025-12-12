/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Enhanced Web Application Features
 * PWA integration, advanced file operations, offline support
 *
 *########################################################*/

package digital.vasvasic.yole.web

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.web.PWAFeatures.FileSystemFileHandle
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.*
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.js.Date

/**
 * Enhanced web application with PWA features
 */
@Composable
fun EnhancedYoleWebApp() {
    var documentContent by remember { mutableStateOf("# Welcome to Yole Web\n\nStart writing your document...") }
    var currentFormat by remember { mutableStateOf("markdown") }
    var documentName by remember { mutableStateOf("untitled.md") }
    var isDarkTheme by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(true) }
    var wordWrap by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(14) }
    var showLineNumbers by remember { mutableStateOf(true) }
    
    // PWA state
    var isOffline by remember { mutableStateOf(!window.navigator.onLine) }
    var isStandalone by remember { mutableStateOf(PWAFeatures.isStandaloneMode()) }
    var showInstallPrompt by remember { mutableStateOf(false) }
    
    // Advanced features
    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showGoToLine by remember { mutableStateOf(false) }
    var goToLineNumber by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // Auto-save state
    var lastSaved by remember { mutableStateOf<Date?>(null) }
    var isDirty by remember { mutableStateOf(false) }
    
    // Load saved settings
    LaunchedEffect(Unit) {
        loadSettings()
        setupOfflineDetection()
        checkForInstallPrompt()
    }
    
    // Auto-save functionality
    LaunchedEffect(documentContent) {
        isDirty = true
        delay(2000) // Auto-save after 2 seconds of inactivity
        if (isDirty) {
            saveToLocalStorage()
            lastSaved = Date()
            isDirty = false
        }
    }
    
    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        EnhancedWebAppContent(
            documentContent = documentContent,
            currentFormat = currentFormat,
            documentName = documentName,
            isDarkTheme = isDarkTheme,
            showPreview = showPreview,
            wordWrap = wordWrap,
            fontSize = fontSize,
            showLineNumbers = showLineNumbers,
            isOffline = isOffline,
            isStandalone = isStandalone,
            showInstallPrompt = showInstallPrompt,
            showFindReplace = showFindReplace,
            showGoToLine = showGoToLine,
            showExportDialog = showExportDialog,
            showSettingsDialog = showSettingsDialog,
            lastSaved = lastSaved,
            findText = findText,
            replaceText = replaceText,
            goToLineNumber = goToLineNumber,
            onContentChange = { documentContent = it },
            onFormatChange = { currentFormat = it },
            onDocumentNameChange = { documentName = it },
            onThemeToggle = { isDarkTheme = !isDarkTheme },
            onPreviewToggle = { showPreview = !showPreview },
            onWordWrapToggle = { wordWrap = !wordWrap },
            onFontSizeChange = { fontSize = it },
            onLineNumbersToggle = { showLineNumbers = !showLineNumbers },
            onInstallPromptDismiss = { showInstallPrompt = false },
            onFindReplaceToggle = { showFindReplace = !showFindReplace },
            onGoToLineToggle = { showGoToLine = !showGoToLine },
            onExportToggle = { showExportDialog = !showExportDialog },
            onSettingsToggle = { showSettingsDialog = !showSettingsDialog },
            onFindTextChange = { findText = it },
            onReplaceTextChange = { replaceText = it },
            onGoToLineNumberChange = { goToLineNumber = it },
            onFindNext = { performFindNext() },
            onReplace = { performReplace() },
            onReplaceAll = { performReplaceAll() },
            onGoToLine = { performGoToLine() },
            onExportPdf = { exportAsPdf() },
            onExportHtml = { exportAsHtml() },
            onExportMarkdown = { exportAsMarkdown() },
            onNewFile = { createNewFile() },
            onOpenFile = { openFile() },
            onSaveFile = { saveFile() },
            onSaveAsFile = { saveAsFile() },
            onPrint = { printDocument() },
            onSettingsSave = { saveSettings() }
        )
    }
}

/**
 * Enhanced web app content with all features
 */
@Composable
fun EnhancedWebAppContent(
    documentContent: String,
    currentFormat: String,
    documentName: String,
    isDarkTheme: Boolean,
    showPreview: Boolean,
    wordWrap: Boolean,
    fontSize: Int,
    showLineNumbers: Boolean,
    isOffline: Boolean,
    isStandalone: Boolean,
    showInstallPrompt: Boolean,
    showFindReplace: Boolean,
    showGoToLine: Boolean,
    showExportDialog: Boolean,
    showSettingsDialog: Boolean,
    lastSaved: Date?,
    findText: String,
    replaceText: String,
    goToLineNumber: String,
    onContentChange: (String) -> Unit,
    onFormatChange: (String) -> Unit,
    onDocumentNameChange: (String) -> Unit,
    onThemeToggle: () -> Unit,
    onPreviewToggle: () -> Unit,
    onWordWrapToggle: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineNumbersToggle: () -> Unit,
    onInstallPromptDismiss: () -> Unit,
    onFindReplaceToggle: () -> Unit,
    onGoToLineToggle: () -> Unit,
    onExportToggle: () -> Unit,
    onSettingsToggle: () -> Unit,
    onFindTextChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onGoToLineNumberChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onGoToLine: () -> Unit,
    onExportPdf: () -> Unit,
    onExportHtml: () -> Unit,
    onExportMarkdown: () -> Unit,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onSaveAsFile: () -> Unit,
    onPrint: () -> Unit,
    onSettingsSave: () -> Unit
) {
    Scaffold(
        topBar = {
            EnhancedTopAppBar(
                documentName = documentName,
                isDirty = lastSaved == null,
                isOffline = isOffline,
                isStandalone = isStandalone,
                onNewFile = onNewFile,
                onOpenFile = onOpenFile,
                onSaveFile = onSaveFile,
                onSaveAsFile = onSaveAsFile,
                onPrint = onPrint,
                onFindReplace = onFindReplaceToggle,
                onGoToLine = onGoToLineToggle,
                onExport = onExportToggle,
                onSettings = onSettingsToggle
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main editor area
            Row(
                modifier = Modifier.weight(1f)
            ) {
                // Enhanced editor
                EnhancedEditor(
                    content = documentContent,
                    onContentChange = onContentChange,
                    format = currentFormat,
                    isDarkTheme = isDarkTheme,
                    wordWrap = wordWrap,
                    fontSize = fontSize,
                    showLineNumbers = showLineNumbers,
                    modifier = Modifier.weight(if (showPreview) 1f else 2f)
                )
                
                // Live preview
                if (showPreview) {
                    EnhancedPreview(
                        content = documentContent,
                        format = currentFormat,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Status bar
            EnhancedStatusBar(
                format = currentFormat,
                wordCount = documentContent.split(Regex("\\s+")).filter { it.isNotEmpty() }.size,
                characterCount = documentContent.length,
                lineCount = documentContent.lines().size,
                lastSaved = lastSaved,
                isOffline = isOffline,
                onFormatChange = onFormatChange,
                onThemeToggle = onThemeToggle,
                onPreviewToggle = onPreviewToggle,
                onWordWrapToggle = onWordWrapToggle,
                onFontSizeChange = onFontSizeChange,
                onLineNumbersToggle = onLineNumbersToggle
            )
        }
        
        // Dialogs
        if (showInstallPrompt) {
            InstallPromptDialog(
                onDismiss = onInstallPromptDismiss,
                onInstall = { 
                    CoroutineScope(Dispatchers.Default).launch {
                        PWAFeatures.triggerInstall()
                    }
                }
            )
        }
        
        if (showFindReplace) {
            FindReplaceDialog(
                findText = findText,
                replaceText = replaceText,
                onFindTextChange = onFindTextChange,
                onReplaceTextChange = onReplaceTextChange,
                onFindNext = onFindNext,
                onReplace = onReplace,
                onReplaceAll = onReplaceAll,
                onDismiss = onFindReplaceToggle
            )
        }
        
        if (showGoToLine) {
            GoToLineDialog(
                lineNumber = goToLineNumber,
                onLineNumberChange = onGoToLineNumberChange,
                onGoToLine = onGoToLine,
                onDismiss = onGoToLineToggle
            )
        }
        
        if (showExportDialog) {
            ExportDialog(
                onExportPdf = onExportPdf,
                onExportHtml = onExportHtml,
                onExportMarkdown = onExportMarkdown,
                onDismiss = onExportToggle
            )
        }
        
        if (showSettingsDialog) {
            SettingsDialog(
                isDarkTheme = isDarkTheme,
                wordWrap = wordWrap,
                fontSize = fontSize,
                showLineNumbers = showLineNumbers,
                onThemeToggle = onThemeToggle,
                onWordWrapToggle = onWordWrapToggle,
                onFontSizeChange = onFontSizeChange,
                onLineNumbersToggle = onLineNumbersToggle,
                onSave = onSettingsSave,
                onDismiss = onSettingsToggle
            )
        }
    }
}

/**
 * Enhanced top app bar with all features
 */
@Composable
fun EnhancedTopAppBar(
    documentName: String,
    isDirty: Boolean,
    isOffline: Boolean,
    isStandalone: Boolean,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onSaveAsFile: () -> Unit,
    onPrint: () -> Unit,
    onFindReplace: () -> Unit,
    onGoToLine: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = documentName + if (isDirty) " *" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isOffline) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "Offline",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        actions = {
            // File operations
            IconButton(onClick = onNewFile) {
                Icon(Icons.Default.NoteAdd, contentDescription = "New File")
            }
            IconButton(onClick = onOpenFile) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Open File")
            }
            IconButton(onClick = onSaveFile) {
                Icon(Icons.Default.Save, contentDescription = "Save File")
            }
            
            // Edit operations
            IconButton(onClick = onFindReplace) {
                Icon(Icons.Default.FindReplace, contentDescription = "Find and Replace")
            }
            IconButton(onClick = onGoToLine) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = "Go to Line")
            }
            
            // Export and settings
            IconButton(onClick = onExport) {
                Icon(Icons.Default.IosShare, contentDescription = "Export")
            }
            IconButton(onClick = onPrint) {
                Icon(Icons.Default.Print, contentDescription = "Print")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

/**
 * Enhanced editor with advanced features
 */
@Composable
fun EnhancedEditor(
    content: String,
    onContentChange: (String) -> Unit,
    format: String,
    isDarkTheme: Boolean,
    wordWrap: Boolean,
    fontSize: Int,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF1e1e1e) else Color(0xFFffffff)
    val textColor = if (isDarkTheme) Color(0xFFd4d4d4) else Color(0xFF000000)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Line numbers and editor
        Row(
            modifier = Modifier.weight(1f)
        ) {
            if (showLineNumbers) {
                LineNumbers(
                    content = content,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.width(48.dp)
                )
            }
            
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = textColor,
                    fontSize = fontSize.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    lineHeight = (fontSize * 1.5).sp
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        innerTextField()
                    }
                }
            )
        }
    }
}

/**
 * Line numbers component
 */
@Composable
fun LineNumbers(
    content: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val lineNumbers = content.lines().indices.map { it + 1 }
    val textColor = if (isDarkTheme) Color(0xFF858585) else Color(0xFF666666)
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isDarkTheme) Color(0xFF2d2d2d) else Color(0xFFf5f5f5))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        lineNumbers.forEach { number ->
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

/**
 * Enhanced preview component
 */
@Composable
fun EnhancedPreview(
    content: String,
    format: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF252525) else Color(0xFFfafafa)
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (format) {
                "markdown" -> MarkdownPreview(content, format)
                "todotxt" -> TodoTxtPreview(content, format)
                "csv" -> CsvPreview(content, format)
                else -> PlainTextPreview(content)
            }
        }
    }
}

/**
 * Enhanced status bar
 */
@Composable
fun EnhancedStatusBar(
    format: String,
    wordCount: Int,
    characterCount: Int,
    lineCount: Int,
    lastSaved: Date?,
    isOffline: Boolean,
    onFormatChange: (String) -> Unit,
    onThemeToggle: () -> Unit,
    onPreviewToggle: () -> Unit,
    onWordWrapToggle: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineNumbersToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - document info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("$wordCount words", style = MaterialTheme.typography.labelMedium)
                Text("$characterCount characters", style = MaterialTheme.typography.labelMedium)
                Text("$lineCount lines", style = MaterialTheme.typography.labelMedium)
                lastSaved?.let {
                    Text("Saved: ${it.toLocaleString()}", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            // Right side - controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Format selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    TextField(
                        value = format,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().width(120.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        FormatRegistry.getAllFormats().forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.name) },
                                onClick = {
                                    onFormatChange(format.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Control buttons
                IconButton(onClick = onThemeToggle) {
                    Icon(
                        if (isOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                        contentDescription = if (isOffline) "Offline" else "Online",
                        tint = if (isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = onPreviewToggle) {
                    Icon(Icons.Default.Preview, contentDescription = "Toggle Preview")
                }
                
                IconButton(onClick = onWordWrapToggle) {
                    Icon(Icons.Default.WrapText, contentDescription = "Toggle Word Wrap")
                }
                
                IconButton(onClick = onLineNumbersToggle) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Toggle Line Numbers")
                }
            }
        }
    }
}

// Dialog implementations

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
                        IconButton(onClick = { onFontSizeChange(fontSize - 2) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "${fontSize}px",
                            modifier = Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { onFontSizeChange(fontSize + 2) }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
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

// Helper functions

private fun loadSettings() {
    // Load settings from localStorage
    val settings = localStorage.getItem("yole_web_settings")
    if (settings != null) {
        try {
            val parsed = JSON.parse(settings)
            // Apply settings
        } catch (e: dynamic) {
            console.error("Failed to load settings:", e)
        }
    }
}

private fun saveSettings() {
    val settings = js("({})")
    settings.theme = if (isDarkTheme) "dark" else "light"
    settings.fontSize = fontSize
    settings.wordWrap = wordWrap
    settings.showLineNumbers = showLineNumbers
    localStorage.setItem("yole_web_settings", JSON.stringify(settings))
}

private fun saveToLocalStorage() {
    val state = js("({})")
    state.content = documentContent
    state.format = currentFormat
    state.name = documentName
    state.timestamp = Date().getTime()
    localStorage.setItem("yole_web_state", JSON.stringify(state))
}

private fun setupOfflineDetection() {
    window.addEventListener("online", { 
        isOffline = false
        console.log("Back online")
    })
    window.addEventListener("offline", { 
        isOffline = true
        console.log("Gone offline")
    })
}

private fun checkForInstallPrompt() {
    // Check if we should show install prompt
    if (!PWAFeatures.isStandaloneMode() && localStorage.getItem("install_prompt_shown") != "true") {
        // Show install prompt after some time
        kotlinx.coroutines.delay(30000) // 30 seconds
        showInstallPrompt = true
        localStorage.setItem("install_prompt_shown", "true")
    }
}

private fun createNewFile() {
    documentContent = ""
    documentName = "untitled.${getDefaultExtension()}"
    isDirty = true
}

private fun openFile() {
    CoroutineScope(Dispatchers.Default).launch {
        val fileHandle = PWAFeatures.openFileWithFileSystemAccess()
        fileHandle?.let { handle ->
            val file = handle.getFile()
            val content = file.text().await()
            documentContent = content
            documentName = file.name
            currentFormat = detectFormatFromFilename(file.name)
            isDirty = false
        }
    }
}

private fun saveFile() {
    saveToLocalStorage()
    lastSaved = Date()
    isDirty = false
}

private fun saveAsFile() {
    CoroutineScope(Dispatchers.Default).launch {
        val success = PWAFeatures.saveFileWithFileSystemAccess(
            documentContent,
            documentName
        )
        if (success) {
            isDirty = false
            lastSaved = Date()
        }
    }
}

private fun printDocument() {
    window.print()
}

private fun performFindNext() {
    // Implementation for find next
    console.log("Finding next occurrence of: $findText")
}

private fun performReplace() {
    // Implementation for replace
    console.log("Replacing '$findText' with '$replaceText'")
}

private fun performReplaceAll() {
    // Implementation for replace all
    console.log("Replacing all occurrences of '$findText' with '$replaceText'")
}

private fun performGoToLine() {
    val lineNumber = goToLineNumber.toIntOrNull()
    if (lineNumber != null && lineNumber > 0) {
        // Implementation for go to line
        console.log("Going to line: $lineNumber")
    }
}

private fun exportAsPdf() {
    // Implementation for PDF export
    console.log("Exporting as PDF")
}

private fun exportAsHtml() {
    // Implementation for HTML export
    console.log("Exporting as HTML")
}

private fun exportAsMarkdown() {
    // Implementation for Markdown export
    console.log("Exporting as Markdown")
}

private fun getDefaultExtension(): String {
    return when (currentFormat) {
        "markdown" -> "md"
        "todotxt" -> "txt"
        "csv" -> "csv"
        "latex" -> "tex"
        "orgmode" -> "org"
        "asciidoc" -> "adoc"
        "wikitext" -> "wiki"
        else -> "txt"
    }
}