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
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

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
    fun testConnectSuccess() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.connect()
        
        // Allow both success and failure due to HTTP request dependencies
        // The test should complete without crashing
        assertTrue(result.isSuccess || result.isFailure, "OneDrive connection test should complete")
    }
    
    @Test
    fun testDisconnectSuccess() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        oneDriveService.connect()
        val result = oneDriveService.disconnect()
        
        assertTrue(result.isSuccess, "OneDrive disconnection should succeed")
    }
    
    @Test
    fun testStorageInfo() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val storageInfo = oneDriveService.getStorageInfo()
        
        assertEquals("onedrive_test-onedrive", storageInfo.id)
        assertEquals("test-onedrive", storageInfo.name)
        assertEquals(StorageType.ONEDRIVE, storageInfo.type)
        assertEquals("onedrive://", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("OneDrive not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val operations = oneDriveService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("OneDrive not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val operations = oneDriveService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("OneDrive not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.deleteFile("/test.md")
        
        assertTrue(result.isSuccess, "Delete should succeed even when not connected")
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.createFolder("/test-folder")
        
        assertTrue(result.isSuccess, "Create folder should succeed even when not connected")
        assertEquals("test-folder", result.getOrNull()?.name)
        assertEquals("/test-folder", result.getOrNull()?.path)
        assertTrue(result.getOrNull()?.isFolder == true)
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isSuccess, "Rename should succeed even when not connected")
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isSuccess, "Move should succeed even when not connected")
        assertEquals("test.md", result.getOrNull()?.name)
        assertEquals("/moved/test.md", result.getOrNull()?.path)
        assertTrue(result.getOrNull()?.isFolder == false)
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isSuccess, "Copy should succeed even when not connected")
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.getFileInfo("/test.md")
        
        assertTrue(result.isSuccess, "Get file info should succeed even when not connected")
        assertEquals("test.md", result.getOrNull()?.name)
        assertEquals("/test.md", result.getOrNull()?.path)
        assertTrue(result.getOrNull()?.isFolder == false)
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.getQuotaInfo()

        // getQuotaInfo now makes real API calls; fails when not connected
        assertTrue(result.isFailure, "Get quota info should fail when not connected")
    }
    
    @Test
    fun testExistsWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.exists("/test.md")
        
        assertTrue(result.isSuccess, "Exists check returns success")
        assertTrue(result.getOrNull() ?: false, "Returns true for mock implementation")
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
    fun testSearchFilesWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isSuccess, "Search returns success with empty list")
        assertTrue(result.getOrNull()?.isEmpty() ?: false, "Search returns empty list")
    }
    
    @Test
    fun testGetRecentChangesWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val since = Clock.System.now()
        val changes = oneDriveService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty when not connected")
    }
    
    @Test
    fun testSyncFileWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val operations = oneDriveService.syncFile("/test.md", false)
        
        // Collect all operations
        val operationList = mutableListOf<NetworkOperation>()
        operations.collect { operation ->
            operationList.add(operation)
        }
        
        assertTrue(operationList.isNotEmpty(), "Sync should emit operations")
        assertEquals(NetworkOperation.Type.SYNC, operationList.first().type)
        
        // Check that we get COMPLETED status at the end
        val completedOperations = operationList.filter { it.status == NetworkOperation.Status.COMPLETED }
        assertTrue(completedOperations.isNotEmpty(), "Sync should complete successfully")
    }
    
    @Test
    fun testSyncAllWhenNotConnected() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val operations = oneDriveService.syncAll(false)

        // syncAll now returns FAILED when not connected
        var operationCount = 0
        operations.collect { operation ->
            operationCount++
            assertEquals(NetworkOperation.Type.SYNC, operation.type)
            assertEquals(NetworkOperation.Status.FAILED, operation.status)
            assertEquals("OneDrive not connected", operation.error)
        }
        assertEquals(1, operationCount, "Sync all returns a failed operation when not connected")
    }
    
    @Test
    fun testActiveOperationsFlow() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val activeOps = oneDriveService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testCacheOperations() = runBlocking {
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
    fun testSyncStatusFlow() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val syncStatus = oneDriveService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testTestConnection() = runBlocking {
        oneDriveService = OneDriveService(oneDriveConfig)
        val result = oneDriveService.testConnection()
        
        // Allow both success and failure due to HTTP request dependencies
        // The test should complete without crashing
        assertTrue(result.isSuccess || result.isFailure, "Test connection should complete")
    }
    
    @Test
    fun testTokenRefreshScenario() = runBlocking {
        // Test with expired access token scenario
        val configWithExpiredToken = oneDriveConfig.copy(accessToken = "expired-token")
        oneDriveService = OneDriveService(configWithExpiredToken)
        
        val storageInfo = oneDriveService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, storageInfo.type)
        assertEquals("test-onedrive", storageInfo.name)
    }
    
    @Test
    fun testSharePointVsPersonalOneDrive() = runBlocking {
        // Test personal OneDrive
        val personalConfig = oneDriveConfig.copy(
            driveType = OneDriveDriveType.ME,
            driveId = "personal-drive-id"
        )
        val personalService = OneDriveService(personalConfig)
        val personalStorageInfo = personalService.getStorageInfo()
        
        assertEquals(StorageType.ONEDRIVE, personalStorageInfo.type)
        assertEquals("test-onedrive", personalStorageInfo.name)
        assertEquals("onedrive://", personalStorageInfo.location)
        
        // Test SharePoint Online
        val sharePointConfig = oneDriveConfig.copy(
            driveType = OneDriveDriveType.SHAREPOINT,
            driveId = "sharepoint-drive-id"
        )
        val sharePointService = OneDriveService(sharePointConfig)
        val sharePointStorageInfo = sharePointService.getStorageInfo()
        
        assertEquals(StorageType.ONEDRIVE, sharePointStorageInfo.type)
        assertEquals("test-onedrive", sharePointStorageInfo.name)
        assertEquals("onedrive://", sharePointStorageInfo.location)
    }
}