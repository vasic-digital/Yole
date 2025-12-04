/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Performance and stress tests for parser system
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlin.test.*

/**
 * Performance and stress tests for the parser system.
 *
 * Tests cover:
 * - Large document handling
 * - Repeated parsing operations
 * - Memory efficiency
 * - Parsing throughput
 * - Complex structure handling
 * - Registry operations under load
 */
class PerformanceTest {

    // ==================== Large Document Tests ====================

    @Test
    fun `should handle 10MB plain text document`() {
        val parser = PlaintextParser()
        val largeContent = "A".repeat(10 * 1024 * 1024) // 10MB

        val document = parser.parse(largeContent)

        assertNotNull(document)
        assertEquals(10 * 1024 * 1024, document.rawContent.length)
    }

    @Test
    fun `should handle 100000 line document`() {
        val parser = PlaintextParser()
        val lines = (1..100000).joinToString("\n") { "Line $it" }

        val document = parser.parse(lines)

        assertNotNull(document)
        val lineCount = document.metadata["lines"]?.toIntOrNull() ?: 0
        assertTrue(lineCount >= 100000)
    }

    @Test
    fun `should handle very wide CSV (1000 columns)`() {
        val parser = CsvParser()
        val header = (1..1000).joinToString(",") { "col$it" }
        val row = (1..1000).joinToString(",") { "val$it" }
        val csv = "$header\n$row\n$row\n$row"

        val document = parser.parse(csv)

        assertNotNull(document)
    }

    @Test
    fun `should handle very long CSV (10000 rows)`() {
        val parser = CsvParser()
        val csv = buildString {
            append("id,name,value\n")
            repeat(10000) {
                append("$it,Name$it,Value$it\n")
            }
        }

        val document = parser.parse(csv)

        assertNotNull(document)
    }

    @Test
    fun `should handle complex markdown document`() {
        val parser = MarkdownParser()
        val complex = buildString {
            repeat(1000) { i ->
                append("# Header $i\n\n")
                append("Paragraph with **bold** and *italic* text.\n\n")
                append("- List item 1\n")
                append("- List item 2\n")
                append("- List item 3\n\n")
                append("```kotlin\n")
                append("fun example$i() = println(\"Hello\")\n")
                append("```\n\n")
                append("> Blockquote\n\n")
                append("[Link $i](https://example.com/$i)\n\n")
                append("---\n\n")
            }
        }

        val document = parser.parse(complex)

        assertNotNull(document)
    }

    // ==================== Repeated Operations Tests ====================

    @Test
    fun `should handle 1000 sequential parses`() {
        val parser = PlaintextParser()

        repeat(1000) { i ->
            val document = parser.parse("Content $i")
            assertNotNull(document)
            assertEquals("Content $i", document.rawContent)
        }
    }

    @Test
    fun `should handle alternating small and large content`() {
        val parser = PlaintextParser()
        val small = "Small"
        val large = "L".repeat(100000)

        repeat(100) { i ->
            val content = if (i % 2 == 0) small else large
            val document = parser.parse(content)
            assertNotNull(document)
        }
    }

    @Test
    fun `should handle repeated format registry lookups`() {
        repeat(10000) {
            val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
            assertNotNull(format)
        }
    }

    @Test
    fun `should handle repeated extension lookups`() {
        repeat(10000) {
            val format = FormatRegistry.detectByExtension(".md")
            assertNotNull(format)
        }
    }

    @Test
    fun `should handle repeated content detection`() {
        val content = "# Markdown Header"

        repeat(1000) {
            val format = FormatRegistry.detectByContent(content)
            assertNotNull(format)
        }
    }

    // ==================== Memory Efficiency Tests ====================

    @Test
    fun `should handle many small documents efficiently`() {
        val parser = PlaintextParser()
        val documents = mutableListOf<ParsedDocument>()

        repeat(10000) { i ->
            val document = parser.parse("Doc $i")
            documents.add(document)
        }

        assertEquals(10000, documents.size)
        // All documents should be independent
        assertEquals("Doc 0", documents[0].rawContent)
        assertEquals("Doc 9999", documents[9999].rawContent)
    }

    @Test
    fun `should not retain references to previous parses`() {
        val parser = PlaintextParser()
        var lastDocument: ParsedDocument? = null

        repeat(100) { i ->
            val document = parser.parse("Content $i")
            // Previous document should not affect new one
            if (lastDocument != null) {
                assertNotEquals(lastDocument!!.rawContent, document.rawContent)
            }
            lastDocument = document
        }
    }

    @Test
    fun `should handle parser re-initialization`() {
        repeat(100) {
            val parser = PlaintextParser()
            val document = parser.parse("Content")
            assertNotNull(document)
        }
    }

    // ==================== Complex Structure Tests ====================

    @Test
    fun `should handle deeply nested markdown lists`() {
        val parser = MarkdownParser()
        val nested = buildString {
            var indent = ""
            repeat(100) { i ->
                append("$indent- Item at depth $i\n")
                indent += "  "
            }
        }

        val document = parser.parse(nested)

        assertNotNull(document)
    }

    @Test
    fun `should handle many markdown tables`() {
        val parser = MarkdownParser()
        val tables = buildString {
            repeat(100) {
                append("| Col1 | Col2 | Col3 |\n")
                append("|------|------|------|\n")
                append("| A    | B    | C    |\n")
                append("| D    | E    | F    |\n\n")
            }
        }

        val document = parser.parse(tables)

        assertNotNull(document)
    }

    @Test
    fun `should handle complex todotxt with all features`() {
        val parser = TodoTxtParser()
        val complex = buildString {
            repeat(1000) { i ->
                append("(${('A'..'Z').random()}) ")
                append("${2025 - (i % 10)}-01-01 ")
                append("Task $i ")
                append("+project${i % 10} ")
                append("@context${i % 5} ")
                append("key${i % 3}:value$i\n")
            }
        }

        val document = parser.parse(complex)

        assertNotNull(document)
    }

    // ==================== Throughput Tests ====================

    @Test
    fun `should parse multiple formats efficiently`() {
        val markdown = MarkdownParser()
        val todotxt = TodoTxtParser()
        val csv = CsvParser()
        val plain = PlaintextParser()

        repeat(250) { i ->
            val mdDoc = markdown.parse("# Header $i")
            val todoDoc = todotxt.parse("(A) Task $i")
            val csvDoc = csv.parse("col1,col2\nval1,val2")
            val plainDoc = plain.parse("Plain text $i")

            assertNotNull(mdDoc)
            assertNotNull(todoDoc)
            assertNotNull(csvDoc)
            assertNotNull(plainDoc)
        }
    }

    @Test
    fun `should handle format detection for many files`() {
        val extensions = listOf(".md", ".txt", ".csv", ".tex", ".org", ".wiki")

        repeat(1000) { i ->
            val ext = extensions[i % extensions.size]
            val format = FormatRegistry.detectByExtension(ext)
            assertNotNull(format)
        }
    }

    @Test
    fun `should handle parser registry operations`() {
        // This test validates that ParserRegistry remains consistent under load
        ParserInitializer.registerAllParsers()

        repeat(1000) { i ->
            val formatId = when (i % 4) {
                0 -> FormatRegistry.ID_MARKDOWN
                1 -> FormatRegistry.ID_CSV
                2 -> FormatRegistry.ID_PLAINTEXT
                else -> FormatRegistry.ID_TODOTXT
            }

            val format = FormatRegistry.getById(formatId)
            assertNotNull(format)

            val parser = ParserRegistry.getParser(format)
            assertNotNull(parser)
        }

        // Clean up
        ParserRegistry.clear()
    }

    // ==================== Edge Case Performance Tests ====================

    @Test
    fun `should handle empty content repeatedly`() {
        val parser = PlaintextParser()

        repeat(10000) {
            val document = parser.parse("")
            assertNotNull(document)
            assertEquals("", document.rawContent)
        }
    }

    @Test
    fun `should handle single character repeatedly`() {
        val parser = PlaintextParser()

        repeat(10000) { i ->
            val char = ('A'..'Z').toList()[i % 26].toString()
            val document = parser.parse(char)
            assertNotNull(document)
            assertEquals(char, document.rawContent)
        }
    }

    @Test
    fun `should handle various unicode strings`() {
        val parser = PlaintextParser()
        val unicodeStrings = listOf(
            "Hello",
            "Привет",
            "你好",
            "مرحبا",
            "שלום",
            "こんにちは",
            "안녕하세요",
            "Γειά",
            "नमस्ते",
            "สวัสดี"
        )

        repeat(1000) { i ->
            val text = unicodeStrings[i % unicodeStrings.size]
            val document = parser.parse(text)
            assertNotNull(document)
        }
    }

    // ==================== Stress Tests ====================

    @Test
    fun `should handle maximum realistic markdown document`() {
        val parser = MarkdownParser()
        val maxDoc = buildString {
            // Simulate a large technical document
            append("# Technical Documentation\n\n")

            repeat(500) { section ->
                append("## Section $section\n\n")
                append("Lorem ipsum dolor sit amet.\n\n")

                // Add some code blocks
                append("```kotlin\n")
                repeat(10) { line ->
                    append("val variable$line = \"value$line\"\n")
                }
                append("```\n\n")

                // Add a table
                append("| Feature | Status | Notes |\n")
                append("|---------|--------|-------|\n")
                repeat(5) { row ->
                    append("| Feature $row | ✓ | Notes $row |\n")
                }
                append("\n")
            }
        }

        val document = parser.parse(maxDoc)

        assertNotNull(document)
        assertTrue(document.rawContent.length > 100000)
    }

    @Test
    fun `should handle maximum CSV data`() {
        val parser = CsvParser()
        val maxCsv = buildString {
            // Header with 50 columns
            append((1..50).joinToString(",") { "col$it" })
            append("\n")

            // 5000 rows
            repeat(5000) { row ->
                append((1..50).joinToString(",") { "val${row}_$it" })
                append("\n")
            }
        }

        val document = parser.parse(maxCsv)

        assertNotNull(document)
    }

    @Test
    fun `should handle complex mixed content`() {
        val parser = MarkdownParser()
        val mixed = buildString {
            repeat(100) {
                // Mix of all markdown features
                append("# H1\n## H2\n### H3\n\n")
                append("**Bold** *Italic* ~~Strike~~\n\n")
                append("> Blockquote\n\n")
                append("- List 1\n- List 2\n\n")
                append("1. Ordered 1\n2. Ordered 2\n\n")
                append("```\ncode block\n```\n\n")
                append("[Link](url)\n\n")
                append("![Image](url)\n\n")
                append("---\n\n")
            }
        }

        val document = parser.parse(mixed)

        assertNotNull(document)
    }

    // ==================== Consistency Tests ====================

    @Test
    fun `should produce consistent results for same input`() {
        val parser = PlaintextParser()
        val content = "Test content for consistency"

        val results = (1..100).map {
            parser.parse(content)
        }

        // All results should be equal
        results.forEach { doc ->
            assertEquals(content, doc.rawContent)
            assertEquals(results[0].format, doc.format)
        }
    }

    @Test
    fun `should handle format registry consistency`() {
        val markdown1 = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val markdown2 = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)

        assertEquals(markdown1, markdown2)

        // Repeated lookups should return consistent results
        repeat(1000) {
            val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
            assertEquals(markdown1, format)
        }
    }
}
