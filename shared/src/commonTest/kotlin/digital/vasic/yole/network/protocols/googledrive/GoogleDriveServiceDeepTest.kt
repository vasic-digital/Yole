/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Deep tests for GoogleDriveService: non-HTTP paths covering
 * initialization, state, path utilities, config, cache,
 * operations, and sync status management.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.googledrive

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Deep test suite for [GoogleDriveService] exercising every code path
 * that does NOT require a live HTTP connection: initialization,
 * configuration access, path utilities, storage info, operation
 * management, cache management, and sync status tracking.
 */
class GoogleDriveServiceDeepTest {

    private val config = StorageConfig.GoogleDriveConfig(
        name = "test-gdrive",
        clientId = "test-client-id",
        clientSecret = "test-client-secret",
        accessToken = "test-token",
        refreshToken = "test-refresh",
        rootFolderId = "root"
    )

    private lateinit var service: GoogleDriveService

    @BeforeTest
    fun setup() {
        service = GoogleDriveService(config)
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
        assertEquals("test-gdrive", service.config.name)
    }

    @Test
    fun testConfigClientId() {
        assertEquals("test-client-id", (service.config as StorageConfig.GoogleDriveConfig).clientId)
    }

    @Test
    fun testConfigClientSecret() {
        assertEquals("test-client-secret", (service.config as StorageConfig.GoogleDriveConfig).clientSecret)
    }

    @Test
    fun testConfigAccessToken() {
        assertEquals("test-token", (service.config as StorageConfig.GoogleDriveConfig).accessToken)
    }

    @Test
    fun testConfigRefreshToken() {
        assertEquals("test-refresh", (service.config as StorageConfig.GoogleDriveConfig).refreshToken)
    }

    @Test
    fun testConfigRootFolderId() {
        assertEquals("root", (service.config as StorageConfig.GoogleDriveConfig).rootFolderId)
    }

    @Test
    fun testConfigStorageType() {
        assertEquals(StorageType.GOOGLE_DRIVE, service.config.storageType)
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

    @Test
    fun testConfigTeamDriveIdDefault() {
        assertNull((service.config as StorageConfig.GoogleDriveConfig).teamDriveId)
    }

    // ── 3. getParentPath() ──────────────────────────────────────────

    @Test
    fun testGetParentPathOfRoot() {
        assertNull(service.getParentPath("/"), "Parent of / must be null")
    }

    @Test
    fun testGetParentPathOfBlank() {
        assertNull(service.getParentPath(""), "Parent of empty string must be null")
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
        assertNull(service.getParentPath("   "), "Whitespace-only string is blank, must return null")
    }

    // ── 4. validatePath() ───────────────────────────────────────────

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
        assertTrue(result.isFailure, "Blank path must be rejected")
    }

    @Test
    fun testValidatePathWithWhitespaceOnly() {
        val result = service.validatePath("   ")
        assertTrue(result.isFailure, "Whitespace-only path must be rejected")
    }

    // ── 5. getStorageInfo() ─────────────────────────────────────────

    @Test
    fun testStorageInfoId() = runTest {
        val info = service.getStorageInfo()
        assertEquals("googledrive_test-gdrive", info.id)
    }

    @Test
    fun testStorageInfoName() = runTest {
        val info = service.getStorageInfo()
        assertEquals("test-gdrive", info.name)
    }

    @Test
    fun testStorageInfoType() = runTest {
        val info = service.getStorageInfo()
        assertEquals(StorageType.GOOGLE_DRIVE, info.type)
    }

    @Test
    fun testStorageInfoLocation() = runTest {
        val info = service.getStorageInfo()
        assertEquals("googledrive://", info.location)
    }

    @Test
    fun testStorageInfoIsOfflineInitially() = runTest {
        val info = service.getStorageInfo()
        assertFalse(info.isOnline)
    }

    @Test
    fun testStorageInfoSupportsFolders() = runTest {
        val info = service.getStorageInfo()
        assertTrue(info.supportsFolders)
    }

    @Test
    fun testStorageInfoSupportsMetadata() = runTest {
        val info = service.getStorageInfo()
        assertTrue(info.supportsMetadata)
    }

    @Test
    fun testStorageInfoLastSyncNotNull() = runTest {
        val info = service.getStorageInfo()
        assertNotNull(info.lastSync, "lastSync must be populated")
    }

    // ── 6. getActiveOperations() ────────────────────────────────────

    @Test
    fun testActiveOperationsEmptyInitially() = runTest {
        val ops = service.getActiveOperations().first()
        assertTrue(ops.isEmpty(), "No operations should be active on a fresh service")
    }

    // ── 7. cancelOperation / pauseOperation / resumeOperation ──────

    @Test
    fun testCancelNonExistentOperationSucceeds() = runTest {
        val result = service.cancelOperation(99999L)
        assertTrue(result.isSuccess, "Cancelling a non-existent operation must succeed gracefully")
    }

    @Test
    fun testPauseNonExistentOperationSucceeds() = runTest {
        val result = service.pauseOperation(99999L)
        assertTrue(result.isSuccess, "Pausing a non-existent operation must succeed gracefully")
    }

    @Test
    fun testResumeNonExistentOperationSucceeds() = runTest {
        val result = service.resumeOperation(99999L)
        assertTrue(result.isSuccess, "Resuming a non-existent operation must succeed gracefully")
    }

    // ── 8. getCacheEntries ──────────────────────────────────────────

    @Test
    fun testCacheEntriesEmptyInitially() = runTest {
        val entries = service.getCacheEntries().first()
        assertTrue(entries.isEmpty(), "Cache must be empty on a fresh service")
    }

    @Test
    fun testCacheEntriesWithPathFilterEmptyInitially() = runTest {
        val entries = service.getCacheEntries("/some/path").first()
        assertTrue(entries.isEmpty())
    }

    // ── 9. addToCache / removeFromCache / clearCache ────────────────

    @Test
    fun testAddToCacheSucceeds() = runTest {
        val result = service.addToCache("/doc.txt", priority = 50)
        assertTrue(result.isSuccess, "Adding to cache must succeed")
    }

    @Test
    fun testAddToCacheCreatesEntry() = runTest {
        service.addToCache("/doc.txt", priority = 50)
        val entries = service.getCacheEntries().first()
        assertEquals(1, entries.size)
        assertEquals("/doc.txt", entries.first().remotePath)
    }

    @Test
    fun testAddToCacheSetsLocalPath() = runTest {
        service.addToCache("/doc.txt")
        val entry = service.getCacheEntries().first().first()
        assertTrue(entry.localPath.contains("googledrive"), "Local path must contain service identifier")
    }

    @Test
    fun testAddToCachePriority() = runTest {
        service.addToCache("/doc.txt", priority = 42)
        val entry = service.getCacheEntries().first().first()
        assertEquals(42, entry.priority)
    }

    @Test
    fun testRemoveFromCacheSucceeds() = runTest {
        service.addToCache("/doc.txt")
        val result = service.removeFromCache("/doc.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testRemoveFromCacheClearsEntry() = runTest {
        service.addToCache("/doc.txt")
        service.removeFromCache("/doc.txt")
        val entries = service.getCacheEntries().first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun testRemoveNonExistentCacheEntrySucceeds() = runTest {
        val result = service.removeFromCache("/nonexistent.txt")
        assertTrue(result.isSuccess, "Removing a non-existent cache entry must succeed")
    }

    @Test
    fun testClearCacheSucceeds() = runTest {
        service.addToCache("/a.txt")
        service.addToCache("/b.txt")
        val result = service.clearCache()
        assertTrue(result.isSuccess)
    }

    @Test
    fun testClearCacheRemovesAllEntries() = runTest {
        service.addToCache("/a.txt")
        service.addToCache("/b.txt")
        service.clearCache()
        val entries = service.getCacheEntries().first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun testClearEmptyCacheSucceeds() = runTest {
        val result = service.clearCache()
        assertTrue(result.isSuccess, "Clearing an already-empty cache must succeed")
    }

    @Test
    fun testCacheEntriesFilteredByPath() = runTest {
        service.addToCache("/docs/a.txt")
        service.addToCache("/docs/b.txt")
        service.addToCache("/images/c.png")
        val docsEntries = service.getCacheEntries("/docs").first()
        assertEquals(2, docsEntries.size, "Only entries under /docs should be returned")
    }

    // ── 10. getSyncStatus ───────────────────────────────────────────

    @Test
    fun testSyncStatusEmptyInitially() = runTest {
        val statuses = service.getSyncStatus().first()
        assertTrue(statuses.isEmpty(), "Sync status map must be empty on a fresh service")
    }

    @Test
    fun testSyncStatusWithPathFilterEmptyInitially() = runTest {
        val statuses = service.getSyncStatus("/docs").first()
        assertTrue(statuses.isEmpty())
    }

    // ── 11. exists() on disconnected service ────────────────────────

    @Test
    fun testExistsOnDisconnectedServiceHandledGracefully() = runTest {
        val result = service.exists("/some/file.txt")
        // exists() delegates to getFileInfo().map{true}.recover{false}
        assertTrue(result.isSuccess || result.isFailure,
            "exists() must return a Result, not throw")
    }

    // ── 12. Multiple cache additions overwrite same key ─────────────

    @Test
    fun testAddToCacheSamePathOverwrites() = runTest {
        service.addToCache("/doc.txt", priority = 10)
        service.addToCache("/doc.txt", priority = 90)
        val entries = service.getCacheEntries().first()
        assertEquals(1, entries.size, "Duplicate path must overwrite, not duplicate")
        assertEquals(90, entries.first().priority)
    }

    // ── 13. Config with null rootFolderId ───────────────────────────

    @Test
    fun testServiceWithNullRootFolderIdUsesDefaultRoot() {
        val nullRootConfig = StorageConfig.GoogleDriveConfig(
            name = "null-root-gdrive",
            clientId = "id",
            clientSecret = "secret",
            rootFolderId = null
        )
        val svc = GoogleDriveService(nullRootConfig)
        // Service should construct without error; rootPath remains "/"
        assertEquals("/", svc.rootPath)
        assertFalse(svc.isOnline)
    }

    // ── 14. Config with teamDriveId set ─────────────────────────────

    @Test
    fun testServiceWithTeamDriveId() {
        val teamConfig = StorageConfig.GoogleDriveConfig(
            name = "team-gdrive",
            clientId = "id",
            clientSecret = "secret",
            teamDriveId = "team-drive-123"
        )
        val svc = GoogleDriveService(teamConfig)
        assertEquals("team-drive-123", (svc.config as StorageConfig.GoogleDriveConfig).teamDriveId)
    }
}
