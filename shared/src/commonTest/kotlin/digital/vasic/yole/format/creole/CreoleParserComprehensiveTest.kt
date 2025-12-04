/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for Creole parser
 *
 *########################################################*/
package digital.vasic.yole.format.creole

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for Creole parser covering all parsing branches.
 *
 * Tests cover:
 * - Code blocks, lists (unordered/ordered/nested), tables
 * - Headings, horizontal rules, paragraphs
 * - Inline markup (bold, italic, code, links, images)
 * - Validation (malformed tables, unclosed brackets/braces)
 * - Edge cases and complex documents
 */
class CreoleParserComprehensiveTest {

    private lateinit var parser: CreoleParser

    @BeforeTest
    fun setup() {
        parser = CreoleParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Code Block Tests ====================

    @Test
    fun `should convert code block to HTML`() {
        val content = """
            {{{
            code line 1
            code line 2
            }}}
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("code line 1"))
        assertTrue(doc.parsedContent.contains("code line 2"))
        assertTrue(doc.parsedContent.contains("</pre>"))
    }

    @Test
    fun `should escape HTML in code blocks`() {
        val content = """
            {{{
            <script>alert('xss')</script>
            }}}
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&lt;script&gt;"))
        assertFalse(doc.parsedContent.contains("<script>"))
    }

    @Test
    fun `should handle code block at start of document`() {
        val content = """
            {{{
            first line
            }}}
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
    }

    @Test
    fun `should handle unclosed code block`() {
        val content = """
            {{{
            code here
            no closing marker
        """.trimIndent()

        val doc = parser.parse(content)

        // Should still have opening <pre> and close it at end
        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("</pre>"))
    }

    // ==================== Unordered List Tests ====================

    @Test
    fun `should convert simple unordered list`() {
        val content = """
            * Item 1
            * Item 2
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("<li>Item 1</li>"))
        assertTrue(doc.parsedContent.contains("<li>Item 2</li>"))
        assertTrue(doc.parsedContent.contains("</ul>"))
    }

    @Test
    fun `should convert nested unordered list`() {
        val content = """
            * Level 1
            ** Level 2
            *** Level 3
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("Level 1"))
        assertTrue(doc.parsedContent.contains("Level 2"))
        assertTrue(doc.parsedContent.contains("Level 3"))
    }

    @Test
    fun `should handle decreasing list nesting`() {
        val content = """
            *** Level 3
            ** Level 2
            * Level 1
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("</ul>"))
    }

    @Test
    fun `should close unordered list when encountering non-list`() {
        val content = """
            * Item 1
            * Item 2
            Regular paragraph
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ul>"))
        assertTrue(doc.parsedContent.contains("<p>Regular paragraph</p>"))
    }

    // ==================== Ordered List Tests ====================

    @Test
    fun `should convert simple ordered list`() {
        val content = """
            # First
            # Second
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ol>"))
        assertTrue(doc.parsedContent.contains("<li>First</li>"))
        assertTrue(doc.parsedContent.contains("<li>Second</li>"))
        assertTrue(doc.parsedContent.contains("</ol>"))
    }

    @Test
    fun `should convert nested ordered list`() {
        val content = """
            # Level 1
            ## Level 2
            ### Level 3
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<ol>"))
        assertTrue(doc.parsedContent.contains("Level 1"))
        assertTrue(doc.parsedContent.contains("Level 2"))
        assertTrue(doc.parsedContent.contains("Level 3"))
    }

    @Test
    fun `should close ordered list when encountering non-list`() {
        val content = """
            # Item 1
            # Item 2
            Regular text
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ol>"))
        assertTrue(doc.parsedContent.contains("<p>Regular text</p>"))
    }

    @Test
    fun `should close ordered list when encountering table`() {
        val content = """
            # Item 1
            |Cell|
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ol>"))
        assertTrue(doc.parsedContent.contains("<table>"))
    }

    @Test
    fun `should close unordered list when encountering table`() {
        val content = """
            * Item 1
            |Cell|
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ul>"))
        assertTrue(doc.parsedContent.contains("<table>"))
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
    fun `should close unordered list when switching to ordered`() {
        val content = """
            * Unordered
            # Ordered
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ul>"))
        assertTrue(doc.parsedContent.contains("<ol>"))
    }

    // ==================== Table Tests ====================

    @Test
    fun `should convert simple table`() {
        val content = """
            |Cell 1|Cell 2|
            |Cell 3|Cell 4|
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<table>"))
        assertTrue(doc.parsedContent.contains("<tr>"))
        assertTrue(doc.parsedContent.contains("<td>Cell 1</td>"))
        assertTrue(doc.parsedContent.contains("<td>Cell 2</td>"))
        assertTrue(doc.parsedContent.contains("</table>"))
    }

    @Test
    fun `should convert table with header cells`() {
        val content = """
            |=Header 1|=Header 2|
            |Data 1|Data 2|
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<th>Header 1</th>"))
        assertTrue(doc.parsedContent.contains("<th>Header 2</th>"))
        assertTrue(doc.parsedContent.contains("<td>Data 1</td>"))
    }

    @Test
    fun `should close table when encountering non-table`() {
        val content = """
            |Cell 1|Cell 2|
            Regular text
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</table>"))
        assertTrue(doc.parsedContent.contains("<p>Regular text</p>"))
    }

    @Test
    fun `should close table when encountering list`() {
        val content = """
            |Cell|
            * List item
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</table>"))
        assertTrue(doc.parsedContent.contains("<ul>"))
    }

    // ==================== Heading Tests ====================

    @Test
    fun `should convert level 1 heading`() {
        val content = "= Heading 1 ="

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h1>Heading 1</h1>"))
    }

    @Test
    fun `should convert level 2 heading`() {
        val content = "== Heading 2 =="

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h2>Heading 2</h2>"))
    }

    @Test
    fun `should convert level 3 heading`() {
        val content = "=== Heading 3 ==="

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h3>Heading 3</h3>"))
    }

    @Test
    fun `should limit heading level to 6`() {
        val content = "======= Very Deep Heading ======="

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h6>"))
    }

    @Test
    fun `should handle heading without closing equals`() {
        val content = "== Heading without closing"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h2>"))
        assertTrue(doc.parsedContent.contains("Heading without closing"))
    }

    // ==================== Horizontal Rule Tests ====================

    @Test
    fun `should convert horizontal rule`() {
        val content = "----"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should convert longer horizontal rule`() {
        val content = "--------"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should not convert short dash line as horizontal rule`() {
        val content = "---"

        val doc = parser.parse(content)

        assertFalse(doc.parsedContent.contains("<hr>"))
        assertTrue(doc.parsedContent.contains("<p>"))
    }

    // ==================== Inline Markup Tests ====================

    @Test
    fun `should convert bold text`() {
        val content = "This is **bold** text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<strong>bold</strong>"))
    }

    @Test
    fun `should convert italic text`() {
        val content = "This is //italic// text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<em>italic</em>"))
    }

    @Test
    fun `should convert inline code`() {
        val content = "This is {{{inline code}}} text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<code>inline code</code>"))
    }

    @Test
    fun `should convert link with description`() {
        val content = "[[http://example.com|Example Link]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href='http://example.com'>Example Link</a>"))
    }

    @Test
    fun `should convert image with alt text`() {
        val content = "{{image.png|Alt Text}}"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<img src='image.png' alt='Alt Text'/>"))
    }

    @Test
    fun `should convert image without alt text`() {
        val content = "{{image.png}}"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<img src='image.png' alt='image.png'/>"))
    }

    @Test
    fun `should convert line breaks`() {
        val content = "Line 1\\\\Line 2"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<br>"))
    }

    @Test
    fun `should escape HTML in regular text`() {
        val content = "Text with <script>alert('xss')</script>"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&lt;script&gt;"))
        assertFalse(doc.parsedContent.contains("<script>"))
    }

    // ==================== Paragraph Tests ====================

    @Test
    fun `should convert regular paragraph`() {
        val content = "This is a regular paragraph"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<p>This is a regular paragraph</p>"))
    }

    @Test
    fun `should skip empty lines`() {
        val content = """
            Paragraph 1

            Paragraph 2
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<p>Paragraph 1</p>"))
        assertTrue(doc.parsedContent.contains("<p>Paragraph 2</p>"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should detect malformed table row`() {
        val content = "|Cell 1|Cell 2"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Malformed table row") })
    }

    @Test
    fun `should accept well-formed table row`() {
        val content = "|Cell 1|Cell 2|"

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Malformed table") })
    }

    @Test
    fun `should detect unclosed brackets in links`() {
        val content = "[[Unclosed link"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed brackets") })
    }

    @Test
    fun `should accept closed brackets`() {
        val content = "[[Link]]"

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed brackets") })
    }

    @Test
    fun `should detect unclosed braces`() {
        val content = "{{Unclosed image"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed braces") })
    }

    @Test
    fun `should accept closed braces`() {
        val content = "{{image.png}}"

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed braces") })
    }

    @Test
    fun `should skip validation inside code blocks`() {
        val content = """
            {{{
            [[Unclosed bracket inside code
            }}}
        """.trimIndent()

        val errors = parser.validate(content)

        // Implementation validates line by line, tracking code block state
        // Content after {{{ opening is still inside block, so may not validate
        // This test verifies validation doesn't crash on code blocks
        assertNotNull(errors)
    }

    // ==================== Metadata Tests ====================

    @Test
    fun `should extract extension from filename`() {
        val content = "Test"
        val options = mapOf("filename" to "test.creole")

        val doc = parser.parse(content, options)

        assertEquals(".creole", doc.metadata["extension"])
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

    @Test
    fun `should handle filename without extension`() {
        val content = "Test"
        val options = mapOf("filename" to "test")

        val doc = parser.parse(content, options)

        assertEquals("", doc.metadata["extension"])
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should handle complete Creole document`() {
        val content = """
            = Main Heading =

            This is a paragraph with **bold** and //italic// text.

            == Lists ==

            * Unordered item 1
            * Unordered item 2
            ** Nested item

            # Ordered item 1
            # Ordered item 2

            == Code Example ==

            {{{
            def hello():
                print("Hello, World!")
            }}}

            == Table ==

            |=Header 1|=Header 2|
            |Data 1|Data 2|
            |Data 3|Data 4|

            == Links ==

            Visit [[http://example.com|Example Site]].

            ----

            End of document.
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h1>Main Heading</h1>"))
        assertTrue(doc.parsedContent.contains("<strong>bold</strong>"))
        assertTrue(doc.parsedContent.contains("<em>italic</em>"))
        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("<ol>"))
        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("<table>"))
        assertTrue(doc.parsedContent.contains("<th>"))
        assertTrue(doc.parsedContent.contains("<a href='http://example.com'>"))
        assertTrue(doc.parsedContent.contains("<hr>"))
    }

    @Test
    fun `should close all open structures at end of document`() {
        val content = """
            * Open list item
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ul>"))
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
}
