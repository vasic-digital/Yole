/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * LRU cache for parsed documents
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

/**
 * Thread-safe LRU (Least Recently Used) cache for [ParsedDocument] instances.
 *
 * Evicts the least recently accessed entry when [maxSize] is exceeded.
 * All operations are protected by a [Mutex] for coroutine safety.
 *
 * @param maxSize Maximum number of entries. Defaults to 100.
 */
class DocumentCache(private val maxSize: Int = 100) {
    private val mutex = Mutex()
    // Use MutableMap + accessOrder list for LRU — LinkedHashMap(accessOrder=true) is JVM-only
    private val cache = mutableMapOf<String, ParsedDocument>()
    private val accessOrder = mutableListOf<String>()

    @kotlin.concurrent.Volatile
    private var _hits = 0L
    @kotlin.concurrent.Volatile
    private var _misses = 0L
    @kotlin.concurrent.Volatile
    private var _size = 0

    /** Current number of cached entries. Volatile read — safe without lock. */
    val size: Int get() = _size
    /** Total cache hits. Volatile read — safe without lock. */
    val hits: Long get() = _hits
    /** Total cache misses. Volatile read — safe without lock. */
    val misses: Long get() = _misses
    /** Hit rate as a ratio [0.0, 1.0]. Volatile reads — may be slightly stale. */
    val hitRate: Double get() {
        val h = _hits
        val m = _misses
        return if (h + m == 0L) 0.0 else h.toDouble() / (h + m)
    }

    suspend fun get(key: String): ParsedDocument? {
        yield()
        return mutex.withLock {
            val value = cache[key]
            if (value != null) {
                _hits++
                // Move to end of access order (most recently used)
                accessOrder.remove(key)
                accessOrder.add(key)
            } else {
                _misses++
            }
            value
        }
    }

    suspend fun put(key: String, document: ParsedDocument) = mutex.withLock {
        if (cache.containsKey(key)) {
            accessOrder.remove(key)
        }
        cache[key] = document
        accessOrder.add(key)
        // Evict least recently used (first in accessOrder)
        while (cache.size > maxSize && accessOrder.isNotEmpty()) {
            val eldest = accessOrder.removeFirst()
            cache.remove(eldest)
        }
        _size = cache.size
    }

    suspend fun invalidate(key: String) = mutex.withLock {
        cache.remove(key)
        accessOrder.remove(key)
        _size = cache.size
    }

    suspend fun clear() = mutex.withLock {
        cache.clear()
        accessOrder.clear()
        _hits = 0
        _misses = 0
        _size = 0
    }

    suspend fun contains(key: String): Boolean = mutex.withLock {
        cache.containsKey(key)
    }
}
