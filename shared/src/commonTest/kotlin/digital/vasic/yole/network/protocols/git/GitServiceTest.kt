package digital.vasic.yole.network.protocols.git

import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocol.MockNetworkStorageService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for GitService network protocol implementation.
 * Tests all core functionality including file operations, sync, and error handling.
 */
class GitServiceTest {
    
    private val gitConfig = StorageConfig.GitConfig(
        name = "test-repo",
        repositoryUrl = "https://github.com/test/repo.git",
        branch = "main",
        username = "testuser",
        password = "test-token",
        localCachePath = "/tmp/test-repo",
        autoSync = true,
        connectionTimeout = 30000
    )
    
    private lateinit var gitService: GitService
    
    @Test
    fun testGitServiceInitialization() {
        gitService = GitService(gitConfig)
        
        assertEquals("test-repo", gitService.config.name)
        assertEquals("https://github.com/test/repo.git", gitService.config.repositoryUrl)
        assertEquals("main", gitService.config.branch)
        assertEquals("/", gitService.rootPath)
        assertFalse(gitService.isOnline)
    }
    
    @Test
    fun testConnectSuccess() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.connect()
        
        assertTrue(result.isSuccess, "Git connection should succeed")
    }
    
    @Test
    fun testDisconnectSuccess() = runTest {
        gitService = GitService(gitConfig)
        gitService.connect()
        val result = gitService.disconnect()
        
        assertTrue(result.isSuccess, "Git disconnection should succeed")
    }
    
    @Test
    fun testStorageInfo() = runTest {
        gitService = GitService(gitConfig)
        val storageInfo = gitService.getStorageInfo()
        
        assertEquals("git_test-repo", storageInfo.id)
        assertEquals("test-repo", storageInfo.name)
        assertEquals(StorageType.GIT, storageInfo.type)
        assertEquals("https://github.com/example/repo", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val operations = gitService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val operations = gitService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.deleteFile("/test.md")
        
        assertTrue(result.isFailure, "Delete should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.createFolder("/test-folder")
        
        assertTrue(result.isFailure, "Create folder should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isFailure, "Rename should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isFailure, "Move should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isFailure, "Copy should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.getFileInfo("/test.md")
        
        assertTrue(result.isFailure, "Get file info should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.getQuotaInfo()
        
        assertTrue(result.isFailure, "Get quota info should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testExistsWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.exists("/test.md")
        
        assertTrue(result.isFailure, "Exists check should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCancelOperation() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.cancelOperation(12345)
        
        assertTrue(result.isFailure, "Cancel operation should fail for non-existent operation")
    }
    
    @Test
    fun testPauseOperation() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.pauseOperation(12345)
        
        assertTrue(result.isFailure, "Pause operation should fail for non-existent operation")
    }
    
    @Test
    fun testResumeOperation() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.resumeOperation(12345)
        
        assertTrue(result.isFailure, "Resume operation should fail for non-existent operation")
    }
    
    @Test
    fun testGetParentPath() {
        gitService = GitService(gitConfig)
        
        assertEquals("/", gitService.getParentPath("/test.md"))
        assertEquals("/folder", gitService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", gitService.getParentPath("/folder/subfolder/test.md"))
        assertEquals(null, gitService.getParentPath("/"))
        assertEquals(null, gitService.getParentPath(""))
    }
    
    @Test
    fun testValidatePath() {
        gitService = GitService(gitConfig)
        
        assertTrue(gitService.validatePath("/test.md").isSuccess)
        assertTrue(gitService.validatePath("/folder/test.md").isSuccess)
        assertTrue(gitService.validatePath("/folder/subfolder/test.md").isSuccess)
        
        assertTrue(gitService.validatePath("").isFailure)
        assertTrue(gitService.validatePath("   ").isFailure)
    }
    
    @Test
    fun testSearchFilesWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetRecentChangesWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val since = Clock.System.now()
        val changes = gitService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty when not connected")
    }
    
    @Test
    fun testSyncFileWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val operations = gitService.syncFile("/test.md", false)
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.SYNC, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testSyncAllWhenNotConnected() = runTest {
        gitService = GitService(gitConfig)
        val operations = gitService.syncAll(false)
        
        // Should return empty flow when not connected
        var operationCount = 0
        operations.collect { operation ->
            operationCount++
        }
        assertEquals(0, operationCount, "Sync all should return empty when not connected")
    }
    
    @Test
    fun testActiveOperationsFlow() = runTest {
        gitService = GitService(gitConfig)
        val activeOps = gitService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testCacheOperations() = runTest {
        gitService = GitService(gitConfig)
        
        val cacheEntries = gitService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        val addToCacheResult = gitService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        val removeFromCacheResult = gitService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        val clearCacheResult = gitService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testSyncStatusFlow() = runTest {
        gitService = GitService(gitConfig)
        val syncStatus = gitService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testTestConnection() = runTest {
        gitService = GitService(gitConfig)
        val result = gitService.testConnection()
        
        assertTrue(result.isSuccess, "Test connection should complete successfully")
        assertFalse(result.getOrNull() ?: true, "Connection should be false when not connected")
    }
}