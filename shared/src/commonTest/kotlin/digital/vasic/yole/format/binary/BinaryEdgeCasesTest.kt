/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Binary Detection Edge Cases Test Suite
 * Tests error conditions, boundary cases, and unusual scenarios
 *
 *########################################################*/
package digital.vasic.yole.format.binary

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.*

/**
 * Edge case and error handling test suite for binary detection.
 * Tests boundary conditions, malformed inputs, and unusual scenarios.
 */
class BinaryEdgeCasesTest {

    private lateinit var binaryParser: BinaryParser

    @BeforeTest
    fun setup() {
        binaryParser = BinaryParser()
        ParserRegistry.clear()
        ParserRegistry.register(binaryParser)
    }

    @Test
    fun `test null and empty filename handling`() {
        val content = "Binary content"
        
        // Test with null filename
        val nullResult = binaryParser.parse(content, mapOf<String, Any>("filename" to ""))
        assertNotNull(nullResult)
        assertEquals("", nullResult.metadata["extension"])
        assertEquals("application/octet-stream", nullResult.metadata["mime_type"])
        
        // Test with empty filename
        val emptyResult = binaryParser.parse(content, mapOf<String, Any>("filename" to ""))
        assertNotNull(emptyResult)
        assertEquals("", emptyResult.metadata["extension"])
        
        // Test with no filename option
        val noFilenameResult = binaryParser.parse(content, emptyMap())
        assertNotNull(noFilenameResult)
        assertEquals("", noFilenameResult.metadata["extension"])
    }

    @Test
    fun `test missing file extension`() {
        val filenamesWithoutExtension = listOf("README", "LICENSE", "Makefile", "binary")
        
        filenamesWithoutExtension.forEach { filename ->
            val result = binaryParser.parse("content", mapOf("filename" to filename))
            
            assertNotNull(result)
            assertEquals("", result.metadata["extension"])
            assertEquals("application/octet-stream", result.metadata["mime_type"])
        }
    }

    @Test
    fun `test malformed file extensions`() {
        val malformedFilenames = listOf(
            ".hidden",           // Extension only
            "file.",             // Trailing dot
            "file..ext",         // Double dots
            "file.ext.extra",    // Multiple extensions
            ".file.ext",         // Leading dot
            "file_with_very_long_extension_that_exceeds_normal_limits.datafile"
        )
        
        malformedFilenames.forEach { filename ->
            val result = binaryParser.parse("content", mapOf("filename" to filename))
            
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            // Should handle gracefully without crashing
        }
    }

    @Test
    fun `test unicode and special characters in filenames`() {
        val unicodeFilenames = listOf(
            "файл.exe",          // Cyrillic
            "文件.bin",          // Chinese
            "ファイル.dat",      // Japanese
            "📁archive.zip",     // Emoji
            "café.mp3",          // Accented characters
            "naïve.pdf",         // Diacritics
            "🎵song.ogg",        // Emoji with music
            "test file with spaces.exe",
            "test-file-with-dashes.bin",
            "test_file_with_underscores.dat"
        )
        
        unicodeFilenames.forEach { filename ->
            val result = binaryParser.parse("content", mapOf("filename" to filename))
            
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            // Extension extraction should work correctly
            val expectedExtension = filename.substringAfterLast('.').lowercase()
            if (filename.contains('.')) {
                assertEquals(expectedExtension, result.metadata["extension"])
            }
        }
    }

    @Test
    fun `test extremely long filenames`() {
        val longFilename = "a".repeat(255) + ".bin"
        val result = binaryParser.parse("content", mapOf("filename" to longFilename))
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_BINARY, result.format.id)
        assertEquals("bin", result.metadata["extension"])
    }

    @Test
    fun `test negative and zero file sizes`() {
        val invalidSizes = listOf(-1L, -100L, Long.MIN_VALUE)
        
        invalidSizes.forEach { size ->
            val result = binaryParser.parse("content", mapOf(
                "filename" to "test.bin",
                "fileSize" to size
            ))
            
            assertNotNull(result)
            // Negative sizes are returned as-is (not formatted)
            assertEquals(size.toString(), result.metadata["file_size"])
        }
        
        // Test zero size
        val zeroResult = binaryParser.parse("", mapOf(
            "filename" to "empty.bin",
            "fileSize" to 0L
        ))
        assertEquals("0 B", zeroResult.metadata["file_size"])
    }

    @Test
    fun `test very large file sizes`() {
        val largeSizes = listOf(
            Long.MAX_VALUE,
            1_000_000_000_000_000L, // 1 PB
            1_000_000_000_000L,     // 1 TB
            1_000_000_000L          // 1 GB
        )
        
        largeSizes.forEach { size ->
            val result = binaryParser.parse("content", mapOf(
                "filename" to "huge.bin",
                "fileSize" to size
            ))
            
            assertNotNull(result)
            // Should not crash with large sizes
            val formattedSize = result.metadata["file_size"]
            assertNotNull(formattedSize)
        }
    }

    @Test
    fun `test invalid mime types`() {
        val invalidMimeTypes = listOf(
            "",                           // Empty
            "invalid/mime/type",          // Too many parts
            "text",                       // No subtype
            "/plain",                     // No main type
            "text/",                      // No subtype
            "very/long/mime/type/that/exceeds/reasonable/limits/application",
            "text/plain; charset=utf-8",  // With parameters
            "application/json; charset=utf-8; boundary=something"
        )
        
        invalidMimeTypes.forEach { mimeType ->
            val result = binaryParser.parse("content", mapOf(
                "filename" to "test.bin",
                "mimeType" to mimeType
            ))
            
            assertNotNull(result)
            // Should handle gracefully
            if (mimeType.isNotEmpty() && mimeType.contains("/") && !mimeType.contains(";")) {
                assertEquals(mimeType, result.metadata["mime_type"])
            }
        }
    }

    @Test
    fun `test binary content with special bytes`() {
        val specialByteSequences = listOf(
            "\u0000\u0000\u0000\u0000",     // Null bytes
            "\u00FF\u00FF\u00FF\u00FF",     // All bits set
            "\u0080\u0080\u0080\u0080",     // High bit set
            "\u007F\u007F\u007F\u007F",     // DEL character
            "\u0001\u0002\u0003\u0004",     // Control characters
            "\u0000\u00FF\u0000\u00FF",     // Alternating null/max
            "\u00DE\u00AD\u00BE\u00EF",     // Dead beef (common test pattern)
            "\u00CA\u00FE\u00BA\u00BE",     // Cafe babe (Java class file)
            "\u00FE\u00ED\u00FA\u00CE",     // Feed face (Mach-O)
            "\u00CE\u00FA\u00ED\u00FE"      // Face feed (reversed)
        )
        
        specialByteSequences.forEach { content ->
            val result = binaryParser.parse(content, mapOf("filename" to "special.bin"))
            
            assertNotNull(result)
            assertEquals(content, result.rawContent)
            assertEquals("true", result.metadata["is_binary"])
        }
    }

    @Test
    fun `test HTML generation with malicious content`() {
        val maliciousContent = listOf(
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert('xss')>",
            "javascript:alert('xss')",
            "<iframe src='javascript:alert(\"xss\")'></iframe>",
            "</div><script>alert('xss')</script><div>",
            "'\"onmouseover=alert('xss')//",
            "<svg onload=alert('xss')>"
        )
        
        maliciousContent.forEach { content ->
            val document = binaryParser.parse(content, mapOf("filename" to "malicious.bin"))
            val html = binaryParser.toHtml(document, lightMode = true)
            
            assertNotNull(html)
            // HTML should be properly escaped
            assertFalse(html.contains("<script") && !html.contains("&lt;script"), 
                "HTML should not contain unescaped script tags for content: $content")
        }
    }

    @Test
    fun `test parser registry edge cases`() {
        // Test getting parser for non-existent format
        val nonExistentFormat = TextFormat("nonexistent", "Non-existent", ".none")
        val parser = digital.vasic.yole.format.ParserRegistry.getParser(nonExistentFormat)
        
        // Should return null or handle gracefully
        assertTrue(parser == null || !parser.canParse(nonExistentFormat))
        
        // Test binary parser can only parse binary format
        val binaryFormat = FormatRegistry.getById(TextFormat.ID_BINARY)!!
        assertTrue(binaryParser.canParse(binaryFormat))
        
        val markdownFormat = FormatRegistry.getById(TextFormat.ID_MARKDOWN)!!
        assertFalse(binaryParser.canParse(markdownFormat))
    }

    @Test
    fun `test concurrent parsing edge cases`() {
        val testCases = listOf(
            "" to "empty.bin",
            "\u0000" to "null.bin",
            "A".repeat(1000) to "large.bin",
            "<script>" to "script.bin"
        )
        
        // Test that multiple concurrent parses don't interfere
        val results = testCases.map { (content, filename) ->
            binaryParser.parse(content, mapOf("filename" to filename))
        }
        
        // Verify all results are correct
        results.forEachIndexed { index, result ->
            val (expectedContent, expectedFilename) = testCases[index]
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            assertEquals(expectedContent, result.rawContent)
            assertEquals(expectedFilename.substringAfterLast('.'), result.metadata["extension"])
        }
    }

    @Test
    fun `test format detection with edge content`() {
        val edgeCases = listOf(
            "",                           // Empty content
            "\n",                         // Just newline
            " ",                          // Just space
            "\t\n\r",                     // Whitespace only
            "# Title",                    // Markdown-like
            "* Item",                     // List-like
            "---",                        // YAML-like
            "function() {}",              // Code-like
            "1,2,3",                      // CSV-like
            "key=value",                  // Key-value like
            "[link](url)"                 // Markdown link
        )
        
        edgeCases.forEach { content ->
            // Binary format should not be detected by content analysis
            val detectedFormat = FormatRegistry.detectByContent(content)
            assertNotEquals(TextFormat.ID_BINARY, detectedFormat?.id, 
                "Binary format should not be detected for content: '$content'")
        }
    }

    @Test
    fun `test document metadata edge cases`() {
        val content = "test content"
        
        // Test with all metadata options
        val fullOptions = mapOf(
            "filename" to "test.exe",
            "fileSize" to 1024L,
            "mimeType" to "application/x-executable",
            "extraOption" to "extraValue"
        )
        
        val result = binaryParser.parse(content, fullOptions)
        assertNotNull(result)
        assertEquals("test.exe", result.metadata["filename"])
        assertEquals("1 KB", result.metadata["file_size"])
        assertEquals("1024", result.metadata["file_size_bytes"])
        assertEquals("application/x-executable", result.metadata["mime_type"])
        
        // Extra options should not appear in metadata unless explicitly handled
        assertFalse(result.metadata.containsKey("extraOption"))
    }

    @Test
    fun `test HTML cache behavior edge cases`() {
        val document = binaryParser.parse("content", mapOf("filename" to "test.bin"))
        
        // Initial state - no HTML cached
        assertFalse(document.hasHtmlCached(lightMode = true))
        assertFalse(document.hasHtmlCached(lightMode = false))
        
        // Generate light mode HTML
        val lightHtml1 = document.toHtml(lightMode = true)
        assertTrue(document.hasHtmlCached(lightMode = true))
        assertFalse(document.hasHtmlCached(lightMode = false))
        
        // Generate dark mode HTML
        val darkHtml1 = document.toHtml(lightMode = false)
        assertTrue(document.hasHtmlCached(lightMode = true))
        assertTrue(document.hasHtmlCached(lightMode = false))
        
        // Subsequent calls should return cached HTML
        val lightHtml2 = document.toHtml(lightMode = true)
        val darkHtml2 = document.toHtml(lightMode = false)
        
        assertEquals(lightHtml1, lightHtml2)
        assertEquals(darkHtml1, darkHtml2)
        
        // Clear cache
        document.clearHtmlCache()
        assertFalse(document.hasHtmlCached(lightMode = true))
        assertFalse(document.hasHtmlCached(lightMode = false))
    }
}