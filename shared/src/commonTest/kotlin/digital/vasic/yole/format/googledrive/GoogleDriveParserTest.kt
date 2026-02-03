/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for Google Drive Parser
 *
 *########################################################*/
package digital.vasic.yole.format.googledrive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleDriveParserTest {

    private val parser = GoogleDriveParser()

    // ==================== Format Tests ====================

    @Test
    fun `test supported format id`() {
        assertEquals("googledrive", parser.supportedFormat.id)
    }

    @Test
    fun `test supported format name`() {
        assertEquals("Google Drive", parser.supportedFormat.name)
    }

    @Test
    fun `test default extension`() {
        assertEquals(".gdoc", parser.supportedFormat.defaultExtension)
    }

    @Test
    fun `test supported extensions`() {
        val extensions = parser.supportedFormat.extensions
        assertTrue(extensions.contains(".gdoc"))
        assertTrue(extensions.contains(".gsheet"))
        assertTrue(extensions.contains(".gslides"))
    }

    @Test
    fun `test multiple extensions count`() {
        assertEquals(3, parser.supportedFormat.extensions.size)
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `test parse simple content`() {
        val content = "Google Drive document content"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertEquals(content, result.parsedContent)
    }

    @Test
    fun `test parse empty content`() {
        val content = ""
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("", result.rawContent)
        assertEquals("", result.parsedContent)
    }

    @Test
    fun `test parse multiline content`() {
        val content = """
            Document Title

            This is a paragraph of text.

            Another paragraph here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertTrue(result.parsedContent.contains("Document Title"))
    }

    @Test
    fun `test parse JSON-like content`() {
        val content = """
            {
                "doc_id": "abc123",
                "title": "My Document",
                "content": "Hello World"
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("abc123"))
        assertTrue(result.parsedContent.contains("My Document"))
    }

    @Test
    fun `test parse content with unicode`() {
        val content = "Google Docs: 文档 📄 מסמך"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    // ==================== Metadata Tests ====================

    @Test
    fun `test metadata is empty`() {
        val content = "Test content"
        val result = parser.parse(content)

        assertTrue(result.metadata.isEmpty())
    }

    @Test
    fun `test errors list is empty`() {
        val content = "Test content"
        val result = parser.parse(content)

        assertTrue(result.errors.isEmpty())
    }

    // ==================== Format Association Tests ====================

    @Test
    fun `test result has correct format`() {
        val content = "Test"
        val result = parser.parse(content)

        assertEquals("googledrive", result.format.id)
        assertEquals("Google Drive", result.format.name)
    }

    // ==================== Options Tests ====================

    @Test
    fun `test parse with empty options`() {
        val content = "Test"
        val result = parser.parse(content, emptyMap())

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `test parse with custom options`() {
        val content = "Test"
        val options = mapOf("format" to "gdoc")
        val result = parser.parse(content, options)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `test parse whitespace only`() {
        val content = "   \t\n   "
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `test parse very long content`() {
        val content = "C".repeat(10000)
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertEquals(10000, result.rawContent.length)
    }

    @Test
    fun `test parse spreadsheet-like content`() {
        val content = """
            Name,Age,City
            John,30,New York
            Jane,25,Los Angeles
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("John"))
        assertTrue(result.parsedContent.contains("Jane"))
    }
}
