/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Semaphore-based connection limiter for protocol services
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Limits concurrent connections/operations for a protocol service using a [Semaphore].
 *
 * Non-blocking: callers suspend (not block) until a permit is available.
 *
 * @param maxConcurrent Maximum number of concurrent operations.
 * @param acquireTimeout Maximum time to wait for a permit before failing.
 * @param name Human-readable name for diagnostics.
 */
class ConnectionLimiter(
    val maxConcurrent: Int = 5,
    private val acquireTimeout: Duration = 30.seconds,
    val name: String = "default"
) {
    private val semaphore = Semaphore(maxConcurrent)

    val availablePermits: Int get() = semaphore.availablePermits

    suspend fun <T> withConnection(block: suspend () -> T): T {
        return withTimeout(acquireTimeout) {
            semaphore.withPermit {
                block()
            }
        }
    }
}
