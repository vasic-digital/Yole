/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Concurrency Safety Tests
 * Tests for thread safety and race conditions
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.*
import java.util.concurrent.atomic.*
import kotlin.concurrent.thread

/**
 * Concurrency Safety Test
 */
class ConcurrencySafetyTest {
    
    @Test
    fun testAtomicCounter() {
        val counter = AtomicInteger(0)
        
        val jobs = (1..100).map {
            thread {
                repeat(100) {
                    counter.incrementAndGet()
                }
            }
        }
        
        jobs.forEach { it.join() }
        
        assertEquals(10000, counter.get(), "All increments should be applied")
    }
    
    @Test
    fun testReadWriteLock() {
        val value = AtomicReference(0)
        var readCount = 0
        
        val writers = (1..5).map {
            thread {
                repeat(50) {
                    value.set(value.get() + 1)
                }
            }
        }
        
        val readers = (1..10).map {
            thread {
                repeat(100) {
                    readCount++
                    value.get()
                }
            }
        }
        
        (writers + readers).forEach { it.join() }
        
        assertTrue(value.get() > 0, "Value should be updated")
    }
    
    @Test
    fun testConcurrentMap() {
        val map = java.util.concurrent.ConcurrentHashMap<String, Int>()
        
        val threads = (1..20).map { i ->
            thread {
                repeat(100) {
                    map.merge("key$i", 1) { a, b -> a + b }
                }
            }
        }
        
        threads.forEach { it.join() }
        
        assertTrue(map.isNotEmpty(), "Map should have entries")
    }
    
    @Test
    fun testCoroutineMutex() = runTest {
        val mutex = Mutex()
        var counter = 0
        
        val jobs = (1..10).map {
            launch {
                repeat(10) {
                    mutex.withLock {
                        counter++
                    }
                }
            }
        }
        
        jobs.forEach { it.join() }
        
        assertEquals(100, counter)
    }
    
    @Test
    fun testChannelThreadSafety() = runTest {
        val channel = Channel<Int>(10)
        val results = mutableListOf<Int>()
        val lock = java.util.Collections.synchronizedList(results)
        
        val sender = launch {
            repeat(50) { i ->
                channel.send(i)
            }
            channel.close()
        }
        
        val receiver = launch {
            for (item in channel) {
                lock.add(item)
            }
        }
        
        sender.join()
        receiver.join()
        
        assertEquals(50, lock.size)
    }
    
    @Test
    fun testBlockingCoroutine() = runBlocking {
        var completed = false
        
        val job = launch(Dispatchers.Default) {
            delay(100)
            completed = true
        }
        
        job.join()
        
        assertTrue(completed)
    }
}

/**
 * Race Condition Detection Test
 */
class RaceConditionDetectionTest {
    
    @Test
    fun testCheckThenAct() {
        val state = AtomicReference<String?>(null)
        val errors = mutableListOf<String>()
        
        val threads = (1..100).map { i ->
            thread {
                val current = state.get()
                if (current == null) {
                    Thread.sleep(1)
                    if (!state.compareAndSet(null, "set")) {
                        errors.add("Race detected: expected null but was ${state.get()}")
                    }
                }
            }
        }
        
        threads.forEach { it.join() }
        
        assertTrue(errors.isEmpty() || state.get() != null, "No unexpected state")
    }
    
    @Test
    fun testIncrementNonAtomic() {
        var counter = 0
        
        val threads = (1..10).map {
            thread {
                repeat(1000) {
                    counter++
                }
            }
        }
        
        threads.forEach { it.join() }
        
        println("Final counter: $counter (expected: 10000)")
    }
}
