package digital.vasic.yole.network.protocols.ftp

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.platform.SecureStorageFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import digital.vasic.yole.network.protocol.createHttpClient

/**
 * FTP implementation of [NetworkStorageService].
 *
 * ## Implementation Status
 *
 * **STUBBED** -- No actual FTP network I/O is performed. There is no pure Kotlin
 * Multiplatform FTP client library, so this service maintains an **in-memory virtual
 * file system** that simulates FTP protocol semantics (LIST, RETR, STOR, MKD,
 * RNFR/RNTO, DELE, SIZE, MDTM). All file operations mutate only the in-memory
 * state; no bytes are transferred over the network.
 *
 * ### What works (in-memory only, no real I/O):
 * - [connect] / [disconnect] -- validates config, manages connection flag
 * - [testConnection] -- validates host and port configuration
 * - [listFiles] -- returns entries from the virtual file system
 * - [downloadFile] / [uploadFile] -- simulate chunked transfers with progress
 * - [deleteFile], [createFolder], [renameFile], [moveFile] -- mutate virtual FS
 * - [getFileInfo], [exists] -- query virtual FS or return synthesized documents
 * - [copyFile] -- returns failure (FTP protocol has no copy command)
 * - Cache and sync status tracking (in-memory maps)
 *
 * ### What is NOT implemented (requires a real FTP client):
 * - Actual TCP socket connection to an FTP server
 * - FTP control/data channel communication
 * - Real file content transfer (RETR/STOR)
 * - Server-side directory listing (LIST)
 * - Quota information (no standard FTP command)
 * - Search (no FTP search command in RFC 959)
 *
 * Resource Management: This class manages a lazily-initialized [HttpClient] that
 * must be properly closed. Call [disconnect] when done using this service.
 */
class FtpService(
    override val config: StorageConfig.FtpConfig
) : NetworkStorageService {

    // Lazy initialization of HttpClient to avoid resource allocation if never used
    private val httpClient by lazy {
        httpClientInitialized = true
        createHttpClient()
    }

    // Track whether httpClient has been initialized to avoid closing uninitialized client
    private var httpClientInitialized = false

    private var _isConnected = false
    private var _rootPath = config.rootPath.ifBlank { "/" }
    private val activeOperations = mutableMapOf<Long, NetworkOperation>()
    private val operationsMutex = Mutex()

    // In-memory cache storage protected by mutex
    private val cacheEntries = mutableMapOf<String, CacheEntry>()
    private val cacheMutex = Mutex()

    // In-memory sync status tracking protected by mutex
    private val syncStatusMap = mutableMapOf<String, SyncStatus>()
    private val syncMutex = Mutex()

    // Virtual file system state for tracking created/moved/deleted files
    private val virtualFileSystem = mutableMapOf<String, NetworkDocument>()
    private val vfsMutex = Mutex()

    // Operation ID counter for unique IDs
    private var operationCounter = 0L
    private val counterMutex = Mutex()

    override val isOnline: Boolean
        get() = _isConnected

    override val rootPath: String
        get() = "/"

    /**
     * Generate a unique operation ID using an atomic counter combined with timestamp
     */
    private suspend fun nextOperationId(): Long {
        return counterMutex.withLock {
            operationCounter++
            Clock.System.now().toEpochMilliseconds() + operationCounter
        }
    }

    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "ftp_${config.name}",
            name = config.name,
            type = StorageType.FTP,
            location = "ftp://${config.host}:${config.port}${_rootPath}",
            isOnline = _isConnected,
            lastSync = Clock.System.now(),
            supportsFolders = false, // Basic FTP doesn't have reliable folder support
            supportsMetadata = false
        )
    }

    override suspend fun connect(): Result<Unit> {
        return try {
            // Test FTP connection by validating configuration and attempting handshake
            val connectionTest = testFtpConnection()
            if (connectionTest.isSuccess) {
                _isConnected = true
                // Initialize the virtual file system with default directory listing
                initializeVirtualFileSystem()
                Result.success(Unit)
            } else {
                Result.failure(NetworkStorageException.ConnectionException.Failed(
                    message = "FTP connection failed",
                    cause = connectionTest.exceptionOrNull()
                ))
            }
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.ConnectionException.Failed(
                message = "FTP connection failed",
                cause = e
            ))
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return try {
            // Send FTP QUIT command equivalent - clean up resources
            // Only close httpClient if it was actually initialized
            if (httpClientInitialized) {
                try {
                    httpClient.close()
                } catch (closeException: Exception) {
                    // Log but don't fail disconnect for close errors
                }
            }
            _isConnected = false
            // Clear active operations on disconnect
            operationsMutex.withLock {
                activeOperations.clear()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            _isConnected = false // Ensure we mark as disconnected even on error
            Result.failure(NetworkStorageException.fromThrowable(e, "disconnect"))
        }
    }

    override suspend fun testConnection(): Result<Boolean> {
        return testFtpConnection().map { true }
    }

    /**
     * Test FTP connection by validating host/port configuration.
     *
     * TODO("Not yet implemented: open TCP control connection on FTP port,
     * verify 220 greeting, authenticate with USER/PASS commands")
     *
     * Currently only validates that the host is non-blank and port is in range.
     */
    private suspend fun testFtpConnection(): Result<Unit> {
        return try {
            // Validate host configuration
            if (config.host.isBlank()) {
                return Result.failure(Exception("FTP host cannot be blank"))
            }

            // Validate port range (1-65535)
            if (config.port <= 0 || config.port > 65535) {
                return Result.failure(Exception("Invalid FTP port: ${config.port}"))
            }

            // TODO("Not yet implemented: FTP handshake sequence")
            // Requires: TCP connect -> 220 greeting -> USER/PASS -> PASV -> TYPE I
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.ConnectionException.Failed(
                message = "FTP connection test failed",
                cause = e
            ))
        }
    }

    /**
     * Initialize the virtual file system with a default directory structure.
     * This represents what the FTP LIST command would return for the root directory.
     */
    private suspend fun initializeVirtualFileSystem() {
        vfsMutex.withLock {
            virtualFileSystem.clear()
            // Seed with default files that would appear from a LIST command
            val basePath = _rootPath
            virtualFileSystem["$basePath/file1.txt"] = NetworkDocument(
                id = "file1.txt",
                name = "file1.txt",
                path = "$basePath/file1.txt",
                isFolder = false,
                size = 1024L,
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                storageId = "ftp"
            )
            virtualFileSystem["$basePath/file2.md"] = NetworkDocument(
                id = "file2.md",
                name = "file2.md",
                path = "$basePath/file2.md",
                isFolder = false,
                size = 2048L,
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                storageId = "ftp"
            )
            virtualFileSystem["$basePath/folder1"] = NetworkDocument(
                id = "folder1",
                name = "folder1",
                path = "$basePath/folder1",
                isFolder = true,
                size = 0L,
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                storageId = "ftp"
            )
        }
    }

    /**
     * List files in the given path.
     *
     * **Stubbed**: Returns entries from the in-memory virtual file system.
     * TODO("Not yet implemented: send FTP LIST command and parse Unix/DOS-style
     * directory listing response from the server")
     */
    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        if (!_isConnected) {
            emit(Result.failure(NetworkStorageException.ConnectionException.NotConnected(
                message = "FTP not connected"
            )))
            return@flow
        }

        val fullPath = normalizePath(path)

        // Retrieve files from virtual file system that are direct children of fullPath
        val files = vfsMutex.withLock {
            virtualFileSystem.values.filter { doc ->
                val docParent = doc.path.substringBeforeLast("/", "")
                val normalizedParent = if (docParent.isEmpty()) "/" else docParent
                normalizedParent == fullPath || (fullPath == "/" && docParent.isEmpty())
            }.toList()
        }

        // If no VFS entries exist for this path, return the default listing
        // This handles the case where the path is a subdirectory not yet populated
        val result = if (files.isEmpty() && fullPath == _rootPath) {
            // Fallback: return default FTP directory listing (LIST command response)
            listOf(
                NetworkDocument(
                    id = "file1.txt",
                    name = "file1.txt",
                    path = "$fullPath/file1.txt",
                    isFolder = false,
                    size = 1024L,
                    lastModified = Clock.System.now(),
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                    storageId = "ftp"
                ),
                NetworkDocument(
                    id = "file2.md",
                    name = "file2.md",
                    path = "$fullPath/file2.md",
                    isFolder = false,
                    size = 2048L,
                    lastModified = Clock.System.now(),
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                    storageId = "ftp"
                ),
                NetworkDocument(
                    id = "folder1",
                    name = "folder1",
                    path = "$fullPath/folder1",
                    isFolder = true,
                    size = 0L,
                    lastModified = Clock.System.now(),
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                    storageId = "ftp"
                )
            )
        } else {
            files
        }

        // Update sync status for listed files
        syncMutex.withLock {
            result.forEach { doc ->
                syncStatusMap[doc.path] = SyncStatus.SYNCED
            }
        }

        emit(Result.success(result))
    }.catch { e ->
        emit(Result.failure(NetworkStorageException.FileOperationException.ListFailed(
            path = path,
            cause = e
        )))
    }

    /**
     * Simulate downloading a file using the FTP RETR command.
     *
     * **Stubbed**: Simulates chunked transfer progress using virtual file system sizes.
     * No actual data connection is opened and no bytes are transferred.
     * TODO("Not yet implemented: open data connection, send RETR, read file bytes")
     */
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = nextOperationId()

        if (!_isConnected) {
            emit(createFailedOperation(operationId, NetworkOperation.Type.DOWNLOAD, remotePath, localPath, "FTP not connected"))
            return@flow
        }

        val fullPath = normalizePath(remotePath)

        try {
            // Start operation - FTP RETR command initiation
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

            // Step 1: Send SIZE command to get file size
            val fileSize = vfsMutex.withLock {
                virtualFileSystem[fullPath]?.size ?: 2048L
            }

            // Step 2: If passive mode, send PASV and parse response for data port
            // If active mode, send PORT with our listening address
            updateActiveOperation(operationId, initialOperation.copy(
                progress = 0.1,
                totalSize = fileSize,
                metadata = mapOf("command" to "PASV", "mode" to if (config.passiveMode) "passive" else "active")
            ))
            emit(initialOperation.copy(progress = 0.1, totalSize = fileSize))

            // Step 3: Send RETR command
            emit(initialOperation.copy(progress = 0.3, totalSize = fileSize))

            // Step 4: Read file data in chunks through data connection
            val chunkSize = 8192L
            var bytesTransferred = 0L

            while (bytesTransferred < fileSize) {
                // Simulate reading a chunk from the FTP data connection
                val remaining = fileSize - bytesTransferred
                val currentChunk = if (remaining < chunkSize) remaining else chunkSize
                bytesTransferred += currentChunk

                val progress = bytesTransferred.toDouble() / fileSize.toDouble()
                val currentOp = initialOperation.copy(
                    progress = progress,
                    totalSize = fileSize,
                    bytesTransferred = bytesTransferred
                )
                updateActiveOperation(operationId, currentOp)
                emit(currentOp)
            }

            // Step 5: Verify transfer complete (226 response)
            val completedOperation = initialOperation.copy(
                status = NetworkOperation.Status.COMPLETED,
                progress = 1.0,
                totalSize = fileSize,
                bytesTransferred = fileSize,
                completedAt = Clock.System.now()
            )

            // Update sync status for the downloaded file
            syncMutex.withLock {
                syncStatusMap[fullPath] = SyncStatus.SYNCED
            }

            removeActiveOperation(operationId)
            emit(completedOperation)

        } catch (e: Exception) {
            val errorOperation = NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.DOWNLOAD,
                status = NetworkOperation.Status.FAILED,
                remotePath = remotePath,
                localPath = localPath,
                error = e.message ?: "FTP download failed",
                createdAt = Clock.System.now(),
                startedAt = Clock.System.now(),
                completedAt = Clock.System.now()
            )

            removeActiveOperation(operationId)
            emit(errorOperation)
        }
    }

    /**
     * Simulate uploading a file using the FTP STOR command.
     *
     * **Stubbed**: Simulates chunked transfer progress and registers the file in
     * the virtual file system. No actual data connection is opened, no bytes are
     * read from disk, and no bytes are transferred to the server.
     * TODO("Not yet implemented: read local file, open data connection, send STOR,
     * write file bytes")
     */
    override suspend fun uploadFile(
        localPath: String,
        remotePath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = nextOperationId()

        if (!_isConnected) {
            emit(createFailedOperation(operationId, NetworkOperation.Type.UPLOAD, remotePath, localPath, "FTP not connected"))
            return@flow
        }

        val fullPath = normalizePath(remotePath)

        try {
            // Start operation - FTP STOR command initiation
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

            // Step 1: Determine file size from local file
            // TODO("Not yet implemented: read actual file size from localPath")
            val fileSize = 1024L

            // Step 2: Send TYPE I (binary mode) if not already set
            emit(initialOperation.copy(progress = 0.1, totalSize = fileSize))

            // Step 3: If passive mode, send PASV and parse data port
            emit(initialOperation.copy(progress = 0.2, totalSize = fileSize))

            // Step 4: Send STOR command with remote filename
            emit(initialOperation.copy(progress = 0.3, totalSize = fileSize))

            // Step 5: Write file data in chunks through data connection
            val chunkSize = 4096L
            var bytesTransferred = 0L

            while (bytesTransferred < fileSize) {
                val remaining = fileSize - bytesTransferred
                val currentChunk = if (remaining < chunkSize) remaining else chunkSize
                bytesTransferred += currentChunk

                val progress = 0.3 + (bytesTransferred.toDouble() / fileSize.toDouble()) * 0.6
                val currentOp = initialOperation.copy(
                    progress = progress,
                    totalSize = fileSize,
                    bytesTransferred = bytesTransferred
                )
                updateActiveOperation(operationId, currentOp)
                emit(currentOp)
            }

            // Step 6: Close data connection and wait for 226 response
            emit(initialOperation.copy(progress = 0.95, totalSize = fileSize, bytesTransferred = fileSize))

            // Step 7: Register the uploaded file in the virtual file system
            val fileName = remotePath.substringAfterLast("/")
            vfsMutex.withLock {
                virtualFileSystem[fullPath] = NetworkDocument(
                    id = fileName,
                    name = fileName,
                    path = fullPath,
                    isFolder = false,
                    size = fileSize,
                    lastModified = Clock.System.now(),
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                    storageId = "ftp"
                )
            }

            // Update sync status
            syncMutex.withLock {
                syncStatusMap[fullPath] = SyncStatus.SYNCED
            }

            val completedOperation = initialOperation.copy(
                status = NetworkOperation.Status.COMPLETED,
                progress = 1.0,
                totalSize = fileSize,
                bytesTransferred = fileSize,
                completedAt = Clock.System.now()
            )

            removeActiveOperation(operationId)
            emit(completedOperation)

        } catch (e: Exception) {
            val errorOperation = NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.UPLOAD,
                status = NetworkOperation.Status.FAILED,
                remotePath = remotePath,
                localPath = localPath,
                error = e.message ?: "FTP upload failed",
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

    private suspend fun updateActiveOperation(operationId: Long, operation: NetworkOperation) {
        operationsMutex.withLock {
            if (activeOperations.containsKey(operationId)) {
                activeOperations[operationId] = operation
            }
        }
    }

    private suspend fun removeActiveOperation(operationId: Long) {
        operationsMutex.withLock {
            activeOperations.remove(operationId)
        }
    }

    /**
     * Delete a file using the FTP DELE command, or a directory using RMD.
     */
    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        return try {
            if (!_isConnected) {
                return Result.failure(NetworkStorageException.ConnectionException.NotConnected(
                    message = "FTP not connected"
                ))
            }

            val fullPath = normalizePath(remotePath)

            // TODO("Not yet implemented: send DELE/RMD command to FTP server")
            // Would send "DELE $fullPath\r\n" and check for 250 response
            vfsMutex.withLock {
                virtualFileSystem.remove(fullPath)
            }

            // Clean up sync status and cache for deleted file
            syncMutex.withLock {
                syncStatusMap.remove(fullPath)
            }
            cacheMutex.withLock {
                cacheEntries.remove(fullPath)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.FileOperationException.DeleteFailed(
                path = remotePath,
                cause = e
            ))
        }
    }

    /**
     * Create a folder using the FTP MKD (Make Directory) command.
     * Note: Basic FTP doesn't have reliable folder creation support across all servers,
     * but most modern FTP servers support MKD.
     */
    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> {
        return try {
            if (!_isConnected) {
                return Result.failure(NetworkStorageException.ConnectionException.NotConnected(
                    message = "FTP not connected"
                ))
            }

            val fullPath = normalizePath(remotePath)
            val folderName = fullPath.substringAfterLast("/")

            // Send MKD command: "MKD $fullPath\r\n"
            // Expect 257 response: "257 "/$fullPath" directory created"
            val folderDoc = NetworkDocument(
                id = fullPath,
                name = folderName,
                path = fullPath,
                isFolder = true,
                size = 0L,
                lastModified = Clock.System.now(),
                storageId = "ftp",
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE)
            )

            // Register the new folder in the virtual file system
            vfsMutex.withLock {
                virtualFileSystem[fullPath] = folderDoc
            }

            // Update sync status
            syncMutex.withLock {
                syncStatusMap[fullPath] = SyncStatus.SYNCED
            }

            Result.success(folderDoc)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.FileOperationException.CreateFolderFailed(
                path = remotePath,
                cause = e
            ))
        }
    }

    /**
     * Rename a file or directory using FTP RNFR (Rename From) and RNTO (Rename To) commands.
     * This is a two-step operation:
     * 1. RNFR old-path (expect 350 response)
     * 2. RNTO new-path (expect 250 response)
     */
    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        return try {
            if (!_isConnected) {
                return Result.failure(NetworkStorageException.ConnectionException.NotConnected(
                    message = "FTP not connected"
                ))
            }

            val fullPath = normalizePath(remotePath)
            val parentDir = fullPath.substringBeforeLast("/", "")
            val newFullPath = if (parentDir.isEmpty()) "/$newName" else "$parentDir/$newName"

            // Step 1: Send RNFR (Rename From) command
            // "RNFR $fullPath\r\n" -> expect 350 "File exists, ready for destination name"

            // Step 2: Send RNTO (Rename To) command
            // "RNTO $newFullPath\r\n" -> expect 250 "File successfully renamed or moved"

            // Update virtual file system
            vfsMutex.withLock {
                val existing = virtualFileSystem.remove(fullPath)
                if (existing != null) {
                    virtualFileSystem[newFullPath] = existing.copy(
                        id = newName,
                        name = newName,
                        path = newFullPath,
                        lastModified = Clock.System.now()
                    )
                }
            }

            // Update sync status
            syncMutex.withLock {
                val status = syncStatusMap.remove(fullPath) ?: SyncStatus.SYNCED
                syncStatusMap[newFullPath] = status
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.fromThrowable(e, "renameFile"))
        }
    }

    /**
     * Move a file to a new location using FTP RNFR/RNTO commands.
     * FTP doesn't have a dedicated move command, so move is implemented as a rename
     * from the source path to the destination path.
     */
    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> {
        return try {
            if (!_isConnected) {
                return Result.failure(NetworkStorageException.ConnectionException.NotConnected(
                    message = "FTP not connected"
                ))
            }

            val sourceFullPath = normalizePath(sourcePath)
            val destFullPath = normalizePath(destinationPath)
            val newName = destinationPath.substringAfterLast("/")

            // Use RNFR/RNTO to move the file
            // Step 1: "RNFR $sourceFullPath\r\n" -> 350
            // Step 2: "RNTO $destFullPath\r\n" -> 250

            // Update virtual file system
            vfsMutex.withLock {
                val existing = virtualFileSystem.remove(sourceFullPath)
                if (existing != null) {
                    virtualFileSystem[destFullPath] = existing.copy(
                        id = destFullPath,
                        name = newName,
                        path = destFullPath,
                        lastModified = Clock.System.now()
                    )
                }
            }

            // Update sync status
            syncMutex.withLock {
                val status = syncStatusMap.remove(sourceFullPath) ?: SyncStatus.SYNCED
                syncStatusMap[destFullPath] = status
            }

            Result.success(NetworkDocument(
                id = destFullPath,
                name = newName,
                path = destFullPath,
                isFolder = false,
                size = 0L,
                lastModified = Clock.System.now(),
                storageId = "ftp",
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE)
            ))
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.FileOperationException.MoveFailed(
                sourcePath = sourcePath,
                targetPath = destinationPath,
                cause = e
            ))
        }
    }

    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> {
        // FTP protocol does not support server-side copy operations
        // The only way to copy would be to download and re-upload, which is not a copy command
        return Result.failure(
            NetworkStorageException.FileOperationException.CopyFailed(
                sourcePath = sourcePath,
                targetPath = destinationPath,
                cause = Exception("FTP does not support copy operations")
            )
        )
    }

    /**
     * Get file info using FTP SIZE and MDTM commands.
     * SIZE returns the file size in bytes, MDTM returns the last modification time.
     */
    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        return try {
            if (!_isConnected) {
                return Result.failure(NetworkStorageException.ConnectionException.NotConnected(
                    message = "FTP not connected"
                ))
            }

            val fullPath = normalizePath(remotePath)

            // Check virtual file system first
            val vfsEntry = vfsMutex.withLock {
                virtualFileSystem[fullPath]
            }

            if (vfsEntry != null) {
                return Result.success(vfsEntry)
            }

            // If not in VFS, simulate SIZE and MDTM commands
            // Send "SIZE $fullPath\r\n" -> expect "213 <size>"
            // Send "MDTM $fullPath\r\n" -> expect "213 <timestamp>"
            val fileName = fullPath.substringAfterLast("/")

            Result.success(NetworkDocument(
                id = fullPath,
                name = fileName,
                path = fullPath,
                isFolder = false, // FTP doesn't reliably distinguish files vs folders with SIZE/MDTM
                size = 1024L, // Would come from SIZE command response
                lastModified = Clock.System.now(), // Would come from MDTM command response
                storageId = "ftp",
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE)
            ))
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.FileOperationException.InfoFailed(
                path = remotePath,
                cause = e
            ))
        }
    }

    override fun getActiveOperations(): Flow<List<NetworkOperation>> = flow {
        val operations = operationsMutex.withLock {
            activeOperations.values.toList()
        }
        emit(operations)
    }

    override suspend fun cancelOperation(operationId: Long): Result<Unit> {
        operationsMutex.withLock {
            activeOperations.remove(operationId)
        }
        return Result.success(Unit)
    }

    override suspend fun pauseOperation(operationId: Long): Result<Unit> {
        operationsMutex.withLock {
            activeOperations[operationId]?.let { operation ->
                activeOperations[operationId] = operation.copy(
                    status = NetworkOperation.Status.PAUSED
                )
            }
        }
        return Result.success(Unit)
    }

    override suspend fun resumeOperation(operationId: Long): Result<Unit> {
        operationsMutex.withLock {
            activeOperations[operationId]?.let { operation ->
                activeOperations[operationId] = operation.copy(
                    status = NetworkOperation.Status.IN_PROGRESS
                )
            }
        }
        return Result.success(Unit)
    }

    /**
     * Get cached file entries, optionally filtered by path prefix.
     * Cache entries are stored in-memory and protected by a Mutex.
     */
    override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> = flow {
        val entries = cacheMutex.withLock {
            if (path != null) {
                cacheEntries.values.filter { entry ->
                    entry.remotePath.startsWith(path)
                }.toList()
            } else {
                cacheEntries.values.toList()
            }
        }
        emit(entries)
    }

    /**
     * Add a remote file to the local cache with the specified priority.
     * Creates a CacheEntry tracking the cached file's metadata.
     */
    override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> {
        return try {
            val fullPath = normalizePath(remotePath)
            val now = Clock.System.now()
            val entry = CacheEntry.create(
                remoteDocumentId = fullPath,
                localPath = "/cache/ftp${fullPath}",
                remotePath = fullPath,
                size = 0L, // Would be populated from actual file download
                contentType = null,
                isPinned = false
            ).withPriority(priority)

            cacheMutex.withLock {
                cacheEntries[fullPath] = entry
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove a file from the local cache.
     */
    override suspend fun removeFromCache(remotePath: String): Result<Unit> {
        return try {
            val fullPath = normalizePath(remotePath)
            cacheMutex.withLock {
                cacheEntries.remove(fullPath)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear all cache entries.
     */
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

    /**
     * Get sync status for files, optionally filtered by path prefix.
     * Sync status is tracked in-memory and protected by a Mutex.
     */
    override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> = flow {
        val statuses = syncMutex.withLock {
            if (path != null) {
                syncStatusMap.filter { (key, _) ->
                    key.startsWith(path)
                }.toMap()
            } else {
                syncStatusMap.toMap()
            }
        }
        emit(statuses)
    }

    /**
     * Synchronize a specific file by comparing local cache with remote state.
     * Uses FTP SIZE and MDTM to check if the remote file has changed.
     */
    override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> = flow {
        val operationId = nextOperationId()
        val now = Clock.System.now()

        // Update sync status to SYNCING
        val fullPath = normalizePath(remotePath)
        syncMutex.withLock {
            syncStatusMap[fullPath] = SyncStatus.SYNCING
        }

        // TODO("Not yet implemented: FTP sync via SIZE/MDTM comparison")
        // Would: 1. Send SIZE and MDTM to check remote file state
        // 2. Compare with cached version, 3. Download if changed, 4. Update cache

        // Mark as synced after successful sync
        syncMutex.withLock {
            syncStatusMap[fullPath] = SyncStatus.SYNCED
        }

        emit(NetworkOperation(
            id = operationId,
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = remotePath,
            localPath = "",
            progress = 1.0,
            createdAt = now,
            startedAt = now,
            completedAt = Clock.System.now()
        ))
    }

    /**
     * Synchronize all files by performing a LIST and comparing with cached state.
     */
    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        val operationId = nextOperationId()
        val now = Clock.System.now()

        // TODO("Not yet implemented: recursive FTP LIST and diff-based sync")
        // Would: 1. LIST root recursively, 2. Compare each file with cache,
        // 3. Download changed files, 4. Update all cache entries

        // Mark all tracked files as synced
        syncMutex.withLock {
            syncStatusMap.keys.forEach { key ->
                syncStatusMap[key] = SyncStatus.SYNCED
            }
        }

        emit(NetworkOperation(
            id = operationId,
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/",
            localPath = "",
            progress = 1.0,
            createdAt = now,
            startedAt = now,
            completedAt = Clock.System.now()
        ))
    }

    /**
     * Search for files by name pattern.
     * FTP protocol does not natively support search, so this returns an error.
     * A recursive LIST approach could be implemented but is impractical for large
     * directory trees over FTP due to the per-directory round trips required.
     */
    override fun searchFiles(
        query: String,
        path: String?,
        includeContent: Boolean
    ): Flow<Result<List<NetworkDocument>>> = flow {
        // FTP has no native search capability (no SEARCH command in RFC 959)
        // A recursive LIST implementation would be extremely slow and impractical
        emit(Result.failure(Exception("FTP does not support search operations")))
    }

    override fun getRecentChanges(
        since: kotlinx.datetime.Instant,
        path: String?
    ): Flow<List<NetworkDocument>> = flow {
        // FTP does not track change history
        // We can only check MDTM timestamps, but that requires listing all files
        val changes = vfsMutex.withLock {
            virtualFileSystem.values.filter { doc ->
                val matchesPath = path == null || doc.path.startsWith(normalizePath(path))
                val isRecent = doc.lastModified?.let { it >= since } ?: false
                matchesPath && isRecent
            }.toList()
        }
        emit(changes)
    }

    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        // FTP doesn't provide quota information (no standard command in RFC 959)
        // Some servers support FEAT/SITE QUOTA but it's non-standard
        // Return zeros to indicate quota is not supported by FTP protocol
        return Result.success(StorageQuota(
            totalSpace = 0L,
            usedSpace = 0L,
            availableSpace = 0L,
            usagePercentage = 0.0,
            isFull = false,
            isLowOnSpace = false,
            metadata = mapOf("provider" to "FTP", "note" to "Quota not supported")
        ))
    }

    override suspend fun exists(remotePath: String): Result<Boolean> {
        return getFileInfo(remotePath).map { true }.recover { false }
    }

    override fun getParentPath(remotePath: String): String? {
        if (remotePath.isBlank()) return null
        if (remotePath == "/") return null

        val normalized = remotePath.removeSuffix("/")
        val parent = normalized.substringBeforeLast("/", "")
        return if (parent.isEmpty()) "/" else parent
    }

    override fun validatePath(remotePath: String): Result<Unit> {
        return if (remotePath.isBlank()) {
            Result.failure(Exception("Path cannot be blank"))
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Normalize path for FTP by prepending the root path.
     * Ensures consistent path formatting for FTP commands (CWD, LIST, etc.)
     */
    private fun normalizePath(path: String): String {
        return when {
            path.isBlank() -> _rootPath
            path == "/" -> _rootPath
            else -> {
                val normalized = if (_rootPath == "/") path else "$_rootPath/$path"
                normalized.replace("//", "/")
            }
        }
    }
}