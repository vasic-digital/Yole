/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for Markdown Parser
 *
 *########################################################*/
package digital.vasic.yole.format.markdown

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarkdownParserTest {

    private val parser = MarkdownParser()

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `test basic parsing`() {
        val content = "# Hello World"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_MARKDOWN, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `test empty content`() {
        val result = parser.parse("")

        assertNotNull(result)
        assertEquals("", result.rawContent)
    }

    @Test
    fun `test metadata extraction`() {
        val content = """
            # Title

            Paragraph one.

            Paragraph two.
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals("5", result.metadata["lines"])
    }

    // ==================== Heading Tests ====================

    @Test
    fun `test H1 heading`() {
        val content = "# Heading 1"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("Heading 1"))
        assertTrue(html.contains("</h1>"))
    }

    @Test
    fun `test H2 heading`() {
        val content = "## Heading 2"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("</h2>"))
    }

    @Test
    fun `test H3 through H6 headings`() {
        val content = """
            ### H3
            #### H4
            ##### H5
            ###### H6
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<h3>"))
        assertTrue(html.contains("<h4>"))
        assertTrue(html.contains("<h5>"))
        assertTrue(html.contains("<h6>"))
    }

    // ==================== Emphasis Tests ====================

    @Test
    fun `test bold with asterisks`() {
        val content = "This is **bold** text"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<strong>bold</strong>"))
    }

    @Test
    fun `test bold with underscores`() {
        val content = "This is __bold__ text"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<strong>bold</strong>"))
    }

    @Test
    fun `test italic with asterisks`() {
        val content = "This is *italic* text"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<em>italic</em>"))
    }

    @Test
    fun `test italic with underscores`() {
        val content = "This is _italic_ text"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<em>italic</em>"))
    }

    @Test
    fun `test bold and italic combined`() {
        val content = "This is ***bold and italic*** text"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<em><strong>bold and italic</strong></em>"))
    }

    @Test
    fun `test strikethrough`() {
        val content = "This is ~~strikethrough~~ text"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<s>strikethrough</s>"))
    }

    // ==================== Code Tests ====================

    @Test
    fun `test inline code`() {
        val content = "Use the `print()` function"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<code>print()</code>"))
    }

    @Test
    fun `test fenced code block`() {
        val content = """
            ```kotlin
            fun main() {
                println("Hello")
            }
            ```
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("fun main()"))
        assertTrue(html.contains("</code></pre>"))
    }

    @Test
    fun `test code block escapes HTML`() {
        val content = """
            ```
            <script>alert('xss')</script>
            ```
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(!html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
    }

    // ==================== Link Tests ====================

    @Test
    fun `test inline link`() {
        val content = "Visit [Google](https://google.com)"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<a href='https://google.com'>Google</a>"))
    }

    @Test
    fun `test image`() {
        val content = "![Alt text](image.png)"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<img src='image.png' alt='Alt text'/>"))
    }

    // ==================== List Tests ====================

    @Test
    fun `test unordered list with dashes`() {
        val content = """
            - Item 1
            - Item 2
            - Item 3
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>Item 1</li>"))
        assertTrue(html.contains("<li>Item 2</li>"))
        assertTrue(html.contains("</ul>"))
    }

    @Test
    fun `test unordered list with asterisks`() {
        val content = """
            * Item 1
            * Item 2
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>"))
    }

    @Test
    fun `test ordered list`() {
        val content = """
            1. First
            2. Second
            3. Third
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<li>First</li>"))
        assertTrue(html.contains("</ol>"))
    }

    // ==================== Task List Tests ====================

    @Test
    fun `test task list checked`() {
        val content = "- [x] Completed task"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<input type='checkbox' disabled checked>"))
    }

    @Test
    fun `test task list unchecked`() {
        val content = "- [ ] Pending task"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<input type='checkbox' disabled>"))
    }

    // ==================== Blockquote Tests ====================

    @Test
    fun `test blockquote`() {
        val content = "> This is a quote"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("This is a quote"))
        assertTrue(html.contains("</blockquote>"))
    }

    @Test
    fun `test multi-line blockquote`() {
        val content = """
            > Line 1
            > Line 2
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("Line 1"))
        assertTrue(html.contains("Line 2"))
    }

    // ==================== Horizontal Rule Tests ====================

    @Test
    fun `test horizontal rule with dashes`() {
        val content = "---"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<hr>"))
    }

    @Test
    fun `test horizontal rule with asterisks`() {
        val content = "***"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<hr>"))
    }

    // ==================== Table Tests ====================

    @Test
    fun `test basic table`() {
        val content = """
            | Name | Age |
            |------|-----|
            | John | 30  |
            | Jane | 25  |
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<th>"))
        assertTrue(html.contains("<td>"))
        assertTrue(html.contains("Name"))
        assertTrue(html.contains("John"))
        assertTrue(html.contains("</table>"))
    }

    // ==================== Paragraph Tests ====================

    @Test
    fun `test paragraph wrapping`() {
        val content = "This is a paragraph of text."
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<p>"))
        assertTrue(html.contains("</p>"))
    }

    @Test
    fun `test multiple paragraphs`() {
        val content = """
            First paragraph.

            Second paragraph.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        // Should have paragraph tags
        assertTrue(html.contains("<p>"))
    }

    // ==================== Format Detection Tests ====================

    @Test
    fun `test format detection by md extension`() {
        val format = FormatRegistry.getByExtension(".md")
        assertNotNull(format)
        assertEquals(TextFormat.ID_MARKDOWN, format.id)
    }

    @Test
    fun `test format detection by markdown extension`() {
        val format = FormatRegistry.getByExtension(".markdown")
        assertNotNull(format)
        assertEquals(TextFormat.ID_MARKDOWN, format.id)
    }

    @Test
    fun `test supported extensions`() {
        assertTrue(MarkdownParser.EXTENSIONS.contains(".md"))
        assertTrue(MarkdownParser.EXTENSIONS.contains(".markdown"))
        assertTrue(MarkdownParser.EXTENSIONS.contains(".mdown"))
        assertTrue(MarkdownParser.EXTENSIONS.contains(".mkd"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `test validation with valid content`() {
        val content = """
            # Title

            This is [a link](https://example.com).
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `test validation with unclosed brackets`() {
        val content = "This is [unclosed link"

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed brackets") })
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `test complex document`() {
        val content = """
            # Project Title

            This is a **bold** statement with *italic* text.

            ## Features

            - [x] Feature 1 complete
            - [ ] Feature 2 pending

            ### Code Example

            ```kotlin
            fun hello() = println("Hello")
            ```

            > Important note here

            | Column A | Column B |
            |----------|----------|
            | Value 1  | Value 2  |
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<h1>"))
        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("<h3>"))
        assertTrue(html.contains("<strong>"))
        assertTrue(html.contains("<em>"))
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("<table>"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `test nested formatting in list items`() {
        val content = "- This is **bold** in a list"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<li>"))
        assertTrue(html.contains("<strong>bold</strong>"))
    }

    @Test
    fun `test link with formatted text`() {
        val content = "Visit [**Bold Link**](https://example.com)"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        assertTrue(html.contains("<a href='https://example.com'>"))
    }

    @Test
    fun `test special characters in content`() {
        val content = "Use the < and > symbols carefully"
        val document = parser.parse(content)
        val html = parser.toHtml(document, true)

        // Should escape HTML entities in paragraph
        assertTrue(!html.contains("<p>Use the < and >"))
    }
}
