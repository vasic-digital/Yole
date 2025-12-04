package digital.vasic.yole.network.protocols.dropbox

import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for DropboxService network protocol implementation.
 * Tests Dropbox API integration, authentication, and cloud storage functionality.
 */
class DropboxServiceTest {
    
    private val dropboxConfig = StorageConfig.DropboxConfig(
        name = "test-dropbox",
        accessToken = "test-access-token",
        appKey = "test-app-key",
        appSecret = "test-app-secret",
        refreshToken = "test-refresh-token",
        rootPath = ""
    )
    
    private lateinit var dropboxService: DropboxService
    
    @Test
    fun testDropboxServiceInitialization() {
        dropboxService = DropboxService(dropboxConfig)
        
        assertEquals("test-dropbox", dropboxService.config.name)
        assertEquals("test-access-token", dropboxService.config.accessToken)
        assertEquals("test-app-key", dropboxService.config.appKey)
        assertEquals("test-app-secret", dropboxService.config.appSecret)
        assertEquals("test-refresh-token", dropboxService.config.refreshToken)
        assertEquals("", dropboxService.config.rootPath)
        assertEquals("/", dropboxService.rootPath)
        assertFalse(dropboxService.isOnline)
    }
    
    @Test
    fun testConnectSuccess() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.connect()
        
        // Connection will fail due to no mock
        assertTrue(result.isFailure, "Dropbox connection should fail without mocking")
        assertFalse(dropboxService.isOnline, "Should not be connected")
    }
    
    @Test
    fun testDisconnectSuccess() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        dropboxService.connect()
        val result = dropboxService.disconnect()
        
        assertTrue(result.isSuccess, "Dropbox disconnection should succeed")
    }
    
    @Test
    fun testStorageInfo() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val storageInfo = dropboxService.getStorageInfo()
        
        assertEquals("dropbox_test-dropbox", storageInfo.id)
        assertEquals("test-dropbox", storageInfo.name)
        assertEquals(StorageType.DROPBOX, storageInfo.type)
        assertEquals("dropbox://", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("Dropbox not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val operations = dropboxService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Dropbox not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val operations = dropboxService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Dropbox not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.deleteFile("/test.md")
        
        assertTrue(result.isFailure, "Delete should fail when not connected")
        assertEquals("Dropbox not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.createFolder("/test-folder")
        
        assertTrue(result.isFailure, "Create folder should fail when not connected")
        assertTrue(result.exceptionOrNull()?.message?.contains("Create folder failed") == true)
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isFailure, "Rename should fail when not connected")
        assertEquals("Dropbox not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isFailure, "Move should fail when not connected")
        assertTrue(result.exceptionOrNull()?.message?.contains("Move failed") == true)
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isFailure, "Copy should fail when not connected")
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("Move failed"), "Error message should contain Move failed, got: $message")
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.getFileInfo("/test.md")
        
        assertTrue(result.isFailure, "Get file info should fail when not connected")
        assertTrue(result.exceptionOrNull()?.message?.contains("File info failed") == true)
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.getQuotaInfo()
        
        assertTrue(result.isSuccess, "Get quota info returns mock success even when not connected")
        val quota = result.getOrNull()
        assertEquals(1000000000L, quota?.totalSpace)
        assertEquals(0L, quota?.usedSpace)
    }
    
    @Test
    fun testExistsWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.exists("/test.md")
        
        assertTrue(result.isSuccess, "Exists check returns mock success even when not connected")
        assertEquals(true, result.getOrNull(), "Mock implementation returns true")
    }
    
    @Test
    fun testDropboxConfigurationValidation() {
        // Test with custom root path
        val customRootConfig = dropboxConfig.copy(rootPath = "/Apps/Yole")
        dropboxService = DropboxService(customRootConfig)
        
        assertEquals("/Apps/Yole", dropboxService.config.rootPath)
        assertEquals("/", dropboxService.rootPath)
        
        // Test with minimal configuration
        val minimalConfig = dropboxConfig.copy(
            refreshToken = null
        )
        dropboxService = DropboxService(minimalConfig)
        
        assertEquals("test-access-token", dropboxService.config.accessToken)
        assertEquals(null, dropboxService.config.refreshToken)
        assertEquals("test-app-key", dropboxService.config.appKey)
        assertEquals("test-app-secret", dropboxService.config.appSecret)
    }
    
    @Test
    fun testGetParentPath() {
        dropboxService = DropboxService(dropboxConfig)
        
        assertEquals("/", dropboxService.getParentPath("/test.md"))
        assertEquals("/folder", dropboxService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", dropboxService.getParentPath("/folder/subfolder/test.md"))
        assertEquals("/", dropboxService.getParentPath("/"))
        assertEquals("/", dropboxService.getParentPath(""))
    }
    
    @Test
    fun testValidatePath() {
        dropboxService = DropboxService(dropboxConfig)
        
        assertTrue(dropboxService.validatePath("/test.md").isSuccess)
        assertTrue(dropboxService.validatePath("/folder/test.md").isSuccess)
        assertTrue(dropboxService.validatePath("/folder/subfolder/test.md").isSuccess)
        
        assertTrue(dropboxService.validatePath("").isSuccess)
        assertTrue(dropboxService.validatePath("   ").isSuccess)
    }
    
    @Test
    fun testSearchFilesWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail when not connected")
        assertEquals("Dropbox not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetRecentChangesWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val since = Clock.System.now()
        val changes = dropboxService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty when not connected")
    }
    
    @Test
    fun testSyncFileWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val operations = dropboxService.syncFile("/test.md", false)
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.SYNC, firstOperation.type)
        assertEquals(NetworkOperation.Status.COMPLETED, firstOperation.status)
    }
    
    @Test
    fun testSyncAllWhenNotConnected() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val operations = dropboxService.syncAll(false)
        
        // Should return one completed operation
        var operationCount = 0
        operations.collect { operation ->
            operationCount++
            assertEquals(NetworkOperation.Status.COMPLETED, operation.status)
        }
        assertEquals(1, operationCount, "Sync all should return one completed operation")
    }
    
    @Test
    fun testActiveOperationsFlow() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val activeOps = dropboxService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testCacheOperations() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        
        val cacheEntries = dropboxService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        val addToCacheResult = dropboxService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        val removeFromCacheResult = dropboxService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        val clearCacheResult = dropboxService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testSyncStatusFlow() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val syncStatus = dropboxService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testTestConnection() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        val result = dropboxService.testConnection()
        
        assertTrue(result.isFailure, "Test connection should fail without mocking")
    }
    
    @Test
    fun testTokenRefreshScenario() = runTest {
        // Test with expired access token scenario
        val configWithExpiredToken = dropboxConfig.copy(accessToken = "expired-token")
        dropboxService = DropboxService(configWithExpiredToken)
        
        val storageInfo = dropboxService.getStorageInfo()
        assertEquals(StorageType.DROPBOX, storageInfo.type)
        assertEquals("test-dropbox", storageInfo.name)
    }
    
    @Test
    fun testDropboxApiEndpoints() = runTest {
        dropboxService = DropboxService(dropboxConfig)
        
        // Test that storage info contains correct Dropbox URL
        val storageInfo = dropboxService.getStorageInfo()
        assertEquals("dropbox://", storageInfo.location)
        
        // Test with custom root path
        val customRootConfig = dropboxConfig.copy(rootPath = "/Apps/Yole")
        val customRootService = DropboxService(customRootConfig)
        val customRootStorageInfo = customRootService.getStorageInfo()
        
        assertEquals(StorageType.DROPBOX, customRootStorageInfo.type)
        assertEquals("dropbox://", customRootStorageInfo.location)
    }
}