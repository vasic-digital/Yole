/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Cache Efficiency Tests
 *
 * Validates DocumentCache hit/miss ratios, LRU eviction
 * accuracy, memory bounds compliance, and concurrent
 * cache access performance.
 *
 *########################################################*/
package digital.vasic.yole.format.performance

import digital.vasic.yole.format.DocumentCache
import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.TextFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.measureTime

/**
 * Tests for DocumentCache efficiency: hit/miss ratios,
 * LRU eviction correctness, size bounds, and concurrent access.
 */
class CacheEfficiencyTest {

    private val testFormat = TextFormat(
        id = "plaintext",
        name = "Plain Text",
        defaultExtension = ".txt",
        extensions = listOf(".txt")
    )

    private fun doc(content: String) = ParsedDocument(
        format = testFormat,
        rawContent = content,
        parsedContent = content
    )

    // ====================================================================
    // 1. Hit/miss ratio under various access patterns
    // ====================================================================

    @Test
    fun `hit rate 100 percent when accessing same key repeatedly`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)
        cache.put("key", doc("content"))

        repeat(100) { cache.get("key") }

        assertEquals(100L, cache.hits)
        assertEquals(0L, cache.misses)
        assertEquals(1.0, cache.hitRate, 0.001)
    }

    @Test
    fun `hit rate 0 percent when all accesses are misses`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)

        repeat(50) { i -> cache.get("miss_$i") }

        assertEquals(0L, cache.hits)
        assertEquals(50L, cache.misses)
        assertEquals(0.0, cache.hitRate, 0.001)
    }

    @Test
    fun `hit rate approximately 50 percent for alternating hit and miss`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)
        cache.put("hit_key", doc("data"))

        repeat(50) { i ->
            if (i % 2 == 0) cache.get("hit_key") else cache.get("miss_$i")
        }

        val expectedRate = 25.0 / 50.0
        assertTrue(kotlin.math.abs(cache.hitRate - expectedRate) < 0.01,
            "Hit rate should be ~0.50, got ${cache.hitRate}")
    }

    @Test
    fun `hit rate degrades after eviction`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 3)
        cache.put("a", doc("aaa"))
        cache.put("b", doc("bbb"))
        cache.put("c", doc("ccc"))

        // All hits
        cache.get("a")
        cache.get("b")
        cache.get("c")
        assertEquals(3L, cache.hits)

        // Evict "a" by adding "d"
        cache.put("d", doc("ddd"))
        // Now "a" is a miss
        val result = cache.get("a")
        assertNull(result)
        assertEquals(1L, cache.misses)
    }

    @Test
    fun `working set fits cache yields high hit rate`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 20)
        // Pre-populate with 10 entries
        repeat(10) { i -> cache.put("key_$i", doc("content_$i")) }

        // Access only these 10 keys repeatedly
        repeat(100) { i ->
            cache.get("key_${i % 10}")
        }

        assertEquals(100L, cache.hits)
        assertEquals(0L, cache.misses)
        assertEquals(1.0, cache.hitRate, 0.001)
    }

    // ====================================================================
    // 2. LRU eviction accuracy
    // ====================================================================

    @Test
    fun `LRU evicts least recently used entry`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 3)
        cache.put("a", doc("aaa"))
        cache.put("b", doc("bbb"))
        cache.put("c", doc("ccc"))

        // Access "a" to make it recently used
        cache.get("a")

        // Insert "d" - should evict "b" (least recently used after "a" was accessed)
        cache.put("d", doc("ddd"))

        assertNotNull(cache.get("a"), "Recently accessed 'a' should survive")
        assertNull(cache.get("b"), "Least recently used 'b' should be evicted")
        assertNotNull(cache.get("c"), "'c' should survive")
        assertNotNull(cache.get("d"), "New entry 'd' should exist")
    }

    @Test
    fun `LRU eviction chain preserves most recently used entries`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 5)
        // Insert 5 items
        repeat(5) { i -> cache.put("key_$i", doc("content_$i")) }

        // Access keys 3 and 4 to make them recently used
        cache.get("key_3")
        cache.get("key_4")

        // Insert 3 more items, evicting 3 old ones
        cache.put("new_0", doc("new_0"))
        cache.put("new_1", doc("new_1"))
        cache.put("new_2", doc("new_2"))

        // key_3 and key_4 should survive (recently accessed)
        assertNotNull(cache.get("key_3"), "Recently accessed key_3 should survive")
        assertNotNull(cache.get("key_4"), "Recently accessed key_4 should survive")
        assertEquals(5, cache.size, "Cache size should remain at maxSize")
    }

    @Test
    fun `LRU with maxSize 1 only keeps the last entry`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 1)
        cache.put("first", doc("first"))
        cache.put("second", doc("second"))
        cache.put("third", doc("third"))

        assertEquals(1, cache.size)
        assertNull(cache.get("first"))
        assertNull(cache.get("second"))
        assertNotNull(cache.get("third"))
    }

    // ====================================================================
    // 3. Cache memory bounds compliance
    // ====================================================================

    @Test
    fun `cache never exceeds maxSize`() = runBlocking<Unit> {
        val maxSize = 10
        val cache = DocumentCache(maxSize = maxSize)

        repeat(100) { i ->
            cache.put("key_$i", doc("content_$i with some extra data for size"))
            assertTrue(cache.size <= maxSize,
                "Cache size ${cache.size} exceeded maxSize $maxSize at iteration $i")
        }
    }

    @Test
    fun `cache size stabilizes at maxSize under continuous inserts`() = runBlocking<Unit> {
        val maxSize = 20
        val cache = DocumentCache(maxSize = maxSize)

        repeat(200) { i ->
            cache.put("key_$i", doc("content_$i"))
        }
        assertEquals(maxSize, cache.size,
            "Cache size should stabilize at maxSize after many inserts")
    }

    // ====================================================================
    // 4. Concurrent cache access performance
    // ====================================================================

    @Test
    fun `concurrent reads do not corrupt cache state`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        repeat(20) { i -> cache.put("key_$i", doc("content_$i")) }

        val results = (1..30).map { i ->
            async { cache.get("key_${i % 20}") }
        }.awaitAll()

        val nonNullResults = results.filterNotNull()
        assertTrue(nonNullResults.isNotEmpty(), "Should have some cache hits")
        assertTrue(cache.size <= 50, "Cache size should be within bounds")
    }

    @Test
    fun `concurrent writes maintain size invariant`() = runBlocking<Unit> {
        val maxSize = 15
        val cache = DocumentCache(maxSize = maxSize)

        val jobs = (1..50).map { i ->
            async { cache.put("key_$i", doc("content_$i")) }
        }
        jobs.awaitAll()

        assertTrue(cache.size <= maxSize,
            "Cache size ${cache.size} should not exceed maxSize $maxSize after concurrent writes")
    }

    @Test
    fun `concurrent mixed operations complete without error`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 20)
        repeat(10) { i -> cache.put("pre_$i", doc("pre_$i")) }

        val jobs = (1..40).map { i ->
            async {
                when (i % 5) {
                    0 -> cache.put("dyn_$i", doc("dynamic_$i"))
                    1 -> cache.get("pre_${i % 10}")
                    2 -> cache.contains("pre_${i % 10}")
                    3 -> cache.invalidate("dyn_${i - 1}")
                    else -> cache.get("dyn_$i")
                }
            }
        }
        jobs.awaitAll()

        assertTrue(cache.size <= 20, "Cache should respect maxSize after concurrent mixed ops")
    }

    @Test
    fun `concurrent access performance 1000 ops under 1000ms`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        repeat(50) { i -> cache.put("key_$i", doc("content_$i")) }

        val elapsed = measureTime {
            val jobs = (1..1000).map { i ->
                async {
                    if (i % 2 == 0) cache.get("key_${i % 50}")
                    else cache.put("key_${i % 50}", doc("updated_$i"))
                }
            }
            jobs.awaitAll()
        }

        assertTrue(elapsed.inWholeMilliseconds < 1000,
            "1000 concurrent ops took ${elapsed.inWholeMilliseconds}ms, expected < 1000ms")
    }
}
