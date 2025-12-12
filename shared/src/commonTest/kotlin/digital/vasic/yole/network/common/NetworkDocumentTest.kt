/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for NetworkDocument
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Comprehensive tests for NetworkDocument covering:
 * - Document creation and properties
 * - File type detection
 * - Path operations
 * - Metadata handling
 * - Copy operations
 */
class NetworkDocumentTest {

    @Test
    fun testBasicDocumentCreation() {
        // Given
        val document = NetworkDocument(
            id = "doc_123",
            name = "document.txt",
            path = "/path/to/document.txt",
            size = 1024L,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        // Then
        assertEquals("doc_123", document.id)
        assertEquals("document.txt", document.name)
        assertEquals("/path/to/document.txt", document.path)
        assertEquals(1024L, document.size)
        assertFalse(document.isFolder)
        assertEquals(SyncStatus.SYNCED, document.syncStatus)
        assertEquals("test-storage", document.storageId)
    }

    @Test
    fun testFolderDocumentCreation() {
        // Given
        val folder = NetworkDocument(
            id = "folder_123",
            name = "myfolder",
            path = "/path/to/myfolder/",
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        // Then
        assertEquals("folder_123", folder.id)
        assertEquals("myfolder", folder.name)
        assertEquals("/path/to/myfolder/", folder.path)
        assertEquals(0L, folder.size) // Folders have size 0
        assertTrue(folder.isFolder)
        assertEquals(SyncStatus.SYNCED, folder.syncStatus)
    }

    @Test
    fun testFileTypeDetection() {
        // Test text file detection
        val textFile = NetworkDocument(
            id = "text_123",
            name = "document.txt",
            path = "/document.txt",
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        assertTrue(textFile.isTextFile, "Should detect .txt as text file")
        assertTrue(textFile.isEditable, "Should detect .txt as editable")
        assertFalse(textFile.isImageFile, "Should not detect .txt as image file")
        assertFalse(textFile.isPdfFile, "Should not detect .txt as PDF file")
        
        // Test image file detection
        val imageFile = NetworkDocument(
            id = "img_123",
            name = "photo.jpg",
            path = "/photo.jpg",
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        assertFalse(imageFile.isTextFile, "Should not detect .jpg as text file")
        assertTrue(imageFile.isImageFile, "Should detect .jpg as image file")
        assertTrue(imageFile.isPreviewable, "Should detect .jpg as previewable")
        
        // Test PDF file detection
        val pdfFile = NetworkDocument(
            id = "pdf_123",
            name = "document.pdf",
            path = "/document.pdf",
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        assertFalse(pdfFile.isTextFile, "Should not detect .pdf as text file")
        assertTrue(pdfFile.isPdfFile, "Should detect .pdf as PDF file")
        assertTrue(pdfFile.isPreviewable, "Should detect .pdf as previewable")
    }

    @Test
    fun testExtensionExtraction() {
        // Test various file extensions
        val testCases = listOf(
            "document.txt" to "txt",
            "image.jpeg" to "jpeg",
            "script.py" to "py",
            "data.csv" to "csv",
            "presentation.pptx" to "pptx",
            "archive.tar.gz" to "gz",
            "noextension" to "",
            ".hidden" to "hidden"
        )
        
        testCases.forEach { (filename, expectedExtension) ->
            val document = NetworkDocument(
                id = "test_123",
                name = filename,
                path = "/$filename",
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                storageId = "test-storage"
            )
            
            assertEquals(expectedExtension, document.extension, "Should extract correct extension for $filename")
        }
    }

    @Test
    fun testFormattedSize() {
        // Test various file sizes
        val sizeTestCases = listOf(
            0L to "0B",
            512L to "512B",
            1024L to "1KB",
            1536L to "1KB",
            1048576L to "1MB",
            1572864L to "1MB",
            1073741824L to "1GB",
            1610612736L to "1GB"
        )
        
        sizeTestCases.forEach { (size, expectedFormattedSize) ->
            val document = NetworkDocument(
                id = "size_123",
                name = "document.txt",
                path = "/document.txt",
                size = size,
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                storageId = "test-storage"
            )
            
            assertEquals(expectedFormattedSize, document.formattedSize, "Should format size correctly for $size bytes")
        }
        
        // Test folder size
        val folder = NetworkDocument(
            id = "folder_123",
            name = "folder",
            path = "/folder/",
            isFolder = true,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        assertEquals("—", folder.formattedSize, "Folders should display em dash for size")
    }

    @Test
    fun testParentPathExtraction() {
        // Test parent path extraction
        val pathTestCases = listOf(
            "/document.txt" to "/",
            "/path/to/document.txt" to "/path/to",
            "/very/deep/path/to/document.txt" to "/very/deep/path/to",
            "document.txt" to "",
            "path/document.txt" to "path"
        )
        
        pathTestCases.forEach { (path, expectedParent) ->
            val document = NetworkDocument(
                id = "path_123",
                name = "document.txt",
                path = path,
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                storageId = "test-storage"
            )
            
            assertEquals(expectedParent, document.parentPath, "Should extract correct parent path for $path")
        }
    }

    @Test
    fun testPathOperations() {
        // Given
        val document = NetworkDocument(
            id = "doc_123",
            name = "document.txt",
            path = "/path/to/document.txt",
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        // Test isInPath
        assertTrue(document.isInPath("/path"), "Should be in parent path")
        assertTrue(document.isInPath("/path/to"), "Should be in direct parent path")
        assertFalse(document.isInPath("/other"), "Should not be in different path")
        assertFalse(document.isInPath("/path/to/other"), "Should not be in sibling path")
        
        // Test isDirectChildOf
        assertTrue(document.isDirectChildOf("/path/to"), "Should be direct child of parent directory")
        assertFalse(document.isDirectChildOf("/path"), "Should not be direct child of grandparent directory")
        assertFalse(document.isDirectChildOf("/other"), "Should not be direct child of different directory")
    }

    @Test
    fun testSyncStatusProperties() {
        // Test different sync statuses
        val syncedDocument = NetworkDocument(
            id = "synced_123",
            name = "synced.txt",
            path = "/synced.txt",
            syncStatus = SyncStatus.SYNCED,
            lastModified = Clock.System.now(),
            storageId = "test-storage"
        )
        
        assertFalse(syncedDocument.isSyncing, "Synced document should not be syncing")
        assertFalse(syncedDocument.hasPendingChanges, "Synced document should not have pending changes")
        assertTrue(syncedDocument.isAvailableOffline, "Synced document should be available offline")
        
        val syncingDocument = NetworkDocument(
            id = "syncing_123",
            name = "syncing.txt",
            path = "/syncing.txt",
            syncStatus = SyncStatus.SYNCING,
            lastModified = Clock.System.now(),
            storageId = "test-storage"
        )
        
        assertTrue(syncingDocument.isSyncing, "Syncing document should be syncing")
        assertFalse(syncingDocument.hasPendingChanges, "Syncing document should not have pending changes")
        assertFalse(syncingDocument.isAvailableOffline, "Syncing document should not be available offline")
        
        val pendingDocument = NetworkDocument(
            id = "pending_123",
            name = "pending.txt",
            path = "/pending.txt",
            syncStatus = SyncStatus.PENDING_UPLOAD,
            lastModified = Clock.System.now(),
            storageId = "test-storage"
        )
        
        assertFalse(pendingDocument.isSyncing, "Pending document should not be syncing")
        assertTrue(pendingDocument.hasPendingChanges, "Pending document should have pending changes")
        assertFalse(pendingDocument.isAvailableOffline, "Pending document should not be available offline")
    }

    @Test
    fun testPermissionProperties() {
        // Test different permission sets
        val readOnlyDocument = NetworkDocument(
            id = "readonly_123",
            name = "readonly.txt",
            path = "/readonly.txt",
            permissions = setOf(DocumentPermission.READ),
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        assertTrue(readOnlyDocument.isReadOnly, "Read-only document should be read-only")
        assertFalse(readOnlyDocument.isEditable, "Read-only document should not be editable")
        
        val editableDocument = NetworkDocument(
            id = "editable_123",
            name = "editable.txt",
            path = "/editable.txt",
            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        assertFalse(editableDocument.isReadOnly, "Editable document should not be read-only")
        assertTrue(editableDocument.isEditable, "Editable document should be editable")
    }

    @Test
    fun testCopyOperations() {
        // Given
        val originalDocument = NetworkDocument(
            id = "original_123",
            name = "document.txt",
            path = "/path/document.txt",
            size = 1024L,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            documentId = "local_doc_123",
            contentType = "text/plain",
            metadata = mapOf("author" to "test_user", "version" to "1.0"),
            tags = listOf("important", "document"),
            owner = "test_owner",
            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE),
            storageId = "test-storage"
        )
        
        // Test withSyncStatus
        val updatedSyncStatus = SyncStatus.SYNCING
        val documentWithNewSync = originalDocument.withSyncStatus(updatedSyncStatus)
        
        assertEquals(updatedSyncStatus, documentWithNewSync.syncStatus, "Should update sync status")
        assertEquals(originalDocument.id, documentWithNewSync.id, "Should preserve other properties")
        assertEquals(originalDocument.name, documentWithNewSync.name, "Should preserve name")
        assertNotSame(originalDocument, documentWithNewSync, "Should create new instance")
        
        // Test withDocumentId
        val newDocumentId = "new_local_doc_456"
        val documentWithNewId = originalDocument.withDocumentId(newDocumentId)
        
        assertEquals(newDocumentId, documentWithNewId.documentId, "Should update document ID")
        assertEquals(originalDocument.syncStatus, documentWithNewId.syncStatus, "Should preserve sync status")
        assertNotSame(originalDocument, documentWithNewId, "Should create new instance")
        
        // Test withMetadata
        val newMetadata = mapOf("updated" to "true", "editor" to "new_editor")
        val documentWithNewMetadata = originalDocument.withMetadata(newMetadata)
        
        assertEquals(newMetadata, documentWithNewMetadata.metadata, "Should update metadata")
        assertEquals(originalDocument.documentId, documentWithNewMetadata.documentId, "Should preserve document ID")
        assertNotSame(originalDocument, documentWithNewMetadata, "Should create new instance")
        
        // Test withPermissions
        val newPermissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.DELETE)
        val documentWithNewPermissions = originalDocument.withPermissions(newPermissions)
        
        assertEquals(newPermissions, documentWithNewPermissions.permissions, "Should update permissions")
        assertEquals(originalDocument.metadata, documentWithNewPermissions.metadata, "Should preserve metadata")
        assertNotSame(originalDocument, documentWithNewPermissions, "Should create new instance")
    }

    @Test
    fun testComplexDocumentCreation() {
        // Test creating a complex document with all properties
        val now = Clock.System.now()
        val complexDocument = NetworkDocument(
            id = "complex_123",
            name = "complex_document.md",
            path = "/documents/projects/complex_document.md",
            isFolder = false,
            size = 2048L,
            lastModified = now,
            syncStatus = SyncStatus.SYNCED,
            documentId = "local_complex_123",
            contentType = "text/markdown",
            metadata = mapOf(
                "author" to "John Doe",
                "created" to "2024-01-01",
                "version" to "2.1",
                "tags" to "important,project,markdown"
            ),
            thumbnails = listOf("/thumbs/complex_123_small.jpg", "/thumbs/complex_123_large.jpg"),
            tags = listOf("important", "project", "documentation"),
            owner = "john.doe@example.com",
            permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE, DocumentPermission.SHARE),
            isHidden = false,
            storageId = "dropbox"
        )
        
        // Verify all properties
        assertEquals("complex_123", complexDocument.id)
        assertEquals("complex_document.md", complexDocument.name)
        assertEquals("/documents/projects/complex_document.md", complexDocument.path)
        assertFalse(complexDocument.isFolder)
        assertEquals(2048L, complexDocument.size)
        assertEquals(now, complexDocument.lastModified)
        assertEquals(SyncStatus.SYNCED, complexDocument.syncStatus)
        assertEquals("local_complex_123", complexDocument.documentId)
        assertEquals("text/markdown", complexDocument.contentType)
        assertEquals("md", complexDocument.extension)
        assertEquals("/documents/projects", complexDocument.parentPath)
        assertEquals(3, complexDocument.metadata.size)
        assertEquals(2, complexDocument.thumbnails.size)
        assertEquals(3, complexDocument.tags.size)
        assertEquals("john.doe@example.com", complexDocument.owner)
        assertEquals(3, complexDocument.permissions.size)
        assertFalse(complexDocument.isHidden)
        assertEquals("dropbox", complexDocument.storageId)
        
        // Test derived properties
        assertTrue(complexDocument.isTextFile)
        assertTrue(complexDocument.isEditable)
        assertFalse(complexDocument.isImageFile)
        assertFalse(complexDocument.isPdfFile)
        assertTrue(complexDocument.isPreviewable)
        assertFalse(complexDocument.isReadOnly)
        assertFalse(complexDocument.isSyncing)
        assertFalse(complexDocument.hasPendingChanges)
        assertTrue(complexDocument.isAvailableOffline)
        assertEquals("2KB", complexDocument.formattedSize)
    }

    @Test
    fun testDocumentEquality() {
        // Given
        val now = Clock.System.now()
        
        val document1 = NetworkDocument(
            id = "test_123",
            name = "document.txt",
            path = "/document.txt",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        val document2 = NetworkDocument(
            id = "test_123",
            name = "document.txt",
            path = "/document.txt",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        val document3 = NetworkDocument(
            id = "different_123",
            name = "document.txt",
            path = "/document.txt",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED,
            storageId = "test-storage"
        )
        
        // Then
        assertEquals(document1, document2, "Documents with same properties should be equal")
        assertNotEquals(document1, document3, "Documents with different IDs should not be equal")
        assertEquals(document1.hashCode(), document2.hashCode(), "Equal documents should have same hash code")
    }
}