/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: OutlineExtractor -- applies the bundled `outline.scm`
 * Tree-Sitter query against a parsed document and returns the list of
 * [OutlineItem]s the outline / breadcrumb UI should render. Cross-
 * platform expect class with per-platform actuals mirroring
 * [FoldQueryRunner]:
 *
 *   - Android + Desktop (JVM): real implementation via the bonede
 *     io.github.bonede:tree-sitter binding (TSQuery + TSQueryCursor +
 *     TSQueryMatch + TSQueryCapture). See research-report.md §6.1.
 *
 *   - iOS: stub returning emptyList() per CONST-035 anti-bluff --
 *     genuine cinterop binding lands in iter-58 Phase 7 once the
 *     pre-existing iOS K/N baseline defect unblocks
 *     :shared:compileKotlinIosArm64.
 *
 *   - Wasm: stub returning emptyList() per CONST-035 anti-bluff --
 *     web target ships outline via web-tree-sitter in iter-58 Phase 6.
 *
 * Anti-bluff anchor (CONST-035): the JVM body MUST exercise the bonede
 * query API. Stubbing it to `return emptyList()` MUST cause the
 * `markdownHeadingsProduceOutlineItems` test in
 * `shared/src/desktopTest/.../OutlineExtractorTest.kt` to FAIL because
 * the input `# H1\n\n## H2\n` produces two `(atx_heading)` nodes which
 * the bundled `outline.scm` captures as `@definition.section`.
 *
 *########################################################*/
package digital.vasic.yole.language.affordance

import digital.vasic.yole.syntax.TokenizerEngine

/**
 * Extracts outline items from a parsed source document.
 *
 * Construct one per editor surface (cheap; no native state). Call
 * [outlineFor] each time the document changes -- the implementation
 * re-parses + re-queries from scratch every call.
 */
expect class OutlineExtractor() {
    /**
     * Compute outline items for [text] using [engine]'s loaded grammar
     * for [langId].
     *
     * Pre-requisites: [engine.initialize] must have returned
     * `Result.success` and [engine.loadGrammar] must have been called
     * for [langId].
     *
     * Returns an empty list if no outline items are detected, or if the
     * platform actual is a stub (iOS / Wasm in Phase 3 v1).
     */
    suspend fun outlineFor(
        text: String,
        langId: String,
        engine: TokenizerEngine,
    ): List<OutlineItem>
}
