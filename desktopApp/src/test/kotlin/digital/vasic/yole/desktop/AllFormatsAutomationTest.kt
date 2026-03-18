/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive All-Formats UI Automation Test for Yole Desktop
 *
 * Tests ALL 17 text formats through the actual Compose Desktop
 * composables: EditorScreen (create/edit) and PreviewScreen
 * (format detection, parsing, rendered output).
 *
 *########################################################*/
package digital.vasic.yole.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import digital.vasic.yole.desktop.ui.*
import digital.vasic.yole.desktop.ui.theme.YoleDesktopTheme
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.ui.ThemeMode
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test data for a single text format, including content samples,
 * expected editor display text, edit addition, and preview verification strings.
 */
data class FormatTestCase(
    val formatId: String,
    val filename: String,
    val content: String,
    val editAddition: String,
    val expectedPreviewContains: String,
    val expectedFormatName: String,
    /** The format name that detectByFilename() returns for this filename.
     *  May differ from expectedFormatName when extension is ambiguous (e.g. .txt -> Plain Text). */
    val detectedFormatName: String = expectedFormatName
)

/**
 * Comprehensive UI automation test that exercises ALL 17 text formats
 * through the Yole Desktop Compose UI.
 *
 * For each format:
 * 1. Renders EditorScreen with format-appropriate filename and content
 * 2. Verifies content appears in the editor
 * 3. Tests text input (editing)
 * 4. Renders PreviewScreen and verifies format detection + parsed output
 * 5. Tests theme switching with each format
 * 6. Validates FormatRegistry detection for the format
 *
 * Also includes combined tests that iterate all formats in sequence and
 * individual per-format tests for granular CI reporting.
 */
@OptIn(ExperimentalTestApi::class)
class AllFormatsAutomationTest {

    private val screenshotDir = File("recordings/desktop/formats/screenshots").apply { mkdirs() }
    private var screenshotCount = 0

    /**
     * Captures a screenshot from the Compose rendering pipeline via [captureToImage].
     * Validates that the screenshot is NOT all-black (prevents false positives
     * from Wayland/XWayland framebuffer issues).
     */
    private fun ComposeUiTest.saveScreenshot(name: String) {
        try {
            val imageBitmap = onRoot().captureToImage()
            val buffered = imageBitmap.toAwtImage()
            val width = buffered.width
            val height = buffered.height

            screenshotCount++
            val file = File(screenshotDir, "%03d-%s.png".format(screenshotCount, name))
            ImageIO.write(buffered, "png", file)
            println("    Screenshot saved: ${file.name} (${width}x${height})")

            // Validate NOT all-black
            var nonBlackPixels = 0
            val sampleStep = maxOf(1, width * height / 10000)
            for (i in 0 until width * height step sampleStep) {
                val px = i % width
                val py = i / width
                val argb = buffered.getRGB(px, py)
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                if (r > 10 || g > 10 || b > 10) {
                    nonBlackPixels++
                }
            }
            assertTrue(
                nonBlackPixels > 0,
                "Screenshot '$name' is ALL BLACK — evidence invalid (non-black pixels: $nonBlackPixels)"
            )
        } catch (e: IllegalStateException) {
            println("    Screenshot '$name': ${e.message}")
        }
    }

    companion object {
        val ALL_FORMATS = listOf(
            FormatTestCase(
                formatId = TextFormat.ID_MARKDOWN,
                filename = "test.md",
                content = "# Title\n\n**bold** text\n\n- item 1\n- item 2",
                editAddition = "\n\n## Added Section",
                expectedPreviewContains = "Title",
                expectedFormatName = "Markdown"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_PLAINTEXT,
                filename = "test.txt",
                content = "Hello World\n\nThis is plain text.",
                editAddition = "\n\nAdded paragraph.",
                expectedPreviewContains = "Hello World",
                expectedFormatName = "Plain Text"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_TODOTXT,
                filename = "todo.txt",
                content = "(A) Task one @context +project\n(B) Task two\nx Done task",
                editAddition = "\n(C) New task",
                expectedPreviewContains = "Task one",
                expectedFormatName = "Todo.txt",
                detectedFormatName = "Plain Text" // .txt extension matches Plain Text first
            ),
            FormatTestCase(
                formatId = TextFormat.ID_CSV,
                filename = "data.csv",
                content = "Name,Age,City\nAlice,30,NYC\nBob,25,London",
                editAddition = "\nCharlie,35,Paris",
                expectedPreviewContains = "Alice",
                expectedFormatName = "CSV"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_LATEX,
                filename = "doc.tex",
                content = "\\documentclass{article}\n\\begin{document}\nHello World\n\\end{document}",
                editAddition = "\n\\section{New}",
                expectedPreviewContains = "Hello",
                expectedFormatName = "LaTeX"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_ORGMODE,
                filename = "notes.org",
                content = "* Heading\n** Sub\n- item\n#+BEGIN_SRC\ncode\n#+END_SRC",
                editAddition = "\n* New Heading",
                expectedPreviewContains = "Heading",
                expectedFormatName = "Org Mode"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_ASCIIDOC,
                filename = "doc.adoc",
                content = "= Title\n== Section\n*bold* _italic_",
                editAddition = "\n== New Section",
                expectedPreviewContains = "Title",
                expectedFormatName = "AsciiDoc"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_WIKITEXT,
                filename = "page.wiki",
                content = "== Section ==\n'''bold'''\n* bullet",
                editAddition = "\n== New ==",
                expectedPreviewContains = "Section",
                expectedFormatName = "WikiText"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_RESTRUCTUREDTEXT,
                filename = "doc.rst",
                content = "Title\n=====\n\nParagraph text.",
                editAddition = "\nNew Section\n-----------",
                expectedPreviewContains = "Title",
                expectedFormatName = "reStructuredText"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_RMARKDOWN,
                filename = "analysis.Rmd",
                content = "---\ntitle: Test\n---\n# Analysis",
                editAddition = "\n## Results",
                expectedPreviewContains = "Analysis",
                expectedFormatName = "R Markdown"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_TASKPAPER,
                filename = "tasks.taskpaper",
                content = "Project:\n\t- Task 1 @due(2026-03-17)",
                editAddition = "\n\t- Task 2 @done",
                expectedPreviewContains = "Task 1",
                expectedFormatName = "TaskPaper"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_TEXTILE,
                filename = "doc.textile",
                content = "h1. Title\n\n*bold* _italic_",
                editAddition = "\nh2. New",
                expectedPreviewContains = "Title",
                expectedFormatName = "Textile"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_CREOLE,
                filename = "page.creole",
                content = "= Title\n**bold** //italic//",
                editAddition = "\n== New",
                expectedPreviewContains = "Title",
                expectedFormatName = "Creole"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_TIDDLYWIKI,
                filename = "note.tid",
                content = "! Title\n!! Sub\n''bold''",
                editAddition = "\n!!! New",
                expectedPreviewContains = "Title",
                expectedFormatName = "TiddlyWiki"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_JUPYTER,
                filename = "notebook.ipynb",
                content = "{\"cells\":[],\"metadata\":{},\"nbformat\":4,\"nbformat_minor\":5}",
                editAddition = "",
                expectedPreviewContains = "cells",
                expectedFormatName = "Jupyter Notebook"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_KEYVALUE,
                filename = "config.properties",
                content = "key1=value1\nkey2=value2",
                editAddition = "\nkey3=value3",
                expectedPreviewContains = "key1",
                expectedFormatName = "Key-Value"
            ),
            FormatTestCase(
                formatId = TextFormat.ID_BINARY,
                filename = "data.bin",
                content = "48656C6C6F",
                editAddition = "",
                expectedPreviewContains = "48656C6C6F",
                expectedFormatName = "Binary",
                detectedFormatName = "Plain Text" // .bin not in Binary's extensions list
            )
        )
    }

    // ======================== Combined Tests ========================

    /**
     * Tests all 17 formats in sequence: editor creation with content verification.
     */
    @Test
    fun testAllFormatsEditorCreate() = runComposeUiTest {
        println("=== All Formats: Editor Create ===")
        for (format in ALL_FORMATS) {
            println("  Testing editor create: ${format.formatId} (${format.filename})")
            var currentContent by mutableStateOf(format.content)

            setContent {
                YoleDesktopTheme {
                    EditorScreen(
                        fileName = format.filename,
                        content = currentContent,
                        onContentChanged = { currentContent = it }
                    )
                }
            }
            waitForIdle()

            // Verify filename appears in editor tab
            onNodeWithText("Editing: ${format.filename}").assertExists()

            // Verify content is present (use first line or first 30 chars as substring)
            val firstLine = format.content.lines().first().take(30)
            if (firstLine.isNotEmpty()) {
                onNodeWithText(firstLine, substring = true).assertExists()
            }
            println("    PASS: ${format.formatId}")
        }
        println("=== All Formats: Editor Create COMPLETE ===")
    }

    /**
     * Tests all 17 formats in sequence: preview rendering with format detection.
     */
    @Test
    fun testAllFormatsPreview() = runComposeUiTest {
        println("=== All Formats: Preview ===")
        for (format in ALL_FORMATS) {
            println("  Testing preview: ${format.formatId} (${format.filename})")

            setContent {
                YoleDesktopTheme {
                    PreviewScreen(
                        fileName = format.filename,
                        content = format.content
                    )
                }
            }
            waitForIdle()

            // Verify preview header shows filename
            onNodeWithText("Preview: ${format.filename}").assertExists()

            // Verify expected content appears in the preview output
            onNodeWithText(format.expectedPreviewContains, substring = true).assertExists()

            println("    PASS: ${format.formatId}")
        }
        println("=== All Formats: Preview COMPLETE ===")
    }

    /**
     * Tests all 17 formats: editor text input (editing).
     * For formats with non-empty editAddition, types the addition and verifies.
     */
    @Test
    fun testAllFormatsEditorEdit() = runComposeUiTest {
        println("=== All Formats: Editor Edit ===")
        for (format in ALL_FORMATS) {
            if (format.editAddition.isEmpty()) {
                println("  Skipping edit for ${format.formatId} (no edit addition)")
                continue
            }
            println("  Testing editor edit: ${format.formatId}")
            var currentContent by mutableStateOf("")

            setContent {
                YoleDesktopTheme {
                    EditorScreen(
                        fileName = format.filename,
                        content = currentContent,
                        onContentChanged = { currentContent = it }
                    )
                }
            }
            waitForIdle()

            // Type format content into the empty editor via placeholder
            onNodeWithText("Start typing...").performTextInput(format.content)
            waitForIdle()
            assertEquals(format.content, currentContent, "Content should match after initial input for ${format.formatId}")

            println("    PASS: ${format.formatId}")
        }
        println("=== All Formats: Editor Edit COMPLETE ===")
    }

    /**
     * Tests all 17 formats: FormatRegistry detection by filename.
     */
    @Test
    fun testAllFormatsRegistryDetection() {
        println("=== All Formats: Registry Detection ===")
        for (format in ALL_FORMATS) {
            println("  Testing detection: ${format.formatId} (${format.filename})")

            // Verify extension-based detection returns the expected format
            val detected = FormatRegistry.detectByFilename(format.filename)
            assertEquals(
                format.detectedFormatName,
                detected.name,
                "FormatRegistry should detect '${format.filename}' as ${format.detectedFormatName}"
            )

            // Also verify the format exists in the registry by ID (true format name)
            val byId = FormatRegistry.getById(format.formatId)
            assertNotNull(byId, "Format '${format.formatId}' should exist in registry")
            assertEquals(format.expectedFormatName, byId.name)

            println("    PASS: ${format.formatId}")
        }
        println("=== All Formats: Registry Detection COMPLETE ===")
    }

    /**
     * Tests all 17 formats: editor with light theme.
     */
    @Test
    fun testAllFormatsLightTheme() = runComposeUiTest {
        println("=== All Formats: Light Theme ===")
        for (format in ALL_FORMATS) {
            setContent {
                YoleDesktopTheme(themeMode = ThemeMode.LIGHT) {
                    EditorScreen(
                        fileName = format.filename,
                        content = format.content,
                        onContentChanged = {}
                    )
                }
            }
            waitForIdle()

            onNodeWithText("Editing: ${format.filename}").assertExists()
        }
        println("=== All Formats: Light Theme COMPLETE ===")
    }

    /**
     * Tests all 17 formats: editor with dark theme.
     */
    @Test
    fun testAllFormatsDarkTheme() = runComposeUiTest {
        println("=== All Formats: Dark Theme ===")
        for (format in ALL_FORMATS) {
            setContent {
                YoleDesktopTheme(themeMode = ThemeMode.DARK) {
                    EditorScreen(
                        fileName = format.filename,
                        content = format.content,
                        onContentChanged = {}
                    )
                }
            }
            waitForIdle()

            onNodeWithText("Editing: ${format.filename}").assertExists()
        }
        println("=== All Formats: Dark Theme COMPLETE ===")
    }

    /**
     * Tests all 17 formats: preview with dark theme.
     */
    @Test
    fun testAllFormatsPreviewDarkTheme() = runComposeUiTest {
        println("=== All Formats: Preview Dark Theme ===")
        for (format in ALL_FORMATS) {
            setContent {
                YoleDesktopTheme(themeMode = ThemeMode.DARK) {
                    PreviewScreen(
                        fileName = format.filename,
                        content = format.content
                    )
                }
            }
            waitForIdle()

            onNodeWithText("Preview: ${format.filename}").assertExists()
            onNodeWithText(format.expectedPreviewContains, substring = true).assertExists()
        }
        println("=== All Formats: Preview Dark Theme COMPLETE ===")
    }

    /**
     * Full round-trip test: editor -> preview -> editor for all formats.
     */
    @Test
    fun testAllFormatsRoundTrip() = runComposeUiTest {
        println("=== All Formats: Round Trip ===")
        for (format in ALL_FORMATS) {
            println("  Round trip: ${format.formatId}")

            // Step 1: Editor
            var editedContent by mutableStateOf(format.content)
            setContent {
                YoleDesktopTheme {
                    EditorScreen(
                        fileName = format.filename,
                        content = editedContent,
                        onContentChanged = { editedContent = it }
                    )
                }
            }
            waitForIdle()
            onNodeWithText("Editing: ${format.filename}").assertExists()

            // Step 2: Preview
            setContent {
                YoleDesktopTheme {
                    PreviewScreen(
                        fileName = format.filename,
                        content = editedContent
                    )
                }
            }
            waitForIdle()
            onNodeWithText("Preview: ${format.filename}").assertExists()
            onNodeWithText(format.expectedPreviewContains, substring = true).assertExists()

            // Step 3: Back to editor
            setContent {
                YoleDesktopTheme {
                    EditorScreen(
                        fileName = format.filename,
                        content = editedContent,
                        onContentChanged = { editedContent = it }
                    )
                }
            }
            waitForIdle()
            onNodeWithText("Editing: ${format.filename}").assertExists()

            println("    PASS: ${format.formatId}")
        }
        println("=== All Formats: Round Trip COMPLETE ===")
    }

    /**
     * Tests editor with line numbers toggled off for all formats.
     */
    @Test
    fun testAllFormatsEditorNoLineNumbers() = runComposeUiTest {
        println("=== All Formats: Editor No Line Numbers ===")
        for (format in ALL_FORMATS) {
            setContent {
                YoleDesktopTheme {
                    EditorScreen(
                        fileName = format.filename,
                        content = format.content,
                        onContentChanged = {},
                        showLineNumbers = false
                    )
                }
            }
            waitForIdle()

            onNodeWithText("Editing: ${format.filename}").assertExists()
            // Content should still be visible
            val firstLine = format.content.lines().first().take(30)
            if (firstLine.isNotEmpty()) {
                onNodeWithText(firstLine, substring = true).assertExists()
            }
        }
        println("=== All Formats: Editor No Line Numbers COMPLETE ===")
    }

    /**
     * Validates that all 17 format IDs exist in FormatRegistry.formats.
     */
    @Test
    fun testAllFormatIdsRegistered() {
        println("=== Verify All 17 Format IDs Registered ===")
        assertTrue(FormatRegistry.formats.size >= 17, "FormatRegistry should have at least 17 formats")

        val registeredIds = FormatRegistry.formats.map { it.id }.toSet()
        for (format in ALL_FORMATS) {
            assertTrue(
                format.formatId in registeredIds,
                "Format ID '${format.formatId}' should be registered"
            )
        }
        println("=== All 17 Format IDs Registered COMPLETE ===")
    }

    /**
     * Tests that FormatRegistry.detectByFilename returns the correct format
     * for every test case filename, and that ParserRegistry can provide
     * a parser for each (where applicable).
     */
    @Test
    fun testAllFormatsParserRegistryIntegration() {
        println("=== All Formats: Parser Registry Integration ===")
        var parsersFound = 0
        for (format in ALL_FORMATS) {
            // Use extension-based detection (matches what PreviewScreen does)
            val detected = FormatRegistry.detectByFilename(format.filename)
            assertEquals(format.detectedFormatName, detected.name)

            val parser = ParserRegistry.getParser(detected)
            if (parser != null) {
                parsersFound++
                // Parse the sample content
                val document = parser.parse(format.content)
                assertNotNull(document, "Parser should produce a document for ${format.formatId}")
                assertTrue(
                    document.rawContent == format.content,
                    "Parsed document should preserve raw content for ${format.formatId}"
                )
                println("    ${format.formatId}: parser found, document parsed")
            } else {
                println("    ${format.formatId}: no parser registered (format-only entry)")
            }
        }
        println("  Parsers found: $parsersFound / ${ALL_FORMATS.size}")
        println("=== All Formats: Parser Registry Integration COMPLETE ===")
    }

    // ======================== Individual Format Tests ========================

    @Test fun testMarkdown() = testFormatEditorAndPreview(ALL_FORMATS[0])
    @Test fun testPlainText() = testFormatEditorAndPreview(ALL_FORMATS[1])
    @Test fun testTodoTxt() = testFormatEditorAndPreview(ALL_FORMATS[2])
    @Test fun testCsv() = testFormatEditorAndPreview(ALL_FORMATS[3])
    @Test fun testLatex() = testFormatEditorAndPreview(ALL_FORMATS[4])
    @Test fun testOrgMode() = testFormatEditorAndPreview(ALL_FORMATS[5])
    @Test fun testAsciiDoc() = testFormatEditorAndPreview(ALL_FORMATS[6])
    @Test fun testWikiText() = testFormatEditorAndPreview(ALL_FORMATS[7])
    @Test fun testReStructuredText() = testFormatEditorAndPreview(ALL_FORMATS[8])
    @Test fun testRMarkdown() = testFormatEditorAndPreview(ALL_FORMATS[9])
    @Test fun testTaskPaper() = testFormatEditorAndPreview(ALL_FORMATS[10])
    @Test fun testTextile() = testFormatEditorAndPreview(ALL_FORMATS[11])
    @Test fun testCreole() = testFormatEditorAndPreview(ALL_FORMATS[12])
    @Test fun testTiddlyWiki() = testFormatEditorAndPreview(ALL_FORMATS[13])
    @Test fun testJupyter() = testFormatEditorAndPreview(ALL_FORMATS[14])
    @Test fun testKeyValue() = testFormatEditorAndPreview(ALL_FORMATS[15])
    @Test fun testBinary() = testFormatEditorAndPreview(ALL_FORMATS[16])

    // ======================== Individual Format Edit Tests ========================

    @Test fun testMarkdownEdit() = testFormatEdit(ALL_FORMATS[0])
    @Test fun testPlainTextEdit() = testFormatEdit(ALL_FORMATS[1])
    @Test fun testTodoTxtEdit() = testFormatEdit(ALL_FORMATS[2])
    @Test fun testCsvEdit() = testFormatEdit(ALL_FORMATS[3])
    @Test fun testLatexEdit() = testFormatEdit(ALL_FORMATS[4])
    @Test fun testOrgModeEdit() = testFormatEdit(ALL_FORMATS[5])
    @Test fun testAsciiDocEdit() = testFormatEdit(ALL_FORMATS[6])
    @Test fun testWikiTextEdit() = testFormatEdit(ALL_FORMATS[7])
    @Test fun testReStructuredTextEdit() = testFormatEdit(ALL_FORMATS[8])
    @Test fun testRMarkdownEdit() = testFormatEdit(ALL_FORMATS[9])
    @Test fun testTaskPaperEdit() = testFormatEdit(ALL_FORMATS[10])
    @Test fun testTextileEdit() = testFormatEdit(ALL_FORMATS[11])
    @Test fun testCreoleEdit() = testFormatEdit(ALL_FORMATS[12])
    @Test fun testTiddlyWikiEdit() = testFormatEdit(ALL_FORMATS[13])
    @Test fun testJupyterEdit() = testFormatEdit(ALL_FORMATS[14])
    @Test fun testKeyValueEdit() = testFormatEdit(ALL_FORMATS[15])
    @Test fun testBinaryEdit() = testFormatEdit(ALL_FORMATS[16])

    // ======================== Individual Format Theme Tests ========================

    @Test fun testMarkdownThemes() = testFormatThemes(ALL_FORMATS[0])
    @Test fun testPlainTextThemes() = testFormatThemes(ALL_FORMATS[1])
    @Test fun testTodoTxtThemes() = testFormatThemes(ALL_FORMATS[2])
    @Test fun testCsvThemes() = testFormatThemes(ALL_FORMATS[3])
    @Test fun testLatexThemes() = testFormatThemes(ALL_FORMATS[4])
    @Test fun testOrgModeThemes() = testFormatThemes(ALL_FORMATS[5])
    @Test fun testAsciiDocThemes() = testFormatThemes(ALL_FORMATS[6])
    @Test fun testWikiTextThemes() = testFormatThemes(ALL_FORMATS[7])
    @Test fun testReStructuredTextThemes() = testFormatThemes(ALL_FORMATS[8])
    @Test fun testRMarkdownThemes() = testFormatThemes(ALL_FORMATS[9])
    @Test fun testTaskPaperThemes() = testFormatThemes(ALL_FORMATS[10])
    @Test fun testTextileThemes() = testFormatThemes(ALL_FORMATS[11])
    @Test fun testCreoleThemes() = testFormatThemes(ALL_FORMATS[12])
    @Test fun testTiddlyWikiThemes() = testFormatThemes(ALL_FORMATS[13])
    @Test fun testJupyterThemes() = testFormatThemes(ALL_FORMATS[14])
    @Test fun testKeyValueThemes() = testFormatThemes(ALL_FORMATS[15])
    @Test fun testBinaryThemes() = testFormatThemes(ALL_FORMATS[16])

    // ======================== Helper Methods ========================

    /**
     * Tests a single format through editor creation and preview rendering.
     */
    private fun testFormatEditorAndPreview(format: FormatTestCase) = runComposeUiTest {
        println("  Testing format: ${format.formatId}")

        // Phase 1: Editor — create with content
        setContent {
            YoleDesktopTheme {
                EditorScreen(
                    fileName = format.filename,
                    content = format.content,
                    onContentChanged = {}
                )
            }
        }
        waitForIdle()

        onNodeWithText("Editing: ${format.filename}").assertExists()
        val firstLine = format.content.lines().first().take(30)
        if (firstLine.isNotEmpty()) {
            onNodeWithText(firstLine, substring = true).assertExists()
        }
        saveScreenshot("${format.formatId}-editor")

        // Phase 2: Preview — verify format detection and rendered output
        setContent {
            YoleDesktopTheme {
                PreviewScreen(
                    fileName = format.filename,
                    content = format.content
                )
            }
        }
        waitForIdle()

        onNodeWithText("Preview: ${format.filename}").assertExists()
        onNodeWithText(format.expectedPreviewContains, substring = true).assertExists()

        // Phase 3: Verify detected format name appears in preview (from "Format: <name>")
        onNodeWithText(format.detectedFormatName, substring = true).assertExists()
        saveScreenshot("${format.formatId}-preview")

        println("    PASS: ${format.formatId} (editor + preview)")
    }

    /**
     * Tests editing for a single format: starts with empty editor, types content,
     * verifies it was captured by the onContentChanged callback.
     */
    private fun testFormatEdit(format: FormatTestCase) = runComposeUiTest {
        println("  Testing edit: ${format.formatId}")
        var captured = ""

        setContent {
            YoleDesktopTheme {
                EditorScreen(
                    fileName = format.filename,
                    content = "",
                    onContentChanged = { captured = it }
                )
            }
        }
        waitForIdle()

        // Type content into the empty editor
        val inputText = if (format.editAddition.isNotEmpty()) {
            format.editAddition.trimStart()
        } else {
            format.content.take(20)
        }

        onNodeWithText("Start typing...").performTextInput(inputText)
        waitForIdle()

        assertEquals(inputText, captured, "Captured content should match input for ${format.formatId}")
        println("    PASS: ${format.formatId} (edit)")
    }

    /**
     * Tests a single format rendered in both light and dark themes.
     */
    private fun testFormatThemes(format: FormatTestCase) = runComposeUiTest {
        println("  Testing themes: ${format.formatId}")

        // Light theme — Editor
        setContent {
            YoleDesktopTheme(themeMode = ThemeMode.LIGHT) {
                EditorScreen(
                    fileName = format.filename,
                    content = format.content,
                    onContentChanged = {}
                )
            }
        }
        waitForIdle()
        onNodeWithText("Editing: ${format.filename}").assertExists()

        // Dark theme — Editor
        setContent {
            YoleDesktopTheme(themeMode = ThemeMode.DARK) {
                EditorScreen(
                    fileName = format.filename,
                    content = format.content,
                    onContentChanged = {}
                )
            }
        }
        waitForIdle()
        onNodeWithText("Editing: ${format.filename}").assertExists()

        // Light theme — Preview
        setContent {
            YoleDesktopTheme(themeMode = ThemeMode.LIGHT) {
                PreviewScreen(
                    fileName = format.filename,
                    content = format.content
                )
            }
        }
        waitForIdle()
        onNodeWithText("Preview: ${format.filename}").assertExists()

        // Dark theme — Preview
        setContent {
            YoleDesktopTheme(themeMode = ThemeMode.DARK) {
                PreviewScreen(
                    fileName = format.filename,
                    content = format.content
                )
            }
        }
        waitForIdle()
        onNodeWithText("Preview: ${format.filename}").assertExists()

        println("    PASS: ${format.formatId} (themes)")
    }
}
