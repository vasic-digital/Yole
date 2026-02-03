/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for FTP Parser
 *
 *########################################################*/
package digital.vasic.yole.format.ftp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FtpParserTest {

    private val parser = FtpParser()

    // ==================== Format Tests ====================

    @Test
    fun `test supported format id`() {
        assertEquals("ftp", parser.supportedFormat.id)
    }

    @Test
    fun `test supported format name`() {
        assertEquals("FTP", parser.supportedFormat.name)
    }

    @Test
    fun `test default extension`() {
        assertEquals(".ftp", parser.supportedFormat.defaultExtension)
    }

    @Test
    fun `test supported extensions`() {
        assertTrue(parser.supportedFormat.extensions.contains(".ftp"))
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `test parse simple content`() {
        val content = "FTP file content"
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
            File 1
            File 2
            Directory/
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `test parse FTP-like listing`() {
        val content = """
            drwxr-xr-x 2 user group 4096 Jan 15 10:30 documents
            -rw-r--r-- 1 user group 1234 Jan 14 09:15 readme.txt
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("documents"))
        assertTrue(result.parsedContent.contains("readme.txt"))
    }

    @Test
    fun `test parse content with special characters`() {
        val content = "Path: /home/user/file@name.txt"
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

        assertEquals("ftp", result.format.id)
        assertEquals("FTP", result.format.name)
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
        val options = mapOf("passive" to true)
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
        val content = "B".repeat(10000)
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertEquals(10000, result.rawContent.length)
    }
}
