/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Yole Web Application
 * Progressive Web App with Kotlin/Wasm
 *
 *########################################################*/

package digital.vasic.yole.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.CanvasBasedWindow
import digital.vasic.yole.format.FormatRegistry
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Main entry point for Yole Web Application
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "yoleCanvas", title = "Yole - Web Editor") {
        MaterialTheme {
            YoleWebApp()
        }
    }
}

/**
 * Yole Web Application UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoleWebApp() {
    var documentContent by remember { mutableStateOf("# Welcome to Yole Web\n\nStart writing your document...") }
    var currentFormat by remember { mutableStateOf("markdown") }
    var documentName by remember { mutableStateOf("untitled.md") }
    var isDarkTheme by remember { mutableStateOf(false) }

    // Theme colors
    val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Yole - Web Editor", fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        // Theme toggle button
                        Button(onClick = { isDarkTheme = !isDarkTheme }) {
                            Text(if (isDarkTheme) "☀️ Light" else "🌙 Dark")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDarkTheme) Color(0xFF1a1a1a) else MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Sidebar
                Surface(
                    modifier = Modifier.width(250.dp).fillMaxHeight(),
                    color = if (isDarkTheme) Color(0xFF2a2a2a) else Color(0xFFf5f5f5),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Document Formats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FormatList(
                            selectedFormat = currentFormat,
                            onFormatSelected = { format, extension ->
                                currentFormat = format.lowercase()
                                documentName = "untitled$extension"
                            }
                        )
                    }
                }

                // Main editor area
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    // Toolbar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Create new document with format-specific template
                                    val format = FormatRegistry.formats.find { it.id == currentFormat }
                                    documentContent = when (currentFormat) {
                                        "markdown" -> "# New Document\n\nStart writing..."
                                        "todotxt" -> "(A) New task @context +project due:${getCurrentDate()}"
                                        "csv" -> "Name,Email,Phone\nJohn Doe,john@example.com,555-1234"
                                        "latex" -> "\\documentclass{article}\n\\begin{document}\nContent here\n\\end{document}"
                                        "plaintext" -> "New document"
                                        else -> "# New ${format?.name ?: "Document"}\n\nContent here..."
                                    }
                                    documentName = "untitled${format?.defaultExtension ?: ".txt"}"
                                }
                            ) {
                                Text("New Document")
                            }

                            Button(
                                onClick = {
                                    // Save document - implementation pending JS interop
                                    // TODO: Implement file download using external JS functions
                                    println("Save clicked: $documentName")
                                    println("Content length: ${documentContent.length} chars")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF388e3c)
                                )
                            ) {
                                Text("Save (TODO)")
                            }
                        }
                    }

                    // Editor and preview
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        // Text editor
                        OutlinedTextField(
                            value = documentContent,
                            onValueChange = { documentContent = it },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            placeholder = { Text("Start writing your document...") },
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        )

                        // Preview
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            color = if (isDarkTheme) Color(0xFF252525) else Color(0xFFfafafa),
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                MarkdownPreview(documentContent, currentFormat)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format selection list
 */
@Composable
fun FormatList(
    selectedFormat: String,
    onFormatSelected: (String, String) -> Unit
) {
    val formats = listOf(
        "Markdown" to ".md",
        "Plain Text" to ".txt",
        "Todo.txt" to ".txt",
        "CSV" to ".csv",
        "LaTeX" to ".tex",
        "Org Mode" to ".org",
        "AsciiDoc" to ".adoc",
        "WikiText" to ".wiki"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        formats.forEach { (name, extension) ->
            val formatId = name.lowercase().replace(" ", "").replace(".", "")
            Surface(
                onClick = { onFormatSelected(formatId, extension) },
                color = if (selectedFormat == formatId)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    Color.Transparent,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "$name ($extension)",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Get current date in YYYY-MM-DD format
 */
fun getCurrentDate(): String {
    val now = Clock.System.now()
    val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return localDate.toString() // Returns YYYY-MM-DD format
}

/**
 * Simple markdown preview component
 */
@Composable
fun MarkdownPreview(content: String, format: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (format) {
            "markdown" -> {
                // Basic markdown rendering
                content.lines().forEach { line ->
                    when {
                        line.startsWith("# ") -> {
                            Text(
                                line.substring(2),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        line.startsWith("## ") -> {
                            Text(
                                line.substring(3),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        line.startsWith("### ") -> {
                            Text(
                                line.substring(4),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        line.isNotBlank() -> {
                            Text(
                                line,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 24.sp
                            )
                        }
                        else -> {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            else -> {
                // Generic preview
                Text(
                    content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Preview for ${FormatRegistry.formats.find { it.id == format }?.name ?: format} format",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
