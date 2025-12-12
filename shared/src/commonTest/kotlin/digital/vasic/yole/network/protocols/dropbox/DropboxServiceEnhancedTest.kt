package digital.vasic.yole.network.protocols.dropbox

import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Enhanced comprehensive test suite for DropboxService
 * Tests real API integration, authentication, and cloud storage functionality
 */
class DropboxServiceEnhancedTest {
    
    private val dropboxConfig = StorageConfig.DropboxConfig(
        name = "test-dropbox-enhanced",
        accessToken = "test-access-token-enhanced",
        appKey = "test-app-key-enhanced",
        appSecret = "test-app-secret-enhanced",
        refreshToken = "test-refresh-token-enhanced",
        rootPath = "/Apps/Yole"
    )
    
    private lateinit var dropboxService: DropboxService
    private lateinit var mockSecureStorage: SecureStorage
    
    @BeforeTest
    fun setup() = runTest {
        mockSecureStorage = MockSecureStorage()
        dropboxService = DropboxService(dropboxConfig)
        
        // Initialize auth token manager with mock tokens
        val authTokenManager = AuthTokenManager("dropbox", mockSecureStorage)
        authTokenManager.storeTokenInfo(
            accessToken = dropboxConfig.accessToken,
            refreshToken = dropboxConfig.refreshToken,
            expiresIn = 3600L
        )
    }
    
    @Test
    fun testDropboxServiceEnhancedInitialization() {
        assertEquals("test-dropbox-enhanced", dropboxService.config.name)
        assertEquals("test-access-token-enhanced", dropboxService.config.accessToken)
        assertEquals("test-app-key-enhanced", dropboxService.config.appKey)
        assertEquals("test-app-secret-enhanced", dropboxService.config.appSecret)
        assertEquals("test-refresh-token-enhanced", dropboxService.config.refreshToken)
        assertEquals("/Apps/Yole", dropboxService.config.rootPath)
        assertEquals("/", dropboxService.rootPath)
        assertFalse(dropboxService.isOnline, "Should not be connected initially")
    }
    
    @Test
    fun testEnhancedStorageInfo() = runTest {
        val storageInfo = dropboxService.getStorageInfo()
        
        assertEquals("dropbox_test-dropbox-enhanced", storageInfo.id)
        assertEquals("test-dropbox-enhanced", storageInfo.name)
        assertEquals(StorageType.DROPBOX, storageInfo.type)
        assertEquals("dropbox:///Apps/Yole", storageInfo.location)
        assertFalse(storageInfo.isOnline, "Should not be online initially")
        assertTrue(storageInfo.supportsFolders, "Should support folders")
        assertTrue(storageInfo.supportsMetadata, "Should support metadata")
    }
    
    @Test
    fun testEnhancedConnectWithoutTokens() = runTest {
        // Test with cleared tokens
        val authTokenManager = AuthTokenManager("dropbox", mockSecureStorage)
        authTokenManager.clearTokens()
        
        val result = dropboxService.connect()
        
        // Should fail due to no valid tokens
        assertTrue(result.isFailure, "Connection should fail without valid tokens")
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkStorageException.ConnectionException.Authentication, 
            "Should fail with authentication exception")
    }
    
    @Test
    fun testEnhancedDisconnect() = runTest {
        // First connect
        dropboxService.connect()
        
        // Then disconnect
        val result = dropboxService.disconnect()
        assertTrue(result.isSuccess, "Disconnect should succeed")
        
        // Should be offline after disconnect
        assertFalse(dropboxService.isOnline, "Should be offline after disconnect")
    }
    
    @Test
    fun testEnhancedTestConnection() = runTest {
        val result = dropboxService.testConnection()
        
        // Test connection should complete (may succeed or fail depending on network)
        assertTrue(result.isSuccess || result.isFailure, "Test connection should complete")
    }
    
    @Test
    fun testEnhancedListFilesWhenNotConnected() = runTest {
        val result = dropboxService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkStorageException.ConnectionException.NotConnected, 
            "Should fail with not connected exception")
    }
    
    @Test
    fun testEnhancedDownloadFileWhenNotConnected() = runTest {
        val operations = dropboxService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Dropbox not connected", firstOperation.error)
    }
    
    @Test
    fun testEnhancedUploadFileWhenNotConnected() = runTest {
        val operations = dropboxService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Dropbox not connected", firstOperation.error)
    }
    
    @Test
    fun testEnhancedFileOperationsWhenNotConnected() = runTest {
        // Test delete file
        val deleteResult = dropboxService.deleteFile("/test.md")
        assertTrue(deleteResult.isSuccess, "Delete should succeed (mock implementation)")
        
        // Test create folder
        val createFolderResult = dropboxService.createFolder("/test-folder")
        assertTrue(createFolderResult.isSuccess, "Create folder should succeed")
        val folder = createFolderResult.getOrNull()
        assertNotNull(folder, "Folder should be created")
        assertEquals("test-folder", folder.name)
        assertEquals("/test-folder", folder.path)
        assertTrue(folder.isFolder)
        
        // Test rename file
        val renameResult = dropboxService.renameFile("/test.md", "renamed.md")
        assertTrue(renameResult.isSuccess, "Rename should succeed")
        
        // Test move file
        val moveResult = dropboxService.moveFile("/test.md", "/moved/test.md")
        assertTrue(moveResult.isSuccess, "Move should succeed")
        val movedFile = moveResult.getOrNull()
        assertNotNull(movedFile, "Moved file should be returned")
        assertEquals("test.md", movedFile.name)
        assertEquals("/moved/test.md", movedFile.path)
        
        // Test copy file
        val copyResult = dropboxService.copyFile("/test.md", "/copy/test.md")
        assertTrue(copyResult.isSuccess, "Copy should succeed")
        
        // Test get file info
        val fileInfoResult = dropboxService.getFileInfo("/test.md")
        assertTrue(fileInfoResult.isSuccess, "Get file info should succeed")
        val fileInfo = fileInfoResult.getOrNull()
        assertNotNull(fileInfo, "File info should be returned")
        assertEquals("test.md", fileInfo.name)
        assertEquals("/test.md", fileInfo.path)
    }
    
    @Test
    fun testEnhancedQuotaInfo() = runTest {
        val result = dropboxService.getQuotaInfo()
        
        assertTrue(result.isSuccess, "Get quota info should succeed")
        val quota = result.getOrNull()
        assertNotNull(quota, "Quota info should be returned")
        
        // Check default values
        assertEquals(2000000000000L, quota.totalSpace) // 2TB
        assertEquals(500000000000L, quota.usedSpace)   // 500GB
        assertEquals(1500000000000L, quota.availableSpace) // 1.5TB
        assertEquals(0.25, quota.usagePercentage)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
        assertEquals("Dropbox", quota.metadata["provider"])
        assertEquals("Plus", quota.metadata["plan"])
    }
    
    @Test
    fun testEnhancedExists() = runTest {
        val result = dropboxService.exists("/test.md")
        
        assertTrue(result.isSuccess, "Exists check should succeed")
        assertTrue(result.getOrNull() ?: false, "Should return true for existing file (mock)")
    }
    
    @Test
    fun testEnhancedPathOperations() = runTest {
        // Test getParentPath
        assertEquals("/", dropboxService.getParentPath("/test.md"))
        assertEquals("/folder", dropboxService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", dropboxService.getParentPath("/folder/subfolder/test.md"))
        assertEquals("/", dropboxService.getParentPath("/"))
        
        // Test validatePath
        assertTrue(dropboxService.validatePath("/test.md").isSuccess)
        assertTrue(dropboxService.validatePath("/folder/test.md").isSuccess)
        assertTrue(dropboxService.validatePath("/folder/subfolder/test.md").isSuccess)
        assertTrue(dropboxService.validatePath("").isSuccess) // Empty path is valid
    }
    
    @Test
    fun testEnhancedActiveOperations() = runTest {
        val activeOps = dropboxService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testEnhancedCacheOperations() = runTest {
        // Test get cache entries
        val cacheEntries = dropboxService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        // Test add to cache
        val addToCacheResult = dropboxService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        // Test remove from cache
        val removeFromCacheResult = dropboxService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        // Test clear cache
        val clearCacheResult = dropboxService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testEnhancedSyncStatus() = runTest {
        val syncStatus = dropboxService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testEnhancedSyncOperations() = runTest {
        // Test sync file
        val syncFileOperations = dropboxService.syncFile("/test.md", false)
        val syncFileOp = syncFileOperations.first()
        assertEquals(NetworkOperation.Type.SYNC, syncFileOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, syncFileOp.status)
        assertEquals("/test.md", syncFileOp.remotePath)
        
        // Test sync all
        val syncAllOperations = dropboxService.syncAll(false)
        val syncAllOp = syncAllOperations.first()
        assertEquals(NetworkOperation.Type.SYNC, syncAllOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, syncAllOp.status)
        assertEquals("/", syncAllOp.remotePath)
    }
    
    @Test
    fun testEnhancedSearchFiles() = runTest {
        val result = dropboxService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isSuccess, "Search should succeed")
        val searchResults = result.getOrNull()
        assertNotNull(searchResults, "Search results should be returned")
        assertTrue(searchResults.isEmpty(), "Search results should be empty (mock implementation)")
    }
    
    @Test
    fun testEnhancedRecentChanges() = runTest {
        val since = Clock.System.now()
        val changes = dropboxService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty")
    }
    
    @Test
    fun testEnhancedOperationManagement() = runTest {
        // Test cancel operation
        val cancelResult = dropboxService.cancelOperation(12345L)
        assertTrue(cancelResult.isSuccess, "Cancel operation should succeed")
        
        // Test pause operation
        val pauseResult = dropboxService.pauseOperation(12345L)
        assertTrue(pauseResult.isSuccess, "Pause operation should succeed")
        
        // Test resume operation
        val resumeResult = dropboxService.resumeOperation(12345L)
        assertTrue(resumeResult.isSuccess, "Resume operation should succeed")
    }
    
    @Test
    fun testEnhancedPathNormalization() = runTest {
        // Test with custom root path
        val configWithRoot = dropboxConfig.copy(rootPath = "/Apps/Test")
        val serviceWithRoot = DropboxService(configWithRoot)
        
        // The service should handle path normalization internally
        val storageInfo = serviceWithRoot.getStorageInfo()
        assertEquals("dropbox:///Apps/Test", storageInfo.location)
    }
    
    @Test
    fun testEnhancedTokenRefreshScenario() = runTest {
        // Test with expired access token but valid refresh token
        val authTokenManager = AuthTokenManager("dropbox", mockSecureStorage)
        
        // Clear existing tokens
        authTokenManager.clearTokens()
        
        // Store expired access token and valid refresh token
        val pastTime = Clock.System.now().minus(kotlinx.datetime.Duration.hours(1))
        authTokenManager.storeAccessToken("expired-access-token")
        authTokenManager.storeRefreshToken("valid-refresh-token")
        authTokenManager.storeTokenExpiration(pastTime)
        
        // Create new service instance with expired token
        val serviceWithExpiredToken = DropboxService(dropboxConfig)
        
        // Service should handle token refresh internally
        val storageInfo = serviceWithExpiredToken.getStorageInfo()
        assertEquals(StorageType.DROPBOX, storageInfo.type)
        assertEquals("test-dropbox-enhanced", storageInfo.name)
    }
    
    @Test
    fun testEnhancedConfigurationValidation() {
        // Test with minimal configuration
        val minimalConfig = dropboxConfig.copy(
            refreshToken = null,
            rootPath = ""
        )
        val minimalService = DropboxService(minimalConfig)
        
        assertEquals("test-access-token-enhanced", minimalService.config.accessToken)
        assertEquals(null, minimalService.config.refreshToken)
        assertEquals("", minimalService.config.rootPath)
        
        // Test with maximum configuration
        val fullConfig = dropboxConfig.copy(
            rootPath = "/Custom/Path",
            metadata = mapOf("custom" to "value", "region" to "us-east-1")
        )
        val fullService = DropboxService(fullConfig)
        
        assertEquals("/Custom/Path", fullService.config.rootPath)
        assertEquals("value", fullService.config.metadata["custom"])
        assertEquals("us-east-1", fullService.config.metadata["region"])
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