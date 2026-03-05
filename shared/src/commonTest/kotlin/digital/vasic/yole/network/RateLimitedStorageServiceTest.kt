package digital.vasic.yole.network

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun testDocument(
    id: String = "test",
    name: String = "test",
    path: String = "/test",
    isFolder: Boolean = false
) = NetworkDocument(
    id = id,
    name = name,
    path = path,
    isFolder = isFolder,
    size = 100
)

/**
 * Tests for [RateLimitedStorageService] decorator.
 * Verifies semaphore-based concurrency limiting for network operations.
 */
class RateLimitedStorageServiceTest {

    /**
     * Minimal mock of NetworkStorageService that tracks concurrent operation count.
     */
    private class ConcurrencyTrackingService : NetworkStorageService {
        private val mutex = Mutex()
        var maxConcurrent = 0
            private set
        var currentConcurrent = 0
            private set
        var totalOperations = 0
            private set

        override val config: StorageConfig
            get() = StorageConfig.SmbConfig(
                name = "mock", host = "localhost", share = "test",
                domain = "", username = "", password = "",
                port = 445, useSsl = false, connectionTimeout = 5000
            )

        override val isOnline: Boolean get() = true
        override val rootPath: String get() = "/"

        private suspend fun <T> trackConcurrency(block: suspend () -> T): T {
            mutex.withLock {
                currentConcurrent++
                if (currentConcurrent > maxConcurrent) {
                    maxConcurrent = currentConcurrent
                }
                totalOperations++
            }
            try {
                delay(50) // Simulate network latency
                return block()
            } finally {
                mutex.withLock { currentConcurrent-- }
            }
        }

        override suspend fun connect(): Result<Unit> =
            trackConcurrency { Result.success(Unit) }

        override suspend fun disconnect(): Result<Unit> =
            trackConcurrency { Result.success(Unit) }

        override suspend fun getStorageInfo(): NetworkStorage =
            trackConcurrency {
                NetworkStorage(
                    id = "mock", name = "mock", type = StorageType.SMB,
                    location = "mock://", isOnline = true,
                    lastSync = Clock.System.now()
                )
            }

        override suspend fun testConnection(): Result<Boolean> =
            trackConcurrency { Result.success(true) }

        override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
            emit(trackConcurrency { Result.success(emptyList()) })
        }

        override suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation> =
            trackConcurrency { flowOf() }

        override suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation> =
            trackConcurrency { flowOf() }

        override suspend fun deleteFile(remotePath: String): Result<Unit> =
            trackConcurrency { Result.success(Unit) }

        override suspend fun createFolder(remotePath: String): Result<NetworkDocument> =
            trackConcurrency {
                Result.success(testDocument(id = "folder", name = remotePath, path = remotePath, isFolder = true))
            }

        override suspend fun renameFile(remotePath: String, newName: String): Result<Unit> =
            trackConcurrency { Result.success(Unit) }

        override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> =
            trackConcurrency {
                Result.success(testDocument(id = "moved", name = destinationPath, path = destinationPath))
            }

        override suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit> =
            trackConcurrency { Result.success(Unit) }

        override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> =
            trackConcurrency {
                Result.success(testDocument(id = "info", name = remotePath, path = remotePath))
            }

        override fun getActiveOperations(): Flow<List<NetworkOperation>> = flowOf(emptyList())

        override suspend fun cancelOperation(operationId: Long): Result<Unit> =
            Result.success(Unit)

        override suspend fun pauseOperation(operationId: Long): Result<Unit> =
            Result.success(Unit)

        override suspend fun resumeOperation(operationId: Long): Result<Unit> =
            Result.success(Unit)

        override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> =
            flowOf(emptyList())

        override suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> =
            trackConcurrency { Result.success(Unit) }

        override suspend fun removeFromCache(remotePath: String): Result<Unit> =
            trackConcurrency { Result.success(Unit) }

        override suspend fun clearCache(): Result<Unit> =
            trackConcurrency { Result.success(Unit) }

        override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> =
            flowOf(emptyMap())

        override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> =
            trackConcurrency { flowOf() }

        override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> =
            trackConcurrency { flowOf() }

        override fun searchFiles(
            query: String, path: String?, includeContent: Boolean
        ): Flow<Result<List<NetworkDocument>>> = flow {
            emit(trackConcurrency { Result.success(emptyList()) })
        }

        override fun getRecentChanges(since: Instant, path: String?): Flow<List<NetworkDocument>> =
            flowOf(emptyList())

        override suspend fun getQuotaInfo(): Result<StorageQuota> =
            trackConcurrency {
                Result.success(
                    StorageQuota(
                        totalSpace = 1_000_000, usedSpace = 500_000,
                        availableSpace = 500_000, usagePercentage = 0.5,
                        isFull = false, isLowOnSpace = false
                    )
                )
            }

        override suspend fun exists(remotePath: String): Result<Boolean> =
            trackConcurrency { Result.success(true) }

        override fun getParentPath(remotePath: String): String? =
            remotePath.substringBeforeLast("/", "").ifEmpty { null }

        override fun validatePath(remotePath: String): Result<Unit> =
            Result.success(Unit)
    }

    @Test
    fun testConcurrencyIsLimited() = runTest {
        val trackingService = ConcurrencyTrackingService()
        val rateLimited = RateLimitedStorageService(trackingService, maxConcurrent = 2)

        // Launch 10 concurrent operations
        val jobs = (1..10).map {
            async { rateLimited.deleteFile("/file$it.txt") }
        }
        jobs.awaitAll()

        // Max concurrent should never exceed the limit of 2
        assertTrue(
            trackingService.maxConcurrent <= 2,
            "Max concurrent was ${trackingService.maxConcurrent}, expected <= 2"
        )
        assertEquals(10, trackingService.totalOperations)
    }

    @Test
    fun testAllOperationsComplete() = runTest {
        val trackingService = ConcurrencyTrackingService()
        val rateLimited = RateLimitedStorageService(trackingService, maxConcurrent = 3)

        val results = (1..20).map {
            async { rateLimited.exists("/path$it") }
        }.awaitAll()

        assertEquals(20, results.size)
        results.forEach { assertTrue(it.isSuccess) }
        assertEquals(20, trackingService.totalOperations)
    }

    @Test
    fun testDelegatesCorrectly() = runTest {
        val trackingService = ConcurrencyTrackingService()
        val rateLimited = RateLimitedStorageService(trackingService, maxConcurrent = 4)

        assertEquals(trackingService.isOnline, rateLimited.isOnline)
        assertEquals(trackingService.rootPath, rateLimited.rootPath)

        val connectResult = rateLimited.connect()
        assertTrue(connectResult.isSuccess)

        val storageInfo = rateLimited.getStorageInfo()
        assertEquals("mock", storageInfo.id)

        val testResult = rateLimited.testConnection()
        assertTrue(testResult.getOrNull() == true)
    }

    @Test
    fun testNonRateLimitedOperationsPassThrough() = runTest {
        val trackingService = ConcurrencyTrackingService()
        val rateLimited = RateLimitedStorageService(trackingService, maxConcurrent = 1)

        // These should pass through without rate limiting
        val parentPath = rateLimited.getParentPath("/a/b/c")
        assertEquals("/a/b", parentPath)

        val validateResult = rateLimited.validatePath("/valid/path")
        assertTrue(validateResult.isSuccess)
    }
}
