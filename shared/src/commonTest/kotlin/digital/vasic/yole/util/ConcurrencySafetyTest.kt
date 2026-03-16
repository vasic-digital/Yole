/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Concurrency Safety Tests
 * Tests for thread safety and race conditions
 * Kotlin Multiplatform compatible (no JVM-only imports)
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.*

/**
 * Concurrency Safety Test — validates thread-safe operations using
 * Kotlin coroutine primitives (multiplatform compatible).
 */
class ConcurrencySafetyTest {

    @Test
    fun testMutexProtectedCounter() = runBlocking<Unit> {
        val mutex = Mutex()
        var counter = 0

        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                repeat(100) {
                    mutex.withLock {
                        counter++
                    }
                }
            }
        }

        jobs.forEach { it.join() }

        assertEquals(10000, counter, "All increments should be applied atomically")
    }

    @Test
    fun testConcurrentReadWrite() = runBlocking<Unit> {
        val mutex = Mutex()
        var value = 0
        var totalReads = 0

        val writers = (1..5).map {
            launch(Dispatchers.Default) {
                repeat(50) {
                    mutex.withLock {
                        value++
                    }
                }
            }
        }

        val readers = (1..10).map {
            launch(Dispatchers.Default) {
                repeat(100) {
                    mutex.withLock {
                        totalReads++
                        @Suppress("UNUSED_VARIABLE")
                        val readValue = value
                    }
                }
            }
        }

        (writers + readers).forEach { it.join() }

        assertEquals(250, value, "Writers should complete all increments")
        assertEquals(1000, totalReads, "Readers should complete all reads")
    }

    @Test
    fun testConcurrentMapAccess() = runBlocking<Unit> {
        val mutex = Mutex()
        val map = mutableMapOf<String, Int>()

        val jobs = (1..20).map { i ->
            launch(Dispatchers.Default) {
                repeat(100) {
                    mutex.withLock {
                        map["key$i"] = (map["key$i"] ?: 0) + 1
                    }
                }
            }
        }

        jobs.forEach { it.join() }

        assertTrue(map.isNotEmpty(), "Map should have entries")
        assertEquals(20, map.size, "Map should have 20 keys")
        map.values.forEach { count ->
            assertEquals(100, count, "Each key should have 100 increments")
        }
    }

    @Test
    fun testCoroutineMutex() = runBlocking<Unit> {
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
    fun testChannelThreadSafety() = runBlocking<Unit> {
        val channel = Channel<Int>(10)
        val mutex = Mutex()
        val results = mutableListOf<Int>()

        val sender = launch {
            repeat(50) { i ->
                channel.send(i)
            }
            channel.close()
        }

        val receiver = launch {
            for (item in channel) {
                mutex.withLock {
                    results.add(item)
                }
            }
        }

        sender.join()
        receiver.join()

        assertEquals(50, results.size)
    }

    @Test
    fun testCoroutineCompletion() = runBlocking<Unit> {
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
 * Race Condition Detection Test — uses coroutines instead of threads
 * for multiplatform compatibility.
 */
class RaceConditionDetectionTest {

    @Test
    fun testCheckThenActWithMutex() = runBlocking<Unit> {
        val mutex = Mutex()
        var state: String? = null
        var setCount = 0

        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                mutex.withLock {
                    if (state == null) {
                        delay(1)
                        state = "set"
                        setCount++
                    }
                }
            }
        }

        jobs.forEach { it.join() }

        assertEquals("set", state, "State should be set")
        assertEquals(1, setCount, "Mutex should prevent race: only one setter")
    }

    @Test
    fun testProtectedIncrement() = runBlocking<Unit> {
        val mutex = Mutex()
        var counter = 0

        val jobs = (1..10).map {
            launch(Dispatchers.Default) {
                repeat(1000) {
                    mutex.withLock {
                        counter++
                    }
                }
            }
        }

        jobs.forEach { it.join() }

        assertEquals(10000, counter, "Mutex-protected counter should be exact")
    }
}
