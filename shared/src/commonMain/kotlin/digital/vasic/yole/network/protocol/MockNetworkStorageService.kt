package digital.vasic.yole.network.protocol

import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.StorageQuota
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Mock implementation of NetworkStorageService for testing
 */
class MockNetworkStorageService(
    override val config: StorageConfig
) : digital.vasic.yole.network.NetworkStorageService {
    
    override var isOnline: Boolean = false
    
    override suspend fun connect(): Result<Unit> {
        isOnline = true
        return Result.success(Unit)
    }
    
    override suspend fun disconnect(): Result<Unit> {
        isOnline = false
        return Result.success(Unit)
    }
    
    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        emit(Result.success(listOf(
            NetworkDocument(
                id = "test1",
                name = "Test File.md",
                path = "/Test File.md",
                isFolder = false,
                size = 1024L,
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                storageId = "mock-storage"
            )
        )))
    }
    
    override suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation> = flow {
        // Mock download operation
        emit(NetworkOperation(
            id = Clock.System.now().toEpochMilliseconds(),
            type = NetworkOperation.Type.DOWNLOAD,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = remotePath,
            localPath = localPath,
            progress = 1.0,
            totalSize = 1024L,
            bytesTransferred = 1024L,
            createdAt = Clock.System.now(),
            startedAt = Clock.System.now(),
            completedAt = Clock.System.now()
        ))
    }
    
    override suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation> = flow {
        // Mock upload operation
        emit(NetworkOperation(
            id = Clock.System.now().toEpochMilliseconds(),
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = remotePath,
            localPath = localPath,
            progress = 1.0,
            totalSize = 1024L,
            bytesTransferred = 1024L,
            createdAt = Clock.System.now(),
            startedAt = Clock.System.now(),
            completedAt = Clock.System.now()
        ))
    }
    
    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> {
        return Result.success(NetworkDocument(
            id = "folder_$remotePath",
            name = remotePath.substringAfterLast("/"),
            path = remotePath,
            isFolder = true,
            syncStatus = SyncStatus.SYNCED,
            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
            size = 0L,
            lastModified = Clock.System.now(),
            storageId = "mock-storage"
        ))
    }
    
    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> {
        return Result.success(NetworkDocument(
            id = "moved_$destinationPath",
            name = destinationPath.substringAfterLast("/"),
            path = destinationPath,
            isFolder = false,
            syncStatus = SyncStatus.SYNCED,
            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
            size = 1024L,
            lastModified = Clock.System.now(),
            storageId = "mock-storage"
        ))
    }
    
    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        return Result.success(NetworkDocument(
            id = "test1",
            name = "Test File.md",
            path = remotePath,
            isFolder = false,
            size = 1024L,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
            storageId = "mock-storage"
        ))
    }
    
    override fun getActiveOperations(): Flow<List<NetworkOperation>> = flow {
        emit(emptyList())
    }
    
    override suspend fun cancelOperation(operationId: Long): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun pauseOperation(operationId: Long): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun resumeOperation(operationId: Long): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "mock",
            name = "Mock Storage",
            type = StorageType.WEBDAV,
            location = "mock://",
            isOnline = isOnline,
            lastSync = Clock.System.now()
        )
    }
    
    override suspend fun testConnection(): Result<Boolean> {
        return Result.success(true)
    }
    
    override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> = flow {
        emit(emptyList())
    }
    
    override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun removeFromCache(remotePath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun clearCache(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> = flow {
        emit(emptyMap())
    }
    
    override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> = flow {
        // Mock sync operation
        emit(NetworkOperation(
            id = Clock.System.now().toEpochMilliseconds(),
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = remotePath,
            progress = 1.0,
            totalSize = 1024L,
            bytesTransferred = 1024L,
            createdAt = Clock.System.now(),
            startedAt = Clock.System.now(),
            completedAt = Clock.System.now()
        ))
    }
    
    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        // Mock sync operations
        emit(NetworkOperation(
            id = Clock.System.now().toEpochMilliseconds(),
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/",
            progress = 1.0,
            totalSize = 1024L,
            bytesTransferred = 1024L,
            createdAt = Clock.System.now(),
            startedAt = Clock.System.now(),
            completedAt = Clock.System.now()
        ))
    }
    
    override fun searchFiles(query: String, path: String?, includeContent: Boolean): Flow<Result<List<NetworkDocument>>> = flow {
        emit(Result.success(listOf(
            NetworkDocument(
                id = "search1",
                name = "Search Result.md",
                path = "/Search Result.md",
                isFolder = false,
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                size = 1024L,
                lastModified = Clock.System.now(),
                storageId = "mock-storage"
            )
        )))
    }
    
    override fun getRecentChanges(since: kotlinx.datetime.Instant, path: String?): Flow<List<NetworkDocument>> = flow {
        emit(emptyList())
    }
    
    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        return Result.success(StorageQuota(
            totalSpace = 1024L * 1024L * 1024L, // 1GB
            usedSpace = 512L * 1024L * 1024L, // 512MB
            availableSpace = 512L * 1024L * 1024L, // 512MB
            usagePercentage = 0.5,
            isFull = false,
            isLowOnSpace = false,
            metadata = mapOf("type" to "mock")
        ))
    }
    
    override suspend fun exists(remotePath: String): Result<Boolean> {
        return Result.success(true)
    }
    
    override fun getParentPath(remotePath: String): String? {
        val parent = remotePath.substringBeforeLast('/', "")
        return if (parent.isEmpty()) "/" else parent
    }
    
    override fun validatePath(remotePath: String): Result<Unit> {
        return Result.success(Unit)
    }
}