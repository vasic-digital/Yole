package digital.vasic.yole.network.common

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for NetworkDocument data class and its methods.
 */
class NetworkDocumentTest {

    private val now = Clock.System.now()

    @Test
    fun testNetworkDocumentCreation() {
        val doc = NetworkDocument(
            id = "test1",
            name = "test.txt",
            path = "/test.txt",
            isFolder = false,
            size = 1024L,
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )

        assertEquals("test1", doc.id)
        assertEquals("test.txt", doc.name)
        assertEquals("/test.txt", doc.path)
        assertFalse(doc.isFolder)
        assertEquals(1024L, doc.size)
        assertEquals(now, doc.lastModified)
        assertEquals(SyncStatus.SYNCED, doc.syncStatus)
        assertEquals("txt", doc.extension)
        assertEquals("/", doc.parentPath)
        assertFalse(doc.isSyncing)
        assertFalse(doc.hasPendingChanges)
        assertTrue(doc.isAvailableOffline)
        assertFalse(doc.isReadOnly)
        assertFalse(doc.isHidden)
        assertTrue(doc.permissions.isEmpty())
    }

    @Test
    fun testComputedProperties() {
        // Test text file
        val textDoc = NetworkDocument(
            id = "text1",
            name = "document.md",
            path = "/document.md",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertTrue(textDoc.isTextFile)
        assertFalse(textDoc.isImageFile)
        assertFalse(textDoc.isPdfFile)
        assertTrue(textDoc.isPreviewable)
        assertTrue(textDoc.isEditable)

        // Test image file
        val imageDoc = NetworkDocument(
            id = "image1",
            name = "photo.jpg",
            path = "/photo.jpg",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertFalse(imageDoc.isTextFile)
        assertTrue(imageDoc.isImageFile)
        assertFalse(imageDoc.isPdfFile)
        assertTrue(imageDoc.isPreviewable)
        assertFalse(imageDoc.isEditable)

        // Test PDF file
        val pdfDoc = NetworkDocument(
            id = "pdf1",
            name = "document.pdf",
            path = "/document.pdf",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertFalse(pdfDoc.isTextFile)
        assertFalse(pdfDoc.isImageFile)
        assertTrue(pdfDoc.isPdfFile)
        assertTrue(pdfDoc.isPreviewable)
        assertFalse(pdfDoc.isEditable)

        // Test folder
        val folderDoc = NetworkDocument(
            id = "folder1",
            name = "folder",
            path = "/folder",
            isFolder = true,
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertFalse(folderDoc.isTextFile)
        assertFalse(folderDoc.isImageFile)
        assertFalse(folderDoc.isPdfFile)
        assertFalse(folderDoc.isPreviewable)
        assertFalse(folderDoc.isEditable)
        assertEquals("—", folderDoc.formattedSize)
    }

    @Test
    fun testFormattedSize() {
        val smallDoc = NetworkDocument(
            id = "small",
            name = "small.txt",
            path = "/small.txt",
            size = 512L,
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertEquals("512B", smallDoc.formattedSize)

        val kbDoc = NetworkDocument(
            id = "kb",
            name = "kb.txt",
            path = "/kb.txt",
            size = 2048L,
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertEquals("2KB", kbDoc.formattedSize)

        val mbDoc = NetworkDocument(
            id = "mb",
            name = "mb.txt",
            path = "/mb.txt",
            size = 2L * 1024L * 1024L,
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertEquals("2MB", mbDoc.formattedSize)

        val gbDoc = NetworkDocument(
            id = "gb",
            name = "gb.txt",
            path = "/gb.txt",
            size = 3L * 1024L * 1024L * 1024L,
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertEquals("3GB", gbDoc.formattedSize)
    }

    @Test
    fun testIsInPath() {
        val doc = NetworkDocument(
            id = "test",
            name = "file.txt",
            path = "/folder/subfolder/file.txt",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )

        assertTrue(doc.isInPath("/"))
        assertTrue(doc.isInPath("/folder"))
        assertTrue(doc.isInPath("/folder/subfolder"))
        assertFalse(doc.isInPath("/other"))
        assertFalse(doc.isInPath("/folder/other"))
        assertFalse(doc.isInPath("/folder/subfolder/file.txt")) // Exact match should be false
    }

    @Test
    fun testIsDirectChildOf() {
        val doc = NetworkDocument(
            id = "test",
            name = "file.txt",
            path = "/folder/subfolder/file.txt",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )

        assertFalse(doc.isDirectChildOf("/")) // Not a direct child of root
        assertFalse(doc.isDirectChildOf("/folder")) // Not a direct child of /folder
        assertTrue(doc.isDirectChildOf("/folder/subfolder")) // Direct child of /folder/subfolder
        assertFalse(doc.isDirectChildOf("/other"))
    }

    @Test
    fun testWithMethods() {
        val doc = NetworkDocument(
            id = "test",
            name = "file.txt",
            path = "/file.txt",
            lastModified = now,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        // Test withSyncStatus
        val syncedDoc = doc.withSyncStatus(SyncStatus.SYNCED)
        assertEquals(SyncStatus.SYNCED, syncedDoc.syncStatus)
        assertEquals(doc.id, syncedDoc.id) // Other properties unchanged

        // Test withDocumentId
        val linkedDoc = doc.withDocumentId("local123")
        assertEquals("local123", linkedDoc.documentId)

        // Test withMetadata
        val metadataDoc = doc.withMetadata(mapOf("key" to "value"))
        assertEquals(mapOf("key" to "value"), metadataDoc.metadata)

        // Test withPermissions
        val permissionsDoc = doc.withPermissions(setOf(DocumentPermission.READ, DocumentPermission.WRITE))
        assertEquals(setOf(DocumentPermission.READ, DocumentPermission.WRITE), permissionsDoc.permissions)
    }

    @Test
    fun testSyncStatusComputedProperties() {
        // Test SYNCING status
        val syncingDoc = NetworkDocument(
            id = "syncing",
            name = "syncing.txt",
            path = "/syncing.txt",
            lastModified = now,
            syncStatus = SyncStatus.SYNCING
        )
        assertTrue(syncingDoc.isSyncing)
        assertFalse(syncingDoc.hasPendingChanges)
        assertFalse(syncingDoc.isAvailableOffline)

        // Test PENDING_UPLOAD status
        val pendingDoc = NetworkDocument(
            id = "pending",
            name = "pending.txt",
            path = "/pending.txt",
            lastModified = now,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        assertFalse(pendingDoc.isSyncing)
        assertTrue(pendingDoc.hasPendingChanges)
        assertFalse(pendingDoc.isAvailableOffline)

        // Test SYNCED status
        val syncedDoc = NetworkDocument(
            id = "synced",
            name = "synced.txt",
            path = "/synced.txt",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertFalse(syncedDoc.isSyncing)
        assertFalse(syncedDoc.hasPendingChanges)
        assertTrue(syncedDoc.isAvailableOffline)
    }

    @Test
    fun testMockFactoryMethods() {
        // Test mock document
        val mockDoc = NetworkDocument.mock("test.md", "/test.md", false, 2048L, SyncStatus.SYNCED)
        assertEquals("test.md", mockDoc.name)
        assertEquals("/test.md", mockDoc.path)
        assertFalse(mockDoc.isFolder)
        assertEquals(2048L, mockDoc.size)
        assertEquals(SyncStatus.SYNCED, mockDoc.syncStatus)

        // Test mock folder
        val mockFolder = NetworkDocument.mockFolder("testfolder", "/testfolder")
        assertEquals("testfolder", mockFolder.name)
        assertEquals("/testfolder", mockFolder.path)
        assertTrue(mockFolder.isFolder)
        assertEquals(0L, mockFolder.size)
        assertEquals(SyncStatus.SYNCED, mockFolder.syncStatus)
    }

    @Test
    fun testExtensionAndParentPath() {
        // Test various extensions
        val noExt = NetworkDocument(
            id = "noext",
            name = "README",
            path = "/README",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertEquals("", noExt.extension)

        val mdExt = NetworkDocument(
            id = "md",
            name = "README.md",
            path = "/README.md",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertEquals("md", mdExt.extension)

        val multiDot = NetworkDocument(
            id = "multidot",
            name = "file.tar.gz",
            path = "/file.tar.gz",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertEquals("gz", multiDot.extension)

        // Test parent paths
        val rootFile = NetworkDocument(
            id = "root",
            name = "root.txt",
            path = "/root.txt",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertEquals("/", rootFile.parentPath)

        val nestedFile = NetworkDocument(
            id = "nested",
            name = "file.txt",
            path = "/folder/sub/file.txt",
            lastModified = now,
            syncStatus = SyncStatus.SYNCED
        )
        assertEquals("/folder/sub", nestedFile.parentPath)
    }
}