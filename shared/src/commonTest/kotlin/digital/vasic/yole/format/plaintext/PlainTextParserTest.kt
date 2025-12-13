/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Plain Text parser
 *
 *########################################################*/
package digital.vasic.yole.format.plaintext

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.format.ParsedDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse


/**
 * Comprehensive unit tests for Plain Text format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic Plain Text parsing (simple text content)
 * - Various text encodings and line endings
 * - Plain text metadata and properties
 * - Round-trip parsing (parse → format → parse)
 * - Edge cases (empty files, special characters)
 * - Performance benchmarks
 * - HTML conversion
 */
class PlainTextParserTest {

    private val parser = PlaintextParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Plain Text format by txt extension`() {
        val format = FormatRegistry.getByExtension(".txt")

        assertNotNull(format)
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
        assertEquals("Plain Text", format.name)
    }

    @Test
    fun `should detect Plain Text format by text extension`() {
        val format = FormatRegistry.getByExtension(".text")

        assertNotNull(format)
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
    }

    @Test
    fun `should detect Plain Text format by log extension`() {
        val format = FormatRegistry.getByExtension(".log")

        assertNotNull(format)
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
    }

    @Test
    fun `should detect Plain Text format by filename`() {
        val format = FormatRegistry.detectByFilename("readme.txt")

        assertNotNull(format)
        assertEquals(TextFormat.ID_PLAINTEXT, format.id)
    }

    @Test
    fun `should support all Plain Text extensions`() {
        val extensions = listOf(".txt", ".text", ".log")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_PLAINTEXT, format.id)
        }
    }

    // ==================== Basic Plain Text Parsing Tests ====================

    @Test
    fun `should parse basic Plain Text document structure`() {
        val content = """
            This is a simple plain text document.
            It contains multiple lines of text.
            Each line represents a paragraph or sentence.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("plain", result.metadata["type"])
        assertEquals("3", result.metadata["lines"])
        assertEquals("120", result.metadata["characters"])
    }

    @Test
    fun `should parse Plain Text with different file extensions`() {
        val content = """
            Application Log File
            ====================
            
            2024-01-01 10:00:00 INFO Application started
            2024-01-01 10:00:05 DEBUG Loading configuration
            2024-01-01 10:00:10 INFO Configuration loaded successfully
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "app.log"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("plain", result.metadata["type"])
        assertEquals(".log", result.metadata["extension"])
        assertEquals("6", result.metadata["lines"])
    }

    @Test
    fun `should parse Plain Text with no extension`() {
        val content = "This is a plain text file without extension."
        
        val result = parser.parse(content, mapOf("filename" to "README"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("plain", result.metadata["type"])
        assertEquals("", result.metadata["extension"])
        assertEquals("1", result.metadata["lines"])
        assertEquals("44", result.metadata["characters"])
    }

    // ==================== Text Encodings and Line Endings Tests ====================

    @Test
    fun `should handle Unix line endings`() {
        val content = "Line 1\nLine 2\nLine 3"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["lines"])
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle Windows line endings`() {
        val content = "Line 1\r\nLine 2\r\nLine 3"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["lines"])
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle mixed line endings`() {
        val content = "Line 1\nLine 2\r\nLine 3\nLine 4\r\nLine 5"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("5", result.metadata["lines"])
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle special Unicode characters`() {
        val content = """
            Unicode test: 你好世界 🌍
            Special chars: café, naïve, résumé
            Math symbols: ∑ ∏ ∫ ∂ ∇
            Emoji: 🚀 💻 📱 ⚡
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("4", result.metadata["lines"])
        assertTrue(result.rawContent.contains("你好世界"))
        assertTrue(result.rawContent.contains("🌍"))
        assertTrue(result.rawContent.contains("café"))
        assertTrue(result.rawContent.contains("∑"))
        assertTrue(result.rawContent.contains("🚀"))
    }

    @Test
    fun `should handle ASCII control characters`() {
        val content = "Line 1\tTab separated\nLine 2\tAnother tab\nLine 3\tFinal tab"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["lines"])
        assertTrue(result.rawContent.contains("\t"))
        assertEquals(content, result.rawContent)
    }

    // ==================== Plain Text Type Detection Tests ====================

    @Test
    fun `should detect HTML type by extension`() {
        val content = "<html><body><h1>Hello World</h1></body></html>"
        
        val result = parser.parse(content, mapOf("filename" to "index.html"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("html", result.metadata["type"])
        assertEquals(".html", result.metadata["extension"])
    }

    @Test
    fun `should detect code type by extension`() {
        val content = """
            fun main() {
                println("Hello, World!")
            }
        """.trimIndent()
        
        val result = parser.parse(content, mapOf("filename" to "main.kt"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("code", result.metadata["type"])
        assertEquals(".kt", result.metadata["extension"])
    }

    @Test
    fun `should detect JSON type by extension`() {
        val content = """{"name": "test", "value": 123}"""
        
        val result = parser.parse(content, mapOf("filename" to "config.json"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("json", result.metadata["type"])
        assertEquals(".json", result.metadata["extension"])
    }

    @Test
    fun `should detect XML type by extension`() {
        val content = """<root><item>test</item></root>"""
        
        val result = parser.parse(content, mapOf("filename" to "data.xml"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("xml", result.metadata["type"])
        assertEquals(".xml", result.metadata["extension"])
    }

    @Test
    fun `should pretty print JSON content`() {
        val content = """{"name":"test","value":123,"nested":{"key":"value"}}"""
        
        val result = parser.parse(content, mapOf("filename" to "config.json"))

        assertNotNull(result)
        assertEquals("json", result.metadata["type"])
        // JSON should be pretty-printed
        assertTrue(result.parsedContent.contains("\n"))
        assertTrue(result.parsedContent.contains("  ")) // Indentation
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty document`() {
        val content = ""

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("0", result.metadata["characters"])
        assertEquals("1", result.metadata["lines"])
    }

    @Test
    fun `should handle document with only whitespace`() {
        val content = "   \n  \n   \n"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("4", result.metadata["lines"])
        assertTrue(result.metadata["characters"]!!.toInt() > 0)
    }

    @Test
    fun `should handle document with only newlines`() {
        val content = "\n\n\n\n"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("5", result.metadata["lines"])
    }

    @Test
    fun `should handle very long lines`() {
        val longLine = "A".repeat(1000)
        val content = "Short line\n$longLine\nAnother short line"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["lines"])
        assertEquals(content.length.toString(), result.metadata["characters"])
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle very large number of lines`() {
        val content = buildString {
            repeat(100) { i ->
                appendLine("This is line number ${i + 1}")
            }
        }.trimEnd()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("100", result.metadata["lines"])
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle special regex characters`() {
        val content = """
            Special chars: . * + ? ^ $ { } ( ) [ ] | \
            More special: \d \w \s \D \W \S
            Quantifiers: * + ? {n} {n,} {n,m}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["lines"])
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle XML-like content in plain text`() {
        val content = """
            This is plain text that happens to contain <tags>.
            It might look like <xml>content</xml> but it's not.
            Even <html><body>content</body></html> should be treated as plain text.
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "note.txt"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("plain", result.metadata["type"])
        assertEquals(content, result.rawContent)
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert plain text to HTML`() {
        val content = """
            This is plain text.
            It should be converted to HTML.
            With proper escaping and formatting.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='plaintext'>"))
        assertTrue(html.contains("<pre"))
        assertTrue(html.contains("white-space: pre-wrap"))
        assertTrue(html.contains("font-family: monospace"))
    }

    @Test
    fun `should escape HTML entities in plain text`() {
        val content = """
            This contains <script>alert('xss')</script>
            And some <b>bold</b> tags.
            Don't forget about < & > characters.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertFalse(html.contains("<script>alert('xss')</script>"))
        assertFalse(html.contains("<b>bold</b>"))
        assertTrue(html.contains("&lt;"))
        assertTrue(html.contains("&gt;"))
        assertTrue(html.contains("&amp;"))
    }

    @Test
    fun `should convert code to HTML with syntax highlighting`() {
        val content = """
            fun main() {
                println("Hello, World!")
            }
        """.trimIndent()

        val document = parser.parse(content, mapOf("filename" to "main.kt"))
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='plaintext code-block'>"))
        assertTrue(html.contains("<pre><code class='language-kotlin'>"))
        assertTrue(html.contains("fun main()"))
    }

    @Test
    fun `should convert HTML content as-is`() {
        val content = """<html><body><h1>Hello World</h1></body></html>"""

        val document = parser.parse(content, mapOf("filename" to "index.html"))
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertEquals(content, html) // HTML should be returned as-is
    }

    @Test
    fun `should apply dark mode styles`() {
        val content = "Simple plain text content"

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("<div class='plaintext'>"))
        assertTrue(html.contains("<pre"))
    }

    // ==================== Round-trip Parsing Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = """
            This is a plain text document.
            It has multiple lines.
            Each line is preserved exactly.
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

    @Test
    fun `should preserve metadata through round-trip`() {
        val originalContent = """
            Application: MyApp
            Version: 1.0.0
            Description: A sample application
        """.trimIndent()

        val firstParse = parser.parse(originalContent, mapOf("filename" to "app.txt"))
        val secondParse = parser.parse(firstParse.rawContent, mapOf("filename" to "app.txt"))
        
        assertEquals(firstParse.metadata, secondParse.metadata)
        assertEquals(firstParse.format.id, secondParse.format.id)
    }

    @Test
    fun `should preserve file type through round-trip`() {
        val originalContent = """{"test": "data", "number": 42}"""

        val firstParse = parser.parse(originalContent, mapOf("filename" to "config.json"))
        val secondParse = parser.parse(firstParse.rawContent, mapOf("filename" to "config.json"))
        
        assertEquals(firstParse.metadata["type"], secondParse.metadata["type"])
        assertEquals(firstParse.metadata["extension"], secondParse.metadata["extension"])
    }

    // ==================== Performance Benchmarks ====================

    @Test
    fun `should parse large Plain Text documents efficiently`() {
        // Create a large document with many lines
        val largeContent = buildString {
            repeat(1000) { lineIndex ->
                appendLine("This is line number ${lineIndex + 1}. It contains some sample text for testing purposes.")
            }
        }.trimEnd()

        val startTime = System.currentTimeMillis()
        val result = parser.parse(largeContent)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("1000", result.metadata["lines"])
        
        // Performance assertion - should parse in reasonable time (less than 1 second for this size)
        val parseTime = endTime - startTime
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took: ${parseTime}ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        // Create a large document
        val largeContent = buildString {
            repeat(500) { lineIndex ->
                appendLine("Line ${lineIndex + 1}: This is sample text content that will be converted to HTML format.")
            }
        }.trimEnd()

        val document = parser.parse(largeContent)
        
        val startTime = System.currentTimeMillis()
        val html = parser.toHtml(document, lightMode = true)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(html)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("<div class='plaintext'>"))
        
        // Performance assertion - should convert in reasonable time (less than 500ms)
        val conversionTime = endTime - startTime
        assertTrue(conversionTime < 500, "HTML conversion should complete within 500ms, took: ${conversionTime}ms")
    }

    @Test
    fun `should handle documents with very long lines efficiently`() {
        // Create a document with very long lines
        val longContent = buildString {
            repeat(100) { lineIndex ->
                append("Line ${lineIndex + 1}: ")
                append("A".repeat(500)) // 500 characters per line
                appendLine()
            }
        }.trimEnd()

        val startTime = System.currentTimeMillis()
        val result = parser.parse(longContent)
        val html = parser.toHtml(result, lightMode = true)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertNotNull(html)
        assertEquals("100", result.metadata["lines"])
        
        // Performance assertion for parsing + HTML conversion
        val totalTime = endTime - startTime
        assertTrue(totalTime < 1000, "Parsing + HTML conversion should complete within 1 second, took: ${totalTime}ms")
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with ParserRegistry`() {
        // Register the parser
        ParserRegistry.register(parser)
        
        // Get parser from registry
        val registryParser = ParserRegistry.getParser(TextFormat.ID_PLAINTEXT)
        
        assertNotNull(registryParser)
        assertEquals(TextFormat.ID_PLAINTEXT, registryParser.supportedFormat.id)
        
        // Test that it works
        val content = "Simple plain text content"
        val result = registryParser.parse(content)
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val plainTextFormat = allFormats.find { it.id == TextFormat.ID_PLAINTEXT }

        assertNotNull(plainTextFormat)
        assertEquals("Plain Text", plainTextFormat.name)
        assertEquals(".txt", plainTextFormat.defaultExtension)
        assertTrue(plainTextFormat.extensions.contains(".txt"))
        assertTrue(plainTextFormat.extensions.contains(".text"))
        assertTrue(plainTextFormat.extensions.contains(".log"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should parse complex Plain Text document`() {
        val content = """
            ================================================================================
            SYSTEM LOG FILE - Application Server
            ================================================================================
            
            Date: 2024-01-15
            Time: 14:30:00 UTC
            Server: app-server-01.example.com
            Version: 2.1.0
            
            ================================================================================
            CONFIGURATION
            ================================================================================
            
            Server Port: 8080
            SSL Enabled: true
            Max Connections: 1000
            Timeout: 30 seconds
            
            ================================================================================
            ERROR LOG
            ================================================================================
            
            [ERROR] 2024-01-15 14:25:15 - Database connection failed
            [ERROR] 2024-01-15 14:25:16 - Retrying connection...
            [ERROR] 2024-01-15 14:25:18 - Connection restored
            
            ================================================================================
            PERFORMANCE METRICS
            ================================================================================
            
            Average Response Time: 125ms
            95th Percentile: 250ms
            99th Percentile: 500ms
            Memory Usage: 2.1GB / 4GB
            CPU Usage: 45%
            
            ================================================================================
            END OF LOG
            ================================================================================
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "server.log"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("plain", result.metadata["type"])
        assertEquals(".log", result.metadata["extension"])
        assertEquals("32", result.metadata["lines"])
        
        // Verify HTML contains expected elements
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("plaintext"))
        assertTrue(html.contains("<pre"))
        assertTrue(html.contains("font-family: monospace"))
    }

    @Test
    fun `should handle source code files correctly`() {
        val content = """
            package digital.vasic.yole.format.plaintext
            
            import digital.vasic.yole.format.TextParser
            import digital.vasic.yole.format.ParsedDocument
            
            class PlaintextParser : TextParser {
                override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                    // Implementation here
                }
            }
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "PlaintextParser.kt"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("code", result.metadata["type"])
        assertEquals(".kt", result.metadata["extension"])
        assertEquals("12", result.metadata["lines"])
        
        // Verify HTML contains code highlighting
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("<div class='plaintext code-block'>"))
        assertTrue(html.contains("<pre><code class='language-kotlin'>"))
    }

    @Test
    fun `should handle mixed content types`() {
        val content = """
            #!/bin/bash
            # This is a shell script
            echo "Hello, World!"
            
            # Check if file exists
            if [ -f "config.json" ]; then
                echo "Config file found"
            else
                echo "Config file not found"
            fi
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "script.sh"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_PLAINTEXT, result.format.id)
        assertEquals("code", result.metadata["type"])
        assertEquals(".sh", result.metadata["extension"])
        assertEquals("bash", result.metadata["extension"]) // Should map to bash language
        
        // Verify HTML contains bash syntax highlighting
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("<pre><code class='language-bash'>"))
    }
}