/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: FoldQueryRunner — applies the bundled `folds.scm`
 * Tree-Sitter query against a parsed document and returns the list of
 * [FoldRange]s the fold-gutter should render. Cross-platform expect
 * class with per-platform actuals:
 *
 *   - Android + Desktop (JVM): real implementation via the bonede
 *     io.github.bonede:tree-sitter binding (TSQuery + TSQueryCursor +
 *     TSQueryMatch + TSQueryCapture). See research-report.md §6.1.
 *
 *   - iOS: stub returning emptyList() per CONST-035 anti-bluff —
 *     genuine cinterop binding lands in iter-58 Phase 7 once the
 *     pre-existing iOS K/N baseline defect (#phase-7-blocked-on-ios-
 *     baseline / CONST-038 sibling submodule) unblocks
 *     :shared:compileKotlinIosArm64. Documented in KNOWN_DEFECTS.md
 *     entry `#f2-phase-3-bonede-query-api-gap`.
 *
 *   - Wasm: stub returning emptyList() per CONST-035 anti-bluff —
 *     the web target ships fold/outline affordances via web-tree-sitter
 *     in iter-58 Phase 6 per the plan; until then Wasm callers get an
 *     honest empty list (not a faked fold) and the editor's fold-gutter
 *     degrades gracefully to no folds on the Web target.
 *
 * Anti-bluff anchor (CONST-035): the JVM body MUST exercise the bonede
 * query API (real TSQuery + TSQueryCursor.nextMatch() loop). Stubbing
 * it to `return emptyList()` MUST cause the
 * `markdownHeadingProducesFoldRange` test in
 * `shared/src/desktopTest/.../FoldQueryRunnerTest.kt` to FAIL because
 * the input `# Heading\n\nLine 1\nLine 2\n` produces a `(section)`
 * node which the bundled `folds.scm` captures as `@fold`.
 *
 *########################################################*/
package digital.vasic.yole.language.affordance

import digital.vasic.yole.syntax.TokenizerEngine

/**
 * Runs the bundled `folds.scm` query against a parsed document and
 * returns the foldable regions.
 *
 * Construct one per editor surface (cheap; no native state). Call
 * [foldRangesFor] each time the document changes — the implementation
 * re-parses + re-queries from scratch every call (a future iteration
 * can wire incremental parse via tree-sitter's edit API).
 */
expect class FoldQueryRunner() {
    /**
     * Compute fold ranges for [text] using [engine]'s loaded grammar
     * for [langId].
     *
     * Pre-requisites: [engine.initialize] must have returned
     * `Result.success` and [engine.loadGrammar] must have been called
     * for [langId]. Callers MUST ensure both — this method does NOT
     * lazy-init.
     *
     * Returns an empty list if no folds are detected, or if the
     * platform actual is a stub (iOS / Wasm in Phase 3 v1).
     */
    suspend fun foldRangesFor(
        text: String,
        langId: String,
        engine: TokenizerEngine,
    ): List<FoldRange>
}
