package digital.vasic.yole.network.protocols.googledrive

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
 * Google Drive implementation of NetworkStorageService
 * Provides Google Drive API integration with OAuth2 authentication
 */
class GoogleDriveService(
    private val config: StorageConfig.GoogleDriveConfig
) : NetworkStorageService {
    
    private val httpClient = HttpClient(CIO) {
        defaultRequest {
            header("Authorization", "Bearer ${getAccessToken()}")
        }
    }
    
    private var _isConnected = false
    private var _rootFolderId = config.rootFolderId ?: "root"
    
    override val isOnline: Boolean
        get() = _isConnected
    
    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "googledrive_${config.name}",
            name = config.name,
            type = StorageType.GOOGLE_DRIVE,
            location = "googledrive://",
            isOnline = _isConnected,
            lastSync = Clock.System.now()
        )
    }
    
    override suspend fun connect(): Result<Unit> = try {
        // Test Google Drive API connection by getting user info
        val response = httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "www.googleapis.com"
                path("drive/v3/about")
                parameter("fields", "user")
            }
        }
        
        if (response.status.isSuccess()) {
            _isConnected = true
            Result.success(Unit)
        } else {
            Result.failure(NetworkStorageException.ConnectionError.Failed(
                message = "Google Drive connection failed: ${response.status}",
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.ConnectionError.Failed(
            message = "Google Drive connection failed",
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
    
    override suspend fun listFiles(path: String): Flow<Result<NetworkDocument>> = flow {
        if (!_isConnected) {
            emit(Result.failure(NetworkStorageException.ConnectionError.NotConnected(
                message = "Google Drive not connected"
            )))
            return@flow
        }
        
        try {
            val folderId = if (path.isBlank()) _rootFolderId else getFolderId(path).getOrNull()
            
            if (folderId == null) {
                emit(Result.failure(NetworkStorageException.FileOperationError.ListFailed(
                    path = path,
                    cause = Exception("Folder not found: $path")
                )))
                return@flow
            }
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive/v3/files")
                    parameter("q", "'$folderId' in parents and trashed=false")
                    parameter("fields", "files(id,name,mimeType,size,modifiedTime,permissions)")
                    parameter("pageSize", "1000")
                }
            }
            
            if (!response.status.isSuccess()) {
                emit(Result.failure(NetworkStorageException.FileOperationError.ListFailed(
                    path = path,
                    cause = Exception(response.status.toString())
                )))
                return@flow
            }
            
            val content = response.bodyAsText()
            val files = parseGoogleDriveFiles(content, path)
            
            files.forEach { document ->
                emit(Result.success(document))
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
                message = "Google Drive not connected"
            ))
        }
        
        val fileBytes = kotlin.io.readBytes(localPath)
        val fileName = remotePath.substringAfterLast("/")
        val parentFolderId = getParentFolderId(remotePath).getOrNull()
        
        if (parentFolderId == null) {
            return Result.failure(NetworkStorageException.FileOperationError.UploadFailed(
                path = remotePath,
                cause = Exception("Parent folder not found")
            ))
        }
        
        progressCallback?.invoke(0f)
        
        // Create file metadata
        val metadata = """
        {
            "name": "$fileName",
            "parents": ["$parentFolderId"]
        }
        """.trimIndent()
        
        // Upload file using multipart upload
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "www.googleapis.com"
                path("upload/drive/v3/files")
                parameter("uploadType", "multipart")
            }
            setBody(
                io.ktor.client.request.forms.MultiPartFormDataContent(
                    io.ktor.client.request.forms.formData {
                        append("metadata", metadata, Headers.build {
                            append(HttpHeaders.ContentType, "application/json")
                        })
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentType, "application/octet-stream")
                        })
                    }
                )
            )
        }
        
        progressCallback?.invoke(1f)
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val document = parseGoogleDriveFile(content, remotePath)
            Result.success(document)
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
                message = "Google Drive not connected"
            ))
        }
        
        val fileId = getFileId(remotePath).getOrNull()
        
        if (fileId == null) {
            return Result.failure(NetworkStorageException.FileOperationError.DownloadFailed(
                path = remotePath,
                cause = Exception("File not found: $remotePath")
            ))
        }
        
        progressCallback?.invoke(0f)
        
        val response = httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "www.googleapis.com"
                path("drive/v3/files/$fileId")
                parameter("alt", "media")
            }
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
                message = "Google Drive not connected"
            ))
        }
        
        val fileId = getFileId(path).getOrNull()
        
        if (fileId == null) {
            return Result.failure(NetworkStorageException.FileOperationError.DeleteFailed(
                path = path,
                cause = Exception("File not found: $path")
            ))
        }
        
        val response = httpClient.delete {
            url {
                protocol = URLProtocol.HTTPS
                host = "www.googleapis.com"
                path("drive/v3/files/$fileId")
            }
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
                message = "Google Drive not connected"
            ))
        }
        
        val folderName = path.substringAfterLast("/")
        val parentFolderId = getParentFolderId(path).getOrNull()
        
        if (parentFolderId == null) {
            return Result.failure(NetworkStorageException.FileOperationError.CreateFolderFailed(
                path = path,
                cause = Exception("Parent folder not found")
            ))
        }
        
        val metadata = """
        {
            "name": "$folderName",
            "mimeType": "application/vnd.google-apps.folder",
            "parents": ["$parentFolderId"]
        }
        """.trimIndent()
        
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "www.googleapis.com"
                path("drive/v3/files")
                parameter("fields", "id,name,mimeType,modifiedTime,permissions")
            }
            setBody(metadata)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val document = parseGoogleDriveFile(content, path)
            Result.success(document)
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
                message = "Google Drive not connected"
            ))
        }
        
        val fileId = getFileId(sourcePath).getOrNull()
        val targetFolderId = getParentFolderId(targetPath).getOrNull()
        
        if (fileId == null) {
            return Result.failure(NetworkStorageException.FileOperationError.MoveFailed(
                sourcePath = sourcePath,
                targetPath = targetPath,
                cause = Exception("Source file not found")
            ))
        }
        
        if (targetFolderId == null) {
            return Result.failure(NetworkStorageException.FileOperationError.MoveFailed(
                sourcePath = sourcePath,
                targetPath = targetPath,
                cause = Exception("Target folder not found")
            ))
        }
        
        val fileName = targetPath.substringAfterLast("/")
        
        val metadata = """
        {
            "name": "$fileName",
            "parents": ["$targetFolderId"]
        }
        """.trimIndent()
        
        val response = httpClient.patch {
            url {
                protocol = URLProtocol.HTTPS
                host = "www.googleapis.com"
                path("drive/v3/files/$fileId")
                parameter("fields", "id,name,mimeType,modifiedTime,permissions")
            }
            setBody(metadata)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val document = parseGoogleDriveFile(content, targetPath)
            Result.success(document)
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
                message = "Google Drive not connected"
            ))
        }
        
        val fileId = getFileId(path).getOrNull()
        
        if (fileId == null) {
            return Result.failure(NetworkStorageException.FileOperationError.InfoFailed(
                path = path,
                cause = Exception("File not found: $path")
            ))
        }
        
        val response = httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "www.googleapis.com"
                path("drive/v3/files/$fileId")
                parameter("fields", "id,name,mimeType,size,modifiedTime,permissions")
            }
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val document = parseGoogleDriveFile(content, path)
            Result.success(document)
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
                message = "Google Drive not connected"
            )))
            return@flow
        }
        
        try {
            val folderId = if (path.isBlank()) _rootFolderId else getFolderId(path).getOrNull()
            
            val searchQuery = if (folderId != null) {
                "name contains '$query' and '$folderId' in parents and trashed=false"
            } else {
                "name contains '$query' and trashed=false"
            }
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive/v3/files")
                    parameter("q", searchQuery)
                    parameter("fields", "files(id,name,mimeType,size,modifiedTime,permissions)")
                    parameter("pageSize", "1000")
                }
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
            val files = parseGoogleDriveFiles(content, path)
            
            files.forEach { document ->
                emit(Result.success(document))
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
     * Get access token from refresh token
     */
    private suspend fun getAccessToken(): String {
        // In a real implementation, this would use OAuth2 flow
        // For now, return the stored access token or refresh it
        val secureStorage = SecureStorageFactory.create().getOrThrow()
        return try {
            // Try to get stored access token
            val accessToken = secureStorage.getToken("googledrive_${config.name}_access")
            if (accessToken.isNotEmpty()) {
                return accessToken
            }
            
            // If no access token, use refresh token to get new one
            val refreshToken = secureStorage.getToken("googledrive_${config.name}")
            if (refreshToken.isNotEmpty()) {
                refreshAccessToken(refreshToken)
            } else {
                // Fallback to provided access token
                config.accessToken ?: ""
            }
        } catch (e: Exception) {
            config.accessToken ?: ""
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    private suspend fun refreshAccessToken(refreshToken: String): String {
        return try {
            val response = HttpClient(CIO).post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "oauth2.googleapis.com"
                    path("token")
                }
                setBody(
                    io.ktor.http.Parameters.build {
                        append("client_id", config.clientId)
                        append("client_secret", config.clientSecret)
                        append("refresh_token", refreshToken)
                        append("grant_type", "refresh_token")
                    }
                )
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
            }
            
            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val tokenResponse = Json.decodeFromString<OAuthTokenResponse>(content)
                
                // Store new access token
                val secureStorage = SecureStorageFactory.create().getOrThrow()
                secureStorage.storeToken("googledrive_${config.name}_access", tokenResponse.accessToken)
                
                tokenResponse.accessToken
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Get folder ID from path
     */
    private suspend fun getFolderId(path: String): Result<String> {
        return try {
            val parts = path.trim('/').split("/")
            var currentFolderId = _rootFolderId
            
            for (folderName in parts) {
                val response = httpClient.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = "www.googleapis.com"
                        path("drive/v3/files")
                        parameter("q", "name='$folderName' and '$currentFolderId' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                        parameter("fields", "files(id)")
                    }
                }
                
                if (!response.status.isSuccess()) {
                    return Result.failure(Exception("Failed to find folder: $folderName"))
                }
                
                val content = response.bodyAsText()
                val files = Json.decodeFromString<GoogleDriveFilesResponse>(content)
                
                if (files.files.isEmpty()) {
                    return Result.failure(Exception("Folder not found: $folderName"))
                }
                
                currentFolderId = files.files.first().id
            }
            
            Result.success(currentFolderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get parent folder ID from path
     */
    private suspend fun getParentFolderId(path: String): Result<String> {
        val parentPath = path.substringBeforeLast("/", "").ifBlank { "/" }
        return getFolderId(parentPath)
    }
    
    /**
     * Get file ID from path
     */
    private suspend fun getFileId(path: String): Result<String> {
        return try {
            val fileName = path.substringAfterLast("/")
            val parentFolderId = getParentFolderId(path).getOrNull()
            
            if (parentFolderId == null) {
                return Result.failure(Exception("Parent folder not found"))
            }
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "www.googleapis.com"
                    path("drive/v3/files")
                    parameter("q", "name='$fileName' and '$parentFolderId' in parents and trashed=false")
                    parameter("fields", "files(id)")
                }
            }
            
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Failed to find file: $fileName"))
            }
            
            val content = response.bodyAsText()
            val files = Json.decodeFromString<GoogleDriveFilesResponse>(content)
            
            if (files.files.isEmpty()) {
                return Result.failure(Exception("File not found: $fileName"))
            }
            
            Result.success(files.files.first().id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse Google Drive files response
     */
    private fun parseGoogleDriveFiles(content: String, parentPath: String): List<NetworkDocument> {
        return try {
            val response = Json.decodeFromString<GoogleDriveFilesResponse>(content)
            response.files.map { file ->
                NetworkDocument(
                    id = file.id,
                    name = file.name,
                    path = "$parentPath/${file.name}".removePrefix("/"),
                    type = if (file.mimeType == "application/vnd.google-apps.folder") DocumentType.FOLDER else DocumentType.FILE,
                    size = file.size?.toLongOrNull() ?: 0L,
                    lastModified = kotlinx.datetime.Instant.parse(file.modifiedTime),
                    permissions = DocumentPermission(
                        canRead = file.permissions.any { it.role == "reader" || it.role == "owner" || it.role == "writer" },
                        canWrite = file.permissions.any { it.role == "writer" || it.role == "owner" },
                        canDelete = file.permissions.any { it.role == "owner" },
                        canExecute = file.mimeType == "application/vnd.google-apps.folder"
                    )
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Parse single Google Drive file
     */
    private fun parseGoogleDriveFile(content: String, path: String): NetworkDocument {
        return try {
            val file = Json.decodeFromString<GoogleDriveFile>(content)
            NetworkDocument(
                id = file.id,
                name = file.name,
                path = path.removePrefix("/"),
                type = if (file.mimeType == "application/vnd.google-apps.folder") DocumentType.FOLDER else DocumentType.FILE,
                size = file.size?.toLongOrNull() ?: 0L,
                lastModified = kotlinx.datetime.Instant.parse(file.modifiedTime),
                permissions = DocumentPermission(
                    canRead = file.permissions.any { it.role == "reader" || it.role == "owner" || it.role == "writer" },
                    canWrite = file.permissions.any { it.role == "writer" || it.role == "owner" },
                    canDelete = file.permissions.any { it.role == "owner" },
                    canExecute = file.mimeType == "application/vnd.google-apps.folder"
                )
            )
        } catch (e: Exception) {
            // Fallback to basic info
            NetworkDocument(
                id = "",
                name = path.substringAfterLast("/"),
                path = path.removePrefix("/"),
                type = DocumentType.FILE,
                size = 0L,
                lastModified = Clock.System.now(),
                permissions = DocumentPermission(
                    canRead = true,
                    canWrite = true,
                    canDelete = true,
                    canExecute = false
                )
            )
        }
    }
    
    @kotlinx.serialization.Serializable
    private data class OAuthTokenResponse(
        val access_token: String,
        val token_type: String,
        val expires_in: Int
    )
    
    @kotlinx.serialization.Serializable
    private data class GoogleDriveFilesResponse(
        val files: List<GoogleDriveFile>
    )
    
    @kotlinx.serialization.Serializable
    private data class GoogleDriveFile(
        val id: String,
        val name: String,
        val mimeType: String,
        val size: String? = null,
        val modifiedTime: String,
        val permissions: List<GoogleDrivePermission>
    )
    
    @kotlinx.serialization.Serializable
    private data class GoogleDrivePermission(
        val id: String,
        val type: String,
        val role: String
    )
}