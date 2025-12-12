/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * In-memory database implementation for testing
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.database.NetworkStorageDatabase
import kotlinx.datetime.Clock
import kotlin.time.Duration

/**
 * In-memory implementation of NetworkStorageDatabase for testing purposes
 */
class InMemoryDatabase : NetworkStorageDatabase {
    
    private val storageMap = mutableMapOf<String, NetworkStorage>()
    private val documentMap = mutableMapOf<String, NetworkDocument>()
    private val cacheEntryMap = mutableMapOf<String, CacheEntry>()
    private val operationMap = mutableMapOf<Long, NetworkOperation>()
    private val syncStatusMap = mutableMapOf<String, SyncStatus>()
    
    override suspend fun initialize(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun close(): Result<Unit> {
        // Clear all data
        storageMap.clear()
        documentMap.clear()
        cacheEntryMap.clear()
        operationMap.clear()
        syncStatusMap.clear()
        return Result.success(Unit)
    }
    
    override suspend fun insertStorage(storage: NetworkStorage): Result<Unit> {
        storageMap[storage.id] = storage
        return Result.success(Unit)
    }
    
    override suspend fun updateStorage(storage: NetworkStorage): Result<Unit> {
        storageMap[storage.id] = storage
        return Result.success(Unit)
    }
    
    override suspend fun getStorage(id: String): Result<NetworkStorage?> {
        return Result.success(storageMap[id])
    }
    
    override suspend fun getAllStorage(): Result<List<NetworkStorage>> {
        return Result.success(storageMap.values.toList())
    }
    
    override suspend fun deleteStorage(id: String): Result<Unit> {
        storageMap.remove(id)
        return Result.success(Unit)
    }
    
    override suspend fun insertDocument(document: NetworkDocument): Result<Unit> {
        documentMap[document.id] = document
        return Result.success(Unit)
    }
    
    override suspend fun updateDocument(document: NetworkDocument): Result<Unit> {
        documentMap[document.id] = document
        return Result.success(Unit)
    }
    
    override suspend fun getDocument(id: String): Result<NetworkDocument?> {
        return Result.success(documentMap[id])
    }
    
    override suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>> {
        return Result.success(documentMap.values.filter { it.storageId == storageId })
    }
    
    override suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>> {
        return Result.success(documentMap.values.filter { it.path == path })
    }
    
    override suspend fun deleteDocument(id: String): Result<Unit> {
        documentMap.remove(id)
        return Result.success(Unit)
    }
    
    override fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>> {
        return MutableStateFlow(documentMap.values.toList()).map { documents ->
            documents.filter { it.storageId == storageId }
        }
    }
    
    override suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit> {
        cacheEntryMap[entry.id] = entry
        return Result.success(Unit)
    }
    
    override suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit> {
        cacheEntryMap[entry.id] = entry
        return Result.success(Unit)
    }
    
    override suspend fun getCacheEntry(id: String): Result<CacheEntry?> {
        return Result.success(cacheEntryMap[id])
    }
    
    override suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>> {
        return Result.success(cacheEntryMap.values.filter { it.remoteDocumentId == documentId })
    }
    
    override suspend fun getAllCacheEntries(): Result<List<CacheEntry>> {
        return Result.success(cacheEntryMap.values.toList())
    }
    
    override suspend fun deleteCacheEntry(id: String): Result<Unit> {
        cacheEntryMap.remove(id)
        return Result.success(Unit)
    }
    
    override suspend fun deleteExpiredCacheEntries(): Result<Int> {
        val currentTime = Clock.System.now()
        val expiredEntries = cacheEntryMap.values.filter { it.expiresAt != null && it.expiresAt!! < currentTime }
        val count = expiredEntries.size
        expiredEntries.forEach { cacheEntryMap.remove(it.id) }
        return Result.success(count)
    }
    
    override suspend fun getCacheUsage(): Result<Long> {
        val totalSize = cacheEntryMap.values.sumOf { it.size }
        return Result.success(totalSize)
    }
    
    override suspend fun insertOperation(operation: NetworkOperation): Result<Unit> {
        operationMap[operation.id] = operation
        return Result.success(Unit)
    }
    
    override suspend fun updateOperation(operation: NetworkOperation): Result<Unit> {
        operationMap[operation.id] = operation
        return Result.success(Unit)
    }
    
    override suspend fun getOperation(id: Long): Result<NetworkOperation?> {
        return Result.success(operationMap[id])
    }
    
    override suspend fun getActiveOperations(): Result<List<NetworkOperation>> {
        return Result.success(
            operationMap.values.filter { 
                it.status == NetworkOperation.Status.IN_PROGRESS || 
                it.status == NetworkOperation.Status.PENDING 
            }
        )
    }
    
    override suspend fun deleteOperation(id: Long): Result<Unit> {
        operationMap.remove(id)
        return Result.success(Unit)
    }
    
    override suspend fun clearCompletedOperations(): Result<Int> {
        val completedOps = operationMap.values.filter { 
            it.status == NetworkOperation.Status.COMPLETED || 
            it.status == NetworkOperation.Status.FAILED || 
            it.status == NetworkOperation.Status.CANCELLED 
        }
        val count = completedOps.size
        completedOps.forEach { operationMap.remove(it.id) }
        return Result.success(count)
    }
    
    override suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit> {
        syncStatusMap[remotePath] = status
        return Result.success(Unit)
    }
    
    override suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?> {
        return Result.success(syncStatusMap[remotePath])
    }
    
    override suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>> {
        return Result.success(syncStatusMap.toMap())
    }
    
    override suspend fun deleteSyncStatus(remotePath: String): Result<Unit> {
        syncStatusMap.remove(remotePath)
        return Result.success(Unit)
    }
    
    override suspend fun clearAll(): Result<Unit> {
        storageMap.clear()
        documentMap.clear()
        cacheEntryMap.clear()
        operationMap.clear()
        syncStatusMap.clear()
        return Result.success(Unit)
    }
    
    override suspend fun vacuum(): Result<Unit> {
        // No-op for in-memory database
        return Result.success(Unit)
    }
}