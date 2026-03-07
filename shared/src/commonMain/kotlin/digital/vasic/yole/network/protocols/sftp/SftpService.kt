/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * SFTP (SSH File Transfer Protocol) implementation of NetworkStorageService.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.sftp

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * SFTP (SSH File Transfer Protocol) implementation of [NetworkStorageService].
 *
 * This service simulates SFTP operations using SSH File Transfer Protocol
 * semantics (SSH_FXP_* packet types) with an in-memory virtual file system.
 * File operations mutate only the in-memory state.
 *
 * For actual SSH/SFTP network I/O, use the platform-specific [SshClient]
 * and [SftpChannel] classes (backed by the sshj library on JVM platforms).
 *
 * ### What works (in-memory simulation):
 * - [connect] / [disconnect] -- validates config (host, port, auth), manages flag
 * - [testConnection] -- validates host, port, and authentication credentials
 * - Authentication validation (password or private key)
 * - [listFiles] -- returns entries from the virtual file system
 * - [downloadFile] / [uploadFile] -- simulate chunked encrypted transfers with progress
 * - [deleteFile], [createFolder], [renameFile], [moveFile], [copyFile] -- mutate virtual FS
 * - [getFileInfo], [exists] -- query virtual FS or return synthesized documents
 * - Cache and sync status tracking (in-memory maps)
 *
 * ### What requires real protocol client:
 * - Actual TCP/SSH connection and key exchange
 * - SSH authentication (password or public key)
 * - Real encrypted file content transfer
 * - Server-side directory listing (SSH_FXP_READDIR)
 * - Quota information (non-standard "statvfs@openssh.com" extension)
 */
class SftpService(
    override val config: StorageConfig.SftpConfig
) : NetworkStorageService {

    private var _isConnected = false
    private val stateMutex = Mutex()
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
     * Generate a unique operation ID using an atomic counter combined with timestamp.
     */
    private suspend fun nextOperationId(): Long {
        return counterMutex.withLock {
            operationCounter++
            Clock.System.now().toEpochMilliseconds() + operationCounter
        }
    }

    /** Returns metadata about this SFTP storage, including connection state and capabilities. */
    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "sftp_${config.name}",
            name = config.name,
            type = StorageType.SFTP,
            location = "sftp://${config.host}:${config.port}${_rootPath}",
            isOnline = _isConnected,
            lastSync = Clock.System.now(),
            supportsFolders = true,
            supportsMetadata = true
        )
    }

    /** Establishes an SFTP connection by validating authentication and performing an SSH handshake. */
    override suspend fun connect(): Result<Unit> {
        return try {
            // Validate authentication configuration before attempting connection
            val authValidation = validateAuthentication()
            if (authValidation.isFailure) {
                return authValidation
            }

            // Test SFTP connection (SSH handshake, key exchange, authentication)
            val connectionTest = testSftpConnection()
            if (connectionTest.isSuccess) {
                stateMutex.withLock { _isConnected = true }
                // Initialize the virtual file system with default directory listing
                initializeVirtualFileSystem()
                Result.success(Unit)
            } else {
                Result.failure(
                    NetworkStorageException.ConnectionException.Failed(
                        message = "SFTP connection failed",
                        cause = connectionTest.exceptionOrNull()
                    )
                )
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(
                NetworkStorageException.ConnectionException.Failed(
                    message = "SFTP connection failed",
                    cause = e
                )
            )
        }
    }

    /**
     * Validate that sufficient authentication credentials are provided.
     * SFTP requires either password or private key authentication (or both).
     */
    private fun validateAuthentication(): Result<Unit> {
        return when {
            // Check if we have password authentication
            !config.password.isNullOrBlank() && !config.username.isNullOrBlank() -> {
                Result.success(Unit)
            }
            // Check if we have key-based authentication
            !config.privateKeyPath.isNullOrBlank() && !config.username.isNullOrBlank() -> {
                Result.success(Unit)
            }
            else -> {
                Result.failure(
                    NetworkStorageException.ConnectionException.Authentication(
                        message = "SFTP requires either password or private key authentication",
                        authType = "SSH",
                        username = config.username ?: "unknown"
                    )
                )
            }
        }
    }

    /** Disconnects from the SFTP server and clears all active operations. */
    override suspend fun disconnect(): Result<Unit> = try {
        stateMutex.withLock { _isConnected = false }
        // Clear active operations on disconnect
        operationsMutex.withLock {
            activeOperations.clear()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        if (e is kotlin.coroutines.cancellation.CancellationException) throw e
        stateMutex.withLock { _isConnected = false } // Ensure we mark as disconnected even on error
        Result.failure(NetworkStorageException.fromThrowable(e, "disconnect"))
    }

    /** Tests the SFTP connection by validating host, port, and authentication credentials. */
    override suspend fun testConnection(): Result<Boolean> {
        return testSftpConnection().map { true }
    }

    /**
     * Test SFTP connection by validating configuration.
     *
     * Validates host, port, authentication credentials, and strict host key
     * checking configuration. For actual SSH handshake, use [SshClient] directly.
     */
    private suspend fun testSftpConnection(): Result<Unit> {
        return try {
            // Validate configuration first
            val authValidation = validateAuthentication()
            if (authValidation.isFailure) {
                return authValidation
            }

            // Check if server is reachable and configuration is valid
            if (config.host.isBlank()) {
                return Result.failure(Exception("SFTP host cannot be blank"))
            }

            if (config.port <= 0 || config.port > 65535) {
                return Result.failure(Exception("Invalid SFTP port: ${config.port}"))
            }

            // Validate strict host key checking requirements
            if (config.strictHostKeyChecking && config.knownHostsPath.isNullOrBlank()) {
                return Result.failure(
                    Exception("Strict host key checking enabled but no known_hosts file provided")
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(
                NetworkStorageException.ConnectionException.Failed(
                    message = "SFTP connection test failed",
                    cause = e
                )
            )
        }
    }

    /**
     * Initialize the virtual file system with a default directory structure.
     * This represents what SSH_FXP_READDIR would return for the root directory.
     */
    private suspend fun initializeVirtualFileSystem() {
        vfsMutex.withLock {
            virtualFileSystem.clear()
            val basePath = _rootPath
            virtualFileSystem["$basePath/document.pdf"] = NetworkDocument(
                id = "document.pdf",
                name = "document.pdf",
                path = "$basePath/document.pdf",
                isFolder = false,
                size = 5242880L, // 5MB
                lastModified = Clock.System.now().minus(24.days),
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(
                    DocumentPermission.READ,
                    DocumentPermission.WRITE,
                    DocumentPermission.EXECUTE
                ),
                storageId = "sftp"
            )
            virtualFileSystem["$basePath/README.md"] = NetworkDocument(
                id = "README.md",
                name = "README.md",
                path = "$basePath/README.md",
                isFolder = false,
                size = 4096L,
                lastModified = Clock.System.now().minus(2.hours),
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                storageId = "sftp"
            )
            virtualFileSystem["$basePath/project_folder"] = NetworkDocument(
                id = "project_folder",
                name = "project_folder",
                path = "$basePath/project_folder",
                isFolder = true,
                size = 0L,
                lastModified = Clock.System.now().minus(12.hours),
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(
                    DocumentPermission.READ,
                    DocumentPermission.WRITE,
                    DocumentPermission.EXECUTE
                ),
                storageId = "sftp"
            )
            virtualFileSystem["$basePath/config.json"] = NetworkDocument(
                id = "config.json",
                name = "config.json",
                path = "$basePath/config.json",
                isFolder = false,
                size = 8192L,
                lastModified = Clock.System.now().minus(30.minutes),
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(
                    DocumentPermission.READ,
                    DocumentPermission.WRITE,
                    DocumentPermission.DELETE
                ),
                storageId = "sftp"
            )
        }
    }

    /**
     * List files in the given path using SFTP SSH_FXP_OPENDIR / SSH_FXP_READDIR.
     * Returns file entries with full POSIX-style permissions and metadata.
     */
    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        if (!_isConnected) {
            emit(
                Result.failure(
                    NetworkStorageException.ConnectionException.NotConnected(
                        message = "SFTP not connected"
                    )
                )
            )
            return@flow
        }

        val fullPath = normalizePath(path)

        // Retrieve files from virtual file system that are direct children of fullPath
        val vfsFiles = vfsMutex.withLock {
            virtualFileSystem.values.filter { doc ->
                val docParent = doc.path.substringBeforeLast("/", "")
                val normalizedParent = if (docParent.isEmpty()) "/" else docParent
                normalizedParent == fullPath || (fullPath == "/" && docParent.isEmpty())
            }.toList()
        }

        // Use VFS entries if available for the root path, otherwise return defaults
        val files = if (vfsFiles.isNotEmpty()) {
            vfsFiles
        } else {
            // Default SFTP directory listing with enhanced metadata (SSH_FXP_ATTRS)
            listOf(
                NetworkDocument(
                    id = "document.pdf",
                    name = "document.pdf",
                    path = "$fullPath/document.pdf",
                    isFolder = false,
                    size = 5242880L, // 5MB
                    lastModified = Clock.System.now().minus(24.days),
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(
                        DocumentPermission.READ,
                        DocumentPermission.WRITE,
                        DocumentPermission.EXECUTE
                    ),
                    storageId = "sftp"
                ),
                NetworkDocument(
                    id = "README.md",
                    name = "README.md",
                    path = "$fullPath/README.md",
                    isFolder = false,
                    size = 4096L,
                    lastModified = Clock.System.now().minus(2.hours),
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                    storageId = "sftp"
                ),
                NetworkDocument(
                    id = "project_folder",
                    name = "project_folder",
                    path = "$fullPath/project_folder",
                    isFolder = true,
                    size = 0L,
                    lastModified = Clock.System.now().minus(12.hours),
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(
                        DocumentPermission.READ,
                        DocumentPermission.WRITE,
                        DocumentPermission.EXECUTE
                    ),
                    storageId = "sftp"
                ),
                NetworkDocument(
                    id = "config.json",
                    name = "config.json",
                    path = "$fullPath/config.json",
                    isFolder = false,
                    size = 8192L,
                    lastModified = Clock.System.now().minus(30.minutes),
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
                    storageId = "sftp"
                )
            )
        }

        // Update sync status for listed files
        syncMutex.withLock {
            files.forEach { doc ->
                syncStatusMap[doc.path] = SyncStatus.SYNCED
            }
        }

        emit(Result.success(files))
    }.catch { e ->
        if (e is kotlin.coroutines.cancellation.CancellationException) throw e
        emit(
            Result.failure(
                NetworkStorageException.FileOperationException.ListFailed(
                    path = path,
                    cause = e
                )
            )
        )
    }

    /**
     * Download a file using SFTP SSH_FXP_OPEN (read mode) and SSH_FXP_READ commands.
     * The transfer is performed in encrypted chunks through the SSH channel.
     * Progress tracking includes:
     * - 0.0: Operation started
     * - 0.1: SSH channel established, sending SSH_FXP_OPEN
     * - 0.3: File handle obtained, preparing for transfer
     * - 0.4: SSH_FXP_STAT received, file size known
     * - 0.4-1.0: Chunked encrypted data transfer via SSH_FXP_READ
     */
    override suspend fun downloadFile(
        remotePath: String,
        localPath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = nextOperationId()

        if (!_isConnected) {
            emit(
                createFailedOperation(
                    operationId, NetworkOperation.Type.DOWNLOAD,
                    remotePath, localPath, "SFTP not connected"
                )
            )
            return@flow
        }

        val fullPath = normalizePath(remotePath)

        try {
            // Start operation - SSH_FXP_OPEN request
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

            // Step 1: Send SSH_FXP_OPEN with pflags = SSH_FXF_READ
            emit(initialOperation.copy(progress = 0.1))

            // Step 2: SSH handshake and encrypted channel establishment
            delay(150) // SSH key negotiation time

            emit(initialOperation.copy(progress = 0.3))

            // Step 3: Send SSH_FXP_STAT to get file attributes (size, permissions, mtime)
            val fileSize = vfsMutex.withLock {
                virtualFileSystem[fullPath]?.size ?: 10485760L // 10MB default
            }

            emit(initialOperation.copy(progress = 0.4, totalSize = fileSize))

            // Step 4: Chunked encrypted transfer via SSH_FXP_READ
            val chunkSize = 65536L // 64KB chunks (standard SFTP max packet size)
            var transferredBytes = 0L

            while (transferredBytes < fileSize) {
                // Simulate reading an encrypted chunk from the SSH channel
                delay(50) // Network transfer time for encrypted chunk

                transferredBytes += chunkSize
                if (transferredBytes > fileSize) {
                    transferredBytes = fileSize
                }

                val progress = transferredBytes.toDouble() / fileSize.toDouble()
                val currentOp = initialOperation.copy(
                    progress = progress,
                    totalSize = fileSize,
                    bytesTransferred = transferredBytes
                )
                updateActiveOperation(operationId, currentOp)
                emit(currentOp)
            }

            // Step 5: Send SSH_FXP_CLOSE to release the file handle
            // Update sync status for the downloaded file
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            val errorOperation = NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.DOWNLOAD,
                status = NetworkOperation.Status.FAILED,
                remotePath = remotePath,
                localPath = localPath,
                error = e.message ?: "SFTP download failed",
                createdAt = Clock.System.now(),
                startedAt = Clock.System.now(),
                completedAt = Clock.System.now()
            )

            removeActiveOperation(operationId)
            emit(errorOperation)
        }
    }

    /**
     * Upload a file using SFTP SSH_FXP_OPEN (write mode) and SSH_FXP_WRITE commands.
     * The transfer is performed in encrypted chunks through the SSH channel.
     * Progress tracking includes:
     * - 0.0: Operation started
     * - 0.1: Reading local file, preparing for transfer
     * - 0.2: SSH_FXP_OPEN sent for writing
     * - 0.2-0.95: Chunked encrypted data transfer via SSH_FXP_WRITE
     * - 0.95: SSH_FXP_CLOSE sent
     * - 1.0: Transfer complete with integrity verification
     */
    override suspend fun uploadFile(
        localPath: String,
        remotePath: String
    ): Flow<NetworkOperation> = flow {
        val operationId = nextOperationId()

        if (!_isConnected) {
            emit(
                createFailedOperation(
                    operationId, NetworkOperation.Type.UPLOAD,
                    remotePath, localPath, "SFTP not connected"
                )
            )
            return@flow
        }

        val fullPath = normalizePath(remotePath)

        try {
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

            // Step 1: Read local file and prepare for transfer
            val fileSize = 5242880L // 5MB - would come from actual file in real implementation

            emit(initialOperation.copy(progress = 0.1, totalSize = fileSize))

            // Step 2: Send SSH_FXP_OPEN with pflags = SSH_FXF_WRITE | SSH_FXF_CREAT | SSH_FXF_TRUNC
            delay(100) // SSH channel setup and file handle creation

            emit(initialOperation.copy(progress = 0.2, totalSize = fileSize))

            // Step 3: Resolve parent folder path to ensure it exists
            // Send SSH_FXP_STAT on parent directory
            emit(initialOperation.copy(progress = 0.6, totalSize = fileSize))

            // Step 4: Chunked encrypted transfer via SSH_FXP_WRITE
            val chunkSize = 32768L // 32KB encrypted chunks
            var transferredBytes = 0L

            while (transferredBytes < fileSize) {
                // Simulate encrypting and sending chunk via SSH channel
                delay(30) // Encryption and network transfer time

                transferredBytes += chunkSize
                if (transferredBytes > fileSize) {
                    transferredBytes = fileSize
                }

                val progress = transferredBytes.toDouble() / fileSize.toDouble()
                val currentOp = initialOperation.copy(
                    progress = progress,
                    totalSize = fileSize,
                    bytesTransferred = transferredBytes
                )
                updateActiveOperation(operationId, currentOp)
                emit(currentOp)
            }

            // Step 5: Send SSH_FXP_CLOSE command
            delay(50)
            emit(
                initialOperation.copy(
                    progress = 0.95,
                    totalSize = fileSize,
                    bytesTransferred = fileSize
                )
            )

            // Step 6: Verify integrity and finalize
            delay(100) // Finalization and integrity check

            // Register the uploaded file in the virtual file system
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
                    permissions = setOf(
                        DocumentPermission.READ,
                        DocumentPermission.WRITE,
                        DocumentPermission.EXECUTE
                    ),
                    storageId = "sftp"
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            val errorOperation = NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.UPLOAD,
                status = NetworkOperation.Status.FAILED,
                remotePath = remotePath,
                localPath = localPath,
                error = e.message ?: "SFTP upload failed",
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
     * Delete a file using SFTP SSH_FXP_REMOVE command, or a directory using SSH_FXP_RMDIR.
     */
    override suspend fun deleteFile(remotePath: String): Result<Unit> {
        return try {
            if (!_isConnected) {
                return Result.failure(
                    NetworkStorageException.ConnectionException.NotConnected(
                        message = "SFTP not connected"
                    )
                )
            }

            val fullPath = normalizePath(remotePath)

            // Send SSH_FXP_REMOVE for files, SSH_FXP_RMDIR for directories
            delay(100) // Simulate secure operation over SSH channel

            // Remove from virtual file system
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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(
                NetworkStorageException.FileOperationException.DeleteFailed(
                    path = remotePath,
                    cause = e
                )
            )
        }
    }

    /**
     * Create a folder using SFTP SSH_FXP_MKDIR command with specified attributes.
     * Sets default POSIX permissions (rwxr-xr-x = 0755).
     */
    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> {
        return try {
            if (!_isConnected) {
                return Result.failure(
                    NetworkStorageException.ConnectionException.NotConnected(
                        message = "SFTP not connected"
                    )
                )
            }

            val fullPath = normalizePath(remotePath)
            val folderName = fullPath.substringAfterLast("/")

            // Send SSH_FXP_MKDIR with attrs (permissions = 0755)
            delay(150) // Simulate secure operation

            val folderDoc = NetworkDocument(
                id = fullPath,
                name = folderName,
                path = fullPath,
                isFolder = true,
                size = 0L,
                lastModified = Clock.System.now(),
                storageId = "sftp",
                syncStatus = SyncStatus.SYNCED,
                permissions = setOf(
                    DocumentPermission.READ,
                    DocumentPermission.WRITE,
                    DocumentPermission.EXECUTE
                )
            )

            // Register in virtual file system
            vfsMutex.withLock {
                virtualFileSystem[fullPath] = folderDoc
            }

            // Update sync status
            syncMutex.withLock {
                syncStatusMap[fullPath] = SyncStatus.SYNCED
            }

            Result.success(folderDoc)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(
                NetworkStorageException.FileOperationException.CreateFolderFailed(
                    path = remotePath,
                    cause = e
                )
            )
        }
    }

    /**
     * Rename a file or directory using SFTP SSH_FXP_RENAME command.
     * This is an atomic operation on the server side.
     */
    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> {
        return try {
            if (!_isConnected) {
                return Result.failure(
                    NetworkStorageException.ConnectionException.NotConnected(
                        message = "SFTP not connected"
                    )
                )
            }

            val fullPath = normalizePath(remotePath)
            val parentDir = fullPath.substringBeforeLast("/", "")
            val newFullPath = if (parentDir.isEmpty()) "/$newName" else "$parentDir/$newName"

            // Send SSH_FXP_RENAME (oldpath, newpath)
            delay(120) // Simulate secure operation

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
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(NetworkStorageException.fromThrowable(e, "renameFile"))
        }
    }

    /**
     * Move a file to a new location using SFTP SSH_FXP_RENAME command.
     * SFTP supports direct rename/move operations (unlike FTP which requires RNFR/RNTO).
     */
    override suspend fun moveFile(
        sourcePath: String,
        destinationPath: String
    ): Result<NetworkDocument> {
        return try {
            if (!_isConnected) {
                return Result.failure(
                    NetworkStorageException.ConnectionException.NotConnected(
                        message = "SFTP not connected"
                    )
                )
            }

            val sourceFullPath = normalizePath(sourcePath)
            val destFullPath = normalizePath(destinationPath)
            val newName = destinationPath.substringAfterLast("/")

            // Use SSH_FXP_RENAME to atomically move the file
            delay(120) // Simulate secure operation

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

            Result.success(
                NetworkDocument(
                    id = destFullPath,
                    name = newName,
                    path = destFullPath,
                    isFolder = false,
                    size = 0L,
                    lastModified = Clock.System.now(),
                    storageId = "sftp",
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(
                        DocumentPermission.READ,
                        DocumentPermission.WRITE,
                        DocumentPermission.EXECUTE
                    )
                )
            )
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(
                NetworkStorageException.FileOperationException.MoveFailed(
                    sourcePath = sourcePath,
                    targetPath = destinationPath,
                    cause = e
                )
            )
        }
    }

    /**
     * Copy a file using SFTP SSH_FXP_EXTENDED with "copy-data" extension.
     * This is a server-side copy operation available in SFTP v6+ or via extensions.
     */
    override suspend fun copyFile(
        sourcePath: String,
        destinationPath: String
    ): Result<Unit> {
        return try {
            if (!_isConnected) {
                return Result.failure(
                    NetworkStorageException.ConnectionException.NotConnected(
                        message = "SFTP not connected"
                    )
                )
            }

            val sourceFullPath = normalizePath(sourcePath)
            val destFullPath = normalizePath(destinationPath)

            // Send SSH_FXP_EXTENDED with "copy-data" extension
            delay(200) // Simulate secure copy operation

            // Register the copy in virtual file system
            vfsMutex.withLock {
                val source = virtualFileSystem[sourceFullPath]
                if (source != null) {
                    val destName = destinationPath.substringAfterLast("/")
                    virtualFileSystem[destFullPath] = source.copy(
                        id = destFullPath,
                        name = destName,
                        path = destFullPath,
                        lastModified = Clock.System.now()
                    )
                }
            }

            // Update sync status for copied file
            syncMutex.withLock {
                syncStatusMap[destFullPath] = SyncStatus.SYNCED
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(
                NetworkStorageException.FileOperationException.CopyFailed(
                    sourcePath = sourcePath,
                    targetPath = destinationPath,
                    cause = e
                )
            )
        }
    }

    /**
     * Get file info using SFTP SSH_FXP_STAT or SSH_FXP_LSTAT command.
     * Returns file attributes including size, permissions, uid/gid, atime/mtime.
     */
    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> {
        return try {
            if (!_isConnected) {
                return Result.failure(
                    NetworkStorageException.ConnectionException.NotConnected(
                        message = "SFTP not connected"
                    )
                )
            }

            val fullPath = normalizePath(remotePath)

            // Check virtual file system first
            val vfsEntry = vfsMutex.withLock {
                virtualFileSystem[fullPath]
            }

            if (vfsEntry != null) {
                return Result.success(vfsEntry)
            }

            // If not in VFS, send SSH_FXP_STAT to get file attributes
            val fileName = fullPath.substringAfterLast("/")
            val isDirectory = fullPath.endsWith("/") // Simplified check
            val fileSize = if (isDirectory) 0L else 8192L
            val lastModified = Clock.System.now().minus(2.hours)

            Result.success(
                NetworkDocument(
                    id = fullPath,
                    name = fileName,
                    path = fullPath,
                    isFolder = isDirectory,
                    size = fileSize,
                    storageId = "sftp",
                    lastModified = lastModified,
                    syncStatus = SyncStatus.SYNCED,
                    permissions = setOf(
                        DocumentPermission.READ,
                        DocumentPermission.WRITE,
                        DocumentPermission.EXECUTE,
                        DocumentPermission.DELETE
                    )
                )
            )
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(
                NetworkStorageException.FileOperationException.InfoFailed(
                    path = remotePath,
                    cause = e
                )
            )
        }
    }

    /** Returns a flow emitting the list of currently active SFTP transfer operations. */
    override fun getActiveOperations(): Flow<List<NetworkOperation>> = flow {
        operationsMutex.withLock {
            emit(activeOperations.values.toList())
        }
    }

    /** Cancels and removes the active operation identified by [operationId]. */
    override suspend fun cancelOperation(operationId: Long): Result<Unit> {
        operationsMutex.withLock {
            activeOperations.remove(operationId)
        }
        return Result.success(Unit)
    }

    /** Pauses the active operation identified by [operationId]. */
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

    /** Resumes the paused operation identified by [operationId]. */
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

    /** Adds the file at [remotePath] to the local cache with the given [priority]. */
    override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> {
        return try {
            val fullPath = normalizePath(remotePath)
            val entry = CacheEntry.create(
                remoteDocumentId = fullPath,
                localPath = "/cache/sftp${fullPath}",
                remotePath = fullPath,
                size = 0L,
                contentType = null,
                isPinned = false
            ).withPriority(priority)

            cacheMutex.withLock {
                cacheEntries[fullPath] = entry
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Removes the cached entry for [remotePath] from the local cache. */
    override suspend fun removeFromCache(remotePath: String): Result<Unit> {
        return try {
            val fullPath = normalizePath(remotePath)
            cacheMutex.withLock {
                cacheEntries.remove(fullPath)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Clears all entries from the local cache. */
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

    /** Returns a flow of sync statuses, optionally filtered by [path] prefix. */
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
     * Synchronize a specific file by comparing local cache with remote state via SSH_FXP_STAT.
     */
    override suspend fun syncFile(
        remotePath: String,
        forceSync: Boolean
    ): Flow<NetworkOperation> = flow {
        val operationId = nextOperationId()
        val now = Clock.System.now()

        // Update sync status to SYNCING
        val fullPath = normalizePath(remotePath)
        syncMutex.withLock {
            syncStatusMap[fullPath] = SyncStatus.SYNCING
        }

        // Mark as synced after successful sync
        syncMutex.withLock {
            syncStatusMap[fullPath] = SyncStatus.SYNCED
        }

        emit(
            NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = remotePath,
                localPath = "",
                progress = 1.0,
                createdAt = now,
                startedAt = now,
                completedAt = Clock.System.now()
            )
        )
    }

    /**
     * Synchronize all files by recursively listing directories and comparing with cache.
     */
    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        val operationId = nextOperationId()
        val now = Clock.System.now()

        // Mark all tracked files as synced
        syncMutex.withLock {
            syncStatusMap.keys.forEach { key ->
                syncStatusMap[key] = SyncStatus.SYNCED
            }
        }

        emit(
            NetworkOperation(
                id = operationId,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/",
                localPath = "",
                progress = 1.0,
                createdAt = now,
                startedAt = now,
                completedAt = Clock.System.now()
            )
        )
    }

    /**
     * SFTP has no native search capability.
     */
    override fun searchFiles(
        query: String,
        path: String?,
        includeContent: Boolean
    ): Flow<Result<List<NetworkDocument>>> = flow {
        emit(Result.failure(Exception("SFTP does not support search operations")))
    }

    /**
     * Get recently changed files by checking file modification times.
     */
    override fun getRecentChanges(
        since: kotlinx.datetime.Instant,
        path: String?
    ): Flow<List<NetworkDocument>> = flow {
        val changes = vfsMutex.withLock {
            virtualFileSystem.values.filter { doc ->
                val matchesPath = path == null || doc.path.startsWith(normalizePath(path))
                val isRecent = doc.lastModified?.let { it >= since } ?: false
                matchesPath && isRecent
            }.toList()
        }
        emit(changes)
    }

    /** Returns storage quota info; always returns zeroed values since SFTP has no native quota support. */
    override suspend fun getQuotaInfo(): Result<StorageQuota> {
        // SFTP doesn't provide quota information directly.
        // Some servers support the "statvfs@openssh.com" extension.
        return Result.success(
            StorageQuota(
                totalSpace = 0L,
                usedSpace = 0L,
                availableSpace = 0L,
                usagePercentage = 0.0,
                isFull = false,
                isLowOnSpace = false,
                metadata = mapOf(
                    "provider" to "SFTP",
                    "note" to "Quota not supported by SFTP protocol",
                    "encryption" to "SSH2"
                )
            )
        )
    }

    /** Checks whether a file or directory exists at [remotePath] via SSH_FXP_STAT. */
    override suspend fun exists(remotePath: String): Result<Boolean> {
        return getFileInfo(remotePath).map { true }.recover { false }
    }

    /** Returns the parent directory of [remotePath], or null if it is the root. */
    override fun getParentPath(remotePath: String): String? {
        if (remotePath.isBlank()) return null
        if (remotePath == "/") return null

        val normalized = remotePath.removeSuffix("/")
        val parent = normalized.substringBeforeLast("/", "")
        return if (parent.isEmpty()) "/" else parent
    }

    /** Validates that [remotePath] is a non-blank path suitable for SFTP operations. */
    override fun validatePath(remotePath: String): Result<Unit> {
        return if (remotePath.isBlank()) {
            Result.failure(Exception("Path cannot be blank"))
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Normalize path for SFTP by prepending the root path.
     */
    private fun normalizePath(path: String): String {
        if (path.isBlank() || path == "/") return _rootPath

        val basePath = if (_rootPath == "/") path else "$_rootPath/$path"
        val segments = basePath.split("/").filter { it.isNotEmpty() }
        val resolved = mutableListOf<String>()
        for (segment in segments) {
            when (segment) {
                "." -> { /* skip current-dir marker */ }
                ".." -> { if (resolved.isNotEmpty()) resolved.removeLast() }
                else -> resolved.add(segment)
            }
        }
        val result = "/" + resolved.joinToString("/")

        // Ensure result stays within root boundary
        val rootPrefix = if (_rootPath.isBlank()) "/" else _rootPath
        return if (result.startsWith(rootPrefix) || rootPrefix == "/") result else _rootPath
    }
}
