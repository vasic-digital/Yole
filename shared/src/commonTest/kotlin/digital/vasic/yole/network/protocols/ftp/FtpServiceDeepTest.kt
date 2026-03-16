/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * FTP Service Deep Test Suite
 *
 * Comprehensive tests for FTP protocol implementation
 * covering initialization, connection management, file
 * operations, cache lifecycle, sync status, operation
 * tracking, path utilities, and protocol-specific limits.
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.ftp

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Deep test suite for FtpService.
 *
 * FTP uses [FtpProtocolClient] which is an `expect` class backed by real TCP
 * sockets on JVM and by UnsupportedOperationException stubs on iOS/Wasm.
 * Because there is no actual FTP server available in the test environment,
 * connect() will fail. This test suite therefore focuses on:
 *
 * - Initialization and configuration access
 * - Connection state management (connect failure handling, disconnect)
 * - Methods that work without an active connection (path utilities, quota, cache, sync)
 * - Methods that correctly fail when not connected (upload, download, delete, etc.)
 * - Protocol-specific limitations (no copy, no search, no quota)
 * - Cache operations lifecycle (add, get, filter, remove, clear)
 * - Sync status operations lifecycle
 * - Operation management (active ops, cancel, pause, resume)
 */
class FtpServiceDeepTest {

    private lateinit var service: FtpService
    private lateinit var config: StorageConfig.FtpConfig

    @BeforeTest
    fun setup() {
        config = StorageConfig.FtpConfig(
            name = "test-ftp",
            host = "ftp.example.com",
            port = 21,
            username = "testuser",
            password = "testpass",
            passiveMode = true,
            secureFtp = false,
            connectionTimeout = 30000
        )
        service = FtpService(config)
    }

    // ==================== INITIALIZATION TESTS ====================

    @Test
    fun `service initializes with correct name`() {
        assertEquals("test-ftp", service.config.name)
    }

    @Test
    fun `service initializes with correct host`() {
        assertEquals("ftp.example.com", (service.config as StorageConfig.FtpConfig).host)
    }

    @Test
    fun `service initializes with correct port`() {
        assertEquals(21, (service.config as StorageConfig.FtpConfig).port)
    }

    @Test
    fun `service initializes with correct username`() {
        assertEquals("testuser", (service.config as StorageConfig.FtpConfig).username)
    }

    @Test
    fun `service initializes with correct password`() {
        assertEquals("testpass", (service.config as StorageConfig.FtpConfig).password)
    }

    @Test
    fun `service initializes with passive mode enabled`() {
        assertTrue((service.config as StorageConfig.FtpConfig).passiveMode)
    }

    @Test
    fun `service initializes with secure ftp disabled`() {
        assertFalse((service.config as StorageConfig.FtpConfig).secureFtp)
    }

    @Test
    fun `service initializes with correct connection timeout`() {
        assertEquals(30000, (service.config as StorageConfig.FtpConfig).connectionTimeout)
    }

    @Test
    fun `service config is FtpConfig type`() {
        assertIs<StorageConfig.FtpConfig>(service.config)
    }

    @Test
    fun `service starts offline`() {
        assertFalse(service.isOnline)
    }

    @Test
    fun `service root path is forward slash`() {
        assertEquals("/", service.rootPath)
    }

    // ==================== CONNECTION STATE MANAGEMENT ====================

    @Test
    fun `connect fails when server is unreachable`() = runBlocking<Unit> {
        val result = service.connect()
        assertTrue(result.isFailure, "FTP connection should fail when no server is available")
    }

    @Test
    fun `connect with blank host fails immediately`() = runBlocking<Unit> {
        val blankHostConfig = config.copy(host = "")
        val blankHostService = FtpService(blankHostConfig)
        val result = blankHostService.connect()
        assertTrue(result.isFailure)
        assertFalse(blankHostService.isOnline)
    }

    @Test
    fun `connect with invalid port fails`() = runBlocking<Unit> {
        val invalidPortConfig = config.copy(port = 99999)
        val invalidPortService = FtpService(invalidPortConfig)
        val result = invalidPortService.connect()
        assertTrue(result.isFailure)
        assertFalse(invalidPortService.isOnline)
    }

    @Test
    fun `connect with port zero fails`() = runBlocking<Unit> {
        val zeroPortConfig = config.copy(port = 0)
        val zeroPortService = FtpService(zeroPortConfig)
        val result = zeroPortService.connect()
        assertTrue(result.isFailure)
        assertFalse(zeroPortService.isOnline)
    }

    @Test
    fun `connect with negative port fails`() = runBlocking<Unit> {
        val negativePortConfig = config.copy(port = -1)
        val negativePortService = FtpService(negativePortConfig)
        val result = negativePortService.connect()
        assertTrue(result.isFailure)
        assertFalse(negativePortService.isOnline)
    }

    @Test
    fun `isOnline remains false after failed connect`() = runBlocking<Unit> {
        service.connect()
        assertFalse(service.isOnline)
    }

    @Test
    fun `disconnect succeeds even without prior connection`() = runBlocking<Unit> {
        val result = service.disconnect()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `disconnect ensures service goes offline`() = runBlocking<Unit> {
        service.connect() // will fail, that's fine
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `multiple disconnect calls succeed`() = runBlocking<Unit> {
        assertTrue(service.disconnect().isSuccess)
        assertTrue(service.disconnect().isSuccess)
        assertTrue(service.disconnect().isSuccess)
        assertFalse(service.isOnline)
    }

    // ==================== TEST CONNECTION ====================

    @Test
    fun `testConnection fails when server is unreachable`() = runBlocking<Unit> {
        val result = service.testConnection()
        assertTrue(result.isFailure, "testConnection should fail without a real server")
    }

    @Test
    fun `testConnection with blank host fails`() = runBlocking<Unit> {
        val blankService = FtpService(config.copy(host = ""))
        val result = blankService.testConnection()
        assertTrue(result.isFailure)
    }

    @Test
    fun `testConnection with invalid port fails`() = runBlocking<Unit> {
        val invalidService = FtpService(config.copy(port = 100000))
        val result = invalidService.testConnection()
        assertTrue(result.isFailure)
    }

    // ==================== STORAGE INFO ====================

    @Test
    fun `getStorageInfo returns correct id`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("ftp_test-ftp", info.id)
    }

    @Test
    fun `getStorageInfo returns correct name`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("test-ftp", info.name)
    }

    @Test
    fun `getStorageInfo returns FTP type`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals(StorageType.FTP, info.type)
    }

    @Test
    fun `getStorageInfo returns correct location with host and port`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("ftp://ftp.example.com:21/", info.location)
    }

    @Test
    fun `getStorageInfo shows offline when not connected`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertFalse(info.isOnline)
    }

    @Test
    fun `getStorageInfo reports supportsFolders as false`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertFalse(info.supportsFolders)
    }

    @Test
    fun `getStorageInfo reports supportsMetadata as false`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertFalse(info.supportsMetadata)
    }

    @Test
    fun `getStorageInfo with custom port reflects in location`() = runBlocking<Unit> {
        val customPortService = FtpService(config.copy(port = 2121))
        val info = customPortService.getStorageInfo()
        assertTrue(info.location.contains("2121"))
    }

    @Test
    fun `getStorageInfo with root path reflects in location`() = runBlocking<Unit> {
        val rootPathService = FtpService(config.copy(rootPath = "/public_html"))
        val info = rootPathService.getStorageInfo()
        assertTrue(info.location.contains("/public_html"))
    }

    // ==================== UPLOAD FILE (disconnected) ====================

    @Test
    fun `uploadFile when disconnected emits FAILED status`() = runBlocking<Unit> {
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(results.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, results.last().status)
    }

    @Test
    fun `uploadFile when disconnected has correct error message`() = runBlocking<Unit> {
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertEquals("FTP not connected", results.last().error)
    }

    @Test
    fun `uploadFile when disconnected has UPLOAD type`() = runBlocking<Unit> {
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(results.all { it.type == NetworkOperation.Type.UPLOAD })
    }

    @Test
    fun `uploadFile when disconnected preserves remote path`() = runBlocking<Unit> {
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertEquals("/remote/file.txt", results.last().remotePath)
    }

    @Test
    fun `uploadFile when disconnected preserves local path`() = runBlocking<Unit> {
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertEquals("/local/file.txt", results.last().localPath)
    }

    // ==================== DOWNLOAD FILE (disconnected) ====================

    @Test
    fun `downloadFile when disconnected emits FAILED status`() = runBlocking<Unit> {
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(results.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, results.last().status)
    }

    @Test
    fun `downloadFile when disconnected has correct error message`() = runBlocking<Unit> {
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertEquals("FTP not connected", results.last().error)
    }

    @Test
    fun `downloadFile when disconnected has DOWNLOAD type`() = runBlocking<Unit> {
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(results.all { it.type == NetworkOperation.Type.DOWNLOAD })
    }

    @Test
    fun `downloadFile when disconnected preserves remote path`() = runBlocking<Unit> {
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertEquals("/remote/file.txt", results.last().remotePath)
    }

    @Test
    fun `downloadFile when disconnected preserves local path`() = runBlocking<Unit> {
        val results = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertEquals("/local/file.txt", results.last().localPath)
    }

    // ==================== COPY FILE (protocol limitation) ====================

    @Test
    fun `copyFile always fails because FTP has no COPY command`() = runBlocking<Unit> {
        val result = service.copyFile("/source.txt", "/dest.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `copyFile failure exception indicates FTP limitation`() = runBlocking<Unit> {
        val result = service.copyFile("/source.txt", "/dest.txt")
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("Copy failed") == true ||
                   exception.message?.contains("copy") == true,
            "Error should indicate copy not supported")
    }

    @Test
    fun `copyFile fails regardless of connection state`() = runBlocking<Unit> {
        // Even after a connect attempt, copy should fail
        service.connect()
        val result = service.copyFile("/a.txt", "/b.txt")
        assertTrue(result.isFailure)
    }

    // ==================== DELETE FILE (disconnected) ====================

    @Test
    fun `deleteFile when disconnected fails with NotConnected`() = runBlocking<Unit> {
        val result = service.deleteFile("/remote/file.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteFile when disconnected exception is NotConnected`() = runBlocking<Unit> {
        val result = service.deleteFile("/remote/file.txt")
        val exception = result.exceptionOrNull()
        assertIs<NetworkStorageException.ConnectionException.NotConnected>(exception)
    }

    // ==================== CREATE FOLDER (disconnected) ====================

    @Test
    fun `createFolder when disconnected fails`() = runBlocking<Unit> {
        val result = service.createFolder("/new-folder")
        assertTrue(result.isFailure)
    }

    @Test
    fun `createFolder when disconnected exception is NotConnected`() = runBlocking<Unit> {
        val result = service.createFolder("/new-folder")
        val exception = result.exceptionOrNull()
        assertIs<NetworkStorageException.ConnectionException.NotConnected>(exception)
    }

    // ==================== RENAME FILE (disconnected) ====================

    @Test
    fun `renameFile when disconnected fails`() = runBlocking<Unit> {
        val result = service.renameFile("/old.txt", "new.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `renameFile when disconnected exception is NotConnected`() = runBlocking<Unit> {
        val result = service.renameFile("/old.txt", "new.txt")
        val exception = result.exceptionOrNull()
        assertIs<NetworkStorageException.ConnectionException.NotConnected>(exception)
    }

    // ==================== MOVE FILE (disconnected) ====================

    @Test
    fun `moveFile when disconnected fails`() = runBlocking<Unit> {
        val result = service.moveFile("/source.txt", "/dest.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `moveFile when disconnected exception is NotConnected`() = runBlocking<Unit> {
        val result = service.moveFile("/source.txt", "/dest.txt")
        val exception = result.exceptionOrNull()
        assertIs<NetworkStorageException.ConnectionException.NotConnected>(exception)
    }

    // ==================== GET FILE INFO (disconnected) ====================

    @Test
    fun `getFileInfo when disconnected fails`() = runBlocking<Unit> {
        val result = service.getFileInfo("/document.pdf")
        assertTrue(result.isFailure)
    }

    @Test
    fun `getFileInfo when disconnected exception is NotConnected`() = runBlocking<Unit> {
        val result = service.getFileInfo("/document.pdf")
        val exception = result.exceptionOrNull()
        assertIs<NetworkStorageException.ConnectionException.NotConnected>(exception)
    }

    // ==================== EXISTS (disconnected) ====================

    @Test
    fun `exists when disconnected returns false`() = runBlocking<Unit> {
        val result = service.exists("/file.txt")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun `exists with deep path when disconnected returns false`() = runBlocking<Unit> {
        val result = service.exists("/a/b/c/d/file.txt")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    // ==================== GET PARENT PATH ====================

    @Test
    fun `getParentPath of file returns parent directory`() {
        assertEquals("/documents", service.getParentPath("/documents/file.txt"))
    }

    @Test
    fun `getParentPath of top-level file returns root`() {
        assertEquals("/", service.getParentPath("/file.txt"))
    }

    @Test
    fun `getParentPath of nested path returns intermediate`() {
        assertEquals("/a/b", service.getParentPath("/a/b/c"))
    }

    @Test
    fun `getParentPath of deeply nested path`() {
        assertEquals("/a/b/c/d", service.getParentPath("/a/b/c/d/file.txt"))
    }

    @Test
    fun `getParentPath of root returns null`() {
        assertNull(service.getParentPath("/"))
    }

    @Test
    fun `getParentPath of empty string returns null`() {
        assertNull(service.getParentPath(""))
    }

    @Test
    fun `getParentPath with trailing slash`() {
        assertEquals("/documents", service.getParentPath("/documents/folder/"))
    }

    // ==================== VALIDATE PATH ====================

    @Test
    fun `validatePath succeeds for root path`() {
        assertTrue(service.validatePath("/").isSuccess)
    }

    @Test
    fun `validatePath succeeds for absolute path`() {
        assertTrue(service.validatePath("/documents/file.txt").isSuccess)
    }

    @Test
    fun `validatePath succeeds for single file`() {
        assertTrue(service.validatePath("/file.txt").isSuccess)
    }

    @Test
    fun `validatePath fails for empty string`() {
        assertTrue(service.validatePath("").isFailure)
    }

    @Test
    fun `validatePath fails for whitespace only`() {
        assertTrue(service.validatePath("   ").isFailure)
    }

    @Test
    fun `validatePath fails for tab characters`() {
        assertTrue(service.validatePath("\t").isFailure)
    }

    // ==================== CACHE OPERATIONS LIFECYCLE ====================

    @Test
    fun `getCacheEntries initially returns empty list`() = runBlocking<Unit> {
        val entries = service.getCacheEntries("/").first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `getCacheEntries with null path initially returns empty list`() = runBlocking<Unit> {
        val entries = service.getCacheEntries(null).first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `addToCache succeeds`() = runBlocking<Unit> {
        val result = service.addToCache("/file.txt", 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addToCache then getCacheEntries returns the entry`() = runBlocking<Unit> {
        service.addToCache("/file.txt", 1)
        val entries = service.getCacheEntries("/").first()
        assertEquals(1, entries.size)
        assertEquals("/file.txt", entries[0].remotePath)
    }

    @Test
    fun `addToCache multiple entries all returned`() = runBlocking<Unit> {
        service.addToCache("/file1.txt", 1)
        service.addToCache("/file2.txt", 2)
        service.addToCache("/file3.txt", 3)
        val entries = service.getCacheEntries(null).first()
        assertEquals(3, entries.size)
    }

    @Test
    fun `getCacheEntries filters by path prefix`() = runBlocking<Unit> {
        service.addToCache("/docs/file1.txt", 1)
        service.addToCache("/photos/file2.jpg", 1)
        val docEntries = service.getCacheEntries("/docs").first()
        assertEquals(1, docEntries.size)
        assertTrue(docEntries[0].remotePath.startsWith("/docs"))
    }

    @Test
    fun `removeFromCache removes specific entry`() = runBlocking<Unit> {
        service.addToCache("/file.txt", 1)
        service.removeFromCache("/file.txt")
        val entries = service.getCacheEntries(null).first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `removeFromCache returns success even for nonexistent entry`() = runBlocking<Unit> {
        val result = service.removeFromCache("/nonexistent.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `clearCache removes all entries`() = runBlocking<Unit> {
        service.addToCache("/file1.txt", 1)
        service.addToCache("/file2.txt", 2)
        service.addToCache("/file3.txt", 3)
        service.clearCache()
        val entries = service.getCacheEntries(null).first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `clearCache returns success when already empty`() = runBlocking<Unit> {
        val result = service.clearCache()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addToCache with different priorities succeeds`() = runBlocking<Unit> {
        assertTrue(service.addToCache("/a.txt", 0).isSuccess)
        assertTrue(service.addToCache("/b.txt", 5).isSuccess)
        assertTrue(service.addToCache("/c.txt", 100).isSuccess)
    }

    @Test
    fun `addToCache overwrites existing entry with same path`() = runBlocking<Unit> {
        service.addToCache("/file.txt", 1)
        service.addToCache("/file.txt", 5)
        val entries = service.getCacheEntries(null).first()
        assertEquals(1, entries.size)
    }

    // ==================== SYNC STATUS OPERATIONS ====================

    @Test
    fun `getSyncStatus initially returns empty map`() = runBlocking<Unit> {
        val status = service.getSyncStatus("/").first()
        assertTrue(status.isEmpty())
    }

    @Test
    fun `getSyncStatus with null path initially returns empty map`() = runBlocking<Unit> {
        val status = service.getSyncStatus(null).first()
        assertTrue(status.isEmpty())
    }

    @Test
    fun `syncFile returns COMPLETED operation`() = runBlocking<Unit> {
        val results = service.syncFile("/test.txt", false).toList()
        assertTrue(results.isNotEmpty())
        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.COMPLETED, lastResult.status)
        assertEquals(NetworkOperation.Type.SYNC, lastResult.type)
    }

    @Test
    fun `syncFile updates sync status to SYNCED`() = runBlocking<Unit> {
        service.syncFile("/test.txt", false).toList()
        val status = service.getSyncStatus("/test").first()
        assertTrue(status.containsKey("/test.txt"))
        assertEquals(SyncStatus.SYNCED, status["/test.txt"])
    }

    @Test
    fun `syncFile with forceSync returns COMPLETED`() = runBlocking<Unit> {
        val results = service.syncFile("/test.txt", true).toList()
        assertEquals(NetworkOperation.Status.COMPLETED, results.last().status)
    }

    @Test
    fun `syncFile preserves remote path in operation`() = runBlocking<Unit> {
        val results = service.syncFile("/documents/file.md", false).toList()
        assertEquals("/documents/file.md", results.last().remotePath)
    }

    @Test
    fun `syncFile ends with progress 1 point 0`() = runBlocking<Unit> {
        val results = service.syncFile("/file.txt", false).toList()
        assertEquals(1.0, results.last().progress)
    }

    @Test
    fun `syncAll when disconnected returns COMPLETED operation`() = runBlocking<Unit> {
        val results = service.syncAll(false).toList()
        assertTrue(results.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, results.last().status)
    }

    @Test
    fun `syncAll returns SYNC type operation`() = runBlocking<Unit> {
        val results = service.syncAll(false).toList()
        assertEquals(NetworkOperation.Type.SYNC, results.last().type)
    }

    @Test
    fun `syncAll remote path is root`() = runBlocking<Unit> {
        val results = service.syncAll(false).toList()
        assertEquals("/", results.last().remotePath)
    }

    // ==================== ACTIVE OPERATIONS MANAGEMENT ====================

    @Test
    fun `getActiveOperations initially returns empty list`() = runBlocking<Unit> {
        val operations = service.getActiveOperations().first()
        assertTrue(operations.isEmpty())
    }

    @Test
    fun `cancelOperation returns success`() = runBlocking<Unit> {
        val result = service.cancelOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `cancelOperation with zero id returns success`() = runBlocking<Unit> {
        val result = service.cancelOperation(0L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `cancelOperation with negative id returns success`() = runBlocking<Unit> {
        val result = service.cancelOperation(-1L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `pauseOperation returns success`() = runBlocking<Unit> {
        val result = service.pauseOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `pauseOperation with nonexistent id returns success`() = runBlocking<Unit> {
        val result = service.pauseOperation(999999L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resumeOperation returns success`() = runBlocking<Unit> {
        val result = service.resumeOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resumeOperation with nonexistent id returns success`() = runBlocking<Unit> {
        val result = service.resumeOperation(999999L)
        assertTrue(result.isSuccess)
    }

    // ==================== SEARCH FILES ====================

    @Test
    fun `searchFiles returns failure because FTP has no search`() = runBlocking<Unit> {
        val results = service.searchFiles("query", "/", false).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isFailure)
    }

    @Test
    fun `searchFiles error message indicates FTP limitation`() = runBlocking<Unit> {
        val results = service.searchFiles("query", "/", false).toList()
        assertEquals("FTP does not support search operations", results[0].exceptionOrNull()?.message)
    }

    @Test
    fun `searchFiles with includeContent returns failure`() = runBlocking<Unit> {
        val results = service.searchFiles("query", "/", true).toList()
        assertTrue(results[0].isFailure)
    }

    @Test
    fun `searchFiles with null path returns failure`() = runBlocking<Unit> {
        val results = service.searchFiles("query", null, false).toList()
        assertTrue(results[0].isFailure)
    }

    // ==================== RECENT CHANGES ====================

    @Test
    fun `getRecentChanges when disconnected returns empty list`() = runBlocking<Unit> {
        val since = kotlinx.datetime.Clock.System.now()
        val results = service.getRecentChanges(since, "/").toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isEmpty())
    }

    @Test
    fun `getRecentChanges with null path when disconnected returns empty list`() = runBlocking<Unit> {
        val since = kotlinx.datetime.Clock.System.now()
        val results = service.getRecentChanges(since, null).toList()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].isEmpty())
    }

    // ==================== QUOTA INFO ====================

    @Test
    fun `getQuotaInfo returns success`() = runBlocking<Unit> {
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getQuotaInfo returns zero total space`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(0L, quota.totalSpace)
    }

    @Test
    fun `getQuotaInfo returns zero used space`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(0L, quota.usedSpace)
    }

    @Test
    fun `getQuotaInfo returns zero available space`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(0L, quota.availableSpace)
    }

    @Test
    fun `getQuotaInfo is not full`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertFalse(quota.isFull)
    }

    @Test
    fun `getQuotaInfo is not low on space`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertFalse(quota.isLowOnSpace)
    }

    @Test
    fun `getQuotaInfo metadata indicates FTP provider`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals("FTP", quota.metadata["provider"])
    }

    @Test
    fun `getQuotaInfo metadata indicates quota not supported`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals("Quota not supported", quota.metadata["note"])
    }

    @Test
    fun `getQuotaInfo usage percentage is zero`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(0.0, quota.usagePercentage)
    }

    // ==================== LIST FILES (disconnected) ====================

    @Test
    fun `listFiles when disconnected fails with NotConnected`() = runBlocking<Unit> {
        val result = service.listFiles("/").first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `listFiles when disconnected exception is NotConnected`() = runBlocking<Unit> {
        val result = service.listFiles("/").first()
        val exception = result.exceptionOrNull()
        assertIs<NetworkStorageException.ConnectionException.NotConnected>(exception)
    }

    @Test
    fun `listFiles when disconnected error message`() = runBlocking<Unit> {
        val result = service.listFiles("/").first()
        assertEquals("FTP not connected", result.exceptionOrNull()?.message)
    }

    // ==================== CONFIGURATION VARIATIONS ====================

    @Test
    fun `service with custom root path includes it in storage info`() = runBlocking<Unit> {
        val customRootService = FtpService(config.copy(rootPath = "/web/data"))
        val info = customRootService.getStorageInfo()
        assertTrue(info.location.contains("/web/data"))
    }

    @Test
    fun `service with secure FTP configuration`() {
        val secureConfig = config.copy(secureFtp = true, port = 990)
        val secureService = FtpService(secureConfig)
        assertTrue((secureService.config as StorageConfig.FtpConfig).secureFtp)
        assertEquals(990, (secureService.config as StorageConfig.FtpConfig).port)
    }

    @Test
    fun `service with active mode configuration`() {
        val activeConfig = config.copy(passiveMode = false)
        val activeService = FtpService(activeConfig)
        assertFalse((activeService.config as StorageConfig.FtpConfig).passiveMode)
    }

    @Test
    fun `service with custom encoding`() {
        val encodingConfig = config.copy(encoding = "ISO-8859-1")
        val encodingService = FtpService(encodingConfig)
        assertEquals("ISO-8859-1", (encodingService.config as StorageConfig.FtpConfig).encoding)
    }

    @Test
    fun `service with custom timeout`() {
        val timeoutConfig = config.copy(connectionTimeout = 60000)
        val timeoutService = FtpService(timeoutConfig)
        assertEquals(60000, (timeoutService.config as StorageConfig.FtpConfig).connectionTimeout)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `special characters in path for validatePath`() {
        assertTrue(service.validatePath("/path with spaces/file.txt").isSuccess)
        assertTrue(service.validatePath("/path-with-dashes/file.txt").isSuccess)
        assertTrue(service.validatePath("/path_with_underscores/file.txt").isSuccess)
        assertTrue(service.validatePath("/path.with.dots/file.txt").isSuccess)
    }

    @Test
    fun `unicode characters in path for validatePath`() {
        assertTrue(service.validatePath("/documents/resume.txt").isSuccess)
    }

    @Test
    fun `deep path for getParentPath`() {
        assertEquals("/a/b/c/d/e/f/g/h/i", service.getParentPath("/a/b/c/d/e/f/g/h/i/j"))
    }

    @Test
    fun `reconnect after disconnect handles state correctly`() = runBlocking<Unit> {
        service.connect() // will fail
        assertFalse(service.isOnline)
        service.disconnect()
        assertFalse(service.isOnline)
        service.connect() // will fail again
        assertFalse(service.isOnline)
    }

    @Test
    fun `multiple sequential operations do not interfere`() = runBlocking<Unit> {
        assertTrue(service.addToCache("/a.txt", 1).isSuccess)
        assertTrue(service.addToCache("/b.txt", 2).isSuccess)
        assertTrue(service.removeFromCache("/a.txt").isSuccess)
        val entries = service.getCacheEntries(null).first()
        assertEquals(1, entries.size)
        assertEquals("/b.txt", entries[0].remotePath)
    }

    @Test
    fun `cache add then sync then cache get consistency`() = runBlocking<Unit> {
        service.addToCache("/file.txt", 1)
        service.syncFile("/file.txt", false).toList()
        val entries = service.getCacheEntries("/file").first()
        assertTrue(entries.isNotEmpty())
    }

    @Test
    fun `sync status updates after sync file`() = runBlocking<Unit> {
        service.syncFile("/docs/readme.md", false).toList()
        val statuses = service.getSyncStatus("/docs").first()
        assertTrue(statuses.containsKey("/docs/readme.md"))
        assertEquals(SyncStatus.SYNCED, statuses["/docs/readme.md"])
    }

    @Test
    fun `getSyncStatus filters by path prefix correctly`() = runBlocking<Unit> {
        service.syncFile("/docs/a.txt", false).toList()
        service.syncFile("/photos/b.jpg", false).toList()
        val docStatuses = service.getSyncStatus("/docs").first()
        assertEquals(1, docStatuses.size)
        assertTrue(docStatuses.containsKey("/docs/a.txt"))
    }
}
