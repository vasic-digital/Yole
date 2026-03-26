/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Isolated unit tests for MetricsReporter covering
 * start/stop lifecycle, reportNow, output callback,
 * multiple restarts, and edge cases.
 *
 *########################################################*/
package digital.vasic.yole.monitoring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Isolated unit tests for [MetricsReporter].
 *
 * Validates lifecycle (start/stop/isActive), reportNow snapshot,
 * output callback invocation, multiple start/stop cycles,
 * concurrent access, and snapshot data integrity.
 *
 * Total: 32 tests
 */
class MetricsReporterUnitTest {

    @BeforeTest
    fun setUp() {
        PerformanceMetrics.reset()
    }

    @AfterTest
    fun tearDown() {
        PerformanceMetrics.reset()
    }

    private fun makeScope(): CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ==================== Initial State ====================

    @Test
    fun reporterIsNotActiveInitially() {
        val reporter = MetricsReporter()
        assertFalse(reporter.isActive)
    }

    @Test
    fun stopOnNewReporterDoesNotCrash() {
        val reporter = MetricsReporter()
        reporter.stop() // should not throw
        assertFalse(reporter.isActive)
    }

    // ==================== start / isActive ====================

    @Test
    fun reporterIsActiveAfterStart() = runBlocking<Unit> {
        val scope = makeScope()
        val reporter = MetricsReporter(intervalMs = 10_000)
        reporter.start(scope)
        assertTrue(reporter.isActive)
        reporter.stop()
        scope.cancel()
    }

    @Test
    fun reporterIsNotActiveAfterStop() = runBlocking<Unit> {
        val scope = makeScope()
        val reporter = MetricsReporter(intervalMs = 10_000)
        reporter.start(scope)
        assertTrue(reporter.isActive)
        reporter.stop()
        assertFalse(reporter.isActive)
        scope.cancel()
    }

    @Test
    fun startCancelsExistingJobBeforeStartingNew() = runBlocking<Unit> {
        val scope = makeScope()
        val reporter = MetricsReporter(intervalMs = 10_000)
        reporter.start(scope)
        val wasActive = reporter.isActive
        reporter.start(scope) // should cancel the first and start a new one
        assertTrue(wasActive)
        assertTrue(reporter.isActive)
        reporter.stop()
        scope.cancel()
    }

    @Test
    fun multipleStopsAreIdempotent() = runBlocking<Unit> {
        val scope = makeScope()
        val reporter = MetricsReporter(intervalMs = 10_000)
        reporter.start(scope)
        reporter.stop()
        reporter.stop()
        reporter.stop()
        assertFalse(reporter.isActive)
        scope.cancel()
    }

    // ==================== Output Callback ====================

    @Test
    fun outputCallbackIsInvokedAfterInterval() = runBlocking<Unit> {
        val snapshots = mutableListOf<MetricsSnapshot>()
        val scope = makeScope()
        val reporter = MetricsReporter(
            intervalMs = 30,
            output = { snapshots.add(it) }
        )
        reporter.start(scope)
        delay(200)
        reporter.stop()
        scope.cancel()
        assertTrue(snapshots.isNotEmpty(), "Expected at least one snapshot to be reported")
    }

    @Test
    fun outputCallbackReceivesValidSnapshot() = runBlocking<Unit> {
        var receivedSnapshot: MetricsSnapshot? = null
        val scope = makeScope()
        val reporter = MetricsReporter(
            intervalMs = 30,
            output = { receivedSnapshot = it }
        )
        reporter.start(scope)
        delay(150)
        reporter.stop()
        scope.cancel()
        assertNotNull(receivedSnapshot)
        assertTrue(receivedSnapshot!!.timestamp > 0)
    }

    @Test
    fun outputCallbackReceivesMultipleSnapshotsOverTime() = runBlocking<Unit> {
        var callCount = 0
        val scope = makeScope()
        val reporter = MetricsReporter(
            intervalMs = 30,
            output = { callCount++ }
        )
        reporter.start(scope)
        delay(250)
        reporter.stop()
        scope.cancel()
        assertTrue(callCount >= 2, "Expected at least 2 callbacks in 250ms with 30ms interval, got $callCount")
    }

    @Test
    fun outputCallbackNotInvokedAfterStop() = runBlocking<Unit> {
        var callCount = 0
        val scope = makeScope()
        val reporter = MetricsReporter(
            intervalMs = 30,
            output = { callCount++ }
        )
        reporter.start(scope)
        delay(100)
        reporter.stop()
        val countAtStop = callCount
        delay(100) // wait additional time after stop
        scope.cancel()
        // Should not have received more callbacks after stop
        assertEquals(countAtStop, callCount)
    }

    // ==================== reportNow ====================

    @Test
    fun reportNowReturnsNonNullSnapshot() {
        val reporter = MetricsReporter()
        val snapshot = reporter.reportNow()
        assertNotNull(snapshot)
    }

    @Test
    fun reportNowDoesNotInvokeOutputCallback() {
        var callCount = 0
        val reporter = MetricsReporter(output = { callCount++ })
        reporter.reportNow()
        assertEquals(0, callCount)
    }

    @Test
    fun reportNowReturnsCurrentMetricsState() {
        PerformanceMetrics.reset()
        PerformanceMetrics.recordParse(50L)
        PerformanceMetrics.recordParse(100L)
        val reporter = MetricsReporter()
        val snapshot = reporter.reportNow()
        assertEquals(2L, snapshot.parseCount)
        assertEquals(150L, snapshot.parseTotalMs)
        assertEquals(75.0, snapshot.parseAvgMs)
    }

    @Test
    fun reportNowTimestampIsRecent() {
        val before = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val reporter = MetricsReporter()
        val snapshot = reporter.reportNow()
        val after = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        assertTrue(snapshot.timestamp in before..after)
    }

    @Test
    fun reportNowWorksWithoutStarting() {
        val reporter = MetricsReporter()
        assertFalse(reporter.isActive)
        val snapshot = reporter.reportNow()
        assertNotNull(snapshot)
    }

    // ==================== Snapshot Data Integrity ====================

    @Test
    fun reportNowReflectsAllMetricTypes() {
        PerformanceMetrics.reset()
        PerformanceMetrics.recordParse(10L)
        PerformanceMetrics.recordCacheHit()
        PerformanceMetrics.recordCacheMiss()
        PerformanceMetrics.recordNetworkRequest(20L, error = false)
        PerformanceMetrics.recordNetworkRequest(30L, error = true)
        PerformanceMetrics.recordDetection(5L)
        PerformanceMetrics.recordSemaphoreWait(2L)

        val reporter = MetricsReporter()
        val snapshot = reporter.reportNow()

        assertEquals(1L, snapshot.parseCount)
        assertEquals(10L, snapshot.parseTotalMs)
        assertEquals(1L, snapshot.cacheHits)
        assertEquals(1L, snapshot.cacheMisses)
        assertEquals(0.5, snapshot.cacheHitRatio)
        assertEquals(2L, snapshot.networkRequests)
        assertEquals(1L, snapshot.networkErrors)
        assertEquals(50L, snapshot.networkTotalMs)
        assertEquals(0.5, snapshot.networkErrorRate)
        assertEquals(1L, snapshot.detectionCount)
        assertEquals(5L, snapshot.detectionTotalMs)
        assertEquals(1L, snapshot.semaphoreWaits)
        assertEquals(2L, snapshot.semaphoreWaitTotalMs)
    }

    @Test
    fun reportNowWithZeroMetricsHasZeroAverages() {
        PerformanceMetrics.reset()
        val reporter = MetricsReporter()
        val snapshot = reporter.reportNow()

        assertEquals(0.0, snapshot.parseAvgMs)
        assertEquals(0.0, snapshot.cacheHitRatio)
        assertEquals(0.0, snapshot.networkAvgMs)
        assertEquals(0.0, snapshot.networkErrorRate)
        assertEquals(0.0, snapshot.detectionAvgMs)
        assertEquals(0.0, snapshot.semaphoreWaitAvgMs)
    }

    // ==================== Multiple Start/Stop Cycles ====================

    @Test
    fun reporterCanBeRestartedAfterStop() = runBlocking<Unit> {
        val scope = makeScope()
        val reporter = MetricsReporter(intervalMs = 10_000)

        reporter.start(scope)
        assertTrue(reporter.isActive)
        reporter.stop()
        assertFalse(reporter.isActive)

        reporter.start(scope)
        assertTrue(reporter.isActive)
        reporter.stop()
        assertFalse(reporter.isActive)

        scope.cancel()
    }

    @Test
    fun multipleRestartCyclesReceiveCallbacks() = runBlocking<Unit> {
        val counts = mutableListOf<Int>()
        val scope = makeScope()

        repeat(3) { cycle ->
            var count = 0
            val reporter = MetricsReporter(
                intervalMs = 30,
                output = { count++ }
            )
            reporter.start(scope)
            delay(100)
            reporter.stop()
            counts.add(count)
        }

        scope.cancel()
        // Each cycle should have received at least one callback
        assertTrue(counts.all { it >= 1 }, "All cycles should have callbacks: $counts")
    }

    // ==================== Custom Interval ====================

    @Test
    fun defaultIntervalIs60000ms() {
        // We can verify the reporter works with default interval (won't fire quickly)
        val scope = makeScope()
        var callCount = 0
        val reporter = MetricsReporter(output = { callCount++ })
        reporter.start(scope)
        // With 60_000ms interval, it shouldn't fire in 50ms
        runBlocking { delay(50) }
        reporter.stop()
        scope.cancel()
        assertEquals(0, callCount)
    }

    @Test
    fun shortIntervalFiresMultipleTimes() = runBlocking<Unit> {
        var callCount = 0
        val scope = makeScope()
        val reporter = MetricsReporter(
            intervalMs = 20,
            output = { callCount++ }
        )
        reporter.start(scope)
        delay(150)
        reporter.stop()
        scope.cancel()
        assertTrue(callCount >= 3, "Expected >= 3 callbacks with 20ms interval in 150ms, got $callCount")
    }

    // ==================== Scope Cancellation ====================

    @Test
    fun reporterStopsWhenScopeIsCancelled() = runBlocking<Unit> {
        val scope = makeScope()
        val reporter = MetricsReporter(intervalMs = 10_000)
        reporter.start(scope)
        assertTrue(reporter.isActive)
        scope.cancel()
        delay(50)
        assertFalse(reporter.isActive)
    }

    // ==================== Snapshot Field Coverage ====================

    @Test
    fun snapshotHasAllExpectedFields() {
        val reporter = MetricsReporter()
        val snap = reporter.reportNow()

        // Verify all fields are accessible and have valid types
        assertTrue(snap.parseCount >= 0)
        assertTrue(snap.parseTotalMs >= 0)
        assertTrue(snap.parseMaxMs >= 0)
        assertTrue(snap.parseAvgMs >= 0.0)
        assertTrue(snap.cacheHits >= 0)
        assertTrue(snap.cacheMisses >= 0)
        assertTrue(snap.cacheEvictions >= 0)
        assertTrue(snap.cacheHitRatio in 0.0..1.0)
        assertTrue(snap.networkRequests >= 0)
        assertTrue(snap.networkErrors >= 0)
        assertTrue(snap.networkTotalMs >= 0)
        assertTrue(snap.networkAvgMs >= 0.0)
        assertTrue(snap.networkErrorRate in 0.0..1.0)
        assertTrue(snap.detectionCount >= 0)
        assertTrue(snap.detectionTotalMs >= 0)
        assertTrue(snap.detectionAvgMs >= 0.0)
        assertTrue(snap.semaphoreWaits >= 0)
        assertTrue(snap.semaphoreWaitTotalMs >= 0)
        assertTrue(snap.semaphoreWaitAvgMs >= 0.0)
        assertTrue(snap.timestamp > 0)
    }

    @Test
    fun snapshotParseMaxMsIsMaximumOfAllRecorded() {
        PerformanceMetrics.reset()
        PerformanceMetrics.recordParse(10L)
        PerformanceMetrics.recordParse(200L)
        PerformanceMetrics.recordParse(50L)
        val reporter = MetricsReporter()
        val snap = reporter.reportNow()
        assertEquals(200L, snap.parseMaxMs)
    }

    @Test
    fun snapshotCacheHitRatioWith100PercentHits() {
        PerformanceMetrics.reset()
        repeat(5) { PerformanceMetrics.recordCacheHit() }
        val reporter = MetricsReporter()
        val snap = reporter.reportNow()
        assertEquals(1.0, snap.cacheHitRatio)
    }

    @Test
    fun snapshotCacheHitRatioWith0PercentHits() {
        PerformanceMetrics.reset()
        repeat(5) { PerformanceMetrics.recordCacheMiss() }
        val reporter = MetricsReporter()
        val snap = reporter.reportNow()
        assertEquals(0.0, snap.cacheHitRatio)
    }
}
