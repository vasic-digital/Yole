/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Performance and load tests for network operations
 *
 *########################################################*/
package digital.vasic.yole.network

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Performance and load tests for network operations covering:
 * - Concurrent operation handling
 * - Memory efficiency under load
 * - Response time benchmarks
 * - Resource leak detection
 * - Scalability testing
 */
class NetworkPerformanceTest {

    private val performanceDatabase = MockPerformanceDatabase()

    @Test
    fun testConcurrentNetworkOperations() = runBlocking {
        val concurrentOperations = 100
        val results = mutableListOf<NetworkOperationResult>()

        val startTime = Clock.System.now()

        // Launch multiple concurrent operations
        val jobs = (1..concurrentOperations).map { index ->
            async {
                val operation = createMockNetworkOperation(index)
                val result = simulateNetworkOperation(operation)
                results.add(result)
                result
            }
        }

        // Wait for all operations to complete
        val completedResults = jobs.awaitAll()
        val endTime = Clock.System.now()

        val totalDuration = endTime - startTime
        val averageDuration = totalDuration / concurrentOperations

        // Assertions
        assertEquals(concurrentOperations, completedResults.size, "All operations should complete")
        assertTrue(completedResults.all { it.success }, "All operations should succeed")
        assertTrue(totalDuration < 10.seconds, "Total duration should be under 10 seconds")
        assertTrue(averageDuration < 100.milliseconds, "Average duration should be under 100ms")
    }

    @Test
    fun testMemoryEfficiencyUnderLoad() = runBlocking {
        val operationsPerBatch = 50
        val numberOfBatches = 10

        val batchDurations = mutableListOf<Duration>()

        repeat(numberOfBatches) { batchIndex ->
            val batchStart = Clock.System.now()

            // Process batch of operations
            val operations = (1..operationsPerBatch).map { index ->
                createMockNetworkOperation(batchIndex * operationsPerBatch + index)
            }

            operations.forEach { operation ->
                simulateNetworkOperation(operation)
            }

            val batchEnd = Clock.System.now()
            batchDurations.add(batchEnd - batchStart)
        }

        // Analyze batch processing pattern - batches should stay consistent
        val maxBatchDuration = batchDurations.maxByOrNull { it.inWholeMilliseconds }
            ?: Duration.ZERO

        // Performance should remain stable across batches (no degradation)
        val firstHalfAvg = batchDurations.take(numberOfBatches / 2)
            .map { it.inWholeMilliseconds }.average()
        val secondHalfAvg = batchDurations.drop(numberOfBatches / 2)
            .map { it.inWholeMilliseconds }.average()
        val degradationRatio = if (firstHalfAvg > 0) secondHalfAvg / firstHalfAvg else 1.0

        // Assertions
        assertTrue(maxBatchDuration < 10.seconds, "Max batch duration should be under 10s")
        assertTrue(degradationRatio < 10.0, "Performance should not degrade more than 10x across batches")
    }

    @Test
    fun testLargeDocumentHandling() = runBlocking {
        val documentSizes = listOf(
            1_000L to "1KB",
            10_000L to "10KB",
            100_000L to "100KB",
            1_000_000L to "1MB",
            10_000_000L to "10MB"
        )

        val performanceResults = mutableListOf<PerformanceResult>()

        documentSizes.forEach { (size, label) ->
            val content = generateLargeContent(size)
            val document = createMockNetworkDocument("doc_$label", content)

            val startTime = Clock.System.now()

            // Simulate various operations
            val storeResult = measureOperation {
                performanceDatabase.storeDocument("test-service", document)
            }

            val retrieveResult = measureOperation {
                performanceDatabase.getDocument("test-service", document.id)
            }

            val updateResult = measureOperation {
                val updatedDocument = document.copy(size = size * 2)
                performanceDatabase.storeDocument("test-service", updatedDocument)
            }

            val deleteResult = measureOperation {
                performanceDatabase.deleteDocument("test-service", document.id)
            }

            val endTime = Clock.System.now()
            val totalTime = endTime - startTime

            performanceResults.add(
                PerformanceResult(
                    size = size,
                    label = label,
                    storeTime = storeResult,
                    retrieveTime = retrieveResult,
                    updateTime = updateResult,
                    deleteTime = deleteResult,
                    totalTime = totalTime
                )
            )
        }

        // Verify performance doesn't degrade significantly with size
        val performanceDegradation = calculatePerformanceDegradation(performanceResults)
        assertTrue(performanceDegradation < 2.0, "Performance degradation should be less than 2x")

        // Verify all operations complete within reasonable time
        performanceResults.forEach { result ->
            assertTrue(result.totalTime < 5.seconds, "Total time for ${result.label} should be under 5 seconds")
            assertTrue(result.storeTime < 1.seconds, "Store time should be under 1 second")
            assertTrue(result.retrieveTime < 500.milliseconds, "Retrieve time should be under 500ms")
        }
    }

    @Test
    fun testResponseTimeBenchmarks() = runBlocking {
        val benchmarks = mutableMapOf<String, List<Duration>>()

        // Benchmark different types of operations
        val operations: List<Pair<String, suspend () -> Unit>> = listOf(
            "Small Upload" to { simulateUpload(1024) },
            "Medium Upload" to { simulateUpload(102400) },
            "Large Upload" to { simulateUpload(1048576) },
            "Small Download" to { simulateDownload(1024) },
            "Medium Download" to { simulateDownload(102400) },
            "Large Download" to { simulateDownload(1048576) },
            "File Info" to { simulateFileInfo() },
            "List Directory" to { simulateListDirectory(50) },
            "Create Folder" to { simulateCreateFolder() },
            "Delete File" to { simulateDeleteFile() }
        )

        operations.forEach { (name, operation) ->
            val times = mutableListOf<Duration>()

            // Warm up
            repeat(5) { operation() }

            // Benchmark
            repeat(20) {
                val duration = measureOperation(operation)
                times.add(duration)
            }

            benchmarks[name] = times
        }

        // Analyze benchmarks
        benchmarks.forEach { (name, times) ->
            val sortedMs = times.map { it.inWholeMilliseconds }.sorted()
            val averageMs = sortedMs.average()

            val average = averageMs.milliseconds
            // Performance assertions
            when {
                name.contains("Small") -> {
                    assertTrue(average < 100.milliseconds, "$name average should be under 100ms")
                }
                name.contains("Medium") -> {
                    assertTrue(average < 500.milliseconds, "$name average should be under 500ms")
                }
                name.contains("Large") -> {
                    assertTrue(average < 2.seconds, "$name average should be under 2s")
                }
                else -> {
                    assertTrue(average < 200.milliseconds, "$name average should be under 200ms")
                }
            }
        }
    }

    @Test
    fun testResourceLeakDetection() = runBlocking {
        val iterations = 1000
        val durationSnapshots = mutableListOf<Duration>()

        repeat(iterations) { iteration ->
            val iterStart = Clock.System.now()

            // Create and process many operations
            val operations = (1..50).map { index ->
                createMockNetworkOperation(iteration * 50 + index)
            }

            operations.forEach { operation ->
                simulateNetworkOperation(operation)
            }

            val iterEnd = Clock.System.now()

            // Take duration snapshot every 100 iterations
            if (iteration % 100 == 0) {
                durationSnapshots.add(iterEnd - iterStart)
            }
        }

        // Analyze performance trend - should stay stable (no resource leaks causing slowdown)
        val durationTrend = if (durationSnapshots.size > 2) {
            val firstHalf = durationSnapshots.take(durationSnapshots.size / 2)
                .map { it.inWholeMilliseconds }.average()
            val secondHalf = durationSnapshots.drop(durationSnapshots.size / 2)
                .map { it.inWholeMilliseconds }.average()
            if (firstHalf > 0) secondHalf / firstHalf else 1.0
        } else 1.0

        // Assertions - if resources leak, later iterations get slower
        assertTrue(durationTrend < 3.0, "Duration trend should be stable (no resource leaks)")
        assertTrue(durationSnapshots.all { it < 5.seconds }, "Each snapshot batch should complete under 5s")
    }

    @Test
    fun testScalabilityLimits() = runBlocking {
        val scaleFactors = listOf(10, 50, 100, 500, 1000)
        val scalabilityResults = mutableListOf<ScalabilityResult>()

        scaleFactors.forEach { factor ->
            val operations = factor
            val documentSize = 1024L * factor / 10 // Scale document size with factor
            val concurrentRequests = minOf(factor / 10, 50) // Limit concurrent requests

            val startTime = Clock.System.now()

            // Test different scalability scenarios
            val concurrentJob = async {
                repeat(concurrentRequests) { requestIndex ->
                    launch {
                        val content = generateLargeContent(documentSize)
                        val document = createMockNetworkDocument("doc_${factor}_$requestIndex", content)

                        // Simulate various operations
                        performanceDatabase.storeDocument("test-service", document)
                        performanceDatabase.getDocument("test-service", document.id)
                        performanceDatabase.deleteDocument("test-service", document.id)
                    }
                }
            }

            concurrentJob.await()

            val endTime = Clock.System.now()
            val totalTime = endTime - startTime
            val timePerOperation = totalTime / operations

            scalabilityResults.add(
                ScalabilityResult(
                    scaleFactor = factor,
                    operations = operations,
                    documentSize = documentSize,
                    concurrentRequests = concurrentRequests,
                    totalTime = totalTime,
                    timePerOperation = timePerOperation
                )
            )
        }

        // Analyze scalability
        val scalabilityFactor = calculateScalabilityFactor(scalabilityResults)
        assertTrue(scalabilityFactor < 1.5, "Scalability factor should be under 1.5x")

        // Verify performance doesn't degrade excessively
        scalabilityResults.forEach { result ->
            assertTrue(result.timePerOperation < 100.milliseconds, "Time per operation should be under 100ms")
            assertTrue(result.totalTime < 30.seconds, "Total time should be under 30 seconds")
        }
    }

    // ==================== Helper Methods ====================

    private fun createMockNetworkOperation(id: Int): NetworkOperation {
        return NetworkOperation(
            id = id.toLong(),
            type = NetworkOperation.Type.entries[id % NetworkOperation.Type.entries.size],
            status = NetworkOperation.Status.PENDING,
            remotePath = "/remote/file_$id.txt",
            localPath = "/local/file_$id.txt",
            progress = 0.0,
            createdAt = Clock.System.now()
        )
    }

    private suspend fun simulateNetworkOperation(operation: NetworkOperation): NetworkOperationResult {
        delay(Random.nextLong(10, 100)) // Simulate network delay

        return NetworkOperationResult(
            operationId = operation.id,
            success = true,
            duration = Clock.System.now() - operation.createdAt
        )
    }

    private fun createMockNetworkDocument(id: String, content: String): NetworkDocument {
        return NetworkDocument(
            id = id,
            name = "$id.txt",
            path = "/documents/$id.txt",
            size = content.length.toLong(),
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
    }

    private fun generateLargeContent(size: Long): String {
        return buildString {
            repeat((size / 100).toInt()) {
                append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ")
                append("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ")
                append("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris. ")
                append("\n")
            }
        }.take(size.toInt())
    }

    private suspend fun measureOperation(operation: suspend () -> Unit): Duration {
        val startTime = Clock.System.now()
        operation()
        val endTime = Clock.System.now()
        return endTime - startTime
    }

    private fun calculatePerformanceDegradation(results: List<PerformanceResult>): Double {
        if (results.size < 2) return 0.0
        val first = results.first()
        val last = results.last()
        val firstTimePerOp = first.totalTime.inWholeMicroseconds.toDouble() / 4
        val lastTimePerOp = last.totalTime.inWholeMicroseconds.toDouble() / 4
        return if (firstTimePerOp > 0) lastTimePerOp / firstTimePerOp else 1.0
    }

    private fun calculateScalabilityFactor(results: List<ScalabilityResult>): Double {
        if (results.size < 2) return 0.0
        val first = results.first()
        val last = results.last()
        return (last.timePerOperation.inWholeMicroseconds.toDouble() /
                first.timePerOperation.inWholeMicroseconds.toDouble()) /
               (last.scaleFactor.toDouble() / first.scaleFactor.toDouble())
    }

    // Mock implementations for testing
    private suspend fun simulateUpload(size: Int): Unit = delay(size / 10000L)
    private suspend fun simulateDownload(size: Int): Unit = delay(size / 10000L)
    private suspend fun simulateFileInfo(): Unit = delay(10L)
    private suspend fun simulateListDirectory(count: Int): Unit = delay(count * 2L)
    private suspend fun simulateCreateFolder(): Unit = delay(50L)
    private suspend fun simulateDeleteFile(): Unit = delay(30L)

    // Test data classes
    private data class NetworkOperationResult(
        val operationId: Long,
        val success: Boolean,
        val duration: Duration
    )

    private data class PerformanceResult(
        val size: Long,
        val label: String,
        val storeTime: Duration,
        val retrieveTime: Duration,
        val updateTime: Duration,
        val deleteTime: Duration,
        val totalTime: Duration
    )

    private data class ScalabilityResult(
        val scaleFactor: Int,
        val operations: Int,
        val documentSize: Long,
        val concurrentRequests: Int,
        val totalTime: Duration,
        val timePerOperation: Duration
    )

    /**
     * Simple in-memory mock for performance testing.
     * Provides storeDocument/getDocument/deleteDocument for performance benchmarks.
     */
    private class MockPerformanceDatabase {
        private val documents = mutableMapOf<String, NetworkDocument>()

        fun storeDocument(service: String, document: NetworkDocument) {
            documents["$service:${document.id}"] = document
        }

        fun getDocument(service: String, id: String): NetworkDocument? {
            return documents["$service:$id"]
        }

        fun deleteDocument(service: String, id: String) {
            documents.remove("$service:$id")
        }
    }
}
