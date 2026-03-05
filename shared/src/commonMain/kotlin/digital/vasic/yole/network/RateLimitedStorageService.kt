/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Rate-Limited Network Storage Service Decorator
 * Wraps any NetworkStorageService to add semaphore-based
 * concurrency limiting for network operations
 *
 *########################################################*/
package digital.vasic.yole.network

import digital.vasic.yole.network.common.CacheEntry
import digital.vasic.yole.network.common.NetworkDocument
import digital.vasic.yole.network.common.NetworkOperation
import digital.vasic.yole.network.common.NetworkStorage
import digital.vasic.yole.network.common.StorageConfig
import digital.vasic.yole.network.common.SyncStatus
import digital.vasic.yole.util.RateLimiter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant

/**
 * Decorator that wraps a [NetworkStorageService] to limit concurrent network operations
 * using a [RateLimiter] (semaphore-based).
 *
 * This prevents overwhelming remote servers and local resources when many operations
 * are initiated simultaneously (e.g., bulk sync, multi-file download).
 *
 * @param delegate The underlying storage service to wrap
 * @param maxConcurrent Maximum number of concurrent operations (default: 4)
 */
class RateLimitedStorageService(
    private val delegate: NetworkStorageService,
    maxConcurrent: Int = 4
) : NetworkStorageService {

    private val rateLimiter = RateLimiter(maxConcurrent)

    override val config: StorageConfig
        get() = delegate.config

    override val isOnline: Boolean
        get() = delegate.isOnline

    override val rootPath: String
        get() = delegate.rootPath

    override suspend fun connect(): Result<Unit> =
        rateLimiter.execute { delegate.connect() }

    override suspend fun disconnect(): Result<Unit> =
        rateLimiter.execute { delegate.disconnect() }

    override suspend fun getStorageInfo(): NetworkStorage =
        rateLimiter.execute { delegate.getStorageInfo() }

    override suspend fun testConnection(): Result<Boolean> =
        rateLimiter.execute { delegate.testConnection() }

    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        val results = rateLimiter.execute { delegate.listFiles(path) }
        results.collect { emit(it) }
    }

    override suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation> =
        rateLimiter.execute { delegate.downloadFile(remotePath, localPath) }

    override suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation> =
        rateLimiter.execute { delegate.uploadFile(localPath, remotePath) }

    override suspend fun deleteFile(remotePath: String): Result<Unit> =
        rateLimiter.execute { delegate.deleteFile(remotePath) }

    override suspend fun createFolder(remotePath: String): Result<NetworkDocument> =
        rateLimiter.execute { delegate.createFolder(remotePath) }

    override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> =
        rateLimiter.execute { delegate.renameFile(remotePath, newName) }

    override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> =
        rateLimiter.execute { delegate.moveFile(sourcePath, destinationPath) }

    override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> =
        rateLimiter.execute { delegate.copyFile(sourcePath, destinationPath) }

    override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> =
        rateLimiter.execute { delegate.getFileInfo(remotePath) }

    override fun getActiveOperations(): Flow<List<NetworkOperation>> =
        delegate.getActiveOperations()

    override suspend fun cancelOperation(operationId: Long): Result<Unit> =
        delegate.cancelOperation(operationId)

    override suspend fun pauseOperation(operationId: Long): Result<Unit> =
        delegate.pauseOperation(operationId)

    override suspend fun resumeOperation(operationId: Long): Result<Unit> =
        delegate.resumeOperation(operationId)

    override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> =
        delegate.getCacheEntries(path)

    override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> =
        rateLimiter.execute { delegate.addToCache(remotePath, priority) }

    override suspend fun removeFromCache(remotePath: String): Result<Unit> =
        rateLimiter.execute { delegate.removeFromCache(remotePath) }

    override suspend fun clearCache(): Result<Unit> =
        rateLimiter.execute { delegate.clearCache() }

    override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> =
        delegate.getSyncStatus(path)

    override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> =
        rateLimiter.execute { delegate.syncFile(remotePath, forceSync) }

    override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> =
        rateLimiter.execute { delegate.syncAll(forceSync) }

    override fun searchFiles(
        query: String,
        path: String?,
        includeContent: Boolean
    ): Flow<Result<List<NetworkDocument>>> = flow {
        val results = rateLimiter.execute { delegate.searchFiles(query, path, includeContent) }
        results.collect { emit(it) }
    }

    override fun getRecentChanges(since: Instant, path: String?): Flow<List<NetworkDocument>> =
        delegate.getRecentChanges(since, path)

    override suspend fun getQuotaInfo(): Result<StorageQuota> =
        rateLimiter.execute { delegate.getQuotaInfo() }

    override suspend fun exists(remotePath: String): Result<Boolean> =
        rateLimiter.execute { delegate.exists(remotePath) }

    override fun getParentPath(remotePath: String): String? =
        delegate.getParentPath(remotePath)

    override fun validatePath(remotePath: String): Result<Unit> =
        delegate.validatePath(remotePath)
}
