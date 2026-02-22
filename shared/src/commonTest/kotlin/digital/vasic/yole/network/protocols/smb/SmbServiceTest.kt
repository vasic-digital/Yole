package digital.vasic.yole.network.protocols.smb

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
 * Comprehensive test suite for SmbService network protocol implementation.
 * Tests SMB file operations, authentication, and Windows network sharing functionality.
 */
class SmbServiceTest {
    
    private val smbConfig = StorageConfig.SmbConfig(
        name = "test-smb",
        host = "192.168.1.100",
        share = "shared",
        domain = "WORKGROUP",
        username = "testuser",
        password = "testpass",
        port = 445,
        useSsl = false,
        connectionTimeout = 30000
    )
    
    private lateinit var smbService: SmbService
    
    @Test
    fun testSmbServiceInitialization() {
        smbService = SmbService(smbConfig)
        
        assertEquals("test-smb", smbService.config.name)
        assertEquals("192.168.1.100", smbService.config.host)
        assertEquals("shared", smbService.config.share)
        assertEquals("WORKGROUP", smbService.config.domain)
        assertEquals("testuser", smbService.config.username)
        assertEquals(445, smbService.config.port)
        assertFalse(smbService.config.useSsl)
        assertEquals("/", smbService.rootPath)
        assertFalse(smbService.isOnline)
    }
    
    @Test
    fun testConnectSuccess() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.connect()
        
        assertTrue(result.isSuccess, "SMB connection should succeed")
    }
    
    @Test
    fun testDisconnectSuccess() = runTest {
        smbService = SmbService(smbConfig)
        smbService.connect()
        val result = smbService.disconnect()
        
        assertTrue(result.isSuccess, "SMB disconnection should succeed")
    }
    
    @Test
    fun testStorageInfo() = runTest {
        smbService = SmbService(smbConfig)
        val storageInfo = smbService.getStorageInfo()
        
        assertEquals("smb_test-smb", storageInfo.id)
        assertEquals("test-smb", storageInfo.name)
        assertEquals(StorageType.SMB, storageInfo.type)
        assertEquals("smb://192.168.1.100/shared/", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("SMB not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val operations = smbService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("SMB not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val operations = smbService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("SMB not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.deleteFile("/test.md")
        
        assertTrue(result.isSuccess, "Delete should succeed even when not connected (SMB implementation)")
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.createFolder("/test-folder")
        
        assertTrue(result.isSuccess, "Create folder should succeed even when not connected (SMB implementation)")
        val document = result.getOrNull()
        assertEquals("test-folder", document?.name)
        assertEquals("/test-folder", document?.path)
        assertTrue(document?.isFolder ?: false)
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isSuccess, "Rename should succeed even when not connected (SMB implementation)")
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isSuccess, "Move should succeed even when not connected (SMB implementation)")
        val document = result.getOrNull()
        assertEquals("test.md", document?.name)
        assertEquals("/moved/test.md", document?.path)
        assertFalse(document?.isFolder ?: true)
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isSuccess, "Copy should succeed even when not connected (SMB implementation)")
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.getFileInfo("/test.md")
        
        assertTrue(result.isSuccess, "Get file info should succeed even when not connected (SMB implementation)")
        val document = result.getOrNull()
        assertEquals("test.md", document?.name)
        assertEquals("/test.md", document?.path)
        assertFalse(document?.isFolder ?: true)
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.getQuotaInfo()
        
        assertTrue(result.isSuccess, "Get quota info returns mock success even when not connected")
        val quota = result.getOrNull()
        assertEquals(1000000000L, quota?.totalSpace)
        assertEquals(100000000L, quota?.usedSpace)
        assertEquals(900000000L, quota?.availableSpace)
        assertEquals(0.1, quota?.usagePercentage)
    }
    
    @Test
    fun testExistsWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.exists("/test.md")
        
        assertTrue(result.isSuccess, "Exists check returns mock success even when not connected")
        assertEquals(false, result.getOrNull(), "Mock implementation returns false")
    }
    
    @Test
    fun testSmbConfigurationValidation() {
        val configWithSSL = smbConfig.copy(useSsl = true, port = 445)
        smbService = SmbService(configWithSSL)
        
        assertEquals("192.168.1.100", smbService.config.host)
        assertTrue(smbService.config.useSsl)
        assertEquals(445, smbService.config.port)
        
        val configWithCustomPort = smbConfig.copy(port = 139)
        smbService = SmbService(configWithCustomPort)
        
        assertEquals(139, smbService.config.port)
    }
    
    @Test
    fun testGetParentPath() {
        smbService = SmbService(smbConfig)
        
        assertEquals("/", smbService.getParentPath("/test.md"))
        assertEquals("/", smbService.getParentPath("/folder/")) // trailing slash means parent is root
        assertEquals("/folder", smbService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", smbService.getParentPath("/folder/subfolder/test.md"))
        assertEquals(null, smbService.getParentPath("/"))
        assertEquals(null, smbService.getParentPath(""))
    }
    
    @Test
    fun testValidatePath() {
        smbService = SmbService(smbConfig)
        
        assertTrue(smbService.validatePath("/test.md").isSuccess)
        assertTrue(smbService.validatePath("/folder/test.md").isSuccess)
        assertTrue(smbService.validatePath("/folder/subfolder/test.md").isSuccess)
        
        assertTrue(smbService.validatePath("").isFailure)
        assertTrue(smbService.validatePath("   ").isFailure)
    }
    
    @Test
    fun testSearchFilesWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail when not connected")
        assertEquals("SMB search not implemented", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetRecentChangesWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val since = Clock.System.now()
        val changes = smbService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty when not connected")
    }
    
    @Test
    fun testSyncFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val operations = smbService.syncFile("/test.md", false)
        
        // Should receive multiple progress updates
        val operationsList = mutableListOf<NetworkOperation>()
        operations.collect { operation ->
            operationsList.add(operation)
        }
        
        assertEquals(4, operationsList.size, "Should receive 4 operations: IN_PROGRESS(0), IN_PROGRESS(0.5), IN_PROGRESS(1.0), COMPLETED")
        
        assertEquals(NetworkOperation.Status.IN_PROGRESS, operationsList[0].status)
        assertEquals(0.0, operationsList[0].progress)
        
        assertEquals(NetworkOperation.Status.IN_PROGRESS, operationsList[1].status)
        assertEquals(0.5, operationsList[1].progress)
        
        assertEquals(NetworkOperation.Status.IN_PROGRESS, operationsList[2].status)
        assertEquals(1.0, operationsList[2].progress)
        
        assertEquals(NetworkOperation.Status.COMPLETED, operationsList[3].status)
        assertEquals(1.0, operationsList[3].progress)
    }
    
    @Test
    fun testSyncAllWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val operations = smbService.syncAll(false)
        
        // Should return empty flow when not connected
        var operationCount = 0
        operations.collect { operation ->
            operationCount++
        }
        assertEquals(0, operationCount, "Sync all should return empty when not connected")
    }
    
    @Test
    fun testActiveOperationsFlow() = runTest {
        smbService = SmbService(smbConfig)
        val activeOps = smbService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testCacheOperations() = runTest {
        smbService = SmbService(smbConfig)
        
        val cacheEntries = smbService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        val addToCacheResult = smbService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        val removeFromCacheResult = smbService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        val clearCacheResult = smbService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testSyncStatusFlow() = runTest {
        smbService = SmbService(smbConfig)
        val syncStatus = smbService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testTestConnection() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.testConnection()
        
        assertTrue(result.isSuccess, "Test connection should complete successfully")
        assertTrue(result.getOrNull() ?: false, "Connection should be true (SMB testConnection attempts to connect)")
    }
}