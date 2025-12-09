package digital.vasic.yole.network.protocol

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
 * Test suite for MockNetworkStorageService in the protocol package.
 * Tests all mock implementations of network storage operations.
 */
class MockNetworkStorageServiceTest {

    private val mockConfig = StorageConfig.WebDavConfig(
        name = "test-mock",
        url = "https://mock.example.com",
        username = "testuser",
        password = "testpass"
    )

    private lateinit var mockService: MockNetworkStorageService

    @Test
    fun testMockServiceInitialization() {
        mockService = MockNetworkStorageService(mockConfig)

        assertEquals("test-mock", mockService.config.name)
        assertEquals("/", mockService.rootPath)
        assertFalse(mockService.isOnline)
    }

    @Test
    fun testConnectDisconnect() = runTest {
        mockService = MockNetworkStorageService(mockConfig)

        // Test connect
        val connectResult = mockService.connect()
        assertTrue(connectResult.isSuccess, "Connect should succeed")
        assertTrue(mockService.isOnline, "Service should be online after connect")

        // Test disconnect
        val disconnectResult = mockService.disconnect()
        assertTrue(disconnectResult.isSuccess, "Disconnect should succeed")
        assertFalse(mockService.isOnline, "Service should be offline after disconnect")
    }

    @Test
    fun testListFiles() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val files = mockService.listFiles("/").first()
        assertTrue(files.isSuccess, "List files should succeed when connected")

        val fileList = files.getOrNull()
        assertEquals(1, fileList?.size, "Should return one mock file")
        assertEquals("Test File.md", fileList?.first()?.name)
        assertEquals("/Test File.md", fileList?.first()?.path)
        assertFalse(fileList?.first()?.isFolder ?: true)
    }

    @Test
    fun testDownloadFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val operations = mockService.downloadFile("/remote.md", "/local.md")

        var operationCount = 0
        operations.collect { operation ->
            operationCount++
            when (operationCount) {
                1 -> {
                    assertEquals(NetworkOperation.Type.DOWNLOAD, operation.type)
                    assertEquals(NetworkOperation.Status.COMPLETED, operation.status)
                    assertEquals("/remote.md", operation.remotePath)
                    assertEquals("/local.md", operation.localPath)
                    assertEquals(1.0, operation.progress)
                    assertEquals(1024L, operation.totalSize)
                    assertEquals(1024L, operation.bytesTransferred)
                }
            }
        }
        assertEquals(1, operationCount, "Should emit one completed operation")
    }

    @Test
    fun testUploadFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val operations = mockService.uploadFile("/local.md", "/remote.md")

        var operationCount = 0
        operations.collect { operation ->
            operationCount++
            when (operationCount) {
                1 -> {
                    assertEquals(NetworkOperation.Type.UPLOAD, operation.type)
                    assertEquals(NetworkOperation.Status.COMPLETED, operation.status)
                    assertEquals("/remote.md", operation.remotePath)
                    assertEquals("/local.md", operation.localPath)
                    assertEquals(1.0, operation.progress)
                    assertEquals(1024L, operation.totalSize)
                    assertEquals(1024L, operation.bytesTransferred)
                }
            }
        }
        assertEquals(1, operationCount, "Should emit one completed operation")
    }

    @Test
    fun testDeleteFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.deleteFile("/test.md")
        assertTrue(result.isSuccess, "Delete file should succeed")
    }

    @Test
    fun testCreateFolder() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.createFolder("/test-folder")
        assertTrue(result.isSuccess, "Create folder should succeed")

        val folder = result.getOrNull()
        assertEquals("test-folder", folder?.name)
        assertEquals("/test-folder", folder?.path)
        assertTrue(folder?.isFolder ?: false)
        assertEquals(setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE), folder?.permissions)
    }

    @Test
    fun testRenameFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.renameFile("/old.md", "new.md")
        assertTrue(result.isSuccess, "Rename file should succeed")
    }

    @Test
    fun testMoveFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.moveFile("/source.md", "/dest.md")
        assertTrue(result.isSuccess, "Move file should succeed")

        val movedFile = result.getOrNull()
        assertEquals("dest.md", movedFile?.name)
        assertEquals("/dest.md", movedFile?.path)
        assertFalse(movedFile?.isFolder ?: true)
    }

    @Test
    fun testCopyFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.copyFile("/source.md", "/dest.md")
        assertTrue(result.isSuccess, "Copy file should succeed")
    }

    @Test
    fun testGetFileInfo() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.getFileInfo("/test.md")
        assertTrue(result.isSuccess, "Get file info should succeed")

        val file = result.getOrNull()
        assertEquals("Test File.md", file?.name)
        assertEquals("/test.md", file?.path)
        assertFalse(file?.isFolder ?: true)
        assertEquals(1024L, file?.size)
        assertEquals(SyncStatus.SYNCED, file?.syncStatus)
        assertEquals(setOf(DocumentPermission.READ, DocumentPermission.WRITE), file?.permissions)
    }

    @Test
    fun testGetActiveOperations() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val operations = mockService.getActiveOperations().first()
        assertTrue(operations.isEmpty(), "Active operations should be empty initially")
    }

    @Test
    fun testCancelOperation() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.cancelOperation(123L)
        assertTrue(result.isSuccess, "Cancel operation should succeed")
    }

    @Test
    fun testPauseOperation() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.pauseOperation(123L)
        assertTrue(result.isSuccess, "Pause operation should succeed")
    }

    @Test
    fun testResumeOperation() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.resumeOperation(123L)
        assertTrue(result.isSuccess, "Resume operation should succeed")
    }

    @Test
    fun testGetStorageInfo() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val storage = mockService.getStorageInfo()
        assertEquals("mock", storage.id)
        assertEquals("Mock Storage", storage.name)
        assertEquals(StorageType.WEBDAV, storage.type)
        assertEquals("mock://", storage.location)
        assertTrue(storage.isOnline)
    }

    @Test
    fun testTestConnection() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.testConnection()
        assertTrue(result.isSuccess, "Test connection should succeed")
        assertTrue(result.getOrNull() ?: false, "Connection test should return true")
    }

    @Test
    fun testGetCacheEntries() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val entries = mockService.getCacheEntries("/").first()
        assertTrue(entries.isEmpty(), "Cache entries should be empty")
    }

    @Test
    fun testAddToCache() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.addToCache("/test.md", 1)
        assertTrue(result.isSuccess, "Add to cache should succeed")
    }

    @Test
    fun testRemoveFromCache() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.removeFromCache("/test.md")
        assertTrue(result.isSuccess, "Remove from cache should succeed")
    }

    @Test
    fun testClearCache() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.clearCache()
        assertTrue(result.isSuccess, "Clear cache should succeed")
    }

    @Test
    fun testGetSyncStatus() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val status = mockService.getSyncStatus("/").first()
        assertTrue(status.isEmpty(), "Sync status should be empty")
    }

    @Test
    fun testSyncFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val operations = mockService.syncFile("/test.md", false)

        var operationCount = 0
        operations.collect { operation ->
            operationCount++
            when (operationCount) {
                1 -> {
                    assertEquals(NetworkOperation.Type.SYNC, operation.type)
                    assertEquals(NetworkOperation.Status.COMPLETED, operation.status)
                    assertEquals("/test.md", operation.remotePath)
                    assertEquals(1.0, operation.progress)
                    assertEquals(1024L, operation.totalSize)
                    assertEquals(1024L, operation.bytesTransferred)
                }
            }
        }
        assertEquals(1, operationCount, "Should emit one completed operation")
    }

    @Test
    fun testSyncAll() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val operations = mockService.syncAll(false)

        var operationCount = 0
        operations.collect { operation ->
            operationCount++
            assertEquals(NetworkOperation.Type.SYNC, operation.type)
            assertEquals(NetworkOperation.Status.COMPLETED, operation.status)
            assertEquals("/", operation.remotePath)
            assertEquals(1.0, operation.progress)
            assertEquals(1024L, operation.totalSize)
            assertEquals(1024L, operation.bytesTransferred)
        }
        assertEquals(1, operationCount, "Should emit one completed operation")
    }

    @Test
    fun testSearchFiles() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.searchFiles("Test", "/", false).first()
        assertTrue(result.isSuccess, "Search should succeed")

        val files = result.getOrNull()
        assertEquals(1, files?.size, "Should find one matching file")
        assertEquals("Search Result.md", files?.first()?.name)
    }

    @Test
    fun testGetRecentChanges() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val since = Clock.System.now()
        val changes = mockService.getRecentChanges(since, "/").first()
        assertTrue(changes.isEmpty(), "Recent changes should be empty")
    }

    @Test
    fun testGetQuotaInfo() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.getQuotaInfo()
        assertTrue(result.isSuccess, "Get quota info should succeed")

        val quota = result.getOrNull()
        assertEquals(1073741824L, quota?.totalSpace) // 1GB
        assertEquals(536870912L, quota?.usedSpace) // 512MB
        assertEquals(536870912L, quota?.availableSpace) // 512MB
        assertEquals(0.5, quota?.usagePercentage)
        assertFalse(quota?.isFull ?: true)
        assertFalse(quota?.isLowOnSpace ?: true)
    }

    @Test
    fun testExists() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.exists("/test.md")
        assertTrue(result.isSuccess, "Exists check should succeed")
        assertTrue(result.getOrNull() ?: false, "File should exist")
    }

    @Test
    fun testGetParentPath() {
        mockService = MockNetworkStorageService(mockConfig)

        assertEquals("/", mockService.getParentPath("/test.md"))
        assertEquals("/folder", mockService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", mockService.getParentPath("/folder/subfolder/test.md"))
        assertEquals("/", mockService.getParentPath("/"))
        assertEquals("/", mockService.getParentPath(""))
    }

    @Test
    fun testValidatePath() {
        mockService = MockNetworkStorageService(mockConfig)

        assertTrue(mockService.validatePath("/test.md").isSuccess)
        assertTrue(mockService.validatePath("/folder/test.md").isSuccess)
        assertTrue(mockService.validatePath("").isSuccess)
        assertTrue(mockService.validatePath("   ").isSuccess)
    }
}