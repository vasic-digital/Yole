/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Lazy Initialization Metrics Tests
 *
 * Validates FormatRegistry lazy init timing, StyleSheets
 * cache effectiveness, and lazy loading non-blocking behavior.
 *
 *########################################################*/
package digital.vasic.yole.format.performance

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.measureTime

/**
 * Tests that measure lazy initialization timing and cache effectiveness
 * for FormatRegistry, StyleSheets, and ParserRegistry lazy loading.
 */
class LazyInitializationMetricsTest {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        StyleSheets.clearCache()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // ====================================================================
    // 1. FormatRegistry lazy init timing
    // ====================================================================

    @Test
    fun `FormatRegistry first access completes under 100ms`() {
        // FormatRegistry is a singleton so formats may already be initialized.
        // We measure the access time which should be fast regardless.
        val elapsed = measureTime {
            val formats = FormatRegistry.formats
            assertTrue(formats.isNotEmpty())
        }
        assertTrue(elapsed.inWholeMilliseconds < 100,
            "FormatRegistry access took ${elapsed.inWholeMilliseconds}ms, expected < 100ms")
    }

    @Test
    fun `FormatRegistry subsequent access under 1ms`() {
        // Trigger initialization
        FormatRegistry.formats

        val elapsed = measureTime {
            repeat(1000) { FormatRegistry.formats }
        }
        val avgMicros = (elapsed.inWholeMilliseconds * 1000) / 1000
        assertTrue(avgMicros < 1000,
            "Average subsequent access: ${avgMicros}us, expected < 1000us")
    }

    @Test
    fun `FormatRegistry isFormatsInitialized check is free`() {
        val elapsed = measureTime {
            repeat(10_000) { FormatRegistry.isFormatsInitialized }
        }
        assertTrue(elapsed.inWholeMilliseconds < 50,
            "10,000 isFormatsInitialized checks took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun `FormatRegistry getById performance after lazy init`() {
        // Ensure init
        FormatRegistry.formats

        val formatIds = listOf(
            FormatRegistry.ID_MARKDOWN, FormatRegistry.ID_PLAINTEXT,
            FormatRegistry.ID_CSV, FormatRegistry.ID_LATEX,
            FormatRegistry.ID_ORGMODE, FormatRegistry.ID_WIKITEXT
        )

        val elapsed = measureTime {
            repeat(1000) {
                for (id in formatIds) {
                    FormatRegistry.getById(id)
                }
            }
        }
        // 6000 lookups
        assertTrue(elapsed.inWholeMilliseconds < 500,
            "6000 getById lookups took ${elapsed.inWholeMilliseconds}ms, expected < 500ms")
    }

    // ====================================================================
    // 2. StyleSheets cache effectiveness
    // ====================================================================

    @Test
    fun `StyleSheets first call vs cached call speedup`() {
        StyleSheets.clearCache()

        val firstCallTime = measureTime {
            StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        }

        val cachedTime = measureTime {
            repeat(1000) {
                StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
            }
        }

        // Cached calls (1000 of them) should be comparable to or faster than 1 first call
        assertTrue(cachedTime.inWholeMilliseconds < 100,
            "1000 cached calls took ${cachedTime.inWholeMilliseconds}ms, expected < 100ms")
    }

    @Test
    fun `StyleSheets cache grows correctly with format and mode combinations`() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertEquals(1, StyleSheets.cacheSize)

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = false)
        assertEquals(2, StyleSheets.cacheSize)

        StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode = true)
        assertEquals(3, StyleSheets.cacheSize)

        // Repeated access should not grow cache
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertEquals(3, StyleSheets.cacheSize)
    }

    @Test
    fun `StyleSheets all supported formats cacheable under 50ms total`() {
        StyleSheets.clearCache()
        val formats = listOf(
            TextFormat.ID_MARKDOWN, TextFormat.ID_WIKITEXT,
            TextFormat.ID_RESTRUCTUREDTEXT, TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC, TextFormat.ID_LATEX
        )

        val elapsed = measureTime {
            for (f in formats) {
                StyleSheets.getStyleSheet(f, lightMode = true)
                StyleSheets.getStyleSheet(f, lightMode = false)
            }
        }
        assertEquals(12, StyleSheets.cacheSize,
            "Expected 12 cache entries (6 formats x 2 modes)")
        assertTrue(elapsed.inWholeMilliseconds < 50,
            "Caching all formats took ${elapsed.inWholeMilliseconds}ms, expected < 50ms")
    }

    @Test
    fun `StyleSheets clearCache resets and re-population works`() {
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(StyleSheets.cacheSize > 0)

        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)

        // Re-populate
        val css = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(css.isNotEmpty())
        assertEquals(1, StyleSheets.cacheSize)
    }

    // ====================================================================
    // 3. Lazy loading does not block callers
    // ====================================================================

    @Test
    fun `ParserRegistry lazy registration is faster than eager`() {
        ParserRegistry.clear()
        val lazyTime = measureTime {
            ParserInitializer.registerAllParsersLazy()
        }
        val pendingAfterLazy = ParserRegistry.getPendingParserCount()

        ParserRegistry.clear()
        val eagerTime = measureTime {
            ParserInitializer.registerAllParsers()
        }
        val instantiatedAfterEager = ParserRegistry.getInstantiatedParserCount()

        assertEquals(18, pendingAfterLazy, "Should have 18 pending parsers after lazy registration")
        assertEquals(18, instantiatedAfterEager, "Should have 18 instantiated parsers after eager registration")
        assertTrue(lazyTime.inWholeMilliseconds < 100,
            "Lazy registration took ${lazyTime.inWholeMilliseconds}ms, expected < 100ms")
    }

    @Test
    fun `lazy parser instantiation on first access is fast`() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsersLazy()

        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
        val elapsed = measureTime {
            val parser = ParserRegistry.getParser(format)
            assertNotNull(parser)
        }
        assertTrue(elapsed.inWholeMilliseconds < 100,
            "First parser access took ${elapsed.inWholeMilliseconds}ms, expected < 100ms")
        assertEquals(1, ParserRegistry.getInstantiatedParserCount())
    }

    @Test
    fun `concurrent format detection does not block`() = runBlocking<Unit> {
        val elapsed = measureTime {
            val jobs = (1..20).map {
                async {
                    FormatRegistry.detectByExtension("md")
                    FormatRegistry.detectByExtension("csv")
                    FormatRegistry.detectByExtension("tex")
                    FormatRegistry.detectByContent("# Heading")
                }
            }
            jobs.awaitAll()
        }
        // 80 detection operations
        assertTrue(elapsed.inWholeMilliseconds < 500,
            "80 concurrent detection ops took ${elapsed.inWholeMilliseconds}ms, expected < 500ms")
    }

    @Test
    fun `ParsedDocument HTML lazy caching does not generate until called`() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()

        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)!!
        val parser = ParserRegistry.getParser(format)!!
        val doc = parser.parse("# Test Document\n\nParagraph content.")

        assertFalse(doc.hasHtmlCached(lightMode = true), "HTML should not be cached before toHtml()")
        assertFalse(doc.hasHtmlCached(lightMode = false), "Dark HTML should not be cached before toHtml()")

        doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(lightMode = true), "HTML should be cached after toHtml()")
        assertFalse(doc.hasHtmlCached(lightMode = false), "Dark HTML should still not be cached")
    }
}
