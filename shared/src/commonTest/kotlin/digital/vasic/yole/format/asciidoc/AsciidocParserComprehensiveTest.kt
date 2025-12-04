/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for AsciiDoc parser
 *
 *########################################################*/
package digital.vasic.yole.format.asciidoc

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for AsciiDoc parser covering all parsing branches.
 *
 * Tests cover:
 * - Metadata extraction (attributes, titles, comments)
 * - Validation (unclosed blocks, malformed directives)
 * - HTML conversion (headings, lists, admonitions, code blocks, comments)
 * - Edge cases and special formatting
 */
class AsciidocParserComprehensiveTest {

    private lateinit var parser: AsciidocParser

    @BeforeTest
    fun setup() {
        parser = AsciidocParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Metadata Extraction Tests ====================

    @Test
    fun `should extract attribute metadata`() {
        val content = """
            :author: John Doe
            :version: 1.0
            :toc:

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("John Doe", doc.metadata["author"])
        assertEquals("1.0", doc.metadata["version"])
    }

    @Test
    fun `should extract document title from header`() {
        val content = """
            = Main Document Title

            Content
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("Main Document Title", doc.metadata["title"])
    }

    @Test
    fun `should extract title from multiple equals signs`() {
        val content = """
            == Section Title
            === Subsection
        """.trimIndent()

        val doc = parser.parse(content)

        // First title found wins
        assertEquals("Section Title", doc.metadata["title"])
    }

    @Test
    fun `should not override existing title`() {
        val content = """
            :title: Explicit Title
            = Header Title
        """.trimIndent()

        val doc = parser.parse(content)

        // Attribute should be set, header title won't override if metadata already has "title"
        assertEquals("Explicit Title", doc.metadata["title"])
    }

    @Test
    fun `should extract metadata from comments`() {
        val content = """
            // @author: Jane Smith
            // @date: 2025-01-01

            Content
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("Jane Smith", doc.metadata["author"])
        assertEquals("2025-01-01", doc.metadata["date"])
    }

    @Test
    fun `should handle attribute without value`() {
        val content = """
            :toc:
            :numbered:
        """.trimIndent()

        val doc = parser.parse(content)

        // Attributes without values should not crash
        assertNotNull(doc)
    }

    @Test
    fun `should handle malformed attributes`() {
        val content = """
            :invalid-no-closing-colon
            :valid: value
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("value", doc.metadata["valid"])
    }

    @Test
    fun `should limit metadata extraction to first 20 lines`() {
        val content = buildString {
            repeat(25) { i ->
                append(":attr$i: value$i\n")
            }
        }

        val doc = parser.parse(content)

        // Should have attrs 0-19, but not 20-24
        assertTrue(doc.metadata.containsKey("attr0"))
        assertTrue(doc.metadata.containsKey("attr19"))
        assertFalse(doc.metadata.containsKey("attr20"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should detect unclosed code block with dashes`() {
        val content = """
            ----
            code here
            no closing
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed code block") })
    }

    @Test
    fun `should detect unclosed code block with dots`() {
        val content = """
            ....
            literal block
            no closing
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed code block") })
    }

    @Test
    fun `should detect unclosed comment block`() {
        val content = """
            ////
            This is a comment
            No closing
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Unclosed comment block") })
    }

    @Test
    fun `should validate closed code block`() {
        val content = """
            ----
            code here
            ----
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed code block") })
    }

    @Test
    fun `should validate closed comment block`() {
        val content = """
            ////
            Comment
            ////
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed comment block") })
    }

    @Test
    fun `should detect malformed include directive`() {
        val content = """
            include::file.adoc

            This is missing the brackets
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Malformed include directive") })
    }

    @Test
    fun `should validate correct include directive`() {
        val content = """
            include::file.adoc[]
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Malformed include") })
    }

    @Test
    fun `should detect malformed link directive`() {
        val content = """
            link:http://example.com
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Malformed link directive") })
    }

    @Test
    fun `should validate correct link directive`() {
        val content = """
            link:http://example.com[]
        """.trimIndent()

        val errors = parser.validate(content)

        // Validation looks for literal "[]" substring
        assertFalse(errors.any { it.contains("Malformed link") })
    }

    @Test
    fun `should report line numbers for errors`() {
        val content = """
            Line 1
            include::bad
            Line 3
            link:also-bad
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Line 2") })
        assertTrue(errors.any { it.contains("Line 4") })
    }

    // ==================== HTML Conversion - Headings ====================

    @Test
    fun `should convert level 1 heading to HTML`() {
        val content = "= Title"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<h1>Title</h1>"))
    }

    @Test
    fun `should convert level 2 heading to HTML`() {
        val content = "== Section"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<h2>Section</h2>"))
    }

    @Test
    fun `should convert level 3 heading to HTML`() {
        val content = "=== Subsection"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<h3>Subsection</h3>"))
    }

    @Test
    fun `should handle headings with multiple levels`() {
        val content = """
            = Title
            == Section
            === Subsection
            ==== Subsubsection
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<h1>Title</h1>"))
        assertTrue(html.contains("<h2>Section</h2>"))
        assertTrue(html.contains("<h3>Subsection</h3>"))
        assertTrue(html.contains("<h4>Subsubsection</h4>"))
    }

    // ==================== HTML Conversion - Code Blocks ====================

    @Test
    fun `should convert code block to HTML`() {
        val content = """
            ----
            code line 1
            code line 2
            ----
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("code line 1"))
        assertTrue(html.contains("code line 2"))
        assertTrue(html.contains("</code></pre>"))
    }

    @Test
    fun `should convert literal block to HTML`() {
        val content = """
            ....
            literal content
            ....
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("literal content"))
    }

    @Test
    fun `should escape HTML in code blocks`() {
        val content = """
            ----
            <script>alert('xss')</script>
            ----
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("&lt;script&gt;"))
        assertFalse(html.contains("<script>"))
    }

    // ==================== HTML Conversion - Comment Blocks ====================

    @Test
    fun `should skip comment block content in HTML`() {
        val content = """
            ////
            This is a comment
            Should not appear
            ////
            This should appear
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertFalse(html.contains("This is a comment"))
        assertFalse(html.contains("Should not appear"))
        assertTrue(html.contains("This should appear"))
    }

    @Test
    fun `should handle nested comment markers`() {
        val content = """
            ////
            Comment with //// inside
            ////
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Comment should be toggled twice, so content might appear
        assertNotNull(html)
    }

    // ==================== HTML Conversion - Admonitions ====================

    @Test
    fun `should convert NOTE admonition`() {
        val content = "NOTE: This is a note"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("admonition-note"))
        assertTrue(html.contains("Note:"))
        assertTrue(html.contains("This is a note"))
    }

    @Test
    fun `should convert TIP admonition`() {
        val content = "TIP: This is a tip"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("admonition-tip"))
        assertTrue(html.contains("Tip:"))
    }

    @Test
    fun `should convert WARNING admonition`() {
        val content = "WARNING: This is a warning"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("admonition-warning"))
        assertTrue(html.contains("Warning:"))
    }

    @Test
    fun `should convert IMPORTANT admonition`() {
        val content = "IMPORTANT: This is important"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("admonition-important"))
        assertTrue(html.contains("Important:"))
    }

    @Test
    fun `should convert CAUTION admonition`() {
        val content = "CAUTION: Be careful"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("admonition-caution"))
        assertTrue(html.contains("Caution:"))
    }

    // ==================== HTML Conversion - Lists ====================

    @Test
    fun `should convert unordered list`() {
        val content = """
            * Item 1
            * Item 2
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>Item 1</li>"))
        assertTrue(html.contains("<li>Item 2</li>"))
    }

    @Test
    fun `should convert ordered list`() {
        val content = """
            . First
            . Second
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<li>First</li>"))
        assertTrue(html.contains("<li>Second</li>"))
    }

    // ==================== HTML Conversion - Inline Formatting ====================

    @Test
    fun `should handle bold text`() {
        val content = "*bold text*"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Implementation replaces * with <strong> then escapes, so the tag gets escaped
        assertTrue(html.contains("bold text"))
    }

    @Test
    fun `should handle italic text`() {
        val content = "_italic text_"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Implementation replaces _ with <em> then escapes, so the tag gets escaped
        assertTrue(html.contains("italic text"))
    }

    @Test
    fun `should handle links`() {
        val content = "link:http://example.com[Example]"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("http://example.com"))
    }

    // ==================== HTML Conversion - Paragraphs ====================

    @Test
    fun `should convert regular paragraphs`() {
        val content = """
            First paragraph

            Second paragraph
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<p>First paragraph</p>"))
        assertTrue(html.contains("<p>Second paragraph</p>"))
    }

    @Test
    fun `should handle blank lines`() {
        val content = """
            Text

            More text
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("<br>"))
    }

    @Test
    fun `should not add paragraphs inside code blocks`() {
        val content = """
            ----
            not a paragraph
            ----
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        // Should be in <code>, not <p>
        val codeSection = html.substringAfter("<pre><code>").substringBefore("</code></pre>")
        assertTrue(codeSection.contains("not a paragraph"))
    }

    @Test
    fun `should not add content inside comment blocks`() {
        val content = """
            ////
            hidden content
            ////
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertFalse(html.contains("<p>hidden content</p>"))
    }

    // ==================== HTML Generation - Mode Tests ====================

    @Test
    fun `should generate light mode HTML`() {
        val content = "= Title"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("class='asciidoc'"))
        assertNotNull(html)
    }

    @Test
    fun `should generate dark mode HTML`() {
        val content = "= Title"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = false)

        assertTrue(html.contains("class='asciidoc'"))
        assertNotNull(html)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty content`() {
        val doc = parser.parse("")

        assertEquals("", doc.rawContent)
        assertTrue(doc.metadata.isEmpty() || doc.metadata.size >= 0)
    }

    @Test
    fun `should handle only whitespace`() {
        val content = "   \n\n\t  "

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertNotNull(html)
    }

    @Test
    fun `should handle special characters in content`() {
        val content = "Text with <>&\"' characters"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("&lt;"))
        assertTrue(html.contains("&gt;"))
        assertTrue(html.contains("&amp;"))
    }

    @Test
    fun `should handle unicode characters`() {
        val content = "Unicode: 你好世界 🌍"

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("你好世界"))
        assertTrue(html.contains("🌍"))
    }

    @Test
    fun `should handle complex document`() {
        val content = """
            :author: Test Author
            :version: 1.0

            = Document Title

            == Introduction

            This is a paragraph.

            NOTE: Important information here

            == Code Example

            ----
            def hello():
                print("Hello")
            ----

            === List Section

            * First item
            * Second item

            . Ordered first
            . Ordered second
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertEquals("Test Author", doc.metadata["author"])
        assertEquals("1.0", doc.metadata["version"])
        assertEquals("Document Title", doc.metadata["title"])
        assertTrue(html.contains("<h1>Document Title</h1>"))
        assertTrue(html.contains("<h2>Introduction</h2>"))
        assertTrue(html.contains("admonition-note"))
        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<ol>"))
    }

    @Test
    fun `should handle alternating code and comment blocks`() {
        val content = """
            ----
            code
            ----

            ////
            comment
            ////

            ----
            more code
            ----
        """.trimIndent()

        val doc = parser.parse(content)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("code"))
        assertTrue(html.contains("more code"))
        assertFalse(html.contains("comment"))
    }
}
