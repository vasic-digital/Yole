/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Deep tests for DropboxService: non-HTTP paths covering
 * initialization, state, path utilities, config, cache,
 * operations, and sync status management.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.dropbox

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Deep test suite for [DropboxService] exercising every code path
 * that does NOT require a live HTTP connection: initialization,
 * configuration access, path utilities, storage info, operation
 * management, cache management, and sync status tracking.
 *
 * Note: Dropbox differs from OneDrive/GoogleDrive in its
 * [getParentPath] (returns "/" for root/blank instead of null) and
 * [validatePath] (always succeeds because blank means root).
 */
class DropboxServiceDeepTest {

    private val config = StorageConfig.DropboxConfig(
        name = "test-dropbox",
        accessToken = "test-token",
        appKey = "test-app-key",
        appSecret = "test-app-secret",
        rootPath = "/test"
    )

    private lateinit var service: DropboxService

    @BeforeTest
    fun setup() {
        service = DropboxService(config)
    }

    // ── 1. Service initialization ───────────────────────────────────

    @Test
    fun testServiceIsNotOnlineAfterConstruction() {
        assertFalse(service.isOnline, "Service must not be online right after construction")
    }

    @Test
    fun testConfigAccessibleAfterConstruction() {
        assertSame(config, service.config, "Config reference must be the one passed to the constructor")
    }

    @Test
    fun testRootPathIsSlash() {
        assertEquals("/", service.rootPath, "Root path must be /")
    }

    // ── 2. Config property access ───────────────────────────────────

    @Test
    fun testConfigName() {
        assertEquals("test-dropbox", service.config.name)
    }

    @Test
    fun testConfigAccessToken() {
        assertEquals("test-token", (service.config as StorageConfig.DropboxConfig).accessToken)
    }

    @Test
    fun testConfigAppKey() {
        assertEquals("test-app-key", (service.config as StorageConfig.DropboxConfig).appKey)
    }

    @Test
    fun testConfigAppSecret() {
        assertEquals("test-app-secret", (service.config as StorageConfig.DropboxConfig).appSecret)
    }

    @Test
    fun testConfigRootPath() {
        assertEquals("/test", (service.config as StorageConfig.DropboxConfig).rootPath)
    }

    @Test
    fun testConfigRefreshTokenDefault() {
        assertNull((service.config as StorageConfig.DropboxConfig).refreshToken)
    }

    @Test
    fun testConfigStorageType() {
        assertEquals(StorageType.DROPBOX, service.config.storageType)
    }

    @Test
    fun testConfigIsEnabledDefault() {
        assertTrue(service.config.isEnabled)
    }

    @Test
    fun testConfigPriorityDefault() {
        assertEquals(100, service.config.priority)
    }

    @Test
    fun testConfigMetadataDefault() {
        assertTrue(service.config.metadata.isEmpty())
    }

    // ── 3. getParentPath() ──────────────────────────────────────────
    //    Dropbox returns "/" for root and blank (not null like OneDrive/GDrive)

    @Test
    fun testGetParentPathOfRoot() {
        assertEquals("/", service.getParentPath("/"), "Parent of / must be / for Dropbox")
    }

    @Test
    fun testGetParentPathOfBlank() {
        assertEquals("/", service.getParentPath(""), "Parent of empty string must be / for Dropbox")
    }

    @Test
    fun testGetParentPathOfSingleSegment() {
        assertEquals("/", service.getParentPath("/a"), "Parent of /a must be /")
    }

    @Test
    fun testGetParentPathOfTwoSegments() {
        assertEquals("/a", service.getParentPath("/a/b"), "Parent of /a/b must be /a")
    }

    @Test
    fun testGetParentPathOfThreeSegments() {
        assertEquals("/a/b", service.getParentPath("/a/b/c"), "Parent of /a/b/c must be /a/b")
    }

    @Test
    fun testGetParentPathOfDeeplyNestedPath() {
        assertEquals("/a/b/c/d", service.getParentPath("/a/b/c/d/e"))
    }

    @Test
    fun testGetParentPathOfWhitespaceOnlyString() {
        assertEquals("/", service.getParentPath("   "), "Whitespace-only string is blank, Dropbox returns /")
    }

    // ── 4. validatePath() ───────────────────────────────────────────
    //    Dropbox always returns success (blank = root)

    @Test
    fun testValidatePathWithValidRoot() {
        val result = service.validatePath("/")
        assertTrue(result.isSuccess, "Root path must be valid")
    }

    @Test
    fun testValidatePathWithValidSubpath() {
        val result = service.validatePath("/documents/notes.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testValidatePathWithBlank() {
        val result = service.validatePath("")
        assertTrue(result.isSuccess, "Blank path is valid for Dropbox (treated as root)")
    }

    @Test
    fun testValidatePathWithWhitespaceOnly() {
        val result = service.validatePath("   ")
        assertTrue(result.isSuccess, "Whitespace-only path is valid for Dropbox (treated as root)")
    }

    // ── 5. getStorageInfo() ─────────────────────────────────────────

    @Test
    fun testStorageInfoId() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("dropbox_test-dropbox", info.id)
    }

    @Test
    fun testStorageInfoName() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("test-dropbox", info.name)
    }

    @Test
    fun testStorageInfoType() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals(StorageType.DROPBOX, info.type)
    }

    @Test
    fun testStorageInfoLocation() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("dropbox:///test", info.location)
    }

    @Test
    fun testStorageInfoIsOfflineInitially() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertFalse(info.isOnline)
    }

    @Test
    fun testStorageInfoSupportsFolders() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertTrue(info.supportsFolders)
    }

    @Test
    fun testStorageInfoSupportsMetadata() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertTrue(info.supportsMetadata)
    }

    @Test
    fun testStorageInfoLastSyncNotNull() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertNotNull(info.lastSync, "lastSync must be populated")
    }

    // ── 6. getActiveOperations() ────────────────────────────────────

    @Test
    fun testActiveOperationsEmptyInitially() = runBlocking<Unit> {
        val ops = service.getActiveOperations().first()
        assertTrue(ops.isEmpty(), "No operations should be active on a fresh service")
    }

    // ── 7. cancelOperation / pauseOperation / resumeOperation ──────

    @Test
    fun testCancelNonExistentOperationSucceeds() = runBlocking<Unit> {
        val result = service.cancelOperation(99999L)
        assertTrue(result.isSuccess, "Cancelling a non-existent operation must succeed gracefully")
    }

    @Test
    fun testPauseNonExistentOperationSucceeds() = runBlocking<Unit> {
        val result = service.pauseOperation(99999L)
        assertTrue(result.isSuccess, "Pausing a non-existent operation must succeed gracefully")
    }

    @Test
    fun testResumeNonExistentOperationSucceeds() = runBlocking<Unit> {
        val result = service.resumeOperation(99999L)
        assertTrue(result.isSuccess, "Resuming a non-existent operation must succeed gracefully")
    }

    // ── 8. getCacheEntries ──────────────────────────────────────────

    @Test
    fun testCacheEntriesEmptyInitially() = runBlocking<Unit> {
        val entries = service.getCacheEntries().first()
        assertTrue(entries.isEmpty(), "Cache must be empty on a fresh service")
    }

    @Test
    fun testCacheEntriesWithPathFilterEmptyInitially() = runBlocking<Unit> {
        val entries = service.getCacheEntries("/some/path").first()
        assertTrue(entries.isEmpty())
    }

    // ── 9. addToCache / removeFromCache / clearCache ────────────────

    @Test
    fun testAddToCacheSucceeds() = runBlocking<Unit> {
        val result = service.addToCache("/doc.txt", priority = 50)
        assertTrue(result.isSuccess, "Adding to cache must succeed")
    }

    @Test
    fun testAddToCacheCreatesEntry() = runBlocking<Unit> {
        service.addToCache("/doc.txt", priority = 50)
        val entries = service.getCacheEntries().first()
        assertEquals(1, entries.size)
        assertEquals("/doc.txt", entries.first().remotePath)
    }

    @Test
    fun testAddToCacheSetsLocalPath() = runBlocking<Unit> {
        service.addToCache("/doc.txt")
        val entry = service.getCacheEntries().first().first()
        assertTrue(entry.localPath.contains("dropbox"), "Local path must contain service identifier")
    }

    @Test
    fun testAddToCachePriority() = runBlocking<Unit> {
        service.addToCache("/doc.txt", priority = 42)
        val entry = service.getCacheEntries().first().first()
        assertEquals(42, entry.priority)
    }

    @Test
    fun testRemoveFromCacheSucceeds() = runBlocking<Unit> {
        service.addToCache("/doc.txt")
        val result = service.removeFromCache("/doc.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testRemoveFromCacheClearsEntry() = runBlocking<Unit> {
        service.addToCache("/doc.txt")
        service.removeFromCache("/doc.txt")
        val entries = service.getCacheEntries().first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun testRemoveNonExistentCacheEntrySucceeds() = runBlocking<Unit> {
        val result = service.removeFromCache("/nonexistent.txt")
        assertTrue(result.isSuccess, "Removing a non-existent cache entry must succeed")
    }

    @Test
    fun testClearCacheSucceeds() = runBlocking<Unit> {
        service.addToCache("/a.txt")
        service.addToCache("/b.txt")
        val result = service.clearCache()
        assertTrue(result.isSuccess)
    }

    @Test
    fun testClearCacheRemovesAllEntries() = runBlocking<Unit> {
        service.addToCache("/a.txt")
        service.addToCache("/b.txt")
        service.clearCache()
        val entries = service.getCacheEntries().first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun testClearEmptyCacheSucceeds() = runBlocking<Unit> {
        val result = service.clearCache()
        assertTrue(result.isSuccess, "Clearing an already-empty cache must succeed")
    }

    @Test
    fun testCacheEntriesFilteredByPath() = runBlocking<Unit> {
        service.addToCache("/docs/a.txt")
        service.addToCache("/docs/b.txt")
        service.addToCache("/images/c.png")
        val docsEntries = service.getCacheEntries("/docs").first()
        assertEquals(2, docsEntries.size, "Only entries under /docs should be returned")
    }

    // ── 10. getSyncStatus ───────────────────────────────────────────

    @Test
    fun testSyncStatusEmptyInitially() = runBlocking<Unit> {
        val statuses = service.getSyncStatus().first()
        assertTrue(statuses.isEmpty(), "Sync status map must be empty on a fresh service")
    }

    @Test
    fun testSyncStatusWithPathFilterEmptyInitially() = runBlocking<Unit> {
        val statuses = service.getSyncStatus("/docs").first()
        assertTrue(statuses.isEmpty())
    }

    // ── 11. exists() on disconnected service ────────────────────────

    @Test
    fun testExistsOnDisconnectedServiceHandledGracefully() = runBlocking<Unit> {
        val result = service.exists("/some/file.txt")
        // exists() delegates to getFileInfo().map{true}.recover{false}
        assertTrue(result.isSuccess || result.isFailure,
            "exists() must return a Result, not throw")
    }

    // ── 12. Multiple cache additions overwrite same key ─────────────

    @Test
    fun testAddToCacheSamePathOverwrites() = runBlocking<Unit> {
        service.addToCache("/doc.txt", priority = 10)
        service.addToCache("/doc.txt", priority = 90)
        val entries = service.getCacheEntries().first()
        assertEquals(1, entries.size, "Duplicate path must overwrite, not duplicate")
        assertEquals(90, entries.first().priority)
    }

    // ── 13. Blank rootPath handling ─────────────────────────────────

    @Test
    fun testServiceWithBlankRootPathUsesEmpty() {
        val blankConfig = StorageConfig.DropboxConfig(
            name = "blank-root",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret",
            rootPath = ""
        )
        val svc = DropboxService(blankConfig)
        assertEquals("/", svc.rootPath, "rootPath property must always be /")
        assertFalse(svc.isOnline)
    }

    @Test
    fun testStorageInfoLocationWithBlankRoot() = runBlocking<Unit> {
        val blankConfig = StorageConfig.DropboxConfig(
            name = "blank-root",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret",
            rootPath = ""
        )
        val svc = DropboxService(blankConfig)
        val info = svc.getStorageInfo()
        // _rootPath is "" when config.rootPath is blank
        assertEquals("dropbox://", info.location)
    }

    @Test
    fun testServiceWithWhitespaceRootPathUsesEmpty() {
        val wsConfig = StorageConfig.DropboxConfig(
            name = "ws-root",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret",
            rootPath = "   "
        )
        val svc = DropboxService(wsConfig)
        assertEquals("/", svc.rootPath)
    }

    // ── 14. Config with refreshToken set ────────────────────────────

    @Test
    fun testServiceWithRefreshToken() {
        val rtConfig = StorageConfig.DropboxConfig(
            name = "rt-dropbox",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret",
            refreshToken = "my-refresh-token"
        )
        val svc = DropboxService(rtConfig)
        assertEquals("my-refresh-token", (svc.config as StorageConfig.DropboxConfig).refreshToken)
    }
}
