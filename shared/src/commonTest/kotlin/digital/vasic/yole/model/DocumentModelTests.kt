/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Isolated unit tests for Document model, DocumentType enum,
 * SyncStatus enum, DocumentSyncStatus, and ConflictResolution.
 *
 *########################################################*/
package digital.vasic.yole.model

import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.network.common.ConflictResolution
import digital.vasic.yole.network.common.DocumentSyncStatus
import digital.vasic.yole.network.common.DocumentType
import digital.vasic.yole.network.common.SyncStatus
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Isolated unit tests for Document model types.
 *
 * Covers Document companion constants, Document properties,
 * DocumentType enum, SyncStatus enum, DocumentSyncStatus
 * computed properties, and ConflictResolution enum.
 *
 * Total: 50 tests
 */
class DocumentModelTests {

    // ==================== Document Companion Constants ====================

    @Test
    fun documentFormatConstantsMatchTextFormatIds() {
        assertEquals(TextFormat.ID_UNKNOWN, Document.FORMAT_UNKNOWN)
        assertEquals(TextFormat.ID_PLAINTEXT, Document.FORMAT_PLAINTEXT)
        assertEquals(TextFormat.ID_MARKDOWN, Document.FORMAT_MARKDOWN)
        assertEquals(TextFormat.ID_TODOTXT, Document.FORMAT_TODOTXT)
        assertEquals(TextFormat.ID_CSV, Document.FORMAT_CSV)
        assertEquals(TextFormat.ID_WIKITEXT, Document.FORMAT_WIKITEXT)
        assertEquals(TextFormat.ID_KEYVALUE, Document.FORMAT_KEYVALUE)
        assertEquals(TextFormat.ID_ASCIIDOC, Document.FORMAT_ASCIIDOC)
        assertEquals(TextFormat.ID_ORGMODE, Document.FORMAT_ORGMODE)
        assertEquals(TextFormat.ID_LATEX, Document.FORMAT_LATEX)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, Document.FORMAT_RESTRUCTUREDTEXT)
        assertEquals(TextFormat.ID_TASKPAPER, Document.FORMAT_TASKPAPER)
        assertEquals(TextFormat.ID_TEXTILE, Document.FORMAT_TEXTILE)
        assertEquals(TextFormat.ID_CREOLE, Document.FORMAT_CREOLE)
        assertEquals(TextFormat.ID_TIDDLYWIKI, Document.FORMAT_TIDDLYWIKI)
        assertEquals(TextFormat.ID_JUPYTER, Document.FORMAT_JUPYTER)
        assertEquals(TextFormat.ID_RMARKDOWN, Document.FORMAT_RMARKDOWN)
        assertEquals(TextFormat.ID_BINARY, Document.FORMAT_BINARY)
    }

    @Test
    fun formatUnknownIsUnknownString() {
        assertEquals("unknown", Document.FORMAT_UNKNOWN)
    }

    @Test
    fun formatMarkdownIsMarkdownString() {
        assertEquals("markdown", Document.FORMAT_MARKDOWN)
    }

    // ==================== Document Construction ====================

    @Test
    fun documentWithMinimalProperties() {
        val doc = Document(path = "/notes/doc.txt", title = "doc", extension = "txt")
        assertEquals("/notes/doc.txt", doc.path)
        assertEquals("doc", doc.title)
        assertEquals("txt", doc.extension)
        assertEquals(Document.FORMAT_UNKNOWN, doc.format)
    }

    @Test
    fun documentWithAllProperties() {
        val doc = Document(
            id = "abc-123",
            path = "/docs/README.md",
            title = "README",
            extension = "md",
            content = "# Hello",
            author = "Alice",
            tags = listOf("docs", "readme"),
            format = Document.FORMAT_MARKDOWN
        )
        assertEquals("abc-123", doc.id)
        assertEquals("/docs/README.md", doc.path)
        assertEquals("README", doc.title)
        assertEquals("md", doc.extension)
        assertEquals("# Hello", doc.content)
        assertEquals("Alice", doc.author)
        assertEquals(listOf("docs", "readme"), doc.tags)
        assertEquals(Document.FORMAT_MARKDOWN, doc.format)
    }

    @Test
    fun documentDefaultIdIsEmpty() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        assertEquals("", doc.id)
    }

    @Test
    fun documentDefaultContentIsEmpty() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        assertEquals("", doc.content)
    }

    @Test
    fun documentDefaultAuthorIsNull() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        assertNull(doc.author)
    }

    @Test
    fun documentDefaultTagsIsEmpty() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        assertTrue(doc.tags.isEmpty())
    }

    @Test
    fun documentDefaultModTimeIsNegative() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        assertEquals(-1L, doc.modTime)
    }

    @Test
    fun documentDefaultTouchTimeIsNegative() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        assertEquals(-1L, doc.touchTime)
    }

    // ==================== Document.filename ====================

    @Test
    fun filenameWithExtension() {
        val doc = Document(path = "/notes.md", title = "notes", extension = "md")
        assertEquals("notes.md", doc.filename)
    }

    @Test
    fun filenameWithEmptyExtension() {
        val doc = Document(path = "/Makefile", title = "Makefile", extension = "")
        assertEquals("Makefile", doc.filename)
    }

    @Test
    fun filenameWithTxtExtension() {
        val doc = Document(path = "/todo.txt", title = "todo", extension = "txt")
        assertEquals("todo.txt", doc.filename)
    }

    // ==================== Document.hasChanged ====================

    @Test
    fun hasChangedReturnsTrueWhenModTimeIsNegative() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        // modTime = -1 by default
        assertTrue(doc.hasChanged())
    }

    @Test
    fun hasChangedReturnsTrueWhenTouchTimeIsNegative() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        doc.modTime = Long.MAX_VALUE
        // touchTime = -1 by default
        assertTrue(doc.hasChanged())
    }

    // ==================== Document.resetChangeTracking ====================

    @Test
    fun resetChangeTrackingSetsModTimeToNegative() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        doc.modTime = 12345L
        doc.resetChangeTracking()
        assertEquals(-1L, doc.modTime)
    }

    @Test
    fun resetChangeTrackingSetsTouchTimeToNegative() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        doc.touchTime = 12345L
        doc.resetChangeTracking()
        assertEquals(-1L, doc.touchTime)
    }

    // ==================== Document.touch ====================

    @Test
    fun touchSetsTouchTimeToPositiveValue() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        assertEquals(-1L, doc.touchTime)
        doc.touch()
        assertTrue(doc.touchTime > 0)
    }

    @Test
    fun touchTimeAfterTouchIsRecentEpochMs() {
        val before = Clock.System.now().toEpochMilliseconds()
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        doc.touch()
        val after = Clock.System.now().toEpochMilliseconds()
        assertTrue(doc.touchTime in before..after)
    }

    // ==================== Document.detectFormatByExtension ====================

    @Test
    fun detectFormatByExtensionSetsMdToMarkdown() {
        val doc = Document(path = "/README.md", title = "README", extension = "md")
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_MARKDOWN, doc.format)
    }

    @Test
    fun detectFormatByExtensionSetsCsvToCsv() {
        val doc = Document(path = "/data.csv", title = "data", extension = "csv")
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_CSV, doc.format)
    }

    @Test
    fun detectFormatByExtensionUnknownExtensionDefaultsToPlaintext() {
        val doc = Document(path = "/file.xyz", title = "file", extension = "xyz")
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_PLAINTEXT, doc.format)
    }

    // ==================== Document.detectFormatByContent ====================

    @Test
    fun detectFormatByContentMarkdownHeadingReturnsTrue() {
        val doc = Document(path = "/notes.txt", title = "notes", extension = "txt")
        val detected = doc.detectFormatByContent("# Heading\n\nContent")
        assertTrue(detected)
        assertEquals(Document.FORMAT_MARKDOWN, doc.format)
    }

    @Test
    fun detectFormatByContentPlainTextReturnsFalse() {
        val doc = Document(path = "/notes.txt", title = "notes", extension = "txt")
        val detected = doc.detectFormatByContent("Just some regular plain text without patterns.")
        // Plain text has no detection patterns, so null is returned
        assertFalse(detected)
    }

    // ==================== Document equality ====================

    @Test
    fun twoDocumentsWithSamePathTitleFormatAreEqual() {
        val doc1 = Document(path = "/notes.md", title = "notes", extension = "md", format = "markdown")
        val doc2 = Document(path = "/notes.md", title = "notes", extension = "md", format = "markdown")
        assertEquals(doc1, doc2)
    }

    @Test
    fun twoDocumentsWithDifferentPathAreNotEqual() {
        val doc1 = Document(path = "/notes.md", title = "notes", extension = "md")
        val doc2 = Document(path = "/other.md", title = "notes", extension = "md")
        assertFalse(doc1 == doc2)
    }

    // ==================== DocumentType Enum ====================

    @Test
    fun documentTypeHasFileValue() {
        val value = DocumentType.FILE
        assertEquals("FILE", value.name)
    }

    @Test
    fun documentTypeHasFolderValue() {
        val value = DocumentType.FOLDER
        assertEquals("FOLDER", value.name)
    }

    @Test
    fun documentTypeHasUnknownValue() {
        val value = DocumentType.UNKNOWN
        assertEquals("UNKNOWN", value.name)
    }

    @Test
    fun documentTypeEnumHasExactlyThreeValues() {
        assertEquals(3, DocumentType.entries.size)
    }

    @Test
    fun documentTypeValuesAreDistinct() {
        val values = DocumentType.entries
        assertEquals(values.size, values.distinct().size)
    }

    // ==================== SyncStatus Enum ====================

    @Test
    fun syncStatusHasAllExpectedValues() {
        val names = SyncStatus.entries.map { it.name }
        assertTrue(names.contains("UNKNOWN"))
        assertTrue(names.contains("SYNCED"))
        assertTrue(names.contains("PENDING_UPLOAD"))
        assertTrue(names.contains("PENDING_DOWNLOAD"))
        assertTrue(names.contains("SYNCING"))
        assertTrue(names.contains("SYNC_ERROR"))
        assertTrue(names.contains("NOT_SYNCED"))
        assertTrue(names.contains("QUEUED"))
        assertTrue(names.contains("CONFLICT"))
        assertTrue(names.contains("UPLOADING"))
        assertTrue(names.contains("DOWNLOADING"))
    }

    @Test
    fun syncStatusHasElevenValues() {
        assertEquals(11, SyncStatus.entries.size)
    }

    @Test
    fun syncStatusValuesAreDistinct() {
        val values = SyncStatus.entries
        assertEquals(values.size, values.distinct().size)
    }

    // ==================== DocumentSyncStatus ====================

    @Test
    fun documentSyncStatusDefaultValues() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCED)
        assertEquals(SyncStatus.SYNCED, status.status)
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
    fun isSyncingTrueWhenStatusIsSyncing() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING)
        assertTrue(status.isSyncing)
    }

    @Test
    fun isSyncingTrueWhenStatusIsUploading() {
        val status = DocumentSyncStatus(status = SyncStatus.UPLOADING)
        assertTrue(status.isSyncing)
    }

    @Test
    fun isSyncingTrueWhenStatusIsDownloading() {
        val status = DocumentSyncStatus(status = SyncStatus.DOWNLOADING)
        assertTrue(status.isSyncing)
    }

    @Test
    fun isSyncingFalseWhenStatusIsSynced() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCED)
        assertFalse(status.isSyncing)
    }

    @Test
    fun hasFailedTrueWhenStatusIsSyncError() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNC_ERROR)
        assertTrue(status.hasFailed)
    }

    @Test
    fun canRetryTrueWhenFailedAndBelowMaxRetries() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNC_ERROR,
            retryCount = 2,
            maxRetries = 3
        )
        assertTrue(status.canRetry)
    }

    @Test
    fun canRetryFalseWhenRetryCountEqualsMaxRetries() {
        val status = DocumentSyncStatus(
            status = SyncStatus.SYNC_ERROR,
            retryCount = 3,
            maxRetries = 3
        )
        assertFalse(status.canRetry)
    }

    @Test
    fun isPendingTrueForPendingUpload() {
        val status = DocumentSyncStatus(status = SyncStatus.PENDING_UPLOAD)
        assertTrue(status.isPending)
    }

    @Test
    fun isPendingTrueForQueued() {
        val status = DocumentSyncStatus(status = SyncStatus.QUEUED)
        assertTrue(status.isPending)
    }

    @Test
    fun progressPercentageCalculatedCorrectly() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING, progress = 0.75)
        assertEquals(75, status.progressPercentage)
    }

    @Test
    fun withStatusCreatesUpdatedCopy() {
        val original = DocumentSyncStatus(status = SyncStatus.PENDING_UPLOAD)
        val updated = original.withStatus(SyncStatus.SYNCED)
        assertEquals(SyncStatus.SYNCED, updated.status)
        // Original unchanged
        assertEquals(SyncStatus.PENDING_UPLOAD, original.status)
    }

    @Test
    fun withProgressClampsToZeroOne() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING, dataSize = 1000L)
        val updated = status.withProgress(1.5)
        assertEquals(1.0, updated.progress)

        val updated2 = status.withProgress(-0.5)
        assertEquals(0.0, updated2.progress)
    }

    @Test
    fun withErrorSetsStatusAndMessage() {
        val status = DocumentSyncStatus(status = SyncStatus.SYNCING)
        val failed = status.withError("Connection lost")
        assertEquals(SyncStatus.SYNC_ERROR, failed.status)
        assertEquals("Connection lost", failed.errorMessage)
        assertEquals(1, failed.retryCount)
    }

    @Test
    fun withSuccessSetsStatusToSynced() {
        val status = DocumentSyncStatus(
            status = SyncStatus.UPLOADING,
            dataSize = 500L,
            retryCount = 2,
            errorMessage = "previous error"
        )
        val done = status.withSuccess()
        assertEquals(SyncStatus.SYNCED, done.status)
        assertNull(done.errorMessage)
        assertEquals(0, done.retryCount)
        assertEquals(1.0, done.progress)
        assertTrue(done.isAvailableOffline)
    }

    @Test
    fun forNewDocumentCreatesPendingUpload() {
        val status = DocumentSyncStatus.forNewDocument()
        assertEquals(SyncStatus.PENDING_UPLOAD, status.status)
        assertFalse(status.isAvailableOffline)
    }

    @Test
    fun forExistingDocumentCreatesSynced() {
        val status = DocumentSyncStatus.forExistingDocument()
        assertEquals(SyncStatus.SYNCED, status.status)
        assertTrue(status.isAvailableOffline)
    }

    // ==================== ConflictResolution Enum ====================

    @Test
    fun conflictResolutionHasAllExpectedValues() {
        val names = ConflictResolution.entries.map { it.name }
        assertTrue(names.contains("LOCAL_WINS"))
        assertTrue(names.contains("REMOTE_WINS"))
        assertTrue(names.contains("KEEP_BOTH"))
        assertTrue(names.contains("MANUAL"))
        assertTrue(names.contains("SKIP"))
    }

    @Test
    fun conflictResolutionHasFiveValues() {
        assertEquals(5, ConflictResolution.entries.size)
    }
}
