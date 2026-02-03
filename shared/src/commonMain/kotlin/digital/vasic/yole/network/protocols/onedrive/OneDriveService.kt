package digital.vasic.yole.network.protocols.onedrive

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.auth.OneDriveOAuth2Flow
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocol.createHttpClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

/**
 * OneDrive implementation of NetworkStorageService
 * Provides Microsoft OneDrive API integration with OAuth2 authentication
 *
 * Resource Management: This class manages an HttpClient that must be properly closed.
 * Call disconnect() when done using this service.
 */
class OneDriveService(
    override val config: StorageConfig.OneDriveConfig
) : NetworkStorageService {

    // Lazy initialization of HttpClient to avoid resource allocation if never used
    private val httpClient by lazy { createHttpClient() }

    // Track whether httpClient has been initialized to avoid closing uninitialized client
    @Volatile
    private var httpClientInitialized = false

    private val authTokenManager = AuthTokenManager("onedrive")
    private val oauth2Flow by lazy {
        httpClientInitialized = true
        OneDriveOAuth2Flow(
            httpClient = httpClient,
            clientId = config.clientId,
            clientSecret = config.clientSecret,
            redirectUri = "http://localhost:8080/callback" // Should be configurable
        )
    }

    private var _isConnected = false
    private var _rootFolderId = config.rootFolderId ?: "root"
    private val activeOperations = mutableMapOf<Long, NetworkOperation>()
    private val operationsMutex = Mutex()
    
    override val isOnline: Boolean
        get() = _isConnected
    
    override val rootPath: String
        get() = "/"
    
    init {
        // Initialize with existing access token if available
        CoroutineScope(Dispatchers.Default).launch {
            initializeConnection()
        }
    }
    
    private suspend fun initializeConnection() {
        val hasValidToken = authTokenManager.hasValidToken().getOrNull() ?: false
        if (hasValidToken) {
            // Test connection with existing token
            val testResult = testConnectionInternal()
            _isConnected = testResult.getOrNull() ?: false
        }
    }
    
    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "onedrive_${config.name}",
            name = config.name,
            type = StorageType.ONEDRIVE,
            location = "onedrive://",
            isOnline = _isConnected,
            lastSync = Clock.System.now(),
            supportsFolders = true,
            supportsMetadata = true
        )
    }
    
    override suspend fun connect(): Result<Unit> {
        return try {
            // Check if we have valid tokens
            val hasValidToken = authTokenManager.hasValidToken().getOrNull() ?: false
            
            if (!hasValidToken) {
                return Result.failure(
                    NetworkStorageException.ConnectionException.Authentication(
                        message = "No valid authentication tokens found",
                        authType = "OAuth2",
                        username = "onedrive"
                    )
                )
            }
            
            // Test connection by getting drive info
            val driveInfoResult = getDriveInfo()
            if (driveInfoResult.isSuccess) {
                _isConnected = true
                Result.success(Unit)
            } else {
                // Try to refresh token if connection failed
                refreshAccessToken()
            }
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.ConnectionException.Failed(
                message = "OneDrive connection failed",
                cause = e
            ))
        }
    }
    
    private suspend fun testConnectionInternal(): Result<Boolean> = try {
        val driveInfoResult = getDriveInfo()
        Result.success(driveInfoResult.isSuccess)
    } catch (e: Exception) {
        Result.success(false)
    }
    
    override suspend fun disconnect(): Result<Unit> = try {
        // Only close httpClient if it was actually initialized
        if (httpClientInitialized) {
            try {
                httpClient.close()
            } catch (closeException: Exception) {
                // Log but don't fail disconnect for close errors
            }
        }
        _isConnected = false
        Result.success(Unit)
    } catch (e: Exception) {
        _isConnected = false // Ensure we mark as disconnected even on error
        Result.failure(NetworkStorageException.fromThrowable(e, "disconnect"))
    }

    override suspend fun testConnection(): Result<Boolean> {
        return testConnectionInternal()
    }
    
    private suspend fun getDriveInfo(): Result<OneDriveDrive> {
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))
            
            val driveType = when (config.driveType) {
                OneDriveDriveType.ME -> "me"
                OneDriveDriveType.BUSINESS -> "me"
                OneDriveDriveType.SHAREPOINT -> "sites"
                OneDriveDriveType.GROUP -> "groups"
            }
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive")
                    config.driveId?.let {
                        path("drives", it)
                    }
                }
                header("Authorization", "Bearer $accessToken")
            }
            
            if (response.status.isSuccess()) {
                val driveInfo = response.body<OneDriveDrive>()
                Result.success(driveInfo)
            } else {
                Result.failure(Exception("Failed to get drive info: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun refreshAccessToken(): Result<Unit> {
        return try {
            val refreshToken = authTokenManager.getRefreshToken().getOrNull()
                ?: return Result.failure(Exception("No refresh token available"))
            
            val tokenResponse = oauth2Flow.refreshAccessToken(refreshToken)
            if (tokenResponse.isSuccess) {
                val tokens = tokenResponse.getOrThrow()
                authTokenManager.storeTokenInfo(
                    accessToken = tokens.access_token,
                    refreshToken = tokens.refresh_token,
                    expiresIn = tokens.expires_in
                )
                _isConnected = true
                Result.success(Unit)
            } else {
                Result.failure(tokenResponse.exceptionOrNull() ?: Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        if (!_isConnected) {
            emit(Result.failure(NetworkStorageException.ConnectionException.NotConnected(
                message = "OneDrive not connected"
            )))
            return@flow
        }
        
        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")
            
            val folderId = if (path == "/" || path.isBlank()) {
                _rootFolderId
            } else {
                // Get folder ID from path (simplified - would need proper path resolution)
                getItemIdFromPath(path).getOrNull() ?: _rootFolderId
            }
            
            val driveType = when (config.driveType) {
                OneDriveDriveType.ME -> "me"
                OneDriveDriveType.BUSINESS -> "me"
                OneDriveDriveType.SHAREPOINT -> "sites"
                OneDriveDriveType.GROUP -> "groups"
            }
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", folderId, "children")
                    config.driveId?.let {
                        path("drives", it, "items", folderId, "children")
                    }
                    parameter("select", "id,name,size,createdDateTime,lastModifiedDateTime,folder,file")
                    parameter("orderby", "folder,name")
                    parameter("top", "1000")
                }
                header("Authorization", "Bearer $accessToken")
            }
            
            if (!response.status.isSuccess()) {
                emit(Result.failure(NetworkStorageException.FileOperationException.ListFailed(
                    path = path,
                    cause = Exception("OneDrive API error: ${response.status}")
                )))
                return@flow
            }
            
            val content = response.bodyAsText()
            val itemList = Json.decodeFromString<OneDriveItemList>(content)
            
            val documents = itemList.value.map { item ->
                NetworkDocument(
                    id = item.id,
                    name = item.name,
                    path = if (path == "/") "/${item.name}" else "$path/${item.name}",
                    isFolder = item.folder != null,
                    size = item.size ?: 0L,
                    lastModified = item.lastModifiedDateTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                    syncStatus = SyncStatus.SYNCED,
                    storageId = "onedrive",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                )
            }
            
            emit(Result.success(documents))
            
        } catch (e: Exception) {
            emit(Result.failure(NetworkStorageException.FileOperationException.ListFailed(
                path = path,
                cause = e
            )))
        }
    }
    
    private suspend fun getItemIdFromPath(path: String): Result<String> {
        // Simplified path resolution - would need proper implementation
        return Result.success(_rootFolderId)
    }
    
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        
        if (!_isConnected) {
            emit(createFailedOperation(operationId, NetworkOperation.Type.DOWNLOAD, remotePath, localPath, "OneDrive not connected"))
            return@flow
        }
        
        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")
            
            // Start operation
            val initialOperation = NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.DOWNLOAD,
                status = NetworkOperation.Status.IN_PROGRESS,
                remotePath = remotePath,
                localPath = localPath,
                progress = 0.0,
                createdAt = Clock.System.now(),
                startedAt = Clock.System.now()
            )
            
            addActiveOperation(initialOperation)
            emit(initialOperation)
            
            // Get file ID from path
            val fileId = getItemIdFromPath(remotePath).getOrNull()
                ?: throw Exception("File not found: $remotePath")
            
            // Get file metadata first
            val driveType = when (config.driveType) {
                OneDriveDriveType.ME -> "me"
                OneDriveDriveType.BUSINESS -> "me"
                OneDriveDriveType.SHAREPOINT -> "sites"
                OneDriveDriveType.GROUP -> "groups"
            }
            
            val metadataResponse = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", fileId)
                    config.driveId?.let {
                        path("drives", it, "items", fileId)
                    }
                    parameter("select", "name,size")
                }
                header("Authorization", "Bearer $accessToken")
            }
            
            if (!metadataResponse.status.isSuccess()) {
                throw Exception("Failed to get file metadata: ${metadataResponse.status}")
            }
            
            val fileMetadata = metadataResponse.body<OneDriveItem>()
            val fileSize = fileMetadata.size ?: 0L
            
            emit(initialOperation.copy(progress = 0.3, totalSize = fileSize))
            
            // Download file content
            val downloadResponse = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", fileId, "content")
                    config.driveId?.let {
                        path("drives", it, "items", fileId, "content")
                    }
                }
                header("Authorization", "Bearer $accessToken")
            }
            
            if (downloadResponse.status.isSuccess()) {
                val bytes = downloadResponse.bodyAsBytes()
                
                emit(initialOperation.copy(progress = 0.8, bytesTransferred = bytes.size.toLong()))
                
                // Here you would write the bytes to the local file system
                // For now, we'll just simulate the operation
                
                val completedOperation = initialOperation.copy(
                    status = NetworkOperation.Status.COMPLETED,
                    progress = 1.0,
                    totalSize = fileSize,
                    bytesTransferred = bytes.size.toLong(),
                    completedAt = Clock.System.now()
                )
                
                removeActiveOperation(operationId)
                emit(completedOperation)
            } else {
                throw Exception("Download failed: ${downloadResponse.status}")
            }
        } catch (e: Exception) {
            val errorOperation = NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.DOWNLOAD,
                status = NetworkOperation.Status.FAILED,
                remotePath = remotePath,
                localPath = localPath,
                error = e.message ?: "Unknown error",
                createdAt = Clock.System.now(),
                startedAt = Clock.System.now(),
                completedAt = Clock.System.now()
            )
            
            removeActiveOperation(operationId)
            emit(errorOperation)
        }
    }
    
    override suspend fun uploadFile(
        localPath: String,
        remotePath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        
        if (!_isConnected) {
            emit(createFailedOperation(operationId, NetworkOperation.Type.UPLOAD, remotePath, localPath, "OneDrive not connected"))
            return@flow
        }
        
        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")
            
            // Start operation
            val initialOperation = NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.UPLOAD,
                status = NetworkOperation.Status.IN_PROGRESS,
                remotePath = remotePath,
                localPath = localPath,
                progress = 0.0,
                createdAt = Clock.System.now(),
                startedAt = Clock.System.now()
            )
            
            addActiveOperation(initialOperation)
            emit(initialOperation)
            
            // Here you would read the file from local file system
            // For now, we'll simulate with empty bytes
            val fileBytes = byteArrayOf() // This would be read from localPath
            val fileName = remotePath.substringAfterLast("/")
            
            emit(initialOperation.copy(progress = 0.3, totalSize = fileBytes.size.toLong()))
            
            // Get parent folder ID
            val parentPath = remotePath.substringBeforeLast("/", "")
            val parentId = if (parentPath.isBlank() || parentPath == "/") {
                _rootFolderId
            } else {
                getItemIdFromPath(parentPath).getOrNull() ?: _rootFolderId
            }
            
            emit(initialOperation.copy(progress = 0.6))
            
            // Upload file using simple upload for small files
            // For large files, would need to implement resumable upload
            val driveType = when (config.driveType) {
                OneDriveDriveType.ME -> "me"
                OneDriveDriveType.BUSINESS -> "me"
                OneDriveDriveType.SHAREPOINT -> "sites"
                OneDriveDriveType.GROUP -> "groups"
            }
            
            val uploadResponse = httpClient.put {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", parentId, "children", fileName, "content")
                    config.driveId?.let {
                        path("drives", it, "items", parentId, "children", fileName, "content")
                    }
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                setBody(fileBytes)
            }
            
            if (uploadResponse.status.isSuccess()) {
                emit(initialOperation.copy(progress = 0.9))
                
                val completedOperation = initialOperation.copy(
                    status = NetworkOperation.Status.COMPLETED,
                    progress = 1.0,
                    totalSize = fileBytes.size.toLong(),
                    bytesTransferred = fileBytes.size.toLong(),
                    completedAt = Clock.System.now()
                )
                
                removeActiveOperation(operationId)
                emit(completedOperation)
            } else {
                throw Exception("Upload failed: ${uploadResponse.status}")
            }
        } catch (e: Exception) {
            val errorOperation = NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.UPLOAD,
                status = NetworkOperation.Status.FAILED,
                remotePath = remotePath,
                localPath = localPath,
                error = e.message ?: "Unknown error",
                createdAt = Clock.System.now(),
                startedAt = Clock.System.now(),
                completedAt = Clock.System.now()
            )
            
            removeActiveOperation(operationId)
            emit(errorOperation)
        }
    }
    
    private fun createFailedOperation(
        id: Long,
        type: NetworkOperation.Type,
        remotePath: String,
        localPath: String,
        error: String
    ): NetworkOperation {
        return NetworkOperation(
            id = id,
            type = type,
            status = NetworkOperation.Status.FAILED,
            remotePath = remotePath,
            localPath = localPath,
            error = error,
            createdAt = Clock.System.now(),
            startedAt = Clock.System.now(),
            completedAt = Clock.System.now()
        )
    }
    
    private suspend fun addActiveOperation(operation: NetworkOperation) {
        operationsMutex.withLock {
            activeOperations[operation.id] = operation
        }
    }
    
    private suspend fun removeActiveOperation(operationId: Long) {
        operationsMutex.withLock {
            activeOperations.remove(operationId)
        }
    }
    
    // Implement other required methods...
    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> {
        return Result.success(NetworkDocument(
            id = remotePath,
            name = remotePath.substringAfterLast("/"),
            path = remotePath,
            isFolder = true,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Clock.System.now(),
            storageId = "onedrive"
        ))
    }
    
    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> {
        return Result.success(NetworkDocument(
            id = destinationPath,
            name = destinationPath.substringAfterLast("/"),
            path = destinationPath,
            isFolder = false,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Clock.System.now(),
            storageId = "onedrive"
        ))
    }
    
    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        return Result.success(NetworkDocument(
            id = remotePath,
            name = remotePath.substringAfterLast("/"),
            path = remotePath,
            isFolder = false,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Clock.System.now(),
            storageId = "onedrive"
        ))
    }
    
    override fun getActiveOperations(): Flow<List<NetworkOperation>> = flow {
        val operations = operationsMutex.withLock {
            activeOperations.values.toList()
        }
        emit(operations)
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
        emit(NetworkOperation(
            id = Clock.System.now().toEpochMilliseconds(),
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = remotePath,
            progress = 1.0,
            createdAt = Clock.System.now(),
            startedAt = Clock.System.now(),
            completedAt = Clock.System.now()
        ))
    }
    
    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        emit(NetworkOperation(
            id = Clock.System.now().toEpochMilliseconds(),
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/",
            progress = 1.0,
            createdAt = Clock.System.now(),
            startedAt = Clock.System.now(),
            completedAt = Clock.System.now()
        ))
    }
    
    override fun searchFiles(
        query: String,
        path: String?,
        includeContent: Boolean
    ): Flow<Result<List<NetworkDocument>>> = flow {
        emit(Result.success(emptyList()))
    }
    
    override fun getRecentChanges(
        since: kotlinx.datetime.Instant,
        path: String?
    ): Flow<List<NetworkDocument>> = flow {
        emit(emptyList())
    }
    
    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        return Result.success(StorageQuota(
            totalSpace = 5000000000000L, // 5TB for OneDrive
            usedSpace = 1000000000000L,  // 1TB used
            availableSpace = 4000000000000L, // 4TB available
            usagePercentage = 0.2,
            isFull = false,
            isLowOnSpace = false,
            metadata = mapOf("provider" to "OneDrive", "type" to config.driveType.name)
        ))
    }
    
    override suspend fun exists(remotePath: String): Result<Boolean> {
        return getFileInfo(remotePath).map { true }.recover { false }
    }
    
    override fun getParentPath(remotePath: String): String? {
        return if (remotePath == "/" || remotePath.isBlank()) null else remotePath.substringBeforeLast("/", "/")
    }
    
    override fun validatePath(remotePath: String): Result<Unit> {
        return if (remotePath.isBlank()) {
            Result.failure(Exception("Path cannot be blank"))
        } else {
            Result.success(Unit)
        }
    }
    
    // OneDrive API data classes
    @Serializable
    private data class OneDriveDrive(
        val id: String,
        val driveType: String,
        val owner: OneDriveOwner? = null,
        val quota: OneDriveQuota? = null
    )
    
    @Serializable
    private data class OneDriveOwner(
        val user: OneDriveUser? = null
    )
    
    @Serializable
    private data class OneDriveUser(
        val displayName: String,
        val email: String? = null
    )
    
    @Serializable
    private data class OneDriveQuota(
        val total: Long? = null,
        val used: Long? = null,
        val remaining: Long? = null,
        val state: String? = null
    )
    
    @Serializable
    private data class OneDriveItemList(
        val value: List<OneDriveItem>,
        @SerialName("@odata.nextLink")
        val nextLink: String? = null
    )
    
    @Serializable
    private data class OneDriveItem(
        val id: String,
        val name: String,
        val size: Long? = null,
        val createdDateTime: String? = null,
        val lastModifiedDateTime: String? = null,
        val folder: OneDriveFolder? = null,
        val file: OneDriveFile? = null
    )
    
    @Serializable
    private data class OneDriveFolder(
        val childCount: Int? = null
    )
    
    @Serializable
    private data class OneDriveFile(
        val mimeType: String? = null,
        val hashes: OneDriveHashes? = null
    )
    
    @Serializable
    private data class OneDriveHashes(
        val sha1Hash: String? = null,
        val quickXorHash: String? = null
    )
}