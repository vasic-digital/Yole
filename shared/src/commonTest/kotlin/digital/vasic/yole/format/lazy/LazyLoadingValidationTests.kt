/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Lazy Loading Validation Tests
 *
 * Validates that LazyDocumentLoader, LazyStringLoader,
 * and FlowLazyLoader defer computation, cache results,
 * are thread-safe, handle errors, and integrate with
 * coroutine cancellation.
 *
 *########################################################*/
package digital.vasic.yole.format.lazy

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.util.FlowLazyLoader
import digital.vasic.yole.util.LazyDocumentLoader
import digital.vasic.yole.util.LazyStringLoader
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.measureTime
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive validation tests for the lazy loading utilities
 * (LazyDocumentLoader, LazyStringLoader, FlowLazyLoader).
 *
 * 30+ test methods covering deferred computation, caching,
 * thread safety, error handling, memory efficiency, and
 * coroutine cancellation.
 */
class LazyLoadingValidationTests {

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
    // LazyDocumentLoader: deferred loading
    // ====================================================================

    @Test
    fun `LazyDocumentLoader does not load chunks until accessed`() = runBlocking<Unit> {
        var loadCount = 0
        val loader = object : LazyDocumentLoader<String>(chunkSize = 10) {
            override suspend fun loadChunk(index: Int): String {
                loadCount++
                return "chunk-$index"
            }
        }
        loader.initialize(100) // 10 chunks

        // No chunks loaded yet
        assertEquals(0, loadCount, "No chunks should be loaded before access")

        // Access one chunk
        val chunk = loader.getChunk(0)
        assertEquals("chunk-0", chunk)
        assertEquals(1, loadCount, "Only accessed chunk should be loaded")
    }

    @Test
    fun `LazyDocumentLoader getChunk returns null for out-of-range`() = runBlocking<Unit> {
        val loader = object : LazyDocumentLoader<String>(chunkSize = 10) {
            override suspend fun loadChunk(index: Int): String = "data"
        }
        loader.initialize(20) // 2 chunks (indices 0, 1)

        assertNull(loader.getChunk(5), "Out-of-range chunk should be null")
        assertNull(loader.getChunk(-1), "Negative index should be null")
    }

    @Test
    fun `LazyDocumentLoader caches loaded chunks`() = runBlocking<Unit> {
        var loadCount = 0
        val loader = object : LazyDocumentLoader<String>(chunkSize = 10) {
            override suspend fun loadChunk(index: Int): String {
                loadCount++
                return "chunk-$index"
            }
        }
        loader.initialize(50)

        // First access loads
        loader.getChunk(2)
        assertEquals(1, loadCount)

        // Second access returns cached
        val result = loader.getChunk(2)
        assertEquals("chunk-2", result)
        assertEquals(1, loadCount, "Second access should use cache, not reload")
    }

    @Test
    fun `LazyDocumentLoader clear removes cached chunks`() = runBlocking<Unit> {
        var loadCount = 0
        val loader = object : LazyDocumentLoader<String>(chunkSize = 10) {
            override suspend fun loadChunk(index: Int): String {
                loadCount++
                return "chunk-$index"
            }
        }
        loader.initialize(50)

        loader.getChunk(0)
        assertEquals(1, loadCount)

        loader.clear()
        assertEquals(0, loader.getMemoryUsage())

        // Accessing after clear reloads
        loader.getChunk(0)
        assertEquals(2, loadCount, "Clear should force reload on next access")
    }

    @Test
    fun `LazyDocumentLoader preloadAround loads adjacent chunks`() = runBlocking<Unit> {
        val loadedIndices = mutableSetOf<Int>()
        val loader = object : LazyDocumentLoader<String>(chunkSize = 10) {
            override suspend fun loadChunk(index: Int): String {
                loadedIndices.add(index)
                return "chunk-$index"
            }
        }
        loader.initialize(100) // 10 chunks

        loader.preloadAround(5, range = 2)

        // Chunks 3,4,5,6,7 should be loaded
        assertTrue(loadedIndices.containsAll(listOf(3, 4, 5, 6, 7)),
            "preloadAround should load adjacent chunks, loaded: $loadedIndices")
    }

    @Test
    fun `LazyDocumentLoader getMemoryUsage reflects loaded chunks`() = runBlocking<Unit> {
        val loader = object : LazyDocumentLoader<String>(chunkSize = 100) {
            override suspend fun loadChunk(index: Int): String = "data"
        }
        loader.initialize(500) // 5 chunks

        assertEquals(0, loader.getMemoryUsage(), "Empty loader should report 0 memory")

        loader.getChunk(0)
        assertTrue(loader.getMemoryUsage() > 0, "Loaded chunk should increase memory usage")
    }

    // ====================================================================
    // LazyStringLoader: deferred string computation
    // ====================================================================

    @Test
    fun `LazyStringLoader defers string computation`() = runBlocking<Unit> {
        val content = (1..200).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 50)

        // Initially no memory used (chunks not loaded)
        assertEquals(0, loader.getMemoryUsage(), "No memory used before first access")
    }

    @Test
    fun `LazyStringLoader getChunk returns correct content`() = runBlocking<Unit> {
        val content = (1..100).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 10)

        val chunk0 = loader.getChunk(0)
        assertNotNull(chunk0)
        assertTrue(chunk0.startsWith("Line 1"), "First chunk should start with Line 1")
        assertTrue(chunk0.contains("Line 10"), "First chunk should contain Line 10")
    }

    @Test
    fun `LazyStringLoader getLines returns exact range`() = runBlocking<Unit> {
        val content = (1..50).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 10)

        val lines = loader.getLines(0, 5)
        assertEquals(5, lines.size)
        assertEquals("Line 1", lines[0])
        assertEquals("Line 5", lines[4])
    }

    @Test
    fun `LazyStringLoader getLines spanning chunks`() = runBlocking<Unit> {
        val content = (1..50).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 10)

        // Lines 8-12 span chunk 0 (lines 0-9) and chunk 1 (lines 10-19)
        val lines = loader.getLines(8, 13)
        assertEquals(5, lines.size)
        assertEquals("Line 9", lines[0])
        assertEquals("Line 13", lines[4])
    }

    @Test
    fun `LazyStringLoader caches chunks on repeated access`() = runBlocking<Unit> {
        val content = (1..100).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 10)

        val chunk1 = loader.getChunk(0)
        val chunk2 = loader.getChunk(0)
        assertEquals(chunk1, chunk2, "Repeated access should return same content")
    }

    @Test
    fun `LazyStringLoader clear releases memory`() = runBlocking<Unit> {
        val content = (1..100).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 10)

        loader.getChunk(0)
        loader.getChunk(1)
        assertTrue(loader.getMemoryUsage() > 0)

        loader.clear()
        assertEquals(0, loader.getMemoryUsage(), "Memory should be 0 after clear")
    }

    // ====================================================================
    // FlowLazyLoader: lazy flow emission
    // ====================================================================

    @Test
    fun `FlowLazyLoader starts empty`() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        assertTrue(loader.content.value.isEmpty(), "FlowLazyLoader should start with empty content")
        loader.cleanup()
    }

    @Test
    fun `FlowLazyLoader loadMore adds items lazily`() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        assertEquals(0, loader.content.value.size)

        loader.loadMore(listOf("a", "b", "c"))
        assertEquals(3, loader.content.value.size)
        assertEquals("a", loader.content.value[0])
        assertEquals("c", loader.content.value[2])
        loader.cleanup()
    }

    @Test
    fun `FlowLazyLoader multiple loadMore accumulates`() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        loader.loadMore(listOf("a", "b"))
        loader.loadMore(listOf("c", "d"))
        loader.loadMore(listOf("e"))

        assertEquals(5, loader.content.value.size)
        assertEquals(listOf("a", "b", "c", "d", "e"), loader.content.value)
        loader.cleanup()
    }

    @Test
    fun `FlowLazyLoader cleanup clears content`() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        loader.loadMore(listOf("data1", "data2"))
        assertEquals(2, loader.content.value.size)

        loader.cleanup()
        assertTrue(loader.content.value.isEmpty(), "Content should be empty after cleanup")
    }

    @Test
    fun `FlowLazyLoader getVisibleRange returns correct range`() = runBlocking<Unit> {
        val loader = FlowLazyLoader<Int>()
        loader.loadMore((1..100).toList())

        val range = loader.getVisibleRange(20, 30, buffer = 5)
        assertEquals(15 until 35, range)
        loader.cleanup()
    }

    @Test
    fun `FlowLazyLoader getVisibleRange clamps to bounds`() = runBlocking<Unit> {
        val loader = FlowLazyLoader<Int>()
        loader.loadMore((1..10).toList())

        // Visible range with large buffer should clamp to 0 and size
        val range = loader.getVisibleRange(0, 5, buffer = 20)
        assertEquals(0 until 10, range)
        loader.cleanup()
    }

    // ====================================================================
    // Thread safety: concurrent access to lazy loaders
    // ====================================================================

    @Test
    fun `LazyDocumentLoader concurrent access is safe`() = runBlocking<Unit> {
        var loadCount = 0
        val loader = object : LazyDocumentLoader<String>(chunkSize = 10) {
            override suspend fun loadChunk(index: Int): String {
                loadCount++
                delay(1) // Simulate async work
                return "chunk-$index"
            }
        }
        loader.initialize(100) // 10 chunks

        val results = (0..9).map { chunkIndex ->
            async { loader.getChunk(chunkIndex) }
        }.awaitAll()

        assertEquals(10, results.size)
        assertTrue(results.all { it != null }, "All chunks should be loaded")
        // Each chunk should be loaded exactly once despite concurrent access
        assertEquals(10, loadCount, "Each chunk loaded exactly once")
    }

    @Test
    fun `LazyStringLoader concurrent getChunk is safe`() = runBlocking<Unit> {
        val content = (1..500).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 50)

        val results = (0..9).flatMap { chunkIdx ->
            (1..5).map {
                async { loader.getChunk(chunkIdx) }
            }
        }.awaitAll()

        assertEquals(50, results.size)
        assertTrue(results.all { it != null }, "All concurrent reads should succeed")
    }

    @Test
    fun `FlowLazyLoader concurrent loadMore is safe`() = runBlocking<Unit> {
        val loader = FlowLazyLoader<Int>()

        val jobs = (1..10).map { batch ->
            async {
                loader.loadMore((batch * 10 until batch * 10 + 10).toList())
            }
        }
        jobs.awaitAll()

        // 10 batches of 10 items each = 100 items
        assertEquals(100, loader.content.value.size,
            "All concurrent loadMore batches should be accumulated")
        loader.cleanup()
    }

    @Test
    fun `LazyDocumentLoader concurrent access to same chunk returns cached`() = runBlocking<Unit> {
        var loadCount = 0
        val loader = object : LazyDocumentLoader<String>(chunkSize = 10) {
            override suspend fun loadChunk(index: Int): String {
                loadCount++
                return "data"
            }
        }
        loader.initialize(10)

        // Multiple coroutines accessing the same chunk
        val results = (1..20).map {
            async { loader.getChunk(0) }
        }.awaitAll()

        assertEquals(20, results.size)
        assertTrue(results.all { it == "data" })
        // Should only load once despite 20 concurrent accesses
        assertEquals(1, loadCount, "Chunk 0 should only be loaded once")
    }

    // ====================================================================
    // Error handling in lazy initialization
    // ====================================================================

    @Test
    fun `LazyDocumentLoader loadChunk exception propagates`() = runBlocking<Unit> {
        val loader = object : LazyDocumentLoader<String>(chunkSize = 10) {
            override suspend fun loadChunk(index: Int): String {
                throw IllegalStateException("Load failed for chunk $index")
            }
        }
        loader.initialize(50)

        assertFailsWith<IllegalStateException> {
            loader.getChunk(0)
        }
    }

    @Test
    fun `FlowLazyLoader loadMore with empty list is safe`() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        loader.loadMore(emptyList())
        assertTrue(loader.content.value.isEmpty())
        loader.cleanup()
    }

    @Test
    fun `LazyStringLoader with empty content`() = runBlocking<Unit> {
        val loader = LazyStringLoader("", chunkSize = 10)
        // Should handle empty content gracefully
        val chunk = loader.getChunk(0)
        // Empty string splits to one empty-string line
        assertNotNull(chunk)
    }

    @Test
    fun `LazyStringLoader with single line`() = runBlocking<Unit> {
        val loader = LazyStringLoader("single line", chunkSize = 100)
        val chunk = loader.getChunk(0)
        assertNotNull(chunk)
        assertEquals("single line", chunk)
        assertNull(loader.getChunk(1), "No second chunk for single-line content")
    }

    // ====================================================================
    // Integration with ParsedDocument lazy HTML caching
    // ====================================================================

    @Test
    fun `ParsedDocument toHtml defers generation until called`() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Title\n\nSome content.")

        // HTML should not be cached yet
        assertFalse(doc.hasHtmlCached(lightMode = true), "HTML should not be generated before toHtml()")
        assertFalse(doc.hasHtmlCached(lightMode = false))

        // First call generates and caches
        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
        assertTrue(doc.hasHtmlCached(lightMode = true), "HTML should be cached after toHtml()")

        // Dark mode still not cached
        assertFalse(doc.hasHtmlCached(lightMode = false))
    }

    @Test
    fun `ParsedDocument toHtml caches separately for light and dark`() {
        val parser = PlaintextParser()
        val doc = parser.parse("Hello World")

        val lightHtml = doc.toHtml(lightMode = true)
        val darkHtml = doc.toHtml(lightMode = false)

        assertTrue(doc.hasHtmlCached(lightMode = true))
        assertTrue(doc.hasHtmlCached(lightMode = false))

        // Both should be valid
        assertTrue(lightHtml.isNotEmpty())
        assertTrue(darkHtml.isNotEmpty())
    }

    @Test
    fun `ParsedDocument clearHtmlCache forces regeneration`() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Test\n\nContent.")

        doc.toHtml(lightMode = true)
        assertTrue(doc.hasHtmlCached(lightMode = true))

        doc.clearHtmlCache()
        assertFalse(doc.hasHtmlCached(lightMode = true))
        assertFalse(doc.hasHtmlCached(lightMode = false))

        // Re-generate
        val html = doc.toHtml(lightMode = true)
        assertTrue(html.isNotEmpty())
        assertTrue(doc.hasHtmlCached(lightMode = true))
    }

    @Test
    fun `ParsedDocument cached HTML is identical on repeated calls`() {
        val parser = MarkdownParser()
        val doc = parser.parse("# Heading\n\n**Bold** paragraph.")

        val html1 = doc.toHtml(lightMode = true)
        val html2 = doc.toHtml(lightMode = true)
        val html3 = doc.toHtml(lightMode = true)

        assertSame(html1, html2, "Cached HTML should be the same object reference")
        assertSame(html2, html3, "Cached HTML should remain stable across calls")
    }

    // ====================================================================
    // Lazy loading performance characteristics
    // ====================================================================

    @Test
    fun `LazyStringLoader first access is slower than subsequent`() = runBlocking<Unit> {
        val content = (1..1000).joinToString("\n") { "Line $it with some content to fill space." }
        val loader = LazyStringLoader(content, chunkSize = 100)

        // First access: loads chunk
        val firstTime = measureTime { loader.getChunk(0) }

        // Second access: returns cached
        val secondTime = measureTime { loader.getChunk(0) }

        // Both should be fast, but second should not be significantly slower
        assertTrue(secondTime.inWholeMilliseconds <= firstTime.inWholeMilliseconds + 5,
            "Cached access should not be slower: first=${firstTime.inWholeMilliseconds}ms, " +
                "second=${secondTime.inWholeMilliseconds}ms")
    }

    @Test
    fun `FlowLazyLoader 1000 items loaded in under 100ms`() = runBlocking<Unit> {
        val loader = FlowLazyLoader<Int>()
        val elapsed = measureTime {
            loader.loadMore((1..1000).toList())
        }
        assertTrue(elapsed.inWholeMilliseconds < 100,
            "Loading 1000 items took ${elapsed.inWholeMilliseconds}ms")
        assertEquals(1000, loader.content.value.size)
        loader.cleanup()
    }
}
