/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for CircuitBreaker, ConnectionLimiter,
 * and DocumentCache wired into protocol services and FormatRegistry
 *
 *########################################################*/
package digital.vasic.yole.network.common

import digital.vasic.yole.format.DocumentCache
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

// ---------------------------------------------------------------------------
// CircuitBreaker Integration Tests
// ---------------------------------------------------------------------------

class CircuitBreakerIntegrationTests {

    @Test
    fun circuitBreakerProtectsConnectMethod() = runBlocking {
        val cb = CircuitBreaker(name = "test-service", failureThreshold = 3)

        // Simulate 3 consecutive failures
        repeat(3) {
            cb.execute { throw RuntimeException("connection refused") }
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Next call should be rejected immediately with CircuitBreakerOpenException
        val result = cb.execute { "connected" }
        assertTrue(result.isFailure)
        assertIs<CircuitBreakerOpenException>(result.exceptionOrNull())
    }

    @Test
    fun circuitBreakerResetsAfterTimeout() = runBlocking {
        val cb = CircuitBreaker(
            name = "timeout-test",
            failureThreshold = 1,
            resetTimeout = 100.milliseconds
        )

        // Trigger open
        cb.execute { throw RuntimeException("fail") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Wait for reset timeout
        delay(150)

        // Should transition to HALF_OPEN and allow trial
        val result = cb.execute { "recovered" }
        assertTrue(result.isSuccess)
        assertEquals("recovered", result.getOrNull())
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun circuitBreakerTracksCallCount() = runBlocking {
        val cb = CircuitBreaker(name = "counter-test", failureThreshold = 10)

        repeat(5) { cb.execute { "ok" } }
        assertEquals(5L, cb.calls)
        assertEquals(5, cb.successes)
        assertEquals(0, cb.failures)
    }

    @Test
    fun circuitBreakerHalfOpenFailureReopensCircuit() = runBlocking {
        val cb = CircuitBreaker(
            name = "half-open-fail",
            failureThreshold = 1,
            resetTimeout = 50.milliseconds
        )

        // Open the circuit
        cb.execute { throw RuntimeException("fail") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Wait for timeout to allow HALF_OPEN
        delay(100)

        // Fail during HALF_OPEN trial
        cb.execute { throw RuntimeException("still broken") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun circuitBreakerResetClearsState() = runBlocking {
        val cb = CircuitBreaker(name = "reset-test", failureThreshold = 1)
        cb.execute { throw RuntimeException("fail") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        cb.reset()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }

    @Test
    fun circuitBreakerSuccessResetsFailureCount() = runBlocking {
        val cb = CircuitBreaker(name = "success-reset", failureThreshold = 3)

        // 2 failures, then a success
        cb.execute { throw RuntimeException("fail-1") }
        cb.execute { throw RuntimeException("fail-2") }
        assertEquals(2, cb.failures)

        cb.execute { "success" }
        assertEquals(0, cb.failures)
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun circuitBreakerWrapsResultFailureCorrectly() = runBlocking {
        val cb = CircuitBreaker(name = "result-wrap", failureThreshold = 3)
        val result = cb.execute<Result<Unit>> {
            Result.failure(Exception("inner failure"))
        }
        // The circuit breaker wraps the successful block execution (no thrown exception)
        assertTrue(result.isSuccess)
        val inner = result.getOrNull()
        assertNotNull(inner)
        assertTrue(inner.isFailure)
    }

    @Test
    fun circuitBreakerNameIsAccessible() {
        val cb = CircuitBreaker(name = "named-breaker", failureThreshold = 5)
        assertEquals("named-breaker", cb.name)
    }

    @Test
    fun circuitBreakerForEachProtocol() = runBlocking {
        val protocols = listOf("dropbox", "googledrive", "onedrive", "ftp", "sftp", "smb", "webdav", "git")
        for (protocol in protocols) {
            val cb = CircuitBreaker(name = protocol, failureThreshold = 5)
            val result = cb.execute { "connected to $protocol" }
            assertTrue(result.isSuccess, "CircuitBreaker for $protocol should succeed")
            assertEquals("connected to $protocol", result.getOrNull())
        }
    }
}

// ---------------------------------------------------------------------------
// ConnectionLimiter Integration Tests
// ---------------------------------------------------------------------------

class ConnectionLimiterIntegrationTests {

    @Test
    fun connectionLimiterAllowsSingleConnection() = runBlocking {
        val limiter = ConnectionLimiter(name = "single", maxConcurrent = 1)
        val result = limiter.withConnection { "done" }
        assertEquals("done", result)
    }

    @Test
    fun connectionLimiterTracksAvailablePermits() = runBlocking {
        val limiter = ConnectionLimiter(name = "permits", maxConcurrent = 5)
        assertEquals(5, limiter.availablePermits)
    }

    @Test
    fun connectionLimiterReleasesPermitAfterCompletion() = runBlocking {
        val limiter = ConnectionLimiter(name = "release", maxConcurrent = 2)
        limiter.withConnection { "first" }
        assertEquals(2, limiter.availablePermits)
    }

    @Test
    fun connectionLimiterReleasesPermitOnException() = runBlocking {
        val limiter = ConnectionLimiter(name = "exception-release", maxConcurrent = 2)
        try {
            limiter.withConnection { throw RuntimeException("oops") }
        } catch (_: RuntimeException) {
            // Expected
        }
        assertEquals(2, limiter.availablePermits)
    }

    @Test
    fun connectionLimiterConcurrentAccessWithinLimit() = runBlocking {
        val limiter = ConnectionLimiter(name = "concurrent", maxConcurrent = 3)
        val results = (1..3).map { i ->
            async {
                limiter.withConnection { "result-$i" }
            }
        }.awaitAll()
        assertEquals(3, results.size)
        assertTrue(results.all { it.startsWith("result-") })
    }

    @Test
    fun connectionLimiterForEachProtocol() = runBlocking {
        val protocols = listOf("dropbox", "googledrive", "onedrive", "ftp", "sftp", "smb", "webdav", "git")
        for (protocol in protocols) {
            val limiter = ConnectionLimiter(name = protocol, maxConcurrent = 5)
            val result = limiter.withConnection { "connected to $protocol" }
            assertEquals("connected to $protocol", result, "ConnectionLimiter for $protocol should work")
        }
    }

    @Test
    fun connectionLimiterNameIsAccessible() {
        val limiter = ConnectionLimiter(name = "named-limiter", maxConcurrent = 3)
        assertEquals("named-limiter", limiter.name)
    }

    @Test
    fun connectionLimiterMaxConcurrentIsAccessible() {
        val limiter = ConnectionLimiter(name = "capacity", maxConcurrent = 10)
        assertEquals(10, limiter.maxConcurrent)
    }
}

// ---------------------------------------------------------------------------
// DocumentCache Integration with FormatRegistry Tests
// ---------------------------------------------------------------------------

class DocumentCacheFormatRegistryIntegrationTests {

    private val plainTextFormat = TextFormat(
        id = "plaintext",
        name = "Plain Text",
        defaultExtension = ".txt",
        extensions = listOf(".txt")
    )

    @Test
    fun documentCacheFieldExistsOnFormatRegistry() {
        val cache = FormatRegistry.documentCache
        assertNotNull(cache)
    }

    @Test
    fun documentCacheStartsEmpty() = runBlocking {
        val cache = DocumentCache()
        assertEquals(0, cache.size)
        assertEquals(0L, cache.hits)
        assertEquals(0L, cache.misses)
    }

    @Test
    fun documentCachePutAndGet() = runBlocking {
        val cache = DocumentCache()
        val doc = ParsedDocument(
            format = plainTextFormat,
            rawContent = "hello",
            parsedContent = "hello"
        )
        cache.put("key1", doc)
        val retrieved = cache.get("key1")
        assertNotNull(retrieved)
        assertEquals("hello", retrieved.rawContent)
    }

    @Test
    fun documentCacheHitIncrementsHitCount() = runBlocking {
        val cache = DocumentCache()
        val doc = ParsedDocument(
            format = plainTextFormat,
            rawContent = "test",
            parsedContent = "test"
        )
        cache.put("k", doc)
        cache.get("k")
        assertEquals(1L, cache.hits)
    }

    @Test
    fun documentCacheMissIncrementsMissCount() = runBlocking {
        val cache = DocumentCache()
        cache.get("nonexistent")
        assertEquals(1L, cache.misses)
    }

    @Test
    fun documentCacheHitRateCalculation() = runBlocking {
        val cache = DocumentCache()
        val doc = ParsedDocument(
            format = plainTextFormat,
            rawContent = "data",
            parsedContent = "data"
        )
        cache.put("k", doc)
        cache.get("k") // hit
        cache.get("missing") // miss
        assertEquals(0.5, cache.hitRate, 0.001)
    }

    @Test
    fun documentCacheEvictsLeastRecentlyUsed() = runBlocking {
        val cache = DocumentCache(maxSize = 2)
        val doc1 = ParsedDocument(format = plainTextFormat, rawContent = "1", parsedContent = "1")
        val doc2 = ParsedDocument(format = plainTextFormat, rawContent = "2", parsedContent = "2")
        val doc3 = ParsedDocument(format = plainTextFormat, rawContent = "3", parsedContent = "3")

        cache.put("a", doc1)
        cache.put("b", doc2)
        cache.put("c", doc3) // Should evict "a"

        assertNull(cache.get("a"))
        assertNotNull(cache.get("b"))
        assertNotNull(cache.get("c"))
    }

    @Test
    fun documentCacheClearRemovesAll() = runBlocking {
        val cache = DocumentCache()
        val doc = ParsedDocument(format = plainTextFormat, rawContent = "x", parsedContent = "x")
        cache.put("key", doc)
        cache.clear()
        assertEquals(0, cache.size)
        assertNull(cache.get("key"))
    }

    @Test
    fun documentCacheInvalidateRemovesSingleKey() = runBlocking {
        val cache = DocumentCache()
        val doc1 = ParsedDocument(format = plainTextFormat, rawContent = "1", parsedContent = "1")
        val doc2 = ParsedDocument(format = plainTextFormat, rawContent = "2", parsedContent = "2")
        cache.put("a", doc1)
        cache.put("b", doc2)
        cache.invalidate("a")
        assertNull(cache.get("a"))
        assertNotNull(cache.get("b"))
    }

    @Test
    fun documentCacheContainsKey() = runBlocking {
        val cache = DocumentCache()
        val doc = ParsedDocument(format = plainTextFormat, rawContent = "x", parsedContent = "x")
        cache.put("present", doc)
        assertTrue(cache.contains("present"))
        assertFalse(cache.contains("absent"))
    }

    @Test
    fun documentCacheConcurrentAccess() = runBlocking {
        val cache = DocumentCache()
        val results = (1..10).map { i ->
            async {
                val doc = ParsedDocument(
                    format = plainTextFormat,
                    rawContent = "content-$i",
                    parsedContent = "content-$i"
                )
                cache.put("key-$i", doc)
                cache.get("key-$i")
            }
        }.awaitAll()
        assertEquals(10, results.filterNotNull().size)
    }
}

// ---------------------------------------------------------------------------
// Combined Resilience Pattern Tests
// ---------------------------------------------------------------------------

class CombinedResiliencePatternTests {

    @Test
    fun circuitBreakerAndConnectionLimiterTogether() = runBlocking {
        val cb = CircuitBreaker(name = "combined", failureThreshold = 3)
        val limiter = ConnectionLimiter(name = "combined", maxConcurrent = 5)

        val result = cb.execute {
            limiter.withConnection {
                Result.success("connected")
            }
        }
        assertTrue(result.isSuccess)
        val inner = result.getOrNull()
        assertNotNull(inner)
        assertTrue(inner.isSuccess)
        assertEquals("connected", inner.getOrNull())
    }

    @Test
    fun circuitBreakerOpenBlocksConnectionLimiter() = runBlocking {
        val cb = CircuitBreaker(name = "blocking", failureThreshold = 1, resetTimeout = 60.seconds)
        val limiter = ConnectionLimiter(name = "blocking", maxConcurrent = 5)

        // Open circuit
        cb.execute { throw RuntimeException("boom") }

        // Attempt through both layers
        val result = cb.execute {
            limiter.withConnection {
                "should not reach"
            }
        }
        assertTrue(result.isFailure)
        assertIs<CircuitBreakerOpenException>(result.exceptionOrNull())
    }

    @Test
    fun foldPatternExtractsInnerResult() = runBlocking {
        val cb = CircuitBreaker(name = "fold-test", failureThreshold = 5)
        val limiter = ConnectionLimiter(name = "fold-test", maxConcurrent = 5)

        val result = cb.execute {
            limiter.withConnection {
                Result.success(Unit)
            }
        }.fold(
            onSuccess = { it },
            onFailure = { Result.failure(it) }
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun foldPatternOnCircuitBreakerFailure() = runBlocking {
        val cb = CircuitBreaker(name = "fold-fail", failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("initial fail") }

        val result = cb.execute {
            Result.success(Unit)
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                Result.failure(Exception("Wrapped: ${e.message}"))
            }
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Wrapped"))
    }

    @Test
    fun pathUtilsThrowsOnTraversal() {
        assertFailsWith<SecurityException> {
            PathUtils.normalizePath("../../etc/passwd", "/home/user")
        }
    }
}
