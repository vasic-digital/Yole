/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contract tests for FileHandle interface.
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlin.test.*

class FileStorageContractTests {

    @Test
    fun `FileHandle stores uri`() {
        val handle = FileHandle("test://path/file.txt")
        assertEquals("test://path/file.txt", handle.uri)
    }

    @Test
    fun `write empty content returns true for valid uri`() {
        val handle = FileHandle(System.getProperty("java.io.tmpdir") +
            "/yole_empty_write_test.txt")
        val result = handle.writeBytes(ByteArray(0))
        assertTrue(result)
    }

    @Test
    fun `readBytes on non-existent file returns null`() {
        val handle = FileHandle(System.getProperty("java.io.tmpdir") +
            "/yole_nonexistent_read_test.txt")
        val result = handle.readBytes()
        assertNull(result)
    }

    @Test
    fun `exists on non-existent file returns false`() {
        val handle = FileHandle(System.getProperty("java.io.tmpdir") +
            "/yole_nonexistent_exists_test.txt")
        assertFalse(handle.exists())
    }

    @Test
    fun `displayName extracts filename from path`() {
        val handle = FileHandle("/tmp/test_display_name.txt")
        assertNotNull(handle.displayName())
        assertTrue(handle.displayName()!!.endsWith(".txt"))
    }

    @Test
    fun `handle uri is immutable after construction`() {
        val uri = "content://test/file.txt"
        val handle = FileHandle(uri)
        assertEquals(uri, handle.uri)
        assertEquals(uri, handle.uri)
    }
}
