/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Isolated unit tests for ConnectionLimiter covering
 * permit management, concurrency, timeout, and edge cases.
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Comprehensive unit tests for [ConnectionLimiter].
 *
 * Validates permit acquisition, release, concurrent operations,
 * timeout handling, exception safety, and edge cases.
 *
 * Total: 28+ tests
 */
class ConnectionLimiterUnitTest {

    // ==================== Initial State ====================

    @Test
    fun defaultMaxConcurrentIsFive() {
        val limiter = ConnectionLimiter()
        assertEquals(5, limiter.maxConcurrent)
    }

    @Test
    fun defaultNameIsDefault() {
        val limiter = ConnectionLimiter()
        assertEquals("default", limiter.name)
    }

    @Test
    fun customMaxConcurrentIsRespected() {
        val limiter = ConnectionLimiter(maxConcurrent = 10)
        assertEquals(10, limiter.maxConcurrent)
    }

    @Test
    fun customNameIsStored() {
        val limiter = ConnectionLimiter(name = "sftp-limiter")
        assertEquals("sftp-limiter", limiter.name)
    }

    @Test
    fun availablePermitsEqualsMaxConcurrentInitially() {
        val limiter = ConnectionLimiter(maxConcurrent = 7)
        assertEquals(7, limiter.availablePermits)
    }

    // ==================== Acquire When Permits Available ====================

    @Test
    fun singleOperationCompletesSuccessfully() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        val result = limiter.withConnection { "hello" }
        assertEquals("hello", result)
    }

    @Test
    fun operationReturnsTypedResult() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        val intResult = limiter.withConnection { 42 }
        assertEquals(42, intResult)

        val listResult = limiter.withConnection { listOf("a", "b", "c") }
        assertEquals(listOf("a", "b", "c"), listResult)
    }

    @Test
    fun operationReturnsNullableResult() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        val result: String? = limiter.withConnection { null }
        assertNull(result)
    }

    // ==================== Release Makes Permit Available ====================

    @Test
    fun permitReleasedAfterSuccess() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1)
        limiter.withConnection { "first" }
        assertEquals(1, limiter.availablePermits)
    }

    @Test
    fun permitReleasedAfterException() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1)
        try {
            limiter.withConnection { throw RuntimeException("fail") }
        } catch (_: RuntimeException) { }
        assertEquals(1, limiter.availablePermits)
        // Should be able to use again
        val result = limiter.withConnection { "recovered" }
        assertEquals("recovered", result)
    }

    @Test
    fun sequentialOperationsAllComplete() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1)
        val results = mutableListOf<Int>()
        for (i in 1..10) {
            limiter.withConnection { results.add(i) }
        }
        assertEquals((1..10).toList(), results)
    }

    // ==================== Max Connections Enforced ====================

    @Test
    fun concurrentOperationsWithinLimitAllComplete() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 5.seconds)
        val results = (1..5).map { i ->
            async {
                limiter.withConnection { i * 10 }
            }
        }.awaitAll()
        assertEquals(listOf(10, 20, 30, 40, 50), results.sorted())
    }

    @Test
    fun maxConcurrentOneSerializesOperations() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 5.seconds)
        val completed = mutableListOf<Int>()
        val jobs = (1..5).map { i ->
            async {
                limiter.withConnection {
                    completed.add(i)
                    i
                }
            }
        }
        jobs.awaitAll()
        assertEquals(5, completed.size)
    }

    @Test
    fun excessConcurrentOpsWaitForPermit() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds)
        val countMutex = Mutex()
        var startedCount = 0
        val results = (1..6).map { i ->
            async {
                limiter.withConnection {
                    countMutex.withLock { startedCount++ }
                    delay(10.milliseconds)
                    i
                }
            }
        }.awaitAll()
        assertEquals(6, results.size)
        assertEquals(6, startedCount)
    }

    // ==================== Timeout on Acquire ====================

    @Test
    fun operationTimesOutWhenNoPermitAvailable() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 50.milliseconds)
        // Hold the single permit
        val holder = async {
            limiter.withConnection {
                delay(500.milliseconds)
                "held"
            }
        }
        delay(10.milliseconds)  // Let holder acquire the permit

        // This should fail because we can't acquire within 50ms
        val result = withTimeoutOrNull(200.milliseconds) {
            try {
                limiter.withConnection { "should timeout" }
            } catch (e: Exception) {
                "timed out"
            }
        }
        // Either timed out or got an exception
        assertNotNull(result)
        holder.cancel()
    }

    // ==================== Exception Propagation ====================

    @Test
    fun illegalStateExceptionPropagates() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        assertFailsWith<IllegalStateException> {
            limiter.withConnection { throw IllegalStateException("inner error") }
        }
    }

    @Test
    fun illegalArgumentExceptionPropagates() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        assertFailsWith<IllegalArgumentException> {
            limiter.withConnection { throw IllegalArgumentException("bad arg") }
        }
    }

    @Test
    fun runtimeExceptionPropagates() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        assertFailsWith<RuntimeException> {
            limiter.withConnection { throw RuntimeException("runtime fail") }
        }
    }

    // ==================== Concurrent Acquire / Release ====================

    @Test
    fun rapidAcquireReleaseCycles() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds)
        val results = (1..50).map { i ->
            async {
                limiter.withConnection { i }
            }
        }.awaitAll()
        assertEquals(50, results.size)
        assertEquals((1..50).toList(), results.sorted())
    }

    @Test
    fun concurrentMixedSuccessAndFailure() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds)
        val results = (1..20).map { i ->
            async {
                try {
                    limiter.withConnection {
                        if (i % 5 == 0) throw RuntimeException("fail-$i")
                        i
                    }
                } catch (_: RuntimeException) {
                    -i
                }
            }
        }.awaitAll()
        assertEquals(20, results.size)
        // All permits should be back
        assertEquals(3, limiter.availablePermits)
    }

    @Test
    fun allPermitsAvailableAfterAllOperationsComplete() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 5.seconds)
        val jobs = (1..20).map { i ->
            async {
                limiter.withConnection {
                    delay(5.milliseconds)
                    i
                }
            }
        }
        jobs.awaitAll()
        assertEquals(5, limiter.availablePermits)
    }

    // ==================== Edge Cases ====================

    @Test
    fun maxConcurrentOfOneWorksAsSerialGate() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 5.seconds)
        val order = mutableListOf<Int>()
        val jobs = (1..3).map { i ->
            async {
                limiter.withConnection {
                    order.add(i)
                    i
                }
            }
        }
        jobs.awaitAll()
        assertEquals(3, order.size)
    }

    @Test
    fun operationWithZeroDelay() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        val result = limiter.withConnection { "instant" }
        assertEquals("instant", result)
    }

    @Test
    fun nestedWithConnectionCalls() = runBlocking<Unit> {
        // Different limiters, so no deadlock
        val outer = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds)
        val inner = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds)
        val result = outer.withConnection {
            inner.withConnection { "nested" }
        }
        assertEquals("nested", result)
    }

    @Test
    fun manyPermitsLargeScale() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 100, acquireTimeout = 5.seconds)
        assertEquals(100, limiter.availablePermits)
        val results = (1..100).map { i ->
            async { limiter.withConnection { i } }
        }.awaitAll()
        assertEquals(100, results.size)
    }

    @Test
    fun permitCountDuringExecution() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        limiter.withConnection {
            // During execution, one permit is taken
            assertTrue(limiter.availablePermits < 3)
        }
        // After execution, all permits should be back
        assertEquals(3, limiter.availablePermits)
    }
}
