/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for platformSynchronized utility function.
 * Validates synchronization behavior across all KMP targets.
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlin.test.*

/**
 * Tests for the [platformSynchronized] facade function.
 *
 * Tests cover:
 * - Basic execution inside a synchronized block
 * - Return value correctness
 * - Different lock objects
 * - Nested synchronized calls
 * - Exception propagation
 * - Side effects within synchronized blocks
 * - Null return values
 * - Lock identity semantics
 */
class PlatformSyncTest {

    // ==================== Basic Execution Tests ====================

    @Test
    fun `platformSynchronized executes block`() {
        val lock = Any()
        var executed = false

        platformSynchronized(lock) {
            executed = true
        }

        assertTrue(executed, "Block should have been executed inside platformSynchronized")
    }

    @Test
    fun `platformSynchronized returns value correctly`() {
        val lock = Any()

        val result = platformSynchronized(lock) {
            42
        }

        assertEquals(42, result, "platformSynchronized should return the block's result")
    }

    @Test
    fun `platformSynchronized returns string value`() {
        val lock = Any()

        val result = platformSynchronized(lock) {
            "hello world"
        }

        assertEquals("hello world", result)
    }

    @Test
    fun `platformSynchronized returns null value`() {
        val lock = Any()

        val result: String? = platformSynchronized(lock) {
            null
        }

        assertNull(result, "platformSynchronized should be able to return null")
    }

    @Test
    fun `platformSynchronized returns complex object`() {
        val lock = Any()

        val result = platformSynchronized(lock) {
            mapOf("key1" to 1, "key2" to 2, "key3" to 3)
        }

        assertEquals(3, result.size)
        assertEquals(1, result["key1"])
        assertEquals(2, result["key2"])
        assertEquals(3, result["key3"])
    }

    // ==================== Lock Object Tests ====================

    @Test
    fun `platformSynchronized works with different lock objects`() {
        val lock1 = Any()
        val lock2 = Any()

        val result1 = platformSynchronized(lock1) { "from lock1" }
        val result2 = platformSynchronized(lock2) { "from lock2" }

        assertEquals("from lock1", result1)
        assertEquals("from lock2", result2)
    }

    @Test
    fun `platformSynchronized works with string as lock`() {
        val lock = "my_lock_object"

        val result = platformSynchronized(lock) { 100 }

        assertEquals(100, result)
    }

    @Test
    fun `platformSynchronized works with custom object as lock`() {
        data class LockHolder(val id: Int)
        val lock = LockHolder(1)

        val result = platformSynchronized(lock) {
            "synchronized with custom object"
        }

        assertEquals("synchronized with custom object", result)
    }

    @Test
    fun `same lock object can be reused sequentially`() {
        val lock = Any()
        val results = mutableListOf<Int>()

        platformSynchronized(lock) { results.add(1) }
        platformSynchronized(lock) { results.add(2) }
        platformSynchronized(lock) { results.add(3) }

        assertEquals(listOf(1, 2, 3), results, "Sequential calls with same lock should all execute")
    }

    // ==================== Nested Synchronization Tests ====================

    @Test
    fun `nested platformSynchronized calls with different locks`() {
        val outerLock = Any()
        val innerLock = Any()

        val result = platformSynchronized(outerLock) {
            val outerValue = 10
            val innerValue = platformSynchronized(innerLock) {
                20
            }
            outerValue + innerValue
        }

        assertEquals(30, result, "Nested synchronized blocks with different locks should work")
    }

    @Test
    fun `nested platformSynchronized calls with same lock`() {
        val lock = Any()

        // Reentrant locking: on JVM synchronized is reentrant,
        // on other platforms this should still work (single-threaded or reentrant).
        val result = platformSynchronized(lock) {
            val outer = 5
            val inner = platformSynchronized(lock) {
                10
            }
            outer + inner
        }

        assertEquals(15, result, "Reentrant synchronized should not deadlock")
    }

    @Test
    fun `deeply nested platformSynchronized calls`() {
        val lock1 = Any()
        val lock2 = Any()
        val lock3 = Any()

        val result = platformSynchronized(lock1) {
            platformSynchronized(lock2) {
                platformSynchronized(lock3) {
                    "deeply nested"
                }
            }
        }

        assertEquals("deeply nested", result)
    }

    // ==================== Exception Propagation Tests ====================

    @Test
    fun `platformSynchronized propagates exceptions`() {
        val lock = Any()

        val exception = assertFailsWith<IllegalStateException> {
            platformSynchronized(lock) {
                throw IllegalStateException("test error")
            }
        }

        assertEquals("test error", exception.message)
    }

    @Test
    fun `platformSynchronized propagates IllegalArgumentException`() {
        val lock = Any()

        assertFailsWith<IllegalArgumentException> {
            platformSynchronized(lock) {
                throw IllegalArgumentException("bad argument")
            }
        }
    }

    @Test
    fun `platformSynchronized propagates RuntimeException`() {
        val lock = Any()

        val exception = assertFailsWith<RuntimeException> {
            platformSynchronized(lock) {
                throw RuntimeException("runtime error")
            }
        }

        assertEquals("runtime error", exception.message)
    }

    @Test
    fun `exception in nested synchronized propagates to outer`() {
        val outerLock = Any()
        val innerLock = Any()

        assertFailsWith<UnsupportedOperationException> {
            platformSynchronized(outerLock) {
                platformSynchronized(innerLock) {
                    throw UnsupportedOperationException("inner error")
                }
            }
        }
    }

    // ==================== Side Effect Tests ====================

    @Test
    fun `platformSynchronized applies side effects`() {
        val lock = Any()
        val list = mutableListOf<String>()

        platformSynchronized(lock) {
            list.add("first")
            list.add("second")
            list.add("third")
        }

        assertEquals(3, list.size)
        assertEquals("first", list[0])
        assertEquals("second", list[1])
        assertEquals("third", list[2])
    }

    @Test
    fun `platformSynchronized with counter increments`() {
        val lock = Any()
        var counter = 0

        repeat(100) {
            platformSynchronized(lock) {
                counter++
            }
        }

        assertEquals(100, counter, "Counter should be incremented 100 times")
    }

    @Test
    fun `platformSynchronized with map mutations`() {
        val lock = Any()
        val map = mutableMapOf<String, Int>()

        platformSynchronized(lock) {
            map["a"] = 1
            map["b"] = 2
        }

        platformSynchronized(lock) {
            map["c"] = 3
        }

        assertEquals(3, map.size)
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
        assertEquals(3, map["c"])
    }

    // ==================== Type Inference Tests ====================

    @Test
    fun `platformSynchronized with Unit return type`() {
        val lock = Any()

        val result: Unit = platformSynchronized(lock) {
            // no return value
        }

        assertEquals(Unit, result)
    }

    @Test
    fun `platformSynchronized with Boolean return type`() {
        val lock = Any()

        val result = platformSynchronized(lock) {
            true
        }

        assertTrue(result)
    }

    @Test
    fun `platformSynchronized with List return type`() {
        val lock = Any()

        val result = platformSynchronized(lock) {
            listOf(1, 2, 3, 4, 5)
        }

        assertEquals(5, result.size)
        assertEquals(listOf(1, 2, 3, 4, 5), result)
    }

    @Test
    fun `platformSynchronized with Pair return type`() {
        val lock = Any()

        val result = platformSynchronized(lock) {
            Pair("key", 42)
        }

        assertEquals("key", result.first)
        assertEquals(42, result.second)
    }
}
