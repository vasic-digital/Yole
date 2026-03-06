/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Lazy Loading Utilities
 * Memory-efficient loading for large documents
 * Thread-safe via Mutex for Kotlin Multiplatform compatibility
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lazy Document Loader.
 *
 * Loads large documents in chunks to avoid memory issues. All mutable state
 * is protected by a [Mutex] to prevent race conditions during concurrent access.
 *
 * @param T The type of document content
 * @param chunkSize Number of lines to load per chunk
 *
 * @example
 * ```kotlin
 * val loader = LazyStringLoader(largeContent, chunkSize = 500)
 * val firstChunk = loader.getChunk(0) // loads lines 0-499
 * loader.preloadAround(2, range = 1)  // preloads chunks 1, 2, 3
 * ```
 */
open class LazyDocumentLoader<T>(
    protected val chunkSize: Int = 1000
) {
    private val mutex = Mutex()
    private val _chunks = mutableMapOf<Int, T?>()
    private var totalChunks: Int = 0
    private val loadingChunks = mutableSetOf<Int>()

    /**
     * Initialize with total content size.
     */
    fun initialize(totalSize: Int) {
        totalChunks = (totalSize + chunkSize - 1) / chunkSize
    }

    /**
     * Get chunk, loading it if necessary.
     *
     * Thread-safe: uses [Mutex] to guard shared state and prevents duplicate
     * loading of the same chunk by tracking in-progress loads.
     */
    suspend fun getChunk(index: Int): T? {
        mutex.withLock {
            if (index !in 0 until totalChunks) return null
            _chunks[index]?.let { return it }
        }

        // Wait if another coroutine is already loading this chunk
        while (true) {
            val shouldLoad = mutex.withLock {
                // Re-check cache after acquiring lock
                _chunks[index]?.let { return it }

                if (index in loadingChunks) {
                    false // another coroutine is loading, wait
                } else {
                    loadingChunks.add(index)
                    true // we will load
                }
            }

            if (shouldLoad) {
                try {
                    val chunk = loadChunk(index)
                    mutex.withLock {
                        _chunks[index] = chunk
                    }
                    return chunk
                } finally {
                    mutex.withLock {
                        loadingChunks.remove(index)
                    }
                }
            } else {
                delay(10)
            }
        }
    }

    /**
     * Override to implement actual loading.
     */
    protected open suspend fun loadChunk(index: Int): T? = null

    /**
     * Preload chunks around a specific index.
     */
    suspend fun preloadAround(index: Int, range: Int = 2) {
        val maxIndex = mutex.withLock { totalChunks - 1 }
        coroutineScope {
            (maxOf(0, index - range)..minOf(maxIndex, index + range)).forEach { i ->
                launch { getChunk(i) }
            }
        }
    }

    /**
     * Clear loaded chunks to free memory.
     */
    suspend fun clear() {
        mutex.withLock {
            _chunks.clear()
        }
    }

    /**
     * Get memory usage estimate.
     */
    suspend fun getMemoryUsage(): Long {
        return mutex.withLock {
            _chunks.size.toLong() * chunkSize.toLong()
        }
    }
}

/**
 * Lazy String Loader for text documents.
 *
 * Splits content into line-based chunks and loads them on demand.
 *
 * @example
 * ```kotlin
 * val loader = LazyStringLoader(fileContent, chunkSize = 1000)
 * val lines = loader.getLines(500, 600) // loads chunk containing lines 500-600
 * ```
 */
class LazyStringLoader(
    private val content: String,
    chunkSize: Int = 1000
) : LazyDocumentLoader<String>(chunkSize) {

    init {
        initialize(content.lines().size)
    }

    override suspend fun loadChunk(index: Int): String? {
        val lines = content.lines()
        val start = index * chunkSize
        val end = minOf(start + chunkSize, lines.size)

        if (start >= lines.size) return null

        return lines.subList(start, end).joinToString("\n")
    }

    /**
     * Get a range of lines lazily.
     */
    suspend fun getLines(startLine: Int, endLine: Int): List<String> {
        val result = mutableListOf<String>()
        var current = startLine / chunkSize

        while (current * chunkSize < endLine) {
            val chunk = getChunk(current) ?: break
            result.addAll(chunk.lines())
            current++
        }

        return result.drop(startLine % chunkSize).take(endLine - startLine)
    }
}

/**
 * Flow-based lazy loader.
 *
 * Provides a [StateFlow] of loaded content that UI can collect. All mutations
 * are protected by [Mutex]. The internal [CoroutineScope] must be cleaned up
 * via [cleanup] to prevent memory leaks.
 *
 * @example
 * ```kotlin
 * val loader = FlowLazyLoader<String>()
 * loader.loadMore(listOf("line1", "line2"))
 *
 * // Collect in UI
 * loader.content.collect { lines -> display(lines) }
 *
 * // IMPORTANT: call cleanup when done
 * loader.cleanup()
 * ```
 */
class FlowLazyLoader<T>(
    private val chunkSize: Int = 1000
) {
    private val mutex = Mutex()
    private val _content = MutableStateFlow<List<T>>(emptyList())
    val content: StateFlow<List<T>> = _content.asStateFlow()

    private var loadedChunks = 0
    private val parentJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + parentJob)

    /**
     * Load more content (thread-safe).
     */
    suspend fun loadMore(items: List<T>) {
        mutex.withLock {
            val current = _content.value.toMutableList()
            current.addAll(items)
            _content.value = current
            loadedChunks++
        }
    }

    /**
     * Get visible range with buffer.
     */
    fun getVisibleRange(visibleStart: Int, visibleEnd: Int, buffer: Int = 10): IntRange {
        val size = _content.value.size
        val start = maxOf(0, visibleStart - buffer)
        val end = minOf(size, visibleEnd + buffer)
        return start until end
    }

    /**
     * Cleanup resources. MUST be called when the loader is no longer needed
     * to prevent memory leaks from the internal coroutine scope.
     */
    fun cleanup() {
        parentJob.cancel()
        _content.value = emptyList()
    }
}
