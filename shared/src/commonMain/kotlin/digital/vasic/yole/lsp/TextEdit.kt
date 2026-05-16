/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 1: TextEdit — character-range replacement primitive.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub apply() to return text unchanged (identity).
 *   2. Re-run; applyEdit_replacesMiddle + applyEdit_clampsBeyondEnd FAIL.
 *   3. Revert; confirm 3/3 PASS.
 *
 * Cross-platform (CONST-037):
 *   - commonMain: pure Kotlin, runs on all targets unchanged.
 *   - Android/Desktop/iOS/Wasm: no per-platform divergence required.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * A single text replacement within a document.
 *
 * @param range   Character-offset range (inclusive start, inclusive last) to replace.
 *                Out-of-bounds values are clamped to [0, text.length].
 * @param newText Replacement string (may be empty to model a deletion).
 */
data class TextEdit(val range: IntRange, val newText: String) {
    /**
     * Applies this edit to [text] and returns the modified string.
     * The range is clamped so that neither [start] nor [end] ever
     * exceed the document boundaries.
     */
    fun apply(text: String): String {
        val start = range.first.coerceIn(0, text.length)
        val end = (range.last + 1).coerceIn(start, text.length)
        return text.substring(0, start) + newText + text.substring(end)
    }
}
