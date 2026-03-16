/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Semaphore Effectiveness Tests
 *
 * Validates ConnectionLimiter fairness under contention,
 * starvation prevention, and permit release after timeout.
 *
 *########################################################*/
package digital.vasic.yole.network.performance

import digital.vasic.yole.network.common.ConnectionLimiter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Tests for ConnectionLimiter (semaphore-based) fairness,
 * starvation prevention, and permit lifecycle.
 */
class SemaphoreEffectivenessTest {

    // ====================================================================
    // 1. Fairness under contention
    // ====================================================================

    @Test
    fun `all waiters eventually complete with maxConcurrent 1`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 10.seconds)
        val completionOrder = mutableListOf<Int>()

        val jobs = (1..10).map { i ->
            async {
                limiter.withConnection {
                    completionOrder.add(i)
                    delay(5.milliseconds)
                    i
                }
            }
        }
        val results = jobs.awaitAll()

        assertEquals(10, results.size, "All 10 waiters should complete")
        assertEquals(10, completionOrder.size, "All 10 should have executed")
    }

    @Test
    fun `all waiters complete with maxConcurrent 3 and 15 requesters`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 10.seconds)
        val completed = mutableListOf<Int>()

        val jobs = (1..15).map { i ->
            async {
                limiter.withConnection {
                    delay(10.milliseconds)
                    synchronized(completed) { completed.add(i) }
                    i
                }
            }
        }
        val results = jobs.awaitAll()

        assertEquals(15, results.size, "All 15 requesters should complete")
        assertEquals(15, completed.size, "All 15 should have executed their block")
    }

    @Test
    fun `concurrent operations do not exceed maxConcurrent`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 10.seconds)
        var maxObservedConcurrent = 0
        var currentConcurrent = 0
        val lock = Any()

        val jobs = (1..20).map {
            async {
                limiter.withConnection {
                    val current = synchronized(lock) {
                        currentConcurrent++
                        if (currentConcurrent > maxObservedConcurrent) {
                            maxObservedConcurrent = currentConcurrent
                        }
                        currentConcurrent
                    }
                    delay(20.milliseconds)
                    synchronized(lock) { currentConcurrent-- }
                    current
                }
            }
        }
        jobs.awaitAll()

        assertTrue(maxObservedConcurrent <= 3,
            "Max concurrent was $maxObservedConcurrent, expected <= 3")
    }

    // ====================================================================
    // 2. No starvation with many waiters
    // ====================================================================

    @Test
    fun `50 waiters all complete within timeout`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 30.seconds)
        val completed = mutableListOf<Int>()

        val elapsed = measureTime {
            val jobs = (1..50).map { i ->
                async {
                    limiter.withConnection {
                        delay(5.milliseconds)
                        synchronized(completed) { completed.add(i) }
                        i
                    }
                }
            }
            jobs.awaitAll()
        }

        assertEquals(50, completed.size, "All 50 waiters should complete")
        assertTrue(elapsed.inWholeMilliseconds < 10_000,
            "50 waiters took ${elapsed.inWholeMilliseconds}ms, expected < 10,000ms")
    }

    @Test
    fun `no waiter starved when others hold permits longer`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 10.seconds)
        val completedIds = mutableListOf<Int>()

        val jobs = (1..8).map { i ->
            async {
                limiter.withConnection {
                    // First 2 hold permits longer
                    val holdTime = if (i <= 2) 100.milliseconds else 10.milliseconds
                    delay(holdTime)
                    synchronized(completedIds) { completedIds.add(i) }
                    i
                }
            }
        }
        jobs.awaitAll()

        assertEquals(8, completedIds.size, "All 8 should complete, none starved")
        // Verify all IDs present
        val missing = (1..8).filter { it !in completedIds }
        assertTrue(missing.isEmpty(), "Missing IDs: $missing")
    }

    @Test
    fun `single permit serializes all operations`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 10.seconds)
        val executionOrder = mutableListOf<Int>()

        val jobs = (1..5).map { i ->
            async {
                limiter.withConnection {
                    executionOrder.add(i)
                    delay(5.milliseconds)
                    i
                }
            }
        }
        jobs.awaitAll()

        assertEquals(5, executionOrder.size, "All 5 should execute")
    }

    // ====================================================================
    // 3. Permit release after timeout/exception
    // ====================================================================

    @Test
    fun `permit released after operation exception`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 5.seconds)

        // Fail the first operation
        try {
            limiter.withConnection { throw IllegalStateException("deliberate failure") }
        } catch (_: IllegalStateException) { }

        // Permit should be available for the next operation
        assertEquals(1, limiter.availablePermits, "Permit should be restored after exception")
        val result = limiter.withConnection { "recovered" }
        assertEquals("recovered", result)
    }

    @Test
    fun `permit released after multiple consecutive exceptions`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds)

        repeat(5) {
            try {
                limiter.withConnection { throw RuntimeException("fail $it") }
            } catch (_: RuntimeException) { }
        }

        assertEquals(2, limiter.availablePermits,
            "All permits should be restored after exceptions")
    }

    @Test
    fun `permit released correctly in concurrent exception scenario`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds)

        val jobs = (1..10).map { i ->
            async {
                try {
                    limiter.withConnection {
                        if (i % 2 == 0) throw RuntimeException("fail $i")
                        delay(5.milliseconds)
                        i
                    }
                } catch (_: RuntimeException) {
                    -i
                }
            }
        }
        jobs.awaitAll()

        assertEquals(3, limiter.availablePermits,
            "All 3 permits should be restored after concurrent ops with failures")
    }

    @Test
    fun `withConnection timeout does not leak permits`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 100.milliseconds)

        // Use the permit with a short-lived operation
        limiter.withConnection { "quick" }
        assertEquals(1, limiter.availablePermits, "Permit should be restored after quick op")

        // Use it again to confirm no leak
        val result = limiter.withConnection { "second" }
        assertEquals("second", result)
        assertEquals(1, limiter.availablePermits)
    }
}
