/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Rate Limiting Utilities
 * Semaphore-based rate limiting for network operations
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.*

/**
 * Rate Limiter using Semaphore
 * Limits concurrent operations to prevent overwhelming resources
 *
 * @param maxConcurrent Maximum number of concurrent operations
 * @param scope Coroutine scope for operations
 */
class RateLimiter(
    private val maxConcurrent: Int = 5,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val semaphore = Semaphore(maxConcurrent)
    private val activeCount = AtomicInteger(0)
    private val queueLength = AtomicInteger(0)
    
    /**
     * Execute operation with rate limiting
     */
    suspend fun <T> execute(operation: suspend () -> T): T {
        queueLength.incrementAndGet()
        
        semaphore.acquire()
        activeCount.incrementAndGet()
        
        return try {
            withContext(Dispatchers.IO) {
                operation()
            }
        } finally {
            activeCount.decrementAndGet()
            semaphore.release()
            queueLength.decrementAndGet()
        }
    }
    
    /**
     * Execute operation with timeout
     */
    suspend fun <T> executeWithTimeout(
        timeout: Long,
        operation: suspend () -> T
    ): T? = withTimeoutOrNull(timeout) {
        execute(operation)
    }
    
    /**
     * Get number of active operations
     */
    fun getActiveCount(): Int = activeCount.get()
    
    /**
     * Get queue length
     */
    fun getQueueLength(): Int = queueLength.get()
    
    /**
     * Check if at capacity
     */
    fun isAtCapacity(): Boolean = activeCount.get() >= maxConcurrent
}

/**
 * Token Bucket Rate Limiter
 * Allows burst traffic while maintaining average rate
 *
 * @param capacity Maximum tokens in bucket
 * @param refillRate Tokens per second
 */
class TokenBucket(
    private val capacity: Int = 10,
    private val refillRate: Double = 5.0
) {
    private val tokens = AtomicInteger(capacity)
    private val lastRefill = AtomicLong(System.currentTimeMillis())
    private val lock = java.util.concurrent.locks.ReentrantLock()
    
    /**
     * Try to acquire a token
     */
    fun tryAcquire(): Boolean {
        refill()
        
        while (true) {
            val current = tokens.get()
            if (current <= 0) return false
            
            if (tokens.compareAndSet(current, current - 1)) {
                return true
            }
        }
    }
    
    /**
     * Acquire token or wait
     */
    suspend fun acquire() {
        while (!tryAcquire()) {
            delay(50)
        }
    }
    
    /**
     * Refill tokens based on time elapsed
     */
    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefill.get()
        
        if (elapsed > 100) {
            lock.lock()
            try {
                val tokensToAdd = (elapsed * refillRate / 1000).toInt()
                val newTokens = minOf(capacity, tokens.get() + tokensToAdd)
                tokens.set(newTokens)
                lastRefill.set(now)
            } finally {
                lock.unlock()
            }
        }
    }
    
    /**
     * Get available tokens
     */
    fun getAvailableTokens(): Int {
        refill()
        return tokens.get()
    }
}

/**
 * Adaptive Rate Limiter
 * Adjusts rate based on success/failure
 */
class AdaptiveRateLimiter(
    initialRate: Int = 5,
    private val minRate: Int = 1,
    private val maxRate: Int = 20
) {
    private val currentRate = AtomicInteger(initialRate)
    private val baseLimiter = RateLimiter(initialRate)
    private val successCount = AtomicInteger(0)
    private val failureCount = AtomicInteger(0)
    
    /**
     * Execute with adaptive rate limiting
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
    
    /**
     * Handle successful operation
     */
    private fun onSuccess() {
        successCount.incrementAndGet()
        
        if (successCount.get() > 10) {
            adjustRate(1)
            successCount.set(0)
        }
    }
    
    /**
     * Handle failed operation
     */
    private fun onFailure() {
        failureCount.incrementAndGet()
        
        if (failureCount.get() >= 3) {
            adjustRate(-1)
            failureCount.set(0)
        }
    }
    
    /**
     * Adjust rate up or down
     */
    private fun adjustRate(delta: Int) {
        val newRate = (currentRate.get() + delta).coerceIn(minRate, maxRate)
        currentRate.set(newRate)
    }
    
    /**
     * Get current rate
     */
    fun getCurrentRate(): Int = currentRate.get()
}

/**
 * Operation Throttler
 * Groups similar operations to prevent flooding
 */
class OperationThrottler(
    private val windowMs: Long = 1000,
    private val maxOperations: Int = 10
) {
    private val operations = ConcurrentHashMap<String, Long>()
    private val counts = ConcurrentHashMap<String, AtomicInteger>()
    
    /**
     * Try to throttle operation
     */
    fun tryThrottle(operationId: String): Boolean {
        val now = System.currentTimeMillis()
        val lastOp = operations.getOrPut(operationId) { now }
        
        if (now - lastOp < windowMs) {
            val count = counts.getOrPut(operationId) { AtomicInteger(0) }
            if (count.incrementAndGet() > maxOperations) {
                return false
            }
        } else {
            operations[operationId] = now
            counts[operationId]?.set(1)
        }
        
        return true
    }
    
    /**
     * Clear throttling state
     */
    fun clear(operationId: String) {
        operations.remove(operationId)
        counts.remove(operationId)
    }
}
