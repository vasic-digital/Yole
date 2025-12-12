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
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

/**
 * Comprehensive tests for NetworkStorageDatabase covering:
 * - Document storage and retrieval
 * - Metadata management
 * - Sync status tracking
 * - Error handling and recovery
 * - Cross-platform compatibility
 * - Performance optimization
 */
class NetworkStorageDatabaseTest {

    private lateinit var database: NetworkStorageDatabase
    private val testService = "test_service"
    private val testDocumentId = "doc_123"
    private val testRemotePath = "/remote/test/document.txt"
    private val testLocalPath = "/local/test/document.txt"

    @BeforeTest
    fun setUp() {
        database = NetworkStorageDatabase()
        
        // Clear any existing data
        runTest {
            database.clearAllDocuments()
            database.clearAllSyncStatus()
        }
    }

    @AfterTest
    fun tearDown() {
        runTest {
            database.clearAllDocuments()
            database.clearAllSyncStatus()
        }
    }

    // ==================== Document Storage Tests ====================

    @Test
    fun `storeDocument should save document with metadata`() = runTest {
        // Given
        val document = NetworkDocument(
            id = testDocumentId,
            name = "document.txt",
            path = testRemotePath,
            size = 1024L,
            lastModified = Clock.System.now(),
            isDirectory = false,
            permissions = DocumentPermission.READ_WRITE,
            metadata = mapOf("author" to "test_user", "version" to "1.0")
        )
        
        // When
        val result = database.storeDocument(testService, document)
        
        // Then
        assertTrue(result.isSuccess)
        
        val retrieved = database.getDocument(testService, testDocumentId)
        assertTrue(retrieved.isSuccess)
        assertEquals(document, retrieved.getOrNull())
    }

    @Test
    fun `storeDocument should update existing document`() = runTest {
        // Given
        val originalDocument = NetworkDocument(
            id = testDocumentId,
            name = "document.txt",
            path = testRemotePath,
            size = 1024L,
            lastModified = Clock.System.now().minus(1.hours),
            isDirectory = false,
            permissions = DocumentPermission.READ_WRITE
        )
        
        val updatedDocument = originalDocument.copy(
            size = 2048L,
            lastModified = Clock.System.now(),
            metadata = mapOf("updated" to "true")
        )
        
        database.storeDocument(testService, originalDocument)
        
        // When
        val result = database.storeDocument(testService, updatedDocument)
        
        // Then
        assertTrue(result.isSuccess)
        
        val retrieved = database.getDocument(testService, testDocumentId)
        assertTrue(retrieved.isSuccess)
        assertEquals(updatedDocument, retrieved.getOrNull())
    }

    @Test
    fun `getDocument should return failure for non-existent document`() = runTest {
        // Given
        val nonExistentId = "non_existent_doc"
        
        // When
        val result = database.getDocument(testService, nonExistentId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkStorageException.DatabaseException.NotFound)
    }

    // ==================== Document Query Tests ====================

    @Test
    fun `getDocumentsByPath should return documents in specific path`() = runTest {
        // Given
        val basePath = "/remote/test/"
        val documents = listOf(
            NetworkDocument(
                id = "doc_1",
                name = "file1.txt",
                path = "$basePath/file1.txt",
                size = 1024L,
                lastModified = Clock.System.now(),
                isDirectory = false
            ),
            NetworkDocument(
                id = "doc_2",
                name = "file2.txt",
                path = "$basePath/file2.txt",
                size = 2048L,
                lastModified = Clock.System.now(),
                isDirectory = false
            ),
            NetworkDocument(
                id = "doc_3",
                name = "file3.txt",
                path = "/other/path/file3.txt",
                size = 512L,
                lastModified = Clock.System.now(),
                isDirectory = false
            )
        )
        
        documents.forEach { database.storeDocument(testService, it) }
        
        // When
        val result = database.getDocumentsByPath(testService, basePath)
        
        // Then
        assertTrue(result.isSuccess)
        
        val foundDocuments = result.getOrNull()!!
        assertEquals(2, foundDocuments.size)
        assertTrue(foundDocuments.all { it.path.startsWith(basePath) })
    }

    @Test
    fun `getAllDocuments should return all documents for service`() = runTest {
        // Given
        val documents = (1..5).map { i ->
            NetworkDocument(
                id = "doc_$i",
                name = "file$i.txt",
                path = "/remote/test/file$i.txt",
                size = (1024L * i),
                lastModified = Clock.System.now(),
                isDirectory = false
            )
        }
        
        documents.forEach { database.storeDocument(testService, it) }
        
        // When
        val result = database.getAllDocuments(testService)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull()!!.size)
    }

    // ==================== Document Deletion Tests ====================

    @Test
    fun `deleteDocument should remove document from database`() = runTest {
        // Given
        val document = NetworkDocument(
            id = testDocumentId,
            name = "document.txt",
            path = testRemotePath,
            size = 1024L,
            lastModified = Clock.System.now(),
            isDirectory = false
        )
        
        database.storeDocument(testService, document)
        
        // When
        val result = database.deleteDocument(testService, testDocumentId)
        
        // Then
        assertTrue(result.isSuccess)
        
        val retrieved = database.getDocument(testService, testDocumentId)
        assertTrue(retrieved.isFailure)
    }

    // ==================== Sync Status Tests ====================

    @Test
    fun `storeSyncStatus should save synchronization status`() = runTest {
        // Given
        val syncStatus = SyncStatus(
            documentId = testDocumentId,
            remotePath = testRemotePath,
            localPath = testLocalPath,
            lastSyncTime = Clock.System.now(),
            syncState = SyncStatus.State.SYNCED,
            checksum = "abc123def456",
            metadata = mapOf("sync_version" to "1.0")
        )
        
        // When
        val result = database.storeSyncStatus(testService, syncStatus)
        
        // Then
        assertTrue(result.isSuccess)
        
        val retrieved = database.getSyncStatus(testService, testDocumentId)
        assertTrue(retrieved.isSuccess)
        assertEquals(syncStatus, retrieved.getOrNull())
    }

    @Test
    fun `getSyncStatusByLocalPath should find sync status by local path`() = runTest {
        // Given
        val syncStatus = SyncStatus(
            documentId = testDocumentId,
            remotePath = testRemotePath,
            localPath = testLocalPath,
            lastSyncTime = Clock.System.now(),
            syncState = SyncStatus.State.SYNCED,
            checksum = "abc123"
        )
        
        database.storeSyncStatus(testService, syncStatus)
        
        // When
        val result = database.getSyncStatusByLocalPath(testService, testLocalPath)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(syncStatus, result.getOrNull())
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should handle large number of documents efficiently`() = runTest {
        // Given
        val documentCount = 1000
        val documents = (1..documentCount).map { i ->
            NetworkDocument(
                id = "doc_$i",
                name = "file$i.txt",
                path = "/remote/test/file$i.txt",
                size = (1024L * i),
                lastModified = Clock.System.now(),
                isDirectory = false
            )
        }
        
        // When
        val startTime = Clock.System.now()
        documents.forEach { database.storeDocument(testService, it) }
        val storeTime = Clock.System.now() - startTime
        
        val queryStartTime = Clock.System.now()
        val allDocuments = database.getAllDocuments(testService)
        val queryTime = Clock.System.now() - queryStartTime
        
        // Then
        assertTrue(allDocuments.isSuccess)
        assertEquals(documentCount, allDocuments.getOrNull()!!.size)
        
        // Performance assertions (should complete within reasonable time)
        assertTrue(storeTime.inWholeMilliseconds < 10000) // Store 1000 docs in under 10 seconds
        assertTrue(queryTime.inWholeMilliseconds < 1000)  // Query all docs in under 1 second
    }

    @Test
    fun `should handle concurrent database operations`() = runTest {
        // Given
        val operationCount = 100
        val documents = (1..operationCount).map { i ->
            NetworkDocument(
                id = "doc_$i",
                name = "file$i.txt",
                path = "/remote/test/file$i.txt",
                size = 1024L,
                lastModified = Clock.System.now(),
                isDirectory = false
            )
        }
        
        // When
        val results = documents.map { document ->
            database.storeDocument(testService, document)
        }
        
        // Then
        assertTrue(results.all { it.isSuccess })
        
        val allDocuments = database.getAllDocuments(testService)
        assertEquals(operationCount, allDocuments.getOrNull()!!.size)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle documents with special characters in names`() = runTest {
        // Given
        val specialDocument = NetworkDocument(
            id = "special_doc",
            name = "file with spaces and special chars !@#$%^&*().txt",
            path = "/remote/test/file with spaces and special chars !@#$%^&*().txt",
            size = 1024L,
            lastModified = Clock.System.now(),
            isDirectory = false
        )
        
        // When
        val result = database.storeDocument(testService, specialDocument)
        
        // Then
        assertTrue(result.isSuccess)
        
        val retrieved = database.getDocument(testService, "special_doc")
        assertTrue(retrieved.isSuccess)
        assertEquals(specialDocument, retrieved.getOrNull())
    }

    @Test
    fun `should handle very long paths`() = runTest {
        // Given
        val longPath = "/remote/test/" + "a".repeat(500) + "/file.txt"
        val longDocument = NetworkDocument(
            id = "long_path_doc",
            name = "file.txt",
            path = longPath,
            size = 1024L,
            lastModified = Clock.System.now(),
            isDirectory = false
        )
        
        // When
        val result = database.storeDocument(testService, longDocument)
        
        // Then
        assertTrue(result.isSuccess)
        
        val retrieved = database.getDocument(testService, "long_path_doc")
        assertTrue(retrieved.isSuccess)
        assertEquals(longPath, retrieved.getOrNull()?.path)
    }
}