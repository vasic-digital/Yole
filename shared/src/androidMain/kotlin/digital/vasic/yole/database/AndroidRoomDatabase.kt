/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android Room database implementation
 *
 *########################################################*/

package digital.vasic.yole.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import digital.vasic.yole.network.common.NetworkStorage
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.CacheEntry
import digital.vasic.yole.network.common.NetworkOperation
import digital.vasic.yole.network.common.SyncStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android Room database implementation
 */
class AndroidRoomDatabase(
    private val name: String,
    private val context: Context,
    private val dispatcher: CoroutineDispatcher
) : CommonDatabase() {
    
    private lateinit var roomDatabase: YoleRoomDatabase
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun doInitialize() = withContext(dispatcher) {
        roomDatabase = Room.databaseBuilder(
            context,
            YoleRoomDatabase::class.java,
            name
        )
        .addMigrations(MIGRATION_1_2)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Enable foreign keys
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        })
        .build()
    }
    
    override suspend fun doClose() = withContext(dispatcher) {
        if (::roomDatabase.isInitialized) {
            roomDatabase.close()
        }
    }
    
    override suspend fun <T> doTransaction(block: suspend () -> T): Result<T> = withContext(dispatcher) {
        try {
            roomDatabase.withTransaction {
                kotlinx.coroutines.runBlocking {
                    block()
                }
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // NetworkStorage operations
    override suspend fun insertStorage(storage: NetworkStorage): Result<Unit> = withContext(dispatcher) {
        validateStorage(storage).mapCatching {
            val entity = storage.toEntity(json)
            roomDatabase.storageDao().insert(entity)
        }
    }
    
    override suspend fun updateStorage(storage: NetworkStorage): Result<Unit> = withContext(dispatcher) {
        validateStorage(storage).mapCatching {
            val entity = storage.toEntity(json)
            roomDatabase.storageDao().update(entity)
        }
    }
    
    override suspend fun getStorage(id: String): Result<NetworkStorage?> = withContext(dispatcher) {
        runCatching {
            roomDatabase.storageDao().getById(id)?.toModel(json)
        }
    }
    
    override suspend fun getAllStorage(): Result<List<NetworkStorage>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.storageDao().getAll().map { it.toModel(json) }
        }
    }
    
    override suspend fun deleteStorage(id: String): Result<Unit> = withContext(dispatcher) {
        runCatching {
            roomDatabase.storageDao().deleteById(id)
        }
    }
    
    override suspend fun getStorageByType(type: String): Result<List<NetworkStorage>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.storageDao().getByType(type).map { it.toModel(json) }
        }
    }
    
    override suspend fun getEnabledStorage(): Result<List<NetworkStorage>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.storageDao().getEnabled().map { it.toModel(json) }
        }
    }
    
    // NetworkDocument operations
    override suspend fun insertDocument(document: NetworkDocument): Result<Unit> = withContext(dispatcher) {
        validateDocument(document).mapCatching {
            val entity = document.toEntity(json)
            roomDatabase.documentDao().insert(entity)
        }
    }
    
    override suspend fun updateDocument(document: NetworkDocument): Result<Unit> = withContext(dispatcher) {
        validateDocument(document).mapCatching {
            val entity = document.toEntity(json)
            roomDatabase.documentDao().update(entity)
        }
    }
    
    override suspend fun getDocument(id: String): Result<NetworkDocument?> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().getById(id)?.toModel(json)
        }
    }
    
    override suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().getByStorageId(storageId).map { it.toModel(json) }
        }
    }
    
    override suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().getByPath(path).map { it.toModel(json) }
        }
    }
    
    override suspend fun getDocumentsByParentPath(parentPath: String): Result<List<NetworkDocument>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().getByParentPath(parentPath).map { it.toModel(json) }
        }
    }
    
    override suspend fun deleteDocument(id: String): Result<Unit> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().deleteById(id)
        }
    }
    
    override suspend fun searchDocuments(query: String): Result<List<NetworkDocument>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().search("%$query%").map { it.toModel(json) }
        }
    }
    
    override suspend fun getDocumentsBySyncStatus(status: SyncStatus): Result<List<NetworkDocument>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().getBySyncStatus(status.name).map { it.toModel(json) }
        }
    }
    
    override fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>> {
        return roomDatabase.documentDao().observeByStorageId(storageId).map { entities ->
            entities.map { it.toModel(json) }
        }
    }
    
    override fun observeAllDocuments(): Flow<List<NetworkDocument>> {
        return roomDatabase.documentDao().observeAll().map { entities ->
            entities.map { it.toModel(json) }
        }
    }
    
    // CacheEntry operations
    override suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit> = withContext(dispatcher) {
        validateCacheEntry(entry).mapCatching {
            val entity = entry.toEntity(json)
            roomDatabase.cacheEntryDao().insert(entity)
        }
    }
    
    override suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit> = withContext(dispatcher) {
        validateCacheEntry(entry).mapCatching {
            val entity = entry.toEntity(json)
            roomDatabase.cacheEntryDao().update(entity)
        }
    }
    
    override suspend fun getCacheEntry(id: String): Result<CacheEntry?> = withContext(dispatcher) {
        runCatching {
            roomDatabase.cacheEntryDao().getById(id)?.toModel(json)
        }
    }
    
    override suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.cacheEntryDao().getByDocumentId(documentId).map { it.toModel(json) }
        }
    }
    
    override suspend fun getAllCacheEntries(): Result<List<CacheEntry>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.cacheEntryDao().getAll().map { it.toModel(json) }
        }
    }
    
    override suspend fun deleteCacheEntry(id: String): Result<Unit> = withContext(dispatcher) {
        runCatching {
            roomDatabase.cacheEntryDao().deleteById(id)
        }
    }
    
    override suspend fun deleteExpiredCacheEntries(): Result<Int> = withContext(dispatcher) {
        runCatching {
            val now = System.currentTimeMillis()
            roomDatabase.cacheEntryDao().deleteExpired(now)
        }
    }
    
    override suspend fun getCacheUsage(): Result<Long> = withContext(dispatcher) {
        runCatching {
            roomDatabase.cacheEntryDao().getTotalSize() ?: 0L
        }
    }
    
    override suspend fun getCacheEntriesByPath(remotePath: String): Result<List<CacheEntry>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.cacheEntryDao().getByRemotePath(remotePath).map { it.toModel(json) }
        }
    }
    
    override suspend fun evictCacheEntries(maxSize: Long): Result<Int> = withContext(dispatcher) {
        runCatching {
            val currentSize = roomDatabase.cacheEntryDao().getTotalSize() ?: 0L
            if (currentSize <= maxSize) return@runCatching 0
            
            val toEvict = roomDatabase.cacheEntryDao().getEvictionCandidates(maxSize)
            roomDatabase.cacheEntryDao().deleteByIds(toEvict.map { it.id })
            toEvict.size
        }
    }
    
    // NetworkOperation operations
    override suspend fun insertOperation(operation: NetworkOperation): Result<Unit> = withContext(dispatcher) {
        validateOperation(operation).mapCatching {
            val entity = operation.toEntity(json)
            val id = roomDatabase.operationDao().insert(entity)
            // Update the operation with the generated ID if it was 0 or negative
            if (operation.id <= 0) {
                val updatedEntity = entity.copy(id = id)
                roomDatabase.operationDao().update(updatedEntity)
            }
        }
    }
    
    override suspend fun updateOperation(operation: NetworkOperation): Result<Unit> = withContext(dispatcher) {
        validateOperation(operation).mapCatching {
            val entity = operation.toEntity(json)
            roomDatabase.operationDao().update(entity)
        }
    }
    
    override suspend fun getOperation(id: Long): Result<NetworkOperation?> = withContext(dispatcher) {
        runCatching {
            roomDatabase.operationDao().getById(id)?.toModel(json)
        }
    }
    
    override suspend fun getActiveOperations(): Result<List<NetworkOperation>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.operationDao().getActive().map { it.toModel(json) }
        }
    }
    
    override suspend fun getOperationsByStatus(status: String): Result<List<NetworkOperation>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.operationDao().getByStatus(status).map { it.toModel(json) }
        }
    }
    
    override suspend fun deleteOperation(id: Long): Result<Unit> = withContext(dispatcher) {
        runCatching {
            roomDatabase.operationDao().deleteById(id)
        }
    }
    
    override suspend fun clearCompletedOperations(): Result<Int> = withContext(dispatcher) {
        runCatching {
            roomDatabase.operationDao().clearCompleted()
        }
    }
    
    override suspend fun getOperationCountByStatus(status: String): Result<Int> = withContext(dispatcher) {
        runCatching {
            roomDatabase.operationDao().countByStatus(status)
        }
    }
    
    // SyncStatus operations
    override suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit> = withContext(dispatcher) {
        runCatching {
            val entity = SyncStatusEntity(remotePath, status.name, System.currentTimeMillis())
            roomDatabase.syncStatusDao().insertOrUpdate(entity)
        }
    }
    
    override suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?> = withContext(dispatcher) {
        runCatching {
            roomDatabase.syncStatusDao().getByPath(remotePath)?.status?.let { SyncStatus.valueOf(it) }
        }
    }
    
    override suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.syncStatusDao().getAll().associate { it.remotePath to SyncStatus.valueOf(it.status) }
        }
    }
    
    override suspend fun deleteSyncStatus(remotePath: String): Result<Unit> = withContext(dispatcher) {
        runCatching {
            roomDatabase.syncStatusDao().deleteByPath(remotePath)
        }
    }
    
    override suspend fun getSyncStatusByPattern(pattern: String): Result<Map<String, SyncStatus>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.syncStatusDao().getByPattern("%$pattern%").associate { 
                it.remotePath to SyncStatus.valueOf(it.status) 
            }
        }
    }
    
    // Settings and preferences
    override suspend fun setSetting(key: String, value: String): Result<Unit> = withContext(dispatcher) {
        runCatching {
            val entity = SettingEntity(key, value)
            roomDatabase.settingDao().insertOrUpdate(entity)
        }
    }
    
    override suspend fun getSetting(key: String): Result<String?> = withContext(dispatcher) {
        runCatching {
            roomDatabase.settingDao().getByKey(key)?.value
        }
    }
    
    override suspend fun getAllSettings(): Result<Map<String, String>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.settingDao().getAll().associate { it.key to it.value }
        }
    }
    
    override suspend fun deleteSetting(key: String): Result<Unit> = withContext(dispatcher) {
        runCatching {
            roomDatabase.settingDao().deleteByKey(key)
        }
    }
    
    override suspend fun setSettingBulk(settings: Map<String, String>): Result<Unit> = withContext(dispatcher) {
        runCatching {
            val entities = settings.map { SettingEntity(it.key, it.value) }
            roomDatabase.settingDao().insertAll(entities)
        }
    }
    
    // Document metadata and search
    override suspend fun setDocumentMetadata(documentId: String, metadata: Map<String, String>): Result<Unit> = withContext(dispatcher) {
        runCatching {
            val metadataJson = json.encodeToString(metadata)
            roomDatabase.documentDao().updateMetadata(documentId, metadataJson)
        }
    }
    
    override suspend fun getDocumentMetadata(documentId: String): Result<Map<String, String>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().getMetadata(documentId)?.let { 
                json.decodeFromString<Map<String, String>>(it)
            } ?: emptyMap()
        }
    }
    
    override suspend fun searchDocumentMetadata(query: String): Result<List<String>> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().searchMetadata("%$query%")
        }
    }
    
    override suspend fun deleteDocumentMetadata(documentId: String): Result<Unit> = withContext(dispatcher) {
        runCatching {
            roomDatabase.documentDao().updateMetadata(documentId, "{}")
        }
    }
    
    // Statistics and maintenance
    override suspend fun getDatabaseStats(): Result<DatabaseStats> = withContext(dispatcher) {
        runCatching {
            val storageCount = roomDatabase.storageDao().count()
            val documentCount = roomDatabase.documentDao().count()
            val cacheCount = roomDatabase.cacheEntryDao().count()
            val operationCount = roomDatabase.operationDao().count()
            val cacheSize = roomDatabase.cacheEntryDao().getTotalSize() ?: 0L
            
            DatabaseStats(
                totalSize = storageCount + documentCount + cacheCount + operationCount, // Estimate
                tableCounts = mapOf(
                    "storage" to storageCount,
                    "documents" to documentCount,
                    "cache_entries" to cacheCount,
                    "operations" to operationCount
                ),
                cacheSize = cacheSize,
                operationCount = operationCount,
                lastVacuumTime = null
            )
        }
    }
    
    override suspend fun vacuum(): Result<Unit> = withContext(dispatcher) {
        runCatching {
            roomDatabase.openHelper.writableDatabase.execSQL("VACUUM")
        }
    }
    
    override suspend fun clearAll(): Result<Unit> = withContext(dispatcher) {
        runCatching {
            roomDatabase.clearAllTables()
        }
    }
    
    override suspend fun clearTable(tableName: String): Result<Unit> = withContext(dispatcher) {
        runCatching {
            when (tableName.lowercase()) {
                "storage" -> roomDatabase.storageDao().clearAll()
                "documents" -> roomDatabase.documentDao().clearAll()
                "cache_entries" -> roomDatabase.cacheEntryDao().clearAll()
                "operations" -> roomDatabase.operationDao().clearAll()
                "sync_status" -> roomDatabase.syncStatusDao().clearAll()
                "settings" -> roomDatabase.settingDao().clearAll()
                else -> throw IllegalArgumentException("Unknown table: $tableName")
            }
        }
    }
    
    override suspend fun getTableRowCount(tableName: String): Result<Long> = withContext(dispatcher) {
        runCatching {
            when (tableName.lowercase()) {
                "storage" -> roomDatabase.storageDao().count()
                "documents" -> roomDatabase.documentDao().count()
                "cache_entries" -> roomDatabase.cacheEntryDao().count()
                "operations" -> roomDatabase.operationDao().count()
                "sync_status" -> roomDatabase.syncStatusDao().count()
                "settings" -> roomDatabase.settingDao().count()
                else -> throw IllegalArgumentException("Unknown table: $tableName")
            }
        }
    }
    
    // Backup and restore
    override suspend fun exportData(): Result<DatabaseBackup> = withContext(dispatcher) {
        runCatching {
            val storage = roomDatabase.storageDao().getAll().map { it.toModel(json) }
            val documents = roomDatabase.documentDao().getAll().map { it.toModel(json) }
            val cacheEntries = roomDatabase.cacheEntryDao().getAll().map { it.toModel(json) }
            val operations = roomDatabase.operationDao().getAll().map { it.toModel(json) }
            val syncStatus = roomDatabase.syncStatusDao().getAll().associate { it.remotePath to SyncStatus.valueOf(it.status) }
            val settings = roomDatabase.settingDao().getAll().associate { it.key to it.value }
            
            DatabaseBackup(
                version = 1,
                timestamp = System.currentTimeMillis(),
                storage = storage,
                documents = documents,
                cacheEntries = cacheEntries,
                operations = operations,
                syncStatus = syncStatus,
                settings = settings
            )
        }
    }
    
    override suspend fun importData(backup: DatabaseBackup): Result<Unit> = withContext(dispatcher) {
        runCatching {
            clearAll().getOrThrow()
            
            backup.storage.forEach { insertStorage(it).getOrThrow() }
            backup.documents.forEach { insertDocument(it).getOrThrow() }
            backup.cacheEntries.forEach { insertCacheEntry(it).getOrThrow() }
            backup.operations.forEach { insertOperation(it).getOrThrow() }
            
            backup.syncStatus.forEach { (path, status) ->
                updateSyncStatus(path, status).getOrThrow()
            }
            
            setSettingBulk(backup.settings).getOrThrow()
        }
    }
    
    override suspend fun validateData(): Result<List<String>> = withContext(dispatcher) {
        runCatching {
            val errors = mutableListOf<String>()
            
            // Validate storage references in documents
            val storageIds = roomDatabase.storageDao().getAllIds().toSet()
            val documentStorageIds = roomDatabase.documentDao().getAllStorageIds()
            documentStorageIds.forEach { storageId ->
                if (storageId !in storageIds) {
                    errors.add("Documents reference non-existent storage: $storageId")
                }
            }
            
            // Validate document references in cache entries
            val documentIds = roomDatabase.documentDao().getAllIds().toSet()
            val cacheDocumentIds = roomDatabase.cacheEntryDao().getAllDocumentIds()
            cacheDocumentIds.forEach { documentId ->
                if (documentId !in documentIds) {
                    errors.add("Cache entries reference non-existent document: $documentId")
                }
            }
            
            errors
        }
    }
    
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any migration logic here
            }
        }
    }
}