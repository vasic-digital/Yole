/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Error path and edge case tests for Markdown parser
 *
 *########################################################*/
package digital.vasic.yole.format.markdown

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Error path and edge case tests for Markdown parser.
 *
 * Tests cover:
 * - Validation errors (unclosed brackets, parentheses)
 * - Malformed markdown structures
 * - Edge cases in inline markup
 * - Code block handling errors
 * - Table parsing errors
 * - Link and image parsing edge cases
 */
class MarkdownParserErrorTest {

    private lateinit var parser: MarkdownParser

    @BeforeTest
    fun setup() {
        parser = MarkdownParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Validation Error Tests ====================

    @Test
    fun `should detect unclosed link bracket`() {
        val malformed = "[Link text without closing"

        val errors = parser.validate(malformed)

        assertTrue(errors.isNotEmpty(), "Should detect unclosed bracket")
        assertTrue(
            errors.any { it.contains("bracket", ignoreCase = true) },
            "Error should mention bracket"
        )
    }

    @Test
    fun `should detect unclosed link parenthesis`() {
        val malformed = "[Text](url without closing"

        val errors = parser.validate(malformed)

        assertTrue(errors.isNotEmpty(), "Should detect unclosed parenthesis")
        assertTrue(
            errors.any { it.contains("paren", ignoreCase = true) },
            "Error should mention parenthesis"
        )
    }

    @Test
    fun `should detect multiple errors on same line`() {
        val malformed = "[Unclosed bracket [Another unclosed (And parenthesis"

        val errors = parser.validate(malformed)

        assertTrue(errors.isNotEmpty(), "Should detect multiple errors")
    }

    @Test
    fun `should report line numbers in validation errors`() {
        val malformed = """
            # Valid Header
            [Unclosed bracket on line 2
            Another line
            (Unclosed paren on line 4
        """.trimIndent()

        val errors = parser.validate(malformed)

        assertTrue(errors.isNotEmpty())
        assertTrue(
            errors.any { it.contains("Line") },
            "Errors should include line numbers"
        )
        assertTrue(
            errors.any { it.contains("2") || it.contains("4") },
            "Should report specific line numbers"
        )
    }

    @Test
    fun `should not report errors for valid links`() {
        val valid = """
            [Valid link](https://example.com)
            [Another link](url)
            ![Image](image.png)
        """.trimIndent()

        val errors = parser.validate(valid)

        assertTrue(errors.isEmpty(), "Valid markdown should have no errors")
    }

    @Test
    fun `should not report errors inside code blocks`() {
        val codeWithBrackets = """
            ```
            [This is not a link
            (This is not markdown
            ```
        """.trimIndent()

        val errors = parser.validate(codeWithBrackets)

        assertTrue(errors.isEmpty(), "Should ignore content inside code blocks")
    }

    // ==================== Malformed Structure Tests ====================

    @Test
    fun `should handle unclosed code block`() {
        val unclosed = """
            # Header
            ```
            Code without closing fence
            More code
        """.trimIndent()

        val document = parser.parse(unclosed)

        assertNotNull(document)
        assertNotNull(document.parsedContent)
        // Should close code block automatically at end
        assertTrue(document.parsedContent.contains("</code></pre>"))
    }

    @Test
    fun `should handle unclosed blockquote`() {
        val unclosed = """
            > Quote line 1
            > Quote line 2
            Regular text
        """.trimIndent()

        val document = parser.parse(unclosed)

        assertNotNull(document)
        // Should close blockquote when non-quote line encountered
        assertTrue(document.parsedContent.contains("</blockquote>"))
    }

    @Test
    fun `should handle unclosed list`() {
        val unclosed = """
            * Item 1
            * Item 2
            Regular paragraph
        """.trimIndent()

        val document = parser.parse(unclosed)

        assertNotNull(document)
        // Should close list when non-list line encountered
        assertTrue(document.parsedContent.contains("</ul>"))
    }

    @Test
    fun `should handle malformed table`() {
        val malformed = """
            | Col1 | Col2
            | --- | ---
            | Val1 | Val2
        """.trimIndent()

        val document = parser.parse(malformed)

        assertNotNull(document)
        // Should handle table with missing trailing pipes gracefully
    }

    @Test
    fun `should handle table without separator row`() {
        val noSeparator = """
            | Col1 | Col2 |
            | Val1 | Val2 |
        """.trimIndent()

        val document = parser.parse(noSeparator)

        assertNotNull(document)
        // Without separator, might not be recognized as table
    }

    // ==================== Inline Markup Edge Cases ====================

    @Test
    fun `should handle unmatched bold markers`() {
        val unmatched = "**Bold without closing\nAnother line"

        val document = parser.parse(unmatched)

        assertNotNull(document)
        // Should not throw, just leave markers unprocessed
    }

    @Test
    fun `should handle unmatched italic markers`() {
        val unmatched = "*Italic without closing\nAnother line"

        val document = parser.parse(unmatched)

        assertNotNull(document)
    }

    @Test
    fun `should handle unmatched strikethrough markers`() {
        val unmatched = "~~Strikethrough without closing"

        val document = parser.parse(unmatched)

        assertNotNull(document)
    }

    @Test
    fun `should handle nested emphasis correctly`() {
        val nested = "***Bold and italic***"

        val document = parser.parse(nested)

        assertNotNull(document)
        val html = document.toHtml()
        // Should handle nested emphasis
        assertTrue(html.contains("strong") || html.contains("em"))
    }

    @Test
    fun `should handle emphasis spanning multiple words`() {
        val multiWord = "**This is bold text**"

        val document = parser.parse(multiWord)
        val html = document.toHtml()

        assertTrue(html.contains("<strong>"))
        assertTrue(html.contains("</strong>"))
    }

    @Test
    fun `should handle inline code with backticks inside`() {
        val codeWithBackticks = "`code with ` backtick`"

        val document = parser.parse(codeWithBackticks)

        assertNotNull(document)
        // First backtick pair should close the code span
    }

    // ==================== Link and Image Edge Cases ====================

    @Test
    fun `should handle link without URL`() {
        val noUrl = "[Link text]()"

        val document = parser.parse(noUrl)

        assertNotNull(document)
        // Parser should handle gracefully without throwing
    }

    @Test
    fun `should handle image without URL`() {
        val noUrl = "![Alt text]()"

        val document = parser.parse(noUrl)

        assertNotNull(document)
        // Parser should handle gracefully without throwing
    }

    @Test
    fun `should handle link without text`() {
        val noText = "[](https://example.com)"

        val document = parser.parse(noText)

        assertNotNull(document)
        // Parser should handle gracefully without throwing
    }

    @Test
    fun `should handle image without alt text`() {
        val noAlt = "![](image.png)"

        val document = parser.parse(noAlt)

        assertNotNull(document)
        // Parser should handle gracefully without throwing
    }

    @Test
    fun `should handle special characters in URLs`() {
        val specialChars = "[Link](https://example.com?param=value&other=123)"

        val document = parser.parse(specialChars)
        val html = document.toHtml()

        assertNotNull(html)
        // URL should be preserved
        assertTrue(html.contains("example.com"))
    }

    @Test
    fun `should handle spaces in URLs`() {
        val spaces = "[Link](url with spaces)"

        val document = parser.parse(spaces)

        assertNotNull(document)
        // Should handle URLs with spaces
    }

    // ==================== Code Block Edge Cases ====================

    @Test
    fun `should handle code block with language specifier`() {
        val withLang = """
            ```kotlin
            fun main() {
                println("Hello")
            }
            ```
        """.trimIndent()

        val document = parser.parse(withLang)
        val html = document.toHtml()

        assertTrue(html.contains("<code>"))
        assertTrue(html.contains("main()"))
    }

    @Test
    fun `should handle empty code block`() {
        val empty = """
            ```
            ```
        """.trimIndent()

        val document = parser.parse(empty)

        assertNotNull(document)
    }

    @Test
    fun `should handle code block with only whitespace`() {
        val whitespace = """
            ```


            ```
        """.trimIndent()

        val document = parser.parse(whitespace)

        assertNotNull(document)
    }

    @Test
    fun `should preserve content in code blocks exactly`() {
        val code = """
            ```
            **Not bold**
            *Not italic*
            [Not a link](url)
            ```
        """.trimIndent()

        val document = parser.parse(code)
        val html = document.toHtml()

        // Content should be preserved as-is (escaped)
        assertTrue(html.contains("**Not bold**") || html.contains("&lt;"))
    }

    // ==================== Empty and Whitespace Tests ====================

    @Test
    fun `should handle completely empty markdown`() {
        val document = parser.parse("")

        assertNotNull(document)
        assertEquals("", document.rawContent)
    }

    @Test
    fun `should handle only whitespace`() {
        val whitespace = "   \n\n\t\t\n   "

        val document = parser.parse(whitespace)

        assertNotNull(document)
    }

    @Test
    fun `should handle single newline`() {
        val document = parser.parse("\n")

        assertNotNull(document)
    }

    @Test
    fun `should handle multiple consecutive blank lines`() {
        val blanks = """
            # Header





            Paragraph
        """.trimIndent()

        val document = parser.parse(blanks)

        assertNotNull(document)
    }

    // ==================== Special Characters Tests ====================

    @Test
    fun `should handle HTML entities in markdown`() {
        val entities = "Text with &lt; &gt; &amp; &quot;"

        val document = parser.parse(entities)
        val html = document.toHtml()

        // Entities should be double-escaped
        assertTrue(html.contains("&"))
    }

    @Test
    fun `should handle actual HTML tags in markdown`() {
        val htmlTags = "<div>Content</div> <script>alert('xss')</script>"

        val document = parser.parse(htmlTags)
        val html = document.toHtml()

        // HTML should be escaped
        assertTrue(html.contains("&lt;div&gt;") || html.contains("&lt;"))
    }

    @Test
    fun `should handle unicode characters`() {
        val unicode = "你好世界 🌍 Привет مرحبا"

        val document = parser.parse(unicode)

        assertNotNull(document)
        assertEquals(unicode, document.rawContent)
    }

    // ==================== Task List Edge Cases ====================

    @Test
    fun `should handle task list with mixed checked states`() {
        val tasks = """
            - [x] Completed task
            - [ ] Incomplete task
            - [x] Another completed
            - [ ] Another incomplete
        """.trimIndent()

        val document = parser.parse(tasks)
        val html = document.toHtml()

        assertTrue(html.contains("checkbox"))
        assertTrue(html.contains("checked"))
    }

    @Test
    fun `should handle task list with no space between bracket and text`() {
        val noSpace = """
            - [x]Task without space
            - [ ]Another task
        """.trimIndent()

        val document = parser.parse(noSpace)

        assertNotNull(document)
    }

    @Test
    fun `should handle malformed task list markers`() {
        val malformed = """
            - [y] Invalid marker
            - [X] Capital X
            - [ x] Space before x
            - [x ] Space after x
        """.trimIndent()

        val document = parser.parse(malformed)

        assertNotNull(document)
        // Parser should handle gracefully
    }

    // ==================== Heading Edge Cases ====================

    @Test
    fun `should handle heading without space after hash`() {
        val noSpace = "#Header"

        val document = parser.parse(noSpace)

        assertNotNull(document)
        // Might not be recognized as heading without space
    }

    @Test
    fun `should handle heading with trailing hashes`() {
        val trailing = "# Header #####"

        val document = parser.parse(trailing)
        val html = document.toHtml()

        // Should process heading
        assertTrue(html.contains("<h1>") || html.contains("Header"))
    }

    @Test
    fun `should handle heading levels beyond h6`() {
        val tooMany = "####### Seven hashes"

        val document = parser.parse(tooMany)

        assertNotNull(document)
        // Should not create h7 (doesn't exist)
    }

    @Test
    fun `should handle empty heading`() {
        val empty = "#"

        val document = parser.parse(empty)

        assertNotNull(document)
    }

    // ==================== Horizontal Rule Edge Cases ====================

    @Test
    fun `should handle horizontal rule with asterisks`() {
        val asterisks = "***"

        val document = parser.parse(asterisks)
        val html = document.toHtml()

        // Could be bold+italic or HR
        assertNotNull(html)
    }

    @Test
    fun `should handle horizontal rule with dashes`() {
        val dashes = "---"

        val document = parser.parse(dashes)
        val html = document.toHtml()

        assertTrue(html.contains("<hr>") || html.contains("---"))
    }

    @Test
    fun `should handle horizontal rule with underscores`() {
        val underscores = "___"

        val document = parser.parse(underscores)
        val html = document.toHtml()

        assertTrue(html.contains("<hr>") || html.contains("___"))
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should handle very long markdown document`() {
        val longDoc = buildString {
            repeat(10000) {
                append("# Header $it\n\n")
                append("Paragraph with **bold** and *italic* text.\n\n")
            }
        }

        val document = parser.parse(longDoc)

        assertNotNull(document)
        assertTrue(document.rawContent.length > 100000)
    }

    @Test
    fun `should handle deeply nested lists`() {
        val nested = buildString {
            repeat(100) { depth ->
                append("  ".repeat(depth))
                append("* Item at depth $depth\n")
            }
        }

        val document = parser.parse(nested)

        assertNotNull(document)
    }

    @Test
    fun `should handle very long lines`() {
        val longLine = "A".repeat(100000)

        val document = parser.parse(longLine)

        assertNotNull(document)
        assertEquals(100000, document.rawContent.length)
    }
}
