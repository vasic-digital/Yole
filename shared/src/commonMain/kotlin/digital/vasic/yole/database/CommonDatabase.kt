/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Common database implementation with shared functionality
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import digital.vasic.yole.network.common.NetworkStorage
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.CacheEntry
import digital.vasic.yole.network.common.NetworkOperation
import digital.vasic.yole.network.common.SyncStatus

/**
 * Abstract base class for database implementations providing common functionality
 */
abstract class CommonDatabase : DatabaseInterface {
    
    protected val mutex = Mutex()
    protected val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    protected var initialized = false
    protected var closed = false
    
    override suspend fun initialize(): Result<Unit> = mutex.withLock {
        try {
            if (initialized) {
                return@withLock Result.success(Unit)
            }
            
            if (closed) {
                return@withLock Result.failure(IllegalStateException("Database is closed"))
            }
            
            doInitialize()
            initialized = true
            _isReady.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun close(): Result<Unit> = mutex.withLock {
        try {
            if (!initialized || closed) {
                return@withLock Result.success(Unit)
            }
            
            doClose()
            closed = true
            initialized = false
            _isReady.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isReady(): Boolean = _isReady.value
    
    override suspend fun <T> transaction(block: suspend () -> T): Result<T> = mutex.withLock {
        try {
            if (!initialized || closed) {
                return@withLock Result.failure(IllegalStateException("Database not ready"))
            }
            
            doTransaction(block)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Protected methods to be implemented by platform-specific classes
    protected abstract suspend fun doInitialize()
    protected abstract suspend fun doClose()
    protected abstract suspend fun <T> doTransaction(block: suspend () -> T): Result<T>
    
    // Common validation methods
    protected fun validateStorage(storage: NetworkStorage): Result<Unit> {
        return when {
            storage.id.isBlank() -> Result.failure(IllegalArgumentException("Storage ID cannot be blank"))
            storage.name.isBlank() -> Result.failure(IllegalArgumentException("Storage name cannot be blank"))
            storage.location.isBlank() -> Result.failure(IllegalArgumentException("Storage location cannot be blank"))
            else -> Result.success(Unit)
        }
    }
    
    protected fun validateDocument(document: NetworkDocument): Result<Unit> {
        return when {
            document.id.isBlank() -> Result.failure(IllegalArgumentException("Document ID cannot be blank"))
            document.name.isBlank() -> Result.failure(IllegalArgumentException("Document name cannot be blank"))
            document.path.isBlank() -> Result.failure(IllegalArgumentException("Document path cannot be blank"))
            else -> Result.success(Unit)
        }
    }
    
    protected fun validateCacheEntry(entry: CacheEntry): Result<Unit> {
        return when {
            entry.id.isBlank() -> Result.failure(IllegalArgumentException("Cache entry ID cannot be blank"))
            entry.remoteDocumentId.isBlank() -> Result.failure(IllegalArgumentException("Remote document ID cannot be blank"))
            entry.localPath.isBlank() -> Result.failure(IllegalArgumentException("Local path cannot be blank"))
            entry.remotePath.isBlank() -> Result.failure(IllegalArgumentException("Remote path cannot be blank"))
            entry.size < 0 -> Result.failure(IllegalArgumentException("Cache entry size cannot be negative"))
            else -> Result.success(Unit)
        }
    }
    
    protected fun validateOperation(operation: NetworkOperation): Result<Unit> {
        return when {
            operation.id < 0 -> Result.failure(IllegalArgumentException("Operation ID cannot be negative"))
            operation.remotePath.isBlank() -> Result.failure(IllegalArgumentException("Remote path cannot be blank"))
            operation.type.name.isBlank() -> Result.failure(IllegalArgumentException("Operation type cannot be blank"))
            operation.status.name.isBlank() -> Result.failure(IllegalArgumentException("Operation status cannot be blank"))
            else -> Result.success(Unit)
        }
    }
    
    // Common utility methods
    protected fun generateId(prefix: String = "id"): String {
        return "$prefix-${currentTimeMillis()}-${(Math.random() * 10000).toInt()}"
    }
    
    protected fun currentTimeMillis(): Long {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }
    
    protected fun <T> ensureReady(block: suspend () -> T): Result<T> {
        return if (!initialized || closed || !_isReady.value) {
            Result.failure(IllegalStateException("Database not ready"))
        } else {
            try {
                Result.success(block())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * In-memory database implementation for testing and fallback
 */
class InMemoryDatabase : CommonDatabase() {
    
    private val storage = mutableMapOf<String, NetworkStorage>()
    private val documents = mutableMapOf<String, NetworkDocument>()
    private val cacheEntries = mutableMapOf<String, CacheEntry>()
    private val operations = mutableMapOf<Long, NetworkOperation>()
    private val syncStatus = mutableMapOf<String, SyncStatus>()
    private val settings = mutableMapOf<String, String>()
    private val documentMetadata = mutableMapOf<String, Map<String, String>>()
    
    private var nextOperationId = 1L
    
    override suspend fun doInitialize() {
        // Nothing to initialize for in-memory database
    }
    
    override suspend fun doClose() {
        storage.clear()
        documents.clear()
        cacheEntries.clear()
        operations.clear()
        syncStatus.clear()
        settings.clear()
        documentMetadata.clear()
    }
    
    override suspend fun <T> doTransaction(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // NetworkStorage operations
    override suspend fun insertStorage(storage: NetworkStorage): Result<Unit> = ensureReady {
        validateStorage(storage).getOrThrow()
        this.storage[storage.id] = storage
        Unit
    }
    
    override suspend fun updateStorage(storage: NetworkStorage): Result<Unit> = ensureReady {
        validateStorage(storage).getOrThrow()
        if (!this.storage.containsKey(storage.id)) {
            throw IllegalArgumentException("Storage not found: ${storage.id}")
        }
        this.storage[storage.id] = storage
        Unit
    }
    
    override suspend fun getStorage(id: String): Result<NetworkStorage?> = ensureReady {
        storage[id]
    }
    
    override suspend fun getAllStorage(): Result<List<NetworkStorage>> = ensureReady {
        storage.values.toList()
    }
    
    override suspend fun deleteStorage(id: String): Result<Unit> = ensureReady {
        storage.remove(id)
        documents.values.filter { it.id == id }.forEach { doc ->
            documents.remove(doc.id)
        }
        Unit
    }
    
    override suspend fun getStorageByType(type: String): Result<List<NetworkStorage>> = ensureReady {
        storage.values.filter { it.type.name == type }.toList()
    }
    
    override suspend fun getEnabledStorage(): Result<List<NetworkStorage>> = ensureReady {
        storage.values.filter { it.isEnabled }.toList()
    }
    
    // NetworkDocument operations
    override suspend fun insertDocument(document: NetworkDocument): Result<Unit> = ensureReady {
        validateDocument(document).getOrThrow()
        documents[document.id] = document
        Unit
    }
    
    override suspend fun updateDocument(document: NetworkDocument): Result<Unit> = ensureReady {
        validateDocument(document).getOrThrow()
        if (!documents.containsKey(document.id)) {
            throw IllegalArgumentException("Document not found: ${document.id}")
        }
        documents[document.id] = document
        Unit
    }
    
    override suspend fun getDocument(id: String): Result<NetworkDocument?> = ensureReady {
        documents[id]
    }
    
    override suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>> = ensureReady {
        documents.values.filter { it.id == storageId }.toList()
    }
    
    override suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>> = ensureReady {
        documents.values.filter { it.path == path }.toList()
    }
    
    override suspend fun getDocumentsByParentPath(parentPath: String): Result<List<NetworkDocument>> = ensureReady {
        documents.values.filter { it.parentPath == parentPath }.toList()
    }
    
    override suspend fun deleteDocument(id: String): Result<Unit> = ensureReady {
        documents.remove(id)
        cacheEntries.values.filter { it.remoteDocumentId == id }.forEach { entry ->
            cacheEntries.remove(entry.id)
        }
        documentMetadata.remove(id)
        Unit
    }
    
    override suspend fun searchDocuments(query: String): Result<List<NetworkDocument>> = ensureReady {
        val lowerQuery = query.lowercase()
        documents.values.filter { doc ->
            doc.name.lowercase().contains(lowerQuery) ||
            doc.path.lowercase().contains(lowerQuery) ||
            doc.tags.any { it.lowercase().contains(lowerQuery) }
        }.toList()
    }
    
    override suspend fun getDocumentsBySyncStatus(status: SyncStatus): Result<List<NetworkDocument>> = ensureReady {
        documents.values.filter { it.syncStatus == status }.toList()
    }
    
    override fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>> {
        // Simple implementation - in real implementation would use StateFlow with proper observation
        return kotlinx.coroutines.flow.flow {
            emit(documents.values.filter { it.id == storageId }.toList())
        }
    }
    
    override fun observeAllDocuments(): Flow<List<NetworkDocument>> {
        return kotlinx.coroutines.flow.flow {
            emit(documents.values.toList())
        }
    }
    
    // CacheEntry operations
    override suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit> = ensureReady {
        validateCacheEntry(entry).getOrThrow()
        cacheEntries[entry.id] = entry
        Unit
    }
    
    override suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit> = ensureReady {
        validateCacheEntry(entry).getOrThrow()
        if (!cacheEntries.containsKey(entry.id)) {
            throw IllegalArgumentException("Cache entry not found: ${entry.id}")
        }
        cacheEntries[entry.id] = entry
        Unit
    }
    
    override suspend fun getCacheEntry(id: String): Result<CacheEntry?> = ensureReady {
        cacheEntries[id]
    }
    
    override suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>> = ensureReady {
        cacheEntries.values.filter { it.remoteDocumentId == documentId }.toList()
    }
    
    override suspend fun getAllCacheEntries(): Result<List<CacheEntry>> = ensureReady {
        cacheEntries.values.toList()
    }
    
    override suspend fun deleteCacheEntry(id: String): Result<Unit> = ensureReady {
        cacheEntries.remove(id)
        Unit
    }
    
    override suspend fun deleteExpiredCacheEntries(): Result<Int> = ensureReady {
        val now = kotlinx.datetime.Clock.System.now()
        val expired = cacheEntries.values.filter { entry ->
            entry.expiresAt?.let { it < now } == true
        }
        expired.forEach { cacheEntries.remove(it.id) }
        expired.size
    }
    
    override suspend fun getCacheUsage(): Result<Long> = ensureReady {
        cacheEntries.values.sumOf { it.size }
    }
    
    override suspend fun getCacheEntriesByPath(remotePath: String): Result<List<CacheEntry>> = ensureReady {
        cacheEntries.values.filter { it.remotePath == remotePath }.toList()
    }
    
    override suspend fun evictCacheEntries(maxSize: Long): Result<Int> = ensureReady {
        val currentSize = cacheEntries.values.sumOf { it.size }
        if (currentSize <= maxSize) return@ensureReady 0
        
        val toEvict = mutableListOf<CacheEntry>()
        var remainingSize = currentSize
        
        // Sort by priority (ascending) and last accessed time (ascending)
        val sorted = cacheEntries.values
            .filter { it.canBeEvicted }
            .sortedWith(compareBy({ it.priority }, { it.lastAccessed }))
        
        for (entry in sorted) {
            if (remainingSize <= maxSize) break
            toEvict.add(entry)
            remainingSize -= entry.size
        }
        
        toEvict.forEach { cacheEntries.remove(it.id) }
        toEvict.size
    }
    
    // NetworkOperation operations
    override suspend fun insertOperation(operation: NetworkOperation): Result<Unit> = ensureReady {
        validateOperation(operation).getOrThrow()
        val opWithId = if (operation.id < 0) {
            operation.copy(id = nextOperationId++)
        } else {
            operation
        }
        operations[opWithId.id] = opWithId
        Unit
    }
    
    override suspend fun updateOperation(operation: NetworkOperation): Result<Unit> = ensureReady {
        validateOperation(operation).getOrThrow()
        if (!operations.containsKey(operation.id)) {
            throw IllegalArgumentException("Operation not found: ${operation.id}")
        }
        operations[operation.id] = operation
        Unit
    }
    
    override suspend fun getOperation(id: Long): Result<NetworkOperation?> = ensureReady {
        operations[id]
    }
    
    override suspend fun getActiveOperations(): Result<List<NetworkOperation>> = ensureReady {
        operations.values.filter { it.status.name == "RUNNING" || it.status.name == "PENDING" }.toList()
    }
    
    override suspend fun getOperationsByStatus(status: String): Result<List<NetworkOperation>> = ensureReady {
        operations.values.filter { it.status.name == status.uppercase() }.toList()
    }
    
    override suspend fun deleteOperation(id: Long): Result<Unit> = ensureReady {
        operations.remove(id)
        Unit
    }
    
    override suspend fun clearCompletedOperations(): Result<Int> = ensureReady {
        val completed = operations.values.filter { 
            it.status.name == "COMPLETED" || it.status.name == "FAILED" || it.status.name == "CANCELLED" 
        }
        completed.forEach { operations.remove(it.id) }
        completed.size
    }
    
    override suspend fun getOperationCountByStatus(status: String): Result<Int> = ensureReady {
        operations.values.count { it.status.name == status.uppercase() }
    }
    
    // SyncStatus operations
    override suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit> = ensureReady {
        syncStatus[remotePath] = status
        Unit
    }
    
    override suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?> = ensureReady {
        syncStatus[remotePath]
    }
    
    override suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>> = ensureReady {
        syncStatus.toMap()
    }
    
    override suspend fun deleteSyncStatus(remotePath: String): Result<Unit> = ensureReady {
        syncStatus.remove(remotePath)
        Unit
    }
    
    override suspend fun getSyncStatusByPattern(pattern: String): Result<Map<String, SyncStatus>> = ensureReady {
        syncStatus.filter { (path, _) ->
            path.contains(pattern, ignoreCase = true)
        }
    }
    
    // Settings and preferences
    override suspend fun setSetting(key: String, value: String): Result<Unit> = ensureReady {
        settings[key] = value
        Unit
    }
    
    override suspend fun getSetting(key: String): Result<String?> = ensureReady {
        settings[key]
    }
    
    override suspend fun getAllSettings(): Result<Map<String, String>> = ensureReady {
        settings.toMap()
    }
    
    override suspend fun deleteSetting(key: String): Result<Unit> = ensureReady {
        settings.remove(key)
        Unit
    }
    
    override suspend fun setSettingBulk(settings: Map<String, String>): Result<Unit> = ensureReady {
        this.settings.putAll(settings)
        Unit
    }
    
    // Document metadata and search
    override suspend fun setDocumentMetadata(documentId: String, metadata: Map<String, String>): Result<Unit> = ensureReady {
        documentMetadata[documentId] = metadata
        Unit
    }
    
    override suspend fun getDocumentMetadata(documentId: String): Result<Map<String, String>> = ensureReady {
        documentMetadata[documentId] ?: emptyMap()
    }
    
    override suspend fun searchDocumentMetadata(query: String): Result<List<String>> = ensureReady {
        val lowerQuery = query.lowercase()
        documentMetadata.filter { (_, metadata) ->
            metadata.any { (key, value) ->
                key.lowercase().contains(lowerQuery) || value.lowercase().contains(lowerQuery)
            }
        }.keys.toList()
    }
    
    override suspend fun deleteDocumentMetadata(documentId: String): Result<Unit> = ensureReady {
        documentMetadata.remove(documentId)
        Unit
    }
    
    // Statistics and maintenance
    override suspend fun getDatabaseStats(): Result<DatabaseStats> = ensureReady {
        DatabaseStats(
            totalSize = (storage.size + documents.size + cacheEntries.size + operations.size + syncStatus.size + settings.size) * 100L, // Estimate
            tableCounts = mapOf(
                "storage" to storage.size.toLong(),
                "documents" to documents.size.toLong(),
                "cache_entries" to cacheEntries.size.toLong(),
                "operations" to operations.size.toLong(),
                "sync_status" to syncStatus.size.toLong(),
                "settings" to settings.size.toLong()
            ),
            cacheSize = cacheEntries.values.sumOf { it.size },
            operationCount = operations.size.toLong(),
            lastVacuumTime = null
        )
    }
    
    override suspend fun vacuum(): Result<Unit> = ensureReady {
        // Nothing to vacuum for in-memory database
        Unit
    }
    
    override suspend fun clearAll(): Result<Unit> = ensureReady {
        storage.clear()
        documents.clear()
        cacheEntries.clear()
        operations.clear()
        syncStatus.clear()
        settings.clear()
        documentMetadata.clear()
        nextOperationId = 1L
        Unit
    }
    
    override suspend fun clearTable(tableName: String): Result<Unit> = ensureReady {
        when (tableName.lowercase()) {
            "storage" -> storage.clear()
            "documents" -> documents.clear()
            "cache_entries" -> cacheEntries.clear()
            "operations" -> operations.clear()
            "sync_status" -> syncStatus.clear()
            "settings" -> settings.clear()
            else -> throw IllegalArgumentException("Unknown table: $tableName")
        }
        Unit
    }
    
    override suspend fun getTableRowCount(tableName: String): Result<Long> = ensureReady {
        when (tableName.lowercase()) {
            "storage" -> storage.size.toLong()
            "documents" -> documents.size.toLong()
            "cache_entries" -> cacheEntries.size.toLong()
            "operations" -> operations.size.toLong()
            "sync_status" -> syncStatus.size.toLong()
            "settings" -> settings.size.toLong()
            else -> throw IllegalArgumentException("Unknown table: $tableName")
        }
    }
    
    // Backup and restore
    override suspend fun exportData(): Result<DatabaseBackup> = ensureReady {
        DatabaseBackup(
            version = 1,
            timestamp = currentTimeMillis(),
            storage = storage.values.toList(),
            documents = documents.values.toList(),
            cacheEntries = cacheEntries.values.toList(),
            operations = operations.values.toList(),
            syncStatus = syncStatus.toMap(),
            settings = settings.toMap()
        )
    }
    
    override suspend fun importData(backup: DatabaseBackup): Result<Unit> = ensureReady {
        clearAll().getOrThrow()
        
        backup.storage.forEach { insertStorage(it).getOrThrow() }
        backup.documents.forEach { insertDocument(it).getOrThrow() }
        backup.cacheEntries.forEach { insertCacheEntry(it).getOrThrow() }
        backup.operations.forEach { insertOperation(it).getOrThrow() }
        
        syncStatus.putAll(backup.syncStatus)
        settings.putAll(backup.settings)
        
        Unit
    }
    
    override suspend fun validateData(): Result<List<String>> = ensureReady {
        val errors = mutableListOf<String>()
        
        // Validate storage references in documents
        documents.values.forEach { doc ->
            if (!storage.containsKey(doc.id)) {
                errors.add("Document ${doc.id} references non-existent storage ${doc.id}")
            }
        }
        
        // Validate document references in cache entries
        cacheEntries.values.forEach { entry ->
            if (!documents.containsKey(entry.remoteDocumentId)) {
                errors.add("Cache entry ${entry.id} references non-existent document ${entry.remoteDocumentId}")
            }
        }
        
        errors.toList()
    }
}