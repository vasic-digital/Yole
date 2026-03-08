/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Git Service Enhanced Test Suite
 *
 * Comprehensive tests for Git protocol implementation
 * covering connection management, file operations, error
 * handling, and Git-specific behaviors.
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.git

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Enhanced test suite for GitService.
 * Tests all Git operations including connection, file operations,
 * cache management, sync status, and error handling.
 */
class GitServiceEnhancedTest {

    private lateinit var service: GitService
    private lateinit var config: StorageConfig.GitConfig

    @BeforeTest
    fun setup() {
        config = StorageConfig.GitConfig(
            name = "test-git",
            repositoryUrl = "https://github.com/example/repo.git",
            branch = "main",
            username = "testuser",
            personalAccessToken = "ghp_testtoken123",
            localCachePath = "/tmp/test-git-cache"
        )
        service = GitService(config)
    }

    // ==================== INITIALIZATION TESTS ====================

    @Test
    fun `service initializes with correct config`() {
        assertEquals("test-git", config.name)
        assertEquals("https://github.com/example/repo.git", config.repositoryUrl)
        assertEquals("main", config.branch)
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
        assertIs<StorageConfig.GitConfig>(service.config)
    }

    // ==================== CONNECTION TESTS ====================

    @Test
    fun `disconnect returns success`() = runBlocking {
        // Note: connect() may fail due to network, but disconnect should always succeed
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
    fun `multiple disconnect calls succeed`() = runBlocking {
        assertTrue(service.disconnect().isSuccess)
        assertFalse(service.isOnline)
        assertTrue(service.disconnect().isSuccess)
        assertFalse(service.isOnline)
    }

    // ==================== STORAGE INFO TESTS ====================

    @Test
    fun `getStorageInfo returns correct metadata`() = runBlocking {
        val info = service.getStorageInfo()
        assertEquals("git_test-git", info.id)
        assertEquals("test-git", info.name)
        assertEquals(StorageType.GIT, info.type)
    }

    @Test
    fun `getStorageInfo reflects disconnected state`() = runBlocking {
        val info = service.getStorageInfo()
        assertFalse(info.isOnline)
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
    fun `listFiles with different paths returns correct failure`() = runBlocking {
        val paths = listOf("/", "/src", "/src/main", "/docs")
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
    fun `uploadFile has correct operation type when failing`() = runBlocking {
        val results = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(results.all { it.type == NetworkOperation.Type.UPLOAD })
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
    fun `downloadFile has correct operation type when failing`() = runBlocking {
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
        // When not connected, moveFile tracks locally and returns PENDING_UPLOAD
        assertEquals(SyncStatus.PENDING_UPLOAD, document.syncStatus)
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
        val result = service.createFolder("/src/newfolder")
        assertTrue(result.isSuccess)
        val document = result.getOrNull()
        assertNotNull(document)
        assertEquals("/src/newfolder", document.path)
        assertEquals("newfolder", document.name)
        assertTrue(document.isFolder)
        assertEquals(0L, document.size)
    }

    @Test
    fun `createFolder returns document with execute permission`() = runBlocking {
        val result = service.createFolder("/src/newfolder")
        val document = result.getOrNull()
        assertNotNull(document)
        assertTrue(document.permissions.contains(DocumentPermission.EXECUTE))
    }

    // ==================== FILE INFO TESTS ====================

    @Test
    fun `getFileInfo returns success with correct document`() = runBlocking {
        val result = service.getFileInfo("/src/main.kt")
        assertTrue(result.isSuccess)
        val document = result.getOrNull()
        assertNotNull(document)
        assertEquals("/src/main.kt", document.path)
        assertEquals("main.kt", document.name)
        assertFalse(document.isFolder)
        assertEquals("git", document.storageId)
    }

    @Test
    fun `exists returns result`() = runBlocking {
        val result = service.exists("/src/main.kt")
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
        val result = service.addToCache("/src/main.kt", 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addToCache with different priorities succeeds`() = runBlocking {
        assertTrue(service.addToCache("/file1.kt", 0).isSuccess)
        assertTrue(service.addToCache("/file2.kt", 1).isSuccess)
        assertTrue(service.addToCache("/file3.kt", 10).isSuccess)
    }

    @Test
    fun `removeFromCache returns success`() = runBlocking {
        val result = service.removeFromCache("/src/main.kt")
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
        val results = service.syncFile("/src/main.kt", false).toList()
        assertTrue(results.isNotEmpty())

        // Should have progress updates
        val lastResult = results.last()
        assertEquals(NetworkOperation.Status.COMPLETED, lastResult.status)
        assertEquals(NetworkOperation.Type.SYNC, lastResult.type)
    }

    @Test
    fun `syncFile with forceSync flag succeeds`() = runBlocking {
        val results = service.syncFile("/src/main.kt", true).toList()
        assertTrue(results.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, results.last().status)
    }

    @Test
    fun `syncAll returns flow`() = runBlocking {
        val results = service.syncAll(false).toList()
        // syncAll now returns FAILED operation when not connected
        assertEquals(1, results.size, "syncAll should return one failed operation when not connected")
        assertEquals(NetworkOperation.Type.SYNC, results[0].type)
        assertEquals(NetworkOperation.Status.FAILED, results[0].status)
        assertEquals("Git not connected", results[0].error)
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
    fun `getQuotaInfo returns valid quota for git`() = runBlocking {
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess)
        val quota = result.getOrNull()
        assertNotNull(quota)
        // Git has unlimited quota
        assertEquals(Long.MAX_VALUE, quota.totalSpace)
        assertEquals(Long.MAX_VALUE, quota.availableSpace)
        assertEquals(0L, quota.usedSpace)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
    }

    @Test
    fun `getQuotaInfo returns zero usage percentage`() = runBlocking {
        val result = service.getQuotaInfo()
        val quota = result.getOrNull()
        assertNotNull(quota)
        assertEquals(0.0, quota.usagePercentage)
    }

    // ==================== PATH UTILITY TESTS ====================

    @Test
    fun `getParentPath returns correct parent`() {
        assertEquals("/src", service.getParentPath("/src/main.kt"))
        assertEquals("/", service.getParentPath("/src"))
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
        assertTrue(service.validatePath("/src").isSuccess)
        assertTrue(service.validatePath("/src/main.kt").isSuccess)
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

    // ==================== GIT-SPECIFIC TESTS ====================

    @Test
    fun `service uses GitConfig`() {
        assertIs<StorageConfig.GitConfig>(service.config)
    }

    @Test
    fun `storage info has GIT type`() = runBlocking {
        val info = service.getStorageInfo()
        assertEquals(StorageType.GIT, info.type)
    }

    @Test
    fun `config branch is accessible`() {
        val gitConfig = service.config as StorageConfig.GitConfig
        assertEquals("main", gitConfig.branch)
    }

    @Test
    fun `config repositoryUrl is accessible`() {
        val gitConfig = service.config as StorageConfig.GitConfig
        assertEquals("https://github.com/example/repo.git", gitConfig.repositoryUrl)
    }

    @Test
    fun `config personalAccessToken is accessible`() {
        val gitConfig = service.config as StorageConfig.GitConfig
        assertEquals("ghp_testtoken123", gitConfig.personalAccessToken)
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `operations handle special characters in paths`() = runBlocking {
        val specialPaths = listOf(
            "/path with spaces/file.kt",
            "/path-with-dashes/file.kt",
            "/path_with_underscores/file.kt",
            "/path.with.dots/file.kt"
        )

        for (path in specialPaths) {
            val result = service.getFileInfo(path)
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `operations handle deep paths`() = runBlocking {
        val deepPath = "/src/main/kotlin/digital/vasic/yole/file.kt"
        val result = service.getFileInfo(deepPath)
        assertTrue(result.isSuccess)
        assertEquals("file.kt", result.getOrNull()?.name)
    }

    @Test
    fun `operations handle unicode paths`() = runBlocking {
        val result = service.getFileInfo("/docs/文档/README.md")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `operations handle git-specific paths`() = runBlocking {
        // Git repo paths
        val gitPaths = listOf(
            "/.gitignore",
            "/.github/workflows/ci.yml",
            "/README.md",
            "/src/main/resources/application.properties"
        )

        for (path in gitPaths) {
            val result = service.getFileInfo(path)
            assertTrue(result.isSuccess)
        }
    }

    // ==================== GIT WORKFLOW TESTS ====================

    @Test
    fun `file operations return git storageId`() = runBlocking {
        val result = service.getFileInfo("/README.md")
        assertEquals("git", result.getOrNull()?.storageId)
    }

    @Test
    fun `folder operations return git storageId`() = runBlocking {
        val result = service.createFolder("/new-folder")
        assertEquals("git", result.getOrNull()?.storageId)
    }

    @Test
    fun `move operation returns git storageId`() = runBlocking {
        val result = service.moveFile("/old/file.kt", "/new/file.kt")
        assertEquals("git", result.getOrNull()?.storageId)
    }

    // ==================== RESOURCE MANAGEMENT TESTS ====================

    @Test
    fun `multiple disconnect calls do not throw`() = runBlocking {
        service.disconnect()
        service.disconnect()
        service.disconnect()
        // Should not throw
        assertFalse(service.isOnline)
    }

    @Test
    fun `service state is consistent after disconnect`() = runBlocking {
        service.disconnect()
        assertFalse(service.isOnline)

        val info = service.getStorageInfo()
        assertFalse(info.isOnline)
    }

    // ==================== CONFIG VARIATIONS ====================

    @Test
    fun `service with empty repository URL uses default`() = runBlocking {
        val emptyConfig = StorageConfig.GitConfig(
            name = "empty-git",
            repositoryUrl = "",
            branch = "main",
            username = "user",
            personalAccessToken = "token",
            localCachePath = "/tmp/test-git-cache"
        )
        val svc = GitService(emptyConfig)
        // Should not throw
        assertFalse(svc.isOnline)
    }

    @Test
    fun `service with different branches works correctly`() = runBlocking {
        val branches = listOf("main", "master", "develop", "feature/test")
        for (branch in branches) {
            val cfg = StorageConfig.GitConfig(
                name = "test",
                repositoryUrl = "https://github.com/example/repo.git",
                branch = branch,
                username = "user",
                personalAccessToken = "token",
                localCachePath = "/tmp/test-git-cache"
            )
            val svc = GitService(cfg)
            assertEquals(branch, (svc.config as StorageConfig.GitConfig).branch)
        }
    }
}
