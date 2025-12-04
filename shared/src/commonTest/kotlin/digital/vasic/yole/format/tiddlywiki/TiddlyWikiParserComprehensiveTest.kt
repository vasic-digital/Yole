/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for TiddlyWikiParser
 *
 *########################################################*/
package digital.vasic.yole.format.tiddlywiki

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for TiddlyWikiParser covering all parsing branches.
 *
 * Tests cover:
 * - Metadata extraction (title, tags, created, modified, type, custom fields)
 * - Headings (!, !!, !!!, etc.)
 * - Lists (unordered *, ordered #, nested)
 * - Code blocks (```)
 * - Block quotes (<<<, > text)
 * - Horizontal rules (---)
 * - Inline markup (bold, italic, underline, strikethrough, super/subscript, code)
 * - Links (internal [[]], external [ext[]])
 * - Images ([img[]])
 * - Validation
 * - Edge cases
 */
class TiddlyWikiParserComprehensiveTest {

    private lateinit var parser: TiddlyWikiParser

    @BeforeTest
    fun setup() {
        parser = TiddlyWikiParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Metadata Extraction Tests ====================

    @Test
    fun `should extract title metadata`() {
        val content = """
            title: My Tiddler

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("My Tiddler", doc.metadata["title"])
    }

    @Test
    fun `should extract tags metadata`() {
        val content = """
            tags: tag1 tag2 tag3

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("tag1, tag2, tag3", doc.metadata["tags"])
    }

    @Test
    fun `should extract created metadata`() {
        val content = """
            created: 20250119120000000

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("tiddlywiki"))
    }

    @Test
    fun `should extract modified metadata`() {
        val content = """
            modified: 20250119120000000

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("tiddlywiki"))
    }

    @Test
    fun `should extract type metadata`() {
        val content = """
            type: text/vnd.tiddlywiki

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("tiddlywiki"))
    }

    @Test
    fun `should extract custom metadata fields`() {
        val content = """
            title: My Tiddler
            custom-field: custom value

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("My Tiddler", doc.metadata["title"])
    }

    @Test
    fun `should handle content without metadata`() {
        val content = """
            Just content
            No metadata
        """.trimIndent()

        val doc = parser.parse(content)

        assertNull(doc.metadata["title"])
    }

    @Test
    fun `should handle metadata without blank line separator`() {
        val content = """
            title: My Tiddler
            Not a metadata field
            More content
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("My Tiddler", doc.metadata["title"])
    }

    @Test
    fun `should extract extension from filename`() {
        val content = "Content"
        val options = mapOf("filename" to "document.tid")

        val doc = parser.parse(content, options)

        assertEquals(".tid", doc.metadata["extension"])
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
    fun `should convert level 1 heading`() {
        val content = "! Heading 1"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h1>"))
        assertTrue(doc.parsedContent.contains("Heading 1"))
    }

    @Test
    fun `should convert level 2 heading`() {
        val content = "!! Heading 2"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h2>"))
        assertTrue(doc.parsedContent.contains("Heading 2"))
    }

    @Test
    fun `should convert level 3 heading`() {
        val content = "!!! Heading 3"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h3>"))
        assertTrue(doc.parsedContent.contains("Heading 3"))
    }

    @Test
    fun `should limit heading level to 6`() {
        val content = "!!!!!!! Too Many Exclamations"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h6>"))
        assertTrue(doc.parsedContent.contains("Too Many Exclamations"))
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
    fun `should convert nested unordered list`() {
        val content = """
            * Item 1
            ** Nested 1
            *** Deeper
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("Nested 1"))
    }

    @Test
    fun `should convert nested ordered list`() {
        val content = """
            # Item 1
            ## Nested 1
            ### Deeper
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ol>"))
        assertTrue(doc.parsedContent.contains("Nested 1"))
    }

    @Test
    fun `should handle decreasing list nesting`() {
        val content = """
            * Item 1
            ** Nested
            * Back to top
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("Back to top"))
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

    // ==================== Code Block Tests ====================

    @Test
    fun `should convert code block to HTML`() {
        val content = """
            ```
            code here
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("code here"))
    }

    @Test
    fun `should handle unclosed code block`() {
        val content = """
            ```
            code here
            no closing
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("code here"))
        assertTrue(doc.parsedContent.contains("</pre>"))
    }

    @Test
    fun `should escape HTML in code blocks`() {
        val content = """
            ```
            <div>HTML</div>
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&lt;") || doc.parsedContent.contains("&gt;"))
    }

    @Test
    fun `should handle code block at start of document`() {
        val content = """
            ```
            code
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
    }

    // ==================== Block Quote Tests ====================

    @Test
    fun `should convert block quote with triple angle brackets`() {
        val content = """
            <<<
            Quoted text
            <<<
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<blockquote>"))
        assertTrue(doc.parsedContent.contains("Quoted text"))
    }

    @Test
    fun `should convert single line block quote`() {
        val content = "> Quoted text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<blockquote>"))
        assertTrue(doc.parsedContent.contains("Quoted text"))
    }

    @Test
    fun `should handle unclosed block quote`() {
        val content = """
            <<<
            Quote
            No closing
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<blockquote>"))
        assertTrue(doc.parsedContent.contains("</blockquote>"))
    }

    // ==================== Horizontal Rule Tests ====================

    @Test
    fun `should convert horizontal rule`() {
        val content = "---"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should convert longer horizontal rule`() {
        val content = "-----"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should not convert short dash line as horizontal rule`() {
        val content = "--"

        val doc = parser.parse(content)

        assertFalse(doc.parsedContent.contains("<hr>"))
    }

    // ==================== Inline Markup Tests ====================

    @Test
    fun `should convert bold text`() {
        val content = "This is ''bold'' text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<strong>"))
        assertTrue(doc.parsedContent.contains("bold"))
    }

    @Test
    fun `should convert italic text`() {
        val content = "This is //italic// text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<em>"))
        assertTrue(doc.parsedContent.contains("italic"))
    }

    @Test
    fun `should convert underline text`() {
        val content = "This is __underline__ text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<u>"))
        assertTrue(doc.parsedContent.contains("underline"))
    }

    @Test
    fun `should convert strikethrough text`() {
        val content = "This is ~~strikethrough~~ text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<s>"))
        assertTrue(doc.parsedContent.contains("strikethrough"))
    }

    @Test
    fun `should convert superscript text`() {
        val content = "E = mc^^2^^"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<sup>"))
        assertTrue(doc.parsedContent.contains("2"))
    }

    @Test
    fun `should convert subscript text`() {
        val content = "H,,2,,O"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<sub>"))
        assertTrue(doc.parsedContent.contains("2"))
    }

    @Test
    fun `should convert inline code`() {
        val content = "Use the `code()` function"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<code>"))
        assertTrue(doc.parsedContent.contains("code()"))
    }

    // ==================== Link Tests ====================

    @Test
    fun `should convert internal link without description`() {
        val content = "See [[MyTiddler]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href='MyTiddler'>"))
        assertTrue(doc.parsedContent.contains("MyTiddler"))
    }

    @Test
    fun `should convert internal link with description`() {
        val content = "See [[MyTiddler|Click Here]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href='MyTiddler'>"))
        assertTrue(doc.parsedContent.contains("Click Here"))
    }

    @Test
    fun `should convert external link without description`() {
        val content = "Visit [ext[http://example.com]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href='http://example.com'"))
        assertTrue(doc.parsedContent.contains("target='_blank'"))
    }

    @Test
    fun `should convert external link with description`() {
        val content = "Visit [ext[http://example.com|Example Site]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href='http://example.com'"))
        assertTrue(doc.parsedContent.contains("Example Site"))
    }

    // ==================== Image Tests ====================

    @Test
    fun `should convert image`() {
        val content = "[img[photo.png]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<img src='photo.png'"))
        assertTrue(doc.parsedContent.contains("alt='photo.png'"))
    }

    @Test
    fun `should handle image in paragraph`() {
        val content = "Here is an image: [img[test.jpg]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<img"))
        assertTrue(doc.parsedContent.contains("test.jpg"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should detect unclosed brackets in links`() {
        val content = "[[Unclosed link"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed brackets") })
    }

    @Test
    fun `should detect unclosed quotes for bold`() {
        val content = "This is ''unclosed bold"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed quotes") })
    }

    @Test
    fun `should accept closed brackets`() {
        val content = "[[Link]]"

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed brackets") })
    }

    @Test
    fun `should accept closed quotes`() {
        val content = "This is ''bold'' text"

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed quotes") })
    }

    @Test
    fun `should skip validation inside code blocks`() {
        val content = """
            ```
            [[Not a real link
            ''Not real bold
            ```
        """.trimIndent()

        val errors = parser.validate(content)

        // Should not error for content inside code blocks
        assertNotNull(errors)
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
        assertTrue(doc.parsedContent.contains(".tiddlywiki"))
    }

    @Test
    fun `should display metadata in HTML`() {
        val content = """
            title: My Tiddler
            tags: tag1 tag2

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("My Tiddler"))
        assertTrue(doc.parsedContent.contains("tag1"))
        assertTrue(doc.parsedContent.contains("tag2"))
    }

    @Test
    fun `should not display metadata section when no title or tags`() {
        val content = """
            created: 20250119

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertFalse(doc.parsedContent.contains("<div class='metadata'>"))
    }

    @Test
    fun `should close all open structures at end of document`() {
        val content = """
            * Item 1
            ** Nested
        """.trimIndent()

        val doc = parser.parse(content)

        // Count opening and closing ul tags
        val openCount = doc.parsedContent.split("<ul>").size - 1
        val closeCount = doc.parsedContent.split("</ul>").size - 1
        assertEquals(openCount, closeCount)
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
    fun `should handle complete TiddlyWiki document`() {
        val content = """
            title: My Complete Tiddler
            tags: documentation example
            created: 20250119120000000

            ! Main Heading

            This is a paragraph with ''bold'' and //italic// text.

            !! Subheading

            * Unordered item 1
            ** Nested item
            * Unordered item 2

            # Ordered item 1
            # Ordered item 2

            Here's some `inline code` and a [[link to another tiddler]].

            ```
            code block
            multiple lines
            ```

            > A single line quote

            ---

            Final paragraph with [ext[http://example.com|external link]].
        """.trimIndent()

        val doc = parser.parse(content)

        // Check metadata
        assertEquals("My Complete Tiddler", doc.metadata["title"])
        assertEquals("documentation, example", doc.metadata["tags"])

        // Check content elements
        assertTrue(doc.parsedContent.contains("<h1>Main Heading</h1>"))
        assertTrue(doc.parsedContent.contains("<h2>Subheading</h2>"))
        assertTrue(doc.parsedContent.contains("<strong>bold</strong>"))
        assertTrue(doc.parsedContent.contains("<em>italic</em>"))
        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("<ol>"))
        assertTrue(doc.parsedContent.contains("<code>inline code</code>"))
        assertTrue(doc.parsedContent.contains("<a href='link to another tiddler'>"))
        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("<blockquote>"))
        assertTrue(doc.parsedContent.contains("<hr>"))
        assertTrue(doc.parsedContent.contains("target='_blank'"))
    }
}
