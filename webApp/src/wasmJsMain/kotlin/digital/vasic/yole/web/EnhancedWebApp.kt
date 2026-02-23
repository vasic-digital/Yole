/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Enhanced Web Application Features
 * PWA integration, advanced file operations, offline support
 *
 *########################################################*/

package digital.vasic.yole.web

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.vasic.yole.format.FormatRegistry
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
    var isOffline by remember { mutableStateOf(!jsNavigatorOnLine()) }
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
    var lastSavedTimestamp by remember { mutableStateOf<String?>(null) }
    var isDirty by remember { mutableStateOf(false) }

    // Load saved settings
    LaunchedEffect(Unit) {
        loadSettings()
        // Set up offline detection via event listeners
        window.addEventListener("online", { _: org.w3c.dom.events.Event ->
            isOffline = false
            println("Back online")
        })
        window.addEventListener("offline", { _: org.w3c.dom.events.Event ->
            isOffline = true
            println("Gone offline")
        })
        // Check for install prompt
        if (!PWAFeatures.isStandaloneMode() && localStorage.getItem("install_prompt_shown") != "true") {
            delay(30000) // 30 seconds
            showInstallPrompt = true
            localStorage.setItem("install_prompt_shown", "true")
        }
    }

    // Auto-save functionality
    LaunchedEffect(documentContent) {
        isDirty = true
        delay(2000) // Auto-save after 2 seconds of inactivity
        if (isDirty) {
            saveDocumentToLocalStorage(documentContent, currentFormat, documentName)
            lastSavedTimestamp = Clock.System.now().toString()
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
            lastSavedTimestamp = lastSavedTimestamp,
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
            onFindNext = { println("Finding next occurrence of: $findText") },
            onReplace = { println("Replacing '$findText' with '$replaceText'") },
            onReplaceAll = { println("Replacing all occurrences of '$findText' with '$replaceText'") },
            onGoToLine = {
                val line = goToLineNumber.toIntOrNull()
                if (line != null && line > 0) {
                    println("Going to line: $line")
                }
            },
            onExportPdf = { println("Exporting as PDF") },
            onExportHtml = { println("Exporting as HTML") },
            onExportMarkdown = { println("Exporting as Markdown") },
            onNewFile = {
                documentContent = ""
                documentName = "untitled.${getDefaultExtensionForFormat(currentFormat)}"
                isDirty = true
            },
            onOpenFile = {
                CoroutineScope(Dispatchers.Default).launch {
                    val fileHandle = PWAFeatures.openFileWithFileSystemAccess()
                    if (fileHandle != null) {
                        println("File opened: ${fileHandle.name}")
                        // TODO: Read file content when File System Access API is available in Wasm
                    }
                }
            },
            onSaveFile = {
                saveDocumentToLocalStorage(documentContent, currentFormat, documentName)
                lastSavedTimestamp = Clock.System.now().toString()
                isDirty = false
            },
            onSaveAsFile = {
                CoroutineScope(Dispatchers.Default).launch {
                    val success = PWAFeatures.saveFileWithFileSystemAccess(
                        documentContent,
                        documentName
                    )
                    if (success) {
                        isDirty = false
                        lastSavedTimestamp = Clock.System.now().toString()
                    }
                }
            },
            onPrint = { jsWindowPrint() },
            onSettingsSave = {
                saveSettingsToLocalStorage(isDarkTheme, fontSize, wordWrap, showLineNumbers)
            }
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
    lastSavedTimestamp: String?,
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
                isDirty = lastSavedTimestamp == null,
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
                lastSavedTimestamp = lastSavedTimestamp,
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
@OptIn(ExperimentalMaterial3Api::class)
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
            // File operations - using text buttons since material-icons-extended is not available in Wasm
            TextButton(onClick = onNewFile) {
                Text("New")
            }
            TextButton(onClick = onOpenFile) {
                Text("Open")
            }
            TextButton(onClick = onSaveFile) {
                Text("Save")
            }

            // Edit operations
            TextButton(onClick = onFindReplace) {
                Text("Find")
            }
            TextButton(onClick = onGoToLine) {
                Text("GoTo")
            }

            // Export and settings
            TextButton(onClick = onExport) {
                Text("Export")
            }
            TextButton(onClick = onPrint) {
                Text("Print")
            }
            TextButton(onClick = onSettings) {
                Text("Settings")
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
                decorationBox = @Composable { innerTextField ->
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
                "todotxt" -> TodoTxtPreviewEnhanced(content)
                "csv" -> CsvPreviewEnhanced(content)
                else -> PlainTextPreviewEnhanced(content)
            }
        }
    }
}

/**
 * Todo.txt preview component
 */
@Composable
private fun TodoTxtPreviewEnhanced(content: String) {
    content.lines().forEach { line ->
        if (line.isNotBlank()) {
            val isDone = line.trimStart().startsWith("x ")
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDone) Color.Gray else Color.Unspecified,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

/**
 * CSV preview component
 */
@Composable
private fun CsvPreviewEnhanced(content: String) {
    content.lines().forEach { line ->
        if (line.isNotBlank()) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

/**
 * Plain text preview component
 */
@Composable
private fun PlainTextPreviewEnhanced(content: String) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    )
}

/**
 * Enhanced status bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedStatusBar(
    format: String,
    wordCount: Int,
    characterCount: Int,
    lineCount: Int,
    lastSavedTimestamp: String?,
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
                lastSavedTimestamp?.let { ts ->
                    Text("Saved: $ts", style = MaterialTheme.typography.labelMedium)
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
                        modifier = @Suppress("DEPRECATION") Modifier.menuAnchor().width(120.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        FormatRegistry.formats.forEach { textFormat ->
                            DropdownMenuItem(
                                text = { Text(textFormat.name) },
                                onClick = {
                                    onFormatChange(textFormat.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Control buttons - using text instead of Icons
                TextButton(onClick = onThemeToggle) {
                    Text(
                        if (isOffline) "Offline" else "Online",
                        color = if (isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(onClick = onPreviewToggle) {
                    Text("Preview")
                }

                TextButton(onClick = onWordWrapToggle) {
                    Text("Wrap")
                }

                TextButton(onClick = onLineNumbersToggle) {
                    Text("Lines")
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

// Helper functions

private fun loadSettings() {
    // Load settings from localStorage
    val settings = localStorage.getItem("yole_web_settings")
    if (settings != null) {
        try {
            // Simple key-value parsing without JSON.parse (not available in Wasm)
            println("Settings loaded: $settings")
        } catch (e: Exception) {
            println("ERROR: Failed to load settings: ${e.message}")
        }
    }
}

private fun saveSettingsToLocalStorage(isDarkTheme: Boolean, fontSize: Int, wordWrap: Boolean, showLineNumbers: Boolean) {
    val theme = if (isDarkTheme) "dark" else "light"
    val settingsJson = """{"theme":"$theme","fontSize":$fontSize,"wordWrap":$wordWrap,"showLineNumbers":$showLineNumbers}"""
    localStorage.setItem("yole_web_settings", settingsJson)
}

private fun saveDocumentToLocalStorage(content: String, format: String, name: String) {
    val timestamp = Clock.System.now().toEpochMilliseconds()
    // Store document state as simple string values to avoid JSON interop issues
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
        else -> "txt"
    }
}
