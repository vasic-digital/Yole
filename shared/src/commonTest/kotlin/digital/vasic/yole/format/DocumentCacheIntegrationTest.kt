/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for DocumentCache with FormatRegistry.parseWithCache
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class DocumentCacheIntegrationTest {

    private val plainTextFormat = TextFormat(
        id = "plaintext",
        name = "Plain Text",
        defaultExtension = ".txt",
        extensions = listOf(".txt")
    )

    // ==================== parseWithCache: Cache Hits and Misses ====================

    @Test
    fun parseWithCacheReturnsParsedDocument() = runTest {
        // Ensure a parser is registered for plaintext
        ensurePlaintextParser()

        val content = "Hello, world!"
        val result = FormatRegistry.parseWithCache(content, plainTextFormat)
        assertNotNull(result)
        assertEquals("Hello, world!", result.rawContent)
    }

    @Test
    fun parseWithCacheReturnsCachedOnSecondCall() = runTest {
        ensurePlaintextParser()
        val cache = FormatRegistry.documentCache
        cache.clear()

        val content = "cache-test-content"
        val first = FormatRegistry.parseWithCache(content, plainTextFormat)
        val hitsBefore = cache.hits

        val second = FormatRegistry.parseWithCache(content, plainTextFormat)
        assertEquals(hitsBefore + 1, cache.hits)
        // Same instance from cache
        assertEquals(first, second)
    }

    @Test
    fun parseWithCacheDifferentContentIsCacheMiss() = runTest {
        ensurePlaintextParser()
        val cache = FormatRegistry.documentCache
        cache.clear()

        FormatRegistry.parseWithCache("content-A", plainTextFormat)
        val missesBefore = cache.misses
        FormatRegistry.parseWithCache("content-B", plainTextFormat)
        assertEquals(missesBefore + 1, cache.misses)
    }

    @Test
    fun parseWithCacheSameContentDifferentFormatIsCacheMiss() = runTest {
        ensurePlaintextParser()
        val cache = FormatRegistry.documentCache
        cache.clear()

        val markdown = TextFormat(
            id = "markdown-test-only",
            name = "Markdown Test",
            defaultExtension = ".md",
            extensions = listOf(".md")
        )
        // Register a minimal parser for the test format
        try {
            ParserRegistry.register(object : TextParser {
                override val supportedFormat = markdown
                override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                    return ParsedDocument(
                        format = markdown,
                        rawContent = content,
                        parsedContent = "<p>$content</p>"
                    )
                }
            })
        } catch (_: IllegalArgumentException) {
            // Already registered from a previous test run
        }

        FormatRegistry.parseWithCache("same-content", plainTextFormat)
        val missesBefore = cache.misses
        FormatRegistry.parseWithCache("same-content", markdown)
        // Different format means different cache key -> miss
        assertEquals(missesBefore + 1, cache.misses)
    }

    // ==================== Cache Invalidation ====================

    @Test
    fun cacheInvalidationForcesFreshParse() = runTest {
        ensurePlaintextParser()
        val cache = FormatRegistry.documentCache
        cache.clear()

        val content = "invalidation-test"
        FormatRegistry.parseWithCache(content, plainTextFormat)
        val cacheKey = "${plainTextFormat.id}:${content.hashCode()}"

        cache.invalidate(cacheKey)
        val missesBefore = cache.misses
        FormatRegistry.parseWithCache(content, plainTextFormat)
        assertEquals(missesBefore + 1, cache.misses)
    }

    @Test
    fun cacheClearForcesFreshParseForAllKeys() = runTest {
        ensurePlaintextParser()
        val cache = FormatRegistry.documentCache
        cache.clear()

        FormatRegistry.parseWithCache("content-1", plainTextFormat)
        FormatRegistry.parseWithCache("content-2", plainTextFormat)

        cache.clear()
        assertEquals(0, cache.size)

        val missesBefore = cache.misses
        FormatRegistry.parseWithCache("content-1", plainTextFormat)
        assertEquals(missesBefore + 1, cache.misses)
    }

    // ==================== Concurrent Cache Access ====================

    @Test
    fun concurrentParseWithCacheIsThreadSafe() = runTest {
        ensurePlaintextParser()
        val cache = FormatRegistry.documentCache
        cache.clear()

        val results = (1..10).map { i ->
            async {
                FormatRegistry.parseWithCache("concurrent-$i", plainTextFormat)
            }
        }.awaitAll()

        assertEquals(10, results.size)
        results.forEachIndexed { index, doc ->
            assertEquals("concurrent-${index + 1}", doc.rawContent)
        }
    }

    @Test
    fun concurrentCacheAccessWithSameContentReturnsSameDocument() = runTest {
        ensurePlaintextParser()
        val cache = FormatRegistry.documentCache
        cache.clear()

        val content = "shared-content"
        val results = (1..5).map {
            async {
                FormatRegistry.parseWithCache(content, plainTextFormat)
            }
        }.awaitAll()

        // All results should be equal
        val first = results.first()
        results.forEach { assertEquals(first, it) }
    }

    @Test
    fun concurrentPutAndGetOnDocumentCache() = runTest {
        val cache = DocumentCache()
        val results = (1..20).map { i ->
            async {
                val doc = ParsedDocument(
                    format = plainTextFormat,
                    rawContent = "item-$i",
                    parsedContent = "item-$i"
                )
                cache.put("key-$i", doc)
                cache.get("key-$i")
            }
        }.awaitAll()

        assertEquals(20, results.filterNotNull().size)
    }

    // ==================== Cache Size and Eviction ====================

    @Test
    fun cacheRespectMaxSize() = runTest {
        val cache = DocumentCache(maxSize = 3)
        for (i in 1..5) {
            val doc = ParsedDocument(format = plainTextFormat, rawContent = "d$i", parsedContent = "d$i")
            cache.put("k$i", doc)
        }
        // Only the last 3 should remain (LRU eviction)
        assertTrue(cache.size <= 3)
    }

    @Test
    fun cacheHitRateStartsAtZero() {
        val cache = DocumentCache()
        assertEquals(0.0, cache.hitRate, 0.001)
    }

    @Test
    fun cacheClearResetsHitAndMissCounters() = runTest {
        val cache = DocumentCache()
        val doc = ParsedDocument(format = plainTextFormat, rawContent = "x", parsedContent = "x")
        cache.put("k", doc)
        cache.get("k")
        cache.get("missing")
        cache.clear()
        assertEquals(0L, cache.hits)
        assertEquals(0L, cache.misses)
    }

    @Test
    fun cacheSizeIsZeroAfterClear() = runTest {
        val cache = DocumentCache()
        val doc = ParsedDocument(format = plainTextFormat, rawContent = "x", parsedContent = "x")
        cache.put("k1", doc)
        cache.put("k2", doc)
        cache.clear()
        assertEquals(0, cache.size)
    }

    @Test
    fun cacheOverwriteExistingKey() = runTest {
        val cache = DocumentCache()
        val doc1 = ParsedDocument(format = plainTextFormat, rawContent = "old", parsedContent = "old")
        val doc2 = ParsedDocument(format = plainTextFormat, rawContent = "new", parsedContent = "new")
        cache.put("key", doc1)
        cache.put("key", doc2)
        val retrieved = cache.get("key")
        assertNotNull(retrieved)
        assertEquals("new", retrieved.rawContent)
    }

    // ==================== Error Handling ====================

    @Test
    fun parseWithCacheThrowsForUnknownFormat() = runTest {
        val unknownFormat = TextFormat(
            id = "totally-unknown-format-xyz",
            name = "Unknown",
            defaultExtension = ".xyz",
            extensions = listOf(".xyz")
        )
        assertFailsWith<IllegalArgumentException> {
            FormatRegistry.parseWithCache("some content", unknownFormat)
        }
    }

    @Test
    fun parseWithCachePassesOptionsToParser() = runTest {
        ensurePlaintextParser()

        val options = mapOf("lineNumbers" to true as Any)
        val result = FormatRegistry.parseWithCache("content with options", plainTextFormat, options)
        assertNotNull(result)
    }

    // ==================== Helper ====================

    private fun ensurePlaintextParser() {
        // ParserRegistry may already have a parser for plaintext
        val existing = ParserRegistry.getParser(plainTextFormat)
        if (existing == null) {
            try {
                ParserRegistry.register(object : TextParser {
                    override val supportedFormat = plainTextFormat
                    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
                        return ParsedDocument(
                            format = plainTextFormat,
                            rawContent = content,
                            parsedContent = content,
                            metadata = mapOf("lines" to content.lines().size.toString())
                        )
                    }
                })
            } catch (_: IllegalArgumentException) {
                // Already registered
            }
        }
    }
}
