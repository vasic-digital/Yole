/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for CSV parser
 *
 *########################################################*/
package digital.vasic.yole.format.csv

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.csv.CsvParser
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for CSV format parser.
 *
 * Tests cover:
 * - Format detection by extension
 * - Basic parsing functionality
 * - Edge cases and error handling
 * - Empty input handling
 * - Special characters
 */
class CsvParserTest {

    private val parser = CsvParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect CSV format by extension`() {
        val format = FormatRegistry.getByExtension(".csv")

        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_CSV)
        assertThat(format.name).isEqualTo("CSV")
    }

    @Test
    fun `should detect CSV format by filename`() {
        val format = FormatRegistry.detectByFilename("test.csv")

        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_CSV)
    }

    @Test
    fun `should support all CSV extensions`() {
        val extensions = listOf(".csv")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertThat(format.id).isEqualTo(FormatRegistry.ID_CSV)
        }
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `should parse basic CSV content`() {
        val content = """
            Sample CSV content here
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
        val content = "Single line of CSV"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Content Detection Tests ====================

    @Test
    fun `should detect format by content patterns`() {
        val content = """
            Sample CSV content here
        """.trimIndent()

        val format = FormatRegistry.detectByContent(content)

        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_CSV)
    }

    @Test
    fun `should not false-positive on plain text`() {
        val plainText = "Just some plain text without special formatting"

        val format = FormatRegistry.detectByContent(plainText)

        // Should detect as plaintext, not CSV
        if (format != null) {
            assertThat(format.id).isNotEqualTo(FormatRegistry.ID_CSV)
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
            Malformed CSV content
        """.trimIndent()

        // Should not throw exception
        val result = parser.parse(malformed)
        assertNotNull(result)
    }

    @Test
    fun `should handle very long input`() {
        val longContent = "Single line of CSV\n".repeat(10000)

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
        val format = FormatRegistry.getById(FormatRegistry.ID_CSV)

        assertNotNull(format)
        assertThat(format.name).isEqualTo("CSV")
        assertThat(format.defaultExtension).isEqualTo(".csv")
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val csvFormat = allFormats.find { it.id == FormatRegistry.ID_CSV }

        assertNotNull(csvFormat)
        assertThat(csvFormat.name).isEqualTo("CSV")
    }
}
