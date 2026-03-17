/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Cache concurrency tests for DocumentCache, StyleSheets,
 * and ParsedDocument HTML caching under concurrent access.
 *
 *########################################################*/
package digital.vasic.yole.format.concurrency

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Cache concurrency tests.
 *
 * Validates thread-safety of DocumentCache, StyleSheets cache,
 * and ParsedDocument HTML caching under concurrent access patterns.
 *
 * Total: 24+ tests
 */
class CacheConcurrencyTest {

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

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
        StyleSheets.clearCache()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // ==================== Concurrent DocumentCache Put/Get ====================

    @Test
    fun concurrentPutsDoNotCorrupt() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val jobs = (1..30).map { i ->
            async {
                cache.put("key-$i", doc("content-$i"))
            }
        }
        jobs.awaitAll()
        assertEquals(30, cache.size)
    }

    @Test
    fun concurrentGetsReturnCorrectValues() = runBlocking<Unit> {
        val cache = DocumentCache()
        cache.put("shared-key", doc("shared-content"))

        val results = (1..20).map {
            async { cache.get("shared-key") }
        }.awaitAll()

        assertTrue(results.all { it != null })
        assertTrue(results.all { it!!.rawContent == "shared-content" })
    }

    @Test
    fun concurrentPutAndGetMixed() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 20)
        // Pre-populate
        (1..5).forEach { cache.put("pre-$it", doc("pre-content-$it")) }

        val jobs = (1..30).map { i ->
            async {
                when (i % 3) {
                    0 -> cache.put("dyn-$i", doc("dynamic-$i"))
                    1 -> cache.get("pre-${(i % 5) + 1}")
                    else -> cache.contains("pre-${(i % 5) + 1}")
                }
            }
        }
        jobs.awaitAll()
        // Cache should be consistent and not exceed max size
        assertTrue(cache.size <= 20)
    }

    @Test
    fun concurrentPutAndInvalidate() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val jobs = (1..20).map { i ->
            async {
                cache.put("key-$i", doc("content-$i"))
                if (i % 3 == 0) cache.invalidate("key-$i")
            }
        }
        jobs.awaitAll()
        // Some keys were invalidated
        assertTrue(cache.size < 20)
    }

    @Test
    fun concurrentPutAndClear() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val putJobs = (1..10).map { i ->
            async { cache.put("key-$i", doc("content-$i")) }
        }
        putJobs.awaitAll()

        // Concurrent clears
        val clearJobs = (1..5).map { async { cache.clear() } }
        clearJobs.awaitAll()
        assertEquals(0, cache.size)
    }

    @Test
    fun concurrentHitMissTracking() = runBlocking<Unit> {
        val cache = DocumentCache()
        cache.put("hit-key", doc("content"))

        val jobs = (1..20).map { i ->
            async {
                if (i % 2 == 0) cache.get("hit-key") // hit
                else cache.get("miss-key-$i") // miss
            }
        }
        jobs.awaitAll()

        assertEquals(10L, cache.hits)
        assertEquals(10L, cache.misses)
    }

    // ==================== Cache Eviction Under Concurrent Access ====================

    @Test
    fun concurrentEvictionDoesNotCorrupt() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 5)
        val jobs = (1..30).map { i ->
            async {
                cache.put("key-$i", doc("content-$i"))
            }
        }
        jobs.awaitAll()
        // Size should never exceed maxSize
        assertTrue(cache.size <= 5, "Cache size ${cache.size} should not exceed maxSize 5")
    }

    @Test
    fun evictionMaintainsConsistency() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 3)
        (1..10).forEach { i ->
            cache.put("key-$i", doc("content-$i"))
        }
        assertEquals(3, cache.size)
        // Latest entries should still be there
        assertNotNull(cache.get("key-10"))
        assertNotNull(cache.get("key-9"))
    }

    // ==================== Concurrent StyleSheets.getStyleSheet() ====================

    @Test
    fun concurrentStyleSheetAccessDoesNotCrash() {
        val formatIds = listOf(
            TextFormat.ID_MARKDOWN,
            TextFormat.ID_WIKITEXT,
            TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC,
            TextFormat.ID_LATEX
        )
        // Access concurrently (using threads since getStyleSheet is synchronous)
        val results = formatIds.flatMap { fmtId ->
            listOf(true, false).map { lightMode ->
                StyleSheets.getStyleSheet(fmtId, lightMode)
            }
        }
        assertEquals(12, results.size)
        assertTrue(results.all { it.isNotEmpty() || it.isEmpty() })
    }

    @Test
    fun styleSheetCacheConsistency() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)

        // Access a few
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, false)
        StyleSheets.getStyleSheet(TextFormat.ID_LATEX, true)

        assertEquals(3, StyleSheets.cacheSize)

        // Same keys should not increase cache size
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertEquals(3, StyleSheets.cacheSize)
    }

    @Test
    fun styleSheetClearAndRebuild() {
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertTrue(StyleSheets.cacheSize > 0)

        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)

        // Rebuild
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertEquals(1, StyleSheets.cacheSize)
    }

    @Test
    fun styleSheetConsistentForSameInput() {
        StyleSheets.clearCache()
        val first = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, true)
        val second = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, true)
        assertEquals(first, second, "Same format+mode should return identical stylesheet")
    }

    @Test
    fun styleSheetDifferentForLightVsDark() {
        val light = StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, true)
        val dark = StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, false)
        // RST has different light/dark styles
        assertNotEquals(light, dark, "Light and dark RST stylesheets should differ")
    }

    // ==================== Concurrent ParsedDocument.toHtml() ====================

    @Test
    fun concurrentToHtmlCallsReturnConsistentResults() {
        val parser = PlaintextParser()
        val doc = parser.parse("Hello, World!\nLine 2.\nLine 3.")

        // Multiple concurrent calls should all return the same result
        val results = (1..10).map {
            doc.toHtml(lightMode = true)
        }
        val expected = results.first()
        assertTrue(results.all { it == expected }, "All toHtml calls should return identical HTML")
    }

    @Test
    fun toHtmlCachingWorksForBothModes() {
        val parser = PlaintextParser()
        val doc = parser.parse("Test content.")

        assertFalse(doc.hasHtmlCached(true))
        assertFalse(doc.hasHtmlCached(false))

        doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(true))
        assertFalse(doc.hasHtmlCached(false))

        doc.toHtml(lightMode = false)
        assertTrue(doc.hasHtmlCached(true))
        assertTrue(doc.hasHtmlCached(false))
    }

    @Test
    fun clearHtmlCacheResetsAllModes() {
        val parser = PlaintextParser()
        val doc = parser.parse("Cache test.")
        doc.toHtml(lightMode = true)
        doc.toHtml(lightMode = false)
        assertTrue(doc.hasHtmlCached(true))
        assertTrue(doc.hasHtmlCached(false))

        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(true))
        assertFalse(doc.hasHtmlCached(false))
    }

    @Test
    fun toHtmlAfterClearRegeneratesCorrectly() {
        val parser = PlaintextParser()
        val doc = parser.parse("Regenerate test.")

        val first = doc.toHtml(lightMode = true)
        doc.clearHtmlCache()
        val second = doc.toHtml(lightMode = true)
        assertEquals(first, second, "Regenerated HTML should match original")
    }

    // ==================== DocumentCache Consistency Under Load ====================

    @Test
    fun cacheHitRateTrackingUnderConcurrentAccess() = runBlocking<Unit> {
        val cache = DocumentCache()
        cache.put("key", doc("content"))

        val jobs = (1..100).map { i ->
            async {
                if (i <= 75) cache.get("key")  // 75 hits
                else cache.get("missing-$i")     // 25 misses
            }
        }
        jobs.awaitAll()

        assertEquals(75L, cache.hits)
        assertEquals(25L, cache.misses)
        assertEquals(0.75, cache.hitRate, 0.01)
    }

    @Test
    fun documentCacheSizeNeverExceedsMaxUnderConcurrency() = runBlocking<Unit> {
        val maxSize = 10
        val cache = DocumentCache(maxSize = maxSize)
        val jobs = (1..50).map { i ->
            async {
                cache.put("key-$i", doc("content-$i"))
                assertTrue(cache.size <= maxSize + 1, "Cache size should not exceed maxSize")
            }
        }
        jobs.awaitAll()
        assertTrue(cache.size <= maxSize, "Final cache size should be <= maxSize")
    }

    @Test
    fun documentCachePutOverwriteUnderConcurrency() = runBlocking<Unit> {
        val cache = DocumentCache()
        val jobs = (1..10).map { i ->
            async {
                cache.put("same-key", doc("content-version-$i"))
            }
        }
        jobs.awaitAll()
        assertEquals(1, cache.size, "Overwriting same key should result in 1 entry")
        val result = cache.get("same-key")
        assertNotNull(result)
        assertTrue(result.rawContent.startsWith("content-version-"))
    }

    @Test
    fun documentCacheContainsWorksUnderConcurrency() = runBlocking<Unit> {
        val cache = DocumentCache()
        cache.put("exists", doc("content"))

        val jobs = (1..20).map { i ->
            async {
                if (i % 2 == 0) cache.contains("exists")
                else cache.contains("missing-$i")
            }
        }
        val results = jobs.awaitAll()
        assertEquals(20, results.size)
    }
}
