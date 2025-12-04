package digital.vasic.yole.network.protocols.smb

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

/**
 * SMB/CIFS implementation of NetworkStorageService
 * Provides SMB file operations with proper authentication
 */
class SmbService(
    private val config: StorageConfig.SmbConfig
) : NetworkStorageService {
    
    private val httpClient = HttpClient(CIO) {
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(
                        username = config.username,
                        password = config.password
                    )
                }
            }
        }
    }
    
    private var _isConnected = false
    private var _rootPath = if (config.path.isBlank()) "/" else config.path
    private var _share = config.share
    
    override val isOnline: Boolean
        get() = _isConnected
    
    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "smb_${config.name}",
            name = config.name,
            type = StorageType.SMB,
            location = "smb://${config.host}/${_share}${_rootPath}",
            isOnline = _isConnected,
            lastSync = Clock.System.now()
        )
    }
    
    override suspend fun connect(): Result<Unit> = try {
        // Test SMB connection by attempting to list share
        val response = httpClient.get {
            url {
                protocol = if (config.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
                host = config.host
                port = config.port
                path("api/smb/connect")
                parameter("share", _share)
                parameter("path", _rootPath)
            }
        }
        
        if (response.status.isSuccess()) {
            _isConnected = true
            Result.success(Unit)
        } else {
            Result.failure(NetworkStorageException.ConnectionError.Failed(
                message = "SMB connection failed: ${response.status}",
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.ConnectionError.Failed(
            message = "SMB connection failed",
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
                message = "SMB not connected"
            )))
            return@flow
        }
        
        try {
            val fullPath = if (path.isBlank()) _rootPath else "$_rootPath$path".removePrefix("/")
            
            val response = httpClient.get {
                url {
                    protocol = if (config.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
                    host = config.host
                    port = config.port
                    path("api/smb/list")
                    parameter("share", _share)
                    parameter("path", fullPath)
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
            parseSmbListing(content, path).forEach { document ->
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
                message = "SMB not connected"
            ))
        }
        
        val fullPath = "$_rootPath$remotePath".removePrefix("/")
        val fileBytes = kotlin.io.readBytes(localPath)
        
        progressCallback?.invoke(0f)
        
        val response = httpClient.put {
            url {
                protocol = if (config.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
                host = config.host
                port = config.port
                path("api/smb/upload")
                parameter("share", _share)
                parameter("path", fullPath)
            }
            setBody(fileBytes)
        }
        
        progressCallback?.invoke(1f)
        
        if (response.status.isSuccess()) {
            Result.success(NetworkDocument(
                id = remotePath,
                name = remotePath.substringAfterLast("/"),
                path = remotePath,
                type = DocumentType.FILE,
                size = fileBytes.size.toLong(),
                lastModified = Clock.System.now(),
                permissions = DocumentPermission(
                    canRead = true,
                    canWrite = true,
                    canDelete = true,
                    canExecute = false
                )
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
                message = "SMB not connected"
            ))
        }
        
        val fullPath = "$_rootPath$remotePath".removePrefix("/")
        
        progressCallback?.invoke(0f)
        
        val response = httpClient.get {
            url {
                protocol = if (config.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
                host = config.host
                port = config.port
                path("api/smb/download")
                parameter("share", _share)
                parameter("path", fullPath)
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
                message = "SMB not connected"
            ))
        }
        
        val fullPath = "$_rootPath$path".removePrefix("/")
        
        val response = httpClient.delete {
            url {
                protocol = if (config.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
                host = config.host
                port = config.port
                path("api/smb/delete")
                parameter("share", _share)
                parameter("path", fullPath)
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
                message = "SMB not connected"
            ))
        }
        
        val fullPath = "$_rootPath$path".removePrefix("/")
        
        val response = httpClient.post {
            url {
                protocol = if (config.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
                host = config.host
                port = config.port
                path("api/smb/mkdir")
                parameter("share", _share)
                parameter("path", fullPath)
            }
        }
        
        if (response.status.isSuccess()) {
            Result.success(NetworkDocument(
                id = path,
                name = path.substringAfterLast("/"),
                path = path,
                type = DocumentType.FOLDER,
                size = 0L,
                lastModified = Clock.System.now(),
                permissions = DocumentPermission(
                    canRead = true,
                    canWrite = true,
                    canDelete = true,
                    canExecute = true
                )
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
                message = "SMB not connected"
            ))
        }
        
        val fullSourcePath = "$_rootPath$sourcePath".removePrefix("/")
        val fullTargetPath = "$_rootPath$targetPath".removePrefix("/")
        
        val response = httpClient.put {
            url {
                protocol = if (config.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
                host = config.host
                port = config.port
                path("api/smb/move")
                parameter("share", _share)
                parameter("source", fullSourcePath)
                parameter("target", fullTargetPath)
            }
        }
        
        if (response.status.isSuccess()) {
            Result.success(NetworkDocument(
                id = targetPath,
                name = targetPath.substringAfterLast("/"),
                path = targetPath,
                type = DocumentType.FILE,
                size = 0L, // Would need to get actual file size
                lastModified = Clock.System.now(),
                permissions = DocumentPermission(
                    canRead = true,
                    canWrite = true,
                    canDelete = true,
                    canExecute = false
                )
            ))
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
                message = "SMB not connected"
            ))
        }
        
        val fullPath = "$_rootPath$path".removePrefix("/")
        
        val response = httpClient.get {
            url {
                protocol = if (config.useSsl) URLProtocol.HTTPS else URLProtocol.HTTP
                host = config.host
                port = config.port
                path("api/smb/info")
                parameter("share", _share)
                parameter("path", fullPath)
            }
        }
        
        if (response.status.isSuccess()) {
            val content = response.bodyAsText()
            val info = parseFileInfo(content, path)
            Result.success(info)
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
        // SMB may support server-side search depending on the server
        // For now, implement client-side search
        listFiles(path).collect { result ->
            if (result.isSuccess) {
                val document = result.getOrThrow()
                if (document.name.contains(query, ignoreCase = true)) {
                    emit(Result.success(document))
                }
                
                // Recursively search in subdirectories if requested
                if (recursive && document.type == DocumentType.FOLDER) {
                    search(query, document.path, true).collect { subResult ->
                        emit(subResult)
                    }
                }
            } else {
                emit(result)
            }
        }
    }
    
    /**
     * Parse SMB directory listing response
     * SMB uses different listing formats depending on the server
     */
    private fun parseSmbListing(content: String, parentPath: String): List<NetworkDocument> {
        return try {
            // Try to parse as JSON first
            kotlinx.serialization.json.Json.decodeFromString<List<SmbFileInfo>>(content)
                .map { info ->
                    NetworkDocument(
                        id = "$parentPath/${info.name}",
                        name = info.name,
                        path = "$parentPath/${info.name}",
                        type = if (info.isDirectory) DocumentType.FOLDER else DocumentType.FILE,
                        size = info.size,
                        lastModified = info.lastModified,
                        permissions = DocumentPermission(
                            canRead = info.canRead,
                            canWrite = info.canWrite,
                            canDelete = info.canDelete,
                            canExecute = info.canExecute
                        )
                    )
                }
        } catch (e: Exception) {
            // Fallback to parsing as directory listing
            content.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
                parseDirectoryLine(line, parentPath)
            }
        }
    }
    
    /**
     * Parse a single line from SMB directory listing
     */
    private fun parseDirectoryLine(line: String, parentPath: String): NetworkDocument? {
        // SMB can return different formats, this is a generic parser
        try {
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 6) return null
            
            // Try to detect if it's a directory by common patterns
            val isDirectory = line.contains("<DIR>") || parts[0].contains("d")
            val name = if (line.contains("<DIR>")) {
                parts.dropWhile { it != "<DIR>" }.drop(1).joinToString(" ")
            } else {
                parts.drop(3).joinToString(" ")
            }
            
            if (name.isBlank() || name == "." || name == "..") return null
            
            val size = if (isDirectory) 0L else {
                try {
                    // Find numeric size in the line
                    line.split("\\s+".toRegex()).find { it.toLongOrNull() != null }?.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
            
            NetworkDocument(
                id = "$parentPath/$name",
                name = name.trim(),
                path = "$parentPath/$name",
                type = if (isDirectory) DocumentType.FOLDER else DocumentType.FILE,
                size = size,
                lastModified = Clock.System.now(),
                permissions = DocumentPermission(
                    canRead = true,
                    canWrite = true,
                    canDelete = true,
                    canExecute = isDirectory
                )
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse file info response
     */
    private fun parseFileInfo(content: String, path: String): NetworkDocument {
        return try {
            // Try to parse as JSON
            val info = kotlinx.serialization.json.Json.decodeFromString<SmbFileInfo>(content)
            NetworkDocument(
                id = path,
                name = path.substringAfterLast("/"),
                path = path,
                type = if (info.isDirectory) DocumentType.FOLDER else DocumentType.FILE,
                size = info.size,
                lastModified = info.lastModified,
                permissions = DocumentPermission(
                    canRead = info.canRead,
                    canWrite = info.canWrite,
                    canDelete = info.canDelete,
                    canExecute = info.canExecute
                )
            )
        } catch (e: Exception) {
            // Fallback to basic info
            NetworkDocument(
                id = path,
                name = path.substringAfterLast("/"),
                path = path,
                type = DocumentType.FILE, // Default to file
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
    
    /**
     * Data class for SMB file info
     */
    @kotlinx.serialization.Serializable
    private data class SmbFileInfo(
        val name: String,
        val size: Long,
        val lastModified: kotlinx.datetime.Instant,
        val isDirectory: Boolean,
        val canRead: Boolean,
        val canWrite: Boolean,
        val canDelete: Boolean,
        val canExecute: Boolean
    )
}