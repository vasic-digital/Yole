/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Textile parser
 *
 *########################################################*/
package digital.vasic.yole.format.textile

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * Comprehensive unit tests for Textile format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic Textile parsing (paragraphs, headings, lists)
 * - Textile formatting (bold, italic, underline, strikethrough)
 * - Links and images
 * - Tables and code blocks
 * - Block quotes and citations
 * - Round-trip parsing (parse → format → parse)
 * - Edge cases (empty files, malformed Textile)
 * - Performance benchmarks
 * - HTML conversion
 */
class TextileParserTest {

    private val parser = TextileParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Textile format by extension`() {
        val format = FormatRegistry.getByExtension(".textile")

        assertNotNull(format)
        assertEquals(TextFormat.ID_TEXTILE, format.id)
        assertEquals("Textile", format.name)
    }

    @Test
    fun `should detect Textile format by txt extension`() {
        val format = FormatRegistry.getByExtension(".txt")

        assertNotNull(format)
        // .txt can be multiple formats, Textile should be one of the options
        val textileFormat = FormatRegistry.formats.first { it.id == TextFormat.ID_TEXTILE }
        assertTrue(textileFormat.extensions.contains(".txt"))
    }

    @Test
    fun `should detect Textile format by filename`() {
        val format = FormatRegistry.detectByFilename("document.textile")

        assertNotNull(format)
        assertEquals(TextFormat.ID_TEXTILE, format.id)
    }

    @Test
    fun `should support all Textile extensions`() {
        val textileFormat = FormatRegistry.formats.first { it.id == TextFormat.ID_TEXTILE }
        val expectedExtensions = listOf(".textile", ".txt")

        expectedExtensions.forEach { ext ->
            assertTrue(textileFormat.extensions.contains(ext), "Textile should support $ext extension")
        }
    }

    // ==================== Basic Textile Parsing Tests ====================

    @Test
    fun `should parse basic Textile document structure`() {
        val content = """
            h1. Main Heading
            
            This is a paragraph in the document.
            
            h2. Subsection
            
            More content here.
            
            h3. Deeper Section
            
            Even deeper content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("", result.metadata["extension"])
        assertEquals("11", result.metadata["lines"])
    }

    @Test
    fun `should parse Textile headings with different levels`() {
        val content = """
            h1. Level 1 Heading
            h2. Level 2 Heading
            h3. Level 3 Heading
            h4. Level 4 Heading
            h5. Level 5 Heading
            h6. Level 6 Heading
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals("6", result.metadata["lines"])
    }

    @Test
    fun `should parse Textile paragraphs`() {
        val content = """
            This is the first paragraph.
            It spans multiple lines.
            
            This is the second paragraph.
            
            And this is the third paragraph.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Lists Tests ====================

    @Test
    fun `should parse unordered lists`() {
        val content = """
            * First item
            * Second item
            * Third item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse ordered lists`() {
        val content = """
            # First item
            # Second item
            # Third item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse mixed lists`() {
        val content = """
            * Unordered item 1
            * Unordered item 2
            # Ordered item 1
            # Ordered item 2
            * Back to unordered
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Code Blocks Tests ====================

    @Test
    fun `should parse pre-formatted blocks`() {
        val content = """
            Here's some code:
            
            pre. fun main() {
                println("Hello, World!")
            }
            
            End of code.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse inline code`() {
        val content = """
            This is inline @code@ in a paragraph.
            And @another code snippet@ here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Block Quotes Tests ====================

    @Test
    fun `should parse block quotes`() {
        val content = """
            This is normal text.
            
            bq. This is a block quote.
            It can span multiple lines.
            
            Back to normal text.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Links and Images Tests ====================

    @Test
    fun `should parse basic links`() {
        val content = """
            This is a link "Example Website":http://example.com.
            
            And "another link":https://github.com here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse images`() {
        val content = """
            Here is an image: !http://example.com/image.jpg!
            
            And another !/local/image.png! image.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
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
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
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
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse strikethrough text`() {
        val content = """
            This is -strikethrough- text.
            And -this is also strikethrough-.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse superscript text`() {
        val content = """
            This is ^superscript^ text.
            And ^this is also superscript^.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse subscript text`() {
        val content = """
            This is ~subscript~ text.
            And ~this is also subscript~.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse strong emphasis text`() {
        val content = """
            This is **strong** text.
            And **this is also strong**.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse double emphasis text`() {
        val content = """
            This is __double emphasis__ text.
            And __this is also double emphasis__.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Combined Formatting Tests ====================

    @Test
    fun `should parse combined formatting`() {
        val content = """
            This is _italic and *bold*_ text.
            And *bold with -strikethrough-* text.
            Plus ^superscript and ~subscript~^ text.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate unclosed inline code`() {
        val content = """
            Here's some text with @unclosed code.
            More text here.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed inline code marker (@)") })
    }

    @Test
    fun `should validate unclosed images`() {
        val content = """
            Here's an image: !http://example.com/image.jpg
            More text here.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed image marker (!)") })
    }

    @Test
    fun `should validate invalid heading levels`() {
        val content = """
            h0. Invalid heading level 0
            h7. Invalid heading level 7
            h10. Invalid heading level 10
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Invalid heading level 0") })
        assertTrue(errors.any { it.contains("Invalid heading level 7") })
        assertTrue(errors.any { it.contains("Invalid heading level 10") })
    }

    @Test
    fun `should validate valid document`() {
        val content = """
            h1. Valid Document
            
            This is a valid Textile document.
            
            h2. Section
            
            * List item 1
            * List item 2
            
            pre. Code block content
            
            bq. This is a block quote.
            
            "Link":http://example.com and !image.jpg!
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
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("0", result.metadata["lines"])
    }

    @Test
    fun `should handle document with only whitespace`() {
        val content = "   \n  \n   \n"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("4", result.metadata["lines"])
    }

    @Test
    fun `should handle malformed headings`() {
        val content = """
            h1.Valid heading (no space)
            h1. Invalid heading (extra space)
            h1.Too many dots (no space)
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle special characters in content`() {
        val content = """
            h1. Special Characters Test
            
            This has & < > " ' characters.
            And @ # $ % ^ & * ( ) _ + = { } [ ] | \\ : ; " ' < > , . ? / characters.
            
            pre. Code with special chars: <>&"'
            
            bq. Quote with "special" <chars> & more.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert simple document to HTML`() {
        val content = """
            h1. Document Title
            
            This is a paragraph.
            
            h2. Section
            
            More content.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='textile'>"))
        assertTrue(html.contains("<h1>Document Title</h1>"))
        assertTrue(html.contains("<h2>Section</h2>"))
        assertTrue(html.contains("<p>This is a paragraph.</p>"))
        assertTrue(html.contains("<p>More content.</p>"))
    }

    @Test
    fun `should convert headings to HTML`() {
        val content = """
            h1. Level 1
            h2. Level 2
            h3. Level 3
            h4. Level 4
            h5. Level 5
            h6. Level 6
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
            * Item 3
            
            # First
            # Second
            # Third
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>Item 1</li>"))
        assertTrue(html.contains("<li>Item 2</li>"))
        assertTrue(html.contains("<li>Item 3</li>"))
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<li>First</li>"))
        assertTrue(html.contains("<li>Second</li>"))
        assertTrue(html.contains("<li>Third</li>"))
    }

    @Test
    fun `should convert formatting to HTML`() {
        val content = """
            This is *bold* and _italic_ text.
            This is -strikethrough- and ^superscript^ text.
            This is ~subscript~ and **strong** text.
            This is __double emphasis__ text.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<b>bold</b>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("<s>strikethrough</s>"))
        assertTrue(html.contains("<sup>superscript</sup>"))
        assertTrue(html.contains("<sub>subscript</sub>"))
        assertTrue(html.contains("<strong>strong</strong>"))
        assertTrue(html.contains("<em><em>double emphasis</em></em>"))
    }

    @Test
    fun `should convert links and images to HTML`() {
        val content = """
            This is a "link text":http://example.com
            And an image: !http://example.com/image.jpg!
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<a href='http://example.com'>link text</a>"))
        assertTrue(html.contains("<img src='http://example.com/image.jpg'"))
    }

    @Test
    fun `should convert block quotes to HTML`() {
        val content = """
            Normal text.
            
            bq. This is a block quote.
            It spans multiple lines.
            
            More normal text.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("This is a block quote."))
        assertTrue(html.contains("It spans multiple lines."))
    }

    @Test
    fun `should convert code blocks to HTML`() {
        val content = """
            Here's some code:
            
            pre. fun main() {
                println("Hello, World!")
            }
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<pre>"))
        assertTrue(html.contains("fun main() {"))
        assertTrue(html.contains("println(&quot;Hello, World!&quot;)"))
    }

    @Test
    fun `should convert inline code to HTML`() {
        val content = """
            This has inline @code@ in the text.
            And @another code snippet@ here.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<code>code</code>"))
        assertTrue(html.contains("<code>another code snippet</code>"))
    }

    @Test
    fun `should apply dark mode styles`() {
        val content = """
            h1. Test Document
            
            Some content.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("<div class='textile'>"))
        // Should contain styles
        assertTrue(html.contains("<style>"))
    }

    // ==================== Round-trip Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = """
            h1. Original Document
            
            This is the original content.
            
            h2. Section
            
            * List item 1
            * List item 2
            
            pre. Code block content
            
            bq. This is a block quote.
            
            "Link":http://example.com and !image.jpg!
            
            This is *bold* and _italic_ text.
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
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should parse large documents efficiently`() {
        // Create a large document with many sections and elements
        val largeContent = buildString {
            appendLine("h1. Large Document")
            appendLine()
            
            repeat(100) { sectionIndex ->
                appendLine("h2. Section ${sectionIndex + 1}")
                appendLine()
                
                repeat(10) { paragraphIndex ->
                    appendLine("This is paragraph ${paragraphIndex + 1} in section ${sectionIndex + 1}.")
                    appendLine()
                }
                
                appendLine("* List item 1")
                appendLine("* List item 2")
                appendLine("* List item 3")
                appendLine()
                
                appendLine("pre. Code block ${sectionIndex + 1}")
                appendLine()
                
                appendLine("bq. Block quote ${sectionIndex + 1}")
                appendLine()
                
                appendLine("\"Link ${sectionIndex + 1}\":http://example.com and !image${sectionIndex + 1}.jpg!")
                appendLine()
            }
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parse(largeContent)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_TEXTILE, result.format.id)
        
        // Performance assertion - should parse in reasonable time (less than 1 second for this size)
        val parseTime = endTime - startTime
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took: ${parseTime}ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        // Create a large document
        val largeContent = buildString {
            appendLine("h1. Performance Test Document")
            appendLine()
            
            repeat(50) { i ->
                appendLine("h2. Section ${i + 1}")
                appendLine()
                appendLine("This is content for section ${i + 1} with *bold* and _italic_ text.")
                appendLine()
                
                repeat(5) { j ->
                    appendLine("* Item ${j + 1}")
                }
                appendLine()
                
                appendLine("pre. Code block ${i + 1}")
                appendLine()
                
                appendLine("bq. Block quote ${i + 1}")
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
        assertTrue(conversionTime < 500, "HTML conversion should complete within 500ms, took: ${conversionTime}ms")
    }
}