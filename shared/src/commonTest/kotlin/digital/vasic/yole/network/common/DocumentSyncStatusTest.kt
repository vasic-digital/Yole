/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for DocumentSyncStatus and related enums
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

class DocumentSyncStatusTest {

    @Test
    fun testIsSyncingWhenSyncing() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING)
        assertTrue(status.isSyncing)
    }

    @Test
    fun testIsSyncingWhenUploading() {
        val status = DocumentSyncStatus(status = SyncStatus.UPLOADING)
        assertTrue(status.isSyncing)
    }

    @Test
    fun testIsSyncingWhenDownloading() {
        val status = DocumentSyncStatus(status = SyncStatus.DOWNLOADING)
        assertTrue(status.isSyncing)
    }

    @Test
    fun testIsNotSyncingWhenSynced() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCED)
        assertFalse(status.isSyncing)
    }

    @Test
    fun testHasFailed() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNC_ERROR)
        assertTrue(status.hasFailed)
    }

    @Test
    fun testHasNotFailed() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCED)
        assertFalse(status.hasFailed)
    }

    @Test
    fun testCanRetryWhenFailedAndRetriesLeft() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNC_ERROR,
            retryCount = 1,
            maxRetries = 3
        )
        assertTrue(status.canRetry)
    }

    @Test
    fun testCannotRetryWhenMaxRetriesReached() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNC_ERROR,
            retryCount = 3,
            maxRetries = 3
        )
        assertFalse(status.canRetry)
    }

    @Test
    fun testCannotRetryWhenNotFailed() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNCED,
            retryCount = 0,
            maxRetries = 3
        )
        assertFalse(status.canRetry)
    }

    @Test
    fun testIsPending() {
        assertTrue(DocumentSyncStatus(status = SyncStatus.PENDING_UPLOAD).isPending)
        assertTrue(DocumentSyncStatus(status = SyncStatus.PENDING_DOWNLOAD).isPending)
        assertTrue(DocumentSyncStatus(status = SyncStatus.QUEUED).isPending)
        assertFalse(DocumentSyncStatus(status = SyncStatus.SYNCED).isPending)
    }

    @Test
    fun testProgressPercentage() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING, progress = 0.75)
        assertEquals(75, status.progressPercentage)
    }

    @Test
    fun testProgressPercentageZero() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING, progress = 0.0)
        assertEquals(0, status.progressPercentage)
    }

    @Test
    fun testWithStatus() {
        val original = DocumentSyncStatus(status = SyncStatus.PENDING_UPLOAD)
        val updated = original.withStatus(SyncStatus.SYNCING)
        assertEquals(SyncStatus.SYNCING, updated.status)
    }

    @Test
    fun testWithProgress() {
        val original = DocumentSyncStatus(
            status = SyncStatus.SYNCING,
            dataSize = 1000L
        )
        val updated = original.withProgress(0.5)
        assertEquals(0.5, updated.progress, 0.001)
        assertEquals(500L, updated.bytesTransferred)
    }

    @Test
    fun testWithProgressClamped() {
        val original = DocumentSyncStatus(status = SyncStatus.SYNCING)
        val updated = original.withProgress(1.5)
        assertEquals(1.0, updated.progress, 0.001)
    }

    @Test
    fun testWithProgressExplicitBytes() {
        val original = DocumentSyncStatus(status = SyncStatus.SYNCING, dataSize = 1000L)
        val updated = original.withProgress(0.5, bytesTransferred = 600L)
        assertEquals(600L, updated.bytesTransferred)
    }

    @Test
    fun testWithError() {
        val original = DocumentSyncStatus(status = SyncStatus.SYNCING, retryCount = 0)
        val errored = original.withError("Network timeout")
        assertEquals(SyncStatus.SYNC_ERROR, errored.status)
        assertEquals("Network timeout", errored.errorMessage)
        assertEquals(1, errored.retryCount)
    }

    @Test
    fun testWithErrorNoIncrement() {
        val original = DocumentSyncStatus(status = SyncStatus.SYNCING, retryCount = 2)
        val errored = original.withError("Error", incrementRetry = false)
        assertEquals(2, errored.retryCount)
    }

    @Test
    fun testWithSuccess() {
        val original = DocumentSyncStatus(
            status = SyncStatus.SYNCING,
            dataSize = 1000L,
            retryCount = 2,
            errorMessage = "previous error"
        )
        val success = original.withSuccess()
        assertEquals(SyncStatus.SYNCED, success.status)
        assertNotNull(success.lastSyncTime)
        assertNull(success.errorMessage)
        assertEquals(0, success.retryCount)
        assertEquals(1.0, success.progress, 0.001)
        assertEquals(1000L, success.bytesTransferred)
        assertTrue(success.isAvailableOffline)
    }

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
    fun testForExistingDocumentNoSyncTime() {
        val status = DocumentSyncStatus.forExistingDocument()
        assertEquals(SyncStatus.SYNCED, status.status)
        assertNull(status.lastSyncTime)
    }

    @Test
    fun testDefaultValues() {
        val status = DocumentSyncStatus(status = SyncStatus.UNKNOWN)
        assertNull(status.lastSyncTime)
        assertNull(status.nextSyncTime)
        assertNull(status.errorMessage)
        assertEquals(0, status.retryCount)
        assertEquals(3, status.maxRetries)
        assertEquals(0.0, status.progress, 0.001)
        assertNull(status.estimatedTimeRemaining)
        assertEquals(0L, status.dataSize)
        assertEquals(0L, status.bytesTransferred)
        assertTrue(status.isAutomatic)
        assertFalse(status.isAvailableOffline)
        assertFalse(status.hasConflicts)
        assertNull(status.conflictResolution)
    }
}

class SyncStatusEnumTest {

    @Test
    fun testAllSyncStatusValues() {
        assertEquals(11, SyncStatus.entries.size)
    }

    @Test
    fun testSyncStatusValues() {
        SyncStatus.UNKNOWN
        SyncStatus.SYNCED
        SyncStatus.PENDING_UPLOAD
        SyncStatus.PENDING_DOWNLOAD
        SyncStatus.SYNCING
        SyncStatus.SYNC_ERROR
        SyncStatus.NOT_SYNCED
        SyncStatus.QUEUED
        SyncStatus.CONFLICT
        SyncStatus.UPLOADING
        SyncStatus.DOWNLOADING
    }
}

class ConflictResolutionEnumTest {

    @Test
    fun testAllConflictResolutionValues() {
        assertEquals(5, ConflictResolution.entries.size)
    }

    @Test
    fun testConflictResolutionValues() {
        ConflictResolution.LOCAL_WINS
        ConflictResolution.REMOTE_WINS
        ConflictResolution.KEEP_BOTH
        ConflictResolution.MANUAL
        ConflictResolution.SKIP
    }
}
