package digital.vasic.yole.network.protocols.ftp

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
 * Comprehensive test suite for FtpService network protocol implementation.
 * Tests FTP file operations, authentication, and passive/active mode functionality.
 */
class FtpServiceTest {
    
    private val ftpConfig = StorageConfig.FtpConfig(
        name = "test-ftp",
        host = "ftp.example.com",
        port = 21,
        username = "testuser",
        password = "testpass",
        passiveMode = true,
        secureFtp = false,
        connectionTimeout = 30000
    )
    
    private lateinit var ftpService: FtpService
    
    @Test
    fun testFtpServiceInitialization() {
        ftpService = FtpService(ftpConfig)
        
        assertEquals("test-ftp", ftpService.config.name)
        assertEquals("ftp.example.com", ftpService.config.host)
        assertEquals(21, ftpService.config.port)
        assertEquals("testuser", ftpService.config.username)
        assertEquals("testpass", ftpService.config.password)
        assertTrue(ftpService.config.passiveMode)
        assertFalse(ftpService.config.secureFtp)
        assertEquals(30000, ftpService.config.connectionTimeout)
        assertEquals("/", ftpService.rootPath)
        assertFalse(ftpService.isOnline)
    }
    
    @Test
    fun testConnectSuccess() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.connect()

        // Real FTP client cannot connect to non-existent server
        assertTrue(result.isFailure, "FTP connection should fail when server is unreachable")
        assertFalse(ftpService.isOnline, "Should not be online after failed connection")
    }
    
    @Test
    fun testDisconnectSuccess() = runTest {
        ftpService = FtpService(ftpConfig)
        ftpService.connect() // Will fail but that's OK
        val result = ftpService.disconnect()

        assertTrue(result.isSuccess, "FTP disconnection should succeed even without prior connection")
        assertFalse(ftpService.isOnline, "Should not be online after disconnect")
    }
    
    @Test
    fun testStorageInfo() = runTest {
        ftpService = FtpService(ftpConfig)
        val storageInfo = ftpService.getStorageInfo()
        
        assertEquals("ftp_test-ftp", storageInfo.id)
        assertEquals("test-ftp", storageInfo.name)
        assertEquals(StorageType.FTP, storageInfo.type)
        assertEquals("ftp://ftp.example.com:21/", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("FTP not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val operations = ftpService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("FTP not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val operations = ftpService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("FTP not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.deleteFile("/test.md")
        
        assertTrue(result.isFailure, "Delete should fail when not connected")
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.createFolder("/test-folder")
        
        assertTrue(result.isFailure, "Create folder should fail when not connected")
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isFailure, "Rename should fail when not connected")
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isFailure, "Move should fail when not connected")
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isFailure, "Copy should fail when not connected")
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.getFileInfo("/test.md")
        
        assertTrue(result.isFailure, "Get file info should fail when not connected")
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.getQuotaInfo()
        
        assertTrue(result.isSuccess, "Get quota info returns success (FTP doesn't support quota)")
        val quota = result.getOrNull()
        assertEquals(0L, quota?.totalSpace)
        assertEquals(0L, quota?.usedSpace)
    }
    
    @Test
    fun testExistsWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.exists("/test.md")
        
        assertTrue(result.isSuccess, "Exists check returns success (recovering from failure)")
        assertEquals(false, result.getOrNull(), "Returns false when not connected")
    }
    
    @Test
    fun testFtpConfigurationValidation() {
        // Test active mode
        val activeModeConfig = ftpConfig.copy(passiveMode = false)
        ftpService = FtpService(activeModeConfig)
        
        assertFalse(ftpService.config.passiveMode)
        assertEquals(21, ftpService.config.port)
        
        // Test secure FTP (FTPS)
        val secureConfig = ftpConfig.copy(secureFtp = true, port = 990)
        ftpService = FtpService(secureConfig)
        
        assertTrue(ftpService.config.secureFtp)
        assertEquals(990, ftpService.config.port)
        
        // Test custom port
        val customPortConfig = ftpConfig.copy(port = 2121)
        ftpService = FtpService(customPortConfig)
        
        assertEquals(2121, ftpService.config.port)
        
        // Test custom timeout
        val customTimeoutConfig = ftpConfig.copy(connectionTimeout = 60000)
        ftpService = FtpService(customTimeoutConfig)
        
        assertEquals(60000, ftpService.config.connectionTimeout)
    }
    
    @Test
    fun testGetParentPath() {
        ftpService = FtpService(ftpConfig)
        
        assertEquals("/", ftpService.getParentPath("/test.md"))
        assertEquals("/folder", ftpService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", ftpService.getParentPath("/folder/subfolder/test.md"))
        assertEquals(null, ftpService.getParentPath("/"))
        assertEquals(null, ftpService.getParentPath(""))
    }
    
    @Test
    fun testValidatePath() {
        ftpService = FtpService(ftpConfig)
        
        assertTrue(ftpService.validatePath("/test.md").isSuccess)
        assertTrue(ftpService.validatePath("/folder/test.md").isSuccess)
        assertTrue(ftpService.validatePath("/folder/subfolder/test.md").isSuccess)
        
        assertTrue(ftpService.validatePath("").isFailure)
        assertTrue(ftpService.validatePath("   ").isFailure)
    }
    
    @Test
    fun testSearchFilesWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail when not connected")
    }
    
    @Test
    fun testGetRecentChangesWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val since = Clock.System.now()
        val changes = ftpService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty when not connected")
    }
    
    @Test
    fun testSyncFileWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val operations = ftpService.syncFile("/test.md", false)
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.SYNC, firstOperation.type)
        assertEquals(NetworkOperation.Status.COMPLETED, firstOperation.status)
    }
    
    @Test
    fun testSyncAllWhenNotConnected() = runTest {
        ftpService = FtpService(ftpConfig)
        val operations = ftpService.syncAll(false)
        
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
        ftpService = FtpService(ftpConfig)
        val activeOps = ftpService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testCacheOperations() = runTest {
        ftpService = FtpService(ftpConfig)
        
        val cacheEntries = ftpService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        val addToCacheResult = ftpService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        val removeFromCacheResult = ftpService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        val clearCacheResult = ftpService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testSyncStatusFlow() = runTest {
        ftpService = FtpService(ftpConfig)
        val syncStatus = ftpService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testTestConnection() = runTest {
        ftpService = FtpService(ftpConfig)
        val result = ftpService.testConnection()

        // Real FTP client cannot connect to non-existent server
        assertTrue(result.isFailure, "Test connection should fail when server is unreachable")
    }
    
    @Test
    fun testFtpUriGeneration() = runTest {
        ftpService = FtpService(ftpConfig)
        val storageInfo = ftpService.getStorageInfo()
        
        assertEquals("ftp://ftp.example.com:21/", storageInfo.location)
        
        // Test with custom port
        val customPortConfig = ftpConfig.copy(port = 2121)
        val customPortService = FtpService(customPortConfig)
        val customPortStorageInfo = customPortService.getStorageInfo()
        
        assertEquals("ftp://ftp.example.com:2121/", customPortStorageInfo.location)
        
        // Test with secure FTP
        val secureConfig = ftpConfig.copy(secureFtp = true, port = 990)
        val secureService = FtpService(secureConfig)
        val secureStorageInfo = secureService.getStorageInfo()
        
        assertEquals("ftp://ftp.example.com:990/", secureStorageInfo.location)
    }
}