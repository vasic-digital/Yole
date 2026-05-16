/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: HeadingDetector — font-size → H1-H6 rank mapping.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

/**
 * Maps a font size to a Markdown heading level (1–6) by comparing it
 * against a caller-supplied sorted-distinct-sizes list.
 *
 * The sizes list must be sorted in **descending** order (largest first).
 * The largest size maps to H1, the next to H2, and so on up to H6.
 * If [currentSize] equals the smallest size in the list (body text),
 * `null` is returned — the caller should emit a plain paragraph instead.
 *
 * Sizes are compared with exact floating-point equality; callers are
 * responsible for rounding before passing values in.
 */
object HeadingDetector {

    /**
     * Returns the heading level (1–6) for [currentSize] within
     * [sortedDistinctSizes] (descending), or `null` when [currentSize]
     * is the body-text size (smallest in the list).
     *
     * @param currentSize      The font size of the run being classified.
     * @param sortedDistinctSizes Distinct font sizes present in the document,
     *                          sorted largest-first (descending).
     * @return heading level in 1..6, or null for body text.
     */
    fun headingLevelByFontSize(currentSize: Float, sortedDistinctSizes: List<Float>): Int? {
        if (sortedDistinctSizes.isEmpty()) return null

        val rank = sortedDistinctSizes.indexOf(currentSize)
        if (rank < 0) return null

        // The smallest entry (last in descending list) is body text → null
        if (rank == sortedDistinctSizes.lastIndex) return null

        // Clamp to H6
        return (rank + 1).coerceAtMost(6)
    }
}
