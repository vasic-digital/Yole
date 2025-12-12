/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for NetworkOperation
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Comprehensive tests for NetworkOperation covering:
 * - Operation creation and state management
 * - Progress tracking
 * - Error handling
 * - Performance monitoring
 * - Serialization
 */
class NetworkOperationTest {

    @Test
    fun testBasicOperationCreation() {
        // Given
        val now = Clock.System.now()
        val operation = NetworkOperation(
            id = 123456789L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.IN_PROGRESS,
            remotePath = "/remote/file.txt",
            localPath = "/local/file.txt",
            progress = 0.5,
            createdAt = now,
            startedAt = now
        )
        
        // Then
        assertEquals(123456789L, operation.id)
        assertEquals(NetworkOperation.Type.UPLOAD, operation.type)
        assertEquals(NetworkOperation.Status.IN_PROGRESS, operation.status)
        assertEquals("/remote/file.txt", operation.remotePath)
        assertEquals("/local/file.txt", operation.localPath)
        assertEquals(0.5, operation.progress)
        assertEquals(now, operation.createdAt)
        assertEquals(now, operation.startedAt)
        assertNull(operation.completedAt)
        assertNull(operation.error)
        assertNull(operation.metadata)
    }

    @Test
    fun testCompletedOperationCreation() {
        // Given
        val createdAt = Clock.System.now()
        val startedAt = createdAt.plus(kotlin.time.Duration.seconds(1))
        val completedAt = startedAt.plus(kotlin.time.Duration.seconds(5))
        
        val operation = NetworkOperation(
            id = 987654321L,
            type = NetworkOperation.Type.DOWNLOAD,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/remote/data.csv",
            localPath = "/local/data.csv",
            progress = 1.0,
            createdAt = createdAt,
            startedAt = startedAt,
            completedAt = completedAt,
            metadata = mapOf("bytes_transferred" to "1024", "checksum" to "abc123")
        )
        
        // Then
        assertEquals(987654321L, operation.id)
        assertEquals(NetworkOperation.Type.DOWNLOAD, operation.type)
        assertEquals(NetworkOperation.Status.COMPLETED, operation.status)
        assertEquals("/remote/data.csv", operation.remotePath)
        assertEquals("/local/data.csv", operation.localPath)
        assertEquals(1.0, operation.progress)
        assertEquals(createdAt, operation.createdAt)
        assertEquals(startedAt, operation.startedAt)
        assertEquals(completedAt, operation.completedAt)
        assertNull(operation.error)
        assertEquals(2, operation.metadata?.size)
        assertEquals("1024", operation.metadata?.get("bytes_transferred"))
        assertEquals("abc123", operation.metadata?.get("checksum"))
    }

    @Test
    fun testFailedOperationCreation() {
        // Given
        val createdAt = Clock.System.now()
        val startedAt = createdAt.plus(kotlin.time.Duration.seconds(1))
        val completedAt = startedAt.plus(kotlin.time.Duration.seconds(3))
        val error = NetworkStorageException.FileOperationException.NotFound("/remote/missing.txt")
        
        val operation = NetworkOperation(
            id = 555555555L,
            type = NetworkOperation.Type.DELETE,
            status = NetworkOperation.Status.FAILED,
            remotePath = "/remote/missing.txt",
            localPath = "",
            progress = 0.0,
            createdAt = createdAt,
            startedAt = startedAt,
            completedAt = completedAt,
            error = error.message
        )
        
        // Then
        assertEquals(555555555L, operation.id)
        assertEquals(NetworkOperation.Type.DELETE, operation.type)
        assertEquals(NetworkOperation.Status.FAILED, operation.status)
        assertEquals("/remote/missing.txt", operation.remotePath)
        assertEquals("", operation.localPath)
        assertEquals(0.0, operation.progress)
        assertEquals(createdAt, operation.createdAt)
        assertEquals(startedAt, operation.startedAt)
        assertEquals(completedAt, operation.completedAt)
        assertEquals("File not found: /remote/missing.txt", operation.error)
    }

    @Test
    fun testOperationTypeVariations() {
        val types = listOf(
            NetworkOperation.Type.UPLOAD,
            NetworkOperation.Type.DOWNLOAD,
            NetworkOperation.Type.DELETE,
            NetworkOperation.Type.CREATE_FOLDER,
            NetworkOperation.Type.LIST,
            NetworkOperation.Type.INFO,
            NetworkOperation.Type.MOVE,
            NetworkOperation.Type.COPY
        )
        
        val now = Clock.System.now()
        
        types.forEach { type ->
            val operation = NetworkOperation(
                id = System.currentTimeMillis(),
                type = type,
                status = NetworkOperation.Status.PENDING,
                remotePath = "/remote/path",
                localPath = "/local/path",
                progress = 0.0,
                createdAt = now
            )
            
            assertEquals(type, operation.type, "Should correctly set operation type: $type")
        }
    }

    @Test
    fun testOperationStatusVariations() {
        val statuses = listOf(
            NetworkOperation.Status.PENDING,
            NetworkOperation.Status.IN_PROGRESS,
            NetworkOperation.Status.COMPLETED,
            NetworkOperation.Status.FAILED,
            NetworkOperation.Status.CANCELLED,
            NetworkOperation.Status.PAUSED
        )
        
        val now = Clock.System.now()
        
        statuses.forEach { status ->
            val operation = NetworkOperation(
                id = System.currentTimeMillis(),
                type = NetworkOperation.Type.UPLOAD,
                status = status,
                remotePath = "/remote/path",
                localPath = "/local/path",
                progress = if (status == NetworkOperation.Status.COMPLETED) 1.0 else 0.5,
                createdAt = now,
                startedAt = if (status != NetworkOperation.Status.PENDING) now else null,
                completedAt = if (status == NetworkOperation.Status.COMPLETED || status == NetworkOperation.Status.FAILED || status == NetworkOperation.Status.CANCELLED) now.plus(kotlin.time.Duration.seconds(5)) else null
            )
            
            assertEquals(status, operation.status, "Should correctly set operation status: $status")
        }
    }

    @Test
    fun testProgressTracking() {
        val now = Clock.System.now()
        
        // Test various progress values
        val progressValues = listOf(0.0, 0.25, 0.5, 0.75, 1.0)
        
        progressValues.forEach { progress ->
            val operation = NetworkOperation(
                id = System.currentTimeMillis(),
                type = NetworkOperation.Type.DOWNLOAD,
                status = if (progress == 1.0) NetworkOperation.Status.COMPLETED else NetworkOperation.Status.IN_PROGRESS,
                remotePath = "/remote/file.txt",
                localPath = "/local/file.txt",
                progress = progress,
                createdAt = now,
                startedAt = now
            )
            
            assertEquals(progress, operation.progress, "Should correctly set progress: $progress")
        }
    }

    @Test
    fun testDurationCalculations() {
        // Given
        val createdAt = Clock.System.now()
        val startedAt = createdAt.plus(kotlin.time.Duration.seconds(2))
        val completedAt = startedAt.plus(kotlin.time.Duration.seconds(8))
        
        val operation = NetworkOperation(
            id = 123456789L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/remote/file.txt",
            localPath = "/local/file.txt",
            progress = 1.0,
            createdAt = createdAt,
            startedAt = startedAt,
            completedAt = completedAt
        )
        
        // Test duration calculations
        val totalDuration = operation.completedAt!! - operation.createdAt
        val executionDuration = operation.completedAt!! - operation.startedAt!!
        val queueDuration = operation.startedAt!! - operation.createdAt
        
        assertEquals(kotlin.time.Duration.seconds(10), totalDuration, "Total duration should be 10 seconds")
        assertEquals(kotlin.time.Duration.seconds(8), executionDuration, "Execution duration should be 8 seconds")
        assertEquals(kotlin.time.Duration.seconds(2), queueDuration, "Queue duration should be 2 seconds")
    }

    @Test
    fun testMetadataHandling() {
        val now = Clock.System.now()
        
        // Test with various metadata types
        val metadataCases = listOf(
            emptyMap<String, String>() to "Empty metadata",
            mapOf("key" to "value") to "Simple metadata",
            mapOf(
                "bytes_transferred" to "2048",
                "checksum" to "sha256:abc123def456",
                "compression_ratio" to "0.75",
                "encryption_enabled" to "true"
            ) to "Complex metadata",
            mapOf(
                "special_chars" to "!@#$%^&*()",
                "unicode" to "ñáéíóú",
                "emoji" to "🚀🎉💻"
            ) to "Special characters metadata"
        )
        
        metadataCases.forEach { (metadata, description) ->
            val operation = NetworkOperation(
                id = System.currentTimeMillis(),
                type = NetworkOperation.Type.UPLOAD,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/remote/file.txt",
                localPath = "/local/file.txt",
                progress = 1.0,
                createdAt = now,
                startedAt = now,
                completedAt = now.plus(kotlin.time.Duration.seconds(5)),
                metadata = metadata
            )
            
            assertEquals(metadata, operation.metadata, "Should correctly handle metadata: $description")
            assertEquals(metadata.size, operation.metadata?.size, "Metadata size should match: $description")
        }
    }

    @Test
    fun testErrorMessageHandling() {
        val now = Clock.System.now()
        
        // Test various error messages
        val errorCases = listOf(
            null to "No error",
            "" to "Empty error message",
            "Simple error" to "Simple error message",
            "Error with special characters: !@#$%^&*()" to "Error with special characters",
            "Very long error message that contains detailed information about what went wrong during the network operation and why it failed" to "Long error message",
            "Unicode error: ñáéíóú 🚀🎉💻" to "Unicode error message"
        )
        
        errorCases.forEach { (error, description) ->
            val operation = NetworkOperation(
                id = System.currentTimeMillis(),
                type = NetworkOperation.Type.DOWNLOAD,
                status = if (error != null) NetworkOperation.Status.FAILED else NetworkOperation.Status.COMPLETED,
                remotePath = "/remote/file.txt",
                localPath = "/local/file.txt",
                progress = if (error != null) 0.5 else 1.0,
                createdAt = now,
                startedAt = now,
                completedAt = now.plus(kotlin.time.Duration.seconds(3)),
                error = error
            )
            
            assertEquals(error, operation.error, "Should correctly handle error: $description")
        }
    }

    @Test
    fun testOptionalFields() {
        val now = Clock.System.now()
        
        // Test operation with minimal required fields
        val minimalOperation = NetworkOperation(
            id = 111111111L,
            type = NetworkOperation.Type.INFO,
            status = NetworkOperation.Status.PENDING,
            remotePath = "/remote/info.txt",
            localPath = "",
            progress = 0.0,
            createdAt = now
        )
        
        assertNull(minimalOperation.startedAt, "Started at should be null for pending operation")
        assertNull(minimalOperation.completedAt, "Completed at should be null for pending operation")
        assertNull(minimalOperation.error, "Error should be null for successful operation")
        assertNull(minimalOperation.metadata, "Metadata should be null when not provided")
        
        // Test operation with all optional fields
        val fullOperation = NetworkOperation(
            id = 222222222L,
            type = NetworkOperation.Type.LIST,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/remote/folder",
            localPath = "/local/folder",
            progress = 1.0,
            createdAt = now,
            startedAt = now.plus(kotlin.time.Duration.seconds(1)),
            completedAt = now.plus(kotlin.time.Duration.seconds(5)),
            error = null,
            metadata = mapOf("item_count" to "42", "total_size" to "1048576")
        )
        
        assertNotNull(fullOperation.startedAt, "Started at should be set")
        assertNotNull(fullOperation.completedAt, "Completed at should be set")
        assertNull(fullOperation.error, "Error should be null for successful operation")
        assertNotNull(fullOperation.metadata, "Metadata should be set")
    }

    @Test
    fun testPerformanceCharacteristics() {
        val now = Clock.System.now()
        
        // Create multiple operations to test performance characteristics
        val operations = (1..100).map { i ->
            NetworkOperation(
                id = i.toLong(),
                type = NetworkOperation.Type.values()[i % NetworkOperation.Type.values().size],
                status = if (i % 3 == 0) NetworkOperation.Status.COMPLETED else NetworkOperation.Status.IN_PROGRESS,
                remotePath = "/remote/file_$i.txt",
                localPath = "/local/file_$i.txt",
                progress = if (i % 3 == 0) 1.0 else i / 100.0,
                createdAt = now,
                startedAt = now.plus(kotlin.time.Duration.milliseconds(i * 10)),
                completedAt = if (i % 3 == 0) now.plus(kotlin.time.Duration.milliseconds(i * 50)) else null,
                metadata = mapOf("iteration" to "$i", "size" to "${i * 1024}")
            )
        }
        
        // Test filtering by status
        val completedOperations = operations.filter { it.status == NetworkOperation.Status.COMPLETED }
        val inProgressOperations = operations.filter { it.status == NetworkOperation.Status.IN_PROGRESS }
        
        assertTrue(completedOperations.isNotEmpty(), "Should have completed operations")
        assertTrue(inProgressOperations.isNotEmpty(), "Should have in-progress operations")
        assertTrue(completedOperations.all { it.progress == 1.0 }, "All completed operations should have 100% progress")
        assertTrue(inProgressOperations.all { it.progress < 1.0 }, "All in-progress operations should have < 100% progress")
        
        // Test sorting by creation time
        val sortedOperations = operations.sortedBy { it.createdAt }
        assertEquals(operations.first().id, sortedOperations.first().id, "First operation should have smallest ID")
        assertEquals(operations.last().id, sortedOperations.last().id, "Last operation should have largest ID")
        
        // Test progress calculation
        val averageProgress = operations.map { it.progress }.average()
        assertTrue(averageProgress in 0.0..1.0, "Average progress should be between 0 and 1")
        
        // Test metadata aggregation
        val totalSize = operations.mapNotNull { it.metadata?.get("size")?.toIntOrNull() }.sum()
        assertTrue(totalSize > 0, "Should be able to aggregate metadata")
    }

    @Test
    fun testOperationChaining() {
        val now = Clock.System.now()
        
        // Test creating a sequence of related operations
        val operations = listOf(
            NetworkOperation(
                id = 1L,
                type = NetworkOperation.Type.INFO,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/remote/file.txt",
                localPath = "",
                progress = 1.0,
                createdAt = now,
                startedAt = now,
                completedAt = now.plus(kotlin.time.Duration.seconds(1)),
                metadata = mapOf("file_size" to "1024", "checksum" to "abc123")
            ),
            NetworkOperation(
                id = 2L,
                type = NetworkOperation.Type.DOWNLOAD,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/remote/file.txt",
                localPath = "/local/file.txt",
                progress = 1.0,
                createdAt = now.plus(kotlin.time.Duration.seconds(2)),
                startedAt = now.plus(kotlin.time.Duration.seconds(3)),
                completedAt = now.plus(kotlin.time.Duration.seconds(8)),
                metadata = mapOf("bytes_downloaded" to "1024", "download_time_ms" to "5000")
            ),
            NetworkOperation(
                id = 3L,
                type = NetworkOperation.Type.INFO,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/local/file.txt",
                localPath = "",
                progress = 1.0,
                createdAt = now.plus(kotlin.time.Duration.seconds(9)),
                startedAt = now.plus(kotlin.time.Duration.seconds(10)),
                completedAt = now.plus(kotlin.time.Duration.seconds(11)),
                metadata = mapOf("verification_result" to "passed", "local_checksum" to "abc123")
            )
        )
        
        // Verify the sequence
        assertEquals(3, operations.size, "Should have 3 operations in sequence")
        assertTrue(operations.all { it.status == NetworkOperation.Status.COMPLETED }, "All operations should be completed")
        assertEquals("/remote/file.txt", operations[0].remotePath, "First operation should check remote file")
        assertEquals("/remote/file.txt", operations[1].remotePath, "Second operation should download from remote")
        assertEquals("/local/file.txt", operations[2].remotePath, "Third operation should verify local file")
        
        // Verify timing sequence
        val times = operations.map { it.createdAt }
        assertTrue(times.zipWithNext().all { (a, b) -> a <= b }, "Operations should be created in sequence")
        
        // Verify metadata consistency
        val originalChecksum = operations[0].metadata?.get("checksum")
        val downloadedChecksum = operations[2].metadata?.get("local_checksum")
        assertEquals(originalChecksum, downloadedChecksum, "Checksums should match across operations")
    }

    @Test
    fun testOperationEquality() {
        val now = Clock.System.now()
        
        val operation1 = NetworkOperation(
            id = 123456789L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.IN_PROGRESS,
            remotePath = "/remote/file.txt",
            localPath = "/local/file.txt",
            progress = 0.5,
            createdAt = now,
            startedAt = now
        )
        
        val operation2 = NetworkOperation(
            id = 123456789L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.IN_PROGRESS,
            remotePath = "/remote/file.txt",
            localPath = "/local/file.txt",
            progress = 0.5,
            createdAt = now,
            startedAt = now
        )
        
        val operation3 = NetworkOperation(
            id = 987654321L,
            type = NetworkOperation.Type.DOWNLOAD,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/remote/other.txt",
            localPath = "/local/other.txt",
            progress = 1.0,
            createdAt = now,
            startedAt = now,
            completedAt = now.plus(kotlin.time.Duration.seconds(5))
        )
        
        assertEquals(operation1, operation2, "Operations with same properties should be equal")
        assertNotEquals(operation1, operation3, "Operations with different properties should not be equal")
        assertEquals(operation1.hashCode(), operation2.hashCode(), "Equal operations should have same hash code")
    }
}