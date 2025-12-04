package digital.vasic.yole.network.protocols.onedrive

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
 * OneDrive implementation of NetworkStorageService
 * Provides Microsoft OneDrive API integration with OAuth2 authentication
 */
class OneDriveService(
    private val config: StorageConfig.OneDriveConfig
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
            id = "onedrive_${config.name}",
            name = config.name,
            type = StorageType.ONEDRIVE,
            location = "onedrive://",
            isOnline = _isConnected,
            lastSync = Clock.System.now()
        )
    }
    
    override suspend fun connect(): Result<Unit> = try {
        // Test OneDrive API connection by getting drive info
        val response = httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "graph.microsoft.com"
                path("v1.0/drive")
            }
        }
        
        if (response.status.isSuccess()) {
            _isConnected = true
            Result.success(Unit)
        } else {
            Result.failure(NetworkStorageException.ConnectionError.Failed(
                message = "OneDrive connection failed: ${response.status}",
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.ConnectionError.Failed(
            message = "OneDrive connection failed",
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
                message = "OneDrive not connected"
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
                    host = "graph.microsoft.com"
                    path("v1.0/drive/items/$folderId/children")
                    parameter("select", "id,name,folder,size,lastModifiedDateTime,permissions")
                    parameter("top", "1000")
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
            val files = parseOneDriveFiles(content, path)
            
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
                message = "OneDrive not connected"
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
        
        // For small files (<4MB), use simple upload
        if (fileBytes.size < 4 * 1024 * 1024) {
            val response = httpClient.put {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0/drive/items/$parentFolderId:/$fileName:/content")
                }
                setBody(fileBytes)
            }
            
            progressCallback?.invoke(1f)
            
            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val document = parseOneDriveFile(content, remotePath)
                Result.success(document)
            } else {
                Result.failure(NetworkStorageException.FileOperationError.UploadFailed(
                    path = remotePath,
                    cause = Exception(response.status.toString())
                ))
            }
        } else {
            // For large files, use resumable upload
            val document = uploadLargeFile(fileBytes, parentFolderId, fileName, remotePath, progressCallback)
            document
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
                message = "OneDrive not connected"
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
                host = "graph.microsoft.com"
                path("v1.0/drive/items/$fileId/content")
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
                message = "OneDrive not connected"
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
                host = "graph.microsoft.com"
                path("v1.0/drive/items/$fileId")
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
                message = "OneDrive not connected"
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
        
        val requestBody = """
        {
            "name": "$folderName",
            "folder": {},
            "@microsoft.graph.conflictBehavior": "fail"
        }
        """.trimIndent()
        
        val response = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "graph.microsoft.com"
                path("v1.0/drive/items/$parentFolderId/children")
            }
            setBody(requestBody)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val document = parseOneDriveFile(content, path)
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
                message = "OneDrive not connected"
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
        
        val requestBody = """
        {
            "name": "$fileName",
            "parentReference": {
                "id": "$targetFolderId"
            }
        }
        """.trimIndent()
        
        val response = httpClient.patch {
            url {
                protocol = URLProtocol.HTTPS
                host = "graph.microsoft.com"
                path("v1.0/drive/items/$fileId")
                parameter("select", "id,name,folder,size,lastModifiedDateTime,permissions")
            }
            setBody(requestBody)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val document = parseOneDriveFile(content, targetPath)
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
                message = "OneDrive not connected"
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
                host = "graph.microsoft.com"
                path("v1.0/drive/items/$fileId")
                parameter("select", "id,name,folder,size,lastModifiedDateTime,permissions")
            }
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val document = parseOneDriveFile(content, path)
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
                message = "OneDrive not connected"
            )))
            return@flow
        }
        
        try {
            val folderId = if (path.isBlank()) _rootFolderId else getFolderId(path).getOrNull()
            
            val searchQuery = if (folderId != null) {
                "name:\"$query\" and parentReference/Id eq '$folderId'"
            } else {
                "name:\"$query\""
            }
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "graph.microsoft.com"
                    path("v1.0/drive/root/search(q='$searchQuery')")
                    parameter("select", "id,name,folder,size,lastModifiedDateTime,permissions")
                    parameter("top", "1000")
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
            val files = parseOneDriveFiles(content, path)
            
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
        val secureStorage = SecureStorageFactory.create().getOrThrow()
        return try {
            // Try to get stored access token
            val accessToken = secureStorage.getToken("onedrive_${config.name}_access")
            if (accessToken.isNotEmpty()) {
                return accessToken
            }
            
            // If no access token, use refresh token to get new one
            val refreshToken = secureStorage.getToken("onedrive_${config.name}")
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
                    host = "login.microsoftonline.com"
                    path("common/oauth2/v2.0/token")
                }
                setBody(
                    io.ktor.http.Parameters.build {
                        append("client_id", config.clientId)
                        append("client_secret", config.clientSecret)
                        append("refresh_token", refreshToken)
                        append("grant_type", "refresh_token")
                        append("scope", "https://graph.microsoft.com/Files.ReadWrite")
                    }
                )
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
            }
            
            if (response.status.isSuccess()) {
                val content = response.bodyAsText()
                val tokenResponse = Json.decodeFromString<OAuthTokenResponse>(content)
                
                // Store new access token
                val secureStorage = SecureStorageFactory.create().getOrThrow()
                secureStorage.storeToken("onedrive_${config.name}_access", tokenResponse.accessToken)
                
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
                        host = "graph.microsoft.com"
                        path("v1.0/drive/items/$currentFolderId/children")
                        parameter("filter", "name eq '$folderName' and folder ne null")
                        parameter("select", "id")
                    }
                }
                
                if (!response.status.isSuccess()) {
                    return Result.failure(Exception("Failed to find folder: $folderName"))
                }
                
                val content = response.bodyAsText()
                val files = Json.decodeFromString<OneDriveFilesResponse>(content)
                
                if (files.value.isEmpty()) {
                    return Result.failure(Exception("Folder not found: $folderName"))
                }
                
                currentFolderId = files.value.first().id
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
                    host = "graph.microsoft.com"
                    path("v1.0/drive/items/$parentFolderId/children")
                    parameter("filter", "name eq '$fileName'")
                    parameter("select", "id")
                }
            }
            
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Failed to find file: $fileName"))
            }
            
            val content = response.bodyAsText()
            val files = Json.decodeFromString<OneDriveFilesResponse>(content)
            
            if (files.value.isEmpty()) {
                return Result.failure(Exception("File not found: $fileName"))
            }
            
            Result.success(files.value.first().id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload large file using resumable upload
     */
    private suspend fun uploadLargeFile(
        fileBytes: ByteArray,
        parentFolderId: String,
        fileName: String,
        remotePath: String,
        progressCallback: ((Float) -> Unit)?
    ): Result<NetworkDocument> = try {
        // Create upload session
        val requestBody = """
        {
            "item": {
                "name": "$fileName",
                "parentReference": {
                    "id": "$parentFolderId"
                }
            },
            "deferCommit": false
        }
        """.trimIndent()
        
        val sessionResponse = httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = "graph.microsoft.com"
                path("v1.0/drive/items/$parentFolderId:/$fileName:/createUploadSession")
            }
            setBody(requestBody)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        
        if (!sessionResponse.status.isSuccess()) {
            return Result.failure(Exception("Failed to create upload session"))
        }
        
        val sessionContent = sessionResponse.bodyAsText()
        val uploadSession = Json.decodeFromString<OneDriveUploadSession>(sessionContent)
        
        // Upload file in chunks (simplified - in reality would handle multiple chunks)
        val response = httpClient.put {
            url(uploadSession.uploadUrl)
            header("Content-Range", "bytes 0-${fileBytes.size - 1}/${fileBytes.size}")
            setBody(fileBytes)
        }
        
        progressCallback?.invoke(1f)
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val document = parseOneDriveFile(content, remotePath)
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
    
    /**
     * Parse OneDrive files response
     */
    private fun parseOneDriveFiles(content: String, parentPath: String): List<NetworkDocument> {
        return try {
            val response = Json.decodeFromString<OneDriveFilesResponse>(content)
            response.value.map { file ->
                NetworkDocument(
                    id = file.id,
                    name = file.name,
                    path = "$parentPath/${file.name}".removePrefix("/"),
                    type = if (file.folder != null) DocumentType.FOLDER else DocumentType.FILE,
                    size = file.size ?: 0L,
                    lastModified = kotlinx.datetime.Instant.parse(file.lastModifiedDateTime),
                    permissions = DocumentPermission(
                        canRead = true, // OneDrive doesn't expose detailed permissions in basic API
                        canWrite = true,
                        canDelete = true,
                        canExecute = file.folder != null
                    )
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Parse single OneDrive file
     */
    private fun parseOneDriveFile(content: String, path: String): NetworkDocument {
        return try {
            val file = Json.decodeFromString<OneDriveFile>(content)
            NetworkDocument(
                id = file.id,
                name = file.name,
                path = path.removePrefix("/"),
                type = if (file.folder != null) DocumentType.FOLDER else DocumentType.FILE,
                size = file.size ?: 0L,
                lastModified = kotlinx.datetime.Instant.parse(file.lastModifiedDateTime),
                permissions = DocumentPermission(
                    canRead = true,
                    canWrite = true,
                    canDelete = true,
                    canExecute = file.folder != null
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
    
    // OneDrive API data classes
    @kotlinx.serialization.Serializable
    private data class OAuthTokenResponse(
        val access_token: String,
        val token_type: String,
        val expires_in: Int
    )
    
    @kotlinx.serialization.Serializable
    private data class OneDriveFilesResponse(
        val value: List<OneDriveFile>
    )
    
    @kotlinx.serialization.Serializable
    private data class OneDriveFile(
        val id: String,
        val name: String,
        val folder: OneDriveFolder? = null,
        val size: Long? = null,
        val lastModifiedDateTime: String
    )
    
    @kotlinx.serialization.Serializable
    private data class OneDriveFolder(
        val childCount: Int
    )
    
    @kotlinx.serialization.Serializable
    private data class OneDriveUploadSession(
        val uploadUrl: String,
        val expirationDateTime: String
    )
}