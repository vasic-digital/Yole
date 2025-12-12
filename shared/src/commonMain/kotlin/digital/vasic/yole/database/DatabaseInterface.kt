/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Cross-platform database interface for Yole
 * Replaces SQLDelight with platform-specific implementations
 *
 *########################################################*/

package digital.vasic.yole.database

import digital.vasic.yole.network.common.*
import digital.vasic.yole.model.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Cross-platform database interface that replaces SQLDelight
 * Provides unified API for different database backends per platform
 */
interface DatabaseInterface {
    
    // Transaction support
    suspend fun <T> transaction(block: suspend () -> T): Result<T>
    
    // Document operations
    suspend fun insertDocument(document: Document): Result<Unit>
    suspend fun updateDocument(document: Document): Result<Unit>
    suspend fun deleteDocument(documentId: String): Result<Unit>
    suspend fun getDocument(documentId: String): Result<Document?>
    suspend fun getAllDocuments(): Result<List<Document>>
    suspend fun getDocumentsByFormat(format: String): Result<List<Document>>
    suspend fun searchDocuments(query: String): Result<List<Document>>
    suspend fun getRecentDocuments(limit: Int = 10): Result<List<Document>>
    
    // Network storage operations
    suspend fun insertStorage(storage: NetworkStorage): Result<Unit>
    suspend fun updateStorage(storage: NetworkStorage): Result<Unit>
    suspend fun deleteStorage(storageId: String): Result<Unit>
    suspend fun getStorage(storageId: String): Result<NetworkStorage?>
    suspend fun getAllStorage(): Result<List<NetworkStorage>>
    suspend fun getEnabledStorage(): Result<List<NetworkStorage>>
    
    // Network document operations
    suspend fun insertNetworkDocument(document: NetworkDocument): Result<Unit>
    suspend fun updateNetworkDocument(document: NetworkDocument): Result<Unit>
    suspend fun deleteNetworkDocument(documentId: String): Result<Unit>
    suspend fun getNetworkDocument(documentId: String): Result<NetworkDocument?>
    suspend fun getNetworkDocumentsByStorage(storageId: String): Result<List<NetworkDocument>>
    suspend fun getNetworkDocumentsByPath(path: String): Result<List<NetworkDocument>>
    
    // Cache operations
    suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit>
    suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit>
    suspend fun deleteCacheEntry(entryId: String): Result<Unit>
    suspend fun getCacheEntry(entryId: String): Result<CacheEntry?>
    suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>>
    suspend fun getValidCacheEntries(): Result<List<CacheEntry>>
    suspend fun cleanupExpiredCache(): Result<Int>
    
    // Operation tracking
    suspend fun insertOperation(operation: NetworkOperation): Result<Unit>
    suspend fun updateOperation(operation: NetworkOperation): Result<Unit>
    suspend fun deleteOperation(operationId: Long): Result<Unit>
    suspend fun getOperation(operationId: Long): Result<NetworkOperation?>
    suspend fun getActiveOperations(): Result<List<NetworkOperation>>
    suspend fun getOperationsByStatus(status: OperationStatus): Result<List<NetworkOperation>>
    
    // Sync status operations
    suspend fun updateSyncStatus(path: String, status: SyncStatus): Result<Unit>
    suspend fun getSyncStatus(path: String): Result<SyncStatus?>
    suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>>
    
    // Settings operations
    suspend fun setSetting(key: String, value: String): Result<Unit>
    suspend fun getSetting(key: String): Result<String?>
    suspend fun deleteSetting(key: String): Result<Unit>
    suspend fun getAllSettings(): Result<Map<String, String>>
    
    // Statistics and analytics
    suspend fun getDocumentStatistics(): Result<DocumentStatistics>
    suspend fun getStorageStatistics(): Result<StorageStatistics>
    suspend fun getCacheStatistics(): Result<CacheStatistics>
    
    // Database maintenance
    suspend fun vacuum(): Result<Unit>
    suspend fun getDatabaseSize(): Result<Long>
    suspend fun backup(backupPath: String): Result<Unit>
    suspend fun restore(backupPath: String): Result<Unit>
    
    // Real-time updates
    fun observeDocuments(): Flow<List<Document>>
    fun observeNetworkDocuments(): Flow<List<NetworkDocument>>
    fun observeOperations(): Flow<List<NetworkOperation>>
    fun observeStorage(): Flow<List<NetworkStorage>>
    
    // Database info
    suspend fun getVersion(): Result<Int>
    suspend fun isHealthy(): Result<Boolean>
    suspend fun close(): Result<Unit>
}

/**
 * Document statistics
 */
data class DocumentStatistics(
    val totalDocuments: Long,
    val totalSize: Long,
    val documentsByFormat: Map<String, Long>,
    val recentlyModified: Long,
    val oldestDocument: Long,
    val averageSize: Long
)

/**
 * Storage statistics
 */
data class StorageStatistics(
    val totalStorage: Long,
    val enabledStorage: Long,
    val totalSpace: Long,
    val usedSpace: Long,
    val storageByType: Map<StorageType, Long>
)

/**
 * Cache statistics
 */
data class CacheStatistics(
    val totalEntries: Long,
    val totalSize: Long,
    val validEntries: Long,
    val expiredEntries: Long,
    val pinnedEntries: Long,
    val hitRate: Double,
    val missRate: Double
)

/**
 * Database configuration
 */
data class DatabaseConfig(
    val name: String = "yole_database",
    val version: Int = 1,
    val enableWal: Boolean = true,
    val enableForeignKeys: Boolean = true,
    val cacheSize: Int = 10000,
    val journalMode: String = "WAL",
    val synchronousMode: String = "NORMAL",
    val tempStore: String = "MEMORY",
    val mmapSize: Long = 268435456L, // 256MB
    val pageSize: Int = 4096,
    val autoVacuum: Boolean = true,
    val incrementalVacuum: Boolean = true
)

/**
 * Database exception
 */
class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Migration exception
 */
class MigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)