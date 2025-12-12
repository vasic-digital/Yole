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
import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.database.NetworkStorageDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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

    private val performanceDatabase = NetworkStorageDatabase()
    private val mockAuthManager = MockAuthTokenManager()

    @Test
    fun testConcurrentNetworkOperations() = runTest {
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
        
        println("Concurrent operations: $concurrentOperations")
        println("Total duration: ${totalDuration.inWholeMilliseconds}ms")
        println("Average duration: ${averageDuration.inWholeMilliseconds}ms")
        println("Operations per second: ${concurrentOperations * 1000 / totalDuration.inWholeMilliseconds}")
    }

    @Test
    fun testMemoryEfficiencyUnderLoad() = runTest {
        val operationsPerBatch = 50
        val numberOfBatches = 10
        
        val memoryUsages = mutableListOf<Long>()
        
        repeat(numberOfBatches) { batchIndex ->
            // Record memory before batch
            System.gc()
            Thread.sleep(10)
            val memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            
            // Process batch of operations
            val operations = (1..operationsPerBatch).map { index ->
                createMockNetworkOperation(batchIndex * operationsPerBatch + index)
            }
            
            operations.forEach { operation ->
                simulateNetworkOperation(operation)
            }
            
            // Record memory after batch
            System.gc()
            Thread.sleep(10)
            val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            
            memoryUsages.add(memoryAfter - memoryBefore)
            
            // Cleanup
            operations.clear()
        }
        
        // Analyze memory usage pattern
        val averageMemoryIncrease = memoryUsages.average()
        val maxMemoryIncrease = memoryUsages.maxOrNull() ?: 0
        val memoryGrowthRate = if (memoryUsages.size > 1) {
            val firstHalf = memoryUsages.take(memoryUsages.size / 2).average()
            val secondHalf = memoryUsages.drop(memoryUsages.size / 2).average()
            secondHalf - firstHalf
        } else 0.0
        
        // Assertions
        assertTrue(averageMemoryIncrease < 10_000_000, "Average memory increase should be under 10MB")
        assertTrue(maxMemoryIncrease < 50_000_000, "Max memory increase should be under 50MB")
        assertTrue(memoryGrowthRate < 5_000_000, "Memory growth rate should be under 5MB per batch")
        
        println("Memory efficiency test completed")
        println("Average memory increase: ${averageMemoryIncrease / 1024 / 1024}MB")
        println("Max memory increase: ${maxMemoryIncrease / 1024 / 1024}MB")
        println("Memory growth rate: ${memoryGrowthRate / 1024 / 1024}MB per batch")
    }

    @Test
    fun testLargeDocumentHandling() = runTest {
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
            
            println("Performance for $label document:")
            println("  Store: ${storeResult.inWholeMilliseconds}ms")
            println("  Retrieve: ${retrieveResult.inWholeMilliseconds}ms")
            println("  Update: ${updateTime.inWholeMilliseconds}ms")
            println("  Delete: ${deleteResult.inWholeMilliseconds}ms")
            println("  Total: ${totalTime.inWholeMilliseconds}ms")
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
    fun testResponseTimeBenchmarks() = runTest {
        val benchmarks = mutableMapOf<String, List<Duration>>()
        
        // Benchmark different types of operations
        val operations = listOf(
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
            val average = times.average()
            val median = times.sorted()[times.size / 2]
            val min = times.minOrNull() ?: Duration.ZERO
            val max = times.maxOrNull() ?: Duration.ZERO
            val percentile95 = times.sorted()[(times.size * 0.95).toInt()]
            
            println("Benchmark: $name")
            println("  Average: ${average.inWholeMilliseconds}ms")
            println("  Median: ${median.inWholeMilliseconds}ms")
            println("  Min: ${min.inWholeMilliseconds}ms")
            println("  Max: ${max.inWholeMilliseconds}ms")
            println("  95th percentile: ${percentile95.inWholeMilliseconds}ms")
            
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
    fun testResourceLeakDetection() = runTest {
        val iterations = 1000
        val initialMemory = getUsedMemory()
        
        val memorySnapshots = mutableListOf<Long>()
        
        repeat(iterations) { iteration ->
            // Create and process many operations
            val operations = (1..50).map { index ->
                createMockNetworkOperation(iteration * 50 + index)
            }
            
            operations.forEach { operation ->
                simulateNetworkOperation(operation)
            }
            
            // Force cleanup
            operations.clear()
            System.gc()
            Thread.sleep(1)
            
            // Take memory snapshot every 100 iterations
            if (iteration % 100 == 0) {
                val currentMemory = getUsedMemory()
                memorySnapshots.add(currentMemory)
            }
        }
        
        val finalMemory = getUsedMemory()
        val memoryGrowth = finalMemory - initialMemory
        val memoryGrowthPerIteration = memoryGrowth.toDouble() / iterations
        
        // Analyze memory trend
        val memoryTrend = if (memorySnapshots.size > 2) {
            val firstHalf = memorySnapshots.take(memorySnapshots.size / 2).average()
            val secondHalf = memorySnapshots.drop(memorySnapshots.size / 2).average()
            secondHalf - firstHalf
        } else 0.0
        
        println("Resource leak detection completed:")
        println("  Initial memory: ${initialMemory / 1024 / 1024}MB")
        println("  Final memory: ${finalMemory / 1024 / 1024}MB")
        println("  Memory growth: ${memoryGrowth / 1024 / 1024}MB")
        println("  Memory growth per iteration: ${memoryGrowthPerIteration / 1024}KB")
        println("  Memory trend: ${memoryTrend / 1024 / 1024}MB")
        
        // Assertions
        assertTrue(memoryGrowth < 50_000_000, "Total memory growth should be under 50MB")
        assertTrue(memoryGrowthPerIteration < 100_000, "Memory growth per iteration should be under 100KB")
        assertTrue(memoryTrend < 10_000_000, "Memory trend should be relatively stable")
    }

    @Test
    fun testScalabilityLimits() = runTest {
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
            
            println("Scalability test for factor $factor:")
            println("  Operations: $operations")
            println("  Document size: ${documentSize / 1024}KB")
            println("  Concurrent requests: $concurrentRequests")
            println("  Total time: ${totalTime.inWholeMilliseconds}ms")
            println("  Time per operation: ${timePerOperation.inWholeMilliseconds}ms")
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
            type = NetworkOperation.Type.values()[id % NetworkOperation.Type.values().size],
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
            success = Random.nextFloat() > 0.1, // 90% success rate
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

    private fun getUsedMemory(): Long {
        System.gc()
        Thread.sleep(10)
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }

    private fun calculatePerformanceDegradation(results: List<PerformanceResult>): Double {
        if (results.size < 2) return 0.0
        val first = results.first()
        val last = results.last()
        return (last.timePerOperation.inWholeMicroseconds.toDouble() / 
                first.timePerOperation.inWholeMicroseconds.toDouble())
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

    private class MockAuthTokenManager : AuthTokenManager {
        override suspend fun storeAccessToken(service: String, token: String) = Result.success(Unit)
        override suspend fun getAccessToken(service: String) = Result.success("mock_token")
        override suspend fun storeRefreshToken(service: String, token: String) = Result.success(Unit)
        override suspend fun getRefreshToken(service: String) = Result.success("mock_refresh_token")
        override suspend fun storeTokenExpiration(service: String, expiresAt: kotlinx.datetime.Instant) = Result.success(Unit)
        override suspend fun isTokenExpired(service: String) = Result.success(false)
        override suspend fun hasValidToken(service: String) = Result.success(true)
        override suspend fun refreshAccessToken(service: String, clientId: String, clientSecret: String, tokenUrl: String) = Result.success("new_mock_token")
        override suspend fun clearTokens(service: String) = Result.success(Unit)
        override suspend fun clearAllTokens() = Result.success(Unit)
    }
}