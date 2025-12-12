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
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

/**
 * Tests for the database interface
 */
class DatabaseInterfaceTest {
    
    private lateinit var database: DatabaseInterface
    
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
        assertTrue(database.isReady())
        
        val result = database.close()
        assertTrue(result.isSuccess)
        assertFalse(database.isReady())
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
        assertEquals(storage, getResult.getOrNull())
        
        // Update storage
        val updatedStorage = storage.copy(name = "Updated Storage")
        val updateResult = database.updateStorage(updatedStorage)
        assertTrue(updateResult.isSuccess)
        
        val getUpdatedResult = database.getStorage(storage.id)
        assertEquals("Updated Storage", getUpdatedResult.getOrNull()?.name)
        
        // Get all storage
        val allStorageResult = database.getAllStorage()
        assertTrue(allStorageResult.isSuccess)
        assertEquals(1, allStorageResult.getOrNull()?.size)
        
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
        assertEquals(document, getResult.getOrNull())
        
        // Search documents
        val searchResult = database.searchDocuments("test")
        assertTrue(searchResult.isSuccess)
        assertEquals(1, searchResult.getOrNull()?.size)
        
        // Get documents by storage
        val byStorageResult = database.getDocumentsByStorage(storage.id)
        assertTrue(byStorageResult.isSuccess)
        assertEquals(1, byStorageResult.getOrNull()?.size)
        
        // Delete document
        val deleteResult = database.deleteDocument(document.id)
        assertTrue(deleteResult.isSuccess)
        
        val getAfterDeleteResult = database.getDocument(document.id)
        assertNull(getAfterDeleteResult.getOrNull())
    }
    
    @Test
    fun testCacheEntryOperations() = runTest {
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
        assertEquals(cacheEntry, getResult.getOrNull())
        
        // Get cache entries by document
        val byDocumentResult = database.getCacheEntriesByDocument(document.id)
        assertTrue(byDocumentResult.isSuccess)
        assertEquals(1, byDocumentResult.getOrNull()?.size)
        
        // Get cache usage
        val usageResult = database.getCacheUsage()
        assertTrue(usageResult.isSuccess)
        assertEquals(1024L, usageResult.getOrNull())
        
        // Delete expired cache entries (none should be expired)
        val deleteExpiredResult = database.deleteExpiredCacheEntries()
        assertTrue(deleteExpiredResult.isSuccess)
        assertEquals(0, deleteExpiredResult.getOrNull())
    }
    
    @Test
    fun testOperationOperations() = runTest {
        val operation = NetworkOperation(
            id = 1L,
            type = OperationType.UPLOAD,
            remotePath = "/test.txt",
            localPath = "/local/test.txt",
            status = OperationStatus.RUNNING,
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
        val updatedOperation = operation.copy(status = OperationStatus.COMPLETED)
        val updateResult = database.updateOperation(updatedOperation)
        assertTrue(updateResult.isSuccess)
        
        // Get operations by status
        val byStatusResult = database.getOperationsByStatus("COMPLETED")
        assertTrue(byStatusResult.isSuccess)
        assertEquals(1, byStatusResult.getOrNull()?.size)
        
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
    fun testSettingsOperations() = runTest {
        val key = "test_setting"
        val value = "test_value"
        
        // Set setting
        val setResult = database.setSetting(key, value)
        assertTrue(setResult.isSuccess)
        
        // Get setting
        val getResult = database.getSetting(key)
        assertTrue(getResult.isSuccess)
        assertEquals(value, getResult.getOrNull())
        
        // Get all settings
        val allResult = database.getAllSettings()
        assertTrue(allResult.isSuccess)
        assertEquals(1, allResult.getOrNull()?.size)
        
        // Delete setting
        val deleteResult = database.deleteSetting(key)
        assertTrue(deleteResult.isSuccess)
        
        val getAfterDeleteResult = database.getSetting(key)
        assertNull(getAfterDeleteResult.getOrNull())
    }
    
    @Test
    fun testDocumentMetadataOperations() = runTest {
        val document = NetworkDocument.mock()
        database.insertDocument(document).getOrThrow()
        
        val metadata = mapOf("author" to "Test Author", "version" to "1.0")
        
        // Set document metadata
        val setResult = database.setDocumentMetadata(document.id, metadata)
        assertTrue(setResult.isSuccess)
        
        // Get document metadata
        val getResult = database.getDocumentMetadata(document.id)
        assertTrue(getResult.isSuccess)
        assertEquals(metadata, getResult.getOrNull())
        
        // Search document metadata
        val searchResult = database.searchDocumentMetadata("author")
        assertTrue(searchResult.isSuccess)
        assertEquals(1, searchResult.getOrNull()?.size)
        assertEquals(document.id, searchResult.getOrNull()?.first())
        
        // Delete document metadata
        val deleteResult = database.deleteDocumentMetadata(document.id)
        assertTrue(deleteResult.isSuccess)
        
        val getAfterDeleteResult = database.getDocumentMetadata(document.id)
        assertEquals(0, getAfterDeleteResult.getOrNull()?.size)
    }
    
    @Test
    fun testDatabaseStats() = runTest {
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
            type = OperationType.UPLOAD,
            remotePath = "/test.txt",
            status = OperationStatus.RUNNING,
            progress = 0.5,
            totalSize = 1024L,
            bytesTransferred = 512L,
            createdAt = Clock.System.now()
        )
        
        database.insertStorage(storage).getOrThrow()
        database.insertDocument(document).getOrThrow()
        database.insertCacheEntry(cacheEntry).getOrThrow()
        database.insertOperation(operation).getOrThrow()
        database.setSetting("test", "value").getOrThrow()
        
        val statsResult = database.getDatabaseStats()
        assertTrue(statsResult.isSuccess)
        
        val stats = statsResult.getOrNull()
        assertNotNull(stats)
        assertTrue(stats.totalSize > 0)
        assertTrue(stats.tableCounts.isNotEmpty())
        assertEquals(1024L, stats.cacheSize)
        assertEquals(1L, stats.operationCount)
    }
    
    @Test
    fun testTransaction() = runTest {
        val storage1 = NetworkStorage.mock("storage1")
        val storage2 = NetworkStorage.mock("storage2")
        
        val transactionResult = database.transaction {
            database.insertStorage(storage1).getOrThrow()
            database.insertStorage(storage2).getOrThrow()
            "success"
        }
        
        assertTrue(transactionResult.isSuccess)
        assertEquals("success", transactionResult.getOrNull())
        
        val allStorageResult = database.getAllStorage()
        assertEquals(2, allStorageResult.getOrNull()?.size)
    }
    
    @Test
    fun testBackupAndRestore() = runTest {
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
            type = OperationType.UPLOAD,
            remotePath = "/test.txt",
            status = OperationStatus.RUNNING,
            progress = 0.5,
            totalSize = 1024L,
            bytesTransferred = 512L,
            createdAt = Clock.System.now()
        )
        
        database.insertStorage(storage).getOrThrow()
        database.insertDocument(document).getOrThrow()
        database.insertCacheEntry(cacheEntry).getOrThrow()
        database.insertOperation(operation).getOrThrow()
        database.setSetting("test", "value").getOrThrow()
        database.updateSyncStatus("/test.txt", SyncStatus.SYNCED).getOrThrow()
        
        // Export data
        val exportResult = database.exportData()
        assertTrue(exportResult.isSuccess)
        
        val backup = exportResult.getOrNull()
        assertNotNull(backup)
        assertEquals(1, backup.storage.size)
        assertEquals(1, backup.documents.size)
        assertEquals(1, backup.cacheEntries.size)
        assertEquals(1, backup.operations.size)
        assertEquals(1, backup.settings.size)
        assertEquals(1, backup.syncStatus.size)
        
        // Clear database
        database.clearAll().getOrThrow()
        
        val emptyStatsResult = database.getDatabaseStats()
        val emptyStats = emptyStatsResult.getOrNull()
        assertEquals(0L, emptyStats?.operationCount)
        
        // Import data
        val importResult = database.importData(backup)
        assertTrue(importResult.isSuccess)
        
        // Verify data was restored
        val restoredStatsResult = database.getDatabaseStats()
        val restoredStats = restoredStatsResult.getOrNull()
        assertEquals(1L, restoredStats?.operationCount)
        
        val restoredStorageResult = database.getAllStorage()
        assertEquals(1, restoredStorageResult.getOrNull()?.size)
    }
    
    @Test
    fun testValidateData() = runTest {
        // Insert valid data
        val storage = NetworkStorage.mock()
        val document = NetworkDocument.mock()
        val cacheEntry = CacheEntry.create(
            remoteDocumentId = document.id,
            localPath = "/cache/test.txt",
            remotePath = "/test.txt",
            size = 1024L
        )
        
        database.insertStorage(storage).getOrThrow()
        database.insertDocument(document).getOrThrow()
        database.insertCacheEntry(cacheEntry).getOrThrow()
        
        // Validation should pass
        val validateResult = database.validateData()
        assertTrue(validateResult.isSuccess)
        assertEquals(0, validateResult.getOrNull()?.size)
        
        // Insert invalid data (cache entry referencing non-existent document)
        val invalidCacheEntry = CacheEntry.create(
            remoteDocumentId = "non-existent",
            localPath = "/cache/invalid.txt",
            remotePath = "/invalid.txt",
            size = 512L
        )
        database.insertCacheEntry(invalidCacheEntry).getOrThrow()
        
        // Validation should fail
        val validateInvalidResult = database.validateData()
        assertTrue(validateInvalidResult.isSuccess)
        assertTrue(validateInvalidResult.getOrNull()?.isNotEmpty() == true)
    }
    
    @Test
    fun testErrorHandling() = runTest {
        // Test invalid storage
        val invalidStorage = NetworkStorage(
            id = "", // Invalid ID
            name = "Test",
            type = StorageType.WEBDAV,
            location = "http://example.com"
        )
        
        val insertInvalidResult = database.insertStorage(invalidStorage)
        assertTrue(insertInvalidResult.isFailure)
        
        // Test operations on closed database
        database.close().getOrThrow()
        
        val closedResult = database.insertStorage(NetworkStorage.mock())
        assertTrue(closedResult.isFailure)
    }
}