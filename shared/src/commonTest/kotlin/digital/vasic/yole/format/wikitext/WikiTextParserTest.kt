/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for WikiText parser
 *
 *########################################################*/
package digital.vasic.yole.format.wikitext

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.system.measureTimeMillis

/**
 * Comprehensive unit tests for WikiText format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic WikiText parsing (sections, paragraphs, lists)
 * - WikiText formatting (bold, italic, links)
 * - Tables and templates
 * - Categories and interwiki links
 * - Round-trip parsing (parse -> format -> parse)
 * - Edge cases (empty files, malformed WikiText)
 * - Performance benchmarks
 * - HTML conversion
 */
class WikiTextParserTest {

    private val parser = WikitextParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect WikiText format by extension`() {
        val format = FormatRegistry.getByExtension(".wiki")

        assertNotNull(format)
        assertEquals(TextFormat.ID_WIKITEXT, format.id)
        assertEquals("WikiText", format.name)
    }

    @Test
    fun `should detect WikiText format by wikitext extension`() {
        val format = FormatRegistry.getByExtension(".wikitext")

        assertNotNull(format)
        assertEquals(TextFormat.ID_WIKITEXT, format.id)
    }

    @Test
    fun `should detect WikiText format by filename`() {
        val format = FormatRegistry.detectByFilename("document.wiki")

        assertNotNull(format)
        assertEquals(TextFormat.ID_WIKITEXT, format.id)
    }

    @Test
    fun `should support all WikiText extensions`() {
        // Only .wiki and .wikitext are registered for WikiText in FormatRegistry.
        // .txt maps to Plain Text, not WikiText.
        val registeredExtensions = listOf(".wiki", ".wikitext")

        registeredExtensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_WIKITEXT, format.id)
        }

        // Verify .txt is recognized but maps to a different format
        val txtFormat = FormatRegistry.getByExtension(".txt")
        assertNotNull(txtFormat, "Extension .txt should be recognized")
        assertEquals(TextFormat.ID_PLAINTEXT, txtFormat.id)
    }

    // ==================== Basic WikiText Parsing Tests ====================

    @Test
    fun `should parse basic WikiText document structure`() {
        val content = """
            = Main Title =

            This is a paragraph in the document.

            == First Section ==

            More content here.

            === Subsection ===

            Even deeper content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)

        // Check metadata extraction
        assertEquals(".wiki", result.metadata["extension"])
        assertEquals("11", result.metadata["lines"])
        assertEquals("false", result.metadata["hasZimHeader"])
    }

    @Test
    fun `should parse WikiText headings with different levels`() {
        val content = """
            = Level 1 Heading =
            == Level 2 Heading ==
            === Level 3 Heading ===
            ==== Level 4 Heading ====
            ===== Level 5 Heading =====
            ====== Level 6 Heading ======
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
    }

    @Test
    fun `should parse WikiText paragraphs`() {
        val content = """
            This is the first paragraph.
            It spans multiple lines.

            This is the second paragraph.

            And this is the third paragraph.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
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
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse ordered lists`() {
        val content = """
            1. First item
            2. Second item
            3. Third item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse checklists`() {
        val content = """
            [ ] Unchecked item
            [*] Checked item
            [x] Crossed item
            [>] Forward item
            [<] Backward item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Code Blocks Tests ====================

    @Test
    fun `should parse code blocks`() {
        val content = """
            Here's some code:

            '''
            fun main() {
                println("Hello, World!")
            }
            '''

            End of code.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Text Formatting Tests ====================

    @Test
    fun `should parse bold text`() {
        val content = """
            This is **bold** text.
            And **this is also bold**.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse italic text`() {
        val content = """
            This is //italic// text.
            And //this is also italic//.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse highlighted text`() {
        val content = """
            This is __highlighted__ text.
            And __this is also highlighted__.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse strikethrough text`() {
        val content = """
            This is ~~strikethrough~~ text.
            And ~~this is also strikethrough~~.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse superscript text`() {
        val content = """
            This is superscript: ^{text}
            And another: ^{2}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse subscript text`() {
        val content = """
            This is subscript: _{text}
            And another: _{2}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse inline code`() {
        val content = """
            This is inline code: ''code''
            And another: ''variable''
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Links and Images Tests ====================

    @Test
    fun `should parse basic links`() {
        val content = """
            This is a link: [[http://example.com]]
            And another: [[http://github.com|GitHub]]
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse images`() {
        val content = """
            This is an image: {{image.jpg}}
            And another: {{photo.png|Alt text}}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Zim Header Tests ====================

    @Test
    fun `should parse Zim header`() {
        val content = """
            [DocumentAttributes]
            title=My Document
            creation=2025-01-01

            = Main Title =

            This is the document content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals("true", result.metadata["hasZimHeader"])
        assertEquals(content, result.rawContent)
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate unbalanced headings`() {
        val content = """
            = Unbalanced Heading ==
            == Another Unbalanced =
            ==== Too Many Equals ===
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unbalanced heading markers") })
    }

    @Test
    fun `should validate unclosed brackets in links`() {
        val content = """
            Here's a malformed link: [[http://example.com
            And another: [[link]]
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed brackets in links") })
    }

    @Test
    fun `should validate unclosed braces in images`() {
        val content = """
            Here's a malformed image: {{image.jpg
            And another: {{photo.png}}
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed braces in images") })
    }

    @Test
    fun `should validate valid document`() {
        val content = """
            = Valid Document =

            This is a valid WikiText document.

            == Section ==

            * List item 1
            * List item 2

            '''
            Code block content
            '''

            This is **bold** and this is //italic//.
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
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
        assertTrue(result.metadata.isNotEmpty())
    }

    @Test
    fun `should handle document with only whitespace`() {
        val content = "   \n  \n   \n"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle document with only code block`() {
        val content = """
            '''
            Only code here
            '''
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle malformed headings`() {
        val content = """
            =No space heading=
            = Invalid heading (extra space) =
            ====Too many equals (no space)====
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert simple document to HTML`() {
        val content = """
            = Document Title =

            This is a paragraph.

            == Section ==

            More content.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='wikitext'>"))
        assertTrue(html.contains("<h1>Document Title</h1>"))
        assertTrue(html.contains("<h2>Section</h2>"))
        assertTrue(html.contains("<p>This is a paragraph.</p>"))
        assertTrue(html.contains("<p>More content.</p>"))
    }

    @Test
    fun `should convert headings to HTML`() {
        val content = """
            = Level 1 =
            == Level 2 ==
            === Level 3 ===
            ==== Level 4 ====
            ===== Level 5 =====
            ====== Level 6 ======
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

            1. First
            2. Second
            3. Third

            [ ] Unchecked
            [*] Checked
            [x] Crossed
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>Item 1</li>"))
        assertTrue(html.contains("<li>Item 2</li>"))
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<li>First</li>"))
        assertTrue(html.contains("<ul class='checklist'>"))
    }

    @Test
    fun `should convert text formatting to HTML`() {
        val content = """
            This is **bold** text.
            This is //italic// text.
            This is __highlighted__ text.
            This is ~~strikethrough~~ text.
            This is ^{superscript} text.
            This is _{subscript} text.
            This is ''inline code''.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("<span class='highlight'>highlighted</span>"))
        assertTrue(html.contains("<s>strikethrough</s>"))
        assertTrue(html.contains("<sup>superscript</sup>"))
        assertTrue(html.contains("<sub>subscript</sub>"))
        assertTrue(html.contains("<code>inline code</code>"))
    }

    @Test
    fun `should convert links and images to HTML`() {
        val content = """
            This is a link: [[http://example.com]]
            And another: [[http://github.com|GitHub]]
            This is an image: {{image.jpg}}
            And another: {{photo.png|Alt text}}
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<a href='http://example.com'>"))
        assertTrue(html.contains("<a href='http://github.com'>GitHub</a>"))
        assertTrue(html.contains("<img src='image.jpg'"))
        assertTrue(html.contains("<img src='photo.png'"))
    }

    @Test
    fun `should convert code blocks to HTML`() {
        val content = """
            Here's some code:

            '''
            fun main() {
                println("Hello, World!")
            }
            '''
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<pre>"))
        assertTrue(html.contains("fun main() {"))
        assertTrue(html.contains("println(&quot;Hello, World!&quot;)"))
        assertTrue(html.contains("</pre>"))
    }

    @Test
    fun `should apply dark mode styles`() {
        val content = """
            = Test Document =

            Some content.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("<div class='wikitext'>"))
        // Should contain styles
        assertTrue(html.contains("<style>"))
    }

    // ==================== Round-trip Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = """
            = Original Document =

            This is the original content.

            == Section ==

            * List item 1
            * List item 2

            '''
            Code block content
            '''

            This is **bold** and this is //italic//.

            Here's a link: [[http://example.com]]
            And an image: {{image.jpg}}
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
            appendLine("= Large Document =")
            appendLine()

            repeat(100) { sectionIndex ->
                appendLine("== Section ${sectionIndex + 1} ==")
                appendLine()

                repeat(10) { paragraphIndex ->
                    appendLine("This is paragraph ${paragraphIndex + 1} in section ${sectionIndex + 1}.")
                    appendLine()
                }

                appendLine("* List item 1")
                appendLine("* List item 2")
                appendLine("* List item 3")
                appendLine()

                appendLine("'''")
                appendLine("Code block content")
                appendLine("'''")
                appendLine()

                appendLine("This is **bold** and this is //italic//.")
                appendLine()
            }
        }

        val parseTime = measureTimeMillis {
            val result = parser.parse(largeContent)
            assertNotNull(result)
            assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        }

        // Performance assertion - should parse in reasonable time (less than 1 second for this size)
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took: ${parseTime}ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        // Create a large document
        val largeContent = buildString {
            appendLine("= Performance Test Document =")
            appendLine()

            repeat(50) { i ->
                appendLine("== Section ${i + 1} ==")
                appendLine()
                appendLine("This is content for section ${i + 1}.")
                appendLine()

                repeat(5) { j ->
                    appendLine("* Item ${j + 1}")
                }
                appendLine()

                appendLine("'''")
                appendLine("Code block ${i + 1}")
                appendLine("'''")
                appendLine()
            }
        }

        val document = parser.parse(largeContent)

        val conversionTime = measureTimeMillis {
            val html = parser.toHtml(document, lightMode = true)
            assertNotNull(html)
            assertTrue(html.isNotEmpty())
        }

        // Performance assertion - should convert in reasonable time (less than 500ms)
        assertTrue(conversionTime < 500, "HTML conversion should complete within 500ms, took: ${conversionTime}ms")
    }
}
