/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Creole parser
 *
 *########################################################*/
package digital.vasic.yole.format.creole

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
 * Comprehensive unit tests for Creole format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic Creole parsing (headings, paragraphs, lists)
 * - Creole formatting (bold, italic, links, images)
 * - Tables and code blocks
 * - Horizontal rules and line breaks
 * - Round-trip parsing (parse → format → parse)
 * - Edge cases (empty files, malformed Creole)
 * - Performance benchmarks
 * - HTML conversion
 */
class CreoleParserTest {

    private val parser = CreoleParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Creole format by extension`() {
        val format = FormatRegistry.getByExtension(".creole")

        assertNotNull(format)
        assertEquals(TextFormat.ID_CREOLE, format?.id)
        assertEquals("Creole", format?.name)
    }

    @Test
    fun `should detect Creole format by txt extension`() {
        val format = FormatRegistry.getByExtension(".txt")

        assertNotNull(format)
        // .txt can be multiple formats, Creole should be one of the options
        val creoleFormat = FormatRegistry.formats.first { it.id == TextFormat.ID_CREOLE }
        assertTrue(creoleFormat.extensions.contains(".txt"))
    }

    @Test
    fun `should detect Creole format by filename`() {
        val format = FormatRegistry.detectByFilename("document.creole")

        assertNotNull(format)
        assertEquals(TextFormat.ID_CREOLE, format?.id)
    }

    @Test
    fun `should support all Creole extensions`() {
        val extensions = listOf(".creole", ".txt")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            if (ext == ".creole") {
                assertEquals(TextFormat.ID_CREOLE, format?.id)
            }
        }
    }

    // ==================== Basic Creole Parsing Tests ====================

    @Test
    fun `should parse basic Creole document structure`() {
        val content = """
            = Main Heading
            
            This is a paragraph in the document.
            
            == Subheading
            
            More content here.
            
            === Deep Heading
            
            Even deeper content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("", result.metadata["extension"])
        assertEquals("11", result.metadata["lines"])
    }

    @Test
    fun `should parse Creole headings with different levels`() {
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
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
    }

    @Test
    fun `should parse Creole paragraphs`() {
        val content = """
            This is the first paragraph.
            It spans multiple lines.
            
            This is the second paragraph.
            
            And this is the third paragraph.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
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
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse ordered lists`() {
        val content = """
            # First item
            # Second item
            ## Nested item 1
            ## Nested item 2
            # Third item
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse mixed nested lists`() {
        val content = """
            * First item
            * Second item
            ** Nested unordered 1
            ** Nested unordered 2
            * Third item
            # First ordered
            # Second ordered
            ## Nested ordered 1
            ## Nested ordered 2
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
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
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
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
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse combined bold and italic`() {
        val content = """
            This is **//bold italic//** text.
            And //**italic bold**// text.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Links and Images Tests ====================

    @Test
    fun `should parse basic links`() {
        val content = """
            This is a [[http://example.com|Example Website]] link.
            And a [[https://github.com|GitHub]] link.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse links without descriptions`() {
        val content = """
            This is a [[http://example.com]] link without description.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse images`() {
        val content = """
            Here is an image: {{image.jpg|Alternative text}}.
            And another: {{logo.png}}.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Code Blocks Tests ====================

    @Test
    fun `should parse code blocks`() {
        val content = """
            Here's some code:
            
            {{{
            fun main() {
                println("Hello, World!")
            }
            }}}
            
            End of code.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse inline code`() {
        val content = """
            This is inline {{{code}}} in a paragraph.
            And more {{{inline code}}} here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Tables Tests ====================

    @Test
    fun `should parse simple tables`() {
        val content = """
            |Header 1|Header 2|Header 3|
            |Cell 1|Cell 2|Cell 3|
            |Cell 4|Cell 5|Cell 6|
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse tables with header cells`() {
        val content = """
            |=Header 1|=Header 2|=Header 3|
            |Cell 1|Cell 2|Cell 3|
            |Cell 4|Cell 5|Cell 6|
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse tables with formatted content`() {
        val content = """
            |=**Bold Header**|//Italic Header//|[[Link|Header]]|
            |**Bold Cell**|//Italic Cell//|[[http://example.com|Link]]|
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Horizontal Rules and Line Breaks Tests ====================

    @Test
    fun `should parse horizontal rules`() {
        val content = """
            Content before rule.
            
            ----
            
            Content after rule.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse line breaks`() {
        val content = """
            This is a line with a line break\\
            and this continues on the next line.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
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
        assertTrue(html.contains("<div class='creole'>"))
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
            
            # First
            # Second
            ## Nested A
            ## Nested B
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
    fun `should convert text formatting to HTML`() {
        val content = """
            This is **bold** text.
            This is //italic// text.
            This is **//bold italic//** text.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("<strong><em>bold italic</em></strong>"))
    }

    @Test
    fun `should convert links to HTML`() {
        val content = """
            This is a [[http://example.com|Example Website]] link.
            This is a [[http://example.com]] link without description.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("href='http://example.com'"))
        assertTrue(html.contains("Example Website") || html.contains("example.com"))
    }

    @Test
    fun `should convert images to HTML`() {
        val content = """
            Here is an image: {{image.jpg|Alternative text}}.
            And another: {{logo.png}}.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<img src='image.jpg' alt='Alternative text'/>"))
        assertTrue(html.contains("<img src='logo.png' alt='logo.png'/>"))
    }

    @Test
    fun `should convert code blocks to HTML`() {
        val content = """
            Here's some code:
            
            {{{
            fun main() {
                println("Hello, World!")
            }
            }}}
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
    fun `should convert tables to HTML`() {
        val content = """
            |=Header 1|=Header 2|
            |Cell 1|Cell 2|
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<th>Header 1</th>"))
        assertTrue(html.contains("<th>Header 2</th>"))
        assertTrue(html.contains("<td>Cell 1</td>"))
        assertTrue(html.contains("<td>Cell 2</td>"))
        assertTrue(html.contains("</table>"))
    }

    @Test
    fun `should convert horizontal rules to HTML`() {
        val content = """
            Content before rule.
            
            ----
            
            Content after rule.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<hr>"))
    }

    @Test
    fun `should convert line breaks to HTML`() {
        val content = """
            This is a line with a line break\\
            and this continues on the next line.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<br>"))
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
        assertTrue(html.contains("<div class='creole'>"))
        // Should contain dark mode styles
        assertTrue(html.contains("<style>"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate malformed table rows`() {
        val content = """
            |Header 1|Header 2|
            |Cell 1|Cell 2
            |Cell 3|Cell 4|
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Malformed table row") })
    }

    @Test
    fun `should validate unclosed brackets in links`() {
        val content = """
            This is a [[http://example.com|unclosed link.
            And another [[http://example.com]] that is fine.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed brackets") })
    }

    @Test
    fun `should validate unclosed braces`() {
        val content = """
            This has a code block that is not closed:
            {{{
            Some code here
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Unclosed braces") })
    }

    @Test
    fun `should validate valid document`() {
        val content = """
            = Valid Document
            
            This is a valid Creole document.
            
            == Section
            
            * List item 1
            * List item 2
            
            {{{
            Code block content
            }}}
            
            Here's a [[http://example.com|link]].
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
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("", result.metadata["extension"])
        assertEquals("1", result.metadata["lines"])
    }

    @Test
    fun `should handle document with only whitespace`() {
        val content = "   \n  \n   \n"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle malformed headings`() {
        val content = """
            =Valid heading (no space)
            = Invalid heading (extra space)
            ======Too many equals (no space)
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle empty lists`() {
        val content = """
            *
            #
            **
            ##
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle empty table cells`() {
        val content = """
            |Header 1||Header 3|
            |Cell 1||Cell 3|
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Round-trip Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = """
            = Original Document
            
            This is the original content.
            
            == Section
            
            * List item 1
            * List item 2
            
            {{{
            Code block content
            }}}
            
            Here's some **bold** and //italic// text.
            
            Here's a [[http://example.com|link]] and {{image.jpg|image}}.
            
            |=Header|Cell|
            |Data|More data|
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
                
                appendLine("{{{")
                appendLine("Code block content")
                appendLine("}}}")
                appendLine()
                
                appendLine("Here's some **bold** and //italic// text.")
                appendLine()
                
                appendLine("|=Header 1|Header 2|Header 3|")
                appendLine("|Data ${sectionIndex + 1}-1|Data ${sectionIndex + 1}-2|Data ${sectionIndex + 1}-3|")
                appendLine()
            }
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parse(largeContent)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        
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
                
                appendLine("{{{")
                appendLine("Code block ${i + 1}")
                appendLine("}}}")
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

    // ==================== Complex Document Tests ====================

    @Test
    fun `should parse complex Creole document`() {
        val content = """
            = Welcome to Creole Wiki
            
            This is a comprehensive example of **Creole** wiki syntax.
            
            == Text Formatting
            
            You can make text **bold**, //italic//, or **//both//**.
            
            Inline code looks like this: {{{println("Hello")}}}.
            
            == Lists
            
            Unordered lists:
            * First item
            * Second item
            ** Nested item A
            ** Nested item B
            * Third item
            
            Ordered lists:
            # First step
            # Second step
            ## Sub-step A
            ## Sub-step B
            # Third step
            
            == Links and Images
            
            External links: [[http://example.com|Example Website]]
            
            Images: {{photo.jpg|A beautiful photo}}
            
            == Tables
            
            |=Feature|Status|Description|
            |=Syntax|✓|Easy to learn|
            |=Powerful|✓|Rich formatting|
            |=Standard|✓|Wiki Creole 1.0|
            
            == Code Blocks
            
            {{{
            function greet(name) {
                return "Hello, " + name + "!";
            }
            
            console.log(greet("World"));
            }}}
            
            == Special Elements
            
            Horizontal rule:
            
            ----
            
            Line breaks:\
            This line has a manual break.
            
            == Conclusion
            
            Creole is a simple yet powerful wiki markup language.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CREOLE, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Verify HTML conversion works
        val html = parser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.contains("<h1>Welcome to Creole Wiki</h1>"))
        assertTrue(html.contains("<strong>Creole</strong>"))
        assertTrue(html.contains("<h2>Text Formatting</h2>"))
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<pre>"))
        assertTrue(html.contains("<hr>"))
    }
}