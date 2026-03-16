/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Git Service Deep Test Suite
 *
 * Comprehensive tests for Git protocol implementation
 * covering path utilities, storage info, connection state,
 * cache lifecycle, sync status, operation management,
 * quota info, and Git-specific configuration variations.
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.git

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Deep test suite for GitService.
 *
 * Git uses ktor HttpClient for real HTTP operations. The connect() method
 * marks the service as connected even when the HTTP request fails (for
 * offline-resilient usage), so many operations can be tested end-to-end
 * without a live server. File operations like createFolder, deleteFile,
 * moveFile, renameFile, and copyFile track changes locally when no
 * platform API is reachable.
 *
 * This test suite focuses on:
 * - Initialization and configuration access
 * - Path utilities (getParentPath, validatePath)
 * - Storage info metadata
 * - Connection state (initially offline)
 * - Cache operations lifecycle (add, filter, remove, clear)
 * - Sync status tracking
 * - Operation management (active ops, cancel, pause, resume)
 * - Quota info (Git has unlimited quota)
 * - Configuration variations (branches, URLs, tokens)
 */
class GitServiceDeepTest {

    private lateinit var service: GitService
    private lateinit var config: StorageConfig.GitConfig

    @BeforeTest
    fun setup() {
        config = StorageConfig.GitConfig(
            name = "test-git",
            repositoryUrl = "https://github.com/user/repo.git",
            branch = "main",
            username = "testuser",
            personalAccessToken = "test-token",
            localCachePath = "/tmp/git-cache"
        )
        service = GitService(config)
    }

    // ==================== INITIALIZATION TESTS ====================

    @Test
    fun `service initializes with correct name`() {
        assertEquals("test-git", service.config.name)
    }

    @Test
    fun `service initializes with correct repository URL`() {
        assertEquals("https://github.com/user/repo.git",
            (service.config as StorageConfig.GitConfig).repositoryUrl)
    }

    @Test
    fun `service initializes with correct branch`() {
        assertEquals("main", (service.config as StorageConfig.GitConfig).branch)
    }

    @Test
    fun `service initializes with correct username`() {
        assertEquals("testuser", (service.config as StorageConfig.GitConfig).username)
    }

    @Test
    fun `service initializes with correct token`() {
        assertEquals("test-token",
            (service.config as StorageConfig.GitConfig).personalAccessToken)
    }

    @Test
    fun `service initializes with correct local cache path`() {
        assertEquals("/tmp/git-cache",
            (service.config as StorageConfig.GitConfig).localCachePath)
    }

    @Test
    fun `service config is GitConfig type`() {
        assertIs<StorageConfig.GitConfig>(service.config)
    }

    @Test
    fun `service starts offline`() {
        assertFalse(service.isOnline)
    }

    @Test
    fun `service root path is forward slash`() {
        assertEquals("/", service.rootPath)
    }

    // ==================== PATH UTILITIES ====================

    @Test
    fun `getParentPath of file returns parent directory`() {
        assertEquals("/src", service.getParentPath("/src/main.kt"))
    }

    @Test
    fun `getParentPath of top-level file returns root`() {
        assertEquals("/", service.getParentPath("/README.md"))
    }

    @Test
    fun `getParentPath of nested path returns intermediate`() {
        assertEquals("/a/b", service.getParentPath("/a/b/c"))
    }

    @Test
    fun `getParentPath of deeply nested path`() {
        assertEquals("/src/main/kotlin/digital",
            service.getParentPath("/src/main/kotlin/digital/Main.kt"))
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
        assertTrue(service.validatePath("/src/main.kt").isSuccess)
    }

    @Test
    fun `validatePath succeeds for single file`() {
        assertTrue(service.validatePath("/README.md").isSuccess)
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

    @Test
    fun `validatePath succeeds for git-specific paths`() {
        assertTrue(service.validatePath("/.gitignore").isSuccess)
        assertTrue(service.validatePath("/.github/workflows/ci.yml").isSuccess)
    }

    // ==================== STORAGE INFO ====================

    @Test
    fun `getStorageInfo returns correct id`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("git_test-git", info.id)
    }

    @Test
    fun `getStorageInfo returns correct name`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("test-git", info.name)
    }

    @Test
    fun `getStorageInfo returns GIT type`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals(StorageType.GIT, info.type)
    }

    @Test
    fun `getStorageInfo shows offline when not connected`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertFalse(info.isOnline)
    }

    @Test
    fun `getStorageInfo has non-null lastSync`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertNotNull(info.lastSync)
    }

    // ==================== CONNECTION STATE ====================

    @Test
    fun `isOnline false initially`() {
        assertFalse(service.isOnline)
    }

    @Test
    fun `disconnect returns success when not connected`() = runBlocking<Unit> {
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `multiple disconnect calls succeed`() = runBlocking<Unit> {
        assertTrue(service.disconnect().isSuccess)
        assertTrue(service.disconnect().isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `service state consistent after disconnect`() = runBlocking<Unit> {
        service.disconnect()
        assertFalse(service.isOnline)
        val info = service.getStorageInfo()
        assertFalse(info.isOnline)
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
        val result = service.addToCache("/src/main.kt", 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addToCache then getCacheEntries returns entry`() = runBlocking<Unit> {
        service.addToCache("/src/main.kt", 1)
        val entries = service.getCacheEntries("/src").first()
        assertEquals(1, entries.size)
        assertEquals("/src/main.kt", entries[0].remotePath)
    }

    @Test
    fun `addToCache multiple entries all returned`() = runBlocking<Unit> {
        service.addToCache("/file1.kt", 1)
        service.addToCache("/file2.kt", 2)
        service.addToCache("/file3.kt", 3)
        val entries = service.getCacheEntries(null).first()
        assertEquals(3, entries.size)
    }

    @Test
    fun `getCacheEntries filters by path prefix`() = runBlocking<Unit> {
        service.addToCache("/src/file1.kt", 1)
        service.addToCache("/docs/file2.md", 1)
        val srcEntries = service.getCacheEntries("/src").first()
        assertEquals(1, srcEntries.size)
    }

    @Test
    fun `removeFromCache removes specific entry`() = runBlocking<Unit> {
        service.addToCache("/file.kt", 1)
        service.removeFromCache("/file.kt")
        val entries = service.getCacheEntries(null).first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `removeFromCache returns success for nonexistent entry`() = runBlocking<Unit> {
        val result = service.removeFromCache("/nonexistent.kt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `clearCache removes all entries`() = runBlocking<Unit> {
        service.addToCache("/file1.kt", 1)
        service.addToCache("/file2.kt", 2)
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
        assertTrue(service.addToCache("/a.kt", 0).isSuccess)
        assertTrue(service.addToCache("/b.kt", 5).isSuccess)
        assertTrue(service.addToCache("/c.kt", 100).isSuccess)
    }

    @Test
    fun `addToCache overwrites existing entry with same path`() = runBlocking<Unit> {
        service.addToCache("/file.kt", 1)
        service.addToCache("/file.kt", 5)
        val entries = service.getCacheEntries(null).first()
        assertEquals(1, entries.size)
    }

    @Test
    fun `cache entry local path uses config localCachePath`() = runBlocking<Unit> {
        service.addToCache("/src/main.kt", 1)
        val entries = service.getCacheEntries("/src").first()
        assertEquals(1, entries.size)
        assertTrue(entries[0].localPath.contains("/tmp/git-cache"))
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
        val results = service.syncFile("/src/main.kt", false).toList()
        assertTrue(results.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, results.last().status)
        assertEquals(NetworkOperation.Type.SYNC, results.last().type)
    }

    @Test
    fun `syncFile with forceSync returns COMPLETED`() = runBlocking<Unit> {
        val results = service.syncFile("/src/main.kt", true).toList()
        assertEquals(NetworkOperation.Status.COMPLETED, results.last().status)
    }

    @Test
    fun `syncFile updates sync status to SYNCED`() = runBlocking<Unit> {
        service.syncFile("/src/main.kt", false).toList()
        val status = service.getSyncStatus("/src").first()
        assertTrue(status.containsKey("/src/main.kt"))
        assertEquals(SyncStatus.SYNCED, status["/src/main.kt"])
    }

    @Test
    fun `syncFile ends with progress 1 point 0`() = runBlocking<Unit> {
        val results = service.syncFile("/file.kt", false).toList()
        assertEquals(1.0, results.last().progress)
    }

    @Test
    fun `syncFile has multiple progress emissions`() = runBlocking<Unit> {
        val results = service.syncFile("/file.kt", false).toList()
        assertTrue(results.size > 1, "syncFile should emit multiple progress updates")
    }

    @Test
    fun `getSyncStatus filters by path prefix`() = runBlocking<Unit> {
        service.syncFile("/src/a.kt", false).toList()
        service.syncFile("/docs/b.md", false).toList()
        val srcStatuses = service.getSyncStatus("/src").first()
        assertEquals(1, srcStatuses.size)
        assertTrue(srcStatuses.containsKey("/src/a.kt"))
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

    // ==================== QUOTA INFO ====================

    @Test
    fun `getQuotaInfo returns success`() = runBlocking<Unit> {
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getQuotaInfo returns MAX_VALUE for total space`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(Long.MAX_VALUE, quota.totalSpace)
    }

    @Test
    fun `getQuotaInfo returns MAX_VALUE for available space`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(Long.MAX_VALUE, quota.availableSpace)
    }

    @Test
    fun `getQuotaInfo returns zero used space`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(0L, quota.usedSpace)
    }

    @Test
    fun `getQuotaInfo usage percentage is zero`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrNull()
        assertNotNull(quota)
        assertEquals(0.0, quota.usagePercentage)
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

    // ==================== CONFIGURATION VARIATIONS ====================

    @Test
    fun `service with different branches`() {
        val branches = listOf("main", "master", "develop", "feature/test", "release/v1.0")
        for (branch in branches) {
            val cfg = config.copy(branch = branch)
            val svc = GitService(cfg)
            assertEquals(branch, (svc.config as StorageConfig.GitConfig).branch)
        }
    }

    @Test
    fun `service with empty repository URL`() {
        val emptyConfig = config.copy(repositoryUrl = "")
        val emptyService = GitService(emptyConfig)
        assertFalse(emptyService.isOnline)
        assertEquals("", (emptyService.config as StorageConfig.GitConfig).repositoryUrl)
    }

    @Test
    fun `service with GitLab URL`() {
        val gitlabConfig = config.copy(repositoryUrl = "https://gitlab.com/user/repo.git")
        val gitlabService = GitService(gitlabConfig)
        assertEquals("https://gitlab.com/user/repo.git",
            (gitlabService.config as StorageConfig.GitConfig).repositoryUrl)
    }

    @Test
    fun `service with Bitbucket URL`() {
        val bbConfig = config.copy(repositoryUrl = "https://bitbucket.org/user/repo.git")
        val bbService = GitService(bbConfig)
        assertEquals("https://bitbucket.org/user/repo.git",
            (bbService.config as StorageConfig.GitConfig).repositoryUrl)
    }

    @Test
    fun `service with custom timeout`() {
        val timeoutConfig = config.copy(connectionTimeout = 60000)
        val timeoutService = GitService(timeoutConfig)
        assertEquals(60000, (timeoutService.config as StorageConfig.GitConfig).connectionTimeout)
    }

    @Test
    fun `service with auto sync disabled`() {
        val noSyncConfig = config.copy(autoSync = false)
        val noSyncService = GitService(noSyncConfig)
        assertFalse((noSyncService.config as StorageConfig.GitConfig).autoSync)
    }

    @Test
    fun `service with custom commit author`() {
        val authorConfig = config.copy(
            commitAuthorName = "Test User",
            commitAuthorEmail = "test@example.com"
        )
        val authorService = GitService(authorConfig)
        assertEquals("Test User",
            (authorService.config as StorageConfig.GitConfig).commitAuthorName)
        assertEquals("test@example.com",
            (authorService.config as StorageConfig.GitConfig).commitAuthorEmail)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `special characters in path for validatePath`() {
        assertTrue(service.validatePath("/path with spaces/file.kt").isSuccess)
        assertTrue(service.validatePath("/path-with-dashes/file.kt").isSuccess)
        assertTrue(service.validatePath("/path_with_underscores/file.kt").isSuccess)
    }

    @Test
    fun `deep path for getParentPath`() {
        assertEquals("/a/b/c/d/e/f/g/h/i",
            service.getParentPath("/a/b/c/d/e/f/g/h/i/j"))
    }

    @Test
    fun `multiple sequential cache operations consistency`() = runBlocking<Unit> {
        service.addToCache("/a.kt", 1)
        service.addToCache("/b.kt", 2)
        service.removeFromCache("/a.kt")
        val entries = service.getCacheEntries(null).first()
        assertEquals(1, entries.size)
        assertEquals("/b.kt", entries[0].remotePath)
    }

    @Test
    fun `sync then check status consistency`() = runBlocking<Unit> {
        service.syncFile("/src/main.kt", false).toList()
        val statuses = service.getSyncStatus("/src").first()
        assertTrue(statuses.containsKey("/src/main.kt"))
    }

    @Test
    fun `getFileInfo when disconnected returns default document`() = runBlocking<Unit> {
        val result = service.getFileInfo("/README.md")
        assertTrue(result.isSuccess)
        val doc = result.getOrNull()
        assertNotNull(doc)
        assertEquals("/README.md", doc.path)
        assertEquals("README.md", doc.name)
        assertFalse(doc.isFolder)
        assertEquals("git", doc.storageId)
    }

    @Test
    fun `exists when disconnected returns false`() = runBlocking<Unit> {
        val result = service.exists("/README.md")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun `createFolder when disconnected succeeds with local tracking`() = runBlocking<Unit> {
        val result = service.createFolder("/new-folder")
        assertTrue(result.isSuccess)
        val doc = result.getOrNull()
        assertNotNull(doc)
        assertEquals("/new-folder", doc.path)
        assertEquals("new-folder", doc.name)
        assertTrue(doc.isFolder)
        assertEquals("git", doc.storageId)
    }

    @Test
    fun `deleteFile when disconnected succeeds with local tracking`() = runBlocking<Unit> {
        val result = service.deleteFile("/old-file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `copyFile when disconnected succeeds with local tracking`() = runBlocking<Unit> {
        val result = service.copyFile("/source.kt", "/dest.kt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `renameFile when disconnected succeeds with local tracking`() = runBlocking<Unit> {
        val result = service.renameFile("/old.kt", "new.kt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `moveFile when disconnected succeeds with local tracking`() = runBlocking<Unit> {
        val result = service.moveFile("/old/file.kt", "/new/file.kt")
        assertTrue(result.isSuccess)
        val doc = result.getOrNull()
        assertNotNull(doc)
        assertEquals("/new/file.kt", doc.path)
        assertEquals("file.kt", doc.name)
        assertEquals("git", doc.storageId)
    }
}
