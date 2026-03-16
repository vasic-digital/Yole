/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for PlatformFileIO interface and PlatformFileIOFactory
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Tests for [PlatformFileIO] interface contract.
 *
 * These tests verify the core file I/O operations provided by each platform
 * implementation via [PlatformFileIOFactory]:
 * - Read/write round-trip
 * - File existence checks
 * - File size queries
 * - Parent directory creation
 * - Error handling for missing files
 */
class PlatformFileIOTest {

    private fun createFileIO(): PlatformFileIO = PlatformFileIOFactory.create()

    // ==================== Factory Tests ====================

    @Test
    fun `PlatformFileIOFactory create returns non-null instance`() {
        val fileIO = createFileIO()
        assertNotNull(fileIO, "PlatformFileIOFactory.create() should return a non-null PlatformFileIO")
    }

    // ==================== readFileBytes Tests ====================

    @Test
    fun `readFileBytes returns failure for non-existent path`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val result = fileIO.readFileBytes("/non/existent/path/that/does/not/exist.txt")
        assertTrue(result.isFailure, "readFileBytes should return failure for non-existent file")
    }

    @Test
    fun `readFileBytes returns failure for empty path`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val result = fileIO.readFileBytes("")
        assertTrue(result.isFailure, "readFileBytes should return failure for empty path")
    }

    // ==================== writeFileBytes + readFileBytes Round-Trip ====================

    @Test
    fun `writeFileBytes followed by readFileBytes returns same content`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val testPath = generateTestPath("roundtrip_test.bin")
        val testContent = "Hello, PlatformFileIO!".encodeToByteArray()

        try {
            // Write
            val writeResult = fileIO.writeFileBytes(testPath, testContent)
            assertTrue(writeResult.isSuccess, "writeFileBytes should succeed: ${writeResult.exceptionOrNull()?.message}")

            // Read back
            val readResult = fileIO.readFileBytes(testPath)
            assertTrue(readResult.isSuccess, "readFileBytes should succeed after write: ${readResult.exceptionOrNull()?.message}")
            assertContentEquals(testContent, readResult.getOrNull(), "Read content should match written content")
        } finally {
            // Clean up: attempt to remove test file (best-effort)
            cleanupTestFile(fileIO, testPath)
        }
    }

    @Test
    fun `writeFileBytes with empty byte array succeeds`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val testPath = generateTestPath("empty_file.bin")

        try {
            val writeResult = fileIO.writeFileBytes(testPath, byteArrayOf())
            assertTrue(writeResult.isSuccess, "writeFileBytes with empty content should succeed")

            val readResult = fileIO.readFileBytes(testPath)
            assertTrue(readResult.isSuccess, "readFileBytes of empty file should succeed")
            assertEquals(0, readResult.getOrNull()?.size ?: -1, "Empty file should have 0 bytes")
        } finally {
            cleanupTestFile(fileIO, testPath)
        }
    }

    // ==================== fileExists Tests ====================

    @Test
    fun `fileExists returns false for non-existent path`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val exists = fileIO.fileExists("/non/existent/path/file.txt")
        assertFalse(exists, "fileExists should return false for non-existent file")
    }

    @Test
    fun `fileExists returns true after writing a file`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val testPath = generateTestPath("exists_test.bin")

        try {
            // File should not exist before writing
            assertFalse(fileIO.fileExists(testPath), "File should not exist before writing")

            // Write file
            fileIO.writeFileBytes(testPath, "test".encodeToByteArray())

            // File should exist after writing
            assertTrue(fileIO.fileExists(testPath), "File should exist after writing")
        } finally {
            cleanupTestFile(fileIO, testPath)
        }
    }

    // ==================== fileSize Tests ====================

    @Test
    fun `fileSize returns negative for non-existent path`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val size = fileIO.fileSize("/non/existent/path/file.txt")
        assertTrue(size < 0, "fileSize should return -1 for non-existent file, got: $size")
    }

    @Test
    fun `fileSize returns correct value after writing`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val testPath = generateTestPath("size_test.bin")
        val testContent = "Hello, World! 1234567890".encodeToByteArray()

        try {
            fileIO.writeFileBytes(testPath, testContent)

            val size = fileIO.fileSize(testPath)
            assertEquals(
                testContent.size.toLong(),
                size,
                "fileSize should return the correct byte count"
            )
        } finally {
            cleanupTestFile(fileIO, testPath)
        }
    }

    // ==================== ensureParentDirectories Tests ====================

    @Test
    fun `ensureParentDirectories does not throw for valid path`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val testPath = generateTestPath("subdir/nested/file.txt")

        val result = fileIO.ensureParentDirectories(testPath)
        assertTrue(result.isSuccess, "ensureParentDirectories should succeed: ${result.exceptionOrNull()?.message}")
    }

    @Test
    fun `ensureParentDirectories followed by write succeeds`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val testPath = generateTestPath("deep/nested/dir/file.txt")

        try {
            val ensureResult = fileIO.ensureParentDirectories(testPath)
            assertTrue(ensureResult.isSuccess, "ensureParentDirectories should succeed")

            val writeResult = fileIO.writeFileBytes(testPath, "data".encodeToByteArray())
            assertTrue(writeResult.isSuccess, "writeFileBytes should succeed after ensureParentDirectories")
        } finally {
            cleanupTestFile(fileIO, testPath)
        }
    }

    // ==================== Overwrite Tests ====================

    @Test
    fun `writeFileBytes overwrites existing file content`() = runBlocking<Unit> {
        val fileIO = createFileIO()
        val testPath = generateTestPath("overwrite_test.bin")

        try {
            // Write initial content
            fileIO.writeFileBytes(testPath, "first".encodeToByteArray())

            // Overwrite with new content
            fileIO.writeFileBytes(testPath, "second".encodeToByteArray())

            // Read back should return the new content
            val readResult = fileIO.readFileBytes(testPath)
            assertTrue(readResult.isSuccess, "readFileBytes should succeed")
            assertEquals(
                "second",
                readResult.getOrNull()?.decodeToString(),
                "Content should be overwritten"
            )
        } finally {
            cleanupTestFile(fileIO, testPath)
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Generate a platform-appropriate temporary test file path.
     * Uses the yole_file_ prefix on Wasm (localStorage) and /tmp/ on other platforms.
     */
    private fun generateTestPath(suffix: String): String {
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return "/tmp/yole_test_${timestamp}_$suffix"
    }

    /**
     * Best-effort cleanup of a test file by overwriting with empty bytes.
     * On Wasm/localStorage, this effectively removes the entry.
     */
    private suspend fun cleanupTestFile(fileIO: PlatformFileIO, path: String) {
        try {
            // On real filesystems, writeFileBytes with empty content leaves a 0-byte file.
            // This is acceptable for test cleanup. A proper delete would require
            // platform-specific code, which is out of scope for this interface.
            fileIO.writeFileBytes(path, byteArrayOf())
        } catch (_: Exception) {
            // Best-effort cleanup; ignore errors
        }
    }
}
