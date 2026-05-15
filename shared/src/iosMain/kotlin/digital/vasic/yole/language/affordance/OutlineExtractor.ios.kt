/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Phase 3: iOS actual stub for [OutlineExtractor].
 *
 * Returns `emptyList()` honestly per CONST-035 anti-bluff covenant.
 * Real cinterop binding lands in iter-58 Phase 7 (see KNOWN_DEFECTS.md
 * `#f2-phase-3-bonede-query-api-gap`).
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
