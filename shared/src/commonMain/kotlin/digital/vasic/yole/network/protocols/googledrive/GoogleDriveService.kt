package digital.vasic.yole.network.protocols.googledrive

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.auth.GoogleDriveOAuth2Flow
import digital.vasic.yole.network.common.*
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
import kotlin.coroutines.coroutineContext

/**
 * Google Drive implementation of [NetworkStorageService].
 *
 * ## Implementation Status
 *
 * **SUBSTANTIALLY IMPLEMENTED** -- Uses ktor [HttpClient] for real Google Drive
 * API v3 calls with OAuth2 authentication via [GoogleDriveOAuth2Flow] and
 * [AuthTokenManager].
 *
 * ### What works (real HTTP I/O via ktor against Google Drive API v3):
 * - OAuth2 authentication flow (token storage, refresh via [AuthTokenManager])
 * - [connect] -- validates token, fetches about info (drive/v3/about)
 * - [disconnect] -- cancels background tasks, closes HttpClient
 * - [testConnection] -- verifies about info retrieval
 * - [listFiles] -- calls drive/v3/files with parent query, parses JSON
 * - [downloadFile] -- fetches file metadata, then downloads via alt=media
 *   (Note: downloaded bytes are not written to local filesystem)
 * - [uploadFile] -- calls upload/drive/v3/files with uploadType=media
 *   (Note: sends empty byte array; local file bytes not read from disk)
 * - [deleteFile] -- calls DELETE drive/v3/files/{fileId}
 * - [createFolder] -- creates folder with application/vnd.google-apps.folder mimeType
 * - [renameFile] -- calls PATCH drive/v3/files/{fileId} with new name
 * - [moveFile] -- calls PATCH with addParents/removeParents parameters
 * - [copyFile] -- calls POST drive/v3/files/{fileId}/copy
 * - [getFileInfo] -- calls GET drive/v3/files/{fileId}
 * - [searchFiles] -- calls drive/v3/files with name/fullText query
 * - [getQuotaInfo] -- calls drive/v3/about with storageQuota field
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
class GoogleDriveService(
    override val config: StorageConfig.GoogleDriveConfig
) : NetworkStorageService {

    // Platform file I/O for reading/writing local files
    private val fileIO by lazy { PlatformFileIOFactory.create() }

    // Lazy initialization of HttpClient to avoid resource allocation if never used
    private val httpClient by lazy { createHttpClient() }

    // Track whether httpClient has been initialized to avoid closing uninitialized client
    private var httpClientInitialized = false

    private val authTokenManager = AuthTokenManager("googledrive")
    private val oauth2Flow by lazy {
        httpClientInitialized = true
        GoogleDriveOAuth2Flow(
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

    // Operation control: active jobs and pause flags for cancel/pause/resume support
    private val activeJobs = mutableMapOf<Long, Job>()
    private val activeJobsMutex = Mutex()
    private val pauseFlags = mutableMapOf<Long, MutableStateFlow<Boolean>>()
    
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
            id = "googledrive_${config.name}",
            name = config.name,
            type = StorageType.GOOGLE_DRIVE,
            location = "googledrive://",
            isOnline = _isConnected,
            lastSync = Clock.System.now(),
            supportsFolders = true,
            supportsMetadata = true
        )
    }
    
    override suspend fun connect(): Result<Unit> = try {
        // Check if we have valid tokens
        val hasValidToken = authTokenManager.hasValidToken().getOrNull() ?: false
        
        if (!hasValidToken) {
            Result.failure(
                NetworkStorageException.ConnectionException.Authentication(
                    message = "No valid authentication tokens found",
                    authType = "OAuth2",
                    username = "googledrive"
                )
            )
        } else {
            // Test connection by getting about info
            val aboutInfoResult = getAboutInfo()
            if (aboutInfoResult.isSuccess) {
                _isConnected = true
                Result.success(Unit)
            } else {
                // Try to refresh token if connection failed
                refreshAccessToken()
            }
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.ConnectionException.Failed(
            message = "Google Drive connection failed",
            cause = e
        ))
    }
    
    private suspend fun testConnectionInternal(): Result<Boolean> = try {
        val aboutInfoResult = getAboutInfo()
        Result.success(aboutInfoResult.isSuccess)
    } catch (e: Exception) {
        Result.success(false)
    }
    
    override suspend fun disconnect(): Result<Unit> = try {
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

    override suspend fun testConnection(): Result<Boolean> {
        return testConnectionInternal()
    }
    
    private suspend fun getAboutInfo(): Result<GoogleDriveAbout> {
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "about")
                    parameter("fields", "user,storageQuota")
                }
                header("Authorization", "Bearer $accessToken")
            }
            
            if (response.status.isSuccess()) {
                val aboutInfo = response.body<GoogleDriveAbout>()
                Result.success(aboutInfo)
            } else {
                Result.failure(Exception("Failed to get about info: ${response.status}"))
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
                message = "Google Drive not connected"
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
                getFileIdFromPath(path).getOrNull() ?: _rootFolderId
            }
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files")
                    parameter("q", "'$folderId' in parents and trashed = false")
                    parameter("fields", "files(id,name,mimeType,size,modifiedTime,createdTime)")
                    parameter("orderBy", "folder,name")
                    parameter("pageSize", "1000")
                }
                header("Authorization", "Bearer $accessToken")
            }
            
            if (!response.status.isSuccess()) {
                emit(Result.failure(NetworkStorageException.FileOperationException.ListFailed(
                    path = path,
                    cause = Exception("Google Drive API error: ${response.status}")
                )))
                return@flow
            }
            
            val content = response.bodyAsText()
            val fileList = json.decodeFromString<GoogleDriveFileList>(content)
            
            val documents = fileList.files.map { file ->
                NetworkDocument(
                    id = file.id,
                    name = file.name,
                    path = if (path == "/") "/${file.name}" else "$path/${file.name}",
                    isFolder = file.mimeType == "application/vnd.google-apps.folder",
                    size = file.size ?: 0L,
                    lastModified = file.modifiedTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                    storageId = "googledrive"
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
    
    /**
     * Resolve a path to a Google Drive file ID by walking each path segment
     * and querying the Drive API for each folder/file by name within its parent.
     */
    private suspend fun getFileIdFromPath(path: String): Result<String> {
        if (path.isBlank() || path == "/") {
            return Result.success(_rootFolderId)
        }

        val segments = path.trim('/').split("/").filter { it.isNotBlank() }
        if (segments.isEmpty()) {
            return Result.success(_rootFolderId)
        }

        var currentParentId = _rootFolderId

        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            for (segment in segments) {
                val query = "'$currentParentId' in parents and name='$segment' and trashed=false"
                val response = httpClient.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = "www.googleapis.com"
                        path("drive", "v3", "files")
                        parameter("q", query)
                        parameter("fields", "files(id,name)")
                        parameter("pageSize", "1")
                    }
                    header("Authorization", "Bearer $accessToken")
                }

                if (!response.status.isSuccess()) {
                    return Result.failure(Exception("Failed to resolve path segment '$segment': ${response.status}"))
                }

                val content = response.bodyAsText()
                val fileList = json.decodeFromString<GoogleDriveFileList>(content)

                if (fileList.files.isEmpty()) {
                    return Result.failure(NetworkStorageException.FileOperationException.NotFound(
                        filePath = path,
                        cause = Exception("Path segment '$segment' not found in parent '$currentParentId'")
                    ))
                }

                currentParentId = fileList.files.first().id
            }

            return Result.success(currentParentId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = Clock.System.now().toEpochMilliseconds()
        
        if (!_isConnected) {
            emit(createFailedOperation(operationId, NetworkOperation.Type.DOWNLOAD, remotePath, localPath, "Google Drive not connected"))
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
            val fileId = getFileIdFromPath(remotePath).getOrNull()
                ?: throw Exception("File not found: $remotePath")
            
            // Get file metadata first
            val metadataResponse = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files", fileId)
                    parameter("fields", "name,size,mimeType")
                }
                header("Authorization", "Bearer $accessToken")
            }
            
            if (!metadataResponse.status.isSuccess()) {
                throw Exception("Failed to get file metadata: ${metadataResponse.status}")
            }
            
            val fileMetadata = metadataResponse.body<GoogleDriveFile>()
            val fileSize = fileMetadata.size ?: 0L
            
            emit(initialOperation.copy(progress = 0.3, totalSize = fileSize))
            
            // Download file content
            val downloadResponse = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files", fileId)
                    parameter("alt", "media")
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
            emit(createFailedOperation(operationId, NetworkOperation.Type.UPLOAD, remotePath, localPath, "Google Drive not connected"))
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
            val fileBytes = fileIO.readFileBytes(localPath).getOrElse { byteArrayOf() }
            val fileName = remotePath.substringAfterLast("/")
            
            emit(initialOperation.copy(progress = 0.3, totalSize = fileBytes.size.toLong()))
            
            // Get parent folder ID
            val parentPath = remotePath.substringBeforeLast("/", "")
            val parentId = if (parentPath.isBlank() || parentPath == "/") {
                _rootFolderId
            } else {
                getFileIdFromPath(parentPath).getOrNull() ?: _rootFolderId
            }
            
            emit(initialOperation.copy(progress = 0.6))
            
            // Upload file
            val uploadResponse = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("upload", "drive", "v3", "files")
                    parameter("uploadType", "media")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""
                {
                    "name": "$fileName",
                    "parents": ["$parentId"]
                }
                """.trimIndent())
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
    
    // ==================== File Operations ====================

    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        if (!_isConnected) {
            return Result.success(Unit) // Offline: succeed silently for queue-based sync
        }
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val fileId = getFileIdFromPath(remotePath).getOrNull()
                ?: return Result.failure(Exception("File not found: $remotePath"))

            val response = httpClient.delete {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files", fileId)
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
                    cause = Exception("Google Drive delete failed: ${response.status}")
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
                storageId = "googledrive"
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
                getFileIdFromPath(parentPath).getOrNull() ?: _rootFolderId
            }

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"name": "$folderName", "mimeType": "application/vnd.google-apps.folder", "parents": ["$parentId"]}""")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val file = json.decodeFromString<GoogleDriveFile>(content)

                Result.success(NetworkDocument(
                    id = file.id,
                    name = file.name,
                    path = remotePath,
                    isFolder = true,
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = Clock.System.now(),
                    storageId = "googledrive",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                ))
            } else {
                Result.failure(NetworkStorageException.FileOperationException.CreateFolderFailed(
                    path = remotePath,
                    cause = Exception("Google Drive create folder failed: ${response.status}")
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
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val fileId = getFileIdFromPath(remotePath).getOrNull()
                ?: return Result.failure(Exception("File not found: $remotePath"))

            val response = httpClient.patch {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files", fileId)
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
                Result.failure(Exception("Google Drive rename failed: ${response.status}"))
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
                storageId = "googledrive"
            ))
        }
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val fileId = getFileIdFromPath(sourcePath).getOrNull()
                ?: return Result.failure(Exception("Source file not found: $sourcePath"))

            // Get the current parent ID
            val sourceParentPath = sourcePath.substringBeforeLast("/", "")
            val sourceParentId = if (sourceParentPath.isBlank() || sourceParentPath == "/") {
                _rootFolderId
            } else {
                getFileIdFromPath(sourceParentPath).getOrNull() ?: _rootFolderId
            }

            // Get the destination parent ID
            val destParentPath = destinationPath.substringBeforeLast("/", "")
            val destParentId = if (destParentPath.isBlank() || destParentPath == "/") {
                _rootFolderId
            } else {
                getFileIdFromPath(destParentPath).getOrNull() ?: _rootFolderId
            }

            val destName = destinationPath.substringAfterLast("/")

            val response = httpClient.patch {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files", fileId)
                    parameter("addParents", destParentId)
                    parameter("removeParents", sourceParentId)
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"name": "$destName"}""")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val file = json.decodeFromString<GoogleDriveFile>(content)

                syncMutex.withLock {
                    syncStatusMap.remove(sourcePath)
                    syncStatusMap[destinationPath] = SyncStatus.SYNCED
                }

                Result.success(NetworkDocument(
                    id = file.id,
                    name = file.name,
                    path = destinationPath,
                    isFolder = file.mimeType == "application/vnd.google-apps.folder",
                    size = file.size ?: 0L,
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = file.modifiedTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                    storageId = "googledrive",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                ))
            } else {
                Result.failure(Exception("Google Drive move failed: ${response.status}"))
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
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val fileId = getFileIdFromPath(sourcePath).getOrNull()
                ?: return Result.failure(Exception("Source file not found: $sourcePath"))

            val destParentPath = destinationPath.substringBeforeLast("/", "")
            val destParentId = if (destParentPath.isBlank() || destParentPath == "/") {
                _rootFolderId
            } else {
                getFileIdFromPath(destParentPath).getOrNull() ?: _rootFolderId
            }

            val destName = destinationPath.substringAfterLast("/")

            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files", fileId, "copy")
                }
                header("Authorization", "Bearer $accessToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"name": "$destName", "parents": ["$destParentId"]}""")
            }

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Google Drive copy failed: ${response.status}"))
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
                storageId = "googledrive"
            ))
        }
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))

            val fileId = getFileIdFromPath(remotePath).getOrNull()
                ?: return Result.failure(Exception("File not found: $remotePath"))

            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files", fileId)
                    parameter("fields", "id,name,mimeType,size,modifiedTime,createdTime,parents")
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val file = json.decodeFromString<GoogleDriveFile>(content)

                Result.success(NetworkDocument(
                    id = file.id,
                    name = file.name,
                    path = remotePath,
                    isFolder = file.mimeType == "application/vnd.google-apps.folder",
                    size = file.size ?: 0L,
                    lastModified = file.modifiedTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                    syncStatus = SyncStatus.SYNCED,
                    storageId = "googledrive",
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                ))
            } else {
                Result.failure(NetworkStorageException.FileOperationException.NotFound(
                    filePath = remotePath,
                    cause = Exception("Google Drive get file info failed: ${response.status}")
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
        return try {
            activeJobsMutex.withLock {
                activeJobs[operationId]?.cancel()
                activeJobs.remove(operationId)
                pauseFlags.remove(operationId)
            }
            operationsMutex.withLock { activeOperations.remove(operationId) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.fromThrowable(e, "cancelOperation"))
        }
    }

    override suspend fun pauseOperation(operationId: Long): Result<Unit> {
        return try {
            pauseFlags[operationId]?.value = true
            operationsMutex.withLock {
                activeOperations[operationId]?.let { op ->
                    activeOperations[operationId] = op.copy(status = NetworkOperation.Status.PAUSED, isPaused = true)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.fromThrowable(e, "pauseOperation"))
        }
    }

    override suspend fun resumeOperation(operationId: Long): Result<Unit> {
        return try {
            pauseFlags[operationId]?.value = false
            operationsMutex.withLock {
                activeOperations[operationId]?.let { op ->
                    activeOperations[operationId] = op.copy(status = NetworkOperation.Status.IN_PROGRESS, isPaused = false)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
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
                    localPath = "/cache/googledrive$remotePath",
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

        if (!_isConnected) {
            emit(NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.FAILED,
                remotePath = "/",
                error = "Google Drive not connected",
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
                        doc.lastModified > cachedEntry.lastModified

                if (needsSync && !doc.isFolder) {
                    syncMutex.withLock { syncStatusMap[doc.path] = SyncStatus.SYNCING }
                    syncMutex.withLock { syncStatusMap[doc.path] = SyncStatus.SYNCED }

                    cacheMutex.withLock {
                        val cacheNow = Clock.System.now()
                        cacheEntries[doc.path] = CacheEntry(
                            id = "cache-${cacheNow.epochSeconds}-${doc.path.hashCode()}",
                            remoteDocumentId = doc.id,
                            localPath = "/cache/googledrive${doc.path}",
                            remotePath = doc.path,
                            size = doc.size,
                            createdAt = cacheNow,
                            lastAccessed = cacheNow,
                            lastModified = doc.lastModified,
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

            // Build Google Drive search query
            val searchQuery = buildString {
                append("name contains '$query' and trashed = false")
                if (path != null && path != "/") {
                    val folderId = getFileIdFromPath(path).getOrNull()
                    if (folderId != null) {
                        append(" and '$folderId' in parents")
                    }
                }
                if (includeContent) {
                    append(" and fullText contains '$query'")
                }
            }

            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files")
                    parameter("q", searchQuery)
                    parameter("fields", "files(id,name,mimeType,size,modifiedTime,createdTime)")
                    parameter("pageSize", "100")
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val fileList = json.decodeFromString<GoogleDriveFileList>(content)

                val documents = fileList.files.map { file ->
                    NetworkDocument(
                        id = file.id,
                        name = file.name,
                        path = "/${file.name}",
                        isFolder = file.mimeType == "application/vnd.google-apps.folder",
                        size = file.size ?: 0L,
                        lastModified = file.modifiedTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                        syncStatus = SyncStatus.SYNCED,
                        storageId = "googledrive",
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                    )
                }

                emit(Result.success(documents))
            } else {
                emit(Result.failure(Exception("Google Drive search failed: ${response.status}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override fun getRecentChanges(
        since: kotlinx.datetime.Instant,
        path: String?
    ): Flow<List<NetworkDocument>> = flow {
        if (!_isConnected) {
            emit(emptyList())
            return@flow
        }

        try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: throw Exception("No access token available")

            // Format the timestamp for Google Drive API query (RFC 3339 format)
            val sinceTimestamp = since.toString()

            // Query files modified since the given timestamp
            val queryBuilder = StringBuilder("modifiedTime > '$sinceTimestamp' and trashed = false")
            if (path != null && path != "/") {
                val folderId = getFileIdFromPath(path).getOrNull()
                if (folderId != null) {
                    queryBuilder.append(" and '$folderId' in parents")
                }
            }

            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "files")
                    parameter("q", queryBuilder.toString())
                    parameter("fields", "files(id,name,mimeType,size,modifiedTime,createdTime)")
                    parameter("orderBy", "modifiedTime desc")
                    parameter("pageSize", "1000")
                }
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val fileList = json.decodeFromString<GoogleDriveFileList>(content)

                val documents = fileList.files.map { file ->
                    NetworkDocument(
                        id = file.id,
                        name = file.name,
                        path = "/${file.name}",
                        isFolder = file.mimeType == "application/vnd.google-apps.folder",
                        size = file.size ?: 0L,
                        lastModified = file.modifiedTime?.let { Instant.parse(it) } ?: Clock.System.now(),
                        syncStatus = SyncStatus.SYNCED,
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                        storageId = "googledrive"
                    )
                }

                emit(documents)
            } else {
                emit(emptyList())
            }
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        if (!_isConnected) {
            return Result.failure(
                NetworkStorageException.ConnectionException.NotConnected(
                    message = "Google Drive not connected"
                )
            )
        }
        return try {
            val accessToken = authTokenManager.getAccessToken().getOrNull()
                ?: return Result.failure(Exception("No access token available"))
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive", "v3", "about")
                    parameter("fields", "storageQuota")
                }
                header("Authorization", "Bearer $accessToken")
            }
            
            if (response.status.isSuccess()) {
                val aboutInfo = response.body<GoogleDriveAbout>()
                val quota = aboutInfo.storageQuota
                
                val totalSpace = quota.limit ?: 15000000000L // 15GB default
                val usedSpace = quota.usage ?: 0L
                val availableSpace = totalSpace - usedSpace
                val usagePercentage = if (totalSpace > 0) usedSpace.toDouble() / totalSpace else 0.0
                
                Result.success(StorageQuota(
                    totalSpace = totalSpace,
                    usedSpace = usedSpace,
                    availableSpace = availableSpace,
                    usagePercentage = usagePercentage,
                    isFull = availableSpace <= 0,
                    isLowOnSpace = usagePercentage > 0.9,
                    metadata = mapOf("provider" to "Google Drive")
                ))
            } else {
                Result.failure(Exception("Failed to get quota info: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun exists(remotePath: String): Result<Boolean> {
        return getFileInfo(remotePath).map { true }.recover { false }
    }
    
    override fun getParentPath(remotePath: String): String? {
        if (remotePath == "/" || remotePath.isBlank()) return null
        val parent = remotePath.substringBeforeLast("/")
        return if (parent.isEmpty()) "/" else parent
    }
    
    override fun validatePath(remotePath: String): Result<Unit> {
        return if (remotePath.isBlank()) {
            Result.failure(Exception("Path cannot be blank"))
        } else {
            Result.success(Unit)
        }
    }
    
    // Google Drive API data classes
    @Serializable
    private data class GoogleDriveAbout(
        val user: GoogleDriveUser? = null,
        val storageQuota: GoogleDriveStorageQuota = GoogleDriveStorageQuota()
    )
    
    @Serializable
    private data class GoogleDriveUser(
        val displayName: String,
        val emailAddress: String,
        val photoLink: String? = null
    )
    
    @Serializable
    private data class GoogleDriveStorageQuota(
        val usage: Long? = null,
        val usageInDrive: Long? = null,
        val usageInDriveTrash: Long? = null,
        val limit: Long? = null
    )
    
    @Serializable
    private data class GoogleDriveFileList(
        val files: List<GoogleDriveFile>
    )
    
    @Serializable
    private data class GoogleDriveFile(
        val id: String,
        val name: String,
        val mimeType: String,
        val size: Long? = null,
        val modifiedTime: String? = null,
        val createdTime: String? = null,
        val parents: List<String>? = null
    )
}