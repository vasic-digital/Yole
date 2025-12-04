/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for OrgModeParser
 *
 *########################################################*/
package digital.vasic.yole.format.orgmode

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for OrgModeParser covering all parsing branches.
 *
 * Tests cover:
 * - Heading extraction (various levels)
 * - TODO/DONE state extraction
 * - Property extraction
 * - Block parsing (#+BEGIN_/#+END_)
 * - Inline formatting (bold, italic, underline, code, links)
 * - HTML generation
 * - Validation (blocks, heading levels)
 * - Edge cases
 */
class OrgModeParserComprehensiveTest {

    private lateinit var parser: OrgModeParser

    @BeforeTest
    fun setup() {
        parser = OrgModeParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Metadata Tests ====================

    @Test
    fun `should count headings`() {
        val content = """
            * Heading 1
            ** Heading 2
            * Heading 3
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("3", doc.metadata["headings"])
    }

    @Test
    fun `should count TODO items`() {
        val content = """
            * TODO Task 1
            * DONE Task 2
            * Regular heading
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("2", doc.metadata["todos"])
    }

    @Test
    fun `should count properties`() {
        val content = """
            :ID: abc123
            :CREATED: 2025-01-01
            :CATEGORY: Work
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("3", doc.metadata["properties"])
    }

    @Test
    fun `should track maximum heading level`() {
        val content = """
            * Level 1
            ** Level 2
            *** Level 3
            **** Level 4
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("4", doc.metadata["max_level"])
    }

    @Test
    fun `should handle content with no headings`() {
        val content = "Just plain text"

        val doc = parser.parse(content)

        assertEquals("0", doc.metadata["headings"])
        assertEquals("0", doc.metadata["max_level"])
    }

    @Test
    fun `should handle content with no TODOs`() {
        val content = """
            * Regular heading
            Some text
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("0", doc.metadata["todos"])
    }

    @Test
    fun `should handle content with no properties`() {
        val content = """
            * Heading
            Some text
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("0", doc.metadata["properties"])
    }

    // ==================== Heading Tests ====================

    @Test
    fun `should extract level 1 heading`() {
        val content = "* Main Heading"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-heading-1"))
        assertTrue(doc.parsedContent.contains("Main Heading"))
    }

    @Test
    fun `should extract level 2 heading`() {
        val content = "** Subheading"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-heading-2"))
        assertTrue(doc.parsedContent.contains("Subheading"))
    }

    @Test
    fun `should extract level 3 heading`() {
        val content = "*** Sub-subheading"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-heading-3"))
        assertTrue(doc.parsedContent.contains("Sub-subheading"))
    }

    @Test
    fun `should extract level 6 heading`() {
        val content = "****** Deep heading"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-heading-6"))
        assertTrue(doc.parsedContent.contains("Deep heading"))
    }

    @Test
    fun `should trim whitespace from heading title`() {
        val content = "*   Heading with spaces   "

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Heading with spaces"))
    }

    // ==================== TODO Tests ====================

    @Test
    fun `should extract TODO state`() {
        val content = "* TODO Task to do"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-todo-todo"))
        assertTrue(doc.parsedContent.contains("TODO"))
        assertTrue(doc.parsedContent.contains("Task to do"))
    }

    @Test
    fun `should extract DONE state`() {
        val content = "* DONE Completed task"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-todo-done"))
        assertTrue(doc.parsedContent.contains("DONE"))
        assertTrue(doc.parsedContent.contains("Completed task"))
    }

    @Test
    fun `should separate TODO state from title`() {
        val content = "* TODO Important task"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("TODO"))
        assertTrue(doc.parsedContent.contains("Important task"))
    }

    @Test
    fun `should handle heading without TODO state`() {
        val content = "* Regular heading"

        val doc = parser.parse(content)

        assertFalse(doc.parsedContent.contains("org-todo"))
        assertTrue(doc.parsedContent.contains("Regular heading"))
    }

    // ==================== Block Tests ====================

    @Test
    fun `should parse BEGIN END block`() {
        val content = """
            #+BEGIN_SRC
            code here
            #+END_SRC
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-block"))
        assertTrue(doc.parsedContent.contains("code here"))
    }

    @Test
    fun `should include block header`() {
        val content = """
            #+BEGIN_EXAMPLE
            example content
            #+END_EXAMPLE
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-block-header"))
        assertTrue(doc.parsedContent.contains("BEGIN_EXAMPLE"))
    }

    @Test
    fun `should include block content`() {
        val content = """
            #+BEGIN_QUOTE
            quoted text
            #+END_QUOTE
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-block-content"))
        assertTrue(doc.parsedContent.contains("quoted text"))
    }

    @Test
    fun `should escape HTML in block content`() {
        val content = """
            #+BEGIN_SRC
            <div>HTML</div>
            #+END_SRC
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&lt;") && doc.parsedContent.contains("&gt;"))
    }

    // ==================== Property Tests ====================

    @Test
    fun `should extract single property`() {
        val content = ":CREATED: 2025-01-01:"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-property"))
        assertTrue(doc.parsedContent.contains("CREATED"))
        assertTrue(doc.parsedContent.contains("2025-01-01"))
    }

    @Test
    fun `should extract multiple properties`() {
        val content = """
            :ID: abc123
            :CATEGORY: Work
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("ID"))
        assertTrue(doc.parsedContent.contains("abc123"))
        assertTrue(doc.parsedContent.contains("CATEGORY"))
        assertTrue(doc.parsedContent.contains("Work"))
    }

    @Test
    fun `should trim property key and value`() {
        val content = ":  KEY  :   value with spaces   "

        val doc = parser.parse(content)

        assertEquals("1", doc.metadata["properties"])
    }

    // ==================== Inline Formatting Tests ====================

    @Test
    fun `should format bold text`() {
        val content = "Text with **bold** formatting"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-bold"))
        assertTrue(doc.parsedContent.contains("bold"))
    }

    @Test
    fun `should format italic text`() {
        val content = "Text with /italic/ formatting"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-italic"))
        assertTrue(doc.parsedContent.contains("italic"))
    }

    @Test
    fun `should format underlined text`() {
        val content = "Text with _underlined_ formatting"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-underline"))
        assertTrue(doc.parsedContent.contains("underlined"))
    }

    @Test
    fun `should format strikethrough text`() {
        val content = "Text with +strikethrough+ formatting"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-strikethrough"))
        assertTrue(doc.parsedContent.contains("strikethrough"))
    }

    @Test
    fun `should format verbatim text`() {
        val content = "Text with =\"verbatim=\" formatting"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-verbatim"))
        assertTrue(doc.parsedContent.contains("verbatim"))
    }

    @Test
    fun `should format code text`() {
        val content = "Text with ~code~ formatting"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-code"))
        assertTrue(doc.parsedContent.contains("code"))
    }

    // ==================== Link Tests ====================

    @Test
    fun `should format link with text`() {
        val content = "[[https://example.com][Example Link]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-link"))
        assertTrue(doc.parsedContent.contains("href=\"https://example.com\""))
        assertTrue(doc.parsedContent.contains("Example Link"))
    }

    @Test
    fun `should format link without text`() {
        val content = "[[https://example.com]]"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-link"))
        assertTrue(doc.parsedContent.contains("href=\"https://example.com\""))
        assertTrue(doc.parsedContent.contains("https://example.com"))
    }

    // ==================== HTML Generation Tests ====================

    @Test
    fun `should generate light mode HTML`() {
        val content = "* Heading"

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("light"))
        assertTrue(html.contains("org-mode-document"))
    }

    @Test
    fun `should generate dark mode HTML`() {
        val content = "* Heading"

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = false)

        assertTrue(html.contains("dark"))
        assertTrue(html.contains("org-mode-document"))
    }

    @Test
    fun `should wrap content in org-mode-document div`() {
        val content = "Content"

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("org-mode-document"))
    }

    @Test
    fun `should convert regular paragraph`() {
        val content = "This is a paragraph"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<p>"))
        assertTrue(doc.parsedContent.contains("paragraph"))
    }

    @Test
    fun `should handle empty lines as breaks`() {
        val content = """
            Line 1

            Line 2
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<br>"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should detect mismatched block delimiters`() {
        val content = """
            #+BEGIN_SRC
            code without end
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Mismatched block delimiters") })
    }

    @Test
    fun `should accept matched block delimiters`() {
        val content = """
            #+BEGIN_SRC
            code here
            #+END_SRC
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Mismatched block delimiters") })
    }

    @Test
    fun `should detect excessive heading level`() {
        val content = "******* Too deep heading"

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("exceeds maximum of 6") })
    }

    @Test
    fun `should accept heading level 6`() {
        val content = "****** Level 6 heading"

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("exceeds maximum") })
    }

    @Test
    fun `should validate multiple headings`() {
        val content = """
            *** Level 3
            ******* Too deep
            ** Level 2
        """.trimIndent()

        val errors = parser.validate(content)

        // Should detect only the level 7 heading
        assertTrue(errors.any { it.contains("exceeds maximum") })
        assertEquals(1, errors.size)
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
        val content = """
            * Unicode Heading 你好世界
            Some unicode content: Привет мир 🌍
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("你好世界"))
        assertTrue(doc.parsedContent.contains("Привет мир"))
    }

    @Test
    fun `should handle heading at end of document`() {
        val content = "* Final Heading"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Final Heading"))
    }

    @Test
    fun `should handle multiple blocks`() {
        val content = """
            #+BEGIN_SRC
            code1
            #+END_SRC

            #+BEGIN_QUOTE
            quote
            #+END_QUOTE
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("code1"))
        assertTrue(doc.parsedContent.contains("quote"))
    }

    @Test
    fun `should handle mixed formatting in one line`() {
        val content = "Text with **bold** and /italic/ and ~code~"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-bold"))
        assertTrue(doc.parsedContent.contains("org-italic"))
        assertTrue(doc.parsedContent.contains("org-code"))
    }

    @Test
    fun `should handle heading with inline formatting`() {
        val content = "* Heading with **bold** text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("org-heading"))
        assertTrue(doc.parsedContent.contains("org-bold"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should handle complete Org Mode document`() {
        val content = """
            * TODO Main Project

            This is an introduction paragraph with **bold** text.

            ** Subproject 1

            Some /italic/ content here.

            :CREATED: 2025-01-01
            :CATEGORY: Work

            *** Details

            More details with ~code~ and +strikethrough+.

            #+BEGIN_SRC python
            def hello():
                print("World")
            #+END_SRC

            ** DONE Completed Task

            This task is [[https://example.com][complete]].

            * Another Section

            Final paragraph.
        """.trimIndent()

        val doc = parser.parse(content)

        // Check metadata
        assertEquals("5", doc.metadata["headings"])
        assertEquals("2", doc.metadata["todos"])
        assertEquals("2", doc.metadata["properties"])
        assertEquals("3", doc.metadata["max_level"])

        // Check content elements
        assertTrue(doc.parsedContent.contains("Main Project"))
        assertTrue(doc.parsedContent.contains("org-heading-1"))
        assertTrue(doc.parsedContent.contains("Subproject 1"))
        assertTrue(doc.parsedContent.contains("org-heading-2"))
        assertTrue(doc.parsedContent.contains("org-todo-todo"))
        assertTrue(doc.parsedContent.contains("org-todo-done"))
        assertTrue(doc.parsedContent.contains("org-bold"))
        assertTrue(doc.parsedContent.contains("org-italic"))
        assertTrue(doc.parsedContent.contains("org-code"))
        assertTrue(doc.parsedContent.contains("org-strikethrough"))
        assertTrue(doc.parsedContent.contains("org-block"))
        assertTrue(doc.parsedContent.contains("org-link"))
        // Note: Properties without trailing ':' are not rendered in HTML
    }
}
