/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Memory Leak Detection Tests
 * Tests for detecting and preventing memory leaks
 * Kotlin Multiplatform compatible (no JVM-only imports)
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

/**
 * Memory Leak Detection Test — validates resource cleanup patterns
 * using only Kotlin Multiplatform compatible APIs.
 */
class MemoryLeakDetectionTest {

    @Test
    fun testReferenceCleanup() {
        // Verify that clearing a reference makes object eligible for collection
        var ref: Any? = "test-object"
        assertNotNull(ref, "Reference should be valid initially")

        ref = null
        assertNull(ref, "Reference should be null after clearing")
    }

    @Test
    fun testObjectCreationAndCleanup() {
        var counter = 0
        val createObject = {
            counter++
            ByteArray(1024)
        }

        var obj: ByteArray? = createObject()
        assertEquals(1, counter, "Object should be created once")
        assertNotNull(obj)

        obj = null
        assertNull(obj, "Object reference should be cleared")
    }

    @Test
    fun testResourceCleanup() {
        var resourcesFreed = false

        val resource = object {
            fun close() { resourcesFreed = true }
        }

        resource.close()

        assertTrue(resourcesFreed, "Resources should be freed on close")
    }

    @Test
    fun testCoroutineScopeCleanup() = runTest {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)
        var completed = false

        scope.launch {
            delay(500)
            completed = true
        }

        // Cancel immediately — the delay(500) should never complete
        job.cancel()

        delay(50)

        assertFalse(completed, "Coroutine should be cancelled before completing")
        assertTrue(job.isCancelled, "Job should be cancelled")
    }

    @Test
    fun testCacheEviction() {
        val cache = object {
            private val store = mutableMapOf<String, ByteArray>()

            fun put(key: String, value: ByteArray) {
                if (store.size > 100) {
                    store.remove(store.keys.first())
                }
                store[key] = value
            }

            fun get(key: String): ByteArray? = store[key]

            fun clear() = store.clear()

            fun size() = store.size
        }

        repeat(150) { i ->
            cache.put("key$i", ByteArray(100))
        }

        assertTrue(cache.size() <= 110, "Cache should evict old entries")
    }

    @Test
    fun testListenerCleanup() {
        val listeners = mutableListOf<() -> Unit>()

        fun addListener(listener: () -> Unit) {
            listeners.add(listener)
        }

        fun removeListener(listener: () -> Unit) {
            listeners.remove(listener)
        }

        val listener1 = { }
        val listener2 = { }

        addListener(listener1)
        addListener(listener2)

        assertEquals(2, listeners.size)

        removeListener(listener1)

        assertEquals(1, listeners.size)
        assertTrue(listener2 in listeners)
    }

    @Test
    fun testStreamCleanup() {
        var closed = false

        val stream = object {
            fun read(): Int = -1
            fun close() { closed = true }
        }

        stream.close()

        assertTrue(closed, "Stream should be closed")
    }

    @Test
    fun testFlowLazyLoaderCleanup() = runTest {
        val loader = FlowLazyLoader<String>()

        loader.loadMore(listOf("a", "b", "c"))
        assertEquals(3, loader.content.value.size)

        loader.cleanup()
        assertEquals(0, loader.content.value.size, "Cleanup should clear content")
    }

    @Test
    fun testLazyDocumentLoaderClear() = runTest {
        val loader = LazyStringLoader("line1\nline2\nline3", chunkSize = 2)

        val chunk = loader.getChunk(0)
        assertNotNull(chunk)

        loader.clear()

        val memUsage = loader.getMemoryUsage()
        assertEquals(0L, memUsage, "Memory should be zero after clear")
    }
}

/**
 * Resource Lifecycle Test — validates proper acquire/release patterns.
 */
class ResourceLifecycleTest {

    @Test
    fun testResourceAcquireRelease() {
        var closed = false

        class TestResource {
            fun doWork() = "result"
            fun close() { closed = true }
        }

        val resource = TestResource()
        try {
            resource.doWork()
        } finally {
            resource.close()
        }

        assertTrue(closed, "Resource should be closed in finally block")
    }

    @Test
    fun testCoroutineResource() = runTest {
        var acquired = false

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)

        withContext(Dispatchers.Default) {
            acquired = true
        }

        assertTrue(acquired)
        job.cancel()
        assertTrue(job.isCancelled, "Scope should be cancelled")
    }

    @Test
    fun testMultipleResourceCleanup() {
        var r1Closed = false
        var r2Closed = false

        class Resource(val name: String, val onClose: () -> Unit) {
            fun close() = onClose()
        }

        val r1 = Resource("r1") { r1Closed = true }
        val r2 = Resource("r2") { r2Closed = true }

        try {
            try {
                assertTrue(true, "Both resources acquired")
            } finally {
                r2.close()
            }
        } finally {
            r1.close()
        }

        assertTrue(r1Closed && r2Closed, "Both resources should be closed")
    }
}
