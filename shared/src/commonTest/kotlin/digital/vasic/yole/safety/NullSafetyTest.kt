/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Null Safety Tests
 *
 * Comprehensive tests for null safety across all modules.
 * Verifies that null values are handled gracefully without
 * throwing NullPointerException.
 *
 *########################################################*/

package digital.vasic.yole.safety

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.model.Document
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.smb.SmbService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Null safety tests to ensure all components handle null gracefully.
 */
class NullSafetyTest {

    // ==================== NETWORK STORAGE NULL SAFETY ====================

    @Test
    fun `NetworkStorage handles null space values`() {
        val storage = NetworkStorage(
            id = "test",
            name = "Test",
            type = StorageType.FTP,
            location = "ftp://test",
            totalSpace = null,
            usedSpace = null
        )

        assertNull(storage.availableSpace)
        assertNull(storage.usagePercentage)
        assertFalse(storage.isFull)
        assertFalse(storage.isLowOnSpace)
    }

    @Test
    fun `NetworkStorage handles partial space values`() {
        val storageWithTotal = NetworkStorage(
            id = "test",
            name = "Test",
            type = StorageType.FTP,
            location = "ftp://test",
            totalSpace = 1000L,
            usedSpace = null
        )

        assertNull(storageWithTotal.availableSpace)
        assertNull(storageWithTotal.usagePercentage)

        val storageWithUsed = NetworkStorage(
            id = "test",
            name = "Test",
            type = StorageType.FTP,
            location = "ftp://test",
            totalSpace = null,
            usedSpace = 500L
        )

        assertNull(storageWithUsed.availableSpace)
        assertNull(storageWithUsed.usagePercentage)
    }

    @Test
    fun `NetworkStorage handles zero total space`() {
        val storage = NetworkStorage(
            id = "test",
            name = "Test",
            type = StorageType.FTP,
            location = "ftp://test",
            totalSpace = 0L,
            usedSpace = 0L
        )

        assertEquals(0L, storage.availableSpace)
        assertNull(storage.usagePercentage) // Division by zero protection
        assertTrue(storage.isFull)
    }

    @Test
    fun `NetworkStorage supports extension with empty list`() {
        val storage = NetworkStorage(
            id = "test",
            name = "Test",
            type = StorageType.FTP,
            location = "ftp://test",
            supportedExtensions = emptyList()
        )

        assertTrue(storage.supportsExtension("txt"))
        assertTrue(storage.supportsExtension(""))
        assertTrue(storage.supportsFile("test.txt"))
        assertTrue(storage.supportsFile("noextension"))
    }

    @Test
    fun `NetworkStorage supports file with no extension`() {
        val storage = NetworkStorage(
            id = "test",
            name = "Test",
            type = StorageType.FTP,
            location = "ftp://test",
            supportedExtensions = listOf("txt", "md")
        )

        assertTrue(storage.supportsFile("noextension"))
    }

    // ==================== NETWORK DOCUMENT NULL SAFETY ====================

    @Test
    fun `NetworkDocument handles null metadata`() {
        val doc = NetworkDocument(
            id = "test",
            storageId = "storage",
            path = "/test.txt",
            name = "test.txt",
            size = 100L,
            modified = Clock.System.now(),
            type = DocumentType.FILE,
            metadata = null,
            permissions = emptySet()
        )

        assertNull(doc.metadata)
    }

    @Test
    fun `NetworkDocument handles empty permissions`() {
        val doc = NetworkDocument(
            id = "test",
            storageId = "storage",
            path = "/test.txt",
            name = "test.txt",
            size = 100L,
            modified = Clock.System.now(),
            type = DocumentType.FILE,
            permissions = emptySet()
        )

        assertTrue(doc.permissions.isEmpty())
        assertFalse(doc.permissions.contains(DocumentPermission.READ))
    }

    // ==================== CACHE ENTRY NULL SAFETY ====================

    @Test
    fun `CacheEntry handles edge case dates`() {
        val entry = CacheEntry(
            id = "test",
            documentId = "doc",
            localPath = "/cache/test",
            size = 100L,
            created = Clock.System.now(),
            expires = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
            checksum = null
        )

        assertNull(entry.checksum)
    }

    // ==================== NETWORK OPERATION NULL SAFETY ====================

    @Test
    fun `NetworkOperation handles null error`() {
        val op = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.UPLOAD,
            sourcePath = "/source",
            destinationPath = "/dest",
            totalBytes = 100L,
            transferredBytes = 50L,
            status = OperationStatus.IN_PROGRESS,
            startTime = Clock.System.now(),
            error = null
        )

        assertNull(op.error)
    }

    @Test
    fun `NetworkOperation handles completed state`() {
        val op = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.DOWNLOAD,
            sourcePath = "/source",
            destinationPath = "/dest",
            totalBytes = 100L,
            transferredBytes = 100L,
            status = OperationStatus.COMPLETED,
            startTime = Clock.System.now(),
            endTime = Clock.System.now()
        )

        assertNotNull(op.endTime)
        assertEquals(OperationStatus.COMPLETED, op.status)
    }

    // ==================== SERVICE NULL SAFETY ====================

    @Test
    fun `service handles null path gracefully`() = runTest {
        val service = FtpService(
            StorageConfig.FtpConfig(
                name = "test",
                host = "ftp.example.com",
                port = 21,
                username = "user",
                password = "pass",
                path = "/"
            )
        )

        // Empty path should be handled
        val result = service.validatePath("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `service handles root path`() = runTest {
        val service = SmbService(
            StorageConfig.SmbConfig(
                name = "test",
                host = "192.168.1.1",
                share = "share",
                domain = "WORKGROUP",
                username = "user",
                password = "pass",
                path = "/"
            )
        )

        val parentResult = service.getParentPath("/")
        assertNull(parentResult)
    }

    // ==================== DOCUMENT MODEL NULL SAFETY ====================

    @Test
    fun `Document handles empty content`() {
        val doc = Document(
            id = "test",
            content = "",
            format = TextFormat.PLAINTEXT
        )

        assertEquals("", doc.content)
    }

    @Test
    fun `Document handles null metadata fields`() {
        val doc = Document(
            id = "test",
            content = "content",
            format = TextFormat.MARKDOWN,
            title = null,
            author = null,
            tags = emptyList()
        )

        assertNull(doc.title)
        assertNull(doc.author)
        assertTrue(doc.tags.isEmpty())
    }

    // ==================== PARSER NULL SAFETY ====================

    @Test
    fun `parsers handle null-like edge cases`() = runTest {
        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser(),
            TodoTxtParser()
        )

        // These should not cause NPE
        val edgeCases = listOf(
            "",
            " ",
            "\u0000",
            "null",
            "undefined"
        )

        parsers.forEach { parser ->
            edgeCases.forEach { input ->
                val result = parser.parse(input)
                assertNotNull(result, "${parser::class.simpleName} should handle '$input'")
            }
        }
    }

    // ==================== STORAGE CONFIG NULL SAFETY ====================

    @Test
    fun `FtpConfig handles default values`() {
        val config = StorageConfig.FtpConfig(
            name = "test",
            host = "ftp.example.com",
            port = 21,
            username = "user",
            password = "pass",
            path = "/"
        )

        assertEquals("test", config.name)
        assertEquals("ftp.example.com", config.host)
    }

    @Test
    fun `SmbConfig handles default domain`() {
        val config = StorageConfig.SmbConfig(
            name = "test",
            host = "192.168.1.1",
            share = "share",
            domain = "",
            username = "user",
            password = "pass",
            path = "/"
        )

        assertEquals("", config.domain)
    }

    @Test
    fun `WebDavConfig handles optional fields`() {
        val config = StorageConfig.WebDavConfig(
            name = "test",
            url = "https://webdav.example.com",
            username = "user",
            password = "pass"
        )

        assertEquals("https://webdav.example.com", config.url)
    }

    // ==================== QUOTA INFO NULL SAFETY ====================

    @Test
    fun `QuotaInfo handles unlimited space`() {
        val quota = QuotaInfo(
            totalSpace = Long.MAX_VALUE,
            usedSpace = 0L,
            availableSpace = Long.MAX_VALUE
        )

        assertEquals(Long.MAX_VALUE, quota.totalSpace)
        assertEquals(Long.MAX_VALUE, quota.availableSpace)
    }

    @Test
    fun `QuotaInfo handles zero values`() {
        val quota = QuotaInfo(
            totalSpace = 0L,
            usedSpace = 0L,
            availableSpace = 0L
        )

        assertEquals(0L, quota.totalSpace)
        assertEquals(0L, quota.usedSpace)
        assertEquals(0L, quota.availableSpace)
    }

    // ==================== STORAGE INFO NULL SAFETY ====================

    @Test
    fun `StorageInfo handles minimal data`() {
        val info = StorageInfo(
            id = "test",
            name = "Test",
            type = StorageType.FTP
        )

        assertEquals("test", info.id)
        assertEquals("Test", info.name)
        assertEquals(StorageType.FTP, info.type)
    }

    // ==================== FILE INFO NULL SAFETY ====================

    @Test
    fun `FileInfo handles optional fields`() {
        val info = FileInfo(
            path = "/test.txt",
            name = "test.txt",
            size = 100L,
            isDirectory = false,
            modified = Clock.System.now(),
            permissions = emptySet()
        )

        assertEquals("/test.txt", info.path)
        assertFalse(info.isDirectory)
    }

    @Test
    fun `FileInfo handles directory type`() {
        val info = FileInfo(
            path = "/testdir",
            name = "testdir",
            size = 0L,
            isDirectory = true,
            modified = Clock.System.now()
        )

        assertTrue(info.isDirectory)
        assertEquals(0L, info.size)
    }
}
