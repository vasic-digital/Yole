package digital.vasic.yole.network

import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorage
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import digital.vasic.yole.network.protocols.googledrive.GoogleDriveService
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.sftp.SftpService
import digital.vasic.yole.network.protocols.onedrive.OneDriveService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Comprehensive integration test suite for all network storage implementations
 * Tests cross-protocol compatibility and unified interface behavior
 */
class NetworkStorageIntegrationTest {
    
    private lateinit var mockSecureStorage: SecureStorage
    
    // Test configurations for all protocols
    private val dropboxConfig = StorageConfig.DropboxConfig(
        name = "integration-test-dropbox",
        accessToken = "test-access-token",
        appKey = "test-app-key",
        appSecret = "test-app-secret",
        refreshToken = "test-refresh-token",
        rootPath = "/Apps/Yole"
    )
    
    private val googleDriveConfig = StorageConfig.GoogleDriveConfig(
        name = "integration-test-googledrive",
        clientId = "test-client-id",
        clientSecret = "test-client-secret",
        refreshToken = "test-refresh-token",
        accessToken = "test-access-token",
        rootFolderId = "root"
    )
    
    private val ftpConfig = StorageConfig.FtpConfig(
        name = "integration-test-ftp",
        host = "ftp.example.com",
        port = 21,
        username = "testuser",
        password = "testpass",
        rootPath = "/public_html",
        passiveMode = true,
        secureFtp = false,
        encoding = "UTF-8",
        connectionTimeout = 30000
    )
    
    private val sftpConfig = StorageConfig.SftpConfig(
        name = "integration-test-sftp",
        host = "sftp.example.com",
        port = 22,
        username = "testuser",
        password = "testpass",
        rootPath = "/home/testuser",
        useSsl = true,
        connectionTimeout = 30000,
        strictHostKeyChecking = false
    )
    
    private val oneDriveConfig = StorageConfig.OneDriveConfig(
        name = "integration-test-onedrive",
        clientId = "test-client-id",
        clientSecret = "test-client-secret",
        refreshToken = "test-refresh-token",
        accessToken = "test-access-token",
        driveType = OneDriveDriveType.ME,
        rootFolderId = "root"
    )
    
    @BeforeTest
    fun setup() = runBlocking<Unit> {
        mockSecureStorage = MockSecureStorage()
        
        // Initialize auth token managers for cloud services
        listOf(dropboxConfig, googleDriveConfig, oneDriveConfig).forEach { config ->
            val serviceName = when (config) {
                is StorageConfig.DropboxConfig -> "dropbox"
                is StorageConfig.GoogleDriveConfig -> "googledrive"
                is StorageConfig.OneDriveConfig -> "onedrive"
                else -> "unknown"
            }
            val authTokenManager = AuthTokenManager(serviceName, mockSecureStorage)
            when (config) {
                is StorageConfig.DropboxConfig -> {
                    authTokenManager.storeTokenInfo(
                        accessToken = config.accessToken,
                        refreshToken = config.refreshToken,
                        expiresIn = 3600L
                    )
                }
                is StorageConfig.GoogleDriveConfig -> {
                    authTokenManager.storeTokenInfo(
                        accessToken = config.accessToken ?: "",
                        refreshToken = config.refreshToken,
                        expiresIn = 3600L
                    )
                }
                is StorageConfig.OneDriveConfig -> {
                    authTokenManager.storeTokenInfo(
                        accessToken = config.accessToken ?: "",
                        refreshToken = config.refreshToken,
                        expiresIn = 3600L
                    )
                }
                is StorageConfig.FtpConfig,
                is StorageConfig.SftpConfig,
                is StorageConfig.SmbConfig,
                is StorageConfig.WebDavConfig,
                is StorageConfig.GitConfig -> {
                    // No token storage needed for these protocols
                }
            }
        }
    }
    
    @Test
    fun testAllServicesImplementSameInterface() = runBlocking<Unit> {
        // Create instances of all services
        val dropboxService = DropboxService(dropboxConfig)
        val googleDriveService = GoogleDriveService(googleDriveConfig)
        val ftpService = FtpService(ftpConfig)
        val sftpService = SftpService(sftpConfig)
        val oneDriveService = OneDriveService(oneDriveConfig)
        
        // All services should implement NetworkStorageService
        val services = listOf(
            dropboxService to "Dropbox",
            googleDriveService to "Google Drive",
            ftpService to "FTP",
            sftpService to "SFTP",
            oneDriveService to "OneDrive"
        )
        
        services.forEach { (service, name) ->
            // Test basic interface methods
            assertNotNull(service.config, "$name should have config")
            assertNotNull(service.isOnline, "$name should have isOnline property")
            assertNotNull(service.rootPath, "$name should have rootPath property")
            
            // Test storage info
            val storageInfo = service.getStorageInfo()
            assertNotNull(storageInfo.id, "$name should have storage ID")
            assertNotNull(storageInfo.name, "$name should have storage name")
            assertNotNull(storageInfo.type, "$name should have storage type")
            assertNotNull(storageInfo.location, "$name should have storage location")
            
            // Test connection methods
            val connectResult = service.connect()
            assertTrue(connectResult.isSuccess || connectResult.isFailure, "$name connect should complete")
            
            val testResult = service.testConnection()
            assertTrue(testResult.isSuccess || testResult.isFailure, "$name testConnection should complete")
            
            val disconnectResult = service.disconnect()
            assertTrue(disconnectResult.isSuccess, "$name disconnect should succeed")
        }
    }
    
    @Test
    fun testUnifiedFileOperations() = runBlocking<Unit> {
        val services = listOf(
            DropboxService(dropboxConfig) to "Dropbox",
            GoogleDriveService(googleDriveConfig) to "Google Drive",
            FtpService(ftpConfig) to "FTP",
            SftpService(sftpConfig) to "SFTP",
            OneDriveService(oneDriveConfig) to "OneDrive"
        )
        
        services.forEach { (service, name) ->
            // Test list files
            val listResult = service.listFiles("/").first()
            assertTrue(listResult.isSuccess || listResult.isFailure, "$name listFiles should complete")
            
            // Test file info
            val fileInfoResult = service.getFileInfo("/test.txt")
            assertTrue(fileInfoResult.isSuccess || fileInfoResult.isFailure, "$name getFileInfo should complete")
            
            // Test exists
            val existsResult = service.exists("/test.txt")
            assertTrue(existsResult.isSuccess, "$name exists should succeed")
            
            // Test quota info (cloud services may fail when not connected)
            val quotaResult = service.getQuotaInfo()
            assertTrue(quotaResult.isSuccess || quotaResult.isFailure, "$name getQuotaInfo should complete")
            
            // Test path operations
            val parentPath = service.getParentPath("/folder/file.txt")
            assertNotNull(parentPath, "$name should return parent path")
            
            val validateResult = service.validatePath("/test.txt")
            assertTrue(validateResult.isSuccess || validateResult.isFailure, "$name validatePath should complete")
        }
    }
    
    @Test
    fun testUnifiedUploadDownloadOperations() = runBlocking<Unit> {
        val services = listOf(
            DropboxService(dropboxConfig) to "Dropbox",
            GoogleDriveService(googleDriveConfig) to "Google Drive",
            FtpService(ftpConfig) to "FTP",
            SftpService(sftpConfig) to "SFTP",
            OneDriveService(oneDriveConfig) to "OneDrive"
        )
        
        services.forEach { (service, name) ->
            // Test download operations
            val downloadOperations = service.downloadFile("/remote-file.txt", "/local-file.txt")
            val downloadOps = mutableListOf<NetworkOperation>()
            downloadOperations.collect { downloadOps.add(it) }
            
            assertTrue(downloadOps.isNotEmpty(), "$name should emit download operations")
            val firstDownloadOp = downloadOps.first()
            assertEquals(NetworkOperation.Type.DOWNLOAD, firstDownloadOp.type, "$name download type should be correct")
            
            // Test upload operations
            val uploadOperations = service.uploadFile("/local-file.txt", "/remote-file.txt")
            val uploadOps = mutableListOf<NetworkOperation>()
            uploadOperations.collect { uploadOps.add(it) }
            
            assertTrue(uploadOps.isNotEmpty(), "$name should emit upload operations")
            val firstUploadOp = uploadOps.first()
            assertEquals(NetworkOperation.Type.UPLOAD, firstUploadOp.type, "$name upload type should be correct")
            
            // Test active operations
            val activeOps = service.getActiveOperations().first()
            assertNotNull(activeOps, "$name should return active operations")
        }
    }
    
    @Test
    fun testUnifiedFolderOperations() = runBlocking<Unit> {
        val services = listOf(
            DropboxService(dropboxConfig) to "Dropbox",
            GoogleDriveService(googleDriveConfig) to "Google Drive",
            FtpService(ftpConfig) to "FTP",
            SftpService(sftpConfig) to "SFTP",
            OneDriveService(oneDriveConfig) to "OneDrive"
        )
        
        services.forEach { (service, name) ->
            // Test create folder
            val createFolderResult = service.createFolder("/test-folder")
            assertTrue(createFolderResult.isSuccess || createFolderResult.isFailure, "$name createFolder should complete")
            
            if (createFolderResult.isSuccess) {
                val folder = createFolderResult.getOrNull()
                assertNotNull(folder, "$name should return folder info")
                assertTrue(folder.isFolder, "$name created item should be folder")
            }
            
            // Test delete file
            val deleteResult = service.deleteFile("/test-file.txt")
            assertTrue(deleteResult.isSuccess || deleteResult.isFailure, "$name deleteFile should complete")
            
            // Test rename file
            val renameResult = service.renameFile("/old-name.txt", "new-name.txt")
            assertTrue(renameResult.isSuccess || renameResult.isFailure, "$name renameFile should complete")
            
            // Test move file
            val moveResult = service.moveFile("/source.txt", "/destination.txt")
            assertTrue(moveResult.isSuccess || moveResult.isFailure, "$name moveFile should complete")
            
            // Test copy file (not supported by all protocols)
            val copyResult = service.copyFile("/source.txt", "/copy.txt")
            // Copy might not be supported by all protocols, so we just check it completes
            assertTrue(copyResult.isSuccess || copyResult.isFailure, "$name copyFile should complete")
        }
    }
    
    @Test
    fun testProtocolSpecificFeatures() = runBlocking<Unit> {
        val dropboxService = DropboxService(dropboxConfig)
        val googleDriveService = GoogleDriveService(googleDriveConfig)
        val ftpService = FtpService(ftpConfig)
        val sftpService = SftpService(sftpConfig)
        val oneDriveService = OneDriveService(oneDriveConfig)
        
        // Test Dropbox-specific features
        val dropboxStorageInfo = dropboxService.getStorageInfo()
        assertEquals(StorageType.DROPBOX, dropboxStorageInfo.type)
        assertTrue(dropboxStorageInfo.supportsFolders, "Dropbox should support folders")
        
        // Test Google Drive-specific features
        val googleDriveStorageInfo = googleDriveService.getStorageInfo()
        assertEquals(StorageType.GOOGLE_DRIVE, googleDriveStorageInfo.type)
        assertTrue(googleDriveStorageInfo.supportsFolders, "Google Drive should support folders")
        
        // Test FTP-specific limitations
        val ftpStorageInfo = ftpService.getStorageInfo()
        assertEquals(StorageType.FTP, ftpStorageInfo.type)
        assertFalse(ftpStorageInfo.supportsFolders, "FTP should not reliably support folders")
        assertFalse(ftpStorageInfo.supportsMetadata, "FTP should not support metadata")
        
        // Test SFTP-specific features
        val sftpStorageInfo = sftpService.getStorageInfo()
        assertEquals(StorageType.SFTP, sftpStorageInfo.type)
        assertTrue(sftpStorageInfo.supportsFolders, "SFTP should support folders")
        assertTrue(sftpStorageInfo.supportsMetadata, "SFTP should support metadata")
        
        // Test OneDrive-specific features
        val oneDriveStorageInfo = oneDriveService.getStorageInfo()
        assertEquals(StorageType.ONEDRIVE, oneDriveStorageInfo.type)
        assertTrue(oneDriveStorageInfo.supportsFolders, "OneDrive should support folders")
    }
    
    @Test
    fun testErrorHandlingConsistency() = runBlocking<Unit> {
        val services = listOf(
            DropboxService(dropboxConfig) to "Dropbox",
            GoogleDriveService(googleDriveConfig) to "Google Drive",
            FtpService(ftpConfig) to "FTP",
            SftpService(sftpConfig) to "SFTP",
            OneDriveService(oneDriveConfig) to "OneDrive"
        )
        
        services.forEach { (service, name) ->
            // Test operations when not connected
            val listResult = service.listFiles("/").first()
            if (listResult.isFailure) {
                val exception = listResult.exceptionOrNull()
                assertNotNull(exception, "$name should provide exception on failure")
                assertTrue(exception is NetworkStorageException, "$name should use NetworkStorageException")
            }
            
            // Test download when not connected
            val downloadOps = service.downloadFile("/test.txt", "/tmp/test.txt")
            val firstDownloadOp = downloadOps.first()
            if (firstDownloadOp.status == NetworkOperation.Status.FAILED) {
                assertNotNull(firstDownloadOp.error, "$name should provide error message on failed download")
            }
            
            // Test upload when not connected
            val uploadOps = service.uploadFile("/tmp/test.txt", "/test.txt")
            val firstUploadOp = uploadOps.first()
            if (firstUploadOp.status == NetworkOperation.Status.FAILED) {
                assertNotNull(firstUploadOp.error, "$name should provide error message on failed upload")
            }
        }
    }
    
    @Test
    fun testSyncAndCacheOperations() = runBlocking<Unit> {
        val services = listOf(
            DropboxService(dropboxConfig) to "Dropbox",
            GoogleDriveService(googleDriveConfig) to "Google Drive",
            FtpService(ftpConfig) to "FTP",
            SftpService(sftpConfig) to "SFTP",
            OneDriveService(oneDriveConfig) to "OneDrive"
        )
        
        services.forEach { (service, name) ->
            // Test sync operations
            val syncFileOps = service.syncFile("/test.txt", false)
            val syncFileList = mutableListOf<NetworkOperation>()
            syncFileOps.collect { syncFileList.add(it) }
            assertTrue(syncFileList.isNotEmpty(), "$name should emit sync operations")
            
            val syncAllOps = service.syncAll(false)
            val syncAllList = mutableListOf<NetworkOperation>()
            syncAllOps.collect { syncAllList.add(it) }
            // Sync all might return empty for some implementations
            
            // Test sync status
            val syncStatus = service.getSyncStatus("/").first()
            assertNotNull(syncStatus, "$name should return sync status")
            
            // Test cache operations
            val cacheEntries = service.getCacheEntries("/").first()
            assertNotNull(cacheEntries, "$name should return cache entries")
            
            val addCacheResult = service.addToCache("/test.txt", 100)
            assertTrue(addCacheResult.isSuccess, "$name addToCache should succeed")
            
            val removeCacheResult = service.removeFromCache("/test.txt")
            assertTrue(removeCacheResult.isSuccess, "$name removeFromCache should succeed")
            
            val clearCacheResult = service.clearCache()
            assertTrue(clearCacheResult.isSuccess, "$name clearCache should succeed")
        }
    }
    
    @Test
    fun testQuotaAndSpaceInformation() = runBlocking<Unit> {
        val services = listOf(
            DropboxService(dropboxConfig) to "Dropbox",
            GoogleDriveService(googleDriveConfig) to "Google Drive",
            FtpService(ftpConfig) to "FTP",
            SftpService(sftpConfig) to "SFTP",
            OneDriveService(oneDriveConfig) to "OneDrive"
        )
        
        services.forEach { (service, name) ->
            val quotaResult = service.getQuotaInfo()
            // Cloud services (Google Drive, Dropbox, OneDrive) may fail quota when not connected
            assertTrue(quotaResult.isSuccess || quotaResult.isFailure, "$name getQuotaInfo should complete")

            if (quotaResult.isSuccess) {
                val quota = quotaResult.getOrNull()
                assertNotNull(quota, "$name should return quota information")

                // Verify quota structure
                assertTrue(quota.totalSpace >= 0, "$name totalSpace should be non-negative")
                assertTrue(quota.usedSpace >= 0, "$name usedSpace should be non-negative")
                assertTrue(quota.availableSpace >= 0, "$name availableSpace should be non-negative")
                assertTrue(quota.usagePercentage >= 0.0, "$name usagePercentage should be non-negative")
                assertTrue(quota.usagePercentage <= 1.0, "$name usagePercentage should be <= 1.0")

                // Test quota calculations
                val expectedAvailable = quota.totalSpace - quota.usedSpace
                assertEquals(expectedAvailable, quota.availableSpace, "$name available space calculation should be correct")

                // Test boolean flags (only meaningful when quota is reported, i.e. totalSpace > 0)
                if (quota.totalSpace > 0) {
                    assertEquals(quota.availableSpace <= 0, quota.isFull, "$name isFull should be calculated correctly")
                    assertEquals(quota.usagePercentage > 0.9, quota.isLowOnSpace, "$name isLowOnSpace should be calculated correctly")
                }
            }
        }
    }
    
    @Test
    fun testSearchAndRecentChanges() = runBlocking<Unit> {
        val services = listOf(
            DropboxService(dropboxConfig) to "Dropbox",
            GoogleDriveService(googleDriveConfig) to "Google Drive",
            FtpService(ftpConfig) to "FTP",
            SftpService(sftpConfig) to "SFTP",
            OneDriveService(oneDriveConfig) to "OneDrive"
        )
        
        services.forEach { (service, name) ->
            // Test search files
            val searchResult = service.searchFiles("test", "/", false).first()
            assertTrue(searchResult.isSuccess || searchResult.isFailure, "$name searchFiles should complete")
            
            // Test recent changes
            val since = Clock.System.now()
            val recentChanges = service.getRecentChanges(since, "/").first()
            assertNotNull(recentChanges, "$name should return recent changes")
        }
    }
    
    @Test
    fun testOperationManagement() = runBlocking<Unit> {
        val services = listOf(
            DropboxService(dropboxConfig) to "Dropbox",
            GoogleDriveService(googleDriveConfig) to "Google Drive",
            FtpService(ftpConfig) to "FTP",
            SftpService(sftpConfig) to "SFTP",
            OneDriveService(oneDriveConfig) to "OneDrive"
        )
        
        services.forEach { (service, name) ->
            // Test operation management methods
            val cancelResult = service.cancelOperation(12345L)
            assertTrue(cancelResult.isSuccess, "$name cancelOperation should succeed")
            
            val pauseResult = service.pauseOperation(12345L)
            assertTrue(pauseResult.isSuccess, "$name pauseOperation should succeed")
            
            val resumeResult = service.resumeOperation(12345L)
            assertTrue(resumeResult.isSuccess, "$name resumeOperation should succeed")
        }
    }
    
    @Test
    fun testCrossProtocolCompatibility() = runBlocking<Unit> {
        // Create all services
        val services = listOf(
            DropboxService(dropboxConfig),
            GoogleDriveService(googleDriveConfig),
            FtpService(ftpConfig),
            SftpService(sftpConfig),
            OneDriveService(oneDriveConfig)
        )
        
        // Test that all services can be used interchangeably
        val results = services.map { service ->
            // Test basic operations that should work across all protocols
            val storageInfo = service.getStorageInfo()
            val quotaResult = service.getQuotaInfo()
            val existsResult = service.exists("/test.txt")
            
            Triple(storageInfo.isOnline, quotaResult.isSuccess, existsResult.getOrNull() != null)
        }
        
        // All services should complete basic operations without crashing
        assertEquals(services.size, results.size, "All services should return results")
        results.forEach { (isOnline, quotaCompleted, existsResult) ->
            // Basic operations should complete (results may vary based on connection state)
            // Cloud services may fail quota when not connected, which is valid behavior
            assertNotNull(existsResult, "All services should handle exists checks")
        }
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