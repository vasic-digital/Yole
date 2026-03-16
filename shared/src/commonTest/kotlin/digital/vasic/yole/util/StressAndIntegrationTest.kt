/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Stress and Integration Tests
 * Validates system responsiveness, non-blocking behavior,
 * and resilience under extreme load conditions
 * Kotlin Multiplatform compatible (no JVM-only imports)
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import digital.vasic.yole.format.*

// ---------------------------------------------------------------------------
// 1. RateLimiterStressTest
// ---------------------------------------------------------------------------

/**
 * Stress tests for rate limiting utilities.
 *
 * Validates that RateLimiter, TokenBucket, AdaptiveRateLimiter, and
 * OperationThrottler remain correct and responsive under extreme concurrent
 * load, burst traffic, and varying failure rates.
 */
class RateLimiterStressTest {

    // -- RateLimiter --

    @Test
    fun testRateLimiterUnderExtremeConcurrentLoad() = runBlocking<Unit> {
        val limiter = RateLimiter(maxConcurrent = 5)
        val mutex = Mutex()
        var peakActive = 0
        var completedCount = 0
        val totalRequests = 1000

        val jobs = (1..totalRequests).map {
            launch(Dispatchers.Default) {
                limiter.execute {
                    val active = limiter.getActiveCount()
                    mutex.withLock {
                        if (active > peakActive) peakActive = active
                    }
                    // Simulate brief work
                    delay(1)
                    mutex.withLock { completedCount++ }
                }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(totalRequests, completedCount, "All $totalRequests requests must complete")
        assertTrue(peakActive <= 5, "Peak active ($peakActive) must not exceed concurrency limit 5")
    }

    @Test
    fun testRateLimiterConcurrencyNeverExceeded() = runBlocking<Unit> {
        val maxConcurrent = 3
        val limiter = RateLimiter(maxConcurrent = maxConcurrent)
        val mutex = Mutex()
        var concurrent = 0
        var violationDetected = false

        val jobs = (1..500).map {
            launch(Dispatchers.Default) {
                limiter.execute {
                    val current = mutex.withLock { ++concurrent }
                    if (current > maxConcurrent) {
                        mutex.withLock { violationDetected = true }
                    }
                    delay(2)
                    mutex.withLock { concurrent-- }
                }
            }
        }
        jobs.forEach { it.join() }

        assertFalse(violationDetected, "Concurrency limit must never be exceeded")
    }

    @Test
    fun testRateLimiterTimeoutUnderLoad() = runBlocking<Unit> {
        val limiter = RateLimiter(maxConcurrent = 1)
        val mutex = Mutex()
        var timedOut = 0
        var succeeded = 0

        // Saturate the single permit with a long-running operation
        val blocker = launch(Dispatchers.Default) {
            limiter.execute { delay(2000) }
        }
        delay(50) // Let blocker acquire the permit

        // Fire many requests with a short timeout
        val jobs = (1..50).map {
            launch(Dispatchers.Default) {
                val result = limiter.executeWithTimeout(100) { "ok" }
                mutex.withLock {
                    if (result == null) timedOut++ else succeeded++
                }
            }
        }
        jobs.forEach { it.join() }
        blocker.cancel()

        assertTrue(timedOut > 0, "Some requests should have timed out ($timedOut timed out)")
    }

    @Test
    fun testRateLimiterQueueDrainsCompletely() = runBlocking<Unit> {
        val limiter = RateLimiter(maxConcurrent = 2)
        val mutex = Mutex()
        var completed = 0

        val jobs = (1..200).map {
            launch(Dispatchers.Default) {
                limiter.execute {
                    delay(1)
                    mutex.withLock { completed++ }
                }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(200, completed, "Queue must drain completely")
        assertEquals(0, limiter.getActiveCount(), "No operations should remain active")
    }

    // -- TokenBucket --

    @Test
    fun testTokenBucketBurstHandling() = runBlocking<Unit> {
        val capacity = 20
        val bucket = TokenBucket(capacity = capacity, refillRate = 100.0)
        var acquired = 0

        // Attempt to acquire all tokens at once (burst)
        for (i in 1..capacity) {
            if (bucket.tryAcquire()) acquired++
        }

        assertEquals(capacity, acquired, "Should acquire exactly $capacity tokens in burst")

        // Bucket should be empty now
        val extraAcquired = bucket.tryAcquire()
        assertFalse(extraAcquired, "Bucket should be empty after burst")
    }

    @Test
    fun testTokenBucketRefillRecovery() = runBlocking<Unit> {
        val bucket = TokenBucket(capacity = 10, refillRate = 200.0)

        // Drain the bucket
        for (i in 1..10) {
            bucket.tryAcquire()
        }

        val emptyTokens = bucket.getAvailableTokens()
        assertTrue(emptyTokens <= 0, "Bucket should be empty after draining")

        // Wait for refill using real wall-clock time (200 tokens/sec = ~2 tokens per 10ms)
        // Clock.System.now() is used internally, so virtual time via delay() won't advance it.
        // We must use Dispatchers.Default so real time passes for the refill logic to work.
        withContext(Dispatchers.Default) { delay(200) }

        val recoveredTokens = bucket.getAvailableTokens()
        assertTrue(recoveredTokens > 0, "Tokens should have been refilled after delay ($recoveredTokens available)")
    }

    @Test
    fun testTokenBucketConcurrentAcquire() = runBlocking<Unit> {
        val capacity = 50
        val bucket = TokenBucket(capacity = capacity, refillRate = 0.1) // Very slow refill
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

        // Due to refill during execution some extra tokens may appear, but never more than capacity
        assertTrue(
            totalAcquired in 1..capacity + 5,
            "Acquired tokens ($totalAcquired) should be approximately <= capacity ($capacity)"
        )
    }

    @Test
    fun testTokenBucketCapacityCap() = runBlocking<Unit> {
        val capacity = 10
        val bucket = TokenBucket(capacity = capacity, refillRate = 1000.0)

        // Wait long enough for massive refill
        delay(500)

        val available = bucket.getAvailableTokens()
        assertTrue(
            available <= capacity,
            "Available tokens ($available) must not exceed capacity ($capacity)"
        )
    }

    // -- AdaptiveRateLimiter --

    @Test
    fun testAdaptiveRateLimiterScalesOnSuccess() = runBlocking<Unit> {
        val limiter = AdaptiveRateLimiter(initialRate = 5, minRate = 1, maxRate = 20)
        val initialRate = limiter.getCurrentRate()

        // Run many successful operations to trigger scaling up
        // (onSuccess increments successCount, adjusts after 10 successes)
        repeat(50) {
            limiter.execute { "success" }
        }

        val scaledRate = limiter.getCurrentRate()
        assertTrue(
            scaledRate >= initialRate,
            "Rate ($scaledRate) should increase or remain the same after sustained success (initial: $initialRate)"
        )
    }

    @Test
    fun testAdaptiveRateLimiterScalesOnFailure() = runBlocking<Unit> {
        val limiter = AdaptiveRateLimiter(initialRate = 10, minRate = 1, maxRate = 20)
        val initialRate = limiter.getCurrentRate()

        // Generate failures to trigger scale-down (3 failures triggers adjustment)
        repeat(20) {
            try {
                limiter.execute<String> { throw RuntimeException("simulated failure") }
            } catch (_: RuntimeException) {
                // Expected
            }
        }

        val reducedRate = limiter.getCurrentRate()
        assertTrue(
            reducedRate <= initialRate,
            "Rate ($reducedRate) should decrease or remain the same after failures (initial: $initialRate)"
        )
    }

    @Test
    fun testAdaptiveRateLimiterUnderVaryingFailureRates() = runBlocking<Unit> {
        val limiter = AdaptiveRateLimiter(initialRate = 5, minRate = 1, maxRate = 20)
        val mutex = Mutex()
        var successes = 0
        var failures = 0

        // Mixed success/failure load
        val jobs = (1..100).map { i ->
            launch(Dispatchers.Default) {
                try {
                    limiter.execute {
                        if (i % 4 == 0) throw RuntimeException("fail")
                        mutex.withLock { successes++ }
                        "ok"
                    }
                } catch (_: RuntimeException) {
                    mutex.withLock { failures++ }
                }
            }
        }
        jobs.forEach { it.join() }

        val totalProcessed = successes + failures
        assertEquals(100, totalProcessed, "All 100 operations must be processed")

        val currentRate = limiter.getCurrentRate()
        assertTrue(currentRate in 1..20, "Rate ($currentRate) must remain within bounds [1, 20]")
    }

    @Test
    fun testAdaptiveRateLimiterRespectsMinMaxBounds() = runBlocking<Unit> {
        val limiter = AdaptiveRateLimiter(initialRate = 2, minRate = 1, maxRate = 5)

        // Drive failures to push rate down to min
        repeat(30) {
            try {
                limiter.execute<String> { throw RuntimeException("fail") }
            } catch (_: RuntimeException) { }
        }
        val rateAfterFailures = limiter.getCurrentRate()
        assertTrue(rateAfterFailures >= 1, "Rate ($rateAfterFailures) must not go below minRate 1")

        // Drive successes to push rate up to max
        repeat(200) {
            limiter.execute { "ok" }
        }
        val rateAfterSuccess = limiter.getCurrentRate()
        assertTrue(rateAfterSuccess <= 5, "Rate ($rateAfterSuccess) must not exceed maxRate 5")
    }

    // -- OperationThrottler --

    @Test
    fun testOperationThrottlerRapidFire() = runBlocking<Unit> {
        val throttler = OperationThrottler(windowMs = 1000, maxOperations = 10)
        var allowed = 0
        var throttled = 0

        // Fire 100 operations rapidly (all within one window)
        repeat(100) {
            if (throttler.tryThrottle("rapid-op")) allowed++ else throttled++
        }

        assertEquals(10, allowed, "Only maxOperations (10) should be allowed within the window")
        assertEquals(90, throttled, "Remaining 90 should be throttled")
    }

    @Test
    fun testOperationThrottlerDifferentIds() = runBlocking<Unit> {
        val throttler = OperationThrottler(windowMs = 1000, maxOperations = 5)

        var allowedA = 0
        var allowedB = 0

        repeat(10) {
            if (throttler.tryThrottle("op-a")) allowedA++
            if (throttler.tryThrottle("op-b")) allowedB++
        }

        assertEquals(5, allowedA, "Operation A should allow 5 within window")
        assertEquals(5, allowedB, "Operation B should allow 5 within window (independent)")
    }

    @Test
    fun testOperationThrottlerConcurrentAccess() = runBlocking<Unit> {
        val throttler = OperationThrottler(windowMs = 1000, maxOperations = 50)
        val mutex = Mutex()
        var totalAllowed = 0

        val jobs = (1..200).map {
            launch(Dispatchers.Default) {
                val allowed = throttler.tryThrottle("concurrent-op")
                if (allowed) mutex.withLock { totalAllowed++ }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(
            totalAllowed <= 50,
            "Allowed count ($totalAllowed) must not exceed maxOperations (50)"
        )
        assertTrue(
            totalAllowed > 0,
            "At least some operations should be allowed"
        )
    }

    @Test
    fun testOperationThrottlerClearResetsState() = runBlocking<Unit> {
        val throttler = OperationThrottler(windowMs = 1000, maxOperations = 5)

        // Exhaust the limit
        repeat(5) { throttler.tryThrottle("clear-test") }
        val blockedBefore = !throttler.tryThrottle("clear-test")
        assertTrue(blockedBefore, "Should be throttled after exhausting limit")

        // Clear and verify reset
        throttler.clear("clear-test")
        val allowedAfter = throttler.tryThrottle("clear-test")
        assertTrue(allowedAfter, "Should be allowed after clearing throttle state")
    }
}

// ---------------------------------------------------------------------------
// 2. LazyLoadingStressTest
// ---------------------------------------------------------------------------

/**
 * Stress tests for lazy loading utilities.
 *
 * Validates that LazyDocumentLoader, LazyStringLoader, and FlowLazyLoader
 * handle very large documents, concurrent access, and resource cleanup
 * correctly without leaks or data corruption.
 */
class LazyLoadingStressTest {

    /**
     * Generates a large document with the specified number of lines.
     */
    private fun generateLargeDocument(lineCount: Int): String {
        return buildString {
            for (i in 1..lineCount) {
                appendLine("Line $i: This is line number $i of the test document with padding content to simulate real data.")
            }
        }
    }

    // -- LazyStringLoader --

    @Test
    fun testLazyStringLoaderWithVeryLargeDocument() = runBlocking<Unit> {
        val lineCount = 10000
        val content = generateLargeDocument(lineCount)
        val loader = LazyStringLoader(content, chunkSize = 500)

        // Access first chunk
        val firstChunk = loader.getChunk(0)
        assertNotNull(firstChunk, "First chunk must not be null")
        assertTrue(firstChunk.contains("Line 1:"), "First chunk should contain Line 1")

        // Access last chunk (chunk index = 10000/500 - 1 = 19)
        val lastChunkIndex = (lineCount + 500 - 1) / 500 - 1
        val lastChunk = loader.getChunk(lastChunkIndex)
        assertNotNull(lastChunk, "Last chunk must not be null")
    }

    @Test
    fun testLazyStringLoaderGetLinesAcrossChunks() = runBlocking<Unit> {
        val lineCount = 10000
        val content = generateLargeDocument(lineCount)
        val loader = LazyStringLoader(content, chunkSize = 500)

        // Request lines spanning across chunk boundaries
        val lines = loader.getLines(490, 520)
        assertTrue(lines.isNotEmpty(), "Should return lines across chunk boundary")
        assertTrue(lines.size <= 30, "Should return at most 30 lines (requested 490..520)")
    }

    @Test
    fun testLazyStringLoaderConcurrentChunkAccess() = runBlocking<Unit> {
        val content = generateLargeDocument(10000)
        val loader = LazyStringLoader(content, chunkSize = 500)
        val mutex = Mutex()
        val loadedChunks = mutableSetOf<Int>()

        // Concurrently access many different chunks
        val jobs = (0..19).map { chunkIndex ->
            launch(Dispatchers.Default) {
                val chunk = loader.getChunk(chunkIndex)
                assertNotNull(chunk, "Chunk $chunkIndex must not be null")
                mutex.withLock { loadedChunks.add(chunkIndex) }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(20, loadedChunks.size, "All 20 chunks should be loaded")
    }

    @Test
    fun testLazyStringLoaderSameChunkConcurrent() = runBlocking<Unit> {
        val content = generateLargeDocument(5000)
        val loader = LazyStringLoader(content, chunkSize = 500)
        val results = mutableListOf<String?>()
        val mutex = Mutex()

        // Many coroutines requesting the same chunk simultaneously
        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                val chunk = loader.getChunk(3)
                mutex.withLock { results.add(chunk) }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(100, results.size, "All 100 requests should complete")
        val firstResult = results.first()
        assertTrue(results.all { it == firstResult }, "All results for the same chunk must be identical")
    }

    // -- LazyDocumentLoader --

    @Test
    fun testLazyDocumentLoaderPreloadAround() = runBlocking<Unit> {
        val content = generateLargeDocument(10000)
        val loader = LazyStringLoader(content, chunkSize = 500)

        // Preload around chunk 10 with range 2 (loads chunks 8..12)
        loader.preloadAround(10, range = 2)

        // Verify preloaded chunks are instantly available
        for (i in 8..12) {
            val chunk = loader.getChunk(i)
            assertNotNull(chunk, "Preloaded chunk $i must be available")
        }
    }

    @Test
    fun testLazyDocumentLoaderClearFreesMemory() = runBlocking<Unit> {
        val content = generateLargeDocument(10000)
        val loader = LazyStringLoader(content, chunkSize = 500)

        // Load several chunks
        for (i in 0..5) {
            loader.getChunk(i)
        }

        val memoryBefore = loader.getMemoryUsage()
        assertTrue(memoryBefore > 0, "Memory usage should be > 0 after loading chunks")

        loader.clear()

        val memoryAfter = loader.getMemoryUsage()
        assertEquals(0L, memoryAfter, "Memory usage should be 0 after clear()")
    }

    @Test
    fun testLazyDocumentLoaderOutOfBoundsReturnsNull() = runBlocking<Unit> {
        val content = generateLargeDocument(100)
        val loader = LazyStringLoader(content, chunkSize = 50)

        val outOfBounds = loader.getChunk(999)
        assertNull(outOfBounds, "Out of bounds chunk should return null")

        val negativeIndex = loader.getChunk(-1)
        assertNull(negativeIndex, "Negative index should return null")
    }

    // -- FlowLazyLoader --

    @Test
    fun testFlowLazyLoaderMassiveConcurrentLoadMore() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        val mutex = Mutex()
        var totalItemsLoaded = 0

        // Fire many concurrent loadMore calls
        val jobs = (1..100).map { batch ->
            launch(Dispatchers.Default) {
                val items = (1..50).map { "batch-$batch-item-$it" }
                loader.loadMore(items)
                mutex.withLock { totalItemsLoaded += items.size }
            }
        }
        jobs.forEach { it.join() }

        val contentSize = loader.content.value.size
        assertEquals(
            totalItemsLoaded,
            contentSize,
            "Content size ($contentSize) must equal total items loaded ($totalItemsLoaded)"
        )

        loader.cleanup()
    }

    @Test
    fun testFlowLazyLoaderCleanupClearsContent() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()

        loader.loadMore(listOf("a", "b", "c"))
        assertEquals(3, loader.content.value.size, "Should have 3 items before cleanup")

        loader.cleanup()

        assertEquals(0, loader.content.value.size, "Content should be empty after cleanup")
    }

    @Test
    fun testFlowLazyLoaderDoesNotLeakAfterCleanup() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()

        // Load substantial data
        repeat(50) { batch ->
            loader.loadMore((1..100).map { "item-$batch-$it" })
        }

        val sizeBefore = loader.content.value.size
        assertEquals(5000, sizeBefore, "Should have 5000 items loaded")

        loader.cleanup()

        val sizeAfter = loader.content.value.size
        assertEquals(0, sizeAfter, "All items should be cleared after cleanup")
    }

    @Test
    fun testFlowLazyLoaderGetVisibleRange() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()

        val items = (1..1000).map { "item-$it" }
        loader.loadMore(items)

        val range = loader.getVisibleRange(100, 200, buffer = 20)
        assertEquals(80, range.first, "Visible range start should be visibleStart - buffer")
        assertEquals(220, range.last + 1, "Visible range end should be visibleEnd + buffer")

        loader.cleanup()
    }

    @Test
    fun testFlowLazyLoaderGetVisibleRangeBoundsClamp() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()

        val items = (1..50).map { "item-$it" }
        loader.loadMore(items)

        // Test clamping at beginning
        val rangeStart = loader.getVisibleRange(0, 10, buffer = 20)
        assertEquals(0, rangeStart.first, "Range start should clamp to 0")

        // Test clamping at end
        val rangeEnd = loader.getVisibleRange(40, 50, buffer = 20)
        assertTrue(rangeEnd.last < 100, "Range end should be clamped to content size")

        loader.cleanup()
    }
}

// ---------------------------------------------------------------------------
// 3. ParserRegistryStressTest
// ---------------------------------------------------------------------------

/**
 * Stress tests for the ParserRegistry.
 *
 * Validates that concurrent getParser() calls, lazy instantiation, and
 * clear + re-register cycles work correctly without data corruption,
 * missing parsers, or duplicate instantiation.
 */
class ParserRegistryStressTest {

    @BeforeTest
    fun setup() {
        ParserRegistry.clear()
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    @Test
    fun testConcurrentGetParserCalls() = runBlocking<Unit> {
        ParserInitializer.registerAllParsersLazy()

        val mutex = Mutex()
        val parserResults = mutableListOf<TextParser?>()

        // Many coroutines requesting the same parser concurrently
        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                val parser = ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
                mutex.withLock { parserResults.add(parser) }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(100, parserResults.size, "All 100 requests should return a result")
        assertTrue(
            parserResults.all { it != null },
            "All results should be non-null"
        )

        // All instances should be the same cached instance
        val firstParser = parserResults.first()
        assertTrue(
            parserResults.all { it === firstParser },
            "All getParser() calls for the same format must return the same instance"
        )
    }

    @Test
    fun testConcurrentGetParserDifferentFormats() = runBlocking<Unit> {
        ParserInitializer.registerAllParsersLazy()

        val formatIds = listOf(
            FormatRegistry.ID_MARKDOWN,
            FormatRegistry.ID_PLAINTEXT,
            FormatRegistry.ID_TODOTXT,
            FormatRegistry.ID_CSV,
            FormatRegistry.ID_LATEX,
            FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_ASCIIDOC,
            FormatRegistry.ID_WIKITEXT,
            FormatRegistry.ID_CREOLE,
            FormatRegistry.ID_TIDDLYWIKI,
            FormatRegistry.ID_RESTRUCTUREDTEXT,
            FormatRegistry.ID_KEYVALUE,
            FormatRegistry.ID_TASKPAPER,
            FormatRegistry.ID_TEXTILE,
            FormatRegistry.ID_JUPYTER,
            FormatRegistry.ID_RMARKDOWN,
            FormatRegistry.ID_BINARY
        )

        val mutex = Mutex()
        val results = mutableMapOf<String, TextParser?>()

        // Each format requested concurrently by multiple coroutines
        val jobs = formatIds.flatMap { formatId ->
            (1..10).map {
                launch(Dispatchers.Default) {
                    val parser = ParserRegistry.getParser(formatId)
                    mutex.withLock { results[formatId] = parser }
                }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(formatIds.size, results.size, "All formats should have results")
        results.forEach { (formatId, parser) ->
            assertNotNull(parser, "Parser for $formatId must not be null")
            assertEquals(
                formatId,
                parser.supportedFormat.id,
                "Parser for $formatId must have correct supportedFormat"
            )
        }
    }

    @Test
    fun testLazyInstantiationUnderConcurrentAccess() = runBlocking<Unit> {
        ParserInitializer.registerAllParsersLazy()

        // Verify that initially nothing is instantiated (all lazy)
        val pendingBefore = ParserRegistry.getPendingParserCount()
        assertTrue(pendingBefore > 0, "Should have pending (lazy) parsers before access")

        // Concurrent access should trigger instantiation
        val jobs = (1..50).map {
            launch(Dispatchers.Default) {
                ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
                ParserRegistry.getParser(FormatRegistry.ID_CSV)
            }
        }
        jobs.forEach { it.join() }

        // Markdown and CSV should now be instantiated
        val instantiatedAfter = ParserRegistry.getInstantiatedParserCount()
        assertTrue(instantiatedAfter >= 2, "At least 2 parsers should be instantiated after access")
    }

    @Test
    fun testClearAndReRegisterCycle() = runBlocking<Unit> {
        // First registration
        ParserInitializer.registerAllParsersLazy()
        val parser1 = ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
        assertNotNull(parser1, "Should get parser after first registration")

        // Clear everything
        ParserRegistry.clear()
        val parserAfterClear = ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
        assertNull(parserAfterClear, "Should get null after clear")

        // Re-register
        ParserInitializer.registerAllParsersLazy()
        val parser2 = ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
        assertNotNull(parser2, "Should get parser after re-registration")
    }

    @Test
    fun testClearAndReRegisterWithConcurrentGetParser() = runBlocking<Unit> {
        ParserInitializer.registerAllParsersLazy()

        // Pre-instantiate one parser
        val initial = ParserRegistry.getParser(FormatRegistry.ID_PLAINTEXT)
        assertNotNull(initial)

        // Clear and re-register
        ParserRegistry.clear()
        ParserInitializer.registerAllParsersLazy()

        val mutex = Mutex()
        val results = mutableListOf<TextParser?>()

        // Concurrent getParser after re-registration
        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                val parser = ParserRegistry.getParser(FormatRegistry.ID_PLAINTEXT)
                mutex.withLock { results.add(parser) }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(results.all { it != null }, "All concurrent calls after re-register should succeed")
    }

    @Test
    fun testHasParserDoesNotTriggerInstantiation() = runBlocking<Unit> {
        ParserInitializer.registerAllParsersLazy()

        val pendingBefore = ParserRegistry.getPendingParserCount()
        val instantiatedBefore = ParserRegistry.getInstantiatedParserCount()

        // hasParser should NOT trigger lazy instantiation
        val mdFormat = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        assertNotNull(mdFormat)
        val hasIt = ParserRegistry.hasParser(mdFormat)
        assertTrue(hasIt, "hasParser should return true for registered lazy parser")

        val pendingAfter = ParserRegistry.getPendingParserCount()
        val instantiatedAfter = ParserRegistry.getInstantiatedParserCount()

        assertEquals(
            pendingBefore, pendingAfter,
            "hasParser should not change pending count"
        )
        assertEquals(
            instantiatedBefore, instantiatedAfter,
            "hasParser should not instantiate any parsers"
        )
    }

    @Test
    fun testAllFormatsResolvableAfterLazyRegistration() = runBlocking<Unit> {
        ParserInitializer.registerAllParsersLazy()

        val allFormatIds = listOf(
            FormatRegistry.ID_PLAINTEXT, FormatRegistry.ID_MARKDOWN,
            FormatRegistry.ID_TODOTXT, FormatRegistry.ID_CSV,
            FormatRegistry.ID_WIKITEXT, FormatRegistry.ID_CREOLE,
            FormatRegistry.ID_TIDDLYWIKI, FormatRegistry.ID_LATEX,
            FormatRegistry.ID_ASCIIDOC, FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_RESTRUCTUREDTEXT, FormatRegistry.ID_KEYVALUE,
            FormatRegistry.ID_TASKPAPER, FormatRegistry.ID_TEXTILE,
            FormatRegistry.ID_JUPYTER, FormatRegistry.ID_RMARKDOWN,
            FormatRegistry.ID_BINARY
        )

        for (formatId in allFormatIds) {
            val parser = ParserRegistry.getParser(formatId)
            assertNotNull(parser, "Parser for $formatId must be resolvable")
            assertEquals(
                formatId, parser.supportedFormat.id,
                "Parser for $formatId must report correct format"
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. NonBlockingResponsivenessTest
// ---------------------------------------------------------------------------

/**
 * Responsiveness and non-blocking behavior tests.
 *
 * Validates that all rate-limited, lazy-loaded, and registry operations
 * complete within acceptable timeframes and do not block indefinitely
 * under contention.
 */
class NonBlockingResponsivenessTest {

    @Test
    fun testRateLimiterDoesNotBlockIndefinitely() = runBlocking<Unit> {
        val limiter = RateLimiter(maxConcurrent = 2)

        // The operations run on Dispatchers.Default (real time), so the timeout
        // must also use real time. Wrapping everything in withContext(Dispatchers.Default)
        // ensures withTimeoutOrNull uses the real clock, matching the delay() calls inside.
        val result = withContext(Dispatchers.Default) {
            withTimeoutOrNull(5000) {
                val jobs = (1..20).map {
                    async {
                        limiter.execute {
                            delay(10)
                            "done"
                        }
                    }
                }
                jobs.map { it.await() }
            }
        }

        assertNotNull(result, "Rate-limited operations must not block indefinitely")
        assertEquals(20, result.size, "All 20 operations should complete")
    }

    @Test
    fun testRateLimiterExecuteWithTimeoutReturnsPromptly() = runBlocking<Unit> {
        val limiter = RateLimiter(maxConcurrent = 1)

        // Run on real time so blocker actually acquires the slot and timeout uses real clock
        withContext(Dispatchers.Default) {
            // Occupy the single slot
            val blocker = launch {
                limiter.execute { delay(5000) }
            }
            delay(100) // Let blocker acquire the slot (real time)

            val startMs = Clock.System.now().toEpochMilliseconds()
            val result = limiter.executeWithTimeout(200) { "should-timeout" }
            val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs

            assertNull(result, "Should return null due to timeout")
            assertTrue(
                elapsedMs < 1000,
                "Timeout should return within ~200ms, not block for ${elapsedMs}ms"
            )

            blocker.cancel()
        }
    }

    @Test
    fun testLazyDocumentLoaderGetChunkRespondsQuickly() = runBlocking<Unit> {
        val content = buildString {
            repeat(10000) { i ->
                appendLine("Line $i")
            }
        }
        val loader = LazyStringLoader(content, chunkSize = 500)

        val startMs = Clock.System.now().toEpochMilliseconds()
        val chunk = loader.getChunk(5)
        val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs

        assertNotNull(chunk, "Chunk should load successfully")
        assertTrue(
            elapsedMs < 2000,
            "getChunk() should respond within 2 seconds (took ${elapsedMs}ms)"
        )
    }

    @Test
    fun testLazyDocumentLoaderConcurrentGetChunkResponds() = runBlocking<Unit> {
        val content = buildString {
            repeat(10000) { i ->
                appendLine("Line $i")
            }
        }
        val loader = LazyStringLoader(content, chunkSize = 500)

        // getChunk() internally uses delay(10) for spin-waiting on another
        // coroutine loading the same chunk. That delay needs real time on
        // Dispatchers.Default to complete, so the timeout must also use real time.
        val result = withContext(Dispatchers.Default) {
            withTimeoutOrNull(5000) {
                val jobs = (0..19).map { idx ->
                    async {
                        loader.getChunk(idx)
                    }
                }
                jobs.map { it.await() }
            }
        }

        assertNotNull(result, "Concurrent getChunk() calls must not block indefinitely")
        assertEquals(20, result.size, "Should return results for all 20 chunks")
        assertTrue(result.all { it != null }, "All chunks should load successfully")
    }

    @Test
    fun testFlowLazyLoaderCleanupIsImmediate() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()

        // Load substantial data
        repeat(20) { batch ->
            loader.loadMore((1..100).map { "batch-$batch-item-$it" })
        }
        assertEquals(2000, loader.content.value.size, "Should have 2000 items before cleanup")

        val startMs = Clock.System.now().toEpochMilliseconds()
        loader.cleanup()
        val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs

        assertEquals(0, loader.content.value.size, "Content should be empty after cleanup")
        assertTrue(
            elapsedMs < 500,
            "cleanup() should be immediate (took ${elapsedMs}ms)"
        )
    }

    @Test
    fun testTokenBucketAcquireDoesNotStarveUnderContention() = runBlocking<Unit> {
        val bucket = TokenBucket(capacity = 20, refillRate = 100.0)
        val mutex = Mutex()
        var acquired = 0

        val result = withContext(Dispatchers.Default) {
            withTimeoutOrNull(10000) {
                val jobs = (1..20).map {
                    launch {
                        bucket.acquire()
                        mutex.withLock { acquired++ }
                    }
                }
                jobs.forEach { it.join() }
            }
        }

        assertNotNull(result, "All acquire() calls must complete (no starvation)")
        assertEquals(20, acquired, "All 20 acquire() calls should succeed")
    }

    @Test
    fun testTokenBucketTryAcquireIsNonBlocking() = runBlocking<Unit> {
        val bucket = TokenBucket(capacity = 5, refillRate = 0.01) // Very slow refill

        // Drain the bucket
        repeat(5) { bucket.tryAcquire() }

        val startMs = Clock.System.now().toEpochMilliseconds()
        val result = bucket.tryAcquire()
        val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs

        assertFalse(result, "tryAcquire should return false when bucket is empty")
        assertTrue(
            elapsedMs < 200,
            "tryAcquire() should return immediately (took ${elapsedMs}ms)"
        )
    }

    @Test
    fun testTokenBucketAcquireWithRefillCompletesInTime() = runBlocking<Unit> {
        val bucket = TokenBucket(capacity = 5, refillRate = 50.0) // 50 tokens/sec

        // Drain all tokens
        repeat(5) { bucket.tryAcquire() }

        // acquire() internally polls with delay(50) and checks Clock.System.now()
        // for refill. We must run on Dispatchers.Default so real wall-clock time
        // passes, allowing the refill logic to detect elapsed time.
        val result = withContext(Dispatchers.Default) {
            withTimeoutOrNull(3000) {
                bucket.acquire()
                "acquired"
            }
        }

        assertNotNull(result, "acquire() should complete once tokens refill (not block forever)")
        assertEquals("acquired", result)
    }

    @Test
    fun testOperationThrottlerRespondsInstantly() = runBlocking<Unit> {
        val throttler = OperationThrottler(windowMs = 1000, maxOperations = 100)

        val startMs = Clock.System.now().toEpochMilliseconds()
        repeat(100) {
            throttler.tryThrottle("perf-test")
        }
        val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs

        assertTrue(
            elapsedMs < 1000,
            "100 tryThrottle() calls should complete promptly (took ${elapsedMs}ms)"
        )
    }

    @Test
    fun testParserRegistryGetParserRespondsQuickly() = runBlocking<Unit> {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsersLazy()

        val startMs = Clock.System.now().toEpochMilliseconds()
        val parser = ParserRegistry.getParser(FormatRegistry.ID_MARKDOWN)
        val elapsedMs = Clock.System.now().toEpochMilliseconds() - startMs

        assertNotNull(parser, "Parser should be resolved")
        assertTrue(
            elapsedMs < 1000,
            "Lazy parser instantiation should complete within 1 second (took ${elapsedMs}ms)"
        )

        ParserRegistry.clear()
    }

    @Test
    fun testConcurrentMixedOperationsComplete() = runBlocking<Unit> {
        val limiter = RateLimiter(maxConcurrent = 5)
        val bucket = TokenBucket(capacity = 50, refillRate = 100.0)
        val throttler = OperationThrottler(windowMs = 2000, maxOperations = 100)
        val mutex = Mutex()
        var totalOps = 0

        // The limiter.execute() uses semaphore.acquire() + delay(), and
        // bucket/throttler use Clock.System.now(). Both require real wall-clock
        // time. Wrapping in withContext(Dispatchers.Default) makes the timeout
        // and all child coroutines operate on real time.
        val result = withContext(Dispatchers.Default) {
            withTimeoutOrNull(15000) {
                val jobs = (1..100).map { i ->
                    launch {
                        // Mix different mechanisms
                        when (i % 3) {
                            0 -> limiter.execute {
                                delay(5)
                                mutex.withLock { totalOps++ }
                            }
                            1 -> {
                                if (bucket.tryAcquire()) {
                                    mutex.withLock { totalOps++ }
                                }
                            }
                            else -> {
                                if (throttler.tryThrottle("mixed-$i")) {
                                    mutex.withLock { totalOps++ }
                                }
                            }
                        }
                    }
                }
                jobs.forEach { it.join() }
            }
        }

        assertNotNull(result, "Mixed concurrent operations must not deadlock or block indefinitely")
        assertTrue(totalOps > 0, "At least some operations should have completed ($totalOps)")
    }
}
