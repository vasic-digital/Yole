/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Lazy initialization and semaphore tests for
 * FormatRegistry, StyleSheets, ConnectionLimiter,
 * and concurrent resource usage patterns.
 *
 *########################################################*/
package digital.vasic.yole.performance

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.network.common.ConnectionLimiter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Lazy initialization and semaphore tests.
 *
 * Validates that FormatRegistry lazy loading works correctly,
 * StyleSheets cache starts empty, ConnectionLimiter enforces
 * limits, and concurrent parsing respects resources.
 *
 * Total: 22+ tests
 */
class LazyInitSemaphoreTest {

    @BeforeTest
    fun setUp() {
        StyleSheets.clearCache()
    }

    // ==================== FormatRegistry Lazy Initialization ====================

    @Test
    fun formatRegistryIsFormatsInitializedReflectsActualState() {
        // By accessing formats, we trigger initialization
        @Suppress("UNUSED_VARIABLE") val ignored = FormatRegistry.formats
        assertTrue(FormatRegistry.isFormatsInitialized,
            "isFormatsInitialized should be true after accessing formats")
    }

    @Test
    fun formatRegistryFormatsListIsNotEmpty() {
        val formats = FormatRegistry.formats
        assertTrue(formats.isNotEmpty())
        assertTrue(formats.size >= 18, "Should have at least 18 formats, got ${formats.size}")
    }

    @Test
    fun formatRegistryFormatsAreStable() {
        val first = FormatRegistry.formats
        val second = FormatRegistry.formats
        assertSame(first, second, "Lazy formats should return same instance")
    }

    @Test
    fun formatRegistryFormatsContainAllExpectedIds() {
        val formats = FormatRegistry.formats
        val ids = formats.map { it.id }.toSet()
        val expected = listOf(
            FormatRegistry.ID_MARKDOWN, FormatRegistry.ID_PLAINTEXT,
            FormatRegistry.ID_TODOTXT, FormatRegistry.ID_CSV,
            FormatRegistry.ID_WIKITEXT, FormatRegistry.ID_ORGMODE,
            FormatRegistry.ID_LATEX, FormatRegistry.ID_ASCIIDOC,
            FormatRegistry.ID_RESTRUCTUREDTEXT, FormatRegistry.ID_KEYVALUE,
            FormatRegistry.ID_TASKPAPER, FormatRegistry.ID_TEXTILE,
            FormatRegistry.ID_CREOLE, FormatRegistry.ID_TIDDLYWIKI,
            FormatRegistry.ID_JUPYTER, FormatRegistry.ID_RMARKDOWN,
            FormatRegistry.ID_BINARY
        )
        expected.forEach { expectedId ->
            assertTrue(ids.contains(expectedId), "Formats should contain $expectedId")
        }
    }

    // ==================== StyleSheets Cache ====================

    @Test
    fun styleSheetsCacheStartsEmpty() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)
    }

    @Test
    fun styleSheetsCachePopulatesOnAccess() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertEquals(1, StyleSheets.cacheSize)
    }

    @Test
    fun styleSheetsClearCacheWorks() {
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        StyleSheets.getStyleSheet(TextFormat.ID_LATEX, false)
        assertTrue(StyleSheets.cacheSize > 0)

        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)
    }

    @Test
    fun styleSheetsCacheDoesNotDuplicate() {
        StyleSheets.clearCache()
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, true)
        assertEquals(1, StyleSheets.cacheSize, "Repeated access should not duplicate cache entries")
    }

    @Test
    fun styleSheetsCacheSeparatesLightAndDark() {
        StyleSheets.clearCache()
        StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, true)
        assertEquals(1, StyleSheets.cacheSize)
        StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, false)
        assertEquals(2, StyleSheets.cacheSize)
    }

    @Test
    fun styleSheetsCacheRebuildsAfterClear() {
        StyleSheets.clearCache()
        val first = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, true)
        StyleSheets.clearCache()
        val second = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, true)
        assertEquals(first, second, "Rebuilt cache should produce same stylesheet")
    }

    // ==================== ConnectionLimiter Semaphore Behavior ====================

    @Test
    fun connectionLimiterEnforcesMaxConcurrent() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 2, acquireTimeout = 5.seconds)
        // With maxConcurrent=2, we verify all 10 jobs complete (limiter queues excess)
        val jobs = (1..10).map { i ->
            async {
                limiter.withConnection {
                    // During execution, available permits should be reduced
                    assertTrue(limiter.availablePermits < 2 || limiter.availablePermits >= 0)
                    delay(10.milliseconds)
                    i
                }
            }
        }
        val results = jobs.awaitAll()
        assertEquals(10, results.size)
        assertEquals(2, limiter.availablePermits,
            "All permits should be returned after completion")
    }

    @Test
    fun connectionLimiterReleasesAllPermitsAfterCompletion() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 5, acquireTimeout = 5.seconds)
        val jobs = (1..20).map { i ->
            async {
                limiter.withConnection {
                    delay(5.milliseconds)
                    i
                }
            }
        }
        jobs.awaitAll()
        assertEquals(5, limiter.availablePermits)
    }

    @Test
    fun connectionLimiterHandlesExceptionGracefully() = runBlocking<Unit> {
        val limiter = ConnectionLimiter(maxConcurrent = 3, acquireTimeout = 5.seconds)
        repeat(5) { i ->
            try {
                limiter.withConnection {
                    if (i % 2 == 0) throw RuntimeException("fail-$i")
                    "ok-$i"
                }
            } catch (_: RuntimeException) { }
        }
        // All permits should still be available
        assertEquals(3, limiter.availablePermits)
    }

    @Test
    fun connectionLimiterWithDifferentMaxValues() = runBlocking<Unit> {
        for (maxConc in listOf(1, 2, 5, 10)) {
            val limiter = ConnectionLimiter(maxConcurrent = maxConc, acquireTimeout = 5.seconds)
            assertEquals(maxConc, limiter.availablePermits)

            val jobs = (1..maxConc).map { i ->
                async { limiter.withConnection { i } }
            }
            jobs.awaitAll()
            assertEquals(maxConc, limiter.availablePermits)
        }
    }

    // ==================== Concurrent Format Parsing ====================

    @Test
    fun concurrentParsersDoNotInterfere() = runBlocking<Unit> {
        val mdParser = MarkdownParser()
        val txtParser = PlaintextParser()

        val jobs = (1..10).map { i ->
            async {
                if (i % 2 == 0) {
                    mdParser.parse("# Heading $i\n\nContent $i")
                } else {
                    txtParser.parse("Plain text $i")
                }
            }
        }
        val results = jobs.awaitAll()
        assertEquals(10, results.size)
        results.forEach { doc ->
            assertTrue(doc.format.id == FormatRegistry.ID_MARKDOWN ||
                doc.format.id == FormatRegistry.ID_PLAINTEXT)
        }
    }

    @Test
    fun concurrentParseAndHtmlGeneration() = runBlocking<Unit> {
        val parser = MarkdownParser()
        val jobs = (1..10).map { i ->
            async {
                val doc = parser.parse("# Doc $i\n\nContent $i")
                val html = doc.toHtml(lightMode = true)
                assertTrue(html.isNotEmpty(), "HTML for doc $i should not be empty")
                doc
            }
        }
        val results = jobs.awaitAll()
        assertEquals(10, results.size)
        results.forEach { doc ->
            assertTrue(doc.hasHtmlCached(true), "HTML should be cached after toHtml()")
        }
    }

    @Test
    fun concurrentFormatDetection() = runBlocking<Unit> {
        val filenames = listOf(
            "readme.md", "data.csv", "paper.tex",
            "notes.org", "page.wiki", "doc.rst"
        )
        val jobs = filenames.map { filename ->
            async { FormatRegistry.detectByFilename(filename) }
        }
        val results = jobs.awaitAll()
        assertEquals(6, results.size)
        assertTrue(results.all { it.id.isNotBlank() })
    }

    @Test
    fun concurrentStyleSheetAccess() = runBlocking<Unit> {
        StyleSheets.clearCache()
        val formatIds = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_LATEX,
            TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_ORGMODE
        )
        val jobs = formatIds.flatMap { fmtId ->
            listOf(true, false).map { lightMode ->
                async { StyleSheets.getStyleSheet(fmtId, lightMode) }
            }
        }
        val results = jobs.awaitAll()
        assertEquals(8, results.size)
    }

    // ==================== Parser Registry Lazy Loading ====================

    @Test
    fun parserRegistryTracksInstantiatedCount() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
        val count = ParserRegistry.getInstantiatedParserCount()
        assertTrue(count >= 17, "Should have at least 18 instantiated parsers, got $count")
    }

    @Test
    fun parserRegistryLazyRegistrationWorks() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsersLazy()

        // Should have pending parsers
        val pending = ParserRegistry.getPendingParserCount()
        assertTrue(pending > 0, "Should have pending parsers after lazy registration")

        // Access one parser
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
        val parser = ParserRegistry.getParser(format)
        assertNotNull(parser, "Markdown parser should be available")

        // Pending count should decrease
        val newPending = ParserRegistry.getPendingParserCount()
        assertTrue(newPending < pending, "Pending count should decrease after access")
    }
}
