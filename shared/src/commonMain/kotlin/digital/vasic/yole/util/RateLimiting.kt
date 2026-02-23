/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Rate Limiting Utilities
 * Semaphore-based rate limiting for network operations
 * Kotlin Multiplatform compatible (no JVM-only imports)
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore as KSemaphore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Rate Limiter using Kotlin Coroutines Semaphore.
 *
 * Limits concurrent operations to prevent overwhelming resources.
 * Uses [kotlinx.coroutines.sync.Semaphore] for multiplatform compatibility
 * (works on JVM, iOS, Wasm, and all Kotlin targets).
 *
 * @param maxConcurrent Maximum number of concurrent operations
 *
 * @example
 * ```kotlin
 * val limiter = RateLimiter(maxConcurrent = 3)
 * val result = limiter.execute { fetchData() }
 * ```
 */
class RateLimiter(
    private val maxConcurrent: Int = 5
) {
    private val semaphore = KSemaphore(maxConcurrent)
    private val mutex = Mutex()
    private var _activeCount: Int = 0
    private var _queueLength: Int = 0

    /**
     * Execute operation with rate limiting.
     *
     * Suspends until a permit is available, then executes the operation.
     * The permit is released when the operation completes (success or failure).
     *
     * @param operation The suspend function to execute
     * @return The result of the operation
     */
    suspend fun <T> execute(operation: suspend () -> T): T {
        mutex.withLock { _queueLength++ }

        semaphore.acquire()
        mutex.withLock {
            _activeCount++
        }

        return try {
            operation()
        } finally {
            mutex.withLock {
                _activeCount--
                _queueLength--
            }
            semaphore.release()
        }
    }

    /**
     * Execute operation with timeout.
     *
     * @param timeout Timeout in milliseconds
     * @param operation The suspend function to execute
     * @return The result, or null if timed out
     */
    suspend fun <T> executeWithTimeout(
        timeout: Long,
        operation: suspend () -> T
    ): T? = withTimeoutOrNull(timeout) {
        execute(operation)
    }

    /**
     * Get number of active operations.
     */
    suspend fun getActiveCount(): Int = mutex.withLock { _activeCount }

    /**
     * Get queue length.
     */
    suspend fun getQueueLength(): Int = mutex.withLock { _queueLength }

    /**
     * Check if at capacity.
     */
    suspend fun isAtCapacity(): Boolean = mutex.withLock { _activeCount >= maxConcurrent }
}

/**
 * Token Bucket Rate Limiter.
 *
 * Allows burst traffic while maintaining average rate. Uses Kotlin Multiplatform
 * compatible time source ([kotlinx.datetime.Clock]) and [Mutex] for thread safety.
 *
 * @param capacity Maximum tokens in bucket
 * @param refillRate Tokens per second
 *
 * @example
 * ```kotlin
 * val bucket = TokenBucket(capacity = 10, refillRate = 5.0)
 * if (bucket.tryAcquire()) { /* proceed */ }
 * // or suspending:
 * bucket.acquire() // waits until token available
 * ```
 */
class TokenBucket(
    private val capacity: Int = 10,
    private val refillRate: Double = 5.0
) {
    private val mutex = Mutex()
    private var tokens: Int = capacity
    private var lastRefillMs: Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Try to acquire a token (non-suspending, mutex-protected).
     *
     * @return true if a token was acquired, false if bucket is empty
     */
    suspend fun tryAcquire(): Boolean {
        mutex.withLock {
            refillLocked()
            if (tokens <= 0) return false
            tokens--
            return true
        }
    }

    /**
     * Acquire token, suspending until one becomes available.
     */
    suspend fun acquire() {
        while (!tryAcquire()) {
            delay(50)
        }
    }

    /**
     * Refill tokens based on time elapsed. Must be called under mutex lock.
     */
    private fun refillLocked() {
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = now - lastRefillMs

        if (elapsed > 0) {
            val tokensToAdd = (elapsed * refillRate / 1000).toInt()
            if (tokensToAdd > 0) {
                tokens = minOf(capacity, tokens + tokensToAdd)
                lastRefillMs = now
            }
        }
    }

    /**
     * Get available tokens.
     */
    suspend fun getAvailableTokens(): Int {
        mutex.withLock {
            refillLocked()
            return tokens
        }
    }
}

/**
 * Adaptive Rate Limiter.
 *
 * Adjusts rate based on success/failure feedback. After sustained success,
 * the concurrency limit increases. After repeated failures, it decreases.
 *
 * @param initialRate Initial maximum concurrent operations
 * @param minRate Minimum concurrent operations (floor)
 * @param maxRate Maximum concurrent operations (ceiling)
 *
 * @example
 * ```kotlin
 * val limiter = AdaptiveRateLimiter(initialRate = 5)
 * val result = limiter.execute { callApi() }
 * println(limiter.getCurrentRate()) // may have adjusted
 * ```
 */
class AdaptiveRateLimiter(
    initialRate: Int = 5,
    private val minRate: Int = 1,
    private val maxRate: Int = 20
) {
    private val mutex = Mutex()
    private var currentRate: Int = initialRate
    private val baseLimiter = RateLimiter(initialRate)
    private var successCount: Int = 0
    private var failureCount: Int = 0

    /**
     * Execute with adaptive rate limiting.
     */
    suspend fun <T> execute(operation: suspend () -> T): T {
        return baseLimiter.execute {
            try {
                val result = operation()
                onSuccess()
                result
            } catch (e: Exception) {
                onFailure()
                throw e
            }
        }
    }

    private suspend fun onSuccess() {
        mutex.withLock {
            successCount++
            if (successCount > 10) {
                adjustRateLocked(1)
                successCount = 0
            }
        }
    }

    private suspend fun onFailure() {
        mutex.withLock {
            failureCount++
            if (failureCount >= 3) {
                adjustRateLocked(-1)
                failureCount = 0
            }
        }
    }

    private fun adjustRateLocked(delta: Int) {
        currentRate = (currentRate + delta).coerceIn(minRate, maxRate)
    }

    /**
     * Get current rate.
     */
    suspend fun getCurrentRate(): Int = mutex.withLock { currentRate }
}

/**
 * Operation Throttler.
 *
 * Groups similar operations by ID and prevents flooding within a time window.
 * Thread-safe via [Mutex] for multiplatform compatibility.
 *
 * @param windowMs Time window in milliseconds
 * @param maxOperations Maximum operations per window
 *
 * @example
 * ```kotlin
 * val throttler = OperationThrottler(windowMs = 1000, maxOperations = 10)
 * if (throttler.tryThrottle("api-call")) { /* proceed */ } else { /* throttled */ }
 * ```
 */
class OperationThrottler(
    private val windowMs: Long = 1000,
    private val maxOperations: Int = 10
) {
    private val mutex = Mutex()
    private val operations = mutableMapOf<String, Long>()
    private val counts = mutableMapOf<String, Int>()

    /**
     * Try to throttle operation. Returns true if operation is allowed, false if throttled.
     */
    suspend fun tryThrottle(operationId: String): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        mutex.withLock {
            val lastOp = operations.getOrPut(operationId) { now }

            if (now - lastOp < windowMs) {
                val count = counts.getOrPut(operationId) { 0 }
                if (count + 1 > maxOperations) {
                    return false
                }
                counts[operationId] = count + 1
            } else {
                operations[operationId] = now
                counts[operationId] = 1
            }

            return true
        }
    }

    /**
     * Clear throttling state for an operation.
     */
    suspend fun clear(operationId: String) {
        mutex.withLock {
            operations.remove(operationId)
            counts.remove(operationId)
        }
    }
}
