/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.sftp

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.*

/**
 * Desktop (JVM) tests for [SshClient] and related SFTP data classes.
 *
 * Since we cannot connect to real SSH/SFTP servers in unit tests, these tests focus on:
 * - [SftpEntry] data class behavior (equality, copy, hashCode, toString)
 * - [SftpFileAttributes] data class behavior
 * - [SshClient] initial state and failure paths
 * - [SshClient] operations when not connected (should fail gracefully)
 * - [SshClient] disconnect lifecycle
 */
class SshClientTest {

    // ================================================================
    // SftpEntry data class tests
    // ================================================================

    @Test
    fun `SftpEntry equality - identical entries are equal`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = SftpEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = ts, permissions = "rw-r--r--")
        val b = SftpEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = ts, permissions = "rw-r--r--")
        assertEquals(a, b)
    }

    @Test
    fun `SftpEntry equality - different name is not equal`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = SftpEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = ts)
        val b = SftpEntry(name = "other.txt", size = 1024, isDirectory = false, lastModified = ts)
        assertNotEquals(a, b)
    }

    @Test
    fun `SftpEntry equality - different size is not equal`() {
        val a = SftpEntry(name = "file.txt", size = 1024, isDirectory = false, lastModified = null)
        val b = SftpEntry(name = "file.txt", size = 2048, isDirectory = false, lastModified = null)
        assertNotEquals(a, b)
    }

    @Test
    fun `SftpEntry equality - different isDirectory is not equal`() {
        val a = SftpEntry(name = "docs", size = 0, isDirectory = false, lastModified = null)
        val b = SftpEntry(name = "docs", size = 0, isDirectory = true, lastModified = null)
        assertNotEquals(a, b)
    }

    @Test
    fun `SftpEntry copy produces independent copy`() {
        val original = SftpEntry(name = "readme.md", size = 512, isDirectory = false, lastModified = null, permissions = "rwxr-xr-x")
        val copied = original.copy(name = "README.md", size = 1024)
        assertEquals("README.md", copied.name)
        assertEquals(1024, copied.size)
        // Original unchanged
        assertEquals("readme.md", original.name)
        assertEquals(512, original.size)
        // Non-copied fields preserved
        assertFalse(copied.isDirectory)
        assertEquals("rwxr-xr-x", copied.permissions)
    }

    @Test
    fun `SftpEntry hashCode - equal entries have same hashCode`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = SftpEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = ts, permissions = "rw-")
        val b = SftpEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = ts, permissions = "rw-")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `SftpEntry toString contains field values`() {
        val entry = SftpEntry(name = "test.txt", size = 42, isDirectory = false, lastModified = null, permissions = "rw-")
        val str = entry.toString()
        assertTrue(str.contains("test.txt"), "toString should contain name")
        assertTrue(str.contains("42"), "toString should contain size")
        assertTrue(str.contains("false"), "toString should contain isDirectory")
        assertTrue(str.contains("rw-"), "toString should contain permissions")
    }

    @Test
    fun `SftpEntry with null lastModified`() {
        val entry = SftpEntry(name = "no-date.txt", size = 0, isDirectory = false, lastModified = null)
        assertNull(entry.lastModified)
        assertEquals("no-date.txt", entry.name)
    }

    @Test
    fun `SftpEntry with null permissions`() {
        val entry = SftpEntry(name = "no-perms.txt", size = 256, isDirectory = false, lastModified = null, permissions = null)
        assertNull(entry.permissions)
    }

    @Test
    fun `SftpEntry with all nullable fields set to null`() {
        val entry = SftpEntry(name = "bare.txt", size = 0, isDirectory = false, lastModified = null, permissions = null, owner = null, group = null)
        assertNull(entry.lastModified)
        assertNull(entry.permissions)
        assertNull(entry.owner)
        assertNull(entry.group)
        assertEquals("bare.txt", entry.name)
        assertEquals(0L, entry.size)
        assertFalse(entry.isDirectory)
    }

    @Test
    fun `SftpEntry directory entry`() {
        val entry = SftpEntry(name = "subdir", size = 4096, isDirectory = true, lastModified = null, permissions = "drwxr-xr-x")
        assertTrue(entry.isDirectory)
        assertEquals("subdir", entry.name)
        assertEquals(4096L, entry.size)
    }

    @Test
    fun `SftpEntry with owner and group`() {
        val entry = SftpEntry(
            name = "owned.txt",
            size = 128,
            isDirectory = false,
            lastModified = null,
            permissions = "rw-r--r--",
            owner = "1000",
            group = "1000"
        )
        assertEquals("1000", entry.owner)
        assertEquals("1000", entry.group)
    }

    @Test
    fun `SftpEntry in set uses structural equality`() {
        val a = SftpEntry(name = "a.txt", size = 1, isDirectory = false, lastModified = null)
        val b = SftpEntry(name = "a.txt", size = 1, isDirectory = false, lastModified = null)
        val set = setOf(a, b)
        assertEquals(1, set.size)
    }

    @Test
    fun `SftpEntry in map as key uses structural equality`() {
        val key1 = SftpEntry(name = "k.txt", size = 10, isDirectory = false, lastModified = null)
        val key2 = SftpEntry(name = "k.txt", size = 10, isDirectory = false, lastModified = null)
        val map = mapOf(key1 to "first", key2 to "second")
        assertEquals(1, map.size)
        assertEquals("second", map[key1])
    }

    @Test
    fun `SftpEntry with empty name`() {
        val entry = SftpEntry(name = "", size = 0, isDirectory = false, lastModified = null)
        assertEquals("", entry.name)
    }

    @Test
    fun `SftpEntry with large size`() {
        val entry = SftpEntry(name = "large.bin", size = Long.MAX_VALUE, isDirectory = false, lastModified = null)
        assertEquals(Long.MAX_VALUE, entry.size)
    }

    @Test
    fun `SftpEntry destructuring`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val entry = SftpEntry(name = "test.txt", size = 100, isDirectory = false, lastModified = ts, permissions = "rw-r--r--", owner = "root", group = "wheel")
        val (name, size, isDir, modified, perms, owner, group) = entry
        assertEquals("test.txt", name)
        assertEquals(100L, size)
        assertFalse(isDir)
        assertEquals(ts, modified)
        assertEquals("rw-r--r--", perms)
        assertEquals("root", owner)
        assertEquals("wheel", group)
    }

    // ================================================================
    // SftpFileAttributes data class tests
    // ================================================================

    @Test
    fun `SftpFileAttributes equality - identical attributes are equal`() {
        val ts = Instant.fromEpochMilliseconds(1700000000000L)
        val a = SftpFileAttributes(size = 2048, isDirectory = false, lastModified = ts)
        val b = SftpFileAttributes(size = 2048, isDirectory = false, lastModified = ts)
        assertEquals(a, b)
    }

    @Test
    fun `SftpFileAttributes with all fields populated`() {
        val modified = Instant.fromEpochMilliseconds(1700000000000L)
        val accessed = Instant.fromEpochMilliseconds(1700001000000L)
        val attrs = SftpFileAttributes(
            size = 4096,
            isDirectory = true,
            lastModified = modified,
            lastAccessed = accessed,
            permissions = "rwxr-xr-x",
            owner = "0",
            group = "0"
        )
        assertEquals(4096L, attrs.size)
        assertTrue(attrs.isDirectory)
        assertEquals(modified, attrs.lastModified)
        assertEquals(accessed, attrs.lastAccessed)
        assertEquals("rwxr-xr-x", attrs.permissions)
        assertEquals("0", attrs.owner)
        assertEquals("0", attrs.group)
    }

    @Test
    fun `SftpFileAttributes with all nullable fields null`() {
        val attrs = SftpFileAttributes(size = 0, isDirectory = false, lastModified = null)
        assertNull(attrs.lastModified)
        assertNull(attrs.lastAccessed)
        assertNull(attrs.permissions)
        assertNull(attrs.owner)
        assertNull(attrs.group)
    }

    @Test
    fun `SftpFileAttributes copy preserves unchanged fields`() {
        val original = SftpFileAttributes(size = 512, isDirectory = false, lastModified = null, permissions = "rw-")
        val copied = original.copy(size = 1024)
        assertEquals(1024L, copied.size)
        assertFalse(copied.isDirectory)
        assertEquals("rw-", copied.permissions)
    }

    @Test
    fun `SftpFileAttributes hashCode consistency`() {
        val a = SftpFileAttributes(size = 10, isDirectory = false, lastModified = null)
        val b = SftpFileAttributes(size = 10, isDirectory = false, lastModified = null)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ================================================================
    // SshClient initial state tests
    // ================================================================

    @Test
    fun `SshClient initial state - isConnected is false`() {
        val client = SshClient()
        assertFalse(client.isConnected)
    }

    @Test
    fun `SshClient connect to localhost port 0 fails`() = runBlocking<Unit> {
        val client = SshClient()
        val result = client.connect("localhost", 0)
        assertTrue(result.isFailure, "Connecting to port 0 should fail")
        assertFalse(client.isConnected)
    }

    @Test
    fun `SshClient connect to invalid host fails`() = runBlocking<Unit> {
        val client = SshClient()
        val result = client.connect("this.host.does.not.exist.invalid", 22)
        assertTrue(result.isFailure, "Connecting to invalid host should fail")
        assertFalse(client.isConnected)
    }

    @Test
    fun `SshClient disconnect when not connected succeeds`() = runBlocking<Unit> {
        val client = SshClient()
        val result = client.disconnect()
        assertTrue(result.isSuccess, "Disconnecting when not connected should succeed gracefully")
        assertFalse(client.isConnected)
    }

    @Test
    fun `SshClient double disconnect is safe`() = runBlocking<Unit> {
        val client = SshClient()
        val r1 = client.disconnect()
        assertTrue(r1.isSuccess)
        val r2 = client.disconnect()
        assertTrue(r2.isSuccess)
        assertFalse(client.isConnected)
    }

    // ================================================================
    // SshClient operations when not connected
    // ================================================================

    @Test
    fun `SshClient authenticatePassword when not connected returns failure`() = runBlocking<Unit> {
        val client = SshClient()
        val result = client.authenticatePassword("user", "pass")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is IllegalStateException)
        assertTrue(ex.message?.contains("Not connected") == true)
    }

    @Test
    fun `SshClient authenticateKey when not connected returns failure`() = runBlocking<Unit> {
        val client = SshClient()
        val result = client.authenticateKey("user", "/path/to/key", null)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is IllegalStateException)
        assertTrue(ex.message?.contains("Not connected") == true)
    }

    @Test
    fun `SshClient authenticateKey with passphrase when not connected returns failure`() = runBlocking<Unit> {
        val client = SshClient()
        val result = client.authenticateKey("user", "/path/to/key", "my-passphrase")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is IllegalStateException)
    }

    @Test
    fun `SshClient openSftpChannel when not connected returns failure`() = runBlocking<Unit> {
        val client = SshClient()
        val result = client.openSftpChannel()
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is IllegalStateException)
        assertTrue(ex.message?.contains("Not connected") == true)
    }

    // ================================================================
    // SshClient connect-disconnect lifecycle
    // ================================================================

    @Test
    fun `SshClient still not connected after failed connect attempt`() = runBlocking<Unit> {
        val client = SshClient()
        client.connect("localhost", 0)
        assertFalse(client.isConnected)
        // Should be safe to disconnect even after failed connect
        val result = client.disconnect()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SshClient connect failure preserves disconnected state for subsequent operations`() = runBlocking<Unit> {
        val client = SshClient()
        // Attempt failed connection
        val connectResult = client.connect("this.host.does.not.exist.invalid", 22)
        assertTrue(connectResult.isFailure)
        assertFalse(client.isConnected)

        // All operations should still fail with not-connected
        val authResult = client.authenticatePassword("user", "pass")
        assertTrue(authResult.isFailure)

        val channelResult = client.openSftpChannel()
        assertTrue(channelResult.isFailure)

        // Disconnect should still succeed
        val disconnectResult = client.disconnect()
        assertTrue(disconnectResult.isSuccess)
    }

    @Test
    fun `SshClient multiple failed connect attempts do not accumulate state`() = runBlocking<Unit> {
        val client = SshClient()
        // Multiple failed connects
        client.connect("localhost", 0)
        client.connect("localhost", 0)
        client.connect("localhost", 0)
        assertFalse(client.isConnected)
        // Single disconnect should clean up
        val result = client.disconnect()
        assertTrue(result.isSuccess)
        assertFalse(client.isConnected)
    }

    // ================================================================
    // SftpEntry edge cases
    // ================================================================

    @Test
    fun `SftpEntry with zero size`() {
        val entry = SftpEntry(name = "empty.txt", size = 0, isDirectory = false, lastModified = null)
        assertEquals(0L, entry.size)
    }

    @Test
    fun `SftpEntry equality with different owner`() {
        val a = SftpEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = null, owner = "root")
        val b = SftpEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = null, owner = "user")
        assertNotEquals(a, b)
    }

    @Test
    fun `SftpEntry equality with different group`() {
        val a = SftpEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = null, group = "wheel")
        val b = SftpEntry(name = "file.txt", size = 100, isDirectory = false, lastModified = null, group = "staff")
        assertNotEquals(a, b)
    }

    @Test
    fun `SftpFileAttributes directory vs file distinction`() {
        val fileAttrs = SftpFileAttributes(size = 1024, isDirectory = false, lastModified = null)
        val dirAttrs = SftpFileAttributes(size = 0, isDirectory = true, lastModified = null)
        assertFalse(fileAttrs.isDirectory)
        assertTrue(dirAttrs.isDirectory)
        assertNotEquals(fileAttrs, dirAttrs)
    }
}
