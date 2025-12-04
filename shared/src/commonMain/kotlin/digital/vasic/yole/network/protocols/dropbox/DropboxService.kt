package digital.vasic.yole.network.protocols.dropbox

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorageFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Dropbox implementation of NetworkStorageService
 * Provides Dropbox API integration with OAuth2 authentication
 */
class DropboxService(
    override val config: StorageConfig.DropboxConfig
) : NetworkStorageService {
    
    private val httpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.DefaultRequest) {
            header("Authorization", "Bearer ${config.accessToken}")
        }
    }
    
    private var _isConnected = false
    private var _rootPath = if (config.rootPath.isBlank()) "/" else config.rootPath
    
    override val isOnline: Boolean
        get() = _isConnected
    
    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "dropbox_${config.name}",
            name = config.name,
            type = StorageType.DROPBOX,
            location = "dropbox://",
            isOnline = _isConnected,
            lastSync = Clock.System.now()
        )
    }
    
    override suspend fun connect(): Result<Unit> = try {
        // Test Dropbox API connection by getting account info
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.dropboxapi.com"
                path("2/users/get_current_account")
            }
        }
        
        if (response.status.isSuccess()) {
            _isConnected = true
            Result.success(Unit)
        } else {
            Result.failure(NetworkStorageException.ConnectionError.Failed(
                message = "Dropbox connection failed: ${response.status}",
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.ConnectionError.Failed(
            message = "Dropbox connection failed",
            cause = e
        ))
    }
    
    override suspend fun disconnect(): Result<Unit> = try {
        httpClient.close()
        _isConnected = false
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "disconnect"))
    }
    
    override suspend fun testConnection(): Result<Boolean> = try {
        val connectResult = connect()
        if (connectResult.isSuccess) {
            disconnect()
            Result.success(true)
        } else {
            Result.failure(connectResult.exceptionOrNull() ?: Exception("Connection test failed"))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "testConnection"))
    }
    
    override suspend fun listFiles(path: String = "/"): Flow<Result<List<NetworkDocument>>> = flow {
        if (!_isConnected) {
            emit(Result.failure(NetworkStorageException.ConnectionError.NotConnected(
                message = "Dropbox not connected"
            )))
            return@flow
        }
        
        try {
            val fullPath = normalizePath(path)
            
            val requestBody = """
            {
                "path": "$fullPath",
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
                setBody(requestBody)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            
            if (!response.status.isSuccess()) {
                emit(Result.failure(NetworkStorageException.FileOperationError.ListFailed(
                    path = path,
                    cause = Exception(response.status.toString())
                )))
                return@flow
            }
            
            val content = response.bodyAsText()
            val listResponse = Json.decodeFromString<DropboxListResponse>(content)
            
            listResponse.entries.forEach { entry ->
                val document = when {
                    entry.tag == "folder" -> NetworkDocument(
                        id = entry.pathLower,
                        name = entry.name,
                        path = entry.pathDisplay,
                        isFolder = true,
                        size = 0L,
                        lastModified = Clock.System.now(),
                        syncStatus = SyncStatus.SYNCED,
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                    )
                    entry.tag == "file" -> NetworkDocument(
                        id = entry.pathLower,
                        name = entry.name,
                        path = entry.pathDisplay,
                        isFolder = false,
                        size = entry.size,
                        lastModified = kotlinx.datetime.Instant.parse(entry.serverModified),
                        syncStatus = SyncStatus.SYNCED,
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
                    )
                    else -> null
                }
                
                document?.let { emit(Result.success(it)) }
            }
            
        } catch (e: Exception) {
            emit(Result.failure(NetworkStorageException.FileOperationError.ListFailed(
                path = path,
                cause = e
            )))
        }
    }
    
    override suspend fun uploadFile(
        localPath: String,
        remotePath: String,
        progressCallback: ((Float) -> Unit)?
    ): Result<NetworkDocument> = try {
        if (!_isConnected) {
            return Result.failure(NetworkStorageException.ConnectionError.NotConnected(
                message = "Dropbox not connected"
            ))
        }
        
        val fullPath = normalizePath(remotePath)
        val fileBytes = kotlin.io.readBytes(localPath)
        
        progressCallback?.invoke(0f)
        
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "content.dropboxapi.com"
                path("2/files/upload")
            }
            header("Dropbox-API-Arg", Json.encodeToString(DropboxUploadArg(fullPath, "overwrite")))
            header(HttpHeaders.ContentType, "application/octet-stream")
            setBody(fileBytes)
        }
        
        progressCallback?.invoke(1f)
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val fileMetadata = Json.decodeFromString<DropboxFileMetadata>(content)
            
            Result.success(NetworkDocument(
                id = fileMetadata.pathLower,
                name = fileMetadata.name,
                path = fileMetadata.pathDisplay,
                isFolder = false,
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                size = fileMetadata.size,
                lastModified = kotlinx.datetime.Instant.parse(fileMetadata.serverModified)
            ))
        } else {
            Result.failure(NetworkStorageException.FileOperationError.UploadFailed(
                path = remotePath,
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.FileOperationError.UploadFailed(
            path = remotePath,
            cause = e
        ))
    }
    
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String,
        progressCallback: ((Float) -> Unit)?
    ): Result<Unit> = try {
        if (!_isConnected) {
            return Result.failure(NetworkStorageException.ConnectionError.NotConnected(
                message = "Dropbox not connected"
            ))
        }
        
        val fullPath = normalizePath(remotePath)
        
        progressCallback?.invoke(0f)
        
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "content.dropboxapi.com"
                path("2/files/download")
            }
            header("Dropbox-API-Arg", Json.encodeToString(DropboxDownloadArg(fullPath)))
        }
        
        progressCallback?.invoke(1f)
        
        if (response.status.isSuccess()) {
            val bytes = response.bodyAsBytes()
            java.io.File(localPath).writeBytes(bytes)
            Result.success(Unit)
        } else {
            Result.failure(NetworkStorageException.FileOperationError.DownloadFailed(
                path = remotePath,
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.FileOperationError.DownloadFailed(
            path = remotePath,
            cause = e
        ))
    }
    
    override suspend fun deleteFile(path: String): Result<Unit> = try {
        if (!_isConnected) {
            return Result.failure(NetworkStorageException.ConnectionError.NotConnected(
                message = "Dropbox not connected"
            ))
        }
        
        val fullPath = normalizePath(path)
        
        val requestBody = """
        {
            "path": "$fullPath"
        }
        """.trimIndent()
        
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.dropboxapi.com"
                path("2/files/delete_v2")
            }
            setBody(requestBody)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            Result.failure(NetworkStorageException.FileOperationError.DeleteFailed(
                path = path,
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.FileOperationError.DeleteFailed(
            path = path,
            cause = e
        ))
    }
    
    override suspend fun createFolder(path: String): Result<NetworkDocument> = try {
        if (!_isConnected) {
            return Result.failure(NetworkStorageException.ConnectionError.NotConnected(
                message = "Dropbox not connected"
            ))
        }
        
        val fullPath = normalizePath(path)
        
        val requestBody = """
        {
            "path": "$fullPath",
            "autorename": false
        }
        """.trimIndent()
        
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.dropboxapi.com"
                path("2/files/create_folder_v2")
            }
            setBody(requestBody)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val folderMetadata = Json.decodeFromString<DropboxFolderMetadata>(content)
            
            Result.success(NetworkDocument(
                id = folderMetadata.metadata.pathLower,
                name = folderMetadata.metadata.name,
                path = folderMetadata.metadata.pathDisplay,
                isFolder = true,
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                size = 0L,
                lastModified = Clock.System.now()
            ))
        } else {
            Result.failure(NetworkStorageException.FileOperationError.CreateFolderFailed(
                path = path,
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.FileOperationError.CreateFolderFailed(
            path = path,
            cause = e
        ))
    }
    
    override suspend fun moveFile(sourcePath: String, targetPath: String): Result<NetworkDocument> = try {
        if (!_isConnected) {
            return Result.failure(NetworkStorageException.ConnectionError.NotConnected(
                message = "Dropbox not connected"
            ))
        }
        
        val fullSourcePath = normalizePath(sourcePath)
        val fullTargetPath = normalizePath(targetPath)
        
        val requestBody = """
        {
            "from_path": "$fullSourcePath",
            "to_path": "$fullTargetPath",
            "autorename": false,
            "allow_shared_folder": false,
            "allow_ownership_transfer": false
        }
        """.trimIndent()
        
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.dropboxapi.com"
                path("2/files/move_v2")
            }
            setBody(requestBody)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val metadata = Json.decodeFromString<DropboxMoveResponse>(content)
            
            val document = when (metadata.metadata.tag) {
                "file" -> {
                    val fileMetadata = Json.decodeFromString<DropboxFileMetadata>(Json.encodeToString(metadata.metadata))
                    NetworkDocument(
                        id = fileMetadata.pathLower,
                        name = fileMetadata.name,
                        path = fileMetadata.pathDisplay,
                        isFolder = false,
                        syncStatus = SyncStatus.SYNCED,
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                        size = fileMetadata.size,
                        lastModified = kotlinx.datetime.Instant.parse(fileMetadata.serverModified)
                    )
                }
                "folder" -> {
                    val folderMetadata = Json.decodeFromString<DropboxFolderMetadata>(Json.encodeToString(metadata.metadata))
                    NetworkDocument(
                        id = folderMetadata.metadata.pathLower,
                        name = folderMetadata.metadata.name,
                        path = folderMetadata.metadata.pathDisplay,
                        isFolder = true,
                        syncStatus = SyncStatus.SYNCED,
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                        size = 0L,
                        lastModified = Clock.System.now()
                    )
                }
                else -> null
            }
            
            document?.let { Result.success(it) }
                ?: Result.failure(Exception("Unknown file type"))
        } else {
            Result.failure(NetworkStorageException.FileOperationError.MoveFailed(
                sourcePath = sourcePath,
                targetPath = targetPath,
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.FileOperationError.MoveFailed(
            sourcePath = sourcePath,
            targetPath = targetPath,
            cause = e
        ))
    }
    
    override suspend fun getDocumentInfo(path: String): Result<NetworkDocument> = try {
        if (!_isConnected) {
            return Result.failure(NetworkStorageException.ConnectionError.NotConnected(
                message = "Dropbox not connected"
            ))
        }
        
        val fullPath = normalizePath(path)
        
        val requestBody = """
        {
            "path": "$fullPath",
            "include_media_info": false,
            "include_deleted": false,
            "include_has_explicit_shared_members": false
        }
        """.trimIndent()
        
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.dropboxapi.com"
                path("2/files/get_metadata")
            }
            setBody(requestBody)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val metadata = Json.decodeFromString<DropboxMetadata>(content)
            
            val document = when (metadata.tag) {
                "file" -> {
                    val fileMetadata = Json.decodeFromString<DropboxFileMetadata>(content)
                    NetworkDocument(
                        id = fileMetadata.pathLower,
                        name = fileMetadata.name,
                        path = fileMetadata.pathDisplay,
                        isFolder = false,
                        syncStatus = SyncStatus.SYNCED,
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                        size = fileMetadata.size,
                        lastModified = kotlinx.datetime.Instant.parse(fileMetadata.serverModified)
                    )
                }
                "folder" -> {
                    val folderMetadata = Json.decodeFromString<DropboxFolderMetadata>(content)
                    NetworkDocument(
                        id = folderMetadata.metadata.pathLower,
                        name = folderMetadata.metadata.name,
                        path = folderMetadata.metadata.pathDisplay,
                        isFolder = true,
                        syncStatus = SyncStatus.SYNCED,
                        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE),
                        size = 0L,
                        lastModified = Clock.System.now()
                    )
                }
                else -> null
            }
            
            document?.let { Result.success(it) }
                ?: Result.failure(Exception("Unknown file type"))
        } else {
            Result.failure(NetworkStorageException.FileOperationError.InfoFailed(
                path = path,
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.FileOperationError.InfoFailed(
            path = path,
            cause = e
        ))
    }
    
    override suspend fun search(
        query: String,
        path: String,
        recursive: Boolean
    ): Flow<Result<NetworkDocument>> = flow {
        if (!_isConnected) {
            emit(Result.failure(NetworkStorageException.ConnectionError.NotConnected(
                message = "Dropbox not connected"
            )))
            return@flow
        }
        
        try {
            val fullPath = normalizePath(path)
            
            val requestBody = """
            {
                "path": "$fullPath",
                "query": "$query",
                "start": 0,
                "max_results": 1000,
                "mode": "filename"
            }
            """.trimIndent()
            
            val response = httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.dropboxapi.com"
                    path("2/files/search_v2")
                }
                setBody(requestBody)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            
            if (!response.status.isSuccess()) {
                emit(Result.failure(NetworkStorageException.FileOperationError.SearchFailed(
                    query = query,
                    path = path,
                    cause = Exception(response.status.toString())
                )))
                return@flow
            }
            
            val content = response.bodyAsText()
            val searchResponse = Json.decodeFromString<DropboxSearchResponse>(content)
            
            searchResponse.matches.forEach { match ->
                val document = when {
                    match.metadata.tag == "folder" -> NetworkDocument(
                        id = match.metadata.pathLower,
                        name = match.metadata.name,
                        path = match.metadata.pathDisplay,
                        type = DocumentType.FOLDER,
                        size = 0L,
                        lastModified = Clock.System.now(),
                        permissions = DocumentPermission(
                            canRead = true,
                            canWrite = true,
                            canDelete = true,
                            canExecute = true
                        )
                    )
                    match.metadata.tag == "file" -> NetworkDocument(
                        id = match.metadata.pathLower,
                        name = match.metadata.name,
                        path = match.metadata.pathDisplay,
                        type = DocumentType.FILE,
                        size = match.metadata.size,
                        lastModified = kotlinx.datetime.Instant.parse(match.metadata.serverModified),
                        permissions = DocumentPermission(
                            canRead = true,
                            canWrite = true,
                            canDelete = true,
                            canExecute = false
                        )
                    )
                    else -> null
                }
                
                document?.let { emit(Result.success(it)) }
            }
            
        } catch (e: Exception) {
            emit(Result.failure(NetworkStorageException.FileOperationError.SearchFailed(
                query = query,
                path = path,
                cause = e
            )))
        }
    }
    
    /**
     * Normalize path for Dropbox API
     */
    private fun normalizePath(path: String): String {
        return if (path.isBlank()) _rootPath
        else "$_rootPath$path".replace("//", "/")
    }
    
    // Dropbox API data classes
    @kotlinx.serialization.Serializable
    private data class DropboxListResponse(
        val entries: List<DropboxMetadata>
    )
    
    @kotlinx.serialization.Serializable
    private data class DropboxMetadata(
        val tag: String,
        val name: String,
        val pathLower: String,
        val pathDisplay: String,
        val size: Long = 0L,
        val serverModified: String = "",
        val id: String = ""
    )
    
    @kotlinx.serialization.Serializable
    private data class DropboxFileMetadata(
        val tag: String,
        val name: String,
        val pathLower: String,
        val pathDisplay: String,
        val size: Long,
        val serverModified: String,
        val id: String
    )
    
    @kotlinx.serialization.Serializable
    private data class DropboxFolderMetadata(
        val tag: String,
        val metadata: DropboxFolderMetadataDetails
    )
    
    @kotlinx.serialization.Serializable
    private data class DropboxFolderMetadataDetails(
        val tag: String,
        val name: String,
        val pathLower: String,
        val pathDisplay: String,
        val id: String
    )
    
    @kotlinx.serialization.Serializable
    private data class DropboxUploadArg(
        val path: String,
        val mode: String
    )
    
    @kotlinx.serialization.Serializable
    private data class DropboxDownloadArg(
        val path: String
    )
    
    @kotlinx.serialization.Serializable
    private data class DropboxMoveResponse(
        val metadata: DropboxMetadata
    )
    
    @kotlinx.serialization.Serializable
    private data class DropboxSearchResponse(
        val matches: List<DropboxSearchMatch>
    )
    
    @kotlinx.serialization.Serializable
    private data class DropboxSearchMatch(
        val metadata: DropboxFileMetadata
    )
}