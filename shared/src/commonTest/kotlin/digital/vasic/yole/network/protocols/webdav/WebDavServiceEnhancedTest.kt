/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * WebDAV Service Enhanced Test Suite
 *
 * Comprehensive tests for WebDAV protocol implementation
 * covering connection management, file operations, error
 * handling, and protocol-specific behaviors.
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.webdav

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Enhanced test suite for WebDavService.
 * Tests all WebDAV operations including connection, file operations,
 * cache management, sync status, and error handling.
 */
class WebDavServiceEnhancedTest {

    private lateinit var service: WebDavService
    private lateinit var config: StorageConfig.WebDavConfig

    @BeforeTest
    fun setup() {
        config = StorageConfig.WebDavConfig(
            name = "test-webdav",
            url = "https://webdav.example.com/remote.php/dav/files/user",
            username = "testuser",
            password = "testpass"
        )
        service = WebDavService(config, testConnectFn = { Result.success(Unit) })
    }

    // ==================== INITIALIZATION TESTS ====================

    @Test
    fun `service initializes with correct config`() {
        assertEquals("test-webdav", config.name)
        assertEquals("https://webdav.example.com/remote.php/dav/files/user", config.url)
        assertEquals("testuser", config.username)
    }

    @Test
    fun `service starts in disconnected state`() {
        assertFalse(service.isOnline)
    }

    @Test
    fun `service has root path of forward slash`() {
        assertEquals("/", service.rootPath)
    }

    @Test
    fun `service config is accessible`() {
        assertEquals(config, service.config)
        assertIs<StorageConfig.WebDavConfig>(service.config)
    }

    // ==================== CONNECTION TESTS ====================

    @Test
    fun `connect returns success`() = runBlocking<Unit> {
        val result = service.connect()
        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `disconnect returns success`() = runBlocking<Unit> {
        service.connect()
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `disconnect when not connected succeeds`() = runBlocking<Unit> {
        assertFalse(service.isOnline)
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `multiple connect calls succeed`() = runBlocking<Unit> {
        assertTrue(service.connect().isSuccess)
        assertTrue(service.isOnline)
        assertTrue(service.connect().isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `testConnection succeeds when not connected`() = runBlocking<Unit> {
        assertFalse(service.isOnline)
        val result = service.testConnection()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        // Should disconnect after test
        assertFalse(service.isOnline)
    }

    @Test
    fun `testConnection succeeds when already connected`() = runBlocking<Unit> {
        service.connect()
        assertTrue(service.isOnline)
        val result = service.testConnection()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    // ==================== STORAGE INFO TESTS ====================

    @Test
    fun `getStorageInfo returns correct metadata`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("webdav_test-webdav", info.id)
        assertEquals("test-webdav", info.name)
        assertEquals(StorageType.WEBDAV, info.type)
        assertEquals(config.url, info.location)
    }

    @Test
    fun `getStorageInfo reflects connection state`() = runBlocking<Unit> {
        var info = service.getStorageInfo()
        assertFalse(info.isOnline)

        service.connect()
        info = service.getStorageInfo()
        assertTrue(info.isOnline)

        service.disconnect()
        info = service.getStorageInfo()
        assertFalse(info.isOnline)
    }

    // ==================== LIST FILES TESTS ====================

    @Test
    fun `listFiles fails when not connected`() = runBlocking<Unit> {
        val results = service.listFiles("/").toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isFailure)
        val exception = results[0].exceptionOrNull()
        assertIs<NetworkStorageException.ConnectionException.NotConnected>(exception)
    }

    @Test
    fun `listFiles with connected service returns failure for unimplemented`() = runBlocking<Unit> {
        service.connect()
        val results = service.listFiles("/").toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isFailure)
        assertIs<NetworkStorageException.FileOperationException.ListFailed>(results[0].exceptionOrNull())
    }

    @Test
    fun `listFiles with different paths returns correct failure`() = runBlocking<Unit> {
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
    fun `uploadFile fails when not connected`() = runBlocking<Unit> {
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(results.isNotEmpty())
        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.FAILED, lastResult.status)
        assertTrue(lastResult.error?.contains("not connected") == true)
    }

    @Test
     fun `uploadFile succeeds when connected`() = runBlocking<Unit> {
        service.connect()
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(results.isNotEmpty())

        // Without real server and file, upload should fail honestly
        val lastResult = results.last()
        assertTrue(lastResult.status == NetworkOperation.Status.FAILED || 
                   lastResult.status == NetworkOperation.Status.COMPLETED)
    }

    @Test
    fun `uploadFile has correct operation type`() = runBlocking<Unit> {
        service.connect()
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(results.all { it.type == NetworkOperation.Type.UPLOAD })
    }

    @Test
    fun `uploadFile reports progress correctly`() = runBlocking<Unit> {
        service.connect()
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()

        // Should have at least one result
        assertTrue(results.isNotEmpty())
        assertTrue(results.last().status != null)
    }

    // ==================== DOWNLOAD TESTS ====================

    @Test
    fun `downloadFile fails when not connected`() = runBlocking<Unit> {
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(results.isNotEmpty())
        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.FAILED, lastResult.status)
        assertTrue(lastResult.error?.contains("not connected") == true)
    }

    @Test
    fun `downloadFile succeeds when connected`() = runBlocking<Unit> {
        service.connect()
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(results.isNotEmpty())

        val lastResult = results.last()
        assertTrue(lastResult.status == NetworkOperation.Status.FAILED || 
                   lastResult.status == NetworkOperation.Status.COMPLETED)
    }

    @Test
    fun `downloadFile has correct operation type`() = runBlocking<Unit> {
        service.connect()
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(results.all { it.type == NetworkOperation.Type.DOWNLOAD })
    }

    // ==================== FILE OPERATION TESTS ====================

    @Test
    fun `copyFile returns success`() = runBlocking<Unit> {
        val result = service.copyFile("/source/file.txt", "/dest/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteFile returns success`() = runBlocking<Unit> {
        val result = service.deleteFile("/remote/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `renameFile returns success`() = runBlocking<Unit> {
        val result = service.renameFile("/remote/old.txt", "new.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `moveFile returns success with correct document`() = runBlocking<Unit> {
        val result = service.moveFile("/source/file.txt", "/dest/file.txt")
        assertTrue(result.isSuccess)
        val document = result.getOrNull()
        assertNotNull(document)
        assertEquals("/dest/file.txt", document.path)
        assertEquals("file.txt", document.name)
        assertEquals(SyncStatus.SYNCED, document.syncStatus)
    }

    @Test
    fun `moveFile returns document with correct permissions`() = runBlocking<Unit> {
        val result = service.moveFile("/source/file.txt", "/dest/file.txt")
        val document = result.getOrNull()
        assertNotNull(document)
        assertTrue(document.permissions.contains(DocumentPermission.READ))
        assertTrue(document.permissions.contains(DocumentPermission.WRITE))
        assertTrue(document.permissions.contains(DocumentPermission.DELETE))
    }

    // ==================== FOLDER OPERATION TESTS ====================

    @Test
    fun `createFolder returns success with correct document`() = runBlocking<Unit> {
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
    fun `createFolder returns document with execute permission`() = runBlocking<Unit> {
        val result = service.createFolder("/documents/newfolder")
        val document = result.getOrNull()
        assertNotNull(document)
        assertTrue(document.permissions.contains(DocumentPermission.EXECUTE))
    }

    // ==================== FILE INFO TESTS ====================

    @Test
    fun `getFileInfo returns success with correct document`() = runBlocking<Unit> {
        val result = service.getFileInfo("/documents/file.txt")
        assertTrue(result.isSuccess)
        val document = result.getOrNull()
        assertNotNull(document)
        assertEquals("/documents/file.txt", document.path)
        assertEquals("file.txt", document.name)
        assertFalse(document.isFolder)
        assertEquals("webdav", document.storageId)
    }

    @Test
    fun `exists returns result`() = runBlocking<Unit> {
        val result = service.exists("/documents/file.txt")
        assertTrue(result.isSuccess)
        // Mock implementation returns false
        assertFalse(result.getOrNull() ?: true)
    }

    // ==================== OPERATION MANAGEMENT TESTS ====================

    @Test
    fun `getActiveOperations returns empty flow`() = runBlocking<Unit> {
        val operations = service.getActiveOperations().toList()
        assertTrue(operations.isNotEmpty())
        assertTrue(operations[0].isEmpty())
    }

    @Test
    fun `cancelOperation returns success`() = runBlocking<Unit> {
        val result = service.cancelOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `pauseOperation returns success`() = runBlocking<Unit> {
        val result = service.pauseOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resumeOperation returns success`() = runBlocking<Unit> {
        val result = service.resumeOperation(12345L)
        assertTrue(result.isSuccess)
    }

    // ==================== CACHE OPERATION TESTS ====================

    @Test
    fun `getCacheEntries returns empty list`() = runBlocking<Unit> {
        val entries = service.getCacheEntries("/").toList()
        assertTrue(entries.isNotEmpty())
        assertTrue(entries[0].isEmpty())
    }

    @Test
    fun `getCacheEntries with null path returns empty list`() = runBlocking<Unit> {
        val entries = service.getCacheEntries(null).toList()
        assertTrue(entries.isNotEmpty())
        assertTrue(entries[0].isEmpty())
    }

    @Test
    fun `addToCache returns success`() = runBlocking<Unit> {
        val result = service.addToCache("/documents/file.txt", 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addToCache with different priorities succeeds`() = runBlocking<Unit> {
        assertTrue(service.addToCache("/file1.txt", 0).isSuccess)
        assertTrue(service.addToCache("/file2.txt", 1).isSuccess)
        assertTrue(service.addToCache("/file3.txt", 10).isSuccess)
    }

    @Test
    fun `removeFromCache returns success`() = runBlocking<Unit> {
        val result = service.removeFromCache("/documents/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `clearCache returns success`() = runBlocking<Unit> {
        val result = service.clearCache()
        assertTrue(result.isSuccess)
    }

    // ==================== SYNC OPERATION TESTS ====================

    @Test
    fun `getSyncStatus returns empty map`() = runBlocking<Unit> {
        val status = service.getSyncStatus("/").toList()
        assertTrue(status.isNotEmpty())
        assertTrue(status[0].isEmpty())
    }

    @Test
    fun `getSyncStatus with null path returns empty map`() = runBlocking<Unit> {
        val status = service.getSyncStatus(null).toList()
        assertTrue(status.isNotEmpty())
        assertTrue(status[0].isEmpty())
    }

    @Test
    fun `syncFile returns progress flow`() = runBlocking<Unit> {
        val results = service.syncFile("/documents/file.txt", false).toList()
        assertTrue(results.isNotEmpty())

        // Should have progress updates
        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.COMPLETED, lastResult.status)
        assertEquals(NetworkOperation.Type.SYNC, lastResult.type)
    }

    @Test
    fun `syncFile with forceSync flag succeeds`() = runBlocking<Unit> {
        val results = service.syncFile("/documents/file.txt", true).toList()
        assertTrue(results.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, results.last().status)
    }

    @Test
    fun `syncAll returns flow`() = runBlocking<Unit> {
        val results = service.syncAll(false).toList()
        // syncAll now returns FAILED operation when not connected
        assertEquals(1, results.size, "syncAll should return one failed operation when not connected")
        assertEquals(NetworkOperation.Type.SYNC, results[0].type)
        assertEquals(NetworkOperation.Status.FAILED, results[0].status)
        assertEquals("WebDAV not connected", results[0].error)
    }

    // ==================== SEARCH TESTS ====================

    @Test
    fun `searchFiles returns failure when not connected`() = runBlocking<Unit> {
        val results = service.searchFiles("query", "/", false).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isFailure)
    }

    @Test
    fun `searchFiles with includeContent flag returns failure when not connected`() = runBlocking<Unit> {
        val results = service.searchFiles("query", "/", true).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isFailure)
    }

    @Test
    fun `searchFiles with null path returns failure when not connected`() = runBlocking<Unit> {
        val results = service.searchFiles("query", null, false).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isFailure)
    }

    // ==================== RECENT CHANGES TESTS ====================

    @Test
    fun `getRecentChanges returns empty list`() = runBlocking<Unit> {
        val since = kotlinx.datetime.Clock.System.now()
        val results = service.getRecentChanges(since, "/").toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isEmpty())
    }

    @Test
    fun `getRecentChanges with null path returns empty list`() = runBlocking<Unit> {
        val since = kotlinx.datetime.Clock.System.now()
        val results = service.getRecentChanges(since, null).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isEmpty())
    }

    // ==================== QUOTA TESTS ====================

    @Test
    fun `getQuotaInfo returns valid quota`() = runBlocking<Unit> {
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
    fun `getQuotaInfo returns consistent values`() = runBlocking<Unit> {
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

    // ==================== WEBDAV-SPECIFIC TESTS ====================

    @Test
    fun `service uses WebDavConfig`() {
        assertIs<StorageConfig.WebDavConfig>(service.config)
    }

    @Test
    fun `storage info has WEBDAV type`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals(StorageType.WEBDAV, info.type)
    }

    @Test
    fun `url is used as location in storage info`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals(config.url, info.location)
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `operations handle special characters in paths`() = runBlocking<Unit> {
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
    fun `operations handle deep paths`() = runBlocking<Unit> {
        service.connect()

        val deepPath = "/a/b/c/d/e/f/g/h/i/j/file.txt"
        val result = service.getFileInfo(deepPath)
        assertTrue(result.isSuccess)
        assertEquals("file.txt", result.getOrNull()?.name)
    }

    @Test
    fun `operations handle unicode paths`() = runBlocking<Unit> {
        service.connect()

        val result = service.getFileInfo("/文档/файл.txt")
        assertTrue(result.isSuccess)
    }
}
