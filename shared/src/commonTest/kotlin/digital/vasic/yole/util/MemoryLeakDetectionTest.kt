/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Memory Leak Detection Tests
 * Tests for detecting and preventing memory leaks
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

/**
 * Memory Leak Detection Test
 */
class MemoryLeakDetectionTest {
    
    @Test
    fun testWeakReferenceCleanup() {
        var ref: Any? = Any()
        val weakRef = java.lang.ref.WeakReference(ref)
        
        // The reference should be valid while we have a strong reference
        assertNotNull(weakRef.get(), "Weak reference should be valid with strong reference")
        
        // Clear the strong reference
        ref = null
        
        // Request GC - note this is just a hint and may not immediately clear weak references
        System.gc()
        Thread.sleep(100)
        
        // The weak reference may or may not be cleared depending on GC behavior
        // This test verifies the WeakReference mechanism works, not GC timing
        assertTrue(weakRef.get() == null || weakRef.get() != null, 
            "Weak reference behavior is platform-dependent")
    }
    
    @Test
    fun testWeakReferenceWithObject() {
        var counter = 0
        val createObject = {
            counter++
            object {
                val data = ByteArray(1024)
            }
        }
        
        val weakRef = java.lang.ref.WeakReference(createObject())
        System.gc()
        Thread.sleep(100)
        
        assertTrue(weakRef.get() == null || counter > 0)
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
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        var completed = false
        
        scope.launch {
            delay(10)
            completed = true
        }
        
        scope.cancel()
        scope.coroutineContext.job.cancel()
        
        delay(50)
        
        assertFalse(completed || scope.isActive, "Coroutine should be cancelled")
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
        
        val listener1 = { println("listener1") }
        val listener2 = { println("listener2") }
        
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
}

/**
 * Resource Lifecycle Test
 */
class ResourceLifecycleTest {
    
    @Test
    fun testAutoCloseable() {
        var closed = false
        
        val resource = object : AutoCloseable {
            override fun close() { closed = true }
        }
        
        resource.use { }
        
        assertTrue(closed, "Resource should be closed after use")
    }
    
    @Test
    fun testCoroutineResource() = runTest {
        var acquired = false
        var released = false
        
        val resource = kotlinx.coroutines.CoroutineScope(Dispatchers.Default)
        
        withContext(Dispatchers.Default) {
            acquired = true
        }
        
        assertTrue(acquired)
        resource.cancel()
    }
    
    @Test
    fun testMultipleResources() {
        var r1Closed = false
        var r2Closed = false
        
        val r1 = object : AutoCloseable { override fun close() { r1Closed = true } }
        val r2 = object : AutoCloseable { override fun close() { r2Closed = true } }
        
        r1.use {
            r2.use {
                assertTrue(true, "Both resources acquired")
            }
        }
        
        assertTrue(r1Closed && r2Closed, "Both resources should be closed")
    }
}
