/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for CircuitBreaker, ConnectionLimiter, and DocumentCache
 *
 *########################################################*/
package digital.vasic.yole.network.common

import digital.vasic.yole.format.DocumentCache
import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.TextFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

// ---------------------------------------------------------------------------
// CircuitBreaker Tests
// ---------------------------------------------------------------------------

class CircuitBreakerStateTransitionTests {

    @Test
    fun initialStateIsClosed() {
        val cb = CircuitBreaker(name = "test")
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun successfulCallKeepsStateClosed() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 3)
        val result = cb.execute { "ok" }
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun singleFailureDoesNotOpenCircuit() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 3)
        val result = cb.execute { throw RuntimeException("boom") }
        assertTrue(result.isFailure)
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(1, cb.failures)
    }

    @Test
    fun consecutiveFailuresOpenCircuit() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 3, resetTimeout = 60.seconds)
        repeat(3) {
            cb.execute { throw RuntimeException("fail-$it") }
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
        assertEquals(3, cb.failures)
    }

    @Test
    fun openCircuitRejectsCallsImmediately() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("trigger") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        val result = cb.execute { "should not run" }
        assertTrue(result.isFailure)
        assertIs<CircuitBreakerOpenException>(result.exceptionOrNull())
    }

    @Test
    fun circuitBreakerOpenExceptionContainsName() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 10.seconds, name = "webdav-breaker")
        cb.execute { throw RuntimeException("fail") }

        val result = cb.execute { "nope" }
        val ex = result.exceptionOrNull()
        assertIs<CircuitBreakerOpenException>(ex)
        assertTrue(ex.message!!.contains("webdav-breaker"))
    }

    @Test
    fun successAfterFailuresResetsCount() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 3)
        cb.execute { throw RuntimeException("f1") }
        cb.execute { throw RuntimeException("f2") }
        assertEquals(2, cb.failures)

        cb.execute { "success" }
        assertEquals(0, cb.failures)
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun resetBringsCircuitToClosed() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("open it") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        cb.reset()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }

    @Test
    fun callCounterIncrements() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 10)
        assertEquals(0, cb.calls)
        cb.execute { "a" }
        cb.execute { "b" }
        cb.execute { throw RuntimeException("c") }
        assertEquals(3, cb.calls)
    }

    @Test
    fun successCounterIncrements() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 10)
        assertEquals(0, cb.successes)
        cb.execute { "a" }
        cb.execute { "b" }
        cb.execute { throw RuntimeException("c") }
        cb.execute { "d" }
        assertEquals(3, cb.successes)
    }

    @Test
    fun defaultParametersAreReasonable() {
        val cb = CircuitBreaker()
        assertEquals("default", cb.name)
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
        assertEquals(0, cb.successes)
        assertEquals(0, cb.calls)
    }

    @Test
    fun failureThresholdOfOneOpensImmediately() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("single") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun halfOpenSuccessTransitionsToClosed() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 1.milliseconds)
        cb.execute { throw RuntimeException("open") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Wait for reset timeout to elapse
        delay(10.milliseconds)

        // Next call should transition to HALF_OPEN then succeed -> CLOSED
        val result = cb.execute { "recovered" }
        assertTrue(result.isSuccess)
        assertEquals("recovered", result.getOrNull())
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    @Test
    fun halfOpenFailureTransitionsBackToOpen() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 1.milliseconds)
        cb.execute { throw RuntimeException("open") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        delay(10.milliseconds)

        // Trial call fails -> back to OPEN
        val result = cb.execute { throw RuntimeException("still broken") }
        assertTrue(result.isFailure)
        assertEquals(CircuitBreaker.State.OPEN, cb.state)
    }

    @Test
    fun openCircuitCountsRejectedCalls() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("open") }
        val callsBefore = cb.calls
        cb.execute { "rejected" }
        assertEquals(callsBefore + 1, cb.calls)
    }

    @Test
    fun executeReturnsTypedResult() = runBlocking {
        val cb = CircuitBreaker()
        val intResult = cb.execute { 42 }
        assertEquals(42, intResult.getOrNull())

        val listResult = cb.execute { listOf("a", "b") }
        assertEquals(listOf("a", "b"), listResult.getOrNull())
    }

    @Test
    fun resetFromHalfOpenNotNeeded() = runBlocking {
        // Resetting from CLOSED is a no-op but should not crash
        val cb = CircuitBreaker()
        cb.reset()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }
}

class CircuitBreakerConcurrencyTests {

    @Test
    fun multipleConcurrentSuccessfulCalls() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 100)
        val results = (1..20).map { i ->
            async { cb.execute { i * 2 } }
        }.awaitAll()

        assertEquals(20, results.size)
        assertTrue(results.all { it.isSuccess })
        assertEquals(20, cb.calls)
        assertEquals(20, cb.successes)
    }

    @Test
    fun mixedConcurrentCallsTrackCorrectly() = runBlocking {
        val cb = CircuitBreaker(failureThreshold = 100)
        val results = (1..10).map { i ->
            async {
                if (i % 3 == 0) {
                    cb.execute { throw RuntimeException("fail-$i") }
                } else {
                    cb.execute { "ok-$i" }
                }
            }
        }.awaitAll()

        assertEquals(10, results.size)
        assertEquals(10, cb.calls)
    }
}

// ---------------------------------------------------------------------------
// ConnectionLimiter Tests
// ---------------------------------------------------------------------------

class ConnectionLimiterBasicTests {

    @Test
    fun singleOperationCompletesSuccessfully() = runBlocking {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        val result = limiter.withConnection { "hello" }
        assertEquals("hello", result)
    }

    @Test
    fun availablePermitsReflectsMaxConcurrent() {
        val limiter = ConnectionLimiter(maxConcurrent = 7)
        assertEquals(7, limiter.availablePermits)
    }

    @Test
    fun defaultMaxConcurrentIsFive() {
        val limiter = ConnectionLimiter()
        assertEquals(5, limiter.maxConcurrent)
        assertEquals(5, limiter.availablePermits)
    }

    @Test
    fun nameIsStoredCorrectly() {
        val limiter = ConnectionLimiter(name = "dropbox-limiter")
        assertEquals("dropbox-limiter", limiter.name)
    }

    @Test
    fun operationExceptionPropagates() = runBlocking {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        assertFailsWith<IllegalStateException> {
            limiter.withConnection { throw IllegalStateException("inner error") }
        }
    }

    @Test
    fun permitReleasedAfterSuccess() = runBlocking {
        val limiter = ConnectionLimiter(maxConcurrent = 1)
        limiter.withConnection { "first" }
        assertEquals(1, limiter.availablePermits)
        val second = limiter.withConnection { "second" }
        assertEquals("second", second)
    }

    @Test
    fun permitReleasedAfterException() = runBlocking {
        val limiter = ConnectionLimiter(maxConcurrent = 1)
        try {
            limiter.withConnection { throw RuntimeException("fail") }
        } catch (_: RuntimeException) { }
        // Permit should be released
        assertEquals(1, limiter.availablePermits)
        val result = limiter.withConnection { "recovered" }
        assertEquals("recovered", result)
    }

    @Test
    fun multipleSequentialOperations() = runBlocking {
        val limiter = ConnectionLimiter(maxConcurrent = 2)
        val results = mutableListOf<Int>()
        for (i in 1..10) {
            limiter.withConnection { results.add(i) }
        }
        assertEquals((1..10).toList(), results)
    }

    @Test
    fun concurrentOperationsWithinLimit() = runBlocking {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 5.seconds)
        val results = (1..5).map { i ->
            async {
                limiter.withConnection { i * 10 }
            }
        }.awaitAll()
        assertEquals(listOf(10, 20, 30, 40, 50), results.sorted())
    }

    @Test
    fun maxConcurrentOneSerializesOperations() = runBlocking {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 5.seconds)
        val order = mutableListOf<Int>()
        val jobs = (1..5).map { i ->
            async {
                limiter.withConnection {
                    order.add(i)
                    i
                }
            }
        }
        jobs.awaitAll()
        // All should complete
        assertEquals(5, order.size)
    }
}

// ---------------------------------------------------------------------------
// DocumentCache Tests
// ---------------------------------------------------------------------------

class DocumentCacheBasicTests {

    private val testFormat = TextFormat(
        id = "plaintext",
        name = "Plain Text",
        defaultExtension = ".txt",
        extensions = listOf(".txt"),
        detectionPatterns = emptyList()
    )

    private fun doc(content: String) = ParsedDocument(
        format = testFormat,
        rawContent = content,
        parsedContent = content
    )

    @Test
    fun emptyDefaultSize() {
        val cache = DocumentCache()
        assertEquals(0, cache.size)
    }

    @Test
    fun putAndGetSingleEntry() = runBlocking {
        val cache = DocumentCache()
        val document = doc("hello world")
        cache.put("key1", document)
        val retrieved = cache.get("key1")
        assertNotNull(retrieved)
        assertEquals("hello world", retrieved.rawContent)
    }

    @Test
    fun getMissingKeyReturnsNull() = runBlocking {
        val cache = DocumentCache()
        assertNull(cache.get("nonexistent"))
    }

    @Test
    fun hitsAndMissesTracking() = runBlocking {
        val cache = DocumentCache()
        cache.put("a", doc("aaa"))

        cache.get("a")  // hit
        cache.get("a")  // hit
        cache.get("b")  // miss

        assertEquals(2, cache.hits)
        assertEquals(1, cache.misses)
    }

    @Test
    fun hitRateCalculation() = runBlocking {
        val cache = DocumentCache()
        cache.put("a", doc("aaa"))

        cache.get("a")  // hit
        cache.get("a")  // hit
        cache.get("a")  // hit
        cache.get("b")  // miss

        assertEquals(0.75, cache.hitRate, 0.001)
    }

    @Test
    fun hitRateZeroWhenNoAccesses() {
        val cache = DocumentCache()
        assertEquals(0.0, cache.hitRate, 0.001)
    }

    @Test
    fun invalidateRemovesEntry() = runBlocking {
        val cache = DocumentCache()
        cache.put("key", doc("content"))
        assertTrue(cache.contains("key"))

        cache.invalidate("key")
        assertFalse(cache.contains("key"))
        assertNull(cache.get("key"))
    }

    @Test
    fun invalidateNonexistentKeyIsNoOp() = runBlocking {
        val cache = DocumentCache()
        cache.invalidate("missing")
        assertEquals(0, cache.size)
    }

    @Test
    fun clearRemovesAllEntries() = runBlocking {
        val cache = DocumentCache()
        cache.put("a", doc("aaa"))
        cache.put("b", doc("bbb"))
        cache.put("c", doc("ccc"))
        assertEquals(3, cache.size)

        cache.clear()
        assertEquals(0, cache.size)
        assertNull(cache.get("a"))
        assertNull(cache.get("b"))
        assertNull(cache.get("c"))
    }

    @Test
    fun clearResetsHitsAndMisses() = runBlocking {
        val cache = DocumentCache()
        cache.put("a", doc("aaa"))
        cache.get("a")
        cache.get("b")

        cache.clear()
        assertEquals(0, cache.hits)
        assertEquals(0, cache.misses)
        assertEquals(0.0, cache.hitRate, 0.001)
    }

    @Test
    fun containsReturnsTrueForExistingKey() = runBlocking {
        val cache = DocumentCache()
        cache.put("exists", doc("yes"))
        assertTrue(cache.contains("exists"))
    }

    @Test
    fun containsReturnsFalseForMissingKey() = runBlocking {
        val cache = DocumentCache()
        assertFalse(cache.contains("nope"))
    }

    @Test
    fun putOverwritesExistingEntry() = runBlocking {
        val cache = DocumentCache()
        cache.put("key", doc("original"))
        cache.put("key", doc("updated"))

        val retrieved = cache.get("key")
        assertNotNull(retrieved)
        assertEquals("updated", retrieved.rawContent)
        assertEquals(1, cache.size)
    }

    @Test
    fun sizeTracksEntries() = runBlocking {
        val cache = DocumentCache()
        assertEquals(0, cache.size)
        cache.put("a", doc("a"))
        assertEquals(1, cache.size)
        cache.put("b", doc("b"))
        assertEquals(2, cache.size)
        cache.invalidate("a")
        assertEquals(1, cache.size)
    }
}

class DocumentCacheLRUEvictionTests {

    private val testFormat = TextFormat(
        id = "plaintext",
        name = "Plain Text",
        defaultExtension = ".txt",
        extensions = listOf(".txt"),
        detectionPatterns = emptyList()
    )

    private fun doc(content: String) = ParsedDocument(
        format = testFormat,
        rawContent = content,
        parsedContent = content
    )

    @Test
    fun evictsLeastRecentlyUsedWhenMaxSizeExceeded() = runBlocking {
        val cache = DocumentCache(maxSize = 3)
        cache.put("a", doc("aaa"))
        cache.put("b", doc("bbb"))
        cache.put("c", doc("ccc"))
        assertEquals(3, cache.size)

        // Adding a 4th entry should evict the least recently used ("a")
        cache.put("d", doc("ddd"))
        assertEquals(3, cache.size)
        assertNull(cache.get("a"))
        assertNotNull(cache.get("b"))
        assertNotNull(cache.get("c"))
        assertNotNull(cache.get("d"))
    }

    @Test
    fun accessReordersEvictionPriority() = runBlocking {
        val cache = DocumentCache(maxSize = 3)
        cache.put("a", doc("aaa"))
        cache.put("b", doc("bbb"))
        cache.put("c", doc("ccc"))

        // Access "a" to make it recently used
        cache.get("a")

        // Adding "d" should now evict "b" (least recently used) instead of "a"
        cache.put("d", doc("ddd"))
        assertEquals(3, cache.size)
        assertNotNull(cache.get("a"))
        assertNull(cache.get("b"))
        assertNotNull(cache.get("c"))
        assertNotNull(cache.get("d"))
    }

    @Test
    fun maxSizeOneKeepsOnlyLatest() = runBlocking {
        val cache = DocumentCache(maxSize = 1)
        cache.put("a", doc("aaa"))
        cache.put("b", doc("bbb"))

        assertEquals(1, cache.size)
        assertNull(cache.get("a"))
        assertNotNull(cache.get("b"))
    }

    @Test
    fun evictionCascadesCorrectly() = runBlocking {
        val cache = DocumentCache(maxSize = 2)
        cache.put("a", doc("aaa"))
        cache.put("b", doc("bbb"))
        cache.put("c", doc("ccc"))  // evicts a
        cache.put("d", doc("ddd"))  // evicts b

        assertEquals(2, cache.size)
        assertNull(cache.get("a"))
        assertNull(cache.get("b"))
        assertNotNull(cache.get("c"))
        assertNotNull(cache.get("d"))
    }

    @Test
    fun fillToExactCapacityNoEviction() = runBlocking {
        val cache = DocumentCache(maxSize = 5)
        for (i in 1..5) {
            cache.put("key-$i", doc("content-$i"))
        }
        assertEquals(5, cache.size)
        for (i in 1..5) {
            assertNotNull(cache.get("key-$i"))
        }
    }
}

class DocumentCacheConcurrencyTests {

    private val testFormat = TextFormat(
        id = "plaintext",
        name = "Plain Text",
        defaultExtension = ".txt",
        extensions = listOf(".txt"),
        detectionPatterns = emptyList()
    )

    private fun doc(content: String) = ParsedDocument(
        format = testFormat,
        rawContent = content,
        parsedContent = content
    )

    @Test
    fun concurrentPutsDoNotCorruptCache() = runBlocking {
        val cache = DocumentCache(maxSize = 50)
        val jobs = (1..20).map { i ->
            async {
                cache.put("key-$i", doc("content-$i"))
            }
        }
        jobs.awaitAll()
        assertEquals(20, cache.size)
    }

    @Test
    fun concurrentGetsReturnCorrectValues() = runBlocking {
        val cache = DocumentCache()
        cache.put("shared", doc("shared-content"))

        val results = (1..10).map {
            async { cache.get("shared") }
        }.awaitAll()

        assertTrue(results.all { it != null })
        assertTrue(results.all { it!!.rawContent == "shared-content" })
    }

    @Test
    fun concurrentMixedOperations() = runBlocking {
        val cache = DocumentCache(maxSize = 10)

        // Pre-populate
        for (i in 1..5) {
            cache.put("pre-$i", doc("pre-content-$i"))
        }

        val jobs = (1..20).map { i ->
            async {
                when (i % 4) {
                    0 -> cache.put("dyn-$i", doc("dynamic-$i"))
                    1 -> cache.get("pre-${(i % 5) + 1}")
                    2 -> cache.contains("pre-${(i % 5) + 1}")
                    else -> cache.invalidate("dyn-${i - 1}")
                }
            }
        }
        jobs.awaitAll()

        // Cache should be in a consistent state (no crashes, size within bounds)
        assertTrue(cache.size <= 10)
    }
}

class DocumentCacheMetadataTests {

    private val testFormat = TextFormat(
        id = "markdown",
        name = "Markdown",
        defaultExtension = ".md",
        extensions = listOf(".md"),
        detectionPatterns = emptyList()
    )

    @Test
    fun cachePreservesDocumentMetadata() = runBlocking {
        val cache = DocumentCache()
        val document = ParsedDocument(
            format = testFormat,
            rawContent = "# Title",
            parsedContent = "<h1>Title</h1>",
            metadata = mapOf("title" to "Title", "lines" to "1"),
            errors = emptyList()
        )
        cache.put("doc1", document)

        val retrieved = cache.get("doc1")
        assertNotNull(retrieved)
        assertEquals(testFormat, retrieved.format)
        assertEquals("# Title", retrieved.rawContent)
        assertEquals("<h1>Title</h1>", retrieved.parsedContent)
        assertEquals("Title", retrieved.metadata["title"])
        assertEquals("1", retrieved.metadata["lines"])
        assertTrue(retrieved.errors.isEmpty())
    }

    @Test
    fun cachePreservesDocumentErrors() = runBlocking {
        val cache = DocumentCache()
        val document = ParsedDocument(
            format = testFormat,
            rawContent = "bad content",
            parsedContent = "",
            errors = listOf("Line 1: syntax error", "Line 2: unexpected token")
        )
        cache.put("err-doc", document)

        val retrieved = cache.get("err-doc")
        assertNotNull(retrieved)
        assertEquals(2, retrieved.errors.size)
        assertEquals("Line 1: syntax error", retrieved.errors[0])
    }
}
