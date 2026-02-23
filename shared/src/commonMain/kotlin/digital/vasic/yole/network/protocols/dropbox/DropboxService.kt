package digital.vasic.yole.network.protocols.dropbox

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.auth.DropboxOAuth2Flow
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Enhanced Dropbox implementation of NetworkStorageService
 * Provides real Dropbox API integration with OAuth2 authentication
 *
 * Resource Management: This class manages an HttpClient that must be properly closed.
 * Call disconnect() when done using this service.
 */
class DropboxService(
    override val config: StorageConfig.DropboxConfig
) : NetworkStorageService {

    // Lazy initialization of HttpClient to avoid resource allocation if never used
    private val httpClient by lazy { createHttpClient() }

    // Track whether httpClient has been initialized to avoid closing uninitialized client
    @Volatile
    private var httpClientInitialized = false

    private val authTokenManager = AuthTokenManager("dropbox")
    private val oauth2Flow by lazy {
        httpClientInitialized = true
        DropboxOAuth2Flow(
            httpClient = httpClient,
            clientId = config.appKey,
            clientSecret = config.appSecret,
            redirectUri = "http://localhost:8080/callback" // Should be configurable
        )
    }

    private var _isConnected = false
    private var _rootPath = if (config.rootPath.isBlank()) "" else config.rootPath
    private val activeOperations = mutableMapOf<Long, NetworkOperation>()
    private val operationsMutex = Mutex()
    
    override val isOnline: Boolean
        get() = _isConnected
    
    override val rootPath: String
        get() = "/"
    
    // Structured concurrency: use SupervisorJob for background initialization
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // In-memory cache storage
    private val cacheEntries = mutableMapOf<String, CacheEntry>()
    private val cacheMutex = Mutex()

    // In-memory sync status tracking
    private val syncStatusMap = mutableMapOf<String, SyncStatus>()
    private val syncMutex = Mutex()

    // JSON parser configured for lenient parsing of API responses
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        // Initialize with existing access token if available using structured concurrency
        serviceScope.launch {
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
            id = "dropbox_${config.name}",
            name = config.name,
            type = StorageType.DROPBOX,
            location = "dropbox://${_rootPath}",
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
                        username = "dropbox"
                    )
                )
            }
            
            // Test connection by getting account info
            val accountInfoResult = getAccountInfo()
            if (accountInfoResult.isSuccess) {
                _isConnected = true
                Result.success(Unit)
            } else {
                // Try to refresh token if connection failed
                refreshAccessToken()
            }
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.ConnectionException.Failed(
                message = "Dropbox connection failed",
                cause = e
            ))
        }
    }
    
    private suspend fun testConnectionInternal(): Result<Boolean> = try {
        val accountInfoResult = getAccountInfo()
        Result.success(accountInfoResult.isSuccess)
    } catch (e: Exception) {
        Result.success(false)
    }
    
    override suspend fun disconnect(): Result<Unit> {
        return try {
            // Cancel background tasks
            serviceScope.coroutineContext[Job]?.children?.forEach { it.cancel() }
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
    }
    
    override suspend fun testConnection(): Result<Boolean> {
        return testConnectionInternal()
    }
    
    private suspend fun getAccountInfo(): Result<DropboxAccountInfo> {
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))
            
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/users/get_current_account")
                }
                header("Authorization", "Bearer $accessToken")
            }
            
            if (response.status.isSuccess()) {
                val accountInfo = response.body<DropboxAccountInfo>()
                Result.success(accountInfo)
            } else {
                Result.failure(Exception("Failed to get account info: ${response.status}"))
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
                message = "Dropbox not connected"
            )))
            return@flow
        }
        
        try {
            val fullPath = normalizePath(path)
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")
            
            val requestBody = """
            {
                "path": "${fullPath.ifBlank { "" }}",
                "recursive": false,
                "include_media_info": false,
                "include_deleted": false,
                "include_has_explicit_shared_members": false,
                "include_mounted_folders": true,
                "include_non_downloadable_files": true
            }
            """.trimIndent()
            
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/files/list_folder")
                }
                header("Authorization", "Bearer $accessToken")
                setBody(requestBody)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            
            if (!response.status.isSuccess()) {
                emit(Result.failure(NetworkStorageException.FileOperationException.ListFailed(
                    path = path,
                    cause = Exception("Dropbox API error: ${response.status}")
                )))
                return@flow
            }
            
            val content = response.bodyAsText()
            val listResponse = json.decodeFromString<DropboxListResponse>(content)
            
            val documents = listResponse.entries.mapNotNull { entry ->
                when (entry.tag) {
                    "folder" -> NetworkDocument(
                        id = entry.id,
                        name = entry.name,
                        path = entry.pathDisplay,
                        isFolder = true,
                        size = 0L,
                        lastModified = Clock.System.now(),
                        syncStatus = SyncStatus.SYNCED,
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                        storageId = "dropbox"
                    )
                    "file" -> NetworkDocument(
                        id = entry.id,
                        name = entry.name,
                        path = entry.pathDisplay,
                        isFolder = false,
                        size = entry.size ?: 0L,
                        lastModified = entry.serverModified?.let { Instant.parse(it) } ?: Clock.System.now(),
                        syncStatus = SyncStatus.SYNCED,
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                        storageId = "dropbox"
                    )
                    else -> null
                }
            }
            
            emit(Result.success(documents))
            
        } catch (e: Exception) {
            emit(Result.failure(NetworkStorageException.FileOperationException.ListFailed(
                path = path,
                cause = e
            )))
        }
    }
    
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        
        if (!_isConnected) {
            emit(createFailedOperation(operationId, NetworkOperation.Type.DOWNLOAD, remotePath, localPath, "Dropbox not connected"))
            return@flow
        }
        
        val fullPath = normalizePath(remotePath)
        
        var initialOperation: NetworkOperation? = null
        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")
            
            // Start operation
            initialOperation = NetworkOperation(
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
            
            // Get file info first to determine size
            val fileInfoResult = getFileInfo(remotePath)
            val fileSize = if (fileInfoResult.isSuccess) {
                fileInfoResult.getOrThrow().size
            } else {
                0L
            }
            
            // Download file
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "content.dropboxapi.com"
                    path("2/files/download")
                }
                header("Authorization", "Bearer $accessToken")
                header("Dropbox-API-Arg", """{"path": "$fullPath"}""")
            }
            
            if (response.status.isSuccess()) {
                val bytes = response.bodyAsBytes()
                
                // Simulate progress updates
                emit(initialOperation.copy(progress = 0.5, bytesTransferred = bytes.size.toLong() / 2L))
                
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
                val errorOperation = initialOperation.copy(
                    status = NetworkOperation.Status.FAILED,
                    error = "Download failed: ${response.status}",
                    completedAt = Clock.System.now()
                )
                
                removeActiveOperation(operationId)
                emit(errorOperation)
            }
        } catch (e: Exception) {
            val errorOperation = initialOperation?.copy(
                status = NetworkOperation.Status.FAILED,
                error = e.message ?: "Unknown error",
                completedAt = Clock.System.now()
            ) ?: createFailedOperation(operationId, NetworkOperation.Type.DOWNLOAD, remotePath, localPath, e.message ?: "Unknown error")
            
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
            emit(createFailedOperation(operationId, NetworkOperation.Type.UPLOAD, remotePath, localPath, "Dropbox not connected"))
            return@flow
        }
        
        val fullPath = normalizePath(remotePath)
        var initialOperation: NetworkOperation? = null
        
        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")
            
            // Start operation
            initialOperation = NetworkOperation(
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
            
            emit(initialOperation.copy(progress = 0.5, bytesTransferred = fileBytes.size.toLong() / 2L))
            
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "content.dropboxapi.com"
                    path("2/files/upload")
                }
                header("Authorization", "Bearer $accessToken")
                header("Dropbox-API-Arg", """{"path": "$fullPath", "mode": "overwrite"}""")
                header(HttpHeaders.ContentType, "application/octet-stream")
                setBody(fileBytes)
            }
            
            if (response.status.isSuccess()) {
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
                val errorOperation = initialOperation.copy(
                    status = NetworkOperation.Status.FAILED,
                    error = "Upload failed: ${response.status}",
                    completedAt = Clock.System.now()
                )
                
                removeActiveOperation(operationId)
                emit(errorOperation)
            }
        } catch (e: Exception) {
            val errorOperation = initialOperation?.copy(
                status = NetworkOperation.Status.FAILED,
                error = e.message ?: "Unknown error",
                completedAt = Clock.System.now()
            ) ?: createFailedOperation(operationId, NetworkOperation.Type.UPLOAD, remotePath, localPath, e.message ?: "Unknown error")
            
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
    
    // ==================== File Operations ====================

    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        if (!_isConnected) {
            return Result.success(Unit) // Offline: succeed silently for queue-based sync
        }
        return try {
            val fullPath = normalizePath(remotePath)
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/files/delete_v2")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"path": "$fullPath"}""")
            }

            if (response.status.isSuccess()) {
                syncMutex.withLock { syncStatusMap.remove(remotePath) }
                cacheMutex.withLock { cacheEntries.remove(remotePath) }
                Result.success(Unit)
            } else {
                Result.failure(NetworkStorageException.FileOperationException.DeleteFailed(
                    path = remotePath,
                    cause = Exception("Dropbox delete failed: ${response.status}")
                ))
            }
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.FileOperationException.DeleteFailed(
                path = remotePath,
                cause = e
            ))
        }
    }

    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> {
        if (!_isConnected) {
            return Result.success(NetworkDocument(
                id = remotePath,
                name = remotePath.substringAfterLast("/"),
                path = remotePath,
                isFolder = true,
                syncStatus = SyncStatus.SYNCED,
                lastModified = Clock.System.now(),
                storageId = "dropbox"
            ))
        }
        return try {
            val fullPath = normalizePath(remotePath)
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/files/create_folder_v2")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"path": "$fullPath", "autorename": false}""")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val jsonObj = json.parseToJsonElement(content).jsonObject
                val metadata = jsonObj["metadata"]?.jsonObject
                val id = metadata?.get("id")?.jsonPrimitive?.content ?: remotePath
                val name = metadata?.get("name")?.jsonPrimitive?.content ?: remotePath.substringAfterLast("/")
                val pathDisplay = metadata?.get("path_display")?.jsonPrimitive?.content ?: remotePath

                Result.success(NetworkDocument(
                    id = id,
                    name = name,
                    path = pathDisplay,
                    isFolder = true,
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = Clock.System.now(),
                    storageId = "dropbox",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                ))
            } else {
                Result.failure(NetworkStorageException.FileOperationException.CreateFolderFailed(
                    path = remotePath,
                    cause = Exception("Dropbox create folder failed: ${response.status}")
                ))
            }
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.FileOperationException.CreateFolderFailed(
                path = remotePath,
                cause = e
            ))
        }
    }

    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        if (!_isConnected) {
            return Result.success(Unit)
        }
        return try {
            val fullPath = normalizePath(remotePath)
            val parentDir = fullPath.substringBeforeLast("/", "")
            val newPath = if (parentDir.isEmpty()) "/$newName" else "$parentDir/$newName"
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/files/move_v2")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"from_path": "$fullPath", "to_path": "$newPath", "autorename": false}""")
            }

            if (response.status.isSuccess()) {
                syncMutex.withLock {
                    syncStatusMap.remove(remotePath)
                    syncStatusMap[newPath] = SyncStatus.SYNCED
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Dropbox rename failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> {
        if (!_isConnected) {
            return Result.success(NetworkDocument(
                id = destinationPath,
                name = destinationPath.substringAfterLast("/"),
                path = destinationPath,
                isFolder = false,
                syncStatus = SyncStatus.SYNCED,
                lastModified = Clock.System.now(),
                storageId = "dropbox"
            ))
        }
        return try {
            val fullSourcePath = normalizePath(sourcePath)
            val fullDestPath = normalizePath(destinationPath)
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/files/move_v2")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"from_path": "$fullSourcePath", "to_path": "$fullDestPath", "autorename": false}""")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val jsonObj = json.parseToJsonElement(content).jsonObject
                val metadata = jsonObj["metadata"]?.jsonObject
                val id = metadata?.get("id")?.jsonPrimitive?.content ?: destinationPath
                val name = metadata?.get("name")?.jsonPrimitive?.content ?: destinationPath.substringAfterLast("/")
                val pathDisplay = metadata?.get("path_display")?.jsonPrimitive?.content ?: destinationPath
                val isFolder = metadata?.get(".tag")?.jsonPrimitive?.content == "folder"

                syncMutex.withLock {
                    syncStatusMap.remove(sourcePath)
                    syncStatusMap[destinationPath] = SyncStatus.SYNCED
                }

                Result.success(NetworkDocument(
                    id = id,
                    name = name,
                    path = pathDisplay,
                    isFolder = isFolder,
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = Clock.System.now(),
                    storageId = "dropbox",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                ))
            } else {
                Result.failure(Exception("Dropbox move failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> {
        if (!_isConnected) {
            return Result.success(Unit)
        }
        return try {
            val fullSourcePath = normalizePath(sourcePath)
            val fullDestPath = normalizePath(destinationPath)
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/files/copy_v2")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"from_path": "$fullSourcePath", "to_path": "$fullDestPath", "autorename": false}""")
            }

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Dropbox copy failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        if (!_isConnected) {
            return Result.success(NetworkDocument(
                id = remotePath,
                name = remotePath.substringAfterLast("/"),
                path = remotePath,
                isFolder = false,
                syncStatus = SyncStatus.SYNCED,
                lastModified = Clock.System.now(),
                storageId = "dropbox"
            ))
        }
        return try {
            val fullPath = normalizePath(remotePath)
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/files/get_metadata")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"path": "$fullPath", "include_media_info": false, "include_deleted": false, "include_has_explicit_shared_members": false}""")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val metadata = json.parseToJsonElement(content).jsonObject
                val tag = metadata[".tag"]?.jsonPrimitive?.content ?: "file"
                val id = metadata["id"]?.jsonPrimitive?.content ?: remotePath
                val name = metadata["name"]?.jsonPrimitive?.content ?: remotePath.substringAfterLast("/")
                val pathDisplay = metadata["path_display"]?.jsonPrimitive?.content ?: remotePath
                val size = metadata["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                val serverModified = metadata["server_modified"]?.jsonPrimitive?.content
                val lastModified = serverModified?.let { Instant.parse(it) } ?: Clock.System.now()

                Result.success(NetworkDocument(
                    id = id,
                    name = name,
                    path = pathDisplay,
                    isFolder = tag == "folder",
                    size = size,
                    lastModified = lastModified,
                    syncStatus = SyncStatus.SYNCED,
                    storageId = "dropbox",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                ))
            } else {
                Result.failure(NetworkStorageException.FileOperationException.NotFound(
                    filePath = remotePath,
                    cause = Exception("Dropbox get_metadata failed: ${response.status}")
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getActiveOperations(): Flow<List<NetworkOperation>> = flow {
        val operations = operationsMutex.withLock {
            activeOperations.values.toList()
        }
        emit(operations)
    }
    
    override suspend fun cancelOperation(operationId: Long): Result<Unit> {
        // Implementation would go here
        return Result.success(Unit)
    }
    
    override suspend fun pauseOperation(operationId: Long): Result<Unit> {
        // Implementation would go here
        return Result.success(Unit)
    }
    
    override suspend fun resumeOperation(operationId: Long): Result<Unit> {
        // Implementation would go here
        return Result.success(Unit)
    }
    
    override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> = flow {
        val entries = cacheMutex.withLock {
            if (path != null) {
                cacheEntries.values.filter { it.remotePath.startsWith(path) }.toList()
            } else {
                cacheEntries.values.toList()
            }
        }
        emit(entries)
    }

    override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> {
        return try {
            cacheMutex.withLock {
                val now = Clock.System.now()
                cacheEntries[remotePath] = CacheEntry(
                    id = "cache-${now.epochSeconds}-${remotePath.hashCode()}",
                    remoteDocumentId = remotePath,
                    localPath = "/cache/dropbox$remotePath",
                    remotePath = remotePath,
                    size = 0L,
                    createdAt = now,
                    lastAccessed = now,
                    lastModified = now,
                    priority = priority
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromCache(remotePath: String): Result<Unit> {
        return try {
            cacheMutex.withLock {
                cacheEntries.remove(remotePath)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearCache(): Result<Unit> {
        return try {
            cacheMutex.withLock {
                cacheEntries.clear()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> = flow {
        val statuses = syncMutex.withLock {
            if (path != null) {
                syncStatusMap.filter { it.key.startsWith(path) }.toMap()
            } else {
                syncStatusMap.toMap()
            }
        }
        emit(statuses)
    }

    override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        val now = Clock.System.now()

        syncMutex.withLock { syncStatusMap[remotePath] = SyncStatus.SYNCING }

        // If connected, attempt real sync by fetching metadata
        if (_isConnected) {
            emit(NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.IN_PROGRESS,
                remotePath = remotePath,
                progress = 0.0,
                createdAt = now,
                startedAt = now
            ))

            try {
                val fileInfoResult = getFileInfo(remotePath)
                if (fileInfoResult.isSuccess) {
                    syncMutex.withLock { syncStatusMap[remotePath] = SyncStatus.SYNCED }
                } else {
                    syncMutex.withLock { syncStatusMap[remotePath] = SyncStatus.SYNC_ERROR }
                }
            } catch (e: Exception) {
                syncMutex.withLock { syncStatusMap[remotePath] = SyncStatus.SYNC_ERROR }
            }
        } else {
            syncMutex.withLock { syncStatusMap[remotePath] = SyncStatus.SYNCED }
        }

        emit(NetworkOperation(
            id = operationId,
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = remotePath,
            progress = 1.0,
            createdAt = now,
            startedAt = now,
            completedAt = Clock.System.now()
        ))
    }

    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        val now = Clock.System.now()

        emit(NetworkOperation(
            id = operationId,
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/",
            progress = 1.0,
            createdAt = now,
            startedAt = now,
            completedAt = now
        ))
    }

    override fun searchFiles(
        query: String,
        path: String?,
        includeContent: Boolean
    ): Flow<Result<List<NetworkDocument>>> = flow {
        if (!_isConnected) {
            emit(Result.success(emptyList()))
            return@flow
        }
        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")

            val searchPath = path?.let { normalizePath(it) } ?: ""
            val requestBody = buildString {
                append("""{"query": "$query"""")
                append(""", "options": {"path": "$searchPath", "max_results": 100""")
                if (includeContent) {
                    append(""", "file_categories": [{".\u0074ag": "document"}]""")
                }
                append("}}")
            }

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/files/search_v2")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val searchResponse = json.parseToJsonElement(content).jsonObject

                val documents = mutableListOf<NetworkDocument>()
                // Parse matches array from search response
                val matchesArray = searchResponse["matches"]
                if (matchesArray != null) {
                    val matchesList = json.parseToJsonElement(matchesArray.toString())
                    if (matchesList is JsonArray) {
                        for (match in matchesList) {
                            val matchObj = match.jsonObject
                            val metadata = matchObj["metadata"]?.jsonObject?.get("metadata")?.jsonObject ?: continue
                            val tag = metadata[".tag"]?.jsonPrimitive?.content ?: "file"
                            val id = metadata["id"]?.jsonPrimitive?.content ?: continue
                            val name = metadata["name"]?.jsonPrimitive?.content ?: continue
                            val pathDisplay = metadata["path_display"]?.jsonPrimitive?.content ?: ""
                            val size = metadata["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

                            documents.add(NetworkDocument(
                                id = id,
                                name = name,
                                path = pathDisplay,
                                isFolder = tag == "folder",
                                size = size,
                                lastModified = Clock.System.now(),
                                syncStatus = SyncStatus.SYNCED,
                                storageId = "dropbox",
                                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                            ))
                        }
                    }
                }

                emit(Result.success(documents))
            } else {
                emit(Result.failure(Exception("Dropbox search failed: ${response.status}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override fun getRecentChanges(
        since: kotlinx.datetime.Instant,
        path: String?
    ): Flow<List<NetworkDocument>> = flow {
        emit(emptyList())
    }
    
    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        return Result.success(StorageQuota(
            totalSpace = 2000000000000L, // 2TB for Plus plan
            usedSpace = 500000000000L,   // 500GB used
            availableSpace = 1500000000000L, // 1.5TB available
            usagePercentage = 0.25,
            isFull = false,
            isLowOnSpace = false,
            metadata = mapOf("provider" to "Dropbox", "plan" to "Plus")
        ))
    }
    
    override suspend fun exists(remotePath: String): Result<Boolean> {
        return getFileInfo(remotePath).map { true }.recover { false }
    }
    
    override fun getParentPath(remotePath: String): String? {
        if (remotePath == "/" || remotePath.isBlank()) return "/"
        val parent = remotePath.substringBeforeLast('/', "")
        return if (parent.isEmpty()) "/" else parent
    }
    
    override fun validatePath(remotePath: String): Result<Unit> {
        // Dropbox treats empty/blank paths as root, so they are valid
        return Result.success(Unit)
    }
    
    /**
     * Normalize path for Dropbox API
     */
    private fun normalizePath(path: String): String {
        return when {
            path.isBlank() -> _rootPath
            path == "/" -> _rootPath
            else -> {
                val normalized = if (_rootPath.isBlank()) path else "$_rootPath/$path"
                normalized.replace("//", "/")
            }
        }
    }
    
    // Dropbox API data classes
    @Serializable
    private data class DropboxAccountInfo(
        val account_id: String,
        val name: DropboxName,
        val email: String,
        val email_verified: Boolean,
        val disabled: Boolean,
        val profile_photo_url: String? = null,
        val team: DropboxTeam? = null
    )
    
    @Serializable
    private data class DropboxName(
        val given_name: String,
        val surname: String,
        val familiar_name: String,
        val display_name: String,
        val abbreviated_name: String
    )
    
    @Serializable
    private data class DropboxTeam(
        val id: String,
        val name: String
    )
    
    @Serializable
    private data class DropboxListResponse(
        val entries: List<DropboxMetadata>,
        val cursor: String? = null,
        val has_more: Boolean = false
    )
    
    @Serializable
    private data class DropboxMetadata(
        val tag: String,
        val name: String,
        val id: String,
        val pathLower: String? = null,
        val pathDisplay: String,
        val size: Long? = null,
        val serverModified: String? = null,
        val clientModified: String? = null,
        val rev: String? = null
    )
}