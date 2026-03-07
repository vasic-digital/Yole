/*#######################################################
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *########################################################*/
package digital.vasic.yole.network.protocols

import digital.vasic.yole.network.protocols.ftp.FtpEntry
import digital.vasic.yole.network.protocols.sftp.SftpEntry
import digital.vasic.yole.network.protocols.sftp.SftpFileAttributes
import digital.vasic.yole.network.protocols.smb.SmbEntry
import digital.vasic.yole.network.protocols.smb.SmbFileInfo
import digital.vasic.yole.network.protocols.smb.SmbShareInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*

class ProtocolDataClassTest {

    private val now = Clock.System.now()
    private val earlier = Instant.fromEpochMilliseconds(1700000000000L)

    // ===== FtpEntry =====

    @Test
    fun testFtpEntryCreation() {
        val entry = FtpEntry("file.txt", 1024, false, now, "rwxr-xr-x")
        assertEquals("file.txt", entry.name)
        assertEquals(1024, entry.size)
        assertFalse(entry.isDirectory)
        assertEquals(now, entry.lastModified)
        assertEquals("rwxr-xr-x", entry.permissions)
    }

    @Test
    fun testFtpEntryDirectory() {
        val entry = FtpEntry("docs", 0, true, now)
        assertTrue(entry.isDirectory)
        assertNull(entry.permissions)
    }

    @Test
    fun testFtpEntryNullFields() {
        val entry = FtpEntry("test", 0, false, null, null)
        assertNull(entry.lastModified)
        assertNull(entry.permissions)
    }

    @Test
    fun testFtpEntryEquality() {
        val a = FtpEntry("file.txt", 100, false, now, "rw")
        val b = FtpEntry("file.txt", 100, false, now, "rw")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testFtpEntryInequality() {
        val a = FtpEntry("file.txt", 100, false, now)
        val b = FtpEntry("other.txt", 100, false, now)
        assertNotEquals(a, b)
    }

    @Test
    fun testFtpEntryCopy() {
        val original = FtpEntry("file.txt", 100, false, now, "rw")
        val copy = original.copy(name = "new.txt", size = 200)
        assertEquals("new.txt", copy.name)
        assertEquals(200, copy.size)
        assertFalse(copy.isDirectory)
        assertEquals(now, copy.lastModified)
    }

    @Test
    fun testFtpEntryToString() {
        val entry = FtpEntry("file.txt", 100, false, now)
        val str = entry.toString()
        assertTrue(str.contains("file.txt"))
        assertTrue(str.contains("100"))
    }

    // ===== SmbEntry =====

    @Test
    fun testSmbEntryCreation() {
        val entry = SmbEntry("data.csv", 2048, false, now, earlier, earlier)
        assertEquals("data.csv", entry.name)
        assertEquals(2048, entry.size)
        assertFalse(entry.isDirectory)
        assertEquals(now, entry.lastModified)
        assertEquals(earlier, entry.lastAccessed)
        assertEquals(earlier, entry.creationTime)
    }

    @Test
    fun testSmbEntryDirectory() {
        val entry = SmbEntry("folder", 0, true, now)
        assertTrue(entry.isDirectory)
        assertNull(entry.lastAccessed)
        assertNull(entry.creationTime)
    }

    @Test
    fun testSmbEntryEquality() {
        val a = SmbEntry("x", 10, false, now, earlier, earlier)
        val b = SmbEntry("x", 10, false, now, earlier, earlier)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testSmbEntryInequality() {
        val a = SmbEntry("x", 10, false, now)
        val b = SmbEntry("y", 10, false, now)
        assertNotEquals(a, b)
    }

    @Test
    fun testSmbEntryCopy() {
        val original = SmbEntry("file", 100, false, now)
        val copy = original.copy(isDirectory = true, size = 0)
        assertTrue(copy.isDirectory)
        assertEquals(0, copy.size)
    }

    @Test
    fun testSmbEntryToString() {
        val entry = SmbEntry("report.doc", 5000, false, now)
        assertTrue(entry.toString().contains("report.doc"))
    }

    @Test
    fun testSmbEntryNullTimestamps() {
        val entry = SmbEntry("f", 0, false, null, null, null)
        assertNull(entry.lastModified)
        assertNull(entry.lastAccessed)
        assertNull(entry.creationTime)
    }

    // ===== SmbFileInfo =====

    @Test
    fun testSmbFileInfoCreation() {
        val info = SmbFileInfo(4096, false, now, earlier, earlier, true)
        assertEquals(4096, info.size)
        assertFalse(info.isDirectory)
        assertEquals(now, info.lastModified)
        assertEquals(earlier, info.lastAccessed)
        assertEquals(earlier, info.creationTime)
        assertTrue(info.isReadOnly)
    }

    @Test
    fun testSmbFileInfoDefaults() {
        val info = SmbFileInfo(100, true, now)
        assertNull(info.lastAccessed)
        assertNull(info.creationTime)
        assertFalse(info.isReadOnly)
    }

    @Test
    fun testSmbFileInfoEquality() {
        val a = SmbFileInfo(100, false, now, earlier, earlier, true)
        val b = SmbFileInfo(100, false, now, earlier, earlier, true)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testSmbFileInfoCopy() {
        val original = SmbFileInfo(100, false, now)
        val copy = original.copy(isReadOnly = true)
        assertTrue(copy.isReadOnly)
        assertEquals(100, copy.size)
    }

    @Test
    fun testSmbFileInfoToString() {
        val info = SmbFileInfo(100, false, now)
        assertTrue(info.toString().contains("100"))
    }

    // ===== SmbShareInfo =====

    @Test
    fun testSmbShareInfoCreation() {
        val info = SmbShareInfo(1000000, 500000, "documents")
        assertEquals(1000000, info.totalSpace)
        assertEquals(500000, info.freeSpace)
        assertEquals("documents", info.shareName)
    }

    @Test
    fun testSmbShareInfoEquality() {
        val a = SmbShareInfo(1000, 500, "share1")
        val b = SmbShareInfo(1000, 500, "share1")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testSmbShareInfoInequality() {
        val a = SmbShareInfo(1000, 500, "share1")
        val b = SmbShareInfo(1000, 500, "share2")
        assertNotEquals(a, b)
    }

    @Test
    fun testSmbShareInfoCopy() {
        val original = SmbShareInfo(1000, 500, "share")
        val copy = original.copy(freeSpace = 100)
        assertEquals(100, copy.freeSpace)
        assertEquals(1000, copy.totalSpace)
    }

    @Test
    fun testSmbShareInfoToString() {
        val info = SmbShareInfo(1000, 500, "myshare")
        assertTrue(info.toString().contains("myshare"))
    }

    // ===== SftpEntry =====

    @Test
    fun testSftpEntryCreation() {
        val entry = SftpEntry("script.sh", 512, false, now, "rwxr-xr-x", "root", "wheel")
        assertEquals("script.sh", entry.name)
        assertEquals(512, entry.size)
        assertFalse(entry.isDirectory)
        assertEquals(now, entry.lastModified)
        assertEquals("rwxr-xr-x", entry.permissions)
        assertEquals("root", entry.owner)
        assertEquals("wheel", entry.group)
    }

    @Test
    fun testSftpEntryDefaults() {
        val entry = SftpEntry("file", 100, false, now)
        assertNull(entry.permissions)
        assertNull(entry.owner)
        assertNull(entry.group)
    }

    @Test
    fun testSftpEntryDirectory() {
        val entry = SftpEntry("dir", 4096, true, now)
        assertTrue(entry.isDirectory)
    }

    @Test
    fun testSftpEntryEquality() {
        val a = SftpEntry("f", 10, false, now, "rw", "user", "group")
        val b = SftpEntry("f", 10, false, now, "rw", "user", "group")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testSftpEntryInequality() {
        val a = SftpEntry("a", 10, false, now)
        val b = SftpEntry("b", 10, false, now)
        assertNotEquals(a, b)
    }

    @Test
    fun testSftpEntryCopy() {
        val original = SftpEntry("f", 100, false, now, "rw")
        val copy = original.copy(owner = "admin", group = "staff")
        assertEquals("admin", copy.owner)
        assertEquals("staff", copy.group)
        assertEquals("rw", copy.permissions)
    }

    @Test
    fun testSftpEntryNullFields() {
        val entry = SftpEntry("f", 0, false, null, null, null, null)
        assertNull(entry.lastModified)
        assertNull(entry.permissions)
        assertNull(entry.owner)
        assertNull(entry.group)
    }

    @Test
    fun testSftpEntryToString() {
        val entry = SftpEntry("config.yml", 256, false, now)
        assertTrue(entry.toString().contains("config.yml"))
    }

    // ===== SftpFileAttributes =====

    @Test
    fun testSftpFileAttributesCreation() {
        val attrs = SftpFileAttributes(8192, true, now, earlier, "drwxr-xr-x", "root", "root")
        assertEquals(8192, attrs.size)
        assertTrue(attrs.isDirectory)
        assertEquals(now, attrs.lastModified)
        assertEquals(earlier, attrs.lastAccessed)
        assertEquals("drwxr-xr-x", attrs.permissions)
        assertEquals("root", attrs.owner)
        assertEquals("root", attrs.group)
    }

    @Test
    fun testSftpFileAttributesDefaults() {
        val attrs = SftpFileAttributes(100, false, now)
        assertNull(attrs.lastAccessed)
        assertNull(attrs.permissions)
        assertNull(attrs.owner)
        assertNull(attrs.group)
    }

    @Test
    fun testSftpFileAttributesEquality() {
        val a = SftpFileAttributes(100, false, now, earlier, "rw", "u", "g")
        val b = SftpFileAttributes(100, false, now, earlier, "rw", "u", "g")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testSftpFileAttributesCopy() {
        val original = SftpFileAttributes(100, false, now)
        val copy = original.copy(size = 200, isDirectory = true)
        assertEquals(200, copy.size)
        assertTrue(copy.isDirectory)
    }

    @Test
    fun testSftpFileAttributesToString() {
        val attrs = SftpFileAttributes(1024, false, now)
        assertTrue(attrs.toString().contains("1024"))
    }

    @Test
    fun testSftpFileAttributesNullFields() {
        val attrs = SftpFileAttributes(0, false, null, null, null, null, null)
        assertNull(attrs.lastModified)
        assertNull(attrs.lastAccessed)
        assertNull(attrs.permissions)
        assertNull(attrs.owner)
        assertNull(attrs.group)
    }

    // ===== Cross-type comparisons =====

    @Test
    fun testDifferentDataClassesNotEqual() {
        val sftpEntry = SftpEntry("file", 100, false, now)
        val smbEntry = SmbEntry("file", 100, false, now)
        val ftpEntry = FtpEntry("file", 100, false, now)
        // Different types should not be equal
        assertNotEquals(sftpEntry as Any, smbEntry as Any)
        assertNotEquals(smbEntry as Any, ftpEntry as Any)
        assertNotEquals(ftpEntry as Any, sftpEntry as Any)
    }

    @Test
    fun testLargeFileSize() {
        val entry = FtpEntry("large.iso", Long.MAX_VALUE, false, now)
        assertEquals(Long.MAX_VALUE, entry.size)
    }

    @Test
    fun testZeroFileSize() {
        val entry = SmbEntry("empty", 0, false, now)
        assertEquals(0, entry.size)
    }

    @Test
    fun testEntryWithSpecialCharNames() {
        val entry = FtpEntry("file with spaces.txt", 100, false, now)
        assertEquals("file with spaces.txt", entry.name)

        val entry2 = SftpEntry("日本語ファイル.txt", 100, false, now)
        assertEquals("日本語ファイル.txt", entry2.name)

        val entry3 = SmbEntry("path/to/file.txt", 100, false, now)
        assertEquals("path/to/file.txt", entry3.name)
    }

    @Test
    fun testEntryWithEmptyName() {
        val entry = FtpEntry("", 0, false, null)
        assertEquals("", entry.name)
    }

    @Test
    fun testSmbShareInfoZeroSpace() {
        val info = SmbShareInfo(0, 0, "empty")
        assertEquals(0, info.totalSpace)
        assertEquals(0, info.freeSpace)
    }

    @Test
    fun testSmbShareInfoLargeSpace() {
        val tb = 1024L * 1024L * 1024L * 1024L
        val info = SmbShareInfo(10 * tb, 5 * tb, "bigshare")
        assertEquals(10 * tb, info.totalSpace)
        assertEquals(5 * tb, info.freeSpace)
    }

    @Test
    fun testDestructuring() {
        val (name, size, isDir, modified, perms) = FtpEntry("test.txt", 42, false, now, "rw")
        assertEquals("test.txt", name)
        assertEquals(42L, size)
        assertFalse(isDir)
        assertEquals(now, modified)
        assertEquals("rw", perms)
    }

    @Test
    fun testSftpAttributesDestructuring() {
        val (size, isDir, modified) = SftpFileAttributes(100, true, now)
        assertEquals(100L, size)
        assertTrue(isDir)
        assertEquals(now, modified)
    }

    @Test
    fun testSmbShareInfoDestructuring() {
        val (total, free, name) = SmbShareInfo(1000, 500, "share")
        assertEquals(1000L, total)
        assertEquals(500L, free)
        assertEquals("share", name)
    }
}
