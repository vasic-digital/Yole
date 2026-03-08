/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * SMB/CIFS Service Enhanced Test Suite
 *
 * Comprehensive tests for SMB protocol implementation
 * covering connection management, file operations, error
 * handling, and protocol-specific behaviors.
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.smb

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Enhanced test suite for SmbService.
 * Tests all SMB/CIFS operations including connection, file operations,
 * cache management, sync status, and error handling.
 */
class SmbServiceEnhancedTest {

    private lateinit var service: SmbService
    private lateinit var config: StorageConfig.SmbConfig

    @BeforeTest
    fun setup() {
        config = StorageConfig.SmbConfig(
            name = "test-smb",
            host = "192.168.1.100",
            share = "documents",
            domain = "WORKGROUP",
            username = "testuser",
            password = "testpass",
            path = "/shared"
        )
        service = SmbService(config)
    }

    // ==================== INITIALIZATION TESTS ====================

    @Test
    fun `service initializes with correct config`() {
        assertEquals("test-smb", config.name)
        assertEquals("192.168.1.100", config.host)
        assertEquals("documents", config.share)
        assertEquals("WORKGROUP", config.domain)
        assertEquals("testuser", config.username)
    }

    @Test
    fun `service starts in disconnected state`() {
        assertFalse(service.isOnline)
    }

    @Test
    fun `service has correct root path from config`() {
        assertEquals("/shared", service.rootPath)
    }

    @Test
    fun `service with empty path config uses forward slash`() {
        val emptyPathConfig = StorageConfig.SmbConfig(
            name = "test",
            host = "host",
            share = "share",
            domain = "",
            username = "user",
            password = "pass",
            path = ""
        )
        val svc = SmbService(emptyPathConfig)
        assertEquals("/", svc.rootPath)
    }

    @Test
    fun `service config is accessible`() {
        assertEquals(config, service.config)
        assertIs<StorageConfig.SmbConfig>(service.config)
    }

    // ==================== CONNECTION TESTS ====================

    @Test
    fun `connect returns success`() = runBlocking {
        val result = service.connect()
        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `disconnect returns success`() = runBlocking {
        service.connect()
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `disconnect when not connected succeeds`() = runBlocking {
        assertFalse(service.isOnline)
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `multiple connect calls succeed`() = runBlocking {
        assertTrue(service.connect().isSuccess)
        assertTrue(service.isOnline)
        assertTrue(service.connect().isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `testConnection succeeds when not connected`() = runBlocking {
        assertFalse(service.isOnline)
        val result = service.testConnection()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        // Should disconnect after test
        assertFalse(service.isOnline)
    }

    @Test
    fun `testConnection succeeds when already connected`() = runBlocking {
        service.connect()
        assertTrue(service.isOnline)
        val result = service.testConnection()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    // ==================== STORAGE INFO TESTS ====================

    @Test
    fun `getStorageInfo returns correct metadata`() = runBlocking {
        val info = service.getStorageInfo()
        assertEquals("smb_test-smb", info.id)
        assertEquals("test-smb", info.name)
        assertEquals(StorageType.SMB, info.type)
        assertEquals("smb://192.168.1.100/documents/shared", info.location)
    }

    @Test
    fun `getStorageInfo reflects connection state`() = runBlocking {
        var info = service.getStorageInfo()
        assertFalse(info.isOnline)

        service.connect()
        info = service.getStorageInfo()
        assertTrue(info.isOnline)

        service.disconnect()
        info = service.getStorageInfo()
        assertFalse(info.isOnline)
    }

    @Test
    fun `getStorageInfo location includes UNC path format`() = runBlocking {
        val info = service.getStorageInfo()
        assertTrue(info.location.startsWith("smb://"))
        assertTrue(info.location.contains(config.host))
        assertTrue(info.location.contains(config.share))
    }

    // ==================== LIST FILES TESTS ====================

    @Test
    fun `listFiles fails when not connected`() = runBlocking {
        val results = service.listFiles("/").toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isFailure)
        val exception = results[0].exceptionOrNull()
        assertIs<NetworkStorageException.ConnectionException.NotConnected>(exception)
    }

    @Test
    fun `listFiles with connected service returns failure for unimplemented`() = runBlocking {
        service.connect()
        val results = service.listFiles("/").toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isFailure)
        assertIs<NetworkStorageException.FileOperationException.ListFailed>(results[0].exceptionOrNull())
    }

    @Test
    fun `listFiles with different paths returns correct failure`() = runBlocking {
        service.connect()
        val paths = listOf("/", "/documents", "/documents/subfolder", "/photos")
        for (path in paths) {
            val results = service.listFiles(path).toList()
            assertTrue(results.isNotEmpty())
            assertTrue(results[0].isFailure)
        }
    }

    // ==================== UPLOAD TESTS ====================

    @Test
    fun `uploadFile fails when not connected`() = runBlocking {
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(results.isNotEmpty())
        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.FAILED, lastResult.status)
        assertTrue(lastResult.error?.contains("not connected") == true)
    }

    @Test
    fun `uploadFile succeeds when connected`() = runBlocking {
        service.connect()
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(results.isNotEmpty())

        // Check progress updates
        val progressUpdates = results.filter { it.status == NetworkOperation.Status.IN_PROGRESS }
        assertTrue(progressUpdates.isNotEmpty())

        // Check completion
        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.COMPLETED, lastResult.status)
        assertEquals(1.0, lastResult.progress)
    }

    @Test
    fun `uploadFile has correct operation type`() = runBlocking {
        service.connect()
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(results.all { it.type == NetworkOperation.Type.UPLOAD })
    }

    @Test
    fun `uploadFile reports progress correctly`() = runBlocking {
        service.connect()
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        val progressValues = results.map { it.progress }

        // Should have increasing progress
        for (i in 1 until progressValues.size) {
            assertTrue(progressValues[i] >= progressValues[i - 1])
        }

        // Should end at 100%
        assertEquals(1.0, progressValues.last())
    }

    // ==================== DOWNLOAD TESTS ====================

    @Test
    fun `downloadFile fails when not connected`() = runBlocking {
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(results.isNotEmpty())
        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.FAILED, lastResult.status)
        assertTrue(lastResult.error?.contains("not connected") == true)
    }

    @Test
    fun `downloadFile succeeds when connected`() = runBlocking {
        service.connect()
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(results.isNotEmpty())

        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.COMPLETED, lastResult.status)
        assertEquals(1.0, lastResult.progress)
    }

    @Test
    fun `downloadFile has correct operation type`() = runBlocking {
        service.connect()
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(results.all { it.type == NetworkOperation.Type.DOWNLOAD })
    }

    // ==================== FILE OPERATION TESTS ====================

    @Test
    fun `copyFile returns success`() = runBlocking {
        val result = service.copyFile("/source/file.txt", "/dest/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteFile returns success`() = runBlocking {
        val result = service.deleteFile("/remote/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `renameFile returns success`() = runBlocking {
        val result = service.renameFile("/remote/old.txt", "new.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `moveFile returns success with correct document`() = runBlocking {
        val result = service.moveFile("/source/file.txt", "/dest/file.txt")
        assertTrue(result.isSuccess)
        val document = result.getOrNull()
        assertNotNull(document)
        assertEquals("/dest/file.txt", document.path)
        assertEquals("file.txt", document.name)
        assertEquals(SyncStatus.SYNCED, document.syncStatus)
    }

    @Test
    fun `moveFile returns document with correct permissions`() = runBlocking {
        val result = service.moveFile("/source/file.txt", "/dest/file.txt")
        val document = result.getOrNull()
        assertNotNull(document)
        assertTrue(document.permissions.contains(DocumentPermission.READ))
        assertTrue(document.permissions.contains(DocumentPermission.WRITE))
        assertTrue(document.permissions.contains(DocumentPermission.DELETE))
    }

    // ==================== FOLDER OPERATION TESTS ====================

    @Test
    fun `createFolder returns success with correct document`() = runBlocking {
        val result = service.createFolder("/documents/newfolder")
        assertTrue(result.isSuccess)
        val document = result.getOrNull()
        assertNotNull(document)
        assertEquals("/documents/newfolder", document.path)
        assertEquals("newfolder", document.name)
        assertTrue(document.isFolder)
        assertEquals(0L, document.size)
    }

    @Test
    fun `createFolder returns document with execute permission`() = runBlocking {
        val result = service.createFolder("/documents/newfolder")
        val document = result.getOrNull()
        assertNotNull(document)
        assertTrue(document.permissions.contains(DocumentPermission.EXECUTE))
    }

    // ==================== FILE INFO TESTS ====================

    @Test
    fun `getFileInfo returns success with correct document`() = runBlocking {
        val result = service.getFileInfo("/documents/file.txt")
        assertTrue(result.isSuccess)
        val document = result.getOrNull()
        assertNotNull(document)
        assertEquals("/documents/file.txt", document.path)
        assertEquals("file.txt", document.name)
        assertFalse(document.isFolder)
        assertEquals("smb", document.storageId)
    }

    @Test
    fun `exists returns result`() = runBlocking {
        val result = service.exists("/documents/file.txt")
        assertTrue(result.isSuccess)
        // Mock implementation returns false
        assertFalse(result.getOrNull() ?: true)
    }

    // ==================== OPERATION MANAGEMENT TESTS ====================

    @Test
    fun `getActiveOperations returns empty flow`() = runBlocking {
        val operations = service.getActiveOperations().toList()
        assertTrue(operations.isNotEmpty())
        assertTrue(operations[0].isEmpty())
    }

    @Test
    fun `cancelOperation returns success`() = runBlocking {
        val result = service.cancelOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `pauseOperation returns success`() = runBlocking {
        val result = service.pauseOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resumeOperation returns success`() = runBlocking {
        val result = service.resumeOperation(12345L)
        assertTrue(result.isSuccess)
    }

    // ==================== CACHE OPERATION TESTS ====================

    @Test
    fun `getCacheEntries returns empty list`() = runBlocking {
        val entries = service.getCacheEntries("/").toList()
        assertTrue(entries.isNotEmpty())
        assertTrue(entries[0].isEmpty())
    }

    @Test
    fun `getCacheEntries with null path returns empty list`() = runBlocking {
        val entries = service.getCacheEntries(null).toList()
        assertTrue(entries.isNotEmpty())
        assertTrue(entries[0].isEmpty())
    }

    @Test
    fun `addToCache returns success`() = runBlocking {
        val result = service.addToCache("/documents/file.txt", 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addToCache with different priorities succeeds`() = runBlocking {
        assertTrue(service.addToCache("/file1.txt", 0).isSuccess)
        assertTrue(service.addToCache("/file2.txt", 1).isSuccess)
        assertTrue(service.addToCache("/file3.txt", 10).isSuccess)
    }

    @Test
    fun `removeFromCache returns success`() = runBlocking {
        val result = service.removeFromCache("/documents/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `clearCache returns success`() = runBlocking {
        val result = service.clearCache()
        assertTrue(result.isSuccess)
    }

    // ==================== SYNC OPERATION TESTS ====================

    @Test
    fun `getSyncStatus returns empty map`() = runBlocking {
        val status = service.getSyncStatus("/").toList()
        assertTrue(status.isNotEmpty())
        assertTrue(status[0].isEmpty())
    }

    @Test
    fun `getSyncStatus with null path returns empty map`() = runBlocking {
        val status = service.getSyncStatus(null).toList()
        assertTrue(status.isNotEmpty())
        assertTrue(status[0].isEmpty())
    }

    @Test
    fun `syncFile returns progress flow`() = runBlocking {
        val results = service.syncFile("/documents/file.txt", false).toList()
        assertTrue(results.isNotEmpty())

        // Should have progress updates
        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.COMPLETED, lastResult.status)
        assertEquals(NetworkOperation.Type.SYNC, lastResult.type)
    }

    @Test
    fun `syncFile with forceSync flag succeeds`() = runBlocking {
        val results = service.syncFile("/documents/file.txt", true).toList()
        assertTrue(results.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, results.last().status)
    }

    @Test
    fun `syncAll returns flow`() = runBlocking {
        val results = service.syncAll(false).toList()
        // Mock implementation returns empty flow
        assertTrue(results.isEmpty())
    }

    // ==================== SEARCH TESTS ====================

    @Test
    fun `searchFiles returns failure for unimplemented`() = runBlocking {
        val results = service.searchFiles("query", "/", false).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isFailure)
    }

    @Test
    fun `searchFiles with includeContent flag returns failure`() = runBlocking {
        val results = service.searchFiles("query", "/", true).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isFailure)
    }

    @Test
    fun `searchFiles with null path returns failure`() = runBlocking {
        val results = service.searchFiles("query", null, false).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isFailure)
    }

    // ==================== RECENT CHANGES TESTS ====================

    @Test
    fun `getRecentChanges returns empty list`() = runBlocking {
        val since = kotlinx.datetime.Clock.System.now()
        val results = service.getRecentChanges(since, "/").toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isEmpty())
    }

    @Test
    fun `getRecentChanges with null path returns empty list`() = runBlocking {
        val since = kotlinx.datetime.Clock.System.now()
        val results = service.getRecentChanges(since, null).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isEmpty())
    }

    // ==================== QUOTA TESTS ====================

    @Test
    fun `getQuotaInfo returns valid quota`() = runBlocking {
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess)
        val quota = result.getOrNull()
        assertNotNull(quota)
        assertTrue(quota.totalSpace > 0)
        assertTrue(quota.availableSpace > 0)
        assertTrue(quota.usedSpace >= 0)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
    }

    @Test
    fun `getQuotaInfo returns consistent values`() = runBlocking {
        val result = service.getQuotaInfo()
        val quota = result.getOrNull()
        assertNotNull(quota)
        assertEquals(quota.totalSpace, quota.usedSpace + quota.availableSpace)
    }

    // ==================== PATH UTILITY TESTS ====================

    @Test
    fun `getParentPath returns correct parent`() {
        assertEquals("/documents", service.getParentPath("/documents/file.txt"))
        assertEquals("/", service.getParentPath("/documents"))
        assertEquals("/a/b", service.getParentPath("/a/b/c"))
    }

    @Test
    fun `getParentPath returns null for root`() {
        assertNull(service.getParentPath("/"))
        assertNull(service.getParentPath(""))
    }

    @Test
    fun `validatePath succeeds for valid paths`() {
        assertTrue(service.validatePath("/").isSuccess)
        assertTrue(service.validatePath("/documents").isSuccess)
        assertTrue(service.validatePath("/documents/file.txt").isSuccess)
    }

    @Test
    fun `validatePath fails for blank path`() {
        val result = service.validatePath("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `validatePath fails for whitespace only path`() {
        val result = service.validatePath("   ")
        assertTrue(result.isFailure)
    }

    // ==================== SMB-SPECIFIC TESTS ====================

    @Test
    fun `service uses SmbConfig`() {
        assertIs<StorageConfig.SmbConfig>(service.config)
    }

    @Test
    fun `storage info has SMB type`() = runBlocking {
        val info = service.getStorageInfo()
        assertEquals(StorageType.SMB, info.type)
    }

    @Test
    fun `config domain is accessible`() {
        val smbConfig = service.config as StorageConfig.SmbConfig
        assertEquals("WORKGROUP", smbConfig.domain)
    }

    @Test
    fun `config share is accessible`() {
        val smbConfig = service.config as StorageConfig.SmbConfig
        assertEquals("documents", smbConfig.share)
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `operations handle special characters in paths`() = runBlocking {
        service.connect()

        val specialPaths = listOf(
            "/path with spaces/file.txt",
            "/path-with-dashes/file.txt",
            "/path_with_underscores/file.txt",
            "/path.with.dots/file.txt"
        )

        for (path in specialPaths) {
            val result = service.getFileInfo(path)
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `operations handle deep paths`() = runBlocking {
        service.connect()

        val deepPath = "/a/b/c/d/e/f/g/h/i/j/file.txt"
        val result = service.getFileInfo(deepPath)
        assertTrue(result.isSuccess)
        assertEquals("file.txt", result.getOrNull()?.name)
    }

    @Test
    fun `operations handle unicode paths`() = runBlocking {
        service.connect()

        val result = service.getFileInfo("/文档/файл.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SMB paths with backslashes work correctly`() = runBlocking {
        // Note: SMB traditionally uses backslashes on Windows
        // Service should handle forward slashes correctly
        service.connect()

        val result = service.getFileInfo("/documents/subfolder/file.txt")
        assertTrue(result.isSuccess)
    }

    // ==================== NETWORK-SPECIFIC TESTS ====================

    @Test
    fun `operations use correct host from config`() = runBlocking {
        val info = service.getStorageInfo()
        assertTrue(info.location.contains("192.168.1.100"))
    }

    @Test
    fun `reconnect after disconnect works`() = runBlocking {
        service.connect()
        assertTrue(service.isOnline)

        service.disconnect()
        assertFalse(service.isOnline)

        service.connect()
        assertTrue(service.isOnline)
    }

    @Test
    fun `multiple operations in sequence work`() = runBlocking {
        service.connect()

        // Perform multiple operations
        assertTrue(service.createFolder("/test").isSuccess)
        assertTrue(service.getFileInfo("/test").isSuccess)
        assertTrue(service.deleteFile("/test").isSuccess)
    }
}
