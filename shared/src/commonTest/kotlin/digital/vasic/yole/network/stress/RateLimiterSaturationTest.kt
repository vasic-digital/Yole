/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Rate Limiter Saturation Tests
 *
 * Validates sustained load handling, starvation prevention,
 * and throughput measurement under saturation for all
 * rate limiting utilities.
 *
 *########################################################*/

package digital.vasic.yole.network.stress

import digital.vasic.yole.util.RateLimiter
import digital.vasic.yole.util.TokenBucket
import digital.vasic.yole.util.AdaptiveRateLimiter
import digital.vasic.yole.util.OperationThrottler
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Saturation tests for rate limiting utilities.
 *
 * Validates that rate limiters handle sustained load correctly,
 * prevent request starvation, and maintain expected throughput
 * under saturation conditions.
 */
class RateLimiterSaturationTest {

    // ==================== RATE LIMITER SATURATION ====================

    @Test
    fun `RateLimiter handles sustained saturation without starvation`() = runBlocking<Unit> {
        val limiter = RateLimiter(maxConcurrent = 3)
        val mutex = Mutex()
        var completedCount = 0
        val totalRequests = 300

        val jobs = (1..totalRequests).map {
            launch(Dispatchers.Default) {
                limiter.execute {
                    delay(2)
                    mutex.withLock { completedCount++ }
                }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(totalRequests, completedCount, "All $totalRequests requests must complete (no starvation)")
    }

    @Test
    fun `RateLimiter throughput is bounded by concurrency limit`() = runBlocking<Unit> {
        val maxConcurrent = 5
        val limiter = RateLimiter(maxConcurrent = maxConcurrent)
        val mutex = Mutex()
        var maxObservedConcurrency = 0

        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                limiter.execute {
                    val active = limiter.getActiveCount()
                    mutex.withLock {
                        if (active > maxObservedConcurrency) maxObservedConcurrency = active
                    }
                    delay(5)
                }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(
            maxObservedConcurrency <= maxConcurrent,
            "Observed concurrency ($maxObservedConcurrency) must not exceed limit ($maxConcurrent)"
        )
    }

    @Test
    fun `RateLimiter queue drains completely after saturation`() = runBlocking<Unit> {
        val limiter = RateLimiter(maxConcurrent = 2)
        val mutex = Mutex()
        var completed = 0

        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                limiter.execute {
                    delay(1)
                    mutex.withLock { completed++ }
                }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(100, completed, "All queued operations should drain")
        assertEquals(0, limiter.getActiveCount(), "No operations should remain active")
        assertEquals(0, limiter.getQueueLength(), "Queue should be empty")
    }

    @Test
    fun `RateLimiter executeWithTimeout returns null under full saturation`() = runBlocking<Unit> {
        val limiter = RateLimiter(maxConcurrent = 1)
        val mutex = Mutex()
        var timedOutCount = 0

        // Saturate the single slot
        val blocker = launch(Dispatchers.Default) {
            limiter.execute { delay(5000) }
        }
        withContext(Dispatchers.Default) { delay(100) }

        // Fire operations with short timeout -- should mostly time out
        val jobs = (1..20).map {
            launch(Dispatchers.Default) {
                val result = limiter.executeWithTimeout(50) { "ok" }
                if (result == null) {
                    mutex.withLock { timedOutCount++ }
                }
            }
        }
        jobs.forEach { it.join() }
        blocker.cancel()

        assertTrue(timedOutCount > 0, "At least some requests should time out under saturation")
    }

    // ==================== TOKEN BUCKET SATURATION ====================

    @Test
    fun `TokenBucket rapid drain and verify depletion`() = runBlocking<Unit> {
        val capacity = 50
        val bucket = TokenBucket(capacity = capacity, refillRate = 0.01) // Very slow refill
        var acquired = 0

        // Drain all tokens
        for (i in 1..capacity + 10) {
            if (bucket.tryAcquire()) acquired++
        }

        assertEquals(capacity, acquired, "Should acquire exactly $capacity tokens")
        assertFalse(bucket.tryAcquire(), "Bucket should be depleted")
    }

    @Test
    fun `TokenBucket concurrent drain respects capacity`() = runBlocking<Unit> {
        val capacity = 30
        val bucket = TokenBucket(capacity = capacity, refillRate = 0.01) // Very slow refill
        val mutex = Mutex()
        var totalAcquired = 0

        val jobs = (1..200).map {
            launch(Dispatchers.Default) {
                if (bucket.tryAcquire()) {
                    mutex.withLock { totalAcquired++ }
                }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(
            totalAcquired <= capacity + 5, // Small tolerance for refill during execution
            "Total acquired ($totalAcquired) should not significantly exceed capacity ($capacity)"
        )
    }

    @Test
    fun `TokenBucket refill recovers from depletion`() = runBlocking<Unit> {
        val bucket = TokenBucket(capacity = 10, refillRate = 100.0) // 100 tokens/sec

        // Drain all tokens
        repeat(10) { bucket.tryAcquire() }
        assertFalse(bucket.tryAcquire(), "Should be empty after drain")

        // Wait for refill using real wall-clock time
        withContext(Dispatchers.Default) { delay(200) }

        val recovered = bucket.getAvailableTokens()
        assertTrue(recovered > 0, "Should have recovered tokens after refill delay ($recovered available)")
    }

    @Test
    fun `TokenBucket capacity is never exceeded after refill`() = runBlocking<Unit> {
        val capacity = 10
        val bucket = TokenBucket(capacity = capacity, refillRate = 1000.0) // Very fast refill

        // Wait for potential over-refill
        withContext(Dispatchers.Default) { delay(500) }

        val available = bucket.getAvailableTokens()
        assertTrue(
            available <= capacity,
            "Available tokens ($available) must never exceed capacity ($capacity)"
        )
    }

    // ==================== ADAPTIVE RATE LIMITER SATURATION ====================

    @Test
    fun `AdaptiveRateLimiter handles sustained mixed success-failure load`() = runBlocking<Unit> {
        val limiter = AdaptiveRateLimiter(initialRate = 5, minRate = 1, maxRate = 20)
        val mutex = Mutex()
        var successes = 0
        var failures = 0

        val jobs = (1..200).map { i ->
            launch(Dispatchers.Default) {
                try {
                    limiter.execute {
                        if (i % 5 == 0) throw RuntimeException("simulated-fail")
                        mutex.withLock { successes++ }
                        "ok"
                    }
                } catch (_: RuntimeException) {
                    mutex.withLock { failures++ }
                }
            }
        }
        jobs.forEach { it.join() }

        val total = successes + failures
        assertEquals(200, total, "All 200 operations must be processed")
        assertTrue(successes > 0, "At least some operations should succeed")
        assertTrue(failures > 0, "Some operations should fail (by design)")

        val rate = limiter.getCurrentRate()
        assertTrue(rate in 1..20, "Rate ($rate) should stay within bounds [1, 20]")
    }

    @Test
    fun `AdaptiveRateLimiter rate stays within bounds under extreme failure`() = runBlocking<Unit> {
        val limiter = AdaptiveRateLimiter(initialRate = 10, minRate = 2, maxRate = 15)

        // All failures
        repeat(100) {
            try {
                limiter.execute<String> { throw RuntimeException("fail") }
            } catch (_: RuntimeException) { }
        }

        val rate = limiter.getCurrentRate()
        assertTrue(rate >= 2, "Rate ($rate) must not go below minRate (2)")
    }

    @Test
    fun `AdaptiveRateLimiter rate stays within bounds under sustained success`() = runBlocking<Unit> {
        val limiter = AdaptiveRateLimiter(initialRate = 3, minRate = 1, maxRate = 10)

        // All successes
        repeat(200) {
            limiter.execute { "ok" }
        }

        val rate = limiter.getCurrentRate()
        assertTrue(rate <= 10, "Rate ($rate) must not exceed maxRate (10)")
    }

    // ==================== OPERATION THROTTLER SATURATION ====================

    @Test
    fun `OperationThrottler enforces limit under rapid fire`() = runBlocking<Unit> {
        val throttler = OperationThrottler(windowMs = 5000, maxOperations = 20)
        var allowed = 0
        var blocked = 0

        repeat(100) {
            if (throttler.tryThrottle("rapid-op")) allowed++ else blocked++
        }

        assertEquals(20, allowed, "Only 20 operations should be allowed within window")
        assertEquals(80, blocked, "80 should be blocked")
    }

    @Test
    fun `OperationThrottler concurrent access is safe`() = runBlocking<Unit> {
        val throttler = OperationThrottler(windowMs = 5000, maxOperations = 50)
        val mutex = Mutex()
        var totalAllowed = 0

        val jobs = (1..200).map {
            launch(Dispatchers.Default) {
                if (throttler.tryThrottle("concurrent-op")) {
                    mutex.withLock { totalAllowed++ }
                }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(
            totalAllowed <= 50,
            "Allowed count ($totalAllowed) must not exceed maxOperations (50)"
        )
        assertTrue(totalAllowed > 0, "At least some operations should be allowed")
    }

    @Test
    fun `OperationThrottler independent operation IDs do not interfere`() = runBlocking<Unit> {
        val throttler = OperationThrottler(windowMs = 5000, maxOperations = 10)
        var allowedA = 0
        var allowedB = 0

        repeat(20) {
            if (throttler.tryThrottle("op-a")) allowedA++
            if (throttler.tryThrottle("op-b")) allowedB++
        }

        assertEquals(10, allowedA, "op-a should allow exactly 10")
        assertEquals(10, allowedB, "op-b should allow exactly 10 (independent)")
    }
}
