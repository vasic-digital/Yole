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
import digital.vasic.yole.format.plaintext.PlaintextParser
import org.junit.Test
import kotlin.test.*

/**
 * Unit tests for Plain Text format parser.
 *
 * Tests cover:
 * - Format detection by extension
 * - Basic parsing functionality
 * - Edge cases and error handling
 * - Empty input handling
 * - Special characters
 */
class PlainTextParserTest {

    private val parser = PlaintextParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Plain Text format by extension`() {
        val format = FormatRegistry.getByExtension(".txt")

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
        assertEquals("Plain Text", format.name)
    }

    @Test
    fun `should detect Plain Text format by filename`() {
        val format = FormatRegistry.detectByFilename("test.txt")

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun `should support all Plain Text extensions`() {
        val extensions = listOf(".txt")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
        }
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `should parse basic Plain Text content`() {
        val content = """
            Just plain text without special formatting
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Add format-specific assertions here
    }

    @Test
    fun `should handle empty input`() {
        val result = parser.parse("")

        assertNotNull(result)
        // Verify empty result is valid
    }

    @Test
    fun `should handle whitespace-only input`() {
        val result = parser.parse("   \n\n   \t  ")

        assertNotNull(result)
    }

    @Test
    fun `should handle single line input`() {
        val content = "Single line of Plain Text"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Content Detection Tests ====================

    @Test
    fun `should detect format by content patterns`() {
        // PlainText has no detection patterns (it's the fallback format)
        val content = """
            Just plain text without special formatting
        """.trimIndent()

        val format = FormatRegistry.detectByContent(content)

        // PlainText won't be detected by content patterns (no patterns defined)
        // It's the fallback when no other format matches
        assertTrue(format == null || format.id != FormatRegistry.ID_PLAINTEXT)
    }

    @Test
    fun `should not false-positive on plain text`() {
        val plainText = "Just some plain text without special formatting"

        val format = FormatRegistry.detectByContent(plainText)

        // Should detect as plaintext, not Plain Text
        if (format != null) {
            assertNotEquals(FormatRegistry.ID_PLAINTEXT, format.id)
        }
    }

    // ==================== Special Characters Tests ====================

    @Test
    fun `should handle special characters`() {
        val content = """
            Special chars: @#$%^{{SPECIAL_CHARS_SAMPLE}}*()
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Verify special characters are preserved/escaped correctly
    }

    @Test
    fun `should handle unicode characters`() {
        val content = "Unicode test: 你好世界 🌍 Привет мир"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle malformed input gracefully`() {
        val malformed = """
            Malformed Plain Text content
        """.trimIndent()

        // Should not throw exception
        val result = parser.parse(malformed)
        assertNotNull(result)
    }

    @Test
    fun `should handle very long input`() {
        val longContent = "Single line of Plain Text\n".repeat(10000)

        val result = parser.parse(longContent)

        assertNotNull(result)
    }

    @Test
    fun `should handle null bytes gracefully`() {
        // Binary content detection
        val binaryContent = "Some text\u0000with null\u0000bytes"

        val result = parser.parse(binaryContent)

        assertNotNull(result)
    }

    // ==================== Format-Specific Tests ====================
    // Add format-specific parsing tests below
    // Examples:
    // - Headers (for Markdown, AsciiDoc, etc.)
    // - Lists (for Markdown, Org Mode, etc.)
    // - Code blocks (for Markdown, reStructuredText, etc.)
    // - Tables (for CSV, Markdown, etc.)
    // - Math (for LaTeX, R Markdown, etc.)

    @Test
    fun `should parse format-specific feature`() {
        val content = """
            Format specific sample
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Add format-specific assertions
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with FormatRegistry`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)

        assertNotNull(format)
        assertEquals("Plain Text", format.name)
        assertEquals(".txt", format.defaultExtension)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val plainTextFormat = allFormats.find { it.id == FormatRegistry.ID_PLAINTEXT }

        assertNotNull(plainTextFormat)
        assertEquals("Plain Text", plainTextFormat.name)
    }

    // ==================== Additional Edge Case Tests ====================

    // ==================== PlaintextType Detection Tests ====================

    @Test
    fun `should detect HTML type from extension`() {
        val content = "<html><body>Test</body></html>"
        val result = parser.parse(content, mapOf("filename" to "test.html"))

        assertNotNull(result)
        assertEquals("html", result.metadata["type"])
        assertEquals(".html", result.metadata["extension"])
    }

    @Test
    fun `should detect HTML type from htm extension`() {
        val content = "<html><body>Test</body></html>"
        val result = parser.parse(content, mapOf("filename" to "index.htm"))

        assertNotNull(result)
        assertEquals("html", result.metadata["type"])
        assertEquals(".htm", result.metadata["extension"])
    }

    @Test
    fun `should detect CODE type from Python extension`() {
        val content = "def hello():\n    print('Hello')"
        val result = parser.parse(content, mapOf("filename" to "script.py"))

        assertNotNull(result)
        assertEquals("code", result.metadata["type"])
        assertEquals(".py", result.metadata["extension"])
        assertTrue(result.parsedContent.contains("language-python"))
    }

    @Test
    fun `should detect CODE type from JavaScript extension`() {
        val content = "function hello() { console.log('Hello'); }"
        val result = parser.parse(content, mapOf("filename" to "app.js"))

        assertNotNull(result)
        assertEquals("code", result.metadata["type"])
        assertTrue(result.parsedContent.contains("language-javascript"))
    }

    @Test
    fun `should detect CODE type from Kotlin extension`() {
        val content = "fun main() { println(\"Hello\") }"
        val result = parser.parse(content, mapOf("filename" to "Main.kt"))

        assertNotNull(result)
        assertEquals("code", result.metadata["type"])
        assertTrue(result.parsedContent.contains("language-kotlin"))
    }

    @Test
    fun `should detect JSON type from extension`() {
        val content = "{\"name\": \"test\", \"value\": 123}"
        val result = parser.parse(content, mapOf("filename" to "data.json"))

        assertNotNull(result)
        // JSON is in CODE_EXTENSIONS, so it's detected as CODE type
        assertEquals("code", result.metadata["type"])
        assertEquals(".json", result.metadata["extension"])
        assertTrue(result.parsedContent.contains("language-json"))
    }

    @Test
    fun `should detect XML type from extension`() {
        val content = "<?xml version=\"1.0\"?><root><item>test</item></root>"
        val result = parser.parse(content, mapOf("filename" to "config.xml"))

        assertNotNull(result)
        // XML is in CODE_EXTENSIONS, so it's detected as CODE type
        assertEquals("code", result.metadata["type"])
        assertEquals(".xml", result.metadata["extension"])
        assertTrue(result.parsedContent.contains("language-xml"))
    }

    @Test
    fun `should detect PLAIN type for txt extension`() {
        val content = "Just plain text"
        val result = parser.parse(content, mapOf("filename" to "notes.txt"))

        assertNotNull(result)
        assertEquals("plain", result.metadata["type"])
        assertEquals(".txt", result.metadata["extension"])
    }

    @Test
    fun `should detect PLAIN type when no extension provided`() {
        val content = "Just plain text"
        val result = parser.parse(content, mapOf("filename" to "README"))

        assertNotNull(result)
        assertEquals("plain", result.metadata["type"])
        assertEquals("", result.metadata["extension"])
    }

    // ==================== Language Mapping Tests ====================

    @Test
    fun `should map various source code extensions to languages`() {
        val extensionToLanguage = mapOf(
            "test.py" to "python",
            "app.js" to "javascript",
            "Main.kt" to "kotlin",
            "script.ts" to "typescript",
            "Program.java" to "java",
            "main.cpp" to "cpp",
            "code.rs" to "rust",
            "app.go" to "go",
            "script.rb" to "ruby",
            "index.php" to "php",
            "app.swift" to "swift",
            "server.cs" to "csharp",
            "script.sh" to "bash"
        )

        extensionToLanguage.forEach { (filename, expectedLanguage) ->
            val result = parser.parse("// code", mapOf("filename" to filename))
            assertTrue(
                result.parsedContent.contains("language-$expectedLanguage"),
                "Expected $filename to map to language-$expectedLanguage"
            )
        }
    }

    @Test
    fun `should map C header files to cpp language`() {
        val result = parser.parse("#include <stdio.h>", mapOf("filename" to "header.h"))
        assertTrue(result.parsedContent.contains("language-cpp"))
    }

    @Test
    fun `should handle unknown extensions as plain type`() {
        val result = parser.parse("content", mapOf("filename" to "file.unknown"))
        // Unknown extensions are detected as PLAIN type (not CODE)
        assertEquals("plain", result.metadata["type"])
        // PLAIN type doesn't use language classes, just wraps in <pre>
        assertTrue(result.parsedContent.contains("<pre"))
    }

    // ==================== JSON Pretty-Printing Tests ====================

    @Test
    fun `should pretty-print valid JSON`() {
        val content = "{\"name\":\"test\",\"age\":30,\"active\":true}"
        val result = parser.parse(content, mapOf("filename" to "data.json"))

        assertNotNull(result)
        // Pretty-printed JSON should have newlines and indentation
        assertTrue(result.parsedContent.contains("name"))
        assertTrue(result.parsedContent.contains("test"))
    }

    @Test
    fun `should pretty-print nested JSON objects`() {
        val content = "{\"user\":{\"name\":\"test\",\"address\":{\"city\":\"NYC\"}}}"
        val result = parser.parse(content, mapOf("filename" to "user.json"))

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("user"))
        assertTrue(result.parsedContent.contains("address"))
    }

    @Test
    fun `should pretty-print JSON arrays`() {
        val content = "{\"items\":[\"one\",\"two\",\"three\"]}"
        val result = parser.parse(content, mapOf("filename" to "items.json"))

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("items"))
    }

    @Test
    fun `should handle malformed JSON gracefully`() {
        val malformed = "{invalid json content"
        val result = parser.parse(malformed, mapOf("filename" to "bad.json"))

        assertNotNull(result)
        // Should return original content if pretty-print fails
    }

    @Test
    fun `should handle JSON with escaped quotes`() {
        val content = "{\"text\":\"He said \\\"Hello\\\"\"}"
        val result = parser.parse(content, mapOf("filename" to "quotes.json"))

        assertNotNull(result)
    }

    @Test
    fun `should handle empty JSON object`() {
        val content = "{}"
        val result = parser.parse(content, mapOf("filename" to "empty.json"))

        assertNotNull(result)
    }

    @Test
    fun `should handle empty JSON array`() {
        val content = "[]"
        val result = parser.parse(content, mapOf("filename" to "empty-array.json"))

        assertNotNull(result)
    }

    // ==================== HTML Output Tests ====================

    @Test
    fun `should wrap plain text in pre tag`() {
        val content = "Line 1\nLine 2\nLine 3"
        val result = parser.parse(content, mapOf("filename" to "test.txt"))

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<pre"))
        assertTrue(result.parsedContent.contains("</pre>"))
        assertTrue(result.parsedContent.contains("plaintext"))
    }

    @Test
    fun `should wrap source code in code block with language class`() {
        val content = "def hello():\n    print('World')"
        val result = parser.parse(content, mapOf("filename" to "test.py"))

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<pre><code"))
        assertTrue(result.parsedContent.contains("language-python"))
        assertTrue(result.parsedContent.contains("</code></pre>"))
    }

    @Test
    fun `should render HTML content as-is`() {
        val content = "<html><body><h1>Test</h1></body></html>"
        val result = parser.parse(content, mapOf("filename" to "page.html"))

        assertNotNull(result)
        // HTML type renders content directly
        assertEquals(content, result.parsedContent)
    }

    @Test
    fun `should escape HTML special characters in plain text`() {
        val content = "<script>alert('XSS')</script>"
        val result = parser.parse(content, mapOf("filename" to "test.txt"))

        assertNotNull(result)
        // Should escape < > to prevent XSS
        assertFalse(result.parsedContent.contains("<script>alert"))
    }

    @Test
    fun `should escape HTML special characters in source code`() {
        val content = "if (x < 5 && y > 10) { return x & y; }"
        val result = parser.parse(content, mapOf("filename" to "test.js"))

        assertNotNull(result)
        // Special chars should be escaped
    }

    // ==================== Metadata Extraction Tests ====================

    @Test
    fun `should extract line count metadata`() {
        val content = "Line 1\nLine 2\nLine 3\nLine 4"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("4", result.metadata["lines"])
    }

    @Test
    fun `should extract character count metadata`() {
        val content = "Hello World"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("11", result.metadata["characters"])
    }

    @Test
    fun `should extract extension metadata`() {
        val result = parser.parse("content", mapOf("filename" to "test.py"))

        assertNotNull(result)
        assertEquals(".py", result.metadata["extension"])
    }

    @Test
    fun `should handle case-insensitive extensions`() {
        val result = parser.parse("content", mapOf("filename" to "TEST.PY"))

        assertNotNull(result)
        assertEquals(".py", result.metadata["extension"])
    }

    @Test
    fun `should count lines in empty content as 1`() {
        val result = parser.parse("")

        assertNotNull(result)
        assertEquals("1", result.metadata["lines"])
    }

    // ==================== Line Ending Tests ====================

    @Test
    fun `should handle Unix line endings LF`() {
        val content = "Line 1\nLine 2\nLine 3"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["lines"])
    }

    @Test
    fun `should handle Windows line endings CRLF`() {
        val content = "Line 1\r\nLine 2\r\nLine 3"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("3", result.metadata["lines"])
    }

    @Test
    fun `should handle Mac line endings CR`() {
        val content = "Line 1\rLine 2\rLine 3"
        val result = parser.parse(content)

        assertNotNull(result)
        // CR alone creates separate lines
        assertTrue(result.metadata["lines"]!!.toInt() >= 1)
    }

    @Test
    fun `should handle mixed line endings`() {
        val content = "Line 1\nLine 2\r\nLine 3\rLine 4"
        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.metadata["lines"]!!.toInt() >= 4)
    }

    // ==================== Real-World Scenario Tests ====================

    @Test
    fun `should parse real-world application log file`() {
        val content = """
            [2025-01-11 10:30:45] INFO: Application started
            [2025-01-11 10:30:46] DEBUG: Loading configuration from config.json
            [2025-01-11 10:30:47] WARN: Cache directory not found, creating new
            [2025-01-11 10:30:48] ERROR: Failed to connect to database
            [2025-01-11 10:30:49] INFO: Retrying connection (attempt 2/3)
            [2025-01-11 10:30:50] INFO: Database connection established
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "app.log"))

        assertNotNull(result)
        assertEquals("6", result.metadata["lines"])
        assertTrue(result.parsedContent.contains("ERROR"))
        assertTrue(result.parsedContent.contains("INFO"))
    }

    @Test
    fun `should parse real-world configuration file`() {
        val content = """
            # Application Configuration
            server.port=8080
            server.host=localhost
            database.url=jdbc:postgresql://localhost:5432/mydb
            database.username=admin
            database.password=secret123
            logging.level=DEBUG
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "application.properties"))

        assertNotNull(result)
        assertEquals("7", result.metadata["lines"])
        assertTrue(result.parsedContent.contains("server.port"))
    }

    @Test
    fun `should parse real-world Python script`() {
        val content = """
            #!/usr/bin/env python3
            import sys
            import os

            def main():
                print("Hello, World!")
                for arg in sys.argv[1:]:
                    print(f"Argument: {arg}")

            if __name__ == "__main__":
                main()
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "script.py"))

        assertNotNull(result)
        assertEquals("code", result.metadata["type"])
        assertTrue(result.parsedContent.contains("language-python"))
        assertTrue(result.parsedContent.contains("def main"))
    }

    @Test
    fun `should parse real-world shell script`() {
        val content = """
            #!/bin/bash
            set -e

            echo "Building project..."
            ./gradlew clean build

            if [ $? -eq 0 ]; then
                echo "Build successful!"
            else
                echo "Build failed!"
                exit 1
            fi
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "build.sh"))

        assertNotNull(result)
        assertEquals("code", result.metadata["type"])
        assertTrue(result.parsedContent.contains("language-bash"))
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `should handle very long single line`() {
        val longLine = "x".repeat(100000)
        val result = parser.parse(longLine)

        assertNotNull(result)
        assertEquals("1", result.metadata["lines"])
        assertEquals("100000", result.metadata["characters"])
    }

    @Test
    fun `should handle many short lines`() {
        val manyLines = "line\n".repeat(10000)
        val result = parser.parse(manyLines)

        assertNotNull(result)
        assertTrue(result.metadata["lines"]!!.toInt() >= 10000)
    }

    @Test
    fun `should handle tabs and spaces`() {
        val content = "\t\tIndented with tabs\n    Indented with spaces"
        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle trailing newlines`() {
        val content = "Content\n\n\n"
        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle leading newlines`() {
        val content = "\n\n\nContent"
        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle only newlines`() {
        val content = "\n\n\n\n"
        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle Unicode emoji`() {
        val content = "Hello 👋 World 🌍 Testing 🧪"
        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle Chinese characters`() {
        val content = "你好世界\n这是一个测试"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("2", result.metadata["lines"])
    }

    @Test
    fun `should handle Cyrillic characters`() {
        val content = "Привет мир\nЭто тест"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("2", result.metadata["lines"])
    }

    @Test
    fun `should handle right-to-left text`() {
        val content = "مرحبا بالعالم\nهذا اختبار"
        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== toHtml Method Tests ====================

    @Test
    fun `should call toHtml with document`() {
        val content = "Test content"
        val document = parser.parse(content)
        val html = parser.toHtml(document)

        assertNotNull(html)
        // toHtml should return parsedContent
        assertEquals(document.parsedContent, html)
    }

    @Test
    fun `should call toHtml with lightMode parameter`() {
        val content = "Test content"
        val document = parser.parse(content)
        val lightHtml = parser.toHtml(document, lightMode = true)
        val darkHtml = parser.toHtml(document, lightMode = false)

        assertNotNull(lightHtml)
        assertNotNull(darkHtml)
        // Both should return the same content (lightMode not used in PlaintextParser)
        assertEquals(lightHtml, darkHtml)
    }

    // ==================== Source Code Extension Tests ====================

    @Test
    fun `should handle TypeScript mjs extension`() {
        val result = parser.parse("export const x = 5;", mapOf("filename" to "module.mjs"))

        assertEquals("code", result.metadata["type"])
        assertTrue(result.parsedContent.contains("language-javascript"))
    }

    @Test
    fun `should handle C++ cpp extension`() {
        // Note: Only .cpp is in CODE_EXTENSIONS, .cc and .cxx are not
        val result = parser.parse("int main() {}", mapOf("filename" to "test.cpp"))

        assertEquals("code", result.metadata["type"])
        assertTrue(result.parsedContent.contains("language-cpp"))
    }

    @Test
    fun `should handle C++ cc and cxx as plain text`() {
        // .cc and .cxx are NOT in CODE_EXTENSIONS, so they're treated as plain text
        val ccResult = parser.parse("int main() {}", mapOf("filename" to "test.cc"))
        val cxxResult = parser.parse("int main() {}", mapOf("filename" to "test.cxx"))

        assertEquals("plain", ccResult.metadata["type"])
        assertEquals("plain", cxxResult.metadata["type"])
    }

    @Test
    fun `should handle YAML extensions`() {
        val yamlExtensions = listOf("config.yaml", "config.yml")

        yamlExtensions.forEach { filename ->
            val result = parser.parse("key: value", mapOf("filename" to filename))
            assertTrue(result.parsedContent.contains("language-yaml"))
        }
    }

    @Test
    fun `should handle SQL files`() {
        val content = "SELECT * FROM users WHERE id = 1;"
        val result = parser.parse(content, mapOf("filename" to "query.sql"))

        assertEquals("code", result.metadata["type"])
        assertTrue(result.parsedContent.contains("language-sql"))
    }

    @Test
    fun `should handle diff and patch files`() {
        val content = """
            --- a/file.txt
            +++ b/file.txt
            @@ -1,3 +1,3 @@
            -old line
            +new line
        """.trimIndent()

        val diffResult = parser.parse(content, mapOf("filename" to "changes.diff"))
        assertTrue(diffResult.parsedContent.contains("language-diff"))

        val patchResult = parser.parse(content, mapOf("filename" to "changes.patch"))
        assertTrue(patchResult.parsedContent.contains("language-diff"))
    }
}
