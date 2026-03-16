/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.smb

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlin.test.*

/**
 * Desktop (JVM) tests for [SmbProtocolClient] and [SmbService].
 *
 * Since we cannot connect to real SMB servers in unit tests, these tests focus on:
 * - [SmbEntry] data class behavior (equality, copy, hashCode, toString)
 * - [SmbFileInfo] data class behavior
 * - [SmbShareInfo] data class behavior
 * - [SmbProtocolClient] initial state and failure paths
 * - [SmbService] validation logic, utility methods, and disconnected behavior
 * - [SmbService] cache and sync operations
 */
class SmbProtocolClientTest {

    // ---- Helper: create default SmbConfig ----

    private fun createConfig(
        name: String = "test-smb",
        host: String = "smb.example.com",
        share: String = "shared",
        domain: String? = null,
        username: String = "user",
        password: String = "pass",
        path: String = "/",
        port: Int = 445
    ) = StorageConfig.SmbConfig(
        name = name,
        host = host,
        share = share,
        domain = domain,
        username = username,
        password = password,
        path = path,
        port = port
    )

    // ================================================================
    // SmbEntry data class tests
    // ================================================================

    @Test
    fun `SmbEntry equality - identical entries are equal`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = SmbEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = ts)
        val b = SmbEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = ts)
        assertEquals(a, b)
    }

    @Test
    fun `SmbEntry equality - different name is not equal`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = SmbEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = ts)
        val b = SmbEntry(name = "other.txt", size = 1024, isDirectory = false, lastModified = ts)
        assertNotEquals(a, b)
    }

    @Test
    fun `SmbEntry equality - different size is not equal`() {
        val a = SmbEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = null)
        val b = SmbEntry(name = "file.txt", size = 2048, isDirectory = false, lastModified = null)
        assertNotEquals(a, b)
    }

    @Test
    fun `SmbEntry equality - different isDirectory is not equal`() {
        val a = SmbEntry(name = "docs", size = 0, isDirectory = false, lastModified = null)
        val b = SmbEntry(name = "docs", size = 0, isDirectory = true, lastModified = null)
        assertNotEquals(a, b)
    }

    @Test
    fun `SmbEntry copy produces independent copy`() {
        val original = SmbEntry(name = "readme.md", size = 512, isDirectory = false, lastModified = null)
        val copied = original.copy(name = "README.md", size = 1024)
        assertEquals("README.md", copied.name)
        assertEquals(1024, copied.size)
        // Original unchanged
        assertEquals("readme.md", original.name)
        assertEquals(512, original.size)
        assertFalse(copied.isDirectory)
    }

    @Test
    fun `SmbEntry hashCode - equal entries have same hashCode`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = SmbEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = ts)
        val b = SmbEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = ts)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `SmbEntry toString contains field values`() {
        val entry = SmbEntry(name = "test.txt", size = 42, isDirectory = false, lastModified = null)
        val str = entry.toString()
        assertTrue(str.contains("test.txt"), "toString should contain name")
        assertTrue(str.contains("42"), "toString should contain size")
        assertTrue(str.contains("false"), "toString should contain isDirectory")
    }

    @Test
    fun `SmbEntry with null lastModified and lastAccessed`() {
        val entry = SmbEntry(name = "no-date.txt", size = 0, isDirectory = false, lastModified = null, lastAccessed = null)
        assertNull(entry.lastModified)
        assertNull(entry.lastAccessed)
    }

    @Test
    fun `SmbEntry with all time fields populated`() {
        val modified = Instant.fromEpochMilliseconds(1700000000000L)
        val accessed = Instant.fromEpochMilliseconds(1700001000000L)
        val created = Instant.fromEpochMilliseconds(1699000000000L)
        val entry = SmbEntry(
            name = "dated.txt",
            size = 256,
            isDirectory = false,
            lastModified = modified,
            lastAccessed = accessed,
            creationTime = created
        )
        assertEquals(modified, entry.lastModified)
        assertEquals(accessed, entry.lastAccessed)
        assertEquals(created, entry.creationTime)
    }

    @Test
    fun `SmbEntry directory entry`() {
        val entry = SmbEntry(name = "subdir", size = 0, isDirectory = true, lastModified = null)
        assertTrue(entry.isDirectory)
        assertEquals("subdir", entry.name)
        assertEquals(0L, entry.size)
    }

    @Test
    fun `SmbEntry with empty name`() {
        val entry = SmbEntry(name = "", size = 0, isDirectory = false, lastModified = null)
        assertEquals("", entry.name)
    }

    @Test
    fun `SmbEntry with large size`() {
        val entry = SmbEntry(name = "large.bin", size = Long.MAX_VALUE, isDirectory = false, lastModified = null)
        assertEquals(Long.MAX_VALUE, entry.size)
    }

    @Test
    fun `SmbEntry in set uses structural equality`() {
        val a = SmbEntry(name = "a.txt", size = 1, isDirectory = false, lastModified = null)
        val b = SmbEntry(name = "a.txt", size = 1, isDirectory = false, lastModified = null)
        val set = setOf(a, b)
        assertEquals(1, set.size)
    }

    @Test
    fun `SmbEntry in map as key uses structural equality`() {
        val key1 = SmbEntry(name = "k.txt", size = 10, isDirectory = false, lastModified = null)
        val key2 = SmbEntry(name = "k.txt", size = 10, isDirectory = false, lastModified = null)
        val map = mapOf(key1 to "first", key2 to "second")
        assertEquals(1, map.size)
        assertEquals("second", map[key1])
    }

    @Test
    fun `SmbEntry destructuring`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val accessed = Instant.fromEpochMilliseconds(1700001000000L)
        val created = Instant.fromEpochMilliseconds(1699000000000L)
        val entry = SmbEntry(name = "test.txt", size = 100, isDirectory = false, lastModified = ts, lastAccessed = accessed, creationTime = created)
        val (name, size, isDir, modified, acc, crt) = entry
        assertEquals("test.txt", name)
        assertEquals(100L, size)
        assertFalse(isDir)
        assertEquals(ts, modified)
        assertEquals(accessed, acc)
        assertEquals(created, crt)
    }

    // ================================================================
    // SmbFileInfo data class tests
    // ================================================================

    @Test
    fun `SmbFileInfo equality - identical info is equal`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = SmbFileInfo(size = 2048, isDirectory = false, lastModified = ts, isReadOnly = false)
        val b = SmbFileInfo(size = 2048, isDirectory = false, lastModified = ts, isReadOnly = false)
        assertEquals(a, b)
    }

    @Test
    fun `SmbFileInfo with all fields populated`() {
        val modified = Instant.fromEpochMilliseconds(1700000000000L)
        val accessed = Instant.fromEpochMilliseconds(1700001000000L)
        val created = Instant.fromEpochMilliseconds(1699000000000L)
        val info = SmbFileInfo(
            size = 4096,
            isDirectory = true,
            lastModified = modified,
            lastAccessed = accessed,
            creationTime = created,
            isReadOnly = true
        )
        assertEquals(4096L, info.size)
        assertTrue(info.isDirectory)
        assertEquals(modified, info.lastModified)
        assertEquals(accessed, info.lastAccessed)
        assertEquals(created, info.creationTime)
        assertTrue(info.isReadOnly)
    }

    @Test
    fun `SmbFileInfo defaults - isReadOnly is false`() {
        val info = SmbFileInfo(size = 0, isDirectory = false, lastModified = null)
        assertFalse(info.isReadOnly)
    }

    @Test
    fun `SmbFileInfo copy preserves unchanged fields`() {
        val original = SmbFileInfo(size = 512, isDirectory = false, lastModified = null, isReadOnly = true)
        val copied = original.copy(size = 1024)
        assertEquals(1024L, copied.size)
        assertFalse(copied.isDirectory)
        assertTrue(copied.isReadOnly)
    }

    // ================================================================
    // SmbShareInfo data class tests
    // ================================================================

    @Test
    fun `SmbShareInfo equality`() {
        val a = SmbShareInfo(totalSpace = 1000000, freeSpace = 500000, shareName = "docs")
        val b = SmbShareInfo(totalSpace = 1000000, freeSpace = 500000, shareName = "docs")
        assertEquals(a, b)
    }

    @Test
    fun `SmbShareInfo different share name is not equal`() {
        val a = SmbShareInfo(totalSpace = 1000000, freeSpace = 500000, shareName = "docs")
        val b = SmbShareInfo(totalSpace = 1000000, freeSpace = 500000, shareName = "media")
        assertNotEquals(a, b)
    }

    @Test
    fun `SmbShareInfo field access`() {
        val info = SmbShareInfo(totalSpace = 10000000000L, freeSpace = 3000000000L, shareName = "backup")
        assertEquals(10000000000L, info.totalSpace)
        assertEquals(3000000000L, info.freeSpace)
        assertEquals("backup", info.shareName)
    }

    // ================================================================
    // SmbProtocolClient initial state tests
    // ================================================================

    @Test
    fun `SmbProtocolClient initial state - isConnected is false`() {
        val client = SmbProtocolClient()
        assertFalse(client.isConnected)
    }

    @Test
    fun `SmbProtocolClient connect to invalid host fails`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.connect("this.host.does.not.exist.invalid", 445, "share")
        assertTrue(result.isFailure, "Connecting to invalid host should fail")
        assertFalse(client.isConnected)
    }

    @Test
    fun `SmbProtocolClient disconnect when not connected succeeds`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.disconnect()
        assertTrue(result.isSuccess, "Disconnecting when not connected should succeed gracefully")
        assertFalse(client.isConnected)
    }

    @Test
    fun `SmbProtocolClient double disconnect is safe`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val r1 = client.disconnect()
        assertTrue(r1.isSuccess)
        val r2 = client.disconnect()
        assertTrue(r2.isSuccess)
        assertFalse(client.isConnected)
    }

    // ================================================================
    // SmbProtocolClient operations when not connected
    // ================================================================

    @Test
    fun `SmbProtocolClient authenticate when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.authenticate("DOMAIN", "user", "pass")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is IllegalStateException)
        assertTrue(ex.message?.contains("Not connected") == true)
    }

    @Test
    fun `SmbProtocolClient list when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.list("/")
        assertTrue(result.isFailure)
    }

    @Test
    fun `SmbProtocolClient read when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.read("/file.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `SmbProtocolClient write when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.write("/file.txt", ByteArray(0))
        assertTrue(result.isFailure)
    }

    @Test
    fun `SmbProtocolClient delete when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.delete("/file.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `SmbProtocolClient mkdir when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.mkdir("/newdir")
        assertTrue(result.isFailure)
    }

    @Test
    fun `SmbProtocolClient rmdir when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.rmdir("/olddir")
        assertTrue(result.isFailure)
    }

    @Test
    fun `SmbProtocolClient rename when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.rename("/old.txt", "/new.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `SmbProtocolClient stat when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.stat("/file.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun `SmbProtocolClient getShareInfo when not connected returns failure`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val result = client.getShareInfo()
        assertTrue(result.isFailure)
    }

    // ================================================================
    // SmbProtocolClient connect-disconnect lifecycle
    // ================================================================

    @Test
    fun `SmbProtocolClient still not connected after failed connect attempt`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        client.connect("localhost", 0, "share")
        assertFalse(client.isConnected)
        val result = client.disconnect()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SmbProtocolClient connect failure preserves disconnected state`() = runBlocking<Unit> {
        val client = SmbProtocolClient()
        val connectResult = client.connect("this.host.does.not.exist.invalid", 445, "share")
        assertTrue(connectResult.isFailure)
        assertFalse(client.isConnected)

        // All operations should still fail
        val authResult = client.authenticate("DOMAIN", "user", "pass")
        assertTrue(authResult.isFailure)

        val listResult = client.list("/")
        assertTrue(listResult.isFailure)

        // Disconnect should still succeed
        val disconnectResult = client.disconnect()
        assertTrue(disconnectResult.isSuccess)
    }

    // ================================================================
    // SmbService initial state and utility methods
    // ================================================================

    @Test
    fun `SmbService isOnline initially false`() {
        val service = SmbService(createConfig())
        assertFalse(service.isOnline)
    }

    @Test
    fun `SmbService rootPath is slash`() {
        val service = SmbService(createConfig())
        assertEquals("/", service.rootPath)
    }

    @Test
    fun `SmbService getStorageInfo returns correct details`() = runBlocking<Unit> {
        val config = createConfig(name = "my-smb", host = "192.168.1.10", share = "docs", path = "/projects")
        val service = SmbService(config)
        val info = service.getStorageInfo()

        assertEquals("smb_my-smb", info.id)
        assertEquals("my-smb", info.name)
        assertEquals(StorageType.SMB, info.type)
        assertTrue(info.location.contains("192.168.1.10"))
        assertTrue(info.location.contains("docs"))
        assertTrue(info.location.contains("/projects"))
        assertFalse(info.isOnline)
    }

    @Test
    fun `SmbService getParentPath of root is null`() {
        val service = SmbService(createConfig())
        assertNull(service.getParentPath("/"))
    }

    @Test
    fun `SmbService getParentPath of blank is null`() {
        val service = SmbService(createConfig())
        assertNull(service.getParentPath(""))
    }

    @Test
    fun `SmbService getParentPath of top-level file is root`() {
        val service = SmbService(createConfig())
        assertEquals("/", service.getParentPath("/file.txt"))
    }

    @Test
    fun `SmbService getParentPath of nested path`() {
        val service = SmbService(createConfig())
        assertEquals("/home/user", service.getParentPath("/home/user/docs"))
    }

    @Test
    fun `SmbService getParentPath of path with trailing slash`() {
        val service = SmbService(createConfig())
        assertEquals("/home", service.getParentPath("/home/user/"))
    }

    @Test
    fun `SmbService validatePath valid path succeeds`() {
        val service = SmbService(createConfig())
        val result = service.validatePath("/some/path")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SmbService validatePath blank path fails`() {
        val service = SmbService(createConfig())
        val result = service.validatePath("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `SmbService validatePath whitespace-only path fails`() {
        val service = SmbService(createConfig())
        val result = service.validatePath("   ")
        assertTrue(result.isFailure)
    }

    // ================================================================
    // SmbService operations when not connected
    // ================================================================

    @Test
    fun `SmbService listFiles when not connected emits failure`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.listFiles("/").first()
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.NotConnected)
    }

    @Test
    fun `SmbService downloadFile when not connected emits failed operation`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val operation = service.downloadFile("/remote/file.txt", "/local/file.txt").first()
        assertEquals(NetworkOperation.Status.FAILED, operation.status)
        assertTrue(operation.error?.contains("not connected") == true)
    }

    @Test
    fun `SmbService uploadFile when not connected emits failed operation`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val operation = service.uploadFile("/local/file.txt", "/remote/file.txt").first()
        assertEquals(NetworkOperation.Status.FAILED, operation.status)
        assertTrue(operation.error?.contains("not connected") == true)
    }

    @Test
    fun `SmbService deleteFile when not connected returns failure`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.deleteFile("/remote/file.txt")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.NotConnected)
    }

    @Test
    fun `SmbService getFileInfo when not connected returns failure`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.getFileInfo("/remote/file.txt")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.ConnectionException.NotConnected)
    }

    @Test
    fun `SmbService disconnect when not connected succeeds`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(service.isOnline)
    }

    @Test
    fun `SmbService searchFiles returns failure - not implemented`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.searchFiles("query", "/").first()
        assertTrue(result.isFailure)
    }

    // ================================================================
    // SmbService cache operations
    // ================================================================

    @Test
    fun `SmbService addToCache and getCacheEntries`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val addResult = service.addToCache("/docs/file.txt", priority = 5)
        assertTrue(addResult.isSuccess)

        val entries = service.getCacheEntries("/docs").first()
        assertTrue(entries.isNotEmpty())
        val entry = entries.first()
        assertEquals("/docs/file.txt", entry.remotePath)
    }

    @Test
    fun `SmbService removeFromCache removes entry`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        service.addToCache("/docs/file.txt", priority = 1)
        service.removeFromCache("/docs/file.txt")
        val entries = service.getCacheEntries("/docs").first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `SmbService clearCache empties all entries`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        service.addToCache("/a.txt", priority = 1)
        service.addToCache("/b.txt", priority = 2)
        service.addToCache("/c.txt", priority = 3)

        val beforeClear = service.getCacheEntries(null).first()
        assertEquals(3, beforeClear.size)

        service.clearCache()
        val afterClear = service.getCacheEntries(null).first()
        assertTrue(afterClear.isEmpty())
    }

    // ================================================================
    // SmbService operation management
    // ================================================================

    @Test
    fun `SmbService getActiveOperations initially empty`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val ops = service.getActiveOperations().first()
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `SmbService cancelOperation for nonexistent operation succeeds`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.cancelOperation(999L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SmbService pauseOperation for nonexistent operation succeeds`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.pauseOperation(999L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SmbService resumeOperation for nonexistent operation succeeds`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.resumeOperation(999L)
        assertTrue(result.isSuccess)
    }

    // ================================================================
    // SmbService quota info
    // ================================================================

    @Test
    fun `SmbService getQuotaInfo returns simulated quota`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.getQuotaInfo()
        assertTrue(result.isSuccess)
        val quota = result.getOrThrow()
        assertTrue(quota.totalSpace > 0)
        assertTrue(quota.usedSpace > 0)
        assertTrue(quota.usagePercentage > 0.0)
    }

    // ================================================================
    // SmbService share name handling via config
    // ================================================================

    @Test
    fun `SmbService config share name is accessible`() {
        val config = createConfig(share = "myshare")
        val service = SmbService(config)
        assertEquals("myshare", service.config.share)
    }

    @Test
    fun `SmbService getStorageInfo with custom share reflects in location`() = runBlocking<Unit> {
        val config = createConfig(host = "fileserver", share = "finance", path = "/reports")
        val service = SmbService(config)
        val info = service.getStorageInfo()
        assertTrue(info.location.contains("finance"))
        assertTrue(info.location.contains("/reports"))
        assertTrue(info.location.contains("fileserver"))
    }

    @Test
    fun `SmbService config properties are accessible`() {
        val config = createConfig(name = "prod-smb", host = "10.0.0.1", port = 445, share = "data", domain = "CORP")
        val service = SmbService(config)
        assertEquals("prod-smb", service.config.name)
        assertEquals("10.0.0.1", service.config.host)
        assertEquals(445, service.config.port)
        assertEquals("data", service.config.share)
        assertEquals("CORP", service.config.domain)
    }

    // ================================================================
    // SmbService getRecentChanges when not connected
    // ================================================================

    @Test
    fun `SmbService getRecentChanges when not connected emits empty list`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val recent = service.getRecentChanges(
            since = Instant.fromEpochMilliseconds(0L),
            path = null
        ).first()
        assertTrue(recent.isEmpty())
    }

    // ================================================================
    // SmbService exists when not connected
    // ================================================================

    @Test
    fun `SmbService exists when nothing uploaded returns false`() = runBlocking<Unit> {
        val service = SmbService(createConfig())
        val result = service.exists("/remote/file.txt")
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }
}
