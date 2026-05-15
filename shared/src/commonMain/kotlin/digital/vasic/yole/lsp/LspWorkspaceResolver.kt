/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 2: LspWorkspaceResolver — walks up parent
 * directories from a given file looking for project markers
 * (Cargo.toml, go.mod, package.json, etc.) declared in
 * LspServerSpec.projectMarkers.
 *
 * Bounded walk: max 20 levels before giving up. Prevents
 * pathological deep-tree traversals and insulates against
 * walking past /home into / and beyond.
 *
 * Pure path manipulation — no filesystem mutation. Uses the
 * okio FileSystem abstraction (already a Yole dependency) so
 * the same code runs unchanged on Android, Desktop, iOS, Wasm.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub resolve() body to: return file.parent ?: file
 *   2. Re-run :shared:desktopTest --tests "...LspWorkspaceResolverTest"
 *   3. Expect: findsProjectMarker_inImmediateParent,
 *              findsProjectMarker_traversingMultipleLevels, and
 *              firstMatchingMarker_wins all FAIL (3 tests).
 *   4. Revert; confirm 5/5 PASS.
 *#######################################################*/
package digital.vasic.yole.lsp

import okio.FileSystem
import okio.Path

/**
 * Resolves the workspace root for an LSP session.
 *
 * Given [file] and a list of [markers] (e.g., `["Cargo.toml"]`), the resolver
 * walks up [file]'s parent chain looking for any directory containing one of the
 * markers. The first match (closest ancestor) is returned as the workspace root.
 * If no marker is found within [MAX_LEVELS] levels, [file.parent] is returned as
 * a safe fallback.
 *
 * The "closest ancestor wins" rule is intentional: a Cargo workspace nested inside
 * a Go module should resolve to the Cargo workspace, not the outer Go module root.
 */
object LspWorkspaceResolver {

    /** Maximum number of parent-directory levels to walk before falling back. */
    private const val MAX_LEVELS = 20

    /**
     * Resolves the workspace root for [file] using [markers] as project-root
     * indicators.
     *
     * @param file   Path to the file being edited.
     * @param markers File/directory names that indicate a project root when found
     *               in a directory (e.g., `"Cargo.toml"`, `"go.mod"`).
     * @param fs     FileSystem to use for existence checks. Defaults to
     *               [FileSystem.SYSTEM]; injectable for unit tests.
     * @return The closest ancestor directory containing any of [markers], or
     *         [file.parent] if no marker is found within [MAX_LEVELS] levels.
     */
    fun resolve(
        file: Path,
        markers: List<String>,
        fs: FileSystem = FileSystem.SYSTEM,
    ): Path {
        var current: Path = file.parent ?: return file
        var depth = 0
        while (depth < MAX_LEVELS) {
            for (marker in markers) {
                if (fs.exists(current / marker)) {
                    return current
                }
            }
            current = current.parent ?: break
            depth++
        }
        return file.parent ?: file
    }
}
