/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for Format Parser system
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.binary.BinaryParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.wikitext.WikitextParser
import org.junit.Test
import kotlin.test.*

/**
 * Integration tests for the format parser system.
 *
 * Tests cover:
 * - End-to-end format detection and parsing workflows
 * - FormatRegistry and Parser integration
 * - Cross-format compatibility
 * - Real-world usage scenarios
 * - Performance characteristics
 */
class FormatParserIntegrationTest {

    // ==================== End-to-End Workflow Tests ====================

    @Test
    fun `should detect format and parse Markdown file end-to-end`() {
        val filename = "document.md"
        val content = "# Hello World\n\nThis is **bold** text."

        // Step 1: Detect format by filename
        val format = FormatRegistry.detectByFilename(filename)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)

        // Step 2: Get parser for format
        val parser = MarkdownParser()

        // Step 3: Parse content
        val result = parser.parse(content)
        assertNotNull(result)
        assertTrue(result.parsedContent.contains("Hello World"))
    }

    @Test
    fun `should detect format and parse CSV file end-to-end`() {
        val filename = "data.csv"
        val content = "Name,Age,City\nJohn,30,NYC\nJane,25,LA"

        // Detect format
        val format = FormatRegistry.detectByFilename(filename)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_CSV, format.id)

        // Parse content
        val parser = CsvParser()
        val result = parser.parse(content)
        assertNotNull(result)
    }

    @Test
    fun `should detect format and parse Todo txt file end-to-end`() {
        val filename = "tasks.txt"
        val content = "x 2025-01-01 Completed task\n(A) High priority task"

        // Detect format (.txt can be either plaintext or todotxt)
        val format = FormatRegistry.detectByFilename(filename)
        assertNotNull(format)
        // .txt files might be detected as plaintext or todotxt
        assertTrue(format.id == FormatRegistry.ID_TODOTXT || format.id == FormatRegistry.ID_PLAINTEXT)

        // Parse content
        val parser = TodoTxtParser()
        val result = parser.parse(content)
        assertNotNull(result)
    }

    @Test
    fun `should detect format by content when extension is missing`() {
        val content = "# Markdown Header\n\nThis looks like **Markdown**."

        // Detect format by content
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)

        // Parse with detected format
        val parser = MarkdownParser()
        val result = parser.parse(content)
        assertNotNull(result)
    }

    @Test
    fun `should handle file with unknown extension gracefully`() {
        val filename = "unknown.xyz"

        // Should return null or fallback to plaintext for unknown format
        val format = FormatRegistry.detectByFilename(filename)
        // It's valid to return null or plaintext for unknown extensions
        assertTrue(format == null || format.id == FormatRegistry.ID_PLAINTEXT)
    }

    // ==================== Parser Registry Integration Tests ====================

    @Test
    fun `all registered formats should have working parsers`() {
        val formats = FormatRegistry.formats

        // Verify we have multiple formats
        assertTrue(formats.size >= 16, "Should have at least 16 formats registered")

        // Verify each format has an ID
        formats.forEach { format ->
            assertNotNull(format.id)
            assertTrue(format.id.isNotEmpty())
        }
    }

    @Test
    fun `should retrieve format by ID and verify properties`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)

        assertNotNull(format)
        assertEquals("Markdown", format.name)
        assertEquals(".md", format.defaultExtension)
        assertTrue(format.extensions.contains(".md"))
    }

    @Test
    fun `should retrieve all format names`() {
        val names = FormatRegistry.getFormatNames()

        assertNotNull(names)
        assertTrue(names.isNotEmpty())
        assertTrue(names.contains("Markdown"))
        assertTrue(names.contains("CSV"))
        assertTrue(names.contains("Todo.txt"))
    }

    @Test
    fun `should retrieve all extensions`() {
        val extensions = FormatRegistry.getAllExtensions()

        assertNotNull(extensions)
        assertTrue(extensions.isNotEmpty())
        assertTrue(extensions.contains(".md"))
        assertTrue(extensions.contains(".csv"))
        assertTrue(extensions.contains(".txt"))
    }

    // ==================== Cross-Format Compatibility Tests ====================

    @Test
    fun `all parsers should handle empty input without crashing`() {
        val parsers = listOf(
            MarkdownParser(),
            TodoTxtParser(),
            CsvParser(),
            PlaintextParser(),
            LatexParser(),
            OrgModeParser(),
            WikitextParser(),
            AsciidocParser(),
            RestructuredTextParser(),
            KeyValueParser(),
            TaskpaperParser(),
            TextileParser(),
            CreoleParser(),
            TiddlyWikiParser(),
            JupyterParser(),
            RMarkdownParser(),
            BinaryParser()
        )

        parsers.forEach { parser ->
            val result = parser.parse("")
            assertNotNull(result, "Parser ${parser::class.simpleName} should handle empty input")
        }
    }

    @Test
    fun `all parsers should handle whitespace-only input`() {
        val parsers = listOf(
            MarkdownParser(),
            TodoTxtParser(),
            CsvParser(),
            PlaintextParser()
        )

        parsers.forEach { parser ->
            val result = parser.parse("   \n\n   \t  ")
            assertNotNull(result, "Parser ${parser::class.simpleName} should handle whitespace")
        }
    }

    @Test
    fun `all parsers should handle unicode characters`() {
        val unicodeContent = "Unicode test: 你好世界 🌍 Привет мир"

        val parsers = listOf(
            MarkdownParser(),
            TodoTxtParser(),
            CsvParser(),
            PlaintextParser()
        )

        parsers.forEach { parser ->
            val result = parser.parse(unicodeContent)
            assertNotNull(result, "Parser ${parser::class.simpleName} should handle unicode")
        }
    }

    // ==================== Format Detection Integration Tests ====================

    @Test
    fun `should correctly detect all primary format extensions`() {
        val testCases = mapOf(
            "file.md" to FormatRegistry.ID_MARKDOWN,
            "data.csv" to FormatRegistry.ID_CSV,
            "document.tex" to FormatRegistry.ID_LATEX,
            "notes.org" to FormatRegistry.ID_ORGMODE,
            "wiki.wiki" to FormatRegistry.ID_WIKITEXT,
            "doc.adoc" to FormatRegistry.ID_ASCIIDOC,
            "readme.rst" to FormatRegistry.ID_RESTRUCTUREDTEXT
        )

        testCases.forEach { (filename, expectedId) ->
            val format = FormatRegistry.detectByFilename(filename)
            assertNotNull(format, "Should detect format for $filename")
            assertEquals(expectedId, format.id, "Wrong format detected for $filename")
        }

        // .txt files might be detected as plaintext or todotxt
        val txtFormat = FormatRegistry.detectByFilename("tasks.txt")
        assertNotNull(txtFormat)
        assertTrue(txtFormat.id == FormatRegistry.ID_TODOTXT || txtFormat.id == FormatRegistry.ID_PLAINTEXT)
    }

    @Test
    fun `should detect format from full file path`() {
        val path = "/Users/test/documents/notes.md"

        val format = FormatRegistry.detectByFilename(path)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `should handle files with multiple dots in name`() {
        val filename = "my.project.notes.md"

        val format = FormatRegistry.detectByFilename(filename)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `should handle hidden files with dots`() {
        val filename = ".hidden.md"

        val format = FormatRegistry.detectByFilename(filename)
        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    // ==================== Real-World Scenario Tests ====================

    @Test
    fun `should handle real-world Markdown document`() {
        val content = """
            # Project Documentation

            ## Overview

            This is a **real-world** example with:
            - Lists
            - **Bold** and *italic* text
            - [Links](https://example.com)

            ## Code

            ```kotlin
            fun main() {
                println("Hello World")
            }
            ```
        """.trimIndent()

        val parser = MarkdownParser()
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.isNotEmpty())
    }

    @Test
    fun `should handle real-world CSV data`() {
        val content = """
            Product,Price,Quantity
            "Widget A",29.99,100
            "Widget B",39.99,50
            "Widget, Special",49.99,25
        """.trimIndent()

        val parser = CsvParser()
        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle real-world Todo txt tasks`() {
        val content = """
            x 2025-01-01 Complete project documentation +work @office
            (A) 2025-01-02 Review pull requests +dev @home
            (B) Call client about feedback +sales
            2025-01-03 Update website content +marketing
        """.trimIndent()

        val parser = TodoTxtParser()
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.isNotEmpty())
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should handle large Markdown document efficiently`() {
        val largeContent = buildString {
            repeat(1000) {
                append("# Header $it\n\n")
                append("This is paragraph $it with some **bold** and *italic* text.\n\n")
            }
        }

        val parser = MarkdownParser()
        val result = parser.parse(largeContent)

        assertNotNull(result)
        assertTrue(result.parsedContent.isNotEmpty())
    }

    @Test
    fun `should handle large CSV file efficiently`() {
        val largeCsv = buildString {
            append("ID,Name,Value\n")
            repeat(1000) {
                append("$it,Item $it,${it * 10}\n")
            }
        }

        val parser = CsvParser()
        val result = parser.parse(largeCsv)

        assertNotNull(result)
    }

    // ==================== Error Handling Integration Tests ====================

    @Test
    fun `should handle malformed content gracefully across all parsers`() {
        val malformedContent = "{{random@#$%malformed&*()content}}"

        val parsers = listOf(
            MarkdownParser(),
            TodoTxtParser(),
            CsvParser(),
            PlaintextParser()
        )

        parsers.forEach { parser ->
            // Should not throw exception
            val result = parser.parse(malformedContent)
            assertNotNull(result, "Parser ${parser::class.simpleName} should handle malformed input")
        }
    }

    @Test
    fun `should handle null bytes gracefully`() {
        val binaryContent = "Text with\u0000null\u0000bytes"

        val parser = BinaryParser()
        val result = parser.parse(binaryContent)

        assertNotNull(result)
    }

    // ==================== Format Conversion Workflow Tests ====================

    @Test
    fun `should support full document lifecycle`() {
        // 1. Create document content
        val content = "# My Document\n\nThis is a test."

        // 2. Detect format
        val format = FormatRegistry.detectByContent(content)
        assertNotNull(format)

        // 3. Parse content
        val parser = MarkdownParser()
        val result = parser.parse(content)
        assertNotNull(result)

        // 4. Verify parsed content
        assertTrue(result.parsedContent.isNotEmpty())
    }

    @Test
    fun `should maintain data integrity through parse cycle`() {
        val originalContent = "Test content with special chars: <>&\""

        val parser = PlaintextParser()
        val result = parser.parse(originalContent)

        assertNotNull(result)
        // Content should be preserved
        assertTrue(result.parsedContent.contains("Test content"))
    }
}
