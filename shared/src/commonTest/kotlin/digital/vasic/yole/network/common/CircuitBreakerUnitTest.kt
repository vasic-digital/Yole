/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Isolated unit tests for CircuitBreaker covering all
 * state transitions, failure counting, reset behavior,
 * concurrent access, and edge cases.
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive unit tests for [CircuitBreaker].
 *
 * Validates state machine transitions, failure counting,
 * success resets, timeout-based recovery, concurrent safety,
 * and edge cases.
 *
 * Total: 40+ tests
 */
class CircuitBreakerUnitTest {

    // ==================== Initial State ====================

    @Test
    fun initialStateIsClosed() {
        val cb = CircuitBreaker(name = "unit-test")
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun initialFailureCountIsZero() {
        val cb = CircuitBreaker()
        assertEquals(0, cb.failures)
    }

    @Test
    fun initialSuccessCountIsZero() {
        val cb = CircuitBreaker()
        assertEquals(0, cb.successes)
    }

    @Test
    fun initialCallCountIsZero() {
        val cb = CircuitBreaker()
        assertEquals(0L, cb.calls)
    }

    @Test
    fun defaultNameIsDefault() {
        val cb = CircuitBreaker()
        assertEquals("default", cb.name)
    }

    @Test
    fun customNameIsStored() {
        val cb = CircuitBreaker(name = "ftp-breaker")
        assertEquals("ftp-breaker", cb.name)
    }

    // ==================== CLOSED -> OPEN Transition ====================

    @Test
    fun singleFailureDoesNotOpenCircuit() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 3)
        cb.execute { throw RuntimeException("fail") }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(1, cb.failures)
    }

    @Test
    fun failuresBelowThresholdStayClosed() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5)
        repeat(4) { cb.execute { throw RuntimeException("fail-$it") } }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(4, cb.failures)
    }

    @Test
    fun failuresAtThresholdOpenCircuit() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 3, resetTimeout = 60.seconds)
        repeat(3) { cb.execute { throw RuntimeException("fail-$it") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
        assertEquals(3, cb.failures)
    }

    @Test
    fun failureThresholdOfOneOpensImmediately() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("boom") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun failuresAboveThresholdRemainOpen() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds)
        repeat(2) { cb.execute { throw RuntimeException("fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
        // Additional call should be rejected but state stays OPEN
        val result = cb.execute { "nope" }
        assertTrue(result.isFailure)
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    // ==================== OPEN Behavior ====================

    @Test
    fun openCircuitRejectsCallsWithCorrectException() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds, name = "test-cb")
        cb.execute { throw RuntimeException("trigger") }

        val result = cb.execute { "should not run" }
        assertTrue(result.isFailure)
        assertIs<CircuitBreakerOpenException>(result.exceptionOrNull())
        assertTrue(result.exceptionOrNull()!!.message!!.contains("test-cb"))
    }

    @Test
    fun openCircuitStillIncrementsTotalCalls() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("open") }
        val callsAfterOpen = cb.calls
        cb.execute { "rejected" }
        assertEquals(callsAfterOpen + 1, cb.calls)
    }

    @Test
    fun openCircuitDoesNotIncrementFailureCount() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("trigger") }
        val failuresAfterOpen = cb.failures
        cb.execute { "rejected" }
        // Rejection is not a failure of the block itself
        assertEquals(failuresAfterOpen, cb.failures)
    }

    // ==================== OPEN -> HALF_OPEN Transition ====================

    @Test
    fun openCircuitTransitionsToHalfOpenAfterTimeout() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 1.milliseconds)
        cb.execute { throw RuntimeException("open") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        delay(10.milliseconds)
        // Next execute should transition to HALF_OPEN and try the block
        val result = cb.execute { "trial" }
        assertTrue(result.isSuccess)
        // After success in HALF_OPEN, should be CLOSED
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun openCircuitStaysOpenIfTimeoutNotElapsed() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("open") }
        // Immediately try again (60 seconds hasn't passed)
        val result = cb.execute { "should be rejected" }
        assertTrue(result.isFailure)
        assertIs<CircuitBreakerOpenException>(result.exceptionOrNull())
    }

    // ==================== HALF_OPEN -> CLOSED Transition ====================

    @Test
    fun halfOpenSuccessTransitionsToClosed() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 1.milliseconds)
        cb.execute { throw RuntimeException("open") }
        delay(10.milliseconds)

        val result = cb.execute { "recovered" }
        assertTrue(result.isSuccess)
        assertEquals("recovered", result.getOrNull())
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }

    @Test
    fun halfOpenSuccessResetsFailureCount() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 2, resetTimeout = 1.milliseconds)
        cb.execute { throw RuntimeException("f1") }
        cb.execute { throw RuntimeException("f2") }
        assertEquals(2, cb.failures)
        delay(10.milliseconds)

        cb.execute { "ok" }
        assertEquals(0, cb.failures)
    }

    // ==================== HALF_OPEN -> OPEN Transition ====================

    @Test
    fun halfOpenFailureTransitionsBackToOpen() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 1.milliseconds)
        cb.execute { throw RuntimeException("open") }
        delay(10.milliseconds)

        val result = cb.execute { throw RuntimeException("still broken") }
        assertTrue(result.isFailure)
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun halfOpenFailureKeepsHighFailureCount() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 1.milliseconds)
        cb.execute { throw RuntimeException("open") }
        delay(10.milliseconds)

        cb.execute { throw RuntimeException("still broken") }
        // Failure count includes the new failure
        assertTrue(cb.failures >= 1)
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    // ==================== Success Resets Failure Count ====================

    @Test
    fun successInClosedStateResetsFailureCount() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5)
        cb.execute { throw RuntimeException("f1") }
        cb.execute { throw RuntimeException("f2") }
        assertEquals(2, cb.failures)

        cb.execute { "success" }
        assertEquals(0, cb.failures)
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun successCountIncrementsOnEachSuccess() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10)
        cb.execute { "a" }
        cb.execute { "b" }
        cb.execute { "c" }
        assertEquals(3, cb.successes)
    }

    @Test
    fun successCountNotIncrementedOnFailure() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10)
        cb.execute { "ok" }
        cb.execute { throw RuntimeException("fail") }
        assertEquals(1, cb.successes)
    }

    // ==================== Execute Passes Through When Closed ====================

    @Test
    fun closedCircuitExecutesBlock() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5)
        val result = cb.execute { 42 }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun closedCircuitPassesThroughTypedResults() = runBlocking<Unit> {
        val cb = CircuitBreaker()
        val listResult = cb.execute { listOf(1, 2, 3) }
        assertEquals(listOf(1, 2, 3), listResult.getOrNull())

        val mapResult = cb.execute { mapOf("k" to "v") }
        assertEquals(mapOf("k" to "v"), mapResult.getOrNull())
    }

    @Test
    fun closedCircuitPropagatesExceptionAsFailure() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10)
        val result = cb.execute { throw IllegalArgumentException("bad arg") }
        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertEquals("bad arg", result.exceptionOrNull()!!.message)
    }

    // ==================== Reset ====================

    @Test
    fun resetBringsCircuitToClosedFromOpen() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("open it") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        cb.reset()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }

    @Test
    fun resetFromClosedIsNoOp() = runBlocking<Unit> {
        val cb = CircuitBreaker()
        cb.reset()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun resetAllowsExecutionAfterOpen() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("open") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        cb.reset()
        val result = cb.execute { "after reset" }
        assertTrue(result.isSuccess)
        assertEquals("after reset", result.getOrNull())
    }

    // ==================== Call Counter ====================

    @Test
    fun callCounterIncrementsOnSuccess() = runBlocking<Unit> {
        val cb = CircuitBreaker()
        cb.execute { "a" }
        cb.execute { "b" }
        assertEquals(2L, cb.calls)
    }

    @Test
    fun callCounterIncrementsOnFailure() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10)
        cb.execute { throw RuntimeException("fail") }
        assertEquals(1L, cb.calls)
    }

    @Test
    fun callCounterIncrementsOnRejection() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("open") }
        cb.execute { "rejected" }
        cb.execute { "rejected again" }
        assertEquals(3L, cb.calls)
    }

    // ==================== Concurrent Access ====================

    @Test
    fun concurrentSuccessfulCallsAllComplete() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100)
        val results = (1..30).map { i ->
            async { cb.execute { i * 2 } }
        }.awaitAll()

        assertEquals(30, results.size)
        assertTrue(results.all { it.isSuccess })
        assertEquals(30L, cb.calls)
        assertEquals(30, cb.successes)
    }

    @Test
    fun concurrentMixedCallsDoNotCorruptState() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100)
        val results = (1..20).map { i ->
            async {
                if (i % 4 == 0) {
                    cb.execute { throw RuntimeException("fail-$i") }
                } else {
                    cb.execute { "ok-$i" }
                }
            }
        }.awaitAll()

        assertEquals(20, results.size)
        assertEquals(20L, cb.calls)
        // State should still be CLOSED since threshold is 100
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun concurrentResetsDoNotCorruptState() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("open") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        val jobs = (1..10).map {
            async { cb.reset() }
        }
        jobs.awaitAll()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }

    // ==================== CancellationException Rethrow ====================

    @Test
    fun cancellationExceptionIsRethrown() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5)
        assertFailsWith<kotlin.coroutines.cancellation.CancellationException> {
            cb.execute { throw kotlin.coroutines.cancellation.CancellationException("cancelled") }
        }
        // CancellationException should not count as a failure
        assertEquals(0, cb.failures)
    }

    // ==================== Edge Cases ====================

    @Test
    fun executeWithNullResult() = runBlocking<Unit> {
        val cb = CircuitBreaker()
        val result = cb.execute<String?> { null }
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun largeFailureThreshold() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = Int.MAX_VALUE)
        repeat(100) { cb.execute { throw RuntimeException("fail") } }
        // Should still be closed since threshold is Int.MAX_VALUE
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun circuitBreakerOpenExceptionMessageFormat() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 30.seconds, name = "test-svc")
        cb.execute { throw RuntimeException("trigger") }
        val result = cb.execute { "rejected" }
        val ex = result.exceptionOrNull() as CircuitBreakerOpenException
        assertTrue(ex.message!!.contains("test-svc"))
        assertTrue(ex.message!!.contains("OPEN"))
    }

    @Test
    fun multipleRecoveryCycles() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 1.milliseconds)

        // Cycle 1: fail -> open -> recover
        cb.execute { throw RuntimeException("fail1") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
        delay(10.milliseconds)
        cb.execute { "recover1" }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)

        // Cycle 2: fail -> open -> recover
        cb.execute { throw RuntimeException("fail2") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
        delay(10.milliseconds)
        cb.execute { "recover2" }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)

        // Cycle 3: fail -> open -> fail again
        cb.execute { throw RuntimeException("fail3") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
        delay(10.milliseconds)
        cb.execute { throw RuntimeException("still broken") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun successAfterPartialFailuresSetsCountToZero() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10)
        repeat(7) { cb.execute { throw RuntimeException("f") } }
        assertEquals(7, cb.failures)

        cb.execute { "success" }
        assertEquals(0, cb.failures)
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun exactlyAtThresholdOpensCircuit() = runBlocking<Unit> {
        val threshold = 5
        val cb = CircuitBreaker(failureThreshold = threshold, resetTimeout = 60.seconds)
        repeat(threshold - 1) { cb.execute { throw RuntimeException("f-$it") } }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)

        cb.execute { throw RuntimeException("final-fail") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }
}
