package digital.vasic.yole.network.common

import kotlinx.datetime.Instant

/**
 * Represents a network storage location with its metadata and status.
 * This is the primary data class for storage configuration and management.
 */
data class NetworkStorage(
    /**
     * Unique identifier for this storage instance
     */
    val id: String,
    
    /**
     * Human-readable name for this storage
     */
    val name: String,
    
    /**
     * Type of storage (WebDAV, FTP, SFTP, SMB, Google Drive, Dropbox, OneDrive)
     */
    val type: StorageType,
    
    /**
     * Location/endpoint URL or path for the storage
     */
    val location: String,
    
    /**
     * Total storage space in bytes (null if unknown)
     */
    val totalSpace: Long? = null,
    
    /**
     * Currently used space in bytes (null if unknown)
     */
    val usedSpace: Long? = null,
    
    /**
     * Whether this storage is currently online and accessible
     */
    val isOnline: Boolean = false,
    
    /**
     * Last synchronization timestamp (null if never synced)
     */
    val lastSync: Instant? = null,
    
    /**
     * Storage-specific configuration and metadata
     */
    val metadata: Map<String, String> = emptyMap(),
    
    /**
     * Whether this storage is enabled for use
     */
    val isEnabled: Boolean = true,
    
    /**
     * Priority for display and operations (lower number = higher priority)
     */
    val priority: Int = 100,
    
    /**
     * Whether this storage supports folder operations
     */
    val supportsFolders: Boolean = true,
    
    /**
     * Whether this storage supports file metadata operations
     */
    val supportsMetadata: Boolean = true,
    
    /**
     * Maximum file size supported in bytes (null if no limit)
     */
    val maxFileSize: Long? = null,
    
    /**
     * Supported file extensions (empty list means all files supported)
     */
    val supportedExtensions: List<String> = emptyList()
) {
    /**
     * Available space in bytes
     */
    val availableSpace: Long?
        get() = if (totalSpace != null && usedSpace != null) {
            totalSpace - usedSpace
        } else null
    
    /**
     * Storage usage as a percentage (0.0 to 1.0)
     */
    val usagePercentage: Double?
        get() = if (totalSpace != null && usedSpace != null && totalSpace > 0) {
            usedSpace.toDouble() / totalSpace.toDouble()
        } else null
    
    /**
     * Whether the storage is currently at capacity
     */
    val isFull: Boolean
        get() = availableSpace == 0L
    
    /**
     * Whether the storage is running low on space (less than 10% available)
     */
    val isLowOnSpace: Boolean
        get() = usagePercentage?.let { it > 0.9 } ?: false
    
    /**
     * Whether a file with the given extension is supported
     */
    fun supportsExtension(extension: String): Boolean {
        return supportedExtensions.isEmpty() || 
               supportedExtensions.any { it.equals(extension, ignoreCase = true) }
    }
    
    /**
     * Whether a file with the given name is supported based on its extension
     */
    fun supportsFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").ifEmpty { return true }
        return supportsExtension(extension)
    }
    
    /**
     * Create a copy with updated online status
     */
    fun withOnlineStatus(isOnline: Boolean): NetworkStorage {
        return copy(isOnline = isOnline)
    }
    
    /**
     * Create a copy with updated last sync timestamp
     */
    fun withLastSync(lastSync: Instant): NetworkStorage {
        return copy(lastSync = lastSync)
    }
    
    /**
     * Create a copy with updated space information
     */
    fun withSpaceInfo(totalSpace: Long?, usedSpace: Long?): NetworkStorage {
        return copy(totalSpace = totalSpace, usedSpace = usedSpace)
    }
    
    companion object {
        /**
         * Create a mock network storage for testing purposes
         */
        fun mock(
            id: String = "mock-storage",
            name: String = "Mock Storage",
            type: StorageType = StorageType.WEBDAV,
            location: String = "mock://localhost",
            isOnline: Boolean = true
        ): NetworkStorage {
            return NetworkStorage(
                id = id,
                name = name,
                type = type,
                location = location,
                totalSpace = 1000000000L, // 1GB
                usedSpace = 100000000L,  // 100MB
                isOnline = isOnline,
                lastSync = kotlinx.datetime.Clock.System.now()
            )
        }
    }
}