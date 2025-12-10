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
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.url.URL

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
                                     createNewDocument(currentFormat) { content, filename ->
                                         documentContent = content
                                         documentName = filename
                                     }
                                 }
                             ) {
                                 Text("New Document")
                             }

                             Button(
                                 onClick = {
                                     // Save document using browser download API
                                     downloadFile(documentContent, documentName)
                                 },
                                 colors = ButtonDefaults.buttonColors(
                                     containerColor = Color(0xFF388e3c)
                                 )
                             ) {
                                 Text("Save")
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
 * Download content as a file using browser APIs
 * TODO: Implement proper file download for WASM
 */
fun downloadFile(content: String, filename: String) {
    // For now, just log the content - proper download implementation needed
    println("Download requested for $filename with content length: ${content.length}")
    // TODO: Implement browser-compatible file download
}

/**
 * Create a new document with format-specific template
 */
fun createNewDocument(formatId: String, onDocumentCreated: (String, String) -> Unit) {
    val format = FormatRegistry.formats.find { it.id == formatId }
    val content = when (formatId) {
        "markdown" -> """
            # New Markdown Document

            ## Introduction

            Start writing your markdown content here.

            ## Features

            - **Bold text**
            - *Italic text*
            - `Code snippets`
            - [Links](https://example.com)

            ## Lists

            1. Ordered list item
            2. Another item

            - Unordered list item
            - Another item

            ## Code Block

            ```kotlin
            fun hello() {
                println("Hello, World!")
            }
            ```
        """.trimIndent()

        "todotxt" -> """
            (A) Review project requirements @work +planning
            (B) Write documentation @work +documentation due:${getCurrentDate()}
            (C) Test new features @work +testing
            x ${getCurrentDate()} Complete setup @work +planning
        """.trimIndent()

        "latex" -> """
            \\documentclass{article}
            \\usepackage[utf8]{inputenc}
            \\usepackage[T1]{fontenc}

            \\title{New LaTeX Document}
            \\author{Your Name}
            \\date{\\today}

            \\begin{document}

            \\maketitle

            \\section{Introduction}

            This is a new LaTeX document. Start writing your content here.

            \\subsection{Subsection}

            You can add subsections, figures, tables, and much more.

            \\begin{itemize}
            \\item First item
            \\item Second item
            \\item Third item
            \\end{itemize}

            \\section{Conclusion}

            This concludes the document.

            \\end{document}
        """.trimIndent()

        "orgmode" -> """
            #+TITLE: New Org Mode Document
            #+AUTHOR: Your Name
            #+DATE: ${getCurrentDate()}

            * Introduction

            This is a new Org mode document.

            ** Features

            - TODO items
            - Timestamps
            - Properties
            - Tables

            * TODO Tasks

            *** TODO Write documentation
                DEADLINE: <${getCurrentDate()}>
                :PROPERTIES:
                :Effort: 2h
                :END:

            *** DONE Set up project
                CLOSED: [${getCurrentDate()}]

            * Tables

            | Name | Age | City |
            |------+-----+------|
            | John |  30 | NYC  |
            | Jane |  25 | LA   |
        """.trimIndent()

        "plaintext" -> """
            New Plain Text Document

            Start writing your content here.

            You can write anything you want in plain text format.
            No special markup is required.
        """.trimIndent()

        "asciidoc" -> """
            = New AsciiDoc Document

            Your Name <your.email@example.com>

            == Introduction

            This is a new AsciiDoc document.

            == Features

            * Bold text
            * Italic text
            * `Monospace text`
            * https://example.com[Links]

            == Lists

            . Ordered list
            . Another item

            * Unordered list
            * Another item

            == Code Block

            [source,kotlin]
            ----
            fun hello() {
                println("Hello, World!")
            }
            ----
        """.trimIndent()

        "csv" -> """
            Name,Email,Phone,Department
            John Doe,john.doe@company.com,555-0101,Engineering
            Jane Smith,jane.smith@company.com,555-0102,Marketing
            Bob Johnson,bob.johnson@company.com,555-0103,Sales
        """.trimIndent()

        else -> """
            # New ${format?.name ?: "Document"}

            Start writing your content here.

            This document uses the ${format?.name ?: "unknown"} format.
        """.trimIndent()
    }

    val filename = "untitled${format?.defaultExtension ?: ".txt"}"
    onDocumentCreated(content, filename)
}

/**
 * HTML preview component using format parsers
 */
@Composable
fun MarkdownPreview(content: String, format: String) {
    var htmlContent by remember { mutableStateOf("<p>Loading preview...</p>") }

    // Generate HTML preview when content or format changes
    LaunchedEffect(content, format) {
        htmlContent = try {
            val parser = ParserRegistry.getParser(format)
            if (parser != null) {
                val document = parser.parse(content)
                parser.toHtml(document)
            } else {
                // Fallback for unknown formats
                "<pre>${content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</pre>"
            }
        } catch (e: Exception) {
            // Error fallback
            "<p style='color: red;'>Preview error: ${e.message}</p><pre>${content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</pre>"
        }
    }

    // Render HTML content
    HtmlContent(htmlContent)
}

/**
 * HTML content renderer using browser DOM
 */
@Composable
fun HtmlContent(html: String) {
    // For WASM, we need to use a different approach since we can't directly render HTML
    // Let's create a simple HTML renderer that handles basic tags
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Basic HTML parsing and rendering
        val lines = html.replace(Regex("<[^>]*>"), "").lines()

        lines.forEach { line ->
            if (line.isNotBlank()) {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (lines.isEmpty()) {
            Text(
                "No content to preview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
