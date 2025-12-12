/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Database service that integrates with the network system
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import digital.vasic.yole.network.common.NetworkStorage
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.CacheEntry
import digital.vasic.yole.network.common.NetworkOperation
import digital.vasic.yole.network.common.SyncStatus

/**
 * Database service that provides high-level database operations
 * and integrates with the existing network system
 */
class DatabaseService(
    private val database: DatabaseInterface
) {
    
    /**
     * Initialize the database service
     */
    suspend fun initialize(): Result<Unit> {
        return database.initialize()
    }
    
    /**
     * Close the database service
     */
    suspend fun close(): Result<Unit> {
        return database.close()
    }
    
    /**
     * Check if the database is ready
     */
    suspend fun isReady(): Boolean {
        return database.isReady()
    }
    
    // Storage management
    
    /**
     * Add or update a storage configuration
     */
    suspend fun saveStorage(storage: NetworkStorage): Result<Unit> {
        return database.getStorage(storage.id).fold(
            onSuccess = { existing ->
                if (existing != null) {
                    database.updateStorage(storage)
                } else {
                    database.insertStorage(storage)
                }
            },
            onFailure = {
                database.insertStorage(storage)
            }
        )
    }
    
    /**
     * Get storage by ID
     */
    suspend fun getStorage(id: String): Result<NetworkStorage?> {
        return database.getStorage(id)
    }
    
    /**
     * Get all storage configurations
     */
    suspend fun getAllStorage(): Result<List<NetworkStorage>> {
        return database.getAllStorage()
    }
    
    /**
     * Get enabled storage configurations
     */
    suspend fun getEnabledStorage(): Result<List<NetworkStorage>> {
        return database.getEnabledStorage()
    }
    
    /**
     * Get storage by type
     */
    suspend fun getStorageByType(type: StorageType): Result<List<NetworkStorage>> {
        return database.getStorageByType(type.name)
    }
    
    /**
     * Remove storage configuration
     */
    suspend fun removeStorage(id: String): Result<Unit> {
        return database.deleteStorage(id)
    }
    
    // Document management
    
    /**
     * Add or update a document
     */
    suspend fun saveDocument(document: NetworkDocument): Result<Unit> {
        return database.getDocument(document.id).fold(
            onSuccess = { existing ->
                if (existing != null) {
                    database.updateDocument(document)
                } else {
                    database.insertDocument(document)
                }
            },
            onFailure = {
                database.insertDocument(document)
            }
        )
    }
    
    /**
     * Get document by ID
     */
    suspend fun getDocument(id: String): Result<NetworkDocument?> {
        return database.getDocument(id)
    }
    
    /**
     * Get documents by storage
     */
    suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>> {
        return database.getDocumentsByStorage(storageId)
    }
    
    /**
     * Get documents by path
     */
    suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>> {
        return database.getDocumentsByPath(path)
    }
    
    /**
     * Get documents by parent path
     */
    suspend fun getDocumentsByParentPath(parentPath: String): Result<List<NetworkDocument>> {
        return database.getDocumentsByParentPath(parentPath)
    }
    
    /**
     * Search documents
     */
    suspend fun searchDocuments(query: String): Result<List<NetworkDocument>> {
        return database.searchDocuments(query)
    }
    
    /**
     * Get documents by sync status
     */
    suspend fun getDocumentsBySyncStatus(status: SyncStatus): Result<List<NetworkDocument>> {
        return database.getDocumentsBySyncStatus(status)
    }
    
    /**
     * Remove document
     */
    suspend fun removeDocument(id: String): Result<Unit> {
        return database.deleteDocument(id)
    }
    
    /**
     * Observe documents by storage
     */
    fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>> {
        return database.observeDocumentsByStorage(storageId)
    }
    
    /**
     * Observe all documents
     */
    fun observeAllDocuments(): Flow<List<NetworkDocument>> {
        return database.observeAllDocuments()
    }
    
    // Cache management
    
    /**
     * Add or update a cache entry
     */
    suspend fun saveCacheEntry(entry: CacheEntry): Result<Unit> {
        return database.getCacheEntry(entry.id).fold(
            onSuccess = { existing ->
                if (existing != null) {
                    database.updateCacheEntry(entry)
                } else {
                    database.insertCacheEntry(entry)
                }
            },
            onFailure = {
                database.insertCacheEntry(entry)
            }
        )
    }
    
    /**
     * Get cache entry by ID
     */
    suspend fun getCacheEntry(id: String): Result<CacheEntry?> {
        return database.getCacheEntry(id)
    }
    
    /**
     * Get cache entries by document
     */
    suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>> {
        return database.getCacheEntriesByDocument(documentId)
    }
    
    /**
     * Get all cache entries
     */
    suspend fun getAllCacheEntries(): Result<List<CacheEntry>> {
        return database.getAllCacheEntries()
    }
    
    /**
     * Remove cache entry
     */
    suspend fun removeCacheEntry(id: String): Result<Unit> {
        return database.deleteCacheEntry(id)
    }
    
    /**
     * Clean up expired cache entries
     */
    suspend fun cleanupExpiredCache(): Result<Int> {
        return database.deleteExpiredCacheEntries()
    }
    
    /**
     * Get total cache usage
     */
    suspend fun getCacheUsage(): Result<Long> {
        return database.getCacheUsage()
    }
    
    /**
     * Evict cache entries to stay within size limit
     */
    suspend fun evictCacheEntries(maxSize: Long): Result<Int> {
        return database.evictCacheEntries(maxSize)
    }
    
    // Operation management
    
    /**
     * Add or update an operation
     */
    suspend fun saveOperation(operation: NetworkOperation): Result<Unit> {
        return database.getOperation(operation.id).fold(
            onSuccess = { existing ->
                if (existing != null) {
                    database.updateOperation(operation)
                } else {
                    database.insertOperation(operation)
                }
            },
            onFailure = {
                database.insertOperation(operation)
            }
        )
    }
    
    /**
     * Get operation by ID
     */
    suspend fun getOperation(id: Long): Result<NetworkOperation?> {
        return database.getOperation(id)
    }
    
    /**
     * Get active operations
     */
    suspend fun getActiveOperations(): Result<List<NetworkOperation>> {
        return database.getActiveOperations()
    }
    
    /**
     * Get operations by status
     */
    suspend fun getOperationsByStatus(status: OperationStatus): Result<List<NetworkOperation>> {
        return database.getOperationsByStatus(status.name)
    }
    
    /**
     * Remove operation
     */
    suspend fun removeOperation(id: Long): Result<Unit> {
        return database.deleteOperation(id)
    }
    
    /**
     * Clear completed operations
     */
    suspend fun clearCompletedOperations(): Result<Int> {
        return database.clearCompletedOperations()
    }
    
    /**
     * Get operation count by status
     */
    suspend fun getOperationCountByStatus(status: OperationStatus): Result<Int> {
        return database.getOperationCountByStatus(status.name)
    }
    
    // Sync status management
    
    /**
     * Update sync status for a remote path
     */
    suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit> {
        return database.updateSyncStatus(remotePath, status)
    }
    
    /**
     * Get sync status for a remote path
     */
    suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?> {
        return database.getSyncStatus(remotePath)
    }
    
    /**
     * Get all sync status entries
     */
    suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>> {
        return database.getAllSyncStatus()
    }
    
    /**
     * Remove sync status
     */
    suspend fun removeSyncStatus(remotePath: String): Result<Unit> {
        return database.deleteSyncStatus(remotePath)
    }
    
    /**
     * Get sync status by pattern
     */
    suspend fun getSyncStatusByPattern(pattern: String): Result<Map<String, SyncStatus>> {
        return database.getSyncStatusByPattern(pattern)
    }
    
    // Settings management
    
    /**
     * Set a setting
     */
    suspend fun setSetting(key: String, value: String): Result<Unit> {
        return database.setSetting(key, value)
    }
    
    /**
     * Get a setting
     */
    suspend fun getSetting(key: String): Result<String?> {
        return database.getSetting(key)
    }
    
    /**
     * Get all settings
     */
    suspend fun getAllSettings(): Result<Map<String, String>> {
        return database.getAllSettings()
    }
    
    /**
     * Remove a setting
     */
    suspend fun removeSetting(key: String): Result<Unit> {
        return database.deleteSetting(key)
    }
    
    /**
     * Set multiple settings
     */
    suspend fun setSettings(settings: Map<String, String>): Result<Unit> {
        return database.setSettingBulk(settings)
    }
    
    // Document metadata management
    
    /**
     * Set document metadata
     */
    suspend fun setDocumentMetadata(documentId: String, metadata: Map<String, String>): Result<Unit> {
        return database.setDocumentMetadata(documentId, metadata)
    }
    
    /**
     * Get document metadata
     */
    suspend fun getDocumentMetadata(documentId: String): Result<Map<String, String>> {
        return database.getDocumentMetadata(documentId)
    }
    
    /**
     * Search document metadata
     */
    suspend fun searchDocumentMetadata(query: String): Result<List<String>> {
        return database.searchDocumentMetadata(query)
    }
    
    /**
     * Remove document metadata
     */
    suspend fun removeDocumentMetadata(documentId: String): Result<Unit> {
        return database.deleteDocumentMetadata(documentId)
    }
    
    // Database maintenance
    
    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): Result<DatabaseStats> {
        return database.getDatabaseStats()
    }
    
    /**
     * Vacuum the database
     */
    suspend fun vacuum(): Result<Unit> {
        return database.vacuum()
    }
    
    /**
     * Clear all data
     */
    suspend fun clearAll(): Result<Unit> {
        return database.clearAll()
    }
    
    /**
     * Clear a specific table
     */
    suspend fun clearTable(tableName: String): Result<Unit> {
        return database.clearTable(tableName)
    }
    
    /**
     * Get table row count
     */
    suspend fun getTableRowCount(tableName: String): Result<Long> {
        return database.getTableRowCount(tableName)
    }
    
    // Backup and restore
    
    /**
     * Export all data
     */
    suspend fun exportData(): Result<DatabaseBackup> {
        return database.exportData()
    }
    
    /**
     * Import data from backup
     */
    suspend fun importData(backup: DatabaseBackup): Result<Unit> {
        return database.importData(backup)
    }
    
    /**
     * Validate data integrity
     */
    suspend fun validateData(): Result<List<String>> {
        return database.validateData()
    }
    
    // High-level convenience methods
    
    /**
     * Get storage with statistics
     */
    suspend fun getStorageWithStats(id: String): Result<StorageWithStats> {
        return database.getStorage(id).map { storage ->
            if (storage != null) {
                val documents = database.getDocumentsByStorage(id).getOrNull() ?: emptyList()
                val documentCount = documents.size
                val totalSize = documents.sumOf { it.size }
                
                StorageWithStats(
                    storage = storage,
                    documentCount = documentCount,
                    totalSize = totalSize,
                    lastSync = storage.lastSync
                )
            } else {
                throw Exception("Storage not found: $id")
            }
        }
    }
    
    /**
     * Get document with cache info
     */
    suspend fun getDocumentWithCacheInfo(id: String): Result<DocumentWithCacheInfo> {
        return database.getDocument(id).map { document ->
            if (document != null) {
                val cacheEntries = database.getCacheEntriesByDocument(id).getOrNull() ?: emptyList()
                val cacheSize = cacheEntries.sumOf { it.size }
                val isCached = cacheEntries.isNotEmpty()
                
                DocumentWithCacheInfo(
                    document = document,
                    cacheEntries = cacheEntries,
                    cacheSize = cacheSize,
                    isCached = isCached
                )
            } else {
                throw Exception("Document not found: $id")
            }
        }
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStatistics(): Result<CacheStatistics> {
        return database.getAllCacheEntries().map { entries ->
            val totalSize = entries.sumOf { it.size }
            val pinnedCount = entries.count { it.isPinned }
            val expiredCount = entries.count { it.isExpired }
            val validCount = entries.count { it.isValid }
            
            CacheStatistics(
                totalEntries = entries.size,
                totalSize = totalSize,
                pinnedCount = pinnedCount,
                expiredCount = expiredCount,
                validCount = validCount,
                averageAccessCount = entries.map { it.accessCount }.average()
            )
        }
    }
    
    /**
     * Search across documents and storage
     */
    suspend fun globalSearch(query: String): Result<GlobalSearchResults> {
        return database.transaction {
            val documents = database.searchDocuments(query).getOrNull() ?: emptyList()
            val storage = database.getAllStorage().getOrNull()?.filter { storage ->
                storage.name.contains(query, ignoreCase = true) ||
                storage.location.contains(query, ignoreCase = true)
            } ?: emptyList()
            
            GlobalSearchResults(
                documents = documents,
                storage = storage,
                totalResults = documents.size + storage.size
            )
        }
    }
    
    /**
     * Clean up database (remove expired cache, completed operations, etc.)
     */
    suspend fun cleanupDatabase(): Result<CleanupResults> {
        return database.transaction {
            val expiredCache = database.deleteExpiredCacheEntries().getOrNull() ?: 0
            val completedOperations = database.clearCompletedOperations().getOrNull() ?: 0
            val stats = database.getDatabaseStats().getOrNull()
            
            CleanupResults(
                expiredCacheEntries = expiredCache,
                completedOperations = completedOperations,
                databaseSize = stats?.totalSize ?: 0L,
                cacheSize = stats?.cacheSize ?: 0L
            )
        }
    }
}

/**
 * Storage with additional statistics
 */
data class StorageWithStats(
    val storage: NetworkStorage,
    val documentCount: Int,
    val totalSize: Long,
    val lastSync: kotlinx.datetime.Instant?
)

/**
 * Document with cache information
 */
data class DocumentWithCacheInfo(
    val document: NetworkDocument,
    val cacheEntries: List<CacheEntry>,
    val cacheSize: Long,
    val isCached: Boolean
)

/**
 * Cache statistics
 */
data class CacheStatistics(
    val totalEntries: Int,
    val totalSize: Long,
    val pinnedCount: Int,
    val expiredCount: Int,
    val validCount: Int,
    val averageAccessCount: Double
)

/**
 * Global search results
 */
data class GlobalSearchResults(
    val documents: List<NetworkDocument>,
    val storage: List<NetworkStorage>,
    val totalResults: Int
)

/**
 * Database cleanup results
 */
data class CleanupResults(
    val expiredCacheEntries: Int,
    val completedOperations: Int,
    val databaseSize: Long,
    val cacheSize: Long
)