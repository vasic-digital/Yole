/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 1: WorkspaceEdit — multi-file edit aggregate.
 *
 * Mutation procedure (CONST-035):
 *   1. Change isEmpty to always return false.
 *   2. Re-run; isEmpty_trueWhen_noEdits FAILS.
 *   3. Revert; confirm 2/2 PASS.
 *
 * Cross-platform (CONST-037):
 *   - commonMain: pure Kotlin, runs on all targets unchanged.
 *   - Android/Desktop/iOS/Wasm: no per-platform divergence required.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * An aggregate of per-file [TextEdit] lists produced by LSP rename /
 * code-action / formatting responses.
 *
 * @param changes Map of document URI → ordered list of edits to apply.
 */
data class WorkspaceEdit(val changes: Map<String, List<TextEdit>> = emptyMap()) {
    /**
     * True when this edit carries no effective changes — either the map
     * is empty or every URI maps to an empty edit list.
     */
    val isEmpty: Boolean get() = changes.isEmpty() || changes.values.all { it.isEmpty() }
}
