/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Rate Limiting Utilities
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {

    @Test
    fun testExecuteReturnsResult() = runTest {
        val limiter = RateLimiter(maxConcurrent = 3)
        val result = limiter.execute { 42 }
        assertEquals(42, result)
    }

    @Test
    fun testExecuteWithTimeout() = runTest {
        val limiter = RateLimiter(maxConcurrent = 1)

        // Fast operation should succeed
        val result = limiter.executeWithTimeout(1000) { "ok" }
        assertEquals("ok", result)
    }

    @Test
    fun testActiveCountTracking() = runTest {
        val limiter = RateLimiter(maxConcurrent = 5)
        assertEquals(0, limiter.getActiveCount())
        assertEquals(0, limiter.getQueueLength())
    }

    @Test
    fun testIsAtCapacity() = runTest {
        val limiter = RateLimiter(maxConcurrent = 1)
        assertFalse(limiter.isAtCapacity())
    }

    @Test
    fun testConcurrentExecutionsAreThrottled() = runTest {
        val limiter = RateLimiter(maxConcurrent = 2)
        var maxActive = 0
        val mutex = kotlinx.coroutines.sync.Mutex()

        val jobs = (1..10).map {
            async(Dispatchers.Default) {
                limiter.execute {
                    val active = limiter.getActiveCount()
                    mutex.withLock { if (active > maxActive) maxActive = active }
                    delay(20)
                }
            }
        }
        jobs.awaitAll()

        assertTrue(maxActive <= 2, "Max active was $maxActive, expected <= 2")
    }
}

class TokenBucketTest {

    @Test
    fun testInitialTokensAvailable() = runTest {
        val bucket = TokenBucket(capacity = 10, refillRate = 5.0)
        val available = bucket.getAvailableTokens()
        assertEquals(10, available)
    }

    @Test
    fun testTryAcquireConsumesToken() = runTest {
        val bucket = TokenBucket(capacity = 5, refillRate = 1.0)
        assertTrue(bucket.tryAcquire())
        val remaining = bucket.getAvailableTokens()
        assertEquals(4, remaining)
    }

    @Test
    fun testTryAcquireFailsWhenEmpty() = runTest {
        val bucket = TokenBucket(capacity = 1, refillRate = 0.001)
        assertTrue(bucket.tryAcquire())
        assertFalse(bucket.tryAcquire())
    }

    @Test
    fun testAcquireSucceedsWhenTokensAvailable() = runTest {
        val bucket = TokenBucket(capacity = 5, refillRate = 1.0)
        // Tokens are available, acquire should succeed immediately
        withTimeout(1000) {
            bucket.acquire()
        }
        assertEquals(4, bucket.getAvailableTokens())
    }
}

class AdaptiveRateLimiterTest {

    @Test
    fun testExecuteSuccessfully() = runTest {
        val limiter = AdaptiveRateLimiter(initialRate = 3)
        val result = limiter.execute { "success" }
        assertEquals("success", result)
    }

    @Test
    fun testGetCurrentRate() = runTest {
        val limiter = AdaptiveRateLimiter(initialRate = 5, minRate = 1, maxRate = 20)
        assertEquals(5, limiter.getCurrentRate())
    }

    @Test
    fun testRateDoesNotExceedBounds() = runTest {
        val limiter = AdaptiveRateLimiter(initialRate = 5, minRate = 2, maxRate = 8)
        // Execute many successful operations
        repeat(50) {
            limiter.execute { Unit }
        }
        val rate = limiter.getCurrentRate()
        assertTrue(rate in 2..8, "Rate $rate should be between 2 and 8")
    }
}

class OperationThrottlerTest {

    @Test
    fun testFirstOperationAllowed() = runTest {
        val throttler = OperationThrottler(windowMs = 1000, maxOperations = 5)
        assertTrue(throttler.tryThrottle("op1"))
    }

    @Test
    fun testThrottlesAfterMaxOperations() = runTest {
        val throttler = OperationThrottler(windowMs = 60000, maxOperations = 3)
        assertTrue(throttler.tryThrottle("op1"))
        assertTrue(throttler.tryThrottle("op1"))
        assertTrue(throttler.tryThrottle("op1"))
        assertFalse(throttler.tryThrottle("op1"))
    }

    @Test
    fun testDifferentOperationIdsIndependent() = runTest {
        val throttler = OperationThrottler(windowMs = 60000, maxOperations = 1)
        assertTrue(throttler.tryThrottle("op1"))
        assertTrue(throttler.tryThrottle("op2"))
        assertFalse(throttler.tryThrottle("op1"))
    }

    @Test
    fun testClearResetsThrottle() = runTest {
        val throttler = OperationThrottler(windowMs = 60000, maxOperations = 1)
        assertTrue(throttler.tryThrottle("op1"))
        assertFalse(throttler.tryThrottle("op1"))
        throttler.clear("op1")
        assertTrue(throttler.tryThrottle("op1"))
    }
}
