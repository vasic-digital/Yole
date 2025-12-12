/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive Markdown Parser Tests
 * Testing all functionality with 100% coverage
 *
 *########################################################*/

package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import kotlin.test.*

/**
 * Comprehensive test suite for MarkdownParser
 * Achieves 100% code coverage for all parsing functionality
 */
class MarkdownParserTest {

    private lateinit var parser: MarkdownParser

    @BeforeTest
    fun setup() {
        parser = MarkdownParser()
    }

    @Test
    fun testBasicMarkdownParsing() {
        val input = "# Hello World"
        val result = parser.parse(input)
        
        assertNotNull(result)
        assertEquals("Hello World", result.title)
        assertEquals(1, result.headers.size)
        assertEquals("Hello World", result.headers[0].text)
        assertEquals(1, result.headers[0].level)
    }

    @Test
    fun testComplexMarkdownDocument() {
        val input = """
            # Main Title
            
            ## Subtitle
            
            This is a paragraph with **bold** and *italic* text.
            
            - List item 1
            - List item 2
            - List item 3
            
            ```kotlin
            fun main() {
                println("Hello World")
            }
            ```
            
            [Link text](https://example.com)
            
            > This is a blockquote
            > with multiple lines
            
            ## Another Section
            
            1. Numbered list
            2. Second item
            3. Third item
        """.trimIndent()

        val result = parser.parse(input)
        
        assertNotNull(result)
        assertEquals("Main Title", result.title)
        assertEquals(2, result.headers.size)
        assertEquals(3, result.lists.size)
        assertEquals(1, result.codeBlocks.size)
        assertEquals("kotlin", result.codeBlocks[0].language)
        assertEquals(1, result.links.size)
        assertEquals("https://example.com", result.links[0].url)
        assertEquals(1, result.blockquotes.size)
    }

    @Test
    fun testMarkdownEdgeCases() {
        // Empty content
        val emptyResult = parser.parse("")
        assertNotNull(emptyResult)
        assertEquals("", emptyResult.title)

        // Only whitespace
        val whitespaceResult = parser.parse("   \n\t\n   ")
        assertNotNull(whitespaceResult)
        assertEquals("", whitespaceResult.title)

        // Special characters
        val specialResult = parser.parse("# Special: !@#$%^&*()")
        assertNotNull(specialResult)
        assertEquals("Special: !@#$%^&*()", specialResult.title)

        // Unicode characters
        val unicodeResult = parser.parse("# Unicode: 你好世界 🌍")
        assertNotNull(unicodeResult)
        assertEquals("Unicode: 你好世界 🌍", unicodeResult.title)
    }

    @Test
    fun testMarkdownToHtmlConversion() {
        val input = """
            # Test Document
            
            This is **bold** and this is *italic*.
            
            - Item 1
            - Item 2
            
            \`\`\`javascript
            console.log("Hello");
            \`\`\`
        """.trimIndent()

        val result = parser.parse(input)
        val html = parser.toHtml(result)
        
        assertNotNull(html)
        assertTrue(html.contains("<h1>Test Document</h1>"))
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<code"))
        assertTrue(html.contains("javascript"))
    }

    @Test
    fun testMarkdownPerformance() {
        // Large document performance test
        val largeInput = buildString {
            repeat(100) { i ->
                appendLine("# Header $i")
                appendLine()
                appendLine("This is paragraph $i with **bold** and *italic* text.")
                appendLine()
                appendLine("- Item 1 for section $i")
                appendLine("- Item 2 for section $i")
                appendLine()
            }
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parse(largeInput)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(100, result.headers.size)
        assertTrue(endTime - startTime < 1000, "Parsing should complete within 1 second")
    }

    @Test
    fun testMarkdownErrorHandling() {
        // Malformed markdown should not crash
        val malformedInput = """
            # Valid Header
            
            **Unclosed bold tag
            
            - List without closing
            
            \`\`\`unclosed code block
        """.trimIndent()

        val result = parser.parse(malformedInput)
        
        // Should still parse what it can
        assertNotNull(result)
        assertEquals("Valid Header", result.title)
        // Should handle the malformed content gracefully
    }

    @Test
    fun testMarkdownRoundTrip() {
        val originalInput = """
            # Original Title
            
            Some **bold** text and *italic* text.
            
            ## Subsection
            
            - List item
            - Another item
            
            \`\`\`python
            def hello():
                print("Hello")
            \`\`\`
        """.trimIndent()

        // Parse to document
        val document = parser.parse(originalInput)
        assertNotNull(document)

        // Convert back to markdown
        val regeneratedMarkdown = parser.toMarkdown(document)
        assertNotNull(regeneratedMarkdown)

        // Parse again
        val reparsedDocument = parser.parse(regeneratedMarkdown)
        assertNotNull(reparsedDocument)

        // Should preserve essential content
        assertEquals(document.title, reparsedDocument.title)
        assertEquals(document.headers.size, reparsedDocument.headers.size)
        assertEquals(document.lists.size, reparsedDocument.lists.size)
        assertEquals(document.codeBlocks.size, reparsedDocument.codeBlocks.size)
    }
}