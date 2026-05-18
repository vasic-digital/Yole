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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.syntax.theme.LegacyThemeBridge
import digital.vasic.yole.syntax.theme.Theme
import digital.vasic.yole.syntax.theme.ThemeProvider
import digital.vasic.yole.syntax.theme.ThemeRegistry
import digital.vasic.yole.syntax.theme.themeUiColor
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
// iter-88: kotlinx.datetime.Clock removed — wasm klib doesn't ship Clock.System
// in 0.6.1. The single callsite at line 296 uses todayIsoDate() from
// WebTime.kt instead, which goes through JS Date directly.
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.File
import org.w3c.files.FileReader

/**
 * Main entry point for Yole Web Application
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize parsers and PWA features
    digital.vasic.yole.format.ParserInitializer.registerAllParsersLazy()
    PWAFeatures.initialize()

    // iter-82: CanvasBasedWindow was removed in CMP 1.11.0.
    // Replaced with ComposeViewport which attaches to a DOM element by ID.
    // The `title` parameter no longer exists on the Compose API; set it via document.title.
    document.title = "Yole - IDE Editor"

    ComposeViewport(viewportContainerId = "yoleCanvas") {
        // iter-57 Phase 3: ThemeProvider publishes the active VS Code theme
        // to every Composable. Wasm/JS cannot yet fetch bundled JSON theme
        // assets (that lands in Phase 6); for now we seed an in-memory Theme
        // built from the LegacyThemeBridge so LocalTheme.current returns a
        // usable value identical to pre-iter-57 dark mode.
        LaunchedEffect(Unit) {
            ThemeRegistry.setActive(
                Theme(
                    name = "Yole Dark",
                    type = "dark",
                    uiColors = LegacyThemeBridge.legacyDark,
                    tokenColors = emptyMap(),
                ),
            )
        }
        ThemeProvider {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                EnhancedYoleWebApp()
            }
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
    var showPreview by remember { mutableStateOf(true) }
    var wordWrap by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(14) }
    var showLineNumbers by remember { mutableStateOf(true) }
    
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
                    color = themeUiColor("sideBar.background"),
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
                             
                             Button(
                                 onClick = {
                                     // Trigger file input for loading
                                     triggerFileInput { content, filename ->
                                         documentContent = content
                                         documentName = filename
                                         // Auto-detect format from filename
                                         val format = detectFormatFromFilename(filename)
                                         currentFormat = format
                                     }
                                 },
                                  colors = ButtonDefaults.buttonColors(
                                      containerColor = themeUiColor("focusBorder")
                                  )
                              ) {
                                  Text("Load")
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
                                fontSize = fontSize.sp,
                                lineHeight = 21.sp
                            )
                        )

                        // Preview
                        if (showPreview) {
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
}

/**
 * Format selection list
 */
@Composable
fun FormatList(
    selectedFormat: String,
    onFormatSelected: (String, String) -> Unit
) {
    val formats = FormatRegistry.getEnabledTextFormats().map { it.name to it.defaultExtension }

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
    // iter-88: was Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    // — failed on wasm with IrLinkageError because kotlinx-datetime 0.6.1 wasm klib
    // doesn't ship Clock.System. WebTime.todayIsoDate() uses JS Date directly.
    return todayIsoDate()
}

/**
 * Download content as a file using browser APIs
 * Simplified implementation for WASM compatibility
 */
fun downloadFile(content: String, filename: String) {
    try {
        // Use data URL approach which is more compatible with WASM
        val encodedContent = content.replace("%", "%25")
            .replace("&", "%26")
            .replace("#", "%23")
            .replace("?", "%3F")
            .replace("=", "%3D")
            .replace("+", "%2B")
            .replace(" ", "%20")
            .replace("\n", "%0A")
            .replace("\r", "%0D")
            .replace("<", "%3C")
            .replace(">", "%3E")
            .replace("\"", "%22")
            .replace("'", "%27")
        
        val dataUrl = "data:text/plain;charset=utf-8,$encodedContent"
        val link = document.createElement("a") as HTMLAnchorElement
        link.href = dataUrl
        link.download = filename
        link.style.display = "none"
        
        document.body?.appendChild(link)
        link.click()
        document.body?.removeChild(link)
        
        println("File download initiated: $filename (${content.length} bytes)")
    } catch (e: Exception) {
        println("Error downloading file: ${e.message}")
        // Simple fallback - just log the content
        println("Download failed for $filename. Content length: ${content.length}")
    }
}

/**
 * Trigger file input dialog and handle file loading
 * Simplified implementation for WASM compatibility
 */
fun triggerFileInput(onFileLoaded: (String, String) -> Unit) {
    try {
        val fileInput = document.createElement("input") as HTMLInputElement
        fileInput.type = "file"
        fileInput.style.display = "none"
        fileInput.accept = ".txt,.md,.csv,.tex,.org,.adoc,.wiki,.json,.xml,.yaml,.yml,.html,.rst,.rmd,.taskpaper,.textile,.creole,.tid,.ipynb,.ini,.properties,.vcf,.ics,.toml"

        fileInput.addEventListener("change", { _ ->
            val files = fileInput.files
            if (files != null && files.length > 0) {
                val file = files.item(0)!!
                val filename = file.name
                val reader = FileReader()
                reader.onload = { _ ->
                    val content = reader.result?.toString() ?: ""
                    onFileLoaded(content, filename)
                    document.body?.removeChild(fileInput)
                }
                reader.onerror = { _ ->
                    println("Error reading file: $filename")
                    onFileLoaded("", filename)
                    document.body?.removeChild(fileInput)
                }
                reader.readAsText(file)
            } else {
                document.body?.removeChild(fileInput)
            }
        })

        document.body?.appendChild(fileInput)
        fileInput.click()
    } catch (e: Exception) {
        println("Error in file input handling: ${e.message}")
        val fallbackContent = "# Fallback Content\n\nFile loading is not available in this environment."
        onFileLoaded(fallbackContent, "fallback.md")
    }
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

        "restructuredtext" -> """
            New reStructuredText Document
            =============================

            Introduction
            ------------

            This is a new reStructuredText document.

            Features
            --------

            - **Bold text**
            - *Italic text*
            - ``Inline code``

            .. code-block:: kotlin

               fun hello() {
                   println("Hello, World!")
               }
        """.trimIndent()

        "rmarkdown" -> """
            ---
            title: "New R Markdown Document"
            author: "Your Name"
            date: "${getCurrentDate()}"
            output: html_document
            ---

            ## Introduction

            This is a new R Markdown document.

            ```{r setup, include=FALSE}
            knitr::opts_chunk${'$'}set(echo = TRUE)
            ```

            ## Analysis

            ```{r}
            summary(cars)
            ```

            ## Plot

            ```{r pressure, echo=FALSE}
            plot(pressure)
            ```
        """.trimIndent()

        "taskpaper" -> """
            Inbox:
            	- New task @today
            	- Review project requirements @priority(high)
            	- Write documentation @due(${getCurrentDate()})

            Work:
            	- Complete feature implementation @priority(medium)
            	- Code review @context(office)
            	- Update tests @done
        """.trimIndent()

        "textile" -> """
            h1. New Textile Document

            h2. Introduction

            This is a new Textile document.

            h2. Formatting

            *Bold text* and _italic text_ and @inline code@.

            h2. Lists

            * Unordered item
            * Another item

            # Ordered item
            # Another item
        """.trimIndent()

        "creole" -> """
            = New Creole Document

            == Introduction

            This is a new Creole document.

            == Formatting

            **Bold text** and //italic text//.

            == Lists

            * Unordered item
            * Another item

            # Ordered item
            # Another item

            == Links

            [[https://example.com|Example Link]]
        """.trimIndent()

        "tiddlywiki" -> """
            ! New TiddlyWiki Document

            !! Introduction

            This is a new TiddlyWiki tiddler.

            !! Formatting

            ''Bold text'' and //italic text// and `code`.

            !! Lists

            * Unordered item
            * Another item

            # Ordered item
            # Another item
        """.trimIndent()

        "jupyter" -> """
            {
              "nbformat": 4,
              "nbformat_minor": 5,
              "metadata": {
                "kernelspec": {
                  "display_name": "Python 3",
                  "language": "python",
                  "name": "python3"
                }
              },
              "cells": [
                {
                  "cell_type": "markdown",
                  "metadata": {},
                  "source": ["# New Jupyter Notebook\n", "\n", "Start writing your notebook here."]
                },
                {
                  "cell_type": "code",
                  "metadata": {},
                  "source": ["print('Hello, World!')"],
                  "outputs": [],
                  "execution_count": null
                }
              ]
            }
        """.trimIndent()

        "keyvalue" -> """
            # Application Configuration
            app.name=Yole
            app.version=2.0.0
            app.debug=false

            # Database Settings
            db.host=localhost
            db.port=5432
            db.name=yole_db

            # Feature Flags
            feature.dark_mode=true
            feature.sync=true
        """.trimIndent()

        "wikitext" -> """
            = New WikiText Document =

            == Introduction ==

            This is a new WikiText document.

            == Formatting ==

            '''Bold text''' and ''italic text''.

            == Lists ==

            * Unordered item
            * Another item

            # Ordered item
            # Another item

            == Links ==

            [[https://example.com|Example Link]]
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
fun MarkdownPreview(content: String, formatId: String) {
    var htmlContent by remember { mutableStateOf("<p>Loading preview...</p>") }

    // Generate HTML preview when content or format changes
    LaunchedEffect(content, formatId) {
        htmlContent = try {
            val parser = ParserRegistry.getParser(formatId)
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
 * Represents a parsed HTML block for rendering.
 */
private data class HtmlBlock(
    val tag: String, // "h1"-"h6", "p", "li", "pre", "code", "hr", "blockquote", "text"
    val content: String
)

/**
 * Parse HTML string into a list of renderable blocks.
 * Splits on block-level tags and extracts tag type and text content.
 */
private fun parseHtmlBlocks(html: String): List<HtmlBlock> {
    val blocks = mutableListOf<HtmlBlock>()

    // iter-85: strip <style>, <script>, <link>, <meta>, and HTML comments
    // BEFORE block-level extraction. Without this, the CSS body inside
    // <style>...</style> (emitted by StyleSheets.MARKDOWN_STYLES and the
    // other per-format stylesheets) was being treated as "text between
    // blocks" — `replace(Regex("<[^>]*>"), "")` stripped the <style> tags
    // and left the raw CSS as visible text in the preview pane. User
    // symptom: the Markdown preview rendered the literal stylesheet
    // (`.markdown { font-family: ... }`) instead of the rendered HTML.
    // Caught by the iter-85 full-UI accessibility-tree suite.
    val cleanedHtml = html
        .replace(Regex("<style\\b[^>]*>.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        .replace(Regex("<script\\b[^>]*>.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        .replace(Regex("<link\\b[^>]*/?>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<meta\\b[^>]*/?>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

    // iter-85 phase-3: strip outer container divs (e.g. <div class='markdown'>...</div>)
    // that wrap the entire emitted HTML for CSS scoping. Without this, the
    // outer-div match is treated as a flat 'div' container — its handler
    // concatenates ALL nested content into a SINGLE text blob, destroying
    // the h1/p/h2/ul block structure. Caught visually by the iter-85 phase-2
    // preview-pane probe: user saw "Welcome to YoleA professional IDE-style
    // text editor.FeaturesMulti-tab editing17+ text format support..." as one
    // unrendered text node instead of properly styled blocks.
    // Strategy: peel off the OUTERMOST <div>...</div> wrapper(s) at the start
    // and end of the cleaned HTML — but only when they are the topmost
    // element. Nested divs / arbitrary divs inside content stay intact and
    // their text content shows up as expected.
    var peeledHtml = cleanedHtml.trim()
    while (true) {
        val openDiv = Regex("""^<div\b[^>]*>""", RegexOption.IGNORE_CASE).find(peeledHtml)
        if (openDiv == null) break
        val candidate = peeledHtml.substring(openDiv.range.last + 1).trimEnd()
        if (!candidate.endsWith("</div>", ignoreCase = true)) break
        peeledHtml = candidate.substring(0, candidate.length - "</div>".length).trim()
    }

    // Match block-level elements. h1-h6, p, li, pre, code, hr, blockquote,
    // plus ul/ol/div which we now recursively descend.
    val blockPattern = Regex(
        """<(h[1-6]|p|li|pre|code|hr|blockquote|div|ul|ol)(?:\s[^>]*)?>(.+?)</\1>|<(hr)\s*/?>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    var lastEnd = 0
    for (match in blockPattern.findAll(peeledHtml)) {
        // Add any text between blocks
        val betweenText = peeledHtml.substring(lastEnd, match.range.first)
            .replace(Regex("<[^>]*>"), "").trim()
        if (betweenText.isNotEmpty()) {
            blocks.add(HtmlBlock("text", betweenText))
        }

        val tag = (match.groupValues[1].ifEmpty { match.groupValues[3] }).lowercase()
        // Raw inner HTML (PRE inline-tag strip — needed for nested handling)
        val rawInner = match.groupValues[2]
        // Plain-text strip for leaf tags
        val content = rawInner.replace(Regex("<[^>]*>"), "").trim()

        when (tag) {
            "hr" -> blocks.add(HtmlBlock("hr", ""))
            "ul", "ol" -> {
                // Extract each <li>...</li> as its own block.
                //   - ul → tag "li"; renderer prepends "• ".
                //   - ol → tag "oli"; renderer prepends "N. " using the
                //         content's leading "N|" marker (stripped before
                //         display) so the index travels with the item.
                val liRegex = Regex("""<li\b[^>]*>(.+?)</li>""",
                    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                var index = 1
                for (li in liRegex.findAll(rawInner)) {
                    val liText = li.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                    if (liText.isNotEmpty()) {
                        if (tag == "ol") {
                            blocks.add(HtmlBlock("oli", "$index|$liText"))
                            index++
                        } else {
                            blocks.add(HtmlBlock("li", liText))
                        }
                    }
                }
            }
            "div" -> {
                // Recursive: a div might wrap inner block structure.
                // parseHtmlBlocks() on its raw inner HTML gives us proper
                // block decomposition (h1/p/ul/...) instead of a flat blob.
                blocks.addAll(parseHtmlBlocks(rawInner))
            }
            else -> {
                if (content.isNotEmpty()) blocks.add(HtmlBlock(tag, content))
            }
        }

        lastEnd = match.range.last + 1
    }

    // Add any trailing text
    val trailing = peeledHtml.substring(lastEnd)
        .replace(Regex("<[^>]*>"), "").trim()
    if (trailing.isNotEmpty()) {
        blocks.add(HtmlBlock("text", trailing))
    }

    // If no blocks were parsed, treat the whole thing as stripped text
    if (blocks.isEmpty()) {
        val stripped = peeledHtml.replace(Regex("<[^>]*>"), "").trim()
        if (stripped.isNotEmpty()) {
            stripped.lines().filter { it.isNotBlank() }.forEach { line ->
                blocks.add(HtmlBlock("text", line.trim()))
            }
        }
    }

    return blocks
}

/**
 * HTML content renderer using block-based parsing.
 * Parses HTML into blocks and renders each as an appropriate Compose element.
 */
@Composable
fun HtmlContent(html: String) {
    val blocks = remember(html) { parseHtmlBlocks(html) }

    // No verticalScroll here — HtmlContent is always called from inside an already-scrollable Column.
    // Adding verticalScroll to a component nested inside another verticalScroll causes an
    // IllegalStateException in CMP 1.11.0 (unbounded height constraint).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (blocks.isEmpty()) {
            Text(
                "No content to preview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            blocks.forEach { block ->
                when (block.tag) {
                    "h1" -> Text(
                        text = block.content,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                    )
                    "h2" -> Text(
                        text = block.content,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        modifier = Modifier.padding(bottom = 10.dp, top = 8.dp)
                    )
                    "h3" -> Text(
                        text = block.content,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp, top = 6.dp)
                    )
                    "h4" -> Text(
                        text = block.content,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
                    )
                    "h5" -> Text(
                        text = block.content,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
                    )
                    "h6" -> Text(
                        text = block.content,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
                    )
                    "p", "text" -> Text(
                        text = block.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    "li" -> Text(
                        text = "\u2022 ${block.content}",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                    "oli" -> {
                        // Ordered-list item: content is "N|<text>" where N is
                        // the 1-based index from parseHtmlBlocks.
                        val pipe = block.content.indexOf('|')
                        val (idx, text) = if (pipe > 0)
                            block.content.substring(0, pipe) to block.content.substring(pipe + 1)
                        else "?" to block.content
                        Text(
                            text = "$idx. $text",
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                        )
                    }
                    "code", "pre" -> {
                        val codeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
                        Text(
                            text = block.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(codeBackgroundColor, shape = MaterialTheme.shapes.small)
                                .padding(12.dp)
                                .padding(bottom = 8.dp)
                        )
                    }
                    "hr" -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    "blockquote" -> {
                        val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        Text(
                            text = block.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .padding(start = 16.dp, bottom = 8.dp)
                                .drawBehind {
                                    drawLine(
                                        color = borderColor,
                                        start = Offset(-8.dp.toPx(), 0f),
                                        end = Offset(-8.dp.toPx(), size.height),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Auto-detect format from filename extension
 */
fun detectFormatFromFilename(filename: String): String {
    val extension = filename.substringAfterLast(".", "").lowercase()
    return when (extension) {
        "md", "markdown" -> "markdown"
        "txt" -> {
            // Could be plain text or todo.txt - default to plain text
            "plaintext"
        }
        "csv" -> "csv"
        "tex" -> "latex"
        "org" -> "orgmode"
        "adoc", "asciidoc" -> "asciidoc"
        "wiki" -> "wikitext"
        "rst" -> "restructuredtext"
        "rmd" -> "rmarkdown"
        "taskpaper" -> "taskpaper"
        "textile" -> "textile"
        "creole" -> "creole"
        "tid" -> "tiddlywiki"
        "ipynb" -> "jupyter"
        "ini", "properties", "vcf", "ics", "toml" -> "keyvalue"
        "json" -> "json"
        "xml" -> "xml"
        "yaml", "yml" -> "yaml"
        "html" -> "html"
        "css" -> "css"
        "js" -> "javascript"
        "ts", "tsx" -> "typescript"
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "py" -> "python"
        "cpp", "cc", "cxx" -> "cpp"
        "c" -> "c"
        "h", "hpp" -> "cpp"
        "rs" -> "rust"
        "go" -> "go"
        "rb" -> "ruby"
        "php" -> "php"
        "swift" -> "swift"
        "scala" -> "scala"
        "r" -> "r"
        "m" -> "matlab"
        "sql" -> "sql"
        "sh" -> "shell"
        "bat", "cmd" -> "batch"
        "ps1" -> "powershell"
        "pl" -> "perl"
        "lua" -> "lua"
        "nim" -> "nim"
        "dart" -> "dart"
        "tsv" -> "tsv"
        "log" -> "log"
        "conf", "config" -> "config"
        "ini" -> "ini"
        "properties" -> "properties"
        "env" -> "env"
        "gitignore" -> "gitignore"
        "dockerignore" -> "dockerignore"
        "editorconfig" -> "editorconfig"
        "eslintrc" -> "eslint"
        "prettierrc" -> "prettier"
        "babelrc" -> "babel"
        "webpack" -> "webpack"
        "rollup" -> "rollup"
        "vite" -> "vite"
        "parcel" -> "parcel"
        "snowpack" -> "snowpack"
        "wmr" -> "wmr"
        "tsconfig" -> "tsconfig"
        "jsconfig" -> "jsconfig"
        "jsonc" -> "jsonc"
        "json5" -> "json5"
        "hjson" -> "hjson"
        else -> "plaintext" // Default fallback
    }
}