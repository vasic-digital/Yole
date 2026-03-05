/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for StorageModels (StorageInfo, QuotaInfo, FileInfo)
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorageInfoTest {

    @Test
    fun testStorageInfoCreation() {
        val info = StorageInfo(
            id = "s1",
            name = "My Storage",
            storageType = StorageType.WEBDAV,
            baseUrl = "http://example.com/dav",
            isConnected = true,
            lastSync = "2026-03-05T12:00:00Z"
        )
        assertEquals("s1", info.id)
        assertEquals("My Storage", info.name)
        assertEquals(StorageType.WEBDAV, info.storageType)
        assertEquals("http://example.com/dav", info.baseUrl)
        assertTrue(info.isConnected)
        assertEquals("2026-03-05T12:00:00Z", info.lastSync)
    }

    @Test
    fun testStorageInfoDefaults() {
        val info = StorageInfo(id = "s2", name = "Test", storageType = StorageType.FTP)
        assertNull(info.baseUrl)
        assertFalse(info.isConnected)
        assertNull(info.lastSync)
    }

    @Test
    fun testStorageInfoDataClassEquality() {
        val i1 = StorageInfo(id = "s1", name = "A", storageType = StorageType.SFTP)
        val i2 = StorageInfo(id = "s1", name = "A", storageType = StorageType.SFTP)
        assertEquals(i1, i2)
    }
}

class QuotaInfoTest {

    @Test
    fun testQuotaInfoCreation() {
        val quota = QuotaInfo(
            usedBytes = 500L,
            totalBytes = 1000L,
            availableBytes = 500L,
            usedPercentage = 50.0
        )
        assertEquals(500L, quota.usedBytes)
        assertEquals(1000L, quota.totalBytes)
        assertEquals(500L, quota.availableBytes)
        assertEquals(50.0, quota.usedPercentage, 0.001)
    }

    @Test
    fun testQuotaInfoDefaults() {
        val quota = QuotaInfo()
        assertEquals(0L, quota.usedBytes)
        assertEquals(0L, quota.totalBytes)
        assertEquals(0L, quota.availableBytes)
        assertEquals(0.0, quota.usedPercentage, 0.001)
    }
}

class FileInfoTest {

    @Test
    fun testFileInfoCreation() {
        val file = FileInfo(
            name = "readme.md",
            path = "/docs/readme.md",
            size = 4096L,
            isDirectory = false,
            lastModified = "2026-03-05"
        )
        assertEquals("readme.md", file.name)
        assertEquals("/docs/readme.md", file.path)
        assertEquals(4096L, file.size)
        assertFalse(file.isDirectory)
        assertEquals("2026-03-05", file.lastModified)
    }

    @Test
    fun testFileInfoDefaults() {
        val file = FileInfo(name = "test", path = "/test", size = 0L)
        assertFalse(file.isDirectory)
        assertNull(file.lastModified)
    }

    @Test
    fun testFileInfoDirectory() {
        val dir = FileInfo(name = "docs", path = "/docs", size = 0L, isDirectory = true)
        assertTrue(dir.isDirectory)
    }
}
