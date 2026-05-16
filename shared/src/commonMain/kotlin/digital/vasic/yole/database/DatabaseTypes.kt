/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Common database types shared across all platform actuals
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.database.NetworkStorageDatabase

/**
 * Platform-specific database platform enum.
 */
enum class DatabasePlatform {
    ANDROID_ROOM,
    DESKTOP_SQLITE,
    IOS_SQLITE,
    WEB_INDEXEDDB,
    IN_MEMORY
}

/**
 * Platform-specific database configuration reported by DatabaseFactory.
 */
data class DatabasePlatformConfig(
    val platform: DatabasePlatform,
    val supportsTransactions: Boolean = true,
    val supportsForeignKeys: Boolean = true,
    val maxDatabaseSize: Long = Long.MAX_VALUE,
    val supportsEncryption: Boolean = false,
    val supportsWAL: Boolean = false,
    val recommendedCacheSize: Int = 1000,
    val supportsAsync: Boolean = true
)

/**
 * Database statistics snapshot.
 */
data class DatabaseStats(
    val totalSize: Long,
    val tableCounts: Map<String, Long>,
    val cacheSize: Long,
    val operationCount: Long,
    val lastVacuumTime: Long?
)

/**
 * Marker interface that platform database implementations must satisfy.
 * Concrete implementations extend [CommonDatabase] which implements
 * [NetworkStorageDatabase].
 */
interface DatabaseInterface : NetworkStorageDatabase {
    suspend fun getDatabaseStats(): Result<DatabaseStats>
}

/**
 * Abstract base class that platform database implementations extend.
 * Provides common lifecycle management and delegates to platform-specific
 * doInitialize / doClose / doTransaction implementations.
 */
abstract class CommonDatabase : DatabaseInterface {

    private var initialized = false

    override suspend fun initialize(): Result<Unit> = runCatching {
        if (!initialized) {
            doInitialize()
            initialized = true
        }
    }

    override suspend fun close(): Result<Unit> = runCatching {
        if (initialized) {
            doClose()
            initialized = false
        }
    }

    /** Platform-specific initialization (open file, connect, etc.). */
    protected abstract suspend fun doInitialize()

    /** Platform-specific teardown. */
    protected abstract suspend fun doClose()

    /** Platform-specific transaction wrapper. */
    protected abstract suspend fun <T> doTransaction(block: suspend () -> T): Result<T>

    // ── Default no-op implementations for NetworkStorageDatabase ────────────
    // Platforms override the ones they support.

    override suspend fun updateStorage(storage: NetworkStorage): Result<Unit> =
        Result.failure(UnsupportedOperationException("updateStorage not implemented on this platform"))

    override suspend fun getAllStorage(): Result<List<NetworkStorage>> =
        Result.success(emptyList())

    override suspend fun deleteStorage(id: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("deleteStorage not implemented on this platform"))

    override suspend fun insertDocument(document: NetworkDocument): Result<Unit> =
        Result.failure(UnsupportedOperationException("insertDocument not implemented on this platform"))

    override suspend fun updateDocument(document: NetworkDocument): Result<Unit> =
        Result.failure(UnsupportedOperationException("updateDocument not implemented on this platform"))

    override suspend fun getDocument(id: String): Result<NetworkDocument?> =
        Result.success(null)

    override suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>> =
        Result.success(emptyList())

    override suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>> =
        Result.success(emptyList())

    override suspend fun deleteDocument(id: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("deleteDocument not implemented on this platform"))

    override fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>> =
        kotlinx.coroutines.flow.flow { emit(emptyList()) }

    override suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit> =
        Result.failure(UnsupportedOperationException("insertCacheEntry not implemented on this platform"))

    override suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit> =
        Result.failure(UnsupportedOperationException("updateCacheEntry not implemented on this platform"))

    override suspend fun getCacheEntry(id: String): Result<CacheEntry?> =
        Result.success(null)

    override suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>> =
        Result.success(emptyList())

    override suspend fun getAllCacheEntries(): Result<List<CacheEntry>> =
        Result.success(emptyList())

    override suspend fun deleteCacheEntry(id: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("deleteCacheEntry not implemented on this platform"))

    override suspend fun deleteExpiredCacheEntries(): Result<Int> =
        Result.success(0)

    override suspend fun getCacheUsage(): Result<Long> =
        Result.success(0L)

    override suspend fun insertOperation(operation: NetworkOperation): Result<Unit> =
        Result.failure(UnsupportedOperationException("insertOperation not implemented on this platform"))

    override suspend fun updateOperation(operation: NetworkOperation): Result<Unit> =
        Result.failure(UnsupportedOperationException("updateOperation not implemented on this platform"))

    override suspend fun getOperation(id: Long): Result<NetworkOperation?> =
        Result.success(null)

    override suspend fun getActiveOperations(): Result<List<NetworkOperation>> =
        Result.success(emptyList())

    override suspend fun deleteOperation(id: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException("deleteOperation not implemented on this platform"))

    override suspend fun clearCompletedOperations(): Result<Int> =
        Result.success(0)

    override suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit> =
        Result.failure(UnsupportedOperationException("updateSyncStatus not implemented on this platform"))

    override suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?> =
        Result.success(null)

    override suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>> =
        Result.success(emptyMap())

    override suspend fun deleteSyncStatus(remotePath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("deleteSyncStatus not implemented on this platform"))

    override suspend fun clearAll(): Result<Unit> =
        Result.failure(UnsupportedOperationException("clearAll not implemented on this platform"))

    override suspend fun getDatabaseStats(): Result<DatabaseStats> =
        Result.failure(UnsupportedOperationException("getDatabaseStats not implemented on this platform"))

    override suspend fun vacuum(): Result<Unit> =
        Result.success(Unit)

    // Shared validation helpers for sub-classes
    protected fun validateStorage(storage: NetworkStorage): Result<Unit> =
        if (storage.id.isBlank() || storage.name.isBlank()) {
            Result.failure(IllegalArgumentException("Storage id and name must not be blank"))
        } else {
            Result.success(Unit)
        }
}
