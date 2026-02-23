/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for AsciiDoc parser
 *
 *########################################################*/
package digital.vasic.yole.format.asciidoc

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * Comprehensive unit tests for AsciiDoc format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic AsciiDoc parsing (sections, paragraphs, lists)
 * - AsciiDoc headers and metadata
 * - Tables and code blocks
 * - Cross-references and links
 * - Admonitions and special blocks
 * - Round-trip parsing (parse → format → parse)
 * - Edge cases (empty files, malformed AsciiDoc)
 * - Performance benchmarks
 * - HTML conversion
 */
class AsciidocParserTest {

    private val parser = AsciidocParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect AsciiDoc format by extension`() {
        val format = FormatRegistry.getByExtension(".adoc")

        assertNotNull(format)
        assertEquals(TextFormat.ID_ASCIIDOC, format?.id)
        assertEquals("AsciiDoc", format?.name)
    }

    @Test
    fun `should detect AsciiDoc format by asciidoc extension`() {
        val format = FormatRegistry.getByExtension(".asciidoc")

        assertNotNull(format)
        assertEquals(TextFormat.ID_ASCIIDOC, format?.id)
    }

    @Test
    fun `should detect AsciiDoc format by filename`() {
        val format = FormatRegistry.detectByFilename("document.adoc")

        assertNotNull(format)
        assertEquals(TextFormat.ID_ASCIIDOC, format?.id)
    }

    @Test
    fun `should support all AsciiDoc extensions`() {
        val extensions = listOf(".adoc", ".asciidoc")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_ASCIIDOC, format?.id)
        }
    }

    // ==================== Basic AsciiDoc Parsing Tests ====================

    @Test
    fun `should parse basic AsciiDoc document structure`() {
        val content = """
            = Document Title
            :author: John Doe
            :date: 2025-01-01
            
            This is a paragraph in the document.
            
            == First Section
            
            More content here.
            
            === Subsection
            
            Even deeper content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("John Doe", result.metadata["author"])
        assertEquals("2025-01-01", result.metadata["date"])
        assertEquals("Document Title", result.metadata["title"])
    }

    @Test
    fun `should parse AsciiDoc headings with different levels`() {
        val content = """
            = Level 1 Heading
            == Level 2 Heading
            === Level 3 Heading
            ==== Level 4 Heading
            ===== Level 5 Heading
            ====== Level 6 Heading
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals("Level 1 Heading", result.metadata["title"])
    }

    @Test
    fun `should parse AsciiDoc paragraphs`() {
        val content = """
            This is the first paragraph.
            It spans multiple lines.
            
            This is the second paragraph.
            
            And this is the third paragraph.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Lists Tests ====================

    @Test
    fun `should parse unordered lists`() {
        val content = """
            * First item
            * Second item
            ** Nested item 1
            ** Nested item 2
            * Third item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse ordered lists`() {
        val content = """
            . First item
            . Second item
            .. Nested item 1
            .. Nested item 2
            . Third item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Code Blocks Tests ====================

    @Test
    fun `should parse code blocks with dashes`() {
        val content = """
            Here's some code:
            
            ----
            fun main() {
                println("Hello, World!")
            }
            ----
            
            End of code.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse code blocks with dots`() {
        val content = """
            Here's some code:
            
            ....
            def hello():
                print("Hello, World!")
            ....
            
            End of code.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Admonitions Tests ====================

    @Test
    fun `should parse admonitions`() {
        val content = """
            NOTE: This is a note admonition.
            
            TIP: This is a tip admonition.
            
            WARNING: This is a warning admonition.
            
            IMPORTANT: This is an important admonition.
            
            CAUTION: This is a caution admonition.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Links and Cross-references Tests ====================

    @Test
    fun `should parse basic links`() {
        val content = """
            This is a link:link:http://example.com[Example Website].
            
            And another link:https://github.com[GitHub].
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse cross-references`() {
        val content = """
            See <<section-id,Section Title>> for more information.
            
            [[section-id]]
            == Section Title
            
            This is the referenced section.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Text Formatting Tests ====================

    @Test
    fun `should parse bold text`() {
        val content = """
            This is *bold* text.
            And *this is also bold*.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse italic text`() {
        val content = """
            This is _italic_ text.
            And _this is also italic_.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Metadata Tests ====================

    @Test
    fun `should parse document attributes`() {
        val content = """
            :author: Jane Smith
            :email: jane@example.com
            :version: 1.0
            :date: 2025-01-15
            :description: A test document
            :keywords: test, asciidoc, parsing
            
            = Document with Attributes
            
            This document has many attributes.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals("Jane Smith", result.metadata["author"])
        assertEquals("jane@example.com", result.metadata["email"])
        assertEquals("1.0", result.metadata["version"])
        assertEquals("2025-01-15", result.metadata["date"])
        assertEquals("A test document", result.metadata["description"])
        assertEquals("test, asciidoc, parsing", result.metadata["keywords"])
    }

    @Test
    fun `should parse comment metadata`() {
        val content = """
            // @custom-meta: custom value
            // @another-meta: another value
            
            = Document Title
            
            Document content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals("custom value", result.metadata["custom-meta"])
        assertEquals("another value", result.metadata["another-meta"])
    }

    // ==================== Comment Blocks Tests ====================

    @Test
    fun `should parse comment blocks`() {
        val content = """
            This is visible text.
            
            ////
            This is a comment block.
            It should not be visible in the output.
            ////
            
            This is more visible text.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate unclosed code blocks`() {
        val content = """
            Here's some code:
            
            ----
            fun main() {
                println("Hello, World!")
            }
            // Missing closing ----
            
            More text.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed code block") })
    }

    @Test
    fun `should validate unclosed comment blocks`() {
        val content = """
            This is visible text.
            
            ////
            This is a comment block.
            // Missing closing ////
            
            This should not be visible.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed comment block") })
    }

    @Test
    fun `should validate malformed includes`() {
        val content = """
            Here's some content.
            
            include::some-file.adoc
            // Missing []
            
            More content.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Malformed include directive") })
    }

    @Test
    fun `should validate malformed links`() {
        val content = """
            Here's some content.
            
            link:http://example.com
            // Missing []
            
            More content.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Malformed link directive") })
    }

    @Test
    fun `should validate valid document`() {
        val content = """
            = Valid Document
            
            This is a valid AsciiDoc document.
            
            == Section
            
            * List item 1
            * List item 2
            
            ----
            Code block
            ----
            
            NOTE: This is a note.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty document`() {
        val content = ""

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
        assertTrue(result.metadata.isEmpty())
    }

    @Test
    fun `should handle document with only whitespace`() {
        val content = "   \n  \n   \n"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle document with only comments`() {
        val content = """
            // This is a comment
            // Another comment
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle malformed headings`() {
        val content = """
            =Valid heading (no space)
            = Invalid heading (extra space)
            ====Too many equals (no space)
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert simple document to HTML`() {
        val content = """
            = Document Title
            
            This is a paragraph.
            
            == Section
            
            More content.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='asciidoc'>"))
        assertTrue(html.contains("<h1>Document Title</h1>"))
        assertTrue(html.contains("<h2>Section</h2>"))
        assertTrue(html.contains("<p>This is a paragraph.</p>"))
        assertTrue(html.contains("<p>More content.</p>"))
    }

    @Test
    fun `should convert headings to HTML`() {
        val content = """
            = Level 1
            == Level 2
            === Level 3
            ==== Level 4
            ===== Level 5
            ====== Level 6
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<h1>Level 1</h1>"))
        assertTrue(html.contains("<h2>Level 2</h2>"))
        assertTrue(html.contains("<h3>Level 3</h3>"))
        assertTrue(html.contains("<h4>Level 4</h4>"))
        assertTrue(html.contains("<h5>Level 5</h5>"))
        assertTrue(html.contains("<h6>Level 6</h6>"))
    }

    @Test
    fun `should convert lists to HTML`() {
        val content = """
            * Item 1
            * Item 2
            ** Nested 1
            ** Nested 2
            * Item 3
            
            . First
            . Second
            .. Nested A
            .. Nested B
            . Third
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<ul><li>Item 1</li></ul>"))
        assertTrue(html.contains("<ul><li>Item 2</li></ul>"))
        assertTrue(html.contains("<ul><li>Item 3</li></ul>"))
        assertTrue(html.contains("<ol><li>First</li></ol>"))
        assertTrue(html.contains("<ol><li>Second</li></ol>"))
        assertTrue(html.contains("<ol><li>Third</li></ol>"))
    }

    @Test
    fun `should convert code blocks to HTML`() {
        val content = """
            Here's some code:
            
            ----
            fun main() {
                println("Hello, World!")
            }
            ----
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<pre><code>"))
        assertTrue(html.contains("fun main() {"))
        assertTrue(html.contains("println(&quot;Hello, World!&quot;)"))
        assertTrue(html.contains("</code></pre>"))
    }

    @Test
    fun `should convert admonitions to HTML`() {
        val content = """
            NOTE: This is a note.
            TIP: This is a tip.
            WARNING: This is a warning.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='admonition admonition-note'>"))
        assertTrue(html.contains("<strong>Note:</strong>"))
        assertTrue(html.contains("<div class='admonition admonition-tip'>"))
        assertTrue(html.contains("<strong>Tip:</strong>"))
        assertTrue(html.contains("<div class='admonition admonition-warning'>"))
        assertTrue(html.contains("<strong>Warning:</strong>"))
    }

    @Test
    fun `should apply dark mode styles`() {
        val content = """
            = Test Document
            
            Some content.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("<div class='asciidoc'>"))
        // Should contain dark mode styles
        assertTrue(html.contains("<style>"))
    }

    // ==================== Round-trip Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = """
            = Original Document
            :author: Test Author
            :version: 1.0
            
            This is the original content.
            
            == Section
            
            * List item 1
            * List item 2
            
            ----
            Code block content
            ----
            
            NOTE: This is a note.
        """.trimIndent()

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
            appendLine("= Large Document")
            appendLine(":author: Performance Test")
            appendLine(":version: 1.0")
            appendLine()
            
            repeat(100) { sectionIndex ->
                appendLine("== Section ${sectionIndex + 1}")
                appendLine()
                
                repeat(10) { paragraphIndex ->
                    appendLine("This is paragraph ${paragraphIndex + 1} in section ${sectionIndex + 1}.")
                    appendLine()
                }
                
                appendLine("* List item 1")
                appendLine("* List item 2")
                appendLine("* List item 3")
                appendLine()
                
                appendLine("----")
                appendLine("Code block content")
                appendLine("----")
                appendLine()
                
                appendLine("NOTE: This is a note in section ${sectionIndex + 1}.")
                appendLine()
            }
        }

        val startTime = Clock.System.now().toEpochMilliseconds()
        val result = parser.parse(largeContent)
        val endTime = Clock.System.now().toEpochMilliseconds()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_ASCIIDOC, result.format.id)
        
        // Performance assertion - should parse in reasonable time (less than 1 second for this size)
        val parseTime = endTime - startTime
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took: ${parseTime}ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        // Create a large document
        val largeContent = buildString {
            appendLine("= Performance Test Document")
            appendLine()
            
            repeat(50) { i ->
                appendLine("== Section ${i + 1}")
                appendLine()
                appendLine("This is content for section ${i + 1}.")
                appendLine()
                
                repeat(5) { j ->
                    appendLine("* Item ${j + 1}")
                }
                appendLine()
                
                appendLine("----")
                appendLine("Code block ${i + 1}")
                appendLine("----")
                appendLine()
            }
        }

        val document = parser.parse(largeContent)
        
        val startTime = Clock.System.now().toEpochMilliseconds()
        val html = parser.toHtml(document, lightMode = true)
        val endTime = Clock.System.now().toEpochMilliseconds()
        
        assertNotNull(html)
        assertTrue(html.isNotEmpty())
        
        // Performance assertion - should convert in reasonable time (less than 500ms)
        val conversionTime = endTime - startTime
        assertTrue(conversionTime < 500, "HTML conversion should complete within 500ms, took: ${conversionTime}ms")
    }
}