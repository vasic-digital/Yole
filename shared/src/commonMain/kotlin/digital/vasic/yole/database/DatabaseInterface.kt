/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unified database interface for Yole
 * Provides cross-platform database abstraction
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.flow.Flow
import digital.vasic.yole.network.common.NetworkStorage
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.CacheEntry
import digital.vasic.yole.network.common.NetworkOperation
import digital.vasic.yole.network.common.SyncStatus

/**
 * Unified database interface that provides cross-platform database operations.
 * This interface abstracts away platform-specific database implementations.
 */
interface DatabaseInterface {
    
    /**
     * Initialize the database connection
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Close the database connection
     */
    suspend fun close(): Result<Unit>
    
    /**
     * Check if database is initialized and ready
     */
    suspend fun isReady(): Boolean
    
    /**
     * Transaction support
     */
    suspend fun <T> transaction(block: suspend () -> T): Result<T>
    
    // NetworkStorage operations
    suspend fun insertStorage(storage: NetworkStorage): Result<Unit>
    suspend fun updateStorage(storage: NetworkStorage): Result<Unit>
    suspend fun getStorage(id: String): Result<NetworkStorage?>
    suspend fun getAllStorage(): Result<List<NetworkStorage>>
    suspend fun deleteStorage(id: String): Result<Unit>
    suspend fun getStorageByType(type: String): Result<List<NetworkStorage>>
    suspend fun getEnabledStorage(): Result<List<NetworkStorage>>
    
    // NetworkDocument operations
    suspend fun insertDocument(document: NetworkDocument): Result<Unit>
    suspend fun updateDocument(document: NetworkDocument): Result<Unit>
    suspend fun getDocument(id: String): Result<NetworkDocument?>
    suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>>
    suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>>
    suspend fun getDocumentsByParentPath(parentPath: String): Result<List<NetworkDocument>>
    suspend fun deleteDocument(id: String): Result<Unit>
    suspend fun searchDocuments(query: String): Result<List<NetworkDocument>>
    suspend fun getDocumentsBySyncStatus(status: SyncStatus): Result<List<NetworkDocument>>
    fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>>
    fun observeAllDocuments(): Flow<List<NetworkDocument>>
    
    // CacheEntry operations
    suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit>
    suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit>
    suspend fun getCacheEntry(id: String): Result<CacheEntry?>
    suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>>
    suspend fun getAllCacheEntries(): Result<List<CacheEntry>>
    suspend fun deleteCacheEntry(id: String): Result<Unit>
    suspend fun deleteExpiredCacheEntries(): Result<Int>
    suspend fun getCacheUsage(): Result<Long>
    suspend fun getCacheEntriesByPath(remotePath: String): Result<List<CacheEntry>>
    suspend fun evictCacheEntries(maxSize: Long): Result<Int>
    
    // NetworkOperation operations
    suspend fun insertOperation(operation: NetworkOperation): Result<Unit>
    suspend fun updateOperation(operation: NetworkOperation): Result<Unit>
    suspend fun getOperation(id: Long): Result<NetworkOperation?>
    suspend fun getActiveOperations(): Result<List<NetworkOperation>>
    suspend fun getOperationsByStatus(status: String): Result<List<NetworkOperation>>
    suspend fun deleteOperation(id: Long): Result<Unit>
    suspend fun clearCompletedOperations(): Result<Int>
    suspend fun getOperationCountByStatus(status: String): Result<Int>
    
    // SyncStatus operations
    suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit>
    suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?>
    suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>>
    suspend fun deleteSyncStatus(remotePath: String): Result<Unit>
    suspend fun getSyncStatusByPattern(pattern: String): Result<Map<String, SyncStatus>>
    
    // Settings and preferences
    suspend fun setSetting(key: String, value: String): Result<Unit>
    suspend fun getSetting(key: String): Result<String?>
    suspend fun getAllSettings(): Result<Map<String, String>>
    suspend fun deleteSetting(key: String): Result<Unit>
    suspend fun setSettingBulk(settings: Map<String, String>): Result<Unit>
    
    // Document metadata and search
    suspend fun setDocumentMetadata(documentId: String, metadata: Map<String, String>): Result<Unit>
    suspend fun getDocumentMetadata(documentId: String): Result<Map<String, String>>
    suspend fun searchDocumentMetadata(query: String): Result<List<String>>
    suspend fun deleteDocumentMetadata(documentId: String): Result<Unit>
    
    // Statistics and maintenance
    suspend fun getDatabaseStats(): Result<DatabaseStats>
    suspend fun vacuum(): Result<Unit>
    suspend fun clearAll(): Result<Unit>
    suspend fun clearTable(tableName: String): Result<Unit>
    suspend fun getTableRowCount(tableName: String): Result<Long>
    
    // Backup and restore
    suspend fun exportData(): Result<DatabaseBackup>
    suspend fun importData(backup: DatabaseBackup): Result<Unit>
    suspend fun validateData(): Result<List<String>>
}

/**
 * Database statistics
 */
data class DatabaseStats(
    val totalSize: Long,
    val tableCounts: Map<String, Long>,
    val cacheSize: Long,
    val operationCount: Long,
    val lastVacuumTime: Long?
)

/**
 * Database backup data
 */
data class DatabaseBackup(
    val version: Int,
    val timestamp: Long,
    val storage: List<NetworkStorage>,
    val documents: List<NetworkDocument>,
    val cacheEntries: List<CacheEntry>,
    val operations: List<NetworkOperation>,
    val syncStatus: Map<String, SyncStatus>,
    val settings: Map<String, String>
)