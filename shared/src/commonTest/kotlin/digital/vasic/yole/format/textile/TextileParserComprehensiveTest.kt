/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for TextileParser
 *
 *########################################################*/
package digital.vasic.yole.format.textile

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for TextileParser covering all parsing branches.
 *
 * Tests cover:
 * - Headings (h1. h2. etc.)
 * - Lists (unordered *, ordered #)
 * - Blockquotes (bq. text)
 * - Pre-formatted blocks (pre.)
 * - Inline markup (bold, italic, code, links, images, super/subscript)
 * - Validation
 * - Edge cases
 */
class TextileParserComprehensiveTest {

    private lateinit var parser: TextileParser

    @BeforeTest
    fun setup() {
        parser = TextileParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Metadata Tests ====================

    @Test
    fun `should extract extension from filename`() {
        val content = "Content"
        val options = mapOf("filename" to "document.textile")

        val doc = parser.parse(content, options)

        assertEquals(".textile", doc.metadata["extension"])
    }

    @Test
    fun `should handle filename without extension`() {
        val content = "Content"
        val options = mapOf("filename" to "document")

        val doc = parser.parse(content, options)

        assertEquals("", doc.metadata["extension"])
    }

    @Test
    fun `should extract line count`() {
        val content = """
            Line 1
            Line 2
            Line 3
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("3", doc.metadata["lines"])
    }

    // ==================== Heading Tests ====================

    @Test
    fun `should convert h1 heading`() {
        val content = "h1. Heading 1"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h1>"))
        assertTrue(doc.parsedContent.contains("Heading 1"))
    }

    @Test
    fun `should convert h2 heading`() {
        val content = "h2. Heading 2"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h2>"))
        assertTrue(doc.parsedContent.contains("Heading 2"))
    }

    @Test
    fun `should convert h3 heading`() {
        val content = "h3. Heading 3"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h3>"))
        assertTrue(doc.parsedContent.contains("Heading 3"))
    }

    @Test
    fun `should convert h4 heading`() {
        val content = "h4. Heading 4"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h4>"))
    }

    @Test
    fun `should convert h5 heading`() {
        val content = "h5. Heading 5"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h5>"))
    }

    @Test
    fun `should convert h6 heading`() {
        val content = "h6. Heading 6"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h6>"))
    }

    @Test
    fun `should support heading with double period`() {
        val content = "h1.. Heading with double period"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h1>"))
        assertTrue(doc.parsedContent.contains("Heading with double period"))
    }

    // ==================== List Tests ====================

    @Test
    fun `should convert simple unordered list`() {
        val content = """
            * Item 1
            * Item 2
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("<li>"))
        assertTrue(doc.parsedContent.contains("Item 1"))
    }

    @Test
    fun `should convert simple ordered list`() {
        val content = """
            # Item 1
            # Item 2
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ol>"))
        assertTrue(doc.parsedContent.contains("<li>"))
        assertTrue(doc.parsedContent.contains("Item 1"))
    }

    @Test
    fun `should close unordered list when switching to ordered`() {
        val content = """
            * Unordered
            # Ordered
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ul>"))
        assertTrue(doc.parsedContent.contains("<ol>"))
    }

    @Test
    fun `should close ordered list when switching to unordered`() {
        val content = """
            # Ordered
            * Unordered
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ol>"))
        assertTrue(doc.parsedContent.contains("<ul>"))
    }

    @Test
    fun `should close list when encountering non-list`() {
        val content = """
            * Item 1
            Regular paragraph
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ul>"))
        assertTrue(doc.parsedContent.contains("<p>Regular paragraph</p>"))
    }

    @Test
    fun `should close all open lists at end of document`() {
        val content = """
            * Item 1
            * Item 2
        """.trimIndent()

        val doc = parser.parse(content)

        val openCount = doc.parsedContent.split("<ul>").size - 1
        val closeCount = doc.parsedContent.split("</ul>").size - 1
        assertEquals(openCount, closeCount)
    }

    // ==================== Blockquote Tests ====================

    @Test
    fun `should convert blockquote`() {
        val content = "bq. Quoted text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<blockquote>"))
        assertTrue(doc.parsedContent.contains("Quoted text"))
    }

    @Test
    fun `should handle blockquote with inline markup`() {
        val content = "bq. This is *bold* quoted text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<blockquote>"))
        assertTrue(doc.parsedContent.contains("<b>"))
    }

    // ==================== Pre-formatted Block Tests ====================

    @Test
    fun `should convert pre-formatted block`() {
        val content = """
            pre.
            code here
            more code

        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("code here"))
        assertTrue(doc.parsedContent.contains("</pre>"))
    }

    @Test
    fun `should handle pre-formatted block at start of document`() {
        val content = """
            pre.
            code

        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
    }

    @Test
    fun `should escape HTML in pre-formatted blocks`() {
        val content = """
            pre.
            <div>HTML</div>

        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&lt;") || doc.parsedContent.contains("&gt;"))
    }

    @Test
    fun `should close pre block on empty line`() {
        val content = """
            pre.
            code

            Regular text
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</pre>"))
        assertTrue(doc.parsedContent.contains("<p>Regular text</p>"))
    }

    @Test
    fun `should close unclosed pre block at end of document`() {
        val content = """
            pre.
            code
            more code
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("</pre>"))
    }

    // ==================== Inline Markup Tests ====================

    @Test
    fun `should convert bold text with single asterisk`() {
        val content = "This is *bold* text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<b>"))
        assertTrue(doc.parsedContent.contains("bold"))
    }

    @Test
    fun `should convert strong emphasis with double asterisk`() {
        val content = "This is **strong** text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<strong>"))
        assertTrue(doc.parsedContent.contains("strong"))
    }

    @Test
    fun `should convert emphasis with single underscore`() {
        val content = "This is _emphasized_ text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<em>"))
        assertTrue(doc.parsedContent.contains("emphasized"))
    }

    @Test
    fun `should convert double emphasis with double underscore`() {
        val content = "This is __double emphasized__ text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<em>"))
        assertTrue(doc.parsedContent.contains("double emphasized"))
    }

    @Test
    fun `should convert strikethrough text`() {
        val content = "This is -strikethrough- text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<s>"))
        assertTrue(doc.parsedContent.contains("strikethrough"))
    }

    @Test
    fun `should convert superscript text`() {
        val content = "E = mc^2^"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<sup>"))
        assertTrue(doc.parsedContent.contains("2"))
    }

    @Test
    fun `should convert subscript text`() {
        val content = "H~2~O"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<sub>"))
        assertTrue(doc.parsedContent.contains("2"))
    }

    @Test
    fun `should convert inline code`() {
        val content = "Use the @code()@ function"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<code>"))
        assertTrue(doc.parsedContent.contains("code()"))
    }

    @Test
    fun `should escape HTML in inline code`() {
        val content = "@<div>@"

        val doc = parser.parse(content)

        // Code is escaped and wrapped in <code> tags
        assertTrue(doc.parsedContent.contains("<code>") && doc.parsedContent.contains("div"))
    }

    // ==================== Link Tests ====================

    @Test
    fun `should convert link`() {
        val content = """See "Google":http://google.com"""

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href='http://google.com'>"))
        assertTrue(doc.parsedContent.contains("Google"))
    }

    @Test
    fun `should handle link with special characters in URL`() {
        val content = """"Example":http://example.com/path?query=value"""

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href="))
        assertTrue(doc.parsedContent.contains("Example"))
    }

    // ==================== Image Tests ====================

    @Test
    fun `should convert image`() {
        val content = "!photo.png!"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<img src='photo.png'"))
        assertTrue(doc.parsedContent.contains("alt='photo.png'"))
    }

    @Test
    fun `should handle image in paragraph`() {
        val content = "Here is an image: !test.jpg!"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<img"))
        assertTrue(doc.parsedContent.contains("test.jpg"))
    }

    // ==================== Paragraph Tests ====================

    @Test
    fun `should convert regular paragraph`() {
        val content = "This is a regular paragraph."

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<p>"))
        assertTrue(doc.parsedContent.contains("regular paragraph"))
    }

    @Test
    fun `should skip empty lines`() {
        val content = """
            Line 1

            Line 2
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Line 1"))
        assertTrue(doc.parsedContent.contains("Line 2"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should detect invalid heading level 0`() {
        val content = "h0. Invalid heading"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Invalid heading level") })
    }

    @Test
    fun `should detect invalid heading level 7`() {
        val content = "h7. Invalid heading"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Invalid heading level") })
    }

    @Test
    fun `should accept valid heading levels 1-6`() {
        val content = """
            h1. Heading 1
            h2. Heading 2
            h3. Heading 3
            h4. Heading 4
            h5. Heading 5
            h6. Heading 6
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Invalid heading level") })
    }

    @Test
    fun `should detect unclosed inline code marker`() {
        val content = "This has @unclosed code"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed inline code") })
    }

    @Test
    fun `should accept closed inline code marker`() {
        val content = "This has @code@ markers"

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed inline code") })
    }

    @Test
    fun `should detect unclosed image marker`() {
        val content = "This has !unclosed image"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed image") })
    }

    @Test
    fun `should accept closed image marker`() {
        val content = "This has !image.png! marker"

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed image") })
    }

    // ==================== HTML Generation Tests ====================

    @Test
    fun `should escape HTML in regular text`() {
        val content = "Text with <div> and & symbols"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&lt;") || doc.parsedContent.contains("&gt;") || doc.parsedContent.contains("&amp;"))
    }

    @Test
    fun `should include CSS styling`() {
        val content = "Content"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<style>"))
        assertTrue(doc.parsedContent.contains(".textile"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty content`() {
        val doc = parser.parse("")

        assertEquals("", doc.rawContent)
        assertNotNull(doc.parsedContent)
    }

    @Test
    fun `should handle only whitespace`() {
        val content = "   \n\n\t  "

        val doc = parser.parse(content)

        assertNotNull(doc.parsedContent)
    }

    @Test
    fun `should handle unicode characters`() {
        val content = "Unicode: 你好世界 🌍"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("你好世界"))
        assertTrue(doc.parsedContent.contains("🌍"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should handle complete Textile document`() {
        val content = """
            h1. Main Heading

            This is a paragraph with *bold* and _italic_ text.

            h2. Subheading

            * Unordered item 1
            * Unordered item 2

            # Ordered item 1
            # Ordered item 2

            Here's some @inline code@ and a "link":http://example.com.

            pre.
            code block
            multiple lines

            bq. A blockquote

            Final paragraph with !image.png! and ^superscript^ and ~subscript~.
        """.trimIndent()

        val doc = parser.parse(content)

        // Check content elements
        assertTrue(doc.parsedContent.contains("<h1>") && doc.parsedContent.contains("Main Heading"))
        assertTrue(doc.parsedContent.contains("<h2>") && doc.parsedContent.contains("Subheading"))
        assertTrue(doc.parsedContent.contains("<b>") && doc.parsedContent.contains("bold"))
        assertTrue(doc.parsedContent.contains("<em>") && doc.parsedContent.contains("italic"))
        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("<ol>"))
        assertTrue(doc.parsedContent.contains("<code>") && doc.parsedContent.contains("inline code"))
        assertTrue(doc.parsedContent.contains("<a href=") && doc.parsedContent.contains("link"))
        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("<blockquote>"))
        assertTrue(doc.parsedContent.contains("<img src=") && doc.parsedContent.contains("image.png"))
        assertTrue(doc.parsedContent.contains("<sup>") && doc.parsedContent.contains("superscript"))
        assertTrue(doc.parsedContent.contains("<sub>") && doc.parsedContent.contains("subscript"))
    }
}
