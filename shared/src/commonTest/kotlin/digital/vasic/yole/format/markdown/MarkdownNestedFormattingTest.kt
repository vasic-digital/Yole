/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for nested formatting in Markdown parser
 *
 *########################################################*/
package digital.vasic.yole.format.markdown

import digital.vasic.yole.format.markdown.MarkdownParser
import kotlin.test.*

/**
 * Comprehensive tests for nested formatting scenarios in Markdown parser.
 * These tests ensure that complex nested formatting is handled correctly
 * and produces valid HTML output.
 */
class MarkdownNestedFormattingTest {

    private lateinit var parser: MarkdownParser

    @BeforeTest
    fun setup() {
        parser = MarkdownParser()
    }

    // ==================== Basic Nested Formatting Tests ====================

    @Test
    fun `should handle italic with bold inside`() {
        val content = "*italic with **bold** inside*"
        val result = parser.parse(content)
        
        // Should produce: <em>italic with <strong>bold</strong> inside</em>
        assertTrue(result.parsedContent.contains("<em>"), "Should contain opening em tag")
        assertTrue(result.parsedContent.contains("</em>"), "Should contain closing em tag")
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain opening strong tag")
        assertTrue(result.parsedContent.contains("</strong>"), "Should contain closing strong tag")
        
        // Verify proper nesting - strong should be inside em
        val emStart = result.parsedContent.indexOf("<em>")
        val strongStart = result.parsedContent.indexOf("<strong>")
        val strongEnd = result.parsedContent.indexOf("</strong>")
        val emEnd = result.parsedContent.indexOf("</em>")
        
        assertTrue(emStart < strongStart, "em tag should start before strong tag")
        assertTrue(strongStart < strongEnd, "strong start should come before strong end")
        assertTrue(strongEnd < emEnd, "strong end should come before em end")
    }

    @Test
    fun `should handle bold with italic inside`() {
        val content = "**bold with *italic* inside**"
        val result = parser.parse(content)
        
        // Should produce: <strong>bold with <em>italic</em> inside</strong>
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain opening strong tag")
        assertTrue(result.parsedContent.contains("</strong>"), "Should contain closing strong tag")
        assertTrue(result.parsedContent.contains("<em>"), "Should contain opening em tag")
        assertTrue(result.parsedContent.contains("</em>"), "Should contain closing em tag")
        
        // Verify proper nesting - em should be inside strong
        val strongStart = result.parsedContent.indexOf("<strong>")
        val emStart = result.parsedContent.indexOf("<em>")
        val emEnd = result.parsedContent.indexOf("</em>")
        val strongEnd = result.parsedContent.indexOf("</strong>")
        
        assertTrue(strongStart < emStart, "strong tag should start before em tag")
        assertTrue(emStart < emEnd, "em start should come before em end")
        assertTrue(emEnd < strongEnd, "em end should come before strong end")
    }

    @Test
    fun `should handle bold and italic combined with triple asterisks`() {
        val content = "***bold and italic***"
        val result = parser.parse(content)
        
        // Should produce: <em><strong>bold and italic</strong></em>
        assertTrue(result.parsedContent.contains("<em><strong>"), "Should contain nested em and strong tags")
        assertTrue(result.parsedContent.contains("</strong></em>"), "Should contain properly nested closing tags")
    }

    @Test
    fun `should handle strikethrough with bold inside`() {
        val content = "~~strikethrough with **bold** inside~~"
        val result = parser.parse(content)
        
        // Should produce: <s>strikethrough with <strong>bold</strong> inside</s>
        assertTrue(result.parsedContent.contains("<s>"), "Should contain opening s tag")
        assertTrue(result.parsedContent.contains("</s>"), "Should contain closing s tag")
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain opening strong tag")
        assertTrue(result.parsedContent.contains("</strong>"), "Should contain closing strong tag")
    }

    @Test
    fun `should handle strikethrough with italic inside`() {
        val content = "~~strikethrough with *italic* inside~~"
        val result = parser.parse(content)
        
        // Should produce: <s>strikethrough with <em>italic</em> inside</s>
        assertTrue(result.parsedContent.contains("<s>"), "Should contain opening s tag")
        assertTrue(result.parsedContent.contains("</s>"), "Should contain closing s tag")
        assertTrue(result.parsedContent.contains("<em>"), "Should contain opening em tag")
        assertTrue(result.parsedContent.contains("</em>"), "Should contain closing em tag")
    }

    @Test
    fun `should handle code with formatting inside`() {
        val content = "`code with *italic* and **bold** inside`"
        val result = parser.parse(content)
        
        // Code should preserve the literal text, no formatting inside
        assertTrue(result.parsedContent.contains("<code>"), "Should contain code tags")
        assertTrue(result.parsedContent.contains("code with *italic* and **bold** inside"), 
                  "Code content should be preserved literally")
        assertFalse(result.parsedContent.contains("<em>"), "Should not contain em tags inside code")
        assertFalse(result.parsedContent.contains("<strong>"), "Should not contain strong tags inside code")
    }

    @Test
    fun `should handle italic with code inside`() {
        val content = "*italic with `code` inside*"
        val result = parser.parse(content)
        
        // Should produce: <em>italic with <code>code</code> inside</em>
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<code>"), "Should contain code tags")
    }

    @Test
    fun `should handle bold with code inside`() {
        val content = "**bold with `code` inside**"
        val result = parser.parse(content)
        
        // Should produce: <strong>bold with <code>code</code> inside</strong>
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
        assertTrue(result.parsedContent.contains("<code>"), "Should contain code tags")
    }

    @Test
    fun `should handle strikethrough with code inside`() {
        val content = "~~strikethrough with `code` inside~~"
        val result = parser.parse(content)
        
        // Should produce: <s>strikethrough with <code>code</code> inside</s>
        assertTrue(result.parsedContent.contains("<s>"), "Should contain s tags")
        assertTrue(result.parsedContent.contains("<code>"), "Should contain code tags")
    }

    // ==================== Complex Multi-level Nesting Tests ====================

    @Test
    fun `should handle triple nested formatting`() {
        val content = "***~~bold italic strikethrough~~***"
        val result = parser.parse(content)
        
        // Should handle all three formatting types
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
        assertTrue(result.parsedContent.contains("<s>"), "Should contain s tags")
    }

    @Test
    fun `should handle italic with bold and strikethrough`() {
        val content = "*italic with **bold** and ~~strikethrough~~ inside*"
        val result = parser.parse(content)
        
        // Should contain all formatting types with proper nesting
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
        assertTrue(result.parsedContent.contains("<s>"), "Should contain s tags")
    }

    @Test
    fun `should handle bold with italic and strikethrough`() {
        val content = "**bold with *italic* and ~~strikethrough~~ inside**"
        val result = parser.parse(content)
        
        // Should contain all formatting types with proper nesting
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<s>"), "Should contain s tags")
    }

    // ==================== Mixed Asterisk and Underscore Tests ====================

    @Test
    fun `should handle mixed asterisk and underscore formatting`() {
        val content = "*italic with __bold__ inside*"
        val result = parser.parse(content)
        
        // Should handle mixed markers correctly
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
    }

    @Test
    fun `should handle mixed underscore and asterisk formatting`() {
        val content = "_italic with **bold** inside_"
        val result = parser.parse(content)
        
        // Should handle mixed markers correctly
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
    }

    // ==================== Links and Images with Formatting Tests ====================

    @Test
    fun `should handle link text with bold formatting`() {
        val content = "[**Bold Link**](https://example.com)"
        val result = parser.parse(content)
        
        // Should process formatting inside link text
        assertTrue(result.parsedContent.contains("<a href='https://example.com'>"), "Should contain link")
        assertTrue(result.parsedContent.contains("<strong>Bold Link</strong>"), "Should contain bold formatting in link text")
    }

    @Test
    fun `should handle link text with italic formatting`() {
        val content = "[*Italic Link*](https://example.com)"
        val result = parser.parse(content)
        
        // Should process formatting inside link text
        assertTrue(result.parsedContent.contains("<a href='https://example.com'>"), "Should contain link")
        assertTrue(result.parsedContent.contains("<em>Italic Link</em>"), "Should contain italic formatting in link text")
    }

    @Test
    fun `should handle link text with multiple formatting`() {
        val content = "[***Bold Italic Link***](https://example.com)"
        val result = parser.parse(content)
        
        // Should process complex formatting inside link text
        assertTrue(result.parsedContent.contains("<a href='https://example.com'>"), "Should contain link")
        assertTrue(result.parsedContent.contains("<em><strong>Bold Italic Link</strong></em>"), 
                  "Should contain bold italic formatting in link text")
    }

    // ==================== Edge Cases and Error Handling Tests ====================

    @Test
    fun `should handle incomplete nested formatting gracefully`() {
        val content = "*incomplete **bold without closing"
        val result = parser.parse(content)
        
        // Should not crash and should produce some output
        assertNotNull(result.parsedContent, "Should produce output even with incomplete formatting")
        assertTrue(result.parsedContent.isNotEmpty(), "Output should not be empty")
    }

    @Test
    fun `should handle empty formatting markers`() {
        val content = "** **** **"
        val result = parser.parse(content)
        
        // Should handle empty markers gracefully
        assertNotNull(result.parsedContent, "Should produce output")
    }

    @Test
    fun `should handle formatting at word boundaries`() {
        val content = "word*italic*word and word**bold**word"
        val result = parser.parse(content)
        
        // Should handle formatting within words
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
    }

    @Test
    fun `should handle consecutive formatting markers`() {
        val content = "***consecutive***formatting**test***"
        val result = parser.parse(content)
        
        // Should handle consecutive markers correctly
        assertNotNull(result.parsedContent, "Should produce output")
    }

    // ==================== Real-world Scenario Tests ====================

    @Test
    fun `should handle real-world documentation formatting`() {
        val content = """
            This is **important** documentation with *emphasis* and `code examples`.
            
            You can also use ~~deprecated~~ methods like `oldFunction()` but **don't** *overuse* formatting.
            
            See the [**official documentation**](https://example.com) for more details.
        """.trimIndent()
        
        val result = parser.parse(content)
        
        // Should handle mixed formatting in real-world context
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<code>"), "Should contain code tags")
        assertTrue(result.parsedContent.contains("<s>"), "Should contain s tags")
        assertTrue(result.parsedContent.contains("<a href='https://example.com'>"), "Should contain links")
    }

    @Test
    fun `should handle table cells with nested formatting`() {
        val content = """
            | **Header** | *Description* | ~~Status~~ |
            |------------|---------------|------------|
            | **Bold**   | *Italic*      | `Code`     |
            | ~~Strike~~ | ***Both***    | [Link](url)|
        """.trimIndent()
        
        val result = parser.parse(content)
        
        // Should handle formatting in table cells
        assertTrue(result.parsedContent.contains("<table>"), "Should contain table")
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<s>"), "Should contain s tags")
        assertTrue(result.parsedContent.contains("<code>"), "Should contain code tags")
        assertTrue(result.parsedContent.contains("<a href='url'>"), "Should contain links")
    }

    @Test
    fun `should handle list items with nested formatting`() {
        val content = """
            - **Bold** item with *italic* text
            - ~~Strikethrough~~ item with `code`
            - ***Bold italic*** item with [link](url)
            - Regular item with **mixed** *formatting* and ~~strike~~
        """.trimIndent()
        
        val result = parser.parse(content)
        
        // Should handle formatting in list items
        assertTrue(result.parsedContent.contains("<ul>"), "Should contain unordered list")
        assertTrue(result.parsedContent.contains("<li>"), "Should contain list items")
        assertTrue(result.parsedContent.contains("<strong>"), "Should contain strong tags")
        assertTrue(result.parsedContent.contains("<em>"), "Should contain em tags")
        assertTrue(result.parsedContent.contains("<s>"), "Should contain s tags")
        assertTrue(result.parsedContent.contains("<code>"), "Should contain code tags")
        assertTrue(result.parsedContent.contains("<a href='url'>"), "Should contain links")
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should produce valid HTML for nested formatting`() {
        val content = "*italic with **bold** and ~~strikethrough~~ inside*"
        val result = parser.parse(content)
        
        val html = result.parsedContent
        
        // Basic HTML validation - check that tags are properly closed
        val openEmCount = html.split("<em>").size - 1
        val closeEmCount = html.split("</em>").size - 1
        val openStrongCount = html.split("<strong>").size - 1
        val closeStrongCount = html.split("</strong>").size - 1
        val openSCount = html.split("<s>").size - 1
        val closeSCount = html.split("</s>").size - 1
        
        assertEquals(openEmCount, closeEmCount, "All em tags should be properly closed")
        assertEquals(openStrongCount, closeStrongCount, "All strong tags should be properly closed")
        assertEquals(openSCount, closeSCount, "All s tags should be properly closed")
    }
}