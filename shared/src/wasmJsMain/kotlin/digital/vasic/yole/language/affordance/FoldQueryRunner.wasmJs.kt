/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: Wasm/JS actual stub for [FoldQueryRunner].
 *
 * Returns `emptyList()` honestly per CONST-035 anti-bluff covenant.
 * The Wasm target ships affordances (fold/outline) via web-tree-sitter
 * in iter-58 Phase 6 per the plan (see research-report.md §6.4 for
 * the second-engine decision rationale). Phase 3 ships the scaffold
 * so the cross-platform expect compiles correctly on Wasm; until
 * Phase 6 lands the editor surface on Web degrades gracefully (no
 * folds shown) -- never a faked fold.
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
