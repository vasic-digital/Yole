/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop-specific FileHandle tests with real filesystem.
 *
 *########################################################*/
package digital.vasic.yole.util

import java.io.File
import kotlin.test.*

class FileStorageDesktopTests {

    private fun tempFile(): File {
        val tmp = File(System.getProperty("java.io.tmpdir"),
            "yole_test_${System.currentTimeMillis()}.txt")
        tmp.deleteOnExit()
        return tmp
    }

    @Test
    fun `write and read roundtrip on real file`() {
        val file = tempFile()
        val handle = FileHandle(file.absolutePath)
        val content = "Hello, Yole!\nSecond line."
        val bytes = content.toByteArray()

        val wrote = handle.writeBytes(bytes)
        assertTrue(wrote, "Write should succeed")
        assertTrue(file.exists(), "File should exist on disk")

        val read = handle.readBytes()
        assertNotNull(read, "Read should return data")
        assertEquals(content, String(read), "Content should match")

        val name = handle.displayName()
        assertTrue(name?.endsWith(".txt") == true, "Display name should end with .txt")
    }

    @Test
    fun `write overwrites existing content`() {
        val file = tempFile()
        file.writeText("original content")
        val handle = FileHandle(file.absolutePath)

        val newContent = "overwritten content"
        handle.writeBytes(newContent.toByteArray())

        val read = handle.readBytes()
        assertEquals(newContent, String(read!!))
    }

    @Test
    fun `readBytes returns null for non-existent file`() {
        val handle = FileHandle("/tmp/yole_nonexistent_${System.currentTimeMillis()}.txt")
        assertNull(handle.readBytes())
    }

    @Test
    fun `exists returns false for non-existent file`() {
        val handle = FileHandle("/tmp/yole_nonexistent_${System.currentTimeMillis()}.txt")
        assertFalse(handle.exists())
    }

    @Test
    fun `write creates parent directories`() {
        val dir = File(System.getProperty("java.io.tmpdir"),
            "yole_nested_${System.currentTimeMillis()}")
        val file = File(dir, "sub/test.txt")
        val handle = FileHandle(file.absolutePath)

        val ok = handle.writeBytes("nested".toByteArray())
        assertTrue(ok, "Write should create parent dirs and succeed")
        assertTrue(file.exists(), "File should exist at nested path")

        file.delete()
        File(file.parent).delete()
        dir.delete()
    }
}
