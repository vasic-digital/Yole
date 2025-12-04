package digital.vasic.yole.network.protocols.webdav

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
 * Comprehensive test suite for WebDavService network protocol implementation.
 * Tests WebDAV file operations, authentication, and web server integration functionality.
 */
class WebDavServiceTest {
    
    private val webDavConfig = StorageConfig.WebDavConfig(
        name = "test-webdav",
        url = "https://webdav.example.com/remote.php/dav/files/username/",
        username = "testuser",
        password = "testpass",
        verifyCertificate = false,
        connectionTimeout = 30000
    )
    
    private lateinit var webDavService: WebDavService
    
    @Test
    fun testWebDavServiceInitialization() {
        webDavService = WebDavService(webDavConfig)
        
        assertEquals("test-webdav", webDavService.config.name)
        assertEquals("https://webdav.example.com/remote.php/dav/files/username/", webDavService.config.url)
        assertEquals("testuser", webDavService.config.username)
        assertEquals("testpass", webDavService.config.password)
        assertFalse(webDavService.config.verifyCertificate)
        assertEquals(30000, webDavService.config.connectionTimeout)
        assertEquals("/", webDavService.rootPath)
        assertFalse(webDavService.isOnline)
    }
    
    @Test
    fun testConnectSuccess() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.connect()
        
        assertTrue(result.isSuccess, "WebDAV connection should succeed")
    }
    
    @Test
    fun testDisconnectSuccess() = runTest {
        webDavService = WebDavService(webDavConfig)
        webDavService.connect()
        val result = webDavService.disconnect()
        
        assertTrue(result.isSuccess, "WebDAV disconnection should succeed")
    }
    
    @Test
    fun testStorageInfo() = runTest {
        webDavService = WebDavService(webDavConfig)
        val storageInfo = webDavService.getStorageInfo()
        
        assertEquals("webdav_test-webdav", storageInfo.id)
        assertEquals("test-webdav", storageInfo.name)
        assertEquals(StorageType.WEBDAV, storageInfo.type)
        assertEquals("https://webdav.example.com/remote.php/dav/files/username/", storageInfo.location)
    }
    
    @Test
    fun testListFilesWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.listFiles("/").first()
        
        assertTrue(result.isFailure, "List files should fail when not connected")
        assertEquals("WebDAV not connected", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testDownloadFileWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val operations = webDavService.downloadFile("/test.md", "/tmp/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("WebDAV not connected", firstOperation.error)
    }
    
    @Test
    fun testUploadFileWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val operations = webDavService.uploadFile("/tmp/test.md", "/test.md")
        
        val firstOperation = operations.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOperation.type)
        assertEquals(NetworkOperation.Status.FAILED, firstOperation.status)
        assertEquals("WebDAV not connected", firstOperation.error)
    }
    
    @Test
    fun testDeleteFileWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.deleteFile("/test.md")
        
        assertTrue(result.isSuccess, "Delete should succeed even when not connected (WebDAV pattern)")
    }
    
    @Test
    fun testCreateFolderWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.createFolder("/test-folder")
        
        assertTrue(result.isSuccess, "Create folder should succeed even when not connected (WebDAV pattern)")
        assertEquals("test-folder", result.getOrNull()?.name)
        assertEquals("/test-folder", result.getOrNull()?.path)
        assertTrue(result.getOrNull()?.isFolder == true)
    }
    
    @Test
    fun testRenameFileWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.renameFile("/test.md", "renamed.md")
        
        assertTrue(result.isSuccess, "Rename should succeed even when not connected (WebDAV pattern)")
    }
    
    @Test
    fun testMoveFileWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.moveFile("/test.md", "/moved/test.md")
        
        assertTrue(result.isSuccess, "Move should succeed even when not connected (WebDAV pattern)")
        assertEquals("test.md", result.getOrNull()?.name)
        assertEquals("/moved/test.md", result.getOrNull()?.path)
        assertTrue(result.getOrNull()?.isFolder == false)
    }
    
    @Test
    fun testCopyFileWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.copyFile("/test.md", "/copy/test.md")
        
        assertTrue(result.isSuccess, "Copy should succeed even when not connected (WebDAV pattern)")
    }
    
    @Test
    fun testGetFileInfoWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.getFileInfo("/test.md")
        
        assertTrue(result.isSuccess, "Get file info should succeed even when not connected (WebDAV pattern)")
        assertEquals("test.md", result.getOrNull()?.name)
        assertEquals("/test.md", result.getOrNull()?.path)
        assertTrue(result.getOrNull()?.isFolder == false)
    }
    
    @Test
    fun testGetQuotaInfoWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.getQuotaInfo()
        
        assertTrue(result.isSuccess, "Get quota info should succeed even when not connected (WebDAV pattern)")
        val quota = result.getOrNull()
        assertEquals(1000000000L, quota?.totalSpace)
        assertEquals(100000000L, quota?.usedSpace)
        assertEquals(900000000L, quota?.availableSpace)
        assertEquals(0.1, quota?.usagePercentage)
        assertFalse(quota?.isFull ?: true)
        assertFalse(quota?.isLowOnSpace ?: true)
    }
    
    @Test
    fun testExistsWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.exists("/test.md")
        
        assertTrue(result.isSuccess, "Exists should succeed even when not connected (WebDAV pattern)")
        assertFalse(result.getOrNull() ?: true, "Exists should return false (WebDAV mock implementation)")
    }
    
    @Test
    fun testWebDavConfigurationValidation() {
        val configWithTrustAll = webDavConfig.copy(verifyCertificate = false)
        webDavService = WebDavService(configWithTrustAll)
        
        assertEquals("https://webdav.example.com/remote.php/dav/files/username/", webDavService.config.url)
        assertFalse(webDavService.config.verifyCertificate)
        
        val configWithCustomTimeout = webDavConfig.copy(connectionTimeout = 60000)
        webDavService = WebDavService(configWithCustomTimeout)
        
        assertEquals(60000, webDavService.config.connectionTimeout)
    }
    
    @Test
    fun testGetParentPath() {
        webDavService = WebDavService(webDavConfig)
        
        assertEquals("/", webDavService.getParentPath("/test.md"))
        assertEquals("/folder", webDavService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", webDavService.getParentPath("/folder/subfolder/test.md"))
        assertEquals(null, webDavService.getParentPath("/"))
        assertEquals(null, webDavService.getParentPath(""))
    }
    
    @Test
    fun testValidatePath() {
        webDavService = WebDavService(webDavConfig)
        
        assertTrue(webDavService.validatePath("/test.md").isSuccess)
        assertTrue(webDavService.validatePath("/folder/test.md").isSuccess)
        assertTrue(webDavService.validatePath("/folder/subfolder/test.md").isSuccess)
        
        assertTrue(webDavService.validatePath("").isFailure)
        assertTrue(webDavService.validatePath("   ").isFailure)
    }
    
    @Test
    fun testSearchFilesWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.searchFiles("test", "/", false).first()
        
        assertTrue(result.isFailure, "Search should fail with not implemented error")
        assertEquals("WebDAV search not implemented", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun testGetRecentChangesWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val since = Clock.System.now()
        val changes = webDavService.getRecentChanges(since, "/").first()
        
        assertTrue(changes.isEmpty(), "Recent changes should be empty when not connected")
    }
    
    @Test
    fun testSyncFileWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val operations = webDavService.syncFile("/test.md", false)
        
        // WebDAV syncFile returns progress updates ending with COMPLETED
        val operationList = mutableListOf<NetworkOperation>()
        operations.collect { operation ->
            operationList.add(operation)
        }
        
        assertTrue(operationList.isNotEmpty(), "Should emit operations")
        assertEquals(NetworkOperation.Type.SYNC, operationList.first().type)
        assertEquals(NetworkOperation.Status.COMPLETED, operationList.last().status)
        assertEquals(1.0, operationList.last().progress)
    }
    
    @Test
    fun testSyncAllWhenNotConnected() = runTest {
        webDavService = WebDavService(webDavConfig)
        val operations = webDavService.syncAll(false)
        
        // Should return empty flow when not connected
        var operationCount = 0
        operations.collect { operation ->
            operationCount++
        }
        assertEquals(0, operationCount, "Sync all should return empty when not connected")
    }
    
    @Test
    fun testActiveOperationsFlow() = runTest {
        webDavService = WebDavService(webDavConfig)
        val activeOps = webDavService.getActiveOperations().first()
        
        assertTrue(activeOps.isEmpty(), "Active operations should be empty initially")
    }
    
    @Test
    fun testCacheOperations() = runTest {
        webDavService = WebDavService(webDavConfig)
        
        val cacheEntries = webDavService.getCacheEntries("/").first()
        assertTrue(cacheEntries.isEmpty(), "Cache entries should be empty")
        
        val addToCacheResult = webDavService.addToCache("/test.md", 1)
        assertTrue(addToCacheResult.isSuccess, "Add to cache should succeed")
        
        val removeFromCacheResult = webDavService.removeFromCache("/test.md")
        assertTrue(removeFromCacheResult.isSuccess, "Remove from cache should succeed")
        
        val clearCacheResult = webDavService.clearCache()
        assertTrue(clearCacheResult.isSuccess, "Clear cache should succeed")
    }
    
    @Test
    fun testSyncStatusFlow() = runTest {
        webDavService = WebDavService(webDavConfig)
        val syncStatus = webDavService.getSyncStatus("/").first()
        
        assertTrue(syncStatus.isEmpty(), "Sync status should be empty initially")
    }
    
    @Test
    fun testTestConnection() = runTest {
        webDavService = WebDavService(webDavConfig)
        val result = webDavService.testConnection()
        
        assertTrue(result.isSuccess, "Test connection should complete successfully")
        assertTrue(result.getOrNull() ?: false, "Connection should be true (WebDAV attempts actual connection)")
    }
    
    @Test
    fun testDifferentWebDavServers() = runTest {
        // Test Nextcloud URL
        val nextcloudConfig = webDavConfig.copy(url = "https://nextcloud.example.com/remote.php/dav/files/username/")
        val nextcloudService = WebDavService(nextcloudConfig)
        assertEquals(StorageType.WEBDAV, nextcloudService.getStorageInfo().type)
        
        // Test Owncloud URL
        val owncloudConfig = webDavConfig.copy(url = "https://owncloud.example.com/remote.php/dav/files/username/")
        val owncloudService = WebDavService(owncloudConfig)
        assertEquals(StorageType.WEBDAV, owncloudService.getStorageInfo().type)
        
        // Test Generic WebDAV
        val genericConfig = webDavConfig.copy(url = "https://dav.example.com/files/")
        val genericService = WebDavService(genericConfig)
        assertEquals(StorageType.WEBDAV, genericService.getStorageInfo().type)
    }
}