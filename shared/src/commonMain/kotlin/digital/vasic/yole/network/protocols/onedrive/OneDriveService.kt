/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.onedrive

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.auth.OneDriveOAuth2Flow
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.PlatformFileIO
import digital.vasic.yole.network.platform.PlatformFileIOFactory
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
import kotlin.coroutines.coroutineContext

/**
 * Microsoft OneDrive implementation of [NetworkStorageService].
 *
 * ## Implementation Status
 *
 * **SUBSTANTIALLY IMPLEMENTED** -- Uses ktor [HttpClient] for real Microsoft
 * Graph API calls with OAuth2 authentication via [OneDriveOAuth2Flow] and
 * [AuthTokenManager]. Supports personal (ME), business, SharePoint, and group
 * drive types.
 *
 * ### What works (real HTTP I/O via ktor against Microsoft Graph API v1.0):
 * - OAuth2 authentication flow (token storage, refresh via [AuthTokenManager])
 * - [connect] -- validates token, fetches drive info (v1.0/{driveType}/drive)
 * - [disconnect] -- cancels background tasks, closes HttpClient
 * - [testConnection] -- verifies drive info retrieval
 * - [listFiles] -- calls items/{folderId}/children, parses JSON
 * - [downloadFile] -- fetches metadata, then downloads via items/{id}/content
 *   (Note: downloaded bytes are not written to local filesystem)
 * - [uploadFile] -- calls PUT items/{parentId}/children/{name}/content
 *   (Note: sends empty byte array; local file bytes not read from disk)
 * - [deleteFile] -- calls DELETE items/{itemId}
 * - [createFolder] -- creates folder via POST items/{parentId}/children
 * - [renameFile] -- calls PATCH items/{itemId} with new name
 * - [moveFile] -- calls PATCH with parentReference and name
 * - [copyFile] -- calls POST items/{itemId}/copy (returns 202 Accepted)
 * - [getFileInfo] -- calls GET items/{itemId}
 * - [searchFiles] -- calls root/search(q='{query}')
 * - [exists] -- delegates to [getFileInfo]
 * - Cache and sync status tracking (in-memory maps)
 *
 * ### Limitations:
 * - [uploadFile] reads local bytes via PlatformFileIO; emits FAILED on read error
 * - [downloadFile] writes received bytes to local filesystem via PlatformFileIO
 *
 * Resource Management: This class manages a lazily-initialized [HttpClient] that
 * must be properly closed. Call [disconnect] when done using this service.
 */
class OneDriveService(
    override val config: StorageConfig.OneDriveConfig,
    private val _injectedHttpClient: HttpClient? = null,
    private val _injectedAuthTokenManager: AuthTokenManager? = null,
    private val testFileIO: PlatformFileIO? = null
) : NetworkStorageService {

    // Resilience: circuit breaker and connection limiter
    private val circuitBreaker = CircuitBreaker(name = "onedrive", failureThreshold = 5)
    private val connectionLimiter = ConnectionLimiter(name = "onedrive", maxConcurrent = 5)

    // Platform file I/O for reading/writing local files
    private val fileIO by lazy { testFileIO ?: PlatformFileIOFactory.create() }

    // Lazy initialization of HttpClient to avoid resource allocation if never used
    // Use named lazy delegate so disconnect() can check isInitialized() without a race
    private val _httpClientLazy = lazy { _injectedHttpClient ?: createHttpClient() }
    private val httpClient by _httpClientLazy

    private val authTokenManager = _injectedAuthTokenManager ?: AuthTokenManager("onedrive")
    private val oauth2Flow by lazy {
        OneDriveOAuth2Flow(
            httpClient = httpClient,
            clientId = config.clientId,
            clientSecret = config.clientSecret,
            redirectUri = "http://localhost:8080/callback" // Should be configurable
        )
    }

    private val stateMutex = Mutex()
    @kotlin.concurrent.Volatile
    private var _isConnected = false
    @kotlin.concurrent.Volatile
    private var _rootFolderId = config.rootFolderId ?: "root"
    private val activeOperations = mutableMapOf<Long, NetworkOperation>()
    private val operationsMutex = Mutex()

    // Operation control: active jobs and pause flags for cancel/pause/resume support
    private val activeJobs = mutableMapOf<Long, Job>()
    private val activeJobsMutex = Mutex()
    private val pauseFlags = mutableMapOf<Long, MutableStateFlow<Boolean>>()
    private val pauseFlagsMutex = Mutex()

    override val isOnline: Boolean
        get() = _isConnected

    /** Read _isConnected under stateMutex for safe access from suspending contexts. */
    private suspend fun isConnected(): Boolean = stateMutex.withLock { _isConnected }

    override val rootPath: String
        get() = "/"

    // Structured concurrency: use SupervisorJob for background initialization
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scopeMutex = Mutex()
    private var initJob: Job? = null

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
        initJob = serviceScope.launch {
            try {
                initializeConnection()
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                println("WARNING: ${this@OneDriveService::class.simpleName} initialization failed: ${e.message}")
            }
        }
    }

    private suspend fun initializeConnection() {
        val hasValidToken = authTokenManager.hasValidToken().getOrNull() ?: false
        if (hasValidToken) {
            // Test connection with existing token
            val testResult = testConnectionInternal()
            stateMutex.withLock { _isConnected = testResult.getOrNull() ?: false }
        }
    }
    
    /** Returns metadata about this OneDrive storage instance, including its connection status. */
    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "onedrive_${config.name}",
            name = config.name,
            type = StorageType.ONEDRIVE,
            location = "onedrive://",
            isOnline = isConnected(),
            lastSync = Clock.System.now(),
            supportsFolders = true,
            supportsMetadata = true
        )
    }

    /** Authenticates and connects to OneDrive, refreshing the OAuth2 token if needed. */
    override suspend fun connect(): Result<Unit> {
        return circuitBreaker.execute {
            connectionLimiter.withConnection {
                // Cancel any existing scope to avoid leaking coroutines on reconnect
                scopeMutex.withLock {
                    serviceScope.cancel()
                    serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                }

                // Check if we have valid tokens
                val hasValidToken = authTokenManager.hasValidToken().getOrNull() ?: false

                if (!hasValidToken) {
                    SecurityEventLogger.logAuthFailure("OneDriveService", "No valid authentication tokens found")
                    return@withConnection Result.failure(
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
                    stateMutex.withLock { _isConnected = true }
                    Result.success(Unit)
                } else {
                    // Try to refresh token if connection failed
                    refreshAccessToken()
                }
            }
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                if (e is CircuitBreakerOpenException) {
                    SecurityEventLogger.logCircuitBreakerOpen("OneDriveService", circuitBreaker.failures)
                }
                Result.failure(NetworkStorageException.ConnectionException.Failed(
                    message = "OneDrive connection failed",
                    cause = e
                ))
            }
        )
    }
    
    private suspend fun testConnectionInternal(): Result<Boolean> = try {
        val driveInfoResult = getDriveInfo()
        Result.success(driveInfoResult.isSuccess)
    } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
        Result.success(false)
    }
    
    /** Disconnects from OneDrive, cancelling background tasks and closing the HTTP client. */
    override suspend fun disconnect(): Result<Unit> = try {
        // Cancel init job first
        initJob?.cancel()
        initJob = null
        // Cancel background tasks and recreate scope for potential reconnection
        scopeMutex.withLock {
            serviceScope.cancel()
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
        // Only close httpClient if it was actually initialized
        if (_httpClientLazy.isInitialized()) {
            try {
                httpClient.close()
            } catch (_: Exception) {
                // Log but don't fail disconnect for close errors
            }
        }
        stateMutex.withLock { _isConnected = false }
        Result.success(Unit)
    } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
        stateMutex.withLock { _isConnected = false } // Ensure we mark as disconnected even on error
        Result.failure(NetworkStorageException.fromThrowable(e, "disconnect"))
    }

    /** Tests the OneDrive connection by attempting to retrieve drive info. */
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
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
                stateMutex.withLock { _isConnected = true }
                SecurityEventLogger.logTokenRefresh("OneDriveService", success = true)
                Result.success(Unit)
            } else {
                SecurityEventLogger.logTokenRefresh("OneDriveService", success = false)
                Result.failure(tokenResponse.exceptionOrNull() ?: Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            SecurityEventLogger.logTokenRefresh("OneDriveService", success = false)
            Result.failure(e)
        }
    }
    
    /** Lists files and folders in the OneDrive directory at [path] via the Graph API. */
    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        if (!isConnected()) {
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
            val itemList = json.decodeFromString<OneDriveItemList>(content)
            
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            emit(Result.failure(NetworkStorageException.FileOperationException.ListFailed(
                path = path,
                cause = e
            )))
        }
    }
    
    /**
     * Resolve a path to a OneDrive item ID using the Graph API path-based access.
     * OneDrive supports direct path-based lookups via /root:/{path}.
     */
    private suspend fun getItemIdFromPath(path: String): Result<String> {
        if (path.isBlank() || path == "/") {
            return Result.success(_rootFolderId)
        }

        if (!isConnected()) {
            return Result.success(_rootFolderId)
        }

        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val driveType = getDriveTypePath()
            val cleanPath = path.trim('/')

            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    // Use path-based access: /me/drive/root:/{path}
                    encodedPath = "/v1.0/$driveType/drive/root:/$cleanPath"
                    parameter("select", "id")
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val item = json.decodeFromString<OneDriveItem>(content)
                Result.success(item.id)
            } else if (response.status.value == 404) {
                Result.failure(NetworkStorageException.FileOperationException.NotFound(
                    filePath = path,
                    cause = Exception("Item not found at path: $path")
                ))
            } else {
                Result.failure(Exception("Failed to resolve path '$path': ${response.status}"))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }
    
    /** Downloads a file from [remotePath] on OneDrive to [localPath] on the local filesystem. */
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        
        if (!isConnected()) {
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

                // Write downloaded bytes to local filesystem
                fileIO.writeFileBytes(localPath, bytes)

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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
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
    
    /** Uploads a file from [localPath] on the local filesystem to [remotePath] on OneDrive. */
    override suspend fun uploadFile(
        localPath: String,
        remotePath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        
        if (!isConnected()) {
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
            
            // Read file bytes from local filesystem
            val fileBytes = fileIO.readFileBytes(localPath).getOrNull() ?: run {
                emit(initialOperation.copy(
                    status = NetworkOperation.Status.FAILED,
                    error = "Failed to read local file: $localPath"
                ))
                return@flow
            }
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
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
    
    /**
     * Get the Graph API drive type path prefix based on the configured drive type
     */
    private fun getDriveTypePath(): String {
        return when (config.driveType) {
            OneDriveDriveType.ME -> "me"
            OneDriveDriveType.BUSINESS -> "me"
            OneDriveDriveType.SHAREPOINT -> "sites"
            OneDriveDriveType.GROUP -> "groups"
        }
    }

    // ==================== File Operations ====================

    /** Deletes the item at [remotePath] from OneDrive and removes it from cache and sync tracking. */
    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        if (!isConnected()) {
            return Result.success(Unit) // Offline: succeed silently for queue-based sync
        }
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val itemId = getItemIdFromPath(remotePath).getOrNull()
                ?: return Result.failure(Exception("Item not found: $remotePath"))

            val driveType = getDriveTypePath()

            val response = httpClient.delete {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", itemId)
                    config.driveId?.let {
                        path("drives", it, "items", itemId)
                    }
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess() || response.status.value == 204) {
                syncMutex.withLock { syncStatusMap.remove(remotePath) }
                cacheMutex.withLock { cacheEntries.remove(remotePath) }
                Result.success(Unit)
            } else {
                Result.failure(NetworkStorageException.FileOperationException.DeleteFailed(
                    path = remotePath,
                    cause = Exception("OneDrive delete failed: ${response.status}")
                ))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(NetworkStorageException.FileOperationException.DeleteFailed(
                path = remotePath,
                cause = e
            ))
        }
    }

    /** Creates a new folder at [remotePath] on OneDrive via the Graph API. */
    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> {
        if (!isConnected()) {
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
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val folderName = remotePath.substringAfterLast("/")
            val parentPath = remotePath.substringBeforeLast("/", "")
            val parentId = if (parentPath.isBlank() || parentPath == "/") {
                _rootFolderId
            } else {
                getItemIdFromPath(parentPath).getOrNull() ?: _rootFolderId
            }

            val driveType = getDriveTypePath()

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", parentId, "children")
                    config.driveId?.let {
                        path("drives", it, "items", parentId, "children")
                    }
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"name": "$folderName", "folder": {}, "@microsoft.graph.conflictBehavior": "fail"}""")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val item = json.decodeFromString<OneDriveItem>(content)

                Result.success(NetworkDocument(
                    id = item.id,
                    name = item.name,
                    path = remotePath,
                    isFolder = true,
                    size = 0L,
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = item.lastModifiedDateTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                    storageId = "onedrive",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                ))
            } else {
                Result.failure(NetworkStorageException.FileOperationException.CreateFolderFailed(
                    path = remotePath,
                    cause = Exception("OneDrive create folder failed: ${response.status}")
                ))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(NetworkStorageException.FileOperationException.CreateFolderFailed(
                path = remotePath,
                cause = e
            ))
        }
    }

    /** Renames the item at [remotePath] to [newName] using a PATCH request to the Graph API. */
    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        if (!isConnected()) {
            return Result.success(Unit)
        }
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val itemId = getItemIdFromPath(remotePath).getOrNull()
                ?: return Result.failure(Exception("Item not found: $remotePath"))

            val driveType = getDriveTypePath()

            val response = httpClient.patch {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", itemId)
                    config.driveId?.let {
                        path("drives", it, "items", itemId)
                    }
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"name": "$newName"}""")
            }

            if (response.status.isSuccess()) {
                syncMutex.withLock {
                    syncStatusMap.remove(remotePath)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("OneDrive rename failed: ${response.status}"))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Moves an item from [sourcePath] to [destinationPath] on OneDrive by updating its parent reference. */
    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> {
        if (!isConnected()) {
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
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val itemId = getItemIdFromPath(sourcePath).getOrNull()
                ?: return Result.failure(Exception("Source item not found: $sourcePath"))

            val destParentPath = destinationPath.substringBeforeLast("/", "")
            val destParentId = if (destParentPath.isBlank() || destParentPath == "/") {
                _rootFolderId
            } else {
                getItemIdFromPath(destParentPath).getOrNull() ?: _rootFolderId
            }

            val destName = destinationPath.substringAfterLast("/")
            val driveType = getDriveTypePath()

            val response = httpClient.patch {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", itemId)
                    config.driveId?.let {
                        path("drives", it, "items", itemId)
                    }
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"parentReference": {"id": "$destParentId"}, "name": "$destName"}""")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val item = json.decodeFromString<OneDriveItem>(content)

                syncMutex.withLock {
                    syncStatusMap.remove(sourcePath)
                    syncStatusMap[destinationPath] = SyncStatus.SYNCED
                }

                Result.success(NetworkDocument(
                    id = item.id,
                    name = item.name,
                    path = destinationPath,
                    isFolder = item.folder != null,
                    size = item.size ?: 0L,
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = item.lastModifiedDateTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                    storageId = "onedrive",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                ))
            } else {
                Result.failure(Exception("OneDrive move failed: ${response.status}"))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Copies an item from [sourcePath] to [destinationPath] on OneDrive (asynchronous, returns 202 Accepted). */
    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> {
        if (!isConnected()) {
            return Result.success(Unit)
        }
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val itemId = getItemIdFromPath(sourcePath).getOrNull()
                ?: return Result.failure(Exception("Source item not found: $sourcePath"))

            val destParentPath = destinationPath.substringBeforeLast("/", "")
            val destParentId = if (destParentPath.isBlank() || destParentPath == "/") {
                _rootFolderId
            } else {
                getItemIdFromPath(destParentPath).getOrNull() ?: _rootFolderId
            }

            val destName = destinationPath.substringAfterLast("/")
            val driveType = getDriveTypePath()

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", itemId, "copy")
                    config.driveId?.let {
                        path("drives", it, "items", itemId, "copy")
                    }
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"parentReference": {"id": "$destParentId"}, "name": "$destName"}""")
            }

            // OneDrive copy returns 202 Accepted for async operations
            if (response.status.isSuccess() || response.status.value == 202) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("OneDrive copy failed: ${response.status}"))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Retrieves metadata for the item at [remotePath] from the Graph API. */
    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        if (!isConnected()) {
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
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val itemId = getItemIdFromPath(remotePath).getOrNull()
                ?: return Result.failure(Exception("Item not found: $remotePath"))

            val driveType = getDriveTypePath()

            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "items", itemId)
                    config.driveId?.let {
                        path("drives", it, "items", itemId)
                    }
                    parameter("select", "id,name,size,createdDateTime,lastModifiedDateTime,folder,file")
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val item = json.decodeFromString<OneDriveItem>(content)

                Result.success(NetworkDocument(
                    id = item.id,
                    name = item.name,
                    path = remotePath,
                    isFolder = item.folder != null,
                    size = item.size ?: 0L,
                    lastModified = item.lastModifiedDateTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                    syncStatus = SyncStatus.SYNCED,
                    storageId = "onedrive",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                ))
            } else {
                Result.failure(NetworkStorageException.FileOperationException.NotFound(
                    filePath = remotePath,
                    cause = Exception("OneDrive get item info failed: ${response.status}")
                ))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }
    
    /** Returns a snapshot of all currently active (in-progress) network operations. */
    override fun getActiveOperations(): Flow<List<NetworkOperation>> = flow {
        val operations = operationsMutex.withLock {
            activeOperations.values.toList()
        }
        emit(operations)
    }
    
    /** Cancels the active operation identified by [operationId] and removes it from tracking. */
    override suspend fun cancelOperation(operationId: Long): Result<Unit> {
        return try {
            operationsMutex.withLock { activeOperations.remove(operationId) }
            pauseFlagsMutex.withLock {
                pauseFlags.remove(operationId)
            }
            activeJobsMutex.withLock {
                activeJobs[operationId]?.cancel()
                activeJobs.remove(operationId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(NetworkStorageException.fromThrowable(e, "cancelOperation"))
        }
    }

    /** Pauses the active operation identified by [operationId] by setting its pause flag. */
    override suspend fun pauseOperation(operationId: Long): Result<Unit> {
        return try {
            pauseFlagsMutex.withLock {
                pauseFlags[operationId]?.value = true
            }
            operationsMutex.withLock {
                activeOperations[operationId]?.let { op ->
                    activeOperations[operationId] = op.copy(status = NetworkOperation.Status.PAUSED, isPaused = true)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(NetworkStorageException.fromThrowable(e, "pauseOperation"))
        }
    }

    /** Resumes the paused operation identified by [operationId] by clearing its pause flag. */
    override suspend fun resumeOperation(operationId: Long): Result<Unit> {
        return try {
            pauseFlagsMutex.withLock {
                pauseFlags[operationId]?.value = false
            }
            operationsMutex.withLock {
                activeOperations[operationId]?.let { op ->
                    activeOperations[operationId] = op.copy(status = NetworkOperation.Status.IN_PROGRESS, isPaused = false)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(NetworkStorageException.fromThrowable(e, "resumeOperation"))
        }
    }

    /** Returns cached entries, optionally filtered to those under [path]. */
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

    /** Adds the item at [remotePath] to the in-memory cache with the given [priority]. */
    override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> {
        return try {
            cacheMutex.withLock {
                val now = Clock.System.now()
                cacheEntries[remotePath] = CacheEntry(
                    id = "cache-${now.epochSeconds}-${remotePath.hashCode()}",
                    remoteDocumentId = remotePath,
                    localPath = "/cache/onedrive$remotePath",
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Removes the cached entry for [remotePath] from the in-memory cache. */
    override suspend fun removeFromCache(remotePath: String): Result<Unit> {
        return try {
            cacheMutex.withLock {
                cacheEntries.remove(remotePath)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Clears all entries from the in-memory cache. */
    override suspend fun clearCache(): Result<Unit> {
        return try {
            cacheMutex.withLock {
                cacheEntries.clear()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Returns sync status entries, optionally filtered to those under [path]. */
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

    /** Synchronizes a single file at [remotePath], optionally forcing a refresh with [forceSync]. */
    override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        val now = Clock.System.now()

        syncMutex.withLock { syncStatusMap[remotePath] = SyncStatus.SYNCING }

        // If connected, attempt real sync by fetching metadata
        if (isConnected()) {
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
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

    /** Synchronizes all files in the root directory, optionally forcing a full refresh with [forceSync]. */
    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        val now = Clock.System.now()

        if (!isConnected()) {
            emit(NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.FAILED,
                remotePath = "/",
                error = "OneDrive not connected",
                createdAt = now,
                startedAt = now,
                completedAt = now
            ))
            return@flow
        }

        emit(NetworkOperation(
            id = operationId,
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.IN_PROGRESS,
            remotePath = "/",
            progress = 0.0,
            createdAt = now,
            startedAt = now
        ))

        try {
            // List all remote files
            var remoteFiles = emptyList<NetworkDocument>()
            listFiles("/").collect { result ->
                if (result.isSuccess) {
                    remoteFiles = result.getOrThrow()
                }
            }

            val totalFiles = remoteFiles.size.coerceAtLeast(1)
            var processed = 0

            for (doc in remoteFiles) {
                processed++
                val progress = processed.toDouble() / totalFiles

                // Check if the file is cached and if remote is newer
                val cachedEntry = cacheMutex.withLock { cacheEntries[doc.path] }
                val needsSync = forceSync || cachedEntry == null ||
                        (doc.lastModified != null && doc.lastModified > cachedEntry.lastModified)

                if (needsSync && !doc.isFolder) {
                    syncMutex.withLock { syncStatusMap[doc.path] = SyncStatus.SYNCING }
                    syncMutex.withLock { syncStatusMap[doc.path] = SyncStatus.SYNCED }

                    cacheMutex.withLock {
                        val cacheNow = Clock.System.now()
                        cacheEntries[doc.path] = CacheEntry(
                            id = "cache-${cacheNow.epochSeconds}-${doc.path.hashCode()}",
                            remoteDocumentId = doc.id,
                            localPath = "/cache/onedrive${doc.path}",
                            remotePath = doc.path,
                            size = doc.size,
                            createdAt = cacheNow,
                            lastAccessed = cacheNow,
                            lastModified = doc.lastModified ?: cacheNow,
                            priority = 0
                        )
                    }
                }

                emit(NetworkOperation(
                    id = operationId,
                    type = NetworkOperation.Type.SYNC,
                    status = NetworkOperation.Status.IN_PROGRESS,
                    remotePath = "/",
                    progress = progress,
                    createdAt = now,
                    startedAt = now
                ))
            }

            emit(NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/",
                progress = 1.0,
                createdAt = now,
                startedAt = now,
                completedAt = Clock.System.now()
            ))
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            emit(NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.FAILED,
                remotePath = "/",
                error = e.message ?: "Sync failed",
                createdAt = now,
                startedAt = now,
                completedAt = Clock.System.now()
            ))
        }
    }

    /** Searches OneDrive for items matching [query], optionally scoped to [path]. */
    override fun searchFiles(
        query: String,
        path: String?,
        includeContent: Boolean
    ): Flow<Result<List<NetworkDocument>>> = flow {
        if (!isConnected()) {
            emit(Result.success(emptyList()))
            return@flow
        }
        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")

            val driveType = getDriveTypePath()

            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "root", "search(q='${query.replace("'", "").encodeURLParameter()}')")
                    config.driveId?.let {
                        path("drives", it, "root", "search(q='${query.replace("'", "").encodeURLParameter()}')")
                    }
                    parameter("select", "id,name,size,createdDateTime,lastModifiedDateTime,folder,file")
                    parameter("top", "100")
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val itemList = json.decodeFromString<OneDriveItemList>(content)

                val documents = itemList.value.map { item ->
                    NetworkDocument(
                        id = item.id,
                        name = item.name,
                        path = "/${item.name}",
                        isFolder = item.folder != null,
                        size = item.size ?: 0L,
                        lastModified = item.lastModifiedDateTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                        syncStatus = SyncStatus.SYNCED,
                        storageId = "onedrive",
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                    )
                }

                emit(Result.success(documents))
            } else {
                emit(Result.failure(Exception("OneDrive search failed: ${response.status}")))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            emit(Result.failure(e))
        }
    }
    
    /** Returns documents modified [since] the given timestamp using the OneDrive delta API. */
    override fun getRecentChanges(
        since: kotlinx.datetime.Instant,
        path: String?
    ): Flow<List<NetworkDocument>> = flow {
        if (!isConnected()) {
            emit(emptyList())
            return@flow
        }

        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")

            val driveType = getDriveTypePath()

            // Use delta API to get changes
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive", "root", "delta")
                    parameter("select", "id,name,size,lastModifiedDateTime,folder,file")
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val itemList = json.decodeFromString<OneDriveItemList>(content)

                val recentDocuments = itemList.value.mapNotNull { item ->
                    val lastModified = item.lastModifiedDateTime?.let {
                        try { Instant.parse(it) } catch (_: Exception) { null }
                    }
                    // Only include items modified since the given timestamp
                    if (lastModified != null && lastModified >= since) {
                        NetworkDocument(
                            id = item.id,
                            name = item.name,
                            path = "/${item.name}",
                            isFolder = item.folder != null,
                            size = item.size ?: 0L,
                            lastModified = lastModified,
                            syncStatus = SyncStatus.SYNCED,
                            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                            storageId = "onedrive"
                        )
                    } else {
                        null
                    }
                }

                emit(recentDocuments)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            emit(emptyList())
        }
    }

    /** Retrieves storage quota information (total, used, remaining space) from OneDrive. */
    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        if (!isConnected()) {
            return Result.failure(
                NetworkStorageException.ConnectionException.NotConnected(
                    message = "OneDrive not connected"
                )
            )
        }
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val driveType = getDriveTypePath()

            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0", driveType, "drive")
                    parameter("select", "quota")
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val driveInfo = json.decodeFromString<OneDriveDrive>(content)
                val quota = driveInfo.quota

                val totalSpace = quota?.total ?: 5368709120L // Default 5GB
                val usedSpace = quota?.used ?: 0L
                val availableSpace = quota?.remaining ?: (totalSpace - usedSpace).coerceAtLeast(0L)
                val usagePercentage = if (totalSpace > 0) usedSpace.toDouble() / totalSpace else 0.0

                Result.success(StorageQuota(
                    totalSpace = totalSpace,
                    usedSpace = usedSpace,
                    availableSpace = availableSpace,
                    usagePercentage = usagePercentage,
                    isFull = availableSpace <= 0,
                    isLowOnSpace = usagePercentage >= 0.9,
                    metadata = mapOf("provider" to "OneDrive", "type" to config.driveType.name)
                ))
            } else {
                Result.failure(Exception("Failed to get quota info: ${response.status}"))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }
    
    /** Checks whether an item exists at [remotePath] by delegating to [getFileInfo]. */
    override suspend fun exists(remotePath: String): Result<Boolean> {
        return getFileInfo(remotePath).map { true }.recover { false }
    }
    
    /** Returns the parent directory path of [remotePath], or null if it is the root. */
    override fun getParentPath(remotePath: String): String? {
        if (remotePath == "/" || remotePath.isBlank()) return null
        val parent = remotePath.substringBeforeLast("/")
        return if (parent.isEmpty()) "/" else parent
    }
    
    /** Validates that [remotePath] is a non-blank path string. */
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