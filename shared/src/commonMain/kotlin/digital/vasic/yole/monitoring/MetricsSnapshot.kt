/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Immutable snapshot of performance metrics
 *
 *########################################################*/
package digital.vasic.yole.monitoring

import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of all performance metrics at a point in time.
 *
 * Computed fields ([parseAvgMs], [cacheHitRatio], [networkAvgMs],
 * [networkErrorRate], [detectionAvgMs], [semaphoreWaitAvgMs]) are derived
 * from the raw counters at snapshot-creation time. Division-by-zero cases
 * yield 0.0.
 *
 * Annotated with [Serializable] so it can be round-tripped to JSON via
 * `kotlinx-serialization-json` if needed.
 *
 * @param parseCount Total number of parse operations recorded
 * @param parseTotalMs Cumulative parse time in milliseconds
 * @param parseMaxMs Maximum single-parse duration in milliseconds
 * @param parseAvgMs Average parse duration (0.0 when parseCount == 0)
 * @param cacheHits Total document-cache hits
 * @param cacheMisses Total document-cache misses
 * @param cacheEvictions Total document-cache evictions
 * @param cacheHitRatio Ratio of hits to total accesses (0.0 when no accesses)
 * @param networkRequests Total network requests recorded
 * @param networkErrors Total network errors recorded
 * @param networkTotalMs Cumulative network request time in milliseconds
 * @param networkAvgMs Average network request duration (0.0 when networkRequests == 0)
 * @param networkErrorRate Ratio of errors to total requests (0.0 when no requests)
 * @param detectionCount Total format-detection operations recorded
 * @param detectionTotalMs Cumulative detection time in milliseconds
 * @param detectionAvgMs Average detection duration (0.0 when detectionCount == 0)
 * @param semaphoreWaits Total semaphore wait events recorded
 * @param semaphoreWaitTotalMs Cumulative semaphore wait time in milliseconds
 * @param semaphoreWaitAvgMs Average semaphore wait duration (0.0 when semaphoreWaits == 0)
 * @param timestamp Epoch milliseconds when the snapshot was taken
 */
@Serializable
data class MetricsSnapshot(
    val parseCount: Long,
    val parseTotalMs: Long,
    val parseMaxMs: Long,
    val parseAvgMs: Double,
    val cacheHits: Long,
    val cacheMisses: Long,
    val cacheEvictions: Long,
    val cacheHitRatio: Double,
    val networkRequests: Long,
    val networkErrors: Long,
    val networkTotalMs: Long,
    val networkAvgMs: Double,
    val networkErrorRate: Double,
    val detectionCount: Long,
    val detectionTotalMs: Long,
    val detectionAvgMs: Double,
    val semaphoreWaits: Long,
    val semaphoreWaitTotalMs: Long,
    val semaphoreWaitAvgMs: Double,
    val timestamp: Long
)
