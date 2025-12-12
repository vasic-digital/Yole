/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Database migration utilities for transitioning from SQLDelight
 *
 *########################################################*/

package digital.vasic.yole.database

import digital.vasic.yole.network.common.*
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Database migration utilities for transitioning from the old SQLDelight schema
 * to the new unified database system
 */
object DatabaseMigration {
    
    /**
     * Version information for migration tracking
     */
    const val CURRENT_VERSION = 1
    const val SQLDELIGHT_VERSION = 0
    
    /**
     * Legacy data structures that match the old SQLDelight schema
     */
    data class LegacyNetworkStorage(
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
    
    data class LegacyNetworkDocument(
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
    
    data class LegacyCacheEntry(
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
    
    data class LegacyNetworkOperation(
        val id: Long,
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
    
    data class LegacySyncStatus(
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
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Convert legacy storage data to new format
     */
    fun convertLegacyStorage(legacy: LegacyNetworkStorage): NetworkStorage {
        return NetworkStorage(
            id = legacy.id,
            name = legacy.name,
            type = StorageType.valueOf(legacy.type),
            location = legacy.location,
            totalSpace = legacy.total_space,
            usedSpace = legacy.used_space,
            isOnline = legacy.is_online,
            lastSync = legacy.last_sync?.let { Instant.fromEpochMilliseconds(it) },
            metadata = legacy.metadata?.let { 
                json.decodeFromString<Map<String, String>>(it) 
            } ?: emptyMap(),
            isEnabled = legacy.is_enabled,
            priority = legacy.priority,
            supportsFolders = legacy.supports_folders,
            supportsMetadata = legacy.supports_metadata,
            maxFileSize = legacy.max_file_size,
            supportedExtensions = legacy.supported_extensions?.let { 
                json.decodeFromString<List<String>>(it) 
            } ?: emptyList()
        )
    }
    
    /**
     * Convert legacy document data to new format
     */
    fun convertLegacyDocument(legacy: LegacyNetworkDocument): NetworkDocument {
        return NetworkDocument(
            id = legacy.id,
            name = legacy.name,
            path = legacy.path,
            isFolder = legacy.is_folder,
            size = legacy.size,
            lastModified = Instant.fromEpochMilliseconds(legacy.last_modified),
            syncStatus = SyncStatus.valueOf(legacy.sync_status),
            documentId = legacy.document_id,
            contentType = legacy.content_type,
            extension = legacy.extension,
            parentPath = legacy.parent_path,
            isSyncing = legacy.is_syncing,
            hasPendingChanges = legacy.has_pending_changes,
            isAvailableOffline = legacy.is_available_offline,
            isReadOnly = legacy.is_read_only,
            isHidden = legacy.is_hidden,
            metadata = legacy.metadata?.let { 
                json.decodeFromString<Map<String, String>>(it) 
            } ?: emptyMap(),
            thumbnails = legacy.thumbnails?.let { 
                json.decodeFromString<List<String>>(it) 
            } ?: emptyList(),
            tags = legacy.tags?.let { 
                json.decodeFromString<List<String>>(it) 
            } ?: emptyList(),
            owner = legacy.owner,
            permissions = legacy.permissions?.let { 
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
    
    /**
     * Convert legacy cache entry data to new format
     */
    fun convertLegacyCacheEntry(legacy: LegacyCacheEntry): CacheEntry {
        return CacheEntry(
            id = legacy.id,
            remoteDocumentId = legacy.remote_document_id,
            localPath = legacy.local_path,
            remotePath = legacy.remote_path,
            size = legacy.size,
            createdAt = Instant.fromEpochMilliseconds(legacy.created_at),
            lastAccessed = Instant.fromEpochMilliseconds(legacy.last_accessed),
            lastModified = Instant.fromEpochMilliseconds(legacy.last_modified),
            expiresAt = legacy.expires_at?.let { Instant.fromEpochMilliseconds(it) },
            isValid = legacy.is_valid,
            isPinned = legacy.is_pinned,
            isInUse = legacy.is_in_use,
            accessCount = legacy.access_count,
            contentType = legacy.content_type,
            checksum = legacy.checksum,
            compression = legacy.compression,
            originalSize = legacy.original_size,
            priority = legacy.priority,
            metadata = legacy.metadata?.let { 
                json.decodeFromString<Map<String, String>>(it) 
            } ?: emptyMap(),
            tags = legacy.tags?.let { 
                json.decodeFromString<List<String>>(it) 
            } ?: emptyList()
        )
    }
    
    /**
     * Convert legacy operation data to new format
     */
    fun convertLegacyOperation(legacy: LegacyNetworkOperation): NetworkOperation {
        return NetworkOperation(
            id = legacy.id,
            type = OperationType.valueOf(legacy.type),
            remotePath = legacy.remote_path,
            localPath = legacy.local_path,
            status = OperationStatus.valueOf(legacy.status),
            progress = legacy.progress,
            totalSize = legacy.total_size,
            bytesTransferred = legacy.bytes_transferred,
            createdAt = Instant.fromEpochMilliseconds(legacy.created_at),
            startedAt = legacy.started_at?.let { Instant.fromEpochMilliseconds(it) },
            completedAt = legacy.completed_at?.let { Instant.fromEpochMilliseconds(it) },
            errorMessage = legacy.error_message,
            retryCount = legacy.retry_count,
            maxRetries = legacy.max_retries,
            priority = legacy.priority,
            canPause = legacy.can_pause,
            canCancel = legacy.can_cancel,
            isPaused = legacy.is_paused,
            estimatedTimeRemaining = legacy.estimated_time_remaining?.let { 
                kotlin.time.Duration.milliseconds(it) 
            },
            transferSpeed = legacy.transfer_speed,
            metadata = legacy.metadata?.let { 
                json.decodeFromString<Map<String, String>>(it) 
            } ?: emptyMap()
        )
    }
    
    /**
     * Convert legacy sync status data to new format
     */
    fun convertLegacySyncStatus(legacy: LegacySyncStatus): Pair<String, SyncStatus> {
        return legacy.remote_path to SyncStatus.valueOf(legacy.status)
    }
    
    /**
     * Migrate data from old SQLDelight database to new unified database
     */
    suspend fun migrateFromSQLDelight(
        legacyData: LegacyDatabaseData,
        newDatabase: DatabaseInterface
    ): Result<MigrationResult> {
        return try {
            newDatabase.transaction {
                // Convert and migrate storage
                val migratedStorage = mutableListOf<NetworkStorage>()
                legacyData.storage.forEach { legacyStorage ->
                    val converted = convertLegacyStorage(legacyStorage)
                    newDatabase.insertStorage(converted).getOrThrow()
                    migratedStorage.add(converted)
                }
                
                // Convert and migrate documents
                val migratedDocuments = mutableListOf<NetworkDocument>()
                legacyData.documents.forEach { legacyDocument ->
                    val converted = convertLegacyDocument(legacyDocument)
                    newDatabase.insertDocument(converted).getOrThrow()
                    migratedDocuments.add(converted)
                }
                
                // Convert and migrate cache entries
                val migratedCacheEntries = mutableListOf<CacheEntry>()
                legacyData.cacheEntries.forEach { legacyCacheEntry ->
                    val converted = convertLegacyCacheEntry(legacyCacheEntry)
                    newDatabase.insertCacheEntry(converted).getOrThrow()
                    migratedCacheEntries.add(converted)
                }
                
                // Convert and migrate operations
                val migratedOperations = mutableListOf<NetworkOperation>()
                legacyData.operations.forEach { legacyOperation ->
                    val converted = convertLegacyOperation(legacyOperation)
                    newDatabase.insertOperation(converted).getOrThrow()
                    migratedOperations.add(converted)
                }
                
                // Convert and migrate sync status
                val migratedSyncStatus = mutableMapOf<String, SyncStatus>()
                legacyData.syncStatus.forEach { legacySyncStatus ->
                    val (path, status) = convertLegacySyncStatus(legacySyncStatus)
                    newDatabase.updateSyncStatus(path, status).getOrThrow()
                    migratedSyncStatus[path] = status
                }
                
                MigrationResult(
                    success = true,
                    migratedStorage = migratedStorage,
                    migratedDocuments = migratedDocuments,
                    migratedCacheEntries = migratedCacheEntries,
                    migratedOperations = migratedOperations,
                    migratedSyncStatus = migratedSyncStatus,
                    errors = emptyList()
                )
            }.getOrThrow()
        } catch (e: Exception) {
            MigrationResult(
                success = false,
                migratedStorage = emptyList(),
                migratedDocuments = emptyList(),
                migratedCacheEntries = emptyList(),
                migratedOperations = emptyList(),
                migratedSyncStatus = emptyMap(),
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }
    
    /**
     * Validate that legacy data can be migrated
     */
    fun validateLegacyData(legacyData: LegacyDatabaseData): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate storage
        legacyData.storage.forEach { storage ->
            if (storage.id.isBlank()) {
                errors.add("Storage with blank ID found")
            }
            if (storage.name.isBlank()) {
                errors.add("Storage '${storage.id}' has blank name")
            }
            try {
                StorageType.valueOf(storage.type)
            } catch (e: IllegalArgumentException) {
                errors.add("Storage '${storage.id}' has invalid type: ${storage.type}")
            }
        }
        
        // Validate documents
        legacyData.documents.forEach { document ->
            if (document.id.isBlank()) {
                errors.add("Document with blank ID found")
            }
            if (document.name.isBlank()) {
                errors.add("Document '${document.id}' has blank name")
            }
            if (document.storage_id.isBlank()) {
                errors.add("Document '${document.id}' has blank storage_id")
            }
            if (legacyData.storage.none { it.id == document.storage_id }) {
                errors.add("Document '${document.id}' references non-existent storage '${document.storage_id}'")
            }
            try {
                SyncStatus.valueOf(document.sync_status)
            } catch (e: IllegalArgumentException) {
                errors.add("Document '${document.id}' has invalid sync_status: ${document.sync_status}")
            }
        }
        
        // Validate cache entries
        legacyData.cacheEntries.forEach { cacheEntry ->
            if (cacheEntry.id.isBlank()) {
                errors.add("Cache entry with blank ID found")
            }
            if (cacheEntry.remote_document_id.isBlank()) {
                errors.add("Cache entry '${cacheEntry.id}' has blank remote_document_id")
            }
            if (legacyData.documents.none { it.id == cacheEntry.remote_document_id }) {
                errors.add("Cache entry '${cacheEntry.id}' references non-existent document '${cacheEntry.remote_document_id}'")
            }
        }
        
        // Validate operations
        legacyData.operations.forEach { operation ->
            if (operation.id < 0) {
                errors.add("Operation with negative ID: ${operation.id}")
            }
            try {
                OperationType.valueOf(operation.type)
            } catch (e: IllegalArgumentException) {
                errors.add("Operation '${operation.id}' has invalid type: ${operation.type}")
            }
            try {
                OperationStatus.valueOf(operation.status)
            } catch (e: IllegalArgumentException) {
                errors.add("Operation '${operation.id}' has invalid status: ${operation.status}")
            }
        }
        
        // Validate sync status
        legacyData.syncStatus.forEach { syncStatus ->
            try {
                SyncStatus.valueOf(syncStatus.status)
            } catch (e: IllegalArgumentException) {
                errors.add("Sync status for '${syncStatus.remote_path}' has invalid status: ${syncStatus.status}")
            }
        }
        
        // Add warnings for potential issues
        if (legacyData.storage.isEmpty()) {
            warnings.add("No storage configurations found")
        }
        if (legacyData.documents.isEmpty()) {
            warnings.add("No documents found")
        }
        if (legacyData.storage.size > 1000) {
            warnings.add("Large number of storage configurations (${legacyData.storage.size}) may slow down migration")
        }
        if (legacyData.documents.size > 10000) {
            warnings.add("Large number of documents (${legacyData.documents.size}) may slow down migration")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Generate a migration report
     */
    fun generateMigrationReport(
        legacyData: LegacyDatabaseData,
        validationResult: ValidationResult,
        migrationResult: MigrationResult
    ): MigrationReport {
        return MigrationReport(
            timestamp = kotlinx.datetime.Clock.System.now(),
            legacyVersion = SQLDELIGHT_VERSION,
            newVersion = CURRENT_VERSION,
            sourceDataStats = DataStats(
                storageCount = legacyData.storage.size.toLong(),
                documentCount = legacyData.documents.size.toLong(),
                cacheEntryCount = legacyData.cacheEntries.size.toLong(),
                operationCount = legacyData.operations.size.toLong(),
                syncStatusCount = legacyData.syncStatus.size.toLong()
            ),
            migratedDataStats = DataStats(
                storageCount = migrationResult.migratedStorage.size.toLong(),
                documentCount = migrationResult.migratedDocuments.size.toLong(),
                cacheEntryCount = migrationResult.migratedCacheEntries.size.toLong(),
                operationCount = migrationResult.migratedOperations.size.toLong(),
                syncStatusCount = migrationResult.migratedSyncStatus.size.toLong()
            ),
            validationResult = validationResult,
            migrationResult = migrationResult,
            success = migrationResult.success && validationResult.isValid
        )
    }
}

/**
 * Legacy database data container
 */
data class LegacyDatabaseData(
    val storage: List<LegacyNetworkStorage>,
    val documents: List<LegacyNetworkDocument>,
    val cacheEntries: List<LegacyCacheEntry>,
    val operations: List<LegacyNetworkOperation>,
    val syncStatus: List<LegacySyncStatus>
)

/**
 * Migration result
 */
data class MigrationResult(
    val success: Boolean,
    val migratedStorage: List<NetworkStorage>,
    val migratedDocuments: List<NetworkDocument>,
    val migratedCacheEntries: List<CacheEntry>,
    val migratedOperations: List<NetworkOperation>,
    val migratedSyncStatus: Map<String, SyncStatus>,
    val errors: List<String>
)

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Data statistics
 */
data class DataStats(
    val storageCount: Long,
    val documentCount: Long,
    val cacheEntryCount: Long,
    val operationCount: Long,
    val syncStatusCount: Long
)

/**
 * Migration report
 */
data class MigrationReport(
    val timestamp: kotlinx.datetime.Instant,
    val legacyVersion: Int,
    val newVersion: Int,
    val sourceDataStats: DataStats,
    val migratedDataStats: DataStats,
    val validationResult: ValidationResult,
    val migrationResult: MigrationResult,
    val success: Boolean
)