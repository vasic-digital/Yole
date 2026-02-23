package digital.vasic.yole.network.protocol

import digital.vasic.yole.network.MockNetworkStorageService
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

        // Use toList() to collect all emissions from the flow to avoid
        // Flow exception transparency violations with first()
        val allEmissions = mockService.listFiles("/").toList()
        assertTrue(allEmissions.isNotEmpty(), "Should emit at least one result")
        val files = allEmissions[0]
        assertTrue(files.isSuccess, "List files should succeed when connected")

        val fileList = files.getOrNull()
        // Mock initializes with 3 entries: /test.md, /notes/, /notes/todo.txt
        // All have paths starting with "/" and != "/", so all 3 are returned
        assertEquals(3, fileList?.size, "Should return three mock entries")
        val names = fileList?.map { it.name }?.sorted()
        assertEquals(listOf("notes", "test.md", "todo.txt"), names)
    }

    @Test
    fun testDownloadFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val operations = mockService.downloadFile("/remote.md", "/local.md")

        val operationList = operations.toList()
        // Mock emits 4 progress updates: 0.25, 0.50, 0.75, 1.0 (completed)
        assertEquals(4, operationList.size, "Should emit four progress operations")

        val lastOp = operationList.last()
        assertEquals(NetworkOperation.Type.DOWNLOAD, lastOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, lastOp.status)
        assertEquals("/remote.md", lastOp.remotePath)
        assertEquals("/local.md", lastOp.localPath)
        assertEquals(1.0, lastOp.progress)
    }

    @Test
    fun testUploadFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val operations = mockService.uploadFile("/local.md", "/remote.md")

        val operationList = operations.toList()
        // Mock emits 4 progress updates: 0.25, 0.50, 0.75, 1.0 (completed)
        assertEquals(4, operationList.size, "Should emit four progress operations")

        val lastOp = operationList.last()
        assertEquals(NetworkOperation.Type.UPLOAD, lastOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, lastOp.status)
        assertEquals("/remote.md", lastOp.remotePath)
        assertEquals("/local.md", lastOp.localPath)
        assertEquals(1.0, lastOp.progress)
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
        assertEquals(SyncStatus.SYNCED, folder?.syncStatus)
    }

    @Test
    fun testRenameFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        // Use a path that exists in the mock data
        val result = mockService.renameFile("/test.md", "renamed.md")
        assertTrue(result.isSuccess, "Rename file should succeed")
    }

    @Test
    fun testMoveFile() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        // Use a path that exists in the mock data
        val result = mockService.moveFile("/test.md", "/dest.md")
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

        // Use a path that exists in the mock data
        val result = mockService.copyFile("/test.md", "/dest.md")
        assertTrue(result.isSuccess, "Copy file should succeed")
    }

    @Test
    fun testGetFileInfo() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.getFileInfo("/test.md")
        assertTrue(result.isSuccess, "Get file info should succeed")

        val file = result.getOrNull()
        assertEquals("test.md", file?.name)
        assertEquals("/test.md", file?.path)
        assertFalse(file?.isFolder ?: true)
        assertEquals(1024L, file?.size)
        assertEquals(SyncStatus.SYNCED, file?.syncStatus)
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

        // No operation with id 123 exists and is IN_PROGRESS, so cancel fails
        val result = mockService.cancelOperation(123L)
        assertTrue(result.isFailure, "Cancel operation should fail for non-existent operation")
    }

    @Test
    fun testPauseOperation() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        // No operation with id 123 exists and is IN_PROGRESS, so pause fails
        val result = mockService.pauseOperation(123L)
        assertTrue(result.isFailure, "Pause operation should fail for non-existent operation")
    }

    @Test
    fun testResumeOperation() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        // No operation with id 123 exists and is PAUSED, so resume fails
        val result = mockService.resumeOperation(123L)
        assertTrue(result.isFailure, "Resume operation should fail for non-existent operation")
    }

    @Test
    fun testGetStorageInfo() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val storage = mockService.getStorageInfo()
        // Mock uses config.name for both id and name
        assertEquals("test-mock", storage.id)
        assertEquals("test-mock", storage.name)
        assertEquals(StorageType.WEBDAV, storage.type)
        assertEquals("mock://localhost", storage.location)
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

        val operationList = operations.toList()
        // Mock emits 4 progress updates: 0.25, 0.50, 0.75, 1.0 (completed)
        assertEquals(4, operationList.size, "Should emit four progress operations")

        val lastOp = operationList.last()
        assertEquals(NetworkOperation.Type.SYNC, lastOp.type)
        assertEquals(NetworkOperation.Status.COMPLETED, lastOp.status)
        assertEquals("/test.md", lastOp.remotePath)
        assertEquals(1.0, lastOp.progress)
    }

    @Test
    fun testSyncAll() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val operations = mockService.syncAll(false)

        val operationList = operations.toList()
        // Mock syncAll returns an empty flow
        assertEquals(0, operationList.size, "Should emit no operations for empty syncAll")
    }

    @Test
    fun testSearchFiles() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        // Use toList() to collect all emissions from the flow to avoid
        // Flow exception transparency violations with first()
        val allEmissions = mockService.searchFiles("test", "/", false).toList()
        assertTrue(allEmissions.isNotEmpty(), "Should emit at least one result")
        val result = allEmissions[0]
        assertTrue(result.isSuccess, "Search should succeed")

        val files = result.getOrNull()
        assertEquals(1, files?.size, "Should find one matching file")
        assertEquals("test.md", files?.first()?.name)
    }

    @Test
    fun testGetRecentChanges() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val since = Clock.System.now()
        // Use toList() to collect all emissions from the flow to avoid
        // Flow exception transparency violations with first()
        val allEmissions = mockService.getRecentChanges(since, "/").toList()
        assertTrue(allEmissions.isNotEmpty(), "Should emit at least one result")
        val changes = allEmissions[0]
        assertTrue(changes.isEmpty(), "Recent changes should be empty")
    }

    @Test
    fun testGetQuotaInfo() = runTest {
        mockService = MockNetworkStorageService(mockConfig)
        mockService.connect()

        val result = mockService.getQuotaInfo()
        assertTrue(result.isSuccess, "Get quota info should succeed")

        val quota = result.getOrNull()
        // Mock uses totalSpace = 1_000_000_000 (1GB decimal)
        assertEquals(1000000000L, quota?.totalSpace)
        // usedSpace = sum of mock document sizes: 1024 + 0 + 512 = 1536
        assertEquals(1536L, quota?.usedSpace)
        assertEquals(1000000000L - 1536L, quota?.availableSpace)
        // usagePercentage = (1536.0 / 1_000_000_000 * 100) - mock multiplies by 100
        val expectedPercentage = 1536.0 / 1000000000.0 * 100
        assertEquals(expectedPercentage, quota?.usagePercentage)
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

        // Mock uses substringBeforeLast("/", "/") which returns "" for top-level paths
        // like "/test.md" since the part before the first "/" is empty
        assertEquals("", mockService.getParentPath("/test.md"))
        assertEquals("/folder", mockService.getParentPath("/folder/test.md"))
        assertEquals("/folder/subfolder", mockService.getParentPath("/folder/subfolder/test.md"))
        // Mock returns null for root and blank paths
        assertNull(mockService.getParentPath("/"))
        assertNull(mockService.getParentPath(""))
    }

    @Test
    fun testValidatePath() {
        mockService = MockNetworkStorageService(mockConfig)

        assertTrue(mockService.validatePath("/test.md").isSuccess)
        assertTrue(mockService.validatePath("/folder/test.md").isSuccess)
        // Mock fails on blank paths
        assertTrue(mockService.validatePath("").isFailure)
        assertTrue(mockService.validatePath("   ").isFailure)
    }
}
