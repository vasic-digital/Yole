/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Singleton performance metrics collector
 *
 *########################################################*/
package digital.vasic.yole.monitoring

import digital.vasic.yole.util.platformSynchronized
import kotlinx.datetime.Clock

/**
 * Singleton collector for application-wide performance metrics.
 *
 * Records timing and count data for parsing, caching, networking,
 * format detection, and semaphore waits. All mutable state is protected
 * by [platformSynchronized] with an internal [lock], matching the
 * thread-safety pattern used by [digital.vasic.yole.network.common.SecurityEventLogger].
 *
 * Counters are plain [Long] fields guarded by [platformSynchronized]
 * rather than atomic types, which ensures cross-platform compatibility
 * (Android, Desktop, iOS, Wasm) without external dependencies.
 *
 * Usage:
 * ```kotlin
 * val start = Clock.System.now().toEpochMilliseconds()
 * val result = parser.parse(content)
 * val elapsed = Clock.System.now().toEpochMilliseconds() - start
 * PerformanceMetrics.recordParse(elapsed)
 *
 * val snapshot = PerformanceMetrics.snapshot()
 * println("Average parse time: ${snapshot.parseAvgMs}ms")
 * ```
 *
 * @see MetricsSnapshot
 * @see MetricsReporter
 */
object PerformanceMetrics {

    // ==================== Parse Metrics ====================
    private var _parseCount: Long = 0
    private var _parseTotalMs: Long = 0
    private var _parseMaxMs: Long = 0

    // ==================== Cache Metrics ====================
    private var _cacheHits: Long = 0
    private var _cacheMisses: Long = 0
    private var _cacheEvictions: Long = 0

    // ==================== Network Metrics ====================
    private var _networkRequests: Long = 0
    private var _networkErrors: Long = 0
    private var _networkTotalMs: Long = 0

    // ==================== Format Detection Metrics ====================
    private var _detectionCount: Long = 0
    private var _detectionTotalMs: Long = 0

    // ==================== Semaphore Metrics ====================
    private var _semaphoreWaits: Long = 0
    private var _semaphoreWaitTotalMs: Long = 0

    private val lock = Any()

    // ==================== Recording Methods ====================

    /**
     * Record a parse operation.
     *
     * @param durationMs Time taken in milliseconds (must be >= 0)
     */
    fun recordParse(durationMs: Long) {
        platformSynchronized(lock) {
            _parseCount++
            _parseTotalMs += durationMs
            if (durationMs > _parseMaxMs) _parseMaxMs = durationMs
        }
    }

    /**
     * Record a document-cache hit.
     */
    fun recordCacheHit() {
        platformSynchronized(lock) { _cacheHits++ }
    }

    /**
     * Record a document-cache miss.
     */
    fun recordCacheMiss() {
        platformSynchronized(lock) { _cacheMisses++ }
    }

    /**
     * Record a document-cache eviction.
     */
    fun recordCacheEviction() {
        platformSynchronized(lock) { _cacheEvictions++ }
    }

    /**
     * Record a network request.
     *
     * @param durationMs Time taken in milliseconds (must be >= 0)
     * @param error Whether the request resulted in an error
     */
    fun recordNetworkRequest(durationMs: Long, error: Boolean = false) {
        platformSynchronized(lock) {
            _networkRequests++
            _networkTotalMs += durationMs
            if (error) _networkErrors++
        }
    }

    /**
     * Record a format-detection operation.
     *
     * @param durationMs Time taken in milliseconds (must be >= 0)
     */
    fun recordDetection(durationMs: Long) {
        platformSynchronized(lock) {
            _detectionCount++
            _detectionTotalMs += durationMs
        }
    }

    /**
     * Record a semaphore wait event.
     *
     * @param durationMs Time spent waiting for the semaphore (must be >= 0)
     */
    fun recordSemaphoreWait(durationMs: Long) {
        platformSynchronized(lock) {
            _semaphoreWaits++
            _semaphoreWaitTotalMs += durationMs
        }
    }

    // ==================== Snapshot & Reset ====================

    /**
     * Create an immutable snapshot of all current metric values.
     *
     * Computed fields (averages, ratios) are derived inside the lock
     * to ensure consistency between numerator and denominator.
     *
     * @return An immutable [MetricsSnapshot] reflecting all counters at this instant
     */
    fun snapshot(): MetricsSnapshot = platformSynchronized(lock) {
        val totalCacheAccesses = _cacheHits + _cacheMisses
        MetricsSnapshot(
            parseCount = _parseCount,
            parseTotalMs = _parseTotalMs,
            parseMaxMs = _parseMaxMs,
            parseAvgMs = if (_parseCount > 0) _parseTotalMs.toDouble() / _parseCount else 0.0,
            cacheHits = _cacheHits,
            cacheMisses = _cacheMisses,
            cacheEvictions = _cacheEvictions,
            cacheHitRatio = if (totalCacheAccesses > 0) _cacheHits.toDouble() / totalCacheAccesses else 0.0,
            networkRequests = _networkRequests,
            networkErrors = _networkErrors,
            networkTotalMs = _networkTotalMs,
            networkAvgMs = if (_networkRequests > 0) _networkTotalMs.toDouble() / _networkRequests else 0.0,
            networkErrorRate = if (_networkRequests > 0) _networkErrors.toDouble() / _networkRequests else 0.0,
            detectionCount = _detectionCount,
            detectionTotalMs = _detectionTotalMs,
            detectionAvgMs = if (_detectionCount > 0) _detectionTotalMs.toDouble() / _detectionCount else 0.0,
            semaphoreWaits = _semaphoreWaits,
            semaphoreWaitTotalMs = _semaphoreWaitTotalMs,
            semaphoreWaitAvgMs = if (_semaphoreWaits > 0) _semaphoreWaitTotalMs.toDouble() / _semaphoreWaits else 0.0,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    /**
     * Reset all counters to zero.
     *
     * Typically called after a snapshot has been persisted or reported,
     * or at the start of a profiling window.
     */
    fun reset() {
        platformSynchronized(lock) {
            _parseCount = 0
            _parseTotalMs = 0
            _parseMaxMs = 0
            _cacheHits = 0
            _cacheMisses = 0
            _cacheEvictions = 0
            _networkRequests = 0
            _networkErrors = 0
            _networkTotalMs = 0
            _detectionCount = 0
            _detectionTotalMs = 0
            _semaphoreWaits = 0
            _semaphoreWaitTotalMs = 0
        }
    }
}
