/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS SQLite database implementation using native SQLite
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import digital.vasic.yole.network.common.*
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.*

/**
 * iOS SQLite database implementation using native SQLite C API
 */
class IosSQLiteDatabase(
    private val name: String,
    private val documentsDirectory: String,
    private val dispatcher: CoroutineDispatcher
) : CommonDatabase() {
    
    private var db: CPointer<sqlite3>? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun doInitialize() = withContext(dispatcher) {
        val dbPath = "$documentsDirectory/$name.db"
        
        // Ensure the documents directory exists
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(documentsDirectory)) {
            fileManager.createDirectoryAtPath(
                documentsDirectory,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }
        
        // Open the database
        val result = memScoped {
            val dbPtr = alloc<CPointerVar<sqlite3>>()
            val status = sqlite3_open(dbPath, dbPtr.ptr)
            if (status == SQLITE_OK) {
                db = dbPtr.value
                true
            } else {
                false
            }
        }
        
        if (!result) {
            throw Exception("Failed to open database: $dbPath")
        }
        
        createTables()
    }
    
    override suspend fun doClose() = withContext(dispatcher) {
        db?.let { 
            sqlite3_close(it)
            db = null
        }
    }
    
    override suspend fun <T> doTransaction(block: suspend () -> T): Result<T> = withContext(dispatcher) {
        executeStatement("BEGIN TRANSACTION")
        try {
            val result = block()
            executeStatement("COMMIT")
            Result.success(result)
        } catch (e: Exception) {
            executeStatement("ROLLBACK")
            Result.failure(e)
        }
    }
    
    private fun createTables() {
        // Enable foreign keys
        executeStatement("PRAGMA foreign_keys = ON")
        
        // Create NetworkStorage table
        executeStatement("""
            CREATE TABLE IF NOT EXISTS network_storage (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                location TEXT NOT NULL,
                total_space INTEGER,
                used_space INTEGER,
                is_online INTEGER NOT NULL DEFAULT 0,
                last_sync INTEGER,
                metadata TEXT,
                is_enabled INTEGER NOT NULL DEFAULT 1,
                priority INTEGER NOT NULL DEFAULT 100,
                supports_folders INTEGER NOT NULL DEFAULT 1,
                supports_metadata INTEGER NOT NULL DEFAULT 1,
                max_file_size INTEGER,
                supported_extensions TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
        
        // Create NetworkDocument table
        executeStatement("""
            CREATE TABLE IF NOT EXISTS network_document (
                id TEXT PRIMARY KEY,
                storage_id TEXT NOT NULL,
                name TEXT NOT NULL,
                path TEXT NOT NULL,
                is_folder INTEGER NOT NULL DEFAULT 0,
                size INTEGER NOT NULL DEFAULT 0,
                last_modified INTEGER NOT NULL,
                sync_status TEXT NOT NULL,
                document_id TEXT,
                content_type TEXT,
                extension TEXT,
                parent_path TEXT,
                is_syncing INTEGER NOT NULL DEFAULT 0,
                has_pending_changes INTEGER NOT NULL DEFAULT 0,
                is_available_offline INTEGER NOT NULL DEFAULT 0,
                is_read_only INTEGER NOT NULL DEFAULT 0,
                is_hidden INTEGER NOT NULL DEFAULT 0,
                metadata TEXT,
                thumbnails TEXT,
                tags TEXT,
                owner TEXT,
                permissions TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (storage_id) REFERENCES network_storage(id) ON DELETE CASCADE
            )
        """)
        
        // Create CacheEntry table
        executeStatement("""
            CREATE TABLE IF NOT EXISTS cache_entry (
                id TEXT PRIMARY KEY,
                remote_document_id TEXT NOT NULL,
                local_path TEXT NOT NULL,
                remote_path TEXT NOT NULL,
                size INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                last_accessed INTEGER NOT NULL,
                last_modified INTEGER NOT NULL,
                expires_at INTEGER,
                is_valid INTEGER NOT NULL DEFAULT 1,
                is_pinned INTEGER NOT NULL DEFAULT 0,
                is_in_use INTEGER NOT NULL DEFAULT 0,
                access_count INTEGER NOT NULL DEFAULT 0,
                content_type TEXT,
                checksum TEXT,
                compression TEXT,
                original_size INTEGER,
                priority INTEGER NOT NULL DEFAULT 100,
                metadata TEXT,
                tags TEXT,
                FOREIGN KEY (remote_document_id) REFERENCES network_document(id) ON DELETE CASCADE
            )
        """)
        
        // Create NetworkOperation table
        executeStatement("""
            CREATE TABLE IF NOT EXISTS network_operation (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                remote_path TEXT NOT NULL,
                local_path TEXT,
                status TEXT NOT NULL,
                progress REAL NOT NULL DEFAULT 0.0,
                total_size INTEGER NOT NULL DEFAULT 0,
                bytes_transferred INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                started_at INTEGER,
                completed_at INTEGER,
                error_message TEXT,
                retry_count INTEGER NOT NULL DEFAULT 0,
                max_retries INTEGER NOT NULL DEFAULT 3,
                priority INTEGER NOT NULL DEFAULT 100,
                can_pause INTEGER NOT NULL DEFAULT 1,
                can_cancel INTEGER NOT NULL DEFAULT 1,
                is_paused INTEGER NOT NULL DEFAULT 0,
                estimated_time_remaining INTEGER,
                transfer_speed INTEGER,
                metadata TEXT
            )
        """)
        
        // Create SyncStatus table
        executeStatement("""
            CREATE TABLE IF NOT EXISTS sync_status (
                remote_path TEXT PRIMARY KEY,
                status TEXT NOT NULL,
                last_sync_time INTEGER,
                next_sync_time INTEGER,
                error_message TEXT,
                retry_count INTEGER NOT NULL DEFAULT 0,
                max_retries INTEGER NOT NULL DEFAULT 3,
                progress REAL NOT NULL DEFAULT 0.0,
                estimated_time_remaining INTEGER,
                data_size INTEGER NOT NULL DEFAULT 0,
                bytes_transferred INTEGER NOT NULL DEFAULT 0,
                is_automatic INTEGER NOT NULL DEFAULT 1,
                is_available_offline INTEGER NOT NULL DEFAULT 0,
                has_conflicts INTEGER NOT NULL DEFAULT 0,
                conflict_resolution TEXT,
                metadata TEXT
            )
        """)
        
        // Create Settings table
        executeStatement("""
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """)
        
        // Create indexes
        executeStatement("CREATE INDEX IF NOT EXISTS idx_storage_type ON network_storage(type)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_storage_enabled ON network_storage(is_enabled)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_document_storage_id ON network_document(storage_id)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_document_path ON network_document(path)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_document_parent_path ON network_document(parent_path)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_document_sync_status ON network_document(sync_status)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_cache_entry_document_id ON cache_entry(remote_document_id)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_cache_entry_remote_path ON cache_entry(remote_path)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_cache_entry_created_at ON cache_entry(created_at)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_cache_entry_last_accessed ON cache_entry(last_accessed)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_cache_entry_expires_at ON cache_entry(expires_at)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_operation_status ON network_operation(status)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_operation_created_at ON network_operation(created_at)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_sync_status_status ON sync_status(status)")
        executeStatement("CREATE INDEX IF NOT EXISTS idx_sync_status_next_sync_time ON sync_status(next_sync_time)")
    }
    
    private fun executeStatement(sql: String) {
        db?.let { database ->
            memScoped {
                val stmtPtr = alloc<CPointerVar<sqlite3_stmt>>()
                val status = sqlite3_prepare_v2(database, sql, -1, stmtPtr.ptr, null)
                
                if (status == SQLITE_OK) {
                    sqlite3_step(stmtPtr.value)
                    sqlite3_finalize(stmtPtr.value)
                } else {
                    throw Exception("Failed to execute statement: $sql, error: ${getErrorMessage()}")
                }
            }
        } ?: throw Exception("Database not initialized")
    }
    
    private fun getErrorMessage(): String {
        return db?.let { database ->
            val errorMsg = sqlite3_errmsg(database)
            errorMsg?.toKString() ?: "Unknown error"
        } ?: "Database not initialized"
    }
    
    // Note: Full implementation would include all database operations
    // This is a simplified version showing the pattern for iOS native SQLite
    
    override suspend fun insertStorage(storage: NetworkStorage): Result<Unit> = withContext(dispatcher) {
        validateStorage(storage).mapCatching {
            executeStatement("""
                INSERT INTO network_storage (
                    id, name, type, location, total_space, used_space, is_online, last_sync,
                    metadata, is_enabled, priority, supports_folders, supports_metadata,
                    max_file_size, supported_extensions, created_at, updated_at
                ) VALUES (
                    '${storage.id}', '${storage.name}', '${storage.type.name}', '${storage.location}',
                    ${storage.totalSpace}, ${storage.usedSpace}, ${if (storage.isOnline) 1 else 0},
                    ${storage.lastSync?.toEpochMilliseconds() ?: "NULL"},
                    '${json.encodeToString(storage.metadata)}', ${if (storage.isEnabled) 1 else 0},
                    ${storage.priority}, ${if (storage.supportsFolders) 1 else 0},
                    ${if (storage.supportsMetadata) 1 else 0}, ${storage.maxFileSize ?: "NULL"},
                    '${json.encodeToString(storage.supportedExtensions)}',
                    ${currentTimeMillis()}, ${currentTimeMillis()}
                )
            """)
        }
    }
    
    override suspend fun getStorage(id: String): Result<NetworkStorage?> = withContext(dispatcher) {
        runCatching {
            db?.let { database ->
                memScoped {
                    val sql = "SELECT * FROM network_storage WHERE id = ?"
                    val stmtPtr = alloc<CPointerVar<sqlite3_stmt>>()
                    val status = sqlite3_prepare_v2(database, sql, -1, stmtPtr.ptr, null)
                    
                    if (status == SQLITE_OK) {
                        sqlite3_bind_text(stmtPtr.value, 1, id, -1, null)
                        
                        if (sqlite3_step(stmtPtr.value) == SQLITE_ROW) {
                            val storage = stmtPtr.value!!.toNetworkStorage(json)
                            sqlite3_finalize(stmtPtr.value)
                            storage
                        } else {
                            sqlite3_finalize(stmtPtr.value)
                            null
                        }
                    } else {
                        throw Exception("Failed to prepare statement: ${getErrorMessage()}")
                    }
                }
            }
        }
    }
    
    override suspend fun getDatabaseStats(): Result<DatabaseStats> = withContext(dispatcher) {
        runCatching {
            val tableCounts = mutableMapOf<String, Long>()
            val tables = listOf(
                "network_storage", "network_document", "cache_entry", 
                "network_operation", "sync_status", "settings"
            )
            
            tables.forEach { table ->
                db?.let { database ->
                    memScoped {
                        val sql = "SELECT COUNT(*) FROM $table"
                        val stmtPtr = alloc<CPointerVar<sqlite3_stmt>>()
                        val status = sqlite3_prepare_v2(database, sql, -1, stmtPtr.ptr, null)
                        
                        if (status == SQLITE_OK) {
                            if (sqlite3_step(stmtPtr.value) == SQLITE_ROW) {
                                tableCounts[table] = sqlite3_column_int64(stmtPtr.value, 0)
                            }
                            sqlite3_finalize(stmtPtr.value)
                        }
                    }
                }
            }
            
            val cacheSize = db?.let { database ->
                memScoped {
                    val sql = "SELECT SUM(size) FROM cache_entry"
                    val stmtPtr = alloc<CPointerVar<sqlite3_stmt>>()
                    val status = sqlite3_prepare_v2(database, sql, -1, stmtPtr.ptr, null)
                    
                    if (status == SQLITE_OK) {
                        if (sqlite3_step(stmtPtr.value) == SQLITE_ROW) {
                            sqlite3_column_int64(stmtPtr.value, 0)
                        } else {
                            0L
                        }.also {
                            sqlite3_finalize(stmtPtr.value)
                        }
                    } else {
                        0L
                    }
                }
            } ?: 0L
            
            DatabaseStats(
                totalSize = tableCounts.values.sum(),
                tableCounts = tableCounts,
                cacheSize = cacheSize,
                operationCount = tableCounts["network_operation"] ?: 0L,
                lastVacuumTime = null
            )
        }
    }
    
    override suspend fun vacuum(): Result<Unit> = withContext(dispatcher) {
        runCatching {
            executeStatement("VACUUM")
        }
    }
    
    // Helper extension function to convert SQLite statement to NetworkStorage
    private fun CPointer<sqlite3_stmt>.toNetworkStorage(json: Json): NetworkStorage {
        return NetworkStorage(
            id = sqlite3_column_text(this, 0)?.toKString() ?: "",
            name = sqlite3_column_text(this, 1)?.toKString() ?: "",
            type = StorageType.valueOf(sqlite3_column_text(this, 2)?.toKString() ?: "WEBDAV"),
            location = sqlite3_column_text(this, 3)?.toKString() ?: "",
            totalSpace = sqlite3_column_int64(this, 4),
            usedSpace = sqlite3_column_int64(this, 5),
            isOnline = sqlite3_column_int(this, 6) != 0,
            lastSync = sqlite3_column_int64(this, 7).takeIf { it > 0 }?.let { 
                Instant.fromEpochMilliseconds(it) 
            },
            metadata = sqlite3_column_text(this, 8)?.toKString()?.let { 
                json.decodeFromString<Map<String, String>>(it) 
            } ?: emptyMap(),
            isEnabled = sqlite3_column_int(this, 9) != 0,
            priority = sqlite3_column_int(this, 10),
            supportsFolders = sqlite3_column_int(this, 11) != 0,
            supportsMetadata = sqlite3_column_int(this, 12) != 0,
            maxFileSize = sqlite3_column_int64(this, 13).takeIf { it > 0 },
            supportedExtensions = sqlite3_column_text(this, 14)?.toKString()?.let { 
                json.decodeFromString<List<String>>(it) 
            } ?: emptyList()
        )
    }
    
    private fun currentTimeMillis(): Long = 
        kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    
    companion object {
        private const val SQLITE_OK = 0
        private const val SQLITE_ROW = 100
    }
}