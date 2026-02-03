/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for SFTP Parser
 *
 *########################################################*/
package digital.vasic.yole.format.sftp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SftpParserTest {

    private val parser = SftpParser()

    // ==================== Format Tests ====================

    @Test
    fun `test supported format id`() {
        assertEquals("sftp", parser.supportedFormat.id)
    }

    @Test
    fun `test supported format name`() {
        assertEquals("SFTP", parser.supportedFormat.name)
    }

    @Test
    fun `test default extension`() {
        assertEquals(".sftp", parser.supportedFormat.defaultExtension)
    }

    @Test
    fun `test supported extensions`() {
        assertTrue(parser.supportedFormat.extensions.contains(".sftp"))
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `test parse simple content`() {
        val content = "SFTP file content"
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
            /home/user/documents
            /home/user/downloads
            /home/user/projects
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `test parse SFTP-like listing`() {
        val content = """
            drwxr-xr-x 5 root root 4096 Jan 15 10:30 .
            drwxr-xr-x 3 root root 4096 Jan 14 09:15 ..
            -rw-r--r-- 1 user user 1234 Jan 13 08:00 config.txt
            drwx------ 2 user user 4096 Jan 12 07:45 .ssh
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("config.txt"))
        assertTrue(result.parsedContent.contains(".ssh"))
    }

    @Test
    fun `test parse content with special characters`() {
        val content = "Path: /home/user/file-name_v2.0.txt"
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `test parse unicode paths`() {
        val content = "/home/用户/文档/файлы"
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

        assertEquals("sftp", result.format.id)
        assertEquals("SFTP", result.format.name)
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
        val options = mapOf("port" to 22, "key" to "/path/to/key")
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
        val content = "E".repeat(10000)
        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(content, result.rawContent)
        assertEquals(10000, result.rawContent.length)
    }

    @Test
    fun `test parse SSH config-like content`() {
        val content = """
            Host myserver
                HostName 192.168.1.100
                User admin
                Port 22
                IdentityFile ~/.ssh/id_rsa
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("myserver"))
        assertTrue(result.parsedContent.contains("192.168.1.100"))
    }
}
