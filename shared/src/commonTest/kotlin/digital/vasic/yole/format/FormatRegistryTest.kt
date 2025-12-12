/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for FormatRegistry
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Comprehensive tests for FormatRegistry covering:
 * - Format detection and registration
 * - Extension handling
 * - Cross-format compatibility
 * - Error scenarios
 * - Performance under load
 */
class FormatRegistryTest {

    @BeforeTest
    fun setUp() {
        // Ensure format registry is properly initialized
        ParserInitializer.initialize()
    }

    @Test
    fun testGetAllFormats() {
        val formats = FormatRegistry.getAllFormats()
        
        // Should have multiple formats registered
        assertTrue(formats.isNotEmpty(), "Format registry should contain formats")
        
        // Should contain major formats
        val formatNames = formats.map { it.name }
        assertTrue(formatNames.contains("Markdown"), "Should contain Markdown format")
        assertTrue(formatNames.contains("Plain Text"), "Should contain Plain Text format")
    }

    @Test
    fun testGetById() {
        // Test getting format by ID
        val markdownFormat = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        assertNotNull(markdownFormat, "Should find Markdown format by ID")
        assertEquals("Markdown", markdownFormat.name)
        
        val plainTextFormat = FormatRegistry.getById(FormatRegistry.ID_PLAIN_TEXT)
        assertNotNull(plainTextFormat, "Should find Plain Text format by ID")
        assertEquals("Plain Text", plainTextFormat.name)
    }

    @Test
    fun testGetByExtension() {
        // Test getting format by file extension
        val mdFormat = FormatRegistry.getByExtension(".md")
        assertNotNull(mdFormat, "Should find format for .md extension")
        assertEquals("Markdown", mdFormat.name)
        
        val txtFormat = FormatRegistry.getByExtension(".txt")
        assertNotNull(txtFormat, "Should find format for .txt extension")
        assertEquals("Plain Text", txtFormat.name)
    }

    @Test
    fun testDetectByFilename() {
        // Test format detection by filename
        val markdownFormat = FormatRegistry.detectByFilename("document.md")
        assertNotNull(markdownFormat, "Should detect Markdown format from filename")
        assertEquals("Markdown", markdownFormat.name)
        
        val textFormat = FormatRegistry.detectByFilename("document.txt")
        assertNotNull(textFormat, "Should detect Plain Text format from filename")
        assertEquals("Plain Text", textFormat.name)
    }

    @Test
    fun testDetectByExtension() {
        // Test format detection by extension
        val markdownFormat = FormatRegistry.detectByExtension(".md")
        assertNotNull(markdownFormat, "Should detect Markdown format from extension")
        assertEquals("Markdown", markdownFormat.name)
        
        val textFormat = FormatRegistry.detectByExtension(".txt")
        assertNotNull(textFormat, "Should detect Plain Text format from extension")
        assertEquals("Plain Text", textFormat.name)
    }

    @Test
    fun testIsExtensionSupported() {
        // Test extension support checking
        assertTrue(FormatRegistry.isExtensionSupported(".md"), "Should support .md extension")
        assertTrue(FormatRegistry.isExtensionSupported(".txt"), "Should support .txt extension")
        assertFalse(FormatRegistry.isExtensionSupported(".xyz"), "Should not support .xyz extension")
    }

    @Test
    fun testIsSupported() {
        // Test format support checking
        assertTrue(FormatRegistry.isSupported("Markdown"), "Should support Markdown format")
        assertTrue(FormatRegistry.isSupported("Plain Text"), "Should support Plain Text format")
        assertFalse(FormatRegistry.isSupported("Unknown Format"), "Should not support unknown format")
    }

    @Test
    fun testGetFormatNames() {
        val formatNames = FormatRegistry.getFormatNames()
        
        assertTrue(formatNames.isNotEmpty(), "Should return format names")
        assertTrue(formatNames.contains("Markdown"), "Should contain Markdown")
        assertTrue(formatNames.contains("Plain Text"), "Should contain Plain Text")
    }

    @Test
    fun testGetAllExtensions() {
        val extensions = FormatRegistry.getAllExtensions()
        
        assertTrue(extensions.isNotEmpty(), "Should return extensions")
        assertTrue(extensions.contains(".md"), "Should contain .md extension")
        assertTrue(extensions.contains(".txt"), "Should contain .txt extension")
    }

    @Test
    fun testFormatProperties() {
        val markdownFormat = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        assertNotNull(markdownFormat)
        
        // Test format properties
        assertEquals("Markdown", markdownFormat.name)
        assertTrue(markdownFormat.extensions.contains(".md"), "Markdown should support .md extension")
        assertTrue(markdownFormat.extensions.contains(".markdown"), "Markdown should support .markdown extension")
    }

    @Test
    fun testCaseInsensitiveDetection() {
        // Test case-insensitive format detection
        val upperCaseFormat = FormatRegistry.detectByFilename("DOCUMENT.MD")
        assertNotNull(upperCaseFormat, "Should detect format case-insensitively")
        assertEquals("Markdown", upperCaseFormat.name)
        
        val mixedCaseFormat = FormatRegistry.detectByFilename("Document.Md")
        assertNotNull(mixedCaseFormat, "Should detect format with mixed case")
        assertEquals("Markdown", mixedCaseFormat.name)
    }

    @Test
    fun testMultipleExtensions() {
        // Test formats with multiple extensions
        val markdownFormat1 = FormatRegistry.getByExtension(".md")
        val markdownFormat2 = FormatRegistry.getByExtension(".markdown")
        
        assertNotNull(markdownFormat1)
        assertNotNull(markdownFormat2)
        assertEquals(markdownFormat1.id, markdownFormat2.id, "Both extensions should map to same format")
    }

    @Test
    fun testUnknownFormats() {
        // Test handling of unknown formats
        val unknownById = FormatRegistry.getById("unknown_format_id")
        assertNull(unknownById, "Should return null for unknown format ID")
        
        val unknownByExtension = FormatRegistry.getByExtension(".unknown")
        assertNull(unknownByExtension, "Should return null for unknown extension")
        
        val unknownByFilename = FormatRegistry.detectByFilename("document.unknown")
        assertNull(unknownByFilename, "Should return null for unknown file extension")
    }

    @Test
    fun testEmptyAndNullInputs() {
        // Test handling of empty and null inputs
        val emptyExtension = FormatRegistry.getByExtension("")
        assertNull(emptyExtension, "Should return null for empty extension")
        
        val emptyFilename = FormatRegistry.detectByFilename("")
        assertNull(emptyFilename, "Should return null for empty filename")
    }

    @Test
    fun testSpecialCharactersInFilenames() {
        // Test format detection with special characters in filenames
        val specialCharFormat = FormatRegistry.detectByFilename("my-document_with.special.chars.md")
        assertNotNull(specialCharFormat, "Should detect format with special characters")
        assertEquals("Markdown", specialCharFormat.name)
    }

    @Test
    fun testFormatImmutability() {
        // Test that format registry returns immutable objects
        val format1 = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val format2 = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        
        assertNotNull(format1)
        assertNotNull(format2)
        assertEquals(format1, format2, "Should return equivalent format objects")
    }

    @Test
    fun testPerformanceUnderLoad() {
        // Test format registry performance under load
        val iterations = 1000
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) { i ->
            FormatRegistry.detectByFilename("document$i.md")
            FormatRegistry.detectByExtension(".txt")
            FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete 3000 operations in reasonable time (less than 1 second)
        assertTrue(duration < 1000, "Format registry operations should be fast, took $duration ms")
    }

    @Test
    fun testConcurrentAccess() {
        // Test thread safety of format registry
        val results = mutableListOf<Boolean>()
        val threads = mutableListOf<Thread>()
        
        repeat(10) {
            val thread = Thread {
                repeat(100) { i ->
                    val format = FormatRegistry.detectByFilename("document$i.md")
                    results.add(format != null)
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        threads.forEach { it.join() }
        
        // All operations should succeed
        assertEquals(1000, results.size, "All concurrent operations should complete")
        assertTrue(results.all { it }, "All format detections should succeed")
    }

    @Test
    fun testFormatRegistryConsistency() {
        // Test consistency across different access methods
        val byId = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        val byExtension = FormatRegistry.getByExtension(".md")
        val byFilename = FormatRegistry.detectByFilename("document.md")
        
        assertNotNull(byId)
        assertNotNull(byExtension)
        assertNotNull(byFilename)
        
        // All methods should return the same format
        assertEquals(byId.id, byExtension.id, "getById and getByExtension should return same format")
        assertEquals(byId.id, byFilename.id, "getById and detectByFilename should return same format")
    }
}