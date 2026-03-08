/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.ftp

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Desktop (JVM) tests for [FtpProtocolClient] and [FtpService].
 *
 * Since we cannot connect to real FTP servers in unit tests, these tests focus on:
 * - [FtpEntry] data class behavior (equality, copy, hashCode, toString)
 * - [FtpProtocolClient] initial state and failure paths
 * - [FtpService] validation logic that returns early without touching the socket
 * - [FtpService] utility methods (getParentPath, validatePath, getStorageInfo)
 * - [FtpService] operations when not connected (should fail gracefully)
 * - [FtpService] cache and sync operations
 */
class FtpProtocolClientTest {

    // ---- Helper: create default FtpConfig ----

    private fun createConfig(
        name: String = "test-ftp",
        host: String = "ftp.example.com",
        port: Int = 21,
        username: String = "user",
        password: String = "pass",
        rootPath: String = "/"
    ) = StorageConfig.FtpConfig(
        name = name,
        host = host,
        port = port,
        username = username,
        password = password,
        rootPath = rootPath
    )

    // ================================================================
    // FtpEntry data class tests
    // ================================================================

    @Test
    fun `FtpEntry equality - identical entries are equal`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = FtpEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = ts, permissions = "rw-r--r--")
        val b = FtpEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = ts, permissions = "rw-r--r--")
        assertEquals(a, b)
    }

    @Test
    fun `FtpEntry equality - different name is not equal`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = FtpEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = ts)
        val b = FtpEntry(name = "other.txt", size = 1024, isDirectory = false, lastModified = ts)
        assertNotEquals(a, b)
    }

    @Test
    fun `FtpEntry equality - different size is not equal`() {
        val a = FtpEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = null)
        val b = FtpEntry(name = "file.txt", size = 2048, isDirectory = false, lastModified = null)
        assertNotEquals(a, b)
    }

    @Test
    fun `FtpEntry equality - different isDirectory is not equal`() {
        val a = FtpEntry(name = "docs", size = 0, isDirectory = false, lastModified = null)
        val b = FtpEntry(name = "docs", size = 0, isDirectory = true, lastModified = null)
        assertNotEquals(a, b)
    }

    @Test
    fun `FtpEntry copy produces independent copy`() {
        val original = FtpEntry(name = "readme.md", size = 512, isDirectory = false, lastModified = null, permissions = "rwxr-xr-x")
        val copied = original.copy(name = "README.md", size = 1024)
        assertEquals("README.md", copied.name)
        assertEquals(1024, copied.size)
        // Original unchanged
        assertEquals("readme.md", original.name)
        assertEquals(512, original.size)
        // Non-copied fields preserved
        assertEquals(false, copied.isDirectory)
        assertEquals("rwxr-xr-x", copied.permissions)
    }

    @Test
    fun `FtpEntry hashCode - equal entries have same hashCode`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = FtpEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = ts, permissions = "rw-")
        val b = FtpEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = ts, permissions = "rw-")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `FtpEntry toString contains field values`() {
        val entry = FtpEntry(name = "test.txt", size = 42, isDirectory = false, lastModified = null, permissions = "rw-")
        val str = entry.toString()
        assertTrue(str.contains("test.txt"), "toString should contain name")
        assertTrue(str.contains("42"), "toString should contain size")
        assertTrue(str.contains("false"), "toString should contain isDirectory")
        assertTrue(str.contains("rw-"), "toString should contain permissions")
    }

    @Test
    fun `FtpEntry with null lastModified`() {
        val entry = FtpEntry(name = "no-date.txt", size = 0, isDirectory = false, lastModified = null)
        assertNull(entry.lastModified)
        assertEquals("no-date.txt", entry.name)
    }

    @Test
    fun `FtpEntry with null permissions`() {
        val entry = FtpEntry(name = "no-perms.txt", size = 256, isDirectory = false, lastModified = null, permissions = null)
        assertNull(entry.permissions)
    }

    @Test
    fun `FtpEntry with all fields null-able set to null`() {
        val entry = FtpEntry(name = "bare.txt", size = 0, isDirectory = false, lastModified = null, permissions = null)
        assertNull(entry.lastModified)
        assertNull(entry.permissions)
        assertEquals("bare.txt", entry.name)
        assertEquals(0L, entry.size)
        assertFalse(entry.isDirectory)
    }

    @Test
    fun `FtpEntry directory entry`() {
        val entry = FtpEntry(name = "subdir", size = 4096, isDirectory = true, lastModified = null, permissions = "drwxr-xr-x")
        assertTrue(entry.isDirectory)
        assertEquals("subdir", entry.name)
        assertEquals(4096L, entry.size)
    }

    // ================================================================
    // FtpProtocolClient initial state tests
    // ================================================================

    @Test
    fun `FtpProtocolClient initial state - isConnected is false`() {
        val client = FtpProtocolClient()
        assertFalse(client.isConnected)
    }

    @Test
    fun `FtpProtocolClient connect to localhost port 0 fails`() = runBlocking {
        val client = FtpProtocolClient()
        val result = client.connect("localhost", 0)
        assertTrue(result.isFailure, "Connecting to port 0 should fail")
        assertFalse(client.isConnected)
    }

    @Test
    fun `FtpProtocolClient connect to invalid host fails`() = runBlocking {
        val client = FtpProtocolClient()
        val result = client.connect("this.host.does.not.exist.invalid", 21)
        assertTrue(result.isFailure, "Connecting to invalid host should fail")
        assertFalse(client.isConnected)
    }

    @Test
    fun `FtpProtocolClient disconnect when not connected succeeds`() = runBlocking {
        val client = FtpProtocolClient()
        val result = client.disconnect()
        assertTrue(result.isSuccess, "Disconnecting when not connected should succeed gracefully")
        assertFalse(client.isConnected)
    }

    // ================================================================
    // FtpService with invalid configs
    // ================================================================

    @Test
    fun `FtpService connect with blank host returns failure`() = runBlocking {
        val config = createConfig(host = "")
        val service = FtpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.Failed)
    }

    @Test
    fun `FtpService connect with blank host containing spaces returns failure`() = runBlocking {
        val config = createConfig(host = "   ")
        val service = FtpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun `FtpService connect with port 0 returns failure`() = runBlocking {
        val config = createConfig(port = 0)
        val service = FtpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.Failed)
    }

    @Test
    fun `FtpService connect with negative port returns failure`() = runBlocking {
        val config = createConfig(port = -1)
        val service = FtpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun `FtpService connect with port exceeding 65535 returns failure`() = runBlocking {
        val config = createConfig(port = 99999)
        val service = FtpService(config)
        val result = service.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun `FtpService testConnection with blank host returns failure`() = runBlocking {
        val config = createConfig(host = "")
        val service = FtpService(config)
        val result = service.testConnection()
        assertTrue(result.isFailure)
    }

    @Test
    fun `FtpService testConnection with port 0 returns failure`() = runBlocking {
        val config = createConfig(port = 0)
        val service = FtpService(config)
        val result = service.testConnection()
        assertTrue(result.isFailure)
    }

    @Test
    fun `FtpService testConnection with negative port returns failure`() = runBlocking {
        val config = createConfig(port = -1)
        val service = FtpService(config)
        val result = service.testConnection()
        assertTrue(result.isFailure)
    }

    // ================================================================
    // FtpService storage info and utility methods
    // ================================================================

    @Test
    fun `FtpService getStorageInfo returns correct details`() = runBlocking {
        val config = createConfig(name = "my-ftp", host = "ftp.example.com", port = 2121, rootPath = "/data")
        val service = FtpService(config)
        val info = service.getStorageInfo()

        assertEquals("ftp_my-ftp", info.id)
        assertEquals("my-ftp", info.name)
        assertEquals(StorageType.FTP, info.type)
        assertTrue(info.location.contains("ftp.example.com"))
        assertTrue(info.location.contains("2121"))
        assertTrue(info.location.contains("/data"))
        assertFalse(info.isOnline)
    }

    @Test
    fun `FtpService isOnline initially false`() {
        val service = FtpService(createConfig())
        assertFalse(service.isOnline)
    }

    @Test
    fun `FtpService rootPath is slash`() {
        val service = FtpService(createConfig())
        assertEquals("/", service.rootPath)
    }

    @Test
    fun `FtpService getParentPath of root is null`() {
        val service = FtpService(createConfig())
        assertNull(service.getParentPath("/"))
    }

    @Test
    fun `FtpService getParentPath of blank is null`() {
        val service = FtpService(createConfig())
        assertNull(service.getParentPath(""))
    }

    @Test
    fun `FtpService getParentPath of top-level file is root`() {
        val service = FtpService(createConfig())
        assertEquals("/", service.getParentPath("/file.txt"))
    }

    @Test
    fun `FtpService getParentPath of nested path`() {
        val service = FtpService(createConfig())
        assertEquals("/home/user", service.getParentPath("/home/user/docs"))
    }

    @Test
    fun `FtpService getParentPath of path with trailing slash`() {
        val service = FtpService(createConfig())
        assertEquals("/home", service.getParentPath("/home/user/"))
    }

    @Test
    fun `FtpService getParentPath of deeply nested path`() {
        val service = FtpService(createConfig())
        assertEquals("/a/b/c", service.getParentPath("/a/b/c/d"))
    }

    @Test
    fun `FtpService validatePath valid path succeeds`() {
        val service = FtpService(createConfig())
        val result = service.validatePath("/some/path")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `FtpService validatePath blank path fails`() {
        val service = FtpService(createConfig())
        val result = service.validatePath("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `FtpService validatePath whitespace-only path fails`() {
        val service = FtpService(createConfig())
        val result = service.validatePath("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun `FtpService validatePath root path succeeds`() {
        val service = FtpService(createConfig())
        val result = service.validatePath("/")
        assertTrue(result.isSuccess)
    }

    // ================================================================
    // FtpService operations when not connected
    // ================================================================

    @Test
    fun `FtpService listFiles when not connected emits failure`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.listFiles("/").first()
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.NotConnected)
    }

    @Test
    fun `FtpService downloadFile when not connected emits failed operation`() = runBlocking {
        val service = FtpService(createConfig())
        val operation = service.downloadFile("/remote/file.txt", "/local/file.txt").first()
        assertEquals(NetworkOperation.Status.FAILED, operation.status)
        assertTrue(operation.error?.contains("not connected") == true)
    }

    @Test
    fun `FtpService uploadFile when not connected emits failed operation`() = runBlocking {
        val service = FtpService(createConfig())
        val operation = service.uploadFile("/local/file.txt", "/remote/file.txt").first()
        assertEquals(NetworkOperation.Status.FAILED, operation.status)
        assertTrue(operation.error?.contains("not connected") == true)
    }

    @Test
    fun `FtpService deleteFile when not connected returns failure`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.deleteFile("/remote/file.txt")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.NotConnected)
    }

    @Test
    fun `FtpService createFolder when not connected returns failure`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.createFolder("/remote/newdir")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.NotConnected)
    }

    @Test
    fun `FtpService renameFile when not connected returns failure`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.renameFile("/remote/old.txt", "new.txt")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.NotConnected)
    }

    @Test
    fun `FtpService moveFile when not connected returns failure`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.moveFile("/remote/src.txt", "/remote/dest.txt")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.NotConnected)
    }

    @Test
    fun `FtpService copyFile always fails - not supported by FTP`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.copyFile("/remote/src.txt", "/remote/dest.txt")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.FileOperationException.CopyFailed)
    }

    @Test
    fun `FtpService getFileInfo when not connected returns failure`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.getFileInfo("/remote/file.txt")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.NotConnected)
    }

    @Test
    fun `FtpService searchFiles returns failure - not supported by FTP`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.searchFiles("query", "/").first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `FtpService exists when not connected returns false`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.exists("/remote/file.txt")
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `FtpService getQuotaInfo returns zero quota - not supported by FTP`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess)
        val quota = result.getOrThrow()
        assertEquals(0L, quota.totalSpace)
        assertEquals(0L, quota.usedSpace)
        assertEquals(0L, quota.availableSpace)
        assertEquals(0.0, quota.usagePercentage)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
        assertTrue(quota.metadata.containsKey("provider"))
        assertEquals("FTP", quota.metadata["provider"])
    }

    @Test
    fun `FtpService disconnect when not connected succeeds`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    // ================================================================
    // FtpService cache operations
    // ================================================================

    @Test
    fun `FtpService addToCache and getCacheEntries`() = runBlocking {
        val service = FtpService(createConfig())
        val addResult = service.addToCache("/docs/file.txt", priority = 5)
        assertTrue(addResult.isSuccess)

        val entries = service.getCacheEntries("/docs").first()
        assertTrue(entries.isNotEmpty())
        val entry = entries.first()
        assertEquals("/docs/file.txt", entry.remotePath)
    }

    @Test
    fun `FtpService removeFromCache removes entry`() = runBlocking {
        val service = FtpService(createConfig())
        service.addToCache("/docs/file.txt", priority = 1)
        service.removeFromCache("/docs/file.txt")
        val entries = service.getCacheEntries("/docs").first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `FtpService clearCache empties all entries`() = runBlocking {
        val service = FtpService(createConfig())
        service.addToCache("/a.txt", priority = 1)
        service.addToCache("/b.txt", priority = 2)
        service.addToCache("/c.txt", priority = 3)

        val beforeClear = service.getCacheEntries(null).first()
        assertEquals(3, beforeClear.size)

        service.clearCache()
        val afterClear = service.getCacheEntries(null).first()
        assertTrue(afterClear.isEmpty())
    }

    @Test
    fun `FtpService getCacheEntries with null path returns all entries`() = runBlocking {
        val service = FtpService(createConfig())
        service.addToCache("/alpha/one.txt", priority = 1)
        service.addToCache("/beta/two.txt", priority = 2)
        val all = service.getCacheEntries(null).first()
        assertEquals(2, all.size)
    }

    @Test
    fun `FtpService getCacheEntries with path prefix filters correctly`() = runBlocking {
        val service = FtpService(createConfig())
        service.addToCache("/alpha/one.txt", priority = 1)
        service.addToCache("/beta/two.txt", priority = 2)
        val alphaOnly = service.getCacheEntries("/alpha").first()
        assertEquals(1, alphaOnly.size)
        assertEquals("/alpha/one.txt", alphaOnly.first().remotePath)
    }

    // ================================================================
    // FtpService sync operations
    // ================================================================

    @Test
    fun `FtpService syncFile when not connected still completes sync operation`() = runBlocking {
        val service = FtpService(createConfig())
        val operation = service.syncFile("/remote/file.txt", forceSync = false).first()
        assertEquals(NetworkOperation.Status.COMPLETED, operation.status)
        assertEquals(NetworkOperation.Type.SYNC, operation.type)
        assertEquals(1.0, operation.progress)
    }

    @Test
    fun `FtpService syncAll when not connected still completes`() = runBlocking {
        val service = FtpService(createConfig())
        val operation = service.syncAll(forceSync = false).first()
        assertEquals(NetworkOperation.Status.COMPLETED, operation.status)
        assertEquals(NetworkOperation.Type.SYNC, operation.type)
    }

    @Test
    fun `FtpService getSyncStatus initially empty`() = runBlocking {
        val service = FtpService(createConfig())
        val statuses = service.getSyncStatus(null).first()
        assertTrue(statuses.isEmpty())
    }

    @Test
    fun `FtpService getSyncStatus after syncFile has entry`() = runBlocking {
        val service = FtpService(createConfig())
        service.syncFile("/remote/test.txt", forceSync = false).first()
        val statuses = service.getSyncStatus(null).first()
        assertTrue(statuses.isNotEmpty())
    }

    // ================================================================
    // FtpService operation management
    // ================================================================

    @Test
    fun `FtpService getActiveOperations initially empty`() = runBlocking {
        val service = FtpService(createConfig())
        val ops = service.getActiveOperations().first()
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `FtpService cancelOperation for nonexistent operation succeeds`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.cancelOperation(999L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `FtpService pauseOperation for nonexistent operation succeeds`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.pauseOperation(999L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `FtpService resumeOperation for nonexistent operation succeeds`() = runBlocking {
        val service = FtpService(createConfig())
        val result = service.resumeOperation(999L)
        assertTrue(result.isSuccess)
    }

    // ================================================================
    // FtpService getRecentChanges when not connected
    // ================================================================

    @Test
    fun `FtpService getRecentChanges when not connected emits empty list`() = runBlocking {
        val service = FtpService(createConfig())
        val recent = service.getRecentChanges(
            since = Instant.fromEpochMilliseconds(0L),
            path = null
        ).first()
        assertTrue(recent.isEmpty())
    }

    // ================================================================
    // FtpService with custom rootPath
    // ================================================================

    @Test
    fun `FtpService getStorageInfo with custom rootPath reflects in location`() = runBlocking {
        val config = createConfig(rootPath = "/uploads/docs")
        val service = FtpService(config)
        val info = service.getStorageInfo()
        assertTrue(info.location.contains("/uploads/docs"))
    }

    @Test
    fun `FtpService getStorageInfo with blank rootPath defaults to slash`() = runBlocking {
        val config = createConfig(rootPath = "")
        val service = FtpService(config)
        val info = service.getStorageInfo()
        assertTrue(info.location.contains("ftp://"))
    }

    // ================================================================
    // FtpEntry edge cases
    // ================================================================

    @Test
    fun `FtpEntry with empty name`() {
        val entry = FtpEntry(name = "", size = 0, isDirectory = false, lastModified = null)
        assertEquals("", entry.name)
    }

    @Test
    fun `FtpEntry with large size`() {
        val entry = FtpEntry(name = "large.bin", size = Long.MAX_VALUE, isDirectory = false, lastModified = null)
        assertEquals(Long.MAX_VALUE, entry.size)
    }

    @Test
    fun `FtpEntry with zero size`() {
        val entry = FtpEntry(name = "empty.txt", size = 0, isDirectory = false, lastModified = null)
        assertEquals(0L, entry.size)
    }

    @Test
    fun `FtpEntry destructuring`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val entry = FtpEntry(name = "test.txt", size = 100, isDirectory = false, lastModified = ts, permissions = "rw-r--r--")
        val (name, size, isDir, modified, perms) = entry
        assertEquals("test.txt", name)
        assertEquals(100L, size)
        assertFalse(isDir)
        assertEquals(ts, modified)
        assertEquals("rw-r--r--", perms)
    }

    @Test
    fun `FtpEntry in set uses structural equality`() {
        val a = FtpEntry(name = "a.txt", size = 1, isDirectory = false, lastModified = null)
        val b = FtpEntry(name = "a.txt", size = 1, isDirectory = false, lastModified = null)
        val set = setOf(a, b)
        assertEquals(1, set.size)
    }

    @Test
    fun `FtpEntry in map as key uses structural equality`() {
        val key1 = FtpEntry(name = "k.txt", size = 10, isDirectory = false, lastModified = null)
        val key2 = FtpEntry(name = "k.txt", size = 10, isDirectory = false, lastModified = null)
        val map = mapOf(key1 to "first", key2 to "second")
        assertEquals(1, map.size)
        assertEquals("second", map[key1])
    }

    // ================================================================
    // FtpService copyFile edge cases
    // ================================================================

    @Test
    fun `FtpService copyFile even when connected would fail`() = runBlocking {
        // copyFile is not supported by FTP regardless of connection state
        val service = FtpService(createConfig())
        val result = service.copyFile("/a", "/b")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is NetworkStorageException.FileOperationException.CopyFailed)
        assertTrue(ex.message?.contains("does not support copy") == true)
    }

    // ================================================================
    // FtpService config property access
    // ================================================================

    @Test
    fun `FtpService config is accessible`() {
        val config = createConfig(name = "my-server", host = "192.168.1.100", port = 2121)
        val service = FtpService(config)
        assertEquals("my-server", service.config.name)
        assertEquals("192.168.1.100", service.config.host)
        assertEquals(2121, service.config.port)
    }

    // ================================================================
    // FtpProtocolClient connect-disconnect lifecycle
    // ================================================================

    @Test
    fun `FtpProtocolClient double disconnect is safe`() = runBlocking {
        val client = FtpProtocolClient()
        val r1 = client.disconnect()
        assertTrue(r1.isSuccess)
        val r2 = client.disconnect()
        assertTrue(r2.isSuccess)
        assertFalse(client.isConnected)
    }

    @Test
    fun `FtpProtocolClient operations fail when not connected`() = runBlocking {
        val client = FtpProtocolClient()

        val loginResult = client.login("user", "pass")
        assertTrue(loginResult.isFailure)

        val listResult = client.list("/")
        assertTrue(listResult.isFailure)

        val retrieveResult = client.retrieve("/file.txt")
        assertTrue(retrieveResult.isFailure)

        val storeResult = client.store("/file.txt", ByteArray(0))
        assertTrue(storeResult.isFailure)

        val deleteResult = client.delete("/file.txt")
        assertTrue(deleteResult.isFailure)

        val mkdirResult = client.mkdir("/newdir")
        assertTrue(mkdirResult.isFailure)

        val rmdirResult = client.rmdir("/olddir")
        assertTrue(rmdirResult.isFailure)

        val renameResult = client.rename("/old", "/new")
        assertTrue(renameResult.isFailure)

        val pwdResult = client.pwd()
        assertTrue(pwdResult.isFailure)

        val cwdResult = client.cwd("/somewhere")
        assertTrue(cwdResult.isFailure)

        val sizeResult = client.size("/file.txt")
        assertTrue(sizeResult.isFailure)

        val mdtmResult = client.mdtm("/file.txt")
        assertTrue(mdtmResult.isFailure)

        val typeResult = client.setTransferType(true)
        assertTrue(typeResult.isFailure)

        val pasvResult = client.pasv()
        assertTrue(pasvResult.isFailure)
    }

    @Test
    fun `FtpProtocolClient still not connected after failed connect attempt`() = runBlocking {
        val client = FtpProtocolClient()
        client.connect("localhost", 0)
        assertFalse(client.isConnected)
        // Should be safe to disconnect even after failed connect
        val result = client.disconnect()
        assertTrue(result.isSuccess)
    }
}
