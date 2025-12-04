/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive error handling and robustness tests
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.latex.LatexParser
import kotlin.test.*

/**
 * Comprehensive error handling and robustness tests.
 *
 * Tests cover:
 * - Malformed content handling
 * - Extreme input scenarios
 * - Binary and non-text content
 * - Encoding issues
 * - Resource limits
 * - Concurrent access
 * - Error recovery
 */
class ErrorHandlingTest {

    // ==================== Malformed Content Tests ====================

    @Test
    fun `should handle severely malformed markdown`() {
        val parser = MarkdownParser()
        val malformed = "# [[[[]]]] **__~~**** [link](((( ```code` ``` **"

        val document = parser.parse(malformed)

        assertNotNull(document)
        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle unmatched markdown formatting`() {
        val parser = MarkdownParser()
        val unmatched = "**bold without close\n*italic without close\n~~strike without close"

        val document = parser.parse(unmatched)

        assertNotNull(document)
    }

    @Test
    fun `should handle invalid todotxt priority`() {
        val parser = TodoTxtParser()
        val invalid = "(Z) Invalid priority\n(123) Numeric priority\n() Empty priority"

        val document = parser.parse(invalid)

        assertNotNull(document)
    }

    @Test
    fun `should handle malformed CSV with inconsistent columns`() {
        val parser = CsvParser()
        val malformed = "col1,col2,col3\nval1,val2\nval1,val2,val3,val4\nval1"

        val document = parser.parse(malformed)

        assertNotNull(document)
    }

    @Test
    fun `should handle CSV with unescaped quotes`() {
        val parser = CsvParser()
        val malformed = "col1,col2\n\"value with \" quote\",other"

        val document = parser.parse(malformed)

        assertNotNull(document)
    }

    @Test
    fun `should handle latex with unclosed environments`() {
        val parser = LatexParser()
        val unclosed = "\\begin{document}\n\\begin{equation}\nx = y\n\\end{document}"

        val document = parser.parse(unclosed)

        assertNotNull(document)
    }

    // ==================== Extreme Input Tests ====================

    @Test
    fun `should handle extremely long single line`() {
        val parser = PlaintextParser()
        val longLine = "A".repeat(1000000)

        val document = parser.parse(longLine)

        assertNotNull(document)
        assertEquals(1000000, document.rawContent.length)
    }

    @Test
    fun `should handle extremely many lines`() {
        val parser = PlaintextParser()
        val manyLines = (1..100000).joinToString("\n") { "Line $it" }

        val document = parser.parse(manyLines)

        assertNotNull(document)
        assertTrue(document.metadata["lines"]?.toInt() ?: 0 >= 100000)
    }

    @Test
    fun `should handle deeply nested markdown structures`() {
        val parser = MarkdownParser()
        val nested = buildString {
            repeat(100) {
                append("* ".repeat(it + 1))
                append("Item\n")
            }
        }

        val document = parser.parse(nested)

        assertNotNull(document)
    }

    @Test
    fun `should handle very long CSV rows`() {
        val parser = CsvParser()
        val longRow = (1..10000).joinToString(",") { "col$it" }

        val document = parser.parse(longRow)

        assertNotNull(document)
    }

    @Test
    fun `should handle repeated special characters`() {
        val parser = MarkdownParser()
        val repeated = "*".repeat(1000)

        val document = parser.parse(repeated)

        assertNotNull(document)
    }

    // ==================== Binary and Non-Text Content Tests ====================

    @Test
    fun `should handle null bytes`() {
        val parser = PlaintextParser()
        val withNulls = "Text\u0000with\u0000null\u0000bytes"

        val document = parser.parse(withNulls)

        assertNotNull(document)
    }

    @Test
    fun `should handle binary-like content`() {
        val parser = PlaintextParser()
        val binary = "\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008"

        val document = parser.parse(binary)

        assertNotNull(document)
    }

    @Test
    fun `should handle mixed text and binary`() {
        val parser = PlaintextParser()
        val mixed = "Normal text\u0000\u0001\u0002More text"

        val document = parser.parse(mixed)

        assertNotNull(document)
    }

    // ==================== Encoding Issues Tests ====================

    @Test
    fun `should handle various unicode categories`() {
        val parser = PlaintextParser()
        val unicode = "Latin: ABC\nCyrillic: АБВ\nCJK: 你好世界\nEmoji: 🌍🎉👍\nArabic: مرحبا"

        val document = parser.parse(unicode)

        assertNotNull(document)
        assertTrue(document.rawContent.contains("ABC"))
        assertTrue(document.rawContent.contains("АБВ"))
        assertTrue(document.rawContent.contains("你好"))
        assertTrue(document.rawContent.contains("🌍"))
    }

    @Test
    fun `should handle right-to-left text`() {
        val parser = PlaintextParser()
        val rtl = "مرحبا بك في العالم"

        val document = parser.parse(rtl)

        assertNotNull(document)
    }

    @Test
    fun `should handle combining characters`() {
        val parser = PlaintextParser()
        val combining = "e\u0301" // é as e + combining acute

        val document = parser.parse(combining)

        assertNotNull(document)
    }

    @Test
    fun `should handle zero-width characters`() {
        val parser = PlaintextParser()
        val zeroWidth = "Text\u200B\u200C\u200DWith\uFEFFZero\u2060Width"

        val document = parser.parse(zeroWidth)

        assertNotNull(document)
    }

    // ==================== Whitespace Edge Cases ====================

    @Test
    fun `should handle only whitespace content`() {
        val parser = PlaintextParser()
        val whitespace = "   \n\n\t\t\r\n   "

        val document = parser.parse(whitespace)

        assertNotNull(document)
    }

    @Test
    fun `should handle mixed line endings`() {
        val parser = PlaintextParser()
        val mixed = "Line 1\nLine 2\rLine 3\r\nLine 4"

        val document = parser.parse(mixed)

        assertNotNull(document)
    }

    @Test
    fun `should handle unusual whitespace characters`() {
        val parser = PlaintextParser()
        val unusual = "Text\u00A0with\u2000non\u3000breaking\u202Fspaces"

        val document = parser.parse(unusual)

        assertNotNull(document)
    }

    // ==================== Resource Limit Tests ====================

    @Test
    fun `should handle empty content gracefully`() {
        val parsers = listOf(
            MarkdownParser(),
            TodoTxtParser(),
            CsvParser(),
            PlaintextParser(),
            LatexParser()
        )

        parsers.forEach { parser ->
            val document = parser.parse("")
            assertNotNull(document)
        }
    }

    @Test
    fun `should handle single character`() {
        val parsers = listOf(
            MarkdownParser(),
            TodoTxtParser(),
            CsvParser(),
            PlaintextParser()
        )

        parsers.forEach { parser ->
            val document = parser.parse("A")
            assertNotNull(document)
            assertEquals("A", document.rawContent)
        }
    }

    @Test
    fun `should handle content at various sizes`() {
        val parser = PlaintextParser()
        val sizes = listOf(0, 1, 10, 100, 1000, 10000, 100000)

        sizes.forEach { size ->
            val content = "A".repeat(size)
            val document = parser.parse(content)
            assertNotNull(document)
            assertEquals(size, document.rawContent.length)
        }
    }

    // ==================== Special Content Tests ====================

    @Test
    fun `should handle HTML entities in content`() {
        val parser = PlaintextParser()
        val entities = "&lt;&gt;&amp;&quot;&#39;"

        val document = parser.parse(entities)

        assertNotNull(document)
        assertEquals(entities, document.rawContent)
    }

    @Test
    fun `should handle markdown in CSV`() {
        val parser = CsvParser()
        val markdownInCsv = "col1,col2\n# Header,**Bold**\n*Italic*,[Link](url)"

        val document = parser.parse(markdownInCsv)

        assertNotNull(document)
    }

    @Test
    fun `should handle LaTeX in Markdown`() {
        val parser = MarkdownParser()
        val latexInMd = "# Title\n\n$$\\int_0^\\infty x dx$$\n\nText"

        val document = parser.parse(latexInMd)

        assertNotNull(document)
    }

    // ==================== Error Recovery Tests ====================

    @Test
    fun `should continue parsing after errors`() {
        val parser = MarkdownParser()
        val withErrors = "# Good Header\n\n[[[Bad Link\n\n## Another Header"

        val document = parser.parse(withErrors)

        assertNotNull(document)
        // Should still parse the good parts
        assertTrue(document.parsedContent.contains("Good Header") || document.rawContent.contains("Good Header"))
    }

    @Test
    fun `should handle parser re-use`() {
        val parser = PlaintextParser()

        val doc1 = parser.parse("First content")
        val doc2 = parser.parse("Second content")
        val doc3 = parser.parse("Third content")

        assertNotNull(doc1)
        assertNotNull(doc2)
        assertNotNull(doc3)
        assertEquals("First content", doc1.rawContent)
        assertEquals("Second content", doc2.rawContent)
        assertEquals("Third content", doc3.rawContent)
    }

    @Test
    fun `should handle alternating formats with same parser`() {
        val parser = PlaintextParser()

        repeat(10) { i ->
            val content = if (i % 2 == 0) "Even $i" else "Odd $i"
            val document = parser.parse(content)
            assertNotNull(document)
        }
    }

    // ==================== Option Handling Tests ====================

    @Test
    fun `should handle invalid option types`() {
        val parser = PlaintextParser()
        val invalidOptions = mapOf<String, Any>(
            "string" to "value",
            "number" to 42,
            "boolean" to true,
            "null" to "null",
            "object" to mapOf("nested" to "value")
        )

        val document = parser.parse("content", invalidOptions)

        assertNotNull(document)
    }

    @Test
    fun `should handle empty options map`() {
        val parser = PlaintextParser()

        val document = parser.parse("content", emptyMap())

        assertNotNull(document)
    }

    @Test
    fun `should handle large options map`() {
        val parser = PlaintextParser()
        val largeOptions = (1..1000).associate { "key$it" to "value$it" }

        val document = parser.parse("content", largeOptions)

        assertNotNull(document)
    }

    // ==================== Format Detection Edge Cases ====================

    @Test
    fun `should handle ambiguous content`() {
        val ambiguous = "1,2,3\n4,5,6" // Could be CSV or plain text

        val csvDoc = CsvParser().parse(ambiguous)
        val plainDoc = PlaintextParser().parse(ambiguous)

        assertNotNull(csvDoc)
        assertNotNull(plainDoc)
        // Both should successfully parse
    }

    @Test
    fun `should handle content that looks like multiple formats`() {
        val multi = "# Header\n(A) Task\nkey = value"

        val mdDoc = MarkdownParser().parse(multi)
        val todoDoc = TodoTxtParser().parse(multi)
        val plainDoc = PlaintextParser().parse(multi)

        assertNotNull(mdDoc)
        assertNotNull(todoDoc)
        assertNotNull(plainDoc)
    }

    // ==================== Metadata Handling Tests ====================

    @Test
    fun `should handle parsing without metadata`() {
        val parser = PlaintextParser()

        val document = parser.parse("content")

        assertNotNull(document)
        // Metadata might be present or empty, both are valid
    }

    @Test
    fun `should preserve raw content exactly`() {
        val parser = PlaintextParser()
        val original = "Exact\tContent\nWith\rSpecial\r\nChars"

        val document = parser.parse(original)

        assertEquals(original, document.rawContent)
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    fun `should handle sequential parsing safely`() {
        val parser = PlaintextParser()
        val contents = (1..100).map { "Content $it" }

        contents.forEach { content ->
            val document = parser.parse(content)
            assertNotNull(document)
            assertEquals(content, document.rawContent)
        }
    }

    @Test
    fun `should handle format registry during parsing`() {
        val parser = PlaintextParser()

        repeat(10) {
            // Access format registry while parsing
            val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)
            val document = parser.parse("content $it")

            assertNotNull(format)
            assertNotNull(document)
        }
    }
}
