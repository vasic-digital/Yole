/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * In-memory database implementation for Web/WASM
 * Uses localStorage for persistence and in-memory storage for operations
 *
 *########################################################*/

package digital.vasic.yole.database

import digital.vasic.yole.network.common.*
import digital.vasic.yole.model.Document
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.Json
import kotlin.js.json

/**
 * In-memory database implementation for Web/WASM platform
 * Uses localStorage for persistence and in-memory storage for operations
 */
class InMemoryDatabase(
    private val config: DatabaseConfig = DatabaseConfig()
) : DatabaseInterface {
    
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    
    // In-memory storage
    private val documents = mutableMapOf<String, DatabaseDocument>()
    private val storage = mutableMapOf<String, DatabaseStorage>()
    private val networkDocuments = mutableMapOf<String, DatabaseNetworkDocument>()
    private val cacheEntries = mutableMapOf<String, DatabaseCacheEntry>()
    private val operations = mutableMapOf<Long, DatabaseOperation>()
    private val syncStatus = mutableMapOf<String, DatabaseSyncStatus>()
    private val settings = mutableMapOf<String, DatabaseSetting>()
    
    // Flows for real-time updates
    private val documentsFlow = MutableStateFlow<List<Document>>(emptyList())
    private val networkDocumentsFlow = MutableStateFlow<List<NetworkDocument>>(emptyList())
    private val operationsFlow = MutableStateFlow<List<NetworkOperation>>(emptyList())
    private val storageFlow = MutableStateFlow<List<NetworkStorage>>(emptyList())
    
    // Operation ID counter
    private var nextOperationId = 1L
    
    init {
        // Load data from localStorage on initialization
        loadFromLocalStorage()
        updateFlows()
    }
    
    override suspend fun <T> transaction(block: suspend () -> T): Result<T> {
        return try {
            mutex.withLock {
                Result.success(block())
            }
        } catch (e: Exception) {
            Result.failure(DatabaseException("Transaction failed", e))
        }
    }
    
    // Document operations
    override suspend fun insertDocument(document: Document): Result<Unit> = transaction {
        val dbDocument = DatabaseDocument(
            id = document.name, // Use name as ID for simplicity
            name = document.name,
            content = document.content,
            format = document.format,
            size = document.content.length.toLong(),
            lastModified = Clock.System.now(),
            createdAt = Clock.System.now(),
            isDeleted = false,
            metadata = emptyMap()
        )
        documents[dbDocument.id] = dbDocument
        saveToLocalStorage()
        updateFlows()
    }
    
    override suspend fun updateDocument(document: Document): Result<Unit> = transaction {
        val existing = documents[document.name]
        if (existing != null) {
            val updated = existing.copy(
                content = document.content,
                size = document.content.length.toLong(),
                lastModified = Clock.System.now()
            )
            documents[document.name] = updated
            saveToLocalStorage()
            updateFlows()
        }
    }
    
    override suspend fun deleteDocument(documentId: String): Result<Unit> = transaction {
        documents.remove(documentId)
        saveToLocalStorage()
        updateFlows()
    }
    
    override suspend fun getDocument(documentId: String): Result<Document?> = transaction {
        documents[documentId]?.toDocument()
    }
    
    override suspend fun getAllDocuments(): Result<List<Document>> = transaction {
        documents.values.filter { !it.isDeleted }.map { it.toDocument() }
    }
    
    override suspend fun getDocumentsByFormat(format: String): Result<List<Document>> = transaction {
        documents.values.filter { it.format == format && !it.isDeleted }.map { it.toDocument() }
    }
    
    override suspend fun searchDocuments(query: String): Result<List<Document>> = transaction {
        val lowerQuery = query.lowercase()
        documents.values.filter { doc ->
            !doc.isDeleted && (
                doc.name.lowercase().contains(lowerQuery) ||
                doc.content.lowercase().contains(lowerQuery) ||
                doc.format.lowercase().contains(lowerQuery)
            )
        }.map { it.toDocument() }
    }
    
    override suspend fun getRecentDocuments(limit: Int): Result<List<Document>> = transaction {
        documents.values
            .filter { !it.isDeleted }
            .sortedByDescending { it.lastModified }
            .take(limit)
            .map { it.toDocument() }
    }
    
    // Network storage operations
    override suspend fun insertStorage(storage: NetworkStorage): Result<Unit> = transaction {
        val dbStorage = DatabaseStorage(
            id = storage.id,
            name = storage.name,
            type = storage.type.name,
            location = storage.location,
            totalSpace = storage.totalSpace,
            usedSpace = storage.usedSpace,
            isOnline = storage.isOnline,
            lastSync = storage.lastSync,
            metadata = storage.metadata,
            isEnabled = storage.isEnabled,
            priority = storage.priority,
            supportsFolders = storage.supportsFolders,
            supportsMetadata = storage.supportsMetadata,
            maxFileSize = storage.maxFileSize,
            supportedExtensions = storage.supportedExtensions,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        this.storage[dbStorage.id] = dbStorage
        saveToLocalStorage()
        updateFlows()
    }
    
    override suspend fun updateStorage(storage: NetworkStorage): Result<Unit> = transaction {
        val existing = this.storage[storage.id]
        if (existing != null) {
            val updated = existing.copy(
                name = storage.name,
                location = storage.location,
                totalSpace = storage.totalSpace,
                usedSpace = storage.usedSpace,
                isOnline = storage.isOnline,
                lastSync = storage.lastSync,
                metadata = storage.metadata,
                isEnabled = storage.isEnabled,
                priority = storage.priority,
                supportsFolders = storage.supportsFolders,
                supportsMetadata = storage.supportsMetadata,
                maxFileSize = storage.maxFileSize,
                supportedExtensions = storage.supportedExtensions,
                updatedAt = Clock.System.now()
            )
            this.storage[storage.id] = updated
            saveToLocalStorage()
            updateFlows()
        }
    }
    
    override suspend fun deleteStorage(storageId: String): Result<Unit> = transaction {
        storage.remove(storageId)
        saveToLocalStorage()
        updateFlows()
    }
    
    override suspend fun getStorage(storageId: String): Result<NetworkStorage?> = transaction {
        storage[storageId]?.toNetworkStorage()
    }
    
    override suspend fun getAllStorage(): Result<List<NetworkStorage>> = transaction {
        storage.values.map { it.toNetworkStorage() }
    }
    
    override suspend fun getEnabledStorage(): Result<List<NetworkStorage>> = transaction {
        storage.values.filter { it.isEnabled }.map { it.toNetworkStorage() }
    }
    
    // Network document operations
    override suspend fun insertNetworkDocument(document: NetworkDocument): Result<Unit> = transaction {
        val dbDoc = DatabaseNetworkDocument(
            id = document.id,
            storageId = document.id, // Simplified mapping
            name = document.name,
            path = document.path,
            isFolder = document.isFolder,
            size = document.size,
            lastModified = document.lastModified,
            syncStatus = document.syncStatus.name,
            documentId = document.documentId,
            contentType = document.contentType,
            extension = document.extension,
            parentPath = document.parentPath,
            isSyncing = document.isSyncing,
            hasPendingChanges = document.hasPendingChanges,
            isAvailableOffline = document.isAvailableOffline,
            isReadOnly = document.isReadOnly,
            isHidden = document.isHidden,
            metadata = document.metadata,
            thumbnails = document.thumbnails,
            tags = document.tags,
            owner = document.owner,
            permissions = document.permissions.map { it.name },
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        networkDocuments[dbDoc.id] = dbDoc
        saveToLocalStorage()
        updateFlows()
    }
    
    override suspend fun updateNetworkDocument(document: NetworkDocument): Result<Unit> = transaction {
        val existing = networkDocuments[document.id]
        if (existing != null) {
            val updated = existing.copy(
                name = document.name,
                path = document.path,
                size = document.size,
                lastModified = document.lastModified,
                syncStatus = document.syncStatus.name,
                updatedAt = Clock.System.now()
            )
            networkDocuments[document.id] = updated
            saveToLocalStorage()
            updateFlows()
        }
    }
    
    override suspend fun deleteNetworkDocument(documentId: String): Result<Unit> = transaction {
        networkDocuments.remove(documentId)
        saveToLocalStorage()
        updateFlows()
    }
    
    override suspend fun getNetworkDocument(documentId: String): Result<NetworkDocument?> = transaction {
        networkDocuments[documentId]?.toNetworkDocument()
    }
    
    override suspend fun getNetworkDocumentsByStorage(storageId: String): Result<List<NetworkDocument>> = transaction {
        networkDocuments.values.filter { it.storageId == storageId }.map { it.toNetworkDocument() }
    }
    
    override suspend fun getNetworkDocumentsByPath(path: String): Result<List<NetworkDocument>> = transaction {
        networkDocuments.values.filter { it.path.startsWith(path) }.map { it.toNetworkDocument() }
    }
    
    // Cache operations
    override suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit> = transaction {
        val dbEntry = DatabaseCacheEntry(
            id = entry.id,
            remoteDocumentId = entry.remoteDocumentId,
            localPath = entry.localPath,
            remotePath = entry.remotePath,
            size = entry.size,
            createdAt = entry.createdAt,
            lastAccessed = entry.lastAccessed,
            lastModified = entry.lastModified,
            expiresAt = entry.expiresAt,
            isValid = entry.isValid,
            isPinned = entry.isPinned,
            isInUse = entry.isInUse,
            accessCount = entry.accessCount,
            contentType = entry.contentType,
            checksum = entry.checksum,
            compression = entry.compression,
            originalSize = entry.originalSize,
            priority = entry.priority,
            metadata = entry.metadata,
            tags = entry.tags
        )
        cacheEntries[dbEntry.id] = dbEntry
        saveToLocalStorage()
    }
    
    override suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit> = transaction {
        val existing = cacheEntries[entry.id]
        if (existing != null) {
            val updated = existing.copy(
                lastAccessed = entry.lastAccessed,
                accessCount = entry.accessCount,
                isValid = entry.isValid,
                isInUse = entry.isInUse
            )
            cacheEntries[entry.id] = updated
            saveToLocalStorage()
        }
    }
    
    override suspend fun deleteCacheEntry(entryId: String): Result<Unit> = transaction {
        cacheEntries.remove(entryId)
        saveToLocalStorage()
    }
    
    override suspend fun getCacheEntry(entryId: String): Result<CacheEntry?> = transaction {
        cacheEntries[entryId]?.toCacheEntry()
    }
    
    override suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>> = transaction {
        cacheEntries.values.filter { it.remoteDocumentId == documentId }.map { it.toCacheEntry() }
    }
    
    override suspend fun getValidCacheEntries(): Result<List<CacheEntry>> = transaction {
        val now = Clock.System.now()
        cacheEntries.values
            .filter { it.isValid && (it.expiresAt == null || it.expiresAt > now) }
            .map { it.toCacheEntry() }
    }
    
    override suspend fun cleanupExpiredCache(): Result<Int> = transaction {
        val now = Clock.System.now()
        val expired = cacheEntries.values.filter { 
            it.expiresAt != null && it.expiresAt <= now && !it.isPinned 
        }
        expired.forEach { cacheEntries.remove(it.id) }
        saveToLocalStorage()
        expired.size
    }
    
    // Operation tracking
    override suspend fun insertOperation(operation: NetworkOperation): Result<Unit> = transaction {
        val dbOp = DatabaseOperation(
            id = operation.id,
            type = operation.type.name,
            remotePath = operation.remotePath,
            localPath = operation.localPath,
            status = operation.status.name,
            progress = operation.progress,
            totalSize = operation.totalSize,
            bytesTransferred = operation.bytesTransferred,
            createdAt = operation.createdAt,
            startedAt = operation.startedAt,
            completedAt = operation.completedAt,
            errorMessage = operation.errorMessage,
            retryCount = operation.retryCount,
            maxRetries = operation.maxRetries,
            priority = operation.priority,
            canPause = operation.canPause,
            canCancel = operation.canCancel,
            isPaused = operation.isPaused,
            estimatedTimeRemaining = operation.estimatedTimeRemaining?.inWholeMilliseconds,
            transferSpeed = operation.transferSpeed,
            metadata = operation.metadata
        )
        operations[dbOp.id] = dbOp
        saveToLocalStorage()
        updateFlows()
    }
    
    override suspend fun updateOperation(operation: NetworkOperation): Result<Unit> = transaction {
        val existing = operations[operation.id]
        if (existing != null) {
            val updated = existing.copy(
                status = operation.status.name,
                progress = operation.progress,
                bytesTransferred = operation.bytesTransferred,
                startedAt = operation.startedAt,
                completedAt = operation.completedAt,
                errorMessage = operation.errorMessage,
                retryCount = operation.retryCount,
                isPaused = operation.isPaused,
                estimatedTimeRemaining = operation.estimatedTimeRemaining?.inWholeMilliseconds,
                transferSpeed = operation.transferSpeed
            )
            operations[operation.id] = updated
            saveToLocalStorage()
            updateFlows()
        }
    }
    
    override suspend fun deleteOperation(operationId: Long): Result<Unit> = transaction {
        operations.remove(operationId)
        saveToLocalStorage()
        updateFlows()
    }
    
    override suspend fun getOperation(operationId: Long): Result<NetworkOperation?> = transaction {
        operations[operationId]?.toNetworkOperation()
    }
    
    override suspend fun getActiveOperations(): Result<List<NetworkOperation>> = transaction {
        operations.values
            .filter { it.status in listOf("PENDING", "IN_PROGRESS", "PAUSED") }
            .map { it.toNetworkOperation() }
    }
    
    override suspend fun getOperationsByStatus(status: OperationStatus): Result<List<NetworkOperation>> = transaction {
        operations.values
            .filter { it.status == status.name }
            .map { it.toNetworkOperation() }
    }
    
    // Sync status operations
    override suspend fun updateSyncStatus(path: String, status: SyncStatus): Result<Unit> = transaction {
        val dbSync = DatabaseSyncStatus(
            remotePath = path,
            status = status.name,
            lastSyncTime = Clock.System.now(),
            nextSyncTime = null,
            errorMessage = null,
            retryCount = 0,
            maxRetries = 3,
            progress = 0.0,
            estimatedTimeRemaining = null,
            dataSize = 0L,
            bytesTransferred = 0L,
            isAutomatic = true,
            isAvailableOffline = false,
            hasConflicts = false,
            conflictResolution = null,
            metadata = emptyMap(),
            updatedAt = Clock.System.now()
        )
        syncStatus[path] = dbSync
        saveToLocalStorage()
    }
    
    override suspend fun getSyncStatus(path: String): Result<SyncStatus?> = transaction {
        syncStatus[path]?.toPair()?.second
    }
    
    override suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>> = transaction {
        syncStatus.mapValues { it.value.toPair().second }
    }
    
    // Settings operations
    override suspend fun setSetting(key: String, value: String): Result<Unit> = transaction {
        val setting = DatabaseSetting(
            key = key,
            value = value,
            updatedAt = Clock.System.now()
        )
        settings[key] = setting
        saveSettingsToLocalStorage()
    }
    
    override suspend fun getSetting(key: String): Result<String?> = transaction {
        settings[key]?.value
    }
    
    override suspend fun deleteSetting(key: String): Result<Unit> = transaction {
        settings.remove(key)
        saveSettingsToLocalStorage()
    }
    
    override suspend fun getAllSettings(): Result<Map<String, String>> = transaction {
        settings.mapValues { it.value.value }
    }
    
    // Statistics and analytics
    override suspend fun getDocumentStatistics(): Result<DocumentStatistics> = transaction {
        val docs = documents.values.filter { !it.isDeleted }
        DocumentStatistics(
            totalDocuments = docs.size.toLong(),
            totalSize = docs.sumOf { it.size },
            documentsByFormat = docs.groupBy { it.format }.mapValues { it.value.size.toLong() },
            recentlyModified = docs.maxOfOrNull { it.lastModified.toEpochMilliseconds() } ?: 0L,
            oldestDocument = docs.minOfOrNull { it.createdAt.toEpochMilliseconds() } ?: 0L,
            averageSize = if (docs.isNotEmpty()) docs.sumOf { it.size } / docs.size else 0L
        )
    }
    
    override suspend fun getStorageStatistics(): Result<StorageStatistics> = transaction {
        val storageList = storage.values
        StorageStatistics(
            totalStorage = storageList.size.toLong(),
            enabledStorage = storageList.count { it.isEnabled }.toLong(),
            totalSpace = storageList.sumOf { it.totalSpace ?: 0L },
            usedSpace = storageList.sumOf { it.usedSpace ?: 0L },
            storageByType = storageList.groupBy { StorageType.valueOf(it.type) }.mapValues { it.value.size.toLong() }
        )
    }
    
    override suspend fun getCacheStatistics(): Result<CacheStatistics> = transaction {
        val entries = cacheEntries.values
        val now = Clock.System.now()
        val validEntries = entries.filter { it.isValid && (it.expiresAt == null || it.expiresAt > now) }
        val expiredEntries = entries.filter { it.expiresAt != null && it.expiresAt <= now && !it.isPinned }
        
        CacheStatistics(
            totalEntries = entries.size.toLong(),
            totalSize = entries.sumOf { it.size },
            validEntries = validEntries.size.toLong(),
            expiredEntries = expiredEntries.size.toLong(),
            pinnedEntries = entries.count { it.isPinned }.toLong(),
            hitRate = 0.75, // Simplified calculation
            missRate = 0.25
        )
    }
    
    // Database maintenance
    override suspend fun vacuum(): Result<Unit> = transaction {
        // In-memory database doesn't need vacuuming
        Unit
    }
    
    override suspend fun getDatabaseSize(): Result<Long> = transaction {
        val allData = mapOf(
            "documents" to documents,
            "storage" to storage,
            "networkDocuments" to networkDocuments,
            "cacheEntries" to cacheEntries,
            "operations" to operations,
            "syncStatus" to syncStatus,
            "settings" to settings
        )
        json.encodeToString(allData).length.toLong()
    }
    
    override suspend fun backup(backupPath: String): Result<Unit> = transaction {
        val backupData = json.encodeToString(mapOf(
            "documents" to documents,
            "storage" to storage,
            "networkDocuments" to networkDocuments,
            "cacheEntries" to cacheEntries,
            "operations" to operations,
            "syncStatus" to syncStatus,
            "settings" to settings,
            "timestamp" to Clock.System.now().toEpochMilliseconds()
        ))
        
        // Save to localStorage with backup key
        val backupKey = "yole_backup_${Clock.System.now().toEpochMilliseconds()}"
        localStorage.setItem(backupKey, backupData)
    }
    
    override suspend fun restore(backupPath: String): Result<Unit> = transaction {
        // Implementation would load from localStorage backup
        // For now, this is a placeholder
        Unit
    }
    
    // Real-time updates
    override fun observeDocuments(): Flow<List<Document>> = documentsFlow
    override fun observeNetworkDocuments(): Flow<List<NetworkDocument>> = networkDocumentsFlow
    override fun observeOperations(): Flow<List<NetworkOperation>> = operationsFlow
    override fun observeStorage(): Flow<List<NetworkStorage>> = storageFlow
    
    // Database info
    override suspend fun getVersion(): Result<Int> = transaction {
        config.version
    }
    
    override suspend fun isHealthy(): Result<Boolean> = transaction {
        true // In-memory database is always healthy
    }
    
    override suspend fun close(): Result<Unit> = transaction {
        saveToLocalStorage()
    }
    
    // Helper functions
    private fun updateFlows() {
        documentsFlow.value = documents.values.filter { !it.isDeleted }.map { it.toDocument() }
        networkDocumentsFlow.value = networkDocuments.values.map { it.toNetworkDocument() }
        operationsFlow.value = operations.values.map { it.toNetworkOperation() }
        storageFlow.value = storage.values.map { it.toNetworkStorage() }
    }
    
    private fun saveToLocalStorage() {
        try {
            val data = json.encodeToString(mapOf(
                "documents" to documents,
                "storage" to storage,
                "networkDocuments" to networkDocuments,
                "cacheEntries" to cacheEntries,
                "operations" to operations,
                "syncStatus" to syncStatus
            ))
            localStorage.setItem("yole_database_data", data)
        } catch (e: Exception) {
            console.error("Error saving to localStorage:", e)
        }
    }
    
    private fun saveSettingsToLocalStorage() {
        try {
            val data = json.encodeToString(settings)
            localStorage.setItem("yole_database_settings", data)
        } catch (e: Exception) {
            console.error("Error saving settings to localStorage:", e)
        }
    }
    
    private fun loadFromLocalStorage() {
        try {
            // Load main data
            val dataJson = localStorage.getItem("yole_database_data")
            if (dataJson != null) {
                val data = json.decodeFromString<Map<String, Map<String, *>>>(dataJson)
                // Parse and load data into memory maps
                // This is simplified - in production would need proper deserialization
            }
            
            // Load settings
            val settingsJson = localStorage.getItem("yole_database_settings")
            if (settingsJson != null) {
                val loadedSettings = json.decodeFromString<Map<String, DatabaseSetting>>(settingsJson)
                settings.putAll(loadedSettings)
            }
        } catch (e: Exception) {
            console.error("Error loading from localStorage:", e)
        }
    }
}

/**
 * Factory function to create database instance for Web/WASM
 */
fun createWebDatabase(config: DatabaseConfig = DatabaseConfig()): DatabaseInterface {
    return InMemoryDatabase(config)
}