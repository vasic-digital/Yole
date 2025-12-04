package digital.vasic.yole.network.protocol

import digital.vasic.yole.network.common.*
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
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                syncStatus = SyncStatus.SYNCED,
                type = DocumentType.FILE,
                size = 1024L,
                lastModified = Clock.System.now()
            )
        )))
    }
    
    override suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation> = flow {
        // Mock download operation
        emit(NetworkOperation(
            id = System.currentTimeMillis(),
            type = OperationType.DOWNLOAD,
            status = OperationStatus.COMPLETED,
            sourcePath = remotePath,
            targetPath = localPath,
            progress = 1.0f,
            bytesTransferred = 1024L,
            totalBytes = 1024L
        ))
    }
    
    override suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation> = flow {
        // Mock upload operation
        emit(NetworkOperation(
            id = System.currentTimeMillis(),
            type = OperationType.UPLOAD,
            status = OperationStatus.COMPLETED,
            sourcePath = localPath,
            targetPath = remotePath,
            progress = 1.0f,
            bytesTransferred = 1024L,
            totalBytes = 1024L
        ))
    }
    
    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun createFolder(remotePath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        return Result.success(NetworkDocument(
            id = "test1",
            name = "Test File.md",
            path = remotePath,
            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
            syncStatus = SyncStatus.SYNCED,
            type = DocumentType.FILE,
            size = 1024L,
            lastModified = Clock.System.now()
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
            lastSync = Clock.System.now(),
            supportsFolders = true,
            supportsMetadata = true,
            supportsPermissions = true,
            supportsEncryption = true
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
            id = System.currentTimeMillis(),
            type = OperationType.SYNC,
            status = OperationStatus.COMPLETED,
            sourcePath = remotePath,
            targetPath = remotePath,
            progress = 1.0f,
            bytesTransferred = 1024L,
            totalBytes = 1024L
        ))
    }
    
    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        // Mock sync operations
        emit(NetworkOperation(
            id = System.currentTimeMillis(),
            type = OperationType.SYNC,
            status = OperationStatus.COMPLETED,
            sourcePath = "/",
            targetPath = "/",
            progress = 1.0f,
            bytesTransferred = 1024L,
            totalBytes = 1024L
        ))
    }
    
    override fun searchFiles(query: String, path: String?, includeContent: Boolean): Flow<Result<List<NetworkDocument>>> = flow {
        emit(Result.success(listOf(
            NetworkDocument(
                id = "search1",
                name = "Search Result.md",
                path = "/Search Result.md",
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                syncStatus = SyncStatus.SYNCED,
                type = DocumentType.FILE,
                size = 2048L,
                lastModified = Clock.System.now()
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
            isLowOnSpace = false
        ))
    }
    
    override suspend fun exists(remotePath: String): Result<Boolean> {
        return Result.success(true)
    }
    
    override fun getParentPath(remotePath: String): String? {
        return if (remotePath == "/") null else remotePath.substringBeforeLast("/")
    }
    
    override fun validatePath(remotePath: String): Result<Unit> {
        return Result.success(Unit)
    }
}