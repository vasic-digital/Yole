package digital.vasic.yole.network.protocols.ftp

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorageFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * FTP implementation of NetworkStorageService
 * Provides FTP/SFTP file operations with proper error handling
 */
class FtpService(
    private val config: StorageConfig.FtpConfig
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
    private var _rootPath = config.rootPath.ifBlank { "/" }
    
    override val isOnline: Boolean
        get() = _isConnected
    
    override val storageInfo: NetworkStorage
        get() = NetworkStorage(
            id = "ftp_${config.name}",
            name = config.name,
            type = StorageType.FTP,
            location = "ftp://${config.host}:${config.port}${_rootPath}",
            isOnline = _isConnected,
            lastSync = Clock.System.now(),
            supportsFolders = true,
            supportsMetadata = false
        )
    
    override suspend fun connect(): Result<Unit> = try {
        // Test connection by listing root directory
        val response = httpClient.get {
            url {
                protocol = URLProtocol.FTP
                host = config.host
                port = config.port
                path(_rootPath)
            }
        }
        
        if (response.status.isSuccess()) {
            _isConnected = true
            Result.success(Unit)
        } else {
            Result.failure(NetworkStorageException.ConnectionError.Failed(
                message = "FTP connection failed: ${response.status}",
                cause = Exception(response.status.toString())
            ))
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.ConnectionError.Failed(
            message = "FTP connection failed",
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
                message = "FTP not connected"
            )))
            return@flow
        }
        
        try {
            val fullPath = if (path.isBlank()) _rootPath else "$_rootPath$path".removePrefix("/")
            
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.FTP
                    host = config.host
                    port = config.port
                    path(fullPath)
                }
            }
            
            if (!response.status.isSuccess()) {
                emit(Result.failure(NetworkStorageException.FileOperationError.ListFailed(
                    path = path,
                    cause = Exception(response.status.toString())
                )))
                return@flow
            }
            
            // Parse FTP directory listing (simplified)
            val content = response.bodyAsText()
            val lines = content.split("\n").filter { it.isNotBlank() }
            
            lines.forEach { line ->
                try {
                    val document = parseFtpLine(line, path)
                    if (document != null) {
                        emit(Result.success(document))
                    }
                } catch (e: Exception) {
                    emit(Result.failure(NetworkStorageException.FileOperationError.ListFailed(
                        path = path,
                        cause = e
                    )))
                }
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
                message = "FTP not connected"
            ))
        }
        
        val fullPath = "$_rootPath$remotePath".removePrefix("/")
        
        // Upload file
        val response = httpClient.put {
            url {
                protocol = URLProtocol.FTP
                host = config.host
                port = config.port
                path(fullPath)
            }
            setBody(kotlin.io.readBytes(localPath))
        }
        
        if (response.status.isSuccess()) {
            Result.success(NetworkDocument(
                id = remotePath,
                name = remotePath.substringAfterLast("/"),
                path = remotePath,
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
                message = "FTP not connected"
            ))
        }
        
        val fullPath = "$_rootPath$remotePath".removePrefix("/")
        
        val response = httpClient.get {
            url {
                protocol = URLProtocol.FTP
                host = config.host
                port = config.port
                path(fullPath)
            }
        }
        
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
                message = "FTP not connected"
            ))
        }
        
        val fullPath = "$_rootPath$path".removePrefix("/")
        
        val response = httpClient.delete {
            url {
                protocol = URLProtocol.FTP
                host = config.host
                port = config.port
                path(fullPath)
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
                message = "FTP not connected"
            ))
        }
        
        val fullPath = "$_rootPath$path".removePrefix("/")
        
        // Create directory
        val response = HttpClient(CIO).use { client ->
            client.request {
                method = HttpMethod("MKD")
                url {
                    protocol = URLProtocol.FTP
                    host = config.host
                    port = config.port
                    path(fullPath)
                }
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
                message = "FTP not connected"
            ))
        }
        
        val fullSourcePath = "$_rootPath$sourcePath".removePrefix("/")
        val fullTargetPath = "$_rootPath$targetPath".removePrefix("/")
        
        // FTP uses RNFR/RNTO commands for rename/move
        val renameResponse = HttpClient(CIO).use { client ->
            client.request {
                method = HttpMethod("RNFR")
                url {
                    protocol = URLProtocol.FTP
                    host = config.host
                    port = config.port
                    path(fullSourcePath)
                }
            }
        }
        
        if (renameResponse.status.isSuccess()) {
            val toResponse = HttpClient(CIO).use { client ->
                client.request {
                    method = HttpMethod("RNTO")
                    url {
                        protocol = URLProtocol.FTP
                        host = config.host
                        port = config.port
                        path(fullTargetPath)
                    }
                }
            }
            
            if (toResponse.status.isSuccess()) {
                Result.success(NetworkDocument(
                    id = targetPath,
                    name = targetPath.substringAfterLast("/"),
                    path = targetPath,
                    type = DocumentType.FILE,
                    size = 0L,
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
                    cause = Exception(toResponse.status.toString())
                ))
            }
        } else {
            Result.failure(NetworkStorageException.FileOperationError.MoveFailed(
                sourcePath = sourcePath,
                targetPath = targetPath,
                cause = Exception(renameResponse.status.toString())
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
                message = "FTP not connected"
            ))
        }
        
        val fullPath = "$_rootPath$path".removePrefix("/")
        
        // Use SIZE command for files, LIST for directories
        val response = HttpClient(CIO).use { client ->
            client.request {
                method = HttpMethod("SIZE")
                url {
                    protocol = URLProtocol.FTP
                    host = config.host
                    port = config.port
                    path(fullPath)
                }
            }
        }
        
        // Parse response to determine if it's a file or directory
        val isFile = response.status.isSuccess()
        
        NetworkDocument(
            id = path,
            name = path.substringAfterLast("/"),
            path = path,
            type = if (isFile) DocumentType.FILE else DocumentType.FOLDER,
            size = if (isFile) {
                try {
                    response.bodyAsText().toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    0L
                }
            } else 0L,
            lastModified = Clock.System.now(), // FTP doesn't always provide modification time
            permissions = DocumentPermission(
                canRead = true,
                canWrite = config.username.isNotBlank(),
                canDelete = true,
                canExecute = !isFile
            )
        ).let { Result.success(it) }
        
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
        // FTP doesn't support server-side search, so we need to list all files and filter
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
     * Parse FTP directory listing line
     * This is a simplified parser - in reality, FTP listings can vary greatly between servers
     */
    private fun parseFtpLine(line: String, parentPath: String): NetworkDocument? {
        val parts = line.trim().split("\\s+".toRegex())
        if (parts.size < 9) return null
        
        try {
            val permissions = parts[0]
            val size = parts[4].toLongOrNull() ?: 0L
            val name = parts.drop(8).joinToString(" ")
            
            val isDirectory = permissions.startsWith("d")
            
            return NetworkDocument(
                id = "$parentPath/$name",
                name = name,
                path = "$parentPath/$name",
                type = if (isDirectory) DocumentType.FOLDER else DocumentType.FILE,
                size = size,
                lastModified = Clock.System.now(), // Would need to parse actual date
                permissions = DocumentPermission(
                    canRead = permissions.contains("r"),
                    canWrite = permissions.contains("w"),
                    canDelete = permissions.contains("w"),
                    canExecute = permissions.contains("x")
                )
            )
        } catch (e: Exception) {
            // If parsing fails, return null
            return null
        }
    }
}