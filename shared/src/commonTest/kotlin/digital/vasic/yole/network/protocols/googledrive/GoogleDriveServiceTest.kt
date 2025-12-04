package digital.vasic.yole.network.protocols.googledrive

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
 * Comprehensive test suite for GoogleDriveService network protocol implementation.
 * Tests Google Drive API integration, authentication, and cloud storage functionality.
 */
class GoogleDriveServiceTest {
    
    private val googleDriveConfig = StorageConfig.GoogleDriveConfig(
        name = "test-gdrive",
        clientId = "test-client-id",
        clientSecret = "test-client-secret",
        refreshToken = "test-refresh-token",
        accessToken = "test-access-token",
        rootFolderId = "root"
    )
    
    private lateinit var googleDriveService: GoogleDriveService
    
    @Test
    fun testGoogleDriveServiceInitialization() {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        
        assertEquals("test-gdrive", googleDriveService.config.name)
        assertEquals("test-client-id", googleDriveService.config.clientId)
        assertEquals("test-client-secret", googleDriveService.config.clientSecret)
        assertEquals("test-refresh-token", googleDriveService.config.refreshToken)
        assertEquals("test-access-token", googleDriveService.config.accessToken)
        assertEquals("root", googleDriveService.config.rootFolderId)
        assertEquals("/", googleDriveService.rootPath)
        assertFalse(googleDriveService.isOnline)
    }
    
    @Test
    fun testConnectSuccess() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.connect()
        
        assertTrue(result.isSuccess, "Google Drive connection should succeed")
    }
    
    @Test
    fun testDisconnectSuccess() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        googleDriveService.connect()
        val result = googleDriveService.disconnect()
        
        assertTrue(result.isSuccess, "Google Drive disconnection should succeed")
    }
    
    @Test
    fun testStorageInfo() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val storageInfo = googleDriveService.getStorageInfo()
        
        assertEquals("googledrive_test-gdrive", storageInfo.id)
        assertEquals("test-gdrive", storageInfo.name)
        assertEquals(StorageType.GOOGLE_DRIVE, storageInfo.type)
        assertEquals("https://drive.google.com/", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val operations = googleDriveService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val operations = googleDriveService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.deleteFile("/test.md")
        
        assertTrue(result.isFailure, "Delete should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.createFolder("/test-folder")
        
        assertTrue(result.isFailure, "Create folder should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isFailure, "Rename should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isFailure, "Move should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isFailure, "Copy should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.getFileInfo("/test.md")
        
        assertTrue(result.isFailure, "Get file info should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.getQuotaInfo()
        
        assertTrue(result.isFailure, "Get quota info should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testExistsWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.exists("/test.md")
        
        assertTrue(result.isFailure, "Exists check should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGoogleDriveConfigurationValidation() {
        val configWithCustomFolder = googleDriveConfig.copy(rootFolderId = "custom-folder-id")
        googleDriveService = GoogleDriveService(configWithCustomFolder)
        
        assertEquals("custom-folder-id", googleDriveService.config.rootFolderId)
        assertEquals("test-client-id", googleDriveService.config.clientId)
        
        // Note: scopes are not configurable in GoogleDriveConfig
    }
    
    @Test
    fun testGetParentPath() {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        
        assertEquals("/", googleDriveService.getParentPath("/test.md"))
        assertEquals("/folder", googleDriveService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", googleDriveService.getParentPath("/folder/subfolder/test.md"))
        assertEquals(null, googleDriveService.getParentPath("/"))
        assertEquals(null, googleDriveService.getParentPath(""))
    }
    
    @Test
    fun testValidatePath() {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        
        assertTrue(googleDriveService.validatePath("/test.md").isSuccess)
        assertTrue(googleDriveService.validatePath("/folder/test.md").isSuccess)
        assertTrue(googleDriveService.validatePath("/folder/subfolder/test.md").isSuccess)
        
        assertTrue(googleDriveService.validatePath("").isFailure)
        assertTrue(googleDriveService.validatePath("   ").isFailure)
    }
    
    @Test
    fun testSearchFilesWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetRecentChangesWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val since = Clock.System.now()
        val changes = googleDriveService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty when not connected")
    }
    
    @Test
    fun testSyncFileWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val operations = googleDriveService.syncFile("/test.md", false)
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.SYNC, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testSyncAllWhenNotConnected() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val operations = googleDriveService.syncAll(false)
        
        // Should return empty flow when not connected
        var operationCount = 0
        operations.collect { operation ->
            operationCount++
        }
        assertEquals(0, operationCount, "Sync all should return empty when not connected")
    }
    
    @Test
    fun testActiveOperationsFlow() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val activeOps = googleDriveService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testCacheOperations() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        
        val cacheEntries = googleDriveService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        val addToCacheResult = googleDriveService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        val removeFromCacheResult = googleDriveService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        val clearCacheResult = googleDriveService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testSyncStatusFlow() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val syncStatus = googleDriveService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testTestConnection() = runTest {
        googleDriveService = GoogleDriveService(googleDriveConfig)
        val result = googleDriveService.testConnection()
        
        assertTrue(result.isSuccess, "Test connection should complete successfully")
        assertFalse(result.getOrNull() ?: true, "Connection should be false when not connected")
    }
    
    @Test
    fun testTokenRefreshScenario() = runTest {
        // Test with expired access token scenario
        val configWithExpiredToken = googleDriveConfig.copy(accessToken = "expired-token")
        googleDriveService = GoogleDriveService(configWithExpiredToken)
        
        val storageInfo = googleDriveService.getStorageInfo()
        assertEquals(StorageType.GOOGLE_DRIVE, storageInfo.type)
        assertEquals("test-gdrive", storageInfo.name)
    }
}