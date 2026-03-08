/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/
package digital.vasic.yole.network

import kotlinx.coroutines.flow.Flow
import digital.vasic.yole.network.common.NetworkStorage
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.StorageConfig
import digital.vasic.yole.network.common.NetworkOperation
import digital.vasic.yole.network.common.SyncStatus
import digital.vasic.yole.network.common.CacheEntry

/**
 * Main interface for network storage operations.
 * This interface provides a unified API for working with different network storage types.
 */
interface NetworkStorageService {
    
    /**
     * Configuration for this storage service
     */
    val config: StorageConfig
    
    /**
     * Connect to the network storage
     * @return Result indicating success or failure
     */
    suspend fun connect(): Result<Unit>
    
    /**
     * Disconnect from the network storage
     * @return Result indicating success or failure
     */
    suspend fun disconnect(): Result<Unit>
    
    /**
     * Whether the service is currently connected and online
     */
    val isOnline: Boolean
    
    /**
     * List files and folders in the specified path
     * @param path Path to list (root = "/")
     * @return Flow emitting list results or errors
     */
    fun listFiles(path: String = "/"): Flow<Result<List<NetworkDocument>>>
    
    /**
     * Download a file from remote storage to local storage
     * @param remotePath Path to the remote file
     * @param localPath Local path where the file should be saved
     * @return Flow emitting operation progress
     */
    suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation>
    
    /**
     * Upload a local file to remote storage
     * @param localPath Local path to the file to upload
     * @param remotePath Remote path where the file should be saved
     * @return Flow emitting operation progress
     */
    suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation>
    
    /**
     * Delete a file or folder from remote storage
     * @param remotePath Path to the file or folder to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteFile(remotePath: String): Result<Unit>
    
    /**
     * Create a new folder in remote storage
     * @param remotePath Path to the folder to create
     * @return Result containing NetworkDocument for created folder or error
     */
    suspend fun createFolder(remotePath: String): Result<NetworkDocument>
    
    /**
     * Rename a file or folder in remote storage
     * @param remotePath Current path of the file/folder
     * @param newName New name for the file/folder
     * @return Result indicating success or failure
     */
    suspend fun renameFile(remotePath: String, newName: String): Result<Unit>
    
    /**
     * Move a file or folder to a new location
     * @param sourcePath Current path of the file/folder
     * @param destinationPath New path for the file/folder
     * @return Result containing NetworkDocument for moved file/folder or error
     */
    suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument>
    
    /**
     * Copy a file or folder to a new location
     * @param sourcePath Path of the source file/folder
     * @param destinationPath Path for the destination file/folder
     * @return Result indicating success or failure
     */
    suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit>
    
    /**
     * Get metadata for a specific file or folder
     * @param remotePath Path to the file/folder
     * @return Result containing NetworkDocument or error
     */
    suspend fun getFileInfo(remotePath: String): Result<NetworkDocument>
    
    /**
     * Get active network operations
     * @return Flow emitting current operations
     */
    fun getActiveOperations(): Flow<List<NetworkOperation>>
    
    /**
     * Cancel an active operation
     * @param operationId ID of the operation to cancel
     * @return Result indicating success or failure
     */
    suspend fun cancelOperation(operationId: Long): Result<Unit>
    
    /**
     * Pause an active operation
     * @param operationId ID of the operation to pause
     * @return Result indicating success or failure
     */
    suspend fun pauseOperation(operationId: Long): Result<Unit>
    
    /**
     * Resume a paused operation
     * @param operationId ID of the operation to resume
     * @return Result indicating success or failure
     */
    suspend fun resumeOperation(operationId: Long): Result<Unit>
    
    /**
     * Get storage information
     * @return NetworkStorage with current storage info
     */
    suspend fun getStorageInfo(): NetworkStorage
    
    /**
     * Test the connection to the storage
     * @return Result indicating whether connection is successful
     */
    suspend fun testConnection(): Result<Boolean>
    
    /**
     * Get cached entries for offline access
     * @param path Optional path to filter by
     * @return Flow emitting cache entries
     */
    fun getCacheEntries(path: String? = null): Flow<List<CacheEntry>>
    
    /**
     * Add a file to the cache for offline access
     * @param remotePath Remote path of the file
     * @param priority Cache priority
     * @return Result indicating success or failure
     */
    suspend fun addToCache(remotePath: String, priority: Int = 100): Result<Unit>
    
    /**
     * Remove a file from the cache
     * @param remotePath Remote path of the file
     * @return Result indicating success or failure
     */
    suspend fun removeFromCache(remotePath: String): Result<Unit>
    
    /**
     * Clear the entire cache
     * @return Result indicating success or failure
     */
    suspend fun clearCache(): Result<Unit>
    
    /**
     * Get synchronization status for all files
     * @param path Optional path to filter by
     * @return Flow emitting sync status information
     */
    fun getSyncStatus(path: String? = null): Flow<Map<String, SyncStatus>>
    
    /**
     * Synchronize a specific file or folder
     * @param remotePath Path to synchronize
     * @param forceSync Whether to force synchronization even if up to date
     * @return Flow emitting synchronization progress
     */
    suspend fun syncFile(remotePath: String, forceSync: Boolean = false): Flow<NetworkOperation>
    
    /**
     * Synchronize all files in the storage
     * @param forceSync Whether to force synchronization even if up to date
     * @return Flow emitting synchronization operations
     */
    suspend fun syncAll(forceSync: Boolean = false): Flow<NetworkOperation>
    
    /**
     * Search for files and folders
     * @param query Search query
     * @param path Path to search within (optional)
     * @param includeContent Whether to search within file contents
     * @return Flow emitting search results
     */
    fun searchFiles(
        query: String,
        path: String? = null,
        includeContent: Boolean = false
    ): Flow<Result<List<NetworkDocument>>>
    
    /**
     * Get recent changes in the storage
     * @param since Get changes since this time
     * @param path Optional path to filter by
     * @return Flow emitting changed files
     */
    fun getRecentChanges(
        since: kotlinx.datetime.Instant,
        path: String? = null
    ): Flow<List<NetworkDocument>>
    
    /**
     * Get quota information for the storage
     * @return Result containing quota information or error
     */
    suspend fun getQuotaInfo(): Result<StorageQuota>
    
    /**
     * Check if a file or folder exists
     * @param remotePath Path to check
     * @return Result indicating existence
     */
    suspend fun exists(remotePath: String): Result<Boolean>
    
    /**
     * Get the parent folder of a given path
     * @param remotePath Path to get parent for
     * @return Parent path or null if already at root
     */
    fun getParentPath(remotePath: String): String?
    
    /**
     * Validate if a path is valid for this storage type
     * @param remotePath Path to validate
     * @return Result indicating validation
     */
    fun validatePath(remotePath: String): Result<Unit>
    
    /**
     * Get the root path for this storage
     */
    val rootPath: String
        get() = "/"
}

/**
 * Storage quota information
 */
data class StorageQuota(
    /**
     * Total storage quota in bytes
     */
    val totalSpace: Long,
    
    /**
     * Used space in bytes
     */
    val usedSpace: Long,
    
    /**
     * Available space in bytes
     */
    val availableSpace: Long,
    
    /**
     * Usage as a percentage (0.0 to 1.0)
     */
    val usagePercentage: Double,
    
    /**
     * Whether the storage is at capacity
     */
    val isFull: Boolean,
    
    /**
     * Whether the storage is running low on space (< 10% available)
     */
    val isLowOnSpace: Boolean,
    
    /**
     * Quota expiration time (null if no expiration)
     */
    val expiresAt: kotlinx.datetime.Instant? = null,
    
    /**
     * Additional quota metadata
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Formatted total space for display
     */
    val formattedTotalSpace: String
        get() = formatBytes(totalSpace)
    
    /**
     * Formatted used space for display
     */
    val formattedUsedSpace: String
        get() = formatBytes(usedSpace)
    
    /**
     * Formatted available space for display
     */
    val formattedAvailableSpace: String
        get() = formatBytes(availableSpace)
    
    /**
     * Usage as a percentage string
     */
    val usagePercentageString: String
        get() = "${(usagePercentage * 100).toInt()}%"
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
}