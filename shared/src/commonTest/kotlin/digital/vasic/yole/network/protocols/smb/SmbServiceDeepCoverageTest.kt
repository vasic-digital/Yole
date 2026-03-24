/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Deep coverage tests for SmbService
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.smb

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Deep coverage test suite for [SmbService].
 *
 * Exercises every code path that existing tests leave uncovered,
 * including upload/download progress flows, file tree mutations,
 * cache lifecycle, sync operations, quota calculations, folder
 * operations with children, rename/move/copy semantics, and
 * operation management.
 */
class SmbServiceDeepCoverageTest {

    private lateinit var service: SmbService

    @BeforeTest
    fun setup() {
        val config = StorageConfig.SmbConfig(
            name = "deep-test",
            host = "10.0.0.1",
            share = "data",
            domain = "TESTDOMAIN",
            username = "admin",
            password = "secret",
            path = "/"
        )
        service = SmbService(config)
    }

    // ----------------------------------------------------------------
    // 1. Upload flow
    // ----------------------------------------------------------------

    @Test
    fun `upload emits exactly 4 operations when connected`() = runBlocking<Unit> {
        service.connect()
        val ops = service.uploadFile("/local/a.txt", "/remote/a.txt").toList()
        assertEquals(4, ops.size)
    }

    @Test
    fun `upload first emission is IN_PROGRESS with progress 0`() = runBlocking<Unit> {
        service.connect()
        val ops = service.uploadFile("/local/a.txt", "/remote/a.txt").toList()
        assertEquals(NetworkOperation.Status.IN_PROGRESS, ops[0].status)
        assertEquals(0.0, ops[0].progress)
    }

    @Test
    fun `upload second emission is IN_PROGRESS with progress 0_5`() = runBlocking<Unit> {
        service.connect()
        val ops = service.uploadFile("/local/a.txt", "/remote/a.txt").toList()
        assertEquals(NetworkOperation.Status.IN_PROGRESS, ops[1].status)
        assertEquals(0.5, ops[1].progress)
    }

    @Test
    fun `upload third emission is IN_PROGRESS with progress 1_0`() = runBlocking<Unit> {
        service.connect()
        val ops = service.uploadFile("/local/a.txt", "/remote/a.txt").toList()
        assertEquals(NetworkOperation.Status.IN_PROGRESS, ops[2].status)
        assertEquals(1.0, ops[2].progress)
    }

    @Test
    fun `upload fourth emission is COMPLETED`() = runBlocking<Unit> {
        service.connect()
        val ops = service.uploadFile("/local/a.txt", "/remote/a.txt").toList()
        assertEquals(NetworkOperation.Status.COMPLETED, ops[3].status)
        assertEquals(1.0, ops[3].progress)
    }

    @Test
    fun `upload startedAt is set on first emission`() = runBlocking<Unit> {
        service.connect()
        val ops = service.uploadFile("/local/a.txt", "/remote/a.txt").toList()
        assertNotNull(ops[0].startedAt)
    }

    @Test
    fun `upload completedAt is set on last emission`() = runBlocking<Unit> {
        service.connect()
        val ops = service.uploadFile("/local/a.txt", "/remote/a.txt").toList()
        assertNotNull(ops[3].completedAt)
    }

    @Test
    fun `uploaded file appears in file tree via getFileInfo`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/a.txt", "/remote/a.txt").toList()

        val info = service.getFileInfo("/remote/a.txt")
        assertTrue(info.isSuccess)
        val doc = info.getOrThrow()
        assertEquals("/remote/a.txt", doc.path)
        assertEquals("a.txt", doc.name)
        assertFalse(doc.isFolder)
        assertEquals("smb", doc.storageId)
    }

    @Test
    fun `uploaded file has SYNCED sync status`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/a.txt", "/remote/a.txt").toList()

        val statusMap = service.getSyncStatus("/remote/a.txt").first()
        assertTrue(statusMap.containsKey("/remote/a.txt"))
        assertEquals(SyncStatus.SYNCED, statusMap["/remote/a.txt"])
    }

    @Test
    fun `uploaded file has default file permissions`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/a.txt", "/remote/a.txt").toList()

        val doc = service.getFileInfo("/remote/a.txt").getOrThrow()
        assertTrue(doc.permissions.contains(DocumentPermission.READ))
        assertTrue(doc.permissions.contains(DocumentPermission.WRITE))
        assertTrue(doc.permissions.contains(DocumentPermission.DELETE))
        assertFalse(doc.permissions.contains(DocumentPermission.EXECUTE))
    }

    @Test
    fun `upload while disconnected emits single FAILED operation`() = runBlocking<Unit> {
        val ops = service.uploadFile("/local/a.txt", "/remote/a.txt").toList()
        assertEquals(1, ops.size)
        assertEquals(NetworkOperation.Status.FAILED, ops[0].status)
        assertTrue(ops[0].error?.contains("not connected") == true)
    }

    @Test
    fun `upload all emissions have type UPLOAD`() = runBlocking<Unit> {
        service.connect()
        val ops = service.uploadFile("/local/a.txt", "/remote/a.txt").toList()
        assertTrue(ops.all { it.type == NetworkOperation.Type.UPLOAD })
    }

    // ----------------------------------------------------------------
    // 2. Download flow
    // ----------------------------------------------------------------

    @Test
    fun `download emits exactly 4 operations when connected`() = runBlocking<Unit> {
        service.connect()
        val ops = service.downloadFile("/remote/b.txt", "/local/b.txt").toList()
        assertEquals(4, ops.size)
    }

    @Test
    fun `download emissions have correct progress sequence`() = runBlocking<Unit> {
        service.connect()
        val ops = service.downloadFile("/remote/b.txt", "/local/b.txt").toList()
        assertEquals(0.0, ops[0].progress)
        assertEquals(0.5, ops[1].progress)
        assertEquals(1.0, ops[2].progress)
        assertEquals(1.0, ops[3].progress)
    }

    @Test
    fun `download final emission is COMPLETED`() = runBlocking<Unit> {
        service.connect()
        val ops = service.downloadFile("/remote/b.txt", "/local/b.txt").toList()
        assertEquals(NetworkOperation.Status.COMPLETED, ops[3].status)
    }

    @Test
    fun `download updates sync status to SYNCED`() = runBlocking<Unit> {
        service.connect()
        service.downloadFile("/remote/b.txt", "/local/b.txt").toList()

        val statusMap = service.getSyncStatus("/remote/b.txt").first()
        assertTrue(statusMap.containsKey("/remote/b.txt"))
        assertEquals(SyncStatus.SYNCED, statusMap["/remote/b.txt"])
    }

    @Test
    fun `download while disconnected emits FAILED`() = runBlocking<Unit> {
        val ops = service.downloadFile("/remote/b.txt", "/local/b.txt").toList()
        assertEquals(1, ops.size)
        assertEquals(NetworkOperation.Status.FAILED, ops[0].status)
        assertTrue(ops[0].error?.contains("not connected") == true)
    }

    @Test
    fun `download all emissions have type DOWNLOAD`() = runBlocking<Unit> {
        service.connect()
        val ops = service.downloadFile("/remote/b.txt", "/local/b.txt").toList()
        assertTrue(ops.all { it.type == NetworkOperation.Type.DOWNLOAD })
    }

    @Test
    fun `download startedAt is set`() = runBlocking<Unit> {
        service.connect()
        val ops = service.downloadFile("/remote/b.txt", "/local/b.txt").toList()
        assertNotNull(ops[0].startedAt)
    }

    // ----------------------------------------------------------------
    // 3. Copy file
    // ----------------------------------------------------------------

    @Test
    fun `copy existing file creates destination with correct properties`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/src.txt", "/src.txt").toList()

        val result = service.copyFile("/src.txt", "/dest.txt")
        assertTrue(result.isSuccess)

        val destInfo = service.getFileInfo("/dest.txt").getOrThrow()
        assertEquals("/dest.txt", destInfo.path)
        assertEquals("dest.txt", destInfo.name)
        assertFalse(destInfo.isFolder)
        assertEquals(SyncStatus.SYNCED, destInfo.syncStatus)
    }

    @Test
    fun `copy existing file leaves source intact`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/src.txt", "/src.txt").toList()
        service.copyFile("/src.txt", "/dest.txt")

        val srcExists = service.exists("/src.txt").getOrThrow()
        assertTrue(srcExists)
    }

    @Test
    fun `copy non-existing file creates placeholder at destination`() = runBlocking<Unit> {
        service.connect()
        val result = service.copyFile("/nonexistent.txt", "/placeholder.txt")
        assertTrue(result.isSuccess)

        val info = service.getFileInfo("/placeholder.txt").getOrThrow()
        assertEquals("/placeholder.txt", info.path)
        assertEquals("placeholder.txt", info.name)
        assertFalse(info.isFolder)
        assertEquals(0L, info.size)
    }

    @Test
    fun `copy sets sync status to SYNCED for destination`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/f.txt", "/f.txt").toList()
        service.copyFile("/f.txt", "/f_copy.txt")

        val statusMap = service.getSyncStatus("/f_copy.txt").first()
        assertEquals(SyncStatus.SYNCED, statusMap["/f_copy.txt"])
    }

    @Test
    fun `copy auto-creates parent folders for deep destination`() = runBlocking<Unit> {
        service.connect()
        service.copyFile("/src.txt", "/deep/nested/copy.txt")

        val parentExists = service.exists("/deep/nested").getOrThrow()
        assertTrue(parentExists)
        val parentInfo = service.getFileInfo("/deep/nested").getOrThrow()
        assertTrue(parentInfo.isFolder)
    }

    // ----------------------------------------------------------------
    // 4. Delete file with children
    // ----------------------------------------------------------------

    @Test
    fun `delete folder removes all children`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/c.txt", "/a/b/c.txt").toList()
        service.uploadFile("/local/d.txt", "/a/b/d.txt").toList()
        service.uploadFile("/local/e.txt", "/a/e.txt").toList()

        val deleteResult = service.deleteFile("/a/b")
        assertTrue(deleteResult.isSuccess)

        assertFalse(service.exists("/a/b/c.txt").getOrThrow())
        assertFalse(service.exists("/a/b/d.txt").getOrThrow())
        assertFalse(service.exists("/a/b").getOrThrow())
    }

    @Test
    fun `delete folder keeps sibling files`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/c.txt", "/a/b/c.txt").toList()
        service.uploadFile("/local/e.txt", "/a/e.txt").toList()

        service.deleteFile("/a/b")

        assertTrue(service.exists("/a/e.txt").getOrThrow())
    }

    @Test
    fun `delete cleans up sync status for removed paths`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/x.txt", "/dir/x.txt").toList()
        service.uploadFile("/local/y.txt", "/dir/y.txt").toList()

        service.deleteFile("/dir")

        val statusMap = service.getSyncStatus("/dir").first()
        assertFalse(statusMap.containsKey("/dir/x.txt"))
        assertFalse(statusMap.containsKey("/dir/y.txt"))
    }

    @Test
    fun `delete cleans up cache for removed paths`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/x.txt", "/dir/x.txt").toList()
        service.addToCache("/dir/x.txt", 5)

        service.deleteFile("/dir")

        val entries = service.getCacheEntries("/dir").first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `delete single file succeeds`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/f.txt", "/single.txt").toList()

        val result = service.deleteFile("/single.txt")
        assertTrue(result.isSuccess)
        assertFalse(service.exists("/single.txt").getOrThrow())
    }

    // ----------------------------------------------------------------
    // 5. Create folder
    // ----------------------------------------------------------------

    @Test
    fun `createFolder auto-creates parent folders`() = runBlocking<Unit> {
        val result = service.createFolder("/deep/nested/folder")
        assertTrue(result.isSuccess)

        assertTrue(service.exists("/deep").getOrThrow())
        assertTrue(service.exists("/deep/nested").getOrThrow())
        assertTrue(service.exists("/deep/nested/folder").getOrThrow())
    }

    @Test
    fun `createFolder returns document with isFolder true`() = runBlocking<Unit> {
        val doc = service.createFolder("/myfolder").getOrThrow()
        assertTrue(doc.isFolder)
    }

    @Test
    fun `createFolder returns document with EXECUTE permission`() = runBlocking<Unit> {
        val doc = service.createFolder("/myfolder").getOrThrow()
        assertTrue(doc.permissions.contains(DocumentPermission.EXECUTE))
        assertTrue(doc.permissions.contains(DocumentPermission.READ))
        assertTrue(doc.permissions.contains(DocumentPermission.WRITE))
        assertTrue(doc.permissions.contains(DocumentPermission.DELETE))
    }

    @Test
    fun `createFolder sets sync status to SYNCED`() = runBlocking<Unit> {
        service.createFolder("/syncfolder")
        val statusMap = service.getSyncStatus("/syncfolder").first()
        assertEquals(SyncStatus.SYNCED, statusMap["/syncfolder"])
    }

    @Test
    fun `createFolder parent folders are also folders`() = runBlocking<Unit> {
        service.connect()
        service.createFolder("/p1/p2/target")

        val p1 = service.getFileInfo("/p1").getOrThrow()
        assertTrue(p1.isFolder)
        val p2 = service.getFileInfo("/p1/p2").getOrThrow()
        assertTrue(p2.isFolder)
    }

    @Test
    fun `createFolder has correct name`() = runBlocking<Unit> {
        val doc = service.createFolder("/path/to/newfolder").getOrThrow()
        assertEquals("newfolder", doc.name)
        assertEquals("/path/to/newfolder", doc.path)
    }

    // ----------------------------------------------------------------
    // 6. Rename file
    // ----------------------------------------------------------------

    @Test
    fun `rename file changes path and name`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/old.txt", "/docs/old.txt").toList()

        val result = service.renameFile("/docs/old.txt", "new.txt")
        assertTrue(result.isSuccess)

        assertFalse(service.exists("/docs/old.txt").getOrThrow())
        assertTrue(service.exists("/docs/new.txt").getOrThrow())

        val doc = service.getFileInfo("/docs/new.txt").getOrThrow()
        assertEquals("new.txt", doc.name)
        assertEquals("/docs/new.txt", doc.path)
    }

    @Test
    fun `rename folder updates all children paths`() = runBlocking<Unit> {
        service.connect()
        service.createFolder("/parent/oldfolder")
        service.uploadFile("/local/child.txt", "/parent/oldfolder/child.txt").toList()

        service.renameFile("/parent/oldfolder", "newfolder")

        assertFalse(service.exists("/parent/oldfolder").getOrThrow())
        assertFalse(service.exists("/parent/oldfolder/child.txt").getOrThrow())
        assertTrue(service.exists("/parent/newfolder").getOrThrow())
        assertTrue(service.exists("/parent/newfolder/child.txt").getOrThrow())
    }

    @Test
    fun `rename migrates sync status to new path`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/s.txt", "/s.txt").toList()

        service.renameFile("/s.txt", "t.txt")

        val statusMap = service.getSyncStatus(null).first()
        assertFalse(statusMap.containsKey("/s.txt"))
        assertEquals(SyncStatus.SYNCED, statusMap["/t.txt"])
    }

    @Test
    fun `rename non-existing file succeeds silently`() = runBlocking<Unit> {
        val result = service.renameFile("/no-such-file.txt", "whatever.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `rename file at root level`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/root.txt", "/root.txt").toList()

        service.renameFile("/root.txt", "renamed.txt")

        assertTrue(service.exists("/renamed.txt").getOrThrow())
        assertFalse(service.exists("/root.txt").getOrThrow())
    }

    // ----------------------------------------------------------------
    // 7. Move file
    // ----------------------------------------------------------------

    @Test
    fun `move file changes path`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/m.txt", "/a/file.txt").toList()

        val result = service.moveFile("/a/file.txt", "/b/file.txt")
        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals("/b/file.txt", doc.path)
        assertEquals("file.txt", doc.name)
    }

    @Test
    fun `move file source no longer exists`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/m.txt", "/a/file.txt").toList()

        service.moveFile("/a/file.txt", "/b/file.txt")

        assertFalse(service.exists("/a/file.txt").getOrThrow())
        assertTrue(service.exists("/b/file.txt").getOrThrow())
    }

    @Test
    fun `move folder with children updates all paths`() = runBlocking<Unit> {
        service.connect()
        service.createFolder("/src/folder")
        service.uploadFile("/local/c1.txt", "/src/folder/c1.txt").toList()
        service.uploadFile("/local/c2.txt", "/src/folder/c2.txt").toList()

        val result = service.moveFile("/src/folder", "/dst/folder")
        assertTrue(result.isSuccess)

        assertFalse(service.exists("/src/folder").getOrThrow())
        assertFalse(service.exists("/src/folder/c1.txt").getOrThrow())
        assertFalse(service.exists("/src/folder/c2.txt").getOrThrow())

        assertTrue(service.exists("/dst/folder").getOrThrow())
        assertTrue(service.exists("/dst/folder/c1.txt").getOrThrow())
        assertTrue(service.exists("/dst/folder/c2.txt").getOrThrow())
    }

    @Test
    fun `move migrates cache entries to new path`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/m.txt", "/cached.txt").toList()
        service.addToCache("/cached.txt", 3)

        service.moveFile("/cached.txt", "/moved.txt")

        val oldEntries = service.getCacheEntries("/cached.txt").first()
        assertTrue(oldEntries.isEmpty())

        val newEntries = service.getCacheEntries("/moved.txt").first()
        assertEquals(1, newEntries.size)
        assertEquals("/moved.txt", newEntries[0].remotePath)
    }

    @Test
    fun `move migrates sync status`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/m.txt", "/before.txt").toList()

        service.moveFile("/before.txt", "/after.txt")

        val statusMap = service.getSyncStatus(null).first()
        assertFalse(statusMap.containsKey("/before.txt"))
        assertEquals(SyncStatus.SYNCED, statusMap["/after.txt"])
    }

    @Test
    fun `move non-existing source creates document at destination`() = runBlocking<Unit> {
        val result = service.moveFile("/nonexist.txt", "/created.txt")
        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals("/created.txt", doc.path)
        assertEquals("created.txt", doc.name)
        assertFalse(doc.isFolder)
        assertEquals(SyncStatus.SYNCED, doc.syncStatus)
    }

    @Test
    fun `move auto-creates parent folders at destination`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/m.txt", "/file.txt").toList()

        service.moveFile("/file.txt", "/deep/nested/file.txt")

        assertTrue(service.exists("/deep").getOrThrow())
        assertTrue(service.exists("/deep/nested").getOrThrow())
    }

    // ----------------------------------------------------------------
    // 8. getFileInfo
    // ----------------------------------------------------------------

    @Test
    fun `getFileInfo for uploaded file returns actual document`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/x.txt", "/uploaded.txt").toList()

        val doc = service.getFileInfo("/uploaded.txt").getOrThrow()
        assertEquals("/uploaded.txt", doc.path)
        assertEquals("uploaded.txt", doc.name)
        assertEquals("smb", doc.storageId)
        assertEquals(SyncStatus.SYNCED, doc.syncStatus)
        assertTrue(doc.permissions.contains(DocumentPermission.DELETE))
    }

    @Test
    fun `getFileInfo for non-existing path returns synthesized document`() = runBlocking<Unit> {
        service.connect()
        val doc = service.getFileInfo("/nowhere/phantom.txt").getOrThrow()
        assertEquals("/nowhere/phantom.txt", doc.path)
        assertEquals("phantom.txt", doc.name)
        assertFalse(doc.isFolder)
        assertEquals(0L, doc.size)
        assertEquals("smb", doc.storageId)
        assertTrue(doc.permissions.contains(DocumentPermission.READ))
        assertTrue(doc.permissions.contains(DocumentPermission.WRITE))
        // Synthesized document has only READ and WRITE
        assertFalse(doc.permissions.contains(DocumentPermission.DELETE))
    }

    @Test
    fun `getFileInfo for created folder returns folder document`() = runBlocking<Unit> {
        service.connect()
        service.createFolder("/myfolder")
        val doc = service.getFileInfo("/myfolder").getOrThrow()
        assertTrue(doc.isFolder)
        assertTrue(doc.permissions.contains(DocumentPermission.EXECUTE))
    }

    // ----------------------------------------------------------------
    // 9. exists
    // ----------------------------------------------------------------

    @Test
    fun `exists returns true for uploaded file`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/e.txt", "/existing.txt").toList()

        assertTrue(service.exists("/existing.txt").getOrThrow())
    }

    @Test
    fun `exists returns false for non-existing file`() = runBlocking<Unit> {
        assertFalse(service.exists("/nonexisting.txt").getOrThrow())
    }

    @Test
    fun `exists returns true for created folder`() = runBlocking<Unit> {
        service.createFolder("/folder")
        assertTrue(service.exists("/folder").getOrThrow())
    }

    @Test
    fun `exists returns true for auto-created parent folder`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/x.txt", "/auto/parent/file.txt").toList()
        assertTrue(service.exists("/auto/parent").getOrThrow())
        assertTrue(service.exists("/auto").getOrThrow())
    }

    // ----------------------------------------------------------------
    // 10. Cache operations - full lifecycle
    // ----------------------------------------------------------------

    @Test
    fun `addToCache creates new entry`() = runBlocking<Unit> {
        service.addToCache("/file.txt", 5)

        val entries = service.getCacheEntries("/file.txt").first()
        assertEquals(1, entries.size)
        assertEquals("/file.txt", entries[0].remotePath)
    }

    @Test
    fun `addToCache for existing entry updates priority and access`() = runBlocking<Unit> {
        service.addToCache("/file.txt", 1)
        service.addToCache("/file.txt", 10)

        val entries = service.getCacheEntries("/file.txt").first()
        assertEquals(1, entries.size)
        assertEquals(10, entries[0].priority)
        assertTrue(entries[0].accessCount > 0)
    }

    @Test
    fun `getCacheEntries with path filter returns matching entries`() = runBlocking<Unit> {
        service.addToCache("/dir/a.txt", 1)
        service.addToCache("/dir/b.txt", 2)
        service.addToCache("/other/c.txt", 3)

        val dirEntries = service.getCacheEntries("/dir").first()
        assertEquals(2, dirEntries.size)
        assertTrue(dirEntries.all { it.remotePath.startsWith("/dir") })
    }

    @Test
    fun `getCacheEntries without path filter returns all entries`() = runBlocking<Unit> {
        service.addToCache("/a.txt", 1)
        service.addToCache("/b.txt", 2)
        service.addToCache("/c.txt", 3)

        val allEntries = service.getCacheEntries(null).first()
        assertEquals(3, allEntries.size)
    }

    @Test
    fun `removeFromCache removes specific entry`() = runBlocking<Unit> {
        service.addToCache("/keep.txt", 1)
        service.addToCache("/remove.txt", 2)

        service.removeFromCache("/remove.txt")

        val entries = service.getCacheEntries(null).first()
        assertEquals(1, entries.size)
        assertEquals("/keep.txt", entries[0].remotePath)
    }

    @Test
    fun `clearCache removes all entries`() = runBlocking<Unit> {
        service.addToCache("/a.txt", 1)
        service.addToCache("/b.txt", 2)
        service.addToCache("/c.txt", 3)

        service.clearCache()

        val entries = service.getCacheEntries(null).first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `addToCache uses file size from tree when file is uploaded`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/f.txt", "/sized.txt").toList()
        service.addToCache("/sized.txt", 1)

        val entries = service.getCacheEntries("/sized.txt").first()
        assertEquals(1, entries.size)
        // File from upload has size 0L (no actual content)
        assertEquals(0L, entries[0].size)
    }

    @Test
    fun `removeFromCache for non-existing entry succeeds`() = runBlocking<Unit> {
        val result = service.removeFromCache("/doesnt/exist.txt")
        assertTrue(result.isSuccess)
    }

    // ----------------------------------------------------------------
    // 11. Sync operations
    // ----------------------------------------------------------------

    @Test
    fun `syncFile transitions status from SYNCING to SYNCED`() = runBlocking<Unit> {
        val ops = service.syncFile("/file.txt", false).toList()

        assertEquals(4, ops.size)
        assertEquals(NetworkOperation.Status.IN_PROGRESS, ops[0].status)
        assertEquals(0.0, ops[0].progress)
        assertEquals(NetworkOperation.Status.IN_PROGRESS, ops[1].status)
        assertEquals(0.5, ops[1].progress)
        assertEquals(NetworkOperation.Status.IN_PROGRESS, ops[2].status)
        assertEquals(1.0, ops[2].progress)
        assertEquals(NetworkOperation.Status.COMPLETED, ops[3].status)
    }

    @Test
    fun `syncFile updates sync status map to SYNCED`() = runBlocking<Unit> {
        service.syncFile("/synced.txt", false).toList()

        val statusMap = service.getSyncStatus("/synced.txt").first()
        assertEquals(SyncStatus.SYNCED, statusMap["/synced.txt"])
    }

    @Test
    fun `syncFile has correct operation type SYNC`() = runBlocking<Unit> {
        val ops = service.syncFile("/file.txt", false).toList()
        assertTrue(ops.all { it.type == NetworkOperation.Type.SYNC })
    }

    @Test
    fun `syncFile with forceSync flag succeeds`() = runBlocking<Unit> {
        val ops = service.syncFile("/file.txt", true).toList()
        assertEquals(NetworkOperation.Status.COMPLETED, ops.last().status)
    }

    @Test
    fun `syncAll marks all tracked files as SYNCED`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/a.txt", "/a.txt").toList()
        service.uploadFile("/local/b.txt", "/b.txt").toList()

        service.syncAll(false).toList()

        val statusMap = service.getSyncStatus(null).first()
        assertEquals(SyncStatus.SYNCED, statusMap["/a.txt"])
        assertEquals(SyncStatus.SYNCED, statusMap["/b.txt"])
    }

    @Test
    fun `syncAll returns empty flow`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/a.txt", "/a.txt").toList()

        val ops = service.syncAll(false).toList()
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `syncAll with forceSync returns empty flow`() = runBlocking<Unit> {
        val ops = service.syncAll(true).toList()
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `getSyncStatus with path filter returns matching entries`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/a.txt", "/dir/a.txt").toList()
        service.uploadFile("/local/b.txt", "/dir/b.txt").toList()
        service.uploadFile("/local/c.txt", "/other/c.txt").toList()

        val statusMap = service.getSyncStatus("/dir").first()
        assertTrue(statusMap.containsKey("/dir/a.txt"))
        assertTrue(statusMap.containsKey("/dir/b.txt"))
        assertFalse(statusMap.containsKey("/other/c.txt"))
    }

    @Test
    fun `getSyncStatus without filter returns all entries`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/a.txt", "/dir/a.txt").toList()
        service.uploadFile("/local/c.txt", "/other/c.txt").toList()

        val statusMap = service.getSyncStatus(null).first()
        assertTrue(statusMap.size >= 2)
        assertTrue(statusMap.containsKey("/dir/a.txt"))
        assertTrue(statusMap.containsKey("/other/c.txt"))
    }

    // ----------------------------------------------------------------
    // 12. getRecentChanges
    // ----------------------------------------------------------------

    @Test
    fun `getRecentChanges returns uploaded files after since timestamp`() = runBlocking<Unit> {
        val before = Clock.System.now()

        service.connect()
        service.uploadFile("/local/recent.txt", "/recent.txt").toList()

        val changes = service.getRecentChanges(before, null).first()
        assertTrue(changes.isNotEmpty())
        assertTrue(changes.any { it.path == "/recent.txt" })
    }

    @Test
    fun `getRecentChanges filters by path prefix`() = runBlocking<Unit> {
        val before = Clock.System.now()
        service.connect()
        service.uploadFile("/local/a.txt", "/dir1/a.txt").toList()
        service.uploadFile("/local/b.txt", "/dir2/b.txt").toList()

        val changes = service.getRecentChanges(before, "/dir1").first()
        assertTrue(changes.all { it.path.startsWith("/dir1") })
    }

    @Test
    fun `getRecentChanges with null path returns all changes`() = runBlocking<Unit> {
        val before = Clock.System.now()
        service.connect()
        service.uploadFile("/local/a.txt", "/x.txt").toList()
        service.uploadFile("/local/b.txt", "/y.txt").toList()

        val changes = service.getRecentChanges(before, null).first()
        assertTrue(changes.size >= 2)
    }

    @Test
    fun `getRecentChanges excludes files modified before since`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/old.txt", "/old.txt").toList()

        val after = Clock.System.now().plus(1.hours)
        val changes = service.getRecentChanges(after, null).first()
        assertFalse(changes.any { it.path == "/old.txt" })
    }

    // ----------------------------------------------------------------
    // 13. getQuotaInfo
    // ----------------------------------------------------------------

    @Test
    fun `getQuotaInfo with empty tree returns 100MB default used`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrThrow()
        assertEquals(1000000000L, quota.totalSpace)
        assertEquals(100000000L, quota.usedSpace)
        assertEquals(900000000L, quota.availableSpace)
        assertEquals(0.1, quota.usagePercentage)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
    }

    @Test
    fun `getQuotaInfo total space is 1GB`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrThrow()
        assertEquals(1000000000L, quota.totalSpace)
    }

    @Test
    fun `getQuotaInfo available plus used equals total`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrThrow()
        assertEquals(quota.totalSpace, quota.usedSpace + quota.availableSpace)
    }

    @Test
    fun `getQuotaInfo usagePercentage is consistent`() = runBlocking<Unit> {
        val quota = service.getQuotaInfo().getOrThrow()
        val expected = quota.usedSpace.toDouble() / quota.totalSpace.toDouble()
        assertEquals(expected, quota.usagePercentage)
    }

    // ----------------------------------------------------------------
    // 14. listFiles
    // ----------------------------------------------------------------

    @Test
    fun `listFiles returns failure when disconnected`() = runBlocking<Unit> {
        val results = service.listFiles("/").toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isFailure)
        assertIs<NetworkStorageException.ConnectionException.NotConnected>(
            results[0].exceptionOrNull()
        )
    }

    @Test
    fun `listFiles returns success with empty list when connected`() = runBlocking<Unit> {
        service.connect()
        val results = service.listFiles("/test").toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        val files = results[0].getOrThrow()
        assertTrue(files.isEmpty(), "Empty file tree should return empty list")
    }

    // ----------------------------------------------------------------
    // 15. searchFiles
    // ----------------------------------------------------------------

    @Test
    fun `searchFiles when not connected returns connection error`() = runBlocking<Unit> {
        val results = service.searchFiles("query", null, false).toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isFailure)
        assertEquals("SMB not connected", results[0].exceptionOrNull()?.message)
    }

    @Test
    fun `searchFiles when connected returns empty list for no matches`() = runBlocking<Unit> {
        service.connect()
        val results = service.searchFiles("nonexistent_file_xyz", "/", false).toList()
        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertTrue(results[0].getOrNull()!!.isEmpty())
    }

    @Test
    fun `searchFiles with path and includeContent when not connected returns failure`() = runBlocking<Unit> {
        val results = service.searchFiles("query", "/dir", true).toList()
        assertTrue(results[0].isFailure)
    }

    // ----------------------------------------------------------------
    // 16. getActiveOperations
    // ----------------------------------------------------------------

    @Test
    fun `getActiveOperations returns empty list when no operations`() = runBlocking<Unit> {
        val ops = service.getActiveOperations().first()
        assertTrue(ops.isEmpty())
    }

    // ----------------------------------------------------------------
    // 17. cancelOperation / pauseOperation / resumeOperation
    // ----------------------------------------------------------------

    @Test
    fun `cancelOperation returns success for nonexistent id`() = runBlocking<Unit> {
        val result = service.cancelOperation(999L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `pauseOperation returns success for nonexistent id`() = runBlocking<Unit> {
        val result = service.pauseOperation(999L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resumeOperation returns success for nonexistent id`() = runBlocking<Unit> {
        val result = service.resumeOperation(999L)
        assertTrue(result.isSuccess)
    }

    // ----------------------------------------------------------------
    // 18. testConnection
    // ----------------------------------------------------------------

    @Test
    fun `testConnection when connected returns true without disconnecting`() = runBlocking<Unit> {
        service.connect()
        assertTrue(service.isOnline)

        val result = service.testConnection()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
        // Should stay connected
        assertTrue(service.isOnline)
    }

    @Test
    fun `testConnection when disconnected performs connect-disconnect cycle`() = runBlocking<Unit> {
        assertFalse(service.isOnline)

        val result = service.testConnection()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())

        // Should be disconnected after test
        assertFalse(service.isOnline)
    }

    // ----------------------------------------------------------------
    // 19. getStorageInfo
    // ----------------------------------------------------------------

    @Test
    fun `getStorageInfo returns correct id`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("smb_deep-test", info.id)
    }

    @Test
    fun `getStorageInfo returns correct name`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("deep-test", info.name)
    }

    @Test
    fun `getStorageInfo returns SMB type`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals(StorageType.SMB, info.type)
    }

    @Test
    fun `getStorageInfo location includes host share and path`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertEquals("smb://10.0.0.1/data/", info.location)
    }

    @Test
    fun `getStorageInfo isOnline reflects connection state`() = runBlocking<Unit> {
        assertFalse(service.getStorageInfo().isOnline)
        service.connect()
        assertTrue(service.getStorageInfo().isOnline)
        service.disconnect()
        assertFalse(service.getStorageInfo().isOnline)
    }

    @Test
    fun `getStorageInfo lastSync is not null`() = runBlocking<Unit> {
        val info = service.getStorageInfo()
        assertNotNull(info.lastSync)
    }

    // ----------------------------------------------------------------
    // Additional edge-case and integration tests
    // ----------------------------------------------------------------

    @Test
    fun `upload creates parent folders in file tree`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/f.txt", "/p1/p2/p3/file.txt").toList()

        assertTrue(service.exists("/p1").getOrThrow())
        assertTrue(service.exists("/p1/p2").getOrThrow())
        assertTrue(service.exists("/p1/p2/p3").getOrThrow())

        val p1 = service.getFileInfo("/p1").getOrThrow()
        assertTrue(p1.isFolder)
    }

    @Test
    fun `connect then disconnect then upload fails`() = runBlocking<Unit> {
        service.connect()
        service.disconnect()

        val ops = service.uploadFile("/local/f.txt", "/fail.txt").toList()
        assertEquals(1, ops.size)
        assertEquals(NetworkOperation.Status.FAILED, ops[0].status)
    }

    @Test
    fun `multiple uploads to same path overwrites in tree`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/v1.txt", "/overwrite.txt").toList()
        service.uploadFile("/local/v2.txt", "/overwrite.txt").toList()

        assertTrue(service.exists("/overwrite.txt").getOrThrow())
    }

    @Test
    fun `getParentPath handles trailing slash`() {
        assertEquals("/", service.getParentPath("/folder/"))
    }

    @Test
    fun `getParentPath for deep path returns parent`() {
        assertEquals("/a/b", service.getParentPath("/a/b/c"))
    }

    @Test
    fun `getParentPath for root returns null`() {
        assertNull(service.getParentPath("/"))
    }

    @Test
    fun `getParentPath for blank returns null`() {
        assertNull(service.getParentPath(""))
    }

    @Test
    fun `validatePath succeeds for non-blank`() {
        assertTrue(service.validatePath("/valid").isSuccess)
    }

    @Test
    fun `validatePath fails for blank`() {
        assertTrue(service.validatePath("").isFailure)
    }

    @Test
    fun `validatePath fails for whitespace`() {
        assertTrue(service.validatePath("   ").isFailure)
    }

    @Test
    fun `rootPath comes from config path`() {
        assertEquals("/", service.rootPath)
    }

    @Test
    fun `isOnline is false initially`() {
        assertFalse(service.isOnline)
    }

    @Test
    fun `connect sets isOnline to true`() = runBlocking<Unit> {
        service.connect()
        assertTrue(service.isOnline)
    }

    @Test
    fun `disconnect sets isOnline to false`() = runBlocking<Unit> {
        service.connect()
        service.disconnect()
        assertFalse(service.isOnline)
    }

    @Test
    fun `copy then delete source preserves destination`() = runBlocking<Unit> {
        service.connect()
        service.uploadFile("/local/s.txt", "/s.txt").toList()
        service.copyFile("/s.txt", "/d.txt")
        service.deleteFile("/s.txt")

        assertFalse(service.exists("/s.txt").getOrThrow())
        assertTrue(service.exists("/d.txt").getOrThrow())
    }
}
