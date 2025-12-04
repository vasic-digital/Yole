package digital.vasic.yole.network.common

import kotlinx.datetime.Instant

/**
 * Represents a cached file or document entry in the local cache.
 * This data class manages local copies of remote documents for offline access.
 */
data class CacheEntry(
    /**
     * Unique identifier for this cache entry
     */
    val id: String,
    
    /**
     * Remote document ID this cache entry represents
     */
    val remoteDocumentId: String,
    
    /**
     * Local file path where the cached content is stored
     */
    val localPath: String,
    
    /**
     * Remote path of the original document
     */
    val remotePath: String,
    
    /**
     * Size of the cached file in bytes
     */
    val size: Long,
    
    /**
     * When this cache entry was created
     */
    val createdAt: Instant,
    
    /**
     * When this cache entry was last accessed
     */
    val lastAccessed: Instant,
    
    /**
     * When this cache entry was last modified
     */
    val lastModified: Instant,
    
    /**
     * When this cache entry expires (null for no expiration)
     */
    val expiresAt: Instant? = null,
    
    /**
     * Whether this cache entry is currently valid (not expired)
     */
    val isValid: Boolean = expiresAt?.let { it > kotlinx.datetime.Clock.System.now() } ?: true,
    
    /**
     * Whether this cache entry is pinned (should not be evicted)
     */
    val isPinned: Boolean = false,
    
    /**
     * Whether this cache entry is currently being used
     */
    val isInUse: Boolean = false,
    
    /**
     * Number of times this cache entry has been accessed
     */
    val accessCount: Int = 0,
    
    /**
     * Content type/MIME type of the cached file
     */
    val contentType: String? = null,
    
    /**
     * Checksum/hash of the cached content for integrity verification
     */
    val checksum: String? = null,
    
    /**
     * Compression algorithm used (null if uncompressed)
     */
    val compression: String? = null,
    
    /**
     * Original size before compression (null if uncompressed)
     */
    val originalSize: Long? = null,
    
    /**
     * Cache priority for eviction (higher number = less likely to be evicted)
     */
    val priority: Int = 100,
    
    /**
     * Additional metadata
     */
    val metadata: Map<String, String> = emptyMap(),
    
    /**
     * Tags for categorization
     */
    val tags: List<String> = emptyList()
) {
    /**
     * Whether this cache entry is expired
     */
    val isExpired: Boolean
        get() = !isValid
    
    /**
     * Whether this cache entry can be safely evicted (not pinned and not in use)
     */
    val canBeEvicted: Boolean
        get() = !isPinned && !isInUse
    
    /**
     * Compression ratio (null if uncompressed)
     */
    val compressionRatio: Double?
        get() = if (compression != null && originalSize != null && size > 0) {
            size.toDouble() / originalSize.toDouble()
        } else null
    
    /**
     * Age of this cache entry in milliseconds
     */
    val age: Long
        get() = kotlinx.datetime.Clock.System.now().minus(createdAt).inWholeMilliseconds
    
    /**
     * Time since last access in milliseconds
     */
    val timeSinceLastAccess: Long
        get() = kotlinx.datetime.Clock.System.now().minus(lastAccessed).inWholeMilliseconds
    
    /**
     * Access frequency (accesses per day, approximate)
     */
    val accessFrequency: Double
        get() = if (age > 0) {
            (accessCount.toDouble() / age.toDouble()) * (24 * 60 * 60 * 1000)
        } else 0.0
    
    /**
     * Create a copy with updated access information
     */
    fun withAccess(): CacheEntry {
        val now = kotlinx.datetime.Clock.System.now()
        return copy(
            lastAccessed = now,
            accessCount = accessCount + 1,
            isInUse = true
        )
    }
    
    /**
     * Create a copy with the file no longer in use
     */
    fun withNotInUse(): CacheEntry {
        return copy(isInUse = false)
    }
    
    /**
     * Create a copy with updated expiration time
     */
    fun withExpiration(expiresAt: Instant?): CacheEntry {
        return copy(
            expiresAt = expiresAt,
            isValid = expiresAt?.let { it > kotlinx.datetime.Clock.System.now() } ?: true
        )
    }
    
    /**
     * Create a copy with updated content information
     */
    fun withContent(size: Long, checksum: String?, lastModified: Instant): CacheEntry {
        return copy(
            size = size,
            checksum = checksum,
            lastModified = lastModified
        )
    }
    
    /**
     * Create a copy with pin status updated
     */
    fun withPinStatus(isPinned: Boolean): CacheEntry {
        return copy(isPinned = isPinned)
    }
    
    /**
     * Create a copy with updated priority
     */
    fun withPriority(priority: Int): CacheEntry {
        return copy(priority = priority)
    }
    
    companion object {
        /**
         * Create a new cache entry for a document
         */
        fun create(
            remoteDocumentId: String,
            localPath: String,
            remotePath: String,
            size: Long,
            contentType: String? = null,
            ttl: Long? = null, // Time to live in milliseconds
            isPinned: Boolean = false
        ): CacheEntry {
            val now = kotlinx.datetime.Clock.System.now()
            return CacheEntry(
                id = "cache-${now.epochSeconds}-${remoteDocumentId.hashCode()}",
                remoteDocumentId = remoteDocumentId,
                localPath = localPath,
                remotePath = remotePath,
                size = size,
                createdAt = now,
                lastAccessed = now,
                lastModified = now,
                expiresAt = ttl?.let { now.plus(it, kotlinx.datetime.DateTimeUnit.MILLISECOND) },
                isPinned = isPinned,
                contentType = contentType
            )
        }
        
        /**
         * Create a mock cache entry for testing
         */
        fun mock(
            remoteDocumentId: String = "doc1",
            localPath: String = "/cache/test.txt",
            remotePath: String = "/test.txt",
            size: Long = 1024L
        ): CacheEntry {
            val now = kotlinx.datetime.Clock.System.now()
            return CacheEntry(
                id = "mock-cache-${now.epochSeconds}",
                remoteDocumentId = remoteDocumentId,
                localPath = localPath,
                remotePath = remotePath,
                size = size,
                createdAt = now.minus(1, kotlinx.datetime.DateTimeUnit.HOUR),
                lastAccessed = now.minus(30, kotlinx.datetime.DateTimeUnit.MINUTE),
                lastModified = now.minus(1, kotlinx.datetime.DateTimeUnit.HOUR),
                isValid = true,
                isPinned = false,
                accessCount = 5
            )
        }
    }
}