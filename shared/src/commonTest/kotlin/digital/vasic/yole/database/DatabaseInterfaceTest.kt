/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for the unified database interface
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.test.runTest
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.database.NetworkStorageDatabase
import digital.vasic.yole.network.common.NetworkOperation.Type
import digital.vasic.yole.network.common.NetworkOperation.Status
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.hours

/**
 * Tests for the database interface
 */
class DatabaseInterfaceTest {
    
    private lateinit var database: NetworkStorageDatabase
    
    @BeforeTest
    fun setup() = runTest {
        // Use in-memory database for testing
        database = InMemoryDatabase()
        database.initialize().getOrThrow()
    }
    
    @AfterTest
    fun tearDown() = runTest {
        database.close().getOrThrow()
    }
    
    @Test
    fun testInitializeAndClose() = runTest {
        // Test that database is properly initialized
        val initResult = database.initialize()
        assertTrue(initResult.isSuccess)
        
        // Test that database can be closed
        val closeResult = database.close()
        assertTrue(closeResult.isSuccess)
    }
    
    @Test
    fun testStorageOperations() = runTest {
        val storage = NetworkStorage.mock()
        
        // Insert storage
        val insertResult = database.insertStorage(storage)
        assertTrue(insertResult.isSuccess)
        
        // Get storage
        val getResult = database.getStorage(storage.id)
        assertTrue(getResult.isSuccess)
        assertEquals(storage.id, getResult.getOrNull()?.id)
        assertEquals(storage.name, getResult.getOrNull()?.name)
        
        // Update storage
        val updatedStorage = storage.copy(name = "Updated Storage")
        val updateResult = database.updateStorage(updatedStorage)
        assertTrue(updateResult.isSuccess)
        
        val getUpdatedResult = database.getStorage(storage.id)
        assertEquals("Updated Storage", getUpdatedResult.getOrNull()?.name)
        
        // Get all storage
        val getAllResult = database.getAllStorage()
        assertTrue(getAllResult.isSuccess)
        assertEquals(1, getAllResult.getOrNull()?.size)
        
        // Delete storage
        val deleteResult = database.deleteStorage(storage.id)
        assertTrue(deleteResult.isSuccess)
        
        val getAfterDeleteResult = database.getStorage(storage.id)
        assertNull(getAfterDeleteResult.getOrNull())
    }
    
    @Test
    fun testDocumentOperations() = runTest {
        val storage = NetworkStorage.mock()
        database.insertStorage(storage).getOrThrow()
        
        val document = NetworkDocument.mock()
        
        // Insert document
        val insertResult = database.insertDocument(document)
        assertTrue(insertResult.isSuccess)
        
        // Get document
        val getResult = database.getDocument(document.id)
        assertTrue(getResult.isSuccess)
        assertEquals(document.id, getResult.getOrNull()?.id)
        assertEquals(document.name, getResult.getOrNull()?.name)
        
        // Update document
        val updatedDocument = document.copy(name = "Updated Document")
        val updateResult = database.updateDocument(updatedDocument)
        assertTrue(updateResult.isSuccess)
        
        val getUpdatedResult = database.getDocument(document.id)
        assertEquals("Updated Document", getUpdatedResult.getOrNull()?.name)
        
        // Get documents by storage
        val byStorageResult = database.getDocumentsByStorage(storage.id)
        assertTrue(byStorageResult.isSuccess)
        assertEquals(1, byStorageResult.getOrNull()?.size)
        
        // Get documents by path
        val byPathResult = database.getDocumentsByPath(document.path)
        assertTrue(byPathResult.isSuccess)
        assertEquals(1, byPathResult.getOrNull()?.size)
        
        // Delete document
        val deleteResult = database.deleteDocument(document.id)
        assertTrue(deleteResult.isSuccess)
        
        val getAfterDeleteResult = database.getDocument(document.id)
        assertNull(getAfterDeleteResult.getOrNull())
    }
    
    @Test
    fun testCacheOperations() = runTest {
        val document = NetworkDocument.mock()
        database.insertDocument(document).getOrThrow()
        
        val cacheEntry = CacheEntry.create(
            remoteDocumentId = document.id,
            localPath = "/cache/test.txt",
            remotePath = "/test.txt",
            size = 1024L
        )
        
        // Insert cache entry
        val insertResult = database.insertCacheEntry(cacheEntry)
        assertTrue(insertResult.isSuccess)
        
        // Get cache entry
        val getResult = database.getCacheEntry(cacheEntry.id)
        assertTrue(getResult.isSuccess)
        assertEquals(cacheEntry.id, getResult.getOrNull()?.id)
        
        // Update cache entry
        val updatedCacheEntry = cacheEntry.copy(size = 2048L)
        val updateResult = database.updateCacheEntry(updatedCacheEntry)
        assertTrue(updateResult.isSuccess)
        
        val getUpdatedResult = database.getCacheEntry(cacheEntry.id)
        assertEquals(2048L, getUpdatedResult.getOrNull()?.size)
        
        // Get cache entries by document
        val byDocumentResult = database.getCacheEntriesByDocument(document.id)
        assertTrue(byDocumentResult.isSuccess)
        assertEquals(1, byDocumentResult.getOrNull()?.size)
        
        // Get all cache entries
        val allResult = database.getAllCacheEntries()
        assertTrue(allResult.isSuccess)
        assertEquals(1, allResult.getOrNull()?.size)
        
        // Get cache usage
        val usageResult = database.getCacheUsage()
        assertTrue(usageResult.isSuccess)
        assertEquals(2048L, usageResult.getOrNull())
        
        // Delete cache entry
        val deleteResult = database.deleteCacheEntry(cacheEntry.id)
        assertTrue(deleteResult.isSuccess)
        
        val getAfterDeleteResult = database.getCacheEntry(cacheEntry.id)
        assertNull(getAfterDeleteResult.getOrNull())
    }
    
    @Test
    fun testExpiredCacheCleanup() = runTest {
        val document = NetworkDocument.mock()
        database.insertDocument(document).getOrThrow()
        
        // Create expired cache entry
        val expiredEntry = CacheEntry(
            id = "expired1",
            remoteDocumentId = document.id,
            localPath = "/cache/expired.txt",
            remotePath = "/expired.txt",
            size = 1024L,
            createdAt = Clock.System.now().minus(2.hours),
            lastAccessed = Clock.System.now().minus(2.hours),
            lastModified = Clock.System.now().minus(2.hours),
            expiresAt = Clock.System.now().minus(1.hours),
            checksum = "checksum1"
        )
        
        // Create non-expired cache entry
        val validEntry = CacheEntry(
            id = "valid1",
            remoteDocumentId = document.id,
            localPath = "/cache/valid.txt",
            remotePath = "/valid.txt",
            size = 2048L,
            createdAt = Clock.System.now(),
            lastAccessed = Clock.System.now(),
            lastModified = Clock.System.now(),
            expiresAt = Clock.System.now().plus(1.hours),
            checksum = "checksum2"
        )
        
        database.insertCacheEntry(expiredEntry).getOrThrow()
        database.insertCacheEntry(validEntry).getOrThrow()
        
        // Delete expired cache entries
        val deleteExpiredResult = database.deleteExpiredCacheEntries()
        assertTrue(deleteExpiredResult.isSuccess)
        assertEquals(1, deleteExpiredResult.getOrNull())
        
        // Verify expired entry is gone
        val expiredGetResult = database.getCacheEntry(expiredEntry.id)
        assertNull(expiredGetResult.getOrNull())
        
        // Verify valid entry is still there
        val validGetResult = database.getCacheEntry(validEntry.id)
        assertNotNull(validGetResult.getOrNull())
    }
    
    @Test
    fun testOperationOperations() = runTest {
        val operation = NetworkOperation(
            id = 1L,
            type = Type.UPLOAD,
            remotePath = "/test.txt",
            localPath = "/local/test.txt",
            status = Status.IN_PROGRESS,
            progress = 0.5,
            totalSize = 1024L,
            bytesTransferred = 512L,
            createdAt = Clock.System.now(),
            retryCount = 0,
            maxRetries = 3,
            priority = 100,
            canPause = true,
            canCancel = true,
            isPaused = false
        )
        
        // Insert operation
        val insertResult = database.insertOperation(operation)
        assertTrue(insertResult.isSuccess)
        
        // Get operation
        val getResult = database.getOperation(operation.id)
        assertTrue(getResult.isSuccess)
        assertEquals(operation.id, getResult.getOrNull()?.id)
        
        // Get active operations
        val activeResult = database.getActiveOperations()
        assertTrue(activeResult.isSuccess)
        assertEquals(1, activeResult.getOrNull()?.size)
        
        // Update operation status
        val updatedOperation = operation.copy(status = Status.COMPLETED)
        val updateResult = database.updateOperation(updatedOperation)
        assertTrue(updateResult.isSuccess)
        
        // Clear completed operations
        val clearResult = database.clearCompletedOperations()
        assertTrue(clearResult.isSuccess)
        assertEquals(1, clearResult.getOrNull())
        
        val activeAfterClearResult = database.getActiveOperations()
        assertEquals(0, activeAfterClearResult.getOrNull()?.size)
    }
    
    @Test
    fun testSyncStatusOperations() = runTest {
        val remotePath = "/test.txt"
        val syncStatus = SyncStatus.SYNCING
        
        // Update sync status
        val updateResult = database.updateSyncStatus(remotePath, syncStatus)
        assertTrue(updateResult.isSuccess)
        
        // Get sync status
        val getResult = database.getSyncStatus(remotePath)
        assertTrue(getResult.isSuccess)
        assertEquals(syncStatus, getResult.getOrNull())
        
        // Get all sync status
        val allResult = database.getAllSyncStatus()
        assertTrue(allResult.isSuccess)
        assertEquals(1, allResult.getOrNull()?.size)
        
        // Delete sync status
        val deleteResult = database.deleteSyncStatus(remotePath)
        assertTrue(deleteResult.isSuccess)
        
        val getAfterDeleteResult = database.getSyncStatus(remotePath)
        assertNull(getAfterDeleteResult.getOrNull())
    }
    
    @Test
    fun testClearAll() = runTest {
        // Insert some test data
        val storage = NetworkStorage.mock()
        val document = NetworkDocument.mock()
        val cacheEntry = CacheEntry.create(
            remoteDocumentId = document.id,
            localPath = "/cache/test.txt",
            remotePath = "/test.txt",
            size = 1024L
        )
        val operation = NetworkOperation(
            id = 1L,
            type = Type.UPLOAD,
            remotePath = "/test.txt",
            status = Status.IN_PROGRESS,
            progress = 0.5,
            totalSize = 1024L,
            bytesTransferred = 512L,
            createdAt = Clock.System.now()
        )
        
        database.insertStorage(storage).getOrThrow()
        database.insertDocument(document).getOrThrow()
        database.insertCacheEntry(cacheEntry).getOrThrow()
        database.insertOperation(operation).getOrThrow()
        database.updateSyncStatus("/test.txt", SyncStatus.SYNCING).getOrThrow()
        
        // Verify data exists
        assertEquals(1, database.getAllStorage().getOrNull()?.size)
        assertEquals(1, database.getDocumentsByStorage(storage.id).getOrNull()?.size)
        assertEquals(1024L, database.getCacheUsage().getOrNull())
        assertEquals(1, database.getActiveOperations().getOrNull()?.size)
        assertEquals(1, database.getAllSyncStatus().getOrNull()?.size)
        
        // Clear all data
        val clearResult = database.clearAll()
        assertTrue(clearResult.isSuccess)
        
        // Verify all data is cleared
        assertEquals(0, database.getAllStorage().getOrNull()?.size)
        assertEquals(0, database.getDocumentsByStorage(storage.id).getOrNull()?.size)
        assertEquals(0L, database.getCacheUsage().getOrNull())
        assertEquals(0, database.getActiveOperations().getOrNull()?.size)
        assertEquals(0, database.getAllSyncStatus().getOrNull()?.size)
    }
    
    @Test
    fun testVacuum() = runTest {
        // Vacuum operation should succeed (no-op for in-memory database)
        val vacuumResult = database.vacuum()
        assertTrue(vacuumResult.isSuccess)
    }
    
    @Test
    fun testObserveDocumentsByStorage() = runTest {
        val storage = NetworkStorage.mock()
        database.insertStorage(storage).getOrThrow()
        
        val document1 = NetworkDocument.mock(storageId = storage.id)
        val document2 = NetworkDocument.mock(storageId = storage.id)
        
        database.insertDocument(document1).getOrThrow()
        database.insertDocument(document2).getOrThrow()
        
        // Observe documents by storage
        val observedDocuments = database.observeDocumentsByStorage(storage.id)
        
        // Collect the flow and verify
        observedDocuments.collect { documents ->
            assertEquals(2, documents.size)
            assertTrue(documents.any { it.id == document1.id })
            assertTrue(documents.any { it.id == document2.id })
        }
    }
}