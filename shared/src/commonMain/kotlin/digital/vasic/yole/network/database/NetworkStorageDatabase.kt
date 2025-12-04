package digital.vasic.yole.network.database

import kotlinx.coroutines.flow.Flow
import digital.vasic.yole.network.common.NetworkStorage
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.CacheEntry
import digital.vasic.yole.network.common.NetworkOperation
import digital.vasic.yole.network.common.SyncStatus

/**
 * Database interface for network storage metadata.
 * Provides persistent storage for network operations and cached files.
 */
interface NetworkStorageDatabase {
    
    /**
     * Initialize the database
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Close the database
     */
    suspend fun close(): Result<Unit>
    
    /**
     * Storage operations
     */
    suspend fun insertStorage(storage: NetworkStorage): Result<Unit>
    suspend fun updateStorage(storage: NetworkStorage): Result<Unit>
    suspend fun getStorage(id: String): Result<NetworkStorage?>
    suspend fun getAllStorage(): Result<List<NetworkStorage>>
    suspend fun deleteStorage(id: String): Result<Unit>
    
    /**
     * Document operations
     */
    suspend fun insertDocument(document: NetworkDocument): Result<Unit>
    suspend fun updateDocument(document: NetworkDocument): Result<Unit>
    suspend fun getDocument(id: String): Result<NetworkDocument?>
    suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>>
    suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>>
    suspend fun deleteDocument(id: String): Result<Unit>
    fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>>
    
    /**
     * Cache operations
     */
    suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit>
    suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit>
    suspend fun getCacheEntry(id: String): Result<CacheEntry?>
    suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>>
    suspend fun getAllCacheEntries(): Result<List<CacheEntry>>
    suspend fun deleteCacheEntry(id: String): Result<Unit>
    suspend fun deleteExpiredCacheEntries(): Result<Int>
    suspend fun getCacheUsage(): Result<Long>
    
    /**
     * Operation operations
     */
    suspend fun insertOperation(operation: NetworkOperation): Result<Unit>
    suspend fun updateOperation(operation: NetworkOperation): Result<Unit>
    suspend fun getOperation(id: Long): Result<NetworkOperation?>
    suspend fun getActiveOperations(): Result<List<NetworkOperation>>
    suspend fun deleteOperation(id: Long): Result<Unit>
    suspend fun clearCompletedOperations(): Result<Int>
    
    /**
     * Sync status operations
     */
    suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit>
    suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?>
    suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>>
    suspend fun deleteSyncStatus(remotePath: String): Result<Unit>
    
    /**
     * Cleanup operations
     */
    suspend fun clearAll(): Result<Unit>
    suspend fun vacuum(): Result<Unit>
}