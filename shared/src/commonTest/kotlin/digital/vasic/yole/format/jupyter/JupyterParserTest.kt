/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Jupyter parser
 *
 *########################################################*/
package digital.vasic.yole.format.jupyter

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.jupyter.JupyterParser
import org.junit.Test
import kotlin.test.*

/**
 * Unit tests for Jupyter format parser.
 *
 * Tests cover:
 * - Format detection by extension
 * - Basic parsing functionality
 * - Edge cases and error handling
 * - Empty input handling
 * - Special characters
 */
class JupyterParserTest {

    private val parser = JupyterParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Jupyter format by extension`() {
        val format = FormatRegistry.getByExtension(".ipynb")

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_JUPYTER, format.id)
        assertEquals("Jupyter Notebook", format.name)
    }

    @Test
    fun `should detect Jupyter format by filename`() {
        val format = FormatRegistry.detectByFilename("test.ipynb")

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_JUPYTER, format.id)
    }

    @Test
    fun `should support all Jupyter extensions`() {
        val extensions = listOf(".ipynb")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(FormatRegistry.ID_JUPYTER, format.id)
        }
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `should parse basic Jupyter content`() {
        val content = """
            Sample Jupyter content here
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
        val content = "Single line of Jupyter"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Content Detection Tests ====================

    @Test
    fun `should detect format by content patterns`() {
        val content = """
            {"nbformat": 4, "cell_type": "code"}
        """.trimIndent()

        val format = FormatRegistry.detectByContent(content)

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_JUPYTER, format.id)
    }

    @Test
    fun `should not false-positive on plain text`() {
        val plainText = "Just some plain text without special formatting"

        val format = FormatRegistry.detectByContent(plainText)

        // Should detect as plaintext, not Jupyter
        if (format != null) {
            assertNotEquals(FormatRegistry.ID_JUPYTER, format.id)
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
            Malformed Jupyter content
        """.trimIndent()

        // Should not throw exception
        val result = parser.parse(malformed)
        assertNotNull(result)
    }

    @Test
    fun `should handle very long input`() {
        val longContent = "Single line of Jupyter\n".repeat(10000)

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
        val format = FormatRegistry.getById(FormatRegistry.ID_JUPYTER)

        assertNotNull(format)
        assertEquals("Jupyter Notebook", format.name)
        assertEquals(".ipynb", format.defaultExtension)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val jupyterFormat = allFormats.find { it.id == FormatRegistry.ID_JUPYTER }

        assertNotNull(jupyterFormat)
        assertEquals("Jupyter Notebook", jupyterFormat.name)
    }
}
