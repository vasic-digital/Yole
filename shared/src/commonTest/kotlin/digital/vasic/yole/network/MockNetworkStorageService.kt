package digital.vasic.yole.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import digital.vasic.yole.network.common.NetworkStorage
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.StorageConfig
import digital.vasic.yole.network.common.NetworkOperation
import digital.vasic.yole.network.common.SyncStatus

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
            progress = 0.0
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
            progress = 0.0
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
    
    override suspend fun createFolder(remotePath: String): Result<Unit> {
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
            Result.success(Unit)
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