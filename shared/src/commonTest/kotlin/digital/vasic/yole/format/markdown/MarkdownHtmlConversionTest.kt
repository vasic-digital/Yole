/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for HTML conversion and preview functionality
 *
 *########################################################*/
package digital.vasic.yole.format.markdown

import digital.vasic.yole.format.markdown.MarkdownParser
import kotlin.test.*

/**
 * Tests for HTML conversion and preview functionality in Markdown parser.
 * These tests ensure that the HTML output is valid, well-formed, and suitable
 * for preview rendering across different platforms.
 */
class MarkdownHtmlConversionTest {

    private lateinit var parser: MarkdownParser

    @BeforeTest
    fun setup() {
        parser = MarkdownParser()
    }

    // ==================== Basic HTML Structure Tests ====================

    @Test
    fun `should wrap content in markdown div with proper styling`() {
        val content = "Simple paragraph"
        val result = parser.parse(content)
        
        val html = result.parsedContent
        
        assertTrue(html.startsWith("<div class='markdown'>"), "Should start with markdown div")
        assertTrue(html.contains("<style>"), "Should include CSS styling")
        assertTrue(html.endsWith("</div>"), "Should end with closing div")
    }

    @Test
    fun `should produce valid HTML structure`() {
        val content = """
            # Header
            
            Paragraph with **bold** text.
            
            - List item 1
            - List item 2
            
            [Link](https://example.com)
        """.trimIndent()
        
        val result = parser.parse(content)
        val html = result.parsedContent
        
        // Basic HTML validation
        assertTrue(html.contains("<h1>"), "Should contain h1 tag")
        assertTrue(html.contains("<p>"), "Should contain p tag")
        assertTrue(html.contains("<strong>"), "Should contain strong tag")
        assertTrue(html.contains("<ul>"), "Should contain ul tag")
        assertTrue(html.contains("<li>"), "Should contain li tag")
        assertTrue(html.contains("<a href='https://example.com'>"), "Should contain a tag")
    }

    // ==================== HTML Escaping Tests ====================

    @Test
    fun `should escape HTML entities in regular text`() {
        val content = "Text with <script>alert('XSS')</script> and <div>HTML</div>"
        val result = parser.parse(content)
        
        val html = result.parsedContent
        
        assertFalse(html.contains("<script>"), "Should not contain raw script tags")
        assertFalse(html.contains("<div>HTML</div>"), "Should not contain raw div tags")
        assertTrue(html.contains("&lt;script&gt;"), "Should escape script tags")
        assertTrue(html.contains("&lt;div&gt;"), "Should escape div tags")
    }

    @Test
    fun `should escape HTML entities in code blocks`() {
        val content = """
            ```html
            <div>HTML content</div>
            <script>alert('test')</script>
            ```
        """.trimIndent()
        
        val result = parser.parse(content)
        
        val html = result.parsedContent
        
        assertTrue(html.contains("&lt;div&gt;"), "Should escape HTML in code blocks")
        assertTrue(html.contains("&lt;script&gt;"), "Should escape script tags in code blocks")
        assertTrue(html.contains("&#39;test&#39;"), "Should escape quotes in code blocks")
    }

    @Test
    fun `should escape HTML entities in inline code`() {
        val content = "Use `<div>HTML</div>` for structure"
        val result = parser.parse(content)
        
        val html = result.parsedContent
        
        assertTrue(html.contains("<code>&lt;div&gt;HTML&lt;/div&gt;</code>"), 
                  "Should escape HTML in inline code")
    }

    // ==================== CSS Styling Tests ====================

    @Test
    fun `should include comprehensive CSS styling`() {
        val content = "Test content"
        val result = parser.parse(content)
        
        val html = result.parsedContent
        
        // Check for key CSS rules
        assertTrue(html.contains(".markdown {"), "Should have markdown class styling")
        assertTrue(html.contains("font-family:"), "Should include font family")
        assertTrue(html.contains("line-height:"), "Should include line height")
        assertTrue(html.contains(".markdown h1 {"), "Should have h1 styling")
        assertTrue(html.contains(".markdown p {"), "Should have paragraph styling")
        assertTrue(html.contains(".markdown code {"), "Should have code styling")
        assertTrue(html.contains(".markdown a {"), "Should have link styling")
    }

    @Test
    fun `should include table styling`() {
        val content = """
            | Header 1 | Header 2 |
            |----------|----------|
            | Cell 1   | Cell 2   |
        """.trimIndent()
        
        val result = parser.parse(content)
        
        val html = result.parsedContent
        
        assertTrue(html.contains(".markdown table {"), "Should have table styling")
        assertTrue(html.contains(".markdown table th {"), "Should have table header styling")
        assertTrue(html.contains(".markdown table td {"), "Should have table cell styling")
    }

    // ==================== Cross-platform Compatibility Tests ====================

    @Test
    fun `should produce consistent HTML across different content`() {
        val testCases = listOf(
            "Simple text",
            "**Bold text**",
            "*Italic text*",
            "`Code text`",
            "~~Strikethrough~~"
        )
        
        val results = testCases.map { parser.parse(it).parsedContent }
        
        // All should be wrapped in the same structure
        results.forEach { html ->
            assertTrue(html.startsWith("<div class='markdown'>"), "Should have consistent wrapper")
            assertTrue(html.contains("<style>"), "Should have consistent styling")
            assertTrue(html.endsWith("</div>"), "Should have consistent closing")
        }
    }

    @Test
    fun `should handle Unicode characters in HTML output`() {
        val content = "Unicode test: 你好世界 🌍 Привет мир"
        val result = parser.parse(content)
        
        val html = result.parsedContent
        
        assertTrue(html.contains("你好世界"), "Should preserve Chinese characters")
        assertTrue(html.contains("🌍"), "Should preserve emoji")
        assertTrue(html.contains("Привет мир"), "Should preserve Cyrillic characters")
    }

    // ==================== Preview Functionality Tests ====================

    @Test
    fun `toHtml should return the same content as parsedContent`() {
        val content = "Test content with **bold** text"
        val document = parser.parse(content)
        
        val htmlFromMethod = parser.toHtml(document)
        val htmlFromProperty = document.parsedContent
        
        assertEquals(htmlFromProperty, htmlFromMethod, "toHtml should return same as parsedContent")
    }

    @Test
    fun `toHtml should work with light mode parameter`() {
        val content = "Test content"
        val document = parser.parse(content)
        
        val htmlLight = parser.toHtml(document, true)
        val htmlDark = parser.toHtml(document, false)
        
        // Currently they should be the same, but this allows for future light/dark theme support
        assertEquals(htmlLight, htmlDark, "Light and dark mode should produce same output for now")
    }

    @Test
    fun `should produce HTML suitable for web preview`() {
        val content = """
            # Web Preview Test
            
            This is a **comprehensive** test for *web preview* functionality.
            
            ## Features
            
            - **Bold items** in lists
            - *Italic items* with `code`
            - ~~Strikethrough~~ for deprecated items
            
            | Feature | Status | Description |
            |---------|--------|-------------|
            | **Bold** | ✅ | Working |
            | *Italic* | ✅ | Working |
            | `Code` | ✅ | Working |
            
            [Visit website](https://example.com) for more info.
        """.trimIndent()
        
        val result = parser.parse(content)
        val html = result.parsedContent
        
        // Check that it's self-contained HTML suitable for embedding
        assertTrue(html.contains("<div class='markdown'>"), "Should be wrapped in div")
        assertTrue(html.contains("<style>"), "Should include CSS")
        assertTrue(html.contains("</div>"), "Should be properly closed")
        
        // Check for all expected elements
        assertTrue(html.contains("<h1>"), "Should have h1 for web preview")
        assertTrue(html.contains("<h2>"), "Should have h2 for web preview")
        assertTrue(html.contains("<ul>"), "Should have lists for web preview")
        assertTrue(html.contains("<table>"), "Should have tables for web preview")
        assertTrue(html.contains("<strong>"), "Should have bold for web preview")
        assertTrue(html.contains("<em>"), "Should have italic for web preview")
        assertTrue(html.contains("<code>"), "Should have code for web preview")
        assertTrue(html.contains("<s>"), "Should have strikethrough for web preview")
        assertTrue(html.contains("<a href='https://example.com'>"), "Should have links for web preview")
    }

    // ==================== Performance and Memory Tests ====================

    @Test
    fun `should handle large documents efficiently`() {
        val content = """
            # Large Document Test
            
            ${"This is a paragraph with **bold**, *italic*, `code`, and ~~strikethrough~~ formatting.\n".repeat(100)}
            
            ## More Content
            
            ${"Another paragraph with [links](https://example.com) and nested **bold *italic* text**.\n".repeat(100)}
            
            | ${"Column 1 | Column 2 | Column 3 |".repeat(10)} |
            |${"----------|".repeat(10)}|
            | ${"Cell 1 | Cell 2 | Cell 3 |".repeat(10)} |
            
            - ${"List item with **bold** and *italic*\n".repeat(50)}
        """.trimIndent()
        
        val startTime = System.currentTimeMillis()
        val result = parser.parse(content)
        val endTime = System.currentTimeMillis()
        
        val html = result.parsedContent
        
        // Should complete within reasonable time (5 seconds for large document)
        assertTrue(endTime - startTime < 5000, "Should process large documents quickly")
        
        // Should produce valid output
        assertTrue(html.contains("<h1>"), "Should have headers in large document")
        assertTrue(html.contains("<table>"), "Should have tables in large document")
        assertTrue(html.contains("<ul>"), "Should have lists in large document")
        assertTrue(html.contains("<strong>"), "Should have bold formatting in large document")
        assertTrue(html.contains("<em>"), "Should have italic formatting in large document")
    }

    // ==================== Error Recovery Tests ====================

    @Test
    fun `should produce usable HTML even with malformed input`() {
        val malformedContent = """
            # Header
            
            **Unclosed bold
            
            *Unclosed italic
            
            `Unclosed code
            
            ~~Unclosed strikethrough
            
            [Unclosed link
            
            ![Unclosed image
            
            Normal text should still work.
        """.trimIndent()
        
        val result = parser.parse(malformedContent)
        val html = result.parsedContent
        
        // Should still produce usable HTML
        assertTrue(html.contains("<div class='markdown'>"), "Should have wrapper")
        assertTrue(html.contains("<h1>Header</h1>"), "Should have properly formatted header")
        assertTrue(html.contains("Normal text should still work"), "Should have normal text")
        
        // Should not crash or produce empty output
        assertFalse(html.isEmpty(), "Should not produce empty output")
    }

    // ==================== HTML Validation Tests ====================

    @Test
    fun `should produce properly nested HTML`() {
        val content = "*italic with **bold** inside*"
        val result = parser.parse(content)
        
        val html = result.parsedContent
        
        // Check proper nesting with regex
        val emPattern = "<em>(.*?)</em>".toRegex()
        val emMatch = emPattern.find(html)
        
        assertNotNull(emMatch, "Should find em tags")
        
        val emContent = emMatch!!.groupValues[1]
        assertTrue(emContent.contains("<strong>bold</strong>"), 
                  "Should have properly nested strong tags inside em tags")
    }

    @Test
    fun `should close all HTML tags properly`() {
        val content = """
            # Header
            
            Paragraph with **bold** and *italic*.
            
            - List item 1
            - List item 2
            
            > Blockquote with `code`
            
            | Col 1 | Col 2 |
            |-------|-------|
            | A     | B     |
        """.trimIndent()
        
        val result = parser.parse(content)
        val html = result.parsedContent
        
        // Count opening and closing tags
        val tags = listOf("h1", "p", "strong", "em", "ul", "li", "blockquote", "code", "table", "tr", "td", "th")
        
        tags.forEach { tag ->
            val openCount = html.split("<$tag>").size - 1
            val closeCount = html.split("</$tag>").size - 1
            assertEquals(openCount, closeCount, "Tag <$tag> should have matching open/close count")
        }
    }
}