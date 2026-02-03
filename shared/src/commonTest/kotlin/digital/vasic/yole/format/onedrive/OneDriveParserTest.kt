/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for OneDrive Parser
 *
 *########################################################*/
package digital.vasic.yole.format.onedrive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OneDriveParserTest {

    private val parser = OneDriveParser()

    // ==================== Format Tests ====================

    @Test
    fun `test supported format id`() {
        assertEquals("onedrive", parser.supportedFormat.id)
    }

    @Test
    fun `test supported format name`() {
        assertEquals("OneDrive", parser.supportedFormat.name)
    }

    @Test
    fun `test default extension`() {
        assertEquals(".one", parser.supportedFormat.defaultExtension)
    }

    @Test
    fun `test supported extensions`() {
        assertTrue(parser.supportedFormat.extensions.contains(".one"))
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `test parse simple content`() {
        val content = "OneDrive document content"
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
            Section 1
            Some notes here

            Section 2
            More notes
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertTrue(result.parsedContent.contains("Section 1"))
        assertTrue(result.parsedContent.contains("Section 2"))
    }

    @Test
    fun `test parse content with special characters`() {
        val content = "OneDrive path: C:\\Users\\Documents\\notes.one"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `test parse unicode content`() {
        val content = "OneDrive: Документи 文件 ファイル"
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

        assertEquals("onedrive", result.format.id)
        assertEquals("OneDrive", result.format.name)
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
        val options = mapOf("sync" to true)
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
        val content = "D".repeat(10000)
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertEquals(10000, result.rawContent.length)
    }

    @Test
    fun `test parse notebook-like content`() {
        val content = """
            [Notebook: My Notes]

            Page 1: Meeting Notes
            - Item 1
            - Item 2

            Page 2: Ideas
            - Idea A
            - Idea B
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("Page 1"))
        assertTrue(result.parsedContent.contains("Page 2"))
    }
}
