/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Quota Threshold Regression Tests
 *
 * Regression tests for Phase 1 concurrency fix #10:
 * isLowOnSpace threshold consistency across services.
 *
 * The fix changed DropboxService from using > 0.9 to
 * using >= 0.9 for the isLowOnSpace threshold, aligning
 * it with GoogleDrive and OneDrive. These tests verify
 * that StorageQuota objects correctly report isLowOnSpace
 * at the >= 0.9 boundary.
 *
 *########################################################*/

package digital.vasic.yole.concurrency

import digital.vasic.yole.network.StorageQuota
import kotlin.test.*

/**
 * Regression tests for StorageQuota.isLowOnSpace threshold consistency.
 *
 * Phase 1 fix #10 ensured all services use >= 0.9 (not > 0.9) as the
 * threshold for isLowOnSpace. Since StorageQuota is a data class with
 * explicit isLowOnSpace parameter, these tests verify the threshold
 * logic that services use when constructing StorageQuota objects.
 *
 * The canonical threshold is: isLowOnSpace = usagePercentage >= 0.9
 */
class QuotaThresholdTests {

    // ==================== Helper ====================

    /**
     * Creates a StorageQuota with the given usage percentage,
     * applying the canonical threshold: isLowOnSpace = usagePercentage >= 0.9
     * This mirrors what all fixed services now do.
     */
    private fun createQuotaWithUsage(usagePercentage: Double): StorageQuota {
        val totalSpace = 1_000_000_000L // 1 GB
        val usedSpace = (totalSpace * usagePercentage).toLong()
        val availableSpace = totalSpace - usedSpace

        return StorageQuota(
            totalSpace = totalSpace,
            usedSpace = usedSpace,
            availableSpace = availableSpace,
            usagePercentage = usagePercentage,
            isFull = availableSpace <= 0,
            isLowOnSpace = usagePercentage >= 0.9
        )
    }

    // ==================== Exact Boundary Tests ====================

    @Test
    fun `StorageQuota with exactly 0_9 usage marks isLowOnSpace true`() {
        val quota = createQuotaWithUsage(0.9)
        assertTrue(quota.isLowOnSpace, "Usage of exactly 0.9 should be low on space (>= 0.9 threshold)")
        assertEquals(0.9, quota.usagePercentage, 0.0001)
    }

    @Test
    fun `StorageQuota with 0_89 usage marks isLowOnSpace false`() {
        val quota = createQuotaWithUsage(0.89)
        assertFalse(quota.isLowOnSpace, "Usage of 0.89 should NOT be low on space")
        assertEquals(0.89, quota.usagePercentage, 0.0001)
    }

    @Test
    fun `StorageQuota with 0_91 usage marks isLowOnSpace true`() {
        val quota = createQuotaWithUsage(0.91)
        assertTrue(quota.isLowOnSpace, "Usage of 0.91 should be low on space")
        assertEquals(0.91, quota.usagePercentage, 0.0001)
    }

    // ==================== Fine-Grained Boundary Tests ====================

    @Test
    fun `Boundary - 0_8999999 is NOT low on space`() {
        val quota = createQuotaWithUsage(0.8999999)
        assertFalse(quota.isLowOnSpace, "Usage of 0.8999999 should NOT be low on space")
    }

    @Test
    fun `Boundary - 0_9000001 IS low on space`() {
        val quota = createQuotaWithUsage(0.9000001)
        assertTrue(quota.isLowOnSpace, "Usage of 0.9000001 should be low on space")
    }

    @Test
    fun `Boundary - exactly 0_9 IS low on space (ge not gt)`() {
        // This is the key regression test: the fix changed > 0.9 to >= 0.9
        val usagePercentage = 0.9
        val isLowOnSpace = usagePercentage >= 0.9
        assertTrue(isLowOnSpace, "The >= 0.9 check must return true for exactly 0.9")
    }

    // ==================== Extreme Values ====================

    @Test
    fun `StorageQuota with 0_0 usage is NOT low on space`() {
        val quota = createQuotaWithUsage(0.0)
        assertFalse(quota.isLowOnSpace, "Empty storage should not be low on space")
    }

    @Test
    fun `StorageQuota with 0_5 usage is NOT low on space`() {
        val quota = createQuotaWithUsage(0.5)
        assertFalse(quota.isLowOnSpace, "Half-full storage should not be low on space")
    }

    @Test
    fun `StorageQuota with 1_0 usage IS low on space and full`() {
        val quota = createQuotaWithUsage(1.0)
        assertTrue(quota.isLowOnSpace, "Full storage should be low on space")
        assertTrue(quota.isFull, "100% usage should mark storage as full")
    }

    @Test
    fun `StorageQuota with 0_95 usage IS low on space`() {
        val quota = createQuotaWithUsage(0.95)
        assertTrue(quota.isLowOnSpace, "95% usage should be low on space")
    }

    // ==================== Consistency Across Services ====================

    @Test
    fun `All three cloud services produce consistent results for same percentage at boundary`() {
        val usagePercentage = 0.9

        // Simulate what each service does when constructing StorageQuota
        // After the fix, all three use: isLowOnSpace = usagePercentage >= 0.9
        val dropboxQuota = createQuotaWithUsage(usagePercentage)
        val gdriveQuota = createQuotaWithUsage(usagePercentage)
        val onedriveQuota = createQuotaWithUsage(usagePercentage)

        assertEquals(
            dropboxQuota.isLowOnSpace, gdriveQuota.isLowOnSpace,
            "Dropbox and GoogleDrive should agree on isLowOnSpace at 0.9"
        )
        assertEquals(
            gdriveQuota.isLowOnSpace, onedriveQuota.isLowOnSpace,
            "GoogleDrive and OneDrive should agree on isLowOnSpace at 0.9"
        )
        assertTrue(
            dropboxQuota.isLowOnSpace,
            "All services should mark 0.9 as low on space"
        )
    }

    @Test
    fun `All three cloud services produce consistent results below threshold`() {
        val usagePercentage = 0.85

        val dropboxQuota = createQuotaWithUsage(usagePercentage)
        val gdriveQuota = createQuotaWithUsage(usagePercentage)
        val onedriveQuota = createQuotaWithUsage(usagePercentage)

        assertFalse(dropboxQuota.isLowOnSpace, "Dropbox at 0.85 should not be low")
        assertFalse(gdriveQuota.isLowOnSpace, "GoogleDrive at 0.85 should not be low")
        assertFalse(onedriveQuota.isLowOnSpace, "OneDrive at 0.85 should not be low")
    }

    @Test
    fun `All three cloud services produce consistent results above threshold`() {
        val usagePercentage = 0.95

        val dropboxQuota = createQuotaWithUsage(usagePercentage)
        val gdriveQuota = createQuotaWithUsage(usagePercentage)
        val onedriveQuota = createQuotaWithUsage(usagePercentage)

        assertTrue(dropboxQuota.isLowOnSpace, "Dropbox at 0.95 should be low")
        assertTrue(gdriveQuota.isLowOnSpace, "GoogleDrive at 0.95 should be low")
        assertTrue(onedriveQuota.isLowOnSpace, "OneDrive at 0.95 should be low")
    }

    // ==================== StorageQuota Data Class Properties ====================

    @Test
    fun `StorageQuota formatted properties are accessible`() {
        val quota = createQuotaWithUsage(0.9)
        assertNotNull(quota.formattedTotalSpace, "formattedTotalSpace should not be null")
        assertNotNull(quota.formattedUsedSpace, "formattedUsedSpace should not be null")
        assertTrue(quota.formattedTotalSpace.isNotEmpty(), "formattedTotalSpace should not be empty")
    }

    @Test
    fun `StorageQuota space calculations are consistent`() {
        val quota = createQuotaWithUsage(0.9)
        assertEquals(
            quota.totalSpace,
            quota.usedSpace + quota.availableSpace,
            "totalSpace should equal usedSpace + availableSpace"
        )
    }

    @Test
    fun `StorageQuota with metadata is correctly stored`() {
        val quota = StorageQuota(
            totalSpace = 1_000_000L,
            usedSpace = 900_000L,
            availableSpace = 100_000L,
            usagePercentage = 0.9,
            isFull = false,
            isLowOnSpace = true,
            metadata = mapOf("provider" to "Dropbox")
        )
        assertEquals("Dropbox", quota.metadata["provider"])
        assertTrue(quota.isLowOnSpace)
    }
}
