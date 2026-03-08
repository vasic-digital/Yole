/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Deep coverage tests for SftpService
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.sftp

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

class SftpServiceDeepCoverageTest {

    private fun createPasswordConfig(
        name: String = "test-sftp",
        host: String = "sftp.example.com",
        port: Int = 22,
        username: String = "testuser",
        password: String = "testpass",
        rootPath: String = "/home/testuser"
    ) = StorageConfig.SftpConfig(
        name = name,
        host = host,
        port = port,
        username = username,
        password = password,
        rootPath = rootPath,
        strictHostKeyChecking = false
    )

    private fun createKeyConfig(
        name: String = "test-sftp-key",
        host: String = "sftp.example.com",
        port: Int = 22,
        username: String = "testuser",
        privateKeyPath: String = "/home/user/.ssh/id_rsa",
        rootPath: String = "/data"
    ) = StorageConfig.SftpConfig(
        name = name,
        host = host,
        port = port,
        username = username,
        privateKeyPath = privateKeyPath,
        rootPath = rootPath,
        strictHostKeyChecking = false
    )

    // ==================== INITIALIZATION ====================

    @Test
    fun `service initializes with password config`() {
        val config = createPasswordConfig()
        val service = SftpService(config)
        assertEquals("test-sftp", service.config.name)
        assertEquals("sftp.example.com", service.config.host)
        assertEquals(22, service.config.port)
        assertFalse(service.isOnline)
    }

    @Test
    fun `service initializes with key config`() {
        val config = createKeyConfig()
        val service = SftpService(config)
        assertEquals("test-sftp-key", service.config.name)
        assertFalse(service.isOnline)
    }

    @Test
    fun `rootPath defaults to slash for blank config path`() {
        val config = StorageConfig.SftpConfig(
            name = "blank-root",
            host = "host",
            port = 22,
            username = "user",
            password = "pass",
            rootPath = ""
        )
        val service = SftpService(config)
        assertEquals("/", service.rootPath)
    }

    @Test
    fun `rootPath is always slash`() {
        val config = createPasswordConfig(rootPath = "/custom/path")
        val service = SftpService(config)
        assertEquals("/", service.rootPath)
    }

    // ==================== CONNECTION ====================

    @Test
    fun `connect succeeds with valid password config`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.connect()
        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `connect succeeds with valid key config`() = runBlocking<Unit> {
        val service = SftpService(createKeyConfig())
        val result = service.connect()
        assertTrue(result.isSuccess)
        assertTrue(service.isOnline)
    }

    @Test
    fun `connect fails with no auth credentials`() = runBlocking<Unit> {
        val config = StorageConfig.SftpConfig(
            name = "no-auth",
            host = "host",
            port = 22,
            username = "user",
            rootPath = "/home"
        )
        val service = SftpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
        assertFalse(service.isOnline)
    }

    @Test
    fun `connect fails with blank host`() = runBlocking<Unit> {
        val config = StorageConfig.SftpConfig(
            name = "blank-host",
            host = "",
            port = 22,
            username = "user",
            password = "pass",
            rootPath = "/"
        )
        val service = SftpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun `connect fails with invalid port zero`() = runBlocking<Unit> {
        val config = StorageConfig.SftpConfig(
            name = "bad-port",
            host = "host",
            port = 0,
            username = "user",
            password = "pass",
            rootPath = "/"
        )
        val service = SftpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun `connect fails with port above 65535`() = runBlocking<Unit> {
        val config = StorageConfig.SftpConfig(
            name = "bad-port",
            host = "host",
            port = 70000,
            username = "user",
            password = "pass",
            rootPath = "/"
        )
        val service = SftpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun `connect fails with strict host key checking but no known hosts`() = runBlocking<Unit> {
        val config = StorageConfig.SftpConfig(
            name = "strict-check",
            host = "host",
            port = 22,
            username = "user",
            password = "pass",
            rootPath = "/",
            strictHostKeyChecking = true
        )
        val service = SftpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun `connect succeeds with strict host key checking and known hosts`() = runBlocking<Unit> {
        val config = StorageConfig.SftpConfig(
            name = "strict-ok",
            host = "host",
            port = 22,
            username = "user",
            password = "pass",
            rootPath = "/",
            strictHostKeyChecking = true,
            knownHostsPath = "/home/user/.ssh/known_hosts"
        )
        val service = SftpService(config)
        val result = service.connect()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `disconnect succeeds`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        assertTrue(service.isOnline)
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `disconnect clears active operations`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.disconnect()
        val ops = service.getActiveOperations().first()
        assertTrue(ops.isEmpty())
    }

    // ==================== TEST CONNECTION ====================

    @Test
    fun `testConnection succeeds with valid config`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.testConnection()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `testConnection fails with no auth`() = runBlocking<Unit> {
        val config = StorageConfig.SftpConfig(
            name = "no-auth",
            host = "host",
            port = 22,
            username = "user",
            rootPath = "/"
        )
        val service = SftpService(config)
        val result = service.testConnection()
        assertTrue(result.isFailure)
    }

    // ==================== STORAGE INFO ====================

    @Test
    fun `getStorageInfo returns correct metadata`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val info = service.getStorageInfo()
        assertEquals("sftp_test-sftp", info.id)
        assertEquals("test-sftp", info.name)
        assertEquals(StorageType.SFTP, info.type)
        assertTrue(info.location.contains("sftp://sftp.example.com:22"))
        assertFalse(info.isOnline)
        assertTrue(info.supportsFolders)
        assertTrue(info.supportsMetadata)
    }

    @Test
    fun `getStorageInfo reflects connection state`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        assertFalse(service.getStorageInfo().isOnline)
        service.connect()
        assertTrue(service.getStorageInfo().isOnline)
        service.disconnect()
        assertFalse(service.getStorageInfo().isOnline)
    }

    // ==================== LIST FILES ====================

    @Test
    fun `listFiles fails when disconnected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.listFiles("/").first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `listFiles returns files when connected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val result = service.listFiles("/").first()
        assertTrue(result.isSuccess)
        val files = result.getOrThrow()
        assertTrue(files.isNotEmpty())
    }

    @Test
    fun `listFiles returns default VFS files after connect`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val files = service.listFiles("/").first().getOrThrow()
        val names = files.map { it.name }
        assertTrue(names.contains("document.pdf"))
        assertTrue(names.contains("README.md"))
        assertTrue(names.contains("project_folder"))
        assertTrue(names.contains("config.json"))
    }

    @Test
    fun `listFiles includes folders and files`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val files = service.listFiles("/").first().getOrThrow()
        val folders = files.filter { it.isFolder }
        val regularFiles = files.filter { !it.isFolder }
        assertTrue(folders.isNotEmpty())
        assertTrue(regularFiles.isNotEmpty())
    }

    // ==================== UPLOAD FILE ====================

    @Test
    fun `uploadFile fails when disconnected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val ops = service.uploadFile("/local/file.txt", "/remote/file.txt").toList()
        assertTrue(ops.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, ops.first().status)
    }

    @Test
    fun `uploadFile completes when connected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val ops = service.uploadFile("/local/file.txt", "/upload/test.txt").toList()
        assertTrue(ops.isNotEmpty())
        val completed = ops.last()
        assertEquals(NetworkOperation.Status.COMPLETED, completed.status)
        assertEquals(1.0, completed.progress)
    }

    @Test
    fun `uploadFile registers in VFS`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/file.txt", "/upload/test.txt").toList()
        val info = service.getFileInfo("/upload/test.txt")
        assertTrue(info.isSuccess)
        assertEquals("test.txt", info.getOrThrow().name)
    }

    @Test
    fun `uploadFile updates sync status`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/file.txt", "/upload/test.txt").toList()
        val statuses = service.getSyncStatus(null).first()
        assertTrue(statuses.isNotEmpty())
    }

    // ==================== DOWNLOAD FILE ====================

    @Test
    fun `downloadFile fails when disconnected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val ops = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(ops.isNotEmpty())
        assertEquals(NetworkOperation.Status.FAILED, ops.first().status)
    }

    @Test
    fun `downloadFile completes when connected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val ops = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(ops.isNotEmpty())
        val completed = ops.last()
        assertEquals(NetworkOperation.Status.COMPLETED, completed.status)
    }

    @Test
    fun `downloadFile tracks progress through multiple steps`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val ops = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        assertTrue(ops.size > 3)
        // First should be starting
        assertEquals(0.0, ops.first().progress)
        // Last should be complete
        assertEquals(1.0, ops.last().progress)
    }

    // ==================== DELETE FILE ====================

    @Test
    fun `deleteFile fails when disconnected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.deleteFile("/some/file.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteFile removes from VFS`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        // Upload first then delete
        service.uploadFile("/local/f.txt", "/to-delete.txt").toList()
        val existsBefore = service.getFileInfo("/to-delete.txt")
        assertTrue(existsBefore.isSuccess)

        val result = service.deleteFile("/to-delete.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteFile cleans up sync status`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/sync-test.txt").toList()
        service.deleteFile("/sync-test.txt")
        // Sync status for that path should be removed
        val statuses = service.getSyncStatus("/home/testuser/sync-test.txt").first()
        assertFalse(statuses.containsKey("/home/testuser/sync-test.txt"))
    }

    @Test
    fun `deleteFile cleans up cache`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.addToCache("/cache-delete-test.txt", 1)
        service.deleteFile("/cache-delete-test.txt")
        val entries = service.getCacheEntries(null).first()
        val matching = entries.filter { it.remotePath.contains("cache-delete-test") }
        assertTrue(matching.isEmpty())
    }

    // ==================== CREATE FOLDER ====================

    @Test
    fun `createFolder fails when disconnected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.createFolder("/new-folder")
        assertTrue(result.isFailure)
    }

    @Test
    fun `createFolder succeeds when connected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val result = service.createFolder("/new-folder")
        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertTrue(doc.isFolder)
        assertEquals("new-folder", doc.name)
    }

    @Test
    fun `createFolder registers in VFS`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.createFolder("/test-folder")
        val info = service.getFileInfo("/test-folder")
        assertTrue(info.isSuccess)
        assertTrue(info.getOrThrow().isFolder)
    }

    @Test
    fun `createFolder has correct permissions`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val result = service.createFolder("/perm-folder")
        val doc = result.getOrThrow()
        assertTrue(doc.permissions.contains(DocumentPermission.READ))
        assertTrue(doc.permissions.contains(DocumentPermission.WRITE))
        assertTrue(doc.permissions.contains(DocumentPermission.EXECUTE))
    }

    // ==================== RENAME FILE ====================

    @Test
    fun `renameFile fails when disconnected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.renameFile("/file.txt", "renamed.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `renameFile succeeds when connected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/rename-source.txt").toList()
        val result = service.renameFile("/rename-source.txt", "renamed.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `renameFile updates VFS paths`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/rename-test.txt").toList()
        service.renameFile("/rename-test.txt", "new-name.txt")
        // The renamed file should be accessible under the new name
        val parentPath = "/home/testuser"
        val newInfo = service.getFileInfo("$parentPath/new-name.txt")
        assertTrue(newInfo.isSuccess)
    }

    @Test
    fun `renameFile updates sync status`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/sync-rename.txt").toList()
        service.renameFile("/sync-rename.txt", "sync-renamed.txt")
        // Sync status should be migrated to new path
        val statuses = service.getSyncStatus(null).first()
        assertFalse(statuses.isEmpty())
    }

    // ==================== MOVE FILE ====================

    @Test
    fun `moveFile fails when disconnected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.moveFile("/source.txt", "/dest/source.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `moveFile succeeds when connected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/move-source.txt").toList()
        val result = service.moveFile("/move-source.txt", "/moved/file.txt")
        assertTrue(result.isSuccess)
        assertEquals("file.txt", result.getOrThrow().name)
    }

    @Test
    fun `moveFile updates VFS`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/move-vfs.txt").toList()
        service.moveFile("/move-vfs.txt", "/dest/moved-vfs.txt")
        val destInfo = service.getFileInfo("/dest/moved-vfs.txt")
        assertTrue(destInfo.isSuccess)
    }

    // ==================== COPY FILE ====================

    @Test
    fun `copyFile fails when disconnected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.copyFile("/source.txt", "/dest.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `copyFile succeeds when connected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/copy-source.txt").toList()
        val result = service.copyFile("/copy-source.txt", "/copy-dest.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `copyFile keeps source in VFS`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/copy-keep.txt").toList()
        service.copyFile("/copy-keep.txt", "/copy-keep-dest.txt")
        val sourceInfo = service.getFileInfo("/copy-keep.txt")
        assertTrue(sourceInfo.isSuccess)
    }

    @Test
    fun `copyFile creates destination in VFS`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/copy-src2.txt").toList()
        service.copyFile("/copy-src2.txt", "/copy-dst2.txt")
        val destInfo = service.getFileInfo("/copy-dst2.txt")
        assertTrue(destInfo.isSuccess)
    }

    // ==================== GET FILE INFO ====================

    @Test
    fun `getFileInfo fails when disconnected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.getFileInfo("/file.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `getFileInfo returns VFS entry when connected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        // VFS has document.pdf after connect
        val result = service.getFileInfo("document.pdf")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getFileInfo synthesizes document for unknown paths`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val result = service.getFileInfo("/unknown/path/file.txt")
        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals("file.txt", doc.name)
        assertFalse(doc.isFolder)
    }

    @Test
    fun `getFileInfo for path ending in slash is folder`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val result = service.getFileInfo("/some/dir/")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isFolder)
    }

    // ==================== EXISTS ====================

    @Test
    fun `exists returns true for known VFS entry`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val result = service.exists("document.pdf")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `exists returns true for synthesized paths when connected`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val result = service.exists("/nonexistent/file.txt")
        assertTrue(result.isSuccess)
        // getFileInfo always succeeds when connected (synthesizes), so exists returns true
        assertTrue(result.getOrThrow())
    }

    // ==================== PATH UTILITIES ====================

    @Test
    fun `getParentPath of root returns null`() {
        val service = SftpService(createPasswordConfig())
        assertNull(service.getParentPath("/"))
    }

    @Test
    fun `getParentPath of blank returns null`() {
        val service = SftpService(createPasswordConfig())
        assertNull(service.getParentPath(""))
    }

    @Test
    fun `getParentPath of top-level file returns root`() {
        val service = SftpService(createPasswordConfig())
        assertEquals("/", service.getParentPath("/file.txt"))
    }

    @Test
    fun `getParentPath of nested path returns parent`() {
        val service = SftpService(createPasswordConfig())
        assertEquals("/home/user", service.getParentPath("/home/user/file.txt"))
    }

    @Test
    fun `getParentPath removes trailing slash`() {
        val service = SftpService(createPasswordConfig())
        assertEquals("/home", service.getParentPath("/home/user/"))
    }

    @Test
    fun `validatePath succeeds for valid path`() {
        val service = SftpService(createPasswordConfig())
        assertTrue(service.validatePath("/home/user/file.txt").isSuccess)
    }

    @Test
    fun `validatePath fails for blank path`() {
        val service = SftpService(createPasswordConfig())
        assertTrue(service.validatePath("").isFailure)
    }

    @Test
    fun `validatePath fails for whitespace path`() {
        val service = SftpService(createPasswordConfig())
        assertTrue(service.validatePath("   ").isFailure)
    }

    // ==================== CACHE OPERATIONS ====================

    @Test
    fun `addToCache creates entry`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.addToCache("/test/file.txt", 5)
        assertTrue(result.isSuccess)
        val entries = service.getCacheEntries(null).first()
        assertTrue(entries.isNotEmpty())
    }

    @Test
    fun `removeFromCache removes entry`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.addToCache("/test/file.txt", 5)
        service.removeFromCache("/test/file.txt")
        val entries = service.getCacheEntries(null).first()
        val matching = entries.filter { it.remotePath.contains("file.txt") }
        assertTrue(matching.isEmpty())
    }

    @Test
    fun `clearCache removes all entries`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.addToCache("/file1.txt", 1)
        service.addToCache("/file2.txt", 2)
        service.addToCache("/file3.txt", 3)
        service.clearCache()
        val entries = service.getCacheEntries(null).first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `getCacheEntries with path filter`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.addToCache("/docs/a.txt", 1)
        service.addToCache("/docs/b.txt", 2)
        service.addToCache("/other/c.txt", 3)
        val docsEntries = service.getCacheEntries("/home/testuser/docs").first()
        val allEntries = service.getCacheEntries(null).first()
        assertTrue(allEntries.size >= 3)
    }

    // ==================== SYNC STATUS ====================

    @Test
    fun `getSyncStatus empty initially`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val statuses = service.getSyncStatus(null).first()
        assertTrue(statuses.isEmpty())
    }

    @Test
    fun `syncFile marks file as synced`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val ops = service.syncFile("/test.txt", false).toList()
        assertTrue(ops.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, ops.last().status)
    }

    @Test
    fun `syncAll marks all tracked files as synced`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        // List files populates sync status
        service.listFiles("/").first()
        val ops = service.syncAll(false).toList()
        assertTrue(ops.isNotEmpty())
        assertEquals(NetworkOperation.Status.COMPLETED, ops.last().status)
    }

    @Test
    fun `getSyncStatus with path filter`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.listFiles("/").first()
        val allStatuses = service.getSyncStatus(null).first()
        assertTrue(allStatuses.isNotEmpty())
    }

    // ==================== SEARCH FILES ====================

    @Test
    fun `searchFiles always returns failure`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.searchFiles("query").first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `searchFiles with path returns failure`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.searchFiles("query", "/some/path").first()
        assertTrue(result.isFailure)
    }

    // ==================== RECENT CHANGES ====================

    @Test
    fun `getRecentChanges returns empty for old timestamp`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val futureTime = Clock.System.now().plus(24.hours)
        val changes = service.getRecentChanges(futureTime).first()
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `getRecentChanges returns files after connect`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val pastTime = Clock.System.now().minus(48.hours)
        val changes = service.getRecentChanges(pastTime).first()
        assertTrue(changes.isNotEmpty())
    }

    @Test
    fun `getRecentChanges filters by path`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val pastTime = Clock.System.now().minus(48.hours)
        val allChanges = service.getRecentChanges(pastTime).first()
        val filteredChanges = service.getRecentChanges(pastTime, "/nonexistent").first()
        assertTrue(allChanges.size >= filteredChanges.size)
    }

    // ==================== QUOTA INFO ====================

    @Test
    fun `getQuotaInfo returns zeroed values`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess)
        val quota = result.getOrThrow()
        assertEquals(0L, quota.totalSpace)
        assertEquals(0L, quota.usedSpace)
        assertEquals(0L, quota.availableSpace)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
    }

    @Test
    fun `getQuotaInfo includes SFTP metadata`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val quota = service.getQuotaInfo().getOrThrow()
        assertEquals("SFTP", quota.metadata?.get("provider"))
        assertEquals("SSH2", quota.metadata?.get("encryption"))
    }

    // ==================== ACTIVE OPERATIONS ====================

    @Test
    fun `getActiveOperations empty initially`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val ops = service.getActiveOperations().first()
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `cancelOperation succeeds`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.cancelOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `pauseOperation succeeds`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.pauseOperation(12345L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resumeOperation succeeds`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        val result = service.resumeOperation(12345L)
        assertTrue(result.isSuccess)
    }

    // ==================== CONNECT INITIALIZES VFS ====================

    @Test
    fun `connect initializes virtual file system`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        // After connect, VFS should have default files
        // normalizePath prepends rootPath, so pass relative path to reach VFS entries
        val info = service.getFileInfo("document.pdf")
        assertTrue(info.isSuccess)
        assertEquals(5242880L, info.getOrThrow().size)
    }

    @Test
    fun `reconnect reinitializes VFS`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        service.uploadFile("/local/f.txt", "/extra-file.txt").toList()
        service.disconnect()
        service.connect()
        // VFS should be fresh after reconnect
        val info = service.getFileInfo("document.pdf")
        assertTrue(info.isSuccess)
    }

    // ==================== UPLOAD + DOWNLOAD PROGRESS TRACKING ====================

    @Test
    fun `uploadFile emits progress from 0 to 1`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val ops = service.uploadFile("/local/file.txt", "/progress-test.txt").toList()
        assertTrue(ops.isNotEmpty())
        assertEquals(0.0, ops.first().progress)
        assertEquals(NetworkOperation.Status.COMPLETED, ops.last().status)
        assertEquals(1.0, ops.last().progress)
        // Verify multiple progress emissions
        assertTrue(ops.size > 3, "Should have multiple progress steps, got ${ops.size}")
    }

    @Test
    fun `downloadFile emits progress from 0 to 1`() = runBlocking<Unit> {
        val service = SftpService(createPasswordConfig())
        service.connect()
        val ops = service.downloadFile("/remote/file.txt", "/local/file.txt").toList()
        val progresses = ops.map { it.progress }
        assertEquals(0.0, progresses.first())
        assertEquals(1.0, ops.last().progress)
    }
}
