/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: Wasm/JS actual stub for [OutlineExtractor].
 *
 * Returns `emptyList()` honestly per CONST-035 anti-bluff covenant.
 * Web target ships outline via web-tree-sitter in iter-58 Phase 6.
 * See KNOWN_DEFECTS.md `#f2-phase-3-bonede-query-api-gap`.
 *
 *########################################################*/
package digital.vasic.yole.language.affordance

import digital.vasic.yole.syntax.TokenizerEngine

actual class OutlineExtractor actual constructor() {
    actual suspend fun outlineFor(
        text: String,
        langId: String,
        engine: TokenizerEngine,
    ): List<OutlineItem> = emptyList()
}
