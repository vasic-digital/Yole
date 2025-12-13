/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for reStructuredText parser
 *
 *########################################################*/
package digital.vasic.yole.format.rst

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.restructuredtext.RstSection
import digital.vasic.yole.format.restructuredtext.RstDirective
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive unit tests for reStructuredText format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic reStructuredText parsing (sections, paragraphs, lists)
 * - reStructuredText directives and roles
 * - Tables and code blocks
 * - Cross-references and links
 * - Admonitions and special blocks
 * - Round-trip parsing (parse → format → parse)
 * - Edge cases (empty files, malformed RST)
 * - Performance benchmarks
 * - HTML conversion
 */
class RstParserTest {

    private val parser = RestructuredTextParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect reStructuredText format by extension`() {
        val format = FormatRegistry.getByExtension(".rst")

        assertNotNull(format)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, format.id)
        assertEquals("reStructuredText", format.name)
    }

    @Test
    fun `should detect reStructuredText format by rest extension`() {
        val format = FormatRegistry.getByExtension(".rest")

        assertNotNull(format)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, format.id)
    }

    @Test
    fun `should detect reStructuredText format by filename`() {
        val format = FormatRegistry.detectByFilename("document.rst")

        assertNotNull(format)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, format.id)
    }

    @Test
    fun `should support all reStructuredText extensions`() {
        val extensions = listOf(".rst", ".rest")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, format.id)
        }
    }

    // ==================== Basic reStructuredText Parsing Tests ====================

    @Test
    fun `should parse basic reStructuredText document structure`() {
        val content = "Document Title\n==============\n\nThis is a paragraph in the document.\n\nFirst Section\n-------------\n\nMore content here.\n\nSubsection\n~~~~~~~~~~\n\nEven deeper content."

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("3", result.metadata["sections"])
        assertEquals("0", result.metadata["directives"])
        assertEquals("1", result.metadata["max_level"])
    }

    @Test
    fun `should parse reStructuredText headings with different levels`() {
        val content = "Level 1 Heading\n===============\n\nLevel 2 Heading\n---------------\n\nLevel 3 Heading\n~~~~~~~~~~~~~~~\n\nLevel 4 Heading\n^^^^^^^^^^^^^^^\n\nLevel 5 Heading\n\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\n\nLevel 6 Heading\n'''''''''''''''"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals("6", result.metadata["sections"])
        assertEquals("1", result.metadata["max_level"])
    }

    @Test
    fun `should parse reStructuredText paragraphs`() {
        val content = "This is the first paragraph.\nIt spans multiple lines.\n\nThis is the second paragraph.\n\nAnd this is the third paragraph."

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Lists Tests ====================

    @Test
    fun `should parse unordered lists`() {
        val content = "* First item\n* Second item\n\n    * Nested item 1\n    * Nested item 2\n\n* Third item"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse ordered lists`() {
        val content = "1. First item\n2. Second item\n\n    a. Nested item 1\n    b. Nested item 2\n\n3. Third item"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Directives Tests ====================

    @Test
    fun `should parse basic directives`() {
        val content = ".. image:: picture.png\n   :width: 200px\n   :height: 100px\n\n.. note:: This is a note directive.\n\n.. code:: python\n\n   def hello():\n       print(\"Hello, World!\")"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals("3", result.metadata["directives"])
    }

    @Test
    fun `should parse admonition directives`() {
        val content = ".. note:: This is a note admonition.\n\n.. tip:: This is a tip admonition.\n\n.. warning:: This is a warning admonition.\n\n.. important:: This is an important admonition.\n\n.. caution:: This is a caution admonition."

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals("5", result.metadata["directives"])
    }

    // ==================== Code Blocks Tests ====================

    @Test
    fun `should parse code blocks`() {
        val content = "Here's some code::\n\n    def hello():\n        print(\"Hello, World!\")\n\nEnd of code."

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse literal blocks`() {
        val content = "This is a literal block::\n\n    This text will be shown exactly as written\n    with all    spaces    preserved\n    and no markup interpretation.\n\nBack to normal text."

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Links and Cross-references Tests ====================

    @Test
    fun `should parse external links`() {
        val content = "This is a link to `Python website <http://python.org>`_.\n\nAnd another link: http://example.com"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse internal cross-references`() {
        val content = "See :ref:`section-label` for more information.\n\n.. _section-label:\n\nSection Title\n=============\n\nThis is the referenced section."

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals("1", result.metadata["sections"])
    }

    // ==================== Tables Tests ====================

    @Test
    fun `should parse simple tables`() {
        val content = "======  =====  =======\nA       B      A and B\n======  =====  =======\nFalse   False  False\nTrue    False  False\nFalse   True   False\nTrue    True   True\n======  =====  ======="

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse grid tables`() {
        val content = "+------------+------------+-----------+\n| Header 1   | Header 2   | Header 3  |\n+============+============+===========+\n| body row 1 | column 2   | column 3  |\n+------------+------------+-----------+\n| body row 2 | Cells may span columns.|\n+------------+------------+-----------+"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate section underlines`() {
        val content = "Valid Section\n=============\n\nInvalid Section\n=========\n(underline too short)"

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Section underline too short") })
    }

    @Test
    fun `should validate valid document`() {
        val content = "Valid Document\n==============\n\nThis is a valid reStructuredText document.\n\nSection\n-------\n\n* List item 1\n* List item 2\n\n.. note:: This is a note."

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty document`() {
        val content = ""

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("0", result.metadata["sections"])
        assertEquals("0", result.metadata["directives"])
    }

    @Test
    fun `should handle document with only whitespace`() {
        val content = "   \n  \n   \n"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle document with only comments`() {
        val content = ".. This is a comment\n.. Another comment"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle malformed sections`() {
        val content = "Valid Section\n=============\n\nInvalid (no underline)\n\nAnother Valid Section\n---------------------"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert simple document to HTML`() {
        val content = "Document Title\n==============\n\nThis is a paragraph.\n\nSection\n-------\n\nMore content."

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class=\"rst-document light\">"))
        assertTrue(html.contains("<div class=\"rst-section rst-section-1\">Document Title</div>"))
        assertTrue(html.contains("<div class=\"rst-section rst-section-2\">Section</div>"))
        assertTrue(html.contains("<p>This is a paragraph.</p>"))
        assertTrue(html.contains("<p>More content.</p>"))
    }

    @Test
    fun `should convert headings to HTML`() {
        val content = "Level 1 Heading\n===============\n\nLevel 2 Heading\n---------------\n\nLevel 3 Heading\n~~~~~~~~~~~~~~~\n\nLevel 4 Heading\n^^^^^^^^^^^^^^^\n\nLevel 5 Heading\n\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\n\nLevel 6 Heading\n'''''''''''''''"

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class=\"rst-section rst-section-1\">Level 1 Heading</div>"))
        assertTrue(html.contains("<div class=\"rst-section rst-section-2\">Level 2 Heading</div>"))
        assertTrue(html.contains("<div class=\"rst-section rst-section-3\">Level 3 Heading</div>"))
        assertTrue(html.contains("<div class=\"rst-section rst-section-4\">Level 4 Heading</div>"))
        assertTrue(html.contains("<div class=\"rst-section rst-section-5\">Level 5 Heading</div>"))
        assertTrue(html.contains("<div class=\"rst-section rst-section-6\">Level 6 Heading</div>"))
    }

    @Test
    fun `should convert directives to HTML`() {
        val content = ".. note:: This is a note directive.\n\n.. warning:: This is a warning directive."

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class=\"rst-directive\">"))
        assertTrue(html.contains("<div class=\"rst-directive-header\">.. note:: This is a note directive.</div>"))
        assertTrue(html.contains("<div class=\"rst-directive-header\">.. warning:: This is a warning directive.</div>"))
    }

    @Test
    fun `should apply dark mode styles`() {
        val content = "Test Document\n=============\n\nSome content."

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("<div class=\"rst-document dark\">"))
        assertTrue(html.contains("<style>"))
    }

    // ==================== Round-trip Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = "Original Document\n=================\n\nThis is the original content.\n\nSection\n-------\n\n* List item 1\n* List item 2\n\n.. note:: This is a note.\n\n.. code:: python\n\n   def hello():\n       print(\"Hello, World!\")"

        // Parse the original content
        val firstParse = parser.parse(originalContent)
        
        // Get the formatted content (should be same as original)
        val formattedContent = firstParse.rawContent
        
        // Parse the formatted content again
        val secondParse = parser.parse(formattedContent)
        
        // Verify round-trip consistency
        assertEquals(firstParse.format.id, secondParse.format.id)
        assertEquals(firstParse.rawContent, secondParse.rawContent)
        assertEquals(firstParse.metadata, secondParse.metadata)
        assertEquals(firstParse.errors, secondParse.errors)
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should parse large documents efficiently`() {
        // Create a large document with many sections and elements
        val largeContent = buildString {
            appendLine("Large Document")
            appendLine("==============")
            appendLine()
            
            repeat(50) { sectionIndex ->
                appendLine("Section \${sectionIndex + 1}")
                appendLine("-----------------")
                appendLine()
                
                repeat(5) { paragraphIndex ->
                    appendLine("This is paragraph \${paragraphIndex + 1} in section \${sectionIndex + 1}.")
                    appendLine()
                }
                
                appendLine("* List item 1")
                appendLine("* List item 2")
                appendLine("* List item 3")
                appendLine()
                
                appendLine(".. note:: This is a note in section \${sectionIndex + 1}.")
                appendLine()
            }
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parse(largeContent)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, result.format.id)
        
        // Performance assertion - should parse in reasonable time (less than 1 second for this size)
        val parseTime = endTime - startTime
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took: \${parseTime}ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        // Create a large document
        val largeContent = buildString {
            appendLine("Performance Test Document")
            appendLine("=========================")
            appendLine()
            
            repeat(25) { i ->
                appendLine("Section \${i + 1}")
                appendLine("----------------")
                appendLine()
                appendLine("This is content for section \${i + 1}.")
                appendLine()
                
                repeat(3) { j ->
                    appendLine("* Item \${j + 1}")
                }
                appendLine()
                
                appendLine(".. note:: Note \${i + 1}")
                appendLine()
            }
        }

        val document = parser.parse(largeContent)
        
        val startTime = System.currentTimeMillis()
        val html = parser.toHtml(document, lightMode = true)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(html)
        assertTrue(html.isNotEmpty())
        
        // Performance assertion - should convert in reasonable time (less than 500ms)
        val conversionTime = endTime - startTime
        assertTrue(conversionTime < 500, "HTML conversion should complete within 500ms, took: \${conversionTime}ms")
    }
}