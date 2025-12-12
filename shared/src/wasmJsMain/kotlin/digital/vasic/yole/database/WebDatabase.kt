/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web/WASM database implementation using localStorage
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import digital.vasic.yole.network.common.*
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Web/WASM database implementation using localStorage for persistence
 * This is a simplified implementation suitable for web environments
 */
class WebDatabase(
    private val name: String,
    private val dispatcher: CoroutineDispatcher
) : CommonDatabase() {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val keyPrefix = "yole_db_$name"
    
    // In-memory caches for better performance
    private val storageCache = mutableMapOf<String, NetworkStorage>()
    private val documentCache = mutableMapOf<String, NetworkDocument>()
    private val cacheEntryCache = mutableMapOf<String, CacheEntry>()
    private val operationCache = mutableMapOf<Long, NetworkOperation>()
    private val syncStatusCache = mutableMapOf<String, SyncStatus>()
    private val settingsCache = mutableMapOf<String, String>()
    private val documentMetadataCache = mutableMapOf<String, Map<String, String>>()
    
    private var nextOperationId = 1L
    
    override suspend fun doInitialize() = withContext(dispatcher) {
        // Load data from localStorage into memory
        loadAllData()
        js("localStorage.setItem('yole_db_" + name + "_exists', 'true')")
    }
    
    override suspend fun doClose() = withContext(dispatcher) {
        // Save all cached data to localStorage
        saveAllData()
    }
    
    override suspend fun <T> doTransaction(block: suspend () -> T): Result<T> = withContext(dispatcher) {
        try {
            // Web implementation doesn't support real transactions
            // We'll just execute the block and save afterwards
            val result = block()
            saveAllData() // Persist changes
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun loadAllData() {
        // Load storage
        val storageData = getLocalStorageItem("${keyPrefix}_storage") ?: "[]"
        val storageList = json.decodeFromString<List<NetworkStorage>>(storageData)
        storageCache.clear()
        storageList.forEach { storageCache[it.id] = it }
        
        // Load documents
        val documentData = getLocalStorageItem("${keyPrefix}_documents") ?: "[]"
        val documentList = json.decodeFromString<List<NetworkDocument>>(documentData)
        documentCache.clear()
        documentList.forEach { documentCache[it.id] = it }
        
        // Load cache entries
        val cacheData = getLocalStorageItem("${keyPrefix}_cache_entries") ?: "[]"
        val cacheList = json.decodeFromString<List<CacheEntry>>(cacheData)
        cacheEntryCache.clear()
        cacheList.forEach { cacheEntryCache[it.id] = it }
        
        // Load operations
        val operationData = getLocalStorageItem("${keyPrefix}_operations") ?: "[]"
        val operationList = json.decodeFromString<List<NetworkOperation>>(operationData)
        operationCache.clear()
        operationList.forEach { operationCache[it.id] = it }
        if (operationList.isNotEmpty()) {
            nextOperationId = operationList.maxOf { it.id } + 1
        }
        
        // Load sync status
        val syncData = getLocalStorageItem("${keyPrefix}_sync_status") ?: "{}"
        val syncMap = json.decodeFromString<Map<String, String>>(syncData)
        syncStatusCache.clear()
        syncMap.forEach { (path, status) ->
            syncStatusCache[path] = SyncStatus.valueOf(status)
        }
        
        // Load settings
        val settingsData = getLocalStorageItem("${keyPrefix}_settings") ?: "{}"
        val settingsMap = json.decodeFromString<Map<String, String>>(settingsData)
        settingsCache.clear()
        settingsCache.putAll(settingsMap)
        
        // Load document metadata
        val metadataData = getLocalStorageItem("${keyPrefix}_document_metadata") ?: "{}"
        val metadataMap = json.decodeFromString<Map<String, Map<String, String>>>(metadataData)
        documentMetadataCache.clear()
        documentMetadataCache.putAll(metadataMap)
    }
    
    private fun saveAllData() {
        // Save storage
        setLocalStorageItem("${keyPrefix}_storage", json.encodeToString(storageCache.values.toList()))
        
        // Save documents
        setLocalStorageItem("${keyPrefix}_documents", json.encodeToString(documentCache.values.toList()))
        
        // Save cache entries
        setLocalStorageItem("${keyPrefix}_cache_entries", json.encodeToString(cacheEntryCache.values.toList()))
        
        // Save operations
        setLocalStorageItem("${keyPrefix}_operations", json.encodeToString(operationCache.values.toList()))
        
        // Save sync status
        val syncData = syncStatusCache.mapValues { it.value.name }
        setLocalStorageItem("${keyPrefix}_sync_status", json.encodeToString(syncData))
        
        // Save settings
        setLocalStorageItem("${keyPrefix}_settings", json.encodeToString(settingsCache))
        
        // Save document metadata
        setLocalStorageItem("${keyPrefix}_document_metadata", json.encodeToString(documentMetadataCache))
    }
    
    private fun getLocalStorageItem(key: String): String? {
        return try {
            js("localStorage.getItem(key)").unsafeCast<String?>()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun setLocalStorageItem(key: String, value: String) {
        try {
            js("localStorage.setItem(key, value)")
        } catch (e: Exception) {
            // Handle quota exceeded or other errors
            console.error("Failed to save to localStorage: ${e.message}")
        }
    }
    
    private fun console.error(message: String) {
        js("console.error(message)")
    }
    
    // NetworkStorage operations
    override suspend fun insertStorage(storage: NetworkStorage): Result<Unit> = withContext(dispatcher) {
        validateStorage(storage).mapCatching {
            storageCache[storage.id] = storage
            saveAllData()
        }
    }
    
    override suspend fun updateStorage(storage: NetworkStorage): Result<Unit> = withContext(dispatcher) {
        validateStorage(storage).mapCatching {
            if (!storageCache.containsKey(storage.id)) {
                throw IllegalArgumentException("Storage not found: ${storage.id}")
            }
            storageCache[storage.id] = storage
            saveAllData()
        }
    }
    
    override suspend fun getStorage(id: String): Result<NetworkStorage?> = withContext(dispatcher) {
        ensureReady { storageCache[id] }
    }
    
    override suspend fun getAllStorage(): Result<List<NetworkStorage>> = withContext(dispatcher) {
        ensureReady { storageCache.values.toList() }
    }
    
    override suspend fun deleteStorage(id: String): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            storageCache.remove(id)
            // Remove associated documents
            val docsToRemove = documentCache.values.filter { it.id == id }.map { it.id }
            docsToRemove.forEach { docId ->
                documentCache.remove(docId)
                documentMetadataCache.remove(docId)
            }
            saveAllData()
        }
    }
    
    override suspend fun getStorageByType(type: String): Result<List<NetworkStorage>> = withContext(dispatcher) {
        ensureReady {
            storageCache.values.filter { it.type.name == type }.toList()
        }
    }
    
    override suspend fun getEnabledStorage(): Result<List<NetworkStorage>> = withContext(dispatcher) {
        ensureReady {
            storageCache.values.filter { it.isEnabled }.toList()
        }
    }
    
    // NetworkDocument operations
    override suspend fun insertDocument(document: NetworkDocument): Result<Unit> = withContext(dispatcher) {
        validateDocument(document).mapCatching {
            documentCache[document.id] = document
            saveAllData()
        }
    }
    
    override suspend fun updateDocument(document: NetworkDocument): Result<Unit> = withContext(dispatcher) {
        validateDocument(document).mapCatching {
            if (!documentCache.containsKey(document.id)) {
                throw IllegalArgumentException("Document not found: ${document.id}")
            }
            documentCache[document.id] = document
            saveAllData()
        }
    }
    
    override suspend fun getDocument(id: String): Result<NetworkDocument?> = withContext(dispatcher) {
        ensureReady { documentCache[id] }
    }
    
    override suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>> = withContext(dispatcher) {
        ensureReady {
            documentCache.values.filter { it.id == storageId }.toList()
        }
    }
    
    override suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>> = withContext(dispatcher) {
        ensureReady {
            documentCache.values.filter { it.path == path }.toList()
        }
    }
    
    override suspend fun getDocumentsByParentPath(parentPath: String): Result<List<NetworkDocument>> = withContext(dispatcher) {
        ensureReady {
            documentCache.values.filter { it.parentPath == parentPath }.toList()
        }
    }
    
    override suspend fun deleteDocument(id: String): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            documentCache.remove(id)
            documentMetadataCache.remove(id)
            // Remove associated cache entries
            val entriesToRemove = cacheEntryCache.values.filter { it.remoteDocumentId == id }.map { it.id }
            entriesToRemove.forEach { cacheEntryCache.remove(it) }
            saveAllData()
        }
    }
    
    override suspend fun searchDocuments(query: String): Result<List<NetworkDocument>> = withContext(dispatcher) {
        ensureReady {
            val lowerQuery = query.lowercase()
            documentCache.values.filter { doc ->
                doc.name.lowercase().contains(lowerQuery) ||
                doc.path.lowercase().contains(lowerQuery) ||
                doc.tags.any { it.lowercase().contains(lowerQuery) }
            }.toList()
        }
    }
    
    override suspend fun getDocumentsBySyncStatus(status: SyncStatus): Result<List<NetworkDocument>> = withContext(dispatcher) {
        ensureReady {
            documentCache.values.filter { it.syncStatus == status }.toList()
        }
    }
    
    override fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>> {
        return flow {
            emit(documentCache.values.filter { it.id == storageId }.toList())
        }
    }
    
    override fun observeAllDocuments(): Flow<List<NetworkDocument>> {
        return flow {
            emit(documentCache.values.toList())
        }
    }
    
    // CacheEntry operations
    override suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit> = withContext(dispatcher) {
        validateCacheEntry(entry).mapCatching {
            cacheEntryCache[entry.id] = entry
            saveAllData()
        }
    }
    
    override suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit> = withContext(dispatcher) {
        validateCacheEntry(entry).mapCatching {
            if (!cacheEntryCache.containsKey(entry.id)) {
                throw IllegalArgumentException("Cache entry not found: ${entry.id}")
            }
            cacheEntryCache[entry.id] = entry
            saveAllData()
        }
    }
    
    override suspend fun getCacheEntry(id: String): Result<CacheEntry?> = withContext(dispatcher) {
        ensureReady { cacheEntryCache[id] }
    }
    
    override suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>> = withContext(dispatcher) {
        ensureReady {
            cacheEntryCache.values.filter { it.remoteDocumentId == documentId }.toList()
        }
    }
    
    override suspend fun getAllCacheEntries(): Result<List<CacheEntry>> = withContext(dispatcher) {
        ensureReady { cacheEntryCache.values.toList() }
    }
    
    override suspend fun deleteCacheEntry(id: String): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            cacheEntryCache.remove(id)
            saveAllData()
        }
    }
    
    override suspend fun deleteExpiredCacheEntries(): Result<Int> = withContext(dispatcher) {
        ensureReady {
            val now = kotlinx.datetime.Clock.System.now()
            val expired = cacheEntryCache.values.filter { entry ->
                entry.expiresAt?.let { it < now } == true
            }
            expired.forEach { cacheEntryCache.remove(it.id) }
            saveAllData()
            expired.size
        }
    }
    
    override suspend fun getCacheUsage(): Result<Long> = withContext(dispatcher) {
        ensureReady { cacheEntryCache.values.sumOf { it.size } }
    }
    
    override suspend fun getCacheEntriesByPath(remotePath: String): Result<List<CacheEntry>> = withContext(dispatcher) {
        ensureReady {
            cacheEntryCache.values.filter { it.remotePath == remotePath }.toList()
        }
    }
    
    override suspend fun evictCacheEntries(maxSize: Long): Result<Int> = withContext(dispatcher) {
        ensureReady {
            val currentSize = cacheEntryCache.values.sumOf { it.size }
            if (currentSize <= maxSize) return@ensureReady 0
            
            val toEvict = mutableListOf<CacheEntry>()
            var remainingSize = currentSize
            
            // Sort by priority (ascending) and last accessed time (ascending)
            val sorted = cacheEntryCache.values
                .filter { it.canBeEvicted }
                .sortedWith(compareBy({ it.priority }, { it.lastAccessed }))
            
            for (entry in sorted) {
                if (remainingSize <= maxSize) break
                toEvict.add(entry)
                remainingSize -= entry.size
            }
            
            toEvict.forEach { cacheEntryCache.remove(it.id) }
            saveAllData()
            toEvict.size
        }
    }
    
    // NetworkOperation operations
    override suspend fun insertOperation(operation: NetworkOperation): Result<Unit> = withContext(dispatcher) {
        validateOperation(operation).mapCatching {
            val opWithId = if (operation.id <= 0) {
                operation.copy(id = nextOperationId++)
            } else {
                operation
            }
            operationCache[opWithId.id] = opWithId
            saveAllData()
        }
    }
    
    override suspend fun updateOperation(operation: NetworkOperation): Result<Unit> = withContext(dispatcher) {
        validateOperation(operation).mapCatching {
            if (!operationCache.containsKey(operation.id)) {
                throw IllegalArgumentException("Operation not found: ${operation.id}")
            }
            operationCache[operation.id] = operation
            saveAllData()
        }
    }
    
    override suspend fun getOperation(id: Long): Result<NetworkOperation?> = withContext(dispatcher) {
        ensureReady { operationCache[id] }
    }
    
    override suspend fun getActiveOperations(): Result<List<NetworkOperation>> = withContext(dispatcher) {
        ensureReady {
            operationCache.values.filter { it.status.name == "RUNNING" || it.status.name == "PENDING" }.toList()
        }
    }
    
    override suspend fun getOperationsByStatus(status: String): Result<List<NetworkOperation>> = withContext(dispatcher) {
        ensureReady {
            operationCache.values.filter { it.status.name == status.uppercase() }.toList()
        }
    }
    
    override suspend fun deleteOperation(id: Long): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            operationCache.remove(id)
            saveAllData()
        }
    }
    
    override suspend fun clearCompletedOperations(): Result<Int> = withContext(dispatcher) {
        ensureReady {
            val completed = operationCache.values.filter { 
                it.status.name == "COMPLETED" || it.status.name == "FAILED" || it.status.name == "CANCELLED" 
            }
            completed.forEach { operationCache.remove(it.id) }
            saveAllData()
            completed.size
        }
    }
    
    override suspend fun getOperationCountByStatus(status: String): Result<Int> = withContext(dispatcher) {
        ensureReady { operationCache.values.count { it.status.name == status.uppercase() } }
    }
    
    // SyncStatus operations
    override suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            syncStatusCache[remotePath] = status
            saveAllData()
        }
    }
    
    override suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?> = withContext(dispatcher) {
        ensureReady { syncStatusCache[remotePath] }
    }
    
    override suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>> = withContext(dispatcher) {
        ensureReady { syncStatusCache.toMap() }
    }
    
    override suspend fun deleteSyncStatus(remotePath: String): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            syncStatusCache.remove(remotePath)
            saveAllData()
        }
    }
    
    override suspend fun getSyncStatusByPattern(pattern: String): Result<Map<String, SyncStatus>> = withContext(dispatcher) {
        ensureReady {
            syncStatusCache.filter { (path, _) ->
                path.contains(pattern, ignoreCase = true)
            }
        }
    }
    
    // Settings and preferences
    override suspend fun setSetting(key: String, value: String): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            settingsCache[key] = value
            saveAllData()
        }
    }
    
    override suspend fun getSetting(key: String): Result<String?> = withContext(dispatcher) {
        ensureReady { settingsCache[key] }
    }
    
    override suspend fun getAllSettings(): Result<Map<String, String>> = withContext(dispatcher) {
        ensureReady { settingsCache.toMap() }
    }
    
    override suspend fun deleteSetting(key: String): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            settingsCache.remove(key)
            saveAllData()
        }
    }
    
    override suspend fun setSettingBulk(settings: Map<String, String>): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            settingsCache.putAll(settings)
            saveAllData()
        }
    }
    
    // Document metadata and search
    override suspend fun setDocumentMetadata(documentId: String, metadata: Map<String, String>): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            documentMetadataCache[documentId] = metadata
            saveAllData()
        }
    }
    
    override suspend fun getDocumentMetadata(documentId: String): Result<Map<String, String>> = withContext(dispatcher) {
        ensureReady { documentMetadataCache[documentId] ?: emptyMap() }
    }
    
    override suspend fun searchDocumentMetadata(query: String): Result<List<String>> = withContext(dispatcher) {
        ensureReady {
            val lowerQuery = query.lowercase()
            documentMetadataCache.filter { (_, metadata) ->
                metadata.any { (key, value) ->
                    key.lowercase().contains(lowerQuery) || value.lowercase().contains(lowerQuery)
                }
            }.keys.toList()
        }
    }
    
    override suspend fun deleteDocumentMetadata(documentId: String): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            documentMetadataCache.remove(documentId)
            saveAllData()
        }
    }
    
    // Statistics and maintenance
    override suspend fun getDatabaseStats(): Result<DatabaseStats> = withContext(dispatcher) {
        ensureReady {
            DatabaseStats(
                totalSize = (storageCache.size + documentCache.size + cacheEntryCache.size + 
                           operationCache.size + syncStatusCache.size + settingsCache.size + 
                           documentMetadataCache.size) * 100L, // Estimate
                tableCounts = mapOf(
                    "storage" to storageCache.size.toLong(),
                    "documents" to documentCache.size.toLong(),
                    "cache_entries" to cacheEntryCache.size.toLong(),
                    "operations" to operationCache.size.toLong(),
                    "sync_status" to syncStatusCache.size.toLong(),
                    "settings" to settingsCache.size.toLong()
                ),
                cacheSize = cacheEntryCache.values.sumOf { it.size },
                operationCount = operationCache.size.toLong(),
                lastVacuumTime = null
            )
        }
    }
    
    override suspend fun vacuum(): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            // Nothing to vacuum for web storage
            Unit
        }
    }
    
    override suspend fun clearAll(): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            storageCache.clear()
            documentCache.clear()
            cacheEntryCache.clear()
            operationCache.clear()
            syncStatusCache.clear()
            settingsCache.clear()
            documentMetadataCache.clear()
            nextOperationId = 1L
            saveAllData()
        }
    }
    
    override suspend fun clearTable(tableName: String): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            when (tableName.lowercase()) {
                "storage" -> storageCache.clear()
                "documents" -> documentCache.clear()
                "cache_entries" -> cacheEntryCache.clear()
                "operations" -> operationCache.clear()
                "sync_status" -> syncStatusCache.clear()
                "settings" -> settingsCache.clear()
                else -> throw IllegalArgumentException("Unknown table: $tableName")
            }
            saveAllData()
        }
    }
    
    override suspend fun getTableRowCount(tableName: String): Result<Long> = withContext(dispatcher) {
        ensureReady {
            when (tableName.lowercase()) {
                "storage" -> storageCache.size.toLong()
                "documents" -> documentCache.size.toLong()
                "cache_entries" -> cacheEntryCache.size.toLong()
                "operations" -> operationCache.size.toLong()
                "sync_status" -> syncStatusCache.size.toLong()
                "settings" -> settingsCache.size.toLong()
                else -> throw IllegalArgumentException("Unknown table: $tableName")
            }
        }
    }
    
    // Backup and restore
    override suspend fun exportData(): Result<DatabaseBackup> = withContext(dispatcher) {
        ensureReady {
            DatabaseBackup(
                version = 1,
                timestamp = currentTimeMillis(),
                storage = storageCache.values.toList(),
                documents = documentCache.values.toList(),
                cacheEntries = cacheEntryCache.values.toList(),
                operations = operationCache.values.toList(),
                syncStatus = syncStatusCache.toMap(),
                settings = settingsCache.toMap()
            )
        }
    }
    
    override suspend fun importData(backup: DatabaseBackup): Result<Unit> = withContext(dispatcher) {
        ensureReady {
            clearAll().getOrThrow()
            
            backup.storage.forEach { storageCache[it.id] = it }
            backup.documents.forEach { documentCache[it.id] = it }
            backup.cacheEntries.forEach { cacheEntryCache[it.id] = it }
            backup.operations.forEach { operationCache[it.id] = it }
            
            syncStatusCache.putAll(backup.syncStatus)
            settingsCache.putAll(backup.settings)
            
            if (backup.operations.isNotEmpty()) {
                nextOperationId = backup.operations.maxOf { it.id } + 1
            }
            
            saveAllData()
        }
    }
    
    override suspend fun validateData(): Result<List<String>> = withContext(dispatcher) {
        ensureReady {
            val errors = mutableListOf<String>()
            
            // Validate storage references in documents
            documentCache.values.forEach { doc ->
                if (!storageCache.containsKey(doc.id)) {
                    errors.add("Document ${doc.id} references non-existent storage ${doc.id}")
                }
            }
            
            // Validate document references in cache entries
            cacheEntryCache.values.forEach { entry ->
                if (!documentCache.containsKey(entry.remoteDocumentId)) {
                    errors.add("Cache entry ${entry.id} references non-existent document ${entry.remoteDocumentId}")
                }
            }
            
            errors.toList()
        }
    }
}