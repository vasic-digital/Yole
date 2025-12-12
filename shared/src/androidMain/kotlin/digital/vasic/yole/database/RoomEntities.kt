/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Room database entities for Android implementation
 *
 *########################################################*/

package digital.vasic.yole.database

import androidx.room.*
import digital.vasic.yole.network.common.*
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// NetworkStorage Entity
@Entity(tableName = "network_storage")
data class StorageEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val location: String,
    val total_space: Long?,
    val used_space: Long?,
    val is_online: Boolean,
    val last_sync: Long?,
    val metadata: String?, // JSON string
    val is_enabled: Boolean,
    val priority: Int,
    val supports_folders: Boolean,
    val supports_metadata: Boolean,
    val max_file_size: Long?,
    val supported_extensions: String?, // JSON array
    val created_at: Long,
    val updated_at: Long
)

// NetworkDocument Entity
@Entity(
    tableName = "network_document",
    foreignKeys = [
        ForeignKey(
            entity = StorageEntity::class,
            parentColumns = ["id"],
            childColumns = ["storage_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("storage_id"),
        Index("path"),
        Index("parent_path"),
        Index("sync_status")
    ]
)
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val storage_id: String,
    val name: String,
    val path: String,
    val is_folder: Boolean,
    val size: Long,
    val last_modified: Long,
    val sync_status: String,
    val document_id: String?,
    val content_type: String?,
    val extension: String,
    val parent_path: String,
    val is_syncing: Boolean,
    val has_pending_changes: Boolean,
    val is_available_offline: Boolean,
    val is_read_only: Boolean,
    val is_hidden: Boolean,
    val metadata: String?, // JSON string
    val thumbnails: String?, // JSON array
    val tags: String?, // JSON array
    val owner: String?,
    val permissions: String?, // JSON array
    val created_at: Long,
    val updated_at: Long
)

// CacheEntry Entity
@Entity(
    tableName = "cache_entry",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["remote_document_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("remote_document_id"),
        Index("remote_path"),
        Index("created_at"),
        Index("last_accessed"),
        Index("expires_at")
    ]
)
data class CacheEntryEntity(
    @PrimaryKey
    val id: String,
    val remote_document_id: String,
    val local_path: String,
    val remote_path: String,
    val size: Long,
    val created_at: Long,
    val last_accessed: Long,
    val last_modified: Long,
    val expires_at: Long?,
    val is_valid: Boolean,
    val is_pinned: Boolean,
    val is_in_use: Boolean,
    val access_count: Int,
    val content_type: String?,
    val checksum: String?,
    val compression: String?,
    val original_size: Long?,
    val priority: Int,
    val metadata: String?, // JSON string
    val tags: String? // JSON array
)

// NetworkOperation Entity
@Entity(
    tableName = "network_operation",
    indices = [
        Index("status"),
        Index("created_at")
    ]
)
data class OperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val remote_path: String,
    val local_path: String?,
    val status: String,
    val progress: Double,
    val total_size: Long,
    val bytes_transferred: Long,
    val created_at: Long,
    val started_at: Long?,
    val completed_at: Long?,
    val error_message: String?,
    val retry_count: Int,
    val max_retries: Int,
    val priority: Int,
    val can_pause: Boolean,
    val can_cancel: Boolean,
    val is_paused: Boolean,
    val estimated_time_remaining: Long?,
    val transfer_speed: Long?,
    val metadata: String? // JSON string
)

// SyncStatus Entity
@Entity(tableName = "sync_status", primaryKeys = ["remote_path"])
data class SyncStatusEntity(
    val remote_path: String,
    val status: String,
    val last_sync_time: Long?,
    val next_sync_time: Long?,
    val error_message: String?,
    val retry_count: Int,
    val max_retries: Int,
    val progress: Double,
    val estimated_time_remaining: Long?,
    val data_size: Long,
    val bytes_transferred: Long,
    val is_automatic: Boolean,
    val is_available_offline: Boolean,
    val has_conflicts: Boolean,
    val conflict_resolution: String?,
    val metadata: String? // JSON string
)

// Setting Entity
@Entity(tableName = "settings", primaryKeys = ["key"])
data class SettingEntity(
    val key: String,
    val value: String
)

// Extension functions to convert between models and entities

fun NetworkStorage.toEntity(json: Json): StorageEntity {
    return StorageEntity(
        id = id,
        name = name,
        type = type.name,
        location = location,
        total_space = totalSpace,
        used_space = usedSpace,
        is_online = isOnline,
        last_sync = lastSync?.toEpochMilliseconds(),
        metadata = json.encodeToString(metadata),
        is_enabled = isEnabled,
        priority = priority,
        supports_folders = supportsFolders,
        supports_metadata = supportsMetadata,
        max_file_size = maxFileSize,
        supported_extensions = json.encodeToString(supportedExtensions),
        created_at = currentTimeMillis(),
        updated_at = currentTimeMillis()
    )
}

fun StorageEntity.toModel(json: Json): NetworkStorage {
    return NetworkStorage(
        id = id,
        name = name,
        type = StorageType.valueOf(type),
        location = location,
        totalSpace = total_space,
        usedSpace = used_space,
        isOnline = is_online,
        lastSync = last_sync?.let { Instant.fromEpochMilliseconds(it) },
        metadata = metadata?.let { json.decodeFromString<Map<String, String>>(it) } ?: emptyMap(),
        isEnabled = is_enabled,
        priority = priority,
        supportsFolders = supports_folders,
        supportsMetadata = supports_metadata,
        maxFileSize = max_file_size,
        supportedExtensions = supported_extensions?.let { 
            json.decodeFromString<List<String>>(it) 
        } ?: emptyList()
    )
}

fun NetworkDocument.toEntity(json: Json): DocumentEntity {
    return DocumentEntity(
        id = id,
        storage_id = id, // Note: This should be the actual storage ID, not document ID
        name = name,
        path = path,
        is_folder = isFolder,
        size = size,
        last_modified = lastModified.toEpochMilliseconds(),
        sync_status = syncStatus.name,
        document_id = documentId,
        content_type = contentType,
        extension = extension,
        parent_path = parentPath,
        is_syncing = isSyncing,
        has_pending_changes = hasPendingChanges,
        is_available_offline = isAvailableOffline,
        is_read_only = isReadOnly,
        is_hidden = isHidden,
        metadata = json.encodeToString(metadata),
        thumbnails = json.encodeToString(thumbnails),
        tags = json.encodeToString(tags),
        owner = owner,
        permissions = json.encodeToString(permissions.map { it.name }),
        created_at = currentTimeMillis(),
        updated_at = currentTimeMillis()
    )
}

fun DocumentEntity.toModel(json: Json): NetworkDocument {
    return NetworkDocument(
        id = id,
        name = name,
        path = path,
        isFolder = is_folder,
        size = size,
        lastModified = Instant.fromEpochMilliseconds(last_modified),
        syncStatus = SyncStatus.valueOf(sync_status),
        documentId = document_id,
        contentType = content_type,
        extension = extension,
        parentPath = parent_path,
        isSyncing = is_syncing,
        hasPendingChanges = has_pending_changes,
        isAvailableOffline = is_available_offline,
        isReadOnly = is_read_only,
        isHidden = is_hidden,
        metadata = metadata?.let { json.decodeFromString<Map<String, String>>(it) } ?: emptyMap(),
        thumbnails = thumbnails?.let { json.decodeFromString<List<String>>(it) } ?: emptyList(),
        tags = tags?.let { json.decodeFromString<List<String>>(it) } ?: emptyList(),
        owner = owner,
        permissions = permissions?.let { 
            json.decodeFromString<List<String>>(it).mapNotNull { perm ->
                try {
                    DocumentPermission.valueOf(perm)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()
        } ?: emptySet()
    )
}

fun CacheEntry.toEntity(json: Json): CacheEntryEntity {
    return CacheEntryEntity(
        id = id,
        remote_document_id = remoteDocumentId,
        local_path = localPath,
        remote_path = remotePath,
        size = size,
        created_at = createdAt.toEpochMilliseconds(),
        last_accessed = lastAccessed.toEpochMilliseconds(),
        last_modified = lastModified.toEpochMilliseconds(),
        expires_at = expiresAt?.toEpochMilliseconds(),
        is_valid = isValid,
        is_pinned = isPinned,
        is_in_use = isInUse,
        access_count = accessCount,
        content_type = contentType,
        checksum = checksum,
        compression = compression,
        original_size = originalSize,
        priority = priority,
        metadata = json.encodeToString(metadata),
        tags = json.encodeToString(tags)
    )
}

fun CacheEntryEntity.toModel(json: Json): CacheEntry {
    return CacheEntry(
        id = id,
        remoteDocumentId = remote_document_id,
        localPath = local_path,
        remotePath = remote_path,
        size = size,
        createdAt = Instant.fromEpochMilliseconds(created_at),
        lastAccessed = Instant.fromEpochMilliseconds(last_accessed),
        lastModified = Instant.fromEpochMilliseconds(last_modified),
        expiresAt = expires_at?.let { Instant.fromEpochMilliseconds(it) },
        isValid = is_valid,
        isPinned = is_pinned,
        isInUse = is_in_use,
        accessCount = access_count,
        contentType = content_type,
        checksum = checksum,
        compression = compression,
        originalSize = original_size,
        priority = priority,
        metadata = metadata?.let { json.decodeFromString<Map<String, String>>(it) } ?: emptyMap(),
        tags = tags?.let { json.decodeFromString<List<String>>(it) } ?: emptyList()
    )
}

fun NetworkOperation.toEntity(json: Json): OperationEntity {
    return OperationEntity(
        id = if (id > 0) id else 0, // Let Room auto-generate if id is 0 or negative
        type = type.name,
        remote_path = remotePath,
        local_path = localPath,
        status = status.name,
        progress = progress,
        total_size = totalSize,
        bytes_transferred = bytesTransferred,
        created_at = createdAt.toEpochMilliseconds(),
        started_at = startedAt?.toEpochMilliseconds(),
        completed_at = completedAt?.toEpochMilliseconds(),
        error_message = errorMessage,
        retry_count = retryCount,
        max_retries = maxRetries,
        priority = priority,
        can_pause = canPause,
        can_cancel = canCancel,
        is_paused = isPaused,
        estimated_time_remaining = estimatedTimeRemaining?.inWholeMilliseconds,
        transfer_speed = transferSpeed,
        metadata = metadata?.let { json.encodeToString(it) }
    )
}

fun OperationEntity.toModel(json: Json): NetworkOperation {
    return NetworkOperation(
        id = id,
        type = OperationType.valueOf(type),
        remotePath = remote_path,
        localPath = local_path,
        status = OperationStatus.valueOf(status),
        progress = progress,
        totalSize = total_size,
        bytesTransferred = bytes_transferred,
        createdAt = Instant.fromEpochMilliseconds(created_at),
        startedAt = started_at?.let { Instant.fromEpochMilliseconds(it) },
        completedAt = completed_at?.let { Instant.fromEpochMilliseconds(it) },
        errorMessage = error_message,
        retryCount = retry_count,
        maxRetries = max_retries,
        priority = priority,
        canPause = can_pause,
        canCancel = can_cancel,
        isPaused = is_paused,
        estimatedTimeRemaining = estimated_time_remaining?.let { kotlin.time.Duration.milliseconds(it) },
        transferSpeed = transfer_speed,
        metadata = metadata?.let { json.decodeFromString<Map<String, String>>(it) } ?: emptyMap()
    )
}

private fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}