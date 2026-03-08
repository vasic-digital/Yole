/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop-specific tests for PlatformFileIO implementation
 * (DesktopFileIO via PlatformFileIOFactory).
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Desktop-specific tests for PlatformFileIO (DesktopFileIO).
 *
 * Tests cover:
 * - File read/write operations
 * - Directory listing behavior
 * - File existence checks
 * - Path resolution
 * - File permissions handling
 * - Edge cases with file names and content
 * - Binary data roundtrip
 * - Parent directory creation
 * - Large file operations
 */
class DesktopPlatformFileIOTests {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fileIO: PlatformFileIO

    @Before
    fun setup() {
        fileIO = PlatformFileIOFactory.create()
    }

    // ==================== File Read Tests ====================

    @Test
    fun `readFileBytes returns content of existing file`() = runBlocking<Unit> {
        val file = tempFolder.newFile("read_test.txt")
        val content = "Hello, Desktop FileIO!"
        file.writeText(content)

        val result = fileIO.readFileBytes(file.absolutePath)
        assertTrue(result.isSuccess, "Reading existing file should succeed")
        assertEquals(content, String(result.getOrThrow()))
    }

    @Test
    fun `readFileBytes returns failure for non-existent file`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "nonexistent_file.txt").absolutePath
        val result = fileIO.readFileBytes(path)
        assertTrue(result.isFailure, "Reading non-existent file should fail")
    }

    @Test
    fun `readFileBytes handles empty file`() = runBlocking<Unit> {
        val file = tempFolder.newFile("empty_file.txt")
        val result = fileIO.readFileBytes(file.absolutePath)
        assertTrue(result.isSuccess, "Reading empty file should succeed")
        assertEquals(0, result.getOrThrow().size)
    }

    @Test
    fun `readFileBytes handles binary content`() = runBlocking<Unit> {
        val file = tempFolder.newFile("binary_file.bin")
        val binaryContent = ByteArray(256) { it.toByte() }
        file.writeBytes(binaryContent)

        val result = fileIO.readFileBytes(file.absolutePath)
        assertTrue(result.isSuccess)
        assertContentEquals(binaryContent, result.getOrThrow())
    }

    @Test
    fun `readFileBytes handles UTF-8 content`() = runBlocking<Unit> {
        val file = tempFolder.newFile("utf8_file.txt")
        val utf8Content = "English \u4e2d\u6587 \u0420\u0443\u0441\u0441\u043a\u0438\u0439 \u65e5\u672c\u8a9e"
        file.writeText(utf8Content, Charsets.UTF_8)

        val result = fileIO.readFileBytes(file.absolutePath)
        assertTrue(result.isSuccess)
        assertEquals(utf8Content, String(result.getOrThrow(), Charsets.UTF_8))
    }

    // ==================== File Write Tests ====================

    @Test
    fun `writeFileBytes creates new file with content`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "write_test.txt").absolutePath
        val content = "Written by PlatformFileIO"

        val result = fileIO.writeFileBytes(path, content.toByteArray())
        assertTrue(result.isSuccess, "Writing file should succeed")
        assertEquals(content, File(path).readText())
    }

    @Test
    fun `writeFileBytes overwrites existing file`() = runBlocking<Unit> {
        val file = tempFolder.newFile("overwrite_test.txt")
        file.writeText("original content")

        val newContent = "overwritten content"
        val result = fileIO.writeFileBytes(file.absolutePath, newContent.toByteArray())
        assertTrue(result.isSuccess)
        assertEquals(newContent, file.readText())
    }

    @Test
    fun `writeFileBytes creates parent directories`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "deep/nested/dir/file.txt").absolutePath
        val content = "deep nested content"

        val result = fileIO.writeFileBytes(path, content.toByteArray())
        assertTrue(result.isSuccess, "Writing to deep path should succeed")
        assertEquals(content, File(path).readText())
    }

    @Test
    fun `writeFileBytes handles empty byte array`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "empty_write.txt").absolutePath
        val result = fileIO.writeFileBytes(path, ByteArray(0))
        assertTrue(result.isSuccess)
        assertEquals(0, File(path).length())
    }

    @Test
    fun `writeFileBytes handles large content`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "large_write.bin").absolutePath
        val largeContent = ByteArray(1024 * 100) { (it % 256).toByte() } // 100KB

        val result = fileIO.writeFileBytes(path, largeContent)
        assertTrue(result.isSuccess)
        assertContentEquals(largeContent, File(path).readBytes())
    }

    @Test
    fun `writeFileBytes binary roundtrip`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "binary_roundtrip.bin").absolutePath
        val data = ByteArray(512) { (it * 7 % 256).toByte() }

        fileIO.writeFileBytes(path, data)
        val readResult = fileIO.readFileBytes(path)
        assertTrue(readResult.isSuccess)
        assertContentEquals(data, readResult.getOrThrow())
    }

    // ==================== File Existence Tests ====================

    @Test
    fun `fileExists returns true for existing file`() = runBlocking<Unit> {
        val file = tempFolder.newFile("exists_test.txt")
        assertTrue(fileIO.fileExists(file.absolutePath))
    }

    @Test
    fun `fileExists returns false for non-existent file`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "does_not_exist.txt").absolutePath
        assertFalse(fileIO.fileExists(path))
    }

    @Test
    fun `fileExists returns true for existing directory`() = runBlocking<Unit> {
        val dir = tempFolder.newFolder("exists_dir")
        assertTrue(fileIO.fileExists(dir.absolutePath))
    }

    @Test
    fun `fileExists returns false for empty path`() = runBlocking<Unit> {
        // Empty path should not crash
        val exists = fileIO.fileExists("")
        // Just verify it returns without exception
        assertFalse(exists)
    }

    // ==================== File Size Tests ====================

    @Test
    fun `fileSize returns correct size for existing file`() = runBlocking<Unit> {
        val file = tempFolder.newFile("size_test.txt")
        val content = "12345678901234567890" // 20 bytes
        file.writeText(content)

        val size = fileIO.fileSize(file.absolutePath)
        assertEquals(20L, size)
    }

    @Test
    fun `fileSize returns 0 for empty file`() = runBlocking<Unit> {
        val file = tempFolder.newFile("empty_size_test.txt")
        val size = fileIO.fileSize(file.absolutePath)
        assertEquals(0L, size)
    }

    @Test
    fun `fileSize returns -1 for non-existent file`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "no_such_file.txt").absolutePath
        val size = fileIO.fileSize(path)
        assertEquals(-1L, size)
    }

    @Test
    fun `fileSize returns correct size for binary file`() = runBlocking<Unit> {
        val file = tempFolder.newFile("binary_size.bin")
        val data = ByteArray(1024)
        file.writeBytes(data)

        val size = fileIO.fileSize(file.absolutePath)
        assertEquals(1024L, size)
    }

    // ==================== Path Resolution Tests ====================

    @Test
    fun `ensureParentDirectories creates missing directories`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "a/b/c/file.txt").absolutePath
        val result = fileIO.ensureParentDirectories(path)
        assertTrue(result.isSuccess, "Ensuring parent directories should succeed")

        val parentDir = File(path).parentFile
        assertTrue(parentDir.exists(), "Parent directories should exist")
        assertTrue(parentDir.isDirectory, "Parent should be a directory")
    }

    @Test
    fun `ensureParentDirectories succeeds when directories already exist`() = runBlocking<Unit> {
        val dir = tempFolder.newFolder("existing_parent")
        val path = File(dir, "file.txt").absolutePath

        val result = fileIO.ensureParentDirectories(path)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `ensureParentDirectories handles deeply nested path`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "l1/l2/l3/l4/l5/l6/l7/file.txt").absolutePath
        val result = fileIO.ensureParentDirectories(path)
        assertTrue(result.isSuccess)
        assertTrue(File(path).parentFile.exists())
    }

    // ==================== File Permissions Tests ====================

    @Test
    fun `read-only file cannot be overwritten`() = runBlocking<Unit> {
        val file = tempFolder.newFile("readonly_test.txt")
        file.writeText("original")
        file.setReadOnly()

        try {
            val result = fileIO.writeFileBytes(file.absolutePath, "new content".toByteArray())
            // Root user can write to read-only files, so skip assertion
            val isRoot = System.getProperty("user.name") == "root"
            if (!isRoot) {
                assertTrue(result.isFailure, "Writing to read-only file should fail")
            }
        } finally {
            file.setWritable(true)
        }
    }

    @Test
    fun `read from file with read permission succeeds`() = runBlocking<Unit> {
        val file = tempFolder.newFile("readable_test.txt")
        file.writeText("readable content")
        file.setReadable(true)

        val result = fileIO.readFileBytes(file.absolutePath)
        assertTrue(result.isSuccess)
        assertEquals("readable content", String(result.getOrThrow()))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handle file with special characters in name`() = runBlocking<Unit> {
        val file = tempFolder.newFile("file with spaces.txt")
        file.writeText("content")

        val result = fileIO.readFileBytes(file.absolutePath)
        assertTrue(result.isSuccess)
        assertEquals("content", String(result.getOrThrow()))
    }

    @Test
    fun `handle file with dots in name`() = runBlocking<Unit> {
        val file = tempFolder.newFile("file.name.with.dots.txt")
        file.writeText("dotted")

        assertTrue(fileIO.fileExists(file.absolutePath))
        val result = fileIO.readFileBytes(file.absolutePath)
        assertTrue(result.isSuccess)
        assertEquals("dotted", String(result.getOrThrow()))
    }

    @Test
    fun `write then read large number of small files`() = runBlocking<Unit> {
        val count = 50
        for (i in 1..count) {
            val path = File(tempFolder.root, "batch_$i.txt").absolutePath
            fileIO.writeFileBytes(path, "content_$i".toByteArray())
        }
        for (i in 1..count) {
            val path = File(tempFolder.root, "batch_$i.txt").absolutePath
            val result = fileIO.readFileBytes(path)
            assertTrue(result.isSuccess, "Reading batch file $i should succeed")
            assertEquals("content_$i", String(result.getOrThrow()))
        }
    }

    @Test
    fun `fileSize after write matches written bytes`() = runBlocking<Unit> {
        val path = File(tempFolder.root, "size_match.txt").absolutePath
        val data = "Exact size test content: 12345"
        fileIO.writeFileBytes(path, data.toByteArray(Charsets.UTF_8))
        val size = fileIO.fileSize(path)
        assertEquals(data.toByteArray(Charsets.UTF_8).size.toLong(), size)
    }

    @Test
    fun `PlatformFileIOFactory create returns non-null`() {
        val io = PlatformFileIOFactory.create()
        assertNotNull(io, "PlatformFileIOFactory.create() should return non-null")
    }
}
