package digital.vasic.yole.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import digital.vasic.yole.network.common.NetworkStorage
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.StorageConfig
import digital.vasic.yole.network.common.NetworkOperation
import digital.vasic.yole.network.common.SyncStatus
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.common.CacheEntry
import digital.vasic.yole.network.common.DocumentPermission

/**
 * Mock implementation of NetworkStorageService for testing purposes.
 * Provides in-memory simulation of network operations without requiring real servers.
 */
class MockNetworkStorageService(
    override val config: StorageConfig
) : NetworkStorageService {
    
    private val mockDocuments = mutableMapOf<String, NetworkDocument>()
    private val mockOperations = mutableListOf<NetworkOperation>()
    private var isConnected = false
    private var operationCounter = 0L
    
    init {
        // Initialize with some test documents
        initializeMockData()
    }
    
    override suspend fun connect(): Result<Unit> {
        return try {
            isConnected = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> {
        return try {
            isConnected = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override val isOnline: Boolean
        get() = isConnected
    
    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        if (!isConnected) {
            emit(Result.failure(Exception("Not connected")))
            return@flow
        }
        
        try {
            val documents = mockDocuments.values.filter { 
                it.path.startsWith(path) && it.path != path 
            }
            emit(Result.success(documents))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun downloadFile(
        remotePath: String, 
        localPath: String
    ): Flow<NetworkOperation> = flow {
        if (!isConnected) {
            emit(NetworkOperation(
                id = ++operationCounter,
                type = NetworkOperation.Type.DOWNLOAD,
                remotePath = remotePath,
                localPath = localPath,
                status = NetworkOperation.Status.FAILED,
                progress = 0.0,
                createdAt = kotlinx.datetime.Clock.System.now(),
                error = "Not connected"
            ))
            return@flow
        }
        
        val operation = NetworkOperation(
            id = ++operationCounter,
            type = NetworkOperation.Type.DOWNLOAD,
            remotePath = remotePath,
            localPath = localPath,
            status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.0,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        
        mockOperations.add(operation)
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.25))
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.50))
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.75))
        emit(operation.copy(status = NetworkOperation.Status.COMPLETED, progress = 1.0))
    }
    
    override suspend fun uploadFile(
        localPath: String, 
        remotePath: String
    ): Flow<NetworkOperation> = flow {
        if (!isConnected) {
            emit(NetworkOperation(
                id = ++operationCounter,
                type = NetworkOperation.Type.UPLOAD,
                remotePath = remotePath,
                localPath = localPath,
                status = NetworkOperation.Status.FAILED,
                progress = 0.0,
                createdAt = kotlinx.datetime.Clock.System.now(),
                error = "Not connected"
            ))
            return@flow
        }
        
        val operation = NetworkOperation(
            id = ++operationCounter,
            type = NetworkOperation.Type.UPLOAD,
            remotePath = remotePath,
            localPath = localPath,
            status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.0,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        
        mockOperations.add(operation)
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.25))
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.50))
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.75))
        emit(operation.copy(status = NetworkOperation.Status.COMPLETED, progress = 1.0))
    }
    
    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(Exception("Not connected"))
            }
            
            mockDocuments.remove(remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> {
        return try {
            if (!isConnected) {
                return Result.failure(Exception("Not connected"))
            }
            
            val folder = NetworkDocument(
                id = "folder_${operationCounter++}",
                name = remotePath.substringAfterLast('/'),
                path = remotePath,
                isFolder = true,
                size = 0L,
                lastModified = kotlinx.datetime.Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                documentId = null
            )
            
            mockDocuments[remotePath] = folder
            Result.success(folder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getActiveOperations(): Flow<List<NetworkOperation>> = flowOf(
        mockOperations.filter { it.status == NetworkOperation.Status.IN_PROGRESS }
    )
    
    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = config.name,
            name = config.name,
            type = config.storageType,
            location = "mock://localhost",
            totalSpace = 1000000000L, // 1GB
            usedSpace = mockDocuments.values.sumOf { it.size },
            isOnline = isConnected,
            lastSync = kotlinx.datetime.Clock.System.now()
        )
    }
    
    override suspend fun testConnection(): Result<Boolean> {
        return try {
            Result.success(isConnected)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(Exception("Not connected"))
            }
            
            val document = mockDocuments[remotePath]
            if (document != null) {
                val newPath = remotePath.substringBeforeLast('/') + "/" + newName
                val updatedDocument = document.copy(
                    name = newName,
                    path = newPath
                )
                mockDocuments.remove(remotePath)
                mockDocuments[newPath] = updatedDocument
                Result.success(Unit)
            } else {
                Result.failure(Exception("File not found: $remotePath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> {
        return try {
            if (!isConnected) {
                return Result.failure(Exception("Not connected"))
            }
            
            val document = mockDocuments[sourcePath]
            if (document != null) {
                val movedDocument = document.copy(
                    name = destinationPath.substringAfterLast('/'),
                    path = destinationPath
                )
                mockDocuments.remove(sourcePath)
                mockDocuments[destinationPath] = movedDocument
                Result.success(movedDocument)
            } else {
                Result.failure(Exception("File not found: $sourcePath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(Exception("Not connected"))
            }
            
            val document = mockDocuments[sourcePath]
            if (document != null) {
                val copiedDocument = document.copy(
                    id = "copy_${operationCounter++}",
                    name = destinationPath.substringAfterLast('/'),
                    path = destinationPath
                )
                mockDocuments[destinationPath] = copiedDocument
                Result.success(Unit)
            } else {
                Result.failure(Exception("File not found: $sourcePath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        return try {
            if (!isConnected) {
                return Result.failure(Exception("Not connected"))
            }
            
            val document = mockDocuments[remotePath]
            if (document != null) {
                Result.success(document)
            } else {
                Result.failure(Exception("File not found: $remotePath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelOperation(operationId: Long): Result<Unit> {
        return try {
            val operation = mockOperations.find { it.id == operationId }
            if (operation != null && operation.status == NetworkOperation.Status.IN_PROGRESS) {
                val cancelledOperation = operation.copy(
                    status = NetworkOperation.Status.CANCELLED,
                    completedAt = kotlinx.datetime.Clock.System.now()
                )
                mockOperations.remove(operation)
                mockOperations.add(cancelledOperation)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Operation not found or not in progress"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun pauseOperation(operationId: Long): Result<Unit> {
        return try {
            val operation = mockOperations.find { it.id == operationId }
            if (operation != null && operation.status == NetworkOperation.Status.IN_PROGRESS) {
                val pausedOperation = operation.copy(
                    status = NetworkOperation.Status.PAUSED,
                    isPaused = true
                )
                mockOperations.remove(operation)
                mockOperations.add(pausedOperation)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Operation not found or not in progress"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resumeOperation(operationId: Long): Result<Unit> {
        return try {
            val operation = mockOperations.find { it.id == operationId }
            if (operation != null && operation.status == NetworkOperation.Status.PAUSED) {
                val resumedOperation = operation.copy(
                    status = NetworkOperation.Status.IN_PROGRESS,
                    isPaused = false
                )
                mockOperations.remove(operation)
                mockOperations.add(resumedOperation)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Operation not found or not paused"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> = flowOf(emptyList())
    
    override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> {
        return Result.success(Unit) // Mock implementation
    }
    
    override suspend fun removeFromCache(remotePath: String): Result<Unit> {
        return Result.success(Unit) // Mock implementation
    }
    
    override suspend fun clearCache(): Result<Unit> {
        return Result.success(Unit) // Mock implementation
    }
    
    override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> = flowOf(emptyMap())
    
    override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> = flow {
        val operation = NetworkOperation(
            id = ++operationCounter,
            type = NetworkOperation.Type.SYNC,
            remotePath = remotePath,
            status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.0,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        
        mockOperations.add(operation)
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.25))
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.50))
        emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.75))
        emit(operation.copy(status = NetworkOperation.Status.COMPLETED, progress = 1.0))
    }
    
    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        // Return empty flow for mock implementation
    }
    
    override fun searchFiles(
        query: String,
        path: String?,
        includeContent: Boolean
    ): Flow<Result<List<NetworkDocument>>> = flow {
        if (!isConnected) {
            emit(Result.failure(Exception("Not connected")))
            return@flow
        }
        
        try {
            val documents = mockDocuments.values.filter { 
                it.name.contains(query, ignoreCase = true) &&
                (path == null || it.path.startsWith(path))
            }
            emit(Result.success(documents))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override fun getRecentChanges(
        since: kotlinx.datetime.Instant,
        path: String?
    ): Flow<List<NetworkDocument>> = flow {
        if (!isConnected) {
            emit(emptyList())
            return@flow
        }
        
        try {
            val documents = mockDocuments.values.filter { 
                it.lastModified > since &&
                (path == null || it.path.startsWith(path))
            }
            emit(documents)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        return try {
            if (!isConnected) {
                return Result.failure(Exception("Not connected"))
            }
            
            val totalSpace = 1000000000L // 1GB
            val usedSpace = mockDocuments.values.sumOf { it.size }
            val availableSpace = totalSpace - usedSpace
            
            Result.success(StorageQuota(
                totalSpace = totalSpace,
                usedSpace = usedSpace,
                availableSpace = availableSpace,
                usagePercentage = (usedSpace.toDouble() / totalSpace * 100),
                isFull = availableSpace <= 0,
                isLowOnSpace = availableSpace < totalSpace * 0.1
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun exists(remotePath: String): Result<Boolean> {
        return try {
            if (!isConnected) {
                return Result.failure(Exception("Not connected"))
            }
            
            Result.success(mockDocuments.containsKey(remotePath))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getParentPath(remotePath: String): String? {
        return if (remotePath == "/" || remotePath.isBlank()) null 
        else remotePath.substringBeforeLast("/", "/")
    }
    
    override fun validatePath(remotePath: String): Result<Unit> {
        return try {
            if (remotePath.isBlank()) {
                Result.failure(Exception("Path cannot be blank"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override val rootPath: String
        get() = "/"
    
    private fun initializeMockData() {
        val now = kotlinx.datetime.Clock.System.now()
        
        // Add some test documents
        mockDocuments["/test.md"] = NetworkDocument(
            id = "doc1",
            name = "test.md",
            path = "/test.md",
            isFolder = false,
            size = 1024L,
            lastModified = now,
            syncStatus = SyncStatus.SYNCED,
            documentId = null
        )
        
        mockDocuments["/notes/"] = NetworkDocument(
            id = "folder1",
            name = "notes",
            path = "/notes/",
            isFolder = true,
            size = 0L,
            lastModified = now,
            syncStatus = SyncStatus.SYNCED,
            documentId = null
        )
        
        mockDocuments["/notes/todo.txt"] = NetworkDocument(
            id = "doc2",
            name = "todo.txt",
            path = "/notes/todo.txt",
            isFolder = false,
            size = 512L,
            lastModified = now,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            documentId = null
        )
    }
}