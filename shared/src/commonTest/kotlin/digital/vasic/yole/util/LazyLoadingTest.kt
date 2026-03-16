/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Lazy Loading Utilities
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LazyStringLoaderTest {

    @Test
    fun testGetChunkReturnsCorrectContent() = runBlocking<Unit> {
        val content = (1..100).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 10)

        val chunk0 = loader.getChunk(0)
        assertNotNull(chunk0)
        assertTrue(chunk0.startsWith("Line 1"))
    }

    @Test
    fun testGetChunkOutOfRangeReturnsNull() = runBlocking<Unit> {
        val content = "single line"
        val loader = LazyStringLoader(content, chunkSize = 100)

        val chunk = loader.getChunk(99)
        assertNull(chunk)
    }

    @Test
    fun testGetLinesReturnsCorrectRange() = runBlocking<Unit> {
        val content = (1..50).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 10)

        val lines = loader.getLines(0, 5)
        assertEquals(5, lines.size)
        assertEquals("Line 1", lines[0])
        assertEquals("Line 5", lines[4])
    }

    @Test
    fun testClearFreesChunks() = runBlocking<Unit> {
        val content = (1..100).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 10)

        loader.getChunk(0)
        loader.getChunk(1)
        assertTrue(loader.getMemoryUsage() > 0)

        loader.clear()
        assertEquals(0, loader.getMemoryUsage())
    }

    @Test
    fun testPreloadAroundLoadsAdjacentChunks() = runBlocking<Unit> {
        val content = (1..100).joinToString("\n") { "Line $it" }
        val loader = LazyStringLoader(content, chunkSize = 10)

        loader.preloadAround(5, range = 2)
        // Chunks 3, 4, 5, 6, 7 should be loaded
        assertNotNull(loader.getChunk(3))
        assertNotNull(loader.getChunk(4))
        assertNotNull(loader.getChunk(5))
        assertNotNull(loader.getChunk(6))
        assertNotNull(loader.getChunk(7))
    }
}

class FlowLazyLoaderTest {

    @Test
    fun testInitialContentIsEmpty() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        assertTrue(loader.content.value.isEmpty())
        loader.cleanup()
    }

    @Test
    fun testLoadMoreAddsItems() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        loader.loadMore(listOf("a", "b", "c"))
        assertEquals(3, loader.content.value.size)
        assertEquals("a", loader.content.value[0])
        loader.cleanup()
    }

    @Test
    fun testMultipleLoadMoreAccumulates() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        loader.loadMore(listOf("a", "b"))
        loader.loadMore(listOf("c", "d"))
        assertEquals(4, loader.content.value.size)
        loader.cleanup()
    }

    @Test
    fun testGetVisibleRange() = runBlocking<Unit> {
        val loader = FlowLazyLoader<Int>()
        loader.loadMore((1..50).toList())
        val range = loader.getVisibleRange(5, 15, buffer = 3)
        assertEquals(2 until 18, range)
        loader.cleanup()
    }

    @Test
    fun testCleanupClearsContent() = runBlocking<Unit> {
        val loader = FlowLazyLoader<String>()
        loader.loadMore(listOf("data"))
        assertEquals(1, loader.content.value.size)
        loader.cleanup()
        assertTrue(loader.content.value.isEmpty())
    }
}
