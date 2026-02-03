/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for Dropbox Parser
 *
 *########################################################*/
package digital.vasic.yole.format.dropbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DropboxParserTest {

    private val parser = DropboxParser()

    // ==================== Format Tests ====================

    @Test
    fun `test supported format id`() {
        assertEquals("dropbox", parser.supportedFormat.id)
    }

    @Test
    fun `test supported format name`() {
        assertEquals("Dropbox", parser.supportedFormat.name)
    }

    @Test
    fun `test default extension`() {
        assertEquals(".dbx", parser.supportedFormat.defaultExtension)
    }

    @Test
    fun `test supported extensions`() {
        assertTrue(parser.supportedFormat.extensions.contains(".dbx"))
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `test parse simple content`() {
        val content = "Hello, Dropbox!"
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
            Line 1
            Line 2
            Line 3
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertTrue(result.parsedContent.contains("Line 1"))
        assertTrue(result.parsedContent.contains("Line 2"))
        assertTrue(result.parsedContent.contains("Line 3"))
    }

    @Test
    fun `test parse content with special characters`() {
        val content = "Special chars: @#$%^&*()!<>{}[]"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `test parse unicode content`() {
        val content = "Unicode: 日本語 中文 한국어 العربية"
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

        assertEquals("dropbox", result.format.id)
        assertEquals("Dropbox", result.format.name)
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
        val options = mapOf("key" to "value")
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
        val content = "A".repeat(10000)
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertEquals(10000, result.rawContent.length)
    }
}
