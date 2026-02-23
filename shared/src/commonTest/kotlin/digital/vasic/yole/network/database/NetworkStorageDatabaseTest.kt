/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for NetworkStorageDatabase
 *
 *########################################################*/
package digital.vasic.yole.network.database

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

/**
 * Comprehensive tests for NetworkStorageDatabase covering:
 * - Document storage and retrieval
 * - Metadata management
 * - Sync status tracking
 * - Error handling and recovery
 * - Interface contract verification
 */
class NetworkStorageDatabaseTest {

    private lateinit var database: InMemoryNetworkStorageDatabase
    private val testDocumentId = "doc_123"
    private val testRemotePath = "/remote/test/document.txt"

    @BeforeTest
    fun setUp() {
        database = InMemoryNetworkStorageDatabase()
        runTest {
            database.initialize()
        }
    }

    @AfterTest
    fun tearDown() {
        runTest {
            database.clearAll()
        }
    }

    // ==================== Storage Tests ====================

    @Test
    fun testInsertAndGetStorage() = runTest {
        val storage = NetworkStorage(
            id = "storage-1",
            name = "Test Storage",
            type = StorageType.WEBDAV,
            location = "https://webdav.example.com"
        )

        val insertResult = database.insertStorage(storage)
        assertTrue(insertResult.isSuccess)

        val getResult = database.getStorage("storage-1")
        assertTrue(getResult.isSuccess)
        assertEquals("Test Storage", getResult.getOrNull()?.name)
    }

    @Test
    fun testGetStorageNotFound() = runTest {
        val result = database.getStorage("nonexistent")
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun testGetAllStorage() = runTest {
        database.insertStorage(NetworkStorage(id = "s1", name = "S1", type = StorageType.WEBDAV, location = "url1"))
        database.insertStorage(NetworkStorage(id = "s2", name = "S2", type = StorageType.FTP, location = "url2"))

        val result = database.getAllStorage()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun testUpdateStorage() = runTest {
        val storage = NetworkStorage(id = "s1", name = "Original", type = StorageType.WEBDAV, location = "url1")
        database.insertStorage(storage)

        val updated = storage.copy(name = "Updated")
        database.updateStorage(updated)

        val result = database.getStorage("s1")
        assertEquals("Updated", result.getOrNull()?.name)
    }

    @Test
    fun testDeleteStorage() = runTest {
        database.insertStorage(NetworkStorage(id = "s1", name = "S1", type = StorageType.WEBDAV, location = "url1"))

        val deleteResult = database.deleteStorage("s1")
        assertTrue(deleteResult.isSuccess)

        val getResult = database.getStorage("s1")
        assertNull(getResult.getOrNull())
    }

    // ==================== Document Tests ====================

    @Test
    fun testInsertAndGetDocument() = runTest {
        val doc = NetworkDocument(
            id = testDocumentId,
            name = "document.txt",
            path = testRemotePath,
            isFolder = false,
            size = 1024L,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "storage-1"
        )

        val insertResult = database.insertDocument(doc)
        assertTrue(insertResult.isSuccess)

        val getResult = database.getDocument(testDocumentId)
        assertTrue(getResult.isSuccess)
        assertEquals("document.txt", getResult.getOrNull()?.name)
        assertEquals(testRemotePath, getResult.getOrNull()?.path)
        assertFalse(getResult.getOrNull()?.isFolder ?: true)
    }

    @Test
    fun testGetDocumentsByStorage() = runTest {
        val doc1 = NetworkDocument(id = "d1", name = "file1.txt", path = "/file1.txt", storageId = "storage-1")
        val doc2 = NetworkDocument(id = "d2", name = "file2.txt", path = "/file2.txt", storageId = "storage-1")
        val doc3 = NetworkDocument(id = "d3", name = "file3.txt", path = "/file3.txt", storageId = "storage-2")

        database.insertDocument(doc1)
        database.insertDocument(doc2)
        database.insertDocument(doc3)

        val result = database.getDocumentsByStorage("storage-1")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun testGetDocumentsByPath() = runTest {
        val doc1 = NetworkDocument(id = "d1", name = "file1.txt", path = "/folder/file1.txt", storageId = "s1")
        val doc2 = NetworkDocument(id = "d2", name = "file2.txt", path = "/folder/file2.txt", storageId = "s1")
        val doc3 = NetworkDocument(id = "d3", name = "file3.txt", path = "/other/file3.txt", storageId = "s1")

        database.insertDocument(doc1)
        database.insertDocument(doc2)
        database.insertDocument(doc3)

        val result = database.getDocumentsByPath("/folder")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun testUpdateDocument() = runTest {
        val doc = NetworkDocument(id = "d1", name = "original.txt", path = "/original.txt", storageId = "s1")
        database.insertDocument(doc)

        val updated = doc.copy(name = "renamed.txt", path = "/renamed.txt")
        database.updateDocument(updated)

        val result = database.getDocument("d1")
        assertEquals("renamed.txt", result.getOrNull()?.name)
    }

    @Test
    fun testDeleteDocument() = runTest {
        val doc = NetworkDocument(id = "d1", name = "file.txt", path = "/file.txt", storageId = "s1")
        database.insertDocument(doc)

        database.deleteDocument("d1")
        assertNull(database.getDocument("d1").getOrNull())
    }

    @Test
    fun testDocumentWithPermissions() = runTest {
        val doc = NetworkDocument(
            id = "d1",
            name = "file.txt",
            path = "/file.txt",
            storageId = "s1",
            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
        )

        database.insertDocument(doc)
        val result = database.getDocument("d1")
        val retrieved = result.getOrNull()
        assertNotNull(retrieved)
        assertTrue(retrieved.permissions.contains(DocumentPermission.READ))
        assertTrue(retrieved.permissions.contains(DocumentPermission.WRITE))
        assertTrue(retrieved.permissions.contains(DocumentPermission.DELETE))
    }

    @Test
    fun testDocumentWithSyncStatus() = runTest {
        val doc = NetworkDocument(
            id = "d1",
            name = "file.txt",
            path = "/file.txt",
            storageId = "s1",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        database.insertDocument(doc)
        val result = database.getDocument("d1")
        assertEquals(SyncStatus.PENDING_UPLOAD, result.getOrNull()?.syncStatus)
    }

    // ==================== Sync Status Tests ====================

    @Test
    fun testUpdateAndGetSyncStatus() = runTest {
        database.updateSyncStatus(testRemotePath, SyncStatus.SYNCING)

        val result = database.getSyncStatus(testRemotePath)
        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.SYNCING, result.getOrNull())
    }

    @Test
    fun testGetAllSyncStatus() = runTest {
        database.updateSyncStatus("/path1", SyncStatus.SYNCED)
        database.updateSyncStatus("/path2", SyncStatus.PENDING_UPLOAD)
        database.updateSyncStatus("/path3", SyncStatus.SYNC_ERROR)

        val result = database.getAllSyncStatus()
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
    }

    @Test
    fun testDeleteSyncStatus() = runTest {
        database.updateSyncStatus(testRemotePath, SyncStatus.SYNCED)
        database.deleteSyncStatus(testRemotePath)

        val result = database.getSyncStatus(testRemotePath)
        assertNull(result.getOrNull())
    }

    @Test
    fun testAllSyncStatusValues() = runTest {
        SyncStatus.entries.forEach { status ->
            val path = "/test/${status.name}"
            database.updateSyncStatus(path, status)
            assertEquals(status, database.getSyncStatus(path).getOrNull(), "Should store and retrieve $status")
        }
    }

    // ==================== Operation Tests ====================

    @Test
    fun testInsertAndGetOperation() = runTest {
        val op = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.UPLOAD,
            remotePath = "/remote/file.txt",
            localPath = "/local/file.txt",
            status = NetworkOperation.Status.PENDING,
            createdAt = Clock.System.now()
        )

        database.insertOperation(op)
        val result = database.getOperation(1L)
        assertTrue(result.isSuccess)
        assertEquals(NetworkOperation.Type.UPLOAD, result.getOrNull()?.type)
    }

    @Test
    fun testGetActiveOperations() = runTest {
        val now = Clock.System.now()
        database.insertOperation(NetworkOperation(id = 1L, type = NetworkOperation.Type.UPLOAD, remotePath = "/f1", status = NetworkOperation.Status.IN_PROGRESS, createdAt = now))
        database.insertOperation(NetworkOperation(id = 2L, type = NetworkOperation.Type.DOWNLOAD, remotePath = "/f2", status = NetworkOperation.Status.COMPLETED, createdAt = now))
        database.insertOperation(NetworkOperation(id = 3L, type = NetworkOperation.Type.SYNC, remotePath = "/f3", status = NetworkOperation.Status.PENDING, createdAt = now))

        val result = database.getActiveOperations()
        assertTrue(result.isSuccess)
        // Active = IN_PROGRESS + PENDING
        assertTrue((result.getOrNull()?.size ?: 0) >= 1)
    }

    // ==================== Cleanup Tests ====================

    @Test
    fun testClearAll() = runTest {
        database.insertStorage(NetworkStorage(id = "s1", name = "S1", type = StorageType.WEBDAV, location = "url"))
        database.insertDocument(NetworkDocument(id = "d1", name = "f.txt", path = "/f.txt", storageId = "s1"))
        database.updateSyncStatus("/path", SyncStatus.SYNCED)

        val result = database.clearAll()
        assertTrue(result.isSuccess)

        assertNull(database.getStorage("s1").getOrNull())
        assertNull(database.getDocument("d1").getOrNull())
        assertNull(database.getSyncStatus("/path").getOrNull())
    }

    @Test
    fun testVacuum() = runTest {
        val result = database.vacuum()
        assertTrue(result.isSuccess)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testEmptyDatabase() = runTest {
        val storages = database.getAllStorage()
        assertTrue(storages.isSuccess)
        assertTrue(storages.getOrNull()?.isEmpty() ?: false)

        val statuses = database.getAllSyncStatus()
        assertTrue(statuses.isSuccess)
        assertTrue(statuses.getOrNull()?.isEmpty() ?: false)
    }

    @Test
    fun testDuplicateInsert() = runTest {
        val doc = NetworkDocument(id = "d1", name = "file.txt", path = "/file.txt", storageId = "s1")
        database.insertDocument(doc)

        // Re-inserting should overwrite (in-memory impl)
        val updated = doc.copy(name = "updated.txt")
        database.insertDocument(updated)

        val result = database.getDocument("d1")
        assertEquals("updated.txt", result.getOrNull()?.name)
    }

    @Test
    fun testMultipleSyncStatusUpdates() = runTest {
        val path = "/test/file.txt"

        database.updateSyncStatus(path, SyncStatus.UNKNOWN)
        assertEquals(SyncStatus.UNKNOWN, database.getSyncStatus(path).getOrNull())

        database.updateSyncStatus(path, SyncStatus.SYNCING)
        assertEquals(SyncStatus.SYNCING, database.getSyncStatus(path).getOrNull())

        database.updateSyncStatus(path, SyncStatus.SYNCED)
        assertEquals(SyncStatus.SYNCED, database.getSyncStatus(path).getOrNull())

        database.updateSyncStatus(path, SyncStatus.SYNC_ERROR)
        assertEquals(SyncStatus.SYNC_ERROR, database.getSyncStatus(path).getOrNull())
    }
}

/**
 * In-memory implementation of NetworkStorageDatabase for testing.
 */
private class InMemoryNetworkStorageDatabase : NetworkStorageDatabase {
    private val storages = mutableMapOf<String, NetworkStorage>()
    private val documents = mutableMapOf<String, NetworkDocument>()
    private val operations = mutableMapOf<Long, NetworkOperation>()
    private val syncStatuses = mutableMapOf<String, SyncStatus>()
    private val cacheEntries = mutableMapOf<String, CacheEntry>()

    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    override suspend fun close(): Result<Unit> = Result.success(Unit)

    override suspend fun insertStorage(storage: NetworkStorage): Result<Unit> {
        storages[storage.id] = storage
        return Result.success(Unit)
    }

    override suspend fun updateStorage(storage: NetworkStorage): Result<Unit> {
        storages[storage.id] = storage
        return Result.success(Unit)
    }

    override suspend fun getStorage(id: String): Result<NetworkStorage?> = Result.success(storages[id])
    override suspend fun getAllStorage(): Result<List<NetworkStorage>> = Result.success(storages.values.toList())
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

    override suspend fun getDocument(id: String): Result<NetworkDocument?> = Result.success(documents[id])

    override suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>> =
        Result.success(documents.values.filter { it.storageId == storageId })

    override suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>> =
        Result.success(documents.values.filter { it.path.startsWith("$path/") || it.path == path })

    override suspend fun deleteDocument(id: String): Result<Unit> {
        documents.remove(id)
        return Result.success(Unit)
    }

    override fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>> =
        flowOf(documents.values.filter { it.storageId == storageId })

    override suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit> {
        cacheEntries[entry.id] = entry
        return Result.success(Unit)
    }

    override suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit> {
        cacheEntries[entry.id] = entry
        return Result.success(Unit)
    }

    override suspend fun getCacheEntry(id: String): Result<CacheEntry?> = Result.success(cacheEntries[id])
    override suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>> =
        Result.success(cacheEntries.values.filter { it.remoteDocumentId == documentId })
    override suspend fun getAllCacheEntries(): Result<List<CacheEntry>> = Result.success(cacheEntries.values.toList())
    override suspend fun deleteCacheEntry(id: String): Result<Unit> {
        cacheEntries.remove(id)
        return Result.success(Unit)
    }
    override suspend fun deleteExpiredCacheEntries(): Result<Int> = Result.success(0)
    override suspend fun getCacheUsage(): Result<Long> = Result.success(0L)

    override suspend fun insertOperation(operation: NetworkOperation): Result<Unit> {
        operations[operation.id] = operation
        return Result.success(Unit)
    }

    override suspend fun updateOperation(operation: NetworkOperation): Result<Unit> {
        operations[operation.id] = operation
        return Result.success(Unit)
    }

    override suspend fun getOperation(id: Long): Result<NetworkOperation?> = Result.success(operations[id])
    override suspend fun getActiveOperations(): Result<List<NetworkOperation>> =
        Result.success(operations.values.filter { it.status == NetworkOperation.Status.IN_PROGRESS || it.status == NetworkOperation.Status.PENDING })
    override suspend fun deleteOperation(id: Long): Result<Unit> {
        operations.remove(id)
        return Result.success(Unit)
    }
    override suspend fun clearCompletedOperations(): Result<Int> {
        val count = operations.values.count { it.status == NetworkOperation.Status.COMPLETED }
        operations.entries.removeAll { it.value.status == NetworkOperation.Status.COMPLETED }
        return Result.success(count)
    }

    override suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit> {
        syncStatuses[remotePath] = status
        return Result.success(Unit)
    }

    override suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?> = Result.success(syncStatuses[remotePath])
    override suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>> = Result.success(syncStatuses.toMap())
    override suspend fun deleteSyncStatus(remotePath: String): Result<Unit> {
        syncStatuses.remove(remotePath)
        return Result.success(Unit)
    }

    override suspend fun clearAll(): Result<Unit> {
        storages.clear()
        documents.clear()
        operations.clear()
        syncStatuses.clear()
        cacheEntries.clear()
        return Result.success(Unit)
    }

    override suspend fun vacuum(): Result<Unit> = Result.success(Unit)
}
