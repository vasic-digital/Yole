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
        assertEquals("smb://192.168.1.100/shared", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val operations = smbService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val operations = smbService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.deleteFile("/test.md")
        
        assertTrue(result.isFailure, "Delete should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.createFolder("/test-folder")
        
        assertTrue(result.isFailure, "Create folder should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isFailure, "Rename should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isFailure, "Move should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isFailure, "Copy should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.getFileInfo("/test.md")
        
        assertTrue(result.isFailure, "Get file info should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.getQuotaInfo()
        
        assertTrue(result.isFailure, "Get quota info should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testExistsWhenNotConnected() = runTest {
        smbService = SmbService(smbConfig)
        val result = smbService.exists("/test.md")
        
        assertTrue(result.isFailure, "Exists check should fail when not connected")
        assertEquals("Not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testSmbConfigurationValidation() {
        val configWithSSL = smbConfig.copy(useSsl = true, port = 445)
        smbService = SmbService(configWithSSL)
        
        assertEquals("smb://192.168.1.100", smbService.config.host)
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
        assertEquals("Not connected", result.exceptionOrNull()?.message)
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
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.SYNC, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("Not connected", firstOperation.error)
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
        assertFalse(result.getOrNull() ?: true, "Connection should be false when not connected")
    }
}