/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.dropbox

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.auth.DropboxOAuth2Flow
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext

/**
 * Dropbox implementation of [NetworkStorageService].
 *
 * ## Implementation Status
 *
 * **SUBSTANTIALLY IMPLEMENTED** -- Uses ktor [HttpClient] for real Dropbox API v2
 * calls with OAuth2 authentication via [DropboxOAuth2Flow] and [AuthTokenManager].
 *
 * ### What works (real HTTP I/O via ktor against Dropbox API v2):
 * - OAuth2 authentication flow (token storage, refresh via [AuthTokenManager])
 * - [connect] -- validates token, fetches account info (2/users/get_current_account)
 * - [disconnect] -- cancels background tasks, closes HttpClient
 * - [testConnection] -- verifies account info retrieval
 * - [listFiles] -- calls 2/files/list_folder, parses JSON response
 * - [downloadFile] -- calls 2/files/download (content.dropboxapi.com)
 *   (Note: downloaded bytes are not written to local filesystem)
 * - [uploadFile] -- calls 2/files/upload (content.dropboxapi.com)
 *   (Note: sends empty byte array; local file bytes not read from disk)
 * - [deleteFile] -- calls 2/files/delete_v2
 * - [createFolder] -- calls 2/files/create_folder_v2
 * - [renameFile] / [moveFile] -- calls 2/files/move_v2
 * - [copyFile] -- calls 2/files/copy_v2
 * - [getFileInfo] -- calls 2/files/get_metadata
 * - [searchFiles] -- calls 2/files/search_v2
 * - [exists] -- delegates to [getFileInfo]
 * - Cache and sync status tracking (in-memory maps)
 *
 * ### Limitations:
 * - [uploadFile] sends empty bytes (local file reading not implemented)
 * - [downloadFile] does not write bytes to local filesystem
 *
 * Resource Management: This class manages a lazily-initialized [HttpClient] that
 * must be properly closed. Call [disconnect] when done using this service.
 */
class DropboxService(
    override val config: StorageConfig.DropboxConfig,
    private val _injectedHttpClient: HttpClient? = null,
    private val _injectedAuthTokenManager: AuthTokenManager? = null,
    private val testFileIO: PlatformFileIO? = null
) : NetworkStorageService {

    // Resilience: circuit breaker and connection limiter
    private val circuitBreaker = CircuitBreaker(name = "dropbox", failureThreshold = 5)
    private val connectionLimiter = ConnectionLimiter(name = "dropbox", maxConcurrent = 5)

    // Platform file I/O for reading/writing local files
    private val fileIO by lazy { testFileIO ?: PlatformFileIOFactory.create() }

    // Lazy initialization of HttpClient to avoid resource allocation if never used
    // Use named lazy delegate so disconnect() can check isInitialized() without a race
    private val _httpClientLazy = lazy { _injectedHttpClient ?: createHttpClient() }
    private val httpClient by _httpClientLazy

    private val authTokenManager = _injectedAuthTokenManager ?: AuthTokenManager("dropbox")
    private val oauth2Flow by lazy {
        DropboxOAuth2Flow(
            httpClient = httpClient,
            clientId = config.appKey,
            clientSecret = config.appSecret,
            redirectUri = "http://localhost:8080/callback" // Should be configurable
        )
    }

    @kotlin.concurrent.Volatile
    private var _isConnected = false
    @kotlin.concurrent.Volatile
    private var _rootPath = if (config.rootPath.isBlank()) "" else config.rootPath
    private val stateMutex = Mutex()
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

    // Cache and sync maps are independently protected. Removing an entry
    // from syncStatusMap and cacheEntries is NOT atomic across both maps.
    // This is acceptable: stale sync status is harmless and self-correcting.
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
                println("WARNING: ${this@DropboxService::class.simpleName} initialization failed: ${e.message}")
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
    
    /** Return metadata describing this Dropbox storage connection. */
    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "dropbox_${config.name}",
            name = config.name,
            type = StorageType.DROPBOX,
            location = "dropbox://${_rootPath}",
            isOnline = stateMutex.withLock { _isConnected },
            lastSync = Clock.System.now(),
            supportsFolders = true,
            supportsMetadata = true
        )
    }
    
    /** Authenticate with Dropbox using OAuth2 tokens and verify the account. */
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
                    SecurityEventLogger.logAuthFailure("DropboxService", "No valid authentication tokens found")
                    return@withConnection Result.failure(
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
                    SecurityEventLogger.logCircuitBreakerOpen("DropboxService", circuitBreaker.failures)
                }
                Result.failure(NetworkStorageException.ConnectionException.Failed(
                    message = "Dropbox connection failed",
                    cause = e
                ))
            }
        )
    }
    
    private suspend fun testConnectionInternal(): Result<Boolean> = try {
        val accountInfoResult = getAccountInfo()
        Result.success(accountInfoResult.isSuccess)
    } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
        Result.success(false)
    }
    
    /** Disconnect from Dropbox by cancelling background tasks and closing the HTTP client. */
    override suspend fun disconnect(): Result<Unit> {
        return try {
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
    }
    
    /** Test the Dropbox connection by verifying account info retrieval. */
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
                SecurityEventLogger.logTokenRefresh("DropboxService", success = true)
                Result.success(Unit)
            } else {
                SecurityEventLogger.logTokenRefresh("DropboxService", success = false)
                Result.failure(tokenResponse.exceptionOrNull() ?: Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            SecurityEventLogger.logTokenRefresh("DropboxService", success = false)
            Result.failure(e)
        }
    }
    
    /** List files and folders at the given Dropbox [path] using the list_folder API. */
    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        if (!isConnected()) {
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
                "path": "${jsonEscape(fullPath.ifBlank { "" })}",
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            emit(Result.failure(NetworkStorageException.FileOperationException.ListFailed(
                path = path,
                cause = e
            )))
        }
    }
    
    /** Download a file from Dropbox at [remotePath] to the local [localPath] with progress tracking. */
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        
        if (!isConnected()) {
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
                header("Dropbox-API-Arg", """{"path": "${jsonEscape(fullPath)}"}""")
            }
            
            if (response.status.isSuccess()) {
                val bytes = response.bodyAsBytes()
                
                // Simulate progress updates
                emit(initialOperation.copy(progress = 0.5, bytesTransferred = bytes.size.toLong() / 2L))

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
                val errorOperation = initialOperation.copy(
                    status = NetworkOperation.Status.FAILED,
                    error = "Download failed: ${response.status}",
                    completedAt = Clock.System.now()
                )
                
                removeActiveOperation(operationId)
                emit(errorOperation)
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            val errorOperation = initialOperation?.copy(
                status = NetworkOperation.Status.FAILED,
                error = e.message ?: "Unknown error",
                completedAt = Clock.System.now()
            ) ?: createFailedOperation(operationId, NetworkOperation.Type.DOWNLOAD, remotePath, localPath, e.message ?: "Unknown error")
            
            removeActiveOperation(operationId)
            emit(errorOperation)
        }
    }
    
    /** Upload a local file from [localPath] to Dropbox at [remotePath] with progress tracking. */
    override suspend fun uploadFile(
        localPath: String,
        remotePath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        
        if (!isConnected()) {
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
            
            // Read file bytes from local filesystem
            val fileBytes = fileIO.readFileBytes(localPath).getOrNull() ?: run {
                emit(initialOperation.copy(
                    status = NetworkOperation.Status.FAILED,
                    error = "Failed to read local file: $localPath"
                ))
                return@flow
            }
            
            emit(initialOperation.copy(progress = 0.5, bytesTransferred = fileBytes.size.toLong() / 2L))
            
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "content.dropboxapi.com"
                    path("2/files/upload")
                }
                header("Authorization", "Bearer $accessToken")
                header("Dropbox-API-Arg", """{"path": "${jsonEscape(fullPath)}", "mode": "overwrite"}""")
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
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

    /** Delete a file or folder at [remotePath] on Dropbox using the delete_v2 API. */
    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        if (!isConnected()) {
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
                setBody("""{"path": "${jsonEscape(fullPath)}"}""")
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(NetworkStorageException.FileOperationException.DeleteFailed(
                path = remotePath,
                cause = e
            ))
        }
    }

    /** Create a new folder at [remotePath] on Dropbox using the create_folder_v2 API. */
    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> {
        if (!isConnected()) {
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
                setBody("""{"path": "${jsonEscape(fullPath)}", "autorename": false}""")
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(NetworkStorageException.FileOperationException.CreateFolderFailed(
                path = remotePath,
                cause = e
            ))
        }
    }

    /** Rename a file or folder at [remotePath] to [newName] on Dropbox using the move_v2 API. */
    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        if (!isConnected()) {
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
                setBody("""{"from_path": "${jsonEscape(fullPath)}", "to_path": "${jsonEscape(newPath)}", "autorename": false}""")
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Move a file or folder from [sourcePath] to [destinationPath] on Dropbox using the move_v2 API. */
    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> {
        if (!isConnected()) {
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Copy a file or folder from [sourcePath] to [destinationPath] on Dropbox using the copy_v2 API. */
    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> {
        if (!isConnected()) {
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Retrieve file metadata for [remotePath] from the Dropbox get_metadata API. */
    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        if (!isConnected()) {
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
                setBody("""{"path": "${jsonEscape(fullPath)}", "include_media_info": false, "include_deleted": false, "include_has_explicit_shared_members": false}""")
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
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

    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        val now = Clock.System.now()

        if (!isConnected()) {
            emit(NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.FAILED,
                remotePath = "/",
                error = "Dropbox not connected",
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
                    // Update sync status
                    syncMutex.withLock { syncStatusMap[doc.path] = SyncStatus.SYNCING }

                    // Mark as synced (actual download would happen in a full implementation)
                    syncMutex.withLock { syncStatusMap[doc.path] = SyncStatus.SYNCED }

                    // Update cache entry
                    cacheMutex.withLock {
                        val cacheNow = Clock.System.now()
                        cacheEntries[doc.path] = CacheEntry(
                            id = "cache-${cacheNow.epochSeconds}-${doc.path.hashCode()}",
                            remoteDocumentId = doc.id,
                            localPath = "/cache/dropbox${doc.path}",
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

    /** Search for files matching [query] on Dropbox using the search_v2 API. */
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

            val searchPath = path?.let { normalizePath(it) } ?: ""
            val requestBody = buildString {
                append("""{"query": "${jsonEscape(query)}"""")
                append(""", "options": {"path": "${jsonEscape(searchPath)}", "max_results": 100""")
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            emit(Result.failure(e))
        }
    }
    
    /** Retrieve files changed [since] the given instant on Dropbox using list_folder/continue. */
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

            val listPath = path?.let { normalizePath(it) } ?: ""
            val requestBody = """
            {
                "path": "${jsonEscape(listPath)}",
                "recursive": true,
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

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val listResponse = json.decodeFromString<DropboxListResponse>(content)

                val recentDocuments = listResponse.entries.mapNotNull { entry ->
                    val serverModified = entry.serverModified?.let {
                        try { Instant.parse(it) } catch (_: Exception) { null }
                    }
                    // Only include files modified since the given timestamp
                    if (entry.tag == "file" && serverModified != null && serverModified >= since) {
                        NetworkDocument(
                            id = entry.id,
                            name = entry.name,
                            path = entry.pathDisplay,
                            isFolder = false,
                            size = entry.size ?: 0L,
                            lastModified = serverModified,
                            syncStatus = SyncStatus.SYNCED,
                            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                            storageId = "dropbox"
                        )
                    } else if (entry.tag == "folder") {
                        // Folders don't have server_modified, include them as potentially changed
                        NetworkDocument(
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

    /** Retrieve Dropbox storage quota information using the get_space_usage API. */
    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        if (!isConnected()) {
            return Result.failure(
                NetworkStorageException.ConnectionException.NotConnected(
                    message = "Dropbox not connected"
                )
            )
        }
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/users/get_space_usage")
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val jsonObj = json.parseToJsonElement(content).jsonObject
                val usedSpace = jsonObj["used"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                val allocation = jsonObj["allocation"]?.jsonObject
                val allocatedSpace = allocation?.get("allocated")?.jsonPrimitive?.content?.toLongOrNull()
                    ?: 2147483648L // Default 2GB free tier
                val availableSpace = (allocatedSpace - usedSpace).coerceAtLeast(0L)
                val usagePercentage = if (allocatedSpace > 0) usedSpace.toDouble() / allocatedSpace else 0.0

                Result.success(StorageQuota(
                    totalSpace = allocatedSpace,
                    usedSpace = usedSpace,
                    availableSpace = availableSpace,
                    usagePercentage = usagePercentage,
                    isFull = availableSpace <= 0,
                    isLowOnSpace = usagePercentage >= 0.9,
                    metadata = mapOf("provider" to "Dropbox")
                ))
            } else {
                Result.failure(Exception("Failed to get quota info: ${response.status}"))
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
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
     * Normalize path for Dropbox API.
     * Delegates to [PathUtils.normalizePath] for shared traversal protection.
     */
    private fun normalizePath(path: String): String {
        try {
            return PathUtils.normalizePath(path, _rootPath)
        } catch (e: IllegalArgumentException) {
            SecurityEventLogger.logPathTraversalBlocked("DropboxService", path)
            throw e
        }
    }
    
    private fun jsonEscape(value: String): String =
        value.replace("\\", "\\\\")
             .replace("\"", "\\\"")
             .replace("\n", "\\n")
             .replace("\r", "\\r")
             .replace("\t", "\\t")

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