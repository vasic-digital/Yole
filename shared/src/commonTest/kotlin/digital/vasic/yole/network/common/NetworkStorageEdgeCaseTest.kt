/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Edge case tests for NetworkStorage, CacheEntry computed
 * properties, and NetworkOperation pause/duration behavior
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class NetworkStorageEdgeCaseTest {

    // ==================== NetworkStorage edge cases ====================

    @Test
    fun testAvailableSpaceWhenTotalNull() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com", totalSpace = null, usedSpace = 100L
        )
        assertNull(storage.availableSpace)
    }

    @Test
    fun testAvailableSpaceWhenUsedNull() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com", totalSpace = 1000L, usedSpace = null
        )
        assertNull(storage.availableSpace)
    }

    @Test
    fun testAvailableSpaceBothNull() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com"
        )
        assertNull(storage.availableSpace)
    }

    @Test
    fun testUsagePercentageWhenTotalZero() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com", totalSpace = 0L, usedSpace = 0L
        )
        assertNull(storage.usagePercentage)
    }

    @Test
    fun testUsagePercentageWhenNull() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com"
        )
        assertNull(storage.usagePercentage)
    }

    @Test
    fun testIsFullWhenExactlyFull() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com", totalSpace = 1000L, usedSpace = 1000L
        )
        assertTrue(storage.isFull)
        assertEquals(0L, storage.availableSpace)
    }

    @Test
    fun testIsFullWhenNotFull() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com", totalSpace = 1000L, usedSpace = 999L
        )
        assertFalse(storage.isFull)
    }

    @Test
    fun testIsFullWhenNoSpaceInfo() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com"
        )
        assertFalse(storage.isFull)
    }

    @Test
    fun testIsLowOnSpaceAt91Percent() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com", totalSpace = 1000L, usedSpace = 910L
        )
        assertTrue(storage.isLowOnSpace)
    }

    @Test
    fun testIsLowOnSpaceAt89Percent() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com", totalSpace = 1000L, usedSpace = 890L
        )
        assertFalse(storage.isLowOnSpace)
    }

    @Test
    fun testIsLowOnSpaceWhenNoInfo() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com"
        )
        assertFalse(storage.isLowOnSpace)
    }

    @Test
    fun testSupportsFileNoExtension() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com",
            supportedExtensions = listOf("txt", "md")
        )
        assertTrue(storage.supportsFile("README"), "File without extension should be supported")
    }

    @Test
    fun testSupportsExtensionEmptyList() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV,
            location = "https://x.com",
            supportedExtensions = emptyList()
        )
        assertTrue(storage.supportsExtension("anything"), "Empty list means all supported")
    }

    @Test
    fun testMockWithCustomParams() {
        val storage = NetworkStorage.mock(
            id = "custom-id",
            name = "Custom",
            type = StorageType.GIT,
            location = "git://repo",
            isOnline = false
        )
        assertEquals("custom-id", storage.id)
        assertEquals("Custom", storage.name)
        assertEquals(StorageType.GIT, storage.type)
        assertEquals("git://repo", storage.location)
        assertFalse(storage.isOnline)
    }

    @Test
    fun testWithOnlineStatusPreservesOther() {
        val storage = NetworkStorage.mock()
        val offline = storage.withOnlineStatus(false)
        assertFalse(offline.isOnline)
        assertEquals(storage.id, offline.id)
        assertEquals(storage.name, offline.name)
        assertEquals(storage.totalSpace, offline.totalSpace)
    }

    @Test
    fun testWithSpaceInfoUpdatesCorrectly() {
        val storage = NetworkStorage.mock()
        val updated = storage.withSpaceInfo(5000L, 2500L)
        assertEquals(5000L, updated.totalSpace)
        assertEquals(2500L, updated.usedSpace)
        assertEquals(2500L, updated.availableSpace)
        assertEquals(0.5, updated.usagePercentage)
    }

    @Test
    fun testIsEnabledDefault() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV, location = "https://x.com"
        )
        assertTrue(storage.isEnabled)
    }

    @Test
    fun testMetadataDefault() {
        val storage = NetworkStorage(
            id = "s1", name = "S1", type = StorageType.WEBDAV, location = "https://x.com"
        )
        assertTrue(storage.metadata.isEmpty())
    }

    // ==================== CacheEntry edge cases ====================

    @Test
    fun testCompressionRatioWhenCompressed() {
        val now = Clock.System.now()
        val entry = CacheEntry(
            id = "c1", remoteDocumentId = "d1", localPath = "/local",
            remotePath = "/remote", size = 500L,
            createdAt = now, lastAccessed = now, lastModified = now,
            compression = "gzip", originalSize = 1000L
        )
        assertNotNull(entry.compressionRatio)
        assertEquals(0.5, entry.compressionRatio)
    }

    @Test
    fun testCompressionRatioWhenUncompressed() {
        val now = Clock.System.now()
        val entry = CacheEntry(
            id = "c1", remoteDocumentId = "d1", localPath = "/local",
            remotePath = "/remote", size = 500L,
            createdAt = now, lastAccessed = now, lastModified = now
        )
        assertNull(entry.compressionRatio)
    }

    @Test
    fun testCompressionRatioWhenNoOriginalSize() {
        val now = Clock.System.now()
        val entry = CacheEntry(
            id = "c1", remoteDocumentId = "d1", localPath = "/local",
            remotePath = "/remote", size = 500L,
            createdAt = now, lastAccessed = now, lastModified = now,
            compression = "gzip", originalSize = null
        )
        assertNull(entry.compressionRatio)
    }

    @Test
    fun testCacheEntryCannotBeEvictedWhenPinned() {
        val now = Clock.System.now()
        val entry = CacheEntry(
            id = "c1", remoteDocumentId = "d1", localPath = "/local",
            remotePath = "/remote", size = 500L,
            createdAt = now, lastAccessed = now, lastModified = now,
            isPinned = true
        )
        assertFalse(entry.canBeEvicted)
    }

    @Test
    fun testCacheEntryCannotBeEvictedWhenInUse() {
        val now = Clock.System.now()
        val entry = CacheEntry(
            id = "c1", remoteDocumentId = "d1", localPath = "/local",
            remotePath = "/remote", size = 500L,
            createdAt = now, lastAccessed = now, lastModified = now,
            isInUse = true
        )
        assertFalse(entry.canBeEvicted)
    }

    @Test
    fun testCacheEntryCanBeEvictedWhenFree() {
        val now = Clock.System.now()
        val entry = CacheEntry(
            id = "c1", remoteDocumentId = "d1", localPath = "/local",
            remotePath = "/remote", size = 500L,
            createdAt = now, lastAccessed = now, lastModified = now,
            isPinned = false, isInUse = false
        )
        assertTrue(entry.canBeEvicted)
    }

    @Test
    fun testCacheEntryIsExpiredInverse() {
        val now = Clock.System.now()
        val validEntry = CacheEntry(
            id = "c1", remoteDocumentId = "d1", localPath = "/local",
            remotePath = "/remote", size = 500L,
            createdAt = now, lastAccessed = now, lastModified = now,
            isValid = true
        )
        assertFalse(validEntry.isExpired)

        val invalidEntry = validEntry.copy(isValid = false)
        assertTrue(invalidEntry.isExpired)
    }

    @Test
    fun testCacheEntryCreateWithContentType() {
        val entry = CacheEntry.create(
            remoteDocumentId = "d1",
            localPath = "/local/file.txt",
            remotePath = "/file.txt",
            size = 2048L,
            contentType = "text/plain"
        )
        assertEquals("text/plain", entry.contentType)
        assertNull(entry.expiresAt)
    }

    @Test
    fun testCacheEntryAgeAndTimeSinceAccess() {
        val now = Clock.System.now()
        val entry = CacheEntry(
            id = "c1", remoteDocumentId = "d1", localPath = "/local",
            remotePath = "/remote", size = 500L,
            createdAt = now.minus(1.hours),
            lastAccessed = now.minus(1.hours),
            lastModified = now
        )
        assertTrue(entry.age >= 3_600_000L - 1000, "Age should be about 1 hour")
        assertTrue(entry.timeSinceLastAccess >= 3_600_000L - 1000, "Time since access should be about 1 hour")
    }

    @Test
    fun testCacheEntryAccessFrequency() {
        val now = Clock.System.now()
        val entry = CacheEntry(
            id = "c1", remoteDocumentId = "d1", localPath = "/local",
            remotePath = "/remote", size = 500L,
            createdAt = now.minus(1.hours),
            lastAccessed = now,
            lastModified = now,
            accessCount = 24
        )
        // 24 accesses over ~1 hour => ~576 accesses per day
        assertTrue(entry.accessFrequency > 500.0)
    }

    @Test
    fun testCacheEntryWithContentUpdates() {
        val now = Clock.System.now()
        val entry = CacheEntry.mock()
        val updated = entry.withContent(4096L, "sha256:abc", now)
        assertEquals(4096L, updated.size)
        assertEquals("sha256:abc", updated.checksum)
        assertEquals(now, updated.lastModified)
    }

    @Test
    fun testCacheEntryWithExpirationNull() {
        val entry = CacheEntry.mock()
        val noExpiry = entry.withExpiration(null)
        assertNull(noExpiry.expiresAt)
        assertTrue(noExpiry.isValid)
    }

    @Test
    fun testCacheEntryWithExpirationFuture() {
        val entry = CacheEntry.mock()
        val future = Clock.System.now().plus(1.hours)
        val withExpiry = entry.withExpiration(future)
        assertEquals(future, withExpiry.expiresAt)
        assertTrue(withExpiry.isValid)
    }

    // ==================== NetworkOperation pause/duration edge cases ====================

    @Test
    fun testIsRunningWhenPaused() {
        val now = Clock.System.now()
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test", status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.5, createdAt = now, startedAt = now, isPaused = true
        )
        assertFalse(op.isRunning, "IN_PROGRESS but paused should NOT be running")
    }

    @Test
    fun testDurationForInProgressOperation() {
        val now = Clock.System.now()
        val startedAt = now.minus(5.seconds)
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test", status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.5, createdAt = now, startedAt = startedAt
        )
        val duration = op.duration
        assertNotNull(duration)
        assertTrue(duration >= 5000L, "Duration should be at least 5000ms")
    }

    @Test
    fun testDurationNullWhenNoStartedAt() {
        val now = Clock.System.now()
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test", status = NetworkOperation.Status.PENDING,
            progress = 0.0, createdAt = now
        )
        assertNull(op.duration)
    }

    @Test
    fun testWithStatusCompletedSetsCompletedAt() {
        val now = Clock.System.now()
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test", status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.9, createdAt = now, startedAt = now
        )
        val completed = op.withStatus(NetworkOperation.Status.COMPLETED)
        assertNotNull(completed.completedAt)
        assertEquals(NetworkOperation.Status.COMPLETED, completed.status)
    }

    @Test
    fun testWithStatusInProgressSetsStartedAt() {
        val now = Clock.System.now()
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test", status = NetworkOperation.Status.PENDING,
            progress = 0.0, createdAt = now
        )
        assertNull(op.startedAt)
        val started = op.withStatus(NetworkOperation.Status.IN_PROGRESS)
        assertNotNull(started.startedAt)
    }

    @Test
    fun testWithStatusInProgressDoesNotOverrideStartedAt() {
        val now = Clock.System.now()
        val originalStart = now.minus(10.seconds)
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test", status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.5, createdAt = now, startedAt = originalStart
        )
        val resumed = op.withStatus(NetworkOperation.Status.IN_PROGRESS)
        assertEquals(originalStart, resumed.startedAt)
    }

    @Test
    fun testWithProgressExplicitBytesTransferred() {
        val now = Clock.System.now()
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.DOWNLOAD,
            remotePath = "/test", status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.0, totalSize = 2000L, createdAt = now, startedAt = now
        )
        val updated = op.withProgress(0.5, bytesTransferred = 999L)
        assertEquals(999L, updated.bytesTransferred)
    }

    @Test
    fun testWithProgressTransferSpeedAndEstimate() {
        val now = Clock.System.now()
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.DOWNLOAD,
            remotePath = "/test", status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.0, totalSize = 2000L, createdAt = now, startedAt = now
        )
        val updated = op.withProgress(0.5, transferSpeed = 1024L, estimatedTimeRemaining = 5000L)
        assertEquals(1024L, updated.transferSpeed)
        assertEquals(5000L, updated.estimatedTimeRemaining)
    }

    @Test
    fun testCanRetryFalseWhenNotFailed() {
        val now = Clock.System.now()
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test", status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.5, createdAt = now, retryCount = 0, maxRetries = 3
        )
        assertFalse(op.canRetry, "Non-failed op should not be retryable")
    }

    @Test
    fun testWithErrorIncreasesRetryCount() {
        val now = Clock.System.now()
        val op = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test", status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.5, createdAt = now, retryCount = 2
        )
        val failed = op.withError("timeout")
        assertEquals(3, failed.retryCount)
        assertEquals("timeout", failed.error)
    }

    @Test
    fun testMockWithCustomParameters() {
        val mock = NetworkOperation.mock(
            type = NetworkOperation.Type.DOWNLOAD,
            status = NetworkOperation.Status.COMPLETED,
            progress = 1.0
        )
        assertEquals(NetworkOperation.Type.DOWNLOAD, mock.type)
        assertEquals(NetworkOperation.Status.COMPLETED, mock.status)
        assertEquals(1.0, mock.progress)
    }

    @Test
    fun testProgressPercentageBoundary() {
        val now = Clock.System.now()
        val zero = NetworkOperation(
            id = 1L, type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test", progress = 0.0, createdAt = now
        )
        assertEquals(0, zero.progressPercentage)

        val full = zero.copy(progress = 1.0)
        assertEquals(100, full.progressPercentage)

        val mid = zero.copy(progress = 0.333)
        assertEquals(33, mid.progressPercentage)
    }

    @Test
    fun testAllOperationTypesCount() {
        assertEquals(8, NetworkOperation.Type.entries.size)
    }

    @Test
    fun testAllOperationStatusesCount() {
        assertEquals(6, NetworkOperation.Status.entries.size)
    }
}
