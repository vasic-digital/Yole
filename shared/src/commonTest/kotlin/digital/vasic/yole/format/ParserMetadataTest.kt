/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for parser metadata extraction
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.latex.LatexParser
import kotlin.test.*

/**
 * Tests for metadata extraction by different parsers.
 *
 * Tests cover:
 * - Line counting
 * - Character counting
 * - Format-specific metadata (rows/columns, title/author, etc.)
 * - Metadata accuracy
 * - Edge cases (empty content, special characters)
 */
class ParserMetadataTest {

    // ==================== Markdown Metadata Tests ====================

    @Test
    fun `should extract line count from markdown`() {
        val parser = MarkdownParser()
        val content = """
            # Title

            Paragraph 1

            Paragraph 2
        """.trimIndent()

        val document = parser.parse(content)

        assertEquals("5", document.metadata["lines"])
    }

    @Test
    fun `should handle single line markdown`() {
        val parser = MarkdownParser()
        val document = parser.parse("# Single line")

        assertEquals("1", document.metadata["lines"])
    }

    @Test
    fun `should handle empty markdown`() {
        val parser = MarkdownParser()
        val document = parser.parse("")

        assertEquals("1", document.metadata["lines"])  // Empty string is 1 line
    }

    @Test
    fun `should extract extension from filename option in markdown`() {
        val parser = MarkdownParser()
        val options = mapOf("filename" to "test.md")

        val document = parser.parse("# Title", options)

        assertEquals(".md", document.metadata["extension"])
    }

    // ==================== Plain Text Metadata Tests ====================

    @Test
    fun `should extract line count from plaintext`() {
        val parser = PlaintextParser()
        val content = "Line 1\nLine 2\nLine 3"

        val document = parser.parse(content)

        assertEquals("3", document.metadata["lines"])
    }

    @Test
    fun `should extract character count from plaintext`() {
        val parser = PlaintextParser()
        val content = "Hello World"

        val document = parser.parse(content)

        assertEquals("11", document.metadata["characters"])
    }

    @Test
    fun `should count characters including whitespace`() {
        val parser = PlaintextParser()
        val content = "Hello   World"  // Extra spaces

        val document = parser.parse(content)

        assertEquals("13", document.metadata["characters"])
    }

    @Test
    fun `should handle multiline plaintext correctly`() {
        val parser = PlaintextParser()
        val content = "Line 1\nLine 2\nLine 3\n"

        val document = parser.parse(content)

        assertEquals("4", document.metadata["lines"])  // Trailing newline creates empty line
        assertTrue(document.metadata["characters"]!!.toInt() > 0)
    }

    // ==================== CSV Metadata Tests ====================

    @Test
    fun `should extract row count from CSV`() {
        val parser = CsvParser()
        val content = """
            col1,col2,col3
            val1,val2,val3
            val4,val5,val6
        """.trimIndent()

        val document = parser.parse(content)

        assertEquals("2", document.metadata["rows"])  // Excluding header
    }

    @Test
    fun `should extract column count from CSV`() {
        val parser = CsvParser()
        val content = "col1,col2,col3\nval1,val2,val3"

        val document = parser.parse(content)

        assertEquals("3", document.metadata["columns"])
    }

    @Test
    fun `should extract delimiter from CSV`() {
        val parser = CsvParser()
        val content = "col1;col2;col3\nval1;val2;val3"

        val document = parser.parse(content)

        assertEquals(";", document.metadata["delimiter"])
    }

    @Test
    fun `should indicate hasHeader in CSV`() {
        val parser = CsvParser()
        val content = "col1,col2\nval1,val2"

        val document = parser.parse(content)

        assertEquals("true", document.metadata["hasHeader"])
    }

    @Test
    fun `should handle single-row CSV`() {
        val parser = CsvParser()
        val content = "col1,col2,col3"

        val document = parser.parse(content)

        // Single row treated as header, so 0 data rows
        assertEquals("0", document.metadata["rows"])
        assertEquals("3", document.metadata["columns"])
    }

    @Test
    fun `should handle empty CSV`() {
        val parser = CsvParser()
        val document = parser.parse("")

        assertEquals("0", document.metadata["rows"])
    }

    // ==================== LaTeX Metadata Tests ====================

    @Test
    fun `should extract title from LaTeX`() {
        val parser = LatexParser()
        val content = """
            \documentclass{article}
            \title{My Document Title}
            \begin{document}
            Content here
            \end{document}
        """.trimIndent()

        val document = parser.parse(content)

        assertEquals("My Document Title", document.metadata["title"])
    }

    @Test
    fun `should extract author from LaTeX`() {
        val parser = LatexParser()
        val content = """
            \documentclass{article}
            \author{John Doe}
            \begin{document}
            Content
            \end{document}
        """.trimIndent()

        val document = parser.parse(content)

        assertEquals("John Doe", document.metadata["author"])
    }

    @Test
    fun `should extract date from LaTeX`() {
        val parser = LatexParser()
        val content = """
            \documentclass{article}
            \date{2023-01-01}
            \begin{document}
            Content
            \end{document}
        """.trimIndent()

        val document = parser.parse(content)

        assertEquals("2023-01-01", document.metadata["date"])
    }

    @Test
    fun `should extract documentclass from LaTeX`() {
        val parser = LatexParser()
        val content = """
            \documentclass{book}
            \begin{document}
            Content
            \end{document}
        """.trimIndent()

        val document = parser.parse(content)

        assertEquals("book", document.metadata["documentclass"])
    }

    @Test
    fun `should handle LaTeX without metadata commands`() {
        val parser = LatexParser()
        val content = """
            \begin{document}
            Just content
            \end{document}
        """.trimIndent()

        val document = parser.parse(content)

        // Should not have title, author, date if not specified
        assertFalse(document.metadata.containsKey("title") && document.metadata["title"]?.isNotEmpty() == true)
    }

    // ==================== Todo.txt Metadata Tests ====================

    @Test
    fun `should handle todo txt metadata extraction`() {
        val parser = TodoTxtParser()
        val content = """
            (A) Task 1 +Project1 @context1
            (B) Task 2 +Project2 @context2
            x Completed task
        """.trimIndent()

        val document = parser.parse(content)

        // Todo.txt parser should handle this without errors
        assertNotNull(document.metadata)
    }

    // ==================== Metadata Accuracy Tests ====================

    @Test
    fun `should count lines correctly with various line endings`() {
        val parser = PlaintextParser()

        // Unix line endings
        val unix = "Line1\nLine2\nLine3"
        val unixDoc = parser.parse(unix)
        assertEquals("3", unixDoc.metadata["lines"])

        // Windows line endings (will be normalized)
        val windows = "Line1\r\nLine2\r\nLine3"
        val windowsDoc = parser.parse(windows)
        assertTrue(windowsDoc.metadata["lines"]!!.toInt() >= 3)
    }

    @Test
    fun `should handle unicode in character count`() {
        val parser = PlaintextParser()
        val content = "Hello 世界 🌍"  // Mixed ASCII, CJK, emoji

        val document = parser.parse(content)

        // Character count should include all unicode characters
        assertTrue(document.metadata["characters"]!!.toInt() > 0)
    }

    @Test
    fun `should handle very large documents`() {
        val parser = PlaintextParser()
        val content = "Line\n".repeat(100000)

        val document = parser.parse(content)

        assertTrue(document.metadata["lines"]!!.toInt() >= 100000)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle content with only whitespace`() {
        val parser = PlaintextParser()
        val content = "   \n\n\t\t\n   "

        val document = parser.parse(content)

        assertNotNull(document.metadata["lines"])
        assertNotNull(document.metadata["characters"])
        assertTrue(document.metadata["lines"]!!.toInt() > 0)
    }

    @Test
    fun `should handle content with null bytes`() {
        val parser = PlaintextParser()
        val content = "Text\u0000with\u0000nulls"

        val document = parser.parse(content)

        assertNotNull(document.metadata["lines"])
        assertNotNull(document.metadata["characters"])
    }

    @Test
    fun `should handle content with control characters`() {
        val parser = PlaintextParser()
        val content = "Text\u0001\u0002\u0003"

        val document = parser.parse(content)

        assertNotNull(document.metadata)
    }

    // ==================== Metadata Consistency Tests ====================

    @Test
    fun `should produce consistent metadata on repeated parses`() {
        val parser = PlaintextParser()
        val content = "Test content\nLine 2\nLine 3"

        val doc1 = parser.parse(content)
        val doc2 = parser.parse(content)
        val doc3 = parser.parse(content)

        assertEquals(doc1.metadata["lines"], doc2.metadata["lines"])
        assertEquals(doc2.metadata["lines"], doc3.metadata["lines"])
        assertEquals(doc1.metadata["characters"], doc2.metadata["characters"])
    }

    @Test
    fun `should have non-null metadata for all parsers`() {
        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser(),
            CsvParser(),
            TodoTxtParser(),
            LatexParser()
        )

        parsers.forEach { parser ->
            val document = parser.parse("Test content")
            assertNotNull(document.metadata)
        }
    }

    // ==================== Options Metadata Tests ====================

    @Test
    fun `should respect filename option in metadata`() {
        val parser = MarkdownParser()
        val options = mapOf("filename" to "document.md")

        val document = parser.parse("# Title", options)

        assertTrue(document.metadata.containsKey("extension"))
    }

    @Test
    fun `should handle missing filename option gracefully`() {
        val parser = MarkdownParser()
        val document = parser.parse("# Title", emptyMap())

        // Should still work without filename
        assertNotNull(document.metadata)
    }

    @Test
    fun `should handle invalid filename option`() {
        val parser = MarkdownParser()
        val options = mapOf("filename" to "")

        val document = parser.parse("# Title", options)

        // Should handle empty filename gracefully
        assertNotNull(document.metadata)
    }
}
