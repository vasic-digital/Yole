/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Periodic metrics reporter
 *
 *########################################################*/
package digital.vasic.yole.monitoring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodic reporter that takes snapshots from [PerformanceMetrics] at a
 * configurable interval and delivers them to an [output] callback.
 *
 * The reporter runs as a single coroutine launched in the provided
 * [CoroutineScope]. Calling [stop] cancels the job; calling [start]
 * again with a new (or the same) scope restarts reporting.
 *
 * Thread-safety: [start] and [stop] must not be called concurrently from
 * different threads. This is consistent with typical lifecycle usage
 * (start on init, stop on teardown).
 *
 * Usage:
 * ```kotlin
 * val reporter = MetricsReporter(
 *     intervalMs = 30_000,
 *     output = { snapshot -> logger.info("metrics: $snapshot") }
 * )
 * reporter.start(viewModelScope)
 * // ... later ...
 * reporter.stop()
 * ```
 *
 * @param intervalMs Delay between successive reports in milliseconds. Defaults to 60 000 (1 minute).
 * @param output Callback invoked with each [MetricsSnapshot]. Defaults to [println].
 *
 * @see PerformanceMetrics
 * @see MetricsSnapshot
 */
class MetricsReporter(
    private val intervalMs: Long = 60_000,
    private val output: (MetricsSnapshot) -> Unit = { println(it) }
) {

    private var reportingJob: Job? = null

    /**
     * Start periodic reporting in the given [scope].
     *
     * If a previous reporting job is still active it is cancelled first,
     * ensuring at most one reporting coroutine runs at a time.
     *
     * @param scope The [CoroutineScope] in which to launch the reporting coroutine
     */
    fun start(scope: CoroutineScope) {
        reportingJob?.cancel()
        reportingJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                val snapshot = PerformanceMetrics.snapshot()
                output(snapshot)
            }
        }
    }

    /**
     * Stop periodic reporting by cancelling the reporting coroutine.
     *
     * Safe to call even if reporting was never started or has already stopped.
     */
    fun stop() {
        reportingJob?.cancel()
        reportingJob = null
    }

    /**
     * Whether the reporter is currently active.
     *
     * @return `true` if a reporting coroutine is running
     */
    val isActive: Boolean
        get() = reportingJob?.isActive == true

    /**
     * Take an immediate snapshot and return it without waiting for the
     * next scheduled interval. Does **not** invoke the [output] callback.
     *
     * @return The current [MetricsSnapshot]
     */
    fun reportNow(): MetricsSnapshot = PerformanceMetrics.snapshot()
}
