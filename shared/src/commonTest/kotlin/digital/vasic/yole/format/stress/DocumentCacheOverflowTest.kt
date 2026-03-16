/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Document Cache Overflow Tests
 *
 * Validates DocumentCache LRU eviction behavior under
 * pressure, concurrent read/write/eviction scenarios,
 * and data integrity during overflow conditions.
 *
 *########################################################*/

package digital.vasic.yole.format.stress

import digital.vasic.yole.format.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.*

/**
 * Stress tests for DocumentCache under overflow conditions.
 *
 * Tests cache at capacity with concurrent reads/writes/evictions,
 * verifies LRU eviction under pressure, and ensures no data
 * corruption during concurrent access.
 */
class DocumentCacheOverflowTest {

    private val testFormat = TextFormat(
        id = "test-format",
        name = "Test Format",
        defaultExtension = ".test"
    )

    private fun createDoc(key: String): ParsedDocument {
        return ParsedDocument(
            format = testFormat,
            rawContent = "raw-$key",
            parsedContent = "parsed-$key",
            metadata = mapOf("key" to key)
        )
    }

    @Test
    fun `cache evicts oldest entry when exceeding max size`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 5)

        // Fill cache to capacity
        for (i in 1..5) {
            cache.put("key-$i", createDoc("$i"))
        }
        assertEquals(5, cache.size, "Cache should be at capacity")

        // Add one more to trigger eviction
        cache.put("key-6", createDoc("6"))

        assertEquals(5, cache.size, "Cache should still be at max size after eviction")

        // The first entry should have been evicted (LRU)
        val evicted = cache.get("key-1")
        assertNull(evicted, "Oldest entry (key-1) should be evicted")

        // The newest should be present
        val newest = cache.get("key-6")
        assertNotNull(newest, "Newest entry (key-6) should be present")
    }

    @Test
    fun `LRU eviction respects access order`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 5)

        // Fill cache
        for (i in 1..5) {
            cache.put("key-$i", createDoc("$i"))
        }

        // Access key-1 to make it recently used
        cache.get("key-1")

        // Add new entry to trigger eviction
        cache.put("key-6", createDoc("6"))

        // key-2 should be evicted (least recently used), not key-1
        val key1 = cache.get("key-1")
        assertNotNull(key1, "key-1 should still be present (was accessed recently)")

        val key2 = cache.get("key-2")
        assertNull(key2, "key-2 should be evicted (was LRU)")
    }

    @Test
    fun `concurrent writes at cache boundary maintain size constraint`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 20)

        val jobs = (1..100).map { i ->
            launch(Dispatchers.Default) {
                cache.put("key-$i", createDoc("$i"))
            }
        }
        jobs.forEach { it.join() }

        assertTrue(
            cache.size <= 20,
            "Cache size (${cache.size}) must not exceed maxSize (20)"
        )
    }

    @Test
    fun `concurrent reads and writes produce no corruption`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val mutex = Mutex()
        var corruptionDetected = false

        // Pre-populate
        for (i in 1..50) {
            cache.put("key-$i", createDoc("$i"))
        }

        // Concurrent reads and writes
        val readers = (1..100).map {
            launch(Dispatchers.Default) {
                val key = "key-${(1..50).random()}"
                val doc = cache.get(key)
                if (doc != null) {
                    val expectedKey = key.removePrefix("key-")
                    if (doc.metadata["key"] != expectedKey) {
                        mutex.withLock { corruptionDetected = true }
                    }
                }
            }
        }

        val writers = (51..150).map { i ->
            launch(Dispatchers.Default) {
                cache.put("key-$i", createDoc("$i"))
            }
        }

        readers.forEach { it.join() }
        writers.forEach { it.join() }

        assertFalse(corruptionDetected, "No data corruption should be detected during concurrent access")
    }

    @Test
    fun `cache hit and miss tracking is accurate under load`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)

        // Pre-populate 5 entries
        for (i in 1..5) {
            cache.put("key-$i", createDoc("$i"))
        }

        // Access known keys (hits) and unknown keys (misses)
        for (i in 1..5) {
            cache.get("key-$i") // hit
        }
        for (i in 6..10) {
            cache.get("key-$i") // miss
        }

        assertEquals(5L, cache.hits, "Should have 5 hits")
        assertEquals(5L, cache.misses, "Should have 5 misses")
        assertEquals(0.5, cache.hitRate, 0.01, "Hit rate should be 50%")
    }

    @Test
    fun `cache clear resets all state`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 20)

        for (i in 1..20) {
            cache.put("key-$i", createDoc("$i"))
        }
        cache.get("key-1") // register a hit
        cache.get("missing") // register a miss

        cache.clear()

        assertEquals(0, cache.size, "Cache size should be 0 after clear")
        assertEquals(0L, cache.hits, "Hits should be 0 after clear")
        assertEquals(0L, cache.misses, "Misses should be 0 after clear")
    }

    @Test
    fun `invalidate removes specific entry without affecting others`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)

        for (i in 1..5) {
            cache.put("key-$i", createDoc("$i"))
        }

        cache.invalidate("key-3")

        assertNull(cache.get("key-3"), "Invalidated key should return null")
        assertNotNull(cache.get("key-1"), "key-1 should still exist")
        assertNotNull(cache.get("key-5"), "key-5 should still exist")
        assertEquals(4, cache.size, "Cache should have 4 entries after invalidation")
    }

    @Test
    fun `concurrent invalidations and reads are safe`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 50)
        val mutex = Mutex()
        var errors = 0

        for (i in 1..50) {
            cache.put("key-$i", createDoc("$i"))
        }

        // Concurrently invalidate even keys and read odd keys
        val invalidators = (1..25).map { i ->
            launch(Dispatchers.Default) {
                cache.invalidate("key-${i * 2}")
            }
        }

        val readers = (1..25).map { i ->
            launch(Dispatchers.Default) {
                val doc = cache.get("key-${i * 2 - 1}")
                // Odd keys might or might not be evicted by this point
                if (doc != null && doc.metadata["key"] != "${i * 2 - 1}") {
                    mutex.withLock { errors++ }
                }
            }
        }

        invalidators.forEach { it.join() }
        readers.forEach { it.join() }

        assertEquals(0, errors, "No corruption should occur during concurrent invalidation")
    }

    @Test
    fun `massive overflow triggers continuous eviction without crash`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)

        // Write 1000 entries into a cache of size 10
        for (i in 1..1000) {
            cache.put("key-$i", createDoc("$i"))
        }

        assertTrue(
            cache.size <= 10,
            "Cache size (${cache.size}) must never exceed maxSize (10)"
        )

        // The most recent entries should be in the cache
        val last = cache.get("key-1000")
        assertNotNull(last, "Most recently added entry should be in cache")
    }

    @Test
    fun `contains check is consistent with get`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)

        cache.put("exists", createDoc("exists"))

        assertTrue(cache.contains("exists"), "contains should return true for existing key")
        assertFalse(cache.contains("missing"), "contains should return false for missing key")

        val doc = cache.get("exists")
        assertNotNull(doc, "get should return document for existing key")
    }

    @Test
    fun `concurrent put with same key overwrites safely`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 10)
        val mutex = Mutex()
        val finalValues = mutableListOf<String>()

        // Concurrently write to the same key
        val jobs = (1..50).map { i ->
            launch(Dispatchers.Default) {
                cache.put("shared-key", createDoc("version-$i"))
            }
        }
        jobs.forEach { it.join() }

        val doc = cache.get("shared-key")
        assertNotNull(doc, "shared-key should exist after concurrent writes")
        assertTrue(doc.metadata["key"]!!.startsWith("version-"), "Value should be one of the versions")
        assertEquals(1, cache.size, "Only one entry should exist for the shared key")
    }

    @Test
    fun `rapid fill and clear cycles maintain consistency`() = runBlocking<Unit> {
        val cache = DocumentCache(maxSize = 20)

        repeat(10) { cycle ->
            // Fill
            for (i in 1..20) {
                cache.put("cycle-$cycle-key-$i", createDoc("$cycle-$i"))
            }
            assertTrue(cache.size <= 20, "Cycle $cycle: size should not exceed maxSize")

            // Clear
            cache.clear()
            assertEquals(0, cache.size, "Cycle $cycle: cache should be empty after clear")
        }
    }
}
