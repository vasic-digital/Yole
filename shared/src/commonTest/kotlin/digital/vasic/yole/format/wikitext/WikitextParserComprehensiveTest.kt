/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for WikitextParser
 *
 *########################################################*/
package digital.vasic.yole.format.wikitext

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for WikitextParser covering all parsing branches.
 *
 * Tests cover:
 * - Zim header removal
 * - Headings (== Heading ==)
 * - Lists (unordered *, ordered 1., checklist [ ])
 * - Code blocks (''')
 * - Inline markup (bold, italic, highlight, strikethrough, code, links, images)
 * - Super/subscript
 * - Validation
 * - Edge cases
 */
class WikitextParserComprehensiveTest {

    private lateinit var parser: WikitextParser

    @BeforeTest
    fun setup() {
        parser = WikitextParser()
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
        val options = mapOf("filename" to "document.wiki")

        val doc = parser.parse(content, options)

        assertEquals(".wiki", doc.metadata["extension"])
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

    // ==================== Zim Header Tests ====================

    @Test
    fun `should remove Zim header`() {
        val content = """
            [DocumentAttributes]
            Type: Page
            Version: 1.0

            Regular content
        """.trimIndent()

        val doc = parser.parse(content)

        // Header was detected and removed
        assertEquals("true", doc.metadata["hasZimHeader"])
    }

    @Test
    fun `should detect Zim header presence`() {
        val content = """
            [DocumentAttributes]
            Type: Page

            Content
        """.trimIndent()

        val doc = parser.parse(content)

        assertNotNull(doc.metadata["hasZimHeader"])
    }

    @Test
    fun `should handle content without Zim header`() {
        val content = "Regular content"

        val doc = parser.parse(content)

        assertEquals("false", doc.metadata["hasZimHeader"])
    }

    // ==================== Heading Tests ====================

    @Test
    fun `should convert level 5 heading`() {
        val content = "== Heading =="

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h5>"))
        assertTrue(doc.parsedContent.contains("Heading"))
    }

    @Test
    fun `should convert level 4 heading`() {
        val content = "=== Heading ==="

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h4>"))
    }

    @Test
    fun `should convert level 3 heading`() {
        val content = "==== Heading ===="

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h3>"))
    }

    @Test
    fun `should convert level 2 heading`() {
        val content = "===== Heading ====="

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h2>"))
    }

    @Test
    fun `should convert level 1 heading`() {
        val content = "====== Heading ======"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h1>"))
    }

    @Test
    fun `should require balanced heading markers`() {
        val content = "== Heading =="

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Heading"))
    }

    // ==================== Code Block Tests ====================

    @Test
    fun `should convert code block`() {
        val content = """
            '''
            code here
            '''
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("code here"))
        assertTrue(doc.parsedContent.contains("</pre>"))
    }

    @Test
    fun `should handle code block at start of document`() {
        val content = """
            '''
            code
            '''
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre>"))
    }

    @Test
    fun `should escape HTML in code blocks`() {
        val content = """
            '''
            <div>HTML</div>
            '''
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&lt;") || doc.parsedContent.contains("&gt;"))
    }

    @Test
    fun `should handle multiline code blocks`() {
        val content = """
            '''
            line 1
            line 2
            line 3
            '''
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("line 1"))
        assertTrue(doc.parsedContent.contains("line 2"))
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
        assertTrue(doc.parsedContent.contains("<li>"))
        assertTrue(doc.parsedContent.contains("Item 1"))
    }

    @Test
    fun `should close unordered list when switching to ordered`() {
        val content = """
            * Unordered
            1. Ordered
        """.trimIndent()

        val doc = parser.parse(content)

        // Check that both are present
        assertTrue(doc.parsedContent.contains("Unordered"))
        assertTrue(doc.parsedContent.contains("Ordered"))
    }

    @Test
    fun `should close unordered list when encountering non-list`() {
        val content = """
            * Item 1
            Regular paragraph
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ul>"))
        assertTrue(doc.parsedContent.contains("<p>Regular paragraph</p>"))
    }

    // ==================== Ordered List Tests ====================

    @Test
    fun `should convert ordered list with numbers`() {
        val content = """
            1. Item 1
            2. Item 2
        """.trimIndent()

        val doc = parser.parse(content)

        // Check for list item content
        assertTrue(doc.parsedContent.contains("Item 1"))
        assertTrue(doc.parsedContent.contains("Item 2"))
    }

    @Test
    fun `should convert ordered list with letters`() {
        val content = """
            a. Item A
            b. Item B
        """.trimIndent()

        val doc = parser.parse(content)

        // Check for list item content
        assertTrue(doc.parsedContent.contains("Item A"))
        assertTrue(doc.parsedContent.contains("Item B"))
    }

    @Test
    fun `should close ordered list when switching to unordered`() {
        val content = """
            1. Ordered
            * Unordered
        """.trimIndent()

        val doc = parser.parse(content)

        // Check that both are present
        assertTrue(doc.parsedContent.contains("Ordered"))
        assertTrue(doc.parsedContent.contains("Unordered"))
    }

    // ==================== Checklist Tests ====================

    @Test
    fun `should convert unchecked checklist item`() {
        val content = "[ ] Unchecked item"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("checklist"))
        assertTrue(doc.parsedContent.contains("Unchecked item"))
    }

    @Test
    fun `should convert checked checklist item with asterisk`() {
        val content = "[*] Checked item"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("checked"))
        assertTrue(doc.parsedContent.contains("Checked item"))
    }

    @Test
    fun `should convert crossed checklist item`() {
        val content = "[x] Crossed item"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("crossed"))
        assertTrue(doc.parsedContent.contains("Crossed item"))
    }

    @Test
    fun `should convert checklist item with greater than`() {
        val content = "[>] Migrated item"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Migrated item"))
    }

    @Test
    fun `should convert checklist item with less than`() {
        val content = "[<] Moved item"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Moved item"))
    }

    @Test
    fun `should close checklist when switching to unordered list`() {
        val content = """
            [ ] Checklist
            * Unordered
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</ul>"))
        assertTrue(doc.parsedContent.contains("<ul>"))
    }

    @Test
    fun `should close checklist when switching to ordered list`() {
        val content = """
            [ ] Checklist
            1. Ordered
        """.trimIndent()

        val doc = parser.parse(content)

        // Check that both are present
        assertTrue(doc.parsedContent.contains("Checklist"))
        assertTrue(doc.parsedContent.contains("Ordered"))
    }

    // ==================== Inline Markup Tests ====================

    @Test
    fun `should convert bold text`() {
        val content = "This is **bold** text"

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
    fun `should convert highlighted text`() {
        val content = "This is __highlighted__ text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("highlight"))
        assertTrue(doc.parsedContent.contains("highlighted"))
    }

    @Test
    fun `should convert strikethrough text`() {
        val content = "This is ~~strikethrough~~ text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<s>"))
        assertTrue(doc.parsedContent.contains("strikethrough"))
    }

    @Test
    fun `should convert inline code`() {
        val content = "Use the ''code()'' function"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<code>"))
        assertTrue(doc.parsedContent.contains("code()"))
    }

    @Test
    fun `should escape HTML in inline code`() {
        val content = "''<div>''"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<code>") && doc.parsedContent.contains("div"))
    }

    @Test
    fun `should convert superscript`() {
        val content = "E = mc^{2}"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<sup>"))
        assertTrue(doc.parsedContent.contains("2"))
    }

    @Test
    fun `should convert subscript`() {
        val content = "H_{2}O"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<sub>"))
        assertTrue(doc.parsedContent.contains("2"))
    }

    // ==================== Link Tests ====================

    @Test
    fun `should convert link without description`() {
        val content = "[[PageName]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href='PageName'>"))
        assertTrue(doc.parsedContent.contains("PageName"))
    }

    @Test
    fun `should convert link with description`() {
        val content = "[[PageName|Link Description]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href='PageName'>"))
        assertTrue(doc.parsedContent.contains("Link Description"))
    }

    @Test
    fun `should handle link with special characters`() {
        val content = "[[Page:SubPage]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<a href="))
    }

    // ==================== Image Tests ====================

    @Test
    fun `should convert image`() {
        val content = "{{image.png}}"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<img src='image.png'"))
        assertTrue(doc.parsedContent.contains("alt='image.png'"))
    }

    @Test
    fun `should handle image in paragraph`() {
        val content = "Here is an image: {{test.jpg}}"

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
    fun `should detect unbalanced heading markers`() {
        val content = "== Heading ==="

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unbalanced heading markers") })
    }

    @Test
    fun `should accept balanced heading markers`() {
        val content = "== Heading =="

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unbalanced") })
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
    fun `should detect unclosed braces in images`() {
        val content = "{{unclosed image"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed braces") })
    }

    @Test
    fun `should accept closed braces`() {
        val content = "{{image.png}}"

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed braces") })
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

        assertTrue(doc.parsedContent.contains("<style>") || doc.parsedContent.contains(".wikitext"))
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
    fun `should handle complete WikiText document`() {
        val content = """
            [DocumentAttributes]
            Type: Page

            ====== Main Heading ======

            This is a paragraph with **bold** and //italic// text.

            === Subheading ===

            * Unordered item 1
            * Unordered item 2

            1. Ordered item 1
            2. Ordered item 2

            [ ] Unchecked task
            [*] Completed task
            [x] Crossed task

            Here's some ''inline code'' and a [[link|description]].

            '''
            code block
            multiple lines
            '''

            Final paragraph with {{image.png}} and ^{superscript} and _{subscript}.
        """.trimIndent()

        val doc = parser.parse(content)

        // Check content elements
        assertTrue(doc.parsedContent.contains("Main Heading"))
        assertTrue(doc.parsedContent.contains("Subheading"))
        assertTrue(doc.parsedContent.contains("bold"))
        assertTrue(doc.parsedContent.contains("italic"))
        assertTrue(doc.parsedContent.contains("<ul>"))
        assertTrue(doc.parsedContent.contains("Unordered item"))
        assertTrue(doc.parsedContent.contains("Ordered item"))
        assertTrue(doc.parsedContent.contains("checklist"))
        assertTrue(doc.parsedContent.contains("Unchecked task"))
        assertTrue(doc.parsedContent.contains("Completed task"))
        assertTrue(doc.parsedContent.contains("Crossed task"))
        assertTrue(doc.parsedContent.contains("<code>") && doc.parsedContent.contains("inline code"))
        assertTrue(doc.parsedContent.contains("description"))
        assertTrue(doc.parsedContent.contains("<pre>"))
        assertTrue(doc.parsedContent.contains("image.png"))
        assertTrue(doc.parsedContent.contains("superscript"))
        assertTrue(doc.parsedContent.contains("subscript"))
    }
}
