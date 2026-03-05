/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Edge case tests for DocumentSyncStatus, SyncStatus,
 * ConflictResolution, and NetworkDocument factory methods
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

class DocumentSyncStatusEdgeCaseTest {

    // ==================== DocumentSyncStatus computed properties ====================

    @Test
    fun testIsSyncingForUploadingStatus() {
        val status = DocumentSyncStatus(status = SyncStatus.UPLOADING)
        assertTrue(status.isSyncing)
    }

    @Test
    fun testIsSyncingForDownloadingStatus() {
        val status = DocumentSyncStatus(status = SyncStatus.DOWNLOADING)
        assertTrue(status.isSyncing)
    }

    @Test
    fun testIsSyncingForSyncingStatus() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING)
        assertTrue(status.isSyncing)
    }

    @Test
    fun testIsSyncingFalseForSynced() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCED)
        assertFalse(status.isSyncing)
    }

    @Test
    fun testHasFailedForSyncError() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNC_ERROR)
        assertTrue(status.hasFailed)
    }

    @Test
    fun testHasFailedFalseForOthers() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCED)
        assertFalse(status.hasFailed)
    }

    @Test
    fun testCanRetryWhenFailedAndRetriesLeft() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNC_ERROR,
            retryCount = 1, maxRetries = 3
        )
        assertTrue(status.canRetry)
    }

    @Test
    fun testCanRetryFalseWhenMaxRetries() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNC_ERROR,
            retryCount = 3, maxRetries = 3
        )
        assertFalse(status.canRetry)
    }

    @Test
    fun testCanRetryFalseWhenNotFailed() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNCED,
            retryCount = 0, maxRetries = 3
        )
        assertFalse(status.canRetry)
    }

    @Test
    fun testIsPendingForPendingUpload() {
        val status = DocumentSyncStatus(status = SyncStatus.PENDING_UPLOAD)
        assertTrue(status.isPending)
    }

    @Test
    fun testIsPendingForPendingDownload() {
        val status = DocumentSyncStatus(status = SyncStatus.PENDING_DOWNLOAD)
        assertTrue(status.isPending)
    }

    @Test
    fun testIsPendingForQueued() {
        val status = DocumentSyncStatus(status = SyncStatus.QUEUED)
        assertTrue(status.isPending)
    }

    @Test
    fun testIsPendingFalseForOthers() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCED)
        assertFalse(status.isPending)
    }

    @Test
    fun testProgressPercentage() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING, progress = 0.75)
        assertEquals(75, status.progressPercentage)
    }

    @Test
    fun testProgressPercentageZero() {
        val status = DocumentSyncStatus(status = SyncStatus.PENDING_UPLOAD, progress = 0.0)
        assertEquals(0, status.progressPercentage)
    }

    @Test
    fun testProgressPercentageFull() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCED, progress = 1.0)
        assertEquals(100, status.progressPercentage)
    }

    @Test
    fun testTransferSpeedWhenSyncing() {
        val status = DocumentSyncStatus(
            status = SyncStatus.UPLOADING,
            bytesTransferred = 10000L,
            estimatedTimeRemaining = 10L
        )
        val speed = status.transferSpeed
        assertNotNull(speed)
        assertEquals(1000L, speed)
    }

    @Test
    fun testTransferSpeedNullWhenNotSyncing() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNCED,
            bytesTransferred = 10000L,
            estimatedTimeRemaining = 10L
        )
        assertNull(status.transferSpeed)
    }

    @Test
    fun testTransferSpeedNullWhenNoEstimate() {
        val status = DocumentSyncStatus(
            status = SyncStatus.UPLOADING,
            bytesTransferred = 10000L,
            estimatedTimeRemaining = null
        )
        assertNull(status.transferSpeed)
    }

    @Test
    fun testTransferSpeedNullWhenZeroBytes() {
        val status = DocumentSyncStatus(
            status = SyncStatus.UPLOADING,
            bytesTransferred = 0L,
            estimatedTimeRemaining = 10L
        )
        assertNull(status.transferSpeed)
    }

    // ==================== Mutation methods ====================

    @Test
    fun testWithStatusChanges() {
        val status = DocumentSyncStatus(status = SyncStatus.PENDING_UPLOAD)
        val updated = status.withStatus(SyncStatus.SYNCING)
        assertEquals(SyncStatus.SYNCING, updated.status)
    }

    @Test
    fun testWithProgressClamping() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING, dataSize = 1000L)
        val clamped = status.withProgress(1.5)
        assertEquals(1.0, clamped.progress)
        val clampedLow = status.withProgress(-0.5)
        assertEquals(0.0, clampedLow.progress)
    }

    @Test
    fun testWithProgressCalculatesBytesTransferred() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING, dataSize = 2000L)
        val updated = status.withProgress(0.5)
        assertEquals(1000L, updated.bytesTransferred)
    }

    @Test
    fun testWithProgressExplicitBytes() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING, dataSize = 2000L)
        val updated = status.withProgress(0.5, bytesTransferred = 999L)
        assertEquals(999L, updated.bytesTransferred)
    }

    @Test
    fun testWithErrorSetsStatusAndMessage() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING)
        val failed = status.withError("Connection lost")
        assertEquals(SyncStatus.SYNC_ERROR, failed.status)
        assertEquals("Connection lost", failed.errorMessage)
        assertEquals(1, failed.retryCount)
    }

    @Test
    fun testWithErrorNoRetryIncrement() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING, retryCount = 2)
        val failed = status.withError("Error", incrementRetry = false)
        assertEquals(2, failed.retryCount)
    }

    @Test
    fun testWithSuccessResetsState() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNCING,
            errorMessage = "old error",
            retryCount = 2,
            progress = 0.5,
            dataSize = 1000L,
            bytesTransferred = 500L
        )
        val success = status.withSuccess()
        assertEquals(SyncStatus.SYNCED, success.status)
        assertNull(success.errorMessage)
        assertEquals(0, success.retryCount)
        assertEquals(1.0, success.progress)
        assertEquals(1000L, success.bytesTransferred)
        assertTrue(success.isAvailableOffline)
        assertNotNull(success.lastSyncTime)
    }

    // ==================== Companion factory methods ====================

    @Test
    fun testForNewDocument() {
        val status = DocumentSyncStatus.forNewDocument()
        assertEquals(SyncStatus.PENDING_UPLOAD, status.status)
        assertTrue(status.isAutomatic)
        assertFalse(status.isAvailableOffline)
    }

    @Test
    fun testForNewDocumentManual() {
        val status = DocumentSyncStatus.forNewDocument(isAutomatic = false)
        assertEquals(SyncStatus.PENDING_UPLOAD, status.status)
        assertFalse(status.isAutomatic)
    }

    @Test
    fun testForExistingDocument() {
        val now = Clock.System.now()
        val status = DocumentSyncStatus.forExistingDocument(lastSyncTime = now)
        assertEquals(SyncStatus.SYNCED, status.status)
        assertEquals(now, status.lastSyncTime)
        assertTrue(status.isAvailableOffline)
    }

    @Test
    fun testForExistingDocumentNoSync() {
        val status = DocumentSyncStatus.forExistingDocument()
        assertEquals(SyncStatus.SYNCED, status.status)
        assertNull(status.lastSyncTime)
    }

    // ==================== SyncStatus enum completeness ====================

    @Test
    fun testSyncStatusCount() {
        assertEquals(11, SyncStatus.entries.size)
    }

    @Test
    fun testConflictResolutionCount() {
        assertEquals(5, ConflictResolution.entries.size)
    }

    @Test
    fun testDocumentSyncStatusDefaults() {
        val status = DocumentSyncStatus(status = SyncStatus.UNKNOWN)
        assertNull(status.lastSyncTime)
        assertNull(status.nextSyncTime)
        assertNull(status.errorMessage)
        assertEquals(0, status.retryCount)
        assertEquals(3, status.maxRetries)
        assertEquals(0.0, status.progress)
        assertNull(status.estimatedTimeRemaining)
        assertEquals(0L, status.dataSize)
        assertEquals(0L, status.bytesTransferred)
        assertTrue(status.isAutomatic)
        assertFalse(status.isAvailableOffline)
        assertFalse(status.hasConflicts)
        assertNull(status.conflictResolution)
        assertTrue(status.metadata.isEmpty())
    }

    @Test
    fun testDocumentSyncStatusWithConflict() {
        val status = DocumentSyncStatus(
            status = SyncStatus.CONFLICT,
            hasConflicts = true,
            conflictResolution = ConflictResolution.MANUAL
        )
        assertTrue(status.hasConflicts)
        assertEquals(ConflictResolution.MANUAL, status.conflictResolution)
    }

    // ==================== NetworkDocument factory methods ====================

    @Test
    fun testNetworkDocumentMock() {
        val doc = NetworkDocument.mock()
        assertNotNull(doc.id)
        assertEquals("test.txt", doc.name)
        assertEquals("/test.txt", doc.path)
        assertFalse(doc.isFolder)
        assertEquals(1024L, doc.size)
        assertEquals(SyncStatus.SYNCED, doc.syncStatus)
        assertEquals("test-storage", doc.storageId)
        assertNotNull(doc.lastModified)
    }

    @Test
    fun testNetworkDocumentMockCustom() {
        val doc = NetworkDocument.mock(
            name = "report.md",
            path = "/docs/report.md",
            size = 4096L,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            storageId = "my-storage"
        )
        assertEquals("report.md", doc.name)
        assertEquals("/docs/report.md", doc.path)
        assertEquals(4096L, doc.size)
        assertEquals(SyncStatus.PENDING_UPLOAD, doc.syncStatus)
        assertEquals("my-storage", doc.storageId)
    }

    @Test
    fun testNetworkDocumentMockFolder() {
        val folder = NetworkDocument.mockFolder()
        assertNotNull(folder.id)
        assertEquals("folder", folder.name)
        assertEquals("/folder", folder.path)
        assertTrue(folder.isFolder)
        assertEquals(SyncStatus.SYNCED, folder.syncStatus)
        assertEquals("test-storage", folder.storageId)
    }

    @Test
    fun testNetworkDocumentMockFolderCustom() {
        val folder = NetworkDocument.mockFolder(
            name = "projects",
            path = "/docs/projects",
            storageId = "custom-storage"
        )
        assertEquals("projects", folder.name)
        assertEquals("/docs/projects", folder.path)
        assertEquals("custom-storage", folder.storageId)
    }

    @Test
    fun testNetworkDocumentIsTextFileForCommonExtensions() {
        val extensions = listOf("txt", "md", "json", "xml", "yaml", "csv", "py", "kt", "java", "go", "rs")
        extensions.forEach { ext ->
            val doc = NetworkDocument(
                id = "test", name = "file.$ext", path = "/file.$ext",
                storageId = "s1"
            )
            assertTrue(doc.isTextFile, ".$ext should be detected as text file")
        }
    }

    @Test
    fun testNetworkDocumentIsImageFileForCommonExtensions() {
        val extensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
        extensions.forEach { ext ->
            val doc = NetworkDocument(
                id = "test", name = "photo.$ext", path = "/photo.$ext",
                storageId = "s1"
            )
            assertTrue(doc.isImageFile, ".$ext should be detected as image file")
        }
    }

    @Test
    fun testNetworkDocumentFolderIsNotTextOrImage() {
        val folder = NetworkDocument(
            id = "test", name = "folder.txt", path = "/folder.txt",
            isFolder = true, storageId = "s1"
        )
        assertFalse(folder.isTextFile, "Folder should not be text file even with .txt extension")
        assertFalse(folder.isImageFile)
        assertFalse(folder.isPdfFile)
    }

    @Test
    fun testNetworkDocumentReadOnlyNotEditable() {
        val doc = NetworkDocument(
            id = "test", name = "file.txt", path = "/file.txt",
            isReadOnly = true, storageId = "s1"
        )
        assertTrue(doc.isTextFile)
        assertFalse(doc.isEditable, "Read-only text file should not be editable")
    }

    @Test
    fun testNetworkDocumentIsPreviewable() {
        val textDoc = NetworkDocument(id = "t1", name = "f.md", path = "/f.md", storageId = "s1")
        assertTrue(textDoc.isPreviewable)

        val imgDoc = NetworkDocument(id = "t2", name = "f.png", path = "/f.png", storageId = "s1")
        assertTrue(imgDoc.isPreviewable)

        val pdfDoc = NetworkDocument(id = "t3", name = "f.pdf", path = "/f.pdf", storageId = "s1")
        assertTrue(pdfDoc.isPreviewable)

        val binDoc = NetworkDocument(id = "t4", name = "f.exe", path = "/f.exe", storageId = "s1")
        assertFalse(binDoc.isPreviewable)
    }

    @Test
    fun testNetworkDocumentHiddenProperty() {
        val doc = NetworkDocument(
            id = "test", name = ".hidden", path = "/.hidden",
            isHidden = true, storageId = "s1"
        )
        assertTrue(doc.isHidden)
    }

    @Test
    fun testNetworkDocumentThumbnailsAndTags() {
        val doc = NetworkDocument(
            id = "test", name = "file.txt", path = "/file.txt",
            thumbnails = listOf("/thumb/small.jpg", "/thumb/large.jpg"),
            tags = listOf("important", "archived"),
            storageId = "s1"
        )
        assertEquals(2, doc.thumbnails.size)
        assertEquals(2, doc.tags.size)
        assertEquals("important", doc.tags[0])
    }

    @Test
    fun testNetworkDocumentOwnerAndAuthor() {
        val doc = NetworkDocument(
            id = "test", name = "file.txt", path = "/file.txt",
            owner = "alice@example.com",
            author = "Alice Smith",
            storageId = "s1"
        )
        assertEquals("alice@example.com", doc.owner)
        assertEquals("Alice Smith", doc.author)
    }
}
