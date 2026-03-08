/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Database Stress Tests
 *
 * Comprehensive stress tests for network storage database
 * operations including concurrent access, large datasets,
 * and cleanup operations.
 *
 *########################################################*/

package digital.vasic.yole.network.stress

import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.database.NetworkStorageDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Stress tests for database operations.
 * Tests concurrent access, large datasets, and edge cases.
 */
class DatabaseStressTest {

    private val testDatabase = InMemoryTestDatabase()

    // ==================== STORAGE OPERATIONS STRESS ====================

    @Test
    fun `concurrent storage insertions`() = runBlocking<Unit> {
        val results = (1..100).map { i ->
            async {
                testDatabase.insertStorage(createTestStorage("storage-$i"))
            }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent storage reads`() = runBlocking<Unit> {
        // Insert some data first
        (1..50).forEach { i ->
            testDatabase.insertStorage(createTestStorage("storage-$i"))
        }

        // Concurrent reads
        val results = (1..200).map { i ->
            async {
                testDatabase.getStorage("storage-${i % 50 + 1}")
            }
        }.awaitAll()

        assertEquals(200, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent storage updates`() = runBlocking<Unit> {
        // Insert initial data
        testDatabase.insertStorage(createTestStorage("storage-1"))

        // Concurrent updates
        val results = (1..100).map { i ->
            async {
                testDatabase.updateStorage(
                    createTestStorage("storage-1").copy(
                        usedSpace = i.toLong() * 1000
                    )
                )
            }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent storage deletions`() = runBlocking<Unit> {
        // Insert data
        (1..50).forEach { i ->
            testDatabase.insertStorage(createTestStorage("storage-$i"))
        }

        // Concurrent deletions
        val results = (1..50).map { i ->
            async {
                testDatabase.deleteStorage("storage-$i")
            }
        }.awaitAll()

        assertEquals(50, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    // ==================== DOCUMENT OPERATIONS STRESS ====================

    @Test
    fun `concurrent document insertions`() = runBlocking<Unit> {
        val results = (1..200).map { i ->
            async {
                testDatabase.insertDocument(createTestDocument("doc-$i", "storage-1"))
            }
        }.awaitAll()

        assertEquals(200, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent document queries by storage`() = runBlocking<Unit> {
        // Insert documents for different storages
        (1..100).forEach { i ->
            testDatabase.insertDocument(createTestDocument("doc-$i", "storage-${i % 5}"))
        }

        // Concurrent queries
        val results = (1..100).map { i ->
            async {
                testDatabase.getDocumentsByStorage("storage-${i % 5}")
            }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent document queries by path`() = runBlocking<Unit> {
        // Insert documents with various paths
        (1..50).forEach { i ->
            testDatabase.insertDocument(
                createTestDocument("doc-$i", "storage-1")
                    .copy(path = "/path/${i % 10}/file.txt")
            )
        }

        // Concurrent path queries
        val results = (1..100).map { i ->
            async {
                testDatabase.getDocumentsByPath("/path/${i % 10}/file.txt")
            }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    // ==================== CACHE OPERATIONS STRESS ====================

    @Test
    fun `concurrent cache entry insertions`() = runBlocking<Unit> {
        val results = (1..300).map { i ->
            async {
                testDatabase.insertCacheEntry(createTestCacheEntry("cache-$i"))
            }
        }.awaitAll()

        assertEquals(300, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent cache entry deletions`() = runBlocking<Unit> {
        // Insert cache entries
        (1..100).forEach { i ->
            testDatabase.insertCacheEntry(createTestCacheEntry("cache-$i"))
        }

        // Concurrent deletions
        val results = (1..100).map { i ->
            async {
                testDatabase.deleteCacheEntry("cache-$i")
            }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `expired cache cleanup under load`() = runBlocking<Unit> {
        // Insert mix of expired and valid entries
        (1..100).forEach { i ->
            testDatabase.insertCacheEntry(
                createTestCacheEntry("cache-$i", expired = i % 2 == 0)
            )
        }

        // Concurrent cleanup operations
        val results = (1..20).map {
            async {
                testDatabase.deleteExpiredCacheEntries()
            }
        }.awaitAll()

        assertEquals(20, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `cache usage calculation under load`() = runBlocking<Unit> {
        // Insert many cache entries
        (1..200).forEach { i ->
            testDatabase.insertCacheEntry(
                createTestCacheEntry("cache-$i").copy(size = i.toLong() * 1024)
            )
        }

        // Concurrent usage calculations
        val results = (1..50).map {
            async {
                testDatabase.getCacheUsage()
            }
        }.awaitAll()

        assertEquals(50, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    // ==================== OPERATION TRACKING STRESS ====================

    @Test
    fun `concurrent operation insertions`() = runBlocking<Unit> {
        val results = (1..150).map { i ->
            async {
                testDatabase.insertOperation(createTestOperation(i.toLong()))
            }
        }.awaitAll()

        assertEquals(150, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent active operation queries`() = runBlocking<Unit> {
        // Insert operations
        (1..50).forEach { i ->
            testDatabase.insertOperation(createTestOperation(i.toLong()))
        }

        // Concurrent queries
        val results = (1..100).map {
            async {
                testDatabase.getActiveOperations()
            }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `clear completed operations under load`() = runBlocking<Unit> {
        // Insert many completed operations
        (1..100).forEach { i ->
            testDatabase.insertOperation(
                createTestOperation(i.toLong(), completed = true)
            )
        }

        // Concurrent cleanup
        val results = (1..20).map {
            async {
                testDatabase.clearCompletedOperations()
            }
        }.awaitAll()

        assertEquals(20, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    // ==================== SYNC STATUS STRESS ====================

    @Test
    fun `concurrent sync status updates`() = runBlocking<Unit> {
        val results = (1..100).map { i ->
            async {
                testDatabase.updateSyncStatus("/path/$i", SyncStatus.SYNCED)
            }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent sync status queries`() = runBlocking<Unit> {
        // Set sync statuses
        (1..50).forEach { i ->
            testDatabase.updateSyncStatus("/path/$i", SyncStatus.SYNCED)
        }

        // Concurrent queries
        val results = (1..200).map { i ->
            async {
                testDatabase.getSyncStatus("/path/${i % 50 + 1}")
            }
        }.awaitAll()

        assertEquals(200, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `all sync status retrieval under load`() = runBlocking<Unit> {
        // Set many sync statuses
        (1..100).forEach { i ->
            testDatabase.updateSyncStatus("/path/$i", SyncStatus.SYNCED)
        }

        // Concurrent full retrievals
        val results = (1..30).map {
            async {
                testDatabase.getAllSyncStatus()
            }
        }.awaitAll()

        assertEquals(30, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    // ==================== CLEANUP OPERATIONS STRESS ====================

    @Test
    fun `concurrent clear all operations`() = runBlocking<Unit> {
        // Insert data
        (1..50).forEach { i ->
            testDatabase.insertStorage(createTestStorage("storage-$i"))
            testDatabase.insertDocument(createTestDocument("doc-$i", "storage-$i"))
            testDatabase.insertCacheEntry(createTestCacheEntry("cache-$i"))
        }

        // Concurrent clear operations
        val results = (1..10).map {
            async {
                testDatabase.clearAll()
            }
        }.awaitAll()

        assertEquals(10, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `concurrent vacuum operations`() = runBlocking<Unit> {
        // Insert and delete data to create fragmentation
        (1..100).forEach { i ->
            testDatabase.insertStorage(createTestStorage("storage-$i"))
        }
        (1..50).forEach { i ->
            testDatabase.deleteStorage("storage-$i")
        }

        // Concurrent vacuum
        val results = (1..5).map {
            async {
                testDatabase.vacuum()
            }
        }.awaitAll()

        assertEquals(5, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    // ==================== MIXED OPERATIONS STRESS ====================

    @Test
    fun `mixed concurrent operations`() = runBlocking<Unit> {
        val jobs = (1..200).map { i ->
            async {
                when (i % 8) {
                    0 -> testDatabase.insertStorage(createTestStorage("mixed-$i"))
                    1 -> testDatabase.getStorage("mixed-${i % 50}")
                    2 -> testDatabase.insertDocument(createTestDocument("doc-$i", "storage-1"))
                    3 -> testDatabase.getDocumentsByStorage("storage-1")
                    4 -> testDatabase.insertCacheEntry(createTestCacheEntry("cache-$i"))
                    5 -> testDatabase.getCacheUsage()
                    6 -> testDatabase.updateSyncStatus("/path/$i", SyncStatus.SYNCED)
                    else -> testDatabase.getAllSyncStatus()
                }
            }
        }

        val results = jobs.awaitAll()
        assertEquals(200, results.size)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `very long storage names`() = runBlocking<Unit> {
        val longName = "a".repeat(1000)
        val result = testDatabase.insertStorage(
            createTestStorage("long-name").copy(name = longName)
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `unicode storage names`() = runBlocking<Unit> {
        val unicodeNames = listOf(
            "хранилище",
            "存储",
            "ストレージ",
            "تخزين"
        )

        unicodeNames.forEach { name ->
            val result = testDatabase.insertStorage(
                createTestStorage(name).copy(name = name)
            )
            assertTrue(result.isSuccess, "Unicode name '$name' should be supported")
        }
    }

    @Test
    fun `empty path handling`() = runBlocking<Unit> {
        val result = testDatabase.getDocumentsByPath("")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() ?: true)
    }

    @Test
    fun `null-safe operations`() = runBlocking<Unit> {
        // Get non-existent storage
        val result = testDatabase.getStorage("non-existent")
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())

        // Get non-existent document
        val docResult = testDatabase.getDocument("non-existent")
        assertTrue(docResult.isSuccess)
        assertNull(docResult.getOrNull())

        // Get non-existent cache entry
        val cacheResult = testDatabase.getCacheEntry("non-existent")
        assertTrue(cacheResult.isSuccess)
        assertNull(cacheResult.getOrNull())
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun createTestStorage(id: String) = NetworkStorage(
        id = id,
        name = "Test Storage $id",
        type = StorageType.WEBDAV,
        location = "https://example.com/$id",
        totalSpace = 1000000000L,
        usedSpace = 100000000L,
        isOnline = true,
        lastSync = Clock.System.now()
    )

    private fun createTestDocument(id: String, storageId: String) = NetworkDocument(
        id = id,
        storageId = storageId,
        path = "/test/$id.txt",
        name = "$id.txt",
        size = 1024L,
        lastModified = Clock.System.now(),
        permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE)
    )

    private fun createTestCacheEntry(id: String, expired: Boolean = false) = CacheEntry(
        id = id,
        remoteDocumentId = "doc-$id",
        localPath = "/cache/$id",
        remotePath = "/remote/$id",
        size = 1024L,
        createdAt = Clock.System.now(),
        lastAccessed = Clock.System.now(),
        lastModified = Clock.System.now(),
        expiresAt = if (expired) {
            kotlinx.datetime.Instant.fromEpochMilliseconds(0)
        } else {
            kotlinx.datetime.Instant.fromEpochMilliseconds(Long.MAX_VALUE - 1000)
        },
        checksum = "abc123"
    )

    private fun createTestOperation(id: Long, completed: Boolean = false) = NetworkOperation(
        id = id,
        type = NetworkOperation.Type.UPLOAD,
        remotePath = "/remote/file.txt",
        localPath = "/local/file.txt",
        totalSize = 1024L,
        bytesTransferred = if (completed) 1024L else 0L,
        status = if (completed) NetworkOperation.Status.COMPLETED else NetworkOperation.Status.IN_PROGRESS,
        createdAt = Clock.System.now()
    )
}

/**
 * In-memory test database implementation.
 */
class InMemoryTestDatabase : NetworkStorageDatabase {
    private val storages = mutableMapOf<String, NetworkStorage>()
    private val documents = mutableMapOf<String, NetworkDocument>()
    private val cacheEntries = mutableMapOf<String, CacheEntry>()
    private val operations = mutableMapOf<Long, NetworkOperation>()
    private val syncStatuses = mutableMapOf<String, SyncStatus>()

    override suspend fun initialize() = Result.success(Unit)
    override suspend fun close() = Result.success(Unit)

    override suspend fun insertStorage(storage: NetworkStorage): Result<Unit> {
        storages[storage.id] = storage
        return Result.success(Unit)
    }

    override suspend fun updateStorage(storage: NetworkStorage): Result<Unit> {
        storages[storage.id] = storage
        return Result.success(Unit)
    }

    override suspend fun getStorage(id: String): Result<NetworkStorage?> {
        return Result.success(storages[id])
    }

    override suspend fun getAllStorage(): Result<List<NetworkStorage>> {
        return Result.success(storages.values.toList())
    }

    override suspend fun deleteStorage(id: String): Result<Unit> {
        storages.remove(id)
        return Result.success(Unit)
    }

    override suspend fun insertDocument(document: NetworkDocument): Result<Unit> {
        documents[document.id] = document
        return Result.success(Unit)
    }

    override suspend fun updateDocument(document: NetworkDocument): Result<Unit> {
        documents[document.id] = document
        return Result.success(Unit)
    }

    override suspend fun getDocument(id: String): Result<NetworkDocument?> {
        return Result.success(documents[id])
    }

    override suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>> {
        return Result.success(documents.values.filter { it.storageId == storageId })
    }

    override suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>> {
        return Result.success(documents.values.filter { it.path == path })
    }

    override suspend fun deleteDocument(id: String): Result<Unit> {
        documents.remove(id)
        return Result.success(Unit)
    }

    override fun observeDocumentsByStorage(storageId: String) = kotlinx.coroutines.flow.flow {
        emit(documents.values.filter { it.storageId == storageId })
    }

    override suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit> {
        cacheEntries[entry.id] = entry
        return Result.success(Unit)
    }

    override suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit> {
        cacheEntries[entry.id] = entry
        return Result.success(Unit)
    }

    override suspend fun getCacheEntry(id: String): Result<CacheEntry?> {
        return Result.success(cacheEntries[id])
    }

    override suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>> {
        return Result.success(cacheEntries.values.filter { it.remoteDocumentId == documentId })
    }

    override suspend fun getAllCacheEntries(): Result<List<CacheEntry>> {
        return Result.success(cacheEntries.values.toList())
    }

    override suspend fun deleteCacheEntry(id: String): Result<Unit> {
        cacheEntries.remove(id)
        return Result.success(Unit)
    }

    override suspend fun deleteExpiredCacheEntries(): Result<Int> {
        val now = Clock.System.now()
        val expired = cacheEntries.filter { (it.value.expiresAt ?: kotlinx.datetime.Instant.DISTANT_FUTURE) < now }.keys
        expired.forEach { cacheEntries.remove(it) }
        return Result.success(expired.size)
    }

    override suspend fun getCacheUsage(): Result<Long> {
        return Result.success(cacheEntries.values.sumOf { it.size })
    }

    override suspend fun insertOperation(operation: NetworkOperation): Result<Unit> {
        operations[operation.id] = operation
        return Result.success(Unit)
    }

    override suspend fun updateOperation(operation: NetworkOperation): Result<Unit> {
        operations[operation.id] = operation
        return Result.success(Unit)
    }

    override suspend fun getOperation(id: Long): Result<NetworkOperation?> {
        return Result.success(operations[id])
    }

    override suspend fun getActiveOperations(): Result<List<NetworkOperation>> {
        return Result.success(operations.values.filter { it.status == NetworkOperation.Status.IN_PROGRESS })
    }

    override suspend fun deleteOperation(id: Long): Result<Unit> {
        operations.remove(id)
        return Result.success(Unit)
    }

    override suspend fun clearCompletedOperations(): Result<Int> {
        val completed = operations.filter { it.value.status == NetworkOperation.Status.COMPLETED }.keys
        completed.forEach { operations.remove(it) }
        return Result.success(completed.size)
    }

    override suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit> {
        syncStatuses[remotePath] = status
        return Result.success(Unit)
    }

    override suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?> {
        return Result.success(syncStatuses[remotePath])
    }

    override suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>> {
        return Result.success(syncStatuses.toMap())
    }

    override suspend fun deleteSyncStatus(remotePath: String): Result<Unit> {
        syncStatuses.remove(remotePath)
        return Result.success(Unit)
    }

    override suspend fun clearAll(): Result<Unit> {
        storages.clear()
        documents.clear()
        cacheEntries.clear()
        operations.clear()
        syncStatuses.clear()
        return Result.success(Unit)
    }

    override suspend fun vacuum(): Result<Unit> {
        return Result.success(Unit)
    }
}
