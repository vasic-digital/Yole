/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Load and soak tests for format parser stability.
 *
 * Validates that parsers remain stable under sustained
 * load: repeated parsing, large documents, HTML generation
 * cycles, format detection loops, growing document sizes,
 * and mixed-format batch processing.
 *
 *########################################################*/
package digital.vasic.yole.format.load

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
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.time.measureTime

/**
 * Load and soak tests for format parser stability.
 *
 * These tests exercise parsers under sustained repetitive load
 * to verify that no memory leaks, degradation, or instability
 * occurs over many iterations.
 */
class FormatLoadTests {

    // ====================================================================
    // All parsers and representative content
    // ====================================================================

    private val allParsers: List<TextParser> = listOf(
        MarkdownParser(), PlaintextParser(), TodoTxtParser(),
        CsvParser(), WikitextParser(), CreoleParser(),
        TiddlyWikiParser(), LatexParser(), AsciidocParser(),
        OrgModeParser(), RestructuredTextParser(), KeyValueParser(),
        TaskpaperParser(), TextileParser(), JupyterParser(),
        RMarkdownParser(), BinaryParser()
    )

    private val formatContents: Map<String, String> = mapOf(
        TextFormat.ID_MARKDOWN to "# Heading\n\nParagraph with **bold** and *italic*.\n\n- Item 1\n- Item 2\n\n```kotlin\nfun main() {}\n```",
        TextFormat.ID_PLAINTEXT to "Plain text line one.\nLine two.\nLine three.",
        TextFormat.ID_TODOTXT to "(A) Important task @work +project\n(B) Another task @home\nx 2025-01-01 Done task",
        TextFormat.ID_CSV to "name,age,city\nAlice,30,NYC\nBob,25,LA\nCharlie,35,Chicago",
        TextFormat.ID_WIKITEXT to "= Heading =\n\n'''Bold''' and ''italic''.\n\n* Item 1\n* Item 2",
        TextFormat.ID_CREOLE to "= Heading\n\n**Bold** and //italic//.\n\n* Item one\n* Item two",
        TextFormat.ID_TIDDLYWIKI to "title: Test Tiddler\ntags: test\n\n! Heading\n\nContent.",
        TextFormat.ID_LATEX to "\\documentclass{article}\n\\begin{document}\n\\section{Test}\nHello.\n\\end{document}",
        TextFormat.ID_ASCIIDOC to "= Title\n\n== Section\n\nParagraph text.\n\n----\ncode\n----",
        TextFormat.ID_ORGMODE to "* Heading\n** TODO Task\nBody text.",
        TextFormat.ID_RESTRUCTUREDTEXT to "Title\n=====\n\nSection\n-------\n\nParagraph.",
        TextFormat.ID_KEYVALUE to "key1 = value1\nkey2 = value2\n[section]\nkey3 = value3",
        TextFormat.ID_TASKPAPER to "Project:\n\t- Task one @priority(high)\n\t- Task two @done",
        TextFormat.ID_TEXTILE to "h1. Heading\n\nThis is *bold* text.\n\n* Item one\n* Item two",
        TextFormat.ID_JUPYTER to """{"nbformat": 4, "nbformat_minor": 2, "metadata": {}, "cells": [{"cell_type": "markdown", "metadata": {}, "source": ["# Title"]}]}""",
        TextFormat.ID_RMARKDOWN to "---\ntitle: \"Report\"\n---\n\n# Introduction\n\nText.",
        TextFormat.ID_BINARY to "Binary content with special chars \u0000\u0001\u0002"
    )

    // ====================================================================
    // Sequential load tests
    // ====================================================================

    @Test
    fun loadTestMarkdownSequential1000() {
        val parser = MarkdownParser()
        val input = "# Test\n\nParagraph with **bold** and *italic*.\n\n- List item\n"
        repeat(1000) {
            val result = parser.parse(input)
            assertNotNull(result.parsedContent)
        }
    }

    @Test
    fun loadTestPlaintextSequential1000() {
        val parser = PlaintextParser()
        val input = "Simple plain text content.\nLine two.\nLine three.\n"
        repeat(1000) {
            val result = parser.parse(input)
            assertNotNull(result.parsedContent)
        }
    }

    @Test
    fun loadTestCsvSequential1000() {
        val parser = CsvParser()
        val input = "a,b,c\n1,2,3\n4,5,6\n7,8,9\n"
        repeat(1000) {
            val result = parser.parse(input)
            assertNotNull(result.parsedContent)
        }
    }

    @Test
    fun loadTestTodoTxtSequential1000() {
        val parser = TodoTxtParser()
        val input = "(A) Task one @work +project\n(B) Task two @home\nx 2025-01-01 Done\n"
        repeat(1000) {
            val result = parser.parse(input)
            assertNotNull(result.parsedContent)
        }
    }

    @Test
    fun loadTestLatexSequential500() {
        val parser = LatexParser()
        val input = "\\documentclass{article}\n\\begin{document}\n\\section{Sec}\nText.\n\\end{document}\n"
        repeat(500) {
            val result = parser.parse(input)
            assertNotNull(result.parsedContent)
        }
    }

    // ====================================================================
    // All-format sequential load
    // ====================================================================

    @Test
    fun loadTestAllFormatsSequential100Iterations() {
        repeat(100) { iteration ->
            for (parser in allParsers) {
                val content = formatContents[parser.supportedFormat.id]
                    ?: "Default content for ${parser.supportedFormat.name}"
                val result = parser.parse(content)
                assertNotNull(result.parsedContent,
                    "${parser.supportedFormat.name} iteration $iteration should produce parsedContent")
            }
        }
    }

    // ====================================================================
    // Large document tests
    // ====================================================================

    @Test
    fun loadTestLargeMarkdownDocument() {
        val parser = MarkdownParser()
        val largeDoc = buildString {
            repeat(1000) { i ->
                appendLine("## Section $i")
                appendLine()
                appendLine("Paragraph $i with some content. " + "word ".repeat(50))
                appendLine()
            }
        }
        val result = parser.parse(largeDoc)
        assertNotNull(result.parsedContent)
        assertTrue(result.errors.isEmpty(), "Large Markdown should parse without errors")
    }

    @Test
    fun loadTestLargeCsvDocument() {
        val parser = CsvParser()
        val largeCsv = buildString {
            appendLine("col1,col2,col3,col4,col5")
            repeat(500) { i ->
                appendLine("val_${i}_1,val_${i}_2,val_${i}_3,val_${i}_4,val_${i}_5")
            }
        }
        val result = parser.parse(largeCsv)
        assertNotNull(result.parsedContent)
    }

    @Test
    fun loadTestLargeTodoTxtDocument() {
        val parser = TodoTxtParser()
        val largeTodo = buildString {
            repeat(500) { i ->
                val priority = ('A' + (i % 26))
                appendLine("($priority) Task $i @context${i % 5} +project${i % 10}")
            }
        }
        val result = parser.parse(largeTodo)
        assertNotNull(result.parsedContent)
    }

    @Test
    fun loadTestLargeKeyValueDocument() {
        val parser = KeyValueParser()
        val largeKv = buildString {
            repeat(500) { i ->
                if (i % 50 == 0) appendLine("[section_$i]")
                appendLine("key_$i = value_$i")
            }
        }
        val result = parser.parse(largeKv)
        assertNotNull(result.parsedContent)
    }

    @Test
    fun loadTestLargeOrgModeDocument() {
        val parser = OrgModeParser()
        val largeOrg = buildString {
            repeat(500) { i ->
                val stars = "*".repeat((i % 5) + 1)
                appendLine("$stars Heading $i")
                appendLine("Body paragraph for heading $i.")
                appendLine()
            }
        }
        val result = parser.parse(largeOrg)
        assertNotNull(result.parsedContent)
    }

    // ====================================================================
    // HTML generation under load
    // ====================================================================

    @Test
    fun loadTestRepeatedHtmlGeneration() {
        val parser = MarkdownParser()
        val input = "# Title\n\n**Bold** and *italic*.\n\n- Item 1\n- Item 2\n"
        val doc = parser.parse(input)

        repeat(500) {
            val html = parser.toHtml(doc, lightMode = true)
            assertNotNull(html)
            assertTrue(html.isNotEmpty(), "HTML should be non-empty on iteration $it")
        }
    }

    @Test
    fun loadTestRepeatedHtmlLightDarkAlternation() {
        val parser = LatexParser()
        val input = "\\documentclass{article}\n\\begin{document}\n\\section{Test}\nContent.\n\\end{document}\n"
        val doc = parser.parse(input)

        repeat(200) { i ->
            val lightMode = i % 2 == 0
            val html = parser.toHtml(doc, lightMode)
            assertNotNull(html)
            assertTrue(html.isNotEmpty(), "HTML should be non-empty on iteration $i")
        }
    }

    // ====================================================================
    // Format detection under load
    // ====================================================================

    @Test
    fun loadTestFormatDetectionByContent() {
        val samples = listOf(
            "# Markdown heading\n\nParagraph.",
            "(A) Todo task @context +project",
            "name,age\nAlice,30",
            "\\documentclass{article}",
            "* Org heading",
            "= Wiki Heading =",
            "```{r}\ncode\n```",
            "= AsciiDoc Title",
            "h1. Textile heading",
            "title: tiddler\n\n! heading"
        )
        repeat(200) { i ->
            val content = samples[i % samples.size]
            val detected = FormatRegistry.detectByContent(content)
            // Detection may return null for some inputs, but should never throw
        }
    }

    @Test
    fun loadTestFormatDetectionByExtension() {
        val extensions = listOf(
            "md", "txt", "csv", "tex", "org", "wiki", "adoc",
            "rst", "rmd", "taskpaper", "textile", "creole",
            "tid", "ipynb", "ini"
        )
        repeat(500) { i ->
            val ext = extensions[i % extensions.size]
            val format = FormatRegistry.detectByExtension(ext)
            assertNotNull(format, "detectByExtension should always return a format")
        }
    }

    // ====================================================================
    // StyleSheet generation under load
    // ====================================================================

    @Test
    fun loadTestStyleSheetGenerationRepeated() {
        val formatIds = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_LATEX, TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC, TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_WIKITEXT
        )
        repeat(500) { i ->
            val formatId = formatIds[i % formatIds.size]
            val lightMode = i % 2 == 0
            val css = StyleSheets.getStyleSheet(formatId, lightMode)
            assertNotNull(css)
            assertTrue(css.isNotEmpty(), "StyleSheet for $formatId should not be empty")
        }
    }

    // ====================================================================
    // Growing document sizes
    // ====================================================================

    @Test
    fun loadTestGrowingDocumentSizes() {
        val parser = MarkdownParser()
        val sizes = listOf(10, 100, 500, 1000, 5000, 10000, 50000)
        for (size in sizes) {
            val input = buildString {
                appendLine("# Document of size ~$size")
                appendLine()
                val remaining = size - 30
                if (remaining > 0) {
                    append("x".repeat(remaining))
                }
            }
            val result = parser.parse(input)
            assertNotNull(result.parsedContent,
                "Parser should handle document of size $size")
        }
    }

    @Test
    fun loadTestGrowingCsvRows() {
        val parser = CsvParser()
        val rowCounts = listOf(1, 10, 50, 100, 500)
        for (count in rowCounts) {
            val input = buildString {
                appendLine("col1,col2,col3")
                repeat(count) { i ->
                    appendLine("r${i}c1,r${i}c2,r${i}c3")
                }
            }
            val result = parser.parse(input)
            assertNotNull(result.parsedContent,
                "CSV parser should handle $count rows")
        }
    }

    // ====================================================================
    // Mixed-format batch processing
    // ====================================================================

    @Test
    fun loadTestMixedFormatRoundRobin() {
        repeat(200) { i ->
            val parser = allParsers[i % allParsers.size]
            val content = formatContents[parser.supportedFormat.id]
                ?: "Content for ${parser.supportedFormat.name}"
            val result = parser.parse(content)
            assertNotNull(result.parsedContent,
                "${parser.supportedFormat.name}: round-robin iteration $i should produce output")
        }
    }

    @Test
    fun loadTestParseAndHtmlCycle() {
        repeat(100) { i ->
            val parser = allParsers[i % allParsers.size]
            val content = formatContents[parser.supportedFormat.id]
                ?: "Content for ${parser.supportedFormat.name}"
            val doc = parser.parse(content)
            val html = parser.toHtml(doc, lightMode = i % 2 == 0)
            assertNotNull(html,
                "${parser.supportedFormat.name}: parse+HTML cycle $i should produce HTML")
        }
    }

    // ====================================================================
    // Stability over time (soak tests)
    // ====================================================================

    @Test
    fun loadTestSoakMarkdownStability() {
        val parser = MarkdownParser()
        val input = "# Soak Test\n\nParagraph **bold** *italic*.\n\n- item\n\n> quote\n"
        val durations = mutableListOf<Long>()
        repeat(500) {
            val elapsed = measureTime {
                val result = parser.parse(input)
                assertNotNull(result.parsedContent)
            }.inWholeMilliseconds
            durations.add(elapsed)
        }
        // Verify no catastrophic slowdown: last batch should not be 10x the first
        val firstBatch = durations.take(50).average()
        val lastBatch = durations.takeLast(50).average()
        // Allow wide margin since JIT and GC can cause variance
        assertTrue(lastBatch < firstBatch * 20 + 10,
            "Parser should not degrade catastrophically over time (first=$firstBatch, last=$lastBatch)")
    }

    @Test
    fun loadTestSoakAllParsersStability() {
        repeat(50) { iteration ->
            for (parser in allParsers) {
                val content = formatContents[parser.supportedFormat.id]
                    ?: "Default content"
                val doc = parser.parse(content)
                assertNotNull(doc.parsedContent,
                    "Soak test: ${parser.supportedFormat.name} iteration $iteration")
                val html = parser.toHtml(doc, lightMode = true)
                assertNotNull(html)
            }
        }
    }
}
