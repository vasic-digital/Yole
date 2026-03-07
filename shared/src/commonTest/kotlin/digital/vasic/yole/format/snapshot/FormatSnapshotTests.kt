/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Snapshot/regression tests for format parser output stability.
 *
 * Each test provides a known input and verifies that the
 * parsed output retains specific structural properties.
 * This catches regressions when parser logic changes.
 *
 *########################################################*/
package digital.vasic.yole.format.snapshot

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.binary.BinaryParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

/**
 * Snapshot/regression tests for format parser output stability.
 *
 * Validates that known inputs produce outputs with specific expected
 * structural properties, catching regressions in parsers, HTML generation,
 * metadata extraction, and stylesheet generation.
 */
class FormatSnapshotTests {

    // ====================================================================
    // Markdown snapshots
    // ====================================================================

    @Test
    fun snapshotMarkdownHeadings() {
        val parser = MarkdownParser()
        val input = "# Heading 1\n\n## Heading 2\n\nParagraph text.\n"
        val result = parser.parse(input)
        assertEquals(input, result.rawContent)
        assertNotNull(result.parsedContent)
        assertTrue(result.errors.isEmpty(), "No parse errors expected")
        assertEquals(FormatRegistry.ID_MARKDOWN, result.format.id)
        // parsedContent is the HTML for Markdown
        assertTrue(result.parsedContent.contains("Heading 1"), "HTML should contain heading text")
        assertTrue(result.parsedContent.contains("Heading 2"), "HTML should contain sub-heading text")
        assertTrue(result.parsedContent.contains("Paragraph text"), "HTML should contain paragraph text")
    }

    @Test
    fun snapshotMarkdownLinks() {
        val parser = MarkdownParser()
        val input = "[Click here](https://example.com)\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("example.com"), "HTML should contain link URL")
        assertTrue(html.contains("Click here"), "HTML should contain link text")
    }

    @Test
    fun snapshotMarkdownCodeBlock() {
        val parser = MarkdownParser()
        val input = "```kotlin\nfun main() { println(\"hello\") }\n```\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("fun main"), "HTML should contain code content")
    }

    @Test
    fun snapshotMarkdownBoldItalic() {
        val parser = MarkdownParser()
        val input = "**bold text** and *italic text*\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("bold text"), "HTML should contain bold content")
        assertTrue(html.contains("italic text"), "HTML should contain italic content")
    }

    @Test
    fun snapshotMarkdownMetadata() {
        val parser = MarkdownParser()
        val input = "# Title\n\nLine 2\n\nLine 3\n"
        val result = parser.parse(input)
        assertTrue(result.metadata.containsKey("lines"), "Metadata should contain 'lines' key")
        val lineCount = result.metadata["lines"]?.toIntOrNull()
        assertNotNull(lineCount, "Line count should be numeric")
        assertTrue(lineCount > 0, "Line count should be positive")
    }

    // ====================================================================
    // TodoTxt snapshots
    // ====================================================================

    @Test
    fun snapshotTodoTxtPriorityTasks() {
        val parser = TodoTxtParser()
        val input = "(A) Important task @work +project1\n(B) Another task @home +project2\n"
        val result = parser.parse(input)
        assertEquals(input, result.rawContent)
        assertNotNull(result.parsedContent)
        assertEquals(FormatRegistry.ID_TODOTXT, result.format.id)
        assertTrue(result.parsedContent.contains("Important task"), "Should contain task description")
        assertTrue(result.parsedContent.contains("Another task"), "Should contain second task")
    }

    @Test
    fun snapshotTodoTxtCompletedTask() {
        val parser = TodoTxtParser()
        val input = "x 2025-01-15 2025-01-01 Complete report @work +quarterly\n"
        val result = parser.parse(input)
        assertTrue(result.parsedContent.contains("Complete report"), "Should contain completed task")
    }

    // ====================================================================
    // CSV snapshots
    // ====================================================================

    @Test
    fun snapshotCsvSimpleTable() {
        val parser = CsvParser()
        val input = "name,age,city\nAlice,30,New York\nBob,25,London\n"
        val result = parser.parse(input)
        assertEquals(input, result.rawContent)
        assertEquals(FormatRegistry.ID_CSV, result.format.id)
        assertTrue(result.parsedContent.contains("Alice"), "HTML should contain data values")
        assertTrue(result.parsedContent.contains("Bob"), "HTML should contain all rows")
        assertTrue(result.parsedContent.contains("name"), "HTML should contain headers")
    }

    @Test
    fun snapshotCsvQuotedValues() {
        val parser = CsvParser()
        val input = "name,description\n\"Smith, John\",\"Has a comma, here\"\n"
        val result = parser.parse(input)
        assertNotNull(result.parsedContent)
        assertTrue(result.errors.isEmpty() || result.parsedContent.isNotEmpty(),
            "Quoted CSV should parse without fatal errors")
    }

    // ====================================================================
    // LaTeX snapshots
    // ====================================================================

    @Test
    fun snapshotLatexDocumentStructure() {
        val parser = LatexParser()
        val input = "\\documentclass{article}\n\\begin{document}\n\\section{Introduction}\nHello world.\n\\end{document}\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_LATEX, result.format.id)
        assertNotNull(result.parsedContent)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("Introduction"), "HTML should contain section title")
        assertTrue(html.contains("Hello world"), "HTML should contain body text")
    }

    @Test
    fun snapshotLatexMathFormulas() {
        val parser = LatexParser()
        val input = "\\documentclass{article}\n\\begin{document}\nInline: \$E = mc^2\$\n\\end{document}\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "LaTeX with math should produce non-empty HTML")
    }

    // ====================================================================
    // Plaintext snapshots
    // ====================================================================

    @Test
    fun snapshotPlaintextPreservesContent() {
        val parser = PlaintextParser()
        val input = "Hello, world!\nThis is plain text.\nLine three.\n"
        val result = parser.parse(input)
        assertEquals(input, result.rawContent, "rawContent should preserve original input exactly")
        assertEquals(FormatRegistry.ID_PLAINTEXT, result.format.id)
        assertTrue(result.errors.isEmpty(), "Plaintext should have no errors")
        assertTrue(result.metadata.containsKey("lines"), "Metadata should have line count")
    }

    @Test
    fun snapshotPlaintextHtmlEscaping() {
        val parser = PlaintextParser()
        val input = "<script>alert('xss')</script>\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        // HTML-escaped content should NOT contain raw script tags
        assertTrue(!html.contains("<script>") || html.contains("&lt;script"),
            "Plaintext HTML should escape dangerous tags")
    }

    // ====================================================================
    // OrgMode snapshots
    // ====================================================================

    @Test
    fun snapshotOrgModeHeadingsAndTodo() {
        val parser = OrgModeParser()
        val input = "* Heading 1\n** TODO Task item\n** DONE Completed item\nSome body text.\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_ORGMODE, result.format.id)
        assertNotNull(result.parsedContent)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("Heading 1"), "HTML should contain heading text")
        assertTrue(html.contains("Task item") || html.contains("TODO"),
            "HTML should contain TODO items")
    }

    @Test
    fun snapshotOrgModeProperties() {
        val parser = OrgModeParser()
        val input = "* Heading\n:PROPERTIES:\n:CUSTOM_ID: my-id\n:END:\nBody.\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "OrgMode with properties should produce HTML")
    }

    // ====================================================================
    // WikiText snapshots
    // ====================================================================

    @Test
    fun snapshotWikitextFormatting() {
        val parser = WikitextParser()
        val input = "= Main Heading =\n\n'''Bold text''' and ''italic text''.\n\n* List item 1\n* List item 2\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_WIKITEXT, result.format.id)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("Main Heading"), "HTML should contain heading")
        assertTrue(html.contains("List item"), "HTML should contain list items")
    }

    @Test
    fun snapshotWikitextLinks() {
        val parser = WikitextParser()
        val input = "[[PageName]]\n[[PageName|Display Text]]\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "Wikitext with links should produce HTML")
    }

    // ====================================================================
    // AsciiDoc snapshots
    // ====================================================================

    @Test
    fun snapshotAsciidocHeadingsAndCode() {
        val parser = AsciidocParser()
        val input = "= Document Title\n\n== Section One\n\nSome text here.\n\n----\ncode block\n----\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_ASCIIDOC, result.format.id)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("Document Title") || html.contains("Section One"),
            "HTML should contain heading content")
    }

    @Test
    fun snapshotAsciidocAdmonition() {
        val parser = AsciidocParser()
        val input = "= Title\n\nNOTE: This is a note.\n\nTIP: This is a tip.\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "AsciiDoc admonitions should produce HTML")
    }

    // ====================================================================
    // reStructuredText snapshots
    // ====================================================================

    @Test
    fun snapshotRstHeadings() {
        val parser = RestructuredTextParser()
        val input = "Document Title\n==============\n\nSection\n-------\n\nParagraph text.\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_RESTRUCTUREDTEXT, result.format.id)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("Document Title"), "HTML should contain heading text")
        assertTrue(html.contains("Paragraph text"), "HTML should contain paragraph text")
    }

    @Test
    fun snapshotRstDirective() {
        val parser = RestructuredTextParser()
        val input = "Title\n=====\n\n.. note::\n\n   This is a note.\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "RST with directives should produce HTML")
    }

    // ====================================================================
    // RMarkdown snapshots
    // ====================================================================

    @Test
    fun snapshotRMarkdownFrontMatter() {
        val parser = RMarkdownParser()
        val input = "---\ntitle: \"Analysis Report\"\nauthor: \"Test\"\n---\n\n# Introduction\n\nSome text.\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_RMARKDOWN, result.format.id)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("Analysis Report") || html.contains("Introduction"),
            "HTML should contain document content")
    }

    @Test
    fun snapshotRMarkdownCodeChunks() {
        val parser = RMarkdownParser()
        val input = "---\ntitle: \"Test\"\n---\n\n```{r setup}\nlibrary(ggplot2)\n```\n\nSome text.\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "RMarkdown with code chunks should produce HTML")
    }

    // ====================================================================
    // TaskPaper snapshots
    // ====================================================================

    @Test
    fun snapshotTaskpaperProjectsAndTasks() {
        val parser = TaskpaperParser()
        val input = "My Project:\n\t- Task one @priority(high)\n\t- Task two @done\n\t\tSub-note here\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_TASKPAPER, result.format.id)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("My Project") || html.contains("Task one"),
            "HTML should contain project/task content")
    }

    @Test
    fun snapshotTaskpaperTags() {
        val parser = TaskpaperParser()
        val input = "Work:\n\t- Call client @phone @due(2025-02-01)\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "TaskPaper with tags should produce HTML")
    }

    // ====================================================================
    // Textile snapshots
    // ====================================================================

    @Test
    fun snapshotTextileFormatting() {
        val parser = TextileParser()
        val input = "h1. Main Heading\n\nThis is *bold* and _italic_ text.\n\n* List item one\n* List item two\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_TEXTILE, result.format.id)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("Main Heading"), "HTML should contain heading")
    }

    @Test
    fun snapshotTextileLinks() {
        val parser = TextileParser()
        val input = "\"Link text\":https://example.com\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "Textile with links should produce HTML")
    }

    // ====================================================================
    // Creole snapshots
    // ====================================================================

    @Test
    fun snapshotCreoleBoldAndLinks() {
        val parser = CreoleParser()
        val input = "= Creole Heading\n\n**Bold text** and //italic text//.\n\n* Item one\n* Item two\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_CREOLE, result.format.id)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("Creole Heading") || html.contains("Bold text"),
            "HTML should contain formatted content")
    }

    @Test
    fun snapshotCreoleWikiLinks() {
        val parser = CreoleParser()
        val input = "[[SomePage|Display Text]]\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "Creole with wiki links should produce HTML")
    }

    // ====================================================================
    // TiddlyWiki snapshots
    // ====================================================================

    @Test
    fun snapshotTiddlyWikiTiddler() {
        val parser = TiddlyWikiParser()
        val input = "title: My Tiddler\ntags: test sample\n\n! Heading\n\n!! Subheading\n\nSome content.\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_TIDDLYWIKI, result.format.id)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("Heading") || html.contains("My Tiddler"),
            "HTML should contain tiddler content")
    }

    @Test
    fun snapshotTiddlyWikiMetadata() {
        val parser = TiddlyWikiParser()
        val input = "title: Test\ntags: [[tag one]] tag2\n\nBody text.\n"
        val result = parser.parse(input)
        assertNotNull(result.parsedContent)
        assertTrue(result.parsedContent.isNotEmpty(), "TiddlyWiki should produce parsed content")
    }

    // ====================================================================
    // Jupyter snapshots
    // ====================================================================

    @Test
    fun snapshotJupyterNotebookStructure() {
        val parser = JupyterParser()
        val input = """{"nbformat": 4, "nbformat_minor": 2, "metadata": {}, "cells": [{"cell_type": "markdown", "metadata": {}, "source": ["# Title"]}]}"""
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_JUPYTER, result.format.id)
        assertNotNull(result.parsedContent)
    }

    @Test
    fun snapshotJupyterCodeCell() {
        val parser = JupyterParser()
        val input = """{"nbformat": 4, "nbformat_minor": 2, "metadata": {}, "cells": [{"cell_type": "code", "metadata": {}, "source": ["print('hello')"], "outputs": []}]}"""
        val result = parser.parse(input)
        assertNotNull(result.parsedContent)
        assertTrue(result.parsedContent.isNotEmpty(), "Jupyter code cell should produce content")
    }

    // ====================================================================
    // KeyValue snapshots
    // ====================================================================

    @Test
    fun snapshotKeyValuePairs() {
        val parser = KeyValueParser()
        val input = "key1 = value1\nkey2 = value2\nkey3 = value3\n"
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_KEYVALUE, result.format.id)
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("key1") && html.contains("value1"),
            "HTML should contain key-value pairs")
    }

    @Test
    fun snapshotKeyValueSections() {
        val parser = KeyValueParser()
        val input = "[section1]\nkey1 = value1\n\n[section2]\nkey2 = value2\n"
        val result = parser.parse(input)
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.isNotEmpty(), "KeyValue with sections should produce HTML")
    }

    // ====================================================================
    // Binary snapshots
    // ====================================================================

    @Test
    fun snapshotBinaryDetection() {
        val parser = BinaryParser()
        val input = buildString {
            repeat(50) { append(it.toChar()) }
        }
        val result = parser.parse(input)
        assertEquals(FormatRegistry.ID_BINARY, result.format.id)
        assertNotNull(result.parsedContent)
    }

    @Test
    fun snapshotBinaryMetadata() {
        val parser = BinaryParser()
        val input = "PK\u0003\u0004binary content here"
        val result = parser.parse(input)
        assertNotNull(result.metadata)
        assertNotNull(result.parsedContent)
    }

    // ====================================================================
    // StyleSheet snapshots
    // ====================================================================

    @Test
    fun snapshotStyleSheetMarkdown() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertNotNull(css)
        assertTrue(css.isNotEmpty(), "Markdown stylesheet should not be empty")
        assertTrue(css.contains("font-family"), "Markdown CSS should define font-family")
        assertTrue(css.contains(".markdown"), "Markdown CSS should target .markdown class")
    }

    @Test
    fun snapshotStyleSheetLatexLightVsDark() {
        val lightCss = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, true)
        val darkCss = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, false)
        assertTrue(lightCss.isNotEmpty(), "LaTeX light CSS should not be empty")
        assertTrue(darkCss.isNotEmpty(), "LaTeX dark CSS should not be empty")
        assertNotEquals(lightCss, darkCss, "Light and dark LaTeX CSS should differ")
        assertTrue(lightCss.contains(".latex"), "LaTeX light CSS should contain .latex class")
        assertTrue(darkCss.contains(".latex"), "LaTeX dark CSS should contain .latex class")
    }

    @Test
    fun snapshotStyleSheetOrgModeLightVsDark() {
        val lightCss = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, true)
        val darkCss = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, false)
        assertTrue(lightCss.isNotEmpty(), "OrgMode light CSS should not be empty")
        assertTrue(darkCss.isNotEmpty(), "OrgMode dark CSS should not be empty")
        assertNotEquals(lightCss, darkCss, "Light and dark OrgMode CSS should differ")
    }

    @Test
    fun snapshotStyleSheetAsciidocLightVsDark() {
        val lightCss = StyleSheets.getStyleSheet(TextFormat.ID_ASCIIDOC, true)
        val darkCss = StyleSheets.getStyleSheet(TextFormat.ID_ASCIIDOC, false)
        assertTrue(lightCss.isNotEmpty(), "AsciiDoc light CSS should not be empty")
        assertTrue(darkCss.isNotEmpty(), "AsciiDoc dark CSS should not be empty")
        assertNotEquals(lightCss, darkCss, "Light and dark AsciiDoc CSS should differ")
    }

    @Test
    fun snapshotStyleSheetRstLightVsDark() {
        val lightCss = StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, true)
        val darkCss = StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, false)
        assertTrue(lightCss.isNotEmpty(), "RST light CSS should not be empty")
        assertTrue(darkCss.isNotEmpty(), "RST dark CSS should not be empty")
        assertNotEquals(lightCss, darkCss, "Light and dark RST CSS should differ")
    }

    @Test
    fun snapshotStyleSheetWikitext() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_WIKITEXT, true)
        assertNotNull(css)
        assertTrue(css.isNotEmpty(), "Wikitext stylesheet should not be empty")
        assertTrue(css.contains(".wikitext"), "Wikitext CSS should target .wikitext class")
    }

    // ====================================================================
    // Cross-format regression tests
    // ====================================================================

    @Test
    fun snapshotAllParsersProduceNonEmptyOutput() {
        val parsersAndInputs: List<Pair<TextParser, String>> = listOf(
            MarkdownParser() to "# Hello\n\nWorld.\n",
            PlaintextParser() to "Plain text content.\n",
            TodoTxtParser() to "(A) Task @context +project\n",
            CsvParser() to "a,b,c\n1,2,3\n",
            WikitextParser() to "= Heading =\n\nText.\n",
            CreoleParser() to "= Heading\n\n**Bold**.\n",
            TiddlyWikiParser() to "title: Test\n\n! Heading\n",
            LatexParser() to "\\documentclass{article}\n\\begin{document}\nTest.\n\\end{document}\n",
            AsciidocParser() to "= Title\n\nParagraph.\n",
            OrgModeParser() to "* Heading\nBody.\n",
            RestructuredTextParser() to "Title\n=====\n\nParagraph.\n",
            KeyValueParser() to "key = value\n",
            TaskpaperParser() to "Project:\n\t- Task @done\n",
            TextileParser() to "h1. Heading\n\nParagraph.\n",
            JupyterParser() to """{"nbformat": 4, "nbformat_minor": 2, "metadata": {}, "cells": []}""",
            RMarkdownParser() to "---\ntitle: \"Test\"\n---\n\n# Heading\n",
            BinaryParser() to "binary content"
        )

        for ((parser, input) in parsersAndInputs) {
            val result = parser.parse(input)
            assertNotNull(result.parsedContent,
                "${parser.supportedFormat.name}: parsedContent should not be null")
            assertTrue(result.rawContent == input,
                "${parser.supportedFormat.name}: rawContent should match input")
        }
    }

    @Test
    fun snapshotFormatDetectionByExtensionNeverReturnsNull() {
        val extensions = listOf(
            "md", "txt", "csv", "tex", "org", "wiki", "adoc",
            "rst", "rmd", "taskpaper", "textile", "creole",
            "tid", "ipynb", "ini", "bin", "xyz", "unknown"
        )
        for (ext in extensions) {
            val format = FormatRegistry.detectByExtension(ext)
            assertNotNull(format, "detectByExtension('$ext') should never return null")
        }
    }

    @Test
    fun snapshotFormatIdConstants() {
        // Verify that all 17 format ID constants resolve to existing formats
        val ids = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_TODOTXT, TextFormat.ID_CSV,
            TextFormat.ID_LATEX, TextFormat.ID_PLAINTEXT, TextFormat.ID_ORGMODE,
            TextFormat.ID_WIKITEXT, TextFormat.ID_ASCIIDOC, TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_RMARKDOWN, TextFormat.ID_TASKPAPER, TextFormat.ID_TEXTILE,
            TextFormat.ID_CREOLE, TextFormat.ID_TIDDLYWIKI, TextFormat.ID_JUPYTER,
            TextFormat.ID_KEYVALUE, TextFormat.ID_BINARY
        )
        for (id in ids) {
            val format = FormatRegistry.getById(id)
            assertNotNull(format, "Format ID '$id' should exist in registry")
            assertEquals(id, format.id, "Format lookup should return matching ID")
        }
    }
}
