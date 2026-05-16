/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 1: WorkspaceEditApplier — applies a WorkspaceEdit to a
 * map of URI → source-text strings.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub apply() to return sources unchanged.
 *   2. Re-run; singleFile_singleEdit, multiFile, and conflict tests FAIL
 *      (3 of the 4 cases detect the stub; nonExistentUri passes the stub).
 *   3. Revert; confirm 4/4 PASS.
 *
 * Cross-platform (CONST-037):
 *   - commonMain: pure Kotlin object, runs on all targets unchanged.
 *   - Android/Desktop/iOS/Wasm: no per-platform divergence required.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Applies a [WorkspaceEdit] to a snapshot of source texts, producing
 * a new map with the modified files.  Untouched URIs are preserved as-is.
 *
 * URIs present in [edit] but absent from [sources] are silently skipped
 * (the server may refer to files the client has not opened).
 */
object WorkspaceEditApplier {

    /**
     * Thrown when two edits in the same file have overlapping ranges, which
     * would produce undefined behaviour if applied sequentially.
     */
    class ApplyConflict(message: String) : RuntimeException(message)

    /**
     * Applies [edit] to [sources] and returns a new map containing the
     * modified texts.  Files not referenced by [edit] are included
     * verbatim in the result.
     *
     * @throws ApplyConflict if any file contains overlapping [TextEdit] ranges.
     */
    fun apply(edit: WorkspaceEdit, sources: Map<String, String>): Map<String, String> {
        if (edit.isEmpty) return sources
        val result = sources.toMutableMap()
        for ((uri, edits) in edit.changes) {
            if (edits.isEmpty()) continue
            // Validate non-overlapping: sort by start offset, then check adjacency.
            val sorted = edits.sortedBy { it.range.first }
            for (i in 1 until sorted.size) {
                if (sorted[i - 1].range.last >= sorted[i].range.first) {
                    throw ApplyConflict("Overlapping edits in $uri")
                }
            }
            // Skip URIs not present in the sources map (honest: server may
            // reference unopened files).
            val source = result[uri] ?: continue
            // Apply in reverse order so earlier offsets stay valid.
            var modified = source
            for (e in sorted.reversed()) {
                modified = e.apply(modified)
            }
            result[uri] = modified
        }
        return result
    }
}
