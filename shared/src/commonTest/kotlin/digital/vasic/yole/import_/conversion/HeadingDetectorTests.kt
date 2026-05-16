/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: anti-bluff tests for HeadingDetector.
 *
 * Mutation stub: replace headingLevelByFontSize body with `return null`
 * → tests headingLevelForLargestSize_returnsH1,
 *          headingLevelForMiddleSize_returnsCorrectRank,
 *          clampsAtH6_whenMoreThan6DistinctSizes all FAIL.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeadingDetectorTests {

    @Test
    fun headingLevelForLargestSize_returnsH1() {
        // 24f is largest → rank 0 → H1
        val sizes = listOf(24f, 18f, 14f, 12f)
        val result = HeadingDetector.headingLevelByFontSize(24f, sizes)
        assertEquals(1, result, "Largest font size must map to H1")
    }

    @Test
    fun headingLevelForMiddleSize_returnsCorrectRank() {
        // 18f is rank 1 → H2; 14f is rank 2 → H3
        val sizes = listOf(24f, 18f, 14f, 12f)
        assertEquals(2, HeadingDetector.headingLevelByFontSize(18f, sizes), "Second-largest must be H2")
        assertEquals(3, HeadingDetector.headingLevelByFontSize(14f, sizes), "Third-largest must be H3")
    }

    @Test
    fun smallestSize_returnsNull_bodyText() {
        // 12f is the smallest → body text → null
        val sizes = listOf(24f, 18f, 14f, 12f)
        val result = HeadingDetector.headingLevelByFontSize(12f, sizes)
        assertNull(result, "Smallest font size must return null (body text)")
    }

    @Test
    fun clampsAtH6_whenMoreThan6DistinctSizes() {
        // 8 distinct sizes — ranks 6 and 7 (indices 6,7) must clamp to H6
        // but only index 7 (last) is body text / null; index 6 clamps to H6
        val sizes = listOf(40f, 36f, 30f, 24f, 18f, 14f, 11f, 10f)
        // rank 6 (11f) → clamped to H6 (not null, because 10f is smallest)
        assertEquals(6, HeadingDetector.headingLevelByFontSize(11f, sizes), "Rank > 5 must clamp to H6")
        // rank 7 (10f) → body text → null
        assertNull(HeadingDetector.headingLevelByFontSize(10f, sizes), "Last entry must be null (body text)")
    }
}
