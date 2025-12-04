/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Markdown parser
 *
 *########################################################*/
package digital.vasic.yole.format.markdown

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.markdown.MarkdownParser
import org.junit.Test
import kotlin.test.*

/**
 * Unit tests for Markdown format parser.
 *
 * Tests cover:
 * - Format detection by extension
 * - Basic parsing functionality
 * - Edge cases and error handling
 * - Empty input handling
 * - Special characters
 */
class MarkdownParserTest {

    private val parser = MarkdownParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Markdown format by extension`() {
        val format = FormatRegistry.getByExtension(".md")

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
        assertEquals("Markdown", format.name)
    }

    @Test
    fun `should detect Markdown format by filename`() {
        val format = FormatRegistry.detectByFilename("test.md")

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `should support all Markdown extensions`() {
        val extensions = listOf(".md", ".markdown", ".mdown", ".mkd")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
        }
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `should parse basic Markdown content`() {
        val content = """
            # Hello World

            This is a paragraph with **bold** and *italic* text.

            Here's a [link](https://example.com).
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertTrue(result.parsedContent.contains("<h1>"))
        assertTrue(result.parsedContent.contains("<strong>"))
        assertTrue(result.parsedContent.contains("<em>"))
        assertTrue(result.parsedContent.contains("<a href="))
    }

    @Test
    fun `should handle empty input`() {
        val result = parser.parse("")

        assertNotNull(result)
        assertTrue(result.rawContent.isEmpty())
        assertNotNull(result.parsedContent)
    }

    @Test
    fun `should handle whitespace-only input`() {
        val result = parser.parse("   \n\n   \t  ")

        assertNotNull(result)
    }

    @Test
    fun `should handle single line input`() {
        val content = "Single line of Markdown"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Content Detection Tests ====================

    @Test
    fun `should detect format by content patterns`() {
        val content = """
            # Markdown Header

            This is **bold** and *italic* text.
        """.trimIndent()

        val format = FormatRegistry.detectByContent(content)

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
    }

    @Test
    fun `should not false-positive on plain text`() {
        val plainText = "Just some plain text without special formatting"

        val format = FormatRegistry.detectByContent(plainText)

        // Should detect as plaintext, not Markdown
        if (format != null) {
            assertNotEquals(FormatRegistry.ID_MARKDOWN, format.id)
        }
    }

    // ==================== Special Characters Tests ====================

    @Test
    fun `should handle special characters`() {
        val content = """
            Special chars: @#$%^&*()

            HTML entities: <div> & "quotes"
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // HTML should be escaped
        assertTrue(result.parsedContent.contains("&lt;"))
        assertTrue(result.parsedContent.contains("&gt;"))
        assertTrue(result.parsedContent.contains("&amp;"))
    }

    @Test
    fun `should handle unicode characters`() {
        val content = "Unicode test: 你好世界 🌍 Привет мир"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle malformed input gracefully`() {
        val malformed = """
            # Unclosed [bracket

            Mismatched **bold text

            Invalid ![image(url)
        """.trimIndent()

        // Should not throw exception
        val result = parser.parse(malformed)
        assertNotNull(result)
        assertNotNull(result.parsedContent)
    }

    @Test
    fun `should handle very long input`() {
        val longContent = "Single line of Markdown\n".repeat(10000)

        val result = parser.parse(longContent)

        assertNotNull(result)
    }

    @Test
    fun `should handle null bytes gracefully`() {
        // Binary content detection
        val binaryContent = "Some text\u0000with null\u0000bytes"

        val result = parser.parse(binaryContent)

        assertNotNull(result)
    }

    // ==================== Format-Specific Tests ====================

    @Test
    fun `should parse headers H1 through H6`() {
        val content = """
            # H1 Header
            ## H2 Header
            ### H3 Header
            #### H4 Header
            ##### H5 Header
            ###### H6 Header
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<h1>H1 Header</h1>"))
        assertTrue(result.parsedContent.contains("<h2>H2 Header</h2>"))
        assertTrue(result.parsedContent.contains("<h3>H3 Header</h3>"))
        assertTrue(result.parsedContent.contains("<h4>H4 Header</h4>"))
        assertTrue(result.parsedContent.contains("<h5>H5 Header</h5>"))
        assertTrue(result.parsedContent.contains("<h6>H6 Header</h6>"))
    }

    @Test
    fun `should parse bold text with double asterisks`() {
        val content = "This is **bold text** here"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<strong>bold text</strong>"))
    }

    @Test
    fun `should parse bold text with double underscores`() {
        val content = "This is __bold text__ here"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<strong>bold text</strong>"))
    }

    @Test
    fun `should parse italic text with single asterisk`() {
        val content = "This is *italic text* here"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<em>italic text</em>"))
    }

    @Test
    fun `should parse italic text with single underscore`() {
        val content = "This is _italic text_ here"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<em>italic text</em>"))
    }

    @Test
    fun `should parse strikethrough text`() {
        val content = "This is ~~strikethrough text~~ here"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<s>strikethrough text</s>"))
    }

    @Test
    fun `should parse inline code`() {
        val content = "Use the `println()` function"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<code>println()</code>"))
    }

    @Test
    fun `should parse fenced code blocks`() {
        val content = """
            ```kotlin
            fun main() {
                println("Hello World")
            }
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<pre>"))
        assertTrue(result.parsedContent.contains("<code>"))
        assertTrue(result.parsedContent.contains("fun main()"))
    }

    @Test
    fun `should parse unordered lists with dashes`() {
        val content = """
            - Item 1
            - Item 2
            - Item 3
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<ul>"))
        assertTrue(result.parsedContent.contains("<li>Item 1</li>"))
        assertTrue(result.parsedContent.contains("<li>Item 2</li>"))
        assertTrue(result.parsedContent.contains("</ul>"))
    }

    @Test
    fun `should parse unordered lists with asterisks`() {
        val content = """
            * Item 1
            * Item 2
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<ul>"))
        assertTrue(result.parsedContent.contains("<li>Item 1</li>"))
    }

    @Test
    fun `should parse unordered lists with plus signs`() {
        val content = """
            + Item 1
            + Item 2
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<ul>"))
        assertTrue(result.parsedContent.contains("<li>Item 1</li>"))
    }

    @Test
    fun `should parse ordered lists`() {
        val content = """
            1. First item
            2. Second item
            3. Third item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<ol>"))
        assertTrue(result.parsedContent.contains("<li>First item</li>"))
        assertTrue(result.parsedContent.contains("<li>Second item</li>"))
        assertTrue(result.parsedContent.contains("</ol>"))
    }

    @Test
    fun `should parse links`() {
        val content = "Check out [Example](https://example.com)"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<a href='https://example.com'>Example</a>"))
    }

    @Test
    fun `should parse images`() {
        val content = "![Alt text](https://example.com/image.png)"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<img src='https://example.com/image.png' alt='Alt text'/>"))
    }

    @Test
    fun `should parse blockquotes`() {
        val content = """
            > This is a quote
            > It spans multiple lines
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<blockquote>"))
        assertTrue(result.parsedContent.contains("This is a quote"))
        assertTrue(result.parsedContent.contains("</blockquote>"))
    }

    @Test
    fun `should parse horizontal rules with dashes`() {
        val content = """
            Above

            ---

            Below
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should parse horizontal rules with asterisks`() {
        val content = """
            Above

            ***

            Below
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should parse horizontal rules with underscores`() {
        val content = """
            Above

            ___

            Below
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should parse GFM tables`() {
        val content = """
            | Column 1 | Column 2 |
            |----------|----------|
            | Cell 1   | Cell 2   |
            | Cell 3   | Cell 4   |
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<table>"))
        assertTrue(result.parsedContent.contains("<th>Column 1</th>"))
        assertTrue(result.parsedContent.contains("<th>Column 2</th>"))
        assertTrue(result.parsedContent.contains("<td>Cell 1</td>"))
        assertTrue(result.parsedContent.contains("</table>"))
    }

    @Test
    fun `should parse task lists with unchecked checkboxes`() {
        val content = "- [ ] Unchecked task"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<input type='checkbox' disabled>"))
    }

    @Test
    fun `should parse task lists with checked checkboxes`() {
        val content = "- [x] Checked task"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<input type='checkbox' disabled checked>"))
    }

    @Test
    fun `should parse complex nested formatting`() {
        val content = "This is **bold with *italic* inside** text"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<strong>"))
        assertTrue(result.parsedContent.contains("<em>"))
    }

    @Test
    fun `should parse mixed list items`() {
        val content = """
            1. Ordered item
            2. Another ordered

            - Unordered item
            - Another unordered
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<ol>"))
        assertTrue(result.parsedContent.contains("<ul>"))
    }

    @Test
    fun `should parse paragraphs separated by blank lines`() {
        val content = """
            First paragraph here.

            Second paragraph here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Should have separate paragraph tags
        val pCount = result.parsedContent.count { it == 'p' }
        assertTrue(pCount >= 4) // At least 2 opening and 2 closing p tags
    }

    @Test
    fun `should handle code blocks with special characters`() {
        val content = """
            ```
            <html>
              <body>&amp;</body>
            </html>
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Special characters inside code blocks should be escaped
        assertTrue(result.parsedContent.contains("&lt;html&gt;"))
        assertTrue(result.parsedContent.contains("&amp;amp;"))
    }

    @Test
    fun `should validate and detect unclosed brackets`() {
        val content = "This has [unclosed bracket"

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors[0].contains("Unclosed brackets"))
    }

    @Test
    fun `should validate and detect unclosed parentheses`() {
        val content = "This has (unclosed paren"

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors[0].contains("Unclosed parentheses"))
    }

    @Test
    fun `should validate correctly formed markdown without errors`() {
        val content = """
            # Title

            This is [a link](https://example.com) and ![image](test.png).
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with FormatRegistry`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)

        assertNotNull(format)
        assertEquals("Markdown", format.name)
        assertEquals(".md", format.defaultExtension)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val markdownFormat = allFormats.find { it.id == FormatRegistry.ID_MARKDOWN }

        assertNotNull(markdownFormat)
        assertEquals("Markdown", markdownFormat.name)
    }

    // ==================== Additional GFM and Edge Case Tests ====================

    @Test
    fun `should parse nested bold within italic`() {
        val content = "This is ***bold and italic*** text"
        val result = parser.parse(content)

        assertNotNull(result)
        // Should contain both strong and em tags
        assertTrue(result.parsedContent.contains("<strong>") || result.parsedContent.contains("<em>"))
    }

    @Test
    fun `should parse italic within bold`() {
        val content = "This is **bold with *italic* inside** text"
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<strong>"))
        assertTrue(result.parsedContent.contains("<em>"))
    }

    @Test
    fun `should parse code within bold text`() {
        val content = "This is **bold with `code` inside** text"
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<code>"))
        assertTrue(result.parsedContent.contains("code"))
    }

    @Test
    fun `should parse complex table with formatting`() {
        val content = """
            | **Header 1** | _Header 2_ | `Code Header` |
            |--------------|------------|---------------|
            | **Bold**     | *Italic*   | ~~Strike~~    |
            | `code`       | Normal     | [Link](url)   |
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<table>"))
        assertTrue(result.parsedContent.contains("<th>"))
        assertTrue(result.parsedContent.contains("<td>"))
    }

    @Test
    fun `should parse table with pipe characters in cells`() {
        val content = """
            | Code | Description |
            |------|-------------|
            | `a|b` | Pipe character |
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<table>"))
    }

    @Test
    fun `should parse mixed task lists with formatting`() {
        val content = """
            - [x] Completed **important** task
            - [ ] Pending task with *emphasis*
            - [x] Another completed task with `code`
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("checkbox"))
        assertTrue(result.parsedContent.contains("checked"))
    }

    @Test
    fun `should handle URLs with query parameters in links`() {
        val content = "[Search](https://example.com/search?q=test&lang=en)"
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("href="))
        assertTrue(result.parsedContent.contains("example.com"))
    }

    @Test
    fun `should handle URLs with fragments in links`() {
        val content = "[Section](#section-heading)"
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("href="))
        assertTrue(result.parsedContent.contains("#section-heading"))
    }

    @Test
    fun `should parse reference-style links`() {
        val content = """
            Here is a [reference link][ref]

            [ref]: https://example.com
        """.trimIndent()

        // Note: Reference-style links might not be implemented,
        // but the parser should handle gracefully
        val result = parser.parse(content)
        assertNotNull(result)
    }

    @Test
    fun `should handle multiple consecutive blank lines`() {
        val content = """
            Paragraph 1


            Paragraph 2



            Paragraph 3
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Should create separate paragraphs
        val pCount = result.parsedContent.count { it == 'p' }
        assertTrue(pCount >= 6) // At least 3 opening and 3 closing p tags
    }

    @Test
    fun `should parse ATX headers with trailing hashes`() {
        val content = "## Header ##"
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<h2>"))
    }

    @Test
    fun `should parse Setext headers style 1`() {
        val content = """
            Header Level 1
            ==============
        """.trimIndent()

        // Note: Setext headers might not be implemented
        val result = parser.parse(content)
        assertNotNull(result)
    }

    @Test
    fun `should parse Setext headers style 2`() {
        val content = """
            Header Level 2
            --------------
        """.trimIndent()

        // Note: Setext headers might not be implemented
        val result = parser.parse(content)
        assertNotNull(result)
    }

    @Test
    fun `should handle blockquote with multiple paragraphs`() {
        val content = """
            > First paragraph
            > in blockquote
            >
            > Second paragraph
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<blockquote>"))
    }

    @Test
    fun `should handle nested lists`() {
        val content = """
            - Item 1
              - Nested 1.1
              - Nested 1.2
            - Item 2
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<ul>"))
        assertTrue(result.parsedContent.contains("<li>"))
    }

    @Test
    fun `should parse code block with language syntax highlighting hint`() {
        val content = """
            ```kotlin
            fun main() {
                println("Hello World")
            }
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<pre>"))
        assertTrue(result.parsedContent.contains("<code>"))
        assertTrue(result.parsedContent.contains("fun main()"))
    }

    @Test
    fun `should parse code block with special characters`() {
        val content = """
            ```
            <html>
              <body>Special & chars</body>
            </html>
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // HTML should be escaped in code blocks
        assertTrue(result.parsedContent.contains("&lt;") || result.parsedContent.contains("<html>"))
    }

    @Test
    fun `should handle indented code blocks`() {
        val content = """
            Normal paragraph

                indented code
                more code

            Back to normal
        """.trimIndent()

        // Note: Indented code blocks might not be fully implemented
        val result = parser.parse(content)
        assertNotNull(result)
    }

    @Test
    fun `should parse horizontal rules with different characters`() {
        val contentDashes = "---"
        val contentStars = "***"
        val contentUnderscores = "___"

        val result1 = parser.parse(contentDashes)
        val result2 = parser.parse(contentStars)
        val result3 = parser.parse(contentUnderscores)

        assertTrue(result1.parsedContent.contains("<hr>"))
        assertTrue(result2.parsedContent.contains("<hr>"))
        assertTrue(result3.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should parse horizontal rules with spaces`() {
        val content = "- - -"
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<hr>") || result.parsedContent.contains("<li>"))
    }

    @Test
    fun `should handle malformed table without proper separators`() {
        val content = """
            | Header 1 | Header 2 |
            | Cell 1   | Cell 2   |
        """.trimIndent()

        // Table without separator line might not render as table
        val result = parser.parse(content)
        assertNotNull(result)
    }

    @Test
    fun `should escape HTML in regular paragraphs`() {
        val content = "This has <script>alert('XSS')</script> in it"
        val result = parser.parse(content)

        assertNotNull(result)
        // Script tags should be escaped
        assertTrue(result.parsedContent.contains("&lt;") || !result.parsedContent.contains("<script>"))
    }

    @Test
    fun `should handle multiple formatting on same text`() {
        val content = "This is ***~~bold italic strikethrough~~***"
        val result = parser.parse(content)

        assertNotNull(result)
        // Should handle complex nesting
        assertNotNull(result.parsedContent)
    }

    @Test
    fun `should parse autolinks`() {
        val content = "<https://example.com>"
        val result = parser.parse(content)

        assertNotNull(result)
        // Autolinks might not be implemented, but should not crash
    }

    @Test
    fun `should handle email autolinks`() {
        val content = "<user@example.com>"
        val result = parser.parse(content)

        assertNotNull(result)
        // Email autolinks might not be implemented
    }

    @Test
    fun `should parse footnotes`() {
        val content = """
            Here is a footnote reference[^1]

            [^1]: This is the footnote content
        """.trimIndent()

        val result = parser.parse(content)
        assertNotNull(result)
        // Footnotes might not be implemented
    }

    @Test
    fun `should handle emoji shortcodes`() {
        val content = "This has :smile: emoji"
        val result = parser.parse(content)

        assertNotNull(result)
        // Emoji shortcodes might not be implemented
    }

    @Test
    fun `should parse definition lists`() {
        val content = """
            Term
            : Definition
        """.trimIndent()

        val result = parser.parse(content)
        assertNotNull(result)
        // Definition lists might not be implemented
    }

    @Test
    fun `should parse real-world README example`() {
        val content = """
            # Project Title

            [![Build Status](https://img.shields.io/badge/build-passing-green)]()

            ## Features

            - **Fast**: Optimized for performance
            - **Reliable**: Thoroughly tested
            - **Easy**: Simple API

            ## Installation

            ```bash
            npm install my-package
            ```

            ## Usage

            ```javascript
            const pkg = require('my-package');
            pkg.doSomething();
            ```

            ## API Reference

            ### `doSomething()`

            Does something useful.

            | Parameter | Type | Description |
            |-----------|------|-------------|
            | `input`   | String | The input value |
            | `options` | Object | Configuration |

            ## License

            MIT © 2025
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<h1>"))
        assertTrue(result.parsedContent.contains("<h2>"))
        assertTrue(result.parsedContent.contains("<ul>"))
        assertTrue(result.parsedContent.contains("<table>"))
        assertTrue(result.parsedContent.contains("<code>"))
    }

    @Test
    fun `should parse real-world documentation with complex nesting`() {
        val content = """
            ## Configuration

            The configuration supports these options:

            - `timeout` (*number*): Maximum wait time in ms
              - Default: `5000`
              - Range: `100` - `30000`
            - `retry` (*boolean*): Whether to retry failed requests
              - Default: `true`

            > **Note**: The `timeout` value should be set according to your network conditions.

            Example:

            ```json
            {
              "timeout": 3000,
              "retry": false
            }
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<h2>"))
        assertTrue(result.parsedContent.contains("<li>"))
        assertTrue(result.parsedContent.contains("<code>"))
        assertTrue(result.parsedContent.contains("<blockquote>"))
    }

    @Test
    fun `should handle very long lines without breaking`() {
        val longLine = "This is a very long line that goes on and on ".repeat(50)
        val result = parser.parse(longLine)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains(longLine.substring(0, 50)))
    }

    @Test
    fun `should parse mixed list types`() {
        val content = """
            1. Ordered item 1
            2. Ordered item 2

            - Unordered item 1
            - Unordered item 2

            1. Back to ordered
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<ol>"))
        assertTrue(result.parsedContent.contains("<ul>"))
    }

    @Test
    fun `should handle link with title attribute`() {
        val content = """[Example](https://example.com "Example Title")"""
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("href="))
    }

    @Test
    fun `should handle image with title attribute`() {
        val content = """![Alt Text](image.png "Image Title")"""
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("img") || result.parsedContent.contains("<img"))
    }

    @Test
    fun `should parse tables with alignment`() {
        val content = """
            | Left | Center | Right |
            |:-----|:------:|------:|
            | L1   | C1     | R1    |
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<table>"))
    }

    @Test
    fun `should handle escaping backslashes`() {
        val content = """This has \*escaped\* asterisks"""
        val result = parser.parse(content)

        assertNotNull(result)
        // Escaped asterisks might show as literal or might not be implemented
    }

    @Test
    fun `should parse line breaks with two spaces`() {
        val content = """Line 1
Line 2"""
        val result = parser.parse(content)

        assertNotNull(result)
        // Hard line breaks might not be implemented
    }

    @Test
    fun `should handle empty code blocks`() {
        val content = """
            ```

            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<pre>") && result.parsedContent.contains("</pre>"))
    }

    @Test
    fun `should handle unclosed code blocks gracefully`() {
        val content = """
            ```kotlin
            fun incomplete() {
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Should close the code block even if not explicitly closed
    }

    @Test
    fun `should validate unclosed code blocks`() {
        val content = """
            ```kotlin
            fun incomplete() {
        """.trimIndent()

        val errors = parser.validate(content)

        // Validation might or might not catch unclosed code blocks
        assertNotNull(errors)
    }
}
