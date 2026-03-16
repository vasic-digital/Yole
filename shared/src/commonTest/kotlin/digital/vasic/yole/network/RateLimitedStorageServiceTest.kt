/*#######################################################
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *########################################################*/
package digital.vasic.yole.network

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Mock implementation of [NetworkStorageService] that records all calls
 * and returns configurable results for testing [RateLimitedStorageService].
 */
private class RateLimitMockService : NetworkStorageService {

    // Call tracking
    val calls = mutableListOf<String>()

    // Configurable return values
    var configValue: StorageConfig = StorageConfig.WebDavConfig(
        name = "TestWebDav",
        url = "https://webdav.example.com",
        username = "user",
        password = "pass"
    )
    var isOnlineValue: Boolean = true
    var rootPathValue: String = "/root"

    var connectResult: Result<Unit> = Result.success(Unit)
    var disconnectResult: Result<Unit> = Result.success(Unit)
    var storageInfoValue: NetworkStorage = NetworkStorage(
        id = "storage-1",
        name = "Test Storage",
        type = StorageType.WEBDAV,
        location = "https://webdav.example.com",
        totalSpace = 10_000_000L,
        usedSpace = 3_000_000L,
        isOnline = true
    )
    var testConnectionResult: Result<Boolean> = Result.success(true)

    var listFilesResult: Result<List<NetworkDocument>> = Result.success(
        listOf(
            NetworkDocument(id = "doc-1", name = "file1.txt", path = "/file1.txt", size = 512L),
            NetworkDocument(id = "doc-2", name = "file2.md", path = "/file2.md", size = 1024L)
        )
    )
    var downloadFileFlow: Flow<NetworkOperation> = flowOf(
        NetworkOperation(
            id = 100L,
            type = NetworkOperation.Type.DOWNLOAD,
            remotePath = "/remote.txt",
            localPath = "/local.txt",
            status = NetworkOperation.Status.COMPLETED,
            progress = 1.0,
            createdAt = Clock.System.now()
        )
    )
    var uploadFileFlow: Flow<NetworkOperation> = flowOf(
        NetworkOperation(
            id = 101L,
            type = NetworkOperation.Type.UPLOAD,
            remotePath = "/remote.txt",
            localPath = "/local.txt",
            status = NetworkOperation.Status.COMPLETED,
            progress = 1.0,
            createdAt = Clock.System.now()
        )
    )
    var deleteFileResult: Result<Unit> = Result.success(Unit)
    var createFolderResult: Result<NetworkDocument> = Result.success(
        NetworkDocument(id = "folder-1", name = "newfolder", path = "/newfolder", isFolder = true)
    )
    var renameFileResult: Result<Unit> = Result.success(Unit)
    var moveFileResult: Result<NetworkDocument> = Result.success(
        NetworkDocument(id = "moved-1", name = "moved.txt", path = "/dest/moved.txt")
    )
    var copyFileResult: Result<Unit> = Result.success(Unit)
    var getFileInfoResult: Result<NetworkDocument> = Result.success(
        NetworkDocument(id = "info-1", name = "info.txt", path = "/info.txt", size = 2048L)
    )

    var activeOperationsFlow: Flow<List<NetworkOperation>> = flowOf(emptyList())
    var cancelOperationResult: Result<Unit> = Result.success(Unit)
    var pauseOperationResult: Result<Unit> = Result.success(Unit)
    var resumeOperationResult: Result<Unit> = Result.success(Unit)

    var cacheEntriesFlow: Flow<List<CacheEntry>> = flowOf(emptyList())
    var addToCacheResult: Result<Unit> = Result.success(Unit)
    var removeFromCacheResult: Result<Unit> = Result.success(Unit)
    var clearCacheResult: Result<Unit> = Result.success(Unit)

    var syncStatusFlow: Flow<Map<String, SyncStatus>> = flowOf(
        mapOf("/file.txt" to SyncStatus.SYNCED)
    )
    var syncFileFlow: Flow<NetworkOperation> = flowOf(
        NetworkOperation(
            id = 200L,
            type = NetworkOperation.Type.SYNC,
            remotePath = "/file.txt",
            status = NetworkOperation.Status.COMPLETED,
            progress = 1.0,
            createdAt = Clock.System.now()
        )
    )
    var syncAllFlow: Flow<NetworkOperation> = flowOf(
        NetworkOperation(
            id = 201L,
            type = NetworkOperation.Type.SYNC,
            remotePath = "/",
            status = NetworkOperation.Status.COMPLETED,
            progress = 1.0,
            createdAt = Clock.System.now()
        )
    )

    var searchFilesResult: Result<List<NetworkDocument>> = Result.success(
        listOf(
            NetworkDocument(id = "search-1", name = "found.txt", path = "/found.txt", size = 256L)
        )
    )
    var recentChangesFlow: Flow<List<NetworkDocument>> = flowOf(
        listOf(
            NetworkDocument(id = "recent-1", name = "changed.txt", path = "/changed.txt")
        )
    )
    var quotaInfoResult: Result<StorageQuota> = Result.success(
        StorageQuota(
            totalSpace = 10_000_000L,
            usedSpace = 3_000_000L,
            availableSpace = 7_000_000L,
            usagePercentage = 0.3,
            isFull = false,
            isLowOnSpace = false
        )
    )
    var existsResult: Result<Boolean> = Result.success(true)
    var parentPathValue: String? = "/parent"
    var validatePathResult: Result<Unit> = Result.success(Unit)

    // Captured arguments
    var lastListFilesPath: String? = null
    var lastDownloadRemotePath: String? = null
    var lastDownloadLocalPath: String? = null
    var lastUploadLocalPath: String? = null
    var lastUploadRemotePath: String? = null
    var lastDeleteFilePath: String? = null
    var lastCreateFolderPath: String? = null
    var lastRenameFilePath: String? = null
    var lastRenameNewName: String? = null
    var lastMoveSourcePath: String? = null
    var lastMoveDestPath: String? = null
    var lastCopySourcePath: String? = null
    var lastCopyDestPath: String? = null
    var lastGetFileInfoPath: String? = null
    var lastCancelOperationId: Long? = null
    var lastPauseOperationId: Long? = null
    var lastResumeOperationId: Long? = null
    var lastGetCacheEntriesPath: String? = null
    var lastAddToCachePath: String? = null
    var lastAddToCachePriority: Int? = null
    var lastRemoveFromCachePath: String? = null
    var lastGetSyncStatusPath: String? = null
    var lastSyncFilePath: String? = null
    var lastSyncFileForceSync: Boolean? = null
    var lastSyncAllForceSync: Boolean? = null
    var lastSearchQuery: String? = null
    var lastSearchPath: String? = null
    var lastSearchIncludeContent: Boolean? = null
    var lastRecentChangesSince: Instant? = null
    var lastRecentChangesPath: String? = null
    var lastExistsPath: String? = null
    var lastGetParentPath: String? = null
    var lastValidatePath: String? = null

    override val config: StorageConfig get() {
        calls.add("config")
        return configValue
    }

    override val isOnline: Boolean get() {
        calls.add("isOnline")
        return isOnlineValue
    }

    override val rootPath: String get() {
        calls.add("rootPath")
        return rootPathValue
    }

    override suspend fun connect(): Result<Unit> {
        calls.add("connect")
        return connectResult
    }

    override suspend fun disconnect(): Result<Unit> {
        calls.add("disconnect")
        return disconnectResult
    }

    override suspend fun getStorageInfo(): NetworkStorage {
        calls.add("getStorageInfo")
        return storageInfoValue
    }

    override suspend fun testConnection(): Result<Boolean> {
        calls.add("testConnection")
        return testConnectionResult
    }

    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> {
        calls.add("listFiles")
        lastListFilesPath = path
        return flowOf(listFilesResult)
    }

    override suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation> {
        calls.add("downloadFile")
        lastDownloadRemotePath = remotePath
        lastDownloadLocalPath = localPath
        return downloadFileFlow
    }

    override suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation> {
        calls.add("uploadFile")
        lastUploadLocalPath = localPath
        lastUploadRemotePath = remotePath
        return uploadFileFlow
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        calls.add("deleteFile")
        lastDeleteFilePath = remotePath
        return deleteFileResult
    }

    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> {
        calls.add("createFolder")
        lastCreateFolderPath = remotePath
        return createFolderResult
    }

    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        calls.add("renameFile")
        lastRenameFilePath = remotePath
        lastRenameNewName = newName
        return renameFileResult
    }

    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> {
        calls.add("moveFile")
        lastMoveSourcePath = sourcePath
        lastMoveDestPath = destinationPath
        return moveFileResult
    }

    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> {
        calls.add("copyFile")
        lastCopySourcePath = sourcePath
        lastCopyDestPath = destinationPath
        return copyFileResult
    }

    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        calls.add("getFileInfo")
        lastGetFileInfoPath = remotePath
        return getFileInfoResult
    }

    override fun getActiveOperations(): Flow<List<NetworkOperation>> {
        calls.add("getActiveOperations")
        return activeOperationsFlow
    }

    override suspend fun cancelOperation(operationId: Long): Result<Unit> {
        calls.add("cancelOperation")
        lastCancelOperationId = operationId
        return cancelOperationResult
    }

    override suspend fun pauseOperation(operationId: Long): Result<Unit> {
        calls.add("pauseOperation")
        lastPauseOperationId = operationId
        return pauseOperationResult
    }

    override suspend fun resumeOperation(operationId: Long): Result<Unit> {
        calls.add("resumeOperation")
        lastResumeOperationId = operationId
        return resumeOperationResult
    }

    override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> {
        calls.add("getCacheEntries")
        lastGetCacheEntriesPath = path
        return cacheEntriesFlow
    }

    override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> {
        calls.add("addToCache")
        lastAddToCachePath = remotePath
        lastAddToCachePriority = priority
        return addToCacheResult
    }

    override suspend fun removeFromCache(remotePath: String): Result<Unit> {
        calls.add("removeFromCache")
        lastRemoveFromCachePath = remotePath
        return removeFromCacheResult
    }

    override suspend fun clearCache(): Result<Unit> {
        calls.add("clearCache")
        return clearCacheResult
    }

    override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> {
        calls.add("getSyncStatus")
        lastGetSyncStatusPath = path
        return syncStatusFlow
    }

    override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> {
        calls.add("syncFile")
        lastSyncFilePath = remotePath
        lastSyncFileForceSync = forceSync
        return syncFileFlow
    }

    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> {
        calls.add("syncAll")
        lastSyncAllForceSync = forceSync
        return syncAllFlow
    }

    override fun searchFiles(
        query: String,
        path: String?,
        includeContent: Boolean
    ): Flow<Result<List<NetworkDocument>>> {
        calls.add("searchFiles")
        lastSearchQuery = query
        lastSearchPath = path
        lastSearchIncludeContent = includeContent
        return flowOf(searchFilesResult)
    }

    override fun getRecentChanges(since: Instant, path: String?): Flow<List<NetworkDocument>> {
        calls.add("getRecentChanges")
        lastRecentChangesSince = since
        lastRecentChangesPath = path
        return recentChangesFlow
    }

    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        calls.add("getQuotaInfo")
        return quotaInfoResult
    }

    override suspend fun exists(remotePath: String): Result<Boolean> {
        calls.add("exists")
        lastExistsPath = remotePath
        return existsResult
    }

    override fun getParentPath(remotePath: String): String? {
        calls.add("getParentPath")
        lastGetParentPath = remotePath
        return parentPathValue
    }

    override fun validatePath(remotePath: String): Result<Unit> {
        calls.add("validatePath")
        lastValidatePath = remotePath
        return validatePathResult
    }
}

/**
 * Comprehensive tests for [RateLimitedStorageService].
 *
 * Verifies that the decorator:
 * 1. Delegates every method to the underlying service
 * 2. Rate-limited methods go through the semaphore (calls complete successfully)
 * 3. Results from the delegate are properly forwarded
 */
class RateLimitedStorageServiceTest {

    private fun createServicePair(maxConcurrent: Int = 4): Pair<RateLimitMockService, RateLimitedStorageService> {
        val mock = RateLimitMockService()
        val rateLimited = RateLimitedStorageService(mock, maxConcurrent)
        return mock to rateLimited
    }

    // ---------------------------------------------------------------
    // Properties
    // ---------------------------------------------------------------

    @Test
    fun configDelegatesToUnderlyingService() {
        val (mock, service) = createServicePair()
        val result = service.config
        assertIs<StorageConfig.WebDavConfig>(result)
        assertEquals("TestWebDav", (result as StorageConfig.WebDavConfig).name)
        assertTrue(mock.calls.contains("config"))
    }

    @Test
    fun isOnlineDelegatesToUnderlyingService() {
        val (mock, service) = createServicePair()
        mock.isOnlineValue = true
        assertTrue(service.isOnline)
        assertTrue(mock.calls.contains("isOnline"))
    }

    @Test
    fun isOnlineReturnsFalseWhenDelegateIsOffline() {
        val (mock, service) = createServicePair()
        mock.isOnlineValue = false
        assertFalse(service.isOnline)
    }

    @Test
    fun rootPathDelegatesToUnderlyingService() {
        val (mock, service) = createServicePair()
        mock.rootPathValue = "/custom/root"
        assertEquals("/custom/root", service.rootPath)
        assertTrue(mock.calls.contains("rootPath"))
    }

    // ---------------------------------------------------------------
    // connect / disconnect
    // ---------------------------------------------------------------

    @Test
    fun connectDelegatesAndForwardsSuccess() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.connect()
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("connect"))
    }

    @Test
    fun connectForwardsFailure() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.connectResult = Result.failure(RuntimeException("connection refused"))
        val result = service.connect()
        assertTrue(result.isFailure)
        assertEquals("connection refused", result.exceptionOrNull()?.message)
    }

    @Test
    fun disconnectDelegatesAndForwardsSuccess() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("disconnect"))
    }

    @Test
    fun disconnectForwardsFailure() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.disconnectResult = Result.failure(IllegalStateException("not connected"))
        val result = service.disconnect()
        assertTrue(result.isFailure)
        assertEquals("not connected", result.exceptionOrNull()?.message)
    }

    // ---------------------------------------------------------------
    // getStorageInfo / testConnection
    // ---------------------------------------------------------------

    @Test
    fun getStorageInfoDelegatesAndForwardsResult() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val info = service.getStorageInfo()
        assertEquals("storage-1", info.id)
        assertEquals("Test Storage", info.name)
        assertEquals(StorageType.WEBDAV, info.type)
        assertTrue(mock.calls.contains("getStorageInfo"))
    }

    @Test
    fun testConnectionDelegatesAndForwardsTrue() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.testConnection()
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        assertTrue(mock.calls.contains("testConnection"))
    }

    @Test
    fun testConnectionForwardsFalse() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.testConnectionResult = Result.success(false)
        val result = service.testConnection()
        assertEquals(false, result.getOrNull())
    }

    // ---------------------------------------------------------------
    // listFiles
    // ---------------------------------------------------------------

    @Test
    fun listFilesDelegatesAndForwardsResults() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val results = service.listFiles("/documents").toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(2, results[0].getOrNull()?.size)
        assertEquals("file1.txt", results[0].getOrNull()?.get(0)?.name)
        assertTrue(mock.calls.contains("listFiles"))
        assertEquals("/documents", mock.lastListFilesPath)
    }

    @Test
    fun listFilesForwardsFailure() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.listFilesResult = Result.failure(RuntimeException("access denied"))
        val results = service.listFiles("/secret").toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isFailure)
    }

    // ---------------------------------------------------------------
    // downloadFile / uploadFile
    // ---------------------------------------------------------------

    @Test
    fun downloadFileDelegatesWithCorrectArguments() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val flow = service.downloadFile("/remote/file.txt", "/local/file.txt")
        val ops = flow.toList()
        assertEquals(1, ops.size)
        assertEquals(NetworkOperation.Status.COMPLETED, ops[0].status)
        assertTrue(mock.calls.contains("downloadFile"))
        assertEquals("/remote/file.txt", mock.lastDownloadRemotePath)
        assertEquals("/local/file.txt", mock.lastDownloadLocalPath)
    }

    @Test
    fun uploadFileDelegatesWithCorrectArguments() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val flow = service.uploadFile("/local/upload.txt", "/remote/upload.txt")
        val ops = flow.toList()
        assertEquals(1, ops.size)
        assertEquals(NetworkOperation.Type.UPLOAD, ops[0].type)
        assertTrue(mock.calls.contains("uploadFile"))
        assertEquals("/local/upload.txt", mock.lastUploadLocalPath)
        assertEquals("/remote/upload.txt", mock.lastUploadRemotePath)
    }

    // ---------------------------------------------------------------
    // deleteFile
    // ---------------------------------------------------------------

    @Test
    fun deleteFileDelegatesAndForwardsSuccess() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.deleteFile("/trash/old.txt")
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("deleteFile"))
        assertEquals("/trash/old.txt", mock.lastDeleteFilePath)
    }

    @Test
    fun deleteFileForwardsFailure() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.deleteFileResult = Result.failure(RuntimeException("file not found"))
        val result = service.deleteFile("/missing.txt")
        assertTrue(result.isFailure)
        assertEquals("file not found", result.exceptionOrNull()?.message)
    }

    // ---------------------------------------------------------------
    // createFolder
    // ---------------------------------------------------------------

    @Test
    fun createFolderDelegatesAndReturnsDocument() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.createFolder("/new/folder")
        assertTrue(result.isSuccess)
        assertEquals("newfolder", result.getOrNull()?.name)
        assertTrue(result.getOrNull()?.isFolder == true)
        assertTrue(mock.calls.contains("createFolder"))
        assertEquals("/new/folder", mock.lastCreateFolderPath)
    }

    // ---------------------------------------------------------------
    // renameFile
    // ---------------------------------------------------------------

    @Test
    fun renameFileDelegatesWithCorrectArguments() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.renameFile("/old-name.txt", "new-name.txt")
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("renameFile"))
        assertEquals("/old-name.txt", mock.lastRenameFilePath)
        assertEquals("new-name.txt", mock.lastRenameNewName)
    }

    // ---------------------------------------------------------------
    // moveFile
    // ---------------------------------------------------------------

    @Test
    fun moveFileDelegatesAndForwardsDocument() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.moveFile("/source/file.txt", "/dest/file.txt")
        assertTrue(result.isSuccess)
        assertEquals("moved.txt", result.getOrNull()?.name)
        assertTrue(mock.calls.contains("moveFile"))
        assertEquals("/source/file.txt", mock.lastMoveSourcePath)
        assertEquals("/dest/file.txt", mock.lastMoveDestPath)
    }

    // ---------------------------------------------------------------
    // copyFile
    // ---------------------------------------------------------------

    @Test
    fun copyFileDelegatesWithCorrectArguments() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.copyFile("/original.txt", "/copy.txt")
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("copyFile"))
        assertEquals("/original.txt", mock.lastCopySourcePath)
        assertEquals("/copy.txt", mock.lastCopyDestPath)
    }

    // ---------------------------------------------------------------
    // getFileInfo
    // ---------------------------------------------------------------

    @Test
    fun getFileInfoDelegatesAndReturnsDocument() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.getFileInfo("/data/file.txt")
        assertTrue(result.isSuccess)
        assertEquals("info.txt", result.getOrNull()?.name)
        assertEquals(2048L, result.getOrNull()?.size)
        assertTrue(mock.calls.contains("getFileInfo"))
        assertEquals("/data/file.txt", mock.lastGetFileInfoPath)
    }

    // ---------------------------------------------------------------
    // getActiveOperations (not rate-limited)
    // ---------------------------------------------------------------

    @Test
    fun getActiveOperationsDelegatesToUnderlyingService() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val ops = service.getActiveOperations().toList()
        assertEquals(1, ops.size)
        assertEquals(emptyList(), ops[0])
        assertTrue(mock.calls.contains("getActiveOperations"))
    }

    // ---------------------------------------------------------------
    // cancelOperation (not rate-limited)
    // ---------------------------------------------------------------

    @Test
    fun cancelOperationDelegatesToUnderlyingService() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.cancelOperation(42L)
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("cancelOperation"))
        assertEquals(42L, mock.lastCancelOperationId)
    }

    // ---------------------------------------------------------------
    // pauseOperation (not rate-limited)
    // ---------------------------------------------------------------

    @Test
    fun pauseOperationDelegatesToUnderlyingService() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.pauseOperation(77L)
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("pauseOperation"))
        assertEquals(77L, mock.lastPauseOperationId)
    }

    // ---------------------------------------------------------------
    // resumeOperation (not rate-limited)
    // ---------------------------------------------------------------

    @Test
    fun resumeOperationDelegatesToUnderlyingService() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.resumeOperation(99L)
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("resumeOperation"))
        assertEquals(99L, mock.lastResumeOperationId)
    }

    // ---------------------------------------------------------------
    // getCacheEntries (not rate-limited)
    // ---------------------------------------------------------------

    @Test
    fun getCacheEntriesDelegatesToUnderlyingService() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val entries = service.getCacheEntries("/cache/path").toList()
        assertEquals(1, entries.size)
        assertEquals(emptyList(), entries[0])
        assertTrue(mock.calls.contains("getCacheEntries"))
        assertEquals("/cache/path", mock.lastGetCacheEntriesPath)
    }

    @Test
    fun getCacheEntriesPassesNullPath() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        service.getCacheEntries(null).toList()
        assertNull(mock.lastGetCacheEntriesPath)
    }

    // ---------------------------------------------------------------
    // addToCache
    // ---------------------------------------------------------------

    @Test
    fun addToCacheDelegatesWithCorrectArguments() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.addToCache("/important.txt", 200)
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("addToCache"))
        assertEquals("/important.txt", mock.lastAddToCachePath)
        assertEquals(200, mock.lastAddToCachePriority)
    }

    // ---------------------------------------------------------------
    // removeFromCache
    // ---------------------------------------------------------------

    @Test
    fun removeFromCacheDelegatesWithCorrectPath() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.removeFromCache("/expired.txt")
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("removeFromCache"))
        assertEquals("/expired.txt", mock.lastRemoveFromCachePath)
    }

    // ---------------------------------------------------------------
    // clearCache
    // ---------------------------------------------------------------

    @Test
    fun clearCacheDelegatesAndForwardsResult() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.clearCache()
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("clearCache"))
    }

    @Test
    fun clearCacheForwardsFailure() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.clearCacheResult = Result.failure(RuntimeException("cache locked"))
        val result = service.clearCache()
        assertTrue(result.isFailure)
        assertEquals("cache locked", result.exceptionOrNull()?.message)
    }

    // ---------------------------------------------------------------
    // getSyncStatus (not rate-limited)
    // ---------------------------------------------------------------

    @Test
    fun getSyncStatusDelegatesToUnderlyingService() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val statuses = service.getSyncStatus("/sync/path").toList()
        assertEquals(1, statuses.size)
        assertEquals(SyncStatus.SYNCED, statuses[0]["/file.txt"])
        assertTrue(mock.calls.contains("getSyncStatus"))
        assertEquals("/sync/path", mock.lastGetSyncStatusPath)
    }

    // ---------------------------------------------------------------
    // syncFile
    // ---------------------------------------------------------------

    @Test
    fun syncFileDelegatesWithCorrectArguments() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val flow = service.syncFile("/data/file.txt", forceSync = true)
        val ops = flow.toList()
        assertEquals(1, ops.size)
        assertEquals(NetworkOperation.Type.SYNC, ops[0].type)
        assertTrue(mock.calls.contains("syncFile"))
        assertEquals("/data/file.txt", mock.lastSyncFilePath)
        assertEquals(true, mock.lastSyncFileForceSync)
    }

    @Test
    fun syncFileWithForceSyncFalse() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        service.syncFile("/data/file.txt", forceSync = false).toList()
        assertEquals(false, mock.lastSyncFileForceSync)
    }

    // ---------------------------------------------------------------
    // syncAll
    // ---------------------------------------------------------------

    @Test
    fun syncAllDelegatesWithCorrectArguments() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val flow = service.syncAll(forceSync = true)
        val ops = flow.toList()
        assertEquals(1, ops.size)
        assertEquals(NetworkOperation.Status.COMPLETED, ops[0].status)
        assertTrue(mock.calls.contains("syncAll"))
        assertEquals(true, mock.lastSyncAllForceSync)
    }

    // ---------------------------------------------------------------
    // searchFiles
    // ---------------------------------------------------------------

    @Test
    fun searchFilesDelegatesWithAllArguments() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val results = service.searchFiles("*.md", "/docs", includeContent = true).toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals("found.txt", results[0].getOrNull()?.get(0)?.name)
        assertTrue(mock.calls.contains("searchFiles"))
        assertEquals("*.md", mock.lastSearchQuery)
        assertEquals("/docs", mock.lastSearchPath)
        assertEquals(true, mock.lastSearchIncludeContent)
    }

    @Test
    fun searchFilesForwardsFailure() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.searchFilesResult = Result.failure(RuntimeException("search timeout"))
        val results = service.searchFiles("query").toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isFailure)
    }

    // ---------------------------------------------------------------
    // getRecentChanges (not rate-limited)
    // ---------------------------------------------------------------

    @Test
    fun getRecentChangesDelegatesToUnderlyingService() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val since = Clock.System.now()
        val changes = service.getRecentChanges(since, "/watched").toList()
        assertEquals(1, changes.size)
        assertEquals("changed.txt", changes[0][0].name)
        assertTrue(mock.calls.contains("getRecentChanges"))
        assertEquals(since, mock.lastRecentChangesSince)
        assertEquals("/watched", mock.lastRecentChangesPath)
    }

    // ---------------------------------------------------------------
    // getQuotaInfo
    // ---------------------------------------------------------------

    @Test
    fun getQuotaInfoDelegatesAndForwardsResult() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess)
        val quota = result.getOrNull()!!
        assertEquals(10_000_000L, quota.totalSpace)
        assertEquals(3_000_000L, quota.usedSpace)
        assertEquals(7_000_000L, quota.availableSpace)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
        assertTrue(mock.calls.contains("getQuotaInfo"))
    }

    // ---------------------------------------------------------------
    // exists
    // ---------------------------------------------------------------

    @Test
    fun existsDelegatesAndForwardsTrue() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        val result = service.exists("/check/exists.txt")
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        assertTrue(mock.calls.contains("exists"))
        assertEquals("/check/exists.txt", mock.lastExistsPath)
    }

    @Test
    fun existsForwardsFalse() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.existsResult = Result.success(false)
        val result = service.exists("/missing.txt")
        assertEquals(false, result.getOrNull())
    }

    // ---------------------------------------------------------------
    // getParentPath (not rate-limited)
    // ---------------------------------------------------------------

    @Test
    fun getParentPathDelegatesToUnderlyingService() {
        val (mock, service) = createServicePair()
        mock.parentPathValue = "/parent/dir"
        val result = service.getParentPath("/parent/dir/child.txt")
        assertEquals("/parent/dir", result)
        assertTrue(mock.calls.contains("getParentPath"))
        assertEquals("/parent/dir/child.txt", mock.lastGetParentPath)
    }

    @Test
    fun getParentPathReturnsNullForRoot() {
        val (mock, service) = createServicePair()
        mock.parentPathValue = null
        val result = service.getParentPath("/")
        assertNull(result)
    }

    // ---------------------------------------------------------------
    // validatePath (not rate-limited)
    // ---------------------------------------------------------------

    @Test
    fun validatePathDelegatesToUnderlyingService() {
        val (mock, service) = createServicePair()
        val result = service.validatePath("/valid/path.txt")
        assertTrue(result.isSuccess)
        assertTrue(mock.calls.contains("validatePath"))
        assertEquals("/valid/path.txt", mock.lastValidatePath)
    }

    @Test
    fun validatePathForwardsFailure() {
        val (mock, service) = createServicePair()
        mock.validatePathResult = Result.failure(IllegalArgumentException("invalid characters"))
        val result = service.validatePath("/bad\u0000path")
        assertTrue(result.isFailure)
        assertEquals("invalid characters", result.exceptionOrNull()?.message)
    }

    // ---------------------------------------------------------------
    // Rate limiting behavior
    // ---------------------------------------------------------------

    @Test
    fun rateLimitedServiceWithMaxConcurrentOneStillCompletes() = runBlocking<Unit> {
        val (mock, service) = createServicePair(maxConcurrent = 1)
        // Even with concurrency of 1, all calls should complete
        val r1 = service.connect()
        val r2 = service.disconnect()
        val r3 = service.deleteFile("/a.txt")
        assertTrue(r1.isSuccess)
        assertTrue(r2.isSuccess)
        assertTrue(r3.isSuccess)
        assertEquals(3, mock.calls.count { it in listOf("connect", "disconnect", "deleteFile") })
    }

    @Test
    fun multipleMethodsAllDelegateCorrectly() = runBlocking<Unit> {
        val (mock, service) = createServicePair()

        service.connect()
        service.getStorageInfo()
        service.testConnection()
        service.listFiles("/").toList()
        service.downloadFile("/r", "/l").toList()
        service.uploadFile("/l", "/r").toList()
        service.deleteFile("/d")
        service.createFolder("/f")
        service.renameFile("/old", "new")
        service.moveFile("/s", "/d")
        service.copyFile("/s", "/d")
        service.getFileInfo("/i")
        service.getActiveOperations().toList()
        service.cancelOperation(1L)
        service.pauseOperation(2L)
        service.resumeOperation(3L)
        service.getCacheEntries().toList()
        service.addToCache("/c", 50)
        service.removeFromCache("/c")
        service.clearCache()
        service.getSyncStatus().toList()
        service.syncFile("/s").toList()
        service.syncAll().toList()
        service.searchFiles("q").toList()
        service.getRecentChanges(Clock.System.now()).toList()
        service.getQuotaInfo()
        service.exists("/e")
        service.getParentPath("/p")
        service.validatePath("/v")

        // Verify every single method was called
        val expectedCalls = listOf(
            "connect", "getStorageInfo", "testConnection", "listFiles",
            "downloadFile", "uploadFile", "deleteFile", "createFolder",
            "renameFile", "moveFile", "copyFile", "getFileInfo",
            "getActiveOperations", "cancelOperation", "pauseOperation",
            "resumeOperation", "getCacheEntries", "addToCache",
            "removeFromCache", "clearCache", "getSyncStatus", "syncFile",
            "syncAll", "searchFiles", "getRecentChanges", "getQuotaInfo",
            "exists", "getParentPath", "validatePath"
        )
        for (call in expectedCalls) {
            assertTrue(
                mock.calls.contains(call),
                "Expected call to '$call' but it was not recorded. Recorded calls: ${mock.calls}"
            )
        }
    }

    @Test
    fun configReturnsExactSameObjectAsDelegate() {
        val (mock, service) = createServicePair()
        val delegateConfig = mock.configValue
        val serviceConfig = service.config
        assertEquals(delegateConfig, serviceConfig)
    }

    @Test
    fun getStorageInfoPreservesAllFields() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.storageInfoValue = NetworkStorage(
            id = "detail-test",
            name = "Detailed Storage",
            type = StorageType.WEBDAV,
            location = "https://detail.example.com",
            totalSpace = 50_000_000L,
            usedSpace = 25_000_000L,
            isOnline = true,
            isEnabled = true,
            priority = 5,
            supportsFolders = true,
            supportsMetadata = false,
            maxFileSize = 100_000L
        )
        val info = service.getStorageInfo()
        assertEquals("detail-test", info.id)
        assertEquals("Detailed Storage", info.name)
        assertEquals(50_000_000L, info.totalSpace)
        assertEquals(25_000_000L, info.usedSpace)
        assertEquals(5, info.priority)
        assertFalse(info.supportsMetadata)
        assertEquals(100_000L, info.maxFileSize)
    }

    @Test
    fun createFolderForwardsFailureResult() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.createFolderResult = Result.failure(RuntimeException("already exists"))
        val result = service.createFolder("/existing")
        assertTrue(result.isFailure)
        assertEquals("already exists", result.exceptionOrNull()?.message)
    }

    @Test
    fun moveFileForwardsFailureResult() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.moveFileResult = Result.failure(RuntimeException("permission denied"))
        val result = service.moveFile("/a", "/b")
        assertTrue(result.isFailure)
    }

    @Test
    fun getQuotaInfoForwardsFailure() = runBlocking<Unit> {
        val (mock, service) = createServicePair()
        mock.quotaInfoResult = Result.failure<StorageQuota>(RuntimeException("quota service unavailable"))
        val result = service.getQuotaInfo()
        assertTrue(result.isFailure)
        assertEquals("quota service unavailable", result.exceptionOrNull()?.message)
    }
}
