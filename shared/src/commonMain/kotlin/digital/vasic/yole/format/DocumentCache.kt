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
    private val cache = LinkedHashMap<String, ParsedDocument>(maxSize + 1, 0.75f, true)

    private var _hits = 0L
    private var _misses = 0L

    val size: Int get() = kotlinx.coroutines.runBlocking { mutex.withLock { cache.size } }
    val hits: Long get() = kotlinx.coroutines.runBlocking { mutex.withLock { _hits } }
    val misses: Long get() = kotlinx.coroutines.runBlocking { mutex.withLock { _misses } }
    val hitRate: Double get() = kotlinx.coroutines.runBlocking {
        mutex.withLock {
            if (_hits + _misses == 0L) 0.0 else _hits.toDouble() / (_hits + _misses)
        }
    }

    suspend fun get(key: String): ParsedDocument? {
        yield()
        return mutex.withLock {
            val value = cache[key]
            if (value != null) _hits++ else _misses++
            value
        }
    }

    suspend fun put(key: String, document: ParsedDocument) = mutex.withLock {
        cache[key] = document
        if (cache.size > maxSize) {
            val eldest = cache.entries.first()
            cache.remove(eldest.key)
        }
    }

    suspend fun invalidate(key: String) = mutex.withLock {
        cache.remove(key)
    }

    suspend fun clear() = mutex.withLock {
        cache.clear()
        _hits = 0
        _misses = 0
    }

    suspend fun contains(key: String): Boolean = mutex.withLock {
        cache.containsKey(key)
    }
}
