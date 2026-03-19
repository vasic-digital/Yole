/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for the monitoring/metrics infrastructure:
 * PerformanceMetrics, MetricsSnapshot, MetricsReporter.
 *
 *########################################################*/
package digital.vasic.yole.monitoring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Tests for [PerformanceMetrics], [MetricsSnapshot], and [MetricsReporter].
 *
 * Covers:
 * - recordParse increments count and total
 * - recordCacheHit/Miss increments correctly
 * - recordCacheEviction increments
 * - recordNetworkRequest with and without error
 * - recordDetection increments
 * - recordSemaphoreWait increments
 * - snapshot returns correct values
 * - snapshot computed fields (avgMs, hitRatio, errorRate)
 * - reset clears all counters
 * - concurrent recording from 50 coroutines
 * - parseMaxMs tracks maximum correctly
 * - cacheHitRatio edge cases (0 total, all hits, all misses)
 * - MetricsSnapshot serialization round-trip
 * - MetricsReporter.reportNow returns snapshot
 * - Metrics accumulation over multiple operations
 *
 * Total: 16 tests
 */
class PerformanceMetricsTests {

    @BeforeTest
    fun setUp() {
        PerformanceMetrics.reset()
    }

    @AfterTest
    fun tearDown() {
        PerformanceMetrics.reset()
    }

    // ==================== Parse Recording ====================

    @Test
    fun recordParseIncrementsCountAndTotal() {
        PerformanceMetrics.recordParse(50)
        PerformanceMetrics.recordParse(100)

        val snap = PerformanceMetrics.snapshot()
        assertEquals(2L, snap.parseCount)
        assertEquals(150L, snap.parseTotalMs)
    }

    @Test
    fun parseMaxMsTracksMaximumCorrectly() {
        PerformanceMetrics.recordParse(10)
        PerformanceMetrics.recordParse(200)
        PerformanceMetrics.recordParse(50)
        PerformanceMetrics.recordParse(200)
        PerformanceMetrics.recordParse(5)

        val snap = PerformanceMetrics.snapshot()
        assertEquals(200L, snap.parseMaxMs, "parseMaxMs should track the highest value")
        assertEquals(5L, snap.parseCount)
    }

    // ==================== Cache Recording ====================

    @Test
    fun recordCacheHitAndMissIncrementsCorrectly() {
        PerformanceMetrics.recordCacheHit()
        PerformanceMetrics.recordCacheHit()
        PerformanceMetrics.recordCacheHit()
        PerformanceMetrics.recordCacheMiss()

        val snap = PerformanceMetrics.snapshot()
        assertEquals(3L, snap.cacheHits)
        assertEquals(1L, snap.cacheMisses)
    }

    @Test
    fun recordCacheEvictionIncrements() {
        PerformanceMetrics.recordCacheEviction()
        PerformanceMetrics.recordCacheEviction()

        val snap = PerformanceMetrics.snapshot()
        assertEquals(2L, snap.cacheEvictions)
    }

    // ==================== Network Recording ====================

    @Test
    fun recordNetworkRequestWithAndWithoutError() {
        PerformanceMetrics.recordNetworkRequest(100, error = false)
        PerformanceMetrics.recordNetworkRequest(200, error = true)
        PerformanceMetrics.recordNetworkRequest(150, error = false)

        val snap = PerformanceMetrics.snapshot()
        assertEquals(3L, snap.networkRequests)
        assertEquals(1L, snap.networkErrors)
        assertEquals(450L, snap.networkTotalMs)
    }

    // ==================== Detection Recording ====================

    @Test
    fun recordDetectionIncrements() {
        PerformanceMetrics.recordDetection(5)
        PerformanceMetrics.recordDetection(10)
        PerformanceMetrics.recordDetection(3)

        val snap = PerformanceMetrics.snapshot()
        assertEquals(3L, snap.detectionCount)
        assertEquals(18L, snap.detectionTotalMs)
    }

    // ==================== Semaphore Recording ====================

    @Test
    fun recordSemaphoreWaitIncrements() {
        PerformanceMetrics.recordSemaphoreWait(20)
        PerformanceMetrics.recordSemaphoreWait(30)

        val snap = PerformanceMetrics.snapshot()
        assertEquals(2L, snap.semaphoreWaits)
        assertEquals(50L, snap.semaphoreWaitTotalMs)
    }

    // ==================== Snapshot Correctness ====================

    @Test
    fun snapshotReturnsCorrectValues() {
        PerformanceMetrics.recordParse(100)
        PerformanceMetrics.recordParse(200)
        PerformanceMetrics.recordCacheHit()
        PerformanceMetrics.recordCacheMiss()
        PerformanceMetrics.recordCacheEviction()
        PerformanceMetrics.recordNetworkRequest(50, error = false)
        PerformanceMetrics.recordDetection(10)
        PerformanceMetrics.recordSemaphoreWait(5)

        val snap = PerformanceMetrics.snapshot()

        assertEquals(2L, snap.parseCount)
        assertEquals(300L, snap.parseTotalMs)
        assertEquals(200L, snap.parseMaxMs)
        assertEquals(1L, snap.cacheHits)
        assertEquals(1L, snap.cacheMisses)
        assertEquals(1L, snap.cacheEvictions)
        assertEquals(1L, snap.networkRequests)
        assertEquals(0L, snap.networkErrors)
        assertEquals(50L, snap.networkTotalMs)
        assertEquals(1L, snap.detectionCount)
        assertEquals(10L, snap.detectionTotalMs)
        assertEquals(1L, snap.semaphoreWaits)
        assertEquals(5L, snap.semaphoreWaitTotalMs)
        assertTrue(snap.timestamp > 0, "Timestamp must be positive")
    }

    @Test
    fun snapshotComputedFieldsAreCorrect() {
        // Parse: 100 + 200 = 300ms total, 2 ops => avg 150.0
        PerformanceMetrics.recordParse(100)
        PerformanceMetrics.recordParse(200)

        // Cache: 3 hits, 1 miss => ratio 0.75
        PerformanceMetrics.recordCacheHit()
        PerformanceMetrics.recordCacheHit()
        PerformanceMetrics.recordCacheHit()
        PerformanceMetrics.recordCacheMiss()

        // Network: 2 requests (1 error), 300ms total => avg 150.0, errorRate 0.5
        PerformanceMetrics.recordNetworkRequest(100, error = false)
        PerformanceMetrics.recordNetworkRequest(200, error = true)

        // Detection: 3 ops, 18ms total => avg 6.0
        PerformanceMetrics.recordDetection(5)
        PerformanceMetrics.recordDetection(10)
        PerformanceMetrics.recordDetection(3)

        // Semaphore: 2 waits, 50ms total => avg 25.0
        PerformanceMetrics.recordSemaphoreWait(20)
        PerformanceMetrics.recordSemaphoreWait(30)

        val snap = PerformanceMetrics.snapshot()

        assertEquals(150.0, snap.parseAvgMs, 0.01)
        assertEquals(0.75, snap.cacheHitRatio, 0.01)
        assertEquals(150.0, snap.networkAvgMs, 0.01)
        assertEquals(0.5, snap.networkErrorRate, 0.01)
        assertEquals(6.0, snap.detectionAvgMs, 0.01)
        assertEquals(25.0, snap.semaphoreWaitAvgMs, 0.01)
    }

    // ==================== Reset ====================

    @Test
    fun resetClearsAllCounters() {
        PerformanceMetrics.recordParse(100)
        PerformanceMetrics.recordCacheHit()
        PerformanceMetrics.recordCacheMiss()
        PerformanceMetrics.recordCacheEviction()
        PerformanceMetrics.recordNetworkRequest(50, error = true)
        PerformanceMetrics.recordDetection(10)
        PerformanceMetrics.recordSemaphoreWait(5)

        PerformanceMetrics.reset()
        val snap = PerformanceMetrics.snapshot()

        assertEquals(0L, snap.parseCount)
        assertEquals(0L, snap.parseTotalMs)
        assertEquals(0L, snap.parseMaxMs)
        assertEquals(0L, snap.cacheHits)
        assertEquals(0L, snap.cacheMisses)
        assertEquals(0L, snap.cacheEvictions)
        assertEquals(0L, snap.networkRequests)
        assertEquals(0L, snap.networkErrors)
        assertEquals(0L, snap.networkTotalMs)
        assertEquals(0L, snap.detectionCount)
        assertEquals(0L, snap.detectionTotalMs)
        assertEquals(0L, snap.semaphoreWaits)
        assertEquals(0L, snap.semaphoreWaitTotalMs)
        assertEquals(0.0, snap.parseAvgMs, 0.01)
        assertEquals(0.0, snap.cacheHitRatio, 0.01)
        assertEquals(0.0, snap.networkAvgMs, 0.01)
        assertEquals(0.0, snap.networkErrorRate, 0.01)
        assertEquals(0.0, snap.detectionAvgMs, 0.01)
        assertEquals(0.0, snap.semaphoreWaitAvgMs, 0.01)
    }

    // ==================== Concurrency ====================

    @Test
    fun concurrentRecordingFrom50Coroutines() = runBlocking<Unit> {
        val jobs = (1..50).map {
            async {
                repeat(100) {
                    PerformanceMetrics.recordParse(1)
                    PerformanceMetrics.recordCacheHit()
                    PerformanceMetrics.recordCacheMiss()
                    PerformanceMetrics.recordCacheEviction()
                    PerformanceMetrics.recordNetworkRequest(1, error = false)
                    PerformanceMetrics.recordDetection(1)
                    PerformanceMetrics.recordSemaphoreWait(1)
                }
            }
        }
        jobs.awaitAll()

        val snap = PerformanceMetrics.snapshot()
        assertEquals(5000L, snap.parseCount, "50 coroutines * 100 iterations = 5000 parse records")
        assertEquals(5000L, snap.parseTotalMs, "Each parse records 1ms => 5000ms total")
        assertEquals(5000L, snap.cacheHits)
        assertEquals(5000L, snap.cacheMisses)
        assertEquals(5000L, snap.cacheEvictions)
        assertEquals(5000L, snap.networkRequests)
        assertEquals(0L, snap.networkErrors)
        assertEquals(5000L, snap.detectionCount)
        assertEquals(5000L, snap.semaphoreWaits)
    }

    // ==================== Cache Hit Ratio Edge Cases ====================

    @Test
    fun cacheHitRatioEdgeCases() {
        // Case 1: No accesses => 0.0
        var snap = PerformanceMetrics.snapshot()
        assertEquals(0.0, snap.cacheHitRatio, 0.01, "No accesses => ratio 0.0")

        // Case 2: All hits => 1.0
        PerformanceMetrics.reset()
        repeat(10) { PerformanceMetrics.recordCacheHit() }
        snap = PerformanceMetrics.snapshot()
        assertEquals(1.0, snap.cacheHitRatio, 0.01, "All hits => ratio 1.0")

        // Case 3: All misses => 0.0
        PerformanceMetrics.reset()
        repeat(10) { PerformanceMetrics.recordCacheMiss() }
        snap = PerformanceMetrics.snapshot()
        assertEquals(0.0, snap.cacheHitRatio, 0.01, "All misses => ratio 0.0")
    }

    // ==================== Serialization ====================

    @Test
    fun metricsSnapshotIsSerializable() {
        PerformanceMetrics.recordParse(42)
        PerformanceMetrics.recordCacheHit()

        val original = PerformanceMetrics.snapshot()
        val json = Json.encodeToString(MetricsSnapshot.serializer(), original)
        val deserialized = Json.decodeFromString(MetricsSnapshot.serializer(), json)

        assertEquals(original, deserialized, "Round-tripped snapshot must equal original")
    }

    // ==================== MetricsReporter ====================

    @Test
    fun reportNowReturnsCurrentSnapshot() {
        PerformanceMetrics.recordParse(77)
        PerformanceMetrics.recordNetworkRequest(33, error = true)

        val reporter = MetricsReporter()
        val snap = reporter.reportNow()

        assertEquals(1L, snap.parseCount)
        assertEquals(77L, snap.parseTotalMs)
        assertEquals(1L, snap.networkRequests)
        assertEquals(1L, snap.networkErrors)
    }

    // ==================== Accumulation ====================

    @Test
    fun metricsAccumulateOverMultipleOperations() {
        // Simulate a realistic sequence of operations
        for (i in 1..10) {
            PerformanceMetrics.recordParse(i.toLong() * 10)
        }
        for (i in 1..7) {
            PerformanceMetrics.recordCacheHit()
        }
        for (i in 1..3) {
            PerformanceMetrics.recordCacheMiss()
        }
        PerformanceMetrics.recordCacheEviction()
        PerformanceMetrics.recordNetworkRequest(100, error = false)
        PerformanceMetrics.recordNetworkRequest(200, error = true)
        PerformanceMetrics.recordNetworkRequest(50, error = false)
        PerformanceMetrics.recordDetection(2)
        PerformanceMetrics.recordDetection(3)
        PerformanceMetrics.recordSemaphoreWait(15)

        val snap = PerformanceMetrics.snapshot()

        // Parse: 10 + 20 + ... + 100 = 550ms total, 10 ops, max = 100
        assertEquals(10L, snap.parseCount)
        assertEquals(550L, snap.parseTotalMs)
        assertEquals(100L, snap.parseMaxMs)
        assertEquals(55.0, snap.parseAvgMs, 0.01)

        // Cache: 7 hits, 3 misses, 1 eviction => ratio = 7/10 = 0.7
        assertEquals(7L, snap.cacheHits)
        assertEquals(3L, snap.cacheMisses)
        assertEquals(1L, snap.cacheEvictions)
        assertEquals(0.7, snap.cacheHitRatio, 0.01)

        // Network: 3 requests, 1 error, 350ms total
        assertEquals(3L, snap.networkRequests)
        assertEquals(1L, snap.networkErrors)
        assertEquals(350L, snap.networkTotalMs)
        assertEquals(350.0 / 3.0, snap.networkAvgMs, 0.1)
        assertEquals(1.0 / 3.0, snap.networkErrorRate, 0.01)

        // Detection: 2 ops, 5ms total => avg 2.5
        assertEquals(2L, snap.detectionCount)
        assertEquals(5L, snap.detectionTotalMs)
        assertEquals(2.5, snap.detectionAvgMs, 0.01)

        // Semaphore: 1 wait, 15ms
        assertEquals(1L, snap.semaphoreWaits)
        assertEquals(15L, snap.semaphoreWaitTotalMs)
        assertEquals(15.0, snap.semaphoreWaitAvgMs, 0.01)
    }
}
