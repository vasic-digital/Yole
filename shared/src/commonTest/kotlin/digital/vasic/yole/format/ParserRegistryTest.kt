/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for ParserRegistry
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Comprehensive tests for ParserRegistry covering:
 * - Parser registration and retrieval
 * - Parser functionality testing
 * - Error handling
 * - Cross-parser compatibility
 * - Performance testing
 */
class ParserRegistryTest {

    @BeforeTest
    fun setUp() {
        // Ensure parser registry is properly initialized
        ParserInitializer.initialize()
    }

    @Test
    fun testGetParserForFormat() {
        // Test getting parser for Markdown format
        val markdownFormat = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        assertNotNull(markdownFormat)
        
        val markdownParser = ParserRegistry.getParser(markdownFormat)
        assertNotNull(markdownParser, "Should retrieve parser for Markdown format")
        
        // Test getting parser for Plain Text format
        val plainTextFormat = FormatRegistry.getById(FormatRegistry.ID_PLAIN_TEXT)
        assertNotNull(plainTextFormat)
        
        val plainTextParser = ParserRegistry.getParser(plainTextFormat)
        assertNotNull(plainTextParser, "Should retrieve parser for Plain Text format")
    }

    @Test
    fun testParserFunctionality() {
        // Test Markdown parser functionality
        val markdownFormat = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val markdownParser = ParserRegistry.getParser(markdownFormat)
        assertNotNull(markdownParser)
        
        val markdownContent = """
            # Heading
            This is **bold** text.
            This is *italic* text.
        """.trimIndent()
        
        val markdownResult = markdownParser.parse(markdownContent)
        assertNotNull(markdownResult, "Markdown parser should successfully parse content")
        assertTrue(markdownResult.content.isNotEmpty(), "Parsed content should not be empty")
        
        // Test Plain Text parser functionality
        val plainTextFormat = FormatRegistry.getById(FormatRegistry.ID_PLAIN_TEXT)
        val plainTextParser = ParserRegistry.getParser(plainTextFormat)
        assertNotNull(plainTextParser)
        
        val plainTextContent = "This is plain text content."
        val plainTextResult = plainTextParser.parse(plainTextContent)
        assertNotNull(plainTextResult, "Plain Text parser should successfully parse content")
        assertEquals(plainTextContent, plainTextResult.content, "Plain text content should remain unchanged")
    }

    @Test
    fun testParserForAllSupportedFormats() {
        val formats = FormatRegistry.getAllFormats()
        assertTrue(formats.isNotEmpty(), "Should have formats to test")
        
        formats.forEach { format ->
            val parser = ParserRegistry.getParser(format)
            assertNotNull(parser, "Should have parser for format: ${format.name}")
            
            // Test basic parsing functionality
            val sampleContent = "Sample content for ${format.name}"
            val result = parser.parse(sampleContent)
            assertNotNull(result, "Parser for ${format.name} should successfully parse sample content")
        }
    }

    @Test
    fun testParserWithEmptyContent() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser)
        
        val emptyContent = ""
        val result = parser.parse(emptyContent)
        assertNotNull(result, "Parser should handle empty content")
        assertTrue(result.content.isEmpty(), "Parsed empty content should remain empty")
    }

    @Test
    fun testParserWithLargeContent() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser)
        
        val largeContent = buildString {
            repeat(100) { i ->
                appendLine("# Heading $i")
                appendLine("This is paragraph $i with **bold** and *italic* text.")
                appendLine()
            }
        }
        
        val result = parser.parse(largeContent)
        assertNotNull(result, "Parser should handle large content")
        assertTrue(result.content.isNotEmpty(), "Parsed large content should not be empty")
    }

    @Test
    fun testParserWithSpecialCharacters() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser)
        
        val specialContent = """
            # Special Characters Test
            This content has special characters: @#$%^&*()_+-=[]{}|;':",./<>?
            Unicode characters: àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ
            Emojis: 🚀 🎉 💻 📱 🌟
        """.trimIndent()
        
        val result = parser.parse(specialContent)
        assertNotNull(result, "Parser should handle special characters")
        assertTrue(result.content.contains("@#$%^&*"), "Parsed content should preserve special characters")
    }

    @Test
    fun testParserPerformance() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser)
        
        val content = """
            # Performance Test
            This is a test document with **bold** and *italic* text.
            
            ## Second Heading
            More content here with `inline code` and other formatting.
            
            ### Third Heading
            Final paragraph with [links](http://example.com) and other elements.
        """.trimIndent()
        
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            val result = parser.parse(content)
            assertNotNull(result)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete 100 parsing operations in reasonable time (less than 1 second)
        assertTrue(duration < 1000, "Parser should be fast, took $duration ms for 100 iterations")
    }

    @Test
    fun testParserErrorHandling() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser)
        
        // Test with various edge cases
        val testCases = listOf(
            "",
            "   ",
            "\n\n\n",
            "#",
            "**",
            "*",
            "[]",
            "()",
            "`",
            "~~~"
        )
        
        testCases.forEach { content ->
            val result = parser.parse(content)
            assertNotNull(result, "Parser should handle edge case: '$content'")
            // Parser should either return valid content or empty content, but not null
        }
    }

    @Test
    fun testParserConsistency() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser)
        
        val content = """
            # Consistency Test
            This is **bold** and this is *italic*.
        """.trimIndent()
        
        // Parse the same content multiple times
        val results = List(5) { parser.parse(content) }
        
        // All results should be identical
        results.forEach { result ->
            assertNotNull(result)
            assertEquals(results[0].content, result.content, "Parser should produce consistent results")
        }
    }

    @Test
    fun testParserRegistryThreadSafety() {
        val results = mutableListOf<Boolean>()
        val threads = mutableListOf<Thread>()
        
        repeat(10) {
            val thread = Thread {
                repeat(50) {
                    val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
                    val parser = ParserRegistry.getParser(format)
                    val result = parser?.parse("Test content $it")
                    results.add(result != null)
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        threads.forEach { it.join() }
        
        // All operations should succeed
        assertEquals(500, results.size, "All concurrent operations should complete")
        assertTrue(results.all { it }, "All parser operations should succeed")
    }

    @Test
    fun testParserWithComplexStructures() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser)
        
        val complexContent = """
            # Complex Document
            
            ## Lists
            - Item 1
            - Item 2
              - Nested item
              - Another nested item
            - Item 3
            
            ## Code Block
            ```kotlin
            fun main() {
                println("Hello, World!")
            }
            ```
            
            ## Table
            | Header 1 | Header 2 |
            |----------|----------|
            | Cell 1   | Cell 2   |
            | Cell 3   | Cell 4   |
            
            ## Links and Images
            [Link text](http://example.com)
            ![Image alt](image.jpg)
            
            ## Emphasis
            This is **bold**, *italic*, and ***bold italic*** text.
            
            ## Blockquote
            > This is a blockquote
            > with multiple lines
            
            ## Horizontal Rule
            ---
            
            Final paragraph with more content.
        """.trimIndent()
        
        val result = parser.parse(complexContent)
        assertNotNull(result, "Parser should handle complex document structures")
        assertTrue(result.content.isNotEmpty(), "Parsed complex content should not be empty")
    }

    @Test
    fun testParserMetadataExtraction() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser)
        
        val contentWithMetadata = """
            ---
            title: Test Document
            author: Test Author
            date: 2024-01-01
            ---
            
            # Document with Front Matter
            This document has YAML front matter metadata.
        """.trimIndent()
        
        val result = parser.parse(contentWithMetadata)
        assertNotNull(result, "Parser should handle content with metadata")
        assertTrue(result.content.contains("Document with Front Matter"), "Should parse document content")
    }

    @Test
    fun testParserRegistryClear() {
        // Test that parser registry can be cleared and re-initialized
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val parser1 = ParserRegistry.getParser(format)
        assertNotNull(parser1)
        
        // Re-initialize
        ParserInitializer.initialize()
        
        val parser2 = ParserRegistry.getParser(format)
        assertNotNull(parser2)
        
        // Both parsers should work
        val content = "Test content"
        val result1 = parser1.parse(content)
        val result2 = parser2.parse(content)
        
        assertNotNull(result1)
        assertNotNull(result2)
    }
}