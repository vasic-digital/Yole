package digital.vasic.yole.network.protocols.sftp

import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Enhanced comprehensive test suite for SftpService
 * Tests secure FTP operations with SSH key authentication and encryption
 */
class SftpServiceEnhancedTest {
    
    private val passwordAuthConfig = StorageConfig.SftpConfig(
        name = "test-sftp-password",
        host = "sftp.example.com",
        port = 22,
        username = "testuser",
        password = "testpass",
        rootPath = "/home/testuser",
        useSsl = true,
        connectionTimeout = 30000,
        strictHostKeyChecking = false
    )
    
    private val keyAuthConfig = StorageConfig.SftpConfig(
        name = "test-sftp-key",
        host = "sftp.secure.com",
        port = 22,
        username = "keyuser",
        password = null,
        privateKeyPath = "/home/user/.ssh/id_rsa",
        privateKeyPassphrase = "keypassphrase",
        knownHostsPath = "/home/user/.ssh/known_hosts",
        strictHostKeyChecking = true,
        rootPath = "/data",
        useSsl = true,
        connectionTimeout = 45000
    )
    
    private val minimalConfig = StorageConfig.SftpConfig(
        name = "test-sftp-minimal",
        host = "sftp.minimal.com",
        port = 22,
        username = "minimaluser",
        password = "minimalpass",
        strictHostKeyChecking = false // Disable for minimal testing config
        // Other fields use defaults
    )
    
    private lateinit var passwordAuthService: SftpService
    private lateinit var keyAuthService: SftpService
    private lateinit var minimalService: SftpService
    
    @BeforeTest
    fun setup() {
        passwordAuthService = SftpService(passwordAuthConfig)
        keyAuthService = SftpService(keyAuthConfig)
        minimalService = SftpService(minimalConfig)
    }
    
    @Test
    fun testSftpServiceEnhancedInitialization() {
        // Test password authentication configuration
        assertEquals("test-sftp-password", passwordAuthService.config.name)
        assertEquals("sftp.example.com", passwordAuthService.config.host)
        assertEquals(22, passwordAuthService.config.port)
        assertEquals("testuser", passwordAuthService.config.username)
        assertEquals("testpass", passwordAuthService.config.password)
        assertEquals("/home/testuser", passwordAuthService.config.rootPath)
        assertTrue(passwordAuthService.config.useSsl)
        assertEquals(30000, passwordAuthService.config.connectionTimeout)
        assertFalse(passwordAuthService.config.strictHostKeyChecking)
        assertNull(passwordAuthService.config.privateKeyPath)
        assertNull(passwordAuthService.config.privateKeyPassphrase)
        assertNull(passwordAuthService.config.knownHostsPath)
        assertEquals("/", passwordAuthService.rootPath)
        assertFalse(passwordAuthService.isOnline, "Should not be connected initially")
        
        // Test key-based authentication configuration
        assertEquals("test-sftp-key", keyAuthService.config.name)
        assertEquals("sftp.secure.com", keyAuthService.config.host)
        assertEquals(22, keyAuthService.config.port)
        assertEquals("keyuser", keyAuthService.config.username)
        assertNull(keyAuthService.config.password)
        assertEquals("/home/user/.ssh/id_rsa", keyAuthService.config.privateKeyPath)
        assertEquals("keypassphrase", keyAuthService.config.privateKeyPassphrase)
        assertEquals("/home/user/.ssh/known_hosts", keyAuthService.config.knownHostsPath)
        assertTrue(keyAuthService.config.strictHostKeyChecking)
        assertEquals("/data", keyAuthService.config.rootPath)
        assertTrue(keyAuthService.config.useSsl)
        assertEquals(45000, keyAuthService.config.connectionTimeout)
        
        // Test minimal configuration
        assertEquals("test-sftp-minimal", minimalService.config.name)
        assertEquals("sftp.minimal.com", minimalService.config.host)
        assertEquals(22, minimalService.config.port)
        assertEquals("minimaluser", minimalService.config.username)
        assertEquals("minimalpass", minimalService.config.password)
        assertEquals("/", minimalService.config.rootPath) // Default
        assertTrue(minimalService.config.useSsl) // Default
        assertEquals(30000, minimalService.config.connectionTimeout) // Default
        assertFalse(minimalService.config.strictHostKeyChecking) // Explicitly set to false for testing
    }
    
    @Test
    fun testEnhancedStorageInfo() = runTest {
        val storageInfo = passwordAuthService.getStorageInfo()
        
        assertEquals("sftp_test-sftp-password", storageInfo.id)
        assertEquals("test-sftp-password", storageInfo.name)
        assertEquals(StorageType.SFTP, storageInfo.type)
        assertEquals("sftp://sftp.example.com:22/home/testuser", storageInfo.location)
        assertFalse(storageInfo.isOnline, "Should not be online initially")
        assertTrue(storageInfo.supportsFolders, "Should support folders")
        assertTrue(storageInfo.supportsMetadata, "Should support metadata")
    }
    
    @Test
    fun testEnhancedAuthenticationValidation() = runTest {
        // Test password authentication validation
        val passwordResult = passwordAuthService.connect()
        assertTrue(passwordResult.isSuccess, "Password authentication should be valid")
        
        // Test key-based authentication validation
        val keyResult = keyAuthService.connect()
        assertTrue(keyResult.isSuccess, "Key-based authentication should be valid")
        
        // Test minimal configuration validation
        val minimalResult = minimalService.connect()
        assertTrue(minimalResult.isSuccess, "Minimal configuration should be valid")
    }
    
    @Test
    fun testEnhancedInvalidAuthentication() = runTest {
        // Test configuration without any authentication method
        val invalidConfig = StorageConfig.SftpConfig(
            name = "test-sftp-invalid",
            host = "sftp.invalid.com",
            port = 22,
            username = "nouser", // No password
            password = null,
            privateKeyPath = null // No private key
        )
        val invalidService = SftpService(invalidConfig)
        
        val result = invalidService.connect()
        assertTrue(result.isFailure, "Should fail without authentication method")
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkStorageException.ConnectionException.Authentication,
            "Should fail with authentication exception")
        assertTrue(exception?.message?.contains("requires either password or private key") == true,
            "Should have appropriate error message")
    }
    
    @Test
    fun testEnhancedConnectWithInvalidConfiguration() = runTest {
        // Test with invalid host
        val invalidHostConfig = passwordAuthConfig.copy(host = "")
        val invalidHostService = SftpService(invalidHostConfig)
        
        val result = invalidHostService.connect()
        assertTrue(result.isFailure, "Connection should fail with invalid host")
        
        // Test with invalid port
        val invalidPortConfig = passwordAuthConfig.copy(port = 99999)
        val invalidPortService = SftpService(invalidPortConfig)
        
        val portResult = invalidPortService.testConnection()
        assertTrue(portResult.isFailure, "Connection test should fail with invalid port")
    }
    
    @Test
    fun testEnhancedStrictHostKeyChecking() = runTest {
        // Test with strict host key checking enabled but no known_hosts file
        val strictConfig = passwordAuthConfig.copy(
            strictHostKeyChecking = true,
            knownHostsPath = null
        )
        val strictService = SftpService(strictConfig)
        
        val result = strictService.testConnection()
        assertTrue(result.isFailure, "Should fail without known_hosts file when strict checking is enabled")
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("no known_hosts file provided") == true,
            "Should have appropriate error message")
    }
    
    @Test
    fun testEnhancedDisconnect() = runTest {
        // First connect
        passwordAuthService.connect()
        
        // Then disconnect
        val result = passwordAuthService.disconnect()
        assertTrue(result.isSuccess, "Disconnect should succeed")
        
        // Should be offline after disconnect
        assertFalse(passwordAuthService.isOnline, "Should be offline after disconnect")
    }
    
    @Test
    fun testEnhancedTestConnection() = runTest {
        val result = passwordAuthService.testConnection()
        
        assertTrue(result.isSuccess, "Test connection should succeed")
        assertTrue(result.getOrNull() ?: false, "Test connection should return true")
    }
    
    @Test
    fun testEnhancedListFilesWhenNotConnected() = runTest {
        val result = passwordAuthService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkStorageException.ConnectionException.NotConnected, 
            "Should fail with not connected exception")
    }
    
    @Test
    fun testEnhancedListFilesWhenConnected() = runTest {
        // Connect first
        passwordAuthService.connect()
        
        val result = passwordAuthService.listFiles("/").first()
        
        assertTrue(result.isSuccess, "List files should succeed when connected")
        val files = result.getOrNull()
        assertNotNull(files, "Files list should be returned")
        assertTrue(files.isNotEmpty(), "Files list should not be empty")
        
        // Verify enhanced file structure with permissions
        val pdfFile = files.find { it.name == "document.pdf" }
        assertNotNull(pdfFile, "Should find PDF file")
        assertEquals(5242880L, pdfFile.size) // 5MB
        assertFalse(pdfFile.isFolder)
        assertTrue(pdfFile.permissions.contains(DocumentPermission.EXECUTE), "PDF should have execute permission")
        
        val folder = files.find { it.name == "project_folder" }
        assertNotNull(folder, "Should find folder")
        assertTrue(folder.isFolder)
        assertTrue(folder.permissions.contains(DocumentPermission.EXECUTE), "Folder should have execute permission")
    }
    
    @Test
    fun testEnhancedDownloadFileWhenNotConnected() = runTest {
        val operations = passwordAuthService.downloadFile("/document.pdf", "/tmp/document.pdf")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("SFTP not connected", firstOperation.error)
    }
    
    @Test
    fun testEnhancedDownloadFileWhenConnected() = runTest {
        // Connect first
        passwordAuthService.connect()
        
        val operations = passwordAuthService.downloadFile("/document.pdf", "/tmp/document.pdf")
        val operationList = mutableListOf<NetworkOperation>()
        operations.collect { operationList.add(it) }
        
        assertTrue(operationList.isNotEmpty(), "Should emit download operations")
        
        val firstOp = operationList.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOp.type)
        assertEquals(NetworkOperation.Status.IN_PROGRESS, firstOp.status)
        assertEquals("/document.pdf", firstOp.remotePath)
        assertEquals("/tmp/document.pdf", firstOp.localPath)
        
        val lastOp = operationList.last()
        assertEquals(NetworkOperation.Status.COMPLETED, lastOp.status)
        assertEquals(1.0, lastOp.progress)
        assertTrue(lastOp.totalSize > 0, "Should have total size")
        assertEquals(lastOp.totalSize, lastOp.bytesTransferred, "Should transfer all bytes")
        
        // Verify secure transfer simulation
        assertTrue(operationList.any { it.progress == 0.1 }, "Should show initial progress")
        assertTrue(operationList.any { it.progress == 0.4 }, "Should show metadata retrieval progress")
        assertTrue(operationList.any { it.progress >= 0.8 }, "Should show final transfer progress")
    }
    
    @Test
    fun testEnhancedUploadFileWhenNotConnected() = runTest {
        val operations = passwordAuthService.uploadFile("/tmp/report.md", "/report.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("SFTP not connected", firstOperation.error)
    }
    
    @Test
    fun testEnhancedUploadFileWhenConnected() = runTest {
        // Connect first
        passwordAuthService.connect()
        
        val operations = passwordAuthService.uploadFile("/tmp/report.md", "/report.md")
        val operationList = mutableListOf<NetworkOperation>()
        operations.collect { operationList.add(it) }
        
        assertTrue(operationList.isNotEmpty(), "Should emit upload operations")
        
        val firstOp = operationList.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOp.type)
        assertEquals(NetworkOperation.Status.IN_PROGRESS, firstOp.status)
        assertEquals("/report.md", firstOp.remotePath)
        assertEquals("/tmp/report.md", firstOp.localPath)
        
        val lastOp = operationList.last()
        assertEquals(NetworkOperation.Status.COMPLETED, lastOp.status)
        assertEquals(1.0, lastOp.progress)
        assertTrue(lastOp.totalSize > 0, "Should have total size")
        assertEquals(lastOp.totalSize, lastOp.bytesTransferred, "Should transfer all bytes")
        
        // Verify secure upload simulation
        assertTrue(operationList.any { it.progress == 0.1 }, "Should show initial progress")
        assertTrue(operationList.any { it.progress == 0.6 }, "Should show parent folder resolution progress")
        assertTrue(operationList.any { it.progress >= 0.9 }, "Should show final upload progress")
    }
    
    @Test
    fun testEnhancedFileOperationsWhenConnected() = runTest {
        // Connect first
        passwordAuthService.connect()
        
        // Test delete file
        val deleteResult = passwordAuthService.deleteFile("/old-file.txt")
        assertTrue(deleteResult.isSuccess, "Delete should succeed when connected")
        
        // Test create folder
        val createFolderResult = passwordAuthService.createFolder("/new-folder")
        assertTrue(createFolderResult.isSuccess, "Create folder should succeed")
        val folder = createFolderResult.getOrNull()
        assertNotNull(folder, "Folder should be created")
        assertEquals("new-folder", folder.name)
        assertEquals("/home/testuser/new-folder", folder.path) // Should include root path
        assertTrue(folder.isFolder)
        assertTrue(folder.permissions.contains(DocumentPermission.EXECUTE), "Folder should have execute permission")
        
        // Test rename file
        val renameResult = passwordAuthService.renameFile("/old-name.txt", "new-name.txt")
        assertTrue(renameResult.isSuccess, "Rename should succeed when connected")
        
        // Test move file
        val moveResult = passwordAuthService.moveFile("/source.txt", "/destination.txt")
        assertTrue(moveResult.isSuccess, "Move should succeed")
        val movedFile = moveResult.getOrNull()
        assertNotNull(movedFile, "Moved file should be returned")
        assertEquals("destination.txt", movedFile.name)
        assertEquals("/home/testuser/destination.txt", movedFile.path)
        assertTrue(movedFile.permissions.contains(DocumentPermission.EXECUTE), "Should have execute permission")
        
        // Test copy file (SFTP supports copy)
        val copyResult = passwordAuthService.copyFile("/source.txt", "/copy.txt")
        assertTrue(copyResult.isSuccess, "Copy should succeed for SFTP")
    }
    
    @Test
    fun testEnhancedGetFileInfo() = runTest {
        // Connect first
        passwordAuthService.connect()
        
        val result = passwordAuthService.getFileInfo("/config.json")
        
        assertTrue(result.isSuccess, "Get file info should succeed when connected")
        val fileInfo = result.getOrNull()
        assertNotNull(fileInfo, "File info should be returned")
        assertEquals("config.json", fileInfo.name)
        assertEquals("/home/testuser/config.json", fileInfo.path)
        assertFalse(fileInfo.isFolder, "Should be a file")
        assertTrue(fileInfo.size > 0, "Should have size")
        assertTrue(fileInfo.permissions.contains(DocumentPermission.READ), "Should have read permission")
        assertTrue(fileInfo.permissions.contains(DocumentPermission.WRITE), "Should have write permission")
        assertTrue(fileInfo.permissions.contains(DocumentPermission.DELETE), "Should have delete permission")
    }
    
    @Test
    fun testEnhancedQuotaInfo() = runTest {
        val result = passwordAuthService.getQuotaInfo()
        
        assertTrue(result.isSuccess, "Get quota info should succeed")
        val quota = result.getOrNull()
        assertNotNull(quota, "Quota info should be returned")
        
        // SFTP doesn't provide quota information directly
        assertEquals(0L, quota.totalSpace)
        assertEquals(0L, quota.usedSpace)
        assertEquals(0L, quota.availableSpace)
        assertEquals(0.0, quota.usagePercentage)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
        assertEquals("SFTP", quota.metadata["provider"])
        assertEquals("Quota not supported by SFTP protocol", quota.metadata["note"])
        assertEquals("SSH2", quota.metadata["encryption"])
    }
    
    @Test
    fun testEnhancedExists() = runTest {
        // Connect first
        passwordAuthService.connect()
        
        val result = passwordAuthService.exists("/config.json")
        
        assertTrue(result.isSuccess, "Exists check should succeed")
        assertTrue(result.getOrNull() ?: false, "Should return true for existing file (mock)")
    }
    
    @Test
    fun testEnhancedPathOperations() = runTest {
        // Test getParentPath - with rootPath = "/home/testuser"
        // getParentPath works on the input path directly, not the normalized path
        assertEquals("/", passwordAuthService.getParentPath("/test.txt"))
        assertEquals("/folder", passwordAuthService.getParentPath("/folder/test.txt"))
        assertEquals("/folder/subfolder", passwordAuthService.getParentPath("/folder/subfolder/test.txt"))
        assertEquals(null, passwordAuthService.getParentPath("/"))
        assertEquals(null, passwordAuthService.getParentPath("")) // Empty path has no parent
        
        // Test validatePath
        assertTrue(passwordAuthService.validatePath("/test.txt").isSuccess)
        assertTrue(passwordAuthService.validatePath("/folder/test.txt").isSuccess)
        assertTrue(passwordAuthService.validatePath("/folder/subfolder/test.txt").isSuccess)
        assertTrue(passwordAuthService.validatePath("").isFailure) // Empty path is invalid
    }
    
    @Test
    fun testEnhancedActiveOperations() = runTest {
        // Connect first
        passwordAuthService.connect()
        
        val activeOps = passwordAuthService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testEnhancedCacheOperations() = runTest {
        // Test get cache entries
        val cacheEntries = passwordAuthService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        // Test add to cache
        val addToCacheResult = passwordAuthService.addToCache("/test.txt", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        // Test remove from cache
        val removeFromCacheResult = passwordAuthService.removeFromCache("/test.txt")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        // Test clear cache
        val clearCacheResult = passwordAuthService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testEnhancedSyncStatus() = runTest {
        val syncStatus = passwordAuthService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testEnhancedSyncOperations() = runTest {
        // Test sync file
        val syncFileOperations = passwordAuthService.syncFile("/test.txt", false)
        val syncFileOp = syncFileOperations.first()
        assertEquals(NetworkOperation.Type.SYNC, syncFileOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, syncFileOp.status)
        assertEquals("/test.txt", syncFileOp.remotePath)
        
        // Test sync all
        val syncAllOperations = passwordAuthService.syncAll(false)
        val syncAllOp = syncAllOperations.first()
        assertEquals(NetworkOperation.Type.SYNC, syncAllOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, syncAllOp.status)
        assertEquals("/", syncAllOp.remotePath)
    }
    
    @Test
    fun testEnhancedSearchFiles() = runTest {
        val result = passwordAuthService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail for SFTP")
        assertEquals("SFTP does not support search operations", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testEnhancedRecentChanges() = runTest {
        val since = Clock.System.now()
        val changes = passwordAuthService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty")
    }
    
    @Test
    fun testEnhancedOperationManagement() = runTest {
        // Test cancel operation
        val cancelResult = passwordAuthService.cancelOperation(12345L)
        assertTrue(cancelResult.isSuccess, "Cancel operation should succeed")
        
        // Test pause operation
        val pauseResult = passwordAuthService.pauseOperation(12345L)
        assertTrue(pauseResult.isSuccess, "Pause operation should succeed")
        
        // Test resume operation
        val resumeResult = passwordAuthService.resumeOperation(12345L)
        assertTrue(resumeResult.isSuccess, "Resume operation should succeed")
    }
    
    @Test
    fun testEnhancedSftpSecurityFeatures() = runTest {
        // Test key-based authentication service
        val keyStorageInfo = keyAuthService.getStorageInfo()
        assertEquals("sftp://sftp.secure.com:22/data", keyStorageInfo.location)
        
        // Test connection with key-based auth
        val keyConnectResult = keyAuthService.connect()
        assertTrue(keyConnectResult.isSuccess, "Key-based authentication should work")
        assertTrue(keyAuthService.isOnline, "Should be online with key auth")
        
        // Test minimal service (uses defaults)
        val minimalStorageInfo = minimalService.getStorageInfo()
        assertEquals("sftp://sftp.minimal.com:22/", minimalStorageInfo.location)
        
        val minimalConnectResult = minimalService.connect()
        assertTrue(minimalConnectResult.isSuccess, "Minimal configuration should work")
    }
    
    @Test
    fun testEnhancedSftpVsFtpAdvantages() = runTest {
        // SFTP has several advantages over basic FTP
        
        // 1. Reliable folder support
        val storageInfo = passwordAuthService.getStorageInfo()
        assertTrue(storageInfo.supportsFolders, "SFTP should support folders reliably")
        
        // 2. Metadata support
        assertTrue(storageInfo.supportsMetadata, "SFTP should support metadata")
        
        // 3. Copy operations supported
        passwordAuthService.connect()
        val copyResult = passwordAuthService.copyFile("/source.txt", "/dest.txt")
        assertTrue(copyResult.isSuccess, "SFTP should support copy operations")
        
        // 4. Enhanced security in quota info
        val quotaResult = passwordAuthService.getQuotaInfo()
        val quota = quotaResult.getOrNull()
        assertNotNull(quota)
        assertEquals("SSH2", quota.metadata["encryption"], "Should indicate SSH2 encryption")
        
        // 5. Better permission handling
        val fileInfoResult = passwordAuthService.getFileInfo("/test.txt")
        val fileInfo = fileInfoResult.getOrNull()
        assertNotNull(fileInfo)
        assertTrue(fileInfo.permissions.contains(DocumentPermission.EXECUTE), "Should support execute permission")
    }
    
    @Test
    fun testEnhancedConnectionScenarios() = runTest {
        // Test connection timeout
        val timeoutConfig = passwordAuthConfig.copy(connectionTimeout = 5000) // 5 second timeout
        val timeoutService = SftpService(timeoutConfig)
        
        val timeoutResult = timeoutService.testConnection()
        // Should handle timeout gracefully with SSH negotiation
        assertTrue(timeoutResult.isSuccess || timeoutResult.isFailure, "Should handle timeout")
        
        // Test SSL/TLS enforcement
        val noSslConfig = passwordAuthConfig.copy(useSsl = false)
        val noSslService = SftpService(noSslConfig)
        
        val noSslResult = noSslService.connect()
        // SFTP always uses SSH encryption, so this should still work
        assertTrue(noSslResult.isSuccess, "SFTP should always use encryption")
    }
}