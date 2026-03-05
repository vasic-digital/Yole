/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for StorageQuota data class
 *
 *########################################################*/
package digital.vasic.yole.network

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorageQuotaTest {

    @Test
    fun testBasicCreation() {
        val quota = StorageQuota(
            totalSpace = 1073741824L, // 1GB
            usedSpace = 536870912L,   // 512MB
            availableSpace = 536870912L,
            usagePercentage = 0.5,
            isFull = false,
            isLowOnSpace = false
        )
        assertEquals(1073741824L, quota.totalSpace)
        assertEquals(536870912L, quota.usedSpace)
        assertEquals(536870912L, quota.availableSpace)
        assertEquals(0.5, quota.usagePercentage)
        assertFalse(quota.isFull)
        assertFalse(quota.isLowOnSpace)
        assertNull(quota.expiresAt)
        assertTrue(quota.metadata.isEmpty())
    }

    @Test
    fun testFullStorage() {
        val quota = StorageQuota(
            totalSpace = 1024L,
            usedSpace = 1024L,
            availableSpace = 0L,
            usagePercentage = 1.0,
            isFull = true,
            isLowOnSpace = true
        )
        assertTrue(quota.isFull)
        assertTrue(quota.isLowOnSpace)
    }

    @Test
    fun testFormattedTotalSpaceBytes() {
        val quota = StorageQuota(
            totalSpace = 512L, usedSpace = 0L, availableSpace = 512L,
            usagePercentage = 0.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("512B", quota.formattedTotalSpace)
    }

    @Test
    fun testFormattedTotalSpaceKilobytes() {
        val quota = StorageQuota(
            totalSpace = 2048L, usedSpace = 0L, availableSpace = 2048L,
            usagePercentage = 0.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("2KB", quota.formattedTotalSpace)
    }

    @Test
    fun testFormattedTotalSpaceMegabytes() {
        val quota = StorageQuota(
            totalSpace = 5242880L, usedSpace = 0L, availableSpace = 5242880L,
            usagePercentage = 0.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("5MB", quota.formattedTotalSpace)
    }

    @Test
    fun testFormattedTotalSpaceGigabytes() {
        val quota = StorageQuota(
            totalSpace = 10737418240L, usedSpace = 0L, availableSpace = 10737418240L,
            usagePercentage = 0.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("10GB", quota.formattedTotalSpace)
    }

    @Test
    fun testFormattedUsedSpace() {
        val quota = StorageQuota(
            totalSpace = 1073741824L, usedSpace = 1048576L, availableSpace = 1072693248L,
            usagePercentage = 0.001, isFull = false, isLowOnSpace = false
        )
        assertEquals("1MB", quota.formattedUsedSpace)
    }

    @Test
    fun testFormattedAvailableSpace() {
        val quota = StorageQuota(
            totalSpace = 1073741824L, usedSpace = 1073741824L - 3072L, availableSpace = 3072L,
            usagePercentage = 0.99, isFull = false, isLowOnSpace = true
        )
        assertEquals("3KB", quota.formattedAvailableSpace)
    }

    @Test
    fun testUsagePercentageString() {
        val quota = StorageQuota(
            totalSpace = 100L, usedSpace = 75L, availableSpace = 25L,
            usagePercentage = 0.75, isFull = false, isLowOnSpace = false
        )
        assertEquals("75%", quota.usagePercentageString)
    }

    @Test
    fun testUsagePercentageStringZero() {
        val quota = StorageQuota(
            totalSpace = 100L, usedSpace = 0L, availableSpace = 100L,
            usagePercentage = 0.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("0%", quota.usagePercentageString)
    }

    @Test
    fun testUsagePercentageStringFull() {
        val quota = StorageQuota(
            totalSpace = 100L, usedSpace = 100L, availableSpace = 0L,
            usagePercentage = 1.0, isFull = true, isLowOnSpace = true
        )
        assertEquals("100%", quota.usagePercentageString)
    }

    @Test
    fun testWithExpiresAt() {
        val now = Clock.System.now()
        val quota = StorageQuota(
            totalSpace = 100L, usedSpace = 50L, availableSpace = 50L,
            usagePercentage = 0.5, isFull = false, isLowOnSpace = false,
            expiresAt = now
        )
        assertEquals(now, quota.expiresAt)
    }

    @Test
    fun testWithMetadata() {
        val quota = StorageQuota(
            totalSpace = 100L, usedSpace = 50L, availableSpace = 50L,
            usagePercentage = 0.5, isFull = false, isLowOnSpace = false,
            metadata = mapOf("plan" to "pro", "region" to "us-west")
        )
        assertEquals("pro", quota.metadata["plan"])
        assertEquals("us-west", quota.metadata["region"])
    }

    @Test
    fun testEquality() {
        val q1 = StorageQuota(100L, 50L, 50L, 0.5, false, false)
        val q2 = StorageQuota(100L, 50L, 50L, 0.5, false, false)
        assertEquals(q1, q2)
        assertEquals(q1.hashCode(), q2.hashCode())
    }

    @Test
    fun testCopy() {
        val original = StorageQuota(100L, 50L, 50L, 0.5, false, false)
        val updated = original.copy(usedSpace = 90L, availableSpace = 10L, usagePercentage = 0.9, isLowOnSpace = true)
        assertEquals(90L, updated.usedSpace)
        assertEquals(10L, updated.availableSpace)
        assertTrue(updated.isLowOnSpace)
        assertEquals(100L, updated.totalSpace)
    }
}
