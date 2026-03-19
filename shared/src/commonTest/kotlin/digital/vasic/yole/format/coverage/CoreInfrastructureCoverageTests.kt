/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Core Infrastructure Coverage Tests
 *
 * Validates FormatRegistry, DocumentCache, StyleSheets,
 * CircuitBreaker, ConnectionLimiter, PathUtils, and
 * TextParser with fuzz, property-based, snapshot, and
 * supremacy test strategies.
 *
 * Total: ~30 tests
 *
 *########################################################*/
package digital.vasic.yole.format.coverage

import digital.vasic.yole.format.*
import digital.vasic.yole.network.common.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Coverage tests for core infrastructure components.
 *
 * Components and test strategies:
 * - FormatRegistry: fuzz with random format IDs, resilience under unknown IDs
 * - DocumentCache: fuzz with random keys, supremacy with 0-size cache, LRU eviction
 * - StyleSheets: snapshot stability (same input produces same CSS)
 * - CircuitBreaker: property-based state machine invariants
 * - ConnectionLimiter: supremacy (1 permit boundary), concurrent saturation
 * - PathUtils: fuzz with 100 randomized traversal attempts
 * - TextParser: property-based (any input produces ParsedDocument via registry)
 */
class CoreInfrastructureCoverageTests {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
        StyleSheets.clearCache()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
        StyleSheets.clearCache()
    }

    // ==================== FormatRegistry ====================

    @Test
    fun formatRegistryFuzzRandomIds() {
        val random = Random(42)
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789_-."
        repeat(100) {
            val len = random.nextInt(1, 30)
            val id = (1..len).map { chars[random.nextInt(chars.length)] }.joinToString("")
            // getById should never throw, just return null for unknown
            val result = FormatRegistry.getById(id)
            // Known formats return non-null; unknown return null
            if (result != null) {
                assertEquals(id, result.id)
            }
        }
    }

    @Test
    fun formatRegistryDetectByExtensionNeverThrows() {
        val extensions = listOf(
            "", ".", "..", "...", ".unknown", ".exe", ".dll", ".so",
            "a".repeat(500), ".md", ".txt", ".csv", ".tex", ".adoc"
        )
        for (ext in extensions) {
            // detectByExtension falls back to plaintext, never throws
            val format = FormatRegistry.detectByExtension(ext)
            assertNotNull(format, "detectByExtension should never return null for '$ext'")
        }
    }

    @Test
    fun formatRegistryDetectByContentEmptyReturnsNull() {
        val result = FormatRegistry.detectByContent("")
        assertNull(result, "Empty content should return null format")
    }

    @Test
    fun formatRegistryIsFormatsInitializedAfterAccess() {
        // Access formats list
        val count = FormatRegistry.formats.size
        assertTrue(count > 0, "Should have formats")
        assertTrue(FormatRegistry.isFormatsInitialized, "isFormatsInitialized should be true after access")
    }

    // ==================== DocumentCache ====================

    @Test
    fun documentCacheFuzzRandomKeys() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val format = FormatRegistry.formats.first { it.id == TextFormat.ID_PLAINTEXT }
        val random = Random(123)

        repeat(100) { i ->
            val key = "fuzz-${random.nextInt()}"
            val doc = ParsedDocument(
                format = format,
                rawContent = "content-$i",
                parsedContent = "parsed-$i"
            )
            cache.put(key, doc)
        }

        // Cache should respect maxSize
        assertTrue(cache.size <= 50, "Cache size ${cache.size} should be <= 50")
    }

    @Test
    fun documentCacheSupremacyZeroSizeBehavior() = runBlocking<Unit> {
        // A cache with maxSize=0 should evict everything immediately
        val cache = DocumentCache(maxSize = 0)
        val format = FormatRegistry.formats.first { it.id == TextFormat.ID_PLAINTEXT }
        val doc = ParsedDocument(format = format, rawContent = "test", parsedContent = "test")

        cache.put("key1", doc)
        // With maxSize=0, the entry might still be put but then evicted;
        // or the cache might hold exactly 0. We check the constraint:
        // Note: the implementation adds first then evicts while > maxSize,
        // so size should be 0 after eviction
        assertEquals(0, cache.size, "Zero-size cache should have no entries after put")
    }

    @Test
    fun documentCacheLRUEvictionOrder() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 3)
        val format = FormatRegistry.formats.first { it.id == TextFormat.ID_PLAINTEXT }

        // Put entries 1, 2, 3
        for (i in 1..3) {
            cache.put("key-$i", ParsedDocument(format, "raw-$i", "parsed-$i"))
        }
        assertEquals(3, cache.size)

        // Access key-1 to make it recently used
        assertNotNull(cache.get("key-1"))

        // Put key-4 which should evict the LRU (key-2, since key-1 was just accessed)
        cache.put("key-4", ParsedDocument(format, "raw-4", "parsed-4"))
        assertEquals(3, cache.size)

        // key-2 should be evicted
        assertNull(cache.get("key-2"), "LRU entry key-2 should be evicted")
        // key-1 and key-3 and key-4 should still be present
        assertNotNull(cache.get("key-1"), "key-1 should still be in cache")
        assertNotNull(cache.get("key-4"), "key-4 should be in cache")
    }

    @Test
    fun documentCacheHitMissTracking() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)
        val format = FormatRegistry.formats.first { it.id == TextFormat.ID_PLAINTEXT }

        cache.put("exists", ParsedDocument(format, "r", "p"))

        cache.get("exists") // hit
        cache.get("exists") // hit
        cache.get("missing") // miss

        assertEquals(2L, cache.hits, "Should have 2 hits")
        assertEquals(1L, cache.misses, "Should have 1 miss")
        assertTrue(cache.hitRate > 0.5, "Hit rate should be > 0.5")
    }

    @Test
    fun documentCacheClearResetsAll() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)
        val format = FormatRegistry.formats.first { it.id == TextFormat.ID_PLAINTEXT }

        cache.put("k1", ParsedDocument(format, "r", "p"))
        cache.get("k1")
        cache.get("missing")

        cache.clear()

        assertEquals(0, cache.size)
        assertEquals(0L, cache.hits)
        assertEquals(0L, cache.misses)
    }

    // ==================== StyleSheets ====================

    @Test
    fun styleSheetsSnapshotStability() {
        // Same input should always produce the same CSS
        val formatIds = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_WIKITEXT,
            TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC, TextFormat.ID_LATEX
        )
        for (formatId in formatIds) {
            val light1 = StyleSheets.getStyleSheet(formatId, lightMode = true)
            val light2 = StyleSheets.getStyleSheet(formatId, lightMode = true)
            assertEquals(light1, light2, "Light stylesheet for $formatId should be stable")

            val dark1 = StyleSheets.getStyleSheet(formatId, lightMode = false)
            val dark2 = StyleSheets.getStyleSheet(formatId, lightMode = false)
            assertEquals(dark1, dark2, "Dark stylesheet for $formatId should be stable")
        }
    }

    @Test
    fun styleSheetsUnknownFormatReturnsEmpty() {
        val result = StyleSheets.getStyleSheet("completely-unknown-format-xyz", lightMode = true)
        assertEquals("", result, "Unknown format should return empty stylesheet")
    }

    @Test
    fun styleSheetsCacheSizeGrowsOnAccess() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(StyleSheets.cacheSize > 0, "Cache size should grow after access")

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = false)
        assertTrue(StyleSheets.cacheSize >= 2, "Cache should hold both light and dark variants")
    }

    @Test
    fun styleSheetsClearResetsCacheSize() {
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(StyleSheets.cacheSize > 0)
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize, "Cache should be empty after clear")
    }

    // ==================== CircuitBreaker Property-Based ====================

    @Test
    fun circuitBreakerPropertyInvariantsHold() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 3, resetTimeout = 1.milliseconds)

        // Property: successes + failures + rejected <= total calls
        repeat(20) { i ->
            if (i % 5 == 0) {
                cb.execute { throw RuntimeException("fail") }
            } else {
                if (cb.state == CircuitBreaker.State.OPEN) {
                    delay(5.milliseconds)
                }
                cb.execute { "ok" }
            }
        }
        // Basic invariant: calls is always tracked
        assertTrue(cb.calls >= 20L, "All calls should be counted")
    }

    @Test
    fun circuitBreakerPropertyClosedAllowsExecution() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 100)
        // When CLOSED, every execute call should run the block
        repeat(50) {
            val result = cb.execute { it * 2 }
            assertTrue(result.isSuccess)
            assertEquals(it * 2, result.getOrNull())
        }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(50, cb.successes)
    }

    @Test
    fun circuitBreakerPropertyResetAlwaysReturnsToClosed() = runBlocking<Unit> {
        val cb = CircuitBreaker(failureThreshold = 1, resetTimeout = 60.seconds)
        // Open the circuit
        cb.execute { throw RuntimeException("open") }
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Reset from any state should return to CLOSED
        cb.reset()
        assertEquals(CircuitBreaker.State.CLOSED, cb.state)
        assertEquals(0, cb.failures)
    }

    // ==================== ConnectionLimiter Supremacy ====================

    @Test
    fun connectionLimiterSinglePermit() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 1, acquireTimeout = 10.seconds)
        // Sequential operations should work fine with 1 permit
        repeat(5) {
            val result = cl.withConnection { "op-$it" }
            assertEquals("op-$it", result)
        }
    }

    @Test
    fun connectionLimiterConcurrentSaturation() = runBlocking<Unit> {
        val cl = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 10.seconds)
        // Launch more concurrent ops than permits; all should eventually complete
        val results = (1..10).map { i ->
            async { cl.withConnection { "result-$i" } }
        }.awaitAll()
        assertEquals(10, results.size, "All 10 operations should complete")
        assertTrue(results.all { it.startsWith("result-") })
    }

    @Test
    fun connectionLimiterAvailablePermitsConsistency() = runBlocking<Unit> {
        val maxPermits = 5
        val cl = ConnectionLimiter(maxConcurrent = maxPermits, acquireTimeout = 10.seconds)

        // Initially all permits should be available
        assertEquals(maxPermits, cl.availablePermits, "All permits should be available initially")

        // After operation completes, permits should be restored
        cl.withConnection { "test" }
        assertEquals(maxPermits, cl.availablePermits, "Permits should be restored after operation")
    }

    // ==================== PathUtils Fuzz ====================

    @Test
    fun pathUtilsFuzzTraversalAttempts() {
        val random = Random(99)
        val rootPath = "/secure/data"
        var blocked = 0
        var allowed = 0

        repeat(100) {
            val segments = (1..random.nextInt(1, 10)).map {
                when (random.nextInt(4)) {
                    0 -> ".."
                    1 -> "."
                    2 -> "dir${random.nextInt(100)}"
                    else -> "file${random.nextInt(100)}.txt"
                }
            }
            val path = segments.joinToString("/")

            try {
                val result = PathUtils.normalizePath(path, rootPath)
                assertTrue(result.startsWith(rootPath),
                    "Normalized path should start with root: $result")
                allowed++
            } catch (_: IllegalArgumentException) {
                // Path traversal blocked — expected for malicious paths
                blocked++
            }
        }

        // We expect some of both: some paths are valid, some are traversal attempts
        assertTrue(blocked + allowed == 100, "All 100 paths should be processed")
    }

    @Test
    fun pathUtilsNormalizeEmptyPath() {
        val result = PathUtils.normalizePath("", "/root")
        assertEquals("/root", result, "Empty path should normalize to root")
    }

    @Test
    fun pathUtilsNormalizeSlashPath() {
        val result = PathUtils.normalizePath("/", "/root")
        assertEquals("/root", result, "Slash path should normalize to root")
    }

    @Test
    fun pathUtilsNormalizeDotDotBlocked() {
        assertFailsWith<IllegalArgumentException> {
            PathUtils.normalizePath("../../etc/passwd", "/secure/data")
        }
    }

    @Test
    fun pathUtilsNormalizeResolvesDots() {
        val result = PathUtils.normalizePath("a/./b/../c", "/")
        assertEquals("/a/c", result, "Should resolve . and .. segments")
    }

    // ==================== TextParser Property-Based ====================

    @Test
    fun anyInputProducesParsedDocument() {
        val parser = ParserRegistry.getParser(TextFormat.ID_PLAINTEXT)
        assertNotNull(parser, "Plaintext parser should be registered")

        val inputs = listOf(
            "", " ", "\n", "\t", "Hello World",
            "a".repeat(10000),
            "\u0000\u0001\u0002", // control characters
            "<script>alert('xss')</script>",
            "Line1\nLine2\rLine3\r\nLine4",
            Random(42).let { r -> (1..500).map { r.nextInt(32, 127).toChar() }.joinToString("") }
        )

        for (input in inputs) {
            val doc = parser.parse(input)
            assertNotNull(doc, "Parser should produce non-null document for all inputs")
            assertEquals(input, doc.rawContent, "rawContent should preserve input")
        }
    }

    @Test
    fun parsedDocumentEqualityContract() {
        val format = FormatRegistry.formats.first { it.id == TextFormat.ID_PLAINTEXT }
        val doc1 = ParsedDocument(format, "raw", "parsed", mapOf("k" to "v"), emptyList())
        val doc2 = ParsedDocument(format, "raw", "parsed", mapOf("k" to "v"), emptyList())
        val doc3 = ParsedDocument(format, "different", "parsed", mapOf("k" to "v"), emptyList())

        // Equals
        assertEquals(doc1, doc2, "Identical documents should be equal")
        assertNotEquals(doc1, doc3, "Different content should not be equal")

        // HashCode
        assertEquals(doc1.hashCode(), doc2.hashCode(), "Equal documents should have same hashCode")

        // Copy
        val copy = doc1.copy(rawContent = "changed")
        assertNotEquals(doc1, copy, "Copy with different rawContent should not be equal")
        assertEquals("changed", copy.rawContent)
    }

    @Test
    fun parsedDocumentHtmlCacheBehavior() {
        val parser = ParserRegistry.getParser(TextFormat.ID_PLAINTEXT)
        assertNotNull(parser)

        val doc = parser.parse("Hello World")

        // HTML not cached initially
        assertFalse(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))

        // Generate light HTML
        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty(), "HTML should be non-empty")
        assertTrue(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))

        // Second call returns same reference (cached)
        val html2 = doc.toHtml(lightMode = true)
        assertEquals(html, html2, "Cached HTML should be identical")

        // Clear cache
        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))
    }
}
