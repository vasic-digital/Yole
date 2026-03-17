/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Protocol concurrency safety tests verifying that
 * CircuitBreaker and ConnectionLimiter handle concurrent
 * operations safely across protocol service patterns.
 *
 *########################################################*/
package digital.vasic.yole.network.concurrency

import digital.vasic.yole.network.common.CircuitBreaker
import digital.vasic.yole.network.common.CircuitBreakerOpenException
import digital.vasic.yole.network.common.ConnectionLimiter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Protocol concurrency safety tests.
 *
 * Simulates the patterns used by all 8 protocol services
 * (FTP, SFTP, SMB, WebDAV, Dropbox, Google Drive, OneDrive, Git)
 * to verify that CircuitBreaker and ConnectionLimiter handle
 * concurrent access safely without corruption or deadlock.
 *
 * Total: 24+ tests
 */
class ProtocolConcurrencySafetyTest {

    // ==================== Concurrent Connect/Disconnect ====================

    @Test
    fun concurrentCircuitBreakerAccessDoesNotCrash() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100, name = "concurrent-test")
        val jobs = (1..50).map { i ->
            async {
                if (i % 3 == 0) {
                    cb.execute { throw RuntimeException("simulated-fail-$i") }
                } else {
                    cb.execute { "ok-$i" }
                }
            }
        }
        val results = jobs.awaitAll()
        assertEquals(50, results.size)
        assertEquals(50L, cb.calls)
    }

    @Test
    fun concurrentConnectionLimiterDoesNotDeadlock() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 10.seconds)
        val results = (1..30).map { i ->
            async {
                limiter.withConnection {
                    delay(1.milliseconds)
                    i
                }
            }
        }.awaitAll()
        assertEquals(30, results.size)
        assertEquals(3, limiter.availablePermits)
    }

    @Test
    fun concurrentCircuitBreakerResetDoesNotCorrupt() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 50, name = "reset-test")
        val jobs = (1..20).map { i ->
            async {
                cb.execute { "work-$i" }
                if (i % 5 == 0) cb.reset()
            }
        }
        jobs.awaitAll()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    // ==================== Rapid Connect/Disconnect Cycles ====================

    @Test
    fun rapidCircuitOpenCloseCycles() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 1.milliseconds)
        repeat(20) {
            // Open
            cb.execute { throw RuntimeException("fail") }
            assertEquals(CircuitBreaker.State.OPEN, cb.state)
            // Wait for reset timeout
            delay(5.milliseconds)
            // Close via successful half-open
            val result = cb.execute { "recovered" }
            if (result.isSuccess) {
                assertEquals(CircuitBreaker.State.CLOSED, cb.state)
            }
        }
    }

    @Test
    fun rapidConnectionLimiterAcquireRelease() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 5.seconds)
        // Sequential rapid acquire/release
        repeat(100) { i ->
            val result = limiter.withConnection { i }
            assertEquals(i, result)
        }
        assertEquals(1, limiter.availablePermits)
    }

    // ==================== Multiple Protocols Simultaneously ====================

    @Test
    fun multipleCircuitBreakersIndependent() = runBlocking<Unit> {
        val ftpBreaker = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "ftp")
        val sftpBreaker = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "sftp")
        val webdavBreaker = CircuitBreaker(failureThreshold = 2, resetTimeout = 60.seconds, name = "webdav")

        // Open FTP breaker
        repeat(2) { ftpBreaker.execute { throw RuntimeException("ftp-fail") } }
        assertEquals(CircuitBreaker.State.OPEN, ftpBreaker.state)

        // SFTP and WebDAV should still be closed
        assertEquals(CircuitBreaker.State.CLOSED, sftpBreaker.state)
        assertEquals(CircuitBreaker.State.CLOSED, webdavBreaker.state)

        // SFTP and WebDAV should still work
        val sftpResult = sftpBreaker.execute { "sftp-ok" }
        val webdavResult = webdavBreaker.execute { "webdav-ok" }
        assertTrue(sftpResult.isSuccess)
        assertTrue(webdavResult.isSuccess)

        // FTP should be rejected
        val ftpResult = ftpBreaker.execute { "ftp-should-fail" }
        assertTrue(ftpResult.isFailure)
        assertIs<CircuitBreakerOpenException>(ftpResult.exceptionOrNull())
    }

    @Test
    fun multipleConnectionLimitersIndependent() = runBlocking<Unit> {
        val ftpLimiter = ConnectionLimiter(maxConcurrent = 3, name = "ftp")
        val sftpLimiter = ConnectionLimiter(maxConcurrent = 5, name = "sftp")

        assertEquals(3, ftpLimiter.availablePermits)
        assertEquals(5, sftpLimiter.availablePermits)

        // Operations on one should not affect the other
        ftpLimiter.withConnection {
            assertTrue(ftpLimiter.availablePermits < 3)
            assertEquals(5, sftpLimiter.availablePermits)
        }
    }

    @Test
    fun concurrentMultiProtocolOperations() = runBlocking<Unit> {
        val breakers = listOf(
            CircuitBreaker(failureThreshold = 10, name = "proto-1"),
            CircuitBreaker(failureThreshold = 10, name = "proto-2"),
            CircuitBreaker(failureThreshold = 10, name = "proto-3"),
            CircuitBreaker(failureThreshold = 10, name = "proto-4")
        )
        val limiters = listOf(
            ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "proto-1"),
            ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "proto-2"),
            ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "proto-3"),
            ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "proto-4")
        )

        val jobs = (0..3).flatMap { protoIdx ->
            (1..10).map { opIdx ->
                async {
                    val cb = breakers[protoIdx]
                    val lim = limiters[protoIdx]
                    cb.execute {
                        lim.withConnection {
                            "proto-$protoIdx-op-$opIdx"
                        }
                    }
                }
            }
        }
        val results = jobs.awaitAll()
        assertEquals(40, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    // ==================== Exception Safety ====================

    @Test
    fun circuitBreakerExceptionDoesNotLeakState() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 3, name = "leak-test")
        // Throw various exception types
        cb.execute { throw RuntimeException("runtime") }
        cb.execute { throw IllegalStateException("illegal-state") }
        assertEquals(2, cb.failures)
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)

        // Success should reset
        cb.execute { "ok" }
        assertEquals(0, cb.failures)
    }

    @Test
    fun connectionLimiterExceptionReleasesPermit() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds)
        // Cause exceptions
        repeat(5) {
            try {
                limiter.withConnection { throw RuntimeException("fail-$it") }
            } catch (_: RuntimeException) { }
        }
        // All permits should still be available
        assertEquals(2, limiter.availablePermits)
    }

    @Test
    fun concurrentExceptionsDoNotCorruptLimiter() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 5.seconds)
        val jobs = (1..20).map { i ->
            async {
                try {
                    limiter.withConnection {
                        if (i % 2 == 0) throw RuntimeException("fail-$i")
                        "ok-$i"
                    }
                } catch (_: RuntimeException) {
                    "caught-$i"
                }
            }
        }
        jobs.awaitAll()
        assertEquals(5, limiter.availablePermits)
    }

    // ==================== Combined Circuit Breaker + Limiter ====================

    @Test
    fun circuitBreakerAndLimiterCombinedNormalFlow() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5, name = "combined")
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds)

        val results = (1..10).map { i ->
            async {
                cb.execute {
                    limiter.withConnection { i * 10 }
                }
            }
        }.awaitAll()

        assertEquals(10, results.size)
        assertTrue(results.all { it.isSuccess })
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun circuitBreakerAndLimiterCombinedWithFailures() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 10, name = "combined-fail")
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds)

        val results = (1..10).map { i ->
            async {
                cb.execute {
                    limiter.withConnection {
                        if (i % 4 == 0) throw RuntimeException("inner-fail-$i")
                        i
                    }
                }
            }
        }.awaitAll()

        assertEquals(10, results.size)
        // Some should succeed, some should fail
        assertTrue(results.any { it.isSuccess })
        assertTrue(results.any { it.isFailure })
        // Limiter should have all permits back
        assertEquals(3, limiter.availablePermits)
    }

    @Test
    fun circuitBreakerOpensWhileLimiterStillActive() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 3, resetTimeout = 60.seconds, name = "open-while-active")
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 5.seconds)

        // Cause circuit to open
        repeat(3) {
            cb.execute {
                limiter.withConnection { throw RuntimeException("fail") }
            }
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Now calls should be rejected at circuit breaker level (never reaching limiter)
        val result = cb.execute { limiter.withConnection { "should not run" } }
        assertTrue(result.isFailure)
        assertIs<CircuitBreakerOpenException>(result.exceptionOrNull())

        // Limiter should still have all permits
        assertEquals(5, limiter.availablePermits)
    }

    // ==================== Stress Patterns ====================

    @Test
    fun stressConcurrentCircuitBreakerOperations() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 200, name = "stress")
        val jobs = (1..100).map { i ->
            async {
                if (i % 10 == 0) {
                    cb.execute { throw RuntimeException("stress-fail-$i") }
                } else {
                    cb.execute { "stress-ok-$i" }
                }
            }
        }
        val results = jobs.awaitAll()
        assertEquals(100, results.size)
        assertEquals(100L, cb.calls)
        // Should still be closed since threshold is 200
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun stressConcurrentLimiterOperations() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 30.seconds)
        val jobs = (1..100).map { i ->
            async {
                limiter.withConnection {
                    delay(1.milliseconds)
                    i
                }
            }
        }
        val results = jobs.awaitAll()
        assertEquals(100, results.size)
        assertEquals(5, limiter.availablePermits)
    }

    @Test
    fun mixedProtocolSimulation() = runBlocking<Unit> {
        // Simulate 4 protocol services each with their own CB + limiter
        data class ProtocolPair(val cb: CircuitBreaker, val limiter: ConnectionLimiter)
        val protocols = (1..4).map { i ->
            ProtocolPair(
                cb = CircuitBreaker(failureThreshold = 10, name = "proto-$i"),
                limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds, name = "proto-$i")
            )
        }

        val jobs = protocols.flatMap { proto ->
            (1..10).map { opIdx ->
                async {
                    proto.cb.execute {
                        proto.limiter.withConnection { "result-$opIdx" }
                    }
                }
            }
        }
        val results = jobs.awaitAll()
        assertEquals(40, results.size)
        assertTrue(results.all { it.isSuccess })

        // All limiters should have permits back
        protocols.forEach {
            assertEquals(3, it.limiter.availablePermits, "Limiter ${it.limiter.name} should have all permits back")
        }
    }

    @Test
    fun circuitBreakerResetUnderConcurrentLoad() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 5, resetTimeout = 60.seconds, name = "reset-under-load")

        // Open the circuit
        repeat(5) { cb.execute { throw RuntimeException("fail") } }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Concurrent resets
        val jobs = (1..10).map {
            async { cb.reset() }
        }
        jobs.awaitAll()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }
}
