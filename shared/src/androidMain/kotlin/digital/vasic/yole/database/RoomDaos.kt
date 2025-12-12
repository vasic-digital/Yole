/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Room DAO interfaces for Android implementation
 *
 *########################################################*/

package digital.vasic.yole.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Storage DAO
@Dao
interface StorageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(storage: StorageEntity)
    
    @Update
    suspend fun update(storage: StorageEntity)
    
    @Query("SELECT * FROM network_storage WHERE id = :id")
    suspend fun getById(id: String): StorageEntity?
    
    @Query("SELECT * FROM network_storage")
    suspend fun getAll(): List<StorageEntity>
    
    @Query("SELECT * FROM network_storage WHERE type = :type")
    suspend fun getByType(type: String): List<StorageEntity>
    
    @Query("SELECT * FROM network_storage WHERE is_enabled = 1")
    suspend fun getEnabled(): List<StorageEntity>
    
    @Query("DELETE FROM network_storage WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT COUNT(*) FROM network_storage")
    suspend fun count(): Long
    
    @Query("SELECT id FROM network_storage")
    suspend fun getAllIds(): List<String>
    
    @Query("DELETE FROM network_storage")
    suspend fun clearAll()
}

// Document DAO
@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)
    
    @Update
    suspend fun update(document: DocumentEntity)
    
    @Query("SELECT * FROM network_document WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?
    
    @Query("SELECT * FROM network_document")
    suspend fun getAll(): List<DocumentEntity>
    
    @Query("SELECT * FROM network_document WHERE storage_id = :storageId")
    suspend fun getByStorageId(storageId: String): List<DocumentEntity>
    
    @Query("SELECT * FROM network_document WHERE path = :path")
    suspend fun getByPath(path: String): List<DocumentEntity>
    
    @Query("SELECT * FROM network_document WHERE parent_path = :parentPath")
    suspend fun getByParentPath(parentPath: String): List<DocumentEntity>
    
    @Query("SELECT * FROM network_document WHERE sync_status = :syncStatus")
    suspend fun getBySyncStatus(syncStatus: String): List<DocumentEntity>
    
    @Query("SELECT * FROM network_document WHERE name LIKE :query OR path LIKE :query")
    suspend fun search(query: String): List<DocumentEntity>
    
    @Query("DELETE FROM network_document WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT COUNT(*) FROM network_document")
    suspend fun count(): Long
    
    @Query("SELECT id FROM network_document")
    suspend fun getAllIds(): List<String>
    
    @Query("SELECT storage_id FROM network_document")
    suspend fun getAllStorageIds(): List<String>
    
    @Query("DELETE FROM network_document")
    suspend fun clearAll()
    
    @Query("UPDATE network_document SET metadata = :metadata WHERE id = :documentId")
    suspend fun updateMetadata(documentId: String, metadata: String)
    
    @Query("SELECT metadata FROM network_document WHERE id = :documentId")
    suspend fun getMetadata(documentId: String): String?
    
    @Query("SELECT id FROM network_document WHERE metadata LIKE :query")
    suspend fun searchMetadata(query: String): List<String>
    
    // Observable queries
    @Query("SELECT * FROM network_document WHERE storage_id = :storageId")
    fun observeByStorageId(storageId: String): Flow<List<DocumentEntity>>
    
    @Query("SELECT * FROM network_document")
    fun observeAll(): Flow<List<DocumentEntity>>
}

// CacheEntry DAO
@Dao
interface CacheEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cacheEntry: CacheEntryEntity)
    
    @Update
    suspend fun update(cacheEntry: CacheEntryEntity)
    
    @Query("SELECT * FROM cache_entry WHERE id = :id")
    suspend fun getById(id: String): CacheEntryEntity?
    
    @Query("SELECT * FROM cache_entry")
    suspend fun getAll(): List<CacheEntryEntity>
    
    @Query("SELECT * FROM cache_entry WHERE remote_document_id = :documentId")
    suspend fun getByDocumentId(documentId: String): List<CacheEntryEntity>
    
    @Query("SELECT * FROM cache_entry WHERE remote_path = :remotePath")
    suspend fun getByRemotePath(remotePath: String): List<CacheEntryEntity>
    
    @Query("SELECT * FROM cache_entry WHERE expires_at < :now AND expires_at IS NOT NULL")
    suspend fun getExpired(now: Long): List<CacheEntryEntity>
    
    @Query("DELETE FROM cache_entry WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM cache_entry WHERE expires_at < :now AND expires_at IS NOT NULL")
    suspend fun deleteExpired(now: Long): Int
    
    @Query("DELETE FROM cache_entry WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
    
    @Query("SELECT SUM(size) FROM cache_entry")
    suspend fun getTotalSize(): Long?
    
    @Query("SELECT COUNT(*) FROM cache_entry")
    suspend fun count(): Long
    
    @Query("SELECT remote_document_id FROM cache_entry")
    suspend fun getAllDocumentIds(): List<String>
    
    @Query("DELETE FROM cache_entry")
    suspend fun clearAll()
    
    // Get candidates for eviction based on priority and last accessed time
    @Query("""
        SELECT * FROM cache_entry 
        WHERE is_pinned = 0 AND is_in_use = 0 
        ORDER BY priority ASC, last_accessed ASC 
        LIMIT 100
    """)
    suspend fun getEvictionCandidates(maxToReturn: Long): List<CacheEntryEntity>
}

// Operation DAO
@Dao
interface OperationDao {
    @Insert
    suspend fun insert(operation: OperationEntity): Long
    
    @Update
    suspend fun update(operation: OperationEntity)
    
    @Query("SELECT * FROM network_operation WHERE id = :id")
    suspend fun getById(id: Long): OperationEntity?
    
    @Query("SELECT * FROM network_operation")
    suspend fun getAll(): List<OperationEntity>
    
    @Query("SELECT * FROM network_operation WHERE status IN ('RUNNING', 'PENDING')")
    suspend fun getActive(): List<OperationEntity>
    
    @Query("SELECT * FROM network_operation WHERE status = :status")
    suspend fun getByStatus(status: String): List<OperationEntity>
    
    @Query("DELETE FROM network_operation WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM network_operation WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun clearCompleted(): Int
    
    @Query("SELECT COUNT(*) FROM network_operation WHERE status = :status")
    suspend fun countByStatus(status: String): Int
    
    @Query("SELECT COUNT(*) FROM network_operation")
    suspend fun count(): Long
    
    @Query("DELETE FROM network_operation")
    suspend fun clearAll()
}

// SyncStatus DAO
@Dao
interface SyncStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(syncStatus: SyncStatusEntity)
    
    @Query("SELECT * FROM sync_status WHERE remote_path = :remotePath")
    suspend fun getByPath(remotePath: String): SyncStatusEntity?
    
    @Query("SELECT * FROM sync_status")
    suspend fun getAll(): List<SyncStatusEntity>
    
    @Query("SELECT * FROM sync_status WHERE remote_path LIKE :pattern")
    suspend fun getByPattern(pattern: String): List<SyncStatusEntity>
    
    @Query("DELETE FROM sync_status WHERE remote_path = :remotePath")
    suspend fun deleteByPath(remotePath: String)
    
    @Query("SELECT COUNT(*) FROM sync_status")
    suspend fun count(): Long
    
    @Query("DELETE FROM sync_status")
    suspend fun clearAll()
}

// Setting DAO
@Dao
interface SettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(setting: SettingEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<SettingEntity>)
    
    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getByKey(key: String): SettingEntity?
    
    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingEntity>
    
    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteByKey(key: String)
    
    @Query("SELECT COUNT(*) FROM settings")
    suspend fun count(): Long
    
    @Query("DELETE FROM settings")
    suspend fun clearAll()
}