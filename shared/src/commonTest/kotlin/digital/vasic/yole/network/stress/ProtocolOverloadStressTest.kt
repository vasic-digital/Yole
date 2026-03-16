/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Protocol Overload Stress Tests
 *
 * Validates CircuitBreaker and ConnectionLimiter behavior
 * under simulated failure floods, overload conditions,
 * and recovery scenarios.
 *
 *########################################################*/

package digital.vasic.yole.network.stress

import digital.vasic.yole.network.common.CircuitBreaker
import digital.vasic.yole.network.common.CircuitBreakerOpenException
import digital.vasic.yole.network.common.ConnectionLimiter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Stress tests for protocol resilience components.
 *
 * Validates that CircuitBreaker trips under failure floods,
 * ConnectionLimiter rejects beyond max concurrent, and
 * recovery works correctly after circuit breaker reset.
 */
class ProtocolOverloadStressTest {

    // ==================== CIRCUIT BREAKER TESTS ====================

    @Test
    fun `CircuitBreaker trips after failure threshold under rapid failures`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5, resetTimeout = 10.seconds, name = "overload-test")

        // Flood with failures
        repeat(5) {
            cb.execute<Unit> { throw RuntimeException("simulated failure $it") }
        }

        assertEquals(CircuitBreaker.State.OPEN, cb.state, "Circuit should be OPEN after 5 failures")

        // Subsequent call should fail immediately with CircuitBreakerOpenException
        val result = cb.execute { "should-not-run" }
        assertTrue(result.isFailure, "Call on OPEN circuit should fail")
        assertTrue(
            result.exceptionOrNull() is CircuitBreakerOpenException,
            "Should throw CircuitBreakerOpenException"
        )
    }

    @Test
    fun `CircuitBreaker stays CLOSED when failures below threshold`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10, resetTimeout = 5.seconds, name = "below-threshold")

        // Only 4 failures (below threshold of 10)
        repeat(4) {
            cb.execute<Unit> { throw RuntimeException("fail") }
        }

        assertEquals(CircuitBreaker.State.CLOSED, cb.state, "Circuit should remain CLOSED below threshold")

        val result = cb.execute { "success" }
        assertTrue(result.isSuccess, "Successful call should work on CLOSED circuit")
        assertEquals("success", result.getOrNull())
    }

    @Test
    fun `CircuitBreaker concurrent failure flood trips correctly`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 3, resetTimeout = 10.seconds, name = "concurrent-flood")

        // Because CircuitBreaker uses a mutex, concurrent calls are serialized
        // Flood with failures from multiple coroutines
        val jobs = (1..20).map {
            launch(Dispatchers.Default) {
                cb.execute<Unit> { throw RuntimeException("flood failure") }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(CircuitBreaker.State.OPEN, cb.state, "Circuit should be OPEN after concurrent failure flood")
    }

    @Test
    fun `CircuitBreaker recovery after reset`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 3, resetTimeout = 100.milliseconds, name = "recovery-test")

        // Trip the breaker
        repeat(3) {
            cb.execute<Unit> { throw RuntimeException("trip failure") }
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Reset and verify recovery
        cb.reset()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state, "Should be CLOSED after reset")

        val result = cb.execute { "recovered" }
        assertTrue(result.isSuccess, "Should succeed after reset")
        assertEquals("recovered", result.getOrNull())
    }

    @Test
    fun `CircuitBreaker HALF_OPEN transitions correctly on success`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 100.milliseconds, name = "half-open-success")

        // Trip the breaker
        repeat(2) {
            cb.execute<Unit> { throw RuntimeException("trip") }
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Wait for reset timeout to elapse (use real time)
        withContext(Dispatchers.Default) { delay(200) }

        // Next call should transition to HALF_OPEN and succeed
        val result = cb.execute { "half-open-success" }
        assertTrue(result.isSuccess, "HALF_OPEN trial should succeed")
        assertEquals(CircuitBreaker.State.CLOSED, cb.state, "Should transition to CLOSED on success")
    }

    @Test
    fun `CircuitBreaker HALF_OPEN transitions back to OPEN on failure`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 100.milliseconds, name = "half-open-fail")

        // Trip the breaker
        repeat(2) {
            cb.execute<Unit> { throw RuntimeException("trip") }
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Wait for reset timeout
        withContext(Dispatchers.Default) { delay(200) }

        // Fail the trial call
        cb.execute<Unit> { throw RuntimeException("trial failure") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state, "Should go back to OPEN on HALF_OPEN failure")
    }

    @Test
    fun `CircuitBreaker success resets failure counter`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5, resetTimeout = 10.seconds, name = "success-reset")

        // Accumulate some failures (below threshold)
        repeat(4) {
            cb.execute<Unit> { throw RuntimeException("fail") }
        }
        assertEquals(4, cb.failures)

        // One success should reset the counter
        cb.execute { "success" }
        assertEquals(0, cb.failures, "Failure count should reset after success")

        // Now 4 more failures should not trip (counter was reset)
        repeat(4) {
            cb.execute<Unit> { throw RuntimeException("fail again") }
        }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state, "Should still be CLOSED after reset + 4 failures")
    }

    @Test
    fun `CircuitBreaker tracks total call count under load`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100, resetTimeout = 10.seconds, name = "call-count")
        val totalCalls = 200

        val jobs = (1..totalCalls).map {
            launch(Dispatchers.Default) {
                cb.execute { "ok" }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(totalCalls.toLong(), cb.calls, "Total call count should be $totalCalls")
        assertEquals(totalCalls, cb.successes, "All calls should be successes")
    }

    // ==================== CONNECTION LIMITER TESTS ====================

    @Test
    fun `ConnectionLimiter limits max concurrent operations`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "limit-test")
        val mutex = Mutex()
        var maxObservedConcurrent = 0
        var currentConcurrent = 0

        val jobs = (1..20).map {
            launch(Dispatchers.Default) {
                limiter.withConnection {
                    mutex.withLock {
                        currentConcurrent++
                        if (currentConcurrent > maxObservedConcurrent) {
                            maxObservedConcurrent = currentConcurrent
                        }
                    }
                    delay(50)
                    mutex.withLock { currentConcurrent-- }
                }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(maxObservedConcurrent <= 3,
            "Max concurrent should not exceed 3, was $maxObservedConcurrent")
        assertEquals(0, currentConcurrent, "All operations should have completed")
    }

    @Test
    fun `ConnectionLimiter allows operations up to max concurrent`() = runBlocking<Unit> {
        val maxConcurrent = 5
        val limiter = ConnectionLimiter(maxConcurrent = maxConcurrent, acquireTimeout = 5.seconds, name = "allow-test")
        val mutex = Mutex()
        var peakConcurrent = 0
        var currentConcurrent = 0

        val jobs = (1..50).map {
            launch(Dispatchers.Default) {
                limiter.withConnection {
                    val current = mutex.withLock { ++currentConcurrent }
                    mutex.withLock { if (current > peakConcurrent) peakConcurrent = current }
                    delay(10)
                    mutex.withLock { currentConcurrent-- }
                }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(
            peakConcurrent <= maxConcurrent,
            "Peak concurrent ($peakConcurrent) must not exceed limit ($maxConcurrent)"
        )
    }

    @Test
    fun `ConnectionLimiter available permits track correctly`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "permits-test")

        assertEquals(3, limiter.availablePermits, "Should start with 3 available permits")

        val blocker = launch(Dispatchers.Default) {
            limiter.withConnection { delay(2000) }
        }
        withContext(Dispatchers.Default) { delay(100) }

        assertTrue(limiter.availablePermits < 3, "Available permits should decrease during operation")

        blocker.cancel()
    }

    @Test
    fun `ConnectionLimiter operations complete fully under sustained load`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 10.seconds, name = "sustained-test")
        val mutex = Mutex()
        var completedCount = 0

        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                limiter.withConnection {
                    delay(5)
                    mutex.withLock { completedCount++ }
                }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(100, completedCount, "All 100 operations should complete under sustained load")
    }
}
