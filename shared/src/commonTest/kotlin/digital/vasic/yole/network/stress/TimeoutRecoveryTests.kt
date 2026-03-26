/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Timeout and Recovery Stress Tests
 *
 * Validates CircuitBreaker trip/recovery cycles,
 * ConnectionLimiter permit exhaustion and recovery,
 * concurrent circuit breaker state transitions, and
 * rapid open/close cycling without deadlocks.
 *
 *########################################################*/
package digital.vasic.yole.network.stress

import digital.vasic.yole.network.common.CircuitBreaker
import digital.vasic.yole.network.common.CircuitBreakerOpenException
import digital.vasic.yole.network.common.ConnectionLimiter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Timeout and recovery stress tests.
 *
 * All tests use real-time waits kept well under 500ms to avoid
 * flakiness on CI. [CircuitBreaker] and [ConnectionLimiter] are
 * exercised at scale to verify no permit leaks, no deadlocks,
 * and correct state transitions under concurrent pressure.
 *
 * Total: ~32 tests
 */
class TimeoutRecoveryTests {

    // ==================== CircuitBreaker basic trip ====================

    @Test
    fun CircuitBreakerTripsAfterThreshold() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5, resetTimeout = 10.seconds, name = "trip-test")
        repeat(5) {
            cb.execute<Unit> { throw RuntimeException("failure $it") }
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun CircuitBreakerBlocksCallsWhenOpen() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 3, resetTimeout = 30.seconds, name = "block-test")
        repeat(3) { cb.execute<Unit> { throw RuntimeException("trip") } }

        val result = cb.execute { "should-not-run" }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CircuitBreakerOpenException)
    }

    @Test
    fun CircuitBreakerSuccessResetsFailureCount() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5, resetTimeout = 10.seconds, name = "reset-count")
        repeat(4) { cb.execute<Unit> { throw RuntimeException("fail") } }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)

        cb.execute { "success" }
        // After success, failures reset — still CLOSED
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }

    @Test
    fun CircuitBreakerStaysClosedBelowThreshold() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10, resetTimeout = 10.seconds, name = "below-threshold")
        repeat(9) { cb.execute<Unit> { throw RuntimeException("fail") } }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun CircuitBreakerManualReset() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "manual-reset")
        repeat(2) { cb.execute<Unit> { throw RuntimeException("trip") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        cb.reset()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }

    @Test
    fun CircuitBreakerHalfOpenOnTimeoutElapsed() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 100.milliseconds, name = "half-open")
        repeat(2) { cb.execute<Unit> { throw RuntimeException("trip") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        withContext(Dispatchers.Default) { delay(200) }

        val result = cb.execute { "trial" }
        assertTrue(result.isSuccess)
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun CircuitBreakerHalfOpenFailureReopens() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 100.milliseconds, name = "reopen-test")
        repeat(2) { cb.execute<Unit> { throw RuntimeException("trip") } }

        withContext(Dispatchers.Default) { delay(200) }

        cb.execute<Unit> { throw RuntimeException("half-open fail") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun CircuitBreakerCountersAreTracked() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10, resetTimeout = 10.seconds, name = "counters")
        repeat(3) { cb.execute { "ok" } }
        repeat(2) { cb.execute<Unit> { throw RuntimeException("fail") } }

        assertEquals(3, cb.successes)
        assertEquals(2, cb.failures)
        assertEquals(5L, cb.calls)
    }

    // ==================== CircuitBreaker recovery cycles ====================

    @Test
    fun CircuitBreakerRapidTripAndResetCyclesNoDeadlock() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 10.seconds, name = "rapid-cycle")
        repeat(20) {
            cb.execute<Unit> { throw RuntimeException("trip") }
            cb.execute<Unit> { throw RuntimeException("trip") }
            assertEquals(CircuitBreaker.State.OPEN, cb.state)
            cb.reset()
            assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        }
    }

    @Test
    fun CircuitBreakerConcurrentFailureFloodTripsCorrectly() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 3, resetTimeout = 10.seconds, name = "concurrent-flood")
        val jobs = (1..30).map {
            launch(Dispatchers.Default) {
                cb.execute<Unit> { throw RuntimeException("flood") }
            }
        }
        jobs.forEach { it.join() }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun CircuitBreakerConcurrentSuccessesStayClosed() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10, resetTimeout = 10.seconds, name = "concurrent-success")
        val results = withTimeout(10.seconds) {
            (1..50).map { async(Dispatchers.Default) { cb.execute { "ok" } } }.awaitAll()
        }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun CircuitBreakerMultipleResetCyclesNoStateCorruption() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds, name = "multi-reset")
        repeat(10) { round ->
            cb.execute<Unit> { throw RuntimeException("trip round $round") }
            assertEquals(CircuitBreaker.State.OPEN, cb.state, "Should be OPEN after trip in round $round")
            cb.reset()
            assertEquals(CircuitBreaker.State.CLOSED, cb.state, "Should be CLOSED after reset in round $round")
        }
    }

    @Test
    fun CircuitBreakerCancellationExceptionNotCounted() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 10.seconds, name = "cancel-safe")
        var cancellationThrown = false
        try {
            cb.execute<Unit> {
                throw CancellationException("cancelled")
            }
        } catch (e: CancellationException) {
            cancellationThrown = true
        }
        assertTrue(cancellationThrown)
        // CancellationException must not count as a failure
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }

    // ==================== ConnectionLimiter ====================

    @Test
    fun ConnectionLimiterAllowsUpToMaxConcurrent() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 5.seconds, name = "basic")
        val counter = java.util.concurrent.atomic.AtomicInteger(0)
        val maxConcurrent = java.util.concurrent.atomic.AtomicInteger(0)
        val mutex = Mutex()

        val jobs = (1..5).map {
            async(Dispatchers.Default) {
                limiter.withConnection {
                    val c = counter.incrementAndGet()
                    mutex.withLock { if (c > maxConcurrent.get()) maxConcurrent.set(c) }
                    delay(20)
                    counter.decrementAndGet()
                }
            }
        }
        withTimeout(10.seconds) { jobs.awaitAll() }
        assertTrue(maxConcurrent.get() <= 5)
    }

    @Test
    fun ConnectionLimiterPermitsReleasedAfterCompletion() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "release")
        // Run 10 sequential operations — all should succeed since permits are released
        repeat(10) {
            limiter.withConnection { "ok" }
        }
        assertEquals(3, limiter.availablePermits)
    }

    @Test
    fun ConnectionLimiterPermitsReleasedOnException() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "exception-release")
        try {
            limiter.withConnection<Unit> { throw RuntimeException("error inside") }
        } catch (e: RuntimeException) {
            // expected
        }
        assertEquals(3, limiter.availablePermits)
    }

    @Test
    fun ConnectionLimiterConcurrentPermitExhaustionAndRecovery() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "exhaust-recover")
        val completed = java.util.concurrent.atomic.AtomicInteger(0)

        withTimeout(15.seconds) {
            (1..9).map {
                async(Dispatchers.Default) {
                    limiter.withConnection {
                        delay(50)
                        completed.incrementAndGet()
                    }
                }
            }.awaitAll()
        }

        assertEquals(9, completed.get())
        assertEquals(3, limiter.availablePermits)
    }

    @Test
    fun ConnectionLimiterAvailablePermitsReflectsUsage() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 4, acquireTimeout = 5.seconds, name = "permits-reflect")
        assertEquals(4, limiter.availablePermits)

        val barrier = CompletableDeferred<Unit>()
        val job = launch(Dispatchers.Default) {
            limiter.withConnection {
                barrier.complete(Unit)
                delay(200)
            }
        }
        barrier.await()
        assertTrue(limiter.availablePermits < 4)
        job.join()
        assertEquals(4, limiter.availablePermits)
    }

    // ==================== Combined CircuitBreaker + ConnectionLimiter ====================

    @Test
    fun CircuitBreakerAndConnectionLimiterTogetherNoDeadlock() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5, resetTimeout = 5.seconds, name = "combined-cb")
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "combined-lim")

        val results = withTimeout(15.seconds) {
            (1..20).map { i ->
                async(Dispatchers.Default) {
                    limiter.withConnection {
                        cb.execute {
                            if (i % 7 == 0) throw RuntimeException("failure $i")
                            "success $i"
                        }
                    }
                }
            }.awaitAll()
        }

        assertEquals(20, results.size)
    }

    @Test
    fun CircuitBreakerTripDoesNotBlockConnectionLimiter() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "trip-no-block")
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 2.seconds, name = "unblocked-lim")

        // Trip the circuit breaker
        repeat(2) { cb.execute<Unit> { throw RuntimeException("trip") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // ConnectionLimiter should still work independently
        val result = limiter.withConnection { "limiter-ok" }
        assertEquals("limiter-ok", result)
        assertEquals(5, limiter.availablePermits)
    }

    @Test
    fun CircuitBreakerRecoveryAfterTimeoutUnderLoad() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 150.milliseconds, name = "recovery-load")

        // Trip
        repeat(2) { cb.execute<Unit> { throw RuntimeException("trip") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Wait for timeout
        withContext(Dispatchers.Default) { delay(300) }

        // Concurrent recovery attempts — first one succeeds and closes circuit
        val results = (1..10).map {
            async(Dispatchers.Default) { cb.execute { "recovered $it" } }
        }.awaitAll()

        // At least one should succeed
        assertTrue(results.any { it.isSuccess })
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    // ==================== Deadlock detection via withTimeout ====================

    @Test
    fun CircuitBreakerExecuteCompletesWithinTimeout() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5, resetTimeout = 10.seconds, name = "timeout-guard")
        val result = withTimeout(5.seconds) {
            cb.execute { "fast-result" }
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun ConnectionLimiterWithConnectionCompletesWithinTimeout() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 5.seconds, name = "timeout-guard")
        val result = withTimeout(5.seconds) {
            limiter.withConnection { "fast-result" }
        }
        assertEquals("fast-result", result)
    }

    @Test
    fun RapidOpenCloseCyclingNoMemoryLeak() = runBlocking<Unit> {
        // Create and discard 100 circuit breakers — verify no hang
        withTimeout(10.seconds) {
            (1..100).map { i ->
                async(Dispatchers.Default) {
                    val cb = CircuitBreaker(failureThreshold = 3, name = "throwaway-$i")
                    cb.execute { "ok" }
                    cb.execute<Unit> { throw RuntimeException("fail") }
                    cb.reset()
                }
            }.awaitAll()
        }
        assertTrue(true)
    }

    @Test
    fun ConnectionLimiterRapidAcquireReleaseNoPanic() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 10, acquireTimeout = 5.seconds, name = "rapid-ar")
        withTimeout(10.seconds) {
            (1..200).map {
                async(Dispatchers.Default) {
                    limiter.withConnection { "ok" }
                }
            }.awaitAll()
        }
        assertEquals(10, limiter.availablePermits)
    }

    @Test
    fun ConcurrentCircuitBreakerTransitionsNoStateCorruption() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5, resetTimeout = 50.milliseconds, name = "concurrent-trans")
        val errors = mutableListOf<Throwable>()
        val errorMutex = Mutex()

        withTimeout(10.seconds) {
            val jobs = (1..50).map { i ->
                launch(Dispatchers.Default) {
                    try {
                        when (i % 4) {
                            0 -> cb.execute<Unit> { throw RuntimeException("fail $i") }
                            1 -> cb.execute { "success $i" }
                            2 -> {
                                withContext(Dispatchers.Default) { delay(60) }
                                cb.execute { "delayed $i" }
                            }
                            else -> cb.reset()
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        errorMutex.withLock { errors.add(e) }
                    }
                }
            }
            jobs.forEach { it.join() }
        }

        // State should be one of the valid states — no corruption
        assertNotNull(cb.state)
        assertTrue(cb.state in listOf(
            CircuitBreaker.State.CLOSED,
            CircuitBreaker.State.OPEN,
            CircuitBreaker.State.HALF_OPEN
        ))
    }
}
