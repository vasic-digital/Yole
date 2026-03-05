/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for NetworkStorage data class
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkStorageTest {

    @Test
    fun testAvailableSpace() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.WEBDAV,
            location = "http://example.com",
            totalSpace = 1000L, usedSpace = 300L
        )
        assertEquals(700L, storage.availableSpace)
    }

    @Test
    fun testAvailableSpaceNullWhenMissing() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.FTP,
            location = "ftp://example.com"
        )
        assertNull(storage.availableSpace)
    }

    @Test
    fun testUsagePercentage() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.WEBDAV,
            location = "http://example.com",
            totalSpace = 1000L, usedSpace = 250L
        )
        assertEquals(0.25, storage.usagePercentage!!, 0.001)
    }

    @Test
    fun testUsagePercentageNullWhenNoInfo() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.SFTP,
            location = "sftp://example.com"
        )
        assertNull(storage.usagePercentage)
    }

    @Test
    fun testIsFull() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.SMB,
            location = "smb://example.com",
            totalSpace = 1000L, usedSpace = 1000L
        )
        assertTrue(storage.isFull)
    }

    @Test
    fun testIsNotFull() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.DROPBOX,
            location = "dropbox://",
            totalSpace = 1000L, usedSpace = 500L
        )
        assertFalse(storage.isFull)
    }

    @Test
    fun testIsLowOnSpace() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.GOOGLE_DRIVE,
            location = "gdrive://",
            totalSpace = 1000L, usedSpace = 950L
        )
        assertTrue(storage.isLowOnSpace)
    }

    @Test
    fun testIsNotLowOnSpace() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.ONEDRIVE,
            location = "onedrive://",
            totalSpace = 1000L, usedSpace = 100L
        )
        assertFalse(storage.isLowOnSpace)
    }

    @Test
    fun testSupportsExtensionAllWhenEmpty() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.WEBDAV,
            location = "http://example.com"
        )
        assertTrue(storage.supportsExtension("md"))
        assertTrue(storage.supportsExtension("txt"))
    }

    @Test
    fun testSupportsExtensionFiltered() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.WEBDAV,
            location = "http://example.com",
            supportedExtensions = listOf("md", "txt")
        )
        assertTrue(storage.supportsExtension("md"))
        assertTrue(storage.supportsExtension("MD"))
        assertFalse(storage.supportsExtension("pdf"))
    }

    @Test
    fun testSupportsFile() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.WEBDAV,
            location = "http://example.com",
            supportedExtensions = listOf("md", "txt")
        )
        assertTrue(storage.supportsFile("readme.md"))
        assertFalse(storage.supportsFile("image.png"))
    }

    @Test
    fun testSupportsFileNoExtension() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.WEBDAV,
            location = "http://example.com",
            supportedExtensions = listOf("md")
        )
        assertTrue(storage.supportsFile("Makefile"))
    }

    @Test
    fun testWithOnlineStatus() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.GIT,
            location = "https://github.com/test"
        )
        assertFalse(storage.isOnline)
        val online = storage.withOnlineStatus(true)
        assertTrue(online.isOnline)
    }

    @Test
    fun testWithLastSync() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.WEBDAV,
            location = "http://example.com"
        )
        assertNull(storage.lastSync)
        val now = kotlinx.datetime.Clock.System.now()
        val synced = storage.withLastSync(now)
        assertEquals(now, synced.lastSync)
    }

    @Test
    fun testWithSpaceInfo() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.WEBDAV,
            location = "http://example.com"
        )
        val updated = storage.withSpaceInfo(totalSpace = 5000L, usedSpace = 1000L)
        assertEquals(5000L, updated.totalSpace)
        assertEquals(1000L, updated.usedSpace)
        assertEquals(4000L, updated.availableSpace)
    }

    @Test
    fun testMock() {
        val mock = NetworkStorage.mock()
        assertNotNull(mock.id)
        assertTrue(mock.isOnline)
        assertNotNull(mock.totalSpace)
        assertNotNull(mock.usedSpace)
    }

    @Test
    fun testDefaultValues() {
        val storage = NetworkStorage(
            id = "s1", name = "Test", type = StorageType.WEBDAV,
            location = "http://example.com"
        )
        assertFalse(storage.isOnline)
        assertTrue(storage.isEnabled)
        assertEquals(100, storage.priority)
        assertTrue(storage.supportsFolders)
        assertTrue(storage.supportsMetadata)
        assertNull(storage.maxFileSize)
        assertTrue(storage.supportedExtensions.isEmpty())
    }
}
