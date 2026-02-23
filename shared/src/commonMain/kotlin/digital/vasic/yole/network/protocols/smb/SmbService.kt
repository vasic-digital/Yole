/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * SMB/CIFS implementation of NetworkStorageService.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.smb

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.StorageQuota
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * SMB/CIFS implementation of [NetworkStorageService].
 *
 * This service maintains an in-memory file tree that tracks all file operations
 * and provides consistent state. File operations mutate only the in-memory state.
 *
 * For actual SMB/CIFS network I/O, use the platform-specific [SmbProtocolClient]
 * class (backed by the smbj library on JVM platforms).
 *
 * ### What works (in-memory simulation):
 * - [connect] / [disconnect] -- sets/clears connection flag (no SMB negotiation)
 * - [testConnection] -- verifies connection flag
 * - [uploadFile] / [downloadFile] -- simulate progress, update internal file tree
 * - [deleteFile] -- removes from file tree (including children for folders)
 * - [createFolder] -- adds folder node to file tree with parent auto-creation
 * - [renameFile] -- renames node and all children in file tree
 * - [moveFile] -- moves node and all children in file tree
 * - [copyFile] -- copies node in file tree
 * - [getFileInfo] -- returns node from file tree or synthesized document
 * - [exists] -- checks file tree for key existence
 * - Cache and sync status tracking (in-memory maps)
 * - [getRecentChanges] -- filters file tree by lastModified
 * - [getQuotaInfo] -- calculates used space from file tree sizes
 *
 * ### What requires real protocol client:
 * - SMB/CIFS protocol negotiation and session setup
 * - NTLM/Kerberos authentication
 * - Real file content transfer (SMB2 READ/WRITE)
 * - [listFiles] -- returns failure (requires native SMB protocol support)
 * - Server-side search (SMB2 QUERY_DIRECTORY with pattern)
 */
class SmbService(
    override val config: StorageConfig.SmbConfig
) : NetworkStorageService {

    private var _isConnected = false
    private var _rootPath = if (config.path.isBlank()) "/" else config.path

    /**
     * Internal file tree node representing a file or folder in the simulated SMB share.
     */
    private data class FileNode(
        val document: NetworkDocument,
        val content: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FileNode) return false
            return document == other.document
        }

        override fun hashCode(): Int = document.hashCode()
    }

    /** Mutex protecting all internal file tree mutations */
    private val fileTreeMutex = Mutex()

    /** Internal file tree: maps normalized path -> FileNode */
    private val fileTree = mutableMapOf<String, FileNode>()

    /** Mutex protecting cache state */
    private val cacheMutex = Mutex()

    /** In-memory cache entries: maps remotePath -> CacheEntry */
    private val cacheEntries = mutableMapOf<String, CacheEntry>()

    /** Mutex protecting sync status state */
    private val syncMutex = Mutex()

    /** In-memory sync status: maps remotePath -> SyncStatus */
    private val syncStatusMap = mutableMapOf<String, SyncStatus>()

    /** Mutex protecting active operations state */
    private val operationsMutex = Mutex()

    /** Active operations tracked by id */
    private val activeOperations = mutableMapOf<Long, NetworkOperation>()

    /** Default permissions for files in this SMB share */
    private val defaultFilePermissions = setOf(
        DocumentPermission.READ,
        DocumentPermission.WRITE,
        DocumentPermission.DELETE
    )

    /** Default permissions for folders in this SMB share */
    private val defaultFolderPermissions = setOf(
        DocumentPermission.READ,
        DocumentPermission.WRITE,
        DocumentPermission.DELETE,
        DocumentPermission.EXECUTE
    )

    /** Storage ID prefix for documents in this service */
    private val storageId = "smb"

    override val isOnline: Boolean
        get() = _isConnected

    override val rootPath: String
        get() = _rootPath

    override suspend fun getStorageInfo(): NetworkStorage {
        return NetworkStorage(
            id = "smb_${config.name}",
            name = config.name,
            type = StorageType.SMB,
            location = "smb://${config.host}/${config.share}${_rootPath}",
            isOnline = _isConnected,
            lastSync = Clock.System.now()
        )
    }

    override suspend fun connect(): Result<Unit> = try {
        _isConnected = true
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(
            NetworkStorageException.ConnectionException.Failed(
                message = "SMB connection failed",
                cause = e
            )
        )
    }

    override suspend fun disconnect(): Result<Unit> = try {
        _isConnected = false
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "disconnect"))
    }

    override suspend fun testConnection(): Result<Boolean> = try {
        if (_isConnected) {
            Result.success(true)
        } else {
            val connectResult = connect()
            if (connectResult.isSuccess) {
                disconnect()
                Result.success(true)
            } else {
                Result.failure(
                    connectResult.exceptionOrNull() ?: Exception("Connection test failed")
                )
            }
        }
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "testConnection"))
    }

    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        if (!_isConnected) {
            emit(
                Result.failure(
                    NetworkStorageException.ConnectionException.NotConnected(
                        message = "SMB not connected"
                    )
                )
            )
            return@flow
        }

        try {
            // The internal file tree is maintained for state consistency across
            // mutation operations (upload, delete, rename, move, copy, createFolder).
            // Full directory listing requires native SMB protocol support.
            emit(
                Result.failure(
                    NetworkStorageException.FileOperationException.ListFailed(
                        path = path,
                        cause = Exception("SMB list files not implemented")
                    )
                )
            )
        } catch (e: Exception) {
            emit(
                Result.failure(
                    NetworkStorageException.FileOperationException.ListFailed(
                        path = path,
                        cause = e
                    )
                )
            )
        }
    }

    override suspend fun uploadFile(
        localPath: String,
        remotePath: String
    ): Flow<NetworkOperation> = flow {
        if (!_isConnected) {
            emit(
                NetworkOperation.error(
                    id = "upload_$remotePath".hashCode().toLong(),
                    operationType = NetworkOperation.Type.UPLOAD,
                    remotePath = remotePath,
                    localPath = localPath,
                    error = "SMB not connected"
                )
            )
            return@flow
        }

        val operation = NetworkOperation.createUpload(
            id = "upload_$remotePath",
            remotePath = remotePath,
            localPath = localPath
        )

        try {
            // Track operation as active
            operationsMutex.withLock {
                activeOperations[operation.id] = operation
            }

            // Emit progress: starting
            val startedOp = operation.copy(
                status = NetworkOperation.Status.IN_PROGRESS,
                progress = 0.0,
                startedAt = Clock.System.now()
            )
            operationsMutex.withLock { activeOperations[operation.id] = startedOp }
            emit(startedOp)

            // Emit progress: mid-transfer
            val midOp = startedOp.copy(progress = 0.5)
            operationsMutex.withLock { activeOperations[operation.id] = midOp }
            emit(midOp)

            // Add file to internal tree
            val normalizedPath = normalizePath(remotePath)
            val now = Clock.System.now()
            val document = NetworkDocument(
                id = normalizedPath,
                name = normalizedPath.substringAfterLast("/"),
                path = normalizedPath,
                isFolder = false,
                size = 0L,
                lastModified = now,
                storageId = storageId,
                permissions = defaultFilePermissions,
                syncStatus = SyncStatus.SYNCED
            )

            fileTreeMutex.withLock {
                ensureParentFolders(normalizedPath)
                fileTree[normalizedPath] = FileNode(document)
            }

            syncMutex.withLock {
                syncStatusMap[normalizedPath] = SyncStatus.SYNCED
            }

            // Emit progress: transfer complete
            val doneOp = startedOp.copy(progress = 1.0)
            operationsMutex.withLock { activeOperations[operation.id] = doneOp }
            emit(doneOp)

            // Emit completed
            val completedOp = startedOp.copy(
                status = NetworkOperation.Status.COMPLETED,
                progress = 1.0,
                completedAt = Clock.System.now()
            )
            operationsMutex.withLock { activeOperations.remove(operation.id) }
            emit(completedOp)
        } catch (e: Exception) {
            val failedOp = operation.copy(
                status = NetworkOperation.Status.FAILED,
                error = e.message ?: "Upload failed"
            )
            operationsMutex.withLock { activeOperations.remove(operation.id) }
            emit(failedOp)
        }
    }

    override suspend fun downloadFile(
        remotePath: String,
        localPath: String
    ): Flow<NetworkOperation> = flow {
        if (!_isConnected) {
            emit(
                NetworkOperation.error(
                    id = "download_$remotePath".hashCode().toLong(),
                    operationType = NetworkOperation.Type.DOWNLOAD,
                    remotePath = remotePath,
                    localPath = localPath,
                    error = "SMB not connected"
                )
            )
            return@flow
        }

        val operation = NetworkOperation.createDownload(
            id = "download_$remotePath",
            remotePath = remotePath,
            localPath = localPath
        )

        try {
            // Track operation as active
            operationsMutex.withLock {
                activeOperations[operation.id] = operation
            }

            // Emit progress: starting
            val startedOp = operation.copy(
                status = NetworkOperation.Status.IN_PROGRESS,
                progress = 0.0,
                startedAt = Clock.System.now()
            )
            operationsMutex.withLock { activeOperations[operation.id] = startedOp }
            emit(startedOp)

            // Emit progress: mid-transfer
            val midOp = startedOp.copy(progress = 0.5)
            operationsMutex.withLock { activeOperations[operation.id] = midOp }
            emit(midOp)

            // Update sync status for downloaded file
            val normalizedPath = normalizePath(remotePath)
            syncMutex.withLock {
                syncStatusMap[normalizedPath] = SyncStatus.SYNCED
            }

            // Emit progress: transfer complete
            val doneOp = startedOp.copy(progress = 1.0)
            operationsMutex.withLock { activeOperations[operation.id] = doneOp }
            emit(doneOp)

            // Emit completed
            val completedOp = startedOp.copy(
                status = NetworkOperation.Status.COMPLETED,
                progress = 1.0,
                completedAt = Clock.System.now()
            )
            operationsMutex.withLock { activeOperations.remove(operation.id) }
            emit(completedOp)
        } catch (e: Exception) {
            val failedOp = operation.copy(
                status = NetworkOperation.Status.FAILED,
                error = e.message ?: "Download failed"
            )
            operationsMutex.withLock { activeOperations.remove(operation.id) }
            emit(failedOp)
        }
    }

    override suspend fun copyFile(
        sourcePath: String,
        destinationPath: String
    ): Result<Unit> = try {
        val normalizedSource = normalizePath(sourcePath)
        val normalizedDest = normalizePath(destinationPath)
        val now = Clock.System.now()

        fileTreeMutex.withLock {
            val sourceNode = fileTree[normalizedSource]
            if (sourceNode != null) {
                // Copy existing node to new location
                ensureParentFolders(normalizedDest)
                val copiedDoc = sourceNode.document.copy(
                    id = normalizedDest,
                    name = normalizedDest.substringAfterLast("/"),
                    path = normalizedDest,
                    lastModified = now,
                    syncStatus = SyncStatus.SYNCED
                )
                fileTree[normalizedDest] = FileNode(copiedDoc, sourceNode.content?.copyOf())
            } else {
                // Source not in tree; create a placeholder at destination
                ensureParentFolders(normalizedDest)
                val newDoc = NetworkDocument(
                    id = normalizedDest,
                    name = normalizedDest.substringAfterLast("/"),
                    path = normalizedDest,
                    isFolder = false,
                    size = 0L,
                    lastModified = now,
                    storageId = storageId,
                    permissions = defaultFilePermissions,
                    syncStatus = SyncStatus.SYNCED
                )
                fileTree[normalizedDest] = FileNode(newDoc)
            }
        }

        syncMutex.withLock {
            syncStatusMap[normalizedDest] = SyncStatus.SYNCED
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(
            NetworkStorageException.FileOperationException.CopyFailed(
                sourcePath = sourcePath,
                targetPath = destinationPath,
                cause = e
            )
        )
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> = try {
        val normalizedPath = normalizePath(remotePath)

        fileTreeMutex.withLock {
            // Remove the node and all children (if it's a folder)
            val keysToRemove = fileTree.keys.filter { key ->
                key == normalizedPath || key.startsWith("$normalizedPath/")
            }
            keysToRemove.forEach { fileTree.remove(it) }
        }

        syncMutex.withLock {
            val keysToRemove = syncStatusMap.keys.filter { key ->
                key == normalizedPath || key.startsWith("$normalizedPath/")
            }
            keysToRemove.forEach { syncStatusMap.remove(it) }
        }

        cacheMutex.withLock {
            val keysToRemove = cacheEntries.keys.filter { key ->
                key == normalizedPath || key.startsWith("$normalizedPath/")
            }
            keysToRemove.forEach { cacheEntries.remove(it) }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(
            NetworkStorageException.FileOperationException.DeleteFailed(
                path = remotePath,
                cause = e
            )
        )
    }

    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> = try {
        val normalizedPath = normalizePath(remotePath)
        val now = Clock.System.now()

        val document = NetworkDocument(
            id = normalizedPath,
            name = normalizedPath.substringAfterLast("/"),
            path = normalizedPath,
            isFolder = true,
            size = 0L,
            lastModified = now,
            storageId = storageId,
            permissions = defaultFolderPermissions,
            syncStatus = SyncStatus.SYNCED
        )

        fileTreeMutex.withLock {
            ensureParentFolders(normalizedPath)
            fileTree[normalizedPath] = FileNode(document)
        }

        syncMutex.withLock {
            syncStatusMap[normalizedPath] = SyncStatus.SYNCED
        }

        Result.success(document)
    } catch (e: Exception) {
        Result.failure(
            NetworkStorageException.FileOperationException.CreateFolderFailed(
                path = remotePath,
                cause = e
            )
        )
    }

    override suspend fun renameFile(
        remotePath: String,
        newName: String
    ): Result<Unit> = try {
        val normalizedPath = normalizePath(remotePath)
        val parentPath = normalizedPath.substringBeforeLast("/").ifEmpty { "/" }
        val newPath = if (parentPath == "/") "/$newName" else "$parentPath/$newName"

        fileTreeMutex.withLock {
            val existingNode = fileTree.remove(normalizedPath)
            if (existingNode != null) {
                val renamedDoc = existingNode.document.copy(
                    id = newPath,
                    name = newName,
                    path = newPath,
                    lastModified = Clock.System.now()
                )
                fileTree[newPath] = FileNode(renamedDoc, existingNode.content)

                // Rename children if it was a folder
                if (existingNode.document.isFolder) {
                    val childPrefix = "$normalizedPath/"
                    val childEntries = fileTree.entries
                        .filter { it.key.startsWith(childPrefix) }
                        .toList()

                    for ((oldChildPath, childNode) in childEntries) {
                        fileTree.remove(oldChildPath)
                        val newChildPath = newPath + oldChildPath.removePrefix(normalizedPath)
                        val renamedChild = childNode.document.copy(
                            id = newChildPath,
                            path = newChildPath,
                            lastModified = Clock.System.now()
                        )
                        fileTree[newChildPath] = FileNode(renamedChild, childNode.content)
                    }
                }
            }
        }

        syncMutex.withLock {
            val oldStatus = syncStatusMap.remove(normalizedPath)
            if (oldStatus != null) {
                syncStatusMap[newPath] = oldStatus
            }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "renameFile"))
    }

    override suspend fun moveFile(
        sourcePath: String,
        destinationPath: String
    ): Result<NetworkDocument> = try {
        val normalizedSource = normalizePath(sourcePath)
        val normalizedDest = normalizePath(destinationPath)
        val now = Clock.System.now()

        val movedDoc: NetworkDocument

        fileTreeMutex.withLock {
            val sourceNode = fileTree.remove(normalizedSource)
            ensureParentFolders(normalizedDest)

            movedDoc = if (sourceNode != null) {
                val doc = sourceNode.document.copy(
                    id = normalizedDest,
                    name = normalizedDest.substringAfterLast("/"),
                    path = normalizedDest,
                    lastModified = now,
                    syncStatus = SyncStatus.SYNCED
                )
                fileTree[normalizedDest] = FileNode(doc, sourceNode.content)

                // Move children if it was a folder
                if (sourceNode.document.isFolder) {
                    val childPrefix = "$normalizedSource/"
                    val childEntries = fileTree.entries
                        .filter { it.key.startsWith(childPrefix) }
                        .toList()

                    for ((oldChildPath, childNode) in childEntries) {
                        fileTree.remove(oldChildPath)
                        val newChildPath = normalizedDest + oldChildPath.removePrefix(normalizedSource)
                        val movedChild = childNode.document.copy(
                            id = newChildPath,
                            path = newChildPath,
                            lastModified = now
                        )
                        fileTree[newChildPath] = FileNode(movedChild, childNode.content)
                    }
                }

                doc
            } else {
                // Source not in tree; create document at destination
                NetworkDocument(
                    id = normalizedDest,
                    name = normalizedDest.substringAfterLast("/"),
                    path = normalizedDest,
                    isFolder = false,
                    size = 0L,
                    lastModified = now,
                    storageId = storageId,
                    permissions = defaultFilePermissions,
                    syncStatus = SyncStatus.SYNCED
                )
            }
        }

        syncMutex.withLock {
            syncStatusMap.remove(normalizedSource)
            syncStatusMap[normalizedDest] = SyncStatus.SYNCED
        }

        cacheMutex.withLock {
            val entry = cacheEntries.remove(normalizedSource)
            if (entry != null) {
                cacheEntries[normalizedDest] = entry.copy(remotePath = normalizedDest)
            }
        }

        Result.success(movedDoc)
    } catch (e: Exception) {
        Result.failure(
            NetworkStorageException.FileOperationException.MoveFailed(
                sourcePath = sourcePath,
                targetPath = destinationPath,
                cause = e
            )
        )
    }

    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> = try {
        val normalizedPath = normalizePath(remotePath)

        // Check internal file tree first
        val existingNode = fileTreeMutex.withLock { fileTree[normalizedPath] }

        val document = if (existingNode != null) {
            existingNode.document
        } else {
            // Returns a synthesized document since no real SMB connection exists.
            NetworkDocument(
                id = remotePath,
                name = remotePath.substringAfterLast("/"),
                path = remotePath,
                isFolder = false,
                size = 0L,
                lastModified = Clock.System.now(),
                storageId = storageId,
                permissions = setOf(
                    DocumentPermission.READ,
                    DocumentPermission.WRITE
                ),
                syncStatus = SyncStatus.SYNCED
            )
        }

        Result.success(document)
    } catch (e: Exception) {
        Result.failure(
            NetworkStorageException.FileOperationException.InfoFailed(
                path = remotePath,
                cause = e
            )
        )
    }

    override fun getActiveOperations(): Flow<List<NetworkOperation>> = flow {
        val ops = operationsMutex.withLock { activeOperations.values.toList() }
        emit(ops)
    }

    override suspend fun cancelOperation(operationId: Long): Result<Unit> = try {
        operationsMutex.withLock {
            val op = activeOperations[operationId]
            if (op != null) {
                activeOperations[operationId] = op.copy(
                    status = NetworkOperation.Status.CANCELLED
                )
                activeOperations.remove(operationId)
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "cancelOperation"))
    }

    override suspend fun pauseOperation(operationId: Long): Result<Unit> = try {
        operationsMutex.withLock {
            val op = activeOperations[operationId]
            if (op != null) {
                activeOperations[operationId] = op.copy(isPaused = true)
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "pauseOperation"))
    }

    override suspend fun resumeOperation(operationId: Long): Result<Unit> = try {
        operationsMutex.withLock {
            val op = activeOperations[operationId]
            if (op != null) {
                activeOperations[operationId] = op.copy(isPaused = false)
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "resumeOperation"))
    }

    override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> = flow {
        val entries = cacheMutex.withLock {
            if (path != null) {
                val normalizedPath = normalizePath(path)
                cacheEntries.values.filter { entry ->
                    entry.remotePath == normalizedPath ||
                        entry.remotePath.startsWith("$normalizedPath/")
                }
            } else {
                cacheEntries.values.toList()
            }
        }
        emit(entries)
    }

    override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> = try {
        val normalizedPath = normalizePath(remotePath)

        // Read file size outside cache lock to avoid nested lock acquisition
        val fileSize = fileTreeMutex.withLock {
            fileTree[normalizedPath]?.document?.size ?: 0L
        }

        cacheMutex.withLock {
            val existing = cacheEntries[normalizedPath]
            if (existing != null) {
                cacheEntries[normalizedPath] = existing.withPriority(priority).withAccess()
            } else {
                cacheEntries[normalizedPath] = CacheEntry.create(
                    remoteDocumentId = normalizedPath,
                    localPath = "/cache$normalizedPath",
                    remotePath = normalizedPath,
                    size = fileSize,
                    isPinned = false
                ).withPriority(priority)
            }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "addToCache"))
    }

    override suspend fun removeFromCache(remotePath: String): Result<Unit> = try {
        val normalizedPath = normalizePath(remotePath)
        cacheMutex.withLock {
            cacheEntries.remove(normalizedPath)
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "removeFromCache"))
    }

    override suspend fun clearCache(): Result<Unit> = try {
        cacheMutex.withLock {
            cacheEntries.clear()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "clearCache"))
    }

    override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> = flow {
        val statusMap = syncMutex.withLock {
            if (path != null) {
                val normalizedPath = normalizePath(path)
                syncStatusMap.filter { (key, _) ->
                    key == normalizedPath || key.startsWith("$normalizedPath/")
                }
            } else {
                syncStatusMap.toMap()
            }
        }
        emit(statusMap)
    }

    override suspend fun syncFile(
        remotePath: String,
        forceSync: Boolean
    ): Flow<NetworkOperation> = flow {
        val operation = NetworkOperation.createSync(
            id = "sync_$remotePath",
            remotePath = remotePath
        )

        try {
            val normalizedPath = normalizePath(remotePath)

            // Track as active operation
            operationsMutex.withLock {
                activeOperations[operation.id] = operation
            }

            // Update sync status to SYNCING
            syncMutex.withLock {
                syncStatusMap[normalizedPath] = SyncStatus.SYNCING
            }

            emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.0))
            emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 0.5))

            // Mark as synced upon completion
            syncMutex.withLock {
                syncStatusMap[normalizedPath] = SyncStatus.SYNCED
            }

            emit(operation.copy(status = NetworkOperation.Status.IN_PROGRESS, progress = 1.0))

            val completedOp = operation.copy(
                status = NetworkOperation.Status.COMPLETED,
                progress = 1.0
            )
            operationsMutex.withLock { activeOperations.remove(operation.id) }
            emit(completedOp)
        } catch (e: Exception) {
            val normalizedPath = normalizePath(remotePath)
            syncMutex.withLock {
                syncStatusMap[normalizedPath] = SyncStatus.SYNC_ERROR
            }
            operationsMutex.withLock { activeOperations.remove(operation.id) }
            emit(
                operation.copy(
                    status = NetworkOperation.Status.FAILED,
                    error = e.message ?: "Sync failed"
                )
            )
        }
    }

    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {
        // Sync all tracked files in the internal tree
        val paths = fileTreeMutex.withLock { fileTree.keys.toList() }

        for (path in paths) {
            syncMutex.withLock {
                syncStatusMap[path] = SyncStatus.SYNCED
            }
        }

        // Return empty flow as per interface contract for bulk sync
        // Individual files can be synced via syncFile()
    }

    override fun searchFiles(
        query: String,
        path: String?,
        includeContent: Boolean
    ): Flow<Result<List<NetworkDocument>>> = flow {
        emit(Result.failure(Exception("SMB search not implemented")))
    }

    override fun getRecentChanges(
        since: kotlinx.datetime.Instant,
        path: String?
    ): Flow<List<NetworkDocument>> = flow {
        // Return files from the internal tree that were modified after the given instant
        val recentDocs = fileTreeMutex.withLock {
            fileTree.values
                .map { it.document }
                .filter { doc ->
                    val modified = doc.lastModified
                    if (modified != null) {
                        modified >= since
                    } else {
                        false
                    }
                }
                .filter { doc ->
                    if (path != null) {
                        val normalizedPath = normalizePath(path)
                        doc.path == normalizedPath || doc.path.startsWith("$normalizedPath/")
                    } else {
                        true
                    }
                }
        }
        emit(recentDocs)
    }

    override suspend fun getQuotaInfo(): Result<StorageQuota> = try {
        // Calculate used space from internal file tree
        val usedSpace = fileTreeMutex.withLock {
            fileTree.values.sumOf { it.document.size }
        }
        val totalSpace = 1000000000L // 1GB simulated quota
        val actualUsed = if (usedSpace > 0) usedSpace else 100000000L
        val available = totalSpace - actualUsed

        Result.success(
            StorageQuota(
                totalSpace = totalSpace,
                usedSpace = actualUsed,
                availableSpace = available,
                usagePercentage = actualUsed.toDouble() / totalSpace.toDouble(),
                isFull = available <= 0L,
                isLowOnSpace = available < (totalSpace / 10)
            )
        )
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "getQuotaInfo"))
    }

    override suspend fun exists(remotePath: String): Result<Boolean> = try {
        val normalizedPath = normalizePath(remotePath)
        val found = fileTreeMutex.withLock { fileTree.containsKey(normalizedPath) }
        Result.success(found)
    } catch (e: Exception) {
        Result.failure(NetworkStorageException.fromThrowable(e, "exists"))
    }

    override fun getParentPath(remotePath: String): String? {
        if (remotePath == "/" || remotePath.isBlank()) return null
        // Remove trailing slash for consistent handling
        val normalized = remotePath.trimEnd('/')
        if (normalized.isEmpty() || normalized == "/") return null
        val parent = normalized.substringBeforeLast("/")
        return if (parent.isEmpty()) "/" else parent
    }

    override fun validatePath(remotePath: String): Result<Unit> = try {
        if (remotePath.isBlank()) {
            Result.failure(Exception("Path cannot be blank"))
        } else {
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== INTERNAL HELPERS ====================

    /**
     * Normalize a file path for consistent lookup in the internal tree.
     * Removes trailing slashes (unless root) and ensures leading slash.
     */
    private fun normalizePath(path: String): String {
        if (path.isBlank() || path == "/") return path
        val trimmed = path.trimEnd('/')
        return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }

    /**
     * Ensure all parent folders exist in the file tree for the given path.
     * Must be called while holding [fileTreeMutex].
     */
    private fun ensureParentFolders(path: String) {
        val parts = path.trimEnd('/').split('/').filter { it.isNotEmpty() }
        var currentPath = ""
        // Create all parent directories (not including the final component)
        for (i in 0 until parts.size - 1) {
            currentPath += "/${parts[i]}"
            if (!fileTree.containsKey(currentPath)) {
                val folderDoc = NetworkDocument(
                    id = currentPath,
                    name = parts[i],
                    path = currentPath,
                    isFolder = true,
                    size = 0L,
                    lastModified = Clock.System.now(),
                    storageId = storageId,
                    permissions = defaultFolderPermissions,
                    syncStatus = SyncStatus.SYNCED
                )
                fileTree[currentPath] = FileNode(folderDoc)
            }
        }
    }
}
