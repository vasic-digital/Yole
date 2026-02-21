/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Lazy Loading Utilities
 * Memory-efficient loading for large documents
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Lazy Document Loader
 * Loads large documents in chunks to avoid memory issues
 *
 * @param T The type of document content
 * @param chunkSize Number of lines to load per chunk
 * @param scope Coroutine scope for loading operations
 */
class LazyDocumentLoader<T>(
    private val chunkSize: Int = 1000,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _chunks = mutableMapOf<Int, T>()
    private var totalChunks: Int = 0
    private var loadingChunks = mutableSetOf<Int>()
    
    /**
     * Initialize with total content size
     */
    fun initialize(totalSize: Int) {
        totalChunks = (totalSize + chunkSize - 1) / chunkSize
    }
    
    /**
     * Get chunk, loading it if necessary
     */
    suspend fun getChunk(index: Int): T? = withContext(Dispatchers.IO) {
        if (index !in 0 until totalChunks) return@withContext null
        
        _chunks[index]?.let { return@withContext it }
        
        if (index in loadingChunks) {
            while (index in loadingChunks) {
                delay(50)
            }
            return@withContext _chunks[index]
        }
        
        loadingChunks.add(index)
        try {
            val chunk = loadChunk(index)
            _chunks[index] = chunk
            chunk
        } finally {
            loadingChunks.remove(index)
        }
    }
    
    /**
     * Override to implement actual loading
     */
    protected open suspend fun loadChunk(index: Int): T? = null
    
    /**
     * Preload chunks around a specific index
     */
    suspend fun preloadAround(index: Int, range: Int = 2) {
        coroutineScope {
            (maxOf(0, index - range)..minOf(totalChunks - 1, index + range)).forEach { i ->
                launch { getChunk(i) }
            }
        }
    }
    
    /**
     * Clear loaded chunks to free memory
     */
    fun clear() {
        _chunks.clear()
    }
    
    /**
     * Get memory usage estimate
     */
    fun getMemoryUsage(): Long {
        return _chunks.size.toLong() * chunkSize.toLong()
    }
}

/**
 * Lazy String Loader for text documents
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
     * Get a range of lines lazily
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
 * Flow-based lazy loader
 */
class FlowLazyLoader<T>(
    private val chunkSize: Int = 1000
) {
    private val _content = MutableStateFlow<List<T>>(emptyList())
    val content: StateFlow<List<T>> = _content.asStateFlow()
    
    private var loadedChunks = 0
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Load more content
     */
    suspend fun loadMore(items: List<T>) {
        val current = _content.value.toMutableList()
        current.addAll(items)
        _content.value = current
        loadedChunks++
    }
    
    /**
     * Load chunk asynchronously
     */
    fun loadChunkAsync(loader: suspend (Int) -> List<T>) {
        scope.launch {
            val chunk = loader(loadedChunks)
            loadMore(chunk)
        }
    }
    
    /**
     * Get visible range with buffer
     */
    fun getVisibleRange(visibleStart: Int, visibleEnd: Int, buffer: Int = 10): IntRange {
        val start = maxOf(0, visibleStart - buffer)
        val end = minOf(_content.value.size, visibleEnd + buffer)
        return start until end
    }
    
    /**
     * Cleanup
     */
    fun cleanup() {
        scope.cancel()
        _content.value = emptyList()
    }
}
