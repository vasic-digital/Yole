/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * API Consistency Tests
 *
 * Comprehensive tests verifying API consistency across
 * all services and parsers.
 *
 *########################################################*/

package digital.vasic.yole.api

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.smb.SmbService
import digital.vasic.yole.network.protocols.webdav.WebDavService
import digital.vasic.yole.network.protocols.git.GitService
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * API consistency tests ensuring uniform behavior across components.
 */
class ApiConsistencyTest {

    // ==================== PARSER API CONSISTENCY ====================

    @Test
    fun `all parsers implement supportedFormat correctly`() {
        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser(),
            TodoTxtParser(),
            CsvParser(),
            LatexParser()
        )

        parsers.forEach { parser ->
            val format = parser.supportedFormat
            assertNotNull(format.id, "${parser::class.simpleName} should have format id")
            assertNotNull(format.name, "${parser::class.simpleName} should have format name")
            assertTrue(format.id.isNotEmpty(), "${parser::class.simpleName} format id should not be empty")
        }
    }

    @Test
    fun `all parsers return ParsedDocument with required fields`() = runTest {
        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser(),
            TodoTxtParser()
        )

        val content = "Test content"

        parsers.forEach { parser ->
            val result = parser.parse(content)

            assertNotNull(result.format, "${parser::class.simpleName} should set format")
            assertEquals(content, result.rawContent, "${parser::class.simpleName} should preserve raw content")
            assertNotNull(result.parsedContent, "${parser::class.simpleName} should set parsed content")
            assertNotNull(result.metadata, "${parser::class.simpleName} should set metadata")
            assertNotNull(result.errors, "${parser::class.simpleName} should set errors")
        }
    }

    @Test
    fun `all parsers handle empty content consistently`() = runTest {
        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser(),
            TodoTxtParser(),
            CsvParser(),
            LatexParser()
        )

        parsers.forEach { parser ->
            val result = parser.parse("")

            assertEquals("", result.rawContent, "${parser::class.simpleName} should preserve empty content")
            assertTrue(result.errors.isEmpty(), "${parser::class.simpleName} should not error on empty content")
        }
    }

    @Test
    fun `canParse returns correct values for own format`() = runTest {
        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser(),
            TodoTxtParser()
        )

        parsers.forEach { parser ->
            assertTrue(
                parser.canParse(parser.supportedFormat),
                "${parser::class.simpleName} should be able to parse its own format"
            )
        }
    }

    @Test
    fun `validate returns list for all parsers`() = runTest {
        val parsers = listOf(
            MarkdownParser(),
            PlaintextParser(),
            TodoTxtParser()
        )

        parsers.forEach { parser ->
            val errors = parser.validate("test content")
            assertNotNull(errors, "${parser::class.simpleName} validate should return non-null list")
        }
    }

    // ==================== SERVICE API CONSISTENCY ====================

    @Test
    fun `all services implement isOnline consistently`() = runTest {
        val services = listOf(
            createFtpService("ftp"),
            createSmbService("smb"),
            createWebDavService("webdav"),
            createGitService("git")
        )

        services.forEach { service ->
            // Initially should be offline
            assertFalse(service.isOnline, "${service::class.simpleName} should start offline")

            // After connect should be online
            service.connect()
            assertTrue(service.isOnline, "${service::class.simpleName} should be online after connect")

            // After disconnect should be offline
            service.disconnect()
            assertFalse(service.isOnline, "${service::class.simpleName} should be offline after disconnect")
        }
    }

    @Test
    fun `all services return Result from operations`() = runTest {
        val services = listOf(
            createFtpService("ftp"),
            createSmbService("smb"),
            createWebDavService("webdav"),
            createGitService("git")
        )

        services.forEach { service ->
            service.connect()

            // getFileInfo returns Result
            val fileInfoResult = service.getFileInfo("/test.txt")
            assertTrue(
                fileInfoResult.isSuccess || fileInfoResult.isFailure,
                "${service::class.simpleName} getFileInfo should return valid Result"
            )

            // createFolder returns Result
            val createFolderResult = service.createFolder("/testfolder")
            assertTrue(
                createFolderResult.isSuccess || createFolderResult.isFailure,
                "${service::class.simpleName} createFolder should return valid Result"
            )

            // exists returns Result
            val existsResult = service.exists("/path")
            assertTrue(
                existsResult.isSuccess || existsResult.isFailure,
                "${service::class.simpleName} exists should return valid Result"
            )

            // validatePath returns Result
            val validateResult = service.validatePath("/valid/path")
            assertTrue(
                validateResult.isSuccess || validateResult.isFailure,
                "${service::class.simpleName} validatePath should return valid Result"
            )

            service.disconnect()
        }
    }

    @Test
    fun `all services return correct storage type`() = runTest {
        val serviceTypeMap = mapOf(
            createFtpService("ftp") to StorageType.FTP,
            createSmbService("smb") to StorageType.SMB,
            createWebDavService("webdav") to StorageType.WEBDAV,
            createGitService("git") to StorageType.GIT
        )

        serviceTypeMap.forEach { (service, expectedType) ->
            val storageInfo = service.getStorageInfo()
            assertEquals(
                expectedType,
                storageInfo.type,
                "${service::class.simpleName} should return correct storage type"
            )
        }
    }

    @Test
    fun `all services implement getQuotaInfo consistently`() = runTest {
        val services = listOf(
            createFtpService("ftp"),
            createSmbService("smb"),
            createWebDavService("webdav"),
            createGitService("git")
        )

        services.forEach { service ->
            val result = service.getQuotaInfo()

            assertTrue(result.isSuccess, "${service::class.simpleName} getQuotaInfo should succeed")

            val quota = result.getOrNull()
            assertNotNull(quota, "${service::class.simpleName} should return quota info")
            assertTrue(quota.totalSpace >= 0, "${service::class.simpleName} totalSpace should be non-negative")
            assertTrue(quota.usedSpace >= 0, "${service::class.simpleName} usedSpace should be non-negative")
        }
    }

    @Test
    fun `all services implement clearCache consistently`() = runTest {
        val services = listOf(
            createFtpService("ftp"),
            createSmbService("smb"),
            createWebDavService("webdav"),
            createGitService("git")
        )

        services.forEach { service ->
            val result = service.clearCache()

            assertTrue(
                result.isSuccess,
                "${service::class.simpleName} clearCache should return success"
            )
        }
    }

    // ==================== FORMAT REGISTRY API CONSISTENCY ====================

    @Test
    fun `getById returns null for unknown format`() {
        val result = FormatRegistry.getById("nonexistent_format_12345")
        assertNull(result)
    }

    @Test
    fun `getByExtension handles various input formats`() {
        // With dot
        val md1 = FormatRegistry.getByExtension(".md")
        assertNotNull(md1)

        // Without dot
        val md2 = FormatRegistry.getByExtension("md")
        assertNotNull(md2)

        // Case variations
        val md3 = FormatRegistry.getByExtension("MD")
        assertNotNull(md3)

        // All should return same format
        assertEquals(md1?.id, md2?.id)
        assertEquals(md2?.id, md3?.id)
    }

    @Test
    fun `detectByExtension always returns a format`() {
        val extensions = listOf(
            "md", "txt", "csv", "unknown", "", "xyz"
        )

        extensions.forEach { ext ->
            val format = FormatRegistry.detectByExtension(ext)
            assertNotNull(format, "detectByExtension should return non-null for '$ext'")
        }
    }

    @Test
    fun `detectByFilename handles edge cases`() {
        val filenames = listOf(
            "file.md",
            "FILE.MD",
            "file",
            ".hidden",
            "file.multiple.dots.md",
            ""
        )

        filenames.forEach { filename ->
            val format = FormatRegistry.detectByFilename(filename)
            assertNotNull(format, "detectByFilename should return non-null for '$filename'")
        }
    }

    // ==================== DATA CLASS API CONSISTENCY ====================

    @Test
    fun `NetworkStorage provides computed properties consistently`() {
        val storage = NetworkStorage(
            id = "test",
            name = "Test",
            type = StorageType.FTP,
            location = "ftp://test",
            totalSpace = 1000L,
            usedSpace = 400L
        )

        assertEquals(600L, storage.availableSpace)
        assertEquals(0.4, storage.usagePercentage!!, 0.01)
        assertFalse(storage.isFull)
        assertFalse(storage.isLowOnSpace)

        val fullStorage = storage.copy(usedSpace = 1000L)
        assertTrue(fullStorage.isFull)

        val lowStorage = storage.copy(usedSpace = 950L)
        assertTrue(lowStorage.isLowOnSpace)
    }

    @Test
    fun `NetworkStorage copy methods work correctly`() {
        val storage = NetworkStorage(
            id = "test",
            name = "Test",
            type = StorageType.FTP,
            location = "ftp://test"
        )

        val withOnline = storage.withOnlineStatus(true)
        assertTrue(withOnline.isOnline)
        assertEquals(storage.id, withOnline.id)

        val now = kotlinx.datetime.Clock.System.now()
        val withSync = storage.withLastSync(now)
        assertEquals(now, withSync.lastSync)

        val withSpace = storage.withSpaceInfo(1000L, 500L)
        assertEquals(1000L, withSpace.totalSpace)
        assertEquals(500L, withSpace.usedSpace)
    }

    @Test
    fun `ParsedDocument implements equals and hashCode correctly`() = runTest {
        val parser = MarkdownParser()
        val doc1 = parser.parse("# Test")
        val doc2 = parser.parse("# Test")
        val doc3 = parser.parse("# Different")

        assertEquals(doc1, doc2)
        assertEquals(doc1.hashCode(), doc2.hashCode())
        assertNotEquals(doc1, doc3)
    }

    @Test
    fun `ParsedDocument copy works correctly`() = runTest {
        val parser = MarkdownParser()
        val doc = parser.parse("# Test")

        val copy = doc.copy(rawContent = "# Modified")

        assertEquals("# Modified", copy.rawContent)
        assertEquals(doc.format, copy.format)
        assertEquals(doc.metadata, copy.metadata)
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun createFtpService(name: String) = FtpService(
        StorageConfig.FtpConfig(
            name = name,
            host = "ftp.example.com",
            port = 21,
            username = "user",
            password = "pass",
            rootPath = "/"
        )
    )

    private fun createSmbService(name: String) = SmbService(
        StorageConfig.SmbConfig(
            name = name,
            host = "192.168.1.100",
            share = "docs",
            domain = "WORKGROUP",
            username = "user",
            password = "pass",
            path = "/"
        )
    )

    private fun createWebDavService(name: String) = WebDavService(
        StorageConfig.WebDavConfig(
            name = name,
            url = "https://webdav.example.com",
            username = "user",
            password = "pass"
        )
    )

    private fun createGitService(name: String) = GitService(
        StorageConfig.GitConfig(
            name = name,
            repositoryUrl = "https://github.com/example/repo.git",
            branch = "main",
            username = "user",
            personalAccessToken = "token",
            localCachePath = "/tmp/git-cache"
        )
    )
}
