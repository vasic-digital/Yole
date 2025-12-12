/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Database models for cross-platform database implementation
 *
 *########################################################*/

package digital.vasic.yole.database

import digital.vasic.yole.network.common.*
import digital.vasic.yole.model.Document
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Database document entity
 */
@Serializable
data class DatabaseDocument(
    val id: String,
    val name: String,
    val content: String,
    val format: String,
    val size: Long,
    val lastModified: Instant,
    val createdAt: Instant,
    val isDeleted: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) {
    fun toDocument(): Document = Document(
        name = name,
        content = content,
        format = format
    )
}

/**
 * Database storage entity
 */
@Serializable
data class DatabaseStorage(
    val id: String,
    val name: String,
    val type: String,
    val location: String,
    val totalSpace: Long?,
    val usedSpace: Long?,
    val isOnline: Boolean,
    val lastSync: Instant?,
    val metadata: Map<String, String> = emptyMap(),
    val isEnabled: Boolean,
    val priority: Int,
    val supportsFolders: Boolean,
    val supportsMetadata: Boolean,
    val maxFileSize: Long?,
    val supportedExtensions: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun toNetworkStorage(): NetworkStorage = NetworkStorage(
        id = id,
        name = name,
        type = StorageType.valueOf(type),
        location = location,
        totalSpace = totalSpace,
        usedSpace = usedSpace,
        isOnline = isOnline,
        lastSync = lastSync,
        metadata = metadata,
        isEnabled = isEnabled,
        priority = priority,
        supportsFolders = supportsFolders,
        supportsMetadata = supportsMetadata,
        maxFileSize = maxFileSize,
        supportedExtensions = supportedExtensions
    )
}

/**
 * Database network document entity
 */
@Serializable
data class DatabaseNetworkDocument(
    val id: String,
    val storageId: String,
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val size: Long,
    val lastModified: Instant,
    val syncStatus: String,
    val documentId: String?,
    val contentType: String?,
    val extension: String,
    val parentPath: String,
    val isSyncing: Boolean,
    val hasPendingChanges: Boolean,
    val isAvailableOffline: Boolean,
    val isReadOnly: Boolean,
    val isHidden: Boolean,
    val metadata: Map<String, String> = emptyMap(),
    val thumbnails: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val owner: String?,
    val permissions: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun toNetworkDocument(): NetworkDocument = NetworkDocument(
        id = id,
        name = name,
        path = path,
        isFolder = isFolder,
        size = size,
        lastModified = lastModified,
        syncStatus = SyncStatus.valueOf(syncStatus),
        documentId = documentId,
        contentType = contentType,
        extension = extension,
        parentPath = parentPath,
        isSyncing = isSyncing,
        hasPendingChanges = hasPendingChanges,
        isAvailableOffline = isAvailableOffline,
        isReadOnly = isReadOnly,
        isHidden = isHidden,
        metadata = metadata,
        thumbnails = thumbnails,
        tags = tags,
        owner = owner,
        permissions = permissions.mapNotNull { perm ->
            try {
                DocumentPermission.valueOf(perm)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()
    )
}

/**
 * Database cache entry entity
 */
@Serializable
data class DatabaseCacheEntry(
    val id: String,
    val remoteDocumentId: String,
    val localPath: String,
    val remotePath: String,
    val size: Long,
    val createdAt: Instant,
    val lastAccessed: Instant,
    val lastModified: Instant,
    val expiresAt: Instant?,
    val isValid: Boolean,
    val isPinned: Boolean,
    val isInUse: Boolean,
    val accessCount: Int,
    val contentType: String?,
    val checksum: String?,
    val compression: String?,
    val originalSize: Long?,
    val priority: Int,
    val metadata: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList()
) {
    fun toCacheEntry(): CacheEntry = CacheEntry(
        id = id,
        remoteDocumentId = remoteDocumentId,
        localPath = localPath,
        remotePath = remotePath,
        size = size,
        createdAt = createdAt,
        lastAccessed = lastAccessed,
        lastModified = lastModified,
        expiresAt = expiresAt,
        isValid = isValid,
        isPinned = isPinned,
        isInUse = isInUse,
        accessCount = accessCount,
        contentType = contentType,
        checksum = checksum,
        compression = compression,
        originalSize = originalSize,
        priority = priority,
        metadata = metadata,
        tags = tags
    )
}

/**
 * Database operation entity
 */
@Serializable
data class DatabaseOperation(
    val id: Long,
    val type: String,
    val remotePath: String,
    val localPath: String?,
    val status: String,
    val progress: Double,
    val totalSize: Long,
    val bytesTransferred: Long,
    val createdAt: Instant,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val errorMessage: String?,
    val retryCount: Int,
    val maxRetries: Int,
    val priority: Int,
    val canPause: Boolean,
    val canCancel: Boolean,
    val isPaused: Boolean,
    val estimatedTimeRemaining: Long?,
    val transferSpeed: Long?,
    val metadata: Map<String, String> = emptyMap()
) {
    fun toNetworkOperation(): NetworkOperation = NetworkOperation(
        id = id,
        type = OperationType.valueOf(type),
        remotePath = remotePath,
        localPath = localPath,
        status = OperationStatus.valueOf(status),
        progress = progress,
        totalSize = totalSize,
        bytesTransferred = bytesTransferred,
        createdAt = createdAt,
        startedAt = startedAt,
        completedAt = completedAt,
        errorMessage = errorMessage,
        retryCount = retryCount,
        maxRetries = maxRetries,
        priority = priority,
        canPause = canPause,
        canCancel = canCancel,
        isPaused = isPaused,
        estimatedTimeRemaining = estimatedTimeRemaining?.let { 
            kotlin.time.Duration.milliseconds(it) 
        },
        transferSpeed = transferSpeed,
        metadata = metadata
    )
}

/**
 * Database sync status entity
 */
@Serializable
data class DatabaseSyncStatus(
    val remotePath: String,
    val status: String,
    val lastSyncTime: Instant?,
    val nextSyncTime: Instant?,
    val errorMessage: String?,
    val retryCount: Int,
    val maxRetries: Int,
    val progress: Double,
    val estimatedTimeRemaining: Long?,
    val dataSize: Long,
    val bytesTransferred: Long,
    val isAutomatic: Boolean,
    val isAvailableOffline: Boolean,
    val hasConflicts: Boolean,
    val conflictResolution: String?,
    val metadata: Map<String, String> = emptyMap(),
    val updatedAt: Instant
) {
    fun toPair(): Pair<String, SyncStatus> = remotePath to SyncStatus.valueOf(status)
}

/**
 * Database settings entity
 */
@Serializable
data class DatabaseSetting(
    val key: String,
    val value: String,
    val updatedAt: Instant
)

/**
 * Database info entity
 */
@Serializable
data class DatabaseInfo(
    val version: Int,
    val size: Long,
    val documentCount: Long,
    val storageCount: Long,
    val cacheEntryCount: Long,
    val operationCount: Long,
    val isHealthy: Boolean,
    val lastVacuum: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)