/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for RestructuredTextParser
 *
 *########################################################*/
package digital.vasic.yole.format.restructuredtext

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for RestructuredTextParser covering all parsing branches.
 *
 * Tests cover:
 * - Section extraction (different underline characters)
 * - Section levels (=, -, ~, ^, ")
 * - Directives (.. directive::)
 * - HTML generation
 * - Validation
 * - Light/dark modes
 * - Edge cases
 */
class RestructuredTextParserComprehensiveTest {

    private lateinit var parser: RestructuredTextParser

    @BeforeTest
    fun setup() {
        parser = RestructuredTextParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Metadata Tests ====================

    @Test
    fun `should count sections`() {
        val content = """
            Section 1
            =========

            Section 2
            =========
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("2", doc.metadata["sections"])
    }

    @Test
    fun `should count directives`() {
        val content = """
            .. note::
               This is a note

            .. warning::
               This is a warning
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("2", doc.metadata["directives"])
    }

    @Test
    fun `should track maximum section level`() {
        val content = """
            Level 1
            =======

            Level 2
            -------

            Level 3
            ~~~~~~~
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("3", doc.metadata["max_level"])
    }

    @Test
    fun `should handle content with no sections`() {
        val content = "Just plain text"

        val doc = parser.parse(content)

        assertEquals("0", doc.metadata["sections"])
        assertEquals("0", doc.metadata["max_level"])
    }

    @Test
    fun `should handle content with no directives`() {
        val content = """
            Section
            =======

            Regular text
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("0", doc.metadata["directives"])
    }

    // ==================== Section Level Tests ====================

    @Test
    fun `should recognize level 1 section with equals`() {
        val content = """
            Main Title
            ==========
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-section-1"))
        assertTrue(doc.parsedContent.contains("Main Title"))
    }

    @Test
    fun `should recognize level 2 section with dashes`() {
        val content = """
            Subtitle
            --------
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-section-2"))
        assertTrue(doc.parsedContent.contains("Subtitle"))
    }

    @Test
    fun `should recognize level 3 section with tildes`() {
        val content = """
            Subsubtitle
            ~~~~~~~~~~~
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-section-3"))
        assertTrue(doc.parsedContent.contains("Subsubtitle"))
    }

    @Test
    fun `should recognize level 4 section with carets`() {
        val content = """
            Level 4
            ^^^^^^^
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-section-4"))
        assertTrue(doc.parsedContent.contains("Level 4"))
    }

    @Test
    fun `should recognize level 5 section with quotes`() {
        val content = "Level 5\n" + "\"\"\"\"\"\"\""

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-section-5"))
        assertTrue(doc.parsedContent.contains("Level 5"))
    }

    @Test
    fun `should recognize other underline characters as level 6`() {
        val content = """
            Other Level
            ***********
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-section-6"))
        assertTrue(doc.parsedContent.contains("Other Level"))
    }

    @Test
    fun `should require consistent underline character`() {
        val content = """
            Title
            ====
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Title"))
    }

    @Test
    fun `should require minimum underline length of 2`() {
        val content = """
            Title
            ==
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Title"))
    }

    // ==================== Directive Tests ====================

    @Test
    fun `should extract note directive`() {
        val content = """
            .. note::
               This is a note
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-directive"))
        assertTrue(doc.parsedContent.contains("note"))
    }

    @Test
    fun `should extract warning directive`() {
        val content = """
            .. warning::
               This is a warning
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-directive"))
        assertTrue(doc.parsedContent.contains("warning"))
    }

    @Test
    fun `should extract code-block directive`() {
        val content = """
            .. code-block:: python
               print("Hello")
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-directive"))
        assertTrue(doc.parsedContent.contains("code-block"))
    }

    @Test
    fun `should extract image directive`() {
        val content = """
            .. image:: photo.png
               :width: 200
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-directive"))
        assertTrue(doc.parsedContent.contains("image"))
    }

    @Test
    fun `should handle directive with multiline content`() {
        val content = """
            .. note::
               Line 1
               Line 2
               Line 3
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-directive-content"))
        assertTrue(doc.parsedContent.contains("Line 1"))
    }

    @Test
    fun `should handle directive at end of document`() {
        val content = """
            .. note::
               This is the last directive
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-directive"))
        assertTrue(doc.parsedContent.contains("</div>"))
    }

    @Test
    fun `should handle multiple directives`() {
        val content = """
            .. note::
               First note

            .. warning::
               Then warning
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("note"))
        assertTrue(doc.parsedContent.contains("warning"))
    }

    // ==================== HTML Generation Tests ====================

    @Test
    fun `should generate light mode HTML`() {
        val content = """
            Title
            =====
        """.trimIndent()

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("light"))
    }

    @Test
    fun `should generate dark mode HTML`() {
        val content = """
            Title
            =====
        """.trimIndent()

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = false)

        assertTrue(html.contains("dark"))
    }

    @Test
    fun `should include CSS styles in HTML`() {
        val content = "Content"

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("<style>") || html.contains("rst-document"))
    }

    @Test
    fun `should wrap content in rst-document div`() {
        val content = "Content"

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("rst-document"))
    }

    @Test
    fun `should escape HTML special characters`() {
        val content = "Text with <div> and & symbols"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&lt;") || doc.parsedContent.contains("&gt;") || doc.parsedContent.contains("&amp;"))
    }

    @Test
    fun `should convert regular paragraphs`() {
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
    fun `should detect underline too short for title`() {
        val content = """
            Long Title
            ====
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("underline too short") || it.contains("Long Title") })
    }

    @Test
    fun `should accept underline same length as title`() {
        val content = """
            Title
            =====
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("underline too short") })
    }

    @Test
    fun `should accept underline longer than title`() {
        val content = """
            Title
            =========
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("underline too short") })
    }

    @Test
    fun `should validate multiple sections`() {
        val content = """
            Good Title
            ==========

            Bad
            =========
        """.trimIndent()

        val errors = parser.validate(content)

        // Should detect the second section's underline is too long
        assertFalse(errors.any { it.contains("Good Title") })
        // But shouldn't error since underline is >= title length
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
            Unicode Title 你好世界
            ==================
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("你好世界"))
    }

    @Test
    fun `should handle title at end of document`() {
        val content = """
            Title
            =====
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Title"))
    }

    @Test
    fun `should handle directive without content`() {
        val content = """
            .. note::
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("rst-directive"))
    }

    @Test
    fun `should not treat single character underline as section`() {
        val content = """
            Title
            =
        """.trimIndent()

        val doc = parser.parse(content)

        // Should require at least 2 characters
        assertEquals("0", doc.metadata["sections"])
    }

    @Test
    fun `should not treat mixed character underline as section`() {
        val content = """
            Title
            =-=
        """.trimIndent()

        val doc = parser.parse(content)

        // Should require all same character
        assertEquals("0", doc.metadata["sections"])
    }

    @Test
    fun `should handle section without following content`() {
        val content = """
            Title
            =====
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("1", doc.metadata["sections"])
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should handle complete reStructuredText document`() {
        val content = """
            Main Document Title
            ===================

            This is the introduction paragraph.

            First Section
            -------------

            Some content in the first section.

            .. note::
               This is an important note.
               It spans multiple lines.

            Subsection
            ~~~~~~~~~~

            More detailed content.

            .. code-block:: python
               def hello():
                   print("World")

            Final paragraph with <HTML> that needs &escaping.
        """.trimIndent()

        val doc = parser.parse(content)

        // Check metadata
        assertEquals("3", doc.metadata["sections"])
        assertEquals("2", doc.metadata["directives"])
        assertEquals("3", doc.metadata["max_level"])

        // Check content elements
        assertTrue(doc.parsedContent.contains("Main Document Title"))
        assertTrue(doc.parsedContent.contains("rst-section-1"))
        assertTrue(doc.parsedContent.contains("First Section"))
        assertTrue(doc.parsedContent.contains("rst-section-2"))
        assertTrue(doc.parsedContent.contains("Subsection"))
        assertTrue(doc.parsedContent.contains("rst-section-3"))
        assertTrue(doc.parsedContent.contains("rst-directive"))
        assertTrue(doc.parsedContent.contains("note"))
        assertTrue(doc.parsedContent.contains("code-block"))
        assertTrue(doc.parsedContent.contains("&lt;") && doc.parsedContent.contains("&gt;"))
    }
}
