/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * WebDAV Service Deep Test Suite
 *
 * Comprehensive tests for WebDAV protocol implementation
 * covering path utilities, storage info, connection state,
 * cache lifecycle, sync status, operation management, and
 * offline file operation behaviors.
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.webdav

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Deep test suite for WebDavService.
 *
 * WebDAV uses ktor HttpClient for real HTTP operations. The connect() method
 * silently marks the service as connected even on network errors (for offline-
 * capable usage), so connect/disconnect cycle works in tests. Most file
 * operations either succeed with fallback/default documents or fail gracefully
 * when the actual HTTP request cannot reach a server.
 *
 * This test suite focuses on all non-HTTP methods and state management:
 * - Initialization and configuration
 * - Path utilities (getParentPath, validatePath)
 * - Storage info metadata
 * - Connection state toggling
 * - Cache operations lifecycle (add, filter, remove, clear)
 * - Sync status tracking
 * - Operation management (active ops, cancel, pause, resume)
 * - Offline behavior for exists and getFileInfo
 */
class WebDavServiceDeepTest {

    private lateinit var service: WebDavService
    private lateinit var config: StorageConfig.WebDavConfig

    @BeforeTest
    fun setup() {
        config = StorageConfig.WebDavConfig(
            name = "test-webdav",
            url = "https://webdav.example.com/remote.php/dav/files/user/",
            username = "testuser",
            password = "testpass",
            authenticationType = WebDavAuthenticationType.BASIC
        )
        service = WebDavService(config)
    }

    // ==================== INITIALIZATION TESTS ====================

    @Test
    fun `service initializes with correct name`() {
        assertEquals("test-webdav", service.config.name)
    }

    @Test
    fun `service initializes with correct url`() {
        assertEquals("https://webdav.example.com/remote.php/dav/files/user/",
            (service.config as StorageConfig.WebDavConfig).url)
    }

    @Test
    fun `service initializes with correct username`() {
        assertEquals("testuser", (service.config as StorageConfig.WebDavConfig).username)
    }

    @Test
    fun `service initializes with correct password`() {
        assertEquals("testpass", (service.config as StorageConfig.WebDavConfig).password)
    }

    @Test
    fun `service config is WebDavConfig type`() {
        assertIs<StorageConfig.WebDavConfig>(service.config)
    }

    @Test
    fun `service starts offline`() {
        assertFalse(service.isOnline)
    }

    @Test
    fun `service root path is forward slash`() {
        assertEquals("/", service.rootPath)
    }

    @Test
    fun `service with basic auth type`() {
        assertEquals(WebDavAuthenticationType.BASIC,
            (service.config as StorageConfig.WebDavConfig).authenticationType)
    }

    // ==================== PATH UTILITIES ====================

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
    fun `getParentPath of blank string returns null`() {
        assertNull(service.getParentPath("   "))
    }

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

    // ==================== STORAGE INFO ====================

    @Test
    fun `getStorageInfo returns correct id`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("webdav_test-webdav", info.id)
    }

    @Test
    fun `getStorageInfo returns correct name`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("test-webdav", info.name)
    }

    @Test
    fun `getStorageInfo returns WEBDAV type`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals(StorageType.WEBDAV, info.type)
    }

    @Test
    fun `getStorageInfo uses config url as location`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals(config.url, info.location)
    }

    @Test
    fun `getStorageInfo shows offline when not connected`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertFalse(info.isOnline)
    }

    @Test
    fun `getStorageInfo shows online after connect`() = runBlocking<Unit> {
        service.connect()
        val info = service.getStorageInfo()
        assertTrue(info.isOnline)
    }

    @Test
    fun `getStorageInfo shows offline after disconnect`() = runBlocking<Unit> {
        service.connect()
        service.disconnect()
        val info = service.getStorageInfo()
        assertFalse(info.isOnline)
    }

    // ==================== CONNECTION STATE ====================

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
    fun `disconnect without prior connect succeeds`() = runBlocking<Unit> {
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `isOnline false initially`() {
        assertFalse(service.isOnline)
    }

    @Test
    fun `testConnection succeeds`() = runBlocking<Unit> {
        val result = service.testConnection()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `testConnection when already connected succeeds`() = runBlocking<Unit> {
        service.connect()
        val result = service.testConnection()
        assertTrue(result.isSuccess)
    }

    // ==================== CACHE OPERATIONS LIFECYCLE ====================

    @Test
    fun `getCacheEntries initially returns empty list`() = runBlocking<Unit> {
        val entries = service.getCacheEntries("/").first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `getCacheEntries with null path initially empty`() = runBlocking<Unit> {
        val entries = service.getCacheEntries(null).first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `addToCache succeeds`() = runBlocking<Unit> {
        val result = service.addToCache("/file.txt", 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addToCache then getCacheEntries returns entry`() = runBlocking<Unit> {
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
    }

    @Test
    fun `removeFromCache removes specific entry`() = runBlocking<Unit> {
        service.addToCache("/file.txt", 1)
        service.removeFromCache("/file.txt")
        val entries = service.getCacheEntries(null).first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `removeFromCache returns success for nonexistent entry`() = runBlocking<Unit> {
        val result = service.removeFromCache("/nonexistent.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `clearCache removes all entries`() = runBlocking<Unit> {
        service.addToCache("/file1.txt", 1)
        service.addToCache("/file2.txt", 2)
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
    fun `getSyncStatus with null path initially empty`() = runBlocking<Unit> {
        val status = service.getSyncStatus(null).first()
        assertTrue(status.isEmpty())
    }

    @Test
    fun `syncFile returns progress flow ending with COMPLETED`() = runBlocking<Unit> {
        val results = service.syncFile("/file.txt", false).toList()
        assertTrue(results.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, results.last().status)
        assertEquals(NetworkOperation.Type.SYNC, results.last().type)
    }

    @Test
    fun `syncFile with forceSync returns COMPLETED`() = runBlocking<Unit> {
        val results = service.syncFile("/file.txt", true).toList()
        assertEquals(NetworkOperation.Status.COMPLETED, results.last().status)
    }

    @Test
    fun `syncFile updates sync status to SYNCED`() = runBlocking<Unit> {
        service.syncFile("/docs/file.txt", false).toList()
        val status = service.getSyncStatus("/docs").first()
        assertTrue(status.containsKey("/docs/file.txt"))
        assertEquals(SyncStatus.SYNCED, status["/docs/file.txt"])
    }

    @Test
    fun `syncFile ends with progress 1 point 0`() = runBlocking<Unit> {
        val results = service.syncFile("/file.txt", false).toList()
        assertEquals(1.0, results.last().progress)
    }

    @Test
    fun `getSyncStatus filters by path prefix`() = runBlocking<Unit> {
        service.syncFile("/docs/a.txt", false).toList()
        service.syncFile("/photos/b.jpg", false).toList()
        val docStatuses = service.getSyncStatus("/docs").first()
        assertEquals(1, docStatuses.size)
        assertTrue(docStatuses.containsKey("/docs/a.txt"))
    }

    // ==================== OPERATION MANAGEMENT ====================

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
    fun `pauseOperation returns success`() = runBlocking<Unit> {
        val result = service.pauseOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resumeOperation returns success`() = runBlocking<Unit> {
        val result = service.resumeOperation(12345L)
        assertTrue(result.isSuccess)
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

    // ==================== GET FILE INFO (disconnected) ====================

    @Test
    fun `getFileInfo when disconnected returns default document`() = runBlocking<Unit> {
        val result = service.getFileInfo("/test.md")
        assertTrue(result.isSuccess)
        val doc = result.getOrNull()
        assertNotNull(doc)
        assertEquals("/test.md", doc.path)
        assertEquals("test.md", doc.name)
        assertFalse(doc.isFolder)
        assertEquals("webdav", doc.storageId)
    }

    @Test
    fun `getFileInfo extracts file name from path`() = runBlocking<Unit> {
        val result = service.getFileInfo("/documents/report.pdf")
        val doc = result.getOrNull()
        assertNotNull(doc)
        assertEquals("report.pdf", doc.name)
    }

    // ==================== QUOTA INFO ====================

    @Test
    fun `getQuotaInfo returns success with default values`() = runBlocking<Unit> {
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess)
        val quota = result.getOrNull()
        assertNotNull(quota)
        assertEquals(1000000000L, quota.totalSpace)
        assertEquals(100000000L, quota.usedSpace)
        assertEquals(900000000L, quota.availableSpace)
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
    fun `getQuotaInfo consistent total equals used plus available`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(quota.totalSpace, quota.usedSpace + quota.availableSpace)
    }

    @Test
    fun `getQuotaInfo usage percentage is 0 point 1`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(0.1, quota.usagePercentage)
    }

    // ==================== CONFIGURATION VARIATIONS ====================

    @Test
    fun `service with different URL`() = runBlocking<Unit> {
        val differentConfig = config.copy(url = "https://nextcloud.example.com/dav/")
        val differentService = WebDavService(differentConfig)
        val info = differentService.getStorageInfo()
        assertEquals("https://nextcloud.example.com/dav/", info.location)
    }

    @Test
    fun `service with custom timeout`() {
        val timeoutConfig = config.copy(connectionTimeout = 60000)
        val timeoutService = WebDavService(timeoutConfig)
        assertEquals(60000, (timeoutService.config as StorageConfig.WebDavConfig).connectionTimeout)
    }

    @Test
    fun `service with verify certificate disabled`() {
        val noVerifyConfig = config.copy(verifyCertificate = false)
        val noVerifyService = WebDavService(noVerifyConfig)
        assertFalse((noVerifyService.config as StorageConfig.WebDavConfig).verifyCertificate)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `special characters in path for validatePath`() {
        assertTrue(service.validatePath("/path with spaces/file.txt").isSuccess)
        assertTrue(service.validatePath("/path-with-dashes/file.txt").isSuccess)
        assertTrue(service.validatePath("/path_with_underscores/file.txt").isSuccess)
    }

    @Test
    fun `multiple sequential cache operations consistency`() = runBlocking<Unit> {
        service.addToCache("/a.txt", 1)
        service.addToCache("/b.txt", 2)
        service.removeFromCache("/a.txt")
        val entries = service.getCacheEntries(null).first()
        assertEquals(1, entries.size)
        assertEquals("/b.txt", entries[0].remotePath)
    }

    @Test
    fun `sync then cache get consistency`() = runBlocking<Unit> {
        service.syncFile("/file.txt", false).toList()
        val statuses = service.getSyncStatus("/file").first()
        assertTrue(statuses.containsKey("/file.txt"))
    }
}
