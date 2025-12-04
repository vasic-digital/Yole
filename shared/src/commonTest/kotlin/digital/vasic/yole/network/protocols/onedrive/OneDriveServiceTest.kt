package digital.vasic.yole.network.protocols.onedrive

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
 * Comprehensive test suite for OneDriveService network protocol implementation.
 * Tests Microsoft Graph API integration, authentication, and OneDrive functionality.
 */
class OneDriveServiceTest {
    
    private val oneDriveConfig = StorageConfig.OneDriveConfig(
        name = "test-onedrive",
        clientId = "test-client-id",
        clientSecret = "test-client-secret",
        accessToken = "test-access-token",
        refreshToken = "test-refresh-token",
        driveType = OneDriveDriveType.ME,
        driveId = "test-drive-id",
        rootFolderId = "root"
    )
    
    private lateinit var oneDriveService: OneDriveService
    
    @Test
    fun testOneDriveServiceInitialization() {
        oneDriveService = OneDriveService(oneDriveConfig)
        
        assertEquals("test-onedrive", oneDriveService.config.name)
        assertEquals("test-client-id", oneDriveService.config.clientId)
        assertEquals("test-client-secret", oneDriveService.config.clientSecret)
        assertEquals(OneDriveDriveType.ME, oneDriveService.config.driveType)
        assertEquals("test-drive-id", oneDriveService.config.driveId)
        assertEquals("root", oneDriveService.config.rootFolderId)
        assertEquals("/", oneDriveService.rootPath)
        assertFalse(oneDriveService.isOnline)
    }
    
    @Test
    fun testConnectSuccess() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.connect()
        
        assertTrue(result.isSuccess, "OneDrive connection should succeed")
    }
    
    @Test
    fun testDisconnectSuccess() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        oneDriveService.connect()
        val result = oneDriveService.disconnect()
        
        assertTrue(result.isSuccess, "OneDrive disconnection should succeed")
    }
    
    @Test
    fun testStorageInfo() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val storageInfo = oneDriveService.getStorageInfo()
        
        assertEquals("onedrive_test-onedrive", storageInfo.id)
        assertEquals("test-onedrive", storageInfo.name)
        assertEquals(StorageType.ONEDRIVE, storageInfo.type)
        assertEquals("https://onedrive.live.com/", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val operations = oneDriveService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val operations = oneDriveService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.deleteFile("/test.md")
        
        assertTrue(result.isFailure, "Delete should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.createFolder("/test-folder")
        
        assertTrue(result.isFailure, "Create folder should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isFailure, "Rename should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isFailure, "Move should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isFailure, "Copy should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.getFileInfo("/test.md")
        
        assertTrue(result.isFailure, "Get file info should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.getQuotaInfo()
        
        assertTrue(result.isFailure, "Get quota info should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testExistsWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.exists("/test.md")
        
        assertTrue(result.isFailure, "Exists check should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testOneDriveConfigurationValidation() {
        // Test with SharePoint site
        val sharePointConfig = oneDriveConfig.copy(
            driveType = OneDriveDriveType.SHAREPOINT,
            driveId = "test-site-drive-id"
        )
        oneDriveService = OneDriveService(sharePointConfig)
        
        assertEquals(OneDriveDriveType.SHAREPOINT, oneDriveService.config.driveType)
        assertEquals("test-site-drive-id", oneDriveService.config.driveId)
        
        // Test with minimal configuration
        val minimalConfig = oneDriveConfig.copy(
            refreshToken = null
        )
        oneDriveService = OneDriveService(minimalConfig)
        
        assertEquals("test-access-token", oneDriveService.config.accessToken)
        assertEquals(null, oneDriveService.config.refreshToken)
        
        // Test with business drive
        val businessConfig = oneDriveConfig.copy(driveType = OneDriveDriveType.BUSINESS)
        oneDriveService = OneDriveService(businessConfig)
        
        assertEquals(OneDriveDriveType.BUSINESS, oneDriveService.config.driveType)
    }
    
    @Test
    fun testGetParentPath() {
        oneDriveService = OneDriveService(oneDriveConfig)
        
        assertEquals("/", oneDriveService.getParentPath("/test.md"))
        assertEquals("/folder", oneDriveService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", oneDriveService.getParentPath("/folder/subfolder/test.md"))
        assertEquals(null, oneDriveService.getParentPath("/"))
        assertEquals(null, oneDriveService.getParentPath(""))
    }
    
    @Test
    fun testValidatePath() {
        oneDriveService = OneDriveService(oneDriveConfig)
        
        assertTrue(oneDriveService.validatePath("/test.md").isSuccess)
        assertTrue(oneDriveService.validatePath("/folder/test.md").isSuccess)
        assertTrue(oneDriveService.validatePath("/folder/subfolder/test.md").isSuccess)
        
        assertTrue(oneDriveService.validatePath("").isFailure)
        assertTrue(oneDriveService.validatePath("   ").isFailure)
    }
    
    @Test
    fun testSearchFilesWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetRecentChangesWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val since = Clock.System.now()
        val changes = oneDriveService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty when not connected")
    }
    
    @Test
    fun testSyncFileWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val operations = oneDriveService.syncFile("/test.md", false)
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.SYNC, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testSyncAllWhenNotConnected() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val operations = oneDriveService.syncAll(false)
        
        // Should return empty flow when not connected
        var operationCount = 0
        operations.collect { operation ->
            operationCount++
        }
        assertEquals(0, operationCount, "Sync all should return empty when not connected")
    }
    
    @Test
    fun testActiveOperationsFlow() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val activeOps = oneDriveService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testCacheOperations() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        
        val cacheEntries = oneDriveService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        val addToCacheResult = oneDriveService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        val removeFromCacheResult = oneDriveService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        val clearCacheResult = oneDriveService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testSyncStatusFlow() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val syncStatus = oneDriveService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testTestConnection() = runTest {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.testConnection()
        
        assertTrue(result.isSuccess, "Test connection should complete successfully")
        assertFalse(result.getOrNull() ?: true, "Connection should be false when not connected")
    }
    
    @Test
    fun testTokenRefreshScenario() = runTest {
        // Test with expired access token scenario
        val configWithExpiredToken = oneDriveConfig.copy(accessToken = "expired-token")
        oneDriveService = OneDriveService(configWithExpiredToken)
        
        val storageInfo = oneDriveService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, storageInfo.type)
        assertEquals("test-onedrive", storageInfo.name)
    }
    
    @Test
    fun testSharePointVsPersonalOneDrive() = runTest {
        // Test personal OneDrive
        val personalConfig = oneDriveConfig.copy(
            driveType = OneDriveDriveType.ME,
            driveId = "personal-drive-id"
        )
        val personalService = OneDriveService(personalConfig)
        val personalStorageInfo = personalService.getStorageInfo()
        
        assertEquals(StorageType.ONEDRIVE, personalStorageInfo.type)
        assertEquals("test-onedrive", personalStorageInfo.name)
        assertEquals("https://onedrive.live.com/", personalStorageInfo.location)
        
        // Test SharePoint Online
        val sharePointConfig = oneDriveConfig.copy(
            driveType = OneDriveDriveType.SHAREPOINT,
            driveId = "sharepoint-drive-id"
        )
        val sharePointService = OneDriveService(sharePointConfig)
        val sharePointStorageInfo = sharePointService.getStorageInfo()
        
        assertEquals(StorageType.ONEDRIVE, sharePointStorageInfo.type)
        assertEquals("test-onedrive", sharePointStorageInfo.name)
        assertEquals("https://onedrive.live.com/", sharePointStorageInfo.location)
    }
}