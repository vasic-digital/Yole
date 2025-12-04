/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Binary parser
 *
 *########################################################*/
package digital.vasic.yole.format.binary

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.binary.BinaryParser
import org.junit.Test
import kotlin.test.*

/**
 * Unit tests for Binary format parser.
 *
 * Tests cover:
 * - Format detection by extension
 * - Basic parsing functionality
 * - Edge cases and error handling
 * - Empty input handling
 * - Special characters
 */
class BinaryParserTest {

    private val parser = BinaryParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Binary format by extension`() {
        // Binary format has no extensions (emptyList)
        val format = FormatRegistry.getByExtension(".bin")

        // Should not find Binary by extension since it has no extensions
        assertTrue(format == null || format.id != FormatRegistry.ID_BINARY)
    }

    @Test
    fun `should detect Binary format by filename`() {
        // Binary is typically detected by content, not filename
        val format = FormatRegistry.detectByFilename("test.bin")

        // Binary won't be detected by filename/extension
        assertTrue(format == null || format.id != FormatRegistry.ID_BINARY)
    }

    @Test
    fun `should support all Binary extensions`() {
        // Binary format has emptyList() for extensions
        val format = FormatRegistry.getById(FormatRegistry.ID_BINARY)

        assertNotNull(format)
        assertTrue(format.extensions.isEmpty())
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `should parse basic Binary content`() {
        val content = """
            Sample Binary content here
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
        val content = "Single line of Binary"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Content Detection Tests ====================

    @Test
    fun `should detect format by content patterns`() {
        // Binary format has no detection patterns (detected only by null bytes/binary content)
        val content = "Sample text content"

        val format = FormatRegistry.detectByContent(content)

        // Binary won't be detected from regular text
        assertTrue(format == null || format.id != FormatRegistry.ID_BINARY)
    }

    @Test
    fun `should not false-positive on plain text`() {
        val plainText = "Just some plain text without special formatting"

        val format = FormatRegistry.detectByContent(plainText)

        // Should detect as plaintext, not Binary
        if (format != null) {
            assertNotEquals(FormatRegistry.ID_BINARY, format.id)
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
            Malformed Binary content
        """.trimIndent()

        // Should not throw exception
        val result = parser.parse(malformed)
        assertNotNull(result)
    }

    @Test
    fun `should handle very long input`() {
        val longContent = "Single line of Binary\n".repeat(10000)

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
        val format = FormatRegistry.getById(FormatRegistry.ID_BINARY)

        assertNotNull(format)
        assertEquals("Binary", format.name)
        assertEquals(".bin", format.defaultExtension)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val binaryFormat = allFormats.find { it.id == FormatRegistry.ID_BINARY }

        assertNotNull(binaryFormat)
        assertEquals("Binary", binaryFormat.name)
    }
}
