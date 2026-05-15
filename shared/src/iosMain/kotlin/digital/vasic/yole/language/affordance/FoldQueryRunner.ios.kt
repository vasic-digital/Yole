/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: iOS actual stub for [FoldQueryRunner].
 *
 * Returns `emptyList()` honestly per CONST-035 anti-bluff covenant.
 * The real cinterop binding (ts_query_new + ts_query_cursor_new +
 * ts_query_cursor_next_match -- see research-report.md §6.2) lands in
 * iter-58 Phase 7 once the pre-existing iOS K/N baseline defect
 * (#phase-7-blocked-on-ios-baseline, CONST-038 sibling submodule
 * Document-KMP iOS compile failure) unblocks the iOS source set.
 *
 * Phase 3 ships the scaffold so the cross-platform expect compiles
 * correctly on iOS. Editor surface code paths on iOS that consult
 * [foldRangesFor] receive an empty list and the fold-gutter degrades
 * gracefully (no folds shown) -- never a faked fold.
 *
 * Tracked in KNOWN_DEFECTS.md entry `#f2-phase-3-bonede-query-api-gap`.
 *
 *########################################################*/
package digital.vasic.yole.language.affordance

import digital.vasic.yole.syntax.TokenizerEngine

actual class FoldQueryRunner actual constructor() {
    actual suspend fun foldRangesFor(
        text: String,
        langId: String,
        engine: TokenizerEngine,
    ): List<FoldRange> = emptyList()
}
