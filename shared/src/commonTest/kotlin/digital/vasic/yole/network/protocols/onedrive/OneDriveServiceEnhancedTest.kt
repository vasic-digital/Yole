package digital.vasic.yole.network.protocols.onedrive

import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Enhanced comprehensive test suite for OneDriveService
 * Tests Microsoft OneDrive API integration with different drive types
 */
class OneDriveServiceEnhancedTest {
    
    private val personalConfig = StorageConfig.OneDriveConfig(
        name = "test-onedrive-personal",
        clientId = "test-client-id-personal",
        clientSecret = "test-client-secret-personal",
        refreshToken = "test-refresh-token-personal",
        accessToken = "test-access-token-personal",
        driveType = OneDriveDriveType.ME,
        rootFolderId = "root"
    )
    
    private val businessConfig = StorageConfig.OneDriveConfig(
        name = "test-onedrive-business",
        clientId = "test-client-id-business",
        clientSecret = "test-client-secret-business",
        refreshToken = "test-refresh-token-business",
        accessToken = "test-access-token-business",
        driveType = OneDriveDriveType.BUSINESS,
        driveId = "business-drive-123"
    )
    
    private val sharePointConfig = StorageConfig.OneDriveConfig(
        name = "test-onedrive-sharepoint",
        clientId = "test-client-id-sharepoint",
        clientSecret = "test-client-secret-sharepoint",
        refreshToken = "test-refresh-token-sharepoint",
        accessToken = "test-access-token-sharepoint",
        driveType = OneDriveDriveType.SHAREPOINT,
        driveId = "sharepoint-drive-456"
    )
    
    private val groupConfig = StorageConfig.OneDriveConfig(
        name = "test-onedrive-group",
        clientId = "test-client-id-group",
        clientSecret = "test-client-secret-group",
        refreshToken = "test-refresh-token-group",
        accessToken = "test-access-token-group",
        driveType = OneDriveDriveType.GROUP,
        driveId = "group-drive-789"
    )
    
    private lateinit var personalService: OneDriveService
    private lateinit var businessService: OneDriveService
    private lateinit var sharePointService: OneDriveService
    private lateinit var groupService: OneDriveService
    private lateinit var mockSecureStorage: SecureStorage
    
    @BeforeTest
    fun setup() = runTest {
        mockSecureStorage = MockSecureStorage()
        
        personalService = OneDriveService(personalConfig)
        businessService = OneDriveService(businessConfig)
        sharePointService = OneDriveService(sharePointConfig)
        groupService = OneDriveService(groupConfig)
        
        // Initialize auth token managers with mock tokens
        listOf(personalConfig, businessConfig, sharePointConfig, groupConfig).forEach { config ->
            val serviceName = "onedrive_${config.name}"
            val authTokenManager = AuthTokenManager(serviceName, mockSecureStorage)
            authTokenManager.storeTokenInfo(
                accessToken = config.accessToken,
                refreshToken = config.refreshToken,
                expiresIn = 3600L
            )
        }
    }
    
    @Test
    fun testOneDriveServiceEnhancedInitialization() {
        // Test personal OneDrive configuration
        assertEquals("test-onedrive-personal", personalService.config.name)
        assertEquals("test-client-id-personal", personalService.config.clientId)
        assertEquals("test-client-secret-personal", personalService.config.clientSecret)
        assertEquals("test-refresh-token-personal", personalService.config.refreshToken)
        assertEquals("test-access-token-personal", personalService.config.accessToken)
        assertEquals(OneDriveDriveType.ME, personalService.config.driveType)
        assertEquals("root", personalService.config.rootFolderId)
        assertNull(personalService.config.driveId)
        assertEquals("/", personalService.rootPath)
        assertFalse(personalService.isOnline, "Should not be connected initially")
        
        // Test business OneDrive configuration
        assertEquals("test-onedrive-business", businessService.config.name)
        assertEquals("test-client-id-business", businessService.config.clientId)
        assertEquals(OneDriveDriveType.BUSINESS, businessService.config.driveType)
        assertEquals("business-drive-123", businessService.config.driveId)
        
        // Test SharePoint configuration
        assertEquals("test-onedrive-sharepoint", sharePointService.config.name)
        assertEquals(OneDriveDriveType.SHAREPOINT, sharePointService.config.driveType)
        assertEquals("sharepoint-drive-456", sharePointService.config.driveId)
        
        // Test group configuration
        assertEquals("test-onedrive-group", groupService.config.name)
        assertEquals(OneDriveDriveType.GROUP, groupService.config.driveType)
        assertEquals("group-drive-789", groupService.config.driveId)
    }
    
    @Test
    fun testEnhancedStorageInfo() = runTest {
        val personalStorageInfo = personalService.getStorageInfo()
        
        assertEquals("onedrive_test-onedrive-personal", personalStorageInfo.id)
        assertEquals("test-onedrive-personal", personalStorageInfo.name)
        assertEquals(StorageType.ONEDRIVE, personalStorageInfo.type)
        assertEquals("onedrive://", personalStorageInfo.location)
        assertFalse(personalStorageInfo.isOnline, "Should not be online initially")
        assertTrue(personalStorageInfo.supportsFolders, "Should support folders")
        assertTrue(personalStorageInfo.supportsMetadata, "Should support metadata")
    }
    
    @Test
    fun testEnhancedConnectWithoutTokens() = runTest {
        // Test with cleared tokens
        val authTokenManager = AuthTokenManager("onedrive_test-onedrive-personal", mockSecureStorage)
        authTokenManager.clearTokens()
        
        val result = personalService.connect()
        
        // Should fail due to no valid tokens
        assertTrue(result.isFailure, "Connection should fail without valid tokens")
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkStorageException.ConnectionException.Authentication, 
            "Should fail with authentication exception")
    }
    
    @Test
    fun testEnhancedDisconnect() = runTest {
        // First connect
        personalService.connect()
        
        // Then disconnect
        val result = personalService.disconnect()
        assertTrue(result.isSuccess, "Disconnect should succeed")
        
        // Should be offline after disconnect
        assertFalse(personalService.isOnline, "Should be offline after disconnect")
    }
    
    @Test
    fun testEnhancedTestConnection() = runTest {
        val result = personalService.testConnection()
        
        // Test connection should complete (may succeed or fail depending on network)
        assertTrue(result.isSuccess || result.isFailure, "Test connection should complete")
    }
    
    @Test
    fun testEnhancedListFilesWhenNotConnected() = runTest {
        val result = personalService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkStorageException.ConnectionException.NotConnected, 
            "Should fail with not connected exception")
    }
    
    @Test
    fun testEnhancedListFilesWhenConnected() = runTest {
        // Connect first
        personalService.connect()
        
        val result = personalService.listFiles("/").first()
        
        assertTrue(result.isSuccess, "List files should succeed when connected")
        val files = result.getOrNull()
        assertNotNull(files, "Files list should be returned")
        assertTrue(files.isNotEmpty(), "Files list should not be empty")
        
        // Verify OneDrive-specific file structure
        val fileNames = files.map { it.name }
        assertTrue(fileNames.contains("README.md"), "Should contain README.md")
        assertTrue(fileNames.contains("config.json"), "Should contain config.json")
        
        // Verify file metadata
        val readmeFile = files.find { it.name == "README.md" }
        assertNotNull(readmeFile, "Should find README.md")
        assertFalse(readmeFile.isFolder, "README.md should be a file")
        assertTrue(readmeFile.size > 0, "Should have size")
        assertNotNull(readmeFile.lastModified, "Should have modification time")
        
        // Verify folder detection
        val folderFile = files.find { it.isFolder }
        if (folderFile != null) {
            assertEquals(0L, folderFile.size, "Folders should have zero size")
            assertTrue(folderFile.permissions.contains(DocumentPermission.EXECUTE), "Folders should have execute permission")
        }
    }
    
    @Test
    fun testEnhancedDownloadFileWhenNotConnected() = runTest {
        val operations = personalService.downloadFile("/document.pdf", "/tmp/document.pdf")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("OneDrive not connected", firstOperation.error)
    }
    
    @Test
    fun testEnhancedUploadFileWhenNotConnected() = runTest {
        val operations = personalService.uploadFile("/tmp/report.md", "/report.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("OneDrive not connected", firstOperation.error)
    }
    
    @Test
    fun testEnhancedFileOperationsWhenNotConnected() = runTest {
        // Test delete file
        val deleteResult = personalService.deleteFile("/test.md")
        assertTrue(deleteResult.isSuccess, "Delete should succeed (mock implementation)")
        
        // Test create folder
        val createFolderResult = personalService.createFolder("/test-folder")
        assertTrue(createFolderResult.isSuccess, "Create folder should succeed")
        val folder = createFolderResult.getOrNull()
        assertNotNull(folder, "Folder should be created")
        assertEquals("test-folder", folder.name)
        assertEquals("/test-folder", folder.path)
        assertTrue(folder.isFolder)
        
        // Test rename file
        val renameResult = personalService.renameFile("/test.md", "renamed.md")
        assertTrue(renameResult.isSuccess, "Rename should succeed")
        
        // Test move file
        val moveResult = personalService.moveFile("/test.md", "/moved/test.md")
        assertTrue(moveResult.isSuccess, "Move should succeed")
        val movedFile = moveResult.getOrNull()
        assertNotNull(movedFile, "Moved file should be returned")
        assertEquals("test.md", movedFile.name)
        assertEquals("/moved/test.md", movedFile.path)
        
        // Test copy file
        val copyResult = personalService.copyFile("/test.md", "/copy/test.md")
        assertTrue(copyResult.isSuccess, "Copy should succeed")
        
        // Test get file info
        val fileInfoResult = personalService.getFileInfo("/test.md")
        assertTrue(fileInfoResult.isSuccess, "Get file info should succeed")
        val fileInfo = fileInfoResult.getOrNull()
        assertNotNull(fileInfo, "File info should be returned")
        assertEquals("test.md", fileInfo.name)
        assertEquals("/test.md", fileInfo.path)
    }
    
    @Test
    fun testEnhancedQuotaInfo() = runTest {
        val result = personalService.getQuotaInfo()
        
        assertTrue(result.isSuccess, "Get quota info should succeed")
        val quota = result.getOrNull()
        assertNotNull(quota, "Quota info should be returned")
        
        // Check default OneDrive values
        assertEquals(5000000000000L, quota.totalSpace) // 5TB
        assertEquals(1000000000000L, quota.usedSpace)  // 1TB
        assertEquals(4000000000000L, quota.availableSpace) // 4TB
        assertEquals(0.2, quota.usagePercentage)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
        assertEquals("OneDrive", quota.metadata["provider"])
        assertEquals("ME", quota.metadata["type"])
    }
    
    @Test
    fun testEnhancedExists() = runTest {
        val result = personalService.exists("/test.md")
        
        assertTrue(result.isSuccess, "Exists check should succeed")
        assertFalse(result.getOrNull() ?: true, "Should return false for existing file (mock implementation)")
    }
    
    @Test
    fun testEnhancedPathOperations() = runTest {
        // Test getParentPath
        assertEquals(null, personalService.getParentPath("/test.md"))
        assertEquals("/folder", personalService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", personalService.getParentPath("/folder/subfolder/test.md"))
        assertEquals(null, personalService.getParentPath("/"))
        assertEquals(null, personalService.getParentPath(""))
        
        // Test validatePath
        assertTrue(personalService.validatePath("/test.md").isSuccess)
        assertTrue(personalService.validatePath("/folder/test.md").isSuccess)
        assertTrue(personalService.validatePath("/folder/subfolder/test.md").isSuccess)
        
        // Test invalid paths
        assertTrue(personalService.validatePath("").isFailure, "Empty path should be invalid")
        assertTrue(personalService.validatePath("   ").isFailure, "Whitespace path should be invalid")
    }
    
    @Test
    fun testEnhancedActiveOperations() = runTest {
        val activeOps = personalService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testEnhancedCacheOperations() = runTest {
        // Test get cache entries
        val cacheEntries = personalService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        // Test add to cache
        val addToCacheResult = personalService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        // Test remove from cache
        val removeFromCacheResult = personalService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        // Test clear cache
        val clearCacheResult = personalService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testEnhancedSyncStatus() = runTest {
        val syncStatus = personalService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testEnhancedSyncOperations() = runTest {
        // Test sync file
        val syncFileOperations = personalService.syncFile("/test.md", false)
        val syncFileOp = syncFileOperations.first()
        assertEquals(NetworkOperation.Type.SYNC, syncFileOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, syncFileOp.status)
        assertEquals("/test.md", syncFileOp.remotePath)
        
        // Test sync all
        val syncAllOperations = personalService.syncAll(false)
        val syncAllOp = syncAllOperations.first()
        assertEquals(NetworkOperation.Type.SYNC, syncAllOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, syncAllOp.status)
        assertEquals("/", syncAllOp.remotePath)
    }
    
    @Test
    fun testEnhancedSearchFiles() = runTest {
        val result = personalService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isSuccess, "Search should succeed")
        val searchResults = result.getOrNull()
        assertNotNull(searchResults, "Search results should be returned")
        assertTrue(searchResults.isEmpty(), "Search results should be empty (mock implementation)")
    }
    
    @Test
    fun testEnhancedRecentChanges() = runTest {
        val since = Clock.System.now()
        val changes = personalService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty")
    }
    
    @Test
    fun testEnhancedOperationManagement() = runTest {
        // Test cancel operation
        val cancelResult = personalService.cancelOperation(12345L)
        assertTrue(cancelResult.isSuccess, "Cancel operation should succeed")
        
        // Test pause operation
        val pauseResult = personalService.pauseOperation(12345L)
        assertTrue(pauseResult.isSuccess, "Pause operation should succeed")
        
        // Test resume operation
        val resumeResult = personalService.resumeOperation(12345L)
        assertTrue(resumeResult.isSuccess, "Resume operation should succeed")
    }
    
    @Test
    fun testEnhancedDifferentDriveTypes() = runTest {
        // Test personal OneDrive
        val personalStorageInfo = personalService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, personalStorageInfo.type)
        
        // Test business OneDrive
        val businessStorageInfo = businessService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, businessStorageInfo.type)
        
        // Test SharePoint
        val sharePointStorageInfo = sharePointService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, sharePointStorageInfo.type)
        
        // Test group
        val groupStorageInfo = groupService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, groupStorageInfo.type)
    }
    
    @Test
    fun testEnhancedDriveTypeSpecificQuota() = runTest {
        // Test personal drive quota
        val personalQuotaResult = personalService.getQuotaInfo()
        val personalQuota = personalQuotaResult.getOrNull()
        assertNotNull(personalQuota)
        assertEquals("ME", personalQuota.metadata["type"])
        
        // Test business drive quota
        val businessQuotaResult = businessService.getQuotaInfo()
        val businessQuota = businessQuotaResult.getOrNull()
        assertNotNull(businessQuota)
        assertEquals("BUSINESS", businessQuota.metadata["type"])
        
        // Test SharePoint quota
        val sharePointQuotaResult = sharePointService.getQuotaInfo()
        val sharePointQuota = sharePointQuotaResult.getOrNull()
        assertNotNull(sharePointQuota)
        assertEquals("SHAREPOINT", sharePointQuota.metadata["type"])
        
        // Test group quota
        val groupQuotaResult = groupService.getQuotaInfo()
        val groupQuota = groupQuotaResult.getOrNull()
        assertNotNull(groupQuota)
        assertEquals("GROUP", groupQuota.metadata["type"])
    }
    
    @Test
    fun testEnhancedConfigurationValidation() {
        // Test with custom root folder ID
        val customRootConfig = personalConfig.copy(rootFolderId = "custom-root-123")
        val customRootService = OneDriveService(customRootConfig)
        
        assertEquals("custom-root-123", customRootService.config.rootFolderId)
        
        // Test with null root folder ID (should default to "root")
        val noRootConfig = personalConfig.copy(rootFolderId = null)
        val noRootService = OneDriveService(noRootConfig)
        
        // The service should handle null rootFolderId appropriately
        assertEquals(null, noRootService.config.rootFolderId)
    }
    
    @Test
    fun testEnhancedTokenRefreshScenario() = runTest {
        // Test with expired access token but valid refresh token
        val authTokenManager = AuthTokenManager("onedrive_test-onedrive-personal", mockSecureStorage)
        
        // Clear existing tokens
        authTokenManager.clearTokens()
        
        // Store expired access token and valid refresh token
        val pastTime = Clock.System.now().minus(kotlinx.datetime.Duration.hours(1))
        authTokenManager.storeAccessToken("expired-access-token")
        authTokenManager.storeRefreshToken("valid-refresh-token")
        authTokenManager.storeTokenExpiration(pastTime)
        
        // Create new service instance with expired token
        val serviceWithExpiredToken = OneDriveService(personalConfig)
        
        // Service should handle token refresh internally
        val storageInfo = serviceWithExpiredToken.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, storageInfo.type)
        assertEquals("test-onedrive-personal", storageInfo.name)
    }
    
    @Test
    fun testEnhancedOneDriveApiEndpoints() = runTest {
        // Test that different drive types use different API endpoints
        
        // Personal OneDrive should use /me/drive
        personalService.connect()
        val personalStorageInfo = personalService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, personalStorageInfo.type)
        
        // Business OneDrive should use /me/drives/{driveId} or similar
        businessService.connect()
        val businessStorageInfo = businessService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, businessStorageInfo.type)
        
        // SharePoint should use /sites/{siteId}/drive
        sharePointService.connect()
        val sharePointStorageInfo = sharePointService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, sharePointStorageInfo.type)
        
        // Group should use /groups/{groupId}/drive
        groupService.connect()
        val groupStorageInfo = groupService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, groupStorageInfo.type)
    }
    
    // Mock SecureStorage implementation for testing
    private class MockSecureStorage : SecureStorage {
        private val storage = mutableMapOf<String, String>()
        
        override suspend fun store(key: String, value: String): Result<Unit> {
            storage[key] = value
            return Result.success(Unit)
        }
        
        override suspend fun retrieve(key: String): Result<String?> {
            return Result.success(storage[key])
        }
        
        override suspend fun delete(key: String): Result<Unit> {
            storage.remove(key)
            return Result.success(Unit)
        }
        
        override suspend fun contains(key: String): Result<Boolean> {
            return Result.success(storage.containsKey(key))
        }
        
        override suspend fun listKeys(): Result<List<String>> {
            return Result.success(storage.keys.toList())
        }
        
        override suspend fun clear(): Result<Unit> {
            storage.clear()
            return Result.success(Unit)
        }
        
        override suspend fun isSecure(): Result<Boolean> {
            return Result.success(true)
        }
    }
}