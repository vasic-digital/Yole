/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Non-Blocking Operation Tests
 *
 * Verifies that format parsing, DocumentCache, ConnectionLimiter,
 * and CircuitBreaker operations are truly suspending (non-blocking),
 * do not deadlock under concurrent use, and respect coroutine
 * cancellation throughout.
 *
 *########################################################*/
package digital.vasic.yole.format.nonblocking

import digital.vasic.yole.format.*
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.network.common.CircuitBreaker
import digital.vasic.yole.network.common.CircuitBreakerOpenException
import digital.vasic.yole.network.common.ConnectionLimiter
import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Non-blocking operation tests verifying that suspending components
 * (DocumentCache, ConnectionLimiter, CircuitBreaker) do not block
 * threads and respect coroutine cancellation.
 *
 * 25+ test methods covering suspension behavior, deadlock avoidance,
 * and cancellation propagation.
 */
class NonBlockingOperationTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // ====================================================================
    // 1. DocumentCache operations are suspending (mutex-based)
    // ====================================================================

    @Test
    fun `DocumentCache get suspends via mutex not blocks`() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )
        val doc = ParsedDocument(
            format = testFormat, rawContent = "test",
            parsedContent = "test"
        )
        cache.put("key", doc)

        // If this were blocking instead of suspending, running many concurrent
        // gets in a single-threaded test dispatcher would deadlock
        val results = (1..100).map { async { cache.get("key") } }.awaitAll()
        assertEquals(100, results.size)
        assertTrue(results.all { it != null })
    }

    @Test
    fun `DocumentCache put suspends via mutex not blocks`() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )

        // Concurrent puts should not block the dispatcher
        val jobs = (1..50).map { i ->
            async {
                val doc = ParsedDocument(
                    format = testFormat,
                    rawContent = "content-$i",
                    parsedContent = "content-$i"
                )
                cache.put("key-$i", doc)
            }
        }
        jobs.awaitAll()
        assertEquals(50, cache.size)
    }

    @Test
    fun `DocumentCache concurrent read-write does not deadlock`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 20)
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )

        // Mix of reads, writes, invalidations, and contains checks
        val jobs = (1..100).map { i ->
            async {
                when (i % 5) {
                    0 -> {
                        val doc = ParsedDocument(
                            format = testFormat,
                            rawContent = "w-$i",
                            parsedContent = "w-$i"
                        )
                        cache.put("key-${i % 20}", doc)
                    }
                    1 -> cache.get("key-${i % 20}")
                    2 -> cache.contains("key-${i % 20}")
                    3 -> cache.invalidate("key-${i % 20}")
                    else -> cache.clear()
                }
            }
        }
        // Must complete without deadlock within the test timeout
        jobs.awaitAll()
    }

    @Test
    fun `DocumentCache clear does not block concurrent reads`() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )

        // Pre-populate
        for (i in 1..10) {
            cache.put("key-$i", ParsedDocument(
                format = testFormat, rawContent = "c-$i", parsedContent = "c-$i"
            ))
        }

        // Concurrent clears and reads should not deadlock
        val results = (1..50).map { i ->
            async {
                if (i % 10 == 0) {
                    cache.clear()
                    "cleared"
                } else {
                    val result = cache.get("key-${i % 10}")
                    result?.rawContent ?: "miss"
                }
            }
        }.awaitAll()

        assertEquals(50, results.size)
    }

    // ====================================================================
    // 2. ConnectionLimiter suspends (not blocks) when permits exhausted
    // ====================================================================

    @Test
    fun `ConnectionLimiter suspends when all permits taken`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 5.seconds)
        val order = mutableListOf<String>()

        val job1 = async {
            limiter.withConnection {
                order.add("job1-start")
                delay(50.milliseconds)
                order.add("job1-end")
                "first"
            }
        }

        // Give job1 a chance to acquire the permit
        delay(10.milliseconds)

        val job2 = async {
            limiter.withConnection {
                order.add("job2-start")
                "second"
            }
        }

        val results = listOf(job1, job2).awaitAll()

        // job2 should have waited (suspended) for job1 to release the permit
        assertTrue(order.indexOf("job1-end") < order.indexOf("job2-start"),
            "job2 should start after job1 ends. Order: $order")
        assertEquals("first", results[0])
        assertEquals("second", results[1])
    }

    @Test
    fun `ConnectionLimiter does not block when permits available`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 10)
        val completed = mutableListOf<Int>()

        val jobs = (1..10).map { i ->
            async {
                limiter.withConnection {
                    completed.add(i)
                    i
                }
            }
        }
        val results = jobs.awaitAll()

        assertEquals(10, completed.size, "All 10 jobs should complete")
        assertEquals(10, results.size)
    }

    @Test
    fun `ConnectionLimiter releases permit on exception`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 5.seconds)

        // First call throws
        try {
            limiter.withConnection { throw RuntimeException("fail") }
        } catch (_: RuntimeException) { }

        // Permit should be released; second call succeeds
        val result = limiter.withConnection { "success" }
        assertEquals("success", result)
        assertEquals(1, limiter.availablePermits)
    }

    @Test
    fun `ConnectionLimiter concurrent beyond limit does not deadlock`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 10.seconds)
        val results = mutableListOf<Int>()

        // 10 concurrent tasks but only 2 permits
        val jobs = (1..10).map { i ->
            async {
                limiter.withConnection {
                    delay(10.milliseconds)
                    results.add(i)
                    i
                }
            }
        }
        jobs.awaitAll()

        assertEquals(10, results.size, "All 10 tasks must complete despite limited permits")
    }

    @Test
    fun `ConnectionLimiter available permits restored after use`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3)
        assertEquals(3, limiter.availablePermits)

        limiter.withConnection { "op1" }
        assertEquals(3, limiter.availablePermits, "Permit should be restored after operation")

        limiter.withConnection { "op2" }
        assertEquals(3, limiter.availablePermits)
    }

    // ====================================================================
    // 3. CircuitBreaker.execute() suspends during state transitions
    // ====================================================================

    @Test
    fun `CircuitBreaker execute suspends not blocks`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100)

        // Many concurrent calls through the circuit breaker
        val results = (1..100).map { i ->
            async { cb.execute { i * 2 } }
        }.awaitAll()

        assertEquals(100, results.size)
        assertTrue(results.all { it.isSuccess })
        assertEquals(100, cb.calls)
    }

    @Test
    fun `CircuitBreaker OPEN state returns immediately without blocking`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        cb.execute { throw RuntimeException("open it") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Open circuit should reject immediately, not block
        val elapsed = measureTime {
            repeat(100) {
                cb.execute { "should be rejected" }
            }
        }

        // 100 rejections should be nearly instant
        assertTrue(elapsed.inWholeMilliseconds < 500,
            "100 open rejections took ${elapsed.inWholeMilliseconds}ms, should be fast")
    }

    @Test
    fun `CircuitBreaker concurrent calls do not deadlock`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 50)

        val results = (1..50).map { i ->
            async {
                if (i % 5 == 0) {
                    cb.execute { throw RuntimeException("fail-$i") }
                } else {
                    cb.execute { "ok-$i" }
                }
            }
        }.awaitAll()

        assertEquals(50, results.size)
        assertEquals(50, cb.calls)
    }

    @Test
    fun `CircuitBreaker half-open transition does not deadlock`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 1.milliseconds)
        cb.execute { throw RuntimeException("open") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Wait for reset timeout
        delay(10.milliseconds)

        // This call transitions to HALF_OPEN then executes
        val result = cb.execute { "recovered" }
        assertTrue(result.isSuccess)
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
    }

    // ====================================================================
    // 4. Format parsing does not block the calling coroutine
    // ====================================================================

    @Test
    fun `concurrent parsing across formats does not block`() = runBlocking<Unit> {
        val parsers = listOf(
            MarkdownParser(),
            CsvParser(),
            LatexParser(),
            OrgModeParser(),
            PlaintextParser()
        )

        val contents = listOf(
            "# Heading\n\n**Bold** text.\n- Item 1\n- Item 2",
            "name,value\nAlice,30\nBob,25",
            "\\documentclass{article}\n\\begin{document}\nHello.\n\\end{document}",
            "* Heading\n** DONE Task\nContent.",
            "Just plain text content here."
        )

        val results = parsers.zip(contents).flatMap { (parser, content) ->
            (1..10).map {
                async { parser.parse(content) }
            }
        }.awaitAll()

        assertEquals(50, results.size)
        assertTrue(results.all { it.rawContent.isNotEmpty() })
    }

    @Test
    fun `parsing does not starve other coroutines`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val content = "# Test\n\n" + "Paragraph text. ".repeat(100)
        var counterRan = false

        // Launch a parser and a simple counter coroutine
        val parseJob = async { parser.parse(content) }
        val counterJob = async {
            // This should run even while parsing is happening
            counterRan = true
            42
        }

        parseJob.await()
        counterJob.await()

        assertTrue(counterRan, "Counter coroutine should have run alongside parsing")
    }

    @Test
    fun `multiple concurrent parse operations do not deadlock`() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val documents = (1..50).map { i ->
            "# Document $i\n\nContent for document $i with **bold** text."
        }

        val results = documents.map { doc ->
            async { parser.parse(doc) }
        }.awaitAll()

        assertEquals(50, results.size)
        assertTrue(results.all { it.format.id == FormatRegistry.ID_MARKDOWN })
    }

    // ====================================================================
    // 5. Coroutine cancellation is properly respected
    // ====================================================================

    @Test
    fun `DocumentCache respects cancellation during get`() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )
        cache.put("key", ParsedDocument(
            format = testFormat, rawContent = "data", parsedContent = "data"
        ))

        val job = launch {
            repeat(10000) {
                cache.get("key")
            }
        }

        // Cancel partway through
        delay(5.milliseconds)
        job.cancelAndJoin()

        // If cancellation is not respected, the job would never end
        assertTrue(job.isCancelled, "Job should be cancelled")
    }

    @Test
    fun `ConnectionLimiter respects cancellation while waiting`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 60.seconds)

        // First job holds the permit for a while
        val holdingJob = async {
            limiter.withConnection {
                delay(500.milliseconds)
                "held"
            }
        }

        // Give holding job time to acquire permit
        delay(10.milliseconds)

        // Second job will suspend waiting for permit
        val waitingJob = async {
            limiter.withConnection { "waited" }
        }

        // Cancel the waiting job
        delay(10.milliseconds)
        waitingJob.cancel()

        holdingJob.await()

        assertTrue(waitingJob.isCancelled, "Waiting job should be cancelled")
    }

    @Test
    fun `CircuitBreaker respects CancellationException`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100)

        val job = launch {
            repeat(10000) {
                cb.execute { delay(1.milliseconds); "result" }
            }
        }

        delay(10.milliseconds)
        job.cancelAndJoin()

        assertTrue(job.isCancelled, "Circuit breaker job should be cancellable")
    }

    @Test
    fun `CircuitBreaker does not count CancellationException as failure`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)

        val job = launch {
            cb.execute {
                delay(100.milliseconds) // Will be cancelled
                "result"
            }
        }

        delay(10.milliseconds)
        job.cancelAndJoin()

        // CancellationException should be rethrown, not counted as failure
        assertEquals(CircuitBreaker.State.CLOSED, cb.state,
            "Circuit should remain CLOSED; CancellationException is not a failure")
    }

    @Test
    fun `DocumentCache operations complete under single-threaded dispatcher`() = runBlocking<Unit> {
        // runTest uses a single-threaded test dispatcher.
        // If any cache operation is blocking (not suspending), this will deadlock.
        val cache = DocumentCache(maxSize = 5)
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )

        for (i in 1..10) {
            cache.put("k$i", ParsedDocument(
                format = testFormat, rawContent = "v$i", parsedContent = "v$i"
            ))
        }

        // LRU eviction should have occurred
        assertTrue(cache.size <= 5, "Cache should respect maxSize")

        for (i in 6..10) {
            val doc = cache.get("k$i")
            assertNotNull(doc, "Recent entries should still be present")
        }
    }

    @Test
    fun `concurrent parse and toHtml do not deadlock`() = runBlocking<Unit> {
        val parser = TodoTxtParser()
        val content = "(A) Important task @work +project1\n(B) Another @home +project2"

        val results = (1..20).map { i ->
            async {
                val doc = parser.parse(content)
                val html = doc.toHtml(lightMode = i % 2 == 0)
                html.isNotEmpty()
            }
        }.awaitAll()

        assertEquals(20, results.size)
        assertTrue(results.all { it }, "All parse+toHtml operations should succeed")
    }

    @Test
    fun `DocumentCache invalidation during concurrent reads completes`() = runBlocking<Unit> {
        val cache = DocumentCache()
        val testFormat = TextFormat(
            id = "plaintext", name = "Plain Text",
            defaultExtension = ".txt", extensions = listOf(".txt")
        )

        // Pre-populate
        for (i in 1..20) {
            cache.put("key-$i", ParsedDocument(
                format = testFormat, rawContent = "data-$i", parsedContent = "data-$i"
            ))
        }

        // Concurrent reads and invalidations
        val results = (1..50).map { i ->
            async {
                if (i % 7 == 0) {
                    cache.invalidate("key-${i % 20 + 1}")
                    "invalidated"
                } else {
                    cache.get("key-${i % 20 + 1}")?.rawContent ?: "miss"
                }
            }
        }.awaitAll()

        assertEquals(50, results.size, "All operations must complete")
    }

    @Test
    fun `ConnectionLimiter withConnection completes normally for all callers`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 10.seconds)
        val completed = mutableListOf<Int>()

        val jobs = (1..15).map { i ->
            async {
                limiter.withConnection {
                    delay(5.milliseconds)
                    completed.add(i)
                    i
                }
            }
        }
        val results = jobs.awaitAll()

        assertEquals(15, results.size)
        assertEquals(15, completed.size, "All 15 callers must eventually complete")
    }

    @Test
    fun `CircuitBreaker reset does not block concurrent execute calls`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100)

        // Mix of execute calls and resets
        val results = (1..30).map { i ->
            async {
                if (i % 10 == 0) {
                    cb.reset()
                    "reset"
                } else {
                    val r = cb.execute { "ok-$i" }
                    r.getOrNull() ?: "fail"
                }
            }
        }.awaitAll()

        assertEquals(30, results.size, "All operations must complete without deadlock")
    }
}
