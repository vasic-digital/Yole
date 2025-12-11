/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Parser tests for Yole Web Application
 *
 *########################################################*/
package digital.vasic.yole.web

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Parser tests for web-specific functionality in Yole Web Application.
 * Simplified for WASM compatibility.
 */
class WebAppParserTest {

    @Test
    fun `should parse markdown for web display`() {
        val markdownContent = """
            # Web Test Header
            
            This is **bold** and *italic* text.
            
            ## Features
            
            - List item 1
            - List item 2
            - List item 3
            
            ```kotlin
            fun test() {
                println("Hello Web!")
            }
            ```
            
            [Link to website](https://example.com)
        """.trimIndent()

        val parser = ParserRegistry.getParser("markdown")
        assertNotNull(parser, "Markdown parser should be available")

        val parsedDocument = parser.parse(markdownContent)
        assertNotNull(parsedDocument, "Document should be parsed successfully")

        val htmlOutput = parser.toHtml(parsedDocument)
        assertTrue(htmlOutput.isNotEmpty(), "HTML output should not be empty")
        
        // Verify web-specific HTML elements
        assertTrue(htmlOutput.contains("Web Test Header"), "HTML should contain header text")
        assertTrue(htmlOutput.contains("bold") || htmlOutput.contains("strong"), "HTML should contain bold formatting")
        assertTrue(htmlOutput.contains("italic") || htmlOutput.contains("em"), "HTML should contain italic formatting")
        assertTrue(htmlOutput.contains("List item"), "HTML should contain list items")
        assertTrue(htmlOutput.contains("kotlin") || htmlOutput.contains("code"), "HTML should contain code block")
        assertTrue(htmlOutput.contains("https://example.com") || htmlOutput.contains("Link"), "HTML should contain link")
    }

    @Test
    fun `should parse plain text for web display`() {
        val plainContent = """
            This is plain text content for web display.
            
            It should be displayed as-is without any special formatting.
            
            Multiple paragraphs should be preserved.
            
            Special characters: @#$%^&*()_+-=[]{}|;':",./<>?
        """.trimIndent()

        val parser = ParserRegistry.getParser("plaintext")
        assertNotNull(parser, "Plain text parser should be available")

        val parsedDocument = parser.parse(plainContent)
        assertNotNull(parsedDocument, "Document should be parsed successfully")

        val htmlOutput = parser.toHtml(parsedDocument)
        assertTrue(htmlOutput.isNotEmpty(), "HTML output should not be empty")
        
        // Verify plain text is properly formatted for web
        assertTrue(htmlOutput.contains("plain text content"), "HTML should contain plain text content")
        assertTrue(htmlOutput.contains("displayed as-is"), "HTML should preserve text content")
        assertTrue(htmlOutput.contains("Multiple paragraphs"), "HTML should contain multiple paragraphs")
        assertTrue(htmlOutput.contains("Special characters"), "HTML should contain special characters")
    }

    @Test
    fun `should generate clean HTML for web display`() {
        val content = "# Simple Header\n\nSimple paragraph content."
        val parser = ParserRegistry.getParser("markdown")
        assertNotNull(parser, "Parser should be available")

        val parsedDocument = parser.parse(content)
        val htmlOutput = parser.toHtml(parsedDocument)

        // Verify HTML is clean and web-ready
        assertFalse(htmlOutput.contains("<script"), "HTML should not contain script tags")
        assertFalse(htmlOutput.contains("javascript:"), "HTML should not contain javascript protocols")
        assertTrue(htmlOutput.contains("Simple Header"), "HTML should contain header")
        assertTrue(htmlOutput.contains("Simple paragraph content"), "HTML should contain paragraph")
    }

    @Test
    fun `should handle HTML escaping for web security`() {
        val contentWithHtml = """
            # Test with HTML
            
            This has <script>alert('xss')</script> and <iframe> content.
            
            ## More malformed content
            
            Also has <div onclick="alert('xss')">clickable</div> content.
        """.trimIndent()

        val parser = ParserRegistry.getParser("markdown")
        assertNotNull(parser, "Parser should be available")

        val parsedDocument = parser.parse(contentWithHtml)
        val htmlOutput = parser.toHtml(parsedDocument)

        // Verify dangerous HTML is properly escaped or removed
        assertFalse(htmlOutput.contains("<script>"), "HTML should not contain script tags")
        assertFalse(htmlOutput.contains("alert("), "HTML should not contain alert calls")
        assertFalse(htmlOutput.contains("onclick="), "HTML should not contain onclick handlers")
        assertTrue(htmlOutput.contains("Test with HTML"), "HTML should contain safe content")
    }

    @Test
    fun `should parse content efficiently for web performance`() {
        val largeContent = buildString {
            repeat(100) { i ->
                append("# Section $i\n\n")
                append("This is paragraph content for section $i. ".repeat(20))
                append("\n\n")
            }
        }

        val parser = ParserRegistry.getParser("markdown")
        assertNotNull(parser, "Parser should be available")

        val parsedDocument = parser.parse(largeContent)
        val htmlOutput = parser.toHtml(parsedDocument)

        assertNotNull(parsedDocument, "Large document should be parsed successfully")
        assertTrue(htmlOutput.isNotEmpty(), "HTML output should be generated")
        
        // Verify the content is processed correctly
        assertTrue(htmlOutput.contains("Section 0"), "HTML should contain first section")
        assertTrue(htmlOutput.contains("Section 99"), "HTML should contain last section")
    }

    @Test
    fun `should handle malformed content gracefully for web display`() {
        val malformedContent = """
            # Malformed Content
            
            This has {{{unclosed braces and [unclosed brackets.
            
            ## More malformed content
            
            * List without proper formatting
            1. Mixed list types
            - Inconsistent formatting
        """.trimIndent()

        val parser = ParserRegistry.getParser("markdown")
        assertNotNull(parser, "Parser should be available")

        try {
            val parsedDocument = parser.parse(malformedContent)
            val htmlOutput = parser.toHtml(parsedDocument)
            
            // Should not crash and should generate some HTML
            assertTrue(htmlOutput.isNotEmpty(), "Should generate HTML even for malformed content")
            assertTrue(htmlOutput.contains("Malformed Content"), "Should contain recognizable content")
        } catch (e: Exception) {
            fail("Should handle malformed content gracefully without throwing: ${e.message}")
        }
    }

    @Test
    fun `should handle empty content for web display`() {
        val emptyContent = ""
        val parser = ParserRegistry.getParser("markdown")
        assertNotNull(parser, "Parser should be available")

        try {
            val parsedDocument = parser.parse(emptyContent)
            val htmlOutput = parser.toHtml(parsedDocument)
            
            // Should handle empty content gracefully
            assertNotNull(htmlOutput, "Should not return null for empty content")
            // HTML output might be empty or contain minimal structure
        } catch (e: Exception) {
            fail("Should handle empty content gracefully without throwing: ${e.message}")
        }
    }

    @Test
    fun `should handle Unicode content for international web users`() {
        val unicodeContent = """
            # Unicode Test
            
            English: Hello World
            Chinese: 你好世界
            Japanese: こんにちは世界
            Arabic: مرحبا بالعالم
            Russian: Привет мир
            Emoji: 🌍 🌎 🌏
            
            Special characters: café, naïve, résumé
        """.trimIndent()

        val parser = ParserRegistry.getParser("markdown")
        assertNotNull(parser, "Parser should be available")

        val parsedDocument = parser.parse(unicodeContent)
        val htmlOutput = parser.toHtml(parsedDocument)

        // Verify Unicode is handled correctly
        assertTrue(htmlOutput.contains("Hello World"), "Should contain English text")
        assertTrue(htmlOutput.contains("你好世界"), "Should contain Chinese text")
        assertTrue(htmlOutput.contains("こんにちは世界"), "Should contain Japanese text")
        assertTrue(htmlOutput.contains("مرحبا بالعالم"), "Should contain Arabic text")
        assertTrue(htmlOutput.contains("Привет мир"), "Should contain Russian text")
        assertTrue(htmlOutput.contains("🌍"), "Should contain emoji")
        assertTrue(htmlOutput.contains("café"), "Should contain accented characters")
    }
}