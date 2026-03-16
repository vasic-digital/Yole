/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Memory Leak Regression Tests
 *
 * Validates scope cleanup after disconnect, resource
 * closure verification, cache bounds enforcement, and
 * no leaked coroutine scopes.
 *
 *########################################################*/
package digital.vasic.yole.safety

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.network.common.CircuitBreaker
import digital.vasic.yole.network.common.ConnectionLimiter
import kotlinx.coroutines.*
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Regression tests for memory leaks: scope cleanup, resource closure,
 * cache bounds enforcement, and coroutine scope lifecycle.
 */
class MemoryLeakRegressionTest {

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
    // 1. Scope cleanup after disconnect
    // ====================================================================

    @Test
    fun `coroutineScope cancellation stops child coroutines`() = runBlocking<Unit> {
        val scope = CoroutineScope(Dispatchers.Default + Job())
        var childCompleted = false

        scope.launch {
            delay(5.seconds)
            childCompleted = true
        }

        // Cancel scope immediately
        scope.cancel()
        delay(100.milliseconds)

        assertFalse(childCompleted, "Child coroutine should not complete after scope cancellation")
    }

    @Test
    fun `cancelled scope does not accept new work`() = runBlocking<Unit> {
        val scope = CoroutineScope(Dispatchers.Default + Job())
        scope.cancel()

        val job = scope.launch {
            delay(100.milliseconds)
        }

        // Job launched on cancelled scope should be cancelled
        delay(50.milliseconds)
        assertTrue(job.isCancelled || job.isCompleted,
            "Job on cancelled scope should be cancelled or completed immediately")
    }

    @Test
    fun `scope with SupervisorJob isolates child failures`() = runBlocking<Unit> {
        // Install exception handler to catch deliberate failures without leaking to test runner
        val handler = CoroutineExceptionHandler { _, _ -> /* deliberately swallowed */ }
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + handler)
        var secondChildCompleted = false

        val failingChild = scope.launch {
            throw RuntimeException("deliberate failure")
        }

        val goodChild = scope.launch {
            delay(50.milliseconds)
            secondChildCompleted = true
        }

        delay(200.milliseconds)
        assertTrue(failingChild.isCompleted, "Failing child should complete")
        assertTrue(secondChildCompleted, "Second child should not be affected by sibling failure")

        scope.cancel()
    }

    // ====================================================================
    // 2. Resource closure verification
    // ====================================================================

    @Test
    fun `DocumentCache clear releases all entries`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val format = TextFormat(id = "plaintext", name = "Plain Text", defaultExtension = ".txt")

        // Fill cache
        repeat(50) { i ->
            cache.put("key_$i", ParsedDocument(
                format = format,
                rawContent = "x".repeat(1000),
                parsedContent = "x".repeat(1000)
            ))
        }
        assertEquals(50, cache.size)

        cache.clear()
        assertEquals(0, cache.size, "Cache should be empty after clear")
        assertEquals(0L, cache.hits, "Hits should be reset")
        assertEquals(0L, cache.misses, "Misses should be reset")
    }

    @Test
    fun `ParsedDocument clearHtmlCache releases cached HTML`() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Large Document\n" + "Content. ".repeat(500))

        doc.toHtml(lightMode = true)
        doc.toHtml(lightMode = false)
        assertTrue(doc.hasHtmlCached(lightMode = true))
        assertTrue(doc.hasHtmlCached(lightMode = false))

        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(lightMode = true),
            "Light HTML should be cleared")
        assertFalse(doc.hasHtmlCached(lightMode = false),
            "Dark HTML should be cleared")
    }

    @Test
    fun `ParserRegistry clear releases all parser references`() {
        ParserRegistry.clear()
        assertEquals(0, ParserRegistry.getInstantiatedParserCount(),
            "No parsers should remain after clear")
        assertEquals(0, ParserRegistry.getPendingParserCount(),
            "No factories should remain after clear")

        // Re-register for subsequent tests
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun `StyleSheets clearCache releases all cached stylesheets`() {
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode = false)
        assertTrue(StyleSheets.cacheSize >= 2)

        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize, "StyleSheets cache should be empty after clear")
    }

    // ====================================================================
    // 3. Cache bounds enforcement over time
    // ====================================================================

    @Test
    fun `DocumentCache never exceeds maxSize over 500 inserts`() = runBlocking<Unit> {
        val maxSize = 20
        val cache = DocumentCache(maxSize = maxSize)
        val format = TextFormat(id = "plaintext", name = "Plain Text", defaultExtension = ".txt")

        repeat(500) { i ->
            cache.put("key_$i", ParsedDocument(
                format = format,
                rawContent = "content_$i",
                parsedContent = "content_$i"
            ))
            assertTrue(cache.size <= maxSize,
                "Cache size ${cache.size} exceeded maxSize $maxSize at iteration $i")
        }
        assertEquals(maxSize, cache.size, "Cache should stabilize at maxSize")
    }

    @Test
    fun `DocumentCache with maxSize 1 never holds more than 1 entry`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 1)
        val format = TextFormat(id = "plaintext", name = "Plain Text", defaultExtension = ".txt")

        repeat(100) { i ->
            cache.put("key_$i", ParsedDocument(
                format = format,
                rawContent = "content_$i",
                parsedContent = "content_$i"
            ))
            assertEquals(1, cache.size, "maxSize=1 cache should always have 1 entry")
        }
    }

    @Test
    fun `DocumentCache eviction preserves recent entries`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 5)
        val format = TextFormat(id = "plaintext", name = "Plain Text", defaultExtension = ".txt")

        // Insert 10 entries
        repeat(10) { i ->
            cache.put("key_$i", ParsedDocument(
                format = format,
                rawContent = "content_$i",
                parsedContent = "content_$i"
            ))
        }

        // Only last 5 should remain
        assertEquals(5, cache.size)
        for (i in 5..9) {
            assertNotNull(cache.get("key_$i"), "Recent entry key_$i should exist")
        }
        for (i in 0..4) {
            // These gets will record misses since entries were evicted
            val result = cache.get("key_$i")
            assertNull(result, "Old entry key_$i should have been evicted")
        }
    }

    // ====================================================================
    // 4. No leaked coroutine scopes
    // ====================================================================

    @Test
    fun `CircuitBreaker does not leak coroutines after many calls`() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100)

        repeat(200) { i ->
            if (i % 3 == 0) {
                cb.execute { throw RuntimeException("fail") }
            } else {
                cb.execute { i * 2 }
            }
        }

        // After all calls, state should be deterministic
        assertEquals(200L, cb.calls)
        // No hanging or leaked scope - if we get here, the test passes
    }

    @Test
    fun `ConnectionLimiter does not leak permits after many operations`() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds)

        repeat(100) { i ->
            try {
                limiter.withConnection {
                    if (i % 5 == 0) throw RuntimeException("fail $i")
                    i
                }
            } catch (_: RuntimeException) { }
        }

        assertEquals(3, limiter.availablePermits,
            "All permits should be restored after 100 operations with failures")
    }

    @Test
    fun `concurrent scope creation and cancellation does not leak`() = runBlocking<Unit> {
        val scopes = (1..20).map {
            CoroutineScope(Dispatchers.Default + Job())
        }

        // Launch work in each scope
        val jobs = scopes.mapIndexed { index, scope ->
            scope.launch {
                delay(50.milliseconds)
                // Simulate some work
                FormatRegistry.detectByExtension("md")
            }
        }

        // Cancel half the scopes
        scopes.take(10).forEach { it.cancel() }

        // Wait for the rest
        delay(200.milliseconds)

        // Cancel remaining
        scopes.drop(10).forEach { it.cancel() }

        // If we reach here without hanging, no scopes leaked
    }

    @Test
    fun `DocumentCache concurrent put and clear does not leak`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)
        val format = TextFormat(id = "plaintext", name = "Plain Text", defaultExtension = ".txt")

        val jobs = (1..30).map { i ->
            async {
                if (i % 10 == 0) {
                    cache.clear()
                } else {
                    cache.put("key_$i", ParsedDocument(
                        format = format,
                        rawContent = "c_$i",
                        parsedContent = "c_$i"
                    ))
                }
            }
        }
        jobs.awaitAll()

        assertTrue(cache.size <= 10,
            "Cache size ${cache.size} should not exceed maxSize 10 after concurrent put+clear")
    }
}
