/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Circuit breaker for network protocol resilience
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Circuit breaker that prevents cascading failures by tracking error rates
 * and temporarily blocking operations when a failure threshold is exceeded.
 *
 * States:
 * - CLOSED: Normal operation. Failures are counted.
 * - OPEN: Operations are blocked. After [resetTimeout], transitions to HALF_OPEN.
 * - HALF_OPEN: A single trial operation is allowed. Success -> CLOSED, failure -> OPEN.
 *
 * @param failureThreshold Number of consecutive failures before opening the circuit.
 * @param resetTimeout Duration to wait before transitioning from OPEN to HALF_OPEN.
 * @param name Human-readable name for logging/diagnostics.
 */
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val resetTimeout: Duration = 30.seconds,
    val name: String = "default"
) {
    enum class State { CLOSED, OPEN, HALF_OPEN }

    private val mutex = Mutex()
    private var _state = State.CLOSED
    private var failureCount = 0
    private var lastFailureTime: Instant? = null
    private var successCount = 0
    private var totalCalls = 0L

    val state: State get() = _state
    val failures: Int get() = failureCount
    val successes: Int get() = successCount
    val calls: Long get() = totalCalls

    suspend fun <T> execute(block: suspend () -> T): Result<T> = mutex.withLock {
        totalCalls++
        when (_state) {
            State.OPEN -> {
                val now = Clock.System.now()
                val elapsed = lastFailureTime?.let { now - it }
                if (elapsed != null && elapsed >= resetTimeout) {
                    _state = State.HALF_OPEN
                    tryExecution(block)
                } else {
                    Result.failure(CircuitBreakerOpenException(name, resetTimeout))
                }
            }
            State.HALF_OPEN -> tryExecution(block)
            State.CLOSED -> tryExecution(block)
        }
    }

    private suspend fun <T> tryExecution(block: suspend () -> T): Result<T> {
        return try {
            val result = block()
            onSuccess()
            Result.success(result)
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            onFailure()
            Result.failure(e)
        }
    }

    private fun onSuccess() {
        failureCount = 0
        successCount++
        _state = State.CLOSED
    }

    private fun onFailure() {
        failureCount++
        lastFailureTime = Clock.System.now()
        if (failureCount >= failureThreshold) {
            _state = State.OPEN
        }
    }

    suspend fun reset() = mutex.withLock {
        _state = State.CLOSED
        failureCount = 0
        lastFailureTime = null
    }
}

class CircuitBreakerOpenException(
    name: String,
    timeout: Duration
) : Exception("Circuit breaker '$name' is OPEN. Retry after $timeout.")
