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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive tests for NetworkOperation covering:
 * - Operation creation and state management
 * - Progress tracking
 * - Error handling
 * - Performance monitoring
 * - Helper methods
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
        assertTrue(operation.metadata.isEmpty())
    }

    @Test
    fun testCompletedOperationCreation() {
        // Given
        val createdAt = Clock.System.now()
        val startedAt = createdAt.plus(1.seconds)
        val completedAt = startedAt.plus(5.seconds)

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
        assertEquals(2, operation.metadata.size)
        assertEquals("1024", operation.metadata["bytes_transferred"])
        assertEquals("abc123", operation.metadata["checksum"])
    }

    @Test
    fun testFailedOperationCreation() {
        // Given
        val createdAt = Clock.System.now()
        val startedAt = createdAt.plus(1.seconds)
        val completedAt = startedAt.plus(3.seconds)

        val operation = NetworkOperation(
            id = 555555555L,
            type = NetworkOperation.Type.DELETE,
            status = NetworkOperation.Status.FAILED,
            remotePath = "/remote/missing.txt",
            localPath = null,
            progress = 0.0,
            createdAt = createdAt,
            startedAt = startedAt,
            completedAt = completedAt,
            error = "File not found: /remote/missing.txt"
        )

        // Then
        assertEquals(555555555L, operation.id)
        assertEquals(NetworkOperation.Type.DELETE, operation.type)
        assertEquals(NetworkOperation.Status.FAILED, operation.status)
        assertEquals("/remote/missing.txt", operation.remotePath)
        assertNull(operation.localPath)
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
            NetworkOperation.Type.RENAME,
            NetworkOperation.Type.MOVE,
            NetworkOperation.Type.COPY,
            NetworkOperation.Type.SYNC
        )

        val now = Clock.System.now()

        types.forEach { type ->
            val operation = NetworkOperation(
                id = type.ordinal.toLong(),
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
    fun testAllOperationTypesExist() {
        val expectedTypes = setOf("UPLOAD", "DOWNLOAD", "DELETE", "CREATE_FOLDER", "RENAME", "COPY", "MOVE", "SYNC")
        val actualTypes = NetworkOperation.Type.entries.map { it.name }.toSet()
        assertEquals(expectedTypes, actualTypes, "Should have all expected operation types")
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
                id = status.ordinal.toLong(),
                type = NetworkOperation.Type.UPLOAD,
                status = status,
                remotePath = "/remote/path",
                localPath = "/local/path",
                progress = if (status == NetworkOperation.Status.COMPLETED) 1.0 else 0.5,
                createdAt = now,
                startedAt = if (status != NetworkOperation.Status.PENDING) now else null,
                completedAt = if (status == NetworkOperation.Status.COMPLETED || status == NetworkOperation.Status.FAILED || status == NetworkOperation.Status.CANCELLED) now.plus(5.seconds) else null
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
                id = (progress * 100).toLong(),
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
        val startedAt = createdAt.plus(2.seconds)
        val completedAt = startedAt.plus(8.seconds)

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

        assertEquals(10.seconds, totalDuration, "Total duration should be 10 seconds")
        assertEquals(8.seconds, executionDuration, "Execution duration should be 8 seconds")
        assertEquals(2.seconds, queueDuration, "Queue duration should be 2 seconds")
    }

    @Test
    fun testDurationProperty() {
        // Given - completed operation
        val now = Clock.System.now()
        val startedAt = now
        val completedAt = now.plus(5.seconds)

        val completedOp = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/remote/file.txt",
            progress = 1.0,
            createdAt = now,
            startedAt = startedAt,
            completedAt = completedAt
        )

        assertEquals(5000L, completedOp.duration, "Duration should be 5000ms for completed operation")

        // Test pending operation (no startedAt)
        val pendingOp = NetworkOperation(
            id = 2L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.PENDING,
            remotePath = "/remote/file.txt",
            progress = 0.0,
            createdAt = now
        )

        assertNull(pendingOp.duration, "Duration should be null for pending operation")
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
                "special_chars" to "!@#\$%^&*()",
                "unicode" to "characters"
            ) to "Special characters metadata"
        )

        metadataCases.forEach { (metadata, description) ->
            val operation = NetworkOperation(
                id = metadata.hashCode().toLong(),
                type = NetworkOperation.Type.UPLOAD,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/remote/file.txt",
                localPath = "/local/file.txt",
                progress = 1.0,
                createdAt = now,
                startedAt = now,
                completedAt = now.plus(5.seconds),
                metadata = metadata
            )

            assertEquals(metadata, operation.metadata, "Should correctly handle metadata: $description")
            assertEquals(metadata.size, operation.metadata.size, "Metadata size should match: $description")
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
            "Error with special characters: !@#\$%^&*()" to "Error with special characters",
            "Very long error message that contains detailed information about what went wrong during the network operation and why it failed" to "Long error message"
        )

        errorCases.forEach { (error, description) ->
            val operation = NetworkOperation(
                id = description.hashCode().toLong(),
                type = NetworkOperation.Type.DOWNLOAD,
                status = if (error != null) NetworkOperation.Status.FAILED else NetworkOperation.Status.COMPLETED,
                remotePath = "/remote/file.txt",
                localPath = "/local/file.txt",
                progress = if (error != null) 0.5 else 1.0,
                createdAt = now,
                startedAt = now,
                completedAt = now.plus(3.seconds),
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
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.PENDING,
            remotePath = "/remote/info.txt",
            localPath = null,
            progress = 0.0,
            createdAt = now
        )

        assertNull(minimalOperation.startedAt, "Started at should be null for pending operation")
        assertNull(minimalOperation.completedAt, "Completed at should be null for pending operation")
        assertNull(minimalOperation.error, "Error should be null for successful operation")
        assertTrue(minimalOperation.metadata.isEmpty(), "Metadata should be empty when not provided")
        assertNull(minimalOperation.localPath, "LocalPath should be null when not provided")

        // Test operation with all optional fields
        val fullOperation = NetworkOperation(
            id = 222222222L,
            type = NetworkOperation.Type.SYNC,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/remote/folder",
            localPath = "/local/folder",
            progress = 1.0,
            createdAt = now,
            startedAt = now.plus(1.seconds),
            completedAt = now.plus(5.seconds),
            error = null,
            metadata = mapOf("item_count" to "42", "total_size" to "1048576")
        )

        assertNotNull(fullOperation.startedAt, "Started at should be set")
        assertNotNull(fullOperation.completedAt, "Completed at should be set")
        assertNull(fullOperation.error, "Error should be null for successful operation")
        assertNotNull(fullOperation.metadata, "Metadata should be set")
    }

    @Test
    fun testComputedProperties() {
        val now = Clock.System.now()

        // Test isRunning
        val runningOp = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.IN_PROGRESS,
            remotePath = "/test",
            progress = 0.5,
            createdAt = now,
            startedAt = now
        )
        assertTrue(runningOp.isRunning, "IN_PROGRESS and not paused should be running")

        // Test isPending
        val pendingOp = NetworkOperation(
            id = 2L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.PENDING,
            remotePath = "/test",
            progress = 0.0,
            createdAt = now
        )
        assertTrue(pendingOp.isPending, "PENDING status should be pending")

        // Test isCompleted
        val completedOp = NetworkOperation(
            id = 3L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.COMPLETED,
            remotePath = "/test",
            progress = 1.0,
            createdAt = now,
            completedAt = now
        )
        assertTrue(completedOp.isCompleted, "COMPLETED status should be completed")

        // Test hasFailed
        val failedOp = NetworkOperation(
            id = 4L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.FAILED,
            remotePath = "/test",
            progress = 0.0,
            createdAt = now,
            error = "failed"
        )
        assertTrue(failedOp.hasFailed, "FAILED status should be hasFailed")

        // Test progressPercentage
        assertEquals(50, runningOp.progressPercentage, "0.5 progress should be 50%")
        assertEquals(100, completedOp.progressPercentage, "1.0 progress should be 100%")
    }

    @Test
    fun testWithStatusHelper() {
        val now = Clock.System.now()
        val operation = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.PENDING,
            remotePath = "/test",
            progress = 0.0,
            createdAt = now
        )

        val inProgress = operation.withStatus(NetworkOperation.Status.IN_PROGRESS)
        assertEquals(NetworkOperation.Status.IN_PROGRESS, inProgress.status)
        assertNotNull(inProgress.startedAt, "Starting should set startedAt")

        val failed = inProgress.withStatus(NetworkOperation.Status.FAILED, "Error occurred")
        assertEquals(NetworkOperation.Status.FAILED, failed.status)
        assertEquals("Error occurred", failed.error)
        assertNotNull(failed.completedAt, "Failed should set completedAt")
    }

    @Test
    fun testWithProgressHelper() {
        val now = Clock.System.now()
        val operation = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.DOWNLOAD,
            status = NetworkOperation.Status.IN_PROGRESS,
            remotePath = "/test",
            progress = 0.0,
            totalSize = 1000L,
            createdAt = now,
            startedAt = now
        )

        val updated = operation.withProgress(0.5)
        assertEquals(0.5, updated.progress)
        assertEquals(500L, updated.bytesTransferred) // 1000 * 0.5

        // Test clamping
        val clamped = operation.withProgress(1.5)
        assertEquals(1.0, clamped.progress, "Progress should be clamped to 1.0")

        val clampedLow = operation.withProgress(-0.5)
        assertEquals(0.0, clampedLow.progress, "Progress should be clamped to 0.0")
    }

    @Test
    fun testWithErrorHelper() {
        val now = Clock.System.now()
        val operation = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.IN_PROGRESS,
            remotePath = "/test",
            progress = 0.5,
            createdAt = now,
            startedAt = now,
            retryCount = 0
        )

        val failed = operation.withError("Network timeout")
        assertEquals(NetworkOperation.Status.FAILED, failed.status)
        assertEquals("Network timeout", failed.error)
        assertEquals(1, failed.retryCount, "Retry count should be incremented")
        assertNotNull(failed.completedAt, "Error should set completedAt")
    }

    @Test
    fun testCanRetryProperty() {
        val now = Clock.System.now()
        val failedOp = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.UPLOAD,
            status = NetworkOperation.Status.FAILED,
            remotePath = "/test",
            progress = 0.0,
            createdAt = now,
            error = "error",
            retryCount = 1,
            maxRetries = 3
        )
        assertTrue(failedOp.canRetry, "Failed op with retries left should be retryable")

        val exhaustedOp = failedOp.copy(retryCount = 3)
        assertFalse(exhaustedOp.canRetry, "Op with max retries reached should not be retryable")
    }

    @Test
    fun testPerformanceCharacteristics() {
        val now = Clock.System.now()

        // Create multiple operations to test performance characteristics
        val typeEntries = NetworkOperation.Type.entries
        val operations = (1..100).map { i ->
            NetworkOperation(
                id = i.toLong(),
                type = typeEntries[i % typeEntries.size],
                status = if (i % 3 == 0) NetworkOperation.Status.COMPLETED else NetworkOperation.Status.IN_PROGRESS,
                remotePath = "/remote/file_$i.txt",
                localPath = "/local/file_$i.txt",
                progress = if (i % 3 == 0) 1.0 else (i % 100) / 100.0,
                createdAt = now,
                startedAt = now.plus((i * 10).milliseconds),
                completedAt = if (i % 3 == 0) now.plus((i * 50).milliseconds) else null,
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
        val totalSize = operations.mapNotNull { it.metadata["size"]?.toIntOrNull() }.sum()
        assertTrue(totalSize > 0, "Should be able to aggregate metadata")
    }

    @Test
    fun testOperationChaining() {
        val now = Clock.System.now()

        // Test creating a sequence of related operations
        val operations = listOf(
            NetworkOperation(
                id = 1L,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/remote/file.txt",
                localPath = null,
                progress = 1.0,
                createdAt = now,
                startedAt = now,
                completedAt = now.plus(1.seconds),
                metadata = mapOf("file_size" to "1024", "checksum" to "abc123")
            ),
            NetworkOperation(
                id = 2L,
                type = NetworkOperation.Type.DOWNLOAD,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/remote/file.txt",
                localPath = "/local/file.txt",
                progress = 1.0,
                createdAt = now.plus(2.seconds),
                startedAt = now.plus(3.seconds),
                completedAt = now.plus(8.seconds),
                metadata = mapOf("bytes_downloaded" to "1024", "download_time_ms" to "5000")
            ),
            NetworkOperation(
                id = 3L,
                type = NetworkOperation.Type.SYNC,
                status = NetworkOperation.Status.COMPLETED,
                remotePath = "/local/file.txt",
                localPath = null,
                progress = 1.0,
                createdAt = now.plus(9.seconds),
                startedAt = now.plus(10.seconds),
                completedAt = now.plus(11.seconds),
                metadata = mapOf("verification_result" to "passed", "local_checksum" to "abc123")
            )
        )

        // Verify the sequence
        assertEquals(3, operations.size, "Should have 3 operations in sequence")
        assertTrue(operations.all { it.status == NetworkOperation.Status.COMPLETED }, "All operations should be completed")

        // Verify timing sequence
        val times = operations.map { it.createdAt }
        assertTrue(times.zipWithNext().all { (a, b) -> a <= b }, "Operations should be created in sequence")

        // Verify metadata consistency
        val originalChecksum = operations[0].metadata["checksum"]
        val downloadedChecksum = operations[2].metadata["local_checksum"]
        assertEquals(originalChecksum, downloadedChecksum, "Checksums should match across operations")
    }

    @Test
    fun testFactoryMethods() {
        // Test createUpload
        val upload = NetworkOperation.createUpload("upload1", "/local/file.txt", "/remote/file.txt", 1024L)
        assertEquals(NetworkOperation.Type.UPLOAD, upload.type)
        assertEquals("/remote/file.txt", upload.remotePath)
        assertEquals("/local/file.txt", upload.localPath)
        assertEquals(1024L, upload.totalSize)

        // Test createDownload
        val download = NetworkOperation.createDownload("download1", "/remote/file.txt", "/local/file.txt", 2048L)
        assertEquals(NetworkOperation.Type.DOWNLOAD, download.type)
        assertEquals("/remote/file.txt", download.remotePath)
        assertEquals("/local/file.txt", download.localPath)
        assertEquals(2048L, download.totalSize)

        // Test createDelete
        val delete = NetworkOperation.createDelete(100L, "/remote/file.txt")
        assertEquals(NetworkOperation.Type.DELETE, delete.type)
        assertEquals("/remote/file.txt", delete.remotePath)
        assertFalse(delete.canPause)

        // Test createFolder
        val folder = NetworkOperation.createFolder(200L, "/remote/newfolder")
        assertEquals(NetworkOperation.Type.CREATE_FOLDER, folder.type)
        assertEquals("/remote/newfolder", folder.remotePath)
        assertFalse(folder.canPause)

        // Test createSync
        val sync = NetworkOperation.createSync("sync1", "/remote/file.txt")
        assertEquals(NetworkOperation.Type.SYNC, sync.type)
        assertEquals("/remote/file.txt", sync.remotePath)

        // Test mock
        val mock = NetworkOperation.mock()
        assertEquals(NetworkOperation.Type.UPLOAD, mock.type)
        assertEquals(NetworkOperation.Status.IN_PROGRESS, mock.status)
        assertEquals(0.5, mock.progress)
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
            completedAt = now.plus(5.seconds)
        )

        assertEquals(operation1, operation2, "Operations with same properties should be equal")
        assertNotEquals(operation1, operation3, "Operations with different properties should not be equal")
        assertEquals(operation1.hashCode(), operation2.hashCode(), "Equal operations should have same hash code")
    }
}
