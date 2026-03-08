package digital.vasic.yole.network.protocols.sftp

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
 * Comprehensive test suite for SftpService network protocol implementation.
 * Tests SFTP file operations, SSH authentication, and secure file transfer functionality.
 */
class SftpServiceTest {
    
    private val sftpConfig = StorageConfig.SftpConfig(
        name = "test-sftp",
        host = "sftp.example.com",
        port = 22,
        username = "testuser",
        password = "testpass",
        privateKeyPath = "/path/to/private/key",
        knownHostsPath = "/path/to/known_hosts",
        strictHostKeyChecking = true,
        connectionTimeout = 30000
    )
    
    private lateinit var sftpService: SftpService
    
    @Test
    fun testSftpServiceInitialization() {
        sftpService = SftpService(sftpConfig)
        
        assertEquals("test-sftp", sftpService.config.name)
        assertEquals("sftp.example.com", sftpService.config.host)
        assertEquals(22, sftpService.config.port)
        assertEquals("testuser", sftpService.config.username)
        assertEquals("testpass", sftpService.config.password)
        assertEquals("/path/to/private/key", sftpService.config.privateKeyPath)
        assertEquals("/path/to/known_hosts", sftpService.config.knownHostsPath)
        assertTrue(sftpService.config.strictHostKeyChecking)
        assertEquals(30000, sftpService.config.connectionTimeout)
        assertEquals("/", sftpService.rootPath)
        assertFalse(sftpService.isOnline)
    }
    
    @Test
    fun testConnectSuccess() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.connect()
        
        assertTrue(result.isSuccess, "SFTP connection should succeed")
    }
    
    @Test
    fun testDisconnectSuccess() = runBlocking {
        sftpService = SftpService(sftpConfig)
        sftpService.connect()
        val result = sftpService.disconnect()
        
        assertTrue(result.isSuccess, "SFTP disconnection should succeed")
    }
    
    @Test
    fun testStorageInfo() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val storageInfo = sftpService.getStorageInfo()
        
        assertEquals("sftp_test-sftp", storageInfo.id)
        assertEquals("test-sftp", storageInfo.name)
        assertEquals(StorageType.SFTP, storageInfo.type)
        assertEquals("sftp://sftp.example.com:22/", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("SFTP not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val operations = sftpService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("SFTP not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val operations = sftpService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("SFTP not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.deleteFile("/test.md")
        
        assertTrue(result.isFailure, "Delete should fail when not connected")
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.createFolder("/test-folder")
        
        assertTrue(result.isFailure, "Create folder should fail when not connected")
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isFailure, "Rename should fail when not connected")
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isFailure, "Move should fail when not connected")
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isFailure, "Copy should fail when not connected")
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.getFileInfo("/test.md")
        
        assertTrue(result.isFailure, "Get file info should fail when not connected")
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.getQuotaInfo()
        
        assertTrue(result.isSuccess, "Get quota info returns success (SFTP doesn't support quota)")
        val quota = result.getOrNull()
        assertEquals(0L, quota?.totalSpace)
        assertEquals(0L, quota?.usedSpace)
    }
    
    @Test
    fun testExistsWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.exists("/test.md")
        
        assertTrue(result.isSuccess, "Exists check returns success (recovering from failure)")
        assertEquals(false, result.getOrNull(), "Returns false when not connected")
    }
    
    @Test
    fun testSftpConfigurationValidation() {
        // Test with password-only authentication
        val passwordOnlyConfig = sftpConfig.copy(
            privateKeyPath = null,
            knownHostsPath = null,
            strictHostKeyChecking = false
        )
        sftpService = SftpService(passwordOnlyConfig)
        
        assertEquals(null, sftpService.config.privateKeyPath)
        assertEquals(null, sftpService.config.knownHostsPath)
        assertFalse(sftpService.config.strictHostKeyChecking)
        
        // Test with key-based authentication
        val keyAuthConfig = sftpConfig.copy(
            password = null,
            privateKeyPath = "/home/user/.ssh/id_rsa",
            knownHostsPath = "/home/user/.ssh/known_hosts"
        )
        sftpService = SftpService(keyAuthConfig)
        
        assertEquals(null, sftpService.config.password)
        assertEquals("/home/user/.ssh/id_rsa", sftpService.config.privateKeyPath)
        assertEquals("/home/user/.ssh/known_hosts", sftpService.config.knownHostsPath)
        
        // Test custom port
        val customPortConfig = sftpConfig.copy(port = 2222)
        sftpService = SftpService(customPortConfig)
        
        assertEquals(2222, sftpService.config.port)
        
        // Test custom timeout
        val customTimeoutConfig = sftpConfig.copy(connectionTimeout = 60000)
        sftpService = SftpService(customTimeoutConfig)
        
        assertEquals(60000, sftpService.config.connectionTimeout)
    }
    
    @Test
    fun testGetParentPath() {
        sftpService = SftpService(sftpConfig)
        
        assertEquals("/", sftpService.getParentPath("/test.md"))
        assertEquals("/folder", sftpService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", sftpService.getParentPath("/folder/subfolder/test.md"))
        assertEquals(null, sftpService.getParentPath("/"))
        assertEquals(null, sftpService.getParentPath(""))
    }
    
    @Test
    fun testValidatePath() {
        sftpService = SftpService(sftpConfig)
        
        assertTrue(sftpService.validatePath("/test.md").isSuccess)
        assertTrue(sftpService.validatePath("/folder/test.md").isSuccess)
        assertTrue(sftpService.validatePath("/folder/subfolder/test.md").isSuccess)
        
        assertTrue(sftpService.validatePath("").isFailure)
        assertTrue(sftpService.validatePath("   ").isFailure)
    }
    
    @Test
    fun testSearchFilesWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail when not connected")
    }
    
    @Test
    fun testGetRecentChangesWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val since = Clock.System.now()
        val changes = sftpService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty when not connected")
    }
    
    @Test
    fun testSyncFileWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val operations = sftpService.syncFile("/test.md", false)
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.SYNC, firstOperation.type)
        assertEquals(NetworkOperation.Status.COMPLETED, firstOperation.status)
    }
    
    @Test
    fun testSyncAllWhenNotConnected() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val operations = sftpService.syncAll(false)
        
        // Should return one completed operation
        var operationCount = 0
        operations.collect { operation ->
            operationCount++
            assertEquals(NetworkOperation.Status.COMPLETED, operation.status)
        }
        assertEquals(1, operationCount, "Sync all should return one completed operation")
    }
    
    @Test
    fun testActiveOperationsFlow() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val activeOps = sftpService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testCacheOperations() = runBlocking {
        sftpService = SftpService(sftpConfig)
        
        val cacheEntries = sftpService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        val addToCacheResult = sftpService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        val removeFromCacheResult = sftpService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        val clearCacheResult = sftpService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testSyncStatusFlow() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val syncStatus = sftpService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testTestConnection() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val result = sftpService.testConnection()
        
        assertTrue(result.isSuccess, "Test connection should complete successfully")
        assertTrue(result.getOrNull() ?: false, "Connection should be true (mock implementation)")
    }
    
    @Test
    fun testSftpUriGeneration() = runBlocking {
        sftpService = SftpService(sftpConfig)
        val storageInfo = sftpService.getStorageInfo()
        
        assertEquals("sftp://sftp.example.com:22/", storageInfo.location)
        
        // Test with custom port
        val customPortConfig = sftpConfig.copy(port = 2222)
        val customPortService = SftpService(customPortConfig)
        val customPortStorageInfo = customPortService.getStorageInfo()
        
        assertEquals("sftp://sftp.example.com:2222/", customPortStorageInfo.location)
        
        // Test with IPv6 address
        val ipv6Config = sftpConfig.copy(host = "2001:db8::1")
        val ipv6Service = SftpService(ipv6Config)
        val ipv6StorageInfo = ipv6Service.getStorageInfo()
        
        assertEquals("sftp://2001:db8::1:22/", ipv6StorageInfo.location)
    }
    
    @Test
    fun testAuthenticationMethods() {
        // Test password authentication
        val passwordConfig = sftpConfig.copy(
            password = "password123",
            privateKeyPath = null
        )
        val passwordService = SftpService(passwordConfig)
        assertEquals("password123", passwordService.config.password)
        assertEquals(null, passwordService.config.privateKeyPath)
        
        // Test key authentication
        val keyConfig = sftpConfig.copy(
            password = null,
            privateKeyPath = "/path/to/key"
        )
        val keyService = SftpService(keyConfig)
        assertEquals(null, keyService.config.password)
        assertEquals("/path/to/key", keyService.config.privateKeyPath)
        
        // Test both authentication methods
        val bothConfig = sftpConfig.copy(
            password = "password123",
            privateKeyPath = "/path/to/key"
        )
        val bothService = SftpService(bothConfig)
        assertEquals("password123", bothService.config.password)
        assertEquals("/path/to/key", bothService.config.privateKeyPath)
    }
}