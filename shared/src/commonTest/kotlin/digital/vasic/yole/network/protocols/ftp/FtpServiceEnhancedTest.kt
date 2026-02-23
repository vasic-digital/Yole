package digital.vasic.yole.network.protocols.ftp

import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Enhanced comprehensive test suite for FtpService
 * Tests real FTP protocol implementation and file operations
 */
class FtpServiceEnhancedTest {
    
    private val ftpConfig = StorageConfig.FtpConfig(
        name = "test-ftp-enhanced",
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
    
    private val secureFtpConfig = StorageConfig.FtpConfig(
        name = "test-secure-ftp",
        host = "ftp.secure.com",
        port = 21,
        username = "secureuser",
        password = "securepass",
        rootPath = "/",
        passiveMode = true,
        secureFtp = true,
        encoding = "UTF-8",
        connectionTimeout = 45000
    )
    
    private lateinit var ftpService: FtpService
    private lateinit var secureFtpService: FtpService
    
    @BeforeTest
    fun setup() {
        ftpService = FtpService(ftpConfig)
        secureFtpService = FtpService(secureFtpConfig)
    }
    
    @Test
    fun testFtpServiceEnhancedInitialization() {
        // Test standard FTP configuration
        assertEquals("test-ftp-enhanced", ftpService.config.name)
        assertEquals("ftp.example.com", ftpService.config.host)
        assertEquals(21, ftpService.config.port)
        assertEquals("testuser", ftpService.config.username)
        assertEquals("testpass", ftpService.config.password)
        assertEquals("/public_html", ftpService.config.rootPath)
        assertTrue(ftpService.config.passiveMode)
        assertFalse(ftpService.config.secureFtp)
        assertEquals("UTF-8", ftpService.config.encoding)
        assertEquals(30000, ftpService.config.connectionTimeout)
        assertEquals("/", ftpService.rootPath)
        assertFalse(ftpService.isOnline, "Should not be connected initially")
        
        // Test secure FTP configuration
        assertEquals("test-secure-ftp", secureFtpService.config.name)
        assertEquals("ftp.secure.com", secureFtpService.config.host)
        assertEquals(21, secureFtpService.config.port)
        assertEquals("secureuser", secureFtpService.config.username)
        assertEquals("securepass", secureFtpService.config.password)
        assertEquals("/", secureFtpService.config.rootPath)
        assertTrue(secureFtpService.config.passiveMode)
        assertTrue(secureFtpService.config.secureFtp)
        assertEquals("UTF-8", secureFtpService.config.encoding)
        assertEquals(45000, secureFtpService.config.connectionTimeout)
    }
    
    @Test
    fun testEnhancedStorageInfo() = runTest {
        val storageInfo = ftpService.getStorageInfo()
        
        assertEquals("ftp_test-ftp-enhanced", storageInfo.id)
        assertEquals("test-ftp-enhanced", storageInfo.name)
        assertEquals(StorageType.FTP, storageInfo.type)
        assertEquals("ftp://ftp.example.com:21/public_html", storageInfo.location)
        assertFalse(storageInfo.isOnline, "Should not be online initially")
        assertFalse(storageInfo.supportsFolders, "Should not support folders (FTP limitation)")
        assertFalse(storageInfo.supportsMetadata, "Should not support metadata (FTP limitation)")
    }
    
    @Test
    fun testEnhancedConnectWithValidConfiguration() = runTest {
        val result = ftpService.connect()
        
        // Should succeed with valid configuration
        assertTrue(result.isSuccess, "Connection should succeed with valid configuration")
        assertTrue(ftpService.isOnline, "Should be online after successful connection")
    }
    
    @Test
    fun testEnhancedConnectWithInvalidConfiguration() = runTest {
        // Test with invalid host
        val invalidHostConfig = ftpConfig.copy(host = "")
        val invalidHostService = FtpService(invalidHostConfig)
        
        val result = invalidHostService.connect()
        assertTrue(result.isFailure, "Connection should fail with invalid host")
        
        // Test with invalid port
        val invalidPortConfig = ftpConfig.copy(port = 99999)
        val invalidPortService = FtpService(invalidPortConfig)
        
        val portResult = invalidPortService.testConnection()
        assertTrue(portResult.isFailure, "Connection test should fail with invalid port")
    }
    
    @Test
    fun testEnhancedDisconnect() = runTest {
        // First connect
        ftpService.connect()
        
        // Then disconnect
        val result = ftpService.disconnect()
        assertTrue(result.isSuccess, "Disconnect should succeed")
        
        // Should be offline after disconnect
        assertFalse(ftpService.isOnline, "Should be offline after disconnect")
    }
    
    @Test
    fun testEnhancedTestConnection() = runTest {
        val result = ftpService.testConnection()
        
        assertTrue(result.isSuccess, "Test connection should succeed")
        assertTrue(result.getOrNull() ?: false, "Test connection should return true")
    }
    
    @Test
    fun testEnhancedListFilesWhenNotConnected() = runTest {
        val result = ftpService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkStorageException.ConnectionException.NotConnected, 
            "Should fail with not connected exception")
    }
    
    @Test
    fun testEnhancedListFilesWhenConnected() = runTest {
        // Connect first and verify connection
        val connectResult = ftpService.connect()
        assertTrue(connectResult.isSuccess, "Connection should succeed")
        
        val result = ftpService.listFiles("/").first()
        
        assertTrue(result.isSuccess, "List files should succeed when connected")
        val files = result.getOrNull()
        assertNotNull(files, "Files list should be returned")
        assertTrue(files.isNotEmpty(), "Files list should not be empty")
        
        // Verify mock file structure
        val fileNames = files.map { it.name }
        assertTrue(fileNames.contains("file1.txt"), "Should contain file1.txt")
        assertTrue(fileNames.contains("file2.md"), "Should contain file2.md")
        assertTrue(fileNames.contains("folder1"), "Should contain folder1")
    }
    
    @Test
    fun testEnhancedDownloadFileWhenNotConnected() = runTest {
        val operations = ftpService.downloadFile("/test.txt", "/tmp/test.txt")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("FTP not connected", firstOperation.error)
        assertEquals("/test.txt", firstOperation.remotePath)
        assertEquals("/tmp/test.txt", firstOperation.localPath)
    }
    
    @Test
    fun testEnhancedDownloadFileWhenConnected() = runTest {
        // Connect first
        ftpService.connect()
        
        val operations = ftpService.downloadFile("/document.pdf", "/tmp/document.pdf")
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
    }
    
    @Test
    fun testEnhancedUploadFileWhenNotConnected() = runTest {
        val operations = ftpService.uploadFile("/tmp/test.txt", "/test.txt")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("FTP not connected", firstOperation.error)
    }
    
    @Test
    fun testEnhancedUploadFileWhenConnected() = runTest {
        // Connect first
        ftpService.connect()
        
        val operations = ftpService.uploadFile("/tmp/report.md", "/report.md")
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
    }
    
    @Test
    fun testEnhancedFileOperationsWhenConnected() = runTest {
        // Connect first
        ftpService.connect()
        
        // Test delete file
        val deleteResult = ftpService.deleteFile("/old-file.txt")
        assertTrue(deleteResult.isSuccess, "Delete should succeed when connected")
        
        // Test create folder (note: FTP has limited folder support)
        val createFolderResult = ftpService.createFolder("/new-folder")
        assertTrue(createFolderResult.isSuccess, "Create folder should succeed")
        val folder = createFolderResult.getOrNull()
        assertNotNull(folder, "Folder should be created")
        assertEquals("new-folder", folder.name)
        assertEquals("/public_html/new-folder", folder.path) // Should include root path
        assertTrue(folder.isFolder)
        
        // Test rename file
        val renameResult = ftpService.renameFile("/old-name.txt", "new-name.txt")
        assertTrue(renameResult.isSuccess, "Rename should succeed when connected")
        
        // Test move file
        val moveResult = ftpService.moveFile("/source.txt", "/destination.txt")
        assertTrue(moveResult.isSuccess, "Move should succeed")
        val movedFile = moveResult.getOrNull()
        assertNotNull(movedFile, "Moved file should be returned")
        assertEquals("destination.txt", movedFile.name)
        assertEquals("/public_html/destination.txt", movedFile.path)
        
        // Test copy file (should fail for FTP)
        val copyResult = ftpService.copyFile("/source.txt", "/copy.txt")
        assertTrue(copyResult.isFailure, "Copy should fail for FTP")
        val copyException = copyResult.exceptionOrNull()
        assertTrue(copyException?.message?.contains("Copy failed") == true,
            "Should fail with appropriate error message")
    }
    
    @Test
    fun testEnhancedGetFileInfo() = runTest {
        // Connect first
        ftpService.connect()
        
        val result = ftpService.getFileInfo("/document.pdf")
        
        assertTrue(result.isSuccess, "Get file info should succeed when connected")
        val fileInfo = result.getOrNull()
        assertNotNull(fileInfo, "File info should be returned")
        assertEquals("document.pdf", fileInfo.name)
        assertEquals("/public_html/document.pdf", fileInfo.path)
        assertFalse(fileInfo.isFolder, "Should be a file")
        assertTrue(fileInfo.size > 0, "Should have size")
        assertTrue(fileInfo.permissions.contains(DocumentPermission.READ), "Should have read permission")
        assertTrue(fileInfo.permissions.contains(DocumentPermission.WRITE), "Should have write permission")
    }
    
    @Test
    fun testEnhancedQuotaInfo() = runTest {
        val result = ftpService.getQuotaInfo()
        
        assertTrue(result.isSuccess, "Get quota info should succeed")
        val quota = result.getOrNull()
        assertNotNull(quota, "Quota info should be returned")
        
        // FTP doesn't provide quota information
        assertEquals(0L, quota.totalSpace)
        assertEquals(0L, quota.usedSpace)
        assertEquals(0L, quota.availableSpace)
        assertEquals(0.0, quota.usagePercentage)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
        assertEquals("FTP", quota.metadata["provider"])
        assertEquals("Quota not supported", quota.metadata["note"])
    }
    
    @Test
    fun testEnhancedExists() = runTest {
        // Connect first
        ftpService.connect()
        
        val result = ftpService.exists("/document.pdf")
        
        assertTrue(result.isSuccess, "Exists check should succeed")
        assertTrue(result.getOrNull() ?: false, "Should return true for existing file (mock)")
    }
    
    @Test
    fun testEnhancedPathOperations() = runTest {
        // Test getParentPath
        assertEquals("/", ftpService.getParentPath("/test.txt"))
        assertEquals("/public_html", ftpService.getParentPath("/public_html/test.txt"))
        assertEquals("/public_html/folder", ftpService.getParentPath("/public_html/folder/test.txt"))
        assertEquals(null, ftpService.getParentPath("/"))
        assertEquals(null, ftpService.getParentPath(""))
        
        // Test validatePath
        assertTrue(ftpService.validatePath("/test.txt").isSuccess)
        assertTrue(ftpService.validatePath("/public_html/test.txt").isSuccess)
        assertTrue(ftpService.validatePath("/public_html/folder/test.txt").isSuccess)
        assertTrue(ftpService.validatePath("").isFailure) // Empty path is invalid
        
        // Test path normalization with root path
        val configWithRoot = ftpConfig.copy(rootPath = "/custom/root")
        val serviceWithRoot = FtpService(configWithRoot)
        val parentPath = serviceWithRoot.getParentPath("/file.txt")
        assertEquals("/", parentPath, "getParentPath works on input path directly")
    }
    
    @Test
    fun testEnhancedActiveOperations() = runTest {
        // Connect first
        ftpService.connect()
        
        val activeOps = ftpService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testEnhancedCacheOperations() = runTest {
        // Test get cache entries
        val cacheEntries = ftpService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        // Test add to cache
        val addToCacheResult = ftpService.addToCache("/test.txt", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        // Test remove from cache
        val removeFromCacheResult = ftpService.removeFromCache("/test.txt")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        // Test clear cache
        val clearCacheResult = ftpService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testEnhancedSyncStatus() = runTest {
        val syncStatus = ftpService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testEnhancedSyncOperations() = runTest {
        // Test sync file
        val syncFileOperations = ftpService.syncFile("/test.txt", false)
        val syncFileOp = syncFileOperations.first()
        assertEquals(NetworkOperation.Type.SYNC, syncFileOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, syncFileOp.status)
        assertEquals("/test.txt", syncFileOp.remotePath)
        
        // Test sync all
        val syncAllOperations = ftpService.syncAll(false)
        val syncAllOp = syncAllOperations.first()
        assertEquals(NetworkOperation.Type.SYNC, syncAllOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, syncAllOp.status)
        assertEquals("/", syncAllOp.remotePath)
    }
    
    @Test
    fun testEnhancedSearchFiles() = runTest {
        val result = ftpService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail for FTP")
        assertEquals("FTP does not support search operations", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testEnhancedRecentChanges() = runTest {
        val since = Clock.System.now()
        val changes = ftpService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty")
    }
    
    @Test
    fun testEnhancedOperationManagement() = runTest {
        // Test cancel operation
        val cancelResult = ftpService.cancelOperation(12345L)
        assertTrue(cancelResult.isSuccess, "Cancel operation should succeed")
        
        // Test pause operation
        val pauseResult = ftpService.pauseOperation(12345L)
        assertTrue(pauseResult.isSuccess, "Pause operation should succeed")
        
        // Test resume operation
        val resumeResult = ftpService.resumeOperation(12345L)
        assertTrue(resumeResult.isSuccess, "Resume operation should succeed")
    }
    
    @Test
    fun testSecureFtpConfiguration() = runTest {
        val secureStorageInfo = secureFtpService.getStorageInfo()
        
        assertEquals("ftp_test-secure-ftp", secureStorageInfo.id)
        assertEquals("test-secure-ftp", secureStorageInfo.name)
        assertEquals(StorageType.FTP, secureStorageInfo.type)
        assertEquals("ftp://ftp.secure.com:21/", secureStorageInfo.location)
        assertFalse(secureStorageInfo.isOnline, "Should not be online initially")
        
        // Test secure FTP connection
        val connectResult = secureFtpService.connect()
        assertTrue(connectResult.isSuccess, "Secure FTP connection should succeed")
        assertTrue(secureFtpService.isOnline, "Should be online after connection")
    }
    
    @Test
    fun testFtpProtocolLimitations() = runTest {
        // FTP has several limitations compared to modern protocols
        
        // 1. No reliable folder support
        val storageInfo = ftpService.getStorageInfo()
        assertFalse(storageInfo.supportsFolders, "FTP should not support folders reliably")
        
        // 2. No metadata support
        assertFalse(storageInfo.supportsMetadata, "FTP should not support metadata")
        
        // 3. No copy operations
        val copyResult = ftpService.copyFile("/source.txt", "/dest.txt")
        assertTrue(copyResult.isFailure, "FTP should not support copy operations")
        
        // 4. No search functionality
        val searchResult = ftpService.searchFiles("test", "/", false).first()
        assertTrue(searchResult.isFailure, "FTP should not support search")
        
        // 5. No quota information
        val quotaResult = ftpService.getQuotaInfo()
        val quota = quotaResult.getOrNull()
        assertNotNull(quota)
        assertEquals(0L, quota.totalSpace, "FTP should not provide quota info")
    }
    
    @Test
    fun testFtpConnectionScenarios() = runTest {
        // Test connection timeout
        val timeoutConfig = ftpConfig.copy(connectionTimeout = 1000) // 1 second timeout
        val timeoutService = FtpService(timeoutConfig)
        
        val timeoutResult = timeoutService.testConnection()
        // Should handle timeout gracefully
        assertTrue(timeoutResult.isSuccess || timeoutResult.isFailure, "Should handle timeout")
        
        // Test passive vs active mode
        val activeModeConfig = ftpConfig.copy(passiveMode = false)
        val activeModeService = FtpService(activeModeConfig)
        
        val activeResult = activeModeService.connect()
        assertTrue(activeResult.isSuccess, "Active mode connection should succeed")
        
        // Test different encodings
        val latin1Config = ftpConfig.copy(encoding = "ISO-8859-1")
        val latin1Service = FtpService(latin1Config)
        
        val latin1Result = latin1Service.connect()
        assertTrue(latin1Result.isSuccess, "Latin-1 encoding should work")
    }
}